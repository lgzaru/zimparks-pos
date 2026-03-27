package com.tenten.zimparks.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard endpoints for Operator, Supervisor and Admin.")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/operator")
    @Operation(summary = "Get dashboard data for the currently logged-in operator.")
    public ResponseEntity<OperatorDashboardDTO> getOperatorDashboard() {
        return ResponseEntity.ok(service.getOperatorDashboard(getCurrentUsername()));
    }

    @GetMapping("/supervisor")
    @Operation(summary = "Get dashboard data for the currently logged-in supervisor.")
    public ResponseEntity<SupervisorDashboardDTO> getSupervisorDashboard() {
        return ResponseEntity.ok(service.getSupervisorDashboard(getCurrentUsername()));
    }

    /**
     * GET /api/dashboard/admin
     *
     * Optional params:
     *   year  – defaults to current year
     *   month – defaults to current month (1–12)
     *
     * Response:
     * {
     *   "totalRevenue": 54332.00,
     *   "revenueGrowthPercent": -5.0,
     *   "atv": 234.50,
     *   "topService":   { "name": "Fishing",     "revenue": 54332.00 },
     *   "topLocation":  { "name": "Lake Kariba", "revenue": 34000.00 },
     *   "topPerformer": { "name": "John Doe", "username": "john", "revenue": 54000.00 }
     * }
     */
    @GetMapping("/admin")
    @Operation(summary = "Get KPI stats for the admin dashboard (revenue, growth, ATV, top service/location/performer).")
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        LocalDate now = LocalDate.now();
        int y = year  != null ? year  : now.getYear();
        int m = month != null ? month : now.getMonthValue();

        return ResponseEntity.ok(service.getAdminDashboard(y, m));
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof UserDetails
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
    }
}
