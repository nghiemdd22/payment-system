package com.payment.wallet_service.service;

import com.payment.wallet_service.dto.request.TransferRequest;
import com.payment.wallet_service.entity.Direction;
import com.payment.wallet_service.entity.LedgerEntry;
import com.payment.wallet_service.entity.Wallet;
import com.payment.wallet_service.exception.InsufficientBalanceException;
import com.payment.wallet_service.exception.ResourceNotFoundException;
import com.payment.wallet_service.repository.LedgerEntryRepository;
import com.payment.wallet_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Luồng chuyển tiền P2P Đồng bộ
     * 
     * @Transactional đảm bảo: Nếu có bất kỳ lỗi gì xảy ra, toàn bộ lệnh cộng/trừ
     *                tiền và ghi sổ cái sẽ bị Rollback.
     */
    @Transactional
    public void transferP2P(Long transactionId, TransferRequest request) {
        log.info("Bắt đầu giao dịch P2P ID: {} từ Ví {} sang Ví {}",
                transactionId, request.getFromWalletId(), request.getToWalletId());

        // 1. Lấy thông tin 2 ví từ Database
        Wallet fromWallet = walletRepository.findById(request.getFromWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Ví gửi"));

        Wallet toWallet = walletRepository.findById(request.getToWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Ví nhận"));

        // 2. Kiểm tra số dư (Business Rule)
        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Số dư không đủ để thực hiện giao dịch");
        }

        // 3. Thực hiện Trừ tiền & Cộng tiền trên RAM (Object)
        fromWallet.setBalance(fromWallet.getBalance().subtract(request.getAmount()));
        toWallet.setBalance(toWallet.getBalance().add(request.getAmount()));

        // Lưu xuống DB.
        // LƯU Ý QUAN TRỌNG: Lúc này Hibernate sẽ tự động so sánh cột @Version.
        // Nếu có 1 thread khác đã update ví này trước đó vài mili-giây, version trong
        // DB đã bị tăng lên.
        // Hibernate thấy version trên RAM khác version trong DB -> Ném ra
        // OptimisticLockException và ROLLBACK ngay lập tức!
        walletRepository.saveAll(Arrays.asList(fromWallet, toWallet));

        // 4. Sinh Bút toán kép (Double-Entry Ledger) - ĐẢM BẢO TÍNH BẤT BIẾN

        // Dòng Nợ (Trừ tiền ví gửi)
        LedgerEntry debitEntry = LedgerEntry.builder()
                .transactionId(transactionId)
                .walletId(fromWallet.getId())
                .amount(request.getAmount())
                .direction(Direction.DEBIT)
                .postBalance(fromWallet.getBalance()) // Số dư sau khi trừ
                .description(request.getDescription())
                .build();

        // Dòng Có (Cộng tiền ví nhận)
        LedgerEntry creditEntry = LedgerEntry.builder()
                .transactionId(transactionId)
                .walletId(toWallet.getId())
                .amount(request.getAmount())
                .direction(Direction.CREDIT)
                .postBalance(toWallet.getBalance()) // Số dư sau khi cộng
                .description(request.getDescription())
                .build();

        // Lưu 2 dòng sổ cái vào DB
        ledgerEntryRepository.saveAll(Arrays.asList(debitEntry, creditEntry));

        log.info("Giao dịch P2P ID: {} thành công!", transactionId);
    }
}