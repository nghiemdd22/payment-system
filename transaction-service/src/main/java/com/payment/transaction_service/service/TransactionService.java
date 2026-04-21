package com.payment.transaction_service.service;

import com.payment.transaction_service.entity.TransactionRequest;
import com.payment.transaction_service.exception.DuplicateTransactionException;
import com.payment.transaction_service.repository.TransactionRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final StringRedisTemplate redisTemplate;
    private final TransactionRequestRepository repository;

    /**
     * Nhận Request và kiểm tra Lũy đẳng (Idempotency)
     */
    public String initTransaction(String idempotencyKey, Long fromWallet, Long toWallet, BigDecimal amount,
            String type) {
        String redisKey = "idem:key:" + idempotencyKey;

        // 1. Tuyệt chiêu SETNX của Redis (setIfAbsent)
        // Cố gắng lưu key này vào Redis với TTL là 10 phút.
        // Lệnh này chạy ở mức Atomic (nguyên tử) trong Redis -> Chống Race Condition
        // tuyệt đối.
        Boolean isNewRequest = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PENDING", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isNewRequest)) {
            // Key ĐÃ TỒN TẠI -> Đây là request bị bấm đúp hoặc spam!
            log.warn("Chan giao dich trung lap Idempotency-Key: {}", idempotencyKey);
            // throw new RuntimeException("Giao dịch đang được xử lý hoặc đã hoàn thành. Xin
            // đừng bấm đúp!");
            throw new DuplicateTransactionException("Giao dịch đang được xử lý. Xin đừng bấm đúp!");
            // (Thực tế bạn nên quăng ra CustomException và dùng GlobalExceptionHandler để
            // trả về HTTP 409 Conflict)
        }

        log.info("Request mới hợp lệ. Bắt đầu xử lý. Idempotency-Key: {}", idempotencyKey);

        // 2. Lưu trạng thái PENDING xuống Database MySQL
        TransactionRequest newTxn = TransactionRequest.builder()
                .idempotencyKey(idempotencyKey)
                .fromWalletId(fromWallet)
                .toWalletId(toWallet)
                .amount(amount)
                .type(type)
                .status("PENDING")
                .build();

        // repository.save(newTxn);
        try {
            repository.save(newTxn);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Redis quên, nhưng MySQL vẫn nhớ -> Chặn đứng!
            log.error("Cảnh báo: Database đã tồn tại Idempotency-Key này: {}", idempotencyKey);
            throw new RuntimeException("Giao dịch đã tồn tại. Xin đừng gửi trùng lặp!");
        }

        // 3. (Tuần sau) Tại đây sẽ áp dụng Strategy Pattern:
        // Nếu type == P2P -> Gọi HTTP sang Wallet Service
        // Nếu type == PAYMENT -> Bắn message vào Kafka

        return "Giao dịch " + newTxn.getId() + " đã được tiếp nhận!";
    }
}