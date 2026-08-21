package com.bank.bank_legacy.policy;

import org.springframework.batch.core.step.skip.SkipPolicy;

public class BankDataSkipPolicy implements SkipPolicy {

    private final Class<? extends Throwable> skippableException;
    private final long maxSkips;

    public BankDataSkipPolicy(
            Class<? extends Throwable> skippableException,
            long maxSkips) {

        this.skippableException = skippableException;
        this.maxSkips = maxSkips;
    }

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {

        boolean knownDataError =
                skippableException.isAssignableFrom(throwable.getClass());

        return knownDataError && skipCount < maxSkips;
    }
}