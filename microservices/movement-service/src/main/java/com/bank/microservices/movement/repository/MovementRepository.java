package com.bank.microservices.movement.repository;

import java.util.List;

import com.bank.microservices.movement.data.MovementData;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MovementRepository {

    private final JdbcClient jdbcClient;

    public MovementRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<MovementData> findByAccountId(long cuentaId) {
        return jdbcClient.sql("""
                SELECT
                    MOVIMIENTO_ID,
                    CUENTA_ID,
                    FECHA,
                    TRANSACCION,
                    MONTO,
                    DESCRIPCION
                FROM ANNUAL_ACCOUNT_ENTRY
                WHERE CUENTA_ID = :cuentaId
                ORDER BY FECHA DESC, MOVIMIENTO_ID DESC
                """)
                .param("cuentaId", cuentaId)
                .query((rs, rowNum) -> new MovementData(
                        rs.getLong("MOVIMIENTO_ID"),
                        rs.getLong("CUENTA_ID"),
                        rs.getDate("FECHA").toLocalDate(),
                        rs.getString("TRANSACCION"),
                        rs.getBigDecimal("MONTO"),
                        rs.getString("DESCRIPCION")))
                .list();
    }
}