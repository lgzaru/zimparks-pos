package com.tenten.zimparks.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository repository;

    @InjectMocks
    private ProductCategoryService service;

    @Test
    void findAll_shouldReturnAllCategories() {
        ProductCategory c1 = new ProductCategory("A", "Category A");
        ProductCategory c2 = new ProductCategory("B", "Category B");
        when(repository.findAll()).thenReturn(Arrays.asList(c1, c2));

        List<ProductCategory> result = service.findAll();

        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    void create_shouldSaveCategory() {
        ProductCategory c = new ProductCategory("A", "Category A");
        when(repository.existsById("A")).thenReturn(false);
        when(repository.save(c)).thenReturn(c);

        ProductCategory result = service.create(c);

        assertNotNull(result);
        assertEquals("A", result.getCode());
        verify(repository).save(c);
    }

    @Test
    void create_shouldThrowExceptionIfAlreadyExists() {
        ProductCategory c = new ProductCategory("A", "Category A");
        when(repository.existsById("A")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> service.create(c));
        verify(repository, never()).save(any());
    }

    @Test
    void update_shouldUpdateDescription() {
        ProductCategory existing = new ProductCategory("A", "Old Description");
        ProductCategory patch = new ProductCategory("A", "New Description");
        when(repository.findById("A")).thenReturn(Optional.of(existing));
        when(repository.save(any(ProductCategory.class))).thenAnswer(i -> i.getArguments()[0]);

        ProductCategory result = service.update("A", patch);

        assertEquals("New Description", result.getDescription());
        verify(repository).save(existing);
    }

    @Test
    void delete_shouldCallRepository() {
        service.delete("A");
        verify(repository).deleteById("A");
    }
}
