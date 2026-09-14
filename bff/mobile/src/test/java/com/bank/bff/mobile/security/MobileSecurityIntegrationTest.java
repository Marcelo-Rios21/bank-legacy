package com.bank.bff.mobile.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "BFF_MOBILE_USERNAME=test-mobile",
        "BFF_MOBILE_PASSWORD=test-mobile-password",
        "BFF_MOBILE_JWT_SECRET=dGVzdC1tb2JpbGUtand0LXNlY3JldC0zMi1ieXRlcyEh"
})
@AutoConfigureMockMvc
class MobileSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debeRechazarSolicitudSinToken() throws Exception {
        mockMvc.perform(get("/api/mobile/accounts/101/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeRechazarTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/mobile/accounts/101/summary")
                        .header(
                                "Authorization",
                                "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debePermitirTokenValidoDelCanal() throws Exception {

        String token = obtenerTokenValido();

        mockMvc.perform(get("/api/mobile/accounts/101/summary")
                        .header(
                                "Authorization",
                                "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String obtenerTokenValido() throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/mobile/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "test-mobile",
                                          "password": "test-mobile-password"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";

        int start = json.indexOf(marker);

        if (start < 0) {
            throw new IllegalStateException(
                    "La respuesta no contiene accessToken");
        }

        start += marker.length();

        int end = json.indexOf('"', start);

        return json.substring(start, end);
    }
}