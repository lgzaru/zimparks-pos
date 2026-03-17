package com.tenten.zimparks.fiscalization;

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
    @Column(name = "device_serial_no", length = 100)
    private String deviceSerialNo;

    @Column(name = "v_device_id", nullable = false, unique = true, length = 100)
    private String virtualDeviceId;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "tax_payer_name", length = 200)
    private String taxPayerName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
