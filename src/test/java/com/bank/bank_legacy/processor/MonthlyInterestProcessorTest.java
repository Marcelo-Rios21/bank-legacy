package com.bank.bank_legacy.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.bank.bank_legacy.exception.InvalidInterestException;
import com.bank.bank_legacy.model.MonthlyInterest;
import com.bank.bank_legacy.model.RawInterest;

import org.junit.jupiter.api.Test;

class MonthlyInterestProcessorTest {

    private final MonthlyInterestProcessor processor =
            new MonthlyInterestProcessor();

    @Test
    void calculaInteresDeAhorro() throws Exception {

        RawInterest raw = new RawInterest();
        raw.setCuentaId("101");
        raw.setNombre("John Doe");
        raw.setSaldo("5000");
        raw.setEdad("30");
        raw.setTipo("ahorro");

        MonthlyInterest resultado = processor.process(raw);

        assertEquals(101L, resultado.getCuentaId());
        assertEquals(
                new BigDecimal("50.00"),
                resultado.getInteres());
        assertEquals(
                new BigDecimal("5050.00"),
                resultado.getSaldoFinal());
    }

    @Test
    void rechazaTipoNoPermitido() {

        RawInterest raw = new RawInterest();
        raw.setCuentaId("105");
        raw.setNombre("Charlie Green");
        raw.setSaldo("7000");
        raw.setEdad("35");
        raw.setTipo("hipoteca");

        assertThrows(
                InvalidInterestException.class,
                () -> processor.process(raw));
    }
}