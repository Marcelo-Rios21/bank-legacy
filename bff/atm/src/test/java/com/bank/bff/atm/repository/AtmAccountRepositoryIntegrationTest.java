package com.bank.bff.atm.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bank.bff.atm.data.AtmBalanceData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AtmAccountRepositoryIntegrationTest {

    @Autowired
    private AtmAccountRepository repository;

    @Test
    void debeEncontrarSaldoDeCuentaExistente() {
        var result = repository.findBalanceById(101L);

        assertTrue(result.isPresent());

        AtmBalanceData account = result.get();

        assertEquals(101L, account.cuentaId());
        assertTrue(account.saldoDisponible().signum() >= 0);
    }

    @Test
    void debeRetornarVacioParaCuentaInexistente() {
        assertTrue(repository.findBalanceById(-1L).isEmpty());
    }
}