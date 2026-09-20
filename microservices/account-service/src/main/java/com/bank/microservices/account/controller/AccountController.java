package com.bank.microservices.account.controller;

import com.bank.microservices.account.dto.AccountResponse;
import com.bank.microservices.account.service.AccountService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping("/{cuentaId}")
    public AccountResponse findById(@PathVariable long cuentaId) {
        return service.findById(cuentaId);
    }
}