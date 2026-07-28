# Backend Prepared Payment Value Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cobrar em crédito, débito e PIX online exatamente o `amount_final` preparado pelo backend, mantendo o valor digitado como `amount_original` e os meios offline inalterados.

**Architecture:** `PaymentDialogViewModel` continuará orquestrando a tentativa online, mas transformará `PaymentAttempt.amountFinal` em uma quantia canônica de centavos antes de chamar o terminal. Os mesmos centavos serão convertidos para `Double` de duas casas apenas para o log e para `PaymentData`, evitando divergência entre o valor cobrado e o persistido.

**Tech Stack:** Kotlin, Android ViewModel, coroutines, PlugPag SDK, JUnit 4, MockK, Gradle.

## Global Constraints

- `amount_original` enviado na preparação continua sendo o valor digitado pelo usuário.
- `amount_final` retornado por `POST /orders/{orderId}/payment-attempts` é a fonte do total de todo pagamento online.
- Crédito, débito e PIX online devem usar o mesmo valor no PagBank, no log local e na conclusão da tentativa.
- Dinheiro, crédito da loja e outros meios com `isOnlinePayment == false` continuam fora do PagBank.
- O aplicativo não recalcula taxas nem exige igualdade entre `amount_original` e `amount_final`.
- Um valor final não finito ou que resulte em zero centavos deve ser rejeitado antes da configuração da maquininha.
- Alterações locais preexistentes e não relacionadas não devem ser editadas nem incluídas nos commits.
- Se o app for instalado ou reinstalado no device, ele deve ser aberto e os logs filtrados devem ser inspecionados imediatamente depois.

---

### Task 1: Use the prepared total across every online payment boundary

**Files:**
- Modify: `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`

**Interfaces:**
- Consumes: `PaymentAttempt.amountFinal: Double`, `OrderPaymentRequest.amount: Double`, `OrderPaymentRequest.paymentMethod.isOnlinePayment: Boolean`.
- Produces: `startPagBank(request: OrderPaymentRequest, attemptId: String, amountFinalCents: Int, serial: String)` and a consistent amount at `PlugPagPaymentData`, `PaymentRepository.saveTransaction`, and `PaymentData.amountFinal`.

- [ ] **Step 1: Replace the surcharge rejection test with a failing credit contract test**

Replace `prepared surcharge never opens PagBank or records payment` with a test that prepares `27.00` from an original request of `25.67`, captures both the terminal payload and completion payload, and expects a successful payment:

```kotlin
@Test
fun `credit charges the final amount prepared by backend`() {
    val terminalSlot = slot<PlugPagPaymentData>()
    val approvalSlot = slot<PaymentData>()
    coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
        Result.Success(attempt(amountOriginal = 25.67, amountFinal = 27.00))
    coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
        Result.Success(Unit)
    every { plugPag.isAuthenticated() } returns true
    every { plugPag.doPayment(capture(terminalSlot)) } returns approvedTransaction()
    coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", capture(approvalSlot)) } returns
        Result.Success(TestOrderFixtures.order())

    viewModel.payOrder(request("credito", online = true, installments = 3), "SER123")

    viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
    assertEquals(2700, readInt(terminalSlot.captured, "amount"))
    assertEquals(25.67, approvalSlot.captured.amountOriginal ?: 0.0, 0.0)
    assertEquals(27.00, approvalSlot.captured.amountFinal ?: 0.0, 0.0)
    coVerify(exactly = 1) {
        paymentRepository.saveTransaction(
            orderId = 10,
            amount = 27.00,
            installments = 3,
            paymentType = "credito",
            transactionId = any(),
            transactionCode = any(),
            date = any(),
            result = PlugPag.RET_OK,
            cardBrand = any(),
            cardLast4 = any(),
            cardHolder = any(),
            pixTxIdCode = any(),
            message = any(),
            errorCode = any(),
        )
    }
}
```

- [ ] **Step 2: Add failing debit and PIX prepared-total tests**

Add a helper-driven assertion that runs the same behavior for the two remaining online types:

```kotlin
@Test
fun `debit charges the final amount prepared by backend`() {
    assertPreparedOnlineAmount(type = "debito", expectedPaymentType = PlugPag.TYPE_DEBITO)
}

@Test
fun `pix charges the final amount prepared by backend`() {
    assertPreparedOnlineAmount(type = "pix", expectedPaymentType = PlugPag.TYPE_PIX)
}

private fun assertPreparedOnlineAmount(type: String, expectedPaymentType: Int) {
    val terminalSlot = slot<PlugPagPaymentData>()
    val approvalSlot = slot<PaymentData>()
    coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
        Result.Success(attempt(amountOriginal = 25.67, amountFinal = 26.40))
    coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
        Result.Success(Unit)
    every { plugPag.isAuthenticated() } returns true
    every { plugPag.doPayment(capture(terminalSlot)) } returns approvedTransaction()
    coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", capture(approvalSlot)) } returns
        Result.Success(TestOrderFixtures.order())

    viewModel.payOrder(request(type, online = true), "SER123")

    viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
    assertEquals(expectedPaymentType, readInt(terminalSlot.captured, "paymentType", "type"))
    assertEquals(2640, readInt(terminalSlot.captured, "amount"))
    assertEquals(26.40, approvalSlot.captured.amountFinal ?: 0.0, 0.0)
}
```

- [ ] **Step 3: Add failing monetary rounding and invalid-total tests**

Change the existing canonical-total test so that the backend returns `27.006` and the request remains `25.67`; expect `2701` cents and `27.01` in the completion payload. Add an invalid-total test:

```kotlin
@Test
fun `invalid prepared final amount never opens PagBank`() {
    coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
        Result.Success(attempt(amountFinal = Double.NaN))

    viewModel.payOrder(request("credito", online = true), "SER123")

    val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
    assertEquals("O backend retornou um valor final invalido para o pagamento.", state.message)
    coVerify(exactly = 0) { orderRepository.updatePaymentAttemptSplitConfig(any(), any()) }
    verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
    coVerify(exactly = 0) { orderRepository.recordApprovedOnlinePayment(any(), any()) }
}
```

- [ ] **Step 4: Run the focused tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest.credit charges the final amount prepared by backend" --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest.debit charges the final amount prepared by backend" --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest.pix charges the final amount prepared by backend" --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest.invalid prepared final amount never opens PagBank"
```

Expected: FAIL because the current code blocks different prepared totals and has no invalid-total-specific validation.

- [ ] **Step 5: Canonicalize and propagate the backend total**

In the success branch of `payOrder`, remove the comparison with `request.amount` and replace it with:

```kotlin
is Result.Success -> {
    val preparedAmount = prepared.data.amountFinal
    val preparedAmountCents = if (preparedAmount.isFinite()) amountInCents(preparedAmount) else 0
    if (preparedAmountCents <= 0) {
        finishWithError("O backend retornou um valor final invalido para o pagamento.")
    } else {
        startPagBank(request, prepared.data.id, preparedAmountCents, serial)
    }
}
```

Change `startPagBank` to accept integer cents and derive the persistable total once:

```kotlin
private suspend fun startPagBank(
    request: OrderPaymentRequest,
    attemptId: String,
    amountFinalCents: Int,
    serial: String,
) {
    val amountFinal = amountFinalCents / 100.0
```

Use `amountFinalCents` in `PlugPagPaymentData`, use `amountFinal` in `PaymentData.amountFinal`, and pass `amountFinal` into both calls to `saveTransactionLog`.

Restore the explicit log parameter:

```kotlin
private suspend fun saveTransactionLog(
    request: OrderPaymentRequest,
    result: PlugPagTransactionResult,
    amountFinal: Double,
) {
    paymentRepository.saveTransaction(
        orderId = request.order.id,
        amount = amountFinal,
```

- [ ] **Step 6: Run the complete ViewModel test class and verify GREEN**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest"
```

Expected: PASS with credit, debit, PIX, rounding, invalid values, offline routing, rejection, and idempotent retry covered.

- [ ] **Step 7: Commit the payment contract correction**

```powershell
git add -- app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt
git commit -m "fix: use backend prepared payment total"
```

### Task 2: Verify application regressions and the Android device

**Files:**
- Verify only: no production files should change.

**Interfaces:**
- Consumes: the online prepared-total behavior from Task 1.
- Produces: JVM regression, install, launch, and filtered log evidence.

- [ ] **Step 1: Run payment and order-flow regressions**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest" --tests "com.detrapay.ui.home.orders.OrderPaymentRouterTest" --tests "com.detrapay.ui.home.orders.OrdersViewModelTest"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run the complete debug unit-test suite**

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` with no failing tests.

- [ ] **Step 3: Check the scoped diff and workspace state**

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; preexisting unrelated modifications and artifacts remain untouched.

- [ ] **Step 4: Verify an authorized device is connected**

```powershell
adb devices
```

Expected: one device in state `device`. If none is available, report device verification as unavailable without claiming installation success.

- [ ] **Step 5: Clear logs and install the debug app**

```powershell
adb logcat -c
.\gradlew.bat installDebug
```

Expected: `BUILD SUCCESSFUL` and successful installation on the connected device.

- [ ] **Step 6: Open the app immediately after installation**

```powershell
adb shell am start -n com.detrapay/.ui.splash.SplashActivity
```

Expected: `Starting: Intent { cmp=com.detrapay/.ui.splash.SplashActivity }` or an equivalent successful activity-start result.

- [ ] **Step 7: Inspect the required filtered logs**

```powershell
adb logcat -d | Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"
```

Expected: no `FATAL EXCEPTION` associated with `com.detrapay`.
