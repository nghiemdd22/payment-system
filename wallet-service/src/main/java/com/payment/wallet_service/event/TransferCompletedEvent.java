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

    private Long transactionId;
    private String status;
    private String message;
    private String idempotencyKey;
}
