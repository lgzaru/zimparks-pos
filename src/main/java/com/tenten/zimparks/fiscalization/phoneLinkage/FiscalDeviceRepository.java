package com.tenten.zimparks.fiscalization.phoneLinkage;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FiscalDeviceRepository extends JpaRepository<FiscalDevice, Long> {
    Optional<FiscalDevice> findByDeviceSerialNo(String deviceSerialNo);
    Optional<FiscalDevice> findByPhoneSerialNumber(String phoneSerialNumber);

    /** Fetch all devices with their station in a single join — avoids N+1 for the device monitor. */
    @EntityGraph(attributePaths = {"station"})
    @Query("SELECT fd FROM FiscalDevice fd")
    List<FiscalDevice> findAllWithStation();
}