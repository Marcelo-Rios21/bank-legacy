package com.bank.bff.mobile.repository;

import java.util.List;

import com.bank.bff.mobile.data.MobileMovementData;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MobileMovementRepository {

    private final JdbcClient jdbcClient;

    public MobileMovementRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<MobileMovementData> findLatestByAccountId(long cuentaId) {
        return jdbcClient.sql("""
                SELECT
                    FECHA,
                    TRANSACCION,
                    MONTO
                FROM ANNUAL_ACCOUNT_ENTRY
                WHERE CUENTA_ID = :cuentaId
                ORDER BY FECHA DESC, MOVIMIENTO_ID DESC
                FETCH FIRST 5 ROWS ONLY
                """)
                .param("cuentaId", cuentaId)
                .query((rs, rowNum) -> new MobileMovementData(
                        rs.getDate("FECHA").toLocalDate(),
                        rs.getString("TRANSACCION"),
                        rs.getBigDecimal("MONTO")))
                .list();
    }
}