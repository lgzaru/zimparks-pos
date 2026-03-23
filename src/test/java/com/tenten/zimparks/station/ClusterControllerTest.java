package com.tenten.zimparks.station;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClusterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StationService stationService;

    @InjectMocks
    private ClusterController clusterController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clusterController).build();
    }

    @Test
    void listClusters_shouldReturnAllClusters() throws Exception {
        // Arrange
        List<ClusterDto> clusters = List.of(
                new ClusterDto("Hwange Cluster", "HW"),
                new ClusterDto("Harare Cluster", "HE")
        );
        when(stationService.getClusters()).thenReturn(clusters);

        // Act & Assert
        mockMvc.perform(get("/api/clusters")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Hwange Cluster"))
                .andExpect(jsonPath("$[0].code").value("HW"))
                .andExpect(jsonPath("$[1].name").value("Harare Cluster"))
                .andExpect(jsonPath("$[1].code").value("HE"));
    }
}
