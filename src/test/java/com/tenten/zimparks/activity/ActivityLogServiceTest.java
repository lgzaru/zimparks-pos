package com.tenten.zimparks.activity;

import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private ActivityLogService activityLogService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("TESTUSER")
                .build();
    }

    @Test
    void logActivity_shouldSaveLog() {
        // Arrange
        when(userRepo.findByUsername("TESTUSER")).thenReturn(Optional.of(user));

        // Act
        activityLogService.logActivity("TESTUSER", "CREATE", "Test details", "POST", "/api/test", 201, "127.0.0.1");

        // Assert
        verify(activityLogRepo, times(1)).save(argThat(log -> 
            "TESTUSER".equals(log.getUsername()) &&
            "CREATE".equals(log.getOperation()) &&
            "POST".equals(log.getMethod()) &&
            "/api/test".equals(log.getUri()) &&
            Integer.valueOf(201).equals(log.getStatus()) &&
            "127.0.0.1".equals(log.getIpAddress()) &&
            "Test details".equals(log.getDetails())
        ));
    }

    @Test
    void getLogsForCurrentUser_shouldReturnLogs() {
        // Arrange
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("TESTUSER")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        user.setRole(Role.OPERATOR);
        when(userRepo.findByUsername("TESTUSER")).thenReturn(Optional.of(user));

        PageRequest pageable = PageRequest.of(0, 10);
        Page<ActivityLog> expectedPage = new PageImpl<>(List.of(new ActivityLog()));
        when(activityLogRepo.findByUsernameOrderByTimestampDesc("TESTUSER", pageable)).thenReturn(expectedPage);

        // Act
        Page<ActivityLog> result = activityLogService.getLogsForCurrentUser(null, pageable);

        // Assert
        assertEquals(expectedPage, result);
        verify(activityLogRepo).findByUsernameOrderByTimestampDesc("TESTUSER", pageable);
    }

    @Test
    void getLogsForCurrentUser_AdminShouldReturnAllLogs() {
        // Arrange
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("ADMINUSER")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        User adminUser = User.builder().username("ADMINUSER").role(Role.ADMIN).build();
        when(userRepo.findByUsername("ADMINUSER")).thenReturn(Optional.of(adminUser));

        PageRequest pageable = PageRequest.of(0, 10);
        Page<ActivityLog> expectedPage = new PageImpl<>(List.of(new ActivityLog(), new ActivityLog()));
        when(activityLogRepo.findAllByOrderByTimestampDesc(pageable)).thenReturn(expectedPage);

        // Act
        Page<ActivityLog> result = activityLogService.getLogsForCurrentUser(null, pageable);

        // Assert
        assertEquals(expectedPage, result);
        verify(activityLogRepo).findAllByOrderByTimestampDesc(pageable);
        verify(activityLogRepo, never()).findByUsernameOrderByTimestampDesc(anyString(), any());
    }
}
