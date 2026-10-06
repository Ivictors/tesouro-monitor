package br.com.tesouromonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.dto.TituloDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CotacaoSincronizacaoServiceTest {

    @Mock
    private TesouroDiretoClient cliente;
    @Mock
    private MonitoramentoService monitoramento;
    @Mock
    private CotacaoDiariaService cotacoes;
    @Mock
    private AtivoCarteiraRepository ativos;

    private CotacaoSincronizacaoService service;
    private TituloDTO titulo;
    private AtivoCarteira ativo;
    private CotacaoDiaria cotacao;

    @BeforeEach
    void configurar() {
        service = new CotacaoSincronizacaoService(cliente, monitoramento, cotacoes, ativos);
        titulo = new TituloDTO("Tesouro Selic", LocalDate.of(2031, 3, 1), LocalDate.of(2026, 10, 2),
                new BigDecimal("0.09"), new BigDecimal("0.10"), new BigDecimal("19943.12"),
                new BigDecimal("19924.17"), new BigDecimal("19924.17"));
        ativo = new AtivoCarteira(1L, "Tesouro Selic - 2031-03-01", LocalDate.of(2026, 1, 10),
                new BigDecimal("0.1"), new BigDecimal("19943.12"), new BigDecimal("0.25"));
        cotacao = new CotacaoDiaria(1L, ativo.getNomeTitulo(), titulo.dataBase(), titulo.taxaVendaManha(),
                titulo.precoCompraManha(), titulo.precoVendaManha());
    }

    @Test
    void importaCotacoesEAtualizaStatusComPosicoesCorrespondentes() {
        when(cliente.buscarTitulos()).thenReturn(Mono.just(new TesouroResponseDTO(List.of(titulo))));
        when(cotacoes.salvarCotacoes(List.of(titulo))).thenReturn(1);
        when(ativos.findAll()).thenReturn(List.of(ativo));
        when(cotacoes.cotacaoMaisRecente(ativo.getNomeTitulo())).thenReturn(Optional.of(cotacao));

        var resultado = service.sincronizar();

        assertTrue(resultado.sucesso());
        assertEquals(1, resultado.cotacoesPersistidas());
        assertEquals(1, resultado.posicoesCorrespondentes());
        assertFalse(resultado.emAndamento());
        verify(monitoramento).avaliarAgio(ativo, cotacao);
    }

    @Test
    void fonteVaziaRegistraFalhaSemAlterarPersistencia() {
        when(cliente.buscarTitulos()).thenReturn(Mono.just(new TesouroResponseDTO(List.of())));

        var resultado = service.sincronizar();

        assertFalse(resultado.sucesso());
        assertFalse(resultado.emAndamento());
        verify(cotacoes, never()).salvarCotacoes(anyList());
        verify(ativos, never()).findAll();
    }

    @Test
    void rejeitaExecucaoConcorrenteELiberaParaNovaTentativa() throws Exception {
        CountDownLatch consultaIniciada = new CountDownLatch(1);
        CountDownLatch liberarConsulta = new CountDownLatch(1);
        when(cliente.buscarTitulos()).thenReturn(Mono.fromCallable(() -> {
            consultaIniciada.countDown();
            if (!liberarConsulta.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tempo limite do teste excedido.");
            }
            return new TesouroResponseDTO(List.of(titulo));
        }));
        when(cotacoes.salvarCotacoes(List.of(titulo))).thenReturn(1);
        when(ativos.findAll()).thenReturn(List.of());

        try (var executor = Executors.newSingleThreadExecutor()) {
            var primeiraExecucao = executor.submit(() -> service.sincronizar());
            assertTrue(consultaIniciada.await(1, TimeUnit.SECONDS));
            assertThrows(SincronizacaoEmAndamentoException.class, () -> service.sincronizar());
            liberarConsulta.countDown();
            assertTrue(primeiraExecucao.get(2, TimeUnit.SECONDS).sucesso());
        }
    }
}
