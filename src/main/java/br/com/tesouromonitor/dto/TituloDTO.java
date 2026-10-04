package br.com.tesouromonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TituloDTO(
        @JsonProperty("nome") String nome,
        @JsonProperty("taxa_juros") BigDecimal taxaJuros,
        @JsonProperty("valor_atual") BigDecimal valorAtual) {
}
