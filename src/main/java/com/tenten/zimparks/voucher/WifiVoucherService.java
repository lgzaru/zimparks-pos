package com.tenten.zimparks.voucher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WifiVoucherService {

    private final WifiVoucherRepository repo;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Value("${mikrotik.api.url}")
    private String mikrotikUrl;

    @Value("${mikrotik.api.username}")
    private String mikrotikUsername;

    @Value("${mikrotik.api.password}")
    private String mikrotikPassword;

    @Value("${mikrotik.api.server}")
    private String mikrotikServer;

    @Value("${mikrotik.api.enabled:true}")
    private boolean mikrotikEnabled;

    // Uppercase only; excludes visually ambiguous chars: 0,1,B,I,L,O,Q,S,Z
    private static final String CHARSET = "23456789ACDEFGHJKMNPRTUVWXY";
    private static final int SUFFIX_LENGTH = 5;
    private static final SecureRandom RNG = new SecureRandom();

    /**
     * Generates a WiFi voucher for the given transaction.
     *
     * When mikrotik.api.enabled=true the voucher is provisioned on the MikroTik
     * hotspot BEFORE it is persisted.  If provisioning fails (non-2xx response or
     * network error) a RuntimeException is thrown so the calling transaction rolls
     * back — no voucher record is saved and no transaction is committed.
     *
     * When mikrotik.api.enabled=false the voucher is saved unconditionally
     * (useful for testing / offline environments).
     */
    public WifiVoucher generate(String txRef, String productCode, String productDescr,
                                BigDecimal amount, String prefix, String mikrotikProfile,
                                String mikrotikLimitUptime, String mikrotikLimitBytes,
                                LocalDateTime now) {
        String code = buildUniqueCode(prefix != null ? prefix : "");
        WifiVoucher voucher = WifiVoucher.builder()
                .code(code)
                .txRef(txRef)
                .productCode(productCode)
                .productDescr(productDescr)
                .amount(amount)
                .createdAt(now)
                .build();

        if (!mikrotikEnabled) {
            log.info("MikroTik: provisioning disabled (mikrotik.api.enabled=false) — skipping for {}", code);
        } else if (mikrotikProfile == null || mikrotikProfile.isBlank()) {
            // Product is not configured for MikroTik — treat as a hard failure when
            // MikroTik is enabled so the operator is forced to fix the product setup.
            throw new RuntimeException(
                    "MikroTik is enabled but no mikrotikProfile is set on product " + productCode +
                    ". Configure the profile or disable MikroTik integration.");
        } else {
            // Throws RuntimeException on any failure — caller's @Transactional rolls back.
            provisionOnMikroTik(voucher, mikrotikProfile, mikrotikLimitUptime, mikrotikLimitBytes);
        }

        // Only reach here if MikroTik succeeded (or is disabled)
        voucher = repo.save(voucher);
        log.info("WiFi voucher saved: code={} txRef={} product={}", voucher.getCode(), txRef, productCode);
        return voucher;
    }

    public List<WifiVoucher> findByTxRef(String txRef) {
        return repo.findByTxRef(txRef);
    }

    /**
     * Provisions the voucher on the MikroTik hotspot.
     * Throws RuntimeException if the API returns a non-2xx status or if the
     * request cannot be sent — so the caller's transaction is rolled back.
     */
    private void provisionOnMikroTik(WifiVoucher voucher, String profileName,
                                     String limitUptime, String limitBytes) {
        String url = mikrotikUrl + "/rest/ip/hotspot/user/add";
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("name", voucher.getCode());
            body.put("password", voucher.getCode());
            body.put("profile", profileName);
            if (limitUptime != null && !limitUptime.isBlank())       body.put("limit-uptime", limitUptime);
            if (limitBytes  != null && !limitBytes.isBlank())        body.put("limit-bytes-total", limitBytes);
            if (mikrotikServer != null && !mikrotikServer.isBlank()) body.put("server", mikrotikServer);
            String date    = voucher.getCreatedAt().format(DateTimeFormatter.ofPattern("MM.dd.yy"));
            String digits  = voucher.getTxRef().replaceAll("[^0-9]", "");
            String counter = digits.length() >= 3
                    ? digits.substring(digits.length() - 3)
                    : String.format("%03d", digits.isEmpty() ? 0 : Integer.parseInt(digits));
            body.put("comment", "vc-" + counter + "-" + date + "-" + abbreviateProfile(profileName));

            String json = objectMapper.writeValueAsString(body);

            log.debug("MikroTik >>> POST {}", url);
            log.debug("MikroTik >>> Body: {}", json);

            String credentials = Base64.getEncoder().encodeToString(
                    (mikrotikUsername + ":" + mikrotikPassword).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + credentials)
                    .header("Accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            long t0 = System.currentTimeMillis();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            long ms = System.currentTimeMillis() - t0;

            log.debug("MikroTik <<< Status: {} ({}ms)", response.statusCode(), ms);
            log.debug("MikroTik <<< Body: {}", response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("MikroTik: SUCCESS user={} status={} ({}ms) response={}",
                        voucher.getCode(), response.statusCode(), ms, response.body());
            } else {
                log.error("MikroTik: FAILED user={} status={} response={}",
                        voucher.getCode(), response.statusCode(), response.body());
                throw new RuntimeException(
                        "MikroTik provisioning failed for voucher " + voucher.getCode() +
                        ": HTTP " + response.statusCode() + " — " + response.body());
            }
        } catch (RuntimeException e) {
            // Re-throw RuntimeExceptions (including the one we just threw above)
            throw e;
        } catch (Exception e) {
            log.error("MikroTik: FAILED to provision user={} url={} error={}",
                    voucher.getCode(), url, e.getMessage(), e);
            throw new RuntimeException(
                    "MikroTik provisioning failed for voucher " + voucher.getCode() +
                    ": " + e.getMessage(), e);
        }
    }

    /** Converts profile names like "14days", "1 week", "2months" → "14d", "1w", "2m". Falls back to the original value if no pattern matches. */
    private static String abbreviateProfile(String profile) {
        if (profile == null || profile.isBlank()) return profile;
        String p = profile.trim().toLowerCase();
        java.util.regex.Matcher m;
        m = java.util.regex.Pattern.compile("(\\d+)\\s*days?").matcher(p);
        if (m.matches()) return m.group(1) + "d";
        m = java.util.regex.Pattern.compile("(\\d+)\\s*weeks?").matcher(p);
        if (m.matches()) return m.group(1) + "w";
        m = java.util.regex.Pattern.compile("(\\d+)\\s*months?").matcher(p);
        if (m.matches()) return m.group(1) + "m";
        m = java.util.regex.Pattern.compile("(\\d+)\\s*hours?").matcher(p);
        if (m.matches()) return m.group(1) + "h";
        return profile;
    }

    private String buildUniqueCode(String prefix) {
        String code;
        int attempts = 0;
        do {
            if (++attempts > 20) throw new RuntimeException("Could not generate a unique voucher code after 20 attempts");
            code = prefix + randomSuffix();
        } while (repo.existsByCode(code));
        return code;
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(CHARSET.charAt(RNG.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}
