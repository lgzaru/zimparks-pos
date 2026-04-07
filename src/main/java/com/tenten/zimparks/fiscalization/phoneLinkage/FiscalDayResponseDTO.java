package com.tenten.zimparks.fiscalization.phoneLinkage;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FiscalDayResponseDTO {
    private String fiscalDayStatus;
    private String fiscalDayReconciliationMode;
    private FiscalDayServerSignatureDTO fiscalDayServerSignature;
    private LocalDateTime fiscalDayClosed;
    private Integer lastFiscalDayNo;
    private String operationID;
}
