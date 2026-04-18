package com.payment.wallet_service.service;

import com.payment.wallet_service.entity.Direction;
import com.payment.wallet_service.entity.LedgerEntry;
import com.payment.wallet_service.entity.Wallet;
import com.payment.wallet_service.repository.LedgerEntryRepository;
import com.payment.wallet_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public void processTransaction(Long userId, Long transactionId, BigDecimal amount, Direction direction) {
        // 1. Tìm ví của người dùng
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví!"));

        // 2. Tính toán số dư mới
        BigDecimal newBalance;
        if (Direction.CREDIT == direction) {
            newBalance = wallet.getBalance().add(amount);
        } else {
            // Kiểm tra xem có đủ tiền để trừ không
            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Số dư không đủ!");
            }
            newBalance = wallet.getBalance().subtract(amount);
        }

        // 3. Cập nhật ví (Hibernate sẽ tự động kiểm tra @Version ở đây)
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // 4. Ghi Sổ cái (Ledger) để lưu vết
        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transactionId)
                .walletId(wallet.getId())
                .amount(amount)
                .direction(direction)
                .postBalance(newBalance)
                .build();

        ledgerEntryRepository.save(entry);
    }
}