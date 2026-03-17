package com.tenten.zimparks.product;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "products")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @EmbeddedId
    private ProductId id;

    @Column(nullable = false, length = 200)
    private String descr;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "category_code")
    private ProductCategory category;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "entry_product", nullable = false)
    @Builder.Default
    private Boolean entryProduct = false;
}
