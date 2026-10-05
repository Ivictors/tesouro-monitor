package br.com.tesouromonitor.repository;

import br.com.tesouromonitor.domain.CotacaoDiaria;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotacaoDiariaRepository extends JpaRepository<CotacaoDiaria, Long> {

    Optional<CotacaoDiaria> findTopByNomeAtivoOrderByDataCotacaoDesc(String nomeAtivo);

    Optional<CotacaoDiaria> findTopByNomeAtivoAndDataCotacaoBeforeOrderByDataCotacaoDesc(
            String nomeAtivo, LocalDate dataCotacao);

    Page<CotacaoDiaria> findByNomeAtivoAndDataCotacaoBetweenOrderByDataCotacaoDesc(
            String nomeAtivo, LocalDate inicio, LocalDate fim, Pageable pageable);

    Page<CotacaoDiaria> findByNomeAtivoAndDataCotacaoGreaterThanEqualOrderByDataCotacaoDesc(
            String nomeAtivo, LocalDate inicio, Pageable pageable);

    Page<CotacaoDiaria> findByNomeAtivoAndDataCotacaoLessThanEqualOrderByDataCotacaoDesc(
            String nomeAtivo, LocalDate fim, Pageable pageable);

    Optional<CotacaoDiaria> findByNomeAtivoAndDataCotacao(String nomeAtivo,
                                                           java.time.LocalDate dataCotacao);

    Page<CotacaoDiaria> findByNomeAtivoOrderByDataCotacaoDesc(String nomeAtivo, Pageable pageable);
}
