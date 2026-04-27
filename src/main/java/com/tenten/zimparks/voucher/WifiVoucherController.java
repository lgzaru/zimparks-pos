package com.tenten.zimparks.voucher;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wifi-vouchers")
@RequiredArgsConstructor
@Tag(name = "WiFi Vouchers", description = "WiFi voucher management endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class WifiVoucherController {

    private final WifiVoucherService service;

    @GetMapping("/tx/{txRef}")
    @Operation(summary = "Get all WiFi vouchers generated for a transaction.")
    public ResponseEntity<List<WifiVoucher>> getByTxRef(@PathVariable String txRef) {
        return ResponseEntity.ok(service.findByTxRef(txRef));
    }
}
