package com.tenten.zimparks.activity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activity Logs", description = "Endpoints for retrieving user activity logs.")
@SecurityRequirement(name = "bearerAuth")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    @Operation(summary = "Get activity logs. Admin sees all logs, other roles see only their own.")
    public ResponseEntity<Page<ActivityLog>> getActivities(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 100) Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getLogsForCurrentUser(search, pageable));
    }
}
