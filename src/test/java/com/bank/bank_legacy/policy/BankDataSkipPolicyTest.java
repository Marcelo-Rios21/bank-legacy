package com.bank.bank_legacy.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankDataSkipPolicyTest {

    @Test
    void permiteOmitirErrorConocidoBajoElLimite() {
        BankDataSkipPolicy policy =
                new BankDataSkipPolicy(IllegalArgumentException.class, 3);

        assertTrue(policy.shouldSkip(
                new IllegalArgumentException("Dato invalido"), 0));
    }

    @Test
    void rechazaErrorNoConfigurado() {
        BankDataSkipPolicy policy =
                new BankDataSkipPolicy(IllegalArgumentException.class, 3);

        assertFalse(policy.shouldSkip(
                new IllegalStateException("Error inesperado"), 0));
    }

    @Test
    void rechazaErrorCuandoSeAlcanzaElLimite() {
        BankDataSkipPolicy policy =
                new BankDataSkipPolicy(IllegalArgumentException.class, 3);

        assertFalse(policy.shouldSkip(
                new IllegalArgumentException("Dato invalido"), 3));
    }
}