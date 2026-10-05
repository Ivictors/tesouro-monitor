package br.com.tesouromonitor.scheduler;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.dto.TituloDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.service.CotacaoDiariaService;
import br.com.tesouromonitor.service.MonitoramentoService;
import br.com.tesouromonitor.service.TesouroDiretoClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TesouroSyncSchedulerTest {

    @Mock
    private TesouroDiretoClient tesouroDiretoClient;
    @Mock
    private MonitoramentoService monitoramentoService;
    @Mock
    private CotacaoDiariaService cotacaoDiariaService;
    @Mock
    private AtivoCarteiraRepository ativoCarteiraRepository;
    @InjectMocks
    private TesouroSyncScheduler scheduler;

    private TituloDTO title;
    private AtivoCarteira holding;
    private CotacaoDiaria quote;

    @BeforeEach
    void prepararDados() {
        title = new TituloDTO("Tesouro Selic", LocalDate.of(2031, 3, 1), LocalDate.of(2026, 10, 2),
                new BigDecimal("0.09"), new BigDecimal("0.10"), new BigDecimal("19943.12"),
                new BigDecimal("19924.17"), new BigDecimal("19924.17"));
        holding = new AtivoCarteira(1L, title.nomeAtivo(), LocalDate.of(2026, 1, 10),
                new BigDecimal("0.1"), new BigDecimal("19943.12"), new BigDecimal("0.25"));
        quote = new CotacaoDiaria(1L, title.nomeAtivo(), title.dataBase(), title.taxaVendaManha(),
                title.precoCompraManha(), title.precoVendaManha());
    }

    @Test
    void persisteCotasEInvocaMonitoramentoParaAtivoCorrespondente() {
        when(tesouroDiretoClient.buscarTitulos()).thenReturn(Mono.just(new TesouroResponseDTO(List.of(title))));
        when(cotacaoDiariaService.salvarCotacoes(anyList())).thenReturn(1);
        when(ativoCarteiraRepository.findAll()).thenReturn(List.of(holding));
        when(cotacaoDiariaService.cotacaoMaisRecente(holding.getNomeTitulo())).thenReturn(Optional.of(quote));

        scheduler.sincronizarCotacoes();

        verify(cotacaoDiariaService).salvarCotacoes(List.of(title));
        verify(monitoramentoService).avaliarAgio(holding, quote);
    }

    @Test
    void respostaVaziaNaoAlteraPersistencia() {
        when(tesouroDiretoClient.buscarTitulos()).thenReturn(Mono.just(new TesouroResponseDTO(List.of())));

        scheduler.sincronizarCotacoes();

        verify(cotacaoDiariaService, never()).salvarCotacoes(anyList());
        verify(ativoCarteiraRepository, never()).findAll();
        verify(monitoramentoService, never()).avaliarAgio(holding, quote);
    }
}
