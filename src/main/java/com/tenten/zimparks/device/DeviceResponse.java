package com.tenten.zimparks.device;

import java.time.LocalDateTime;

public record DeviceResponse(
        String serialNumber,
        String deviceName,
        String stationId,
        String stationName,
        String appVersion,
        String ipAddress,
        String localIp,
        String platform,
        Integer batteryLevel,
        Boolean isCharging,
        String loggedInUser,
        String loggedInUserRole,
        String shiftId,
        LocalDateTime lastSeen,
        LocalDateTime registeredAt
) {}
