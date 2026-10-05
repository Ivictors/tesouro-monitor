package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.dto.ResultadoAgioDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoramentoService {

    private static final int CASAS_DECIMAIS = 2;
    private static final BigDecimal CEM = new BigDecimal("100");

    @Transactional(readOnly = true)
    public Optional<ResultadoAgioDTO> avaliarAgio(AtivoCarteira ativo, CotacaoDiaria cotacaoAtual) {
        if (ativo == null || cotacaoAtual == null
                || !positivo(ativo.getPrecoCompra())
                || !positivo(ativo.getQuantidade())
                || !positivo(cotacaoAtual.getPrecoResgateAtual())) {
            return Optional.empty();
        }

        BigDecimal valorInvestidoExato = ativo.getPrecoCompra().multiply(ativo.getQuantidade());
        if (valorInvestidoExato.signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal valorAtualExato = cotacaoAtual.getPrecoResgateAtual().multiply(ativo.getQuantidade());
        BigDecimal variacaoExata = valorAtualExato.subtract(valorInvestidoExato);
        BigDecimal percentual = variacaoExata.multiply(CEM)
                .divide(valorInvestidoExato, CASAS_DECIMAIS, RoundingMode.HALF_UP);

        return Optional.of(new ResultadoAgioDTO(
                arredondar(valorInvestidoExato), arredondar(valorAtualExato),
                arredondar(variacaoExata), percentual));
    }

    @Transactional(readOnly = true)
    public Optional<ResultadoAgioDTO> calcularAgio(AtivoCarteira ativo, CotacaoDiaria cotacaoAtual) {
        return avaliarAgio(ativo, cotacaoAtual);
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> calcularValorInvestido(AtivoCarteira ativo) {
        if (ativo == null || !positivo(ativo.getPrecoCompra()) || !positivo(ativo.getQuantidade())) {
            return Optional.empty();
        }
        return Optional.of(arredondar(ativo.getPrecoCompra().multiply(ativo.getQuantidade())));
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
