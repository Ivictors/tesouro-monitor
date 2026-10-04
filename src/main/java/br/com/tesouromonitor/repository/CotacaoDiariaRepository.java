package br.com.tesouromonitor.repository;

import br.com.tesouromonitor.domain.CotacaoDiaria;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotacaoDiariaRepository extends JpaRepository<CotacaoDiaria, Long> {

    Optional<CotacaoDiaria> findTopByNomeAtivoOrderByDataCotacaoDesc(String nomeAtivo);
}
