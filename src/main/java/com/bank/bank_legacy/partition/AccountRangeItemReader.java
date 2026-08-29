package com.bank.bank_legacy.partition;

import com.bank.bank_legacy.model.RawInterest;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;

public class AccountRangeItemReader
        implements ItemStreamReader<RawInterest> {

    private final FlatFileItemReader<RawInterest> delegate;
    private final int minAccountId;
    private final int maxAccountId;
    private final boolean includeInvalidAccountIds;

    public AccountRangeItemReader(
            FlatFileItemReader<RawInterest> delegate,
            int minAccountId,
            int maxAccountId,
            boolean includeInvalidAccountIds) {

        this.delegate = delegate;
        this.minAccountId = minAccountId;
        this.maxAccountId = maxAccountId;
        this.includeInvalidAccountIds =
                includeInvalidAccountIds;
    }

    @Override
    public RawInterest read() throws Exception {

        RawInterest item;

        while ((item = delegate.read()) != null) {

            String rawAccountId =
                    item.getCuentaId();

            if (rawAccountId == null
                    || rawAccountId.isBlank()) {

                if (includeInvalidAccountIds) {
                    return item;
                }

                continue;
            }

            try {

                int accountId =
                        Integer.parseInt(
                                rawAccountId.trim());

                if (accountId >= minAccountId
                        && accountId <= maxAccountId) {

                    return item;
                }

            } catch (NumberFormatException e) {

                if (includeInvalidAccountIds) {
                    return item;
                }
            }
        }

        return null;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        delegate.open(executionContext);
    }

    @Override
    public void update(
            ExecutionContext executionContext) {

        delegate.update(executionContext);
    }

    @Override
    public void close() {
        delegate.close();
    }
}