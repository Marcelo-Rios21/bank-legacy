package com.bank.bff.web.data;

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