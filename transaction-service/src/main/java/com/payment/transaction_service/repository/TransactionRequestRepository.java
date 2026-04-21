package com.payment.transaction_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.transaction_service.entity.TransactionRequest;

public interface TransactionRequestRepository extends JpaRepository<TransactionRequest, Long> {

}
