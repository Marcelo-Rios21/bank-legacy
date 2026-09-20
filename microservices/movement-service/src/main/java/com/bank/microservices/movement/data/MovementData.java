package com.bank.microservices.movement.data;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovementData(
        Long movimientoId,
        Long cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}