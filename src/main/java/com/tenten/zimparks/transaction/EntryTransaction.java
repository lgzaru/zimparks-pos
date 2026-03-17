package com.tenten.zimparks.transaction;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "entry_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_ref", length = 30, nullable = false)
    private String txRef;

    @Column(name = "product_code", length = 20, nullable = false)
    private String productCode;

    @Column(name = "product_descr", length = 200, nullable = false)
    private String productDescr;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Additional relevant fields can be added here
}
