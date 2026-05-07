package com.payment.wallet_service.kafka;

import com.payment.wallet_service.dto.request.TransferRequest;
import com.payment.wallet_service.event.TransferCompletedEvent;
import com.payment.wallet_service.event.TransferRequestedEvent;
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
// Listener nhận yêu cầu chuyển tiền async từ Kafka và trả về kết quả xử lý
public class TransferRequestedListener {

    private final WalletService walletService;
    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.transfer-completed}")
    private String transferCompletedTopic;

    @KafkaListener(topics = "${app.kafka.topics.transfer-requested}")
    public void onTransferRequested(TransferRequestedEvent event) {
        // Validate event cơ bản để tránh lỗi null
        if (event == null || event.getTransactionId() == null) {
            log.warn("TransferRequestedEvent is empty");
            return;
        }

        // Chuyển event thành request nội bộ để gọi walletService
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(event.getFromWalletId());
        request.setToWalletId(event.getToWalletId());
        request.setAmount(event.getAmount());
        request.setDescription(event.getDescription() == null ? "BUSINESS transfer" : event.getDescription());

        // Mặc định trạng thái thành công, sẽ đổi nếu có lỗi
        String status = "SUCCESS";
        String message = "Processed";

        // Thực thi giao dịch P2P và bắt lỗi nếu thất bại
        try {
            walletService.transferP2P(event.getTransactionId(), request);
        } catch (Exception ex) {
            status = "FAILED";
            message = ex.getMessage() == null ? "Transfer failed" : ex.getMessage();
            log.error("Async transfer failed: {}", event.getTransactionId(), ex);
        }

        // Tạo event kết quả và gửi ngược về transaction-service
        TransferCompletedEvent completedEvent = TransferCompletedEvent.builder()
                .transactionId(event.getTransactionId())
                .status(status)
                .message(message)
                .idempotencyKey(event.getIdempotencyKey())
                .rollbackRequired(false)
                .build();

        // Dùng transactionId làm key để giữ thứ tự trong cùng partition
        String key = event.getTransactionId().toString();
        kafkaTemplate.send(transferCompletedTopic, key, completedEvent);
    }
}
