package com.payment.transaction_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", "ERR_400_BAD_REQUEST",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Tham số '%s' không hợp lệ", ex.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", "ERR_400_INVALID_PARAM",
                "message", message));
    }

    @ExceptionHandler(TransactionProcessingException.class)
    public ResponseEntity<Map<String, String>> handleProcessingError(TransactionProcessingException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "ERR_503_PROCESSING",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", "ERR_500_INTERNAL",
                "message", "Hệ thống đang bận. Vui lòng thử lại sau."));
    }
}