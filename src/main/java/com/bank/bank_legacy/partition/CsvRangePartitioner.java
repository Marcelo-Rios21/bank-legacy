package com.bank.bank_legacy.partition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

public class CsvRangePartitioner implements Partitioner {

    private static final Logger log =
            LoggerFactory.getLogger(CsvRangePartitioner.class);

    private final int totalItems;

    public CsvRangePartitioner(int totalItems) {
        this.totalItems = totalItems;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        if (gridSize <= 0) {
            throw new IllegalArgumentException(
                    "gridSize debe ser mayor que cero");
        }

        int partitionCount = Math.min(gridSize, totalItems);

        int baseSize = totalItems / partitionCount;
        int remainder = totalItems % partitionCount;

        Map<String, ExecutionContext> partitions =
                new LinkedHashMap<>();

        int startItem = 0;

        for (int i = 0; i < partitionCount; i++) {

            int size = baseSize + (i < remainder ? 1 : 0);
            int endItem = startItem + size;

            ExecutionContext context = new ExecutionContext();

            context.putInt("startItem", startItem);
            context.putInt("endItem", endItem);
            context.putInt("partitionNumber", i);

            partitions.put("partition" + i, context);

            log.info(
                    "[PARTICION] partition{} procesa items {}-{}",
                    i,
                    startItem + 1,
                    endItem);

            startItem = endItem;
        }

        return partitions;
    }
}
