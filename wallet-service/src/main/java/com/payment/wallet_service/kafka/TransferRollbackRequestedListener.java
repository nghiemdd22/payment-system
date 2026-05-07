package com.payment.wallet_service.kafka;

import com.payment.wallet_service.dto.request.TransferRequest;
import com.payment.wallet_service.event.TransferRollbackCompletedEvent;
import com.payment.wallet_service.event.TransferRollbackRequestedEvent;
import com.payment.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
// Listener nhận yêu cầu rollback và thực hiện hoàn tiền cho ví gửi
public class TransferRollbackRequestedListener {

    private final WalletService walletService;
    private final KafkaTemplate<String, TransferRollbackCompletedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.transfer-rollback-completed}")
    private String rollbackCompletedTopic;

    @KafkaListener(topics = "${app.kafka.topics.transfer-rollback-requested}")
    public void onRollbackRequested(TransferRollbackRequestedEvent event) {
        // Validate event cơ bản để tránh lỗi null
        if (event == null || event.getTransactionId() == null) {
            log.warn("TransferRollbackRequestedEvent is empty");
            return;
        }

        // Tạo request hoàn tiền: đảo chiều ví gửi/nhận
        TransferRequest refundRequest = new TransferRequest();
        refundRequest.setFromWalletId(event.getToWalletId());
        refundRequest.setToWalletId(event.getFromWalletId());
        refundRequest.setAmount(event.getAmount());
        refundRequest.setDescription("Hoan tien giao dich loi ID: " + event.getTransactionId());

        // Mặc định rollback thành công, sẽ đổi nếu có lỗi
        String status = "SUCCESS";
        String message = "Rollback completed";

        // Thực thi hoàn tiền và bắt lỗi nếu thất bại
        try {
            walletService.transferP2P(event.getTransactionId(), refundRequest);
        } catch (Exception ex) {
            status = "FAILED";
            message = ex.getMessage() == null ? "Rollback failed" : ex.getMessage();
            log.error("Rollback failed for transaction {}", event.getTransactionId(), ex);
        }

        // Tạo event kết quả rollback và gửi về transaction-service
        TransferRollbackCompletedEvent completedEvent = TransferRollbackCompletedEvent.builder()
                .transactionId(event.getTransactionId())
                .status(status)
                .message(message)
                .idempotencyKey(event.getIdempotencyKey())
                .build();

        // Dùng transactionId làm key để giữ thứ tự trong cùng partition
        String key = event.getTransactionId().toString();
        kafkaTemplate.send(rollbackCompletedTopic, key, completedEvent);
    }
}
