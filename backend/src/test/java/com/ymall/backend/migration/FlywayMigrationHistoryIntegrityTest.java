package com.ymall.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.CRC32;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywayMigrationHistoryIntegrityTest {

    private static final Map<String, Integer> APPLIED_MIGRATION_CHECKSUMS = Map.of(
        "db/migration/V20__add_order_delivery_operations.sql", -85917804,
        "db/migration/V24__create_product_return_requests.sql", -2073083079,
        "db/migration/V25__expand_refund_status_constraints.sql", -1795236840,
        "db/migration/V27__expand_notification_type_constraint.sql", -2076353839
    );

    @Test
    void appliedMigrationChecksumsRemainUnchanged() throws IOException {
        for (Map.Entry<String, Integer> migration : APPLIED_MIGRATION_CHECKSUMS.entrySet()) {
            assertThat(calculateFlywayChecksum(migration.getKey()))
                .as(migration.getKey())
                .isEqualTo(migration.getValue());
        }
    }

    private int calculateFlywayChecksum(String path) throws IOException {
        CRC32 crc32 = new CRC32();
        ClassPathResource resource = new ClassPathResource(path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            resource.getInputStream(),
            StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String normalizedLine = line.startsWith("\uFEFF") ? line.substring(1) : line;
                crc32.update(normalizedLine.getBytes(StandardCharsets.UTF_8));
            }
        }

        return (int) crc32.getValue();
    }
}
