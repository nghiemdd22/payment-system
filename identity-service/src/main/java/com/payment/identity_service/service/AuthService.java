package com.payment.identity_service.service;

import com.payment.identity_service.dto.LoginRequest;
import com.payment.identity_service.dto.RegisterRequest;
import com.payment.identity_service.entity.RefreshToken;
import com.payment.identity_service.entity.Role;
import com.payment.identity_service.entity.User;
import com.payment.identity_service.repository.RefreshTokenRepository;
import com.payment.identity_service.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // Tự động tạo Constructor cho các biến final
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public String register(RegisterRequest request) {
        // 1. Kiểm tra trùng lặp
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // 2. Tạo đối tượng User mới và băm mật khẩu
        // Trong hàm register của AuthService.java
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                // Chuyển chữ gửi lên thành Enum, nếu không gửi mặc định là USER
                .role(request.getRole() != null ? Role.valueOf(request.getRole().toUpperCase()) : Role.USER)
                .status("ACTIVE")
                .build();

        // 3. Lưu xuống Database
        if (user != null) {
            userRepository.save(user);
        }

        return "Đăng ký tài khoản thành công!";
    }

    // Trong AuthService.java
    @Transactional
    public Map<String, String> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Sai thông tin!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Sai thông tin!");
        }

        // Xóa Refresh Token cũ của user này nếu có (để tránh lãng phí db)
        refreshTokenRepository.deleteByUser(user);

        // Tạo mới cặp đôi Access & Refresh
        String accessToken = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiryDate(Instant.now().plus(200, ChronoUnit.DAYS)) // Refresh token sống 200 ngày
                .build();
        if (rt != null) {
            refreshTokenRepository.save(rt);
        }

        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    public String refreshToken(String requestRefreshToken) {
        // 1. Tìm token trong bảng refresh_tokens
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token không tồn tại!"));

        // 2. Kiểm tra xem đã hết hạn 200 ngày chưa
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh Token đã hết hạn, vui lòng đăng nhập lại!");
        }

        // 3. Nếu ổn, in một cái Access Token mới cho user đó
        return jwtService.generateToken(token.getUser());
    }
}