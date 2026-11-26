package br.com.tesouromonitor.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "tesouro.monitoramento")
public class MonitoramentoProperties {

    private BigDecimal limiteAgioPercentual = new BigDecimal("5.00");

    @NotNull(message = "tesouro.monitoramento.limite-agio-percentual deve ser informado.")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "tesouro.monitoramento.limite-agio-percentual deve ser maior que zero.")
    public BigDecimal getLimiteAgioPercentual() {
        return limiteAgioPercentual;
    }

    public void setLimiteAgioPercentual(BigDecimal limiteAgioPercentual) {
        this.limiteAgioPercentual = limiteAgioPercentual;
    }
}
