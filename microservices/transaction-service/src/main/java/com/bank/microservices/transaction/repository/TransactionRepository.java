package com.bank.microservices.transaction.repository;

import java.util.Optional;

import com.bank.microservices.transaction.data.TransactionData;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {

    private final JdbcClient jdbcClient;

    public TransactionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<TransactionData> findById(long id) {
        return jdbcClient.sql("""
                SELECT
                    ID,
                    FECHA,
                    MONTO,
                    TIPO
                FROM DAILY_TRANSACTION
                WHERE ID = :id
                """)
                .param("id", id)
                .query((rs, rowNum) -> new TransactionData(
                        rs.getLong("ID"),
                        rs.getDate("FECHA").toLocalDate(),
                        rs.getBigDecimal("MONTO"),
                        rs.getString("TIPO")))
                .optional();
    }
}