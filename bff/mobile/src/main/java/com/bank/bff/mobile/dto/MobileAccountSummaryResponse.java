package com.bank.bff.mobile.dto;

import java.math.BigDecimal;

public record MobileAccountSummaryResponse(
        Long cuentaId,
        String tipo,
        BigDecimal saldo) {
}