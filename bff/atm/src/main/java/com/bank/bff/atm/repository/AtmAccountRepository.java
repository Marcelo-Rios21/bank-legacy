package com.bank.bff.atm.repository;

import java.util.Optional;

import com.bank.bff.atm.data.AtmBalanceData;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AtmAccountRepository {

    private final JdbcClient jdbcClient;

    public AtmAccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<AtmBalanceData> findBalanceById(long cuentaId) {
        return jdbcClient.sql("""
                SELECT
                    CUENTA_ID,
                    SALDO_FINAL
                FROM MONTHLY_INTEREST
                WHERE CUENTA_ID = :cuentaId
                """)
                .param("cuentaId", cuentaId)
                .query((rs, rowNum) -> new AtmBalanceData(
                        rs.getLong("CUENTA_ID"),
                        rs.getBigDecimal("SALDO_FINAL")))
                .optional();
    }
}