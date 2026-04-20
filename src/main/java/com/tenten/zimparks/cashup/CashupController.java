package com.tenten.zimparks.cashup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cashup")
@RequiredArgsConstructor
@Tag(name = "Cashup", description = "Cashup submission and review endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class CashupController {

    private final CashupService cashupService;

    /** Admin: paginated cashup history with optional filters */
    @GetMapping("/history")
    @Operation(summary = "Admin — paginated cashup history with optional status, search, and date filters.")
    public ResponseEntity<Page<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String dateFrom,
            @RequestParam(required = false)    String dateTo) {
        return ResponseEntity.ok(cashupService.getHistoryForAdmin(
                status, search, parseDate(dateFrom, false), parseDate(dateTo, true), page, size));
    }

    /** Admin: export all matching cashup records (with lines) for Excel/PDF generation */
    @GetMapping("/history/export")
    @Operation(summary = "Admin — export all cashup records matching the given filters.")
    public ResponseEntity<List<Map<String, Object>>> exportHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(cashupService.getExportForAdmin(
                status, search, parseDate(dateFrom, false), parseDate(dateTo, true)));
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Parses "yyyy-MM-dd" to LocalDateTime; endOfDay=true sets time to 23:59:59 for upper bounds. */
    private static LocalDateTime parseDate(String date, boolean endOfDay) {
        if (date == null || date.isBlank()) return null;
        try {
            var d = java.time.LocalDate.parse(date, DATE_FMT);
            return endOfDay ? d.atTime(23, 59, 59) : d.atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    /** Supervisor: list all PENDING_REVIEW cashups for operators in their station */
    @GetMapping("/pending")
    @Operation(summary = "Get pending cashup submissions for the current supervisor's station.")
    public ResponseEntity<List<Map<String, Object>>> getPending() {
        return ResponseEntity.ok(cashupService.getPendingForSupervisor(getCurrentUsername()));
    }

    /** Get a cashup submission with submitted vs actual comparison */
    @GetMapping("/{id}")
    @Operation(summary = "Get cashup submission with actual vs submitted comparison.")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cashupService.getCashupWithActuals(id));
    }

    /** Get the cashup for a specific shift (if one exists) */
    @GetMapping("/shift/{shiftId}")
    @Operation(summary = "Get cashup submission for a specific shift.")
    public ResponseEntity<?> getByShift(@PathVariable String shiftId) {
        return cashupService.getByShiftId(shiftId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Get actual payment-type totals for a shift (for non-blind operators or supervisors).
     * Returns Map of "type|currency" → actualAmount.
     */
    @GetMapping("/actuals/{shiftId}")
    @Operation(summary = "Get system-calculated actual totals by payment type and currency for a shift.")
    public ResponseEntity<Map<String, BigDecimal>> getActuals(@PathVariable String shiftId) {
        return ResponseEntity.ok(cashupService.getActuals(shiftId));
    }

    /** Supervisor: approve or waive a cashup. Corrected declared lines are optional. */
    @PostMapping("/{id}/review")
    @Operation(summary = "Review (approve / waive) a cashup submission.")
    public ResponseEntity<CashupSubmission> review(@PathVariable Long id,
                                                   @RequestBody ReviewRequest body) {
        return ResponseEntity.ok(cashupService.review(
                id, body.getDecision(), body.getNotes(), body.getCorrectedLines()));
    }

    // ── Inner DTOs ──────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ReviewRequest {
        private String decision; // APPROVED | WAIVED
        private String notes;    // optional for APPROVED, required for WAIVED
        /** Supervisor-corrected declared amounts (replaces operator submission on save). */
        private List<CashupLineDto> correctedLines;
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) return ud.getUsername();
        return principal.toString();
    }
}
