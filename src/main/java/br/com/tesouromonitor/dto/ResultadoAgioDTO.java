package br.com.tesouromonitor.dto;

import java.math.BigDecimal;

public record ResultadoAgioDTO(
        BigDecimal valorInvestido,
        BigDecimal valorAtualEstimado,
        BigDecimal variacaoValor,
        BigDecimal variacaoPercentual) {
}
