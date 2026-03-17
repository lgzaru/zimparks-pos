package com.tenten.zimparks.fiscalization;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fiscalization")
@RequiredArgsConstructor
@Tag(name = "Fiscalization", description = "Endpoints for device fiscalization setup.")
@SecurityRequirement(name = "bearerAuth")
public class FiscalizationController {

    private final FiscalizationService service;

    @PostMapping("/configure")
    @Operation(summary = "Initial device configuration for fiscalization.")
    public ResponseEntity<FiscalDevice> configure(@RequestBody FiscalDeviceConfigDTO dto) {
        return ResponseEntity.ok(service.configureDevice(dto));
    }

    @GetMapping
    @Operation(summary = "Get all fiscal devices.")
    public List<FiscalDevice> list() {
        return service.getAllDevices();
    }

    @GetMapping("/config/{deviceSerialNo}")
    @Operation(summary = "Get current fiscalization config for a device.")
    public ResponseEntity<FiscalDevice> getConfig(@PathVariable String deviceSerialNo) {
        return service.getDeviceConfig(deviceSerialNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/config/{deviceSerialNo}")
    @Operation(summary = "Update fiscalization config for a device.")
    public ResponseEntity<FiscalDevice> update(@PathVariable String deviceSerialNo, @RequestBody FiscalDeviceConfigDTO dto) {
        return ResponseEntity.ok(service.updateDevice(deviceSerialNo, dto));
    }

    @DeleteMapping("/config/{deviceSerialNo}")
    @Operation(summary = "Delete fiscalization config for a device.")
    public ResponseEntity<Void> delete(@PathVariable String deviceSerialNo) {
        service.deleteDevice(deviceSerialNo);
        return ResponseEntity.noContent().build();
    }
}
