package com.bank.microservices.movement.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionEventListener.class);

    @KafkaListener(topics = "${bank.kafka.topic}")
    public void process(ConsumerRecord<String, TransactionSnapshotPublished> record) {
        var event = record.value();

        if (event == null
                || event.id() == null
                || event.id() <= 0
                || event.fecha() == null
                || event.monto() == null
                || event.tipo() == null
                || event.tipo().isBlank()) {
            throw new IllegalArgumentException("Evento de transaccion invalido");
        }

        log.info(
                "Evento procesado: transaccion={}, particion={}, offset={}",
                event.id(),
                record.partition(),
                record.offset());
    }
}
