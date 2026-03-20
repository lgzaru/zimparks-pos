package com.tenten.zimparks.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    Page<ActivityLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);
    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<ActivityLog> findByUsernameAndOperationContainingIgnoreCaseOrUsernameAndDetailsContainingIgnoreCaseOrderByTimestampDesc(
            String username1, String operation, String username2, String details, Pageable pageable);

    Page<ActivityLog> findByOperationContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrDetailsContainingIgnoreCaseOrderByTimestampDesc(
            String operation, String username, String details, Pageable pageable);
}
