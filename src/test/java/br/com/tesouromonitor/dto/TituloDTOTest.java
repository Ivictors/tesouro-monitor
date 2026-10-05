package br.com.tesouromonitor.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.tesouromonitor.service.TesouroCsvParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TituloDTOTest {

    private final TesouroCsvParser parser = new TesouroCsvParser();

    @Test
    void deveMapearCamposEFiltrarSomenteDataBaseMaisRecente() throws IOException {
        String csv;
        try (var stream = getClass().getResourceAsStream("/tesouro/tesouro-direto.csv")) {
            csv = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        TesouroResponseDTO response = parser.parse(csv);

        assertThat(response.titulos()).hasSize(3);
        TituloDTO title = response.titulos().getFirst();
        assertThat(title.nome()).isEqualTo("Tesouro Selic");
        assertThat(title.dataVencimento()).isEqualTo(LocalDate.of(2031, 3, 1));
        assertThat(title.dataBase()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(title.taxaVendaManha()).isEqualByComparingTo(new BigDecimal("0.10"));
        assertThat(title.precoCompraManha()).isEqualByComparingTo(new BigDecimal("19943.12"));
        assertThat(title.precoVendaManha()).isEqualByComparingTo(new BigDecimal("19924.17"));
        assertThat(title.precoBaseManha()).isEqualByComparingTo(new BigDecimal("19924.17"));
    }

    @Test
    void rejeitaCsvSemColunasObrigatorias() {
        assertThatThrownBy(() -> parser.parse("nome;data\nTitulo;01/01/2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coluna obrigatória ausente");
    }
}
