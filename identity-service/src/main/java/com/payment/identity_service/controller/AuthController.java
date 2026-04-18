package com.payment.identity_service.controller;

import com.payment.identity_service.dto.LoginRequest;
import com.payment.identity_service.dto.RegisterRequest;
import com.payment.identity_service.repository.RefreshTokenRepository;
import com.payment.identity_service.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth") // Đúng với đường dẫn bạn đã định tuyến ở Gateway
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            String message = authService.register(request);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            Map<String, String> tokens = authService.login(request);
            String accessToken = tokens.get("accessToken");
            String refreshToken = tokens.get("refreshToken");

            // 1. Tạo Cookie cho Access Token (Hết hạn sau 15 phút)
            Cookie accessCookie = new Cookie("access_token", accessToken);
            accessCookie.setHttpOnly(true); // Chốt chặn quan trọng nhất: JS không đọc được
            accessCookie.setSecure(false); // Đặt là true nếu chạy HTTPS, đang chạy Local thì để false
            accessCookie.setPath("/"); // Cookie có hiệu lực cho toàn bộ domain
            accessCookie.setMaxAge(200 * 24 * 60 * 60); // 200 ngày
            response.addCookie(accessCookie);

            // 2. Tạo Cookie cho Refresh Token (Hết hạn sau 200 ngày)
            Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(200 * 24 * 60 * 60); // 200 ngày
            response.addCookie(refreshCookie);

            return ResponseEntity.ok("Đăng nhập thành công! Token đã được lưu vào Cookie.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refresh_token") String refreshToken,
            HttpServletResponse response) {
        try {
            String newAccessToken = authService.refreshToken(refreshToken);

            // Ghi đè Access Token mới vào Cookie
            Cookie accessCookie = new Cookie("access_token", newAccessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(200 * 24 * 60 * 60); // 200 ngày
            response.addCookie(accessCookie);

            return ResponseEntity.ok("Đã gia hạn thẻ thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            // Xóa trong DB
            refreshTokenRepository.findByToken(refreshToken).ifPresent(refreshTokenRepository::delete);
        }

        // Xóa Cookie bằng cách cho MaxAge = 0
        Cookie cookie = new Cookie("access_token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok("Đã đăng xuất và hủy thẻ thành công!");
    }

}
