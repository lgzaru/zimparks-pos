package com.tenten.zimparks.activity;
import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import com.tenten.zimparks.util.RequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ActivityLogFilter extends OncePerRequestFilter {

    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = (request instanceof ContentCachingRequestWrapper) 
                ? (ContentCachingRequestWrapper) request 
                : new ContentCachingRequestWrapper(request, 10000); // 10KB limit or whatever is appropriate

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            logActivity(wrappedRequest, response);
        }
    }

    private void logActivity(ContentCachingRequestWrapper request, HttpServletResponse response) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Skip login/logout as they are already logged in AuthService
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/logout")) {
            return;
        }

        // Only log /api/ requests
        if (!uri.startsWith("/api/")) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }

        Role role = user.getRole();

        // Admin logs all state-changing operations
        if (role == Role.ADMIN) {
            logStateChangingOperation(request, response, username, method, uri);
            return;
        }

        // Supervisor: product creation, bank linking, void approvals, credit note approvals, plus state-changing
        if (role == Role.SUPERVISOR) {
            if (isSupervisorAction(method, uri)) {
                logStateChangingOperation(request, response, username, method, uri);
            } else {
                // Also log other state-changing operations for supervisor as before
                logStateChangingOperation(request, response, username, method, uri);
            }
            return;
        }

        // Operator: capture transactions creations, void requests, credit request.
        if (role == Role.OPERATOR) {
            if (isOperatorAction(method, uri)) {
                logStateChangingOperation(request, response, username, method, uri);
            }
            return;
        }
    }

    private boolean isSupervisorAction(String method, String uri) {
        // product creation: POST /api/products
        if ("POST".equalsIgnoreCase(method) && uri.equals("/api/products")) return true;
        // bank linking/unlinking: POST /api/banks, DELETE /api/banks/{id}
        if ("POST".equalsIgnoreCase(method) && uri.equals("/api/banks")) return true;
        if ("DELETE".equalsIgnoreCase(method) && uri.startsWith("/api/banks/")) return true;
        // void approvals/rejections: PATCH /api/transactions/{ref}/void/approve, PATCH /api/transactions/{ref}/void/reject
        if ("PATCH".equalsIgnoreCase(method) && uri.matches("/api/transactions/[^/]+/void/approve")) return true;
        if ("PATCH".equalsIgnoreCase(method) && uri.matches("/api/transactions/[^/]+/void/reject")) return true;
        // credit note approvals/rejections: PATCH /api/credit-notes/{id}/approve, PATCH /api/credit-notes/{id}/reject
        if ("PATCH".equalsIgnoreCase(method) && uri.matches("/api/credit-notes/[^/]+/approve")) return true;
        if ("PATCH".equalsIgnoreCase(method) && uri.matches("/api/credit-notes/[^/]+/reject")) return true;
        // open/close shift: POST /api/shifts/open, POST /api/shifts/close/{username}
        if ("POST".equalsIgnoreCase(method) && uri.equals("/api/shifts/open")) return true;
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/shifts/close/")) return true;

        return false;
    }

    private boolean isOperatorAction(String method, String uri) {
        // transactions creations: POST /api/transactions
        if ("POST".equalsIgnoreCase(method) && uri.equals("/api/transactions")) return true;
        // void requests: PATCH /api/transactions/{ref}/void
        if ("PATCH".equalsIgnoreCase(method) && uri.matches("/api/transactions/[^/]+/void")) return true;
        // credit request: POST /api/credit-notes
        if ("POST".equalsIgnoreCase(method) && uri.equals("/api/credit-notes")) return true;

        return false;
    }

    private void logAdminOperation(ContentCachingRequestWrapper request, HttpServletResponse response, String username, String method, String uri) {
        String operation = determineOperationName(method);
        String body = getRequestBody(request);
        String details = body != null ? body : "NONE";
        int status = response.getStatus();
        String ip = RequestUtils.getClientIp(request);
        activityLogService.logActivity(username, operation, details, method, uri, status, ip);
    }

    private void logStateChangingOperation(ContentCachingRequestWrapper request, HttpServletResponse response, String username, String method, String uri) {
        if (!"GET".equalsIgnoreCase(method)) {
            logAdminOperation(request, response, username, method, uri);
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String determineOperationName(String method) {
        if ("POST".equalsIgnoreCase(method)) return "CREATE";
        if ("PUT".equalsIgnoreCase(method)) return "UPDATE";
        if ("PATCH".equalsIgnoreCase(method)) return "EDIT";
        if ("DELETE".equalsIgnoreCase(method)) return "DELETE";
        return method;
    }
}
