package com.bank.bank_legacy.processor;

import com.bank.bank_legacy.exception.ControlledRestartException;
import com.bank.bank_legacy.model.DailyTransaction;
import com.bank.bank_legacy.model.RawTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class RestartFailureTransactionProcessor
        implements ItemProcessor<RawTransaction, DailyTransaction> {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RestartFailureTransactionProcessor.class);

    private final DailyTransactionProcessor delegate;
    private final boolean enabled;
    private final long failOnId;

    public RestartFailureTransactionProcessor(
            DailyTransactionProcessor delegate,
            boolean enabled,
            long failOnId) {

        this.delegate = delegate;
        this.enabled = enabled;
        this.failOnId = failOnId;
    }

    @Override
    public DailyTransaction process(
            RawTransaction item) throws Exception {

        if (enabled
                && item.getId() != null
                && Long.toString(failOnId)
                        .equals(item.getId().trim())) {

            log.error(
                    "[RESTART DEMO] Fallo controlado en transaccion {}. "
                            + "La particion debe quedar FAILED.",
                    item.getId());

            throw new ControlledRestartException(
                    "Fallo controlado para demostrar restart");
        }

        return delegate.process(item);
    }
}