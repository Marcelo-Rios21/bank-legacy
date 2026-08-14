package com.bank.bank_legacy.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.bank.bank_legacy.exception.InvalidInterestException;
import com.bank.bank_legacy.model.MonthlyInterest;
import com.bank.bank_legacy.model.RawInterest;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class MonthlyInterestProcessor
        implements ItemProcessor<RawInterest, MonthlyInterest> {

    private static final BigDecimal TASA_AHORRO =
            new BigDecimal("0.01");

    private static final BigDecimal TASA_PRESTAMO =
            new BigDecimal("0.02");

    @Override
    public MonthlyInterest process(RawInterest item) {

        Long cuentaId = parseCuentaId(item.getCuentaId());
        String nombre = parseNombre(item.getNombre());
        BigDecimal saldo = parseSaldo(item.getSaldo());
        Integer edad = parseEdad(item.getEdad());
        String tipo = parseTipo(item.getTipo());

        BigDecimal tasa = tipo.equals("ahorro")
                ? TASA_AHORRO
                : TASA_PRESTAMO;

        BigDecimal interes = saldo
                .multiply(tasa)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal saldoFinal = saldo
                .add(interes)
                .setScale(2, RoundingMode.HALF_UP);

        return new MonthlyInterest(
                cuentaId,
                nombre,
                saldo,
                edad,
                tipo,
                interes,
                saldoFinal);
    }

    private Long parseCuentaId(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidInterestException(
                    "El id de cuenta estÃƒÂ¡ vacÃƒÂ­o");
        }

        try {
            long cuentaId = Long.parseLong(value.trim());

            if (cuentaId <= 0) {
                throw new InvalidInterestException(
                        "El id de cuenta debe ser mayor que cero: " + value);
            }

            return cuentaId;

        } catch (NumberFormatException e) {
            throw new InvalidInterestException(
                    "El id de cuenta no es numÃƒÂ©rico: " + value);
        }
    }

    private String parseNombre(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidInterestException(
                    "El nombre estÃƒÂ¡ vacÃƒÂ­o");
        }

        return value.trim();
    }

    private BigDecimal parseSaldo(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidInterestException(
                    "El saldo estÃƒÂ¡ vacÃƒÂ­o");
        }

        try {
            BigDecimal saldo = new BigDecimal(value.trim());

            if (saldo.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidInterestException(
                        "El saldo no puede ser negativo: " + value);
            }

            return saldo;

        } catch (NumberFormatException e) {
            throw new InvalidInterestException(
                    "El saldo no es numÃƒÂ©rico: " + value);
        }
    }

    private Integer parseEdad(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidInterestException(
                    "La edad estÃƒÂ¡ vacÃƒÂ­a");
        }

        try {
            int edad = Integer.parseInt(value.trim());

            if (edad <= 0 || edad > 100) {
                throw new InvalidInterestException(
                        "Edad fuera de rango: " + value);
            }

            return edad;

        } catch (NumberFormatException e) {
            throw new InvalidInterestException(
                    "La edad no es numÃƒÂ©rica: " + value);
        }
    }

    private String parseTipo(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidInterestException(
                    "El tipo de cuenta estÃƒÂ¡ vacÃƒÂ­o");
        }

        String tipo = value.trim().toLowerCase();

        if (!tipo.equals("ahorro") &&
                !tipo.equals("prestamo")) {

            throw new InvalidInterestException(
                    "Tipo de cuenta invÃƒÂ¡lido: " + value);
        }

        return tipo;
    }
}