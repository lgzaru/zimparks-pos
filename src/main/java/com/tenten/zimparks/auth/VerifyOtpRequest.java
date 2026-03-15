package com.tenten.zimparks.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String otp;
}
