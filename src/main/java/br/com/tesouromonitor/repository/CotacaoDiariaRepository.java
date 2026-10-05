package br.com.tesouromonitor.repository;

import br.com.tesouromonitor.domain.CotacaoDiaria;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotacaoDiariaRepository extends JpaRepository<CotacaoDiaria, Long> {

    Optional<CotacaoDiaria> findTopByNomeAtivoOrderByDataCotacaoDesc(String nomeAtivo);

    Optional<CotacaoDiaria> findByNomeAtivoAndDataCotacao(String nomeAtivo,
                                                           java.time.LocalDate dataCotacao);

    Page<CotacaoDiaria> findByNomeAtivoOrderByDataCotacaoDesc(String nomeAtivo, Pageable pageable);
}
