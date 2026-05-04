package com.payment.transaction_service.strategy;

import com.payment.transaction_service.client.WalletClient;
import com.payment.transaction_service.dto.WalletTransferRequest;
import com.payment.transaction_service.entity.TransactionRequest;
import com.payment.transaction_service.exception.TransactionProcessingException;
import com.payment.transaction_service.repository.TransactionRequestRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class PaymentTransactionStrategy implements TransactionStrategy {

    private static final Duration IDEM_TTL = Duration.ofMinutes(10);

    private final WalletClient walletClient;
    private final TransactionRequestRepository transactionRequestRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean supports(String walletType) {
        return "DEFAULT".equalsIgnoreCase(walletType) || "SYSTEM".equalsIgnoreCase(walletType);
    }

    @Override
    public String process(TransactionRequest transactionRequest) {
        WalletTransferRequest walletTransferRequest = WalletTransferRequest.builder()
                .fromWalletId(transactionRequest.getFromWalletId())
                .toWalletId(transactionRequest.getToWalletId())
                .amount(transactionRequest.getAmount())
                .description("PAYMENT transfer from transaction-service")
                .build();

        String redisKey = "idem:key:" + transactionRequest.getIdempotencyKey();

        try {
            walletClient.transferP2P(transactionRequest.getId(), walletTransferRequest);
            transactionRequest.setStatus("SUCCESS");
            transactionRequestRepository.save(transactionRequest);
            redisTemplate.opsForValue().set(redisKey, "SUCCESS:" + transactionRequest.getId(), IDEM_TTL);
            return "Giao dịch " + transactionRequest.getId() + " đã hoàn thành thành công!";
        } catch (FeignException ex) {
            markFailed(transactionRequest, redisKey);
            log.error("Wallet service tu choi giao dich Id: {}", transactionRequest.getId(), ex);
            throw new TransactionProcessingException("Wallet service từ chối giao dịch. HTTP status: " + ex.status());
        } catch (Exception ex) {
            markFailed(transactionRequest, redisKey);
            log.error("Loi he thong khi goi wallet service. Transaction Id: {}", transactionRequest.getId(), ex);
            throw new TransactionProcessingException("Không thể xử lý giao dịch P2P lúc này. Vui lòng thử lại.");
        }
    }

    private void markFailed(TransactionRequest transactionRequest, String redisKey) {
        transactionRequest.setStatus("FAILED");
        transactionRequestRepository.save(transactionRequest);
        redisTemplate.opsForValue().set(redisKey, "FAILED:" + transactionRequest.getId(), IDEM_TTL);
    }
}
