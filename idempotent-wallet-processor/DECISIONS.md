
# DECISIONS

## 1. How did you handle the concurrency race condition?

I used a database unique constraint on `transaction_id` to handle duplicate requests. Only one request with the same transaction ID can be processed.

For wallet balance updates, I used `PESSIMISTIC_WRITE` locking. This makes concurrent requests for the same wallet wait, so the balance is checked and updated safely.

If a transaction fails, its status is marked as `FAILED` so it does not remain stuck in `PROCESSING`.

## 2. Where did your AI assistant give you an incorrect or sub-optimal suggestion?

An early suggestion was to use `ConcurrentHashMap` or `synchronized` for concurrency. I did not use this because it would only work properly in a single application instance.

Instead, I used database-level locking and a unique constraint because these are safer for handling concurrent requests.
