package com.bank.bff.web.dto;

public record WebTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {
}