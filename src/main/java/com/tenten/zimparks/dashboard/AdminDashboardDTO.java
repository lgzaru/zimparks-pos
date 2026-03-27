package com.tenten.zimparks.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminDashboardDTO {

    // ── Revenue ───────────────────────────────────────────────────────────
    private BigDecimal totalRevenue;          // PAID txns, current month
    private BigDecimal previousMonthRevenue;  // PAID txns, previous month (exact)
    private double     revenueGrowthPercent;  // vs previous month (negative = decrease)
    private BigDecimal atv;                   // average transaction value, all PAID txns
    private int        transactionCount;      // number of PAID txns in current month

    // ── Top cards ─────────────────────────────────────────────────────────
    private TopItem       topService;         // single top service (backwards-compat)
    private List<TopItem> topServices;        // top 3 services by revenue for donut
    private TopItem       topLocation;
    private TopPerformer  topPerformer;

    @Data
    @Builder
    public static class TopItem {
        private String     name;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class TopPerformer {
        private String     name;
        private String     username;
        private BigDecimal revenue;
    }
}
