package com.ymall.backend.testsupport;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("postgres")
public abstract class PostgresIntegrationTestSupport {

    private static final PostgreSQLContainer<?> POSTGRES = createAndStartContainer();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    protected static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @BeforeEach
    protected void cleanPostgresDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
            """
            SELECT quote_ident(tablename)
            FROM pg_tables
            WHERE schemaname = 'public'
              AND tablename <> 'flyway_schema_history'
            ORDER BY tablename
            """,
            String.class
        );
        if (!tables.isEmpty()) {
            jdbcTemplate.execute(
                "TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE"
            );
        }
    }

    private static PostgreSQLContainer<?> createAndStartContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
        )
            .withDatabaseName("ymall_test")
            .withUsername("ymall_test")
            .withPassword("ymall_test");
        container.start();
        return container;
    }
}
