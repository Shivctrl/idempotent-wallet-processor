package com.shivam.wallet.repository;

import com.shivam.wallet.domain.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Optional<WalletTransaction> findByTransactionId(UUID transactionId);
}
