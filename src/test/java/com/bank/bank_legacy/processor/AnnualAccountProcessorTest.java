package com.bank.bank_legacy.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.bank.bank_legacy.exception.InvalidAnnualAccountException;
import com.bank.bank_legacy.model.AnnualAccountEntry;
import com.bank.bank_legacy.model.RawAnnualAccount;

import org.junit.jupiter.api.Test;

class AnnualAccountProcessorTest {

    private final AnnualAccountProcessor processor =
            new AnnualAccountProcessor();

    @Test
    void permiteMontoNegativoEnRetiro() throws Exception {

        RawAnnualAccount raw = new RawAnnualAccount();
        raw.setCuentaId("101");
        raw.setFecha("2024-03-15");
        raw.setTransaccion("retiro");
        raw.setMonto("-500");
        raw.setDescripcion("Retiro parcial");

        AnnualAccountEntry resultado = processor.process(raw);

        assertEquals(101L, resultado.getCuentaId());
        assertEquals("retiro", resultado.getTransaccion());
        assertEquals(
                new BigDecimal("-500"),
                resultado.getMonto());
    }

    @Test
    void rechazaDescripcionVacia() {

        RawAnnualAccount raw = new RawAnnualAccount();
        raw.setCuentaId("110");
        raw.setFecha("24-07-2024");
        raw.setTransaccion("retiro");
        raw.setMonto("1500");
        raw.setDescripcion("");

        assertThrows(
                InvalidAnnualAccountException.class,
                () -> processor.process(raw));
    }
}