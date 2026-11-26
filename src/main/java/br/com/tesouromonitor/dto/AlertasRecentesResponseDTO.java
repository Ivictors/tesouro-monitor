package br.com.tesouromonitor.dto;

import java.math.BigDecimal;
import java.util.List;

public record AlertasRecentesResponseDTO(
        List<AlertaMonitoramentoResponseDTO> alertas,
        int pagina,
        int tamanho,
        long total,
        BigDecimal limitePercentual) {
}
