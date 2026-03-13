package com.tenten.zimparks.station;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
@Tag(name = "Regions", description = "Region configuration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class RegionController {

    private final RegionService service;

    @GetMapping
    @Operation(summary = "List all regions.")
    public List<Region> list(@RequestParam(required = false) Cluster cluster) {
        if (cluster != null) {
            return service.findAllByCluster(cluster);
        }
        return service.findAll();
    }
}
