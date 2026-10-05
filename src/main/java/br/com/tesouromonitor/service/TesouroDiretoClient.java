package br.com.tesouromonitor.service;

import br.com.tesouromonitor.dto.TesouroResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TesouroDiretoClient {

    private final WebClient webClient;
    private final String titulosPath;
    private final TesouroCsvParser csvParser;

    public TesouroDiretoClient(WebClient webClient,
                               @Value("${tesouro.api.titulos-path}") String titulosPath,
                               TesouroCsvParser csvParser) {
        this.webClient = webClient;
        this.titulosPath = titulosPath;
        this.csvParser = csvParser;
    }

    public Mono<TesouroResponseDTO> buscarTitulos() {
        return webClient.get()
                .uri(titulosPath)
                .accept(MediaType.parseMediaType("text/csv"), MediaType.TEXT_PLAIN)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(
                        new TesouroDiretoIntegracaoException(
                                "A fonte do Tesouro Direto respondeu HTTP " + response.statusCode().value() + ".")))
                .bodyToMono(String.class)
                .switchIfEmpty(Mono.error(
                        new TesouroDiretoIntegracaoException("A fonte do Tesouro Direto retornou resposta vazia.")))
                .map(csvParser::parse)
                .onErrorMap(throwable -> !(throwable instanceof TesouroDiretoIntegracaoException),
                        throwable -> new TesouroDiretoIntegracaoException(
                                "Não foi possível consultar ou interpretar os dados do Tesouro Direto.", throwable));
    }
}
