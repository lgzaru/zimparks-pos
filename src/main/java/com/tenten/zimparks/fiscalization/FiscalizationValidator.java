package com.tenten.zimparks.fiscalization;

import com.tenten.zimparks.transaction.PaymentBreakdown;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pre-flight validation of a Transaction before it is sent to the ZIMRA FDMS gateway.
 * All checks are pure in-memory — no DB calls — to keep the transaction response fast.
 *
 * Implements the locally-evaluable checks from spec §8.2.1.
 * Error codes and severity colours follow the spec table:
 *   RED    – major; blocks fiscalization and fiscal day auto-close.
 *   YELLOW – minor; fiscal day may still auto-close.
 *
 * Checks deliberately omitted (require DB or FDMS-side state):
 *   RCPT011/RCPT012 – sequential receiptCounter / receiptGlobalNo  (counters not stored)
 *   RCPT013         – invoice number uniqueness                     (needs DB lookup)
 *   RCPT014/RCPT041 – date vs fiscal day open/close time            (fiscal day times not stored)
 *   RCPT015/RCPT029/RCPT032-036/RCPT042 – credit/debit note rules  (credit note type not in Transaction)
 *   RCPT020         – invoice signature                             (verified by FDMS)
 *   RCPT021         – VAT used but taxpayer not VAT registered       (VAT status not stored)
 *   RCPT025         – invalid tax code                               (valid codes are FDMS-side config)
 *   RCPT030         – date earlier than previous receipt             (needs DB lookup)
 *   RCPT043         – mandatory buyer data fields                    (field set not specified locally)
 *   RCPT047/RCPT048 – HS code presence/length                       (supplied via gateway config)
 */
@Service
@Slf4j
public class FiscalizationValidator {

    private static final DateTimeFormatter TX_DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Allow 1 cent tolerance for tax/total comparisons to absorb rounding
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    public List<FiscalizationValidationError> validate(Transaction tx) {
        List<FiscalizationValidationError> errors = new ArrayList<>();

        // ── Device sanity (prerequisite, not a spec RCPT code) ────────
        if (tx.getVirtualDeviceId() == null || tx.getVirtualDeviceId().isBlank()) {
            errors.add(red("DEVICE_01", "virtualDeviceId is required for fiscalization"));
            return errors;
        }
        try {
            Integer.parseInt(tx.getVirtualDeviceId());
        } catch (NumberFormatException e) {
            errors.add(red("DEVICE_02",
                    "virtualDeviceId must be a valid integer, got: " + tx.getVirtualDeviceId()));
            return errors;
        }

        // ── RCPT010 (Red) — Wrong currency code ───────────────────────
        if (tx.getCurrency() == null || tx.getCurrency().isBlank()) {
            errors.add(red("RCPT010", "Currency is required"));
        } else if (!tx.getCurrency().matches("[A-Z]{3}")) {
            errors.add(red("RCPT010",
                    "Currency must be a 3-letter ISO 4217 code, got: " + tx.getCurrency()));
        }

        // ── RCPT016 (Red) — No receipt lines provided ─────────────────
        if (tx.getItemsList() == null || tx.getItemsList().isEmpty()) {
            errors.add(red("RCPT016", "At least one receipt line is required"));
        }

        // ── RCPT017 (Red) — Tax information not provided ─────────────
        if (tx.getVatRate() == null) {
            errors.add(red("RCPT017", "Tax information is not provided: vatRate is required"));
        }
        if (tx.getVatAmount() == null) {
            errors.add(red("RCPT017", "Tax information is not provided: vatAmount is required"));
        }

        // ── RCPT018 (Red) — Payment information not provided ──────────
        // Absent breakdown defaults to Cash (acceptable). If present, each entry needs a type.
        if (tx.getBreakdown() != null && !tx.getBreakdown().isEmpty()) {
            tx.getBreakdown().forEach(b -> {
                if (b.getType() == null || b.getType().isBlank()) {
                    errors.add(red("RCPT018",
                            "Payment type (moneyTypeCode) is required for every payment entry"));
                }
            });
        }

        // ── RCPT040 (Red) — Invoice total must be >= 0 ───────────────
        if (tx.getAmount() == null) {
            errors.add(red("RCPT040", "Invoice total amount is required"));
        } else if (tx.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(red("RCPT040",
                    "Invoice total amount must be greater than or equal to 0"));
        }

        // ── Per-line validations ───────────────────────────────────────
        if (tx.getItemsList() != null && !tx.getItemsList().isEmpty()) {
            int lineNo = 1;
            for (TransactionItem item : tx.getItemsList()) {

                // RCPT022 (Red) — sales line price must be > 0
                if (item.getUnitPrice() == null
                        || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(red("RCPT022",
                            "Line " + lineNo + ": unit price must be greater than 0"));
                }

                // RCPT023 (Red) — quantity must be positive
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    errors.add(red("RCPT023",
                            "Line " + lineNo + ": quantity must be positive"));
                }

                // RCPT024 (Red) — line total must equal unit price * quantity
                if (item.getUnitPrice() != null && item.getQuantity() != null
                        && item.getTotalPrice() != null) {
                    BigDecimal expected = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal actual = item.getTotalPrice().setScale(2, RoundingMode.HALF_UP);
                    if (expected.compareTo(actual) != 0) {
                        errors.add(red("RCPT024",
                                "Line " + lineNo + ": total " + actual
                                        + " != unitPrice * quantity = " + expected));
                    }
                }

                if (item.getDescr() == null || item.getDescr().isBlank()) {
                    errors.add(red("LINE_01",
                            "Line " + lineNo + ": description is required"));
                }

                lineNo++;
            }
        }

        // ── RCPT019 (Red) — Invoice total != sum of all invoice lines ─
        // Also covers RCPT027 and RCPT037 for the tax-inclusive model:
        // each line's totalPrice already includes VAT so lineSum == invoiceTotal.
        if (tx.getAmount() != null
                && tx.getItemsList() != null && !tx.getItemsList().isEmpty()) {
            BigDecimal lineSum = tx.getItemsList().stream()
                    .filter(i -> i.getTotalPrice() != null)
                    .map(TransactionItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal txTotal = tx.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (txTotal.subtract(lineSum).abs().compareTo(TOLERANCE) > 0) {
                errors.add(red("RCPT019",
                        "Invoice total " + txTotal + " != sum of receipt lines " + lineSum));
            }
        }

        // ── RCPT026 (Red) — Incorrectly calculated tax amount ─────────
        // Tax-inclusive formula: vatAmount = totalAmount * vatRate / (100 + vatRate)
        if (tx.getAmount() != null && tx.getVatRate() != null && tx.getVatAmount() != null
                && tx.getVatRate().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = tx.getVatRate();
            BigDecimal expectedVat = tx.getAmount()
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100).add(rate), 2, RoundingMode.HALF_UP);
            BigDecimal actualVat = tx.getVatAmount().setScale(2, RoundingMode.HALF_UP);
            if (expectedVat.subtract(actualVat).abs().compareTo(TOLERANCE) > 0) {
                errors.add(red("RCPT026",
                        "Tax amount " + actualVat + " != expected " + expectedVat
                                + " (totalAmount * vatRate / (100 + vatRate))"));
            }
        }

        // ── RCPT028 (Red) — Payment amount must be >= 0 ───────────────
        if (tx.getBreakdown() != null) {
            tx.getBreakdown().forEach(b -> {
                if (b.getAmount() == null || b.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(red("RCPT028",
                            "Payment amount must be greater than or equal to 0"));
                }
            });
        }

        // ── RCPT039 (Red) — Invoice total != sum of all payment amounts
        if (tx.getAmount() != null
                && tx.getBreakdown() != null && !tx.getBreakdown().isEmpty()) {
            BigDecimal paymentTotal = tx.getBreakdown().stream()
                    .filter(b -> b.getAmount() != null)
                    .map(PaymentBreakdown::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal txAmount = tx.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (txAmount.compareTo(paymentTotal) != 0) {
                errors.add(red("RCPT039",
                        "Payment total " + paymentTotal + " != invoice total " + txAmount));
            }
        }

        // ── Date / time presence ───────────────────────────────────────
        if (tx.getTxDate() == null || tx.getTxDate().isBlank()) {
            errors.add(red("DATE_01", "Transaction date is required"));
        }
        if (tx.getTxTime() == null || tx.getTxTime().isBlank()) {
            errors.add(red("DATE_02", "Transaction time is required"));
        }

        // ── RCPT031 (Yellow) — Invoice submitted with a future date ───
        LocalDate txDate = parseTxDate(tx.getTxDate());
        if (txDate != null && txDate.isAfter(LocalDate.now())) {
            errors.add(yellow("RCPT031",
                    "Invoice date " + tx.getTxDate() + " is in the future"));
        }

        return errors;
    }

    /** Returns true only when there are no RED errors. YELLOW is a warning, not a blocker. */
    public boolean isValid(Transaction tx) {
        return validate(tx).stream()
                .noneMatch(e -> e.getSeverity() == ValidationSeverity.RED);
    }

    public boolean hasRedErrors(List<FiscalizationValidationError> errors) {
        return errors.stream().anyMatch(e -> e.getSeverity() == ValidationSeverity.RED);
    }

    private LocalDate parseTxDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, TX_DATE_FMT);
        } catch (DateTimeParseException e) {
            log.warn("[fiscalization-validation] unparseable txDate: '{}'", dateStr);
            return null;
        }
    }

    private FiscalizationValidationError red(String code, String message) {
        log.warn("[fiscalization-validation] RED    {} — {}", code, message);
        return new FiscalizationValidationError(code, message, ValidationSeverity.RED);
    }

    private FiscalizationValidationError yellow(String code, String message) {
        log.warn("[fiscalization-validation] YELLOW {} — {}", code, message);
        return new FiscalizationValidationError(code, message, ValidationSeverity.YELLOW);
    }
}
