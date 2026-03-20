package com.tenten.zimparks.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class SupervisorDashboardDTO {
    private String stationId;
    private String stationName;
    private long totalOpenShifts;
    private BigDecimal totalRevenueFromOpenShifts;
    private Map<String, BigDecimal> revenueByCurrency;
    private long pendingCreditNotes;
    private long pendingVoids;
}
