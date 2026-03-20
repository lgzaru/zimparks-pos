package com.tenten.zimparks.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByCellPhone(String cellPhone);
    boolean existsByUsername(String username);
    List<User> findAllByActiveTrue();
    Optional<User> findByIdAndActiveTrue(UUID id);
    List<User> findByStationId(String stationId);
    List<User> findByStationIdAndRoleAndActiveTrue(String stationId, Role role);
    List<User> findByRoleAndActiveTrue(Role role);
}
