package com.tenten.zimparks.transaction;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, String>,
        JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByShiftId(String shiftId);
    List<Transaction> findByStatusAndShiftId(TransactionStatus status, String shiftId);
    List<Transaction> findByStatusAndShiftIdIn(TransactionStatus status, List<String> shiftIds);
    List<Transaction> findByStatusAndOperatorNameIn(TransactionStatus status, List<String> usernames);
    List<Transaction> findByCustomerId(String customerId);

    List<Transaction> findByStationId(String stationId);
    List<Transaction> findByStatusAndStationId(TransactionStatus status, String stationId);
    List<Transaction> findByCustomerIdAndStationId(String customerId, String stationId);

    // txDate is a String like "2026-03-27" — StartingWith filters by month prefix
    List<Transaction> findByStatusAndTxDateStartingWith(TransactionStatus status, String monthPrefix);

    List<Transaction> findByStatusAndTxDateContaining(TransactionStatus status, String monthToken);



}