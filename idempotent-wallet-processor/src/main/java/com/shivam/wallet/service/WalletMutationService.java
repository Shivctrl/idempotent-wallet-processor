package com.shivam.wallet.service;

import com.shivam.wallet.api.TransactionRequest;
import com.shivam.wallet.api.TransactionResponse;
import com.shivam.wallet.domain.TransactionType;
import com.shivam.wallet.domain.Wallet;
import com.shivam.wallet.domain.WalletTransaction;
import com.shivam.wallet.exception.InsufficientFundsException;
import com.shivam.wallet.exception.WalletNotFoundException;
import com.shivam.wallet.repository.WalletRepository;
import com.shivam.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletMutationService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletMutationService(WalletRepository walletRepository,
                                 WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse apply(TransactionRequest request) {
        // Lock the wallet row for the entire read-check-update operation.
        Wallet wallet = walletRepository.findWithLockById(request.userId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + request.userId()));

        if (request.type() == TransactionType.DEBIT) {
            if (wallet.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }
            wallet.debit(request.amount());
        } else {
            wallet.credit(request.amount());
        }

        WalletTransaction transaction = transactionRepository.findByTransactionId(request.transactionId())
                .orElseThrow(() -> new IllegalStateException("Reserved transaction disappeared"));

        transaction.complete(wallet.getBalance());

        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getStatus(),
                transaction.getResultingBalance(),
                false
        );
    }
}
