package com.tenten.zimparks.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, ProductId> {
    Page<Product> findByIdStationId(String stationId, Pageable pageable);
    boolean existsByCategoryCode(String categoryCode);
}
