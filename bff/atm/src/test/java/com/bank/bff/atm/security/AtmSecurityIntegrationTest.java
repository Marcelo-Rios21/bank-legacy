package com.bank.bff.atm.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "BFF_ATM_USERNAME=test-atm",
        "BFF_ATM_PASSWORD=test-atm-password"
})
@AutoConfigureMockMvc
class AtmSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debeRechazarSolicitudSinCredenciales() throws Exception {
        mockMvc.perform(get("/api/atm/accounts/101/balance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeRechazarCredencialesIncorrectas() throws Exception {
        mockMvc.perform(get("/api/atm/accounts/101/balance")
                        .header(
                                "Authorization",
                                basicAuth("incorrecto", "incorrecto")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debePermitirCredencialesCorrectasDelCanal() throws Exception {
        mockMvc.perform(get("/api/atm/accounts/101/balance")
                        .header(
                                "Authorization",
                                basicAuth("test-atm", "test-atm-password")))
                .andExpect(status().isOk());
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;

        return "Basic " + Base64.getEncoder()
                .encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8));
    }
}