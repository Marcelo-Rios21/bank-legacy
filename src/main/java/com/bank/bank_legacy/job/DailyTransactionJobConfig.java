package com.bank.bank_legacy.job;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DailyTransactionJobConfig {

    @Bean
    public Step dailyTransactionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("dailyTransactionStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    System.out.println("Ejecutando proceso de transacciones diarias...");

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    public Job dailyTransactionJob(
            JobRepository jobRepository,
            Step dailyTransactionStep) {

        return new JobBuilder("dailyTransactionJob", jobRepository)
                .start(dailyTransactionStep)
                .build();
    }
}