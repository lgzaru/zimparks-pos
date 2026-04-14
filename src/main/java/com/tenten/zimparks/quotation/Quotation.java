package com.tenten.zimparks.quotation;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation {

    /**
     * Quotation reference — set by the originating system (HQ, back-office, etc.).
     * Must be unique and is used as the primary key.
     */
    @Id
    @Column(length = 50, nullable = false)
    private String ref;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_id", length = 50)
    private String customerId;

    /** Pre-calculated total in USD (sum of all item totalPrices). */
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 5)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private QuotationStatus status;

    /** Human-readable creation date from the originating system (e.g. "15 Apr 2026"). */
    @Column(name = "quotation_date", length = 30)
    private String quotationDate;

    /** Date after which this quotation must not be converted. Null means no expiry. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 1000)
    private String notes;

    /** Optional — restricts which station can convert this quotation. Null means any station. */
    @Column(name = "station_id", length = 20)
    private String stationId;

    /** Set when status becomes CONVERTED. Links back to the resulting transaction. */
    @Column(name = "converted_txn_ref", length = 50)
    private String convertedTxnRef;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "quotation_ref")
    @Builder.Default
    private List<QuotationItem> items = new ArrayList<>();
}
