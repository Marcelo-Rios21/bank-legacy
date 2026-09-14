package com.bank.bff.atm.dto;

public record AtmAuthRequest(
        String username,
        String password) {
}