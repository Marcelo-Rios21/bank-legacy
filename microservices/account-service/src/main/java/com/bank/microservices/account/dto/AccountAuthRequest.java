package com.bank.microservices.account.dto;

public record AccountAuthRequest(
        String username,
        String password) {
}