package com.bank.microservices.transaction.dto;

public record TransactionAuthRequest(
        String username,
        String password) {
}