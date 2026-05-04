package com.payment.transaction_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WalletTransferRequest {
    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private String description;
}
