package com.tenten.zimparks.shift;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Tag(name = "Shifts", description = "Shift lifecycle endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class ShiftController {

    private final ShiftService service;

    @GetMapping("/{username}")
    @Operation(summary = "Get latest shift for a given username.")
    public ResponseEntity<?> get(@PathVariable String username) {
        return service.getLatest(username)
                .map(ResponseEntity::ok)
                .<ResponseEntity<?>>map(r -> r)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active shifts.")
    public ResponseEntity<List<Map<String, Object>>> getActive() {
        return ResponseEntity.ok(service.getActiveShifts());
    }

    @PostMapping("/open")
    @Operation(summary = "Open a new shift for a user.")
    public ResponseEntity<Shift> open(@RequestBody OpenShiftRequest body) {
        return ResponseEntity.ok(service.open(body.getUsername()));
    }

    @PostMapping("/close/{username}")
    @Operation(summary = "Close the currently open shift for a user. " +
               "When called by the operator themselves, cashupLines is required. " +
               "When called by a supervisor or admin, cashupLines is ignored.")
    public ResponseEntity<Map<String, Object>> close(
            @PathVariable String username,
            @RequestBody(required = false) CloseShiftRequest body) {
        return ResponseEntity.ok(service.close(username, body));
    }

    @GetMapping("/summary/{username}")
    @Operation(summary = "Get the summary of the latest shift for a user.")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable String username) {
        return ResponseEntity.ok(service.getSummary(username));
    }

    @GetMapping("/{shiftId}/transactions")
    @Operation(summary = "Get all transactions for a specific shift.")
    public ResponseEntity<?> getTransactions(@PathVariable String shiftId) {
        return ResponseEntity.ok(service.getTransactions(shiftId));
    }

    @GetMapping("/station/{stationId}")
    @Operation(summary = "Get all shifts for a specific station.")
    public ResponseEntity<List<Shift>> getByStation(@PathVariable String stationId) {
        return ResponseEntity.ok(service.getAllByStationId(stationId));
    }
}
