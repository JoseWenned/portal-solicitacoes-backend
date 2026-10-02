package br.com.wenned.portalsolicitacoes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PortalSolicitacoesApplicationTests {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarMigrationsEmBancoVazio() {
        Long migrations = jdbcTemplate.queryForObject("""
            SELECT count(*)
            FROM flyway_schema_history
            WHERE success = true AND version IN ('1', '2', '3')
            """, Long.class);

        assertThat(migrations).isEqualTo(3L);

        assertThat(tableExists("usuarios")).isTrue();
        assertThat(tableExists("solicitacoes")).isTrue();
        assertThat(tableExists("sessoes_autenticacao")).isTrue();
    }

    private boolean tableExists(String tableName) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT EXISTS (
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = 'public'
                    AND table_name = ?
            )
            """, Boolean.class, tableName));
    }
}