package com.bank.microservices.transaction.service;

import java.util.Optional;

import com.bank.microservices.transaction.data.TransactionData;
import com.bank.microservices.transaction.dto.TransactionResponse;
import com.bank.microservices.transaction.repository.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class TransactionService {

    private static final String CIRCUIT_BREAKER = "transactionDatabase";

    private final TransactionRepository repository;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public TransactionService(
            TransactionRepository repository,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.repository = repository;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public TransactionResponse findById(long id) {

        Optional<TransactionData> transaction = circuitBreakerFactory
                .create(CIRCUIT_BREAKER)
                .run(
                        () -> repository.findById(id),
                        throwable -> {
throw new ResponseStatusException(
                                    SERVICE_UNAVAILABLE,
                                    "Servicio de transacciones temporalmente no disponible",
                                    throwable);
                        });

        return transaction
                .map(data -> new TransactionResponse(
                        data.id(),
                        data.fecha(),
                        data.monto(),
                        data.tipo()))
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Transaccion no encontrada"));
    }
}