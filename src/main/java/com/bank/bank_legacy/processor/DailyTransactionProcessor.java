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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class DailyTransactionProcessor
        implements ItemProcessor<RawTransaction, DailyTransaction> {

    
    private static final Logger log =
            LoggerFactory.getLogger(DailyTransactionProcessor.class);
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

        
        log.info("[PARALELO] Hilo {} procesando transaccion {}",
                Thread.currentThread().getName(),
                item.getId());
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
            throw new InvalidTransactionException("El id estÃƒÆ’Ã‚Â¡ vacÃƒÆ’Ã‚Â­o");
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
                    "El id no es numÃƒÆ’Ã‚Â©rico: " + value
            );
        }
    }

    private LocalDate parseFecha(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException("La fecha estÃƒÆ’Ã‚Â¡ vacÃƒÆ’Ã‚Â­a");
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
                "Fecha invÃƒÆ’Ã‚Â¡lida: " + value
        );
    }

    private BigDecimal parseMonto(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException("El monto estÃƒÆ’Ã‚Â¡ vacÃƒÆ’Ã‚Â­o");
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
                    "El monto no es numÃƒÆ’Ã‚Â©rico: " + value
            );
        }
    }

    private String parseTipo(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException(
                    "El tipo de transacciÃƒÆ’Ã‚Â³n estÃƒÆ’Ã‚Â¡ vacÃƒÆ’Ã‚Â­o"
            );
        }

        String tipo = value.trim().toLowerCase();

        if (!tipo.equals("debito") && !tipo.equals("credito")) {
            throw new InvalidTransactionException(
                    "Tipo de transacciÃƒÆ’Ã‚Â³n invÃƒÆ’Ã‚Â¡lido: " + value
            );
        }

        return tipo;
    }
}