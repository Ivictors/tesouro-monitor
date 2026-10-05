package br.com.tesouromonitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tesouromonitor.domain.AlertaMonitoramento;
import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonitoramentoServiceTest {

    @Mock
    private CotacaoDiariaRepository cotacaoRepository;
    @Mock
    private AlertaMonitoramentoRepository alertaRepository;

    private MonitoramentoService service;
    private AtivoCarteira ativo;

    @BeforeEach
    void setUp() {
        service = new MonitoramentoService(cotacaoRepository, alertaRepository, new BigDecimal("5.00"));
        ativo = new AtivoCarteira(4L, "Tesouro Selic - 2031-03-01", LocalDate.of(2026, 1, 10),
                new BigDecimal("0.1"), new BigDecimal("100.00"), new BigDecimal("2.00"));
    }

    @Test
    void calculaValoresDaPosicaoEPercentualComDuasCasas() {
        var resultado = service.calcularAgio(ativo, quote(10L, "102.00")).orElseThrow();

        assertThat(resultado.valorInvestido()).isEqualByComparingTo("200.00");
        assertThat(resultado.valorAtualEstimado()).isEqualByComparingTo("204.00");
        assertThat(resultado.variacaoValor()).isEqualByComparingTo("4.00");
        assertThat(resultado.variacaoPercentual()).isEqualByComparingTo("2.00");
    }

    @Test
    void calculaDesagioComSinalNegativo() {
        var resultado = service.calcularAgio(ativo, quote(10L, "98.00")).orElseThrow();

        assertThat(resultado.variacaoValor()).isEqualByComparingTo("-4.00");
        assertThat(resultado.variacaoPercentual()).isEqualByComparingTo("-2.00");
    }

    @Test
    void arredondaPercentualParaDuasCasasComHalfUp() {
        var resultado = service.calcularAgio(ativo, quote(10L, "100.005")).orElseThrow();

        assertThat(resultado.variacaoPercentual()).isEqualByComparingTo("0.01");
        assertThat(resultado.valorAtualEstimado()).isEqualByComparingTo("200.01");
    }

    @Test
    void retornaVazioParaPrecosOuQuantidadeNulosOuNaoPositivos() {
        assertThat(service.calcularAgio(ativo, quote(10L, null))).isEmpty();
        ativo.setPrecoCompra(BigDecimal.ZERO);
        assertThat(service.calcularAgio(ativo, quote(10L, "105.00"))).isEmpty();
        ativo.setPrecoCompra(new BigDecimal("100.00"));
        ativo.setQuantidade(null);
        assertThat(service.calcularAgio(ativo, quote(10L, "105.00"))).isEmpty();
        assertThat(service.calcularAgio(null, quote(10L, "105.00"))).isEmpty();
    }

    @Test
    void criaAlertaNoLimiteExatoEUsaCompraComoPrimeiraReferencia() {
        CotacaoDiaria atual = quote(10L, "105.00");
        when(cotacaoRepository.findTopByNomeAtivoAndDataCotacaoBeforeOrderByDataCotacaoDesc(
                ativo.getNomeTitulo(), atual.getDataCotacao())).thenReturn(Optional.empty());
        when(alertaRepository.findByAtivoIdAndCotacaoId(ativo.getId(), atual.getId())).thenReturn(Optional.empty());

        service.avaliarAgio(ativo, atual);

        verify(alertaRepository).save(org.mockito.ArgumentMatchers.argThat(alerta ->
                alerta.getPercentualObservado().compareTo(new BigDecimal("5.00")) == 0
                        && alerta.getLimitePercentual().compareTo(new BigDecimal("5.00")) == 0));
    }

    @Test
    void soCriaOutroAlertaDepoisDeVoltarParaAbaixoEDepoisCruzar() {
        CotacaoDiaria anterior = quote(9L, "104.00");
        anterior.setDataCotacao(LocalDate.of(2026, 10, 1));
        CotacaoDiaria atual = quote(10L, "105.00");
        when(cotacaoRepository.findTopByNomeAtivoAndDataCotacaoBeforeOrderByDataCotacaoDesc(
                ativo.getNomeTitulo(), atual.getDataCotacao())).thenReturn(Optional.of(anterior));
        when(alertaRepository.findByAtivoIdAndCotacaoId(ativo.getId(), atual.getId())).thenReturn(Optional.empty());

        service.avaliarAgio(ativo, atual);

        verify(alertaRepository).save(any(AlertaMonitoramento.class));
    }

    @Test
    void naoRepeteAlertaEnquantoAcimaDoLimiteOuParaMesmaCotacao() {
        CotacaoDiaria anterior = quote(9L, "106.00");
        anterior.setDataCotacao(LocalDate.of(2026, 10, 1));
        CotacaoDiaria atual = quote(10L, "108.00");
        when(cotacaoRepository.findTopByNomeAtivoAndDataCotacaoBeforeOrderByDataCotacaoDesc(
                ativo.getNomeTitulo(), atual.getDataCotacao())).thenReturn(Optional.of(anterior));

        service.avaliarAgio(ativo, atual);

        verify(alertaRepository, never()).save(any(AlertaMonitoramento.class));
    }

    @Test
    void desagioNuncaGeraAlerta() {
        service.avaliarAgio(ativo, quote(10L, "98.00"));

        verify(alertaRepository, never()).save(any(AlertaMonitoramento.class));
    }

    @Test
    void deduplicaAlertaPorAtivoECotacao() {
        CotacaoDiaria atual = quote(10L, "105.00");
        when(cotacaoRepository.findTopByNomeAtivoAndDataCotacaoBeforeOrderByDataCotacaoDesc(
                ativo.getNomeTitulo(), atual.getDataCotacao())).thenReturn(Optional.empty());
        when(alertaRepository.findByAtivoIdAndCotacaoId(ativo.getId(), atual.getId()))
                .thenReturn(Optional.of(new AlertaMonitoramento()));

        service.avaliarAgio(ativo, atual);

        verify(alertaRepository, never()).save(any(AlertaMonitoramento.class));
    }

    private CotacaoDiaria quote(Long id, String resalePrice) {
        return new CotacaoDiaria(id, ativo.getNomeTitulo(), LocalDate.of(2026, 10, 2),
                new BigDecimal("0.10"), new BigDecimal("100.00"),
                resalePrice == null ? null : new BigDecimal(resalePrice));
    }
}
