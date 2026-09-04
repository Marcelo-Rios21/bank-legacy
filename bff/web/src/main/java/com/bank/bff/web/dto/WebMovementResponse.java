package com.bank.bff.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WebMovementResponse(
        Long movimientoId,
        Long cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}