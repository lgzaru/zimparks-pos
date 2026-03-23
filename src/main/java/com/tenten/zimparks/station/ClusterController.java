package com.tenten.zimparks.station;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clusters")
@RequiredArgsConstructor
@Tag(name = "Clusters", description = "Cluster configuration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class ClusterController {

    private final StationService service;

    @GetMapping
    @Operation(summary = "List all clusters.")
    public List<ClusterDto> list() {
        return service.getClusters();
    }
}
