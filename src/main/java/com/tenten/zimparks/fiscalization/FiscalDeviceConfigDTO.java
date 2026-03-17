package com.tenten.zimparks.fiscalization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalDeviceConfigDTO {
    private String deviceSerialNo;
    private String virtualDeviceId;
    private String stationId;
    private String taxPayerName;
}
