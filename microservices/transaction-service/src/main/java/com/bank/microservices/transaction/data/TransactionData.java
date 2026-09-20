package com.bank.microservices.transaction.data;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionData(
        Long id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo) {
}