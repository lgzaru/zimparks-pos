package com.tenten.zimparks.fiscalization;

import com.tenten.zimparks.transaction.ReceiptRepository;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FiscalizationBridgeService {

    private final RestTemplate restTemplate;
    private final ReceiptRepository receiptRepository;

    @Value("${zimra.fiscalization.enabled:false}")
    private boolean fiscalizationEnabled;

    @Value("${fiscalization.base-url:http://localhost:8087}")
    private String gatewayUrl;

    @Value("${zimra.receipt.hs-code:99999999}")
    private String hsCode;

    @Value("${zimra.receipt.tax-code:A}")
    private String taxCode;

    @Value("${zimra.receipt.tax-id:517}")
    private Integer taxId;

    @Value("${zimra.receipt.print-form:Receipt48}")
    private String receiptPrintForm;

    @Value("${zimra.receipt.line-type:Sale}")
    private String receiptLineType;

    @Value("${zimra.receipt.type:FiscalInvoice}")
    private String receiptType;

    @Value("${zimra.receipt.default-payment-type:Cash}")
    private String defaultPaymentType;

    @Value("${zimra.receipt.currency:USD}")
    private String defaultCurrency;

    private final FiscalizationValidator validator;

    public void fiscalize(Transaction tx) {
        if (!fiscalizationEnabled) {
            log.info("[fiscalization] disabled — skipping for tx: {}", tx.getRef());
            return;
        }

        if (tx.getVirtualDeviceId() == null) {
            log.warn("[fiscalization] no virtualDeviceId on tx: {} — skipping", tx.getRef());
            return;
        }

        // Validate before attempting — errors are categorised RED / YELLOW / GREY per spec §8.2.1
        List<FiscalizationValidationError> validationErrors = validator.validate(tx);

        List<FiscalizationValidationError> redErrors = validationErrors.stream()
                .filter(e -> e.getSeverity() == ValidationSeverity.RED)
                .collect(Collectors.toList());
        List<FiscalizationValidationError> yellowErrors = validationErrors.stream()
                .filter(e -> e.getSeverity() == ValidationSeverity.YELLOW)
                .collect(Collectors.toList());
        List<FiscalizationValidationError> greyErrors = validationErrors.stream()
                .filter(e -> e.getSeverity() == ValidationSeverity.GREY)
                .collect(Collectors.toList());

        if (!redErrors.isEmpty()) {
            // RED — major violations; abort and mark FAILED
            String summary = redErrors.stream()
                    .map(e -> e.getCode() + ": " + e.getMessage())
                    .collect(Collectors.joining(" | "));
            log.warn("╔══════════════════════════════════════════════════");
            log.warn("║ FISCALIZATION BLOCKED — RED VALIDATION ERRORS");
            log.warn("║ Transaction : {}", tx.getRef());
            log.warn("║ Device      : {}", tx.getVirtualDeviceId());
            log.warn("║ Errors      : {}", summary);
            log.warn("╚══════════════════════════════════════════════════");
            updateFiscalStatus(tx.getRef(), "FAILED", "Validation: " + summary);
            return;
        }

        if (!yellowErrors.isEmpty()) {
            // YELLOW — minor violations; log but proceed (fiscal day may still auto-close)
            String summary = yellowErrors.stream()
                    .map(e -> e.getCode() + ": " + e.getMessage())
                    .collect(Collectors.joining(" | "));
            log.warn("[fiscalization] YELLOW warnings for tx {} — proceeding: {}", tx.getRef(), summary);
        }

        if (!greyErrors.isEmpty()) {
            // GREY — chain-integrity issues; FDMS will re-evaluate when missing receipt arrives
            String summary = greyErrors.stream()
                    .map(e -> e.getCode() + ": " + e.getMessage())
                    .collect(Collectors.joining(" | "));
            log.warn("[fiscalization] GREY chain warnings for tx {} — submitting for FDMS revalidation: {}",
                    tx.getRef(), summary);
        }

        updateFiscalStatus(tx.getRef(), "PENDING", null);

        Integer deviceId;
        try {
            deviceId = Integer.parseInt(tx.getVirtualDeviceId());
        } catch (NumberFormatException e) {
            log.error("[fiscalization] invalid virtualDeviceId '{}' on tx: {}",
                    tx.getVirtualDeviceId(), tx.getRef());
            updateFiscalStatus(tx.getRef(), "FAILED",
                    "Invalid virtualDeviceId: " + tx.getVirtualDeviceId());
            return;
        }

        try {
            // Wrap receipt with deviceID for admin endpoint
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("deviceID", deviceId);
            requestBody.put("receipt", buildReceiptRequest(tx));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = gatewayUrl + "/api/fiscal/submit-receipt";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                persistFiscalResponse(tx.getRef(), response.getBody());
                log.info("[fiscalization] tx {} fiscalized successfully", tx.getRef());
            } else {
                updateFiscalStatus(tx.getRef(), "FAILED",
                        "Non-2xx response: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            // Gateway returned 4xx/5xx — log full error body
            log.error("[fiscalization] gateway returned error for tx: {} — {} {}",
                    tx.getRef(), e.getStatusCode(), e.getResponseBodyAsString());
            updateFiscalStatus(tx.getRef(), "FAILED",
                    e.getStatusCode() + ": " + e.getResponseBodyAsString().substring(
                            0, Math.min(400, e.getResponseBodyAsString().length())));

        } catch (Exception e) {
            // Any other failure — network, parse error etc
            log.error("[fiscalization] failed for tx: {} — {}", tx.getRef(), e.getMessage(), e);
            updateFiscalStatus(tx.getRef(), "FAILED", e.getMessage() != null
                    && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500) : e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void persistFiscalResponse(String txRef, Map<String, Object> response) {
        receiptRepository.findById(txRef).ifPresent(receipt -> {
            receipt.setFiscalReceiptId(response.get("receiptID") != null
                    ? Long.valueOf(response.get("receiptID").toString()) : null);
            receipt.setFiscalOperationId((String) response.get("operationID"));
            receipt.setFiscalQrUrl((String) response.get("receiptQrUrl"));
            receipt.setFiscalVerificationCode((String) response.get("verificationCode"));
            receipt.setFiscalDay(response.get("fiscalDayNo") != null
                    ? response.get("fiscalDayNo").toString() : null);
            receipt.setFiscalStatus("SUCCESS");
            receipt.setFiscalError(null);
            receiptRepository.save(receipt);
            log.info("[fiscalization] receipt {} updated with fiscal data — fiscalDay: {}",
                    txRef, receipt.getFiscalDay());
        });
    }

    private void updateFiscalStatus(String txRef, String status, String error) {
        receiptRepository.findById(txRef).ifPresent(receipt -> {
            receipt.setFiscalStatus(status);
            receipt.setFiscalError(error != null && error.length() > 500
                    ? error.substring(0, 500) : error);
            receiptRepository.save(receipt);
        });
    }

    private Map<String, Object> buildReceiptRequest(Transaction tx) {
        double totalAmount = tx.getAmount().doubleValue();
        double vatRate     = tx.getVatRate().doubleValue();
        double taxAmount   = tx.getVatAmount().doubleValue();
        String currency    = tx.getCurrency() != null ? tx.getCurrency() : defaultCurrency;

        List<Map<String, Object>> receiptLines = new ArrayList<>();
        if (tx.getItemsList() != null && !tx.getItemsList().isEmpty()) {
            int lineNo = 1;
            for (TransactionItem item : tx.getItemsList()) {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("receiptLineType", receiptLineType);
                line.put("receiptLineNo", lineNo++);
                line.put("receiptLineHSCode", hsCode);
                line.put("receiptLineName", item.getDescr());
                line.put("receiptLinePrice", item.getUnitPrice().doubleValue());
                line.put("receiptLineQuantity", item.getQuantity().doubleValue());
                line.put("receiptLineTotal", item.getTotalPrice().doubleValue());
                line.put("taxCode", taxCode);
                line.put("taxPercent", vatRate);
                line.put("taxID", taxId);
                receiptLines.add(line);
            }
        } else {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("receiptLineType", receiptLineType);
            line.put("receiptLineNo", 1);
            line.put("receiptLineHSCode", hsCode);
            line.put("receiptLineName", "Transaction " + tx.getRef());
            line.put("receiptLinePrice", totalAmount);
            line.put("receiptLineQuantity", 1.0);
            line.put("receiptLineTotal", totalAmount);
            line.put("taxCode", taxCode);
            line.put("taxPercent", vatRate);
            line.put("taxID", taxId);
            receiptLines.add(line);
        }

        Map<String, Object> receiptTax = new LinkedHashMap<>();
        receiptTax.put("taxCode", taxCode);
        receiptTax.put("taxPercent", vatRate);
        receiptTax.put("taxID", taxId);
        receiptTax.put("taxAmount", taxAmount);
        receiptTax.put("salesAmountWithTax", totalAmount);

        List<Map<String, Object>> payments = new ArrayList<>();
        if (tx.getBreakdown() != null && !tx.getBreakdown().isEmpty()) {
            for (var breakdown : tx.getBreakdown()) {
                Map<String, Object> payment = new LinkedHashMap<>();
                payment.put("moneyTypeCode", breakdown.getType());
                payment.put("paymentAmount", breakdown.getAmount().doubleValue());
                payments.add(payment);
            }
        } else {
            Map<String, Object> payment = new LinkedHashMap<>();
            payment.put("moneyTypeCode", defaultPaymentType);
            payment.put("paymentAmount", totalAmount);
            payments.add(payment);
        }

        String receiptDate;
        try {
            // txDate format is "dd MMM yyyy", txTime format is "hh:mm a"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a");
            LocalDateTime dateTime = LocalDateTime.parse(tx.getTxDate() + " " + tx.getTxTime(), formatter);
            receiptDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e) {
            log.warn("[fiscalization] failed to parse tx date/time '{}' '{}' — using now",
                    tx.getTxDate(), tx.getTxTime());
            receiptDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        }

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("receiptType", receiptType);
        receipt.put("receiptCurrency", currency);
        receipt.put("invoiceNo", tx.getRef());
        receipt.put("receiptDate", receiptDate);
        receipt.put("receiptLinesTaxInclusive", true);
        receipt.put("receiptLines", receiptLines);
        receipt.put("receiptTaxes", List.of(receiptTax));
        receipt.put("receiptPayments", payments);
        receipt.put("receiptTotal", totalAmount);
        receipt.put("receiptPrintForm", receiptPrintForm);

        return receipt;
    }
}