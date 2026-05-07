package com.payment.transaction_service.kafka;

import com.payment.transaction_service.event.TransferRollbackRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferRollbackProducer {

    // KafkaTemplate dùng để gửi message rollback
    private final KafkaTemplate<String, TransferRollbackRequestedEvent> kafkaTemplate;

    // Tên topic rollback lấy từ cấu hình
    @Value("${app.kafka.topics.transfer-rollback-requested}")
    private String rollbackRequestedTopic;

    // Gửi yêu cầu rollback lên Kafka
    public void sendRollbackRequested(TransferRollbackRequestedEvent event) {
        // Dùng transactionId làm key để giữ thứ tự trong cùng partition
        String key = event.getTransactionId() == null ? null : event.getTransactionId().toString();
        kafkaTemplate.send(rollbackRequestedTopic, key, event);
    }
}
