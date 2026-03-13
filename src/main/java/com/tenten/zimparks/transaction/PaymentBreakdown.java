package com.tenten.zimparks.transaction;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity @Table(name = "transaction_payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentBreakdown {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_ref", length = 30)
    private String txRef;

    @Column(name = "original_currency", length = 5)
    private String originalCurrency;

    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(length = 30)
    private String type;

    @Column(length = 5)
    private String currency;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;
}
