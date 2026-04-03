package com.tenten.zimparks.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve index.html at the root
        registry.addResourceHandler("/")
                .addResourceLocations("classpath:/static/index.html")
                .setCachePeriod(3600);

        // Serve everything inside static/static/**
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/static/")
                .setCachePeriod(3600);

        // Serve root level files
        registry.addResourceHandler("/*.ico", "/*.json", "/*.png", "/*.jpg", "/*.svg", "/*.txt", "/*.html")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward non-API, non-static paths to index.html for React Router
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}