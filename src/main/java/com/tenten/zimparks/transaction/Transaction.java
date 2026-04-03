package com.tenten.zimparks.transaction;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tenten.zimparks.station.Station;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity @Table(name = "transactions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id @Column(length = 30)
    private String ref;

    @Column(name = "product_code", length = 20)
    private String productCode;

    @Column
    private Integer items;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;            // PAID | VOID_PENDING | VOIDED | VOID_REJECTED

    @Column(name = "tx_time", length = 10)
    private String txTime;

    @Column(name = "tx_date")
    private String txDate;

    @Column(length = 5)
    private String currency;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_id", length = 20)
    private String customerId;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "operator_username", length = 100)
    private String operatorUsername;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id")
    @JsonIgnoreProperties({"banks","region"})
    private Station station;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

    @Column(name = "voided_by", length = 100)
    private String voidedBy;

    @Column(name = "void_requested_by", length = 100)
    private String voidRequestedBy;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "vat_amount", precision = 10, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "shift_id", length = 20)
    private String shiftId;

    @Column(name = "v_device_id", length = 100)
    private String virtualDeviceId;

    @Column(name = "has_entry")
    @Builder.Default
    private Boolean hasEntry = false;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "tx_ref")
    private List<TransactionItem> itemsList;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "tx_ref")
    private List<PaymentBreakdown> breakdown;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "ref", referencedColumnName = "tx_ref",
            foreignKey = @ForeignKey(name = "fk_tx_vehicle"))
    private TransactionVehicle vehicle;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "ref", referencedColumnName = "tx_ref",
            foreignKey = @ForeignKey(name = "fk_tx_receipt"))
    private Receipt receipt;
}
