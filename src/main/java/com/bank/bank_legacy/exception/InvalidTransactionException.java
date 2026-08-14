package com.bank.bank_legacy.exception;

public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}