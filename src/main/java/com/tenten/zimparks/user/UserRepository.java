package com.tenten.zimparks.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByCellPhone(String cellPhone);
    boolean existsByUsername(String username);
    Page<User> findAllByActiveTrue(Pageable pageable);
    Optional<User> findByIdAndActiveTrue(UUID id);
    List<User> findByStationId(String stationId);
    Page<User> findByStationIdAndRoleAndActiveTrue(String stationId, Role role, Pageable pageable);
    Page<User> findByRoleAndActiveTrue(Role role, Pageable pageable);
}
