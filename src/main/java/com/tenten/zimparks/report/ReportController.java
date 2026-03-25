package com.tenten.zimparks.report;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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
    public Page<Map<String, Object>> get(
            @RequestParam String type,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 20) Pageable pageable) {
        allParams.remove("type");
        allParams.remove("page");
        allParams.remove("size");
        allParams.remove("sort");
        return service.build(type, allParams, pageable);
    }
}
