package com.bank.bff.mobile.dto;

public record MobileAuthRequest(
        String username,
        String password) {
}