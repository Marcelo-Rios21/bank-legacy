package com.bank.microservices.movement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovementResponse(
        Long movimientoId,
        Long cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}