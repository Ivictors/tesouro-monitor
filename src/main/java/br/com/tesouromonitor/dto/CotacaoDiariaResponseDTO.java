package br.com.tesouromonitor.dto;

import br.com.tesouromonitor.domain.CotacaoDiaria;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CotacaoDiariaResponseDTO(
        Long id,
        String nomeAtivo,
        LocalDate dataCotacao,
        BigDecimal taxaAtual,
        BigDecimal precoCompraAtual,
        BigDecimal precoResgateAtual) {

    public static CotacaoDiariaResponseDTO from(CotacaoDiaria cotacao) {
        return new CotacaoDiariaResponseDTO(cotacao.getId(), cotacao.getNomeAtivo(),
                cotacao.getDataCotacao(), cotacao.getTaxaAtual(),
                cotacao.getPrecoCompraAtual(), cotacao.getPrecoResgateAtual());
    }
}
