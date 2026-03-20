package com.tenten.zimparks.activity;

import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepo;
    private final UserRepository userRepo;

    @Transactional
    public void logActivity(String username, String operation, String details) {
        log.debug("Logging activity: username={}, operation={}, details={}", username, operation, details);
        User user = userRepo.findByUsername(username).orElse(null);
        if (user != null) {
            ActivityLog logEntry = ActivityLog.builder()
                    .user(user)
                    .username(username)
                    .operation(operation)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();
            activityLogRepo.save(logEntry);
        } else {
            log.warn("Could not log activity: user {} not found", username);
        }
    }

    public Page<ActivityLog> getLogsForCurrentUser(String search, Pageable pageable) {
        String username = getCurrentUsername();
        User user = userRepo.findByUsername(username).orElse(null);

        if (user != null && user.getRole() == Role.ADMIN) {
            if (search != null && !search.isBlank()) {
                return activityLogRepo.findByOperationContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrDetailsContainingIgnoreCaseOrderByTimestampDesc(
                        search, search, search, pageable);
            }
            return activityLogRepo.findAllByOrderByTimestampDesc(pageable);
        }

        if (search != null && !search.isBlank()) {
            return activityLogRepo.findByUsernameAndOperationContainingIgnoreCaseOrUsernameAndDetailsContainingIgnoreCaseOrderByTimestampDesc(
                    username, search, username, search, pageable);
        }
        return activityLogRepo.findByUsernameOrderByTimestampDesc(username, pageable);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }
}
