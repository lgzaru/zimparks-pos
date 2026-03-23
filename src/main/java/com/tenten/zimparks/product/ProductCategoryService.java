package com.tenten.zimparks.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {
    private final ProductCategoryRepository repository;
    private final ProductRepository productRepository;

    public List<ProductCategory> findAll() {
        return repository.findAll();
    }

    public ProductCategory create(ProductCategory category) {
        if (repository.existsById(category.getCode())) {
            throw new RuntimeException("Category with code " + category.getCode() + " already exists");
        }
        return repository.save(category);
    }

    public ProductCategory update(String code, ProductCategory patch) {
        ProductCategory category = repository.findById(code)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setDescription(patch.getDescription());
        return repository.save(category);
    }

    public void delete(String code) {
        if (productRepository.existsByCategoryCode(code)) {
            throw new RuntimeException("Cannot delete category linked to products");
        }
        repository.deleteById(code);
    }
}
