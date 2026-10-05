package br.com.tesouromonitor.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record CotacaoHistoricoResponseDTO(
        List<CotacaoDiariaResponseDTO> cotacoes,
        int pagina,
        int tamanho,
        long total,
        int totalPaginas) {

    public static CotacaoHistoricoResponseDTO from(Page<CotacaoDiariaResponseDTO> page) {
        return new CotacaoHistoricoResponseDTO(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
