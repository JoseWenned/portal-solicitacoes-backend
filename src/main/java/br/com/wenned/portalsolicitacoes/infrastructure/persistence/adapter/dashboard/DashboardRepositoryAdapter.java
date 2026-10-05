package br.com.wenned.portalsolicitacoes.infrastructure.persistence.adapter.dashboard;

import br.com.wenned.portalsolicitacoes.application.dto.dashboard.DashboardResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.port.out.dashboard.DashboardRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardRepositoryAdapter implements DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResultadoDTO consultarPorProprietario(UUID solicitanteId) {
        Objects.requireNonNull(
            solicitanteId,
            "Solicitante obrigatório."
        );

        return jdbcTemplate.queryForObject("""
            SELECT
                count(*) AS total,
                count(*) FILTER (WHERE status = 'ABERTO') AS abertas,
                count(*) FILTER (
                    WHERE status = 'EM_ATENDIMENTO'
                ) AS em_atendimento,
                count(*) FILTER (WHERE status = 'CONCLUIDO') AS concluidas
            FROM solicitacoes
            WHERE solicitante_id = ?
            """,
            (rs, rowNum) -> new DashboardResultadoDTO(
                rs.getLong("total"),
                rs.getLong("abertas"),
                rs.getLong("em_atendimento"),
                rs.getLong("concluidas")
            ),
            solicitanteId
        );
    }
}