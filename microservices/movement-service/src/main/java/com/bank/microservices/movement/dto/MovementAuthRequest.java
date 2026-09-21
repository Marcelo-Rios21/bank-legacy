package com.bank.microservices.movement.dto;

public record MovementAuthRequest(
        String username,
        String password) {
}