package com.bank.bank_legacy.exception;

public class TransientBankException extends RuntimeException {

    public TransientBankException(String message) {
        super(message);
    }

    public TransientBankException(String message, Throwable cause) {
        super(message, cause);
    }
}