package com.payment.wallet_service.event;

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
public class TransferRollbackRequestedEvent {

    // Mã giao dịch cần rollback
    private Long transactionId;
    // Ví gửi ban đầu
    private Long fromWalletId;
    // Ví nhận ban đầu
    private Long toWalletId;
    // Số tiền cần hoàn
    private BigDecimal amount;
    // Idempotency key để chống gửi trùng
    private String idempotencyKey;
    // Lý do rollback
    private String reason;
}
