package com.tenten.zimparks.auth;


import com.tenten.zimparks.util.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and token issuance endpoints.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT token.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        log.info("Login request received for username={}", req.getUsername());
        String ipAddress = RequestUtils.getClientIp(request);
        return ResponseEntity.ok(authService.login(req, ipAddress));
    }

    //Heartbeat (safety net, ≤15s):
    //Every 15 seconds, GET /auth/me is called. If the token is no longer the currentToken on the backend, it returns 401
    @GetMapping("/me")
    public ResponseEntity<Void> me() {
        return ResponseEntity.ok().build();
    }


    @PostMapping("/logout")
    @Operation(summary = "Invalidate user's current JWT token.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String ipAddress = RequestUtils.getClientIp(request);
        authService.logout(ipAddress);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate forgot password flow by sending an SMS OTP.")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        log.info("Forgot password request received for phone={}", req.getPhoneNumber());
        authService.initiateForgotPassword(req);
        return ResponseEntity.ok(Map.of("message", "OTP sent via SMS."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP sent via SMS.")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        log.info("Verify OTP request received for phone={}", req.getPhoneNumber());
        authService.verifyOtp(req);
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using an OTP sent via SMS.")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        log.info("Reset password request received for phone={}", req.getPhoneNumber());
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}
