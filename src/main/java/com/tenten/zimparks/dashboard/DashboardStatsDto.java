package com.tenten.zimparks.dashboard;

import java.math.BigDecimal;

/**
 * Response payload for GET /dashboard/stats
 */
public class DashboardStatsDto {

    private BigDecimal totalRevenue;          // this month, PAID txns only
    private double    revenueGrowthPercent;   // vs last month (negative = decrease)
    private BigDecimal atv;                   // average transaction value (all PAID txns)

    private TopItem topService;
    private TopItem topLocation;
    private TopPerformer topPerformer;

    // ── nested records ──────────────────────────────────────────────────────

    public static class TopItem {
        private String name;
        private BigDecimal revenue;

        public TopItem() {}
        public TopItem(String name, BigDecimal revenue) {
            this.name    = name;
            this.revenue = revenue;
        }
        public String     getName()    { return name; }
        public BigDecimal getRevenue() { return revenue; }
        public void setName(String n)        { this.name    = n; }
        public void setRevenue(BigDecimal r) { this.revenue = r; }
    }

    public static class TopPerformer {
        private String name;
        private String username;
        private BigDecimal revenue;

        public TopPerformer() {}
        public TopPerformer(String name, String username, BigDecimal revenue) {
            this.name     = name;
            this.username = username;
            this.revenue  = revenue;
        }
        public String     getName()     { return name; }
        public String     getUsername() { return username; }
        public BigDecimal getRevenue()  { return revenue; }
        public void setName(String n)        { this.name     = n; }
        public void setUsername(String u)    { this.username = u; }
        public void setRevenue(BigDecimal r) { this.revenue  = r; }
    }

    // ── constructors ────────────────────────────────────────────────────────

    public DashboardStatsDto() {}

    // ── getters & setters ───────────────────────────────────────────────────

    public BigDecimal getTotalRevenue()          { return totalRevenue; }
    public void setTotalRevenue(BigDecimal v)    { this.totalRevenue = v; }

    public double getRevenueGrowthPercent()          { return revenueGrowthPercent; }
    public void setRevenueGrowthPercent(double v)    { this.revenueGrowthPercent = v; }

    public BigDecimal getAtv()          { return atv; }
    public void setAtv(BigDecimal v)    { this.atv = v; }

    public TopItem getTopService()           { return topService; }
    public void setTopService(TopItem v)     { this.topService = v; }

    public TopItem getTopLocation()          { return topLocation; }
    public void setTopLocation(TopItem v)    { this.topLocation = v; }

    public TopPerformer getTopPerformer()          { return topPerformer; }
    public void setTopPerformer(TopPerformer v)    { this.topPerformer = v; }
}
