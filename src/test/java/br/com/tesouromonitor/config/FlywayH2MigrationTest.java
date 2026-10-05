package br.com.tesouromonitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayH2MigrationTest {

    @Autowired
    private AtivoCarteiraRepository repository;

    @Test
    void migrationCriaSchemaCompativelComEntidades() {
        assertThat(repository.count()).isZero();
    }
}
