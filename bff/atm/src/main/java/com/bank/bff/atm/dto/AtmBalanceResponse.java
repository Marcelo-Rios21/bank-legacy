package com.bank.bff.atm.dto;

import java.math.BigDecimal;

public record AtmBalanceResponse(
        Long cuentaId,
        BigDecimal saldoDisponible) {
}