package com.payment.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    // Lấy chuỗi Public Key từ file application.yml
    @Value("${jwt.public-key}")
    private String publicKeyStr;

    // Lưu trữ khóa đã được giải mã trên RAM để tái sử dụng cho mọi request
    private PublicKey publicKey;

    // Hàm này chỉ chạy ĐÚNG 1 LẦN khi Gateway khởi động
    @PostConstruct
    public void init() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.publicKey = kf.generatePublic(spec);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Bỏ qua các đường dẫn không cần xác thực (Login/Register)
        if (path.contains("/api/v1/auth/login") || path.contains("/api/v1/auth/register")
                || path.contains("/api/v1/auth/logout") || path.contains("/api/v1/auth/refresh")) {
            return chain.filter(exchange);
        }

        // 2. Lấy access_token từ Cookie
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("access_token");
        if (cookie == null) {
            return unauthorized(exchange);
        }

        String token = cookie.getValue();

        try {
            // 3. Soi thẻ siêu tốc bằng Public Key đã lưu sẵn trong RAM
            Claims claims = Jwts.parser()
                    .verifyWith(this.publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 4. Nhét thông tin User vào Header để Wallet Service và Transaction Service
            // sau này sử dụng
            exchange.getRequest().mutate()
                    .header("X-User-Id", claims.get("id").toString())
                    .header("X-User-Role", claims.get("role").toString())
                    .build();

        } catch (Exception e) {
            // Nếu token hết hạn, bị sửa đổi, hoặc chữ ký sai thì sẽ nhảy vào đây
            return unauthorized(exchange);
        }

        return chain.filter(exchange);
    }

    // private Mono<Void> unauthorized(ServerWebExchange exchange) {
    // exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    // return exchange.getResponse().setComplete();
    // }
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponse = "{\"code\": 401, \"error\": \"Unauthorized\", \"message\": \"Thẻ JWT không hợp lệ hoặc đã hết hạn!\"}";
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1; // Đảm bảo bộ lọc này chạy trước các bộ lọc khác (như Rate Limiter)
    }
}