package com.tenten.zimparks.cashup;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class CashupLineDto {
    private String type;
    private String currency;
    private BigDecimal submittedAmount;
}
