package com.ymall.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "MIGRATION_TEST_DATABASE_URL", matches = ".+")
class FlywayPostgresMigrationIntegrationTest {

    @Test
    void migratesEmptyPostgresFromLatestBaseline() throws SQLException {
        String url = System.getenv("MIGRATION_TEST_DATABASE_URL");
        String username = System.getenv("MIGRATION_TEST_DATABASE_USERNAME");
        String password = System.getenv().getOrDefault(
            "MIGRATION_TEST_DATABASE_PASSWORD",
            ""
        );

        assertDatabaseIsEmpty(url, username, password);

        Flyway flyway = Flyway.configure()
            .dataSource(url, username, password)
            .locations("classpath:db/migration")
            .load();
        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(2);
        flyway.validate();

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            assertThat(queryForInt(
                connection,
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE"
            )).isEqualTo(2);
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
