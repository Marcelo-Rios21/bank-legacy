package com.bank.bank_legacy.exception;

public class ControlledRestartException extends RuntimeException {

    public ControlledRestartException(String message) {
        super(message);
    }
}