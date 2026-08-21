package com.bank.bank_legacy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchScalingConfig {

    @Bean
    public AsyncTaskExecutor batchTaskExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setThreadNamePrefix("batch-worker-");
        executor.setDaemon(true);
        executor.initialize();

        return executor;
    }
}