package com.bank.bff.atm.dto;

public record AtmTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {
}