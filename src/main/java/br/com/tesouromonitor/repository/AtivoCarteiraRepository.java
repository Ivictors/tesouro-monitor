package br.com.tesouromonitor.repository;

import br.com.tesouromonitor.domain.AtivoCarteira;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtivoCarteiraRepository extends JpaRepository<AtivoCarteira, Long> {
}
