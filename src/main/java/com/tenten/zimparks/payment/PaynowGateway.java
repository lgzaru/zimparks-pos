package com.tenten.zimparks.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import zw.co.paynow.constants.MobileMoneyMethod;
import zw.co.paynow.core.Payment;
import zw.co.paynow.core.Paynow;
import zw.co.paynow.responses.MobileInitResponse;
import zw.co.paynow.responses.StatusResponse;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Wraps the Paynow Java SDK for mobile money USSD push payments.
 * Supports EcoCash (077/078), OneMoney (071), and TeleCash (073).
 *
 * @see <a href="https://developers.paynow.co.zw/docs/java_quickstart.html">Paynow Java Quickstart</a>
 */
@Slf4j
@Service
public class PaynowGateway {

    private final OnlinePaymentRepository repository;
    private final Paynow paynow;

    @Value("${paynow.result.url}")
    private String resultUrl;

    @Value("${paynow.merchant.email:pos@zimparks.co.zw}")
    private String merchantEmail;

    public PaynowGateway(OnlinePaymentRepository repository,
                         @Value("${paynow.integration.id}") String integrationId,
                         @Value("${paynow.integration.key}") String integrationKey) {
        this.repository = repository;
        this.paynow = new Paynow(integrationId, integrationKey);
    }

    /**
     * Initiates a USSD payment push to the customer's phone.
     * Sets paynowRef, pollUrl, paymentRef, status, and description on the entity and persists it.
     */
    public OnlinePayment initiatePayment(OnlinePayment op) {
        op.setPaynowRef(generateRef());
        paynow.setResultUrl(resultUrl + "/api/online-payments/callback/" + op.getPaynowRef());

        try {
            String cell = normalizeCell(op.getCell());
            op.setCell(cell);

            Payment payment = paynow.createPayment("ZIMPARKS-" + op.getPaynowRef(), merchantEmail);
            payment.add("Park Entry / Service Fee", op.getAmount());

            MobileInitResponse response = sendMobile(payment, cell, op.getPaymentMethod());

            if (response != null && response.success()) {
                StatusResponse status = paynow.pollTransaction(response.pollUrl());
                op.setPollUrl(response.pollUrl());
                op.setPaymentRef(status.getPaynowReference());
                op.setStatus(status.getStatus().name().toUpperCase());
                op.setDescription(status.getStatus().getDescription());
                log.info("Payment initiated — ref: {}, status: {}, cell: {}", op.getPaynowRef(), op.getStatus(), cell);
            } else {
                String errMsg = response != null ? response.getStatus().getDescription() : "No response from Paynow";
                op.setStatus("FAILED");
                op.setErrorMessage(errMsg);
                log.error("Paynow initiation failed for ref {}: {}", op.getPaynowRef(), errMsg);
            }
        } catch (IllegalArgumentException e) {
            op.setStatus("FAILED");
            op.setErrorMessage(e.getMessage());
            log.warn("Invalid phone number for payment {}: {}", op.getPaynowRef(), e.getMessage());
        } catch (Exception e) {
            op.setStatus("FAILED");
            op.setErrorMessage("Payment processing error: " + e.getMessage());
            log.error("Unexpected error initiating payment {}: {}", op.getPaynowRef(), e.getMessage(), e);
        }

        op.setCreatedAt(LocalDateTime.now());
        return repository.save(op);
    }

    /**
     * Polls Paynow for the latest status of an in-progress payment.
     */
    public StatusResponse pollStatus(OnlinePayment op) {
        return paynow.pollTransaction(op.getPollUrl());
    }

    // ── internals ────────────────────────────────────────────────────────────

    private String generateRef() {
        // Short unique ref: PN- + last 10 digits of epoch millis
        String epoch = String.valueOf(Instant.now().toEpochMilli());
        return "PN-" + epoch.substring(epoch.length() - 10);
    }

    /**
     * Normalises Zimbabwean phone numbers to 07XXXXXXXX format required by Paynow.
     * Accepts: +263XXXXXXXXX, 263XXXXXXXXX, 07XXXXXXXX, 7XXXXXXXX
     */
    private String normalizeCell(String rawCell) {
        if (rawCell == null || rawCell.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be blank");
        }
        String digits = rawCell.trim().replaceAll("\\s+", "");

        if (digits.startsWith("+263")) {
            digits = "0" + digits.substring(4);
        } else if (digits.startsWith("263") && digits.length() == 12) {
            digits = "0" + digits.substring(3);
        }

        if (digits.length() == 9 && !digits.startsWith("0")) {
            digits = "0" + digits;
        }

        if (!digits.startsWith("0") || digits.length() != 10) {
            throw new IllegalArgumentException(
                "Unsupported phone format: " + rawCell + ". Use 07XXXXXXXX (e.g. 0771234567).");
        }
        return digits;
    }

    private MobileInitResponse sendMobile(Payment payment, String cell, String explicitMethod) {
        // Prefer operator-selected method; fall back to phone-prefix detection
        if (explicitMethod != null) {
            switch (explicitMethod.toUpperCase()) {
                case "ECOCASH": return paynow.sendMobile(payment, cell, MobileMoneyMethod.ECOCASH);
                case "ONEMONEY": return paynow.sendMobile(payment, cell, MobileMoneyMethod.ONEMONEY);
                case "TELECASH": return paynow.sendMobile(payment, cell, MobileMoneyMethod.TELECASH);
                // "PAYNOW" and unknown values fall through to phone-prefix detection below
            }
        }
        if (cell.matches("^07[7-8][0-9]{7}$")) {
            return paynow.sendMobile(payment, cell, MobileMoneyMethod.ECOCASH);
        }
        if (cell.matches("^071[0-9]{7}$")) {
            return paynow.sendMobile(payment, cell, MobileMoneyMethod.ONEMONEY);
        }
        if (cell.matches("^073[0-9]{7}$")) {
            return paynow.sendMobile(payment, cell, MobileMoneyMethod.TELECASH);
        }
        throw new IllegalArgumentException(
            "Unsupported mobile number: " + cell
            + ". Accepted prefixes: 077/078 (EcoCash), 071 (OneMoney), 073 (TeleCash).");
    }
}
