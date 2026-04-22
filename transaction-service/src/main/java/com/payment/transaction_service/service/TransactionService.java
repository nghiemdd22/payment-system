package com.payment.transaction_service.service;

import com.payment.transaction_service.entity.TransactionRequest;
import com.payment.transaction_service.exception.DuplicateTransactionException;
import com.payment.transaction_service.exception.TransactionProcessingException;
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
    private final TransactionRequestRepository transactionRequestRepository;

    /**
     * Nhận Request và kiểm tra Lũy đẳng (Idempotency)
     */
    public String initTransaction(String idempotencyKey, Long fromWallet, Long toWallet, BigDecimal amount,
            String type) {
        validateRequest(idempotencyKey, fromWallet, toWallet, amount, type);

        String redisKey = "idem:key:" + idempotencyKey;

        // 1. Tuyệt chiêu SETNX của Redis (setIfAbsent)
        // Cố gắng lưu key này vào Redis với TTL là 10 phút.
        // Lệnh này chạy ở mức Atomic (nguyên tử) trong Redis -> Chống Race Condition
        // tuyệt đối.
        Boolean isNewRequest = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PENDING", Duration.ofMinutes(10));

        if (isNewRequest == null) {
            throw new TransactionProcessingException("Không thể kiểm tra idempotency lúc này. Vui lòng thử lại.");
        }

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

        // transactionRequestRepository.save(newTxn);
        try {
            transactionRequestRepository.save(newTxn);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Redis quên, nhưng MySQL vẫn nhớ -> Chặn đứng!
            log.warn("Database da ton tai Idempotency-Key: {}", idempotencyKey);
            throw new DuplicateTransactionException("Giao dịch đã tồn tại. Xin đừng gửi trùng lặp!");
        } catch (Exception ex) {
            // Không lưu được DB thì xóa lock Redis để request hợp lệ có thể retry.
            redisTemplate.delete(redisKey);
            log.error("Loi khi luu giao dich Idempotency-Key: {}", idempotencyKey, ex);
            throw new TransactionProcessingException("Không thể khởi tạo giao dịch lúc này. Vui lòng thử lại.");
        }

        // 3. (Tuần sau) Tại đây sẽ áp dụng Strategy Pattern:
        // Nếu type == P2P -> Gọi HTTP sang Wallet Service
        // Nếu type == PAYMENT -> Bắn message vào Kafka

        return "Giao dịch " + newTxn.getId() + " đã được tiếp nhận!";
    }

    private void validateRequest(String idempotencyKey, Long fromWallet, Long toWallet, BigDecimal amount,
            String type) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key không được để trống");
        }
        if (fromWallet == null || toWallet == null) {
            throw new IllegalArgumentException("fromWallet và toWallet là bắt buộc");
        }
        if (fromWallet.equals(toWallet)) {
            throw new IllegalArgumentException("fromWallet và toWallet không được trùng nhau");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount phải lớn hơn 0");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type không được để trống");
        }
    }
}