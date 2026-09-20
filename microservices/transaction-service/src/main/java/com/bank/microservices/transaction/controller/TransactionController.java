package com.bank.microservices.transaction.controller;

import com.bank.microservices.transaction.dto.TransactionResponse;
import com.bank.microservices.transaction.service.TransactionService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(@PathVariable long id) {
        return service.findById(id);
    }
}