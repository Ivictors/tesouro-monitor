package br.com.tesouromonitor;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TesouroMonitorApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TesouroMonitorApplication.class);
        application.setDefaultProperties(Map.of("server.address", "127.0.0.1"));
        application.run(args);
    }
}
