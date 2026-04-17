package com.tenten.zimparks.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks a mobile money payment request (EcoCash / OneMoney / TeleCash via Paynow).
 * Created when the frontend initiates an online payment.
 * txRef is populated once Paynow confirms payment and the POS transaction is created.
 */
@Entity
@Table(name = "online_payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OnlinePayment {

    @Id
    @Column(name = "paynow_ref", length = 50)
    private String paynowRef;

    @Column(name = "cell", length = 20)
    private String cell;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 5)
    private String currency;

    /** PENDING | PAID | FAILED | CANCELLED */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "poll_url", columnDefinition = "TEXT")
    private String pollUrl;

    @Column(name = "payment_ref", length = 100)
    private String paymentRef;

    @Column(name = "description", length = 255)
    private String description;

    /** Set once the POS Transaction is created after payment confirmation. */
    @Column(name = "tx_ref", length = 30)
    private String txRef;

    /** JSON snapshot of the full transaction payload — used to create the Transaction when confirmed. */
    @Column(name = "tx_payload", columnDefinition = "TEXT")
    private String txPayload;

    /** ECOCASH | ONEMONEY | TELECASH | PAYNOW — selected by the operator at initiation */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "operator_username", length = 100)
    private String operatorUsername;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
