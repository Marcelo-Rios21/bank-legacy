package com.bank.bff.mobile.data;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MobileMovementData(
        LocalDate fecha,
        String transaccion,
        BigDecimal monto) {
}