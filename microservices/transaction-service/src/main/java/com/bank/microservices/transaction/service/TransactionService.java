package com.bank.microservices.transaction.service;

import com.bank.microservices.transaction.dto.TransactionResponse;
import com.bank.microservices.transaction.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public TransactionResponse findById(long id) {
        return repository.findById(id)
                .map(transaction -> new TransactionResponse(
                        transaction.id(),
                        transaction.fecha(),
                        transaction.monto(),
                        transaction.tipo()))
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Transaccion no encontrada"));
    }
}