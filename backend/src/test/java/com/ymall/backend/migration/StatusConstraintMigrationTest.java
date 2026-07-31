package com.ymall.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OutboxEventStatus;
import com.ymall.backend.order.entity.OrderStatus;

class StatusConstraintMigrationTest {

    private static final String MIGRATION =
        "db/migration/V29__align_order_payment_outbox_status_constraints.sql";

    @Test
    void migrationContainsEveryCurrentStatusValue() throws IOException {
        String sql = new ClassPathResource(MIGRATION)
            .getContentAsString(StandardCharsets.UTF_8);

        assertContainsAll(sql, OrderStatus.values());
        assertContainsAll(sql, OrderEventType.values());
        assertContainsAll(sql, OutboxEventStatus.values());
        assertThat(sql).contains(
            "DROP CONSTRAINT IF EXISTS orders_status_check",
            "DROP CONSTRAINT IF EXISTS payments_order_status_check",
            "DROP CONSTRAINT IF EXISTS order_outbox_events_event_type_check",
            "DROP CONSTRAINT IF EXISTS order_outbox_events_status_check"
        );
    }

    private void assertContainsAll(String sql, Enum<?>[] values) {
        for (Enum<?> value : values) {
            assertThat(sql).contains("'" + value.name() + "'");
        }
    }
}
