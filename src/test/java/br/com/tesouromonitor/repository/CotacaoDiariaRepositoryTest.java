package br.com.tesouromonitor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.tesouromonitor.domain.CotacaoDiaria;
import br.com.tesouromonitor.dto.TituloDTO;
import br.com.tesouromonitor.service.CotacaoDiariaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(CotacaoDiariaService.class)
class CotacaoDiariaRepositoryTest {

    @Autowired
    private CotacaoDiariaRepository repository;

    @Autowired
    private CotacaoDiariaService service;

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

    @Test
    void salvarNovamenteAtualizaCotacaoSemDuplicarRegistro() {
        TituloDTO first = title("Tesouro Selic", LocalDate.of(2031, 3, 1), new BigDecimal("19924.17"));
        TituloDTO updated = title("Tesouro Selic", LocalDate.of(2031, 3, 1), new BigDecimal("19930.00"));

        service.salvarCotacoes(java.util.List.of(first));
        service.salvarCotacoes(java.util.List.of(updated));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findTopByNomeAtivoOrderByDataCotacaoDesc(updated.nomeAtivo()))
                .get().extracting(CotacaoDiaria::getPrecoResgateAtual)
                .isEqualTo(new BigDecimal("19930.00"));
    }

    @Test
    void diferenciaVencimentosDoMesmoTipoDeTitulo() {
        TituloDTO first = title("Tesouro Selic", LocalDate.of(2028, 3, 1), new BigDecimal("19924.17"));
        TituloDTO second = title("Tesouro Selic", LocalDate.of(2031, 3, 1), new BigDecimal("19930.00"));

        service.salvarCotacoes(java.util.List.of(first, second));

        assertThat(repository.count()).isEqualTo(2);
        assertThat(first.nomeAtivo()).isNotEqualTo(second.nomeAtivo());
    }

    private TituloDTO title(String name, LocalDate dueDate, BigDecimal resalePrice) {
        return new TituloDTO(name, dueDate, LocalDate.of(2026, 10, 2),
                new BigDecimal("0.09"), new BigDecimal("0.10"),
                new BigDecimal("19943.12"), resalePrice, new BigDecimal("19924.17"));
    }
}
