package com.tenten.zimparks.cashup;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cashup_lines")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CashupLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // cashup_id FK is managed by CashupSubmission.lines @JoinColumn — no field needed here

    @Column(length = 30)
    private String type;        // Cash, Card, Mobile Wallet, etc.

    @Column(length = 5)
    private String currency;    // USD, ZWG, ZAR

    @Column(name = "submitted_amount", precision = 12, scale = 2)
    private BigDecimal submittedAmount;
}
