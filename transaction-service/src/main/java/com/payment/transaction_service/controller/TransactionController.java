package com.payment.transaction_service.controller;

import com.payment.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/init")
    public ResponseEntity<String> initTxn(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam Long fromWallet,
            @RequestParam Long toWallet,
            @RequestParam BigDecimal amount,
            @RequestParam String type) {

        String response = transactionService.initTransaction(idempotencyKey, fromWallet, toWallet, amount, type);
        return ResponseEntity.ok(response);
    }
}