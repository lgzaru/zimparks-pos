package com.tenten.zimparks.fiscalization.phoneLinkage;

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

    // ── External-system views ────────────────────────────────────────────────

    @GetMapping("/external/available-devices")
    @Operation(summary = "List unlinked devices from the external fiscal system.")
    public List<FiscalDeviceDTO> getUnlinkedDevices() {
        return service.getUnlinkedDevices();
    }

    @GetMapping("/external/linked-devices")
    @Operation(summary = "List linked devices from the external fiscal system (verification).")
    public List<FiscalDeviceDTO> getLinkedDevices() {
        return service.getLinkedDevices();
    }

    @GetMapping("/external/device/{serialNo}")
    @Operation(summary = "Get device detail by serial number from the external fiscal system.")
    public ResponseEntity<FiscalDeviceDTO> getExternalDeviceDetail(@PathVariable String serialNo) {
        return service.getExternalDeviceDetail(serialNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Link / Unlink ────────────────────────────────────────────────────────

    @PostMapping("/link-device")
    @Operation(summary = "Link a fiscal device via the external system and persist locally with station.")
    public ResponseEntity<FiscalDevice> linkDevice(
            @RequestBody FiscalLinkRequestDTO dto,
            @RequestParam(required = false) String stationId) {
        return ResponseEntity.ok(service.linkDevice(dto, stationId));
    }

    @PostMapping("/{phoneSerial}/unlink")
    @Operation(summary = "Unlink a fiscal device via the external system and mark it unlinked locally.")
    public ResponseEntity<FiscalDevice> unlinkDevice(@PathVariable String phoneSerial) {
        FiscalDevice device = service.unlinkDevice(phoneSerial);
        if (device == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(device);
    }

    // ── Local DB views ───────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all locally persisted fiscal devices.")
    public List<FiscalDevice> listLocalDevices() {
        return service.getAllLocalDevices();
    }

    @GetMapping("/config/{deviceSerialNo}")
    @Operation(summary = "Get local config for a specific fiscal device.")
    public ResponseEntity<FiscalDevice> getLocalConfig(@PathVariable String deviceSerialNo) {
        return service.getLocalDeviceConfig(deviceSerialNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/config/{deviceSerialNo}")
    @Operation(summary = "Remove local fiscal device record.")
    public ResponseEntity<Void> deleteLocalConfig(@PathVariable String deviceSerialNo) {
        service.deleteDevice(deviceSerialNo);
        return ResponseEntity.noContent().build();
    }
}