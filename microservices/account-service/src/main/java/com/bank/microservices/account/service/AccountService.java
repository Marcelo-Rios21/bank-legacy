package com.bank.microservices.account.service;

import com.bank.microservices.account.dto.AccountResponse;
import com.bank.microservices.account.repository.AccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public AccountResponse findById(long cuentaId) {
        return repository.findById(cuentaId)
                .map(account -> new AccountResponse(
                        account.cuentaId(),
                        account.nombre(),
                        account.saldoInicial(),
                        account.edad(),
                        account.tipo(),
                        account.interes(),
                        account.saldoFinal()))
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Cuenta no encontrada"));
    }
}