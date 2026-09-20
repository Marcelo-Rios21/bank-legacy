package com.bank.microservices.movement.service;

import java.util.List;

import com.bank.microservices.movement.data.MovementData;
import com.bank.microservices.movement.dto.MovementResponse;
import com.bank.microservices.movement.repository.MovementRepository;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class MovementService {

    private static final String CIRCUIT_BREAKER = "movementDatabase";

    private final MovementRepository repository;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public MovementService(
            MovementRepository repository,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.repository = repository;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public List<MovementResponse> findByAccountId(long cuentaId) {

        List<MovementData> movements = circuitBreakerFactory
                .create(CIRCUIT_BREAKER)
                .run(
                        () -> repository.findByAccountId(cuentaId),
                        throwable -> {
                            throw new ResponseStatusException(
                                    SERVICE_UNAVAILABLE,
                                    "Servicio de movimientos temporalmente no disponible",
                                    throwable);
                        });

        return movements.stream()
                .map(movement -> new MovementResponse(
                        movement.movimientoId(),
                        movement.cuentaId(),
                        movement.fecha(),
                        movement.transaccion(),
                        movement.monto(),
                        movement.descripcion()))
                .toList();
    }
}