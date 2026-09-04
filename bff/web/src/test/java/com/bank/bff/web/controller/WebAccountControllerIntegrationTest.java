package com.bank.bff.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class WebAccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debeRetornarDetalleCompletoDeCuenta() throws Exception {
        mockMvc.perform(get("/api/web/accounts/101"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cuentaId").value(101))
                .andExpect(jsonPath("$.nombre").exists())
                .andExpect(jsonPath("$.saldoInicial").exists())
                .andExpect(jsonPath("$.edad").exists())
                .andExpect(jsonPath("$.tipo").exists())
                .andExpect(jsonPath("$.interes").exists())
                .andExpect(jsonPath("$.saldoFinal").exists());
    }

    @Test
    void debeRetornar404ParaCuentaInexistente() throws Exception {
        mockMvc.perform(get("/api/web/accounts/-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeRetornarMovimientosDeCuenta() throws Exception {
        mockMvc.perform(get("/api/web/accounts/101/movements"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void debeRetornar404EnMovimientosSiCuentaNoExiste() throws Exception {
        mockMvc.perform(get("/api/web/accounts/-1/movements"))
                .andExpect(status().isNotFound());
    }
}