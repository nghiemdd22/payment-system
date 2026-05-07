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
public class TransferCompletedEvent {

    // Mã giao dịch trong transaction-service
    private Long transactionId;
    // Trạng thái xử lý: SUCCESS hoặc FAILED
    private String status;
    // Thông điệp kết quả
    private String message;
    // Idempotency key để truy vết trạng thái
    private String idempotencyKey;
    // Có cần kích hoạt rollback không
    private boolean rollbackRequired;
}
