# DECISIONS

## 1. How did you handle the concurrency race condition?

I used two database-level guarantees for two different problems.

### Idempotency
`wallet_transactions.transaction_id` has a unique database constraint. The first request reserves the transaction ID in a `REQUIRES_NEW` transaction. Concurrent duplicate requests cannot reserve the same ID, so only one request can enter the balance-mutation path. A duplicate receives the already completed response when the original transaction is `SUCCESS`; otherwise it receives `409 Conflict`, which is permitted by the assignment.

### Concurrent balance updates
The wallet is loaded with JPA `PESSIMISTIC_WRITE` locking. The read, sufficient-funds check, balance update, and transaction completion happen inside one database transaction. Requests mutating the same wallet row are therefore serialized, preventing multiple debits from using the same stale balance and preventing a negative balance.

### Failed transactions
Because reservation commits independently, a business failure is explicitly marked `FAILED` in another `REQUIRES_NEW` transaction. This prevents records from being permanently stuck in `PROCESSING`.

## 2. Where did your AI assistant give you an incorrect or sub-optimal suggestion?

An early suggestion was to use an in-memory `ConcurrentHashMap` or a JVM `synchronized` block for idempotency/concurrency. That is sub-optimal for a backend service because it only protects one application instance and fails when the application is horizontally scaled.

The final implementation instead uses database guarantees:

- unique database constraint for transaction-id idempotency
- `PESSIMISTIC_WRITE` database locking for wallet balance updates
- separate transactional reservation and failure-status updates
- H2 in-memory database for zero-config integration testing

These choices keep correctness in the persistence layer rather than process-local memory.
