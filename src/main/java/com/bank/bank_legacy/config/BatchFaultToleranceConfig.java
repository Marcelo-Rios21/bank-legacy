package com.bank.bank_legacy.config;

import java.sql.SQLTransientException;
import java.time.Duration;

import com.bank.bank_legacy.exception.TransientBankException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessException;

@Configuration
public class BatchFaultToleranceConfig {

    @Bean
    public RetryPolicy bankRetryPolicy(
            @Value("${batch.fault-tolerance.max-retries:2}")
            long maxRetries,
            @Value("${batch.fault-tolerance.retry-delay-ms:200}")
            long retryDelayMs) {

        return RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(Duration.ofMillis(retryDelayMs))
                .includes(
                        TransientBankException.class,
                        SQLTransientException.class,
                        TransientDataAccessException.class)
                .build();
    }
}