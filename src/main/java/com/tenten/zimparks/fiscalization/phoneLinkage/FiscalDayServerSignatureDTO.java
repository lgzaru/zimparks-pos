package com.tenten.zimparks.fiscalization.phoneLinkage;

import lombok.Data;

@Data
public class FiscalDayServerSignatureDTO {
    private String certificateThumbprint;
    private String hash;
    private String signature;
}
