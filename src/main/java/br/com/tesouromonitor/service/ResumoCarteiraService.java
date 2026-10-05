package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.dto.AtivoCarteiraResponseDTO;
import br.com.tesouromonitor.dto.PosicaoCarteiraResponseDTO;
import br.com.tesouromonitor.dto.ResumoCarteiraResponseDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumoCarteiraService {

    private final AtivoCarteiraRepository ativoRepository;
    private final CotacaoDiariaService cotacaoService;
    private final MonitoramentoService monitoramentoService;

    public ResumoCarteiraService(AtivoCarteiraRepository ativoRepository,
                                 CotacaoDiariaService cotacaoService,
                                 MonitoramentoService monitoramentoService) {
        this.ativoRepository = ativoRepository;
        this.cotacaoService = cotacaoService;
        this.monitoramentoService = monitoramentoService;
    }

    @Transactional(readOnly = true)
    public ResumoCarteiraResponseDTO consultar() {
        List<AtivoCarteira> ativos = ativoRepository.findAll();
        List<PosicaoCarteiraResponseDTO> posicoes = new ArrayList<>();
        BigDecimal totalInvestido = BigDecimal.ZERO;
        BigDecimal valorAtualEstimado = BigDecimal.ZERO;
        int indisponiveis = 0;
        boolean investimentoCompleto = true;

        for (AtivoCarteira ativo : ativos) {
            var investido = monitoramentoService.calcularValorInvestido(ativo);
            if (investido.isPresent()) {
                totalInvestido = totalInvestido.add(investido.orElseThrow());
            } else {
                investimentoCompleto = false;
            }

            var cotacao = cotacaoService.cotacaoMaisRecente(ativo.getNomeTitulo());
            var resultado = cotacao.flatMap(atual -> monitoramentoService.calcularAgio(ativo, atual));
            if (resultado.isPresent()) {
                valorAtualEstimado = valorAtualEstimado.add(resultado.orElseThrow().valorAtualEstimado());
                var atual = cotacao.orElseThrow();
                var calculo = resultado.orElseThrow();
                posicoes.add(new PosicaoCarteiraResponseDTO(AtivoCarteiraResponseDTO.from(ativo),
                        atual.getDataCotacao(), atual.getPrecoResgateAtual(), calculo.valorInvestido(),
                        calculo.valorAtualEstimado(), calculo.variacaoValor(), calculo.variacaoPercentual()));
            } else {
                indisponiveis++;
                posicoes.add(new PosicaoCarteiraResponseDTO(AtivoCarteiraResponseDTO.from(ativo),
                        cotacao.map(c -> c.getDataCotacao()).orElse(null),
                        cotacao.map(c -> c.getPrecoResgateAtual()).orElse(null), investido.orElse(null),
                        null, null, null));
            }
        }

        boolean valuationComplete = indisponiveis == 0 && investimentoCompleto;
        BigDecimal totalRounded = totalInvestido.setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentRounded = valuationComplete
                ? valorAtualEstimado.setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal variationValue = currentRounded == null ? null : currentRounded.subtract(totalRounded);
        BigDecimal variationPercent = currentRounded == null || totalRounded.signum() <= 0
                ? null : variationValue.multiply(new BigDecimal("100"))
                        .divide(totalRounded, 2, RoundingMode.HALF_UP);

        return new ResumoCarteiraResponseDTO(investimentoCompleto ? totalRounded : null,
                currentRounded, variationValue, variationPercent, indisponiveis, List.copyOf(posicoes));
    }
}
