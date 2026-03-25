package com.tenten.zimparks.transaction;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @Column(name = "tx_ref", length = 30)
    private String txRef;

    @Column(name = "original_currency", length = 5)
    private String originalCurrency;

    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "base_currency", length = 5)
    private String baseCurrency;

    @Column(name = "base_amount", precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "vat_amount", precision = 10, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "shift_id", length = 20)
    private String shiftId;
}
