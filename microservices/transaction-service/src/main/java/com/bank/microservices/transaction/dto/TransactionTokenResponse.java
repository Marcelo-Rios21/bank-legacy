package com.bank.microservices.transaction.dto;

public record TransactionTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {
}