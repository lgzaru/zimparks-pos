package com.tenten.zimparks.transaction;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, String>,
        JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByStatus(String status);
    List<Transaction> findByShiftId(String shiftId);
    List<Transaction> findByStatusAndShiftId(String status, String shiftId);
    List<Transaction> findByCustomerId(String customerId);

    List<Transaction> findByStationId(String stationId);
    List<Transaction> findByStatusAndStationId(String status, String stationId);
    List<Transaction> findByCustomerIdAndStationId(String customerId, String stationId);
}