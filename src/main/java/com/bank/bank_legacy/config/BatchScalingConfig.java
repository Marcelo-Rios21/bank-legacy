package com.bank.bank_legacy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchScalingConfig {

    @Bean
    public AsyncTaskExecutor batchTaskExecutor(
            @Value("${batch.scaling.threads:4}") int threads) {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setThreadNamePrefix("partition-worker-");
        executor.setDaemon(true);
        executor.initialize();

        return executor;
    }
}