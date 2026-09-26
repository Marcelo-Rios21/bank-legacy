package com.bank.microservices.movement.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionSnapshotPublished(
        Long id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo) {
}
