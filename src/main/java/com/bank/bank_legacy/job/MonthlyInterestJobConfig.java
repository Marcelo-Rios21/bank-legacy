package com.bank.bank_legacy.job;

import javax.sql.DataSource;

import com.bank.bank_legacy.exception.InvalidInterestException;
import com.bank.bank_legacy.policy.BankDataSkipPolicy;
import com.bank.bank_legacy.model.MonthlyInterest;
import com.bank.bank_legacy.model.RawInterest;
import com.bank.bank_legacy.processor.MonthlyInterestProcessor;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MonthlyInterestJobConfig {

    @Bean
    public FlatFileItemReader<RawInterest> monthlyInterestReader() {

        return new FlatFileItemReaderBuilder<RawInterest>()
                .name("monthlyInterestReader")
                .resource(new FileSystemResource(
                        "data/semana_2/intereses.csv"))
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
    public Step monthlyInterestStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<RawInterest> monthlyInterestReader,
            MonthlyInterestProcessor monthlyInterestProcessor,
            JdbcBatchItemWriter<MonthlyInterest> monthlyInterestWriter,
            AsyncTaskExecutor batchTaskExecutor) {

        return new StepBuilder("monthlyInterestStep", jobRepository)
                .<RawInterest, MonthlyInterest>chunk(5)
                .reader(monthlyInterestReader)
                .processor(monthlyInterestProcessor)
                .writer(monthlyInterestWriter)
                .transactionManager(transactionManager)
                .taskExecutor(batchTaskExecutor)
                .faultTolerant()
                .skipPolicy(new BankDataSkipPolicy(
                        InvalidInterestException.class,
                        10))
                .build();
    }

    @Bean
    public Job monthlyInterestJob(
            JobRepository jobRepository,
            Step monthlyInterestStep) {

        return new JobBuilder("monthlyInterestJob", jobRepository)
                .start(monthlyInterestStep)
                .build();
    }
}