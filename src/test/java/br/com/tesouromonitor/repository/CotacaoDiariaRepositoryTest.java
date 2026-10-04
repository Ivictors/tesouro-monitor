package br.com.tesouromonitor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.tesouromonitor.domain.CotacaoDiaria;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CotacaoDiariaRepositoryTest {

    @Autowired
    private CotacaoDiariaRepository repository;

    @Test
    void buscaCotacaoMaisRecenteDoAtivo() {
        repository.save(new CotacaoDiaria(null, "Tesouro Selic", LocalDate.of(2026, 10, 2),
                new BigDecimal("0.1"), new BigDecimal("1000"), new BigDecimal("1010")));
        repository.save(new CotacaoDiaria(null, "Tesouro Selic", LocalDate.of(2026, 10, 3),
                new BigDecimal("0.11"), new BigDecimal("1010"), new BigDecimal("1020")));

        var cotacao = repository.findTopByNomeAtivoOrderByDataCotacaoDesc("Tesouro Selic");

        assertThat(cotacao).isPresent();
        assertThat(cotacao.orElseThrow().getDataCotacao()).isEqualTo(LocalDate.of(2026, 10, 3));
    }
}
