package com.bank.bff.mobile.dto;

public record MobileTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {
}