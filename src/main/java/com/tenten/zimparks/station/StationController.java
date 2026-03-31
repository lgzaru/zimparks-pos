package com.tenten.zimparks.station;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@Tag(name = "Stations", description = "Station configuration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class StationController {

    private final StationService service;

    @GetMapping
    @Operation(summary = "List all stations.", security = {})
    public List<Station> list() {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Create a station.")
    public ResponseEntity<Station> create(@RequestBody Station s) {
        return ResponseEntity.ok(service.create(s));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing station.")
    public ResponseEntity<Station> update(@PathVariable String id, @RequestBody Station s) {
        return ResponseEntity.ok(service.update(id, s));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update an existing station.")
    public ResponseEntity<Station> patch(@PathVariable String id, @RequestBody Station s) {
        return ResponseEntity.ok(service.update(id, s));
    }

    @PostMapping("/{stationId}/banks/{bankCode}")
    @Operation(summary = "Add a bank to a station.")
    public ResponseEntity<Station> addBank(@PathVariable String stationId, @PathVariable String bankCode) {
        return ResponseEntity.ok(service.addBank(stationId, bankCode));
    }

    @DeleteMapping("/{id}/banks/{bankCode}")
    @Operation(summary = "Remove a bank from a station.")
    public ResponseEntity<Station> removeBank(@PathVariable String id, @PathVariable String bankCode) {
        return ResponseEntity.ok(service.removeBank(id, bankCode));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a station.")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
