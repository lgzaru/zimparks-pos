package com.tenten.zimparks.shift;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Payload to open a new shift.")
public class OpenShiftRequest {

    @NotBlank
    @Schema(description = "Username of the operator opening the shift.", example = "cashier01")
    private String username;
}
