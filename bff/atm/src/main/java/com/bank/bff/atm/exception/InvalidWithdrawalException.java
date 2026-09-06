package com.bank.bff.atm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidWithdrawalException extends RuntimeException {

    public InvalidWithdrawalException(String message) {
        super(message);
    }
}