package com.tenten.zimparks.fiscalization.phoneLinkage;

import lombok.Data;

@Data
public class FiscalLinkRequestDTO {
    private Long deviceID;
    private String phoneSerialNumber;
}
