package com.payment.transaction_service.strategy;

import com.payment.transaction_service.entity.TransactionRequest;
import com.payment.transaction_service.event.TransferRequestedEvent;
import com.payment.transaction_service.kafka.TransferEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class BusinessAsyncTransactionStrategy implements TransactionStrategy {

    private final TransferEventProducer transferEventProducer;

    @Override
    public boolean supports(String walletType) {
        return "BUSINESS".equalsIgnoreCase(walletType);
    }

    @Override
    public String process(TransactionRequest transactionRequest) {
        TransferRequestedEvent event = TransferRequestedEvent.builder()
                .transactionId(transactionRequest.getId())
                .fromWalletId(transactionRequest.getFromWalletId())
                .toWalletId(transactionRequest.getToWalletId())
                .amount(transactionRequest.getAmount())
                .idempotencyKey(transactionRequest.getIdempotencyKey())
                .type(transactionRequest.getType())
                .description("BUSINESS transfer from transaction-service")
                .build();

        transferEventProducer.sendTransferRequested(event);
        log.info("Published transfer requested event: {}", transactionRequest.getId());

        return "Giao dịch " + transactionRequest.getId() + " da duoc tiep nhan va cho xu ly bat dong bo.";
    }
}
