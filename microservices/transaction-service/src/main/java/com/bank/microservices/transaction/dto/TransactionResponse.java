package com.bank.microservices.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo) {
}