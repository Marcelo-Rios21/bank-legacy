package com.bank.microservices.account.dto;

public record AccountTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {
}