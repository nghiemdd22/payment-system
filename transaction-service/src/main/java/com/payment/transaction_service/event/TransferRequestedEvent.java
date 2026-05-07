package com.payment.transaction_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestedEvent {

    // Mã giao dịch trong transaction-service
    private Long transactionId;
    // Ví gửi
    private Long fromWalletId;
    // Ví nhận
    private Long toWalletId;
    // Số tiền giao dịch
    private BigDecimal amount;
    // Idempotency key để chống gửi trùng
    private String idempotencyKey;
    // Loại/nguồn giao dịch
    private String type;
    // Mô tả giao dịch
    private String description;
}
