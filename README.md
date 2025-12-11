# Ordering API

## Architectural Decisions

### 1. Java 17 Records for Domain Objects
**File:** `domain/OrderRequest.java`
**Decision:** Used `record` types for DTOs.
**Why:** Immutability (thread safety), reduces boilerplate, and provides a clear data contract.

### 2. Polymorphism
**File:** `service/OrderProcessor.java`
**Decision:** Dependencies injected as `Map<String, Provider>`.
**Why:** Allows dynamic selection of implementations (switching between Stripe and Adyen) without code changes, leveraging Spring's IoC container.

---

## Business Logic Decisions

### 1. Compensating Transactions
**File:** `service/OrderProcessor.java` (Line 80)
* **Decision:** We implement a manual compensation step where `payment.voidPayment()` is triggered immediately if `pos.sendOrder()` fails.
* **Reasoning:** Ensures consistency between Payment Gateway and POS. Prevents "Ghost Charges" where a customer is charged for an order the restaurant never received.

### 2. Fail-Fast Authorization
**File:** `service/OrderProcessor.java` (Line 66)
* **Decision:** Payment authorization is the very first step. If it fails, processing stops.
* **Reasoning:** Protects downstream resources (POS, Kitchen) from processing orders that cannot be paid for.

### 3. Dispatch Failure Handling
**File:** `service/OrderProcessor.java` (Line 104)
* **Decision:** If `dispatch.schedule()` fails after payment capture, the status is `DISPATCH_FAILED`, but the payment is **not** voided.
* **Reasoning:** At this stage, food is likely being prepared. Voiding payment causes inventory loss. This state requires manual intervention rather than auto-cancellation.

### 4. Idempotency
**File:** `service/OrderProcessor.java` (Line 43)
* **Decision:** The `idempotencyKey` lock is checked before any logic execution.
* **Reasoning:** Financial safety. Prevents double-charging cards if the client retries a request due to network latency.