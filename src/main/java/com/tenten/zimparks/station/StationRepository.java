package com.tenten.zimparks.station;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, String> {

    // StationRepository.java
// StationRepository.java
    @Query("SELECT MAX(s.id) FROM Station s WHERE s.id LIKE :prefix%")
    Optional<String> findMaxIdByPrefix(@Param("prefix") String prefix);
}

