package br.com.tesouromonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TesouroResponseDTO(@JsonProperty("titulos") List<TituloDTO> titulos) {
}
