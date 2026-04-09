package com.tenten.zimparks.vat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vat")
@RequiredArgsConstructor
@Tag(name = "VAT", description = "VAT configuration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class VatController {

    private final VatService service;

    @GetMapping
    @Operation(summary = "Get current VAT settings.")
    public ResponseEntity<VatSettings> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    @Operation(summary = "Update VAT rates.")
    public ResponseEntity<VatSettings> set(@RequestBody VatUpdateRequest body) {
        return ResponseEntity.ok(service.set(body.getZwgRate(), body.getOtherRate(), body.getRevenueAccount(), body.getVatAccount()));
    }
}
