package com.payment.transaction_service.kafka;

import com.payment.transaction_service.entity.TransactionRequest;
import com.payment.transaction_service.event.TransferRollbackCompletedEvent;
import com.payment.transaction_service.repository.TransactionRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferRollbackCompletedListener {

    // TTL cho trạng thái idempotency trong Redis
    private static final Duration IDEM_TTL = Duration.ofMinutes(10);

    private final TransactionRequestRepository transactionRequestRepository;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "${app.kafka.topics.transfer-rollback-completed}")
    public void onRollbackCompleted(TransferRollbackCompletedEvent event) {
        // Validate event cơ bản để tránh lỗi null
        if (event == null || event.getTransactionId() == null) {
            log.warn("TransferRollbackCompletedEvent is empty");
            return;
        }

        // Tìm giao dịch theo transactionId
        TransactionRequest transactionRequest = transactionRequestRepository.findById(event.getTransactionId())
                .orElse(null);
        if (transactionRequest == null) {
            log.warn("Transaction not found for rollback event: {}", event.getTransactionId());
            return;
        }

        // SUCCESS -> ROLLBACKED, còn lại giữ FAILED
        String status = event.getStatus() == null ? "FAILED" : event.getStatus().toUpperCase();
        if ("SUCCESS".equals(status)) {
            transactionRequest.setStatus("ROLLBACKED");
        } else {
            transactionRequest.setStatus("FAILED");
        }
        transactionRequestRepository.save(transactionRequest);

        // Đồng bộ trạng thái vào Redis để idempotency trả về kết quả đúng
        String idempotencyKey = event.getIdempotencyKey() == null
                ? transactionRequest.getIdempotencyKey()
                : event.getIdempotencyKey();
        if (idempotencyKey != null) {
            String redisKey = "idem:key:" + idempotencyKey;
            String redisValue = transactionRequest.getStatus() + ":" + transactionRequest.getId();
            redisTemplate.opsForValue().set(redisKey, redisValue, IDEM_TTL);
        }

        log.info("Rollback completed for transaction {} with status {}",
                transactionRequest.getId(), transactionRequest.getStatus());
    }
}
