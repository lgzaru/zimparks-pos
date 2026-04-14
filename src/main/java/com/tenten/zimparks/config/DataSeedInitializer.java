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

        // Fix stale enum check constraint for user_permissions table (Hibernate doesn't update it)
        try {
            jdbcTemplate.execute("ALTER TABLE user_permissions DROP CONSTRAINT IF EXISTS user_permissions_permission_check");
            log.info("Ensured user_permissions_permission_check constraint is dropped to allow enum expansion.");
        } catch (Exception e) {
            log.warn("Could not drop user_permissions_permission_check constraint; it might not exist or another error occurred: {}", e.getMessage());
        }

        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (userCount != null && userCount > 0) {
            log.info("Skipping data.sql seed; users table already contains {} row(s)", userCount);
        } else {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("data.sql"));
            DatabasePopulatorUtils.execute(populator, dataSource);
            log.info("Seeded database using data.sql");
        }

        seedDemoQuotations();
    }

    /** Inserts two demo quotations for presentation purposes.
     *  Parent rows use ON CONFLICT (ref) DO NOTHING; items are only inserted
     *  when the parent was freshly created (0 existing items), so restarts are safe. */
    private void seedDemoQuotations() {
        try {
            seedQuotation(
                "QUO-2026-001",
                "Hwange Safari Group",
                285.00,
                "14 Apr 2026",
                "2026-04-30",
                "Group booking confirmed over phone",
                new String[][]{
                    {"PE-ADU",  "Park Entry - Adult (x10)",         "10", "20.00", "200.00"},
                    {"PE-CHI",  "Park Entry - Child (x5)",          "5",  "5.00",  "25.00" },
                    {"VEH-LT",  "Vehicle Entry - Light (x3)",       "3",  "20.00", "60.00" },
                }
            );

            seedQuotation(
                "QUO-2026-002",
                "Victoria Falls Lodge",
                620.00,
                "14 Apr 2026",
                "2026-04-28",
                "Annual corporate package - invoiced quarterly",
                new String[][]{
                    {"CAMP-STD", "Campsite Fee - Standard (x7 nights)",    "7", "30.00", "210.00"},
                    {"ABL-FEE",  "Ablution Facilities Fee (x7 nights)",    "7", "10.00", "70.00" },
                    {"VEH-LT",   "Vehicle Entry - Light (x2)",             "2", "20.00", "40.00" },
                    {"MISC",     "Guided Bush Walk (per person, x6)",      "6", "50.00", "300.00"},
                }
            );

            log.info("Demo quotation seed completed.");
        } catch (Exception e) {
            log.warn("Demo quotation seed skipped or partially failed: {}", e.getMessage());
        }
    }

    private void seedQuotation(String ref, String customerName, double amount,
                                String quotationDate, String expiryDate, String notes,
                                String[][] items) {
        int inserted = jdbcTemplate.update("""
            INSERT INTO quotations (ref, customer_name, customer_id, amount, currency, status,
                quotation_date, expiry_date, notes, station_id, converted_txn_ref)
            VALUES (?, ?, NULL, ?, 'USD', 'ACTIVE', ?, CAST(? AS date), ?, NULL, NULL)
            ON CONFLICT (ref) DO NOTHING
        """, ref, customerName, amount, quotationDate, expiryDate, notes);

        if (inserted == 0) {
            log.debug("Quotation {} already exists — skipping items.", ref);
            return;
        }

        for (String[] item : items) {
            jdbcTemplate.update("""
                INSERT INTO quotation_items
                    (quotation_ref, product_code, descr, hs_code, quantity, unit_price, total_price)
                VALUES (?, ?, ?, '99001000', ?, ?, ?)
            """, ref, item[0], item[1],
                Integer.parseInt(item[2]),
                new java.math.BigDecimal(item[3]),
                new java.math.BigDecimal(item[4]));
        }
        log.info("Seeded quotation {} with {} items.", ref, items.length);
    }
}
