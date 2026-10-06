package br.com.tesouromonitor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "spring.datasource.url=jdbc:h2:mem:tesouro-csv-workflow;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class TesouroCsvWorkflowIntegrationTest {

    private static final String CSV_VALIDO = """
            Tipo Titulo;Data Vencimento;Data Base;Taxa Compra Manha;Taxa Venda Manha;PU Compra Manha;PU Venda Manha;PU Base Manha
            Tesouro Selic;01/03/2031;02/10/2026;0,09;0,10;104,00;105,00;104,00
            """;
    private static final AtomicReference<String> corpoCsv = new AtomicReference<>(CSV_VALIDO);
    private static final AtomicInteger statusHttp = new AtomicInteger(200);
    private static final AtomicInteger requisicoes = new AtomicInteger();
    private static final HttpServer servidor = iniciarServidor();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AtivoCarteiraRepository ativoRepository;
    @Autowired
    private CotacaoDiariaRepository cotacaoRepository;
    @Autowired
    private AlertaMonitoramentoRepository alertaRepository;

    @DynamicPropertySource
    static void propriedadesFonteCsv(DynamicPropertyRegistry registry) {
        registry.add("tesouro.api.base-url", () -> "http://127.0.0.1:" + servidor.getAddress().getPort());
        registry.add("tesouro.api.titulos-path", () -> "/cotacoes.csv");
    }

    @AfterAll
    static void pararServidor() {
        servidor.stop(0);
    }

    @BeforeEach
    void limparBancoEPrepararFonte() {
        alertaRepository.deleteAll();
        cotacaoRepository.deleteAll();
        ativoRepository.deleteAll();
        corpoCsv.set(CSV_VALIDO);
        statusHttp.set(200);
        requisicoes.set(0);
    }

    @Test
    void percorreCadastroImportacaoIdempotenciaCalculoAlertaEConsultasRest() throws Exception {
        cadastrarAtivo();

        sincronizarComSucesso();
        sincronizarComSucesso();

        mockMvc.perform(get("/api/ativos/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvestido").value(200.00))
                .andExpect(jsonPath("$.valorAtualEstimado").value(210.00))
                .andExpect(jsonPath("$.variacaoPercentual").value(5.00));
        mockMvc.perform(get("/api/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.alertas[0].percentualObservado").value(5.00));

        org.assertj.core.api.Assertions.assertThat(cotacaoRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(alertaRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(requisicoes.get()).isEqualTo(2);
    }

    @Test
    void falhaDaFonteNaoExibeCorpoSensivelNemApagaCotacaoAnterior() throws Exception {
        cadastrarAtivo();
        sincronizarComSucesso();
        statusHttp.set(503);
        corpoCsv.set("conteudo confidencial retornado pelo servidor");

        mockMvc.perform(post("/api/sincronizacao"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("Verifique a conexão")))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("confidencial"))));

        org.assertj.core.api.Assertions.assertThat(cotacaoRepository.count()).isEqualTo(1);
        mockMvc.perform(get("/api/ativos/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorAtualEstimado").value(210.00));
    }

    private void cadastrarAtivo() throws Exception {
        mockMvc.perform(post("/api/ativos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeTitulo":"Tesouro Selic - 2031-03-01","dataCompra":"2026-01-10",
                                "taxaContratada":0.1,"precoCompra":100.00,"quantidade":2.00}
                                """))
                .andExpect(status().isCreated());
    }

    private void sincronizarComSucesso() throws Exception {
        mockMvc.perform(post("/api/sincronizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.cotacoesPersistidas").value(1))
                .andExpect(jsonPath("$.posicoesCorrespondentes").value(1));
    }

    private static HttpServer iniciarServidor() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/cotacoes.csv", exchange -> {
                requisicoes.incrementAndGet();
                byte[] corpo = corpoCsv.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                exchange.sendResponseHeaders(statusHttp.get(), corpo.length);
                exchange.getResponseBody().write(corpo);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
