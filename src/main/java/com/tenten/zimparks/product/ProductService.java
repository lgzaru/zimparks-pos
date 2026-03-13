package com.tenten.zimparks.product;

import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repo;
    private final StationRepository stationRepo;

    public List<Product> findAll()               { return repo.findAll(); }
    public List<Product> findByStation(String stationId) { return repo.findByIdStationId(stationId); }

    public Product create(Product p)             {
        if (p.getId() != null && p.getId().getStationId() != null && p.getCategory() != null) {
            Station station = stationRepo.findById(p.getId().getStationId())
                    .orElseThrow(() -> new RuntimeException("Station not found"));
            String clusterCode = station.getCluster().getCode();
            String stationId = station.getId();
            String categoryCode = p.getCategory().getCode();
            String originalProductCode = p.getId().getCode();

            String newCode = clusterCode + stationId + categoryCode + originalProductCode;
            p.getId().setCode(newCode);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            p.setCreatedBy(auth.getName());
        }
        p.setCreatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    public Product update(ProductId id, Product patch) {
        Product p = repo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        p.setDescr(patch.getDescr());
        p.setPrice(patch.getPrice());

        if (patch.getCategory() != null && !patch.getCategory().equals(p.getCategory())) {
            // Category changed, need to update the product code as well
            String oldCategoryCode = p.getCategory().getCode();
            String newCategoryCode = patch.getCategory().getCode();

            Station station = stationRepo.findById(id.getStationId())
                    .orElseThrow(() -> new RuntimeException("Station not found"));

            String clusterCode = station.getCluster().getCode();
            String stationId = station.getId();

            // Extract the original product code from the old ID code
            // Convention: Cluster(2) + Station(4) + Category(1) + OriginalCode
            // But wait, stationId might not be 4 chars.
            // Let's use the actual codes to be sure where they start/end.
            String oldFullCode = id.getCode();
            String prefixToRemove = clusterCode + stationId + oldCategoryCode;
            String originalProductCode = oldFullCode.substring(prefixToRemove.length());

            String newFullCode = clusterCode + stationId + newCategoryCode + originalProductCode;

            // Since ProductId is the primary key, we cannot just change it in the existing entity
            // We must delete the old one and save a new one.
            repo.deleteById(id);
            p.setId(new ProductId(newFullCode, stationId));
            p.setCategory(patch.getCategory());
        }

        return repo.save(p);
    }

    public void delete(ProductId id)              { repo.deleteById(id); }
}
