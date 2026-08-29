package com.bank.bank_legacy.writer;

import java.util.concurrent.atomic.AtomicBoolean;

import com.bank.bank_legacy.exception.TransientBankException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class TransientFailureItemWriter<T>
        implements ItemWriter<T> {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TransientFailureItemWriter.class);

    private final ItemWriter<T> delegate;
    private final boolean enabled;

    private final AtomicBoolean failureTriggered =
            new AtomicBoolean(false);

    private final AtomicBoolean recoveryLogged =
            new AtomicBoolean(false);

    public TransientFailureItemWriter(
            ItemWriter<T> delegate,
            boolean enabled) {

        this.delegate = delegate;
        this.enabled = enabled;
    }

    @Override
    public void write(
            Chunk<? extends T> chunk)
            throws Exception {

        if (enabled
                && failureTriggered.compareAndSet(
                        false,
                        true)) {

            log.warn(
                    "[RETRY DEMO] Fallo transitorio simulado. "
                            + "El chunk debe reintentarse.");

            throw new TransientBankException(
                    "Fallo transitorio controlado");
        }

        if (enabled
                && failureTriggered.get()
                && recoveryLogged.compareAndSet(
                        false,
                        true)) {

            log.info(
                    "[RETRY DEMO] Reintento exitoso. "
                            + "El procesamiento continua.");
        }

        delegate.write(chunk);
    }
}