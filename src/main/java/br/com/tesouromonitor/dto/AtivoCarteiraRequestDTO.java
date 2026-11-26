package br.com.tesouromonitor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AtivoCarteiraRequestDTO(
        @NotBlank String nomeTitulo,
        @NotNull LocalDate dataCompra,
        @NotNull @DecimalMin(value = "0.0", inclusive = true)
        @Digits(integer = 36, fraction = 4) BigDecimal taxaContratada,
        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 36, fraction = 2) BigDecimal precoCompra,
        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 36, fraction = 4) BigDecimal quantidade) {
}
