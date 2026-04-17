package com.tenten.zimparks.cashup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashupSubmissionRepository extends JpaRepository<CashupSubmission, Long> {
    Optional<CashupSubmission> findByShiftId(String shiftId);
    List<CashupSubmission> findByStatusAndShiftIdIn(String status, List<String> shiftIds);
    List<CashupSubmission> findByShiftIdIn(List<String> shiftIds);
}
