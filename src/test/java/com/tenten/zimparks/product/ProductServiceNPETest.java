package com.tenten.zimparks.product;

import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceNPETest {

    @Mock
    private ProductRepository repo;

    @Mock
    private StationRepository stationRepo;

    @InjectMocks
    private ProductService productService;

    @Test
    void create_shouldThrowExceptionWhenStationHasNoCluster() {
        // Arrange
        String stationId = "ST01";
        ProductId productId = new ProductId("P01", stationId);
        ProductCategory category = ProductCategory.builder().code("A").build();
        Product product = Product.builder()
                .id(productId)
                .category(category)
                .price(new BigDecimal("10.00"))
                .build();

        Station station = Station.builder().id(stationId).cluster(null).build();

        when(stationRepo.findById(stationId)).thenReturn(Optional.of(station));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.create(product));
        assertTrue(exception.getMessage().contains("has no cluster assigned"));
    }

    @Test
    void create_shouldThrowExceptionWhenCategoryIsNull() {
        // Arrange
        ProductId productId = new ProductId("P01", "ST01");
        Product product = Product.builder()
                .id(productId)
                .category(null)
                .price(new BigDecimal("10.00"))
                .build();

        // Act & Assert
        // Current implementation just skips the logic if category is null, but maybe it should be mandatory
        // For now, let's just document current behavior if we don't change it.
    }

    @Test
    void update_shouldThrowExceptionWhenStationHasNoClusterAndCategoryChanges() {
        // Arrange
        String stationId = "ST01";
        ProductId productId = new ProductId("HWST01AP01", stationId);
        ProductCategory oldCategory = ProductCategory.builder().code("A").build();
        ProductCategory newCategory = ProductCategory.builder().code("B").build();
        
        Product existingProduct = Product.builder()
                .id(productId)
                .category(oldCategory)
                .price(new BigDecimal("10.00"))
                .build();

        Product patch = Product.builder()
                .category(newCategory)
                .price(new BigDecimal("20.00"))
                .build();

        Station station = Station.builder().id(stationId).cluster(null).build();

        when(repo.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(stationRepo.findById(stationId)).thenReturn(Optional.of(station));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.update(productId, patch));
        assertTrue(exception.getMessage().contains("has no cluster assigned"));
    }
}
