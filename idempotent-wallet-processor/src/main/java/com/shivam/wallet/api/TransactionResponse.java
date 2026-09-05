package com.shivam.wallet.api;

import com.shivam.wallet.domain.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        String type,
        TransactionStatus status,
        BigDecimal balance,
        boolean idempotentReplay
) {
}
