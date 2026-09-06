package com.bank.bff.atm.dto;

import java.math.BigDecimal;

public record WithdrawalRequest(
        BigDecimal monto) {
}