package com.tenten.zimparks.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response for online payment endpoints.
 * Frontend polls on paynowRef until status is PAID, then uses txRef to fetch the receipt.
 */
@Data
@Builder
public class OnlinePaymentResponse {
    private String paynowRef;
    /** PENDING | PAID | FAILED | CANCELLED */
    private String status;
    /** Populated once Paynow confirms payment and the POS transaction is created. */
    private String txRef;
    private String description;
    private String cell;
    private BigDecimal amount;
    private String currency;
    private String errorMessage;
}
