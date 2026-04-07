package com.tenten.zimparks.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Static resources are served automatically by Spring Boot from classpath:/static/.
 * SPA routing is handled by SpaController.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
