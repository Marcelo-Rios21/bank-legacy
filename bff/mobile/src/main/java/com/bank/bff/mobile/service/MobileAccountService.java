package com.bank.bff.mobile.service;

import java.util.List;
import java.util.Optional;

import com.bank.bff.mobile.dto.MobileAccountSummaryResponse;
import com.bank.bff.mobile.dto.MobileMovementResponse;
import com.bank.bff.mobile.repository.MobileAccountRepository;
import com.bank.bff.mobile.repository.MobileMovementRepository;

import org.springframework.stereotype.Service;

@Service
public class MobileAccountService {

    private final MobileAccountRepository accountRepository;
    private final MobileMovementRepository movementRepository;

    public MobileAccountService(
            MobileAccountRepository accountRepository,
            MobileMovementRepository movementRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    public Optional<MobileAccountSummaryResponse> findSummaryById(long cuentaId) {
        return accountRepository.findSummaryById(cuentaId)
                .map(account -> new MobileAccountSummaryResponse(
                        account.cuentaId(),
                        account.tipo(),
                        account.saldo()));
    }

    public List<MobileMovementResponse> findLatestMovements(long cuentaId) {
        return movementRepository.findLatestByAccountId(cuentaId)
                .stream()
                .map(movement -> new MobileMovementResponse(
                        movement.fecha(),
                        movement.transaccion(),
                        movement.monto()))
                .toList();
    }
}