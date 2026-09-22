# Session and Cache Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent any cached order, detail, salesperson, token refresh, or pending financial operation from crossing authenticated user sessions.

**Architecture:** Every in-memory cache is keyed by a stable session identity resolved before lookup. A single session-lifecycle coordinator performs logout for manual and expired-session paths. Token refresh uses a process-wide mutex and rechecks the latest token inside the critical section.

**Tech Stack:** Kotlin, Coroutines, Hilt, Room, OkHttp Authenticator, MockK, JUnit.

**Spec:** `docs/superpowers/specs/2026-09-22-payment-session-security-hardening-design.md`

## Global Constraints

- Preserve existing local POS changes and pending approved-payment rows.
- Logout clears in-memory application caches before tokens and local user data.
- Pending financial rows remain stored and scoped to their original `sessionUserId`.
- Manual logout and session-expired logout must use one implementation.
- Do not change backend authorization rules or deploy anything.
- Use TDD and narrow commits.

## Review Focus

- User B logging in immediately after user A must never receive A's 30-second order cache; Task 1 adds this regression test.
- Detail cache keys with the same order ID across companies must remain session-specific; Task 1 adds this regression test.
- Logout while a cache fill is in flight must not repopulate the next session's cache; Task 2 adds a generation-token test.
- Multiple simultaneous 401 responses must perform one refresh request; Task 4 adds the concurrency test.
- Failed refresh must not loop authentication or retain a half-updated token set; Task 4 adds the failure test.

---

### Task 1: Key repository caches by authenticated identity

**Files:**
- Modify: `app/src/main/java/com/detrapay/data/repositories/AuthRepository.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/OrderRepository.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/SalesmanRepository.kt`
- Modify: `app/src/test/java/com/detrapay/data/repositories/OrderRepositoryTest.kt`
- Modify: `app/src/test/java/com/detrapay/data/repositories/SalesmanRepositoryTest.kt`

**Interfaces:**
- Consumes: `LoggedInUser.id`, company ID, and dispatcher ID.
- Produces: `AuthRepository.currentSessionScope(): SessionScope?` and `clearCache()` on both cached repositories.

- [ ] **Step 1: Add failing cross-session cache tests**

```kotlin
@Test
fun `user B never receives user A orders cache`() = runTest {
    coEvery { authRepository.currentSessionScope() } returnsMany listOf(scope("a"), scope("b"))
    coEvery { remote.getOrders(any(), any()) } returnsMany listOf(successOrders(1), successOrders(2))
    repository.getOrders()
    val second = repository.getOrders()
    assertEquals(listOf(2), (second as Result.Success).data.map { it.id })
}

@Test
fun `detail cache includes session identity`() = runTest {
    coEvery { authRepository.currentSessionScope() } returnsMany listOf(scope("a"), scope("b"))
    coEvery { remote.getOrder(10) } returnsMany listOf(successOrder(10, "A"), successOrder(10, "B"))
    repository.getOrder(10)
    assertEquals("B", ((repository.getOrder(10) as Result.Success).data.customer.name))
}
```

- [ ] **Step 2: Run repository tests and verify they fail on cache reuse**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.repositories.OrderRepositoryTest" --tests "com.detrapay.data.repositories.SalesmanRepositoryTest"`

Expected: FAIL because cache lookup occurs before user resolution.

- [ ] **Step 3: Introduce an explicit immutable session scope**

```kotlin
data class SessionScope(
    val userId: String,
    val companyId: Int,
    val dispatcherId: Int?,
)
```

`currentSessionScope()` resolves the logged user and returns null when company context is absent. Cache entries contain the full scope. Detail keys are `Pair<SessionScope, Int>`. Both repositories resolve scope before any cache read.

- [ ] **Step 4: Add synchronized cache clearing**

Expose `clearCache()` in both repositories. Synchronize cache reads/writes and clearing so a stale entry cannot be observed while logout clears it.

- [ ] **Step 5: Run repository tests**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.repositories.OrderRepositoryTest" --tests "com.detrapay.data.repositories.SalesmanRepositoryTest"`

Expected: PASS for same-session reuse and cross-session misses.

- [ ] **Step 6: Commit scoped caches**

```powershell
git add app/src/main/java/com/detrapay/data/repositories/AuthRepository.kt app/src/main/java/com/detrapay/data/repositories/OrderRepository.kt app/src/main/java/com/detrapay/data/repositories/SalesmanRepository.kt app/src/test/java/com/detrapay/data/repositories/OrderRepositoryTest.kt app/src/test/java/com/detrapay/data/repositories/SalesmanRepositoryTest.kt
git commit -m "fix: scope repository caches to the session"
```

### Task 2: Prevent in-flight requests from repopulating cleared caches

**Files:**
- Modify: `app/src/main/java/com/detrapay/data/repositories/OrderRepository.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/SalesmanRepository.kt`
- Modify: `app/src/test/java/com/detrapay/data/repositories/OrderRepositoryTest.kt`
- Modify: `app/src/test/java/com/detrapay/data/repositories/SalesmanRepositoryTest.kt`

**Interfaces:**
- Consumes: scope-keyed caches from Task 1.
- Produces: monotonically increasing `cacheGeneration` checked before cache writes.

- [ ] **Step 1: Add a failing logout-during-load test**

```kotlin
@Test
fun `clear during remote load prevents stale cache write`() = runTest {
    val response = CompletableDeferred<Result<List<OrderResponse>>>()
    coEvery { remote.getOrders(any(), any()) } coAnswers { response.await() }
    val load = async { repository.getOrders() }
    repository.clearCache()
    response.complete(successOrders(1))
    load.await()
    repository.getOrders()
    coVerify(exactly = 2) { remote.getOrders(any(), any()) }
}
```

- [ ] **Step 2: Run tests and verify the stale response repopulates cache**

Run the two repository test classes from Task 1.

Expected: FAIL with one remote call instead of two.

- [ ] **Step 3: Add generation checks**

Capture `generationAtStart` before the remote call. `clearCache()` increments `cacheGeneration`. Store a response only when the generation and `SessionScope` still match the values captured before the request.

- [ ] **Step 4: Run repository tests and commit**

Run the two repository test classes from Task 1; expected PASS.

```powershell
git add app/src/main/java/com/detrapay/data/repositories/OrderRepository.kt app/src/main/java/com/detrapay/data/repositories/SalesmanRepository.kt app/src/test/java/com/detrapay/data/repositories/OrderRepositoryTest.kt app/src/test/java/com/detrapay/data/repositories/SalesmanRepositoryTest.kt
git commit -m "fix: reject stale cache fills after logout"
```

### Task 3: Centralize manual and expired-session logout

**Files:**
- Create: `app/src/main/java/com/detrapay/data/repositories/SessionLifecycleCoordinator.kt`
- Create: `app/src/test/java/com/detrapay/data/repositories/SessionLifecycleCoordinatorTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/detrapay/ui/session_expired_dialog/SessionExpiredViewModel.kt`
- Modify: `app/src/main/java/com/detrapay/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: `OrderRepository.clearCache()`, `SalesmanRepository.clearCache()`, `AuthRepository.logout()`.
- Produces: `SessionLifecycleCoordinator.logout(): Result<Unit>`.

- [ ] **Step 1: Write an ordered failing test**

```kotlin
@Test
fun `logout clears caches before deleting credentials`() = runTest {
    coordinator.logout()
    coVerifySequence {
        orderRepository.clearCache()
        salesmanRepository.clearCache()
        authRepository.logout()
    }
}
```

- [ ] **Step 2: Run the focused test and verify the coordinator is missing**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.repositories.SessionLifecycleCoordinatorTest"`

Expected: FAIL at compilation.

- [ ] **Step 3: Implement one logout path**

The coordinator uses a `Mutex`, clears both caches, preserves `PendingPaymentCompletion` rows by not touching their DAO, then calls `AuthRepository.logout()`. Both ViewModels inject and invoke the coordinator instead of AuthRepository directly.

- [ ] **Step 4: Run coordinator and ViewModel tests**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.repositories.SessionLifecycleCoordinatorTest" --tests "com.detrapay.ui.home.*" --tests "com.detrapay.ui.session_expired_dialog.*"`

Expected: PASS; both UI paths call the same coordinator.

- [ ] **Step 5: Commit the shared logout lifecycle**

```powershell
git add app/src/main/java/com/detrapay/data/repositories/SessionLifecycleCoordinator.kt app/src/test/java/com/detrapay/data/repositories/SessionLifecycleCoordinatorTest.kt app/src/main/java/com/detrapay/ui/home/HomeViewModel.kt app/src/main/java/com/detrapay/ui/session_expired_dialog/SessionExpiredViewModel.kt app/src/main/java/com/detrapay/di/RepositoryModule.kt
git commit -m "fix: centralize secure session cleanup"
```

### Task 4: Make token refresh single-flight

**Files:**
- Modify: `app/src/main/java/com/detrapay/data/api/SessionAuthenticator.kt`
- Create: `app/src/test/java/com/detrapay/data/api/SessionAuthenticatorTest.kt`

**Interfaces:**
- Consumes: `AuthRepository` token access/update and no-auth refresh service.
- Produces: one refresh request for concurrent 401 responses.

- [ ] **Step 1: Add failing concurrent-refresh tests**

```kotlin
@Test
fun `concurrent 401 responses refresh once`() = runTest {
    coEvery { service.refresh(any(), any()) } coAnswers { delay(50); Response.success(refreshResponse("new")) }
    coroutineScope { List(5) { async(Dispatchers.IO) { authenticator.authenticate(null, unauthorized("old")) } }.awaitAll() }
    coVerify(exactly = 1) { service.refresh(any(), any()) }
}

@Test
fun `failed refresh returns null without retry loop`() {
    coEvery { service.refresh(any(), any()) } returns Response.error(401, body())
    assertNull(authenticator.authenticate(null, unauthorized("old")))
    coVerify(exactly = 1) { service.refresh(any(), any()) }
}
```

- [ ] **Step 2: Run the focused test and verify multiple refreshes**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.api.SessionAuthenticatorTest"`

Expected: FAIL because concurrent calls enter refresh independently.

- [ ] **Step 3: Guard refresh and recheck token inside the lock**

Use one `Mutex`. Inside `withLock`, compare the request token with `currentAccessToken()` again; if another call refreshed it, rebuild immediately. Otherwise perform one refresh and atomically update the stored session. Keep the existing response-count guard.

- [ ] **Step 4: Run the focused test and commit**

Run the SessionAuthenticator test; expected PASS.

```powershell
git add app/src/main/java/com/detrapay/data/api/SessionAuthenticator.kt app/src/test/java/com/detrapay/data/api/SessionAuthenticatorTest.kt
git commit -m "fix: serialize access token refresh"
```

### Task 5: Verify session isolation

**Files:**
- Verify only.

**Interfaces:**
- Consumes: Tasks 1-4.
- Produces: test/build evidence without installation.

- [ ] **Step 1: Run focused session tests**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.repositories.OrderRepositoryTest" --tests "com.detrapay.data.repositories.SalesmanRepositoryTest" --tests "com.detrapay.data.repositories.SessionLifecycleCoordinatorTest" --tests "com.detrapay.data.api.SessionAuthenticatorTest" --max-workers=1`

Expected: PASS.

- [ ] **Step 2: Run the final POS batch if the payment plan did not already do so at the same revision**

Run: `.\gradlew.bat --offline testDebugUnitTest lintDebug assembleDebug --max-workers=1`

Expected: tests/build pass and no session-related lint errors. Do not install the APK.
