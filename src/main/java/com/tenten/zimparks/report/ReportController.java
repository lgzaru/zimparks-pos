package com.tenten.zimparks.report;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reporting endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService service;

    /**
     * GET /api/reports?type=Shift+Revenue+Summary&dateFrom=...&dateTo=...
     *                 &station=All&user=All&status=All
     */
    @GetMapping
    @Operation(summary = "Build a report by type and query parameters.")
    public List<Map<String, Object>> get(
            @RequestParam String type,
            @RequestParam Map<String, String> allParams) {
        allParams.remove("type");
        return service.build(type, allParams);
    }
}
