package com.tenten.zimparks.vat;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity @Table(name = "vat_settings")
@Data @NoArgsConstructor @AllArgsConstructor
public class VatSettings {

    @Id
    private Long id = 1L;

    @Column(name = "zwg_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal zwgRate;

    @Column(name = "other_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal otherRate;
}
