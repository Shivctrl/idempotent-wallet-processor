package com.shivam.wallet;

import com.shivam.wallet.api.TransactionRequest;
import com.shivam.wallet.api.TransactionResponse;
import com.shivam.wallet.domain.TransactionStatus;
import com.shivam.wallet.domain.TransactionType;
import com.shivam.wallet.domain.Wallet;
import com.shivam.wallet.exception.InsufficientFundsException;
import com.shivam.wallet.repository.WalletRepository;
import com.shivam.wallet.repository.WalletTransactionRepository;
import com.shivam.wallet.service.WalletTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class WalletTransactionIntegrationTest {

    @Autowired private WalletTransactionService service;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletTransactionRepository transactionRepository;

    @Test
    @DisplayName("Processes a single valid debit transaction successfully")
    void happyPath() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        walletRepository.save(new Wallet(userId, new BigDecimal("500.00")));

        TransactionResponse response = service.process(
                new TransactionRequest(transactionId, userId, new BigDecimal("100.00"), TransactionType.DEBIT));

        assertEquals(new BigDecimal("400.00"), response.balance());
        assertFalse(response.idempotentReplay());
        assertEquals(TransactionStatus.SUCCESS, response.status());
        System.out.println("INTENT: Process a single valid debit transaction successfully.");
        System.out.println("RESULT: PASS - balance is ₹400.00.");
    }

    @Test
    @DisplayName("Sends 3 identical transactionIds simultaneously. Ensures the balance is only deducted once.")
    void idempotency() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        walletRepository.save(new Wallet(userId, new BigDecimal("500.00")));

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Result>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 3; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return Result.success(service.process(new TransactionRequest(
                                transactionId, userId, new BigDecimal("100.00"), TransactionType.DEBIT)));
                    } catch (Exception ex) {
                        return Result.failure(ex);
                    }
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int completedCalls = 0;
            for (Future<Result> future : futures) {
                if (future.get(15, TimeUnit.SECONDS).error == null) completedCalls++;
            }

            assertTrue(completedCalls >= 1);
            assertEquals(new BigDecimal("400.00"), walletRepository.findById(userId).orElseThrow().getBalance());
            System.out.println("INTENT: Send 3 identical transactionIds concurrently.");
            System.out.println("RESULT: PASS - final balance is ₹400.00, deducted exactly once.");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.")
    void raceCondition() throws Exception {
        UUID userId = UUID.randomUUID();
        walletRepository.save(new Wallet(userId, new BigDecimal("500.00")));

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch ready = new CountDownLatch(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Result>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 10; i++) {
                UUID transactionId = UUID.randomUUID();
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return Result.success(service.process(new TransactionRequest(
                                transactionId, userId, new BigDecimal("100.00"), TransactionType.DEBIT)));
                    } catch (Exception ex) {
                        return Result.failure(ex);
                    }
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int successes = 0;
            int insufficientFundsFailures = 0;
            for (Future<Result> future : futures) {
                Result result = future.get(20, TimeUnit.SECONDS);
                if (result.error == null) successes++;
                else if (result.error instanceof InsufficientFundsException) insufficientFundsFailures++;
            }

            assertEquals(5, successes);
            assertEquals(5, insufficientFundsFailures);
            assertEquals(new BigDecimal("0.00"), walletRepository.findById(userId).orElseThrow().getBalance());
            System.out.println("INTENT: Send 10 concurrent ₹100 debits against a ₹500 wallet.");
            System.out.println("RESULT: PASS - 5 succeeded, 5 failed with insufficient funds, final balance is ₹0.00.");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Marks an insufficient-funds transaction as FAILED instead of leaving it stuck in PROCESSING")
    void failedTransactionIsPersistedAsFailed() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        walletRepository.save(new Wallet(userId, new BigDecimal("50.00")));

        assertThrows(InsufficientFundsException.class, () -> service.process(
                new TransactionRequest(transactionId, userId, new BigDecimal("100.00"), TransactionType.DEBIT)));

        assertEquals(TransactionStatus.FAILED,
                transactionRepository.findByTransactionId(transactionId).orElseThrow().getStatus());
        assertEquals(new BigDecimal("50.00"), walletRepository.findById(userId).orElseThrow().getBalance());
        System.out.println("INTENT: Persist terminal failure after an insufficient-funds debit.");
        System.out.println("RESULT: PASS - transaction is FAILED and balance is unchanged.");
    }

    private static final class Result {
        private final TransactionResponse response;
        private final Exception error;

        private Result(TransactionResponse response, Exception error) {
            this.response = response;
            this.error = error;
        }

        static Result success(TransactionResponse response) { return new Result(response, null); }
        static Result failure(Exception error) { return new Result(null, error); }
    }
}
