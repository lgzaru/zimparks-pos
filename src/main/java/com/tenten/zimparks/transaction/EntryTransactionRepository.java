package com.tenten.zimparks.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EntryTransactionRepository extends JpaRepository<EntryTransaction, Long> {
    List<EntryTransaction> findByTxRef(String txRef);
}
