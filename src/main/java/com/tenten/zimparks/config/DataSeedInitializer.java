package com.tenten.zimparks.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeedInitializer {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfNeeded() {
        if (!seedEnabled) {
            log.info("Seed initialization disabled by app.seed.enabled=false");
            return;
        }

        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (userCount != null && userCount > 0) {
            log.info("Skipping data.sql seed; users table already contains {} row(s)", userCount);
            return;
        }

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("data.sql"));
        DatabasePopulatorUtils.execute(populator, dataSource);
        log.info("Seeded database using data.sql");
    }
}
