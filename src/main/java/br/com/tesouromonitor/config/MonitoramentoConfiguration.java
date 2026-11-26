package br.com.tesouromonitor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MonitoramentoProperties.class)
public class MonitoramentoConfiguration {
}
