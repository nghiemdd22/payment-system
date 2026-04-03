package com.payment.identity_service.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String role; // Khách hàng gửi lên chữ "PERSONAL" hoặc "MERCHANT"
}