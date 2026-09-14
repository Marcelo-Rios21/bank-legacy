package com.bank.bff.web.security;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "BFF_WEB_USERNAME=test-web",
        "BFF_WEB_PASSWORD=test-web-password",
        "BFF_WEB_JWT_SECRET=dGVzdC13ZWItand0LXNlY3JldC0zMi1ieXRlcy1sb25nISE="
})
@AutoConfigureMockMvc
class WebSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void debeRechazarSolicitudSinToken() throws Exception {
        mockMvc.perform(get("/api/web/accounts/101"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeRechazarTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/web/accounts/101")
                        .header(
                                "Authorization",
                                "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeRechazarTokenConRolDeOtroCanal() throws Exception {

        String token = crearTokenConRol("ROLE_MOBILE");

        mockMvc.perform(get("/api/web/accounts/101")
                        .header(
                                "Authorization",
                                "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void debePermitirTokenValidoDelCanal() throws Exception {

        String token = obtenerTokenValido();

        mockMvc.perform(get("/api/web/accounts/101")
                        .header(
                                "Authorization",
                                "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String obtenerTokenValido() throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/web/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "test-web",
                                          "password": "test-web-password"
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

    private String crearTokenConRol(String role) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("test-security")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .subject("test-cross-channel")
                .claim("roles", List.of(role))
                .claim("channel", "test")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                        JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}