# Home Direct Checkout Compose Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the Home direct checkout slice into an organized Compose-first surface while keeping `HomeActivity`, `NavHostFragment`, and `home_navigation.xml` as temporary hosts.

**Architecture:** `DirectCheckoutFragment` remains the Android integration boundary. `DirectCheckoutRoute` owns ViewModel observation and local route state, `DirectCheckoutScreen` renders immutable `DirectCheckoutUiState`, and small screen/component composables replace the current giant `DirectCheckoutFlowScreen.kt`.

**Tech Stack:** Kotlin, AndroidX Fragment, Hilt ViewModels, LiveData, Jetpack Compose Material3/Foundation, JUnit4.

## Global Constraints

- This slice covers only Home shell behavior needed to reach direct checkout and the direct checkout mode itself.
- Do not migrate legacy Home tabs (`Registro`, `Meus pedidos`, `Historico`, `Perfil`).
- Do not migrate registration flow, payment details flow, reports, or login.
- Keep `HomeActivity`, `NavHostFragment`, and `home_navigation.xml`.
- Do not introduce Navigation Compose.
- Do not change backend contracts.
- Do not change payment SDK/dialog behavior.
- Do not mask missing backend fields with client workarounds.
- Keep XML resources that are still used by other flows.
- Preserve unrelated dirty worktree changes.
- Do not run Gradle tasks in parallel in this project.
- Final Android validation, when needed, uses `.\scripts\dev-device.ps1` and then logcat monitoring for `com.detrapay`, `AndroidRuntime`, and `FATAL EXCEPTION`.

---

## File Structure

Create:

- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutContract.kt`
  - Defines UI state, local state, actions, and Android effects.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducer.kt`
  - Pure helpers for local direct checkout state transitions.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutScreen.kt`
  - Stateless Compose shell that routes to each screen by step.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutRoute.kt`
  - Compose route that observes ViewModels and maps `DirectCheckoutAction` to state updates, ViewModel calls, or effects.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/components/DirectCheckoutBlocks.kt`
  - Shared loading, empty, nav bar, method icon, and amount card widgets.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/components/DirectCheckoutColors.kt`
  - Shared color object currently embedded in `DirectCheckoutFlowScreen.kt`.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/OrdersScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/DetailScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/KeypadScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/MethodScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/CreditScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/DebitScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/WaitingScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/InstallmentSimulatorScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutPreviewFixtures.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutScreenPreview.kt`
- `app/src/test/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducerTest.kt`

Modify:

- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutFragment.kt`
  - Replace direct local `mutableStateOf` screen state with route invocation and Android effect handling.
- `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreen.kt`
  - Delete after all composables are moved.
- `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreenPreview.kt`
  - Delete after previews move to `ui/home/direct_checkout`.
- `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutOrderPresentation.kt`
  - Keep in place for this slice to avoid widening imports and tests.

---

### Task 1: Contract And Reducer

**Files:**
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutContract.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducer.kt`
- Test: `app/src/test/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducerTest.kt`

**Interfaces:**
- Consumes: `com.detrapay.data.model.Order`, `com.detrapay.data.model.remote.InstallmentFee`, `com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation`
- Produces:
  - `enum class DirectCheckoutStep`
  - `data class DirectCheckoutLocalState`
  - `data class DirectCheckoutUiState`
  - `sealed interface DirectCheckoutAction`
  - `sealed interface DirectCheckoutEffect`
  - `object DirectCheckoutReducer`

- [ ] **Step 1: Write the failing reducer test**

Create `app/src/test/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducerTest.kt`:

```kotlin
package com.detrapay.ui.home.direct_checkout

import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderItem
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DirectCheckoutReducerTest {

    @Test
    fun `startPayment selects order and opens keypad with missing amount digits`() {
        val order = order(total = 2570.18, paidAmount = 1200.0)

        val state = DirectCheckoutReducer.startPayment(DirectCheckoutLocalState(), order)

        assertEquals(order, state.selectedOrder)
        assertEquals(DirectCheckoutStep.Keypad, state.step)
        assertEquals("137018", state.paymentDigits)
        assertEquals("", state.selectedPaymentType)
        assertEquals(1, state.selectedInstallment)
        assertFalse(state.showSimulator)
    }

    @Test
    fun `showDetail selects order and opens detail`() {
        val order = order()

        val state = DirectCheckoutReducer.showDetail(DirectCheckoutLocalState(), order)

        assertEquals(order, state.selectedOrder)
        assertEquals(DirectCheckoutStep.Detail, state.step)
    }

    @Test
    fun `payment key delegates to presentation rules`() {
        val state = DirectCheckoutLocalState(paymentDigits = "12")

        val next = DirectCheckoutReducer.applyPaymentKey(state, "DEL")

        assertEquals("1", next.paymentDigits)
    }

    @Test
    fun `open methods moves from keypad to method`() {
        val state = DirectCheckoutLocalState(step = DirectCheckoutStep.Keypad)

        val next = DirectCheckoutReducer.openMethods(state)

        assertEquals(DirectCheckoutStep.Method, next.step)
    }

    @Test
    fun `select credit resets installment and opens credit`() {
        val state = DirectCheckoutLocalState(selectedInstallment = 4)

        val next = DirectCheckoutReducer.selectPaymentType(state, "credito")

        assertEquals("credito", next.selectedPaymentType)
        assertEquals(1, next.selectedInstallment)
        assertEquals(DirectCheckoutStep.Credit, next.step)
    }

    @Test
    fun `select debit opens debit`() {
        val state = DirectCheckoutLocalState()

        val next = DirectCheckoutReducer.selectPaymentType(state, "debito")

        assertEquals("debito", next.selectedPaymentType)
        assertEquals(DirectCheckoutStep.Debit, next.step)
    }

    @Test
    fun `select manual method opens waiting`() {
        val state = DirectCheckoutLocalState()

        val next = DirectCheckoutReducer.selectPaymentType(state, "pix")

        assertEquals("pix", next.selectedPaymentType)
        assertEquals(DirectCheckoutStep.Waiting, next.step)
    }

    @Test
    fun `back follows current direct checkout step order`() {
        assertEquals(
            DirectCheckoutStep.Orders,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Detail)).step,
        )
        assertEquals(
            DirectCheckoutStep.Orders,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Keypad)).step,
        )
        assertEquals(
            DirectCheckoutStep.Keypad,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Method)).step,
        )
        assertEquals(
            DirectCheckoutStep.Method,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Credit)).step,
        )
        assertEquals(
            DirectCheckoutStep.Method,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Debit)).step,
        )
        assertEquals(
            DirectCheckoutStep.Method,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Waiting)).step,
        )
    }

    @Test
    fun `simulator state opens closes and updates amount`() {
        val opened = DirectCheckoutReducer.openSimulator(
            DirectCheckoutLocalState(simulatorAmountDigits = "123", simulatorSelectedInstallment = 2),
        )

        assertEquals(true, opened.showSimulator)
        assertEquals("", opened.simulatorAmountDigits)
        assertNull(opened.simulatorSelectedInstallment)

        val updated = DirectCheckoutReducer.updateSimulatorAmount(opened, "R$ 45,67")

        assertEquals("4567", updated.simulatorAmountDigits)
        assertEquals(emptyList<Any>(), updated.simulatorInstallments)
        assertNull(updated.simulatorSelectedInstallment)

        val closed = DirectCheckoutReducer.closeSimulator(updated)

        assertFalse(closed.showSimulator)
        assertFalse(closed.simulatorRequestActive)
    }

    private fun order(
        total: Double = 100.0,
        paidAmount: Double = 0.0,
    ): Order {
        val receivables = if (paidAmount > 0.0) {
            listOf(
                OrderReceivableItem(
                    id = 1,
                    documentId = "rec-1",
                    amountOriginal = paidAmount,
                    amountFinal = paidAmount,
                    installments = 1,
                    status = OrderReceivableItemStatus.PAID,
                    paymentMethod = PaymentMethod(1, "Pix", 1, 0.0, "pix"),
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
            )
        } else {
            emptyList()
        }

        return Order(
            id = 10,
            serviceName = "Servico",
            status = OrderStatus.PENDING,
            creationDate = "2026-07-22T10:00:00",
            vehiclePrice = total,
            billingDate = "2026-07-22",
            originalAmount = total,
            currentAmount = total - paidAmount,
            isVehicleFinanced = false,
            isVehicleSpecialPlate = false,
            customer = OrderCustomer(10, "Cliente", "12345678901", "11999999999", null),
            vehicleType = VehicleType(1, "Carro"),
            items = listOf(OrderItem(1, total, 0.0, null, "Item", total)),
            receivables = receivables,
            salesman = Salesman(1, "Vendedor"),
        )
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --no-watch-fs
```

Expected: FAIL because `DirectCheckoutReducer`, `DirectCheckoutLocalState`, and `DirectCheckoutStep` do not exist.

- [ ] **Step 3: Create the contract**

Create `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutContract.kt`:

```kotlin
package com.detrapay.ui.home.direct_checkout

import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.simplified.DirectCheckoutPendingPayment

enum class DirectCheckoutStep {
    Orders,
    Detail,
    Keypad,
    Method,
    Credit,
    Debit,
    Waiting,
}

data class DirectCheckoutLocalState(
    val step: DirectCheckoutStep = DirectCheckoutStep.Orders,
    val selectedOrder: Order? = null,
    val paymentDigits: String = "",
    val selectedPaymentType: String = "",
    val selectedInstallment: Int = 1,
    val creditInstallments: List<InstallmentFee> = emptyList(),
    val feesLoading: Boolean = false,
    val feesError: String? = null,
    val showSimulator: Boolean = false,
    val simulatorAmountDigits: String = "",
    val simulatorInstallments: List<InstallmentFee> = emptyList(),
    val simulatorSelectedInstallment: Int? = null,
    val simulatorLoading: Boolean = false,
    val simulatorError: String? = null,
    val simulatorRequestActive: Boolean = false,
)

data class DirectCheckoutUiState(
    val companyName: String,
    val companyDocument: String,
    val orders: List<Order>,
    val isLoading: Boolean,
    val errorMessage: String?,
    val availablePaymentTypes: List<String>,
    val local: DirectCheckoutLocalState,
)

sealed interface DirectCheckoutAction {
    data object Logout : DirectCheckoutAction
    data object Reload : DirectCheckoutAction
    data object NewOrder : DirectCheckoutAction
    data class OrderPay(val order: Order) : DirectCheckoutAction
    data class OrderDetail(val order: Order) : DirectCheckoutAction
    data object Back : DirectCheckoutAction
    data class Key(val value: String) : DirectCheckoutAction
    data object OpenMethods : DirectCheckoutAction
    data class SelectPaymentType(val paymentType: String) : DirectCheckoutAction
    data class SelectInstallment(val installment: Int) : DirectCheckoutAction
    data object ContinueCredit : DirectCheckoutAction
    data object ContinueDebit : DirectCheckoutAction
    data object OpenSimulator : DirectCheckoutAction
    data object CloseSimulator : DirectCheckoutAction
    data class SimulatorAmountChange(val raw: String) : DirectCheckoutAction
    data object ConsultSimulator : DirectCheckoutAction
    data class SelectSimulatorInstallment(val installment: Int) : DirectCheckoutAction
    data class CopySimulator(val text: String) : DirectCheckoutAction
    data class ShareSimulator(val text: String) : DirectCheckoutAction
}

sealed interface DirectCheckoutEffect {
    data object ShowLogoutConfirmation : DirectCheckoutEffect
    data object NavigateToRegistration : DirectCheckoutEffect
    data class OpenPaymentDialog(val pendingPayment: DirectCheckoutPendingPayment) : DirectCheckoutEffect
    data class ConfirmManualPayment(
        val pendingPayment: DirectCheckoutPendingPayment,
        val paymentData: PaymentData,
    ) : DirectCheckoutEffect
    data class CopySimulatorText(val text: String) : DirectCheckoutEffect
    data class ShareSimulatorText(val text: String) : DirectCheckoutEffect
    data class ShowToast(val message: String, val long: Boolean = true) : DirectCheckoutEffect
    data class ShowSessionExpired(val exception: Exception) : DirectCheckoutEffect
}
```

- [ ] **Step 4: Create the reducer**

Create `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducer.kt`:

```kotlin
package com.detrapay.ui.home.direct_checkout

import com.detrapay.data.model.Order
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.util.PaymentTypeRules
import kotlin.math.roundToLong

object DirectCheckoutReducer {

    fun showDetail(state: DirectCheckoutLocalState, order: Order): DirectCheckoutLocalState {
        return state.copy(selectedOrder = order, step = DirectCheckoutStep.Detail)
    }

    fun startPayment(state: DirectCheckoutLocalState, order: Order): DirectCheckoutLocalState {
        val missingAmount = DirectCheckoutOrderPresentation.summary(order).missingAmount
        return state.copy(
            selectedOrder = order,
            paymentDigits = (missingAmount * 100).roundToLong().toString(),
            selectedPaymentType = "",
            selectedInstallment = 1,
            creditInstallments = emptyList(),
            feesLoading = false,
            feesError = null,
            showSimulator = false,
            step = DirectCheckoutStep.Keypad,
        )
    }

    fun applyPaymentKey(state: DirectCheckoutLocalState, key: String): DirectCheckoutLocalState {
        return state.copy(
            paymentDigits = DirectCheckoutOrderPresentation.nextPaymentDigits(state.paymentDigits, key),
        )
    }

    fun openMethods(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(step = DirectCheckoutStep.Method)
    }

    fun selectPaymentType(state: DirectCheckoutLocalState, paymentType: String): DirectCheckoutLocalState {
        val normalized = PaymentTypeRules.normalize(paymentType)
        return when (normalized) {
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT -> state.copy(
                selectedPaymentType = paymentType,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = true,
                feesError = null,
                simulatorRequestActive = false,
                step = DirectCheckoutStep.Credit,
            )
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT -> state.copy(
                selectedPaymentType = paymentType,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = false,
                feesError = null,
                step = DirectCheckoutStep.Debit,
            )
            else -> state.copy(
                selectedPaymentType = paymentType,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = false,
                feesError = null,
                step = DirectCheckoutStep.Waiting,
            )
        }
    }

    fun selectInstallment(state: DirectCheckoutLocalState, installment: Int): DirectCheckoutLocalState {
        return state.copy(selectedInstallment = installment)
    }

    fun back(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        val nextStep = when (state.step) {
            DirectCheckoutStep.Orders -> DirectCheckoutStep.Orders
            DirectCheckoutStep.Detail -> DirectCheckoutStep.Orders
            DirectCheckoutStep.Keypad -> DirectCheckoutStep.Orders
            DirectCheckoutStep.Method -> DirectCheckoutStep.Keypad
            DirectCheckoutStep.Credit,
            DirectCheckoutStep.Debit -> DirectCheckoutStep.Method
            DirectCheckoutStep.Waiting -> DirectCheckoutStep.Method
        }
        return state.copy(step = nextStep)
    }

    fun openSimulator(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(
            showSimulator = true,
            simulatorRequestActive = false,
            simulatorAmountDigits = "",
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorLoading = false,
            simulatorError = null,
        )
    }

    fun closeSimulator(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(
            showSimulator = false,
            simulatorRequestActive = false,
            simulatorLoading = false,
            simulatorError = null,
        )
    }

    fun updateSimulatorAmount(state: DirectCheckoutLocalState, raw: String): DirectCheckoutLocalState {
        return state.copy(
            simulatorAmountDigits = raw.filter(Char::isDigit),
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorError = null,
            simulatorRequestActive = false,
        )
    }

    fun startSimulatorLoading(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(
            simulatorRequestActive = true,
            simulatorLoading = true,
            simulatorError = null,
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
        )
    }

    fun simulatorLoaded(
        state: DirectCheckoutLocalState,
        installments: List<InstallmentFee>,
        emptyMessage: String,
    ): DirectCheckoutLocalState {
        return state.copy(
            simulatorLoading = false,
            simulatorRequestActive = false,
            simulatorInstallments = installments,
            simulatorSelectedInstallment = installments.lastOrNull()?.installmentNumber,
            simulatorError = if (installments.isEmpty()) emptyMessage else null,
        )
    }

    fun simulatorFailed(state: DirectCheckoutLocalState, message: String): DirectCheckoutLocalState {
        return state.copy(
            simulatorLoading = false,
            simulatorRequestActive = false,
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorError = message,
        )
    }

    fun feesLoaded(
        state: DirectCheckoutLocalState,
        installments: List<InstallmentFee>,
        emptyMessage: String,
    ): DirectCheckoutLocalState {
        return state.copy(
            feesLoading = false,
            creditInstallments = installments,
            selectedInstallment = installments.firstOrNull()?.installmentNumber ?: 1,
            feesError = if (installments.isEmpty()) emptyMessage else null,
        )
    }

    fun feesFailed(state: DirectCheckoutLocalState, message: String): DirectCheckoutLocalState {
        return state.copy(
            feesLoading = false,
            creditInstallments = emptyList(),
            feesError = message,
        )
    }
}
```

- [ ] **Step 5: Run the reducer test**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --no-watch-fs
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutContract.kt app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducer.kt app/src/test/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducerTest.kt
git commit -m "test: add direct checkout reducer contract"
```

---

### Task 2: Stateless Screen Shell

**Files:**
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/components/DirectCheckoutColors.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreen.kt`

**Interfaces:**
- Consumes: `DirectCheckoutUiState`, `DirectCheckoutAction`, existing private composables in `DirectCheckoutFlowScreen.kt`
- Produces:
  - `@Composable fun DirectCheckoutScreen(state: DirectCheckoutUiState, onAction: (DirectCheckoutAction) -> Unit)`
  - `internal object DirectCheckoutColors`

- [ ] **Step 1: Create shared colors**

Create `app/src/main/java/com/detrapay/ui/home/direct_checkout/components/DirectCheckoutColors.kt` by moving the values from the current private `FigmaColors` object:

```kotlin
package com.detrapay.ui.home.direct_checkout.components

import androidx.compose.ui.graphics.Color

internal object DirectCheckoutColors {
    val Background = Color(0xFFF5F7FF)
    val Ink = Color(0xFF0F172A)
    val Text = Color(0xFF334155)
    val Muted = Color(0xFF64748B)
    val Faint = Color(0xFF94A3B8)
    val Pale = Color(0xFFCBD5E1)
    val Border = Color(0xFFE2E8F0)
    val Track = Color(0xFFF1F5F9)
    val Key = Color(0xFFF8FAFC)
    val MutedSurface = Color(0xFFF1F5F9)
    val Blue = Color(0xFF2563EB)
    val BlueLight = Color(0xFF60A5FA)
    val BlueSoft = Color(0xFFEFF6FF)
    val BlueBorder = Color(0xFFDBEAFE)
    val BlueOnSoft = Color(0xFFBFDBFE)
    val IndigoSoft = Color(0xFFEEF2FF)
    val Amber = Color(0xFFFBBF24)
    val AmberSoft = Color(0xFFFFFBEB)
    val AmberText = Color(0xFFD97706)
    val Warning = Color(0xFFDB9101)
    val WarningSoft = Color(0xFFFEF3C7)
    val WarningText = Color(0xFF92400E)
    val Green = Color(0xFF10B981)
    val GreenSoft = Color(0xFFECFDF5)
    val Red = Color(0xFFCA170B)
    val RedSoft = Color(0xFFFEE2E2)
    val Teal = Color(0xFF0D9488)
    val Purple = Color(0xFF7C3AED)
    val WhatsappGreen = Color(0xFF25D366)
}
```

- [ ] **Step 2: Create the stateless screen shell**

Create `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutScreen.kt`:

```kotlin
package com.detrapay.ui.home.direct_checkout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.simplified.DirectCheckoutFlowScreen

@Composable
fun DirectCheckoutScreen(
    state: DirectCheckoutUiState,
    onAction: (DirectCheckoutAction) -> Unit,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DirectCheckoutColors.Background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DirectCheckoutFlowScreen(
                    companyName = state.companyName,
                    companyDocument = state.companyDocument,
                    orders = state.orders,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    availablePaymentTypes = state.availablePaymentTypes,
                    step = com.detrapay.ui.home.simplified.DirectCheckoutStep.valueOf(state.local.step.name),
                    selectedOrder = state.local.selectedOrder,
                    paymentDigits = state.local.paymentDigits,
                    selectedPaymentType = state.local.selectedPaymentType,
                    creditInstallments = state.local.creditInstallments,
                    selectedInstallment = state.local.selectedInstallment,
                    isFeesLoading = state.local.feesLoading,
                    feesError = state.local.feesError,
                    showSimulator = state.local.showSimulator,
                    simulatorAmountDigits = state.local.simulatorAmountDigits,
                    simulatorInstallments = state.local.simulatorInstallments,
                    simulatorSelectedInstallment = state.local.simulatorSelectedInstallment,
                    isSimulatorLoading = state.local.simulatorLoading,
                    simulatorError = state.local.simulatorError,
                    onLogout = { onAction(DirectCheckoutAction.Logout) },
                    onReload = { onAction(DirectCheckoutAction.Reload) },
                    onNewOrder = { onAction(DirectCheckoutAction.NewOrder) },
                    onOrderPay = { onAction(DirectCheckoutAction.OrderPay(it)) },
                    onOrderDetail = { onAction(DirectCheckoutAction.OrderDetail(it)) },
                    onBack = { onAction(DirectCheckoutAction.Back) },
                    onKey = { onAction(DirectCheckoutAction.Key(it)) },
                    onOpenMethods = { onAction(DirectCheckoutAction.OpenMethods) },
                    onSelectPaymentType = { onAction(DirectCheckoutAction.SelectPaymentType(it)) },
                    onSelectInstallment = { onAction(DirectCheckoutAction.SelectInstallment(it)) },
                    onContinueCredit = { onAction(DirectCheckoutAction.ContinueCredit) },
                    onContinueDebit = { onAction(DirectCheckoutAction.ContinueDebit) },
                    onOpenSimulator = { onAction(DirectCheckoutAction.OpenSimulator) },
                    onCloseSimulator = { onAction(DirectCheckoutAction.CloseSimulator) },
                    onSimulatorAmountChange = { onAction(DirectCheckoutAction.SimulatorAmountChange(it)) },
                    onConsultSimulator = { onAction(DirectCheckoutAction.ConsultSimulator) },
                    onSelectSimulatorInstallment = { onAction(DirectCheckoutAction.SelectSimulatorInstallment(it)) },
                    onCopySimulator = { onAction(DirectCheckoutAction.CopySimulator(it)) },
                    onShareSimulator = { onAction(DirectCheckoutAction.ShareSimulator(it)) },
                )
            }
        }
    }
}
```

This compatibility shell deliberately delegates to the existing screen. Later tasks replace this delegation with the split screen files.

- [ ] **Step 3: Compile Kotlin**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --no-watch-fs
```

Expected: PASS.

- [ ] **Step 4: Commit**

Run:

```powershell
git add app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutScreen.kt app/src/main/java/com/detrapay/ui/home/direct_checkout/components/DirectCheckoutColors.kt
git commit -m "feat: add direct checkout compose screen contract"
```

---

### Task 3: Route And Thin Fragment Boundary

**Files:**
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutRoute.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutFragment.kt`

**Interfaces:**
- Consumes:
  - `HomeViewModel.homeState`
  - `SimplifiedReceivableListViewModel.directOrderListState`
  - `SimplifiedReceivableListViewModel.paymentMethodsState`
  - `SimplifiedReceivableListViewModel.calculateFeesState`
  - `SimplifiedReceivableListViewModel.pendingPaymentState`
  - `SimplifiedReceivableListViewModel.manualPaymentState`
- Produces:
  - `@Composable fun DirectCheckoutRoute(homeViewModel: HomeViewModel, viewModel: SimplifiedReceivableListViewModel, defaultCompanyName: String, defaultCompanyDocument: String, directCheckoutErrorMessage: String, installmentErrorMessage: String, addPaymentLoadErrorMessage: String, paymentSuccessMessage: String, invalidSimulatorAmountMessage: String, onEffect: (DirectCheckoutEffect) -> Unit)`

- [ ] **Step 1: Create the route**

Create `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutRoute.kt`:

```kotlin
package com.detrapay.ui.home.direct_checkout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
import com.detrapay.ui.home.simplified.DirectCheckoutPendingPayment
import com.detrapay.ui.home.simplified.SimplifiedReceivableListViewModel
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.PaymentTypeRules
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectCheckoutRoute(
    homeViewModel: HomeViewModel,
    viewModel: SimplifiedReceivableListViewModel,
    defaultCompanyName: String,
    defaultCompanyDocument: String,
    directCheckoutErrorMessage: String,
    installmentErrorMessage: String,
    addPaymentLoadErrorMessage: String,
    paymentSuccessMessage: String,
    invalidSimulatorAmountMessage: String,
    onEffect: (DirectCheckoutEffect) -> Unit,
) {
    var localState by remember { mutableStateOf(DirectCheckoutLocalState()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var availableTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    var companyName by remember { mutableStateOf(defaultCompanyName) }
    var companyDocument by remember { mutableStateOf(defaultCompanyDocument) }

    val homeState by homeViewModel.homeState.observeAsState()
    val orderState by viewModel.directOrderListState.observeAsState()
    val paymentMethodsState by viewModel.paymentMethodsState.observeAsState()
    val feesState by viewModel.calculateFeesState.observeAsState()
    val pendingPaymentState by viewModel.pendingPaymentState.observeAsState()
    val manualPaymentState by viewModel.manualPaymentState.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDirectCheckoutOrders(forceRefresh = false)
    }

    LaunchedEffect(homeState) {
        val data = (homeState as? UIState.Success)?.data ?: return@LaunchedEffect
        companyName = data.companyName.ifBlank { defaultCompanyName }
        companyDocument = formatCnpj(data.companyDocument).ifBlank { defaultCompanyDocument }
    }

    LaunchedEffect(orderState) {
        when (val state = orderState) {
            is UIState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            is UIState.Success -> {
                isLoading = false
                errorMessage = null
                orders = state.data.orEmpty()
            }
            is UIState.Error -> {
                isLoading = false
                orders = emptyList()
                errorMessage = state.message ?: directCheckoutErrorMessage
                state.exception?.let { onEffect(DirectCheckoutEffect.ShowSessionExpired(it)) }
            }
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(paymentMethodsState) {
        when (val state = paymentMethodsState) {
            is UIState.Success -> availableTypes = viewModel.availablePaymentTypes()
            is UIState.Error -> onEffect(
                DirectCheckoutEffect.ShowToast(state.message ?: addPaymentLoadErrorMessage),
            )
            is UIState.Loading,
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(feesState) {
        when (val state = feesState) {
            is UIState.Loading -> {
                localState = if (localState.simulatorRequestActive || localState.showSimulator) {
                    localState.copy(simulatorLoading = true, simulatorError = null)
                } else {
                    localState.copy(feesLoading = true, feesError = null)
                }
            }
            is UIState.Success -> {
                val installments = state.data?.data.orEmpty().firstOrNull()?.installments.orEmpty()
                localState = if (localState.simulatorRequestActive || localState.showSimulator) {
                    DirectCheckoutReducer.simulatorLoaded(localState, installments, installmentErrorMessage)
                } else {
                    DirectCheckoutReducer.feesLoaded(localState, installments, installmentErrorMessage)
                }
            }
            is UIState.Error -> {
                val message = state.message ?: installmentErrorMessage
                localState = if (localState.simulatorRequestActive || localState.showSimulator) {
                    DirectCheckoutReducer.simulatorFailed(localState, message)
                } else {
                    DirectCheckoutReducer.feesFailed(localState, message)
                }
            }
            is UIState.Idle,
            null -> localState = localState.copy(feesLoading = false, simulatorLoading = false)
        }
    }

    LaunchedEffect(pendingPaymentState) {
        when (val state = pendingPaymentState) {
            is UIState.Success -> {
                val pendingPayment = viewModel.consumeDirectCheckoutPendingPayment() ?: return@LaunchedEffect
                if (PaymentTypeRules.requiresTerminalApproval(pendingPayment.paymentType)) {
                    onEffect(DirectCheckoutEffect.OpenPaymentDialog(pendingPayment))
                } else {
                    onEffect(
                        DirectCheckoutEffect.ConfirmManualPayment(
                            pendingPayment = pendingPayment,
                            paymentData = buildManualPaymentData(pendingPayment.amount),
                        ),
                    )
                }
            }
            is UIState.Error -> {
                localState = localState.copy(step = DirectCheckoutStep.Method)
                onEffect(DirectCheckoutEffect.ShowToast(state.message ?: addPaymentLoadErrorMessage))
                viewModel.clearDirectCheckoutPaymentState()
            }
            is UIState.Loading,
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(manualPaymentState) {
        when (val state = manualPaymentState) {
            is UIState.Success -> {
                onEffect(DirectCheckoutEffect.ShowToast(paymentSuccessMessage, long = false))
                localState = localState.copy(step = DirectCheckoutStep.Orders)
                viewModel.clearDirectCheckoutPaymentState()
                viewModel.loadDirectCheckoutOrders(forceRefresh = true)
            }
            is UIState.Error -> {
                localState = localState.copy(step = DirectCheckoutStep.Method)
                onEffect(DirectCheckoutEffect.ShowToast(state.message ?: addPaymentLoadErrorMessage))
                viewModel.clearDirectCheckoutPaymentState()
            }
            is UIState.Loading,
            is UIState.Idle,
            null -> Unit
        }
    }

    DirectCheckoutScreen(
        state = DirectCheckoutUiState(
            companyName = companyName,
            companyDocument = companyDocument,
            orders = orders,
            isLoading = isLoading,
            errorMessage = errorMessage,
            availablePaymentTypes = availableTypes,
            local = localState,
        ),
        onAction = { action ->
            when (action) {
                DirectCheckoutAction.Logout -> onEffect(DirectCheckoutEffect.ShowLogoutConfirmation)
                DirectCheckoutAction.Reload -> viewModel.loadDirectCheckoutOrders(forceRefresh = true)
                DirectCheckoutAction.NewOrder -> onEffect(DirectCheckoutEffect.NavigateToRegistration)
                is DirectCheckoutAction.OrderPay -> {
                    localState = DirectCheckoutReducer.startPayment(localState, action.order)
                    viewModel.clearFeesState()
                    viewModel.loadPaymentMethods()
                }
                is DirectCheckoutAction.OrderDetail -> {
                    localState = DirectCheckoutReducer.showDetail(localState, action.order)
                }
                DirectCheckoutAction.Back -> {
                    localState = DirectCheckoutReducer.back(localState)
                }
                is DirectCheckoutAction.Key -> {
                    localState = DirectCheckoutReducer.applyPaymentKey(localState, action.value)
                }
                DirectCheckoutAction.OpenMethods -> {
                    localState = DirectCheckoutReducer.openMethods(localState)
                }
                is DirectCheckoutAction.SelectPaymentType -> {
                    val previous = localState
                    val next = DirectCheckoutReducer.selectPaymentType(localState, action.paymentType)
                    localState = next
                    when (PaymentTypeRules.normalize(action.paymentType)) {
                        OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT -> {
                            viewModel.calculateFees(currentPaymentAmount(next), action.paymentType)
                        }
                        OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT -> {
                            viewModel.clearFeesState()
                        }
                        else -> {
                            viewModel.clearFeesState()
                            previous.selectedOrder?.let { order ->
                                viewModel.addDirectCheckoutPendingPayment(
                                    order = order,
                                    paymentType = action.paymentType,
                                    amount = currentPaymentAmount(next),
                                    installments = 1,
                                )
                            }
                        }
                    }
                }
                is DirectCheckoutAction.SelectInstallment -> {
                    localState = DirectCheckoutReducer.selectInstallment(localState, action.installment)
                }
                DirectCheckoutAction.ContinueCredit -> {
                    localState.selectedOrder?.let { order ->
                        localState = localState.copy(step = DirectCheckoutStep.Waiting)
                        viewModel.addDirectCheckoutPendingPayment(
                            order = order,
                            paymentType = localState.selectedPaymentType,
                            amount = currentPaymentAmount(localState),
                            installments = localState.selectedInstallment,
                        )
                    }
                }
                DirectCheckoutAction.ContinueDebit -> {
                    localState.selectedOrder?.let { order ->
                        localState = localState.copy(step = DirectCheckoutStep.Waiting)
                        viewModel.addDirectCheckoutPendingPayment(
                            order = order,
                            paymentType = localState.selectedPaymentType,
                            amount = currentPaymentAmount(localState),
                            installments = 1,
                        )
                    }
                }
                DirectCheckoutAction.OpenSimulator -> {
                    localState = DirectCheckoutReducer.openSimulator(localState)
                    viewModel.clearFeesState()
                }
                DirectCheckoutAction.CloseSimulator -> {
                    localState = DirectCheckoutReducer.closeSimulator(localState)
                    viewModel.clearFeesState()
                }
                is DirectCheckoutAction.SimulatorAmountChange -> {
                    localState = DirectCheckoutReducer.updateSimulatorAmount(localState, action.raw)
                    viewModel.clearFeesState()
                }
                DirectCheckoutAction.ConsultSimulator -> {
                    val amount = DirectCheckoutOrderPresentation.currencyInputAmount(localState.simulatorAmountDigits)
                    if (amount <= 0.0) {
                        localState = localState.copy(simulatorError = invalidSimulatorAmountMessage)
                    } else {
                        localState = DirectCheckoutReducer.startSimulatorLoading(localState)
                        viewModel.calculateFees(amount, OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT)
                    }
                }
                is DirectCheckoutAction.SelectSimulatorInstallment -> {
                    localState = localState.copy(simulatorSelectedInstallment = action.installment)
                }
                is DirectCheckoutAction.CopySimulator -> {
                    onEffect(DirectCheckoutEffect.CopySimulatorText(action.text))
                }
                is DirectCheckoutAction.ShareSimulator -> {
                    onEffect(DirectCheckoutEffect.ShareSimulatorText(action.text))
                }
            }
        },
    )
}

private fun currentPaymentAmount(state: DirectCheckoutLocalState): Double {
    val order = state.selectedOrder ?: return 0.0
    val pendingAmount = DirectCheckoutOrderPresentation.summary(order).missingAmount
    return DirectCheckoutOrderPresentation.paymentAmount(state.paymentDigits, pendingAmount)
}

private fun buildManualPaymentData(amountFinal: Double): PaymentData {
    val now = Date()
    val locale = Locale("pt", "BR")
    return PaymentData(
        date = SimpleDateFormat("dd/MM/yyyy", locale).format(now),
        time = SimpleDateFormat("HH:mm:ss", locale).format(now),
        amountFinal = amountFinal,
    )
}

private fun formatCnpj(document: String): String {
    val numbers = document.filter(Char::isDigit)
    if (numbers.length != 14) return if (document.isBlank()) "-" else document

    return "${numbers.substring(0, 2)}.${numbers.substring(2, 5)}.${numbers.substring(5, 8)}/" +
        "${numbers.substring(8, 12)}-${numbers.substring(12, 14)}"
}
```

- [ ] **Step 2: Add lifecycle runtime compose dependency if missing**

Check whether `androidx.compose.runtime:runtime-livedata` is already in the version catalog:

```powershell
rg -n "runtime-livedata|livedata" gradle\libs.versions.toml app\build.gradle.kts
```

If it is missing, add this catalog entry under `[libraries]` in `gradle/libs.versions.toml`:

```toml
androidx-runtime-livedata = { group = "androidx.compose.runtime", name = "runtime-livedata" }
```

Then add this dependency in `app/build.gradle.kts` next to the other Compose dependencies:

```kotlin
implementation(libs.androidx.runtime.livedata)
```

- [ ] **Step 3: Thin `DirectCheckoutFragment`**

Replace `DirectCheckoutFragment.onCreateView` content with:

```kotlin
override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
): View {
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            DirectCheckoutRoute(
                homeViewModel = homeViewModel,
                viewModel = viewModel,
                defaultCompanyName = getString(R.string.home_default_company_name),
                defaultCompanyDocument = getString(R.string.home_company_document_preview),
                directCheckoutErrorMessage = getString(R.string.direct_checkout_error),
                installmentErrorMessage = getString(R.string.direct_checkout_installments_error),
                addPaymentLoadErrorMessage = getString(R.string.order_details_add_payment_load_error),
                paymentSuccessMessage = getString(R.string.order_details_payment_success_toast),
                invalidSimulatorAmountMessage = "Informe um valor maior que zero.",
                onEffect = ::handleEffect,
            )
        }
    }
}
```

Add this method to `DirectCheckoutFragment`:

```kotlin
private fun handleEffect(effect: DirectCheckoutEffect) {
    when (effect) {
        DirectCheckoutEffect.ShowLogoutConfirmation -> showLogoutConfirmation()
        DirectCheckoutEffect.NavigateToRegistration -> openNewOrderFlow()
        is DirectCheckoutEffect.OpenPaymentDialog -> openPaymentDialog(effect.pendingPayment)
        is DirectCheckoutEffect.ConfirmManualPayment -> {
            viewModel.confirmDirectCheckoutManualPayment(effect.pendingPayment, effect.paymentData)
        }
        is DirectCheckoutEffect.CopySimulatorText -> copySimulatorText(effect.text)
        is DirectCheckoutEffect.ShareSimulatorText -> shareSimulatorText(effect.text)
        is DirectCheckoutEffect.ShowToast -> Toast.makeText(
            requireContext(),
            effect.message,
            if (effect.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
        ).show()
        is DirectCheckoutEffect.ShowSessionExpired -> validateErrorType(effect.exception)
    }
}
```

Remove the old route-local observation methods from the fragment after the route compiles:

- `observeHomeState`
- `observeOrders`
- `observePaymentMethods`
- `observeFees`
- `observePendingPayment`
- `observeManualPayment`
- `reloadOrders`
- `startPayment`
- `showDetail`
- `selectPaymentType`
- `openInstallmentSimulator`
- `closeInstallmentSimulator`
- `updateSimulatorAmount`
- `consultSimulatorInstallments`
- `continueCreditPayment`
- `continueDebitPayment`
- `currentPaymentAmount`
- `buildManualPaymentData`
- `popStep`
- `formatCnpj`

Keep these Android integration methods in the fragment:

- `openPaymentDialog`
- `copySimulatorText`
- `shareSimulatorText`
- `openNewOrderFlow`
- `showLogoutConfirmation`
- `getSerialForPrePay`
- `validateErrorType`

- [ ] **Step 4: Run targeted tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --tests "com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentationTest" --no-watch-fs
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutRoute.kt app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutFragment.kt gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat: route direct checkout compose state"
```

Only include `gradle/libs.versions.toml` and `app/build.gradle.kts` if Step 2 changed them.

---

### Task 4: Split Compose Screens And Components

**Files:**
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/components/DirectCheckoutBlocks.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/OrdersScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/DetailScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/KeypadScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/MethodScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/CreditScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/DebitScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/WaitingScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/InstallmentSimulatorScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutScreen.kt`
- Delete: `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreen.kt`

**Interfaces:**
- Consumes: existing composables from `DirectCheckoutFlowScreen.kt`
- Produces:
  - `OrdersScreen`
  - `DetailScreen`
  - `KeypadScreen`
  - `MethodScreen`
  - `CreditScreen`
  - `DebitScreen`
  - `WaitingScreen`
  - `InstallmentSimulatorScreen`
  - Shared internal widgets in `DirectCheckoutBlocks.kt`

- [ ] **Step 1: Move shared blocks**

Create `DirectCheckoutBlocks.kt` and move these functions from the old file unchanged except package/import/color rename:

- `NavBar`
- `AmountCard`
- `PaymentMethodRow`
- `SmallMethod`
- `MethodIcon`
- `InstallmentRow`
- `LoadingBlock`
- `EmptyBlock`
- `Metric`
- `DetailCell`
- `MetricCell`
- `SummaryRow`
- `currencyText`
- `simulatorShareText`

Use package:

```kotlin
package com.detrapay.ui.home.direct_checkout.components
```

Replace `FigmaColors` references with `DirectCheckoutColors`.

- [ ] **Step 2: Move the order list screen**

Create `OrdersScreen.kt` with package:

```kotlin
package com.detrapay.ui.home.direct_checkout.screens
```

Move these functions from the old file unchanged except package/import/color rename:

- `SellerOrdersScreen`, renamed to `OrdersScreen`
- `FabMenuButton`
- `SellerOrderCard`

Use imports from:

```kotlin
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.components.EmptyBlock
import com.detrapay.ui.home.direct_checkout.components.LoadingBlock
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
```

- [ ] **Step 3: Move remaining step screens**

Create one file per screen under `screens` and move the corresponding composables from the old file:

- `DetailScreen.kt`: `DetailScreen`
- `KeypadScreen.kt`: `KeypadScreen`
- `MethodScreen.kt`: `MethodScreen`
- `CreditScreen.kt`: `CreditScreen`
- `DebitScreen.kt`: `DebitScreen`
- `WaitingScreen.kt`: `WaitingScreen`
- `InstallmentSimulatorScreen.kt`: `InstallmentSimulatorScreen` and `SimulatorInstallmentRow`

For every moved file:

- Use package `com.detrapay.ui.home.direct_checkout.screens`.
- Replace `FigmaColors` with `DirectCheckoutColors`.
- Import shared widgets from `components`.
- Keep text, sizing, behavior, and click handlers unchanged.
- Keep `DirectCheckoutOrderPresentation` in the existing `ui.home.simplified` package.

- [ ] **Step 4: Replace compatibility shell with real split shell**

Update `DirectCheckoutScreen.kt` so it no longer calls `DirectCheckoutFlowScreen`:

```kotlin
package com.detrapay.ui.home.direct_checkout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.screens.CreditScreen
import com.detrapay.ui.home.direct_checkout.screens.DebitScreen
import com.detrapay.ui.home.direct_checkout.screens.DetailScreen
import com.detrapay.ui.home.direct_checkout.screens.InstallmentSimulatorScreen
import com.detrapay.ui.home.direct_checkout.screens.KeypadScreen
import com.detrapay.ui.home.direct_checkout.screens.MethodScreen
import com.detrapay.ui.home.direct_checkout.screens.OrdersScreen
import com.detrapay.ui.home.direct_checkout.screens.WaitingScreen
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun DirectCheckoutScreen(
    state: DirectCheckoutUiState,
    onAction: (DirectCheckoutAction) -> Unit,
) {
    val local = state.local
    val currentOrder = local.selectedOrder
    val pendingAmount = currentOrder?.let { DirectCheckoutOrderPresentation.summary(it).missingAmount } ?: 0.0
    val amount = DirectCheckoutOrderPresentation.paymentAmount(local.paymentDigits, pendingAmount)

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DirectCheckoutColors.Background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (local.step) {
                    DirectCheckoutStep.Orders -> OrdersScreen(
                        orders = state.orders,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onReload = { onAction(DirectCheckoutAction.Reload) },
                        onNewOrder = { onAction(DirectCheckoutAction.NewOrder) },
                        onOpenSimulator = { onAction(DirectCheckoutAction.OpenSimulator) },
                        onOrderPay = { onAction(DirectCheckoutAction.OrderPay(it)) },
                        onOrderDetail = { onAction(DirectCheckoutAction.OrderDetail(it)) },
                    )
                    DirectCheckoutStep.Detail -> if (currentOrder != null) {
                        DetailScreen(
                            order = currentOrder,
                            onBack = { onAction(DirectCheckoutAction.Back) },
                            onPay = { onAction(DirectCheckoutAction.OrderPay(currentOrder)) },
                        )
                    }
                    DirectCheckoutStep.Keypad -> if (currentOrder != null) {
                        KeypadScreen(
                            order = currentOrder,
                            displayAmount = DirectCheckoutOrderPresentation.paymentDisplayAmount(
                                local.paymentDigits,
                                pendingAmount,
                            ),
                            onBack = { onAction(DirectCheckoutAction.Back) },
                            onKey = { onAction(DirectCheckoutAction.Key(it)) },
                            onPay = { onAction(DirectCheckoutAction.OpenMethods) },
                        )
                    }
                    DirectCheckoutStep.Method -> if (currentOrder != null) {
                        MethodScreen(
                            order = currentOrder,
                            amount = amount,
                            availablePaymentTypes = state.availablePaymentTypes,
                            onBack = { onAction(DirectCheckoutAction.Back) },
                            onSelectPaymentType = { onAction(DirectCheckoutAction.SelectPaymentType(it)) },
                        )
                    }
                    DirectCheckoutStep.Credit -> CreditScreen(
                        amount = amount,
                        installments = local.creditInstallments,
                        selectedInstallment = local.selectedInstallment,
                        isLoading = local.feesLoading,
                        errorMessage = local.feesError,
                        onBack = { onAction(DirectCheckoutAction.Back) },
                        onSelectInstallment = { onAction(DirectCheckoutAction.SelectInstallment(it)) },
                        onContinue = { onAction(DirectCheckoutAction.ContinueCredit) },
                    )
                    DirectCheckoutStep.Debit -> DebitScreen(
                        amount = amount,
                        onBack = { onAction(DirectCheckoutAction.Back) },
                        onContinue = { onAction(DirectCheckoutAction.ContinueDebit) },
                    )
                    DirectCheckoutStep.Waiting -> WaitingScreen(
                        total = amount,
                        paymentType = local.selectedPaymentType,
                        onBack = { onAction(DirectCheckoutAction.Back) },
                    )
                }

                if (local.showSimulator) {
                    InstallmentSimulatorScreen(
                        amountDigits = local.simulatorAmountDigits,
                        installments = local.simulatorInstallments,
                        selectedInstallment = local.simulatorSelectedInstallment,
                        isLoading = local.simulatorLoading,
                        errorMessage = local.simulatorError,
                        onClose = { onAction(DirectCheckoutAction.CloseSimulator) },
                        onAmountChange = { onAction(DirectCheckoutAction.SimulatorAmountChange(it)) },
                        onConsult = { onAction(DirectCheckoutAction.ConsultSimulator) },
                        onSelectInstallment = { onAction(DirectCheckoutAction.SelectSimulatorInstallment(it)) },
                        onCopy = { onAction(DirectCheckoutAction.CopySimulator(it)) },
                        onShare = { onAction(DirectCheckoutAction.ShareSimulator(it)) },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: Delete the old monolithic screen file**

Delete:

```powershell
Remove-Item -LiteralPath app\src\main\java\com\detrapay\ui\home\simplified\DirectCheckoutFlowScreen.kt
```

This delete is safe only after Step 4 compiles imports against the moved screen files.

- [ ] **Step 6: Run targeted tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --tests "com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentationTest" --no-watch-fs
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/java/com/detrapay/ui/home/direct_checkout app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreen.kt
git commit -m "refactor: split direct checkout compose screens"
```

---

### Task 5: Move And Expand Previews

**Files:**
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutPreviewFixtures.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutScreenPreview.kt`
- Delete: `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreenPreview.kt`

**Interfaces:**
- Consumes: preview fixture helpers currently in `DirectCheckoutFlowScreenPreview.kt`
- Produces:
  - `internal fun previewOrders(): List<Order>`
  - `internal fun previewInstallments(): List<InstallmentFee>`
  - Previews for loaded, empty, error, detail, keypad, method, credit loading, credit error, credit loaded, debit, waiting, simulator states.

- [ ] **Step 1: Move preview fixtures**

Create `DirectCheckoutPreviewFixtures.kt` with package:

```kotlin
package com.detrapay.ui.home.direct_checkout
```

Move these helper functions from the old preview file and mark them `internal`:

- `previewOrders`
- `previewSellerParityOrders`
- `previewOrder`
- `previewReceivables`
- `previewReceivable`
- `previewInstallments`

Keep the same fake data values.

- [ ] **Step 2: Create screen previews**

Create `DirectCheckoutScreenPreview.kt`:

```kotlin
package com.detrapay.ui.home.direct_checkout

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Direct checkout - Orders loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutOrdersLoadedPreview() {
    PreviewContent(local = DirectCheckoutLocalState(step = DirectCheckoutStep.Orders))
}

@Preview(name = "Direct checkout - Orders empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutOrdersEmptyPreview() {
    PreviewContent(
        orders = emptyList(),
        local = DirectCheckoutLocalState(step = DirectCheckoutStep.Orders),
    )
}

@Preview(name = "Direct checkout - Orders error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutOrdersErrorPreview() {
    PreviewContent(
        orders = emptyList(),
        errorMessage = "Falha ao carregar pedidos.",
        local = DirectCheckoutLocalState(step = DirectCheckoutStep.Orders),
    )
}

@Preview(name = "Direct checkout - Detail", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutDetailPreview() {
    val order = previewOrders().first()
    PreviewContent(local = DirectCheckoutLocalState(step = DirectCheckoutStep.Detail, selectedOrder = order))
}

@Preview(name = "Direct checkout - Keypad", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutKeypadPreview() {
    val order = previewOrders().first()
    PreviewContent(local = DirectCheckoutLocalState(step = DirectCheckoutStep.Keypad, selectedOrder = order, paymentDigits = "12500"))
}

@Preview(name = "Direct checkout - Method", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutMethodPreview() {
    val order = previewOrders().first()
    PreviewContent(local = DirectCheckoutLocalState(step = DirectCheckoutStep.Method, selectedOrder = order, paymentDigits = "12500"))
}

@Preview(name = "Direct checkout - Credit loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutCreditLoadingPreview() {
    val order = previewOrders().first()
    PreviewContent(
        local = DirectCheckoutLocalState(
            step = DirectCheckoutStep.Credit,
            selectedOrder = order,
            paymentDigits = "12500",
            selectedPaymentType = "credito",
            feesLoading = true,
        ),
    )
}

@Preview(name = "Direct checkout - Credit error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutCreditErrorPreview() {
    val order = previewOrders().first()
    PreviewContent(
        local = DirectCheckoutLocalState(
            step = DirectCheckoutStep.Credit,
            selectedOrder = order,
            paymentDigits = "12500",
            selectedPaymentType = "credito",
            feesError = "Nao foi possivel consultar parcelas.",
        ),
    )
}

@Preview(name = "Direct checkout - Credit loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutCreditLoadedPreview() {
    val order = previewOrders().first()
    PreviewContent(
        local = DirectCheckoutLocalState(
            step = DirectCheckoutStep.Credit,
            selectedOrder = order,
            paymentDigits = "12500",
            selectedPaymentType = "credito",
            creditInstallments = previewInstallments(),
            selectedInstallment = 3,
        ),
    )
}

@Preview(name = "Direct checkout - Debit", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutDebitPreview() {
    val order = previewOrders().first()
    PreviewContent(
        local = DirectCheckoutLocalState(
            step = DirectCheckoutStep.Debit,
            selectedOrder = order,
            paymentDigits = "12500",
            selectedPaymentType = "debito",
        ),
    )
}

@Preview(name = "Direct checkout - Waiting", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutWaitingPreview() {
    val order = previewOrders().first()
    PreviewContent(
        local = DirectCheckoutLocalState(
            step = DirectCheckoutStep.Waiting,
            selectedOrder = order,
            paymentDigits = "12500",
            selectedPaymentType = "pix",
        ),
    )
}

@Preview(name = "Direct checkout - Simulator loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutSimulatorPreview() {
    PreviewContent(
        local = DirectCheckoutLocalState(
            step = DirectCheckoutStep.Orders,
            showSimulator = true,
            simulatorAmountDigits = "50000",
            simulatorInstallments = previewInstallments(),
            simulatorSelectedInstallment = 3,
        ),
    )
}

@Composable
private fun PreviewContent(
    orders: List<com.detrapay.data.model.Order> = previewOrders(),
    errorMessage: String? = null,
    local: DirectCheckoutLocalState,
) {
    DirectCheckoutScreen(
        state = DirectCheckoutUiState(
            companyName = "Detrapay Motors",
            companyDocument = "12.345.678/0001-90",
            orders = orders,
            isLoading = false,
            errorMessage = errorMessage,
            availablePaymentTypes = listOf("pix", "credito", "debito", "dinheiro"),
            local = local,
        ),
        onAction = {},
    )
}
```

- [ ] **Step 3: Delete old preview file**

Delete:

```powershell
Remove-Item -LiteralPath app\src\main\java\com\detrapay\ui\home\simplified\DirectCheckoutFlowScreenPreview.kt
```

- [ ] **Step 4: Run targeted tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --tests "com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentationTest" --no-watch-fs
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/com/detrapay/ui/home/direct_checkout app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreenPreview.kt
git commit -m "refactor: move direct checkout compose previews"
```

---

### Task 6: Final Verification

**Files:**
- Verify: `app/src/main/java/com/detrapay/ui/home/direct_checkout/*`
- Verify: `app/src/main/java/com/detrapay/ui/home/direct_checkout/components/*`
- Verify: `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/*`
- Verify: `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutOrderPresentation.kt`
- Verify: `app/src/test/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducerTest.kt`
- Verify: `app/src/test/java/com/detrapay/ui/home/simplified/DirectCheckoutOrderPresentationTest.kt`

**Interfaces:**
- Consumes: all outputs from Tasks 1-5.
- Produces: verified migration slice with passing targeted tests and Android runtime validation when a device is available.

- [ ] **Step 1: Run targeted unit tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --tests "com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentationTest" --no-watch-fs
```

Expected: PASS.

- [ ] **Step 2: Run broader unit tests if time allows**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --no-watch-fs
```

Expected: PASS.

- [ ] **Step 3: Run Android validation on device**

Run the project standard script:

```powershell
.\scripts\dev-device.ps1
```

Expected:

- The script clears logcat, installs the debug app, opens `com.detrapay/.ui.splash.SplashActivity`, and prints filtered logcat.
- No `AndroidRuntime` crash.
- No `FATAL EXCEPTION`.
- Direct checkout opens for seller mode and preserves the existing list, detail, keypad, method, credit/debit, waiting, simulator, and payment dialog behaviors.

- [ ] **Step 4: Capture remaining risks**

If Android validation cannot run because no device is attached, record this in the final response:

```text
Android device validation was not run because no device was available. Unit tests passed; runtime payment/dialog/navigation behavior remains the main residual risk.
```

- [ ] **Step 5: Commit final verification-only adjustments**

If final verification required small import or preview fixes, commit them:

```powershell
git add app/src/main/java/com/detrapay/ui/home/direct_checkout app/src/test/java/com/detrapay/ui/home/direct_checkout
git commit -m "fix: stabilize direct checkout compose migration"
```

Skip this commit if there are no changes after verification.

---

## Self-Review

Spec coverage:

- Progressive migration host is covered by Tasks 2-3.
- Thin `DirectCheckoutFragment` boundary is covered by Task 3.
- Stateless Compose UI split is covered by Tasks 2 and 4.
- Android-only effects are covered by Task 3.
- Reducer/controller tests are covered by Task 1.
- Previews are covered by Task 5.
- Final Android validation is covered by Task 6.

Placeholder scan:

- The plan contains no deferred implementation markers or incomplete sections.
- Mechanical moves explicitly name the source functions and target files.

Type consistency:

- `DirectCheckoutStep`, `DirectCheckoutLocalState`, `DirectCheckoutUiState`, `DirectCheckoutAction`, and `DirectCheckoutEffect` are defined in Task 1 and consumed by later tasks.
- `DirectCheckoutScreen(state, onAction)` is introduced in Task 2 and finalized in Task 4.
- `DirectCheckoutRoute` is introduced and consumed by `DirectCheckoutFragment` in Task 3.
