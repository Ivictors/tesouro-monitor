package br.com.tesouromonitor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
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
class CotacaoDiariaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AtivoCarteiraRepository ativoRepository;

    @Autowired
    private CotacaoDiariaRepository cotacaoRepository;

    private Long ativoId;

    @BeforeEach
    void prepararAtivo() {
        cotacaoRepository.deleteAll();
        ativoRepository.deleteAll();
        AtivoCarteira ativo = ativoRepository.save(new AtivoCarteira(null,
                "Tesouro Selic - 2031-03-01", LocalDate.of(2026, 1, 10),
                new BigDecimal("0.1"), new BigDecimal("19943.12"), new BigDecimal("0.25")));
        ativoId = ativo.getId();
    }

    @Test
    void listaHistoricoPaginadoDoAtivo() throws Exception {
        saveQuote(LocalDate.of(2026, 10, 1), "19910.00");
        saveQuote(LocalDate.of(2026, 10, 2), "19924.17");

        mockMvc.perform(get("/api/ativos/{id}/cotacoes", ativoId)
                        .param("pagina", "0")
                        .param("tamanho", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.cotacoes.length()").value(1))
                .andExpect(jsonPath("$.cotacoes[0].dataCotacao").value("2026-10-02"));
    }

    @Test
    void historicoSemCotacoesRetornaListaVazia() throws Exception {
        mockMvc.perform(get("/api/ativos/{id}/cotacoes", ativoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.cotacoes").isEmpty());
    }

    @Test
    void cotacaoAtualAusenteRetornaErroExplicito() throws Exception {
        mockMvc.perform(get("/api/ativos/{id}/cotacoes/atual", ativoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Cotação indisponível"));
    }

    @Test
    void rejeitaTamanhoDePaginaAcimaDoLimite() throws Exception {
        mockMvc.perform(get("/api/ativos/{id}/cotacoes", ativoId).param("tamanho", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    private void saveQuote(LocalDate date, String price) {
        cotacaoRepository.save(new CotacaoDiaria(null, "Tesouro Selic - 2031-03-01", date,
                new BigDecimal("0.10"), new BigDecimal("19943.12"), new BigDecimal(price)));
    }
}
