package com.tenten.zimparks.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for POST /api/online-payments/initiate.
 * Contains all fields needed to create a POS transaction plus the customer's phone number.
 */
@Data
public class OnlinePaymentRequest {

    /** Customer phone number for USSD push (07XXXXXXXX or +263XXXXXXXXX). */
    private String cell;

    // ── Transaction fields (mirrors POST /transactions payload) ───────────────

    private String productCode;
    private Integer items;
    /**
     * Amount pushed to the customer via USSD — the remaining balance in USD
     * (may be less than txTotalAmount when a split payment is in progress).
     */
    private BigDecimal amount;
    /**
     * Full cart total in USD — always the Transaction.amount stored in the DB.
     * Defaults to amount if not provided (pure online payment, no prior cash/card split).
     */
    private BigDecimal txTotalAmount;
    private String currency;        // Payment currency (USD, ZWG, ZAR)
    private String customerName;
    private String customerId;
    private String operatorName;
    private String operatorUsername;
    private String stationId;
    private String shiftId;
    private String virtualDeviceId;
    private String bankCode;
    private String quotationRef;
    /** Explicitly selected mobile money method: EcoCash, OneMoney, TeleCash, Paynow */
    private String paymentMethod;

    private List<BreakdownItem> breakdown;
    private List<ItemEntry> itemsList;
    private VehicleInfo vehicle;

    @Data
    public static class BreakdownItem {
        private String type;
        private String currency;
        private BigDecimal amount;
    }

    @Data
    public static class ItemEntry {
        private String productCode;
        private String descr;
        private String hsCode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Data
    public static class VehicleInfo {
        private String plate;
        private String make;
        private String model;
        private String colour;
    }
}
