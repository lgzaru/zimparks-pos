package com.tenten.zimparks.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenten.zimparks.station.StationRepository;
import com.tenten.zimparks.transaction.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.paynow.responses.StatusResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlinePaymentService {

    private final OnlinePaymentRepository repository;
    private final PaynowGateway gateway;
    private final TransactionService transactionService;
    private final StationRepository stationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Initiates a mobile money payment push and persists a PENDING record.
     * Called immediately when the operator clicks "Send USSD Prompt" on the POS.
     */
    public OnlinePaymentResponse initiate(OnlinePaymentRequest req) {
        String txPayloadJson;
        try {
            txPayloadJson = objectMapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize transaction payload", e);
        }

        OnlinePayment op = OnlinePayment.builder()
                .cell(req.getCell())
                .amount(req.getAmount())
                .currency(req.getCurrency() != null ? req.getCurrency() : "USD")
                .status("PENDING")
                .paymentMethod(req.getPaymentMethod())
                .operatorUsername(req.getOperatorUsername())
                .txPayload(txPayloadJson)
                .build();

        op = gateway.initiatePayment(op);
        return toResponse(op);
    }

    /**
     * Handles the POST callback Paynow sends when payment status changes.
     * Also creates the POS transaction when status transitions to PAID.
     * PUBLIC endpoint — no JWT.
     */
    public OnlinePaymentResponse handleCallback(String paynowRef) {
        log.info("Paynow callback received for ref: {}", paynowRef);
        return pollAndUpdate(paynowRef);
    }

    /**
     * Returns the current status for frontend polling.
     * If still PENDING, also calls Paynow to refresh the status (drives transaction creation).
     * Requires JWT.
     */
    public OnlinePaymentResponse getStatus(String paynowRef) {
        OnlinePayment op = findOrThrow(paynowRef);

        if ("PENDING".equals(op.getStatus()) && op.getPollUrl() != null) {
            return pollAndUpdate(paynowRef);
        }
        return toResponse(op);
    }

    // ── internals ────────────────────────────────────────────────────────────

    private OnlinePaymentResponse pollAndUpdate(String paynowRef) {
        OnlinePayment op = findOrThrow(paynowRef);

        // Already resolved — skip to avoid double-processing
        String s = op.getStatus();
        if ("PAID".equals(s) || "FAILED".equals(s) || "CANCELLED".equals(s)) {
            return toResponse(op);
        }
        if (op.getPollUrl() == null) {
            return toResponse(op);
        }

        try {
            // Perform network call OUTSIDE of @Transactional
            StatusResponse status = gateway.pollStatus(op);
            
            // Apply updates in a transaction
            updatePaymentStatus(paynowRef, status);
            
            // Re-fetch to return latest state
            op = findOrThrow(paynowRef);
        } catch (Exception e) {
            log.error("Failed to poll/update status for {}: {}", paynowRef, e.getMessage(), e);
        }

        return toResponse(op);
    }

    @Transactional
    public void updatePaymentStatus(String paynowRef, StatusResponse status) {
        OnlinePayment op = findOrThrow(paynowRef);
        
        // Check status again inside transaction to prevent race conditions
        String s = op.getStatus();
        if ("PAID".equals(s) || "FAILED".equals(s) || "CANCELLED".equals(s)) {
            return;
        }

        op.setStatus(status.getStatus().name().toUpperCase());
        op.setDescription(status.getStatus().getDescription());
        if (status.getPaynowReference() != null) {
            op.setPaymentRef(status.getPaynowReference());
        }
        op.setUpdatedAt(LocalDateTime.now());

        if ("PAID".equals(op.getStatus()) && op.getTxRef() == null) {
            createTransactionForPayment(op);
        }

        repository.save(op);
        log.info("Online payment {} updated to {}", paynowRef, op.getStatus());
    }

    /**
     * Builds a Transaction from the stored JSON payload and creates it via TransactionService.
     * Uses the operator username stored at initiation time so we don't need a live JWT.
     */
    private void createTransactionForPayment(OnlinePayment op) {
        try {
            OnlinePaymentRequest req = objectMapper.readValue(op.getTxPayload(), OnlinePaymentRequest.class);
            Transaction tx = buildTransaction(req);
            Transaction saved = transactionService.createAsUser(tx, op.getOperatorUsername());
            op.setTxRef(saved.getRef());
            log.info("Created transaction {} for online payment {}", saved.getRef(), op.getPaynowRef());
        } catch (Exception e) {
            log.error("Transaction creation failed for paid online payment {}: {}", op.getPaynowRef(), e.getMessage(), e);
            op.setErrorMessage("Payment confirmed but transaction creation failed: " + e.getMessage());
            // Do not flip status to FAILED — payment WAS received; this is a system error to retry.
        }
    }

    private Transaction buildTransaction(OnlinePaymentRequest req) {
        List<PaymentBreakdown> breakdown = null;
        if (req.getBreakdown() != null) {
            breakdown = req.getBreakdown().stream()
                    .map(b -> PaymentBreakdown.builder()
                            .type(b.getType())
                            .currency(b.getCurrency())
                            .amount(b.getAmount())
                            .build())
                    .collect(Collectors.toList());
        }

        List<TransactionItem> itemsList = null;
        if (req.getItemsList() != null) {
            itemsList = req.getItemsList().stream()
                    .map(i -> TransactionItem.builder()
                            .productCode(i.getProductCode())
                            .descr(i.getDescr())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .totalPrice(i.getTotalPrice())
                            .hsCode(i.getHsCode())
                            .build())
                    .collect(Collectors.toList());
        }

        TransactionVehicle vehicle = null;
        if (req.getVehicle() != null && req.getVehicle().getPlate() != null
                && !req.getVehicle().getPlate().isBlank()) {
            vehicle = TransactionVehicle.builder()
                    .plate(req.getVehicle().getPlate())
                    .make(req.getVehicle().getMake())
                    .model(req.getVehicle().getModel())
                    .colour(req.getVehicle().getColour())
                    .build();
        }

        com.tenten.zimparks.station.Station station = null;
        if (req.getStationId() != null) {
            station = stationRepository.findById(req.getStationId()).orElse(null);
        }

        // txTotalAmount = full cart USD total; falls back to amount for pure online payments
        BigDecimal txAmount = req.getTxTotalAmount() != null ? req.getTxTotalAmount() : req.getAmount();

        return Transaction.builder()
                .productCode(req.getProductCode())
                .items(req.getItems())
                .amount(txAmount)
                .currency(req.getCurrency())
                .customerName(req.getCustomerName())
                .customerId(req.getCustomerId())
                .operatorName(req.getOperatorName())
                .shiftId(req.getShiftId())
                .virtualDeviceId(req.getVirtualDeviceId())
                .bankCode(req.getBankCode())
                .quotationRef(req.getQuotationRef())
                .station(station)
                .breakdown(breakdown)
                .itemsList(itemsList)
                .vehicle(vehicle)
                .build();
    }

    private OnlinePayment findOrThrow(String paynowRef) {
        return repository.findById(paynowRef)
                .orElseThrow(() -> new RuntimeException("Online payment not found: " + paynowRef));
    }

    private OnlinePaymentResponse toResponse(OnlinePayment op) {
        return OnlinePaymentResponse.builder()
                .paynowRef(op.getPaynowRef())
                .status(op.getStatus())
                .txRef(op.getTxRef())
                .description(op.getDescription())
                .cell(op.getCell())
                .amount(op.getAmount())
                .currency(op.getCurrency())
                .errorMessage(op.getErrorMessage())
                .build();
    }
}
