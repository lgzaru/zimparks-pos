package com.tenten.zimparks.bank;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
@Tag(name = "Banks", description = "Bank configuration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class BankController {

    private final BankService service;

    @GetMapping
    @Operation(summary = "List all banks.")
    public List<Bank> list() { return service.findAll(); }

    @PostMapping
    @Operation(summary = "Create a bank.")
    public ResponseEntity<Bank> create(@RequestBody Bank b) {
        return ResponseEntity.ok(service.create(b));
    }
}
