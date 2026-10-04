package br.com.tesouromonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TesouroMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TesouroMonitorApplication.class, args);
    }
}
