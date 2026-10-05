package br.com.tesouromonitor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AtivoCarteiraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AtivoCarteiraRepository repository;

    @BeforeEach
    void limparCarteira() {
        repository.deleteAll();
    }

    @Test
    void executaFluxoCrudDaCarteira() throws Exception {
        String request = """
                {"nomeTitulo":"Tesouro Selic 2031","dataCompra":"2026-01-10",
                 "taxaContratada":0.1,"precoCompra":19943.12,"quantidade":0.25}
                """;

        String location = mockMvc.perform(post("/api/ativos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nomeTitulo").value("Tesouro Selic 2031"))
                .andReturn().getResponse().getHeader("Location");

        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        mockMvc.perform(get("/api/ativos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(0.25));
        mockMvc.perform(get("/api/ativos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(put("/api/ativos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.replace("0.25", "0.50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(0.50));

        mockMvc.perform(delete("/api/ativos/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/ativos/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void validaCamposObrigatoriosEValoresPositivos() throws Exception {
        mockMvc.perform(post("/api/ativos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeTitulo":" ","dataCompra":null,"taxaContratada":-1,
                                "precoCompra":0,"quantidade":-2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void retornaNotFoundParaAtivoInexistente() throws Exception {
        mockMvc.perform(get("/api/ativos/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ativo não encontrado"));
    }
}
