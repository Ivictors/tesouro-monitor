package br.com.tesouromonitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient tesouroWebClient(WebClient.Builder builder,
                                      @Value("${tesouro.api.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
