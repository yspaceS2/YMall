package com.ymall.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("postgres")
@Testcontainers
class FlywayPostgresMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    )
        .withDatabaseName("ymall_migration_test")
        .withUsername("ymall_test")
        .withPassword("ymall_test");

    @Test
    void migratesEmptyPostgresFromLatestBaseline() throws SQLException {
        String url = POSTGRES.getJdbcUrl();
        String username = POSTGRES.getUsername();
        String password = POSTGRES.getPassword();

        assertDatabaseIsEmpty(url, username, password);

        Flyway flyway = Flyway.configure()
            .dataSource(url, username, password)
            .locations("classpath:db/migration")
            .load();
        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(10);
        flyway.validate();

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE"
            )).isEqualTo(10);
            assertThat(queryForString(
                connection,
                "SELECT version || ':' || type || ':' || success "
                    + "FROM flyway_schema_history ORDER BY installed_rank"
            )).isEqualTo("32:SQL_BASELINE:true");
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM information_schema.tables "
                    + "WHERE table_schema = 'public' "
                    + "AND table_type = 'BASE TABLE' "
                    + "AND table_name <> 'flyway_schema_history'"
            )).isGreaterThanOrEqualTo(32);
            assertThat(queryForInt(
                connection,
                "SELECT character_maximum_length FROM information_schema.columns "
                    + "WHERE table_schema = 'public' "
                    + "AND table_name = 'order_items' "
                    + "AND column_name = 'tracking_number'"
            )).isEqualTo(100);
            assertThat(queryForString(
                connection,
                "SELECT is_nullable FROM information_schema.columns "
                    + "WHERE table_schema = 'public' "
                    + "AND table_name = 'order_items' "
                    + "AND column_name = 'fulfillment_status'"
            )).isEqualTo("NO");
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema = 'public' "
                    + "AND table_name = 'products' "
                    + "AND column_name = 'approved_at'"
            )).isEqualTo(1);
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema = 'public' "
                    + "AND table_name = 'products' "
                    + "AND column_name IN ('search_normalized_name', 'search_chosung') "
                    + "AND is_nullable = 'NO'"
            )).isEqualTo(2);
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'pg_trgm'"
            )).isEqualTo(1);
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM pg_indexes "
                    + "WHERE schemaname = 'public' "
                    + "AND indexname IN ("
                    + "'idx_products_search_normalized_name_trgm', "
                    + "'idx_products_search_chosung_trgm')"
            )).isEqualTo(2);
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema = 'public' "
                    + "AND table_name = 'members' "
                    + "AND column_name IN ("
                    + "'access_status', 'last_login_at', 'restriction_reason', "
                    + "'restricted_at', 'restricted_by')"
            )).isEqualTo(5);
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM pg_indexes "
                    + "WHERE schemaname = 'public' "
                    + "AND indexname = 'idx_members_admin_operations'"
            )).isEqualTo(1);
            assertThat(queryForString(
                connection,
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                    + "WHERE conname = 'seller_applications_status_check'"
            )).contains("NEEDS_REVISION");
        }
    }

    private void assertDatabaseIsEmpty(
        String url,
        String username,
        String password
    ) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM information_schema.tables "
                    + "WHERE table_schema = 'public'"
            )).as("Migration integration test requires a dedicated empty database")
                .isZero();
        }
    }

    private int queryForInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }

    private String queryForString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }
}
