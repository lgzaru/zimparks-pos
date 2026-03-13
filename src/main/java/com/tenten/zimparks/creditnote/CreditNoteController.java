package com.tenten.zimparks.creditnote;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-notes")
@RequiredArgsConstructor
@Tag(name = "Credit Notes", description = "Credit note workflow endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class CreditNoteController {

    private final CreditNoteService service;

    @GetMapping
    @Operation(summary = "List credit notes, optionally filtered by status.")
    public List<CreditNote> list(@RequestParam(required = false) String status) {
        return status != null ? service.findByStatus(status) : service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a credit note by ID.")
    public ResponseEntity<CreditNote> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a credit note.")
    public ResponseEntity<CreditNote> create(@RequestBody CreditNote cn) {
        return ResponseEntity.ok(service.create(cn));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a credit note.")
    public ResponseEntity<CreditNote> approve(@PathVariable String id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject a credit note.")
    public ResponseEntity<CreditNote> reject(@PathVariable String id) {
        return ResponseEntity.ok(service.reject(id));
    }
}
