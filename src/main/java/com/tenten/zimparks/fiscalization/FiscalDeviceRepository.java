package com.tenten.zimparks.fiscalization;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FiscalDeviceRepository extends JpaRepository<FiscalDevice, String> {
    Optional<FiscalDevice> findByVirtualDeviceId(String vDeviceId);
}
