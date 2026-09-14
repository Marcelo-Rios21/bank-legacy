package com.bank.bff.web.dto;

public record WebAuthRequest(
        String username,
        String password) {
}