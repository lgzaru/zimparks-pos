package com.tenten.zimparks.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String otp;
}
