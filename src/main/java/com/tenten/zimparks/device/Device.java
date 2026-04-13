package com.tenten.zimparks.device;

import com.tenten.zimparks.station.Station;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @Column(length = 100)
    private String serialNumber;

    @Column(length = 150)
    private String deviceName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id")
    private Station station;

    @Column(length = 50)
    private String appVersion;

    @Column(length = 50)
    private String ipAddress;

    /** Device-reported local/LAN IP (collected via WebRTC on the client). */
    @Column(length = 50)
    private String localIp;

    /** android | ios | web */
    @Column(length = 20)
    private String platform;

    /** 0–100, null if unavailable */
    @Column
    private Integer batteryLevel;

    @Column
    private Boolean isCharging;

    /** Username of whoever is currently logged in on this device. */
    @Column(length = 100)
    private String loggedInUser;

    /** ID of the currently open shift on this device, if any. */
    @Column(length = 50)
    private String shiftId;

    @Column
    private LocalDateTime lastSeen;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    void onInsert() {
        if (registeredAt == null) registeredAt = LocalDateTime.now();
    }
}
