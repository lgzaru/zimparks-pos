package com.tenten.zimparks.fiscalization.phoneLinkage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FiscalizationClient {

    private final RestTemplate restTemplate;

    @Value("${fiscalization.base-url}")
    private String baseUrl;

    /**
     * GET /api/mobile/fiscal/devices?linked=false  → unlinked devices
     * GET /api/mobile/fiscal/devices?linked=true   → linked devices
     */
    public List<FiscalDeviceDTO> getDevices(boolean linked) {
        String url = baseUrl + "/api/mobile/fiscal/devices?linked=" + linked;
        log.debug("Fetching fiscal devices (linked={}) from: {}", linked, url);
        ResponseEntity<List<FiscalDeviceDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<FiscalDeviceDTO>>() {}
        );
        return response.getBody();
    }

    /**
     * POST /api/mobile/fiscal/link-device
     */
    public FiscalDeviceDTO linkDevice(FiscalLinkRequestDTO request) {
        String url = baseUrl + "/api/mobile/fiscal/link-device";
        log.debug("Linking fiscal device (deviceID={}) at: {}", request.getDeviceID(), url);
        return restTemplate.postForObject(url, request, FiscalDeviceDTO.class);
    }

    /**
     * POST /api/mobile/fiscal/unlink/{phoneSerial}
     */
    public void unlinkDevice(String phoneSerial) {
        String url = baseUrl + "/api/mobile/fiscal/" + phoneSerial+"/unlink";
        log.debug("Unlinking fiscal device (phoneSerial={}) at: {}", phoneSerial, url);
        restTemplate.postForObject(url, null, Void.class);
    }

    /**
     * GET /api/mobile/fiscal/device/{serialNo}
     * Returns empty Optional when the external API responds with 404.
     */
    public Optional<FiscalDeviceDTO> getDeviceBySerialNo(String serialNo) {
        String url = baseUrl + "/api/mobile/fiscal/device/" + serialNo;
        log.debug("Fetching fiscal device detail for serialNo={} from: {}", serialNo, url);
        try {
            FiscalDeviceDTO dto = restTemplate.getForObject(url, FiscalDeviceDTO.class);
            return Optional.ofNullable(dto);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Device not found on external system for serialNo={}", serialNo);
            return Optional.empty();
        }
    }
}