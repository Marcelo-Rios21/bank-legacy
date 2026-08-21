package com.bank.bank_legacy.job;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import com.bank.bank_legacy.exception.InvalidAnnualAccountException;
import com.bank.bank_legacy.policy.BankDataSkipPolicy;
import com.bank.bank_legacy.model.AnnualAccountEntry;
import com.bank.bank_legacy.model.RawAnnualAccount;
import com.bank.bank_legacy.processor.AnnualAccountProcessor;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AnnualAccountJobConfig {

    @Bean
    public FlatFileItemReader<RawAnnualAccount> annualAccountReader() {

        return new FlatFileItemReaderBuilder<RawAnnualAccount>()
                .name("annualAccountReader")
                .resource(new FileSystemResource(
                        "data/semana_2/cuentas_anuales.csv"))
                .linesToSkip(1)
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

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return new StepBuilder(
                "annualAccountCleanupStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    int eliminados = jdbcTemplate.update(
                            "DELETE FROM ANNUAL_ACCOUNT_ENTRY");

                    System.out.println(
                            "Carga anual anterior eliminada: "
                                    + eliminados + " registro(s)");

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    public Step annualAccountStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<RawAnnualAccount> annualAccountReader,
            AnnualAccountProcessor annualAccountProcessor,
            JdbcBatchItemWriter<AnnualAccountEntry> annualAccountWriter,
            AsyncTaskExecutor batchTaskExecutor) {

        return new StepBuilder("annualAccountStep", jobRepository)
                .<RawAnnualAccount, AnnualAccountEntry>chunk(5)
                .reader(annualAccountReader)
                .processor(annualAccountProcessor)
                .writer(annualAccountWriter)
                .transactionManager(transactionManager)
                .taskExecutor(batchTaskExecutor)
                .faultTolerant()
                .skipPolicy(new BankDataSkipPolicy(
                        InvalidAnnualAccountException.class,
                        10))
                .build();
    }

    @Bean
    public Step annualAccountAuditStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

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

                    Path outputDirectory = Path.of("output");
                    Files.createDirectories(outputDirectory);

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
            Step annualAccountCleanupStep,
            Step annualAccountStep,
            Step annualAccountAuditStep) {

        return new JobBuilder("annualAccountJob", jobRepository)
                .start(annualAccountCleanupStep)
                .next(annualAccountStep)
                .next(annualAccountAuditStep)
                .build();
    }
}