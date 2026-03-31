package com.tenten.zimparks.fiscalization.phoneLinkage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mirrors the JSON shape returned/accepted by the external fiscalization API.
 * Kept separate from the JPA entity so the external contract and the DB schema
 * can evolve independently.
 *
 * NOTE: No @Builder — Jackson deserialization requires a no-args constructor,
 * which @Builder suppresses. @Data alone is sufficient here.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FiscalDeviceDTO {

    private Long deviceID;
    private String deviceSerialNo;
    private String fiscalDayStatus;
    private String certValidTill;
    private String tin;
    private boolean linked;
    private String phoneSerialNumber;
    private String linkStatus;
    private LocalDateTime linkedAt;
    private String taxpayerName;
    private String vatNumber;
}
