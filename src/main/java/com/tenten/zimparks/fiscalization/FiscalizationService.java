package com.tenten.zimparks.fiscalization;

import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FiscalizationService {

    private final FiscalDeviceRepository fiscalDeviceRepo;
    private final StationRepository stationRepo;

    @Transactional
    public FiscalDevice configureDevice(FiscalDeviceConfigDTO dto) {
        // 0. Validation: Basic null/empty checks
        if (dto.getDeviceSerialNo() == null || dto.getDeviceSerialNo().trim().isEmpty()) {
            throw new RuntimeException("Device serial number is required.");
        }
        if (dto.getVirtualDeviceId() == null || dto.getVirtualDeviceId().trim().isEmpty()) {
            throw new RuntimeException("Virtual Device ID is required.");
        }
        if (dto.getStationId() == null || dto.getStationId().trim().isEmpty()) {
            throw new RuntimeException("Station ID is required.");
        }

        // 1. Validation: Check if device is already configured (by Serial No)
        Optional<FiscalDevice> existingBySerial = fiscalDeviceRepo.findById(dto.getDeviceSerialNo());
        if (existingBySerial.isPresent()) {
            throw new RuntimeException("Device with serial " + dto.getDeviceSerialNo() + " is already configured.");
        }

        // 2. Validation: Check if virtualDeviceId is already linked to another device
        Optional<FiscalDevice> existingByVId = fiscalDeviceRepo.findByVirtualDeviceId(dto.getVirtualDeviceId());
        if (existingByVId.isPresent()) {
            throw new RuntimeException("Virtual Device ID " + dto.getVirtualDeviceId() + " is already linked to another device.");
        }

        // 3. Resolve Station
        Station station = stationRepo.findById(dto.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found: " + dto.getStationId()));

        // 4. Create and Save FiscalDevice
        FiscalDevice device = FiscalDevice.builder()
                .deviceSerialNo(dto.getDeviceSerialNo())
                .virtualDeviceId(dto.getVirtualDeviceId())
                .station(station)
                .taxPayerName(dto.getTaxPayerName())
                .build();

        return fiscalDeviceRepo.save(device);
    }

    public Optional<FiscalDevice> getDeviceConfig(String deviceSerialNo) {
        return fiscalDeviceRepo.findById(deviceSerialNo);
    }

    @Transactional
    public FiscalDevice updateDevice(String deviceSerialNo, FiscalDeviceConfigDTO dto) {
        FiscalDevice device = fiscalDeviceRepo.findById(deviceSerialNo)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceSerialNo));

        // 1. If virtual ID is changed, validate it's not taken
        if (!device.getVirtualDeviceId().equals(dto.getVirtualDeviceId())) {
            Optional<FiscalDevice> existingByVId = fiscalDeviceRepo.findByVirtualDeviceId(dto.getVirtualDeviceId());
            if (existingByVId.isPresent()) {
                throw new RuntimeException("Virtual Device ID " + dto.getVirtualDeviceId() + " is already linked to another device.");
            }
            device.setVirtualDeviceId(dto.getVirtualDeviceId());
        }

        // 2. Update Station
        Station station = stationRepo.findById(dto.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found: " + dto.getStationId()));
        device.setStation(station);

        // 3. Update Other Fields
        device.setTaxPayerName(dto.getTaxPayerName());

        return fiscalDeviceRepo.save(device);
    }

    @Transactional
    public void deleteDevice(String deviceSerialNo) {
        fiscalDeviceRepo.deleteById(deviceSerialNo);
    }

    public List<FiscalDevice> getAllDevices() {
        return fiscalDeviceRepo.findAll();
    }
}
