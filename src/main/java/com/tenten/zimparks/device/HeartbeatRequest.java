package com.tenten.zimparks.device;

import lombok.Data;

@Data
public class HeartbeatRequest {
    private String  deviceName;
    private String  stationId;
    private String  appVersion;
    private String  platform;
    private String  loggedInUser;
    private String  shiftId;
    private Integer batteryLevel;
    private Boolean isCharging;
    private String  localIp;
}
