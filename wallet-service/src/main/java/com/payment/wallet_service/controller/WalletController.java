package com.payment.wallet_service.controller;

import com.payment.wallet_service.dto.request.TransferRequest;
import com.payment.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * API: POST /api/v1/wallets/transfer
     * Header giả lập: X-Transaction-Id (Do Transaction Service truyền sang)
     */
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferP2P(
            @RequestHeader("X-Transaction-Id") Long transactionId,
            @RequestBody TransferRequest request) {

        // Gọi xuống Service để xử lý giao dịch ACID
        walletService.transferP2P(transactionId, request);

        // Trả về kết quả JSON chuẩn RESTful
        return ResponseEntity.ok(Map.of(
                "code", "200",
                "message", "Chuyển tiền P2P thành công",
                "transaction_id", transactionId));
    }

    @GetMapping("/{walletId}/type")
    public ResponseEntity<Map<String, Object>> getWalletType(@PathVariable Long walletId) {
        String type = walletService.getWalletType(walletId);
        return ResponseEntity.ok(Map.of(
                "wallet_id", walletId,
                "type", type));
    }
}