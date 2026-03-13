package com.tenten.zimparks.station;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository repo;

    public List<Region> findAll() {
        return repo.findAll();
    }

    public List<Region> findAllByCluster(Cluster cluster) {
        return repo.findAllByCluster(cluster);
    }

    public Region findById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Region not found"));
    }

    public Region create(Region region) {
        return repo.save(region);
    }
}
