package com.bank.bank_legacy.processor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

import com.bank.bank_legacy.exception.InvalidAnnualAccountException;
import com.bank.bank_legacy.model.AnnualAccountEntry;
import com.bank.bank_legacy.model.RawAnnualAccount;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class AnnualAccountProcessor
        implements ItemProcessor<RawAnnualAccount, AnnualAccountEntry> {

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
    public AnnualAccountEntry process(RawAnnualAccount item) {

        Long cuentaId = parseCuentaId(item.getCuentaId());
        LocalDate fecha = parseFecha(item.getFecha());
        String transaccion = parseTransaccion(item.getTransaccion());
        BigDecimal monto = parseMonto(item.getMonto());
        String descripcion = parseDescripcion(item.getDescripcion());

        return new AnnualAccountEntry(
                cuentaId,
                fecha,
                transaccion,
                monto,
                descripcion);
    }

    private Long parseCuentaId(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidAnnualAccountException(
                    "El id de cuenta está vacío");
        }

        try {
            long cuentaId = Long.parseLong(value.trim());

            if (cuentaId <= 0) {
                throw new InvalidAnnualAccountException(
                        "El id de cuenta debe ser mayor que cero: " + value);
            }

            return cuentaId;

        } catch (NumberFormatException e) {
            throw new InvalidAnnualAccountException(
                    "El id de cuenta no es numérico: " + value);
        }
    }

    private LocalDate parseFecha(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidAnnualAccountException(
                    "La fecha está vacía");
        }

        String fecha = value.trim();

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(fecha, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new InvalidAnnualAccountException(
                "Fecha inválida: " + value);
    }

    private String parseTransaccion(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidAnnualAccountException(
                    "El tipo de transacción está vacío");
        }

        String transaccion = Normalizer
                .normalize(
                        value.trim().toLowerCase(),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        if (!transaccion.equals("deposito")
                && !transaccion.equals("retiro")
                && !transaccion.equals("compra")
                && !transaccion.equals("pago")) {

            throw new InvalidAnnualAccountException(
                    "Tipo de transacción inválido: " + value);
        }

        return transaccion;
    }

    private BigDecimal parseMonto(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidAnnualAccountException(
                    "El monto está vacío");
        }

        try {
            return new BigDecimal(value.trim());

        } catch (NumberFormatException e) {
            throw new InvalidAnnualAccountException(
                    "El monto no es numérico: " + value);
        }
    }

    private String parseDescripcion(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidAnnualAccountException(
                    "La descripción está vacía");
        }

        return value.trim();
    }
}