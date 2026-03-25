package com.tenten.zimparks.auth;

import com.tenten.zimparks.activity.ActivityLogService;
import com.tenten.zimparks.config.JwtConfig;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserDetailsService    userDetailsService;
    private final JwtConfig jwtConfig;
    private final UserRepository        userRepo;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public void initiateForgotPassword(ForgotPasswordRequest req) {
        String phoneNumber = req.getPhoneNumber();
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.replaceAll("[\\+\\s]", "");
        }
        User user = userRepo.findByCellPhone(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("the provided number is not linked to any user"));

        String otp = String.format("%04d", new Random().nextInt(10000));
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(15));
        userRepo.save(user);

        String message = "Your ZimParks POS password reset code is: " + otp;
        smsService.sendSms(user.getCellPhone(), message);
        log.info("Sent forgot password OTP to phone={}", phoneNumber);
    }

    public void verifyOtp(VerifyOtpRequest req) {
        String phoneNumber = req.getPhoneNumber();
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.replaceAll("[\\+\\s]", "");
        }
        User user = userRepo.findByCellPhone(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("the provided number is not linked to any user"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(req.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP.");
        }

        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired.");
        }
        log.info("OTP verified successfully for phone={}", phoneNumber);
    }

    public void resetPassword(ResetPasswordRequest req) {
        String phoneNumber = req.getPhoneNumber();
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.replaceAll("[\\+\\s]", "");
        }
        User user = userRepo.findByCellPhone(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("the provided number is not linked to any user"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(req.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP.");
        }

        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword().trim().toUpperCase()));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepo.save(user);
        log.info("Password reset successfully for phone={}", phoneNumber);
    }

    public LoginResponse login(LoginRequest req, String ipAddress) {
        try {
            String username = req.getUsername() != null ? req.getUsername().trim().toUpperCase() : null;
            String password = req.getPassword() != null ? req.getPassword().trim().toUpperCase() : null;

            log.debug("Authenticating username={}", username);
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            log.debug("AuthenticationManager accepted username={}", username);

            UserDetails ud = userDetailsService.loadUserByUsername(username);
            log.debug("UserDetails loaded for username={}", username);

            User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));
            log.debug("User entity loaded for username={} role={} active={}",
                    user.getUsername(), user.getRole(), user.getActive());

            String token = jwtConfig.generateToken(ud, user.getRole().name());
            user.setCurrentToken(token);
            userRepo.save(user);
            log.info("Login successful for username={}", req.getUsername());

            activityLogService.logActivity(user.getUsername(), "LOGIN", "User logged in successfully", "POST", "/api/auth/login", 200, ipAddress);

            String stationId = (user.getStation() != null) ? user.getStation().getId() : null;
            var banks = (user.getStation() != null) ? user.getStation().getBanks() : null;

            return new LoginResponse(
                    token, 
                    user.getUsername(), 
                    user.getFullName(), 
                    user.getRole().name(),
                    stationId,
                    banks
            );
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for username={} reason={} message={}",
                    req.getUsername(), e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Login flow failed for username={} reason={} message={}",
                    req.getUsername(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void logout(String ipAddress) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found during logout"));

        user.setCurrentToken(null);
        userRepo.save(user);

        activityLogService.logActivity(username, "LOGOUT", "User logged out successfully", "POST", "/api/auth/logout", 200, ipAddress);
        log.info("Logout successful for username={}", username);
    }
}
