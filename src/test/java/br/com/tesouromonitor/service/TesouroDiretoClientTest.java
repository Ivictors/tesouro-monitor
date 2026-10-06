package br.com.tesouromonitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.tesouromonitor.dto.TesouroResponseDTO;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

class TesouroDiretoClientTest {

    private HttpServer server;
    private int responseStatus;
    private long responseDelayMillis;
    private String responseBody;

    @BeforeEach
    void iniciarServidor() throws IOException {
        responseStatus = 200;
        responseDelayMillis = 0;
        responseBody = "Tipo Titulo;Data Vencimento;Data Base;Taxa Compra Manha;Taxa Venda Manha;"
                + "PU Compra Manha;PU Venda Manha;PU Base Manha\n"
                + "Tesouro Selic;01/03/2031;02/10/2026;0,09;0,10;19943,12;19924,17;19924,17\n";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/data.csv", exchange -> {
            try {
                if (responseDelayMillis > 0) {
                    Thread.sleep(responseDelayMillis);
                }
                byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                exchange.sendResponseHeaders(responseStatus, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @AfterEach
    void encerrarServidor() {
        server.stop(0);
    }

    @Test
    void consultaEInterpretaCsvDoServico() {
        TesouroResponseDTO response = client(Duration.ofSeconds(10)).buscarTitulos().block(Duration.ofSeconds(15));

        assertThat(response).isNotNull();
        assertThat(response.titulos()).hasSize(1);
        assertThat(response.titulos().getFirst().nome()).isEqualTo("Tesouro Selic");
    }

    @Test
    void informaStatusHttpNaoExitosoSemExporCorpoDoServidor() {
        responseStatus = 503;
        responseBody = "resposta sensível do servidor";

        assertThatThrownBy(() -> client(Duration.ofSeconds(2)).buscarTitulos().block(Duration.ofSeconds(3)))
                .isInstanceOf(TesouroDiretoIntegracaoException.class)
                .hasMessageContaining("HTTP 503")
                .hasMessageNotContaining("resposta sensível");
    }

    @Test
    void respeitaTimeoutDeResposta() {
        responseDelayMillis = 300;

        assertThatThrownBy(() -> client(Duration.ofMillis(100)).buscarTitulos().block(Duration.ofSeconds(3)))
                .isInstanceOf(TesouroDiretoIntegracaoException.class);
    }

    private TesouroDiretoClient client(Duration timeout) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(timeout)))
                .build();
        return new TesouroDiretoClient(webClient, "/data.csv", new TesouroCsvParser());
    }
}
