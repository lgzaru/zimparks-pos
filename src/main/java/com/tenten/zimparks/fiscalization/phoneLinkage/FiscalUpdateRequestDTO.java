package com.tenten.zimparks.fiscalization.phoneLinkage;

import lombok.Data;

@Data
public class FiscalUpdateRequestDTO {
    private String virtualDeviceId;
    private String stationId;
    private String taxPayerName;
}
