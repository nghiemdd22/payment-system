package com.payment.wallet_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRollbackCompletedEvent {

    // Mã giao dịch đang được rollback
    private Long transactionId;
    // Trạng thái rollback: SUCCESS hoặc FAILED
    private String status;
    // Thông điệp kết quả rollback
    private String message;
    // Idempotency key để truy vết trạng thái
    private String idempotencyKey;
}
