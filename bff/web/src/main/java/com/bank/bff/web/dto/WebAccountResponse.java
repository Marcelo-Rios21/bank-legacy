package com.bank.bff.web.dto;

import java.math.BigDecimal;

public record WebAccountResponse(
        Long cuentaId,
        String nombre,
        BigDecimal saldoInicial,
        Integer edad,
        String tipo,
        BigDecimal interes,
        BigDecimal saldoFinal) {
}