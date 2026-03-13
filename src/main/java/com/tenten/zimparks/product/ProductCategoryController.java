package com.tenten.zimparks.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-categories")
@RequiredArgsConstructor
@Tag(name = "Product Categories", description = "Product category endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class ProductCategoryController {

    private final ProductCategoryService service;

    @GetMapping
    @Operation(summary = "List all product categories.")
    public List<ProductCategory> getAllCategories() {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Add a new product category.")
    public ResponseEntity<ProductCategory> addNewCategory(@RequestBody ProductCategory category) {
        return ResponseEntity.ok(service.create(category));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Update a product category.")
    public ResponseEntity<ProductCategory> update(@PathVariable String code, @RequestBody ProductCategory category) {
        return ResponseEntity.ok(service.update(code, category));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a product category.")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }
}
