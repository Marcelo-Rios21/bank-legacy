package com.bank.bff.mobile.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bank.bff.mobile.data.MobileAccountSummaryData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MobileAccountRepositoryIntegrationTest {

    @Autowired
    private MobileAccountRepository repository;

    @Test
    void debeEncontrarResumenDeCuentaExistente() {
        var result = repository.findSummaryById(101L);

        assertTrue(result.isPresent());

        MobileAccountSummaryData account = result.get();

        assertEquals(101L, account.cuentaId());
        assertTrue(account.saldo().signum() >= 0);
    }

    @Test
    void debeRetornarVacioParaCuentaInexistente() {
        assertTrue(repository.findSummaryById(-1L).isEmpty());
    }
}