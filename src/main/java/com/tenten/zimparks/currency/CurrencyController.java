package com.tenten.zimparks.currency;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
@Tag(name = "Currencies", description = "Currency and exchange rate configuration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class CurrencyController {

    private final CurrencyService service;

    @GetMapping
    @Operation(summary = "List all currencies.")
    public List<Currency> list() {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Add or update a currency.")
    public ResponseEntity<Currency> save(@RequestBody Currency currency) {
        return ResponseEntity.ok(service.save(currency));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a currency.")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.ok().build();
    }
}
