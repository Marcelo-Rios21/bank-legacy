package com.bank.bff.mobile.controller;

import java.util.List;

import com.bank.bff.mobile.dto.MobileAccountSummaryResponse;
import com.bank.bff.mobile.dto.MobileMovementResponse;
import com.bank.bff.mobile.service.MobileAccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/accounts")
public class MobileAccountController {

    private final MobileAccountService service;

    public MobileAccountController(MobileAccountService service) {
        this.service = service;
    }

    @GetMapping("/{cuentaId}/summary")
    public ResponseEntity<MobileAccountSummaryResponse> findSummary(
            @PathVariable long cuentaId) {

        return service.findSummaryById(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/movements")
    public ResponseEntity<List<MobileMovementResponse>> findLatestMovements(
            @PathVariable long cuentaId) {

        if (service.findSummaryById(cuentaId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(service.findLatestMovements(cuentaId));
    }
}