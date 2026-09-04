package com.bank.bff.mobile.repository;

import java.util.Optional;

import com.bank.bff.mobile.data.MobileAccountSummaryData;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MobileAccountRepository {

    private final JdbcClient jdbcClient;

    public MobileAccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<MobileAccountSummaryData> findSummaryById(long cuentaId) {
        return jdbcClient.sql("""
                SELECT
                    CUENTA_ID,
                    TIPO,
                    SALDO_FINAL
                FROM MONTHLY_INTEREST
                WHERE CUENTA_ID = :cuentaId
                """)
                .param("cuentaId", cuentaId)
                .query((rs, rowNum) -> new MobileAccountSummaryData(
                        rs.getLong("CUENTA_ID"),
                        rs.getString("TIPO"),
                        rs.getBigDecimal("SALDO_FINAL")))
                .optional();
    }
}