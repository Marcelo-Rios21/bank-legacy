package com.bank.microservices.account.data;

import java.math.BigDecimal;

public record AccountData(
        Long cuentaId,
        String nombre,
        BigDecimal saldoInicial,
        Integer edad,
        String tipo,
        BigDecimal interes,
        BigDecimal saldoFinal) {
}