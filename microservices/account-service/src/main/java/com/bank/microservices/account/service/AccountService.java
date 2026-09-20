package com.bank.microservices.account.service;

import java.util.Optional;

import com.bank.microservices.account.data.AccountData;
import com.bank.microservices.account.dto.AccountResponse;
import com.bank.microservices.account.repository.AccountRepository;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class AccountService {

    private static final String CIRCUIT_BREAKER = "accountDatabase";

    private final AccountRepository repository;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public AccountService(
            AccountRepository repository,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.repository = repository;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public AccountResponse findById(long cuentaId) {

        Optional<AccountData> account = circuitBreakerFactory
                .create(CIRCUIT_BREAKER)
                .run(
                        () -> repository.findById(cuentaId),
                        throwable -> {
                            throw new ResponseStatusException(
                                    SERVICE_UNAVAILABLE,
                                    "Servicio de cuentas temporalmente no disponible",
                                    throwable);
                        });

        return account
                .map(data -> new AccountResponse(
                        data.cuentaId(),
                        data.nombre(),
                        data.saldoInicial(),
                        data.edad(),
                        data.tipo(),
                        data.interes(),
                        data.saldoFinal()))
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Cuenta no encontrada"));
    }
}