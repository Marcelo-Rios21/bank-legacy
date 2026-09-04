package com.bank.bff.atm.data;

import java.math.BigDecimal;

public record AtmBalanceData(
        Long cuentaId,
        BigDecimal saldoDisponible) {
}