package com.bank.microservices.account.repository;

import java.util.Optional;

import com.bank.microservices.account.data.AccountData;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private final JdbcClient jdbcClient;

    public AccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<AccountData> findById(long cuentaId) {
        return jdbcClient.sql("""
                SELECT
                    CUENTA_ID,
                    NOMBRE,
                    SALDO_INICIAL,
                    EDAD,
                    TIPO,
                    INTERES,
                    SALDO_FINAL
                FROM MONTHLY_INTEREST
                WHERE CUENTA_ID = :cuentaId
                """)
                .param("cuentaId", cuentaId)
                .query((rs, rowNum) -> new AccountData(
                        rs.getLong("CUENTA_ID"),
                        rs.getString("NOMBRE"),
                        rs.getBigDecimal("SALDO_INICIAL"),
                        rs.getInt("EDAD"),
                        rs.getString("TIPO"),
                        rs.getBigDecimal("INTERES"),
                        rs.getBigDecimal("SALDO_FINAL")))
                .optional();
    }
}