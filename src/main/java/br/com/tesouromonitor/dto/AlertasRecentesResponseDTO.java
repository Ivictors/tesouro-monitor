package br.com.tesouromonitor.dto;

import java.util.List;

public record AlertasRecentesResponseDTO(
        List<AlertaMonitoramentoResponseDTO> alertas,
        int pagina,
        int tamanho,
        long total) {
}
