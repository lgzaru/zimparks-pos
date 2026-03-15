package com.tenten.zimparks.auth;

import com.tenten.zimparks.config.JwtConfig;
import com.tenten.zimparks.config.JwtFilter;
import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SingleSessionTest {

    @Mock
    private AuthenticationManager authManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtConfig jwtConfig;
    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private AuthService authService;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;
    @Mock
    private jakarta.servlet.http.HttpServletResponse response;
    @Mock
    private jakarta.servlet.FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        user = User.builder()
                .username("TESTUSER")
                .role(Role.OPERATOR)
                .active(true)
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("TESTUSER")
                .password("PASSWORD")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void login_shouldUpdateCurrentToken() {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password");

        when(userDetailsService.loadUserByUsername("TESTUSER")).thenReturn(userDetails);
        when(userRepo.findByUsername("TESTUSER")).thenReturn(Optional.of(user));
        when(jwtConfig.generateToken(any(), any())).thenReturn("new-token");

        // Act
        LoginResponse response = authService.login(req);

        // Assert
        assertEquals("new-token", response.getToken());
        assertEquals("new-token", user.getCurrentToken());
        verify(userRepo).save(user);
    }

    @Test
    void filter_shouldDenyOldToken() throws Exception {
        // Arrange
        String oldToken = "old-token";
        String currentToken = "current-token";
        user.setCurrentToken(currentToken);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + oldToken);
        when(jwtConfig.extractUsername(oldToken)).thenReturn("TESTUSER");
        when(userDetailsService.loadUserByUsername("TESTUSER")).thenReturn(userDetails);
        when(jwtConfig.validateToken(oldToken, userDetails)).thenReturn(true);
        when(userRepo.findByUsername("TESTUSER")).thenReturn(Optional.of(user));

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void filter_shouldAllowCurrentToken() throws Exception {
        // Arrange
        String currentToken = "current-token";
        user.setCurrentToken(currentToken);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + currentToken);
        when(jwtConfig.extractUsername(currentToken)).thenReturn("TESTUSER");
        when(userDetailsService.loadUserByUsername("TESTUSER")).thenReturn(userDetails);
        when(jwtConfig.validateToken(currentToken, userDetails)).thenReturn(true);
        when(userRepo.findByUsername("TESTUSER")).thenReturn(Optional.of(user));

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        assertNotNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        assertEquals("TESTUSER", org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }
}
