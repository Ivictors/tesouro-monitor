package br.com.tesouromonitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient tesouroWebClient(WebClient.Builder builder,
                                      @Value("${tesouro.api.base-url}") String baseUrl,
                                      @Value("${tesouro.api.connection-timeout-ms:3000}") int connectionTimeoutMs,
                                      @Value("${tesouro.api.response-timeout-seconds:30}") long responseTimeoutSeconds,
                                      @Value("${tesouro.api.max-in-memory-mb:32}") int maxInMemoryMb) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds));
        return builder.baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(maxInMemoryMb * 1024 * 1024))
                .build();
    }
}
