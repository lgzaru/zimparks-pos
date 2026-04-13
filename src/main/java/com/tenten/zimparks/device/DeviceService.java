package com.tenten.zimparks.device;

import com.tenten.zimparks.fiscalization.phoneLinkage.FiscalDevice;
import com.tenten.zimparks.fiscalization.phoneLinkage.FiscalDeviceRepository;
import com.tenten.zimparks.station.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository       deviceRepository;
    private final StationRepository      stationRepository;
    private final FiscalDeviceRepository fiscalDeviceRepository;

    public List<DeviceResponse> findAll() {
        List<FiscalDevice> fiscalDevices = fiscalDeviceRepository.findAllWithStation();

        Map<String, Device> heartbeats = deviceRepository.findAll().stream()
                .collect(Collectors.toMap(Device::getSerialNumber, d -> d));

        return fiscalDevices.stream()
                .map(fd -> {
                    Device hb = heartbeats.get(fd.getDeviceSerialNo());
                    return new DeviceResponse(
                            fd.getDeviceSerialNo(),
                            fd.getTaxpayerName() != null ? fd.getTaxpayerName() : fd.getPhoneSerialNumber(),
                            fd.getStation() != null ? fd.getStation().getId()   : null,
                            fd.getStation() != null ? fd.getStation().getName() : null,
                            hb != null ? hb.getAppVersion()   : null,
                            hb != null ? hb.getIpAddress()    : null,
                            hb != null ? hb.getLocalIp()      : null,
                            hb != null ? hb.getPlatform()     : null,
                            hb != null ? hb.getBatteryLevel() : null,
                            hb != null ? hb.getIsCharging()   : null,
                            hb != null ? hb.getLoggedInUser() : null,
                            hb != null ? hb.getShiftId()      : null,
                            hb != null ? hb.getLastSeen()     : null,
                            fd.getCreatedAt()
                    );
                })
                .sorted((a, b) -> {
                    if (a.lastSeen() == null && b.lastSeen() == null) return 0;
                    if (a.lastSeen() == null) return 1;
                    if (b.lastSeen() == null) return -1;
                    return b.lastSeen().compareTo(a.lastSeen());
                })
                .toList();
    }

    public Device heartbeat(String serialNumber, String clientIp, HeartbeatRequest req) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseGet(() -> Device.builder()
                        .serialNumber(serialNumber)
                        .registeredAt(LocalDateTime.now())
                        .build());

        if (req.getDeviceName()    != null) device.setDeviceName(req.getDeviceName());
        if (req.getAppVersion()    != null) device.setAppVersion(req.getAppVersion());
        if (req.getPlatform()      != null) device.setPlatform(req.getPlatform());
        if (req.getLoggedInUser()  != null) device.setLoggedInUser(req.getLoggedInUser());
        if (req.getShiftId()       != null) device.setShiftId(req.getShiftId());
        if (req.getBatteryLevel()  != null) device.setBatteryLevel(req.getBatteryLevel());
        if (req.getIsCharging()    != null) device.setIsCharging(req.getIsCharging());
        if (clientIp               != null) device.setIpAddress(clientIp);
        if (req.getLocalIp()       != null) device.setLocalIp(req.getLocalIp());

        if (req.getStationId() != null) {
            stationRepository.findById(req.getStationId()).ifPresent(device::setStation);
        }

        device.setLastSeen(LocalDateTime.now());
        return deviceRepository.save(device);
    }
}
