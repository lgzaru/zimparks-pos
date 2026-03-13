package com.tenten.zimparks.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, String> {
    List<Receipt> findByShiftId(String shiftId);
}
