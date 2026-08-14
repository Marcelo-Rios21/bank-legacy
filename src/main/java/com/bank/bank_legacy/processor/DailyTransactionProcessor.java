package com.bank.bank_legacy.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

import com.bank.bank_legacy.exception.InvalidTransactionException;
import com.bank.bank_legacy.model.DailyTransaction;
import com.bank.bank_legacy.model.RawTransaction;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class DailyTransactionProcessor
        implements ItemProcessor<RawTransaction, DailyTransaction> {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("uuuu/MM/dd")
                    .withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd-MM-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT)
    );

    @Override
    public DailyTransaction process(RawTransaction item) {

        Long id = parseId(item.getId());
        LocalDate fecha = parseFecha(item.getFecha());
        BigDecimal monto = parseMonto(item.getMonto());
        String tipo = parseTipo(item.getTipo());

        return new DailyTransaction(
                id,
                fecha,
                monto,
                tipo
        );
    }

    private Long parseId(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException("El id está vacío");
        }

        try {
            long id = Long.parseLong(value.trim());

            if (id <= 0) {
                throw new InvalidTransactionException(
                        "El id debe ser mayor que cero: " + value
                );
            }

            return id;

        } catch (NumberFormatException e) {
            throw new InvalidTransactionException(
                    "El id no es numérico: " + value
            );
        }
    }

    private LocalDate parseFecha(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException("La fecha está vacía");
        }

        String fecha = value.trim();

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(fecha, formatter);
            } catch (DateTimeParseException ignored) {
                // Se intenta con el siguiente formato conocido.
            }
        }

        throw new InvalidTransactionException(
                "Fecha inválida: " + value
        );
    }

    private BigDecimal parseMonto(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException("El monto está vacío");
        }

        try {
            BigDecimal monto = new BigDecimal(value.trim());

            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransactionException(
                        "El monto debe ser mayor que cero: " + value
                );
            }

            return monto;

        } catch (NumberFormatException e) {
            throw new InvalidTransactionException(
                    "El monto no es numérico: " + value
            );
        }
    }

    private String parseTipo(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException(
                    "El tipo de transacción está vacío"
            );
        }

        String tipo = value.trim().toLowerCase();

        if (!tipo.equals("debito") && !tipo.equals("credito")) {
            throw new InvalidTransactionException(
                    "Tipo de transacción inválido: " + value
            );
        }

        return tipo;
    }
}