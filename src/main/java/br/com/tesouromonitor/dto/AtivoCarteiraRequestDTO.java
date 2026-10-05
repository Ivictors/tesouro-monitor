package br.com.tesouromonitor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AtivoCarteiraRequestDTO(
        @NotBlank String nomeTitulo,
        @NotNull LocalDate dataCompra,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal taxaContratada,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal precoCompra,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantidade) {
}
