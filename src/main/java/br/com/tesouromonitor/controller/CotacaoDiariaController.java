package br.com.tesouromonitor.controller;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.dto.CotacaoDiariaResponseDTO;
import br.com.tesouromonitor.dto.CotacaoHistoricoResponseDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.service.AtivoCarteiraNaoEncontradoException;
import br.com.tesouromonitor.service.CotacaoDiariaService;
import br.com.tesouromonitor.service.CotacaoNaoEncontradaException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ativos/{ativoId}/cotacoes")
public class CotacaoDiariaController {

    private final AtivoCarteiraRepository ativoRepository;
    private final CotacaoDiariaService cotacaoService;

    public CotacaoDiariaController(AtivoCarteiraRepository ativoRepository,
                                   CotacaoDiariaService cotacaoService) {
        this.ativoRepository = ativoRepository;
        this.cotacaoService = cotacaoService;
    }

    @GetMapping
    public CotacaoHistoricoResponseDTO historico(
            @PathVariable Long ativoId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int tamanho) {
        AtivoCarteira ativo = encontrarAtivo(ativoId);
        var page = cotacaoService.historico(ativo.getNomeTitulo(), PageRequest.of(pagina, tamanho))
                .map(CotacaoDiariaResponseDTO::from);
        return CotacaoHistoricoResponseDTO.from(page);
    }

    @GetMapping("/atual")
    public ResponseEntity<CotacaoDiariaResponseDTO> atual(@PathVariable Long ativoId) {
        AtivoCarteira ativo = encontrarAtivo(ativoId);
        return cotacaoService.cotacaoMaisRecente(ativo.getNomeTitulo())
                .map(CotacaoDiariaResponseDTO::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CotacaoNaoEncontradaException(ativoId));
    }

    private AtivoCarteira encontrarAtivo(Long id) {
        return ativoRepository.findById(id)
                .orElseThrow(() -> new AtivoCarteiraNaoEncontradoException(id));
    }
}
