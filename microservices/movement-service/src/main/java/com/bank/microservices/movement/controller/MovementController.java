package com.bank.microservices.movement.controller;

import java.util.List;

import com.bank.microservices.movement.dto.MovementResponse;
import com.bank.microservices.movement.service.MovementService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService service;

    public MovementController(MovementService service) {
        this.service = service;
    }

    @GetMapping("/accounts/{cuentaId}")
    public List<MovementResponse> findByAccountId(
            @PathVariable long cuentaId) {

        return service.findByAccountId(cuentaId);
    }
}