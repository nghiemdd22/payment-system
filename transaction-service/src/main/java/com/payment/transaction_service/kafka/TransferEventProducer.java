package com.payment.transaction_service.kafka;

import com.payment.transaction_service.event.TransferRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferEventProducer {

    private final KafkaTemplate<String, TransferRequestedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.transfer-requested}")
    private String transferRequestedTopic;

    public void sendTransferRequested(TransferRequestedEvent event) {
        String key = event.getTransactionId() == null ? null : event.getTransactionId().toString();
        kafkaTemplate.send(transferRequestedTopic, key, event);
    }
}
