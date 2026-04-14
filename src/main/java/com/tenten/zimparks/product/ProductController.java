package com.tenten.zimparks.product;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService service;

    @GetMapping
    @Operation(summary = "List all products, optionally filtered by station.")
    public ResponseEntity<Page<Product>> list(
            @Parameter(description = "Station ID to filter products by.") @RequestParam(required = false) String stationId,
            @PageableDefault(size = 30) Pageable pageable) {
        if (stationId != null) {
            return ResponseEntity.ok(service.findByStation(stationId, pageable));
        }
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/station/{stationId}")
    @Operation(summary = "Get all products for a specific station.")
    public ResponseEntity<List<Product>> getByStation(@PathVariable String stationId) {
        return ResponseEntity.ok(service.findByStation(stationId));
    }

    @PostMapping
    @Operation(summary = "Create a product.")
    public ResponseEntity<Product> create(@RequestBody Product p) {
        return ResponseEntity.ok(service.create(p));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk import products. Returns imported products and per-row errors.")
    public ResponseEntity<BulkImportResult> bulkCreate(@RequestBody List<Product> products) {
        return ResponseEntity.ok(service.bulkCreate(products));
    }

    @PutMapping("/{stationId}/{code}")
    @Operation(summary = "Update a product by station and code.")
    public ResponseEntity<Product> update(@PathVariable String stationId, @PathVariable String code, @RequestBody Product p) {
        return ResponseEntity.ok(service.update(new ProductId(code, stationId), p));
    }

    @PatchMapping("/{stationId}/{code}")
    @Operation(summary = "Partially update a product by station and code.")
    public ResponseEntity<Product> patch(@PathVariable String stationId, @PathVariable String code, @RequestBody Product p) {
        return ResponseEntity.ok(service.update(new ProductId(code, stationId), p));
    }

    @DeleteMapping("/{stationId}/{code}")
    @Operation(summary = "Delete a product by station and code.")
    public ResponseEntity<Void> delete(@PathVariable String stationId, @PathVariable String code) {
        service.delete(new ProductId(code, stationId));
        return ResponseEntity.noContent().build();
    }
}
