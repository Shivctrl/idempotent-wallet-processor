package com.shivam.wallet.service;

import com.shivam.wallet.api.TransactionRequest;
import com.shivam.wallet.domain.TransactionStatus;
import com.shivam.wallet.domain.WalletTransaction;
import com.shivam.wallet.exception.DuplicateTransactionException;
import com.shivam.wallet.repository.WalletTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionReservationService {

    private final WalletTransactionRepository transactionRepository;

    public TransactionReservationService(WalletTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(TransactionRequest request) {
        try {
            transactionRepository.saveAndFlush(
                    new WalletTransaction(
                            request.transactionId(),
                            request.userId(),
                            request.amount(),
                            request.type(),
                            TransactionStatus.PROCESSING,
                            BigDecimal.ZERO
                    )
            );
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateTransactionException(
                    "Transaction already exists: " + request.transactionId()
            );
        }
    }
}
