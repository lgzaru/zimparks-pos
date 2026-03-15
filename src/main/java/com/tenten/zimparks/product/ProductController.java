package com.tenten.zimparks.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService service;

    @GetMapping
    @Operation(summary = "List all products.")
    public List<Product> list(@RequestParam(required = false) String stationId) {
        if (stationId != null) {
            return service.findByStation(stationId);
        }
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Create a product.")
    public ResponseEntity<Product> create(@RequestBody Product p) {
        return ResponseEntity.ok(service.create(p));
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
