package com.tenten.zimparks.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Payload to void a transaction.")
public class VoidTransactionRequest {

    @Schema(description = "Reason for voiding the transaction.", example = "Duplicate payment")
    private String reason;

    @Schema(description = "Username of the user performing the void.", example = "supervisor01")
    private String voidedBy;
}
