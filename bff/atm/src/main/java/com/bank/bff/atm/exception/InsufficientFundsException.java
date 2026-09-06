package com.bank.bff.atm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Saldo insuficiente para realizar el retiro");
    }
}