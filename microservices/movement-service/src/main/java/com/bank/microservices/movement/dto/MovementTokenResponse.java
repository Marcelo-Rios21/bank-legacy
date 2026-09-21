package com.bank.microservices.movement.dto;

public record MovementTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {
}