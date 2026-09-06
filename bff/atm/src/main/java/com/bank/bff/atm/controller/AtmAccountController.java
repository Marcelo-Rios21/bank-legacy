package com.bank.bff.atm.controller;

import com.bank.bff.atm.dto.AtmBalanceResponse;
import com.bank.bff.atm.dto.WithdrawalRequest;
import com.bank.bff.atm.dto.WithdrawalResponse;
import com.bank.bff.atm.service.AtmAccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atm/accounts")
public class AtmAccountController {

    private final AtmAccountService service;

    public AtmAccountController(AtmAccountService service) {
        this.service = service;
    }

    @GetMapping("/{cuentaId}/balance")
    public ResponseEntity<AtmBalanceResponse> findBalance(
            @PathVariable long cuentaId) {

        return service.findBalanceById(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{cuentaId}/withdrawals")
    public ResponseEntity<WithdrawalResponse> withdraw(
            @PathVariable long cuentaId,
            @RequestBody WithdrawalRequest request) {

        return ResponseEntity.ok(
                service.withdraw(cuentaId, request.monto()));
    }
}