package br.com.tesouromonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TituloDTO(
        @JsonProperty("Tipo Titulo") String nome,
        @JsonProperty("Data Vencimento") LocalDate dataVencimento,
        @JsonProperty("Data Base") LocalDate dataBase,
        @JsonProperty("Taxa Compra Manha") BigDecimal taxaCompraManha,
        @JsonProperty("Taxa Venda Manha") BigDecimal taxaVendaManha,
        @JsonProperty("PU Compra Manha") BigDecimal precoCompraManha,
        @JsonProperty("PU Venda Manha") BigDecimal precoVendaManha,
        @JsonProperty("PU Base Manha") BigDecimal precoBaseManha) {

    public String nomeAtivo() {
        return nome + " - " + dataVencimento.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
