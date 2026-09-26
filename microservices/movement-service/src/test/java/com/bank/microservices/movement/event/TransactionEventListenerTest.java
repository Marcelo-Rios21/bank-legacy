package com.bank.microservices.movement.event;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionEventListenerTest {

    private final TransactionEventListener listener =
            new TransactionEventListener();

    @Test
    void procesaEventoValido() {
        var event = new TransactionSnapshotPublished(
                17L, LocalDate.of(2026, 9, 26),
                new BigDecimal("100.00"), "CARGO");

        var record = new ConsumerRecord<>(
                "bank.transactions", 0, 0L, "17", event);

        assertDoesNotThrow(() -> listener.process(record));
    }

    @Test
    void rechazaEventoInvalido() {
        var event = new TransactionSnapshotPublished(
                null, LocalDate.of(2026, 9, 26),
                new BigDecimal("100.00"), "CARGO");

        var record = new ConsumerRecord<>(
                "bank.transactions", 0, 0L, "17", event);

        assertThrows(
                IllegalArgumentException.class,
                () -> listener.process(record));
    }
}
