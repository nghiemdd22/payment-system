package com.payment.identity_service.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
public class userC {
    @GetMapping("/user")
    public ResponseEntity<String> getMethodName() {
        return ResponseEntity.ok("Hello, User!");
    }

}
