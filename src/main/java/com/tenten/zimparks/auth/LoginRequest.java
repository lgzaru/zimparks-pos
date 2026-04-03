package com.tenten.zimparks.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Authentication request payload.")
public class LoginRequest {
    @NotBlank
    @Schema(description = "Username for login.", example = "admin")
    String username;

    @NotBlank
    @Schema(description = "Password for login.", example = "P@ssw0rd")
    String password;

    @Schema(description = "Phone/device serial number. Required when the authenticating user is an OPERATOR.", example = "3444-00001")
    String deviceSerial;
}
