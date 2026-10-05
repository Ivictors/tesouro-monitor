package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.AlertaMonitoramento;
import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.dto.ResultadoAgioDTO;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoramentoService {

    private static final Logger log = LoggerFactory.getLogger(MonitoramentoService.class);
    private static final int CASAS_DECIMAIS = 2;
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final String REGRA_VALORIZACAO_LIMITE = "VALORIZACAO_ACIMA_LIMITE";

    private final CotacaoDiariaRepository cotacaoRepository;
    private final AlertaMonitoramentoRepository alertaRepository;
    private final BigDecimal limiteAgioPercentual;

    public MonitoramentoService(CotacaoDiariaRepository cotacaoRepository,
                                AlertaMonitoramentoRepository alertaRepository,
                                @Value("${tesouro.monitoramento.limite-agio-percentual:5.00}")
                                BigDecimal limiteAgioPercentual) {
        this.cotacaoRepository = cotacaoRepository;
        this.alertaRepository = alertaRepository;
        this.limiteAgioPercentual = limiteAgioPercentual;
    }

    @Transactional
    public Optional<ResultadoAgioDTO> avaliarAgio(AtivoCarteira ativo, CotacaoDiaria cotacaoAtual) {
        Optional<ResultadoAgioDTO> resultado = calcularVariacao(ativo, cotacaoAtual);
        if (resultado.isEmpty() || ativo.getId() == null || cotacaoAtual.getId() == null) {
            return resultado;
        }

        ResultadoAgioDTO atual = resultado.orElseThrow();
        if (atual.variacaoPercentual().compareTo(limiteAgioPercentual) < 0) {
            return resultado;
        }

        BigDecimal percentualAnterior = cotacaoRepository
                .findTopByNomeAtivoAndDataCotacaoBeforeOrderByDataCotacaoDesc(
                        ativo.getNomeTitulo(), cotacaoAtual.getDataCotacao())
                .flatMap(cotacaoAnterior -> calcularVariacao(ativo, cotacaoAnterior))
                .map(ResultadoAgioDTO::variacaoPercentual)
                .orElse(BigDecimal.ZERO);

        boolean cruzouLimite = percentualAnterior.compareTo(limiteAgioPercentual) < 0;
        boolean alertaJaRegistrado = alertaRepository
                .findByAtivoIdAndCotacaoId(ativo.getId(), cotacaoAtual.getId()).isPresent();
        if (cruzouLimite && !alertaJaRegistrado) {
            alertaRepository.save(new AlertaMonitoramento(null, ativo.getId(), cotacaoAtual.getId(),
                    ativo.getNomeTitulo(), cotacaoAtual.getDataCotacao(), atual.variacaoPercentual(),
                    REGRA_VALORIZACAO_LIMITE, limiteAgioPercentual, LocalDateTime.now(), false, null));
            log.info("Alerta de valorização criado para {}: {}%.",
                    ativo.getNomeTitulo(), atual.variacaoPercentual());
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<ResultadoAgioDTO> calcularAgio(AtivoCarteira ativo, CotacaoDiaria cotacaoAtual) {
        return calcularVariacao(ativo, cotacaoAtual);
    }

    private Optional<ResultadoAgioDTO> calcularVariacao(AtivoCarteira ativo, CotacaoDiaria cotacaoAtual) {
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
