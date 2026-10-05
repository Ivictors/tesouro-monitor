package br.com.tesouromonitor.dto;

import br.com.tesouromonitor.domain.AlertaMonitoramento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AlertaMonitoramentoResponseDTO(
        Long id,
        Long ativoId,
        String nomeAtivo,
        LocalDate dataCotacao,
        BigDecimal percentualObservado,
        String regraAplicada,
        BigDecimal limitePercentual,
        LocalDateTime criadoEm,
        boolean lido,
        LocalDateTime lidoEm) {

    public static AlertaMonitoramentoResponseDTO from(AlertaMonitoramento alerta) {
        return new AlertaMonitoramentoResponseDTO(alerta.getId(), alerta.getAtivoId(), alerta.getNomeAtivo(),
                alerta.getDataCotacao(), alerta.getPercentualObservado(), alerta.getRegraAplicada(), alerta.getLimitePercentual(),
                alerta.getCriadoEm(), alerta.isLido(), alerta.getLidoEm());
    }
}
