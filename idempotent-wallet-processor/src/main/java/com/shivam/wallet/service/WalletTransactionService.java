package com.shivam.wallet.service;

import com.shivam.wallet.api.TransactionRequest;
import com.shivam.wallet.api.TransactionResponse;
import com.shivam.wallet.domain.TransactionStatus;
import com.shivam.wallet.domain.WalletTransaction;
import com.shivam.wallet.exception.DuplicateTransactionException;
import com.shivam.wallet.exception.InsufficientFundsException;
import com.shivam.wallet.exception.WalletNotFoundException;
import com.shivam.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class WalletTransactionService {

    private final TransactionReservationService reservationService;
    private final WalletMutationService walletMutationService;
    private final TransactionStatusService transactionStatusService;
    private final WalletTransactionRepository transactionRepository;

    public WalletTransactionService(TransactionReservationService reservationService,
                                    WalletMutationService walletMutationService,
                                    TransactionStatusService transactionStatusService,
                                    WalletTransactionRepository transactionRepository) {
        this.reservationService = reservationService;
        this.walletMutationService = walletMutationService;
        this.transactionStatusService = transactionStatusService;
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse process(TransactionRequest request) {
        try {
            // First request wins the unique transaction_id reservation.
            reservationService.reserve(request);
        } catch (DuplicateTransactionException duplicate) {
            return handleDuplicate(request.transactionId(), duplicate);
        }

        try {
            // Wallet balance mutation and SUCCESS status are one atomic transaction.
            return walletMutationService.apply(request);
        } catch (InsufficientFundsException | WalletNotFoundException ex) {
            // Reservation committed independently, so terminal failures must also be persisted independently.
            transactionStatusService.markFailed(request.transactionId());
            throw ex;
        } catch (RuntimeException ex) {
            transactionStatusService.markFailed(request.transactionId());
            throw ex;
        }
    }

    private TransactionResponse handleDuplicate(java.util.UUID transactionId,
                                                DuplicateTransactionException duplicate) {
        WalletTransaction existing = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> duplicate);

        if (existing.getStatus() == TransactionStatus.SUCCESS) {
            return toResponse(existing, true);
        }

        // PROCESSING or FAILED is not a successful replay. The assignment explicitly allows 409.
        throw duplicate;
    }

    private TransactionResponse toResponse(WalletTransaction tx, boolean replay) {
        return new TransactionResponse(
                tx.getTransactionId(),
                tx.getUserId(),
                tx.getAmount(),
                tx.getType().name(),
                tx.getStatus(),
                tx.getResultingBalance(),
                replay
        );
    }
}
