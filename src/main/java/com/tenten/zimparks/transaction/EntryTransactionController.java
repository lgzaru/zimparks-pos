package com.tenten.zimparks.transaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entry-transactions")
@RequiredArgsConstructor
@Tag(name = "Entry Transactions", description = "Endpoints for managing entry-specific transactions.")
@SecurityRequirement(name = "bearerAuth")
public class EntryTransactionController {

    private final TransactionService service;

    @GetMapping
    @Operation(summary = "List all entry transactions.")
    public List<EntryTransaction> listAll() {
        return service.findAllEntryTransactions();
    }

    @GetMapping("/tx/{txRef}")
    @Operation(summary = "List all entry tickets for a given transaction reference.")
    public List<EntryTransaction> findByTxRef(@PathVariable String txRef) {
        return service.findEntriesByTxRef(txRef);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single entry transaction by ID.")
    public ResponseEntity<EntryTransaction> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findEntryById(id));
    }
}
