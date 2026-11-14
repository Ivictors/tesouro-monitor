package br.com.tesouromonitor.controller;

import br.com.tesouromonitor.dto.TituloDisponivelResponseDTO;
import br.com.tesouromonitor.service.CotacaoDiariaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/titulos")
public class TitulosController {

    private final CotacaoDiariaService cotacaoService;

    public TitulosController(CotacaoDiariaService cotacaoService) {
        this.cotacaoService = cotacaoService;
    }

    @GetMapping
    public List<TituloDisponivelResponseDTO> listarDisponiveis() {
        return cotacaoService.titulosDisponiveis();
    }
}
