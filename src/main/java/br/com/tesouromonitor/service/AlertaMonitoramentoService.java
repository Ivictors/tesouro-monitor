package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.AlertaMonitoramento;
import br.com.tesouromonitor.dto.AlertaMonitoramentoResponseDTO;
import br.com.tesouromonitor.dto.AlertasRecentesResponseDTO;
import br.com.tesouromonitor.repository.AlertaMonitoramentoRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AlertaMonitoramentoService {

    private final AlertaMonitoramentoRepository repository;

    public AlertaMonitoramentoService(AlertaMonitoramentoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AlertasRecentesResponseDTO recentes(Pageable pageable) {
        Page<AlertaMonitoramentoResponseDTO> page = repository.findAllByOrderByCriadoEmDesc(pageable)
                .map(AlertaMonitoramentoResponseDTO::from);
        return new AlertasRecentesResponseDTO(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements());
    }

    public AlertaMonitoramentoResponseDTO marcarComoLido(Long id) {
        AlertaMonitoramento alerta = repository.findById(id)
                .orElseThrow(() -> new AlertaNaoEncontradoException(id));
        if (!alerta.isLido()) {
            alerta.setLido(true);
            alerta.setLidoEm(LocalDateTime.now());
            alerta = repository.save(alerta);
        }
        return AlertaMonitoramentoResponseDTO.from(alerta);
    }
}
