package com.tenten.zimparks.product;

import com.tenten.zimparks.station.Cluster;
import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repo;

    @Mock
    private StationRepository stationRepo;

    @InjectMocks
    private ProductService productService;

    @Test
    void create_shouldPopulateFieldsAndGenerateCode() {
        // Arrange
        String stationId = "ST01";
        String categoryCode = "A";
        String initialCode = "P01";
        
        ProductCategory category = ProductCategory.builder().code(categoryCode).description("Test Category").build();
        Station station = Station.builder().id(stationId).cluster(Cluster.HW).build();
        
        Product p = Product.builder()
                .id(new ProductId(initialCode, stationId))
                .descr("Test Product")
                .price(new BigDecimal("10.00"))
                .category(category)
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(stationRepo.findById(stationId)).thenReturn(java.util.Optional.of(station));
        when(repo.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Product result = productService.create(p);

        // Assert
        assertNotNull(result.getCreatedAt());
        assertEquals("testuser", result.getCreatedBy());
        // Cluster(HW) + Station(ST01) + Category(A) + Code(P01)
        assertEquals("HWST01AP01", result.getId().getCode());
        verify(repo).save(p);

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void update_shouldUpdateCategoryAndGenerateNewCode() {
        // Arrange
        String stationId = "ST01";
        String oldCategoryCode = "A";
        String newCategoryCode = "B";
        String originalCode = "P01";
        String oldCode = "HWST01A" + originalCode;
        ProductId oldId = new ProductId(oldCode, stationId);

        ProductCategory oldCategory = ProductCategory.builder().code(oldCategoryCode).description("Category A").build();
        ProductCategory newCategory = ProductCategory.builder().code(newCategoryCode).description("Category B").build();
        Station station = Station.builder().id(stationId).cluster(Cluster.HW).build();

        Product existingProduct = Product.builder()
                .id(oldId)
                .descr("Old Descr")
                .price(new BigDecimal("10.00"))
                .category(oldCategory)
                .build();

        Product patch = Product.builder()
                .descr("New Descr")
                .price(new BigDecimal("20.00"))
                .category(newCategory)
                .build();

        when(repo.findById(oldId)).thenReturn(java.util.Optional.of(existingProduct));
        when(stationRepo.findById(stationId)).thenReturn(java.util.Optional.of(station));
        when(repo.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Product result = productService.update(oldId, patch);

        // Assert
        assertEquals("New Descr", result.getDescr());
        assertEquals(new BigDecimal("20.00"), result.getPrice());
        assertEquals(newCategoryCode, result.getCategory().getCode());
        // Cluster(HW) + Station(ST01) + NewCategory(B) + OriginalCode(P01)
        assertEquals("HWST01BP01", result.getId().getCode());
        
        // Since the ID changed, it should have been deleted and a new one saved, 
        // or the service should handle the ID update correctly.
        verify(repo).deleteById(oldId);
        verify(repo).save(any(Product.class));
    }

    @Test
    void update_shouldOnlyUpdateDescrAndPriceWhenCategoryIsNull() {
        // Arrange
        String stationId = "ST01";
        String categoryCode = "A";
        String originalCode = "P01";
        String code = "HWST01A" + originalCode;
        ProductId id = new ProductId(code, stationId);

        ProductCategory category = ProductCategory.builder().code(categoryCode).description("Category A").build();
        
        Product existingProduct = Product.builder()
                .id(id)
                .descr("Old Descr")
                .price(new BigDecimal("10.00"))
                .category(category)
                .build();

        Product patch = Product.builder()
                .descr("New Descr")
                .price(new BigDecimal("20.00"))
                .category(null)
                .build();

        when(repo.findById(id)).thenReturn(java.util.Optional.of(existingProduct));
        when(repo.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Product result = productService.update(id, patch);

        // Assert
        assertEquals("New Descr", result.getDescr());
        assertEquals(new BigDecimal("20.00"), result.getPrice());
        assertEquals(categoryCode, result.getCategory().getCode());
        assertEquals(code, result.getId().getCode());
        
        verify(repo, never()).deleteById(any());
        verify(repo).save(existingProduct);
    }
}
