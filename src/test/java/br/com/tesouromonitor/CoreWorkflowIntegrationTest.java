package br.com.tesouromonitor;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.dto.TituloDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import br.com.tesouromonitor.scheduler.TesouroSyncScheduler;
import br.com.tesouromonitor.service.TesouroDiretoClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

@SpringBootTest
@AutoConfigureMockMvc
class CoreWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AtivoCarteiraRepository ativoRepository;

    @Autowired
    private CotacaoDiariaRepository cotacaoRepository;

    @Autowired
    private AlertaMonitoramentoRepository alertaRepository;

    @Autowired
    private TesouroSyncScheduler scheduler;

    @MockitoBean
    private TesouroDiretoClient tesouroDiretoClient;

    @BeforeEach
    void limparDados() {
        alertaRepository.deleteAll();
        cotacaoRepository.deleteAll();
        ativoRepository.deleteAll();
    }

    @Test
    void sincronizacaoManualImportaDadosERetornaEstado() throws Exception {
        TituloDTO title = new TituloDTO("Tesouro Selic - 2031-03-01", LocalDate.of(2031, 3, 1),
                LocalDate.of(2026, 10, 2), new BigDecimal("0.09"), new BigDecimal("0.10"),
                new BigDecimal("19943.12"), new BigDecimal("19924.17"), new BigDecimal("19924.17"));
        when(tesouroDiretoClient.buscarTitulos()).thenReturn(Mono.just(new TesouroResponseDTO(List.of(title))));

        mockMvc.perform(post("/api/sincronizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emAndamento").value(false))
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.cotacoesPersistidas").value(1));

        mockMvc.perform(get("/api/sincronizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Sincronização concluída com sucesso."));
    }

    @Test
    void cadastraAtivoSincronizaCotacaoEConsultaValorAtual() throws Exception {
        String request = """
                {"nomeTitulo":"Tesouro Selic - 2031-03-01","dataCompra":"2026-01-10",
                "taxaContratada":0.1,"precoCompra":19943.12,"quantidade":0.25}
                """;
        String location = mockMvc.perform(post("/api/ativos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        long ativoId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        TituloDTO title = new TituloDTO("Tesouro Selic", LocalDate.of(2031, 3, 1),
                LocalDate.of(2026, 10, 2), new BigDecimal("0.09"), new BigDecimal("0.10"),
                new BigDecimal("19943.12"), new BigDecimal("19924.17"), new BigDecimal("19924.17"));
        when(tesouroDiretoClient.buscarTitulos()).thenReturn(Mono.just(new TesouroResponseDTO(List.of(title))));

        scheduler.sincronizarCotacoes();

        mockMvc.perform(get("/api/ativos/{id}/cotacoes/atual", ativoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeAtivo").value(title.nomeAtivo()))
                .andExpect(jsonPath("$.dataCotacao").value("2026-10-02"))
                .andExpect(jsonPath("$.precoResgateAtual").value(19924.17));

        mockMvc.perform(get("/api/ativos/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvestido").value(4985.78))
                .andExpect(jsonPath("$.valorAtualEstimado").value(4981.04))
                .andExpect(jsonPath("$.variacaoPercentual").value(-0.10))
                .andExpect(jsonPath("$.posicoes[0].puVenda").value(19924.17));

        mockMvc.perform(get("/api/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void geraAlertaNoCruzamentoDoLimiteEConsultaNaApi() throws Exception {
        String request = """
                {"nomeTitulo":"Tesouro Selic - 2031-03-01","dataCompra":"2026-01-10",
                "taxaContratada":0.1,"precoCompra":100.00,"quantidade":2.00}
                """;
        mockMvc.perform(post("/api/ativos").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());

        TituloDTO title = new TituloDTO("Tesouro Selic", LocalDate.of(2031, 3, 1),
                LocalDate.of(2026, 10, 2), new BigDecimal("0.09"), new BigDecimal("0.10"),
                new BigDecimal("104.00"), new BigDecimal("105.00"), new BigDecimal("104.00"));
        when(tesouroDiretoClient.buscarTitulos()).thenReturn(Mono.just(new TesouroResponseDTO(List.of(title))));

        scheduler.sincronizarCotacoes();
        scheduler.sincronizarCotacoes();

        mockMvc.perform(get("/api/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.alertas[0].percentualObservado").value(5.00))
                .andExpect(jsonPath("$.alertas[0].regraAplicada").value("VALORIZACAO_ACIMA_LIMITE"))
                .andExpect(jsonPath("$.alertas[0].lido").value(false));
    }
}
