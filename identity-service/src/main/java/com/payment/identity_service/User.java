package com.payment.identity_service;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users") // Ánh xạ chính xác vào cái bảng 'users' bạn đã tạo
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String status;
}