package com.tenten.zimparks.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    List<Customer> findByNameContainingIgnoreCase(String name);
}
