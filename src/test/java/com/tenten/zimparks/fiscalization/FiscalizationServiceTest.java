package com.tenten.zimparks.fiscalization;

import com.tenten.zimparks.fiscalization.phoneLinkage.*;
import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

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

    @Mock
    private FiscalizationClient fiscalizationClient;

    @InjectMocks
    private FiscalizationService fiscalizationService;

    @Test
    void getUnlinkedDevices_shouldCallClient() {
        // Arrange
        FiscalDeviceDTO d1 = new FiscalDeviceDTO();
        d1.setDeviceSerialNo("SN1");
        when(fiscalizationClient.getDevices(false)).thenReturn(List.of(d1));

        // Act
        List<FiscalDeviceDTO> result = fiscalizationService.getUnlinkedDevices();

        // Assert
        assertEquals(1, result.size());
        assertEquals("SN1", result.get(0).getDeviceSerialNo());
        verify(fiscalizationClient).getDevices(false);
    }

    @Test
    void linkDevice_shouldCallClientAndSave() {
        // Arrange
        FiscalLinkRequestDTO dto = new FiscalLinkRequestDTO();
        dto.setDeviceID(33941L);
        dto.setPhoneSerialNumber("IMEI-123");

        FiscalDeviceDTO linkedDeviceDTO = new FiscalDeviceDTO();
        linkedDeviceDTO.setDeviceID(33941L);
        linkedDeviceDTO.setDeviceSerialNo("SN-LINKED");

        Station station = Station.builder().id("ST01").name("Station 01").build();

        when(fiscalizationClient.linkDevice(dto)).thenReturn(linkedDeviceDTO);
        when(stationRepo.findById("ST01")).thenReturn(Optional.of(station));
        when(fiscalDeviceRepo.findByDeviceSerialNo("SN-LINKED")).thenReturn(Optional.empty());
        when(fiscalDeviceRepo.save(any(FiscalDevice.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        FiscalDevice result = fiscalizationService.linkDevice(dto, "ST01");

        // Assert
        assertNotNull(result);
        assertEquals(33941L, result.getDeviceId());
        assertEquals(station, result.getStation());
        verify(fiscalizationClient).linkDevice(dto);
        verify(fiscalDeviceRepo).save(any(FiscalDevice.class));
    }

    @Test
    void deleteDevice_shouldCallRepository() {
        // Arrange
        String serial = "SN123";
        FiscalDevice device = FiscalDevice.builder().deviceSerialNo(serial).build();
        when(fiscalDeviceRepo.findByDeviceSerialNo(serial)).thenReturn(Optional.of(device));

        // Act
        fiscalizationService.deleteDevice(serial);

        // Assert
        verify(fiscalDeviceRepo).delete(device);
    }

    @Test
    void getAllLocalDevices_shouldReturnList() {
        // Arrange
        FiscalDevice d1 = FiscalDevice.builder().deviceSerialNo("SN1").build();
        FiscalDevice d2 = FiscalDevice.builder().deviceSerialNo("SN2").build();
        when(fiscalDeviceRepo.findAll()).thenReturn(List.of(d1, d2));

        // Act
        List<FiscalDevice> result = fiscalizationService.getAllLocalDevices();

        // Assert
        assertEquals(2, result.size());
        verify(fiscalDeviceRepo).findAll();
    }

    @Test
    void unlinkDevice_shouldSucceedWhenDeviceFound() {
        // Arrange
        String phoneSerial = "IMEI-123";
        FiscalDevice device = FiscalDevice.builder()
                .deviceSerialNo("SN-123")
                .phoneSerialNumber(phoneSerial)
                .linked(true)
                .build();

        when(fiscalDeviceRepo.findByPhoneSerialNumber(phoneSerial)).thenReturn(Optional.of(device));
        when(fiscalDeviceRepo.save(any(FiscalDevice.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        FiscalDevice result = fiscalizationService.unlinkDevice(phoneSerial);

        // Assert
        assertNotNull(result);
        assertFalse(result.isLinked());
        assertNull(result.getPhoneSerialNumber());
        assertEquals("Unlinked", result.getLinkStatus());
        verify(fiscalizationClient).unlinkDevice(phoneSerial);
        verify(fiscalDeviceRepo).save(device);
    }

    @Test
    void unlinkDevice_shouldReturnNullWhenLocalNotFound() {
        // Arrange
        String phoneSerial = "MISSING";
        when(fiscalDeviceRepo.findByPhoneSerialNumber(phoneSerial)).thenReturn(Optional.empty());

        // Act
        FiscalDevice result = fiscalizationService.unlinkDevice(phoneSerial);

        // Assert
        assertNull(result);
        verify(fiscalizationClient).unlinkDevice(phoneSerial);
    }

    @Test
    void unlinkDevice_shouldSucceedWhenExternalNotFound() {
        // Arrange
        String phoneSerial = "IMEI-123";
        FiscalDevice device = FiscalDevice.builder()
                .deviceSerialNo("SN-123")
                .phoneSerialNumber(phoneSerial)
                .linked(true)
                .build();

        doThrow(HttpClientErrorException.NotFound.class).when(fiscalizationClient).unlinkDevice(phoneSerial);
        when(fiscalDeviceRepo.findByPhoneSerialNumber(phoneSerial)).thenReturn(Optional.of(device));
        when(fiscalDeviceRepo.save(any(FiscalDevice.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        FiscalDevice result = fiscalizationService.unlinkDevice(phoneSerial);

        // Assert
        assertNotNull(result);
        assertFalse(result.isLinked());
        assertNull(result.getPhoneSerialNumber());
        verify(fiscalizationClient).unlinkDevice(phoneSerial);
        verify(fiscalDeviceRepo).save(device);
    }
}
