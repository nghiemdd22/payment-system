package com.payment.transaction_service.strategy;

import com.payment.transaction_service.entity.TransactionRequest;

public interface TransactionStrategy {

    boolean supports(String walletType);

    String process(TransactionRequest transactionRequest);
}
