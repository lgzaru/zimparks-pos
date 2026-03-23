package com.tenten.zimparks.shift;

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
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ShiftControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShiftService shiftService;

    @InjectMocks
    private ShiftController shiftController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shiftController).build();
    }

    @Test
    void getActive_shouldReturnActiveShifts() throws Exception {
        // Arrange
        List<Map<String, Object>> activeShifts = List.of(
                Map.of(
                        "username", "operator1",
                        "id", "SHF-001",
                        "status", "Open",
                        "startTime", "08:30"
                ),
                Map.of(
                        "username", "operator2",
                        "id", "SHF-002",
                        "status", "Open",
                        "startTime", "09:15"
                )
        );
        when(shiftService.getActiveShifts()).thenReturn(activeShifts);

        // Act & Assert
        mockMvc.perform(get("/api/shifts/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("operator1"))
                .andExpect(jsonPath("$[0].id").value("SHF-001"))
                .andExpect(jsonPath("$[0].status").value("Open"))
                .andExpect(jsonPath("$[0].startTime").value("08:30"))
                .andExpect(jsonPath("$[1].username").value("operator2"))
                .andExpect(jsonPath("$[1].id").value("SHF-002"))
                .andExpect(jsonPath("$[1].status").value("Open"))
                .andExpect(jsonPath("$[1].startTime").value("09:15"));
    }
}
