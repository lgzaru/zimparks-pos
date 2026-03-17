package com.tenten.zimparks.transaction;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService service;

    @GetMapping
    @Operation(summary = "List transactions, optionally filtered by status or customer ID.")
    public List<Transaction> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String shiftId) {

        if (shiftId != null)    return service.findByShiftId(shiftId);
        if (status != null)     return service.findByStatus(status.toUpperCase());
        if (customerId != null) return service.findByCustomer(customerId);
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Create a new transaction.")
    public ResponseEntity<Transaction> create(@RequestBody Transaction tx) {
        return ResponseEntity.ok(service.create(tx));
    }

    @GetMapping("/{ref}")
    @Operation(summary = "Get transaction by reference.")
    public ResponseEntity<Transaction> getByRef(@PathVariable String ref) {
        return ResponseEntity.ok(service.findByRef(ref));
    }

    @GetMapping("/{ref}/receipt")
    @Operation(summary = "Get receipt by transaction reference.")
    public ResponseEntity<Receipt> getReceiptByRef(@PathVariable String ref) {
        return ResponseEntity.ok(service.findReceiptByRef(ref));
    }

    @PatchMapping("/{ref}/void")
    @Operation(summary = "Request or perform void on a transaction.")
    public ResponseEntity<Transaction> voidTx(@PathVariable String ref,
                                              @RequestBody VoidTransactionRequest body) {
        return ResponseEntity.ok(service.voidTx(ref, body));
    }

    @PatchMapping("/{ref}/void/approve")
    @Operation(summary = "Approve a pending void.")
    public ResponseEntity<Transaction> approveVoid(@PathVariable String ref) {
        return ResponseEntity.ok(service.approveVoid(ref));
    }

    @PatchMapping("/{ref}/void/reject")
    @Operation(summary = "Reject a pending void.")
    public ResponseEntity<Transaction> rejectVoid(@PathVariable String ref,
                                                  @RequestBody(required = false) VoidTransactionRequest body) {
        String reason = (body != null) ? body.getReason() : null;
        return ResponseEntity.ok(service.rejectVoid(ref, reason));
    }

    @PutMapping("/{ref}")
    @Operation(summary = "Update an existing transaction.")
    public ResponseEntity<Transaction> update(@PathVariable String ref, @RequestBody Transaction tx) {
        return ResponseEntity.ok(service.update(ref, tx));
    }

    @DeleteMapping("/{ref}")
    @Operation(summary = "Delete a transaction.")
    public ResponseEntity<Void> delete(@PathVariable String ref) {
        service.delete(ref);
        return ResponseEntity.noContent().build();
    }
}
