package com.bank.microservices.transaction.controller;

import com.bank.microservices.transaction.dto.TransactionResponse;
import com.bank.microservices.transaction.service.TransactionService;
import com.bank.microservices.transaction.service.TransactionEventPublisher;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;
    private final TransactionEventPublisher publisher;

    public TransactionController(
            TransactionService service,
            TransactionEventPublisher publisher) {
        this.service = service;
        this.publisher = publisher;
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable long id) {
        publisher.publish(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(@PathVariable long id) {
        return service.findById(id);
    }
}