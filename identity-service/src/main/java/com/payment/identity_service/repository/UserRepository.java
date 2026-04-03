package com.payment.identity_service.repository;

import com.payment.identity_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Hàm tự động sinh ra câu lệnh SQL: SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // Hàm kiểm tra xem user/email đã tồn tại chưa để chặn đăng ký trùng
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}