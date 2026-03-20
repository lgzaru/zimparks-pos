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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard endpoints for Operator and Supervisor.")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/operator")
    @Operation(summary = "Get dashboard data for the currently logged-in operator.")
    public ResponseEntity<OperatorDashboardDTO> getOperatorDashboard() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(service.getOperatorDashboard(username));
    }

    @GetMapping("/supervisor")
    @Operation(summary = "Get dashboard data for the currently logged-in supervisor.")
    public ResponseEntity<SupervisorDashboardDTO> getSupervisorDashboard() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(service.getSupervisorDashboard(username));
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }
}
