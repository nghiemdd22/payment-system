package com.payment.identity_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.identity_service.entity.RefreshToken;
import com.payment.identity_service.entity.User;

import jakarta.transaction.Transactional;

// Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Transactional
    void deleteByUser(User user); // Dùng để xóa token cũ khi đăng nhập mới
}