package com.bank.bff.web.data;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WebMovementData(
        Long movimientoId,
        Long cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}