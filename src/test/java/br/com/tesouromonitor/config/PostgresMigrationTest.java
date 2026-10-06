package br.com.tesouromonitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.tesouromonitor.domain.AlertaMonitoramento;
import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PostgresMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AtivoCarteiraRepository repository;
    @Autowired
    private CotacaoDiariaRepository cotacaoRepository;
    @Autowired
    private AlertaMonitoramentoRepository alertaRepository;
    @Autowired
    private Flyway flyway;

    @BeforeEach
    void limparBanco() {
        alertaRepository.deleteAll();
        cotacaoRepository.deleteAll();
        repository.deleteAll();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void executaMigracoesEInicializaRepositorioEmBancoVazio() {
        assertThat(repository.count()).isZero();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    @Test
    void preservaDadosAoRevalidarMigracoesEFazBackupERestauracao() throws Exception {
        AtivoCarteira ativo = repository.save(new AtivoCarteira(null, "Tesouro Selic - 2031-03-01",
                LocalDate.of(2026, 1, 10), new BigDecimal("0.10"),
                new BigDecimal("100.00"), new BigDecimal("2.00")));
        CotacaoDiaria cotacao = cotacaoRepository.save(new CotacaoDiaria(null, ativo.getNomeTitulo(),
                LocalDate.of(2026, 10, 2), new BigDecimal("0.10"),
                new BigDecimal("104.00"), new BigDecimal("105.00")));
        alertaRepository.save(new AlertaMonitoramento(null, ativo.getId(), cotacao.getId(),
                ativo.getNomeTitulo(), cotacao.getDataCotacao(), new BigDecimal("5.00"),
                "VALORIZACAO_ACIMA_LIMITE", new BigDecimal("5.00"), LocalDateTime.now(), false, null));

        flyway.migrate();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(cotacaoRepository.count()).isEqualTo(1);
        assertThat(alertaRepository.count()).isEqualTo(1);

        String restoredDatabase = "tesouro_restore_test";
        assertCommandSucceeded(POSTGRES.execInContainer("createdb", "-U", POSTGRES.getUsername(),
                restoredDatabase));
        assertCommandSucceeded(POSTGRES.execInContainer("pg_dump", "-U", POSTGRES.getUsername(), "-d",
                POSTGRES.getDatabaseName(), "-Fc", "-f", "/tmp/tesouro-monitor-test.dump"));
        assertCommandSucceeded(POSTGRES.execInContainer("pg_restore", "-U", POSTGRES.getUsername(), "-d",
                restoredDatabase, "--no-owner", "--no-privileges", "/tmp/tesouro-monitor-test.dump"));
        assertThat(queryCount(restoredDatabase, "ativos_carteira")).isEqualTo("1");
        assertThat(queryCount(restoredDatabase, "cotacoes_diarias")).isEqualTo("1");
        assertThat(queryCount(restoredDatabase, "alertas_monitoramento")).isEqualTo("1");
    }

    private String queryCount(String database, String table) throws Exception {
        var result = POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", database,
                "-tA", "-c", "SELECT COUNT(*) FROM " + table);
        assertCommandSucceeded(result);
        return result.getStdout().trim();
    }

    private void assertCommandSucceeded(org.testcontainers.containers.Container.ExecResult result) {
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
    }
}
