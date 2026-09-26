package com.bank.microservices.transaction.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.bank.microservices.transaction.dto.TransactionResponse;
import com.bank.microservices.transaction.event.TransactionSnapshotPublished;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionEventPublisher {

    private final TransactionService transactionService;
    private final KafkaTemplate<String, TransactionSnapshotPublished> kafkaTemplate;
    private final String topic;

    public TransactionEventPublisher(
            TransactionService transactionService,
            KafkaTemplate<String, TransactionSnapshotPublished> kafkaTemplate,
            @Value("${bank.kafka.topic}") String topic) {

        this.transactionService = transactionService;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(long id) {
        TransactionResponse transaction = transactionService.findById(id);

        var event = new TransactionSnapshotPublished(
                transaction.id(),
                transaction.fecha(),
                transaction.monto(),
                transaction.tipo());

        try {
            kafkaTemplate.send(topic, String.valueOf(id), event)
                    .get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Publicacion interrumpida",
                    ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo confirmar la publicacion en Kafka",
                    ex);
        }
    }
}
