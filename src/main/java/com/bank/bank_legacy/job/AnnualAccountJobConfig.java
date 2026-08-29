package com.bank.bank_legacy.job;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import com.bank.bank_legacy.exception.InvalidAnnualAccountException;
import com.bank.bank_legacy.model.AnnualAccountEntry;
import com.bank.bank_legacy.model.RawAnnualAccount;
import com.bank.bank_legacy.partition.CsvRangePartitioner;
import com.bank.bank_legacy.policy.BankDataSkipPolicy;
import com.bank.bank_legacy.processor.AnnualAccountProcessor;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
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
public class AnnualAccountJobConfig {

    @Bean
    public Partitioner annualAccountPartitioner(
            @Value("${batch.input.total-items:1000}") int totalItems) {

        return new CsvRangePartitioner(totalItems);
    }

    @Bean
    @StepScope
    public FlatFileItemReader<RawAnnualAccount> annualAccountReader(
            @Value("#{stepExecutionContext['startItem']}")
            Integer startItem,
            @Value("#{stepExecutionContext['endItem']}")
            Integer endItem,
            @Value("#{stepExecutionContext['partitionNumber']}")
            Integer partitionNumber) {

        return new FlatFileItemReaderBuilder<RawAnnualAccount>()
                .name("annualAccountReader-" + partitionNumber)
                .resource(new FileSystemResource(
                        "data/semana_3/cuentas_anuales.csv"))
                .linesToSkip(1)
                .currentItemCount(startItem)
                .maxItemCount(endItem)
                .delimited(delimited -> delimited
                        .delimiter(",")
                        .names(
                                "cuentaId",
                                "fecha",
                                "transaccion",
                                "monto",
                                "descripcion"))
                .targetType(RawAnnualAccount.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<AnnualAccountEntry> annualAccountWriter(
            DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<AnnualAccountEntry>()
                .dataSource(dataSource)
                .sql("""
                        INSERT /*+ DISABLE_PARALLEL_DML */
                        INTO ANNUAL_ACCOUNT_ENTRY (
                            CUENTA_ID,
                            FECHA,
                            TRANSACCION,
                            MONTO,
                            DESCRIPCION
                        )
                        VALUES (
                            :cuentaId,
                            :fecha,
                            :transaccion,
                            :monto,
                            :descripcion
                        )
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public Step annualAccountCleanupStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource) {

        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(dataSource);

        return new StepBuilder(
                "annualAccountCleanupStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    int eliminados = jdbcTemplate.update(
                            "DELETE FROM ANNUAL_ACCOUNT_ENTRY");

                    System.out.println(
                            "Carga anual anterior eliminada: "
                                    + eliminados
                                    + " registro(s)");

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    public Step annualAccountWorkerStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("annualAccountReader")
            FlatFileItemReader<RawAnnualAccount> annualAccountReader,
            AnnualAccountProcessor annualAccountProcessor,
            @Qualifier("annualAccountWriter")
            JdbcBatchItemWriter<AnnualAccountEntry> annualAccountWriter,
            @Value("${batch.scaling.chunk-size:25}")
            int chunkSize,
            @Value("${batch.fault-tolerance.max-skips:1000}")
            long maxSkips) {

        return new StepBuilder(
                "annualAccountWorkerStep",
                jobRepository)
                .<RawAnnualAccount, AnnualAccountEntry>chunk(chunkSize)
                .reader(annualAccountReader)
                .processor(annualAccountProcessor)
                .writer(annualAccountWriter)
                .transactionManager(transactionManager)
                .faultTolerant()
                .skipPolicy(new BankDataSkipPolicy(
                        InvalidAnnualAccountException.class,
                        maxSkips))
                .build();
    }

    @Bean
    public Step annualAccountStep(
            JobRepository jobRepository,
            @Qualifier("annualAccountWorkerStep")
            Step annualAccountWorkerStep,
            @Qualifier("annualAccountPartitioner")
            Partitioner annualAccountPartitioner,
            @Qualifier("batchTaskExecutor")
            AsyncTaskExecutor batchTaskExecutor,
            @Value("${batch.scaling.grid-size:4}")
            int gridSize) {

        return new StepBuilder(
                "annualAccountStep",
                jobRepository)
                .partitioner(
                        "annualAccountWorkerStep",
                        annualAccountPartitioner)
                .step(annualAccountWorkerStep)
                .gridSize(gridSize)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Step annualAccountAuditStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource) {

        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(dataSource);

        return new StepBuilder(
                "annualAccountAuditStep",
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
                                            .equals("annualAccountStep"))
                            .findFirst()
                            .orElseThrow();

                    Long cuentas = jdbcTemplate.queryForObject("""
                            SELECT COUNT(DISTINCT CUENTA_ID)
                            FROM ANNUAL_ACCOUNT_ENTRY
                            """, Long.class);

                    Long depositos = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM ANNUAL_ACCOUNT_ENTRY
                            WHERE TRANSACCION = 'deposito'
                            """, Long.class);

                    Long retiros = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM ANNUAL_ACCOUNT_ENTRY
                            WHERE TRANSACCION = 'retiro'
                            """, Long.class);

                    Long compras = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM ANNUAL_ACCOUNT_ENTRY
                            WHERE TRANSACCION = 'compra'
                            """, Long.class);

                    Long pagos = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM ANNUAL_ACCOUNT_ENTRY
                            WHERE TRANSACCION = 'pago'
                            """, Long.class);

                    Long montosCero = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM ANNUAL_ACCOUNT_ENTRY
                            WHERE MONTO = 0
                            """, Long.class);

                    Long montosNegativos = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM ANNUAL_ACCOUNT_ENTRY
                            WHERE MONTO < 0
                            """, Long.class);

                    BigDecimal balance = jdbcTemplate.queryForObject("""
                            SELECT COALESCE(SUM(MONTO), 0)
                            FROM ANNUAL_ACCOUNT_ENTRY
                            """, BigDecimal.class);

                    String fechaInicio = jdbcTemplate.queryForObject("""
                            SELECT TO_CHAR(
                                MIN(FECHA),
                                'YYYY-MM-DD'
                            )
                            FROM ANNUAL_ACCOUNT_ENTRY
                            """, String.class);

                    String fechaFin = jdbcTemplate.queryForObject("""
                            SELECT TO_CHAR(
                                MAX(FECHA),
                                'YYYY-MM-DD'
                            )
                            FROM ANNUAL_ACCOUNT_ENTRY
                            """, String.class);

                    String reporte = """
                            ===== REPORTE DE AUDITORIA ANUAL =====
                            Periodo: %s a %s

                            Registros leidos: %d
                            Registros persistidos: %d
                            Registros invalidos omitidos: %d
                            Cuentas distintas: %d

                            Depositos: %d
                            Retiros: %d
                            Compras: %d
                            Pagos: %d

                            Movimientos con monto cero: %d
                            Movimientos con monto negativo: %d
                            Balance neto de movimientos: %s
                            ======================================
                            """.formatted(
                                    fechaInicio,
                                    fechaFin,
                                    processingStep.getReadCount(),
                                    processingStep.getWriteCount(),
                                    processingStep.getProcessSkipCount(),
                                    cuentas,
                                    depositos,
                                    retiros,
                                    compras,
                                    pagos,
                                    montosCero,
                                    montosNegativos,
                                    balance);

                    System.out.println();
                    System.out.println(reporte);

                    Path outputDirectory =
                            Path.of("output");

                    Files.createDirectories(
                            outputDirectory);

                    Files.writeString(
                            outputDirectory.resolve(
                                    "auditoria_anual.txt"),
                            reporte,
                            StandardCharsets.UTF_8);

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    public Job annualAccountJob(
            JobRepository jobRepository,
            @Qualifier("annualAccountCleanupStep")
            Step annualAccountCleanupStep,
            @Qualifier("annualAccountStep")
            Step annualAccountStep,
            @Qualifier("annualAccountAuditStep")
            Step annualAccountAuditStep) {

        return new JobBuilder(
                "annualAccountJob",
                jobRepository)
                .start(annualAccountCleanupStep)
                .next(annualAccountStep)
                .next(annualAccountAuditStep)
                .build();
    }
}