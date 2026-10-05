package br.com.tesouromonitor.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoCarteiraResponseDTO(
        BigDecimal totalInvestido,
        BigDecimal valorAtualEstimado,
        BigDecimal variacaoValor,
        BigDecimal variacaoPercentual,
        int cotacoesIndisponiveis,
        List<PosicaoCarteiraResponseDTO> posicoes) {
}
