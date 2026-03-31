package com.tenten.zimparks.fiscalization.phoneLinkage;

import com.tenten.zimparks.station.Station;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fiscal_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalDevice {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "device_serial_no", length = 100, unique = true)
    private String deviceSerialNo;

    @Column(name = "fiscal_day_status")
    private String fiscalDayStatus;

    @Column(name = "cert_valid_till")
    private String certValidTill;

    @Column(name = "tin")
    private String tin;

    @Column(name = "linked")
    private boolean linked;

    @Column(name = "phone_serial_number")
    private String phoneSerialNumber;

    @Column(name = "link_status")
    private String linkStatus;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "taxpayer_name", length = 200)
    private String taxpayerName;

    @Column(name = "vat_number")
    private String vatNumber;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = true)
    private Station station;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
