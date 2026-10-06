package br.com.tesouromonitor.controller;

import br.com.tesouromonitor.dto.SincronizacaoStatusDTO;
import br.com.tesouromonitor.service.CotacaoSincronizacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sincronizacao")
public class SincronizacaoController {

    private final CotacaoSincronizacaoService sincronizacaoService;

    public SincronizacaoController(CotacaoSincronizacaoService sincronizacaoService) {
        this.sincronizacaoService = sincronizacaoService;
    }

    @GetMapping
    public SincronizacaoStatusDTO status() {
        return sincronizacaoService.statusAtual();
    }

    @PostMapping
    public ResponseEntity<SincronizacaoStatusDTO> sincronizar() {
        SincronizacaoStatusDTO resultado = sincronizacaoService.sincronizar();
        return ResponseEntity.status(resultado.sucesso() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY)
                .body(resultado);
    }
}
