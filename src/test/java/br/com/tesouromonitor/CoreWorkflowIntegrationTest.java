package br.com.tesouromonitor;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.dto.TituloDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
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
    private TesouroSyncScheduler scheduler;

    @MockitoBean
    private TesouroDiretoClient tesouroDiretoClient;

    @BeforeEach
    void limparDados() {
        cotacaoRepository.deleteAll();
        ativoRepository.deleteAll();
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
    }
}
