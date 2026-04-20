package com.tenten.zimparks.cashup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashupSubmissionRepository extends JpaRepository<CashupSubmission, Long> {
    Optional<CashupSubmission> findByShiftId(String shiftId);
    List<CashupSubmission> findByStatusAndShiftIdIn(String status, List<String> shiftIds);
    List<CashupSubmission> findByShiftIdIn(List<String> shiftIds);

    @Query(value = "SELECT * FROM cashup_submissions c " +
                   "WHERE (:status   IS NULL OR c.status = :status) " +
                   "AND   (:search   IS NULL " +
                   "       OR c.submitted_by::text ILIKE CONCAT('%', :search, '%') " +
                   "       OR c.shift_id::text     ILIKE CONCAT('%', :search, '%')) " +
                   "AND   (CAST(:dateFrom AS timestamp) IS NULL OR c.submitted_at >= CAST(:dateFrom AS timestamp)) " +
                   "AND   (CAST(:dateTo   AS timestamp) IS NULL OR c.submitted_at <= CAST(:dateTo   AS timestamp)) " +
                   "ORDER BY c.submitted_at DESC",
           countQuery = "SELECT COUNT(*) FROM cashup_submissions c " +
                        "WHERE (:status   IS NULL OR c.status = :status) " +
                        "AND   (:search   IS NULL " +
                        "       OR c.submitted_by::text ILIKE CONCAT('%', :search, '%') " +
                        "       OR c.shift_id::text     ILIKE CONCAT('%', :search, '%')) " +
                        "AND   (CAST(:dateFrom AS timestamp) IS NULL OR c.submitted_at >= CAST(:dateFrom AS timestamp)) " +
                        "AND   (CAST(:dateTo   AS timestamp) IS NULL OR c.submitted_at <= CAST(:dateTo   AS timestamp))",
           nativeQuery = true)
    Page<CashupSubmission> findHistory(@Param("status")   String        status,
                                       @Param("search")   String        search,
                                       @Param("dateFrom") LocalDateTime dateFrom,
                                       @Param("dateTo")   LocalDateTime dateTo,
                                       Pageable pageable);

    @Query(value = "SELECT * FROM cashup_submissions c " +
                   "WHERE (:status   IS NULL OR c.status = :status) " +
                   "AND   (:search   IS NULL " +
                   "       OR c.submitted_by::text ILIKE CONCAT('%', :search, '%') " +
                   "       OR c.shift_id::text     ILIKE CONCAT('%', :search, '%')) " +
                   "AND   (CAST(:dateFrom AS timestamp) IS NULL OR c.submitted_at >= CAST(:dateFrom AS timestamp)) " +
                   "AND   (CAST(:dateTo   AS timestamp) IS NULL OR c.submitted_at <= CAST(:dateTo   AS timestamp)) " +
                   "ORDER BY c.submitted_at DESC",
           nativeQuery = true)
    List<CashupSubmission> findForExport(@Param("status")   String        status,
                                         @Param("search")   String        search,
                                         @Param("dateFrom") LocalDateTime dateFrom,
                                         @Param("dateTo")   LocalDateTime dateTo);
}
