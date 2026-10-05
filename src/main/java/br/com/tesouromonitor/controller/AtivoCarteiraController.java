package br.com.tesouromonitor.controller;

import br.com.tesouromonitor.dto.AtivoCarteiraRequestDTO;
import br.com.tesouromonitor.dto.AtivoCarteiraResponseDTO;
import br.com.tesouromonitor.service.AtivoCarteiraService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ativos")
public class AtivoCarteiraController {

    private final AtivoCarteiraService service;

    public AtivoCarteiraController(AtivoCarteiraService service) {
        this.service = service;
    }

    @GetMapping
    public List<AtivoCarteiraResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AtivoCarteiraResponseDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<AtivoCarteiraResponseDTO> criar(@Valid @RequestBody AtivoCarteiraRequestDTO request) {
        AtivoCarteiraResponseDTO created = service.criar(request);
        return ResponseEntity.created(URI.create("/api/ativos/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AtivoCarteiraResponseDTO atualizar(@PathVariable Long id,
                                               @Valid @RequestBody AtivoCarteiraRequestDTO request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
