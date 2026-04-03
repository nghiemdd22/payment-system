package com.payment.identity_service.service;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import com.payment.identity_service.entity.User;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Service
public class JwtService {

    private final KeyPair keyPair;

    // Khi Service này khởi động, nó sẽ tự động rèn ra 1 cặp khóa RSA 2048-bit
    public JwtService() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(2048);
        this.keyPair = keyGenerator.generateKeyPair();
    }

    // Hàm này làm nhiệm vụ in thẻ JWT
    public String generateToken(User user) {
        long expirationTime = 1000 * 60 * 60 * 24 * 200; // 200 ngày

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("id", user.getId()) // Lưu ID vào thẻ
                .claim("role", user.getRole()) // Lưu Role vào thẻ để Gateway Service đọc được
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(keyPair.getPrivate())
                .compact();
    }

    // Hàm này để mốt Gateway gọi sang lấy Public Key về xác thực
    public Object getPublicKey() {
        return keyPair.getPublic();
    }

    // Trong JwtService.java
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(keyPair.getPublic()) // Dùng Public Key để soi chữ ký RSA
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Thẻ giả, thẻ hết hạn hoặc bị sửa đổi sẽ nhảy vào đây
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
}