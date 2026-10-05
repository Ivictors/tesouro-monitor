package br.com.tesouromonitor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCarteiraControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AtivoCarteiraRepository ativoRepository;
    @Autowired
    private CotacaoDiariaRepository cotacaoRepository;
    @Autowired
    private AlertaMonitoramentoRepository alertaRepository;

    private AtivoCarteira ativo;

    @BeforeEach
    void prepararCarteira() {
        alertaRepository.deleteAll();
        cotacaoRepository.deleteAll();
        ativoRepository.deleteAll();
        ativo = ativoRepository.save(new AtivoCarteira(null, "Tesouro Selic - 2031-03-01",
                LocalDate.of(2026, 1, 10), new BigDecimal("0.10"),
                new BigDecimal("100.00"), new BigDecimal("2.00")));
    }

    @Test
    void calculaResumoDaPosicaoUsandoPuVenda() throws Exception {
        cotacaoRepository.save(new CotacaoDiaria(null, ativo.getNomeTitulo(), LocalDate.of(2026, 10, 2),
                new BigDecimal("0.10"), new BigDecimal("104.00"), new BigDecimal("105.00")));

        mockMvc.perform(get("/api/ativos/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvestido").value(200.00))
                .andExpect(jsonPath("$.valorAtualEstimado").value(210.00))
                .andExpect(jsonPath("$.variacaoValor").value(10.00))
                .andExpect(jsonPath("$.variacaoPercentual").value(5.00))
                .andExpect(jsonPath("$.posicoes[0].puVenda").value(105.00));
    }

    @Test
    void informaCotacaoAusenteSemInventarTotalAtual() throws Exception {
        mockMvc.perform(get("/api/ativos/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvestido").value(200.00))
                .andExpect(jsonPath("$.valorAtualEstimado").doesNotExist())
                .andExpect(jsonPath("$.cotacoesIndisponiveis").value(1))
                .andExpect(jsonPath("$.posicoes[0].variacaoPercentual").doesNotExist());
    }
}
