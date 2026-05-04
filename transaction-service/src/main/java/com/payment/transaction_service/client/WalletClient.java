package com.payment.transaction_service.client;

import com.payment.transaction_service.dto.WalletTransferRequest;
import com.payment.transaction_service.dto.WalletTypeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "walletServiceClient", url = "${wallet-service.base-url}")
public interface WalletClient {

    @PostMapping("/api/v1/wallets/transfer")
    Map<String, Object> transferP2P(
            @RequestHeader("X-Transaction-Id") Long transactionId,
            @RequestBody WalletTransferRequest request);

    @GetMapping("/api/v1/wallets/{walletId}/type")
    WalletTypeResponse getWalletType(@PathVariable("walletId") Long walletId);
}
