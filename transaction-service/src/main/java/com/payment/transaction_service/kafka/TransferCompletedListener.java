package com.payment.transaction_service.kafka;

import com.payment.transaction_service.entity.TransactionRequest;
import com.payment.transaction_service.event.TransferCompletedEvent;
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
public class TransferCompletedListener {

    private static final Duration IDEM_TTL = Duration.ofMinutes(10);

    private final TransactionRequestRepository transactionRequestRepository;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "${app.kafka.topics.transfer-completed}")
    public void onTransferCompleted(TransferCompletedEvent event) {
        if (event == null || event.getTransactionId() == null) {
            log.warn("TransferCompletedEvent is empty");
            return;
        }

        TransactionRequest transactionRequest = transactionRequestRepository.findById(event.getTransactionId())
                .orElse(null);
        if (transactionRequest == null) {
            log.warn("Transaction not found for completed event: {}", event.getTransactionId());
            return;
        }

        String status = event.getStatus() == null ? "FAILED" : event.getStatus().toUpperCase();
        transactionRequest.setStatus(status);
        transactionRequestRepository.save(transactionRequest);

        String idempotencyKey = event.getIdempotencyKey() == null
                ? transactionRequest.getIdempotencyKey()
                : event.getIdempotencyKey();
        if (idempotencyKey != null) {
            String redisKey = "idem:key:" + idempotencyKey;
            String redisValue = status + ":" + transactionRequest.getId();
            redisTemplate.opsForValue().set(redisKey, redisValue, IDEM_TTL);
        }

        log.info("Updated transaction {} to status {}", transactionRequest.getId(), status);
    }
}
