package com.bank.bff.mobile.data;

import java.math.BigDecimal;

public record MobileAccountSummaryData(
        Long cuentaId,
        String tipo,
        BigDecimal saldo) {
}