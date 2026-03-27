package com.tenten.zimparks.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, ProductId> {
    Page<Product> findByIdStationId(String stationId, Pageable pageable);
    List<Product> findByIdStationId(String stationId);
    boolean existsByCategoryCode(String categoryCode);
    @Query("SELECT MAX(p.id.code) FROM Product p WHERE p.id.stationId = :stationId AND p.id.code LIKE 'P%'")
    Optional<String> findMaxCodeByStationId(@Param("stationId") String stationId);

    // ProductId has a 'code' field → Spring Data resolves id.code automatically
    List<Product> findByIdCodeIn(Collection<String> codes);

}
