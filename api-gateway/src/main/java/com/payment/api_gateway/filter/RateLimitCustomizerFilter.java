package com.payment.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class RateLimitCustomizerFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();

        // Tạo một "Bức tường chặn" (Decorator) để can thiệp vào kết quả trả về
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> setComplete() {
                // Nếu phát hiện Gateway chuẩn bị sập cửa vì lỗi 429
                if (getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {

                    // 1. Đổi kiểu dữ liệu thành JSON
                    getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    // 2. Viết câu thông báo lỗi tùy chỉnh của bạn
                    String jsonResponse = "{\"code\": 429, \"error\": \"Too Many Requests\", \"message\": \"Bạn đang thao tác quá nhanh. Hệ thống đã chặn để chống DDoS!\"}";

                    // 3. Đóng gói thành DataBuffer (chuẩn của WebFlux)
                    DataBuffer buffer = bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));

                    // 4. Ghi đè body và trả về cho Client
                    return writeWith(Mono.just(buffer));
                }

                // Nếu không phải lỗi 429, cứ để nó đi qua bình thường
                return super.setComplete();
            }
        };

        // Đưa response đã "độ chế" vào chuỗi lọc tiếp theo
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        // Phải chạy đầu tiên (Order âm cao) để bọc cái Response lại trước khi Rate
        // Limiter đụng vào
        return -100;
    }
}