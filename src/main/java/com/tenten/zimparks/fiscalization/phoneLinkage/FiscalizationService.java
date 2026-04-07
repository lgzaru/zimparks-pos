package com.tenten.zimparks.fiscalization.phoneLinkage;

import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalizationService {

    private final FiscalDeviceRepository fiscalDeviceRepo;
    private final StationRepository stationRepo;
    private final FiscalizationClient fiscalizationClient;

    // ── External-system queries (no local DB involvement) ────────────────────

    /** Returns unlinked devices straight from the external system. */
    public List<FiscalDeviceDTO> getUnlinkedDevices() {
        return fiscalizationClient.getDevices(false);
    }

    /** Returns linked devices straight from the external system (verification). */
    public List<FiscalDeviceDTO> getLinkedDevices() {
        return fiscalizationClient.getDevices(true);
    }

    /** Device detail by serial number from the external system. */
    public Optional<FiscalDeviceDTO> getExternalDeviceDetail(String serialNo) {
        return fiscalizationClient.getDeviceBySerialNo(serialNo);
    }

    // ── Link + local persistence ─────────────────────────────────────────────

    /**
     * 1. Calls the external API to link the device.
     * 2. Resolves the Station (if stationId provided).
     * 3. Upserts the result into the local DB so the device is tracked here
     *    and associated with a Station for downstream usage.
     */
    @Transactional
    public FiscalDevice linkDevice(FiscalLinkRequestDTO dto, String stationId) {
        FiscalDeviceDTO linked = fiscalizationClient.linkDevice(dto);

        Station resolvedStation = null;
        if (stationId != null) {
            resolvedStation = stationRepo.findById(stationId)
                    .orElseThrow(() -> new RuntimeException("Station not found: " + stationId));
        }

        final Station station = resolvedStation;

        return fiscalDeviceRepo.findByDeviceSerialNo(linked.getDeviceSerialNo())
                .map(existing -> {
                    updateEntity(existing, linked);
                    if (station != null) existing.setStation(station);
                    return fiscalDeviceRepo.save(existing);
                })
                .orElseGet(() -> {
                    FiscalDevice entity = toEntity(linked);
                    entity.setStation(station);
                    return fiscalDeviceRepo.save(entity);
                });
    }

    // ── Unlink ───────────────────────────────────────────────────────────────

    /**
     * 1. Calls the external API to unlink the device by phone serial.
     * 2. Marks the local record as linked=false and clears the phone serial.
     *    The record is kept for audit purposes.
     *
     * Throws if the device is not found locally.
     */
    @Transactional
    public FiscalDevice unlinkDevice(String phoneSerial) {
        // 1. Notify external system
        try {
            fiscalizationClient.unlinkDevice(phoneSerial);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Device already unlinked or not found in external system for phoneSerial: {}", phoneSerial);
        }

        // 2. Delete local record
        Optional<FiscalDevice> deviceOpt = fiscalDeviceRepo.findByPhoneSerialNumber(phoneSerial);
        if (deviceOpt.isEmpty()) {
            log.warn("No local record found to delete for phoneSerial: {}", phoneSerial);
            return null;
        }

        FiscalDevice device = deviceOpt.get();
        fiscalDeviceRepo.delete(device);
        return device;
    }

    // ── Local DB queries ─────────────────────────────────────────────────────

    public List<FiscalDevice> getAllLocalDevices() {
        return fiscalDeviceRepo.findAll();
    }

    public Optional<FiscalDevice> getLocalDeviceConfig(String deviceSerialNo) {
        return fiscalDeviceRepo.findByDeviceSerialNo(deviceSerialNo);
    }

    @Transactional
    public void deleteDevice(String deviceSerialNo) {
        fiscalDeviceRepo.findByDeviceSerialNo(deviceSerialNo)
                .ifPresent(fiscalDeviceRepo::delete);
    }

    public FiscalDayResponseDTO getFiscalStatus(FiscalDayRequestDTO request) {
        return fiscalizationClient.getFiscalStatus(request);
    }

    public FiscalDayResponseDTO openFiscalDay(FiscalDayRequestDTO request) {
        return fiscalizationClient.openFiscalDay(request);
    }

    public FiscalDayResponseDTO closeFiscalDay(FiscalDayRequestDTO request) {
        return fiscalizationClient.closeFiscalDay(request);
    }

    // ── Mapping helpers (DTO → Entity) ───────────────────────────────────────

    private FiscalDevice toEntity(FiscalDeviceDTO dto) {
        return FiscalDevice.builder()
                .deviceId(dto.getDeviceID())
                .deviceSerialNo(dto.getDeviceSerialNo())
                .fiscalDayStatus(dto.getFiscalDayStatus())
                .certValidTill(dto.getCertValidTill())
                .tin(dto.getTin())
                .linked(dto.isLinked())
                .phoneSerialNumber(dto.getPhoneSerialNumber())
                .linkStatus(dto.getLinkStatus())
                .linkedAt(dto.getLinkedAt())
                .taxpayerName(dto.getTaxpayerName())
                .vatNumber(dto.getVatNumber())
                .build();
    }

    private void updateEntity(FiscalDevice entity, FiscalDeviceDTO dto) {
        entity.setFiscalDayStatus(dto.getFiscalDayStatus());
        entity.setCertValidTill(dto.getCertValidTill());
        entity.setLinked(dto.isLinked());
        entity.setPhoneSerialNumber(dto.getPhoneSerialNumber());
        entity.setLinkStatus(dto.getLinkStatus());
        entity.setLinkedAt(dto.getLinkedAt());
    }
}