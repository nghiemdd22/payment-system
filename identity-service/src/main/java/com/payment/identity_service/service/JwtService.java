package com.payment.identity_service.service;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.payment.identity_service.entity.User;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.private-key}")
    private String privateKeyStr;

    @Value("${jwt.public-key}")
    private String publicKeyStr;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        byte[] privateBytes = Base64.getDecoder().decode(privateKeyStr);
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);

        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateBytes);
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicBytes);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = kf.generatePrivate(privateSpec);
        this.publicKey = kf.generatePublic(publicSpec);
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
                .signWith(this.privateKey)
                .compact();
    }

    // Trong JwtService.java
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(this.publicKey) // Dùng Public Key để soi chữ ký RSA
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
                .verifyWith(this.publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .verifyWith(this.publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
}