package com.tenten.zimparks.device;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Devices", description = "Registered POS device registry and sync tracking")
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "List all registered devices with last-seen status",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<List<DeviceResponse>> list() {
        return ResponseEntity.ok(deviceService.findAll());
    }

    @Operation(summary = "Device heartbeat — register or refresh last-seen timestamp")
    @PostMapping("/{serialNumber}/ping")
    public ResponseEntity<Device> ping(
            @PathVariable String serialNumber,
            @RequestBody(required = false) HeartbeatRequest body,
            HttpServletRequest request) {

        HeartbeatRequest req = body != null ? body : new HeartbeatRequest();
        Device device = deviceService.heartbeat(serialNumber, resolveClientIp(request), req);
        return ResponseEntity.ok(device);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
