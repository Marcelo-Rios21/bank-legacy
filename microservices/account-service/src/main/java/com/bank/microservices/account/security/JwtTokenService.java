package com.bank.microservices.account.security;

import java.time.Instant;
import java.util.List;

import com.bank.microservices.account.dto.AccountTokenResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final long EXPIRES_IN_SECONDS = 900;

    private final JwtEncoder jwtEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public AccountTokenResponse createToken(Authentication authentication) {

        Instant now = Instant.now();

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bank-account-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(EXPIRES_IN_SECONDS))
                .subject(authentication.getName())
                .claim("roles", roles)
                .claim("service", "account")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder.encode(
                        JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new AccountTokenResponse(
                token,
                "Bearer",
                EXPIRES_IN_SECONDS);
    }
}