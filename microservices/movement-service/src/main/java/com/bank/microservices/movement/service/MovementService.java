package com.bank.microservices.movement.service;

import java.util.List;

import com.bank.microservices.movement.dto.MovementResponse;
import com.bank.microservices.movement.repository.MovementRepository;

import org.springframework.stereotype.Service;

@Service
public class MovementService {

    private final MovementRepository repository;

    public MovementService(MovementRepository repository) {
        this.repository = repository;
    }

    public List<MovementResponse> findByAccountId(long cuentaId) {
        return repository.findByAccountId(cuentaId)
                .stream()
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