package com.tenten.zimparks.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * Serves index.html for all React Router paths directly (avoids forward dispatch issues).
 * Excludes: file extensions, api, static, actuator, swagger-ui, v3 prefixes.
 */
@Controller
public class SpaController {

    private final byte[] indexHtml;

    public SpaController() throws IOException {
        this.indexHtml = FileCopyUtils.copyToByteArray(
                new ClassPathResource("static/index.html").getInputStream());
    }

    @GetMapping(value = {
            "/",
            "/{path:^(?!api|static|actuator|swagger-ui|v3)[^\\.]+}",
            "/{path:^(?!api|static|actuator|swagger-ui|v3)[^\\.]+}/**"
    })
    @ResponseBody
    public ResponseEntity<byte[]> serveIndex() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }
}
