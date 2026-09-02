package com.bank.bff.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bank.bff.web.data.AccountData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WebAccountRepositoryIntegrationTest {

    @Autowired
    private WebAccountRepository repository;

    @Test
    void debeEncontrarCuentaExistente() {
        var result = repository.findById(101L);

        assertTrue(result.isPresent());

        AccountData account = result.get();

        assertEquals(101L, account.cuentaId());
        assertTrue(account.saldoFinal().signum() >= 0);
    }

    @Test
    void debeRetornarVacioParaCuentaInexistente() {
        assertTrue(repository.findById(-1L).isEmpty());
    }
}