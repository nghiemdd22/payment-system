package com.payment.transaction_service.strategy;

import com.payment.transaction_service.entity.TransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1000)
public class AsyncFallbackTransactionStrategy implements TransactionStrategy {

    @Override
    public boolean supports(String walletType) {
        return true;
    }

    @Override
    public String process(TransactionRequest transactionRequest) {
        log.info("Async fallback for type: {}", transactionRequest.getType());
        return "Giao dịch " + transactionRequest.getId() + " đã được tiếp nhận và chờ xử lý bất đồng bộ.";
    }
}
