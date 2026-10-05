package br.com.tesouromonitor.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonitoramentoServiceTest {

    private final MonitoramentoService service = new MonitoramentoService();
    private AtivoCarteira ativo;

    @BeforeEach
    void setUp() {
        ativo = new AtivoCarteira(4L, "Tesouro Selic - 2031-03-01", LocalDate.of(2026, 1, 10),
                new BigDecimal("0.1"), new BigDecimal("100.00"), new BigDecimal("2.00"));
    }

    @Test
    void calculaValoresDaPosicaoEPercentualComDuasCasas() {
        var resultado = service.avaliarAgio(ativo, quote("102.00")).orElseThrow();

        assertThat(resultado.valorInvestido()).isEqualByComparingTo("200.00");
        assertThat(resultado.valorAtualEstimado()).isEqualByComparingTo("204.00");
        assertThat(resultado.variacaoValor()).isEqualByComparingTo("4.00");
        assertThat(resultado.variacaoPercentual()).isEqualByComparingTo("2.00");
    }

    @Test
    void calculaDesagioComSinalNegativo() {
        var resultado = service.avaliarAgio(ativo, quote("98.00")).orElseThrow();

        assertThat(resultado.variacaoValor()).isEqualByComparingTo("-4.00");
        assertThat(resultado.variacaoPercentual()).isEqualByComparingTo("-2.00");
    }

    @Test
    void arredondaPercentualParaDuasCasasComHalfUp() {
        var resultado = service.avaliarAgio(ativo, quote("100.005")).orElseThrow();

        assertThat(resultado.variacaoPercentual()).isEqualByComparingTo("0.01");
        assertThat(resultado.valorAtualEstimado()).isEqualByComparingTo("200.01");
    }

    @Test
    void retornaVazioParaPrecosOuQuantidadeNulosOuNaoPositivos() {
        assertThat(service.avaliarAgio(ativo, quote(null))).isEmpty();
        ativo.setPrecoCompra(BigDecimal.ZERO);
        assertThat(service.avaliarAgio(ativo, quote("105.00"))).isEmpty();
        ativo.setPrecoCompra(new BigDecimal("100.00"));
        ativo.setQuantidade(null);
        assertThat(service.avaliarAgio(ativo, quote("105.00"))).isEmpty();
        assertThat(service.avaliarAgio(null, quote("105.00"))).isEmpty();
    }

    private CotacaoDiaria quote(String resalePrice) {
        return new CotacaoDiaria(10L, ativo.getNomeTitulo(), LocalDate.of(2026, 10, 2),
                new BigDecimal("0.10"), new BigDecimal("100.00"),
                resalePrice == null ? null : new BigDecimal(resalePrice));
    }
}
