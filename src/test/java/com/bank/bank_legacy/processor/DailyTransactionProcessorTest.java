package com.bank.bank_legacy.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bank.bank_legacy.exception.InvalidTransactionException;
import com.bank.bank_legacy.model.DailyTransaction;
import com.bank.bank_legacy.model.RawTransaction;

import org.junit.jupiter.api.Test;

class DailyTransactionProcessorTest {

    private final DailyTransactionProcessor processor =
            new DailyTransactionProcessor();

    @Test
    void procesaTransaccionValida() throws Exception {

        RawTransaction raw = new RawTransaction();
        raw.setId("1");
        raw.setFecha("2024-01-01");
        raw.setMonto("1000");
        raw.setTipo("debito");

        DailyTransaction resultado = processor.process(raw);

        assertEquals(1L, resultado.getId());
        assertEquals(
                LocalDate.of(2024, 1, 1),
                resultado.getFecha());
        assertEquals(
                new BigDecimal("1000"),
                resultado.getMonto());
        assertEquals("debito", resultado.getTipo());
    }

    @Test
    void rechazaMontoNegativo() {

        RawTransaction raw = new RawTransaction();
        raw.setId("3");
        raw.setFecha("2024-01-03");
        raw.setMonto("-200");
        raw.setTipo("debito");

        assertThrows(
                InvalidTransactionException.class,
                () -> processor.process(raw));
    }
}