package com.tenten.zimparks.voucher;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wifi_vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

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
}
