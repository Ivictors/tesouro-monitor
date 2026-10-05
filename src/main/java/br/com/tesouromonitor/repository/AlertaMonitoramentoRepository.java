package br.com.tesouromonitor.repository;

import br.com.tesouromonitor.domain.AlertaMonitoramento;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaMonitoramentoRepository extends JpaRepository<AlertaMonitoramento, Long> {

    Optional<AlertaMonitoramento> findByAtivoIdAndCotacaoId(Long ativoId, Long cotacaoId);

    Page<AlertaMonitoramento> findAllByOrderByCriadoEmDesc(Pageable pageable);

    void deleteByAtivoId(Long ativoId);
}
