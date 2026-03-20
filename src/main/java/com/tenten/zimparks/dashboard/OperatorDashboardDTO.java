package com.tenten.zimparks.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class OperatorDashboardDTO {
    private String shiftId;
    private String status;
    private String startTime;
    private BigDecimal totalRevenue;
    private long totalVoids;
    private long totalCreditNotes;
    private Map<String, BigDecimal> revenueByCurrency;
}
