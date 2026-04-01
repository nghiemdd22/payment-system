package com.payment.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    // Hàm này sẽ moi địa chỉ IP của người dùng ra để làm chìa khóa đếm số lần truy
    // cập
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress()
                        .getHostAddress());
    }
}