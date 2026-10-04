package br.com.tesouromonitor.service;

import br.com.tesouromonitor.dto.TesouroResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TesouroDiretoClient {

    private final WebClient webClient;
    private final String titulosPath;

    public TesouroDiretoClient(WebClient webClient,
                               @Value("${tesouro.api.titulos-path}") String titulosPath) {
        this.webClient = webClient;
        this.titulosPath = titulosPath;
    }

    public Mono<TesouroResponseDTO> buscarTitulos() {
        return webClient.get()
                .uri(titulosPath)
                .retrieve()
                .bodyToMono(TesouroResponseDTO.class);
    }
}
