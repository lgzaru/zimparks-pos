package com.tenten.zimparks.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnlinePaymentRepository extends JpaRepository<OnlinePayment, String> {
}
