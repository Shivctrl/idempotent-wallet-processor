# Idempotent Payment / Wallet Event Processor

## Stack
- Java 17
- Spring Boot
- Spring Data JPA
- H2 in-memory database
- JUnit 5

## Endpoint

`POST /api/v1/transactions/process`

Example request:

```json
{
  "transactionId": "11111111-1111-1111-1111-111111111111",
  "userId": "22222222-2222-2222-2222-222222222222",
  "amount": 250.00,
  "type": "DEBIT"
}
```

## Run tests

```bash
mvn test
```

The integration tests require no database setup. H2 starts automatically.

## Required tests included

1. Happy path: valid debit is processed successfully.
2. Idempotency: 3 identical transaction IDs are sent concurrently and the balance is deducted once.
3. Race condition: 10 concurrent ₹100 debits against a ₹500 wallet result in exactly 5 successes, 5 insufficient-funds failures, and a final balance of ₹0.

## Important implementation details

- `wallet_transactions.transaction_id` has a database unique constraint.
- The first request reserves the transaction ID in a separate transaction.
- Duplicate transaction IDs either return the completed cached response or receive a conflict.
- Wallet updates use `PESSIMISTIC_WRITE` locking.
