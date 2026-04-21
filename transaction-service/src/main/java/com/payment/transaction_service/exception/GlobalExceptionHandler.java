package com.payment.transaction_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateTxn(DuplicateTransactionException ex) {
        // Trả về mã 409 Conflict (Xung đột) thay vì 500 Internal Server Error
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "ERR_409_DUPLICATE",
                "message", ex.getMessage()));
    }
}