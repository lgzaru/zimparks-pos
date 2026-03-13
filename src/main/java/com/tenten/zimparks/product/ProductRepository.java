package com.tenten.zimparks.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, ProductId> {
    List<Product> findByIdStationId(String stationId);
}
