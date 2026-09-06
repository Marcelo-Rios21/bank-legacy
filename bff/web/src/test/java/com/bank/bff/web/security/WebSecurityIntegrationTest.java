package com.bank.bff.web.security;

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
        "BFF_WEB_USERNAME=test-web",
        "BFF_WEB_PASSWORD=test-web-password"
})
@AutoConfigureMockMvc
class WebSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debeRechazarSolicitudSinCredenciales() throws Exception {
        mockMvc.perform(get("/api/web/accounts/101"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeRechazarCredencialesIncorrectas() throws Exception {
        mockMvc.perform(get("/api/web/accounts/101")
                        .header(
                                "Authorization",
                                basicAuth("incorrecto", "incorrecto")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debePermitirCredencialesCorrectasDelCanal() throws Exception {
        mockMvc.perform(get("/api/web/accounts/101")
                        .header(
                                "Authorization",
                                basicAuth("test-web", "test-web-password")))
                .andExpect(status().isOk());
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;

        return "Basic " + Base64.getEncoder()
                .encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8));
    }
}