package com.bank.bff.mobile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MobileMovementResponse(
        LocalDate fecha,
        String transaccion,
        BigDecimal monto) {
}