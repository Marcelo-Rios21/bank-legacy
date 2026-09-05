package com.bank.bff.mobile.controller;

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
class MobileAccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debeRetornarResumenReducidoDeCuenta() throws Exception {
        mockMvc.perform(get("/api/mobile/accounts/101/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cuentaId").value(101))
                .andExpect(jsonPath("$.tipo").exists())
                .andExpect(jsonPath("$.saldo").exists())
                .andExpect(jsonPath("$.nombre").doesNotExist())
                .andExpect(jsonPath("$.saldoInicial").doesNotExist())
                .andExpect(jsonPath("$.interes").doesNotExist());
    }

    @Test
    void debeRetornar404ParaCuentaInexistente() throws Exception {
        mockMvc.perform(get("/api/mobile/accounts/-1/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeRetornarComoMaximoCincoMovimientosReducidos() throws Exception {
        mockMvc.perform(get("/api/mobile/accounts/101/movements"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$[0].fecha").exists())
                .andExpect(jsonPath("$[0].transaccion").exists())
                .andExpect(jsonPath("$[0].monto").exists())
                .andExpect(jsonPath("$[0].descripcion").doesNotExist())
                .andExpect(jsonPath("$[0].cuentaId").doesNotExist());
    }

    @Test
    void debeRetornar404EnMovimientosSiCuentaNoExiste() throws Exception {
        mockMvc.perform(get("/api/mobile/accounts/-1/movements"))
                .andExpect(status().isNotFound());
    }
}