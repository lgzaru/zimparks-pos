package com.tenten.zimparks.voucher;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WifiVoucherRepository extends JpaRepository<WifiVoucher, Long> {
    List<WifiVoucher> findByTxRef(String txRef);
    boolean existsByCode(String code);
}
