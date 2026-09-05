package com.shivam.wallet.service;

import com.shivam.wallet.domain.WalletTransaction;
import com.shivam.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionStatusService {

    private final WalletTransactionRepository transactionRepository;

    public TransactionStatusService(WalletTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID transactionId) {
        transactionRepository.findByTransactionId(transactionId)
                .ifPresent(WalletTransaction::fail);
    }
}
