package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.dto.TituloDTO;
import br.com.tesouromonitor.repository.CotacaoDiariaRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class CotacaoDiariaService {

    private final CotacaoDiariaRepository repository;

    public CotacaoDiariaService(CotacaoDiariaRepository repository) {
        this.repository = repository;
    }

    public int salvarCotacoes(List<TituloDTO> titulos) {
        int count = 0;
        for (TituloDTO titulo : titulos) {
            CotacaoDiaria cotacao = repository
                    .findByNomeAtivoAndDataCotacao(titulo.nomeAtivo(), titulo.dataBase())
                    .orElseGet(CotacaoDiaria::new);
            cotacao.setNomeAtivo(titulo.nomeAtivo());
            cotacao.setDataCotacao(titulo.dataBase());
            cotacao.setTaxaAtual(titulo.taxaVendaManha());
            cotacao.setPrecoCompraAtual(titulo.precoCompraManha());
            cotacao.setPrecoResgateAtual(titulo.precoVendaManha());
            repository.save(cotacao);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public Optional<CotacaoDiaria> cotacaoMaisRecente(String nomeAtivo) {
        return repository.findTopByNomeAtivoOrderByDataCotacaoDesc(nomeAtivo);
    }

    @Transactional(readOnly = true)
    public Page<CotacaoDiaria> historico(String nomeAtivo, Pageable pageable) {
        return historico(nomeAtivo, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CotacaoDiaria> historico(String nomeAtivo, LocalDate inicio, LocalDate fim, Pageable pageable) {
        if (inicio != null && fim != null) {
            return repository.findByNomeAtivoAndDataCotacaoBetweenOrderByDataCotacaoDesc(
                    nomeAtivo, inicio, fim, pageable);
        }
        if (inicio != null) {
            return repository.findByNomeAtivoAndDataCotacaoGreaterThanEqualOrderByDataCotacaoDesc(
                    nomeAtivo, inicio, pageable);
        }
        if (fim != null) {
            return repository.findByNomeAtivoAndDataCotacaoLessThanEqualOrderByDataCotacaoDesc(
                    nomeAtivo, fim, pageable);
        }
        return repository.findByNomeAtivoOrderByDataCotacaoDesc(nomeAtivo, pageable);
    }
}
