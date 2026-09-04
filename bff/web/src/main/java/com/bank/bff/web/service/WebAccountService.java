package com.bank.bff.web.service;

import java.util.List;
import java.util.Optional;

import com.bank.bff.web.dto.WebAccountResponse;
import com.bank.bff.web.dto.WebMovementResponse;
import com.bank.bff.web.repository.WebAccountRepository;
import com.bank.bff.web.repository.WebMovementRepository;

import org.springframework.stereotype.Service;

@Service
public class WebAccountService {

    private final WebAccountRepository accountRepository;
    private final WebMovementRepository movementRepository;

    public WebAccountService(
            WebAccountRepository accountRepository,
            WebMovementRepository movementRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    public Optional<WebAccountResponse> findById(long cuentaId) {
        return accountRepository.findById(cuentaId)
                .map(account -> new WebAccountResponse(
                        account.cuentaId(),
                        account.nombre(),
                        account.saldoInicial(),
                        account.edad(),
                        account.tipo(),
                        account.interes(),
                        account.saldoFinal()));
    }

    public List<WebMovementResponse> findMovements(long cuentaId) {
        return movementRepository.findByAccountId(cuentaId)
                .stream()
                .map(movement -> new WebMovementResponse(
                        movement.movimientoId(),
                        movement.cuentaId(),
                        movement.fecha(),
                        movement.transaccion(),
                        movement.monto(),
                        movement.descripcion()))
                .toList();
    }
}