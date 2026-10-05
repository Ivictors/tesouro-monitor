package br.com.tesouromonitor.dto;

import br.com.tesouromonitor.domain.AtivoCarteira;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AtivoCarteiraResponseDTO(
        Long id,
        String nomeTitulo,
        LocalDate dataCompra,
        BigDecimal taxaContratada,
        BigDecimal precoCompra,
        BigDecimal quantidade) {

    public static AtivoCarteiraResponseDTO from(AtivoCarteira ativo) {
        return new AtivoCarteiraResponseDTO(ativo.getId(), ativo.getNomeTitulo(), ativo.getDataCompra(),
                ativo.getTaxaContratada(), ativo.getPrecoCompra(), ativo.getQuantidade());
    }
}
