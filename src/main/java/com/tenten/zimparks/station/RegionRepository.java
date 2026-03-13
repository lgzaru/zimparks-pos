package com.tenten.zimparks.station;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    List<Region> findAllByCluster(Cluster cluster);
}
