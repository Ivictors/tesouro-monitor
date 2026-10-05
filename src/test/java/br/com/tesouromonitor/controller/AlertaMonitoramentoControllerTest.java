package br.com.tesouromonitor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.domain.AlertaMonitoramento;
import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AlertaMonitoramentoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AlertaMonitoramentoRepository alertaRepository;
    @Autowired
    private AtivoCarteiraRepository ativoRepository;
    @Autowired
    private CotacaoDiariaRepository cotacaoRepository;

    private Long alertaId;

    @BeforeEach
    void prepararAlerta() {
        alertaRepository.deleteAll();
        cotacaoRepository.deleteAll();
        ativoRepository.deleteAll();
        AtivoCarteira ativo = ativoRepository.save(new AtivoCarteira(null,
                "Tesouro Selic - 2031-03-01", LocalDate.of(2026, 1, 10),
                new BigDecimal("0.10"), new BigDecimal("100.00"), new BigDecimal("2.00")));
        CotacaoDiaria cotacao = cotacaoRepository.save(new CotacaoDiaria(null, ativo.getNomeTitulo(),
                LocalDate.of(2026, 10, 2), new BigDecimal("0.10"), new BigDecimal("104.00"),
                new BigDecimal("105.00")));
        alertaId = alertaRepository.save(new AlertaMonitoramento(null, ativo.getId(), cotacao.getId(),
                ativo.getNomeTitulo(), cotacao.getDataCotacao(), new BigDecimal("5.00"),
                "VALORIZACAO_ACIMA_LIMITE", new BigDecimal("5.00"), LocalDateTime.now(), false, null)).getId();
    }

    @Test
    void listaAlertasRecentesComEstadoDeLeitura() throws Exception {
        mockMvc.perform(get("/api/alertas").param("tamanho", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.alertas[0].percentualObservado").value(5.00))
                .andExpect(jsonPath("$.alertas[0].regraAplicada").value("VALORIZACAO_ACIMA_LIMITE"))
                .andExpect(jsonPath("$.alertas[0].lido").value(false));
    }

    @Test
    void marcaAlertaComoLido() throws Exception {
        mockMvc.perform(patch("/api/alertas/{id}/lido", alertaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lido").value(true))
                .andExpect(jsonPath("$.lidoEm").isNotEmpty());
    }
}
