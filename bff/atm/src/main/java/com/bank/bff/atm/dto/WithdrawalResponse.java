package com.bank.bff.atm.dto;

import java.math.BigDecimal;

public record WithdrawalResponse(
        Long cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoDisponible) {
}