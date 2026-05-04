package com.payment.transaction_service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionStrategyResolver {

    private final List<TransactionStrategy> strategies;

    public TransactionStrategy resolve(String walletType) {
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalStateException("Chua dang ky TransactionStrategy nao.");
        }
        for (TransactionStrategy strategy : strategies) {
            if (strategy.supports(walletType)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Loai vi khong duoc ho tro: " + walletType);
    }
}
