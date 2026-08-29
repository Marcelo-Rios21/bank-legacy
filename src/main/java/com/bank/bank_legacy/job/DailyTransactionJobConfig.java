package com.bank.bank_legacy.job;

import javax.sql.DataSource;

import com.bank.bank_legacy.exception.InvalidTransactionException;
import com.bank.bank_legacy.model.DailyTransaction;
import com.bank.bank_legacy.model.RawTransaction;
import com.bank.bank_legacy.partition.CsvRangePartitioner;
import com.bank.bank_legacy.policy.BankDataSkipPolicy;
import com.bank.bank_legacy.processor.DailyTransactionProcessor;
import com.bank.bank_legacy.writer.TransientFailureItemWriter;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DailyTransactionJobConfig {

    @Bean
    public Partitioner dailyTransactionPartitioner(
            @Value("${batch.input.total-items:1000}")
            int totalItems) {

        return new CsvRangePartitioner(totalItems);
    }

    @Bean
    @StepScope
    public FlatFileItemReader<RawTransaction> dailyTransactionReader(
            @Value("#{stepExecutionContext['startItem']}")
            Integer startItem,
            @Value("#{stepExecutionContext['endItem']}")
            Integer endItem,
            @Value("#{stepExecutionContext['partitionNumber']}")
            Integer partitionNumber) {

        return new FlatFileItemReaderBuilder<RawTransaction>()
                .name("dailyTransactionReader-" + partitionNumber)
                .resource(new FileSystemResource(
                        "data/semana_3/transacciones.csv"))
                .linesToSkip(1)
                .currentItemCount(startItem)
                .maxItemCount(endItem)
                .delimited(delimited -> delimited
                        .delimiter(",")
                        .names("id", "fecha", "monto", "tipo"))
                .targetType(RawTransaction.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<DailyTransaction> dailyTransactionWriter(
            DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<DailyTransaction>()
                .dataSource(dataSource)
                .sql("""
                        MERGE /*+ DISABLE_PARALLEL_DML */
                        INTO DAILY_TRANSACTION destino
                        USING (
                            SELECT
                                :id AS ID,
                                :fecha AS FECHA,
                                :monto AS MONTO,
                                :tipo AS TIPO
                            FROM DUAL
                        ) origen
                        ON (destino.ID = origen.ID)
                        WHEN MATCHED THEN
                            UPDATE SET
                                destino.FECHA = origen.FECHA,
                                destino.MONTO = origen.MONTO,
                                destino.TIPO = origen.TIPO
                        WHEN NOT MATCHED THEN
                            INSERT (
                                ID,
                                FECHA,
                                MONTO,
                                TIPO
                            )
                            VALUES (
                                origen.ID,
                                origen.FECHA,
                                origen.MONTO,
                                origen.TIPO
                            )
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public ItemWriter<DailyTransaction> dailyTransactionRetryWriter(
            @Qualifier("dailyTransactionWriter")
            JdbcBatchItemWriter<DailyTransaction> delegate,
            @Value("${batch.fault-tolerance.simulate-transient-failure:false}")
            boolean simulateTransientFailure) {

        return new TransientFailureItemWriter<>(
                delegate,
                simulateTransientFailure);
    }
    @Bean
    public Step dailyTransactionCleanupStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource) {

        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(dataSource);

        return new StepBuilder(
                "dailyTransactionCleanupStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    int eliminados = jdbcTemplate.update(
                            "DELETE FROM DAILY_TRANSACTION");

                    System.out.println(
                            "Carga diaria anterior eliminada: "
                                    + eliminados
                                    + " registro(s)");

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    public Step dailyTransactionWorkerStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("dailyTransactionReader")
            FlatFileItemReader<RawTransaction> dailyTransactionReader,
            DailyTransactionProcessor dailyTransactionProcessor,
            @Qualifier("dailyTransactionRetryWriter")
            ItemWriter<DailyTransaction> dailyTransactionRetryWriter,
            @Value("${batch.scaling.chunk-size:25}")
            int chunkSize,
            @Value("${batch.fault-tolerance.max-skips:1000}")
            long maxSkips,
            @Qualifier("bankRetryPolicy")
            RetryPolicy bankRetryPolicy) {

        return new StepBuilder(
                "dailyTransactionWorkerStep",
                jobRepository)
                .<RawTransaction, DailyTransaction>chunk(chunkSize)
                .reader(dailyTransactionReader)
                .processor(dailyTransactionProcessor)
                .writer(dailyTransactionRetryWriter)
                .transactionManager(transactionManager)
                .faultTolerant()
                .retryPolicy(bankRetryPolicy)
                .skipPolicy(new BankDataSkipPolicy(
                        InvalidTransactionException.class,
                        maxSkips))
                .build();
    }

    @Bean
    public Step dailyTransactionStep(
            JobRepository jobRepository,
            @Qualifier("dailyTransactionWorkerStep")
            Step dailyTransactionWorkerStep,
            @Qualifier("dailyTransactionPartitioner")
            Partitioner dailyTransactionPartitioner,
            @Qualifier("batchTaskExecutor")
            AsyncTaskExecutor batchTaskExecutor,
            @Value("${batch.scaling.grid-size:4}")
            int gridSize) {

        return new StepBuilder(
                "dailyTransactionStep",
                jobRepository)
                .partitioner(
                        "dailyTransactionWorkerStep",
                        dailyTransactionPartitioner)
                .step(dailyTransactionWorkerStep)
                .gridSize(gridSize)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Step dailyTransactionSummaryStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource) {

        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(dataSource);

        return new StepBuilder(
                "dailyTransactionSummaryStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    StepExecution processingStep = chunkContext
                            .getStepContext()
                            .getStepExecution()
                            .getJobExecution()
                            .getStepExecutions()
                            .stream()
                            .filter(step ->
                                    step.getStepName()
                                            .equals(
                                                    "dailyTransactionStep"))
                            .findFirst()
                            .orElseThrow();

                    Long duplicateGroups =
                            jdbcTemplate.queryForObject("""
                                    SELECT COUNT(*)
                                    FROM (
                                        SELECT FECHA, MONTO, TIPO
                                        FROM DAILY_TRANSACTION
                                        GROUP BY FECHA, MONTO, TIPO
                                        HAVING COUNT(*) > 1
                                    )
                                    """, Long.class);

                    Long duplicateRecords =
                            jdbcTemplate.queryForObject("""
                                    SELECT COALESCE(
                                        SUM(CANTIDAD),
                                        0
                                    )
                                    FROM (
                                        SELECT COUNT(*) AS CANTIDAD
                                        FROM DAILY_TRANSACTION
                                        GROUP BY FECHA, MONTO, TIPO
                                        HAVING COUNT(*) > 1
                                    )
                                    """, Long.class);

                    System.out.println();
                    System.out.println(
                            "===== RESUMEN TRANSACCIONES DIARIAS =====");

                    System.out.println(
                            "Total recibidas: "
                                    + processingStep.getReadCount());

                    System.out.println(
                            "Validas persistidas: "
                                    + processingStep.getWriteCount());

                    System.out.println(
                            "Invalidas omitidas: "
                                    + processingStep
                                            .getProcessSkipCount());

                    System.out.println(
                            "Posibles duplicados: "
                                    + duplicateGroups
                                    + " grupo(s), "
                                    + duplicateRecords
                                    + " registro(s)");

                    System.out.println(
                            "==========================================");

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    public Job dailyTransactionJob(
            JobRepository jobRepository,
            @Qualifier("dailyTransactionCleanupStep")
            Step dailyTransactionCleanupStep,
            @Qualifier("dailyTransactionStep")
            Step dailyTransactionStep,
            @Qualifier("dailyTransactionSummaryStep")
            Step dailyTransactionSummaryStep) {

        return new JobBuilder(
                "dailyTransactionJob",
                jobRepository)
                .start(dailyTransactionCleanupStep)
                .next(dailyTransactionStep)
                .next(dailyTransactionSummaryStep)
                .build();
    }
}