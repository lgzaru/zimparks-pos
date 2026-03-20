package com.tenten.zimparks.creditnote;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditNoteRepository extends JpaRepository<CreditNote, String> {
    List<CreditNote> findByStatus(String status);
    List<CreditNote> findByShiftId(String shiftId);
    List<CreditNote> findByStatusNotAndShiftId(String status, String shiftId);
    List<CreditNote> findByStatusAndShiftIdIn(String status, List<String> shiftIds);
    List<CreditNote> findByStatusAndRaisedByIn(String status, List<String> usernames);
    long countByStatusNot(String status);
}
