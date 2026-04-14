package com.tenten.zimparks.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuotationRepository extends JpaRepository<Quotation, String> {

    List<Quotation> findByStatus(QuotationStatus status);

    List<Quotation> findByCustomerId(String customerId);

    List<Quotation> findByRefContainingIgnoreCaseOrCustomerNameContainingIgnoreCase(
            String ref, String customerName);
}
