package com.bank.bff.mobile.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

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
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
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

    @Autowired
    private JwtEncoder jwtEncoder;

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
    void debeRechazarTokenConRolDeOtroCanal() throws Exception {

        String token = crearTokenConRol("ROLE_WEB");

        mockMvc.perform(get("/api/mobile/accounts/101/summary")
                        .header(
                                "Authorization",
                                "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void debeRechazarTokenFirmadoPorOtroCanal() throws Exception {

        String tokenWeb = crearTokenFirmadoPorOtroCanal(
                "ROLE_WEB");

        mockMvc.perform(get("/api/mobile/accounts/101/summary")
                        .header(
                                "Authorization",
                                "Bearer " + tokenWeb))
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

    private String crearTokenFirmadoPorOtroCanal(String role) {

        var otherKey = new SecretKeySpec(
                "web-channel-secret-for-cross-test-123456"
                        .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");

        var otherEncoder = NimbusJwtEncoder
                .withSecretKey(otherKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bank-bff-web")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .subject("test-web")
                .claim("roles", List.of(role))
                .claim("channel", "web")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return otherEncoder.encode(
                        JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }}