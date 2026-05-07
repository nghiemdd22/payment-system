package com.payment.transaction_service.kafka;

import com.payment.transaction_service.entity.TransactionRequest;
import com.payment.transaction_service.event.TransferCompletedEvent;
import com.payment.transaction_service.event.TransferRollbackRequestedEvent;
import com.payment.transaction_service.repository.TransactionRequestRepository;
import com.payment.transaction_service.kafka.TransferRollbackProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferCompletedListener {

    // TTL cho key idempotency trong Redis
    private static final Duration IDEM_TTL = Duration.ofMinutes(10);

    private final TransactionRequestRepository transactionRequestRepository;
    private final StringRedisTemplate redisTemplate;
    private final TransferRollbackProducer rollbackProducer;

    @KafkaListener(topics = "${app.kafka.topics.transfer-completed}")
    public void onTransferCompleted(TransferCompletedEvent event) {
        // Validate event cơ bản để tránh lỗi null
        if (event == null || event.getTransactionId() == null) {
            log.warn("TransferCompletedEvent is empty");
            return;
        }

        // Tìm transaction theo ID từ event
        TransactionRequest transactionRequest = transactionRequestRepository.findById(event.getTransactionId())
                .orElse(null);
        if (transactionRequest == null) {
            log.warn("Transaction not found for completed event: {}", event.getTransactionId());
            return;
        }

        // Cập nhật trạng thái giao dịch theo kết quả xử lý
        String status = event.getStatus() == null ? "FAILED" : event.getStatus().toUpperCase();
        transactionRequest.setStatus(status);
        transactionRequestRepository.save(transactionRequest);

        // Đồng bộ trạng thái vào Redis để idempotency trả về kết quả đúng
        String idempotencyKey = event.getIdempotencyKey() == null
                ? transactionRequest.getIdempotencyKey()
                : event.getIdempotencyKey();
        if (idempotencyKey != null) {
            String redisKey = "idem:key:" + idempotencyKey;
            String redisValue = status + ":" + transactionRequest.getId();
            redisTemplate.opsForValue().set(redisKey, redisValue, IDEM_TTL);
        }

        // Nếu bước sau thất bại và cần hoàn tiền thì gửi yêu cầu rollback
        if (event.isRollbackRequired()) {
            TransferRollbackRequestedEvent rollbackEvent = TransferRollbackRequestedEvent.builder()
                    .transactionId(transactionRequest.getId())
                    .fromWalletId(transactionRequest.getFromWalletId())
                    .toWalletId(transactionRequest.getToWalletId())
                    .amount(transactionRequest.getAmount())
                    .idempotencyKey(idempotencyKey)
                    .reason(event.getMessage())
                    .build();
            rollbackProducer.sendRollbackRequested(rollbackEvent);
            log.info("Rollback requested for transaction {}", transactionRequest.getId());
        }

        log.info("Updated transaction {} to status {}", transactionRequest.getId(), status);
    }
}
