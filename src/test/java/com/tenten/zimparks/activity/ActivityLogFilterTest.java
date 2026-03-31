package com.tenten.zimparks.activity;

import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActivityLogFilterTest {

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityLogFilter activityLogFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void doFilterInternal_AdminUser_ShouldLogWithBody() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");
        request.setContent("{\"name\":\"Test Product\"}".getBytes());
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(201);

        User admin = User.builder().username("admin").role(Role.ADMIN).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        doAnswer(invocation -> {
            ContentCachingRequestWrapper wrapper = invocation.getArgument(0);
            // Simulate reading the input stream to trigger content caching
            wrapper.getInputStream().readAllBytes();
            return null;
        }).when(filterChain).doFilter(any(), any());

        // Act
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService).logActivity(
            eq("admin"), 
            eq("CREATE"), 
            eq("{\"name\":\"Test Product\"}"),
            eq("POST"),
            eq("/api/products"),
            eq(201),
            anyString()
        );
    }

    @Test
    void doFilterInternal_OperatorUser_AllowedAction_ShouldLog() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions");
        request.setContent("{\"amount\":100}".getBytes());
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);

        User operator = User.builder().username("operator").role(Role.OPERATOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("operator");
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operator));

        doAnswer(invocation -> {
            ContentCachingRequestWrapper wrapper = invocation.getArgument(0);
            wrapper.getInputStream().readAllBytes();
            return null;
        }).when(filterChain).doFilter(any(), any());

        // Act
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService).logActivity(
            eq("operator"), 
            eq("CREATE"), 
            eq("{\"amount\":100}"),
            eq("POST"),
            eq("/api/transactions"),
            eq(200),
            anyString()
        );
    }

    @Test
    void doFilterInternal_OperatorUser_VoidRequest_ShouldLog() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/transactions/TX123/void");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);

        User operator = User.builder().username("operator").role(Role.OPERATOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("operator");
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operator));

        // Act
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService).logActivity(
            eq("operator"), 
            eq("EDIT"), 
            eq("NONE"),
            eq("PATCH"),
            eq("/api/transactions/TX123/void"),
            eq(200),
            anyString()
        );
    }

    @Test
    void doFilterInternal_OperatorUser_NotAllowedAction_ShouldNotLog() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");
        HttpServletResponse response = mock(HttpServletResponse.class);

        User operator = User.builder().username("operator").role(Role.OPERATOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("operator");
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operator));

        // Act
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService, never()).logActivity(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void doFilterInternal_SupervisorUser_VoidApprove_ShouldLog() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/transactions/TX123/void/approve");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);

        User supervisor = User.builder().username("supervisor").role(Role.SUPERVISOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("supervisor");
        when(userRepository.findByUsername("supervisor")).thenReturn(Optional.of(supervisor));

        // Act
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService).logActivity(
            eq("supervisor"), 
            eq("EDIT"), 
            eq("NONE"),
            eq("PATCH"),
            eq("/api/transactions/TX123/void/approve"),
            eq(200),
            anyString()
        );
    }

    @Test
    void doFilterInternal_AdminUser_GetRequest_ShouldNotLog() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        HttpServletResponse response = mock(HttpServletResponse.class);

        User admin = User.builder().username("admin").role(Role.ADMIN).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        // Act
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService, never()).logActivity(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void doFilterInternal_NonAdminUser_GetRequest_ShouldNotLog() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        HttpServletResponse response = mock(HttpServletResponse.class);

        User supervisor = User.builder().username("supervisor").role(Role.SUPERVISOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("supervisor");
        when(userRepository.findByUsername("supervisor")).thenReturn(Optional.of(supervisor));

        // Act
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService, never()).logActivity(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }
    @Test
    void doFilterInternal_SupervisorUser_ShiftOperations_ShouldLog() throws Exception {
        // Arrange
        User supervisor = User.builder().username("supervisor").role(Role.SUPERVISOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("supervisor");
        when(userRepository.findByUsername("supervisor")).thenReturn(Optional.of(supervisor));

        // Act & Assert - Open Shift
        MockHttpServletRequest openRequest = new MockHttpServletRequest("POST", "/api/shifts/open");
        HttpServletResponse openResponse = mock(HttpServletResponse.class);
        when(openResponse.getStatus()).thenReturn(200);
        activityLogFilter.doFilterInternal(openRequest, openResponse, filterChain);
        verify(activityLogService).logActivity(eq("supervisor"), eq("CREATE"), eq("NONE"), eq("POST"), eq("/api/shifts/open"), eq(200), anyString());

        // Act & Assert - Close Shift
        MockHttpServletRequest closeRequest = new MockHttpServletRequest("POST", "/api/shifts/close/user1");
        HttpServletResponse closeResponse = mock(HttpServletResponse.class);
        when(closeResponse.getStatus()).thenReturn(200);
        activityLogFilter.doFilterInternal(closeRequest, closeResponse, filterChain);
        verify(activityLogService).logActivity(eq("supervisor"), eq("CREATE"), eq("NONE"), eq("POST"), eq("/api/shifts/close/user1"), eq(200), anyString());
    }

    @Test
    void doFilterInternal_SupervisorUser_CreditNoteApprovals_ShouldLog() throws Exception {
        // Arrange
        User supervisor = User.builder().username("supervisor").role(Role.SUPERVISOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("supervisor");
        when(userRepository.findByUsername("supervisor")).thenReturn(Optional.of(supervisor));

        // Act & Assert - Approve
        MockHttpServletRequest approveReq = new MockHttpServletRequest("PATCH", "/api/credit-notes/CN1/approve");
        HttpServletResponse approveRes = mock(HttpServletResponse.class);
        when(approveRes.getStatus()).thenReturn(200);
        activityLogFilter.doFilterInternal(approveReq, approveRes, filterChain);
        verify(activityLogService).logActivity(eq("supervisor"), eq("EDIT"), eq("NONE"), eq("PATCH"), eq("/api/credit-notes/CN1/approve"), eq(200), anyString());

        // Act & Assert - Reject
        MockHttpServletRequest rejectReq = new MockHttpServletRequest("PATCH", "/api/credit-notes/CN2/reject");
        HttpServletResponse rejectRes = mock(HttpServletResponse.class);
        when(rejectRes.getStatus()).thenReturn(200);
        activityLogFilter.doFilterInternal(rejectReq, rejectRes, filterChain);
        verify(activityLogService).logActivity(eq("supervisor"), eq("EDIT"), eq("NONE"), eq("PATCH"), eq("/api/credit-notes/CN2/reject"), eq(200), anyString());
    }

    @Test
    void doFilterInternal_SupervisorUser_VoidRejection_ShouldLog() throws Exception {
        // Arrange
        User supervisor = User.builder().username("supervisor").role(Role.SUPERVISOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("supervisor");
        when(userRepository.findByUsername("supervisor")).thenReturn(Optional.of(supervisor));

        // Act
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/transactions/TX123/void/reject");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService).logActivity(eq("supervisor"), eq("EDIT"), eq("NONE"), eq("PATCH"), eq("/api/transactions/TX123/void/reject"), eq(200), anyString());
    }

    @Test
    void doFilterInternal_OperatorUser_CreditNoteInitiation_ShouldLog() throws Exception {
        // Arrange
        User operator = User.builder().username("operator").role(Role.OPERATOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("operator");
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operator));

        // Act
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/credit-notes");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(201);
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(activityLogService).logActivity(eq("operator"), eq("CREATE"), eq("NONE"), eq("POST"), eq("/api/credit-notes"), eq(201), anyString());
    }

    @Test
    void doFilterInternal_TransactionCreation_WithResponseLoggingEnabled_ShouldLogRequestAndResponse() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(activityLogFilter, "logTransactionResponse", true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions");
        String requestBody = "{\"amount\":100}";
        request.setContent(requestBody.getBytes());
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}
            @Override public void write(int b) throws java.io.IOException {}
        });
        when(response.getStatus()).thenReturn(201);

        User operator = User.builder().username("operator").role(Role.OPERATOR).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("operator");
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operator));

        String responseBody = "{\"ref\":\"TXN-123\",\"status\":\"PAID\"}";
        doAnswer(invocation -> {
            ContentCachingRequestWrapper reqWrapper = invocation.getArgument(0);
            reqWrapper.getInputStream().readAllBytes(); // consume request body

            ContentCachingResponseWrapper resWrapper = invocation.getArgument(1);
            resWrapper.getWriter().write(responseBody);
            return null;
        }).when(filterChain).doFilter(any(), any());

        // Act
        activityLogFilter.doFilterInternal(request, response, filterChain);

        // Assert
        String expectedDetails = "REQUEST:\n" + requestBody + "\n\nRESPONSE:\n" + responseBody;
        verify(activityLogService).logActivity(
            eq("operator"),
            eq("CREATE"),
            eq(expectedDetails),
            eq("POST"),
            eq("/api/transactions"),
            eq(201),
            anyString()
        );
    }
}
