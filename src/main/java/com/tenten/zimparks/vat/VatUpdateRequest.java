package com.tenten.zimparks.vat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Payload to update VAT settings.")
public class VatUpdateRequest {

    @NotNull
    @Schema(description = "VAT rate for ZWG currency.", example = "15.0")
    private BigDecimal zwgRate;

    @NotNull
    @Schema(description = "VAT rate for other currencies.", example = "15.5")
    private BigDecimal otherRate;

    @Schema(description = "GL account code for revenue recognition.", example = "60001000")
    private String revenueAccount;

    @Schema(description = "GL account code for VAT payable.", example = "42029000")
    private String vatAccount;
}
