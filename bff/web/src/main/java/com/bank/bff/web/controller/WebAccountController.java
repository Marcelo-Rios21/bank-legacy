package com.bank.bff.web.controller;

import java.util.List;

import com.bank.bff.web.dto.WebAccountResponse;
import com.bank.bff.web.dto.WebMovementResponse;
import com.bank.bff.web.service.WebAccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/accounts")
public class WebAccountController {

    private final WebAccountService service;

    public WebAccountController(WebAccountService service) {
        this.service = service;
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<WebAccountResponse> findById(
            @PathVariable long cuentaId) {

        return service.findById(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/movements")
    public ResponseEntity<List<WebMovementResponse>> findMovements(
            @PathVariable long cuentaId) {

        if (service.findById(cuentaId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(service.findMovements(cuentaId));
    }
}