package com.bank.microservices.account.dto;

import java.math.BigDecimal;

public record AccountResponse(
        Long cuentaId,
        String nombre,
        BigDecimal saldoInicial,
        Integer edad,
        String tipo,
        BigDecimal interes,
        BigDecimal saldoFinal) {
}