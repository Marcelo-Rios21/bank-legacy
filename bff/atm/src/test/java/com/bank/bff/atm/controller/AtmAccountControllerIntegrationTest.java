package com.bank.bff.atm.controller;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AtmAccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void debeConsultarSaldoConRespuestaMinima() throws Exception {
        Long cuentaId = buscarCuentaConSaldo();

        mockMvc.perform(get("/api/atm/accounts/{cuentaId}/balance", cuentaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentaId").value(cuentaId))
                .andExpect(jsonPath("$.saldoDisponible").exists())
                .andExpect(jsonPath("$.nombre").doesNotExist())
                .andExpect(jsonPath("$.tipo").doesNotExist());
    }

    @Test
    void debeRealizarRetiroActualizarSaldoYRegistrarMovimiento() throws Exception {
        Long cuentaId = buscarCuentaConSaldo();
        BigDecimal saldoInicial = buscarSaldo(cuentaId);
        BigDecimal monto = BigDecimal.ONE;
        BigDecimal saldoEsperado = saldoInicial.subtract(monto);

        Integer movimientosAntes = contarRetirosAtm(cuentaId);

        mockMvc.perform(post("/api/atm/accounts/{cuentaId}/withdrawals", cuentaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "monto": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentaId").value(cuentaId))
                .andExpect(jsonPath("$.montoRetirado").value(1))
                .andExpect(jsonPath("$.saldoDisponible").isNumber());

        BigDecimal saldoDespues = buscarSaldo(cuentaId);
        Integer movimientosDespues = contarRetirosAtm(cuentaId);
        BigDecimal ultimoMonto = buscarUltimoMontoRetiroAtm(cuentaId);

        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                saldoEsperado.compareTo(saldoDespues));

        org.junit.jupiter.api.Assertions.assertEquals(
                movimientosAntes + 1,
                movimientosDespues);

        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                BigDecimal.ONE.negate().compareTo(ultimoMonto));
    }

    @Test
    void debeRechazarMontoCero() throws Exception {
        Long cuentaId = buscarCuentaConSaldo();

        mockMvc.perform(post("/api/atm/accounts/{cuentaId}/withdrawals", cuentaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "monto": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debeRechazarSaldoInsuficiente() throws Exception {
        Long cuentaId = buscarCuentaConSaldo();
        BigDecimal saldo = buscarSaldo(cuentaId);
        BigDecimal montoExcesivo = saldo.add(BigDecimal.ONE);

        mockMvc.perform(post("/api/atm/accounts/{cuentaId}/withdrawals", cuentaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "monto": %s
                                }
                                """.formatted(montoExcesivo.toPlainString())))
                .andExpect(status().isConflict());
    }

    @Test
    void debeRetornar404ParaCuentaInexistente() throws Exception {
        mockMvc.perform(get("/api/atm/accounts/-1/balance"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/atm/accounts/-1/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "monto": 1
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    private Long buscarCuentaConSaldo() {
        return jdbcClient.sql("""
                SELECT CUENTA_ID
                FROM MONTHLY_INTEREST
                WHERE SALDO_FINAL > 1
                ORDER BY CUENTA_ID
                FETCH FIRST 1 ROWS ONLY
                """)
                .query(Long.class)
                .single();
    }

    private BigDecimal buscarSaldo(long cuentaId) {
        return jdbcClient.sql("""
                SELECT SALDO_FINAL
                FROM MONTHLY_INTEREST
                WHERE CUENTA_ID = :cuentaId
                """)
                .param("cuentaId", cuentaId)
                .query(BigDecimal.class)
                .single();
    }

    private Integer contarRetirosAtm(long cuentaId) {
        return jdbcClient.sql("""
                SELECT COUNT(*)
                FROM ANNUAL_ACCOUNT_ENTRY
                WHERE CUENTA_ID = :cuentaId
                  AND TRANSACCION = 'RETIRO_ATM'
                """)
                .param("cuentaId", cuentaId)
                .query(Integer.class)
                .single();
    }

    private BigDecimal buscarUltimoMontoRetiroAtm(long cuentaId) {
        return jdbcClient.sql("""
                SELECT MONTO
                FROM ANNUAL_ACCOUNT_ENTRY
                WHERE CUENTA_ID = :cuentaId
                  AND TRANSACCION = 'RETIRO_ATM'
                ORDER BY MOVIMIENTO_ID DESC
                FETCH FIRST 1 ROWS ONLY
                """)
                .param("cuentaId", cuentaId)
                .query(BigDecimal.class)
                .single();
    }
}