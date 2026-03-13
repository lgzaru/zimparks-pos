package com.tenten.zimparks.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
@Schema(description = "Authentication response payload.")
public class LoginResponse {
    @Schema(description = "JWT bearer token.")
    private String token;

    @Schema(description = "Authenticated username.")
    private String username;

    @Schema(description = "Display name of the authenticated user.")
    private String fullName;

    @Schema(description = "Role assigned to the authenticated user.")
    private String role;

    @Schema(description = "ID of the user's assigned station.")
    private String stationId;

    @Schema(description = "List of banks linked to the user's station.")
    private java.util.List<com.tenten.zimparks.bank.Bank> stationBanks;
}
