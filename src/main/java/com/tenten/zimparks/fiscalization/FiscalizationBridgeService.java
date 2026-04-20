package com.tenten.zimparks.fiscalization;

import com.tenten.zimparks.creditnote.CreditNote;
import com.tenten.zimparks.creditnote.CreditNoteRepository;
import com.tenten.zimparks.customer.CustomerRepository;
import com.tenten.zimparks.transaction.Receipt;
import com.tenten.zimparks.transaction.ReceiptRepository;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionItem;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FiscalizationBridgeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final TransactionRepository transactionRepository;

    @Value("${zimra.fiscalization.enabled:false}")
    private boolean fiscalizationEnabled;

    @Value("${zimra.fiscalization.credit-note.enabled:false}")
    private boolean creditNoteFiscalizationEnabled;

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
                .toList();
        List<FiscalizationValidationError> yellowErrors = validationErrors.stream()
                .filter(e -> e.getSeverity() == ValidationSeverity.YELLOW)
                .toList();
        List<FiscalizationValidationError> greyErrors = validationErrors.stream()
                .filter(e -> e.getSeverity() == ValidationSeverity.GREY)
                .toList();

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
            throw new FiscalizationRedErrorException(summary);
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

            try {
                log.info("[fiscalization] ZIMRA payload for tx {}:\n{}", tx.getRef(),
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody));
            } catch (JsonProcessingException ex) {
                log.warn("[fiscalization] could not serialize payload for tx {}: {}", tx.getRef(), ex.getMessage());
            }

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFiscalResponse(String txRef, Map<String, Object> response) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateFiscalStatus(String txRef, String status, String error) {
        receiptRepository.findById(txRef).ifPresent(receipt -> {
            receipt.setFiscalStatus(status);
            receipt.setFiscalError(error != null && error.length() > 500
                    ? error.substring(0, 500) : error);
            receiptRepository.save(receipt);
        });
    }

    private Map<String, Object> buildReceiptRequest(Transaction tx) {
        BigDecimal vatRate = tx.getVatRate();

        // Resolve receipt currency and amounts.
        // All receipts use a single currency (no mixed currencies per receipt).
        // If the customer paid in a non-USD currency (e.g. ZWG), receipt.originalAmount
        // holds the invoice total in that currency and we use it throughout.
        Receipt txReceipt = tx.getReceipt();
        final String currency;
        final BigDecimal totalAmount;
        final BigDecimal exchangeRate; // factor to convert stored USD line prices to receipt currency

        if (txReceipt != null
                && txReceipt.getOriginalCurrency() != null
                && !txReceipt.getOriginalCurrency().equalsIgnoreCase("USD")
                && txReceipt.getOriginalAmount() != null
                && txReceipt.getBaseAmount() != null
                && txReceipt.getBaseAmount().compareTo(BigDecimal.ZERO) != 0) {
            currency     = txReceipt.getOriginalCurrency();
            totalAmount  = txReceipt.getOriginalAmount();
            exchangeRate = txReceipt.getOriginalAmount()
                    .divide(txReceipt.getBaseAmount(), 10, RoundingMode.HALF_UP);
        } else {
            // USD transaction — use stored values directly, no conversion needed
            currency     = tx.getCurrency() != null ? tx.getCurrency() : defaultCurrency;
            totalAmount  = tx.getAmount();
            exchangeRate = BigDecimal.ONE;
        }

        // Recompute taxAmount directly from totalAmount in receipt currency.
        // ZIMRA validates: taxAmount = salesAmountWithTax × vatRate / (1 + vatRate).
        // Using the stored vatAmount (computed in USD then converted) causes rounding
        // divergence that triggers ReceiptsWithValidationErrors at day close.
        BigDecimal vatDivisor = vatRate.add(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal amountExclVat = totalAmount.divide(vatDivisor, 2, RoundingMode.HALF_UP);
        final BigDecimal taxAmount = totalAmount.subtract(amountExclVat);

        List<Map<String, Object>> receiptLines = new ArrayList<>();
        if (tx.getItemsList() != null && !tx.getItemsList().isEmpty()) {
            int lineNo = 1;
            for (TransactionItem item : tx.getItemsList()) {
                // Item prices are stored in USD; convert to receipt currency using exchange rate.
                BigDecimal linePrice = item.getUnitPrice().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineTotal = item.getTotalPrice().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("receiptLineType", receiptLineType);
                line.put("receiptLineNo", lineNo++);
                line.put("receiptLineHSCode", item.getHsCode() != null ? item.getHsCode() : hsCode);
                line.put("receiptLineName", item.getDescr());
                line.put("receiptLinePrice", linePrice);
                line.put("receiptLineQuantity", BigDecimal.valueOf(item.getQuantity()));
                line.put("receiptLineTotal", lineTotal);
                line.put("taxCode", taxCode);
                line.put("taxPercent", vatRate);
                line.put("taxID", taxId);
                receiptLines.add(line);
            }
        } else {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("receiptLineType", receiptLineType);
            line.put("receiptLineNo", 1);
            line.put("receiptLineHSCode", hsCode);  // fallback — no product to derive from
            line.put("receiptLineName", "Transaction " + tx.getRef());
            line.put("receiptLinePrice", totalAmount);
            line.put("receiptLineQuantity", BigDecimal.ONE);
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
            // All breakdown entries share the same currency (no mixed currencies).
            // Use originalAmount (tendered in receipt currency) for non-USD receipts;
            // fall back to the USD-converted amount for USD receipts.
            boolean useOriginal = !currency.equalsIgnoreCase("USD");

            BigDecimal breakdownSum = tx.getBreakdown().stream()
                    .map(b -> {
                        BigDecimal amt = useOriginal && b.getOriginalAmount() != null
                                ? b.getOriginalAmount() : b.getAmount();
                        return amt != null ? amt : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (var breakdown : tx.getBreakdown()) {
                BigDecimal brkAmt = useOriginal && breakdown.getOriginalAmount() != null
                        ? breakdown.getOriginalAmount() : breakdown.getAmount();

                Map<String, Object> payment = new LinkedHashMap<>();
                payment.put("moneyTypeCode", breakdown.getType());

                BigDecimal payAmt;
                if (breakdownSum.compareTo(totalAmount) > 0 && breakdownSum.compareTo(BigDecimal.ZERO) > 0) {
                    // Customer tendered more than invoice (cash change) — pro-rate to invoice total.
                    BigDecimal ratio = brkAmt.divide(breakdownSum, 10, RoundingMode.HALF_UP);
                    payAmt = totalAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                } else {
                    payAmt = brkAmt;
                }
                payment.put("paymentAmount", payAmt);
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
            // txDate format is "dd MMM yyyy", txTime format is "hh:mm a" (may be upper or lowercase am/pm)
            DateTimeFormatter formatter = new java.time.format.DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy hh:mm a")
                    .toFormatter(java.util.Locale.ENGLISH);
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
        if (tx.getOperatorUsername() != null) {
            receipt.put("username", tx.getOperatorUsername());
        }
        if (tx.getOperatorName() != null) {
            receipt.put("userNameSurname", tx.getOperatorName());
        }

        // buyerData — only included when customer has a TIN (RCPT043 requires both name + TIN)
        if (tx.getCustomerId() != null) {
            customerRepository.findById(tx.getCustomerId()).ifPresent(customer -> {
                if (customer.getTin() != null && !customer.getTin().isBlank()) {
                    Map<String, Object> buyerData = new LinkedHashMap<>();
                    buyerData.put("buyerRegisterName", customer.getName());
                    buyerData.put("buyerTIN", customer.getTin());
                    if (customer.getVatNumber() != null && !customer.getVatNumber().isBlank()) {
                        buyerData.put("VATNumber", customer.getVatNumber());
                    }
                    receipt.put("buyerData", buyerData);
                }
            });
        }

        return receipt;
    }

    // ── Credit Note Fiscalization ─────────────────────────────────────────────

    public void fiscalizeCreditNote(CreditNote cn) {
        if (!fiscalizationEnabled) {
            log.info("[fiscalization] disabled — skipping credit note: {}", cn.getId());
            return;
        }
        if (!creditNoteFiscalizationEnabled) {
            log.info("[fiscalization] credit note fiscalization disabled — skipping: {}", cn.getId());
            return;
        }

        Transaction originalTx = transactionRepository.findById(cn.getTxRef()).orElse(null);
        if (originalTx == null) {
            log.error("[fiscalization] credit note {} — original tx not found: {}", cn.getId(), cn.getTxRef());
            updateCreditNoteFiscalStatus(cn.getId(), "FAILED", "Original transaction not found");
            return;
        }

        if (originalTx.getVirtualDeviceId() == null) {
            log.warn("[fiscalization] credit note {} — no virtualDeviceId — skipping", cn.getId());
            return;
        }

        Receipt originalReceipt = receiptRepository.findById(cn.getTxRef()).orElse(null);
        if (originalReceipt == null || originalReceipt.getFiscalReceiptId() == null) {
            log.warn("[fiscalization] credit note {} — original receipt not fiscalized — cannot build creditDebitNote", cn.getId());
            updateCreditNoteFiscalStatus(cn.getId(), "FAILED", "Original receipt not fiscalized");
            return;
        }

        Integer deviceId;
        try {
            deviceId = Integer.parseInt(originalTx.getVirtualDeviceId());
        } catch (NumberFormatException e) {
            log.error("[fiscalization] invalid virtualDeviceId '{}' for credit note: {}", originalTx.getVirtualDeviceId(), cn.getId());
            updateCreditNoteFiscalStatus(cn.getId(), "FAILED", "Invalid virtualDeviceId");
            return;
        }

        updateCreditNoteFiscalStatus(cn.getId(), "PENDING", null);

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("deviceID", deviceId);
            requestBody.put("receipt", buildCreditNoteReceiptRequest(cn, originalTx, originalReceipt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            try {
                log.info("[fiscalization] ZIMRA payload for credit note {}:\n{}", cn.getId(),
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody));
            } catch (JsonProcessingException ex) {
                log.warn("[fiscalization] could not serialize payload for credit note {}: {}", cn.getId(), ex.getMessage());
            }

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    gatewayUrl + "/api/fiscal/submit-receipt", entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                persistCreditNoteFiscalResponse(cn.getId(), response.getBody());
                log.info("[fiscalization] credit note {} fiscalized successfully", cn.getId());
            } else {
                updateCreditNoteFiscalStatus(cn.getId(), "FAILED", "Non-2xx: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            log.error("[fiscalization] gateway error for credit note {} — {} {}", cn.getId(), e.getStatusCode(), body);
            updateCreditNoteFiscalStatus(cn.getId(), "FAILED",
                    e.getStatusCode() + ": " + body.substring(0, Math.min(400, body.length())));
        } catch (Exception e) {
            log.error("[fiscalization] credit note {} failed — {}", cn.getId(), e.getMessage(), e);
            updateCreditNoteFiscalStatus(cn.getId(), "FAILED",
                    e.getMessage() != null && e.getMessage().length() > 500
                            ? e.getMessage().substring(0, 500) : e.getMessage());
        }
    }

    private Map<String, Object> buildCreditNoteReceiptRequest(CreditNote cn, Transaction originalTx, Receipt originalReceipt) {
        // Use original currency and compute credit amount in that currency
        String currency = originalReceipt.getOriginalCurrency() != null
                ? originalReceipt.getOriginalCurrency()
                : (originalTx.getCurrency() != null ? originalTx.getCurrency() : defaultCurrency);

        BigDecimal originalAmount = originalReceipt.getOriginalAmount() != null
                ? originalReceipt.getOriginalAmount() : originalReceipt.getBaseAmount();

        // Apply deduction percentage (0 for National Disaster, 15 for Personal Illness)
        BigDecimal deductionPct = cn.getDeductionPercentage() != null ? cn.getDeductionPercentage() : BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.ONE.subtract(deductionPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal creditAmount = originalAmount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        // VAT portion (tax-inclusive)
        BigDecimal vatRate = originalReceipt.getVatRate() != null ? originalReceipt.getVatRate() : BigDecimal.ZERO;
        BigDecimal vatDivisor = vatRate.add(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal amountExclVat = creditAmount.divide(vatDivisor, 2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = creditAmount.subtract(amountExclVat);

        String receiptDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        // Receipt line — price and total must be negative for CreditNote (RCPT022)
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("receiptLineType", "Sale");
        line.put("receiptLineNo", 1);
        line.put("receiptLineHSCode", hsCode);
        line.put("receiptLineName", cn.getReason().getDescription());
        line.put("receiptLinePrice", creditAmount.negate());
        line.put("receiptLineQuantity", BigDecimal.ONE);
        line.put("receiptLineTotal", creditAmount.negate());
        line.put("taxCode", taxCode);
        line.put("taxPercent", vatRate);
        line.put("taxID", taxId);

        // Tax summary — negative amounts
        Map<String, Object> receiptTax = new LinkedHashMap<>();
        receiptTax.put("taxCode", taxCode);
        receiptTax.put("taxPercent", vatRate);
        receiptTax.put("taxID", taxId);
        receiptTax.put("taxAmount", vatAmount.negate());
        receiptTax.put("salesAmountWithTax", creditAmount.negate());

        // Payment — must be negative for CreditNote (RCPT028)
        String paymentType = originalTx.getBreakdown() != null && !originalTx.getBreakdown().isEmpty()
                ? originalTx.getBreakdown().get(0).getType() : defaultPaymentType;
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("moneyTypeCode", paymentType);
        payment.put("paymentAmount", creditAmount.negate());

        // creditDebitNote — links to original FDMS receipt (RCPT015, mandatory for CreditNote)
        Map<String, Object> creditDebitNote = new LinkedHashMap<>();
        creditDebitNote.put("receiptID", originalReceipt.getFiscalReceiptId());

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("receiptType", "CreditNote");
        receipt.put("receiptCurrency", currency);
        receipt.put("invoiceNo", cn.getId());
        receipt.put("receiptNotes", cn.getReason().getDescription()); // RCPT034 mandatory for CreditNote
        receipt.put("receiptDate", receiptDate);
        receipt.put("creditDebitNote", creditDebitNote);             // RCPT015 mandatory for CreditNote
        receipt.put("receiptLinesTaxInclusive", true);
        receipt.put("receiptLines", List.of(line));
        receipt.put("receiptTaxes", List.of(receiptTax));
        receipt.put("receiptPayments", List.of(payment));
        receipt.put("receiptTotal", creditAmount.negate());          // RCPT040 must be <= 0
        receipt.put("receiptPrintForm", receiptPrintForm);
        if (cn.getApprovedBy() != null) {
            receipt.put("username", cn.getApprovedBy());
        }

        return receipt;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCreditNoteFiscalStatus(String cnId, String status, String error) {
        creditNoteRepository.findById(cnId).ifPresent(cn -> {
            cn.setFiscalStatus(status);
            cn.setFiscalError(error != null && error.length() > 500 ? error.substring(0, 500) : error);
            creditNoteRepository.save(cn);
        });
    }

    @SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistCreditNoteFiscalResponse(String cnId, Map<String, Object> response) {
        creditNoteRepository.findById(cnId).ifPresent(cn -> {
            cn.setFiscalReceiptId(response.get("receiptID") != null
                    ? Long.valueOf(response.get("receiptID").toString()) : null);
            cn.setFiscalOperationId((String) response.get("operationID"));
            cn.setFiscalStatus("SUCCESS");
            cn.setFiscalError(null);
            creditNoteRepository.save(cn);
            log.info("[fiscalization] credit note {} updated — fiscalReceiptId: {}", cnId, cn.getFiscalReceiptId());
        });
    }
}



/*
ZIMRA HS Codes are 8-digit international standards for classifying traded goods to determine customs duties, while domestic VAT fiscalisation often uses simplified or mapped codes for local sales.

Hs-codes  - international standards
99001000 – Services at 15.5% VAT
99002000 – Services at 0% VAT
99003000 – Services — VAT Exempt

Codes - Local goog
Code - A (FR(l)): Provision of Services/Sales at 15% VAT (Standard Rate).
Code - B (FR(m)): Provision of Services/Sales at 0% VAT (Zero Rated).
Code - C (FR(n)): Provision of Services/Sales at VAT Exempted.
Code - D: Often used for specific zero-rated or exempt transactions depending on the fiscal machine setting (usually corresponds to specific VAT categories).
*/
