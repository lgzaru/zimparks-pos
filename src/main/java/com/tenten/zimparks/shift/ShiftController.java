package com.tenten.zimparks.shift;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/open")
    @Operation(summary = "Open a new shift for a user.")
    public ResponseEntity<Shift> open(@RequestBody OpenShiftRequest body) {
        return ResponseEntity.ok(service.open(body.getUsername()));
    }

    @PostMapping("/close/{username}")
    @Operation(summary = "Close the currently open shift for a user.")
    public ResponseEntity<Map<String, Object>> close(@PathVariable String username) {
        return ResponseEntity.ok(service.close(username));
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
}
