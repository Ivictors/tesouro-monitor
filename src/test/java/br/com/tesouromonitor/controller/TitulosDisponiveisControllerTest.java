package br.com.tesouromonitor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TitulosDisponiveisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CotacaoDiariaRepository cotacaoRepository;

    @BeforeEach
    void limparCotacoes() {
        cotacaoRepository.deleteAll();
    }

    @Test
    @DisplayName("AC-21-01 T1-T1: lista nomes canônicos distintos apenas da última data-base")
    void listaTitulosDisponiveisNaUltimaDataBase() throws Exception {
        LocalDate dataAntiga = LocalDate.of(2026, 11, 10);
        LocalDate dataMaisRecente = LocalDate.of(2026, 11, 11);
        salvar("Tesouro Selic - 2031-03-01", dataAntiga);
        salvar("Tesouro IPCA+ - 2035-05-15", dataMaisRecente);
        salvar("Tesouro Selic - 2031-03-01", dataMaisRecente);

        mockMvc.perform(get("/api/titulos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeTitulo").value("Tesouro IPCA+ - 2035-05-15"))
                .andExpect(jsonPath("$[1].nomeTitulo").value("Tesouro Selic - 2031-03-01"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("AC-21-01 T1-T2: retorna lista vazia quando não há cotações")
    void retornaListaVaziaSemCotacoes() throws Exception {
        mockMvc.perform(get("/api/titulos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private void salvar(String nome, LocalDate dataBase) {
        cotacaoRepository.save(new CotacaoDiaria(null, nome, dataBase,
                new BigDecimal("0.10"), new BigDecimal("100.00"), new BigDecimal("101.00")));
    }
}
