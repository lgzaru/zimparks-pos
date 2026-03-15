package com.tenten.zimparks.auth;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class SmsService {

    @Value("${sms.base-url}")
    private String baseUrl;

    @Value("${sms.application-id}")
    private String applicationId;

    @Value("${sms.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendSms(String cell, String message) {
        try {
            // 1. Renew Token
            String newToken = renewToken();
            if (newToken != null) {
                this.token = newToken;
            }

            // 2. Send SMS
            String url = baseUrl + "/api/v1/tenten/sendSMS";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(this.token);

            Map<String, String> body = Map.of(
                "txGuid", UUID.randomUUID().toString(),
                "applicationId", applicationId,
                "cell", cell,
                "message", message
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully to {}", cell);
            } else {
                log.error("Failed to send SMS to {}. Status: {}, Response: {}", cell, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending SMS to {}", cell, e);
        }
    }

    private String renewToken() {
        try {
            String url = baseUrl + "/api/v1/tenten/renewAccessToken?applicationId=" + applicationId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(this.token);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("Token");
            }
        } catch (Exception e) {
            log.error("Error renewing SMS token", e);
        }
        return null;
    }
}
