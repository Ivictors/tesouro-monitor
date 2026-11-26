package br.com.tesouromonitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.tesouromonitor.TesouroMonitorApplication;
import br.com.tesouromonitor.domain.AlertaMonitoramento;
import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
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
    @Autowired
    private EntityManager entityManager;

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
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
    }

    @Test
    @Transactional
    void preservaEscalasDeQuatroCasasAposFlushEClearNoPostgres() {
        AtivoCarteira saved = repository.save(new AtivoCarteira(null, "Tesouro Selic - 2031-03-01",
                LocalDate.of(2026, 1, 10), new BigDecimal("0.0001"),
                new BigDecimal("100.00"), new BigDecimal("0.1234")));
        entityManager.flush();
        Long id = saved.getId();
        entityManager.clear();

        AtivoCarteira reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getTaxaContratada()).isEqualByComparingTo("0.0001");
        assertThat(reloaded.getQuantidade()).isEqualByComparingTo("0.1234");
        assertThat(reloaded.getPrecoCompra()).isEqualByComparingTo("100.00");
    }

    @Test
    void atualizaBancoV2SemPerderAtivoCotacaoOuAlerta() throws Exception {
        String schema = "tesouro_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        Flyway v2 = flywayForSchema(schema).target("2").load();
        v2.migrate();

        try {
            try (var connection = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            INSERT INTO ativos_carteira (id, nome_titulo, data_compra, taxa_contratada,
                                preco_compra, quantidade)
                            VALUES (7, 'Tesouro Selic - 2031-03-01', DATE '2026-01-10', 0.10, 100.00, 0.25)
                            """);
                    statement.executeUpdate("""
                            INSERT INTO cotacoes_diarias (id, nome_ativo, data_cotacao, taxa_atual,
                                preco_compra_atual, preco_resgate_atual)
                            VALUES (8, 'Tesouro Selic - 2031-03-01', DATE '2026-10-02', 0.10, 104.00, 105.00)
                            """);
                    statement.executeUpdate("""
                            INSERT INTO alertas_monitoramento (id, ativo_id, cotacao_id, nome_ativo,
                                data_cotacao, percentual_observado, regra_aplicada, limite_percentual,
                                criado_em, lido)
                            VALUES (9, 7, 8, 'Tesouro Selic - 2031-03-01', DATE '2026-10-02',
                                5.00, 'VALORIZACAO_ACIMA_LIMITE', 5.00, TIMESTAMP '2026-10-02 10:00:00', FALSE)
                            """);
                }
            }

            Flyway latest = flywayForSchema(schema).load();
            latest.migrate();

            try (var connection = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    assertThat(count(statement, "ativos_carteira")).isEqualTo(1);
                    assertThat(count(statement, "cotacoes_diarias")).isEqualTo(1);
                    assertThat(count(statement, "alertas_monitoramento")).isEqualTo(1);
                    try (var result = statement.executeQuery("""
                            SELECT a.quantidade, al.ativo_id, al.cotacao_id
                            FROM ativos_carteira a
                            JOIN alertas_monitoramento al ON al.ativo_id = a.id
                            JOIN cotacoes_diarias c ON c.id = al.cotacao_id
                            WHERE a.id = 7 AND c.id = 8
                            """)) {
                        assertThat(result.next()).isTrue();
                        assertThat(result.getBigDecimal("quantidade")).isEqualByComparingTo("0.25");
                        assertThat(result.getLong("ativo_id")).isEqualTo(7);
                        assertThat(result.getLong("cotacao_id")).isEqualTo(8);
                    }
                }
            }
        } finally {
            flywayForSchema(schema).load().clean();
        }
    }

    private FluentConfiguration flywayForSchema(String schema) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .cleanDisabled(false)
                .locations("classpath:db/migration");
    }

    private int count(java.sql.Statement statement, String table) throws Exception {
        try (var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    @Test
    void preservaDadosAoRevalidarMigracoesEFazBackupERestauracao() throws Exception {
        AtivoCarteira ativo = repository.save(new AtivoCarteira(null, "Tesouro Selic - 2031-03-01",
                LocalDate.of(2026, 1, 10), new BigDecimal("0.1234"),
                new BigDecimal("100.00"), new BigDecimal("2.1234")));
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
        verificarDadosAposReiniciarAplicacao(ativo.getId());

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

    private void verificarDadosAposReiniciarAplicacao(Long ativoId) {
        for (int reinicializacao = 0; reinicializacao < 2; reinicializacao++) {
            try (ConfigurableApplicationContext application = new SpringApplicationBuilder(
                    TesouroMonitorApplication.class)
                    .web(WebApplicationType.NONE)
                    .run("--spring.profiles.active=prod",
                            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                            "--spring.datasource.username=" + POSTGRES.getUsername(),
                            "--spring.datasource.password=" + POSTGRES.getPassword(),
                            "--spring.datasource.driver-class-name=org.postgresql.Driver",
                            "--spring.jpa.hibernate.ddl-auto=validate",
                            "--spring.flyway.enabled=true")) {
                AtivoCarteiraRepository repositoryAposReinicio =
                        application.getBean(AtivoCarteiraRepository.class);
                AtivoCarteira ativoAposReinicio = repositoryAposReinicio.findById(ativoId).orElseThrow();
                assertThat(ativoAposReinicio.getTaxaContratada()).isEqualByComparingTo("0.1234");
                assertThat(ativoAposReinicio.getQuantidade()).isEqualByComparingTo("2.1234");
                assertThat(ativoAposReinicio.getPrecoCompra()).isEqualByComparingTo("100.00");
            }
        }
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
