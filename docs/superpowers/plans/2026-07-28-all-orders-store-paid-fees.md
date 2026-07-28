# All Orders and Store-Paid PagBank Fees Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show every order returned by the backend and guarantee that PagBank charges the exact checkout total with installment fees paid by the store.

**Architecture:** Keep list selection in `OrderPresentation` and publish the complete sorted result from `OrdersViewModel`. Derive checkout installment text from the requested payment total rather than the fee quote totals. At the terminal boundary, compare the prepared attempt in integer cents with the requested total, reject any backend surcharge, and send the requested total using seller-paid installments.

**Tech Stack:** Kotlin, Android ViewModel/LiveData, Jetpack Compose, PagBank PlugPag SDK, JUnit 4, MockK, kotlinx-coroutines-test, Gradle, ADB.

## Global Constraints

- Display `PENDING`, `PAID`, `AUTHORIZED`, `COMPLETED`, and `CANCELLED` orders in descending order by identifier.
- Final or fully paid orders may open details but must not start another payment.
- The checkout total displayed to the user must equal the total sent to PagBank in integer cents.
- PagBank installments greater than one must use `PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR`.
- Do not call PagBank when the prepared backend attempt adds any amount to the requested total after conversion to cents.
- Do not add a client-side workaround that records a different amount from the backend attempt; report the backend contract violation explicitly.
- Preserve all pre-existing uncommitted workspace changes.
- Do not add receipt-printing validation; printing remains an effect of `printReceipt = true`.
- After installation, open `com.detrapay/.ui.splash.SplashActivity` and inspect logcat for `com.detrapay`, `AndroidRuntime`, and `FATAL EXCEPTION`.

---

## File Structure

- `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`: sort all orders and derive store-paid installment labels from one total.
- `app/src/main/java/com/detrapay/ui/home/orders/OrdersViewModel.kt`: publish every sorted order returned by the repository.
- `app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt`: render checkout installment labels from the derived presentation.
- `app/src/main/java/com/detrapay/ui/home/orders/screens/CreditScreen.kt`: pass the checkout total into each installment row.
- `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`: enforce amount equality at the PagBank boundary and select seller-paid installments.
- `app/src/main/java/com/detrapay/data/model/OrderReceivableItemDeleteRules.kt`: allow deletion only for eligible offline payments.
- `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`: cover all-status sorting and store-paid installment text.
- `app/src/test/java/com/detrapay/ui/home/orders/OrdersViewModelTest.kt`: prove the ViewModel publishes all repository orders.
- `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`: prove exact terminal amount, seller-paid installment type, and surcharge rejection.
- `app/src/test/java/com/detrapay/data/model/OrderReceivableItemDeleteRulesTest.kt`: prove offline deletion eligibility and online protection.

### Task 1: Publish every order

**Files:**
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersViewModel.kt`
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`
- Create: `app/src/test/java/com/detrapay/ui/home/orders/OrdersViewModelTest.kt`

**Interfaces:**
- Consumes: `OrderRepository.getOrders(forceRefresh: Boolean): Result<List<Order>>`
- Produces: `OrderPresentation.allOrders(orders: List<Order>): List<Order>` and `OrdersViewModel.orderListState` containing every sorted order.

- [ ] **Step 1: Replace the pending-only presentation test with an all-status failing test**

In `OrderPresentationTest.kt`, replace `pending orders excludes cancelled completed and settled orders` with:

```kotlin
@Test
fun `all orders includes every status sorted by newest id`() {
    val orders = listOf(
        order(id = 1, total = 100.0, status = OrderStatus.PENDING),
        order(id = 2, total = 100.0, status = OrderStatus.CANCELLED),
        order(id = 3, total = 100.0, status = OrderStatus.COMPLETED),
        order(id = 4, total = 100.0, status = OrderStatus.PAID),
        order(id = 5, total = 100.0, status = OrderStatus.AUTHORIZED),
    )

    val visible = OrderPresentation.allOrders(orders)

    assertEquals(listOf(5, 4, 3, 2, 1), visible.map { it.id })
}
```

- [ ] **Step 2: Run the presentation test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
```

Expected: compilation failure because `OrderPresentation.allOrders` does not exist.

- [ ] **Step 3: Add a failing ViewModel regression test**

Create `OrdersViewModelTest.kt`:

```kotlin
package com.detrapay.ui.home.orders

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.detrapay.data.Result
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.testing.MainDispatcherRule
import com.detrapay.testing.getOrAwaitValueMatching
import com.detrapay.ui.state.UIState
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrdersViewModelTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val orderRepository = mockk<OrderRepository>()
    private val registrationRepository = mockk<RegistrationRepository>(relaxed = true)
    private val salesmanRepository = mockk<SalesmanRepository>(relaxed = true)

    @Test
    fun `load orders publishes every repository status newest first`() {
        val pending = TestOrderFixtures.order().copy(id = 20, status = OrderStatus.PENDING)
        val paid = TestOrderFixtures.order().copy(id = 22, status = OrderStatus.PAID)
        val cancelled = TestOrderFixtures.order().copy(id = 21, status = OrderStatus.CANCELLED)
        coEvery { orderRepository.getOrders(false) } returns
            Result.Success(listOf(pending, paid, cancelled))
        val viewModel = OrdersViewModel(orderRepository, registrationRepository, salesmanRepository)

        viewModel.loadOrders()

        val state = viewModel.orderListState.getOrAwaitValueMatching { it is UIState.Success }
        assertEquals(listOf(22, 21, 20), (state as UIState.Success).data?.map { it.id })
    }
}
```

- [ ] **Step 4: Run both order tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest" --tests "com.detrapay.ui.home.orders.OrdersViewModelTest"
```

Expected: compilation fails because `OrderPresentation.allOrders` does not exist; before the test rename, the ViewModel would also remove paid and cancelled orders through `pendingOrders`.

- [ ] **Step 5: Add the minimal all-orders selector**

Replace `pendingOrders` in `OrderPresentation.kt` with:

```kotlin
fun allOrders(orders: List<Order>): List<Order> {
    return orders.sortedByDescending { it.id }
}
```

Change the success branch in `OrdersViewModel.loadOrders` to:

```kotlin
is Result.Success -> _orderListState.postValue(
    UIState.Success(OrderPresentation.allOrders(result.data)),
)
```

- [ ] **Step 6: Run both order tests and verify GREEN**

Run the command from Step 4.

Expected: both tests PASS with every status retained and sorted.

- [ ] **Step 7: Commit the all-orders behavior**

```powershell
git add -- app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt app/src/main/java/com/detrapay/ui/home/orders/OrdersViewModel.kt app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt app/src/test/java/com/detrapay/ui/home/orders/OrdersViewModelTest.kt
git commit -m "fix: show orders from every status"
```

### Task 2: Present checkout installments without client fees

**Files:**
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/CreditScreen.kt`
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`

**Interfaces:**
- Consumes: checkout `amount: Double` and `InstallmentFee.installmentNumber`.
- Produces: `StorePaidInstallmentPresentation` and `OrderPresentation.storePaidInstallment(amount: Double, installments: Int)`.

- [ ] **Step 1: Add a failing installment presentation test**

Add to `OrderPresentationTest.kt`:

```kotlin
@Test
fun `store paid installment keeps displayed total equal to checkout amount`() {
    val presentation = OrderPresentation.storePaidInstallment(amount = 25.67, installments = 3)

    assertEquals("R$ 8,56", presentation.installmentValueLabel)
    assertEquals("R$ 25,67", presentation.totalValueLabel)
    assertEquals("Taxas por conta da loja", presentation.feePayerLabel)
}
```

- [ ] **Step 2: Run the presentation test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
```

Expected: compilation failure because `storePaidInstallment` does not exist.

- [ ] **Step 3: Add the store-paid installment presentation**

Add beside the other presentation data classes in `OrderPresentation.kt`:

```kotlin
data class StorePaidInstallmentPresentation(
    val installmentValueLabel: String,
    val totalValueLabel: String,
    val feePayerLabel: String,
)
```

Add inside `OrderPresentation`:

```kotlin
fun storePaidInstallment(amount: Double, installments: Int): StorePaidInstallmentPresentation {
    val count = installments.coerceAtLeast(1)
    return StorePaidInstallmentPresentation(
        installmentValueLabel = formatCurrency(amount / count),
        totalValueLabel = formatCurrency(amount),
        feePayerLabel = "Taxas por conta da loja",
    )
}
```

- [ ] **Step 4: Run the presentation test and verify GREEN**

Run the command from Step 2.

Expected: PASS with total `R$ 25,67` regardless of the number of installments.

- [ ] **Step 5: Render checkout rows from the requested total**

Change `InstallmentRow` in `OrderFlowBlocks.kt` to accept `amount` and use the presentation:

```kotlin
@Composable
fun InstallmentRow(
    amount: Double,
    installment: InstallmentFee,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val presentation = OrderPresentation.storePaidInstallment(
        amount = amount,
        installments = installment.installmentNumber,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) OrderFlowColors.Blue else OrderFlowColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${installment.installmentNumber}x",
            modifier = Modifier.width(42.dp),
            color = if (isSelected) OrderFlowColors.Blue else OrderFlowColors.Faint,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(presentation.installmentValueLabel, color = OrderFlowColors.Ink, fontWeight = FontWeight.Bold)
            Text("Total ${presentation.totalValueLabel}", color = OrderFlowColors.Faint, fontSize = 12.sp)
        }
        Text(
            presentation.feePayerLabel,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(OrderFlowColors.GreenSoft)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = OrderFlowColors.Green,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
```

In `CreditScreen.kt`, pass the existing total:

```kotlin
InstallmentRow(
    amount = amount,
    installment = installment,
    isSelected = selectedInstallment == installment.installmentNumber,
    onClick = { onSelectInstallment(installment.installmentNumber) },
)
```

- [ ] **Step 6: Compile and run the presentation tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
```

Expected: PASS and Compose production sources compile.

- [ ] **Step 7: Commit the checkout presentation**

```powershell
git add -- app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt app/src/main/java/com/detrapay/ui/home/orders/screens/CreditScreen.kt app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt
git commit -m "fix: display store-paid PagBank installments"
```

### Task 3: Enforce exact PagBank amount and seller installments

**Files:**
- Modify: `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`
- Modify: `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`

**Interfaces:**
- Consumes: `OrderPaymentRequest.amount` and `PaymentAttempt.amountOriginal/amountFinal`.
- Produces: `PlugPagPaymentData` whose amount is `OrderPaymentRequest.amount` in cents and whose installment type is seller-paid for two or more installments.

- [ ] **Step 1: Make the payment-attempt fixture configurable**

Change the test fixture to:

```kotlin
private fun attempt(
    amountOriginal: Double = 25.67,
    amountFinal: Double = 25.67,
) = PaymentAttempt(
    id = "attempt-1",
    status = "prepared",
    orderId = 10,
    paymentMethodId = 1,
    amountOriginal = amountOriginal,
    amountFinal = amountFinal,
    installments = 3,
    expiresAt = "2026-07-29T03:30:00Z",
)
```

- [ ] **Step 2: Add failing surcharge and canonical-total tests**

Add to `PaymentDialogViewModelTest.kt`:

```kotlin
@Test
fun `prepared surcharge never opens PagBank or records payment`() {
    coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
        Result.Success(attempt(amountFinal = 27.00))

    viewModel.payOrder(request("credito", online = true, installments = 3), "SER123")

    val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
    assertTrue(state.message?.contains("valor diferente") == true)
    verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
    coVerify(exactly = 0) { orderRepository.updatePaymentAttemptSplitConfig(any(), any()) }
    coVerify(exactly = 0) { orderRepository.recordApprovedOnlinePayment(any(), any()) }
}

@Test
fun `approved payment uses displayed amount as canonical total`() {
    val paymentSlot = slot<PlugPagPaymentData>()
    coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
        Result.Success(attempt(amountOriginal = 25.674, amountFinal = 25.674))
    coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
        Result.Success(Unit)
    every { plugPag.isAuthenticated() } returns true
    every { plugPag.doPayment(capture(paymentSlot)) } returns approvedTransaction()
    coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
        Result.Success(TestOrderFixtures.order())

    viewModel.payOrder(request("credito", online = true, installments = 3), "SER123")

    val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
    assertEquals(25.67, state.data?.amountFinal ?: 0.0, 0.0)
    assertEquals(2567, readInt(paymentSlot.captured, "amount"))
    assertEquals(
        PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR,
        readInt(paymentSlot.captured, "installmentType"),
    )
}
```

- [ ] **Step 3: Run both new payment tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest.prepared surcharge never opens PagBank or records payment" --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest.approved payment uses displayed amount as canonical total"
```

Expected: FAIL because production currently calls the split configuration for a surcharge and publishes the backend raw `amountFinal` instead of the displayed total.

- [ ] **Step 4: Validate integer cents and use the requested amount throughout**

In the success branch of `payOrder`, replace the direct call with:

```kotlin
is Result.Success -> {
    val requestedCents = amountInCents(request.amount)
    val preparedOriginalCents = amountInCents(prepared.data.amountOriginal)
    val preparedFinalCents = amountInCents(prepared.data.amountFinal)
    if (preparedOriginalCents != requestedCents || preparedFinalCents != requestedCents) {
        finishWithError(
            "O backend preparou um valor diferente do exibido. O pagamento foi bloqueado para nao cobrar acrescimo do cliente.",
        )
    } else {
        startPagBank(request, prepared.data.id, serial)
    }
}
```

Change the `startPagBank` signature from:

```kotlin
private suspend fun startPagBank(
    request: OrderPaymentRequest,
    attemptId: String,
    amountFinal: Double,
    serial: String,
)
```

to:

```kotlin
private suspend fun startPagBank(
    request: OrderPaymentRequest,
    attemptId: String,
    serial: String,
)
```

Replace the PagBank amount expression:

```kotlin
(amountFinal * 100).roundToInt()
```

with:

```kotlin
amountInCents(request.amount)
```

Replace both calls:

```kotlin
saveTransactionLog(request, result, amountFinal)
```

with:

```kotlin
saveTransactionLog(request, result)
```

Replace:

```kotlin
amountFinal = amountFinal,
```

with:

```kotlin
amountFinal = request.amount,
```

Use `request.amount` for `PaymentData.amountFinal` and transaction logging. Change `saveTransactionLog` to remove its `amountFinal` parameter and use `request.amount` internally:

```kotlin
private suspend fun saveTransactionLog(
    request: OrderPaymentRequest,
    result: PlugPagTransactionResult,
) {
    paymentRepository.saveTransaction(
        orderId = request.order.id,
        amount = request.amount,
        installments = request.installments,
        paymentType = request.paymentMethod.name,
        transactionId = result.transactionId,
        transactionCode = result.transactionCode,
        date = result.date,
        result = result.result,
        cardBrand = result.cardBrand,
        cardLast4 = result.holder,
        cardHolder = result.holderName,
        pixTxIdCode = result.pixTxIdCode,
        message = result.message,
        errorCode = result.errorCode,
    )
}
```

Add near `installmentType`:

```kotlin
private fun amountInCents(amount: Double): Int = (amount * 100).roundToInt()
```

- [ ] **Step 5: Run all payment ViewModel tests and verify GREEN**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest"
```

Expected: PASS; surcharge test observes no split call, no terminal call, and no persistence call.

- [ ] **Step 6: Commit the terminal boundary protection**

```powershell
git add -- app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt
git commit -m "fix: keep PagBank fees with the store"
```

### Task 4: Allow eligible offline payments to be deleted from order details

**Files:**
- Modify: `app/src/main/java/com/detrapay/data/model/OrderReceivableItemDeleteRules.kt`
- Create: `app/src/test/java/com/detrapay/data/model/OrderReceivableItemDeleteRulesTest.kt`
- Modify: `app/src/test/java/com/detrapay/ui/order_details/OrderDetailsViewModelTest.kt`

**Interfaces:**
- Consumes: `OrderReceivableItem.paymentMethod.isOnlinePayment` and `OrderReceivableItem.status`.
- Produces: `OrderReceivableItem.canBeDeleted(): Boolean`, shared by the details adapter, Activity, ViewModel, and repository.

- [ ] **Step 1: Add failing domain-rule tests**

Create `OrderReceivableItemDeleteRulesTest.kt`:

```kotlin
package com.detrapay.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderReceivableItemDeleteRulesTest {
    @Test
    fun `paid offline payment can be deleted`() {
        assertTrue(receivable(online = false, status = OrderReceivableItemStatus.PAID).canBeDeleted())
    }

    @Test
    fun `pending offline payment can be deleted`() {
        assertTrue(receivable(online = false, status = OrderReceivableItemStatus.PENDING).canBeDeleted())
    }

    @Test
    fun `online payment cannot be deleted in any payable status`() {
        assertFalse(receivable(online = true, status = OrderReceivableItemStatus.PENDING).canBeDeleted())
        assertFalse(receivable(online = true, status = OrderReceivableItemStatus.PAID).canBeDeleted())
    }

    @Test
    fun `refunded or cancelled offline payment cannot be deleted`() {
        assertFalse(receivable(online = false, status = OrderReceivableItemStatus.REFUNDED).canBeDeleted())
        assertFalse(receivable(online = false, status = OrderReceivableItemStatus.CANCELLED).canBeDeleted())
    }

    private fun receivable(
        online: Boolean,
        status: OrderReceivableItemStatus,
    ) = OrderReceivableItem(
        id = 20,
        documentId = "receivable-20",
        amountOriginal = 100.0,
        amountFinal = 100.0,
        installments = 1,
        status = status,
        paymentMethod = PaymentMethod(
            id = 2,
            name = if (online) "Pix" else "Dinheiro",
            installments = 1,
            interestTax = 0.0,
            paymentType = if (online) "pix" else "cash",
            isOnlinePayment = online,
        ),
        paymentDate = null,
        refundDate = null,
        cardLast4 = null,
        cardHolder = null,
        tax = null,
        cardBrand = null,
        authorizationId = null,
        authorizationCode = null,
        pixTxIdCode = null,
    )
}
```

- [ ] **Step 2: Run the domain-rule tests and verify RED**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.data.model.OrderReceivableItemDeleteRulesTest"
```

Expected: FAIL because the current name/type heuristic still permits pending online payments and does not reject cancelled offline payments.

- [ ] **Step 3: Replace the heuristic with the explicit backend flag**

Replace `canBeDeleted` with:

```kotlin
fun OrderReceivableItem.canBeDeleted(): Boolean {
    return !paymentMethod.isOnlinePayment &&
        status != OrderReceivableItemStatus.REFUNDED &&
        status != OrderReceivableItemStatus.CANCELLED
}
```

- [ ] **Step 4: Align ViewModel regression tests with the shared rule**

Rename `cancelPendingItem allows paid pix receivable` to `cancelPendingItem rejects paid pix receivable`, remove its repository success stub, and assert:

```kotlin
val state = viewModel.orderState.getOrAwaitValueMatching { it is UIState.Error<*> } as UIState.Error
assertEquals("Este pagamento nao pode ser excluido no status atual.", state.message)
coVerify(exactly = 0) { orderRepository.cancelPendingReceivable(any(), any()) }
```

Keep `cancelPendingItem allows paid cash receivable` unchanged because paid offline corrections are explicitly supported.

- [ ] **Step 5: Run domain and details ViewModel tests and verify GREEN**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.data.model.OrderReceivableItemDeleteRulesTest" --tests "com.detrapay.ui.order_details.OrderDetailsViewModelTest"
```

Expected: PASS. Because the adapter, Activity, ViewModel, and repository already call `canBeDeleted`, no duplicated UI eligibility change is needed.

- [ ] **Step 6: Commit the offline deletion behavior**

```powershell
git add -- app/src/main/java/com/detrapay/data/model/OrderReceivableItemDeleteRules.kt app/src/test/java/com/detrapay/data/model/OrderReceivableItemDeleteRulesTest.kt app/src/test/java/com/detrapay/ui/order_details/OrderDetailsViewModelTest.kt
git commit -m "fix: allow deleting offline order payments"
```

### Task 5: Verify the complete change on JVM and device

**Files:**
- Verify only; do not modify unrelated files.

**Interfaces:**
- Consumes: the completed Tasks 1-3.
- Produces: test, install, launch, UI inspection, and crash-log evidence.

- [ ] **Step 1: Run focused regression tests**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest" --tests "com.detrapay.ui.home.orders.OrdersViewModelTest" --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest" --tests "com.detrapay.data.model.OrderReceivableItemDeleteRulesTest" --tests "com.detrapay.ui.order_details.OrderDetailsViewModelTest"
```

Expected: all focused tests PASS.

- [ ] **Step 2: Run the complete debug unit-test suite**

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` with no failing tests.

- [ ] **Step 3: Check the final diff without disturbing local work**

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; pre-existing unrelated files remain untouched.

- [ ] **Step 4: Confirm a device and Android tooling are available**

```powershell
adb devices
android --version
```

Expected: one authorized Android device and an available Android CLI. If `android` is unavailable, continue with the project-required Gradle/ADB sequence because it is already installed in the workspace environment.

- [ ] **Step 5: Clear logs, install, open, and immediately inspect filtered logs**

```powershell
adb logcat -c
.\gradlew.bat installDebug
adb shell am start -n com.detrapay/.ui.splash.SplashActivity
adb logcat -d | Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"
```

Expected: installation succeeds, `SplashActivity` starts, and filtered output contains no `AndroidRuntime` crash or `FATAL EXCEPTION`.

- [ ] **Step 6: Inspect the visible app layout**

```powershell
android layout --pretty
```

Expected: the application is visible and responsive. If the Android CLI cannot inspect this device, use `adb shell uiautomator dump` as read-only fallback and record the limitation.

- [ ] **Step 7: Report the backend contract requirement**

State explicitly in the handoff:

```text
POST /orders/{orderId}/payment-attempts must return amount_original and amount_final equal to the checkout total in integer cents when PagBank fees are paid by the store. A different value is now rejected before the terminal opens, preventing the UI from displaying one amount while charging another.
```

No extra commit is required for verification-only evidence.
