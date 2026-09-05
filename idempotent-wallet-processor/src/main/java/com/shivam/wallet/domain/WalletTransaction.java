package com.shivam.wallet.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions",
       uniqueConstraints = @UniqueConstraint(name = "uk_transaction_id", columnNames = "transaction_id"))
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, updatable = false, unique = true)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal resultingBalance;

    protected WalletTransaction() {
    }

    public WalletTransaction(UUID transactionId, UUID userId, BigDecimal amount,
                             TransactionType type, TransactionStatus status,
                             BigDecimal resultingBalance) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.resultingBalance = resultingBalance;
        this.createdAt = Instant.now();
    }

    public void complete(BigDecimal resultingBalance) {
        this.status = TransactionStatus.SUCCESS;
        this.resultingBalance = resultingBalance;
    }

    public void fail() {
        this.status = TransactionStatus.FAILED;
    }

    public UUID getTransactionId() { return transactionId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public BigDecimal getResultingBalance() { return resultingBalance; }
}
