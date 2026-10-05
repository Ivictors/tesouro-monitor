package br.com.tesouromonitor.controller;

import br.com.tesouromonitor.dto.ResumoCarteiraResponseDTO;
import br.com.tesouromonitor.service.ResumoCarteiraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ativos")
public class ResumoCarteiraController {

    private final ResumoCarteiraService service;

    public ResumoCarteiraController(ResumoCarteiraService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public ResumoCarteiraResponseDTO consultar() {
        return service.consultar();
    }
}
