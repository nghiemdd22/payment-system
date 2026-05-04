package com.payment.transaction_service.service;

import com.payment.transaction_service.client.WalletClient;
import com.payment.transaction_service.dto.WalletTypeResponse;
import com.payment.transaction_service.entity.TransactionRequest;
import com.payment.transaction_service.exception.DuplicateTransactionException;
import com.payment.transaction_service.exception.TransactionProcessingException;
import com.payment.transaction_service.repository.TransactionRequestRepository;
import com.payment.transaction_service.strategy.TransactionStrategy;
import com.payment.transaction_service.strategy.TransactionStrategyResolver;
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
    private final TransactionStrategyResolver strategyResolver;
    private final WalletClient walletClient;

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
                .setIfAbsent(redisKey, "PENDING:NA", Duration.ofMinutes(10));

        if (isNewRequest == null) {
            throw new TransactionProcessingException("Không thể kiểm tra idempotency lúc này. Vui lòng thử lại.");
        }

        if (Boolean.FALSE.equals(isNewRequest)) {
            String existingStatus = redisTemplate.opsForValue().get(redisKey);
            return handleDuplicateRequest(idempotencyKey, existingStatus);
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

        try {
            transactionRequestRepository.save(newTxn);
            redisTemplate.opsForValue().set(redisKey, "PENDING:" + newTxn.getId(), Duration.ofMinutes(10));
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

        WalletTypeResponse walletTypeResponse = walletClient.getWalletType(toWallet);
        String walletType = walletTypeResponse == null ? null : walletTypeResponse.getType();
        TransactionStrategy strategy = strategyResolver.resolve(walletType);
        return strategy.process(newTxn);

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

    private String handleDuplicateRequest(String idempotencyKey, String existingStatus) {
        if (existingStatus == null) {
            log.warn("Chan giao dich trung lap Idempotency-Key: {} - status: UNKNOWN", idempotencyKey);
            throw new DuplicateTransactionException("Yêu cầu đã được gửi trước đó. Vui lòng thử lại sau ít phút.");
        }

        if (existingStatus.startsWith("PENDING:")) {
            String txnId = existingStatus.substring("PENDING:".length());
            log.warn("Chan giao dich trung lap Idempotency-Key: {} - status: PENDING", idempotencyKey);
            if ("NA".equalsIgnoreCase(txnId)) {
                throw new DuplicateTransactionException("Giao dịch đang được xử lý. Xin đừng bấm đúp!");
            }
            throw new DuplicateTransactionException("Giao dịch " + txnId + " đang được xử lý. Xin đừng bấm đúp!");
        }

        if (existingStatus.startsWith("SUCCESS:")) {
            String txnId = existingStatus.substring("SUCCESS:".length());
            log.info("Idempotency hit Idempotency-Key: {} - status: SUCCESS", idempotencyKey);
            return "Giao dịch " + txnId + " đã hoàn thành trước đó.";
        }

        if (existingStatus.startsWith("FAILED:")) {
            String txnId = existingStatus.substring("FAILED:".length());
            log.info("Idempotency hit Idempotency-Key: {} - status: FAILED", idempotencyKey);
            return "Giao dịch " + txnId + " đã thất bại trước đó. Vui lòng tạo Idempotency-Key mới để thử lại.";
        }

        if (existingStatus.startsWith("ROLLBACK:") || existingStatus.startsWith("ROLLBACKED:")) {
            int prefixLength = existingStatus.startsWith("ROLLBACK:") ? "ROLLBACK:".length() : "ROLLBACKED:".length();
            String txnId = existingStatus.substring(prefixLength);
            log.info("Idempotency hit Idempotency-Key: {} - status: ROLLBACK", idempotencyKey);
            return "Giao dịch " + txnId
                    + " đã được hoàn tác trước đó. Vui lòng tạo Idempotency-Key mới để gửi giao dịch mới.";
        }

        log.warn("Chan giao dich trung lap Idempotency-Key: {} - status: {}", idempotencyKey, existingStatus);
        throw new DuplicateTransactionException(
                "Yêu cầu đã được gửi trước đó. Vui lòng kiểm tra trạng thái giao dịch.");
    }
}