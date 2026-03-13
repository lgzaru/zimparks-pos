package com.tenten.zimparks.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repo;

    public List<Customer> findAll() {
        return repo.findAll();
    }

    public List<Customer> search(String q) {
        return repo.findByNameContainingIgnoreCase(q);
    }

    public Customer create(Customer c) {
        if (c.getId() == null || c.getId().isBlank())
            c.setId("CUST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        return repo.save(c);
    }

    public Customer update(String id, Customer patch) {
        Customer c = repo.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
        c.setName(patch.getName());
        c.setEmail(patch.getEmail());
        c.setPhone(patch.getPhone());
        c.setType(patch.getType());
        c.setNationality(patch.getNationality());
        return repo.save(c);
    }
}