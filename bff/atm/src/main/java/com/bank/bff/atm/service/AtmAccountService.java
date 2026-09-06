package com.bank.bff.atm.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.bank.bff.atm.dto.AtmBalanceResponse;
import com.bank.bff.atm.dto.WithdrawalResponse;
import com.bank.bff.atm.exception.AccountNotFoundException;
import com.bank.bff.atm.exception.InsufficientFundsException;
import com.bank.bff.atm.exception.InvalidWithdrawalException;
import com.bank.bff.atm.repository.AtmAccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtmAccountService {

    private final AtmAccountRepository repository;

    public AtmAccountService(AtmAccountRepository repository) {
        this.repository = repository;
    }

    public Optional<AtmBalanceResponse> findBalanceById(long cuentaId) {
        return repository.findBalanceById(cuentaId)
                .map(account -> new AtmBalanceResponse(
                        account.cuentaId(),
                        account.saldoDisponible()));
    }

    @Transactional
    public WithdrawalResponse withdraw(long cuentaId, BigDecimal monto) {

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWithdrawalException(
                    "El monto del retiro debe ser mayor que cero");
        }

        var account = repository.findBalanceForUpdateById(cuentaId)
                .orElseThrow(() -> new AccountNotFoundException(cuentaId));

        if (account.saldoDisponible().compareTo(monto) < 0) {
            throw new InsufficientFundsException();
        }

        BigDecimal nuevoSaldo =
                account.saldoDisponible().subtract(monto);

        repository.updateBalance(cuentaId, nuevoSaldo);
        repository.insertWithdrawal(cuentaId, monto);

        return new WithdrawalResponse(
                cuentaId,
                monto,
                nuevoSaldo);
    }
}