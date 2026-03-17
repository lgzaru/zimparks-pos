package com.tenten.zimparks.fiscalization;

import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiscalizationServiceTest {

    @Mock
    private FiscalDeviceRepository fiscalDeviceRepo;

    @Mock
    private StationRepository stationRepo;

    @InjectMocks
    private FiscalizationService fiscalizationService;

    @Test
    void configureDevice_shouldCreateNewFiscalDevice() {
        // Arrange
        FiscalDeviceConfigDTO dto = FiscalDeviceConfigDTO.builder()
                .deviceSerialNo("SN123")
                .virtualDeviceId("V-001")
                .stationId("ST01")
                .taxPayerName("Test TaxPayer")
                .build();

        Station station = Station.builder().id("ST01").name("Station 01").build();

        when(fiscalDeviceRepo.findById(dto.getDeviceSerialNo())).thenReturn(Optional.empty());
        when(fiscalDeviceRepo.findByVirtualDeviceId(dto.getVirtualDeviceId())).thenReturn(Optional.empty());
        when(stationRepo.findById(dto.getStationId())).thenReturn(Optional.of(station));
        when(fiscalDeviceRepo.save(any(FiscalDevice.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        FiscalDevice result = fiscalizationService.configureDevice(dto);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getDeviceSerialNo(), result.getDeviceSerialNo());
        assertEquals(dto.getVirtualDeviceId(), result.getVirtualDeviceId());
        assertEquals(station, result.getStation());
        verify(fiscalDeviceRepo).save(any(FiscalDevice.class));
    }

    @Test
    void configureDevice_shouldThrowExceptionIfDeviceAlreadyConfigured() {
        // Arrange
        FiscalDeviceConfigDTO dto = FiscalDeviceConfigDTO.builder()
                .deviceSerialNo("SN123")
                .virtualDeviceId("V-001")
                .stationId("ST01")
                .build();

        FiscalDevice existingDevice = FiscalDevice.builder().deviceSerialNo("SN123").build();
        when(fiscalDeviceRepo.findById(dto.getDeviceSerialNo())).thenReturn(Optional.of(existingDevice));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fiscalizationService.configureDevice(dto);
        });

        assertTrue(exception.getMessage().contains("already configured"));
        verify(fiscalDeviceRepo, never()).save(any());
    }

    @Test
    void configureDevice_shouldThrowExceptionIfVDeviceIdAlreadyUsed() {
        // Arrange
        FiscalDeviceConfigDTO dto = FiscalDeviceConfigDTO.builder()
                .deviceSerialNo("SN124")
                .virtualDeviceId("V-001")
                .stationId("ST01")
                .build();

        FiscalDevice existingDeviceWithVId = FiscalDevice.builder().deviceSerialNo("SN123").virtualDeviceId("V-001").build();
        
        when(fiscalDeviceRepo.findById(dto.getDeviceSerialNo())).thenReturn(Optional.empty());
        when(fiscalDeviceRepo.findByVirtualDeviceId(dto.getVirtualDeviceId())).thenReturn(Optional.of(existingDeviceWithVId));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fiscalizationService.configureDevice(dto);
        });

        assertTrue(exception.getMessage().contains("already linked to another device"));
        verify(fiscalDeviceRepo, never()).save(any());
    }

    @Test
    void configureDevice_shouldThrowExceptionIfStationNotFound() {
        // Arrange
        FiscalDeviceConfigDTO dto = FiscalDeviceConfigDTO.builder()
                .deviceSerialNo("SN125")
                .virtualDeviceId("V-002")
                .stationId("INVALID_STATION")
                .build();

        when(fiscalDeviceRepo.findById(dto.getDeviceSerialNo())).thenReturn(Optional.empty());
        when(fiscalDeviceRepo.findByVirtualDeviceId(dto.getVirtualDeviceId())).thenReturn(Optional.empty());
        when(stationRepo.findById(dto.getStationId())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fiscalizationService.configureDevice(dto);
        });

        assertTrue(exception.getMessage().contains("Station not found"));
        verify(fiscalDeviceRepo, never()).save(any());
    }

    @Test
    void configureDevice_shouldThrowExceptionIfVirtualDeviceIdIsNull() {
        // Arrange
        FiscalDeviceConfigDTO dto = FiscalDeviceConfigDTO.builder()
                .deviceSerialNo("SN126")
                .virtualDeviceId(null)
                .stationId("ST01")
                .build();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fiscalizationService.configureDevice(dto);
        });

        assertTrue(exception.getMessage().contains("Virtual Device ID is required"));
        verify(fiscalDeviceRepo, never()).save(any());
    }

    @Test
    void updateDevice_shouldUpdateFields() {
        // Arrange
        String serial = "SN123";
        FiscalDeviceConfigDTO dto = FiscalDeviceConfigDTO.builder()
                .deviceSerialNo(serial)
                .virtualDeviceId("V-NEW")
                .stationId("ST02")
                .taxPayerName("Updated Name")
                .build();

        Station oldStation = Station.builder().id("ST01").build();
        Station newStation = Station.builder().id("ST02").build();
        FiscalDevice existing = FiscalDevice.builder()
                .deviceSerialNo(serial)
                .virtualDeviceId("V-OLD")
                .station(oldStation)
                .build();

        when(fiscalDeviceRepo.findById(serial)).thenReturn(Optional.of(existing));
        when(fiscalDeviceRepo.findByVirtualDeviceId("V-NEW")).thenReturn(Optional.empty());
        when(stationRepo.findById("ST02")).thenReturn(Optional.of(newStation));
        when(fiscalDeviceRepo.save(any(FiscalDevice.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        FiscalDevice result = fiscalizationService.updateDevice(serial, dto);

        // Assert
        assertEquals("V-NEW", result.getVirtualDeviceId());
        assertEquals(newStation, result.getStation());
        assertEquals("Updated Name", result.getTaxPayerName());
        verify(fiscalDeviceRepo).save(existing);
    }

    @Test
    void deleteDevice_shouldCallRepository() {
        // Arrange
        String serial = "SN123";

        // Act
        fiscalizationService.deleteDevice(serial);

        // Assert
        verify(fiscalDeviceRepo).deleteById(serial);
    }

    @Test
    void getAllDevices_shouldReturnList() {
        // Arrange
        FiscalDevice d1 = FiscalDevice.builder().deviceSerialNo("SN1").build();
        FiscalDevice d2 = FiscalDevice.builder().deviceSerialNo("SN2").build();
        when(fiscalDeviceRepo.findAll()).thenReturn(List.of(d1, d2));

        // Act
        List<FiscalDevice> result = fiscalizationService.getAllDevices();

        // Assert
        assertEquals(2, result.size());
        assertEquals("SN1", result.get(0).getDeviceSerialNo());
        assertEquals("SN2", result.get(1).getDeviceSerialNo());
        verify(fiscalDeviceRepo).findAll();
    }
}
