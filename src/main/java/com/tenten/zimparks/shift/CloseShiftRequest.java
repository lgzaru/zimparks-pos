package com.tenten.zimparks.shift;

import com.tenten.zimparks.cashup.CashupLineDto;
import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CloseShiftRequest {
    /**
     * Declared cashup lines submitted by the operator when closing their own shift.
     * Required when the authenticated user is the operator closing their own shift.
     * Ignored (may be null) when a supervisor or admin closes the shift.
     */
    private List<CashupLineDto> cashupLines;
}
