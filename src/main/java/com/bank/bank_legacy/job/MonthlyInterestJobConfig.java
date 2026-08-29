package com.bank.bank_legacy.job;

import javax.sql.DataSource;

import com.bank.bank_legacy.exception.InvalidInterestException;
import com.bank.bank_legacy.model.MonthlyInterest;
import com.bank.bank_legacy.model.RawInterest;
import com.bank.bank_legacy.partition.AccountRangeItemReader;
import com.bank.bank_legacy.partition.AccountRangePartitioner;
import com.bank.bank_legacy.policy.BankDataSkipPolicy;
import com.bank.bank_legacy.processor.MonthlyInterestProcessor;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MonthlyInterestJobConfig {

    @Bean
    public Partitioner monthlyInterestPartitioner() {
        return new AccountRangePartitioner(101, 150);
    }

    @Bean
    @StepScope
    public ItemStreamReader<RawInterest> monthlyInterestReader(
            @Value("#{stepExecutionContext['minAccountId']}")
            Integer minAccountId,
            @Value("#{stepExecutionContext['maxAccountId']}")
            Integer maxAccountId,
            @Value("#{stepExecutionContext['partitionNumber']}")
            Integer partitionNumber) {

        FlatFileItemReader<RawInterest> delegate =
                new FlatFileItemReaderBuilder<RawInterest>()
                        .name("monthlyInterestReader-" + partitionNumber)
                        .resource(new FileSystemResource(
                                "data/semana_3/intereses.csv"))
                        .linesToSkip(1)
                        .delimited(delimited -> delimited
                                .delimiter(",")
                                .names(
                                        "cuentaId",
                                        "nombre",
                                        "saldo",
                                        "edad",
                                        "tipo"))
                        .targetType(RawInterest.class)
                        .build();

        return new AccountRangeItemReader(
                delegate,
                minAccountId,
                maxAccountId,
                partitionNumber == 0);
    }

    @Bean
    public JdbcBatchItemWriter<MonthlyInterest> monthlyInterestWriter(
            DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<MonthlyInterest>()
                .dataSource(dataSource)
                .sql("""
                        MERGE /*+ DISABLE_PARALLEL_DML */
                        INTO MONTHLY_INTEREST destino
                        USING (
                            SELECT
                                :cuentaId AS CUENTA_ID,
                                :nombre AS NOMBRE,
                                :saldoInicial AS SALDO_INICIAL,
                                :edad AS EDAD,
                                :tipo AS TIPO,
                                :interes AS INTERES,
                                :saldoFinal AS SALDO_FINAL
                            FROM DUAL
                        ) origen
                        ON (destino.CUENTA_ID = origen.CUENTA_ID)
                        WHEN MATCHED THEN
                            UPDATE SET
                                destino.NOMBRE = origen.NOMBRE,
                                destino.SALDO_INICIAL = origen.SALDO_INICIAL,
                                destino.EDAD = origen.EDAD,
                                destino.TIPO = origen.TIPO,
                                destino.INTERES = origen.INTERES,
                                destino.SALDO_FINAL = origen.SALDO_FINAL
                        WHEN NOT MATCHED THEN
                            INSERT (
                                CUENTA_ID,
                                NOMBRE,
                                SALDO_INICIAL,
                                EDAD,
                                TIPO,
                                INTERES,
                                SALDO_FINAL
                            )
                            VALUES (
                                origen.CUENTA_ID,
                                origen.NOMBRE,
                                origen.SALDO_INICIAL,
                                origen.EDAD,
                                origen.TIPO,
                                origen.INTERES,
                                origen.SALDO_FINAL
                            )
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public Step monthlyInterestCleanupStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource) {

        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(dataSource);

        return new StepBuilder(
                "monthlyInterestCleanupStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    int eliminados =
                            jdbcTemplate.update(
                                    "DELETE FROM MONTHLY_INTEREST");

                    System.out.println(
                            "Carga mensual anterior eliminada: "
                                    + eliminados
                                    + " registro(s)");

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step monthlyInterestWorkerStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("monthlyInterestReader")
            ItemStreamReader<RawInterest> monthlyInterestReader,
            MonthlyInterestProcessor monthlyInterestProcessor,
            @Qualifier("monthlyInterestWriter")
            JdbcBatchItemWriter<MonthlyInterest> monthlyInterestWriter,
            @Value("${batch.scaling.chunk-size:25}")
            int chunkSize,
            @Value("${batch.fault-tolerance.max-skips:1000}")
            long maxSkips) {

        return new StepBuilder(
                "monthlyInterestWorkerStep",
                jobRepository)
                .<RawInterest, MonthlyInterest>chunk(chunkSize)
                .reader(monthlyInterestReader)
                .processor(monthlyInterestProcessor)
                .writer(monthlyInterestWriter)
                .transactionManager(transactionManager)
                .faultTolerant()
                .skipPolicy(new BankDataSkipPolicy(
                        InvalidInterestException.class,
                        maxSkips))
                .build();
    }

    @Bean
    public Step monthlyInterestStep(
            JobRepository jobRepository,
            @Qualifier("monthlyInterestWorkerStep")
            Step monthlyInterestWorkerStep,
            @Qualifier("monthlyInterestPartitioner")
            Partitioner monthlyInterestPartitioner,
            @Qualifier("batchTaskExecutor")
            AsyncTaskExecutor batchTaskExecutor,
            @Value("${batch.scaling.grid-size:4}")
            int gridSize) {

        return new StepBuilder(
                "monthlyInterestStep",
                jobRepository)
                .partitioner(
                        "monthlyInterestWorkerStep",
                        monthlyInterestPartitioner)
                .step(monthlyInterestWorkerStep)
                .gridSize(gridSize)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Job monthlyInterestJob(
            JobRepository jobRepository,
            @Qualifier("monthlyInterestCleanupStep")
            Step monthlyInterestCleanupStep,
            @Qualifier("monthlyInterestStep")
            Step monthlyInterestStep) {

        return new JobBuilder(
                "monthlyInterestJob",
                jobRepository)
                .start(monthlyInterestCleanupStep)
                .next(monthlyInterestStep)
                .build();
    }
}