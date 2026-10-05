package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.dto.AtivoCarteiraRequestDTO;
import br.com.tesouromonitor.dto.AtivoCarteiraResponseDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AtivoCarteiraService {

    private final AtivoCarteiraRepository repository;

    public AtivoCarteiraService(AtivoCarteiraRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AtivoCarteiraResponseDTO> listar() {
        return repository.findAll().stream().map(AtivoCarteiraResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public AtivoCarteiraResponseDTO buscar(Long id) {
        return AtivoCarteiraResponseDTO.from(encontrar(id));
    }

    public AtivoCarteiraResponseDTO criar(AtivoCarteiraRequestDTO request) {
        AtivoCarteira ativo = new AtivoCarteira(null, request.nomeTitulo(), request.dataCompra(),
                request.taxaContratada(), request.precoCompra(), request.quantidade());
        return AtivoCarteiraResponseDTO.from(repository.save(ativo));
    }

    public AtivoCarteiraResponseDTO atualizar(Long id, AtivoCarteiraRequestDTO request) {
        AtivoCarteira ativo = encontrar(id);
        ativo.setNomeTitulo(request.nomeTitulo());
        ativo.setDataCompra(request.dataCompra());
        ativo.setTaxaContratada(request.taxaContratada());
        ativo.setPrecoCompra(request.precoCompra());
        ativo.setQuantidade(request.quantidade());
        return AtivoCarteiraResponseDTO.from(repository.save(ativo));
    }

    public void remover(Long id) {
        repository.delete(encontrar(id));
    }

    private AtivoCarteira encontrar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AtivoCarteiraNaoEncontradoException(id));
    }
}
