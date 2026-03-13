package com.tenten.zimparks.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductId implements Serializable {

    @Column(length = 20)
    private String code;

    @Column(name = "station_id", length = 10)
    private String stationId;
}
