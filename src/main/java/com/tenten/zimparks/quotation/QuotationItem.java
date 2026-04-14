package com.tenten.zimparks.quotation;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quotation_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quotation_ref", length = 50, nullable = false)
    private String quotationRef;

    /**
     * Product code from the catalog. Nullable — free-form items created by
     * external systems or HQ may not map to a POS product code. The POS will
     * treat null / "MISC" product codes as custom items (no catalog lookup).
     */
    @Column(name = "product_code", length = 20)
    private String productCode;

    @Column(name = "descr", length = 200, nullable = false)
    private String descr;

    /** HS code for fiscalization. Falls back to the POS default misc HS code if blank. */
    @Column(name = "hs_code", length = 8)
    private String hsCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
