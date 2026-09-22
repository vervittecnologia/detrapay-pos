# Atomic Payment Integrity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the POS/Manager atomic payment contract so an approved terminal transaction is durable, idempotent, recoverable, and can never create more than one receivable.

**Architecture:** The Manager prepares a payment attempt and completes it through transactional PostgreSQL RPCs. The POS persists a terminal approval in Room before auxiliary logging or HTTP completion, then retries only the idempotent completion call. A small payment-operation coordinator serializes terminal, abort, reconciliation, persistence, and completion transitions.

**Tech Stack:** Kotlin, Android ViewModel, Coroutines, Room 2.6, Retrofit/OkHttp, PlugPag SDK, TypeScript/Deno Edge Functions, Supabase PostgreSQL, Vitest.

**Spec:** `docs/superpowers/specs/2026-09-22-payment-session-security-hardening-design.md`

## Global Constraints

- Preserve all pre-existing local POS changes; stage and commit only files named by the active task.
- Do not change fee, interest, installment, simulation, or displayed-total rules.
- Do not fall back to legacy receivable mutation when an atomic endpoint fails.
- Do not deploy Edge Functions, apply remote migrations, install the APK, rotate keys, or rewrite Git history.
- `terminal_reference` is stable, unique, PlugPag-safe, and no longer than 10 characters.
- An approved transaction must be persisted before auxiliary logging or backend completion.
- A pending approval is deleted only after the Manager returns idempotent success.
- Use TDD for every behavior change and make one narrow commit per task in the affected repository.

## Review Focus

- Two simultaneous completion requests for one attempt must return one receivable and one `created: true` result at most; Task 3 adds this contract test.
- One external transaction reused across different attempts must return 409 without mutating either order; Task 3 adds this conflict test.
- A process death after terminal approval but before HTTP completion must resume without calling PlugPag; Task 7 adds this recovery test.
- An abort request racing with approval must persist and complete the approval before another charge is allowed; Task 8 adds this state-machine test.
- A 10-character terminal-reference collision must retry generation rather than break idempotency; Task 2 adds this collision test.

---

### Task 1: Version the Manager payment-attempt schema and RPC boundaries

**Files:**
- Create: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/migrations/20260922090000_atomic_payment_attempts.sql`
- Create: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/atomic-payment-migration.test.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/src/integrations/supabase/types.ts`

**Interfaces:**
- Consumes: existing `sales_orders`, `receivables`, `payment_methods`, and company/user relations.
- Produces: `complete_payment_attempt(p_attempt_id uuid, p_user_id uuid, p_transaction_id text, p_authorization_code text, p_card_brand text, p_card_last4 text, p_card_holder text, p_transaction_log jsonb) -> jsonb` and `record_manual_payment(p_order_id bigint, p_user_id uuid, p_payment_method_id bigint, p_amount_original numeric, p_installments integer, p_idempotency_key text, p_receivable jsonb) -> jsonb`.

- [ ] **Step 1: Write a migration contract test that fails because the migration is absent**

```ts
import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

const sql = readFileSync(
  new URL("../../../migrations/20260922090000_atomic_payment_attempts.sql", import.meta.url),
  "utf8",
);

describe("atomic payment migration", () => {
  it("adds unique idempotency, terminal and external transaction guards", () => {
    expect(sql).toContain("payment_attempts_idempotency_key_key");
    expect(sql).toContain("payment_attempts_terminal_reference_key");
    expect(sql).toContain("payment_attempts_external_transaction_id_key");
  });

  it("locks attempts and exposes both transactional RPCs", () => {
    expect(sql).toMatch(/for update/i);
    expect(sql).toContain("complete_payment_attempt");
    expect(sql).toContain("record_manual_payment");
  });
});
```

- [ ] **Step 2: Run the focused test and verify the missing-file failure**

Run from `detrapay-services`: `npm test -- supabase/functions/mobile/utils/atomic-payment-migration.test.ts`

Expected: FAIL because `20260922090000_atomic_payment_attempts.sql` does not exist.

- [ ] **Step 3: Create the additive migration**

The SQL must use `create table if not exists`, `alter table ... add column if not exists`, named unique indexes, and `security definer set search_path = public`. The completion RPC must lock the attempt with `FOR UPDATE`, compare an already completed transaction, reject conflicting transaction IDs with SQLSTATE `23505`, insert exactly one paid receivable from `calculated_receivable`, update `sales_orders.current_amount` and derived payment status in the same transaction, and return:

```sql
jsonb_build_object(
  'receivable_id', v_receivable_id,
  'order_id', v_attempt.sales_order_id,
  'created', v_created
)
```

The manual RPC must lock by the unique `receivables.idempotency_key`, reject online methods, insert one manual receivable, update the order amount, and return the same JSON shape. Revoke both functions from `public`, `anon`, and `authenticated`; grant execution only to `service_role`.

- [ ] **Step 4: Update generated database types for the additive columns**

Add `terminal_reference: string` to `payment_attempts.Row`, `Insert`, and `Update`, preserving the existing nullable fields and relationships.

- [ ] **Step 5: Run the migration contract test**

Run: `npm test -- supabase/functions/mobile/utils/atomic-payment-migration.test.ts`

Expected: PASS with both unique-guard and RPC assertions.

- [ ] **Step 6: Commit the Manager schema contract**

```powershell
git add supabase/migrations/20260922090000_atomic_payment_attempts.sql supabase/functions/mobile/utils/atomic-payment-migration.test.ts src/integrations/supabase/types.ts
git commit -m "feat: add atomic payment persistence contract"
```

### Task 2: Return a stable terminal reference during preparation

**Files:**
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/payment-attempt.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/payment-attempt.test.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/handlers/payment-attempts.ts`

**Interfaces:**
- Consumes: `AttemptDb.findAttemptByKey` and `AttemptDb.insertAttempt`.
- Produces: `AttemptRecord.terminal_reference: string`; `AttemptDb.nextTerminalReference(): Promise<string>`; prepare response containing `terminal_reference`.

- [ ] **Step 1: Add failing preparation and collision tests**

```ts
it("returns the same terminal reference for an idempotent retry", async () => {
  const first = await preparePaymentAttempt(input);
  const second = await preparePaymentAttempt(input);
  expect(second.body.data.terminal_reference).toBe(first.body.data.terminal_reference);
});

it("retries a terminal reference collision", async () => {
  db.nextTerminalReference = vi.fn()
    .mockResolvedValueOnce("ABC1234567")
    .mockResolvedValueOnce("DEF1234567");
  db.insertAttempt = vi.fn()
    .mockRejectedValueOnce(Object.assign(new Error("duplicate"), { code: "23505" }))
    .mockResolvedValueOnce({ ...record, id: "attempt-2", terminal_reference: "DEF1234567" });
  const result = await preparePaymentAttempt(input);
  expect(result.body.data.terminal_reference).toBe("DEF1234567");
});
```

- [ ] **Step 2: Run the focused test and confirm the missing-interface failure**

Run: `npm test -- supabase/functions/mobile/utils/payment-attempt.test.ts`

Expected: FAIL because the attempt has no `terminal_reference` and the DB adapter has no generator.

- [ ] **Step 3: Implement bounded reference generation and collision retry**

Generate uppercase references from cryptographic UUID material, strip hyphens, prefix with `P`, and take 10 characters. Attempt insertion at most five times; only retry PostgreSQL unique-violation code `23505`. An idempotency-key race must still return the already stored attempt before generating a replacement.

```ts
function formatTerminalReference(uuid: string): string {
  return `P${uuid.replaceAll("-", "").toUpperCase()}`.slice(0, 10);
}
```

- [ ] **Step 4: Run all payment-attempt utility tests**

Run: `npm test -- supabase/functions/mobile/utils/payment-attempt.test.ts`

Expected: PASS, including stable retry and collision coverage.

- [ ] **Step 5: Commit terminal-reference preparation**

```powershell
git add supabase/functions/mobile/utils/payment-attempt.ts supabase/functions/mobile/utils/payment-attempt.test.ts supabase/functions/mobile/handlers/payment-attempts.ts
git commit -m "feat: identify terminal payment attempts uniquely"
```

### Task 3: Implement idempotent online completion in the Manager

**Files:**
- Create: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/atomic-payment.ts`
- Create: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/atomic-payment.test.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/handlers/payment-attempts.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/index.ts`

**Interfaces:**
- Consumes: `complete_payment_attempt` RPC from Task 1 and authenticated `userId`.
- Produces: `validateCompletePaymentPayload(value): { value: CompletePaymentPayload } | { error: string }`; `handleCompletePaymentAttempt(req, attemptId, corsHeaders): Promise<Response>`.

- [ ] **Step 1: Write failing payload and database-result tests**

```ts
it("requires a nonblank transaction_id", () => {
  expect(validateCompletePaymentPayload({ transaction_id: " " }))
    .toEqual({ error: "transaction_id is required" });
});

it("maps same-attempt retry to created false", async () => {
  db.complete = vi.fn().mockResolvedValue({ receivable_id: 9, order_id: 4, created: false });
  const result = await completePreparedPayment(validInput);
  expect(result).toEqual({ status: 200, data: { receivable_id: 9, order_id: 4, created: false } });
});

it("maps reused external transaction to conflict", async () => {
  db.complete = vi.fn().mockRejectedValue(Object.assign(new Error("external_transaction_conflict"), { code: "23505" }));
  const result = await completePreparedPayment(validInput);
  expect(result.status).toBe(409);
});
```

- [ ] **Step 2: Run the focused test and verify the missing-module failure**

Run: `npm test -- supabase/functions/mobile/utils/atomic-payment.test.ts`

Expected: FAIL because `atomic-payment.ts` does not exist.

- [ ] **Step 3: Implement validation, RPC adapter, and error mapping**

The utility accepts only a nonblank `transaction_id`, trims optional strings, caps the serialized transaction log at 32 KiB, invokes one `db.complete(...)` call, maps `23505` to HTTP 409, missing/inaccessible/expired attempts to 404/403/422, and never inserts a receivable itself.

- [ ] **Step 4: Register the completion route before the general `/orders` branch**

```ts
} else if (/^\/payment-attempts\/[0-9a-f-]+\/complete\/?$/i.test(path) && req.method === "POST") {
  const attemptId = path.split("/")[2];
  response = await handleCompletePaymentAttempt(req, attemptId, corsHeaders);
}
```

The handler validates the JWT, calls `adminClient.rpc("complete_payment_attempt", ...)`, fetches the full updated order using the returned `order_id`, and responds with `{ data, updatedOrder: { data: order } }`.

- [ ] **Step 5: Run atomic and preparation tests**

Run: `npm test -- supabase/functions/mobile/utils/atomic-payment.test.ts supabase/functions/mobile/utils/payment-attempt.test.ts`

Expected: PASS, including idempotent retry, conflict, invalid payload, and no-direct-insert cases.

- [ ] **Step 6: Commit the online completion endpoint**

```powershell
git add supabase/functions/mobile/utils/atomic-payment.ts supabase/functions/mobile/utils/atomic-payment.test.ts supabase/functions/mobile/handlers/payment-attempts.ts supabase/functions/mobile/index.ts
git commit -m "feat: complete prepared payments idempotently"
```

### Task 4: Implement idempotent manual payments in the Manager

**Files:**
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/atomic-payment.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/atomic-payment.test.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/handlers/payment-attempts.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/index.ts`

**Interfaces:**
- Consumes: `calculateReceivables`, authenticated user, and `record_manual_payment` RPC.
- Produces: `validateManualPaymentPayload`; `handleRecordManualPayment(req, orderId, corsHeaders)`.

- [ ] **Step 1: Add failing manual-payment tests**

```ts
it("rejects an online payment method before mutation", async () => {
  db.findPaymentMethod = vi.fn().mockResolvedValue({ id: 3, is_online_payment: true });
  const result = await recordManualPayment(validManualInput);
  expect(result.status).toBe(422);
  expect(db.record).not.toHaveBeenCalled();
});

it("returns the same receivable for an idempotent manual retry", async () => {
  db.record = vi.fn().mockResolvedValue({ receivable_id: 12, order_id: 4, created: false });
  const result = await recordManualPayment(validManualInput);
  expect(result.data.created).toBe(false);
});
```

- [ ] **Step 2: Run the focused test and verify failure**

Run: `npm test -- supabase/functions/mobile/utils/atomic-payment.test.ts`

Expected: FAIL because manual-payment validation and orchestration are absent.

- [ ] **Step 3: Implement manual orchestration without a legacy fallback**

Validate `payment_method_id`, positive finite `amount_original`, installments >= 1, and nonblank `idempotency_key`. Resolve the order and payment method, reject online methods, calculate the receivable once, then pass the calculated JSON to `record_manual_payment`. Register:

```ts
} else if (/^\/orders\/\d+\/manual-payments\/?$/.test(path) && req.method === "POST") {
  const orderId = Number(path.split("/")[2]);
  response = await handleRecordManualPayment(req, orderId, corsHeaders);
}
```

- [ ] **Step 4: Run the atomic-payment tests**

Run: `npm test -- supabase/functions/mobile/utils/atomic-payment.test.ts`

Expected: PASS for manual idempotency, online-method rejection, invalid amount, and inaccessible order.

- [ ] **Step 5: Commit manual payment support**

```powershell
git add supabase/functions/mobile/utils/atomic-payment.ts supabase/functions/mobile/utils/atomic-payment.test.ts supabase/functions/mobile/handlers/payment-attempts.ts supabase/functions/mobile/index.ts
git commit -m "feat: record manual payments idempotently"
```

### Task 5: Consume terminal references in the POS contract

**Files:**
- Create: `app/src/main/java/com/detrapay/data/ConflictException.kt`
- Modify: `app/src/main/java/com/detrapay/data/model/remote/AtomicPayment.kt`
- Modify: `app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt`
- Modify: `app/src/test/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSourceTest.kt`

**Interfaces:**
- Consumes: Manager `terminal_reference` and HTTP 409 error responses.
- Produces: `PaymentAttempt.terminalReference: String` and `ConflictException(code: String)` for integrity conflicts.

- [ ] **Step 1: Add a failing DTO/remote contract test**

```kotlin
@Test
fun `prepare payment exposes terminal reference`() = runTest {
    coEvery { detrapayService.prepareOnlinePayment(any(), any()) } returns
        Response.success(PaymentAttemptResponse(paymentAttempt(terminalReference = "PABC123456")))

    val result = remoteDataSource.prepareOnlinePayment(10, 3, 100.0, 1, "key")

    assertEquals("PABC123456", (result as Result.Success).data.terminalReference)
}
```

- [ ] **Step 2: Run the focused test and verify compilation fails**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.datasources.remote.DetrapayRemoteDataSourceTest"`

Expected: FAIL because `terminalReference` is not defined.

- [ ] **Step 3: Add the serialized field**

```kotlin
@SerializedName("terminal_reference") val terminalReference: String,
```

Reject blank or longer-than-10 values in `prepareOnlinePayment` with a contract error before opening PlugPag.
Map HTTP 409 from online completion to `ConflictException` so the ViewModel can keep the durable pending row and present an integrity error without retrying the terminal.

- [ ] **Step 4: Run the focused test**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.datasources.remote.DetrapayRemoteDataSourceTest"`

Expected: PASS.

- [ ] **Step 5: Commit the POS network contract**

```powershell
git add app/src/main/java/com/detrapay/data/ConflictException.kt app/src/main/java/com/detrapay/data/model/remote/AtomicPayment.kt app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt app/src/test/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSourceTest.kt
git commit -m "feat: consume terminal payment references"
```

### Task 6: Persist approved completions in Room

**Files:**
- Create: `app/src/main/java/com/detrapay/data/model/local/PendingPaymentCompletion.kt`
- Create: `app/src/main/java/com/detrapay/data/datasources/local/PendingPaymentCompletionDao.kt`
- Create: `app/src/main/java/com/detrapay/data/repositories/PendingPaymentRepository.kt`
- Create: `app/src/test/java/com/detrapay/data/repositories/PendingPaymentRepositoryTest.kt`
- Modify: `app/src/main/java/com/detrapay/data/database/AppDatabase.kt`
- Modify: `app/src/main/java/com/detrapay/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/detrapay/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: approved `PaymentData`, session user ID, attempt ID, idempotency key, terminal reference, order ID.
- Produces: `suspend fun saveApproved(completion: PendingPaymentCompletion)`, `suspend fun findForAttempt(attemptId: String, sessionUserId: String): PendingPaymentCompletion?`, `suspend fun listPending(sessionUserId: String): List<PendingPaymentCompletion>`, and `suspend fun deleteCompleted(attemptId: String)`.

- [ ] **Step 1: Write failing repository tests**

```kotlin
@Test
fun `approved payment is reloaded for the same session`() = runTest {
    repository.saveApproved(pending(sessionUserId = "user-a"))
    assertEquals("tx-1", repository.findForAttempt("attempt-1", "user-a")?.transactionId)
}

@Test
fun `pending payment is invisible to another session`() = runTest {
    repository.saveApproved(pending(sessionUserId = "user-a"))
    assertNull(repository.findForAttempt("attempt-1", "user-b"))
}
```

- [ ] **Step 2: Run the focused test and verify missing classes fail compilation**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.repositories.PendingPaymentRepositoryTest"`

Expected: FAIL because the entity, DAO, and repository do not exist.

- [ ] **Step 3: Add the Room entity and DAO**

Use `attemptId` as the primary key and store non-null `sessionUserId`, `idempotencyKey`, `terminalReference`, `orderId`, `transactionId`, status, timestamps, and nullable terminal fields needed to reconstruct `PaymentData`. Add a `(sessionUserId, status)` index. DAO queries must always include `session_user_id` except deletion by completed attempt ID.

- [ ] **Step 4: Add database version 5 and repository mapping**

Add the entity to `AppDatabase`, add `AutoMigration(from = 4, to = 5)`, expose the DAO, and provide the repository through Hilt. Repository mapping must not serialize the entire PlugPag object.

- [ ] **Step 5: Run the focused repository tests**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.repositories.PendingPaymentRepositoryTest"`

Expected: PASS for save/reload, session isolation, and deletion after completion.

- [ ] **Step 6: Commit durable pending completions**

```powershell
git add app/src/main/java/com/detrapay/data/model/local/PendingPaymentCompletion.kt app/src/main/java/com/detrapay/data/datasources/local/PendingPaymentCompletionDao.kt app/src/main/java/com/detrapay/data/repositories/PendingPaymentRepository.kt app/src/test/java/com/detrapay/data/repositories/PendingPaymentRepositoryTest.kt app/src/main/java/com/detrapay/data/database/AppDatabase.kt app/src/main/java/com/detrapay/di/DatabaseModule.kt app/src/main/java/com/detrapay/di/RepositoryModule.kt
git commit -m "feat: persist approved payment completions"
```

### Task 7: Recover approved payments without reopening PlugPag

**Files:**
- Modify: `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`
- Modify: `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/PaymentRepository.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt`

**Interfaces:**
- Consumes: `PendingPaymentRepository` from Task 6, `PaymentAttempt.terminalReference` from Task 5, and `AuthRepository.getLoggedUser()`.
- Produces: `fun resumePendingPayments()` and a completion path that never calls `doPayment` for stored approvals.

- [ ] **Step 1: Add failing recovery and log-failure tests**

```kotlin
@Test
fun `process recreation completes stored approval without charging again`() = runTest {
    coEvery { authRepository.getLoggedUser() } returns loggedUser("user-a")
    coEvery { pendingRepository.listPending("user-a") } returns listOf(pendingCompletion())
    coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns Result.Success(order())

    viewModel.resumePendingPayments()
    advanceUntilIdle()

    verify(exactly = 0) { plugPag.doPayment(any()) }
    coVerify { pendingRepository.deleteCompleted("attempt-1") }
}

@Test
fun `local audit log failure does not retry terminal`() = runTest {
    coEvery { paymentRepository.saveTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws IOException("disk")
    approveTerminalPayment()
    advanceUntilIdle()
    coVerify { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) }
    verify(exactly = 1) { plugPag.doPayment(any()) }
}

@Test
fun `completion conflict keeps durable pending row`() = runTest {
    coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
        Result.Error(ConflictException("external_transaction_conflict"))
    approveTerminalPayment()
    advanceUntilIdle()
    coVerify(exactly = 0) { pendingRepository.deleteCompleted("attempt-1") }
}
```

- [ ] **Step 2: Run the focused ViewModel test**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest"`

Expected: FAIL because recovery and durable persistence are not wired.

- [ ] **Step 3: Persist approval before the audit log**

Replace the in-memory `PendingCompletion` source of truth with `PendingPaymentRepository`. After validating `transactionId`, call `saveApproved` first, wrap `saveTransaction` in `runCatching`, then call backend completion. Delete the row only on `Result.Success`.

- [ ] **Step 4: Use terminal reference in PlugPag and A011 matching**

Pass `attempt.terminalReference` to `PlugPagPaymentData.userReference`. Match recovery against that exact reference, amount, result, transaction ID, and compatible payment type. Remove `orderUserReference` from online payment matching.

- [ ] **Step 5: Add explicit resume entry point**

`resumePendingPayments()` resolves the current user through `AuthRepository`, reads only that user's rows, processes them sequentially, posts `RECORDING`, calls only `recordApprovedOnlinePayment`, and leaves failed rows intact. `OrdersRoute` invokes it once after the authenticated orders surface is initialized. The route does not pass or enumerate another user's identity.

- [ ] **Step 6: Run the focused tests**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest"`

Expected: PASS for recovery, audit-log failure, exact A011 matching, and no second terminal call.

- [ ] **Step 7: Commit recovery behavior**

```powershell
git add app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt app/src/main/java/com/detrapay/data/repositories/PaymentRepository.kt app/src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt
git commit -m "fix: recover approved payments without recharging"
```

### Task 8: Serialize abort, approval, and new-payment transitions

**Files:**
- Create: `app/src/main/java/com/detrapay/ui/payment/PaymentOperationCoordinator.kt`
- Create: `app/src/test/java/com/detrapay/ui/payment/PaymentOperationCoordinatorTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`
- Modify: `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`

**Interfaces:**
- Consumes: payment operation ID and durable pending repository.
- Produces: `PaymentOperationState`; `begin(operationId)`, `requestAbort(operationId)`, `terminalApproved(operationId)`, `terminalRejected(operationId)`, `completionSucceeded(operationId)` guarded by `Mutex`.

- [ ] **Step 1: Write failing coordinator tests**

```kotlin
@Test
fun `approval after abort request remains recoverable`() = runTest {
    coordinator.begin("attempt-1")
    coordinator.requestAbort("attempt-1")
    assertEquals(PaymentOperationState.ApprovedPendingPersistence("attempt-1"), coordinator.terminalApproved("attempt-1"))
}

@Test
fun `second payment cannot begin while abort is unresolved`() = runTest {
    coordinator.begin("attempt-1")
    coordinator.requestAbort("attempt-1")
    assertFalse(coordinator.begin("attempt-2"))
}
```

- [ ] **Step 2: Run focused coordinator tests and verify failure**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentOperationCoordinatorTest"`

Expected: FAIL because the coordinator does not exist.

- [ ] **Step 3: Implement the explicit state machine**

Define `Idle`, `Preparing`, `TerminalActive`, `AbortRequested`, `ApprovedPendingPersistence`, `ApprovedPendingServer`, `Completed`, and `Failed`. All transitions run inside `Mutex.withLock`. A late event with another operation ID cannot change UI state. An approval for the active operation always transitions toward persistence, including from `AbortRequested`.

- [ ] **Step 4: Wire ViewModel abort semantics**

`abortPayment()` marks `AbortRequested`, calls `plugPag.abort()`, and does not set the coordinator to idle until the SDK produces a definitive rejection/cancel or approval. `disposeSubscriber()` runs only after the active operation is terminal. `payOrder()` returns without charging when `begin` rejects a second operation.

- [ ] **Step 5: Run payment coordinator and ViewModel tests**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentOperationCoordinatorTest" --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest"`

Expected: PASS for abort/approval race, second-payment blocking, stale-event rejection, and normal success.

- [ ] **Step 6: Commit serialized payment transitions**

```powershell
git add app/src/main/java/com/detrapay/ui/payment/PaymentOperationCoordinator.kt app/src/test/java/com/detrapay/ui/payment/PaymentOperationCoordinatorTest.kt app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt
git commit -m "fix: serialize terminal payment lifecycle"
```

### Task 9: Verify the complete payment subsystem

**Files:**
- Verify only; modify code only to fix failures caused by Tasks 1-8.

**Interfaces:**
- Consumes: all preceding tasks.
- Produces: reproducible local evidence for Manager and POS without deployment or device installation.

- [ ] **Step 1: Run the Manager payment tests**

Run from `detrapay-services`: `npm test -- supabase/functions/mobile/utils/payment-attempt.test.ts supabase/functions/mobile/utils/atomic-payment.test.ts supabase/functions/mobile/utils/atomic-payment-migration.test.ts`

Expected: all focused tests PASS.

- [ ] **Step 2: Run the Manager full checks**

Run: `npm run lint`

Run: `npm test`

Run: `npm run build`

Expected: all commands exit 0. Record unrelated pre-existing failures separately rather than weakening payment assertions.

- [ ] **Step 3: Run POS unit tests and one final build batch**

Run from `detrapay-pos`: `.\gradlew.bat --offline testDebugUnitTest lintDebug assembleDebug --max-workers=1`

Expected: unit tests and build pass; lint has zero errors introduced by the payment changes. Do not install the APK.

- [ ] **Step 4: Inspect scoped diffs and repository state**

Run in both repositories: `git status --short --branch` and `git log -10 --oneline`.

Expected: no unrelated POS files are staged or committed; Manager contains only atomic-payment commits; no remote state changed.
