package com.tenten.zimparks.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findAllByOrderByLastSeenDesc();

    @Modifying
    @Query("DELETE FROM Device d WHERE d.serialNumber LIKE 'browser\\_%' ESCAPE '\\' AND d.lastSeen < :cutoff")
    void deleteStaleBrowserSessions(@Param("cutoff") LocalDateTime cutoff);
}
