package com.tenten.zimparks.product;

import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<Product> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public Page<Product> findByStation(String stationId, Pageable pageable) {
        return repo.findByIdStationId(stationId, pageable);
    }

    public List<Product> findByStation(String stationId) {
        return repo.findByIdStationId(stationId);
    }

    public Product create(Product p) {
        if (p.getId() != null && p.getId().getStationId() != null && p.getCategory() != null) {
            Station station = stationRepo.findById(p.getId().getStationId())
                    .orElseThrow(() -> new RuntimeException("Station not found"));

            if (station.getCluster() == null) {
                throw new RuntimeException("Station " + station.getId() + " has no cluster assigned. Cannot create product.");
            }

            String stationId = station.getId();                    // "ST_HE_02"
            String stationPart = stationId.replace("_", "");       // "STHE02"

            int next = repo.findMaxCodeByStationId(stationId, stationPart)
                    .map(maxCode -> {
                        // Extract trailing digits: "STHE01P012" → 12
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)$").matcher(maxCode);
                        return m.find() ? Integer.parseInt(m.group(1)) + 1 : 1;
                    })
                    .orElse(1);

            String newCode = String.format("%sP%03d", stationPart, next); // "STHE02P001"
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

            if (station.getCluster() == null) {
                throw new RuntimeException("Station " + station.getId() + " has no cluster assigned. Cannot update product category.");
            }

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

    public void delete(ProductId id) {
        repo.deleteById(id);
    }
}
