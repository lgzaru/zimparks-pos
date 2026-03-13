package com.tenten.zimparks.customer;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer lookup and maintenance.")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    @Operation(summary = "List customers, optionally filtered by search text.")
    public List<Customer> list(@RequestParam(required = false) String q) {
        return (q != null && !q.isBlank()) ? service.search(q) : service.findAll();
    }

    @PostMapping
    @Operation(summary = "Create a customer record.")
    public ResponseEntity<Customer> create(@RequestBody Customer c) {
        return ResponseEntity.ok(service.create(c));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer record.")
    public ResponseEntity<Customer> update(@PathVariable String id, @RequestBody Customer c) {
        return ResponseEntity.ok(service.update(id, c));
    }
}
