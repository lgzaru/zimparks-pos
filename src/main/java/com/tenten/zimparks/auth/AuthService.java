package com.tenten.zimparks.auth;

import com.tenten.zimparks.config.JwtConfig;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserDetailsService    userDetailsService;
    private final JwtConfig jwtConfig;
    private final UserRepository        userRepo;

    public LoginResponse login(LoginRequest req) {
        try {
            log.debug("Authenticating username={}", req.getUsername());
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
            log.debug("AuthenticationManager accepted username={}", req.getUsername());

            UserDetails ud = userDetailsService.loadUserByUsername(req.getUsername());
            log.debug("UserDetails loaded for username={}", req.getUsername());

            User user = userRepo.findByUsername(req.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));
            log.debug("User entity loaded for username={} role={} active={}",
                    user.getUsername(), user.getRole(), user.isActive());

            String token = jwtConfig.generateToken(ud, user.getRole().name());
            log.info("Login successful for username={}", req.getUsername());
            
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
}
