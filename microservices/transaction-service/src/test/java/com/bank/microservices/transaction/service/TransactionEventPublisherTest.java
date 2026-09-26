package com.bank.microservices.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import com.bank.microservices.transaction.dto.TransactionResponse;
import com.bank.microservices.transaction.event.TransactionSnapshotPublished;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionEventPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void publicaUnaTransaccionExistente() {
        TransactionService service = mock(TransactionService.class);
        KafkaTemplate<String, TransactionSnapshotPublished> kafka =
                mock(KafkaTemplate.class);

        when(service.findById(17L)).thenReturn(new TransactionResponse(
                17L, LocalDate.of(2026, 9, 26),
                new BigDecimal("100.00"), "CARGO"));

        when(kafka.send(eq("bank.transactions"), eq("17"), any(TransactionSnapshotPublished.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        var publisher = new TransactionEventPublisher(
                service, kafka, "bank.transactions");

        publisher.publish(17L);

        ArgumentCaptor<TransactionSnapshotPublished> captor =
                ArgumentCaptor.forClass(TransactionSnapshotPublished.class);

        verify(kafka).send(eq("bank.transactions"), eq("17"), captor.capture());

        assertEquals(17L, captor.getValue().id());
        assertEquals(new BigDecimal("100.00"), captor.getValue().monto());
        assertEquals("CARGO", captor.getValue().tipo());
    }
}
