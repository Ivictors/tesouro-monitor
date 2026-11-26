package br.com.tesouromonitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MonitoramentoPropertiesContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MonitoramentoConfiguration.class);

    @Test
    void iniciaComLimitePadraoDeCincoPorCento() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MonitoramentoProperties.class).getLimiteAgioPercentual())
                    .isEqualByComparingTo("5.00");
        });
    }

    @Test
    void iniciaComLimiteCustomizadoPositivo() {
        contextRunner.withPropertyValues("tesouro.monitoramento.limite-agio-percentual=2.75")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MonitoramentoProperties.class).getLimiteAgioPercentual())
                            .isEqualByComparingTo("2.75");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.25"})
    void impedeInicializacaoComLimiteZeroOuNegativo(String limite) {
        contextRunner.withPropertyValues("tesouro.monitoramento.limite-agio-percentual=" + limite)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining(
                                    "tesouro.monitoramento.limite-agio-percentual deve ser maior que zero");
                });
    }
}
