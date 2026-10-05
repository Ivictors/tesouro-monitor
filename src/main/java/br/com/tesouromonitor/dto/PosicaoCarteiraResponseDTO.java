package br.com.tesouromonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PosicaoCarteiraResponseDTO(
        AtivoCarteiraResponseDTO ativo,
        LocalDate dataCotacao,
        BigDecimal puVenda,
        BigDecimal valorInvestido,
        BigDecimal valorAtualEstimado,
        BigDecimal variacaoValor,
        BigDecimal variacaoPercentual) {
}
