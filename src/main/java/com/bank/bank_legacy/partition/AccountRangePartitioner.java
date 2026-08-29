package com.bank.bank_legacy.partition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

public class AccountRangePartitioner implements Partitioner {

    private static final Logger log =
            LoggerFactory.getLogger(AccountRangePartitioner.class);

    private final int minAccountId;
    private final int maxAccountId;

    public AccountRangePartitioner(
            int minAccountId,
            int maxAccountId) {

        this.minAccountId = minAccountId;
        this.maxAccountId = maxAccountId;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        if (gridSize <= 0) {
            throw new IllegalArgumentException(
                    "gridSize debe ser mayor que cero");
        }

        int totalAccounts =
                maxAccountId - minAccountId + 1;

        int partitionCount =
                Math.min(gridSize, totalAccounts);

        int baseSize =
                totalAccounts / partitionCount;

        int remainder =
                totalAccounts % partitionCount;

        Map<String, ExecutionContext> partitions =
                new LinkedHashMap<>();

        int currentMin = minAccountId;

        for (int i = 0; i < partitionCount; i++) {

            int size =
                    baseSize + (i < remainder ? 1 : 0);

            int currentMax =
                    currentMin + size - 1;

            ExecutionContext context =
                    new ExecutionContext();

            context.putInt("minAccountId", currentMin);
            context.putInt("maxAccountId", currentMax);
            context.putInt("partitionNumber", i);

            partitions.put(
                    "partition" + i,
                    context);

            log.info(
                    "[PARTICION] partition{} procesa cuentas {}-{}",
                    i,
                    currentMin,
                    currentMax);

            currentMin = currentMax + 1;
        }

        return partitions;
    }
}