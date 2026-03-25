package com.tenten.zimparks.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve everything inside build/static/**
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/static/")
                .setCachePeriod(3600);

        // Serve root level files
        registry.addResourceHandler("/*.ico", "/*.json", "/*.png", "/*.jpg", "/*.svg", "/*.txt")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}