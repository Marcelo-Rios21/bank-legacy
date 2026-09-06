package com.bank.bff.atm.repository;

import java.math.BigDecimal;
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

    public Optional<AtmBalanceData> findBalanceForUpdateById(long cuentaId) {
        return jdbcClient.sql("""
                SELECT
                    CUENTA_ID,
                    SALDO_FINAL
                FROM MONTHLY_INTEREST
                WHERE CUENTA_ID = :cuentaId
                FOR UPDATE
                """)
                .param("cuentaId", cuentaId)
                .query((rs, rowNum) -> new AtmBalanceData(
                        rs.getLong("CUENTA_ID"),
                        rs.getBigDecimal("SALDO_FINAL")))
                .optional();
    }

    public void updateBalance(long cuentaId, BigDecimal nuevoSaldo) {
        jdbcClient.sql("""
                UPDATE MONTHLY_INTEREST
                SET SALDO_FINAL = :nuevoSaldo
                WHERE CUENTA_ID = :cuentaId
                """)
                .param("nuevoSaldo", nuevoSaldo)
                .param("cuentaId", cuentaId)
                .update();
    }

    public void insertWithdrawal(long cuentaId, BigDecimal monto) {
        jdbcClient.sql("""
                INSERT INTO ANNUAL_ACCOUNT_ENTRY (
                    CUENTA_ID,
                    FECHA,
                    TRANSACCION,
                    MONTO,
                    DESCRIPCION
                )
                VALUES (
                    :cuentaId,
                    CURRENT_DATE,
                    'RETIRO_ATM',
                    :monto,
                    'Retiro realizado por cajero automatico'
                )
                """)
                .param("cuentaId", cuentaId)
                .param("monto", monto.negate())
                .update();
    }
}