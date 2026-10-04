package br.com.tesouromonitor.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TituloDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveDesserializarCamposConhecidosEIgnorarCamposAdicionais() throws Exception {
        String json = """
                {"nome":"Tesouro Selic","taxa_juros":0.1,"valor_atual":1234.56,"campo_novo":true}
                """;

        TituloDTO titulo = objectMapper.readValue(json, TituloDTO.class);

        assertThat(titulo.nome()).isEqualTo("Tesouro Selic");
        assertThat(titulo.taxaJuros()).isEqualByComparingTo(new BigDecimal("0.1"));
        assertThat(titulo.valorAtual()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }
}
