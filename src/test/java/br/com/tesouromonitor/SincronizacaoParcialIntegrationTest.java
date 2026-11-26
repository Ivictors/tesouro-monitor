package br.com.tesouromonitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.dto.TituloDTO;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import br.com.tesouromonitor.service.MonitoramentoService;
import br.com.tesouromonitor.service.TesouroDiretoClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

@SpringBootTest
@AutoConfigureMockMvc
class SincronizacaoParcialIntegrationTest {

    private static final String NOME_TITULO = "Tesouro Selic - 2031-03-01";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AtivoCarteiraRepository ativoRepository;
    @Autowired
    private CotacaoDiariaRepository cotacaoRepository;
    @Autowired
    private AlertaMonitoramentoRepository alertaRepository;
    @MockitoBean
    private TesouroDiretoClient tesouroDiretoClient;
    @MockitoBean
    private MonitoramentoService monitoramentoService;

    @BeforeEach
    void prepararDados() {
        alertaRepository.deleteAll();
        cotacaoRepository.deleteAll();
        ativoRepository.deleteAll();
    }

    @Test
    void informaFalhaParcialNoStatusGetEPermiteNovaTentativa() throws Exception {
        ativoRepository.saveAll(List.of(
                criarAtivo(new BigDecimal("100.00")),
                criarAtivo(new BigDecimal("101.00"))));
        TituloDTO titulo = new TituloDTO("Tesouro Selic", LocalDate.of(2031, 3, 1),
                LocalDate.of(2026, 10, 2), new BigDecimal("0.09"), new BigDecimal("0.10"),
                new BigDecimal("104.00"), new BigDecimal("105.00"), new BigDecimal("104.00"));
        when(tesouroDiretoClient.buscarTitulos()).thenReturn(Mono.just(new TesouroResponseDTO(List.of(titulo))));
        when(monitoramentoService.avaliarAgio(any(AtivoCarteira.class), any()))
                .thenThrow(new IllegalStateException("Falha simulada no primeiro lote."))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/sincronizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.cotacoesPersistidas").value(1))
                .andExpect(jsonPath("$.posicoesCorrespondentes").value(2))
                .andExpect(jsonPath("$.avaliacoesFalhas").value(1))
                .andExpect(jsonPath("$.monitoramentoCompleto").value(false))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("incompleto")));
        assertEquals(1, cotacaoRepository.count());
        verify(monitoramentoService, times(2)).avaliarAgio(any(AtivoCarteira.class), any());

        mockMvc.perform(get("/api/sincronizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.cotacoesPersistidas").value(1))
                .andExpect(jsonPath("$.avaliacoesFalhas").value(1))
                .andExpect(jsonPath("$.monitoramentoCompleto").value(false));

        mockMvc.perform(post("/api/sincronizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotacoesPersistidas").value(1))
                .andExpect(jsonPath("$.avaliacoesFalhas").value(0))
                .andExpect(jsonPath("$.monitoramentoCompleto").value(true));
        mockMvc.perform(get("/api/sincronizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitoramentoCompleto").value(true))
                .andExpect(jsonPath("$.avaliacoesFalhas").value(0));
    }

    private AtivoCarteira criarAtivo(BigDecimal precoCompra) {
        return new AtivoCarteira(null, NOME_TITULO, LocalDate.of(2026, 1, 10),
                new BigDecimal("0.10"), precoCompra, BigDecimal.ONE);
    }
}
