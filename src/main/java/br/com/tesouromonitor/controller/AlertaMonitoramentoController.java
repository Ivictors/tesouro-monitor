package br.com.tesouromonitor.controller;

import br.com.tesouromonitor.dto.AlertaMonitoramentoResponseDTO;
import br.com.tesouromonitor.dto.AlertasRecentesResponseDTO;
import br.com.tesouromonitor.service.AlertaMonitoramentoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/alertas")
public class AlertaMonitoramentoController {

    private final AlertaMonitoramentoService service;

    public AlertaMonitoramentoController(AlertaMonitoramentoService service) {
        this.service = service;
    }

    @GetMapping
    public AlertasRecentesResponseDTO recentes(
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return service.recentes(PageRequest.of(pagina, tamanho));
    }

    @PatchMapping("/{id}/lido")
    public AlertaMonitoramentoResponseDTO marcarComoLido(@PathVariable Long id) {
        return service.marcarComoLido(id);
    }
}
