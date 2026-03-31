package com.tenten.zimparks.fiscalization.phoneLinkage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FiscalDeviceRepository extends JpaRepository<FiscalDevice, Long> {
    Optional<FiscalDevice> findByDeviceSerialNo(String deviceSerialNo);
    Optional<FiscalDevice> findByPhoneSerialNumber(String phoneSerialNumber);
}