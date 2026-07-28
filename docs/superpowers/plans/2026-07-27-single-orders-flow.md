# Single Orders Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the current Pedidos experience the only supported Home flow, removing all application-mode concepts, legacy Home surfaces, and mode-specific naming without changing current order or payment behavior.

**Architecture:** `HomeActivity` remains the Android host and loads shared company state, while `OrdersFragment` becomes the navigation graph start destination. The canonical `com.detrapay.ui.home.orders` package owns the Compose route, immutable flow contract, reducer, payment routing, ViewModel, presentation rules, screens, and Android effects.

**Tech Stack:** Kotlin 2.1.10, Android SDK 35, XML Navigation 2.8.9, Jetpack Compose Material 3, Hilt 2.57.1, LiveData/coroutines, JUnit 4, MockK, Gradle wrapper, ADB.

## Global Constraints

- Preserve the current device UI and copy; this work is a structural consolidation, not a redesign.
- Support exactly one Home surface: Pedidos.
- Do not select, persist, normalize, or branch on `complete`, `simplified`, `direct_checkout`, or any equivalent mode.
- Preserve new-order, detail, payment, installment simulation, refresh, search, report, and logout journeys reachable from Pedidos.
- Keep existing backend endpoints and payload behavior unchanged.
- Do not hide missing backend fields or contracts with an app workaround; report the affected endpoint, missing field, expected contract, and UI impact.
- Use TDD for each behavior-bearing task and keep commits atomic.
- After every install on the connected device, open the app and inspect logs filtered for `com.detrapay`, `AndroidRuntime`, and `FATAL EXCEPTION`.

---

## Reference flow and ASCII wireframes

These wireframes preserve the current device screens. They are acceptance references, not a request for new layout work.

```text
[Splash / Login]
       |
       v
+------------------+
| PEDIDOS          |
| lista + busca    |
+------------------+
   |       |      |
   |       |      +--------------------> [Logout]
   |       |
   |       +--> [+ Acoes] --> [Novo pedido] --> [RegistrationActivity]
   |                         |                         |
   |                         |                         +--> volta para Pedidos
   |                         |
   |                         +--> [Simular parcelas] --> [Simulador]
   |
   +--> [Cartao do pedido] --> [Detalhes] --> [Pagar saldo]
                                                  |
                                                  v
                                          [Digitar valor]
                                                  |
                                                  v
                                        [Forma de pagamento]
                                           |      |      |
                                           |      |      +--> Pix/manual
                                           |      +---------> Debito
                                           +----------------> Credito --> [Parcelas]
                                                                  |
                                                                  v
                                                        [Aguardando/resultado]
                                                                  |
                                                                  +--> Pedidos
```

```text
+------------------------------------------------+
| Pedidos                         [Sair] [Buscar] |
| CONCESSIONARIA TESTE                           |
| 11.222.333/0001-81                             |
+------------------------------------------------+
| #544   28/07/2026              [ PENDENTE ]    |
| WESLEY DE CASTRO                               |
|------------------------------------------------|
| TOTAL          PAGO             FALTA          |
| R$ 2.570,18    R$ 0,00          R$ 2.570,18    |
| [progresso-----------------------------------] |
+------------------------------------------------+
| #537   23/07/2026              [ PENDENTE ]    |
| alberto de lima                                |
|------------------------------------------------|
| TOTAL          PAGO             FALTA          |
| R$ 2.330,71    R$ 2.330,71      R$ 2.330,71    |
+------------------------------------------------+
|                                          ( + ) |
+------------------------------------------------+
```

```text
+-----------------------+  +-----------------------+
| [<] PAGAMENTO         |  | [<] R$ 1.000,00       |
| DIGITE O VALOR        |  | Escolha a forma      |
| R$ 1.000,00           |  | de pagamento         |
| Pedido #544           |  |                       |
| [1] [2] [3]           |  | [ Credito           ] |
| [4] [5] [6]           |  | [ Debito            ] |
| [7] [8] [9]           |  | [ Pix               ] |
| [,] [0] [apagar]      |  | [Pix] [Loja] [Dinheiro]|
| [       Pagar       ] |  |                       |
+-----------------------+  +-----------------------+
```

```text
+------------------------------------------------+
| [<] Simular parcelas                       [x] |
+------------------------------------------------+
| Credito - Simular parcelamento em ate 18x      |
| Valor [ R$ 2.570,18                          ] |
| [ Consultar Parcelas ]                         |
| ( ) 1x de R$ 2.570,18   Total R$ 2.570,18      |
| ( ) 6x de R$   465,00   Total R$ 2.790,00      |
| ( ) 12x de R$  252,00   Total R$ 3.024,00      |
| [ Copiar ]                 [ WhatsApp ]         |
+------------------------------------------------+
```

Flow examples to verify during implementation:

- Open order `#544`, enter `R$ 1.000,00`, choose credit, select an installment, complete, and return to Pedidos.
- Choose Pix, generate/copy its code, and return to Pedidos without losing the root navigation state.
- Open `+`, enter `RegistrationActivity`, cancel or finish, and return to the canonical list.
- Open `+`, simulate `R$ 2.570,18`, select an installment, and exercise copy/share.
- Search by order number, customer, and CPF/CNPJ, then clear the filter without mutating loaded data.

## Target file structure

```text
app/src/main/java/com/detrapay/ui/home/
  HomeActivity.kt                 Android host and root-back/logout behavior
  HomeState.kt                    Company/operator/salesman data only
  HomeViewModel.kt                Loads shared Home data and logs out
  orders/
    OrdersFragment.kt             Android effects, dialogs, intents, clipboard, navigation
    OrdersRoute.kt                ViewModel observation and action/effect coordination
    OrdersScreen.kt               Stateless Compose flow switch
    OrderFlowContract.kt          Flow state, step, action, effect, fee request target
    OrderFlowReducer.kt           Pure local state transitions
    OrderPaymentRouter.kt         Terminal versus manual payment routing
    OrdersViewModel.kt            Orders, methods, fees, and payment mutations
    OrderPresentation.kt          Pure formatting and order/payment presentation rules
    OrderPreviewFixtures.kt       Compose preview fixtures
    OrdersScreenPreview.kt        Main flow previews
    components/
      OrderFlowBlocks.kt          Shared Compose UI blocks
      OrderFlowColors.kt          Flow palette
    screens/
      OrdersListScreen.kt
      DetailScreen.kt
      KeypadScreen.kt
      MethodScreen.kt
      CreditScreen.kt
      DebitScreen.kt
      WaitingScreen.kt
      InstallmentSimulatorScreen.kt
```

The following production packages are removed completely: `ui.home.registration`, `ui.home.order_list`, `ui.home.profile`, `ui.home.payment_history`, `ui.home.simplified`, `ui.home.direct_checkout`, and `ui.notification`.

---

### Task 1: Remove application modes from domain and session

**Files:**
- Create: `app/src/test/java/com/detrapay/architecture/ModeFreeDomainArchitectureTest.kt`
- Modify: `app/src/main/java/com/detrapay/data/model/LoggedInUser.kt`
- Modify: `app/src/main/java/com/detrapay/data/model/Company.kt`
- Modify: `app/src/main/java/com/detrapay/data/model/remote/AuthResponse.kt`
- Modify: `app/src/main/java/com/detrapay/data/model/remote/LoginCompanyResponse.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/AuthRepository.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/LoginRepository.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/HomeState.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/HomeViewModel.kt`
- Modify: `app/src/test/java/com/detrapay/data/repositories/LoginRepositoryTest.kt`
- Delete: `app/src/main/java/com/detrapay/data/model/SellerAppMode.kt`
- Delete: `app/src/main/java/com/detrapay/ui/home/HomeModeRouter.kt`
- Delete: `app/src/test/java/com/detrapay/data/model/LoggedInUserTest.kt`
- Delete: `app/src/test/java/com/detrapay/ui/home/HomeModeRouterTest.kt`

**Interfaces:**
- Consumes: Existing auth/session DTOs and repositories.
- Produces: `LoggedInUser` and `HomeState` without mode properties; auth persistence with access/refresh/token metadata only.

- [ ] **Step 1: Write the failing architecture test**

```kotlin
package com.detrapay.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class ModeFreeDomainArchitectureTest {
    @Test
    fun `domain session and home state contain no application modes`() {
        val roots = listOf(
            File("src/main/java/com/detrapay/data/model"),
            File("src/main/java/com/detrapay/data/repositories/AuthRepository.kt"),
            File("src/main/java/com/detrapay/data/repositories/LoginRepository.kt"),
            File("src/main/java/com/detrapay/ui/home/HomeState.kt"),
            File("src/main/java/com/detrapay/ui/home/HomeViewModel.kt"),
            File("src/main/java/com/detrapay/ui/home/HomeModeRouter.kt"),
        )
        val forbidden = listOf("SellerAppMode", "appMode", "sellerAppMode", "HomeModeRouter")
        val source = roots.flatMap { root ->
            when {
                !root.exists() -> emptyList()
                root.isFile -> listOf(root)
                else -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
            }
        }.joinToString("\n") { it.readText() }

        forbidden.forEach { token ->
            assertFalse("Found forbidden mode token: $token", source.contains(token))
        }
    }
}
```

- [ ] **Step 2: Run the test and verify the existing mode model fails it**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.architecture.ModeFreeDomainArchitectureTest"
```

Expected: FAIL with `Found forbidden mode token: SellerAppMode`.

- [ ] **Step 3: Remove mode fields from the domain and DTOs**

Use these exact target declarations:

```kotlin
data class LoggedInUser(
    val id: String,
    val sessionToken: String,
    val displayName: String,
    val username: String,
    val cpfCnpj: String,
    val email: String,
    val companies: List<Company>,
    val dispatchers: List<Dispatcher>,
    val salesmen: List<Salesman>,
)

data class Company(
    val id: Int,
    val name: String,
    val logoUrl: String? = null,
    val logoKey: String? = null,
) : Serializable

@Serializable
data class LoginCompanyResponse(
    val id: Int,
    val name: String,
    @SerializedName("logo_url") val logoUrl: String? = null,
)
```

Remove `AuthResponse.appMode` entirely. Extra `appMode` and `seller_app_mode` JSON properties remain safe because Gson ignores unknown properties.

- [ ] **Step 4: Remove mode persistence from repositories**

`AuthRepository.persistSessionTokens` must have this signature and preference edit:

```kotlin
private fun persistSessionTokens(
    accessToken: String,
    refreshToken: String?,
    expiresIn: Long?,
    expiresAt: Long?,
    tokenType: String?,
) {
    val computedExpiresAt = expiresAt ?: expiresIn?.let { (System.currentTimeMillis() / 1000L) + it }
    preferences.edit()
        .putString(KEY_ACCESS_TOKEN, accessToken)
        .putString(KEY_REFRESH_TOKEN, refreshToken)
        .putString(KEY_TOKEN_TYPE, tokenType ?: DEFAULT_TOKEN_TYPE)
        .putLong(KEY_EXPIRES_AT, computedExpiresAt ?: NO_EXPIRATION)
        .apply()
}
```

Update both callers to omit the mode argument, construct `LoggedInUser` without a final mode argument, remove `currentAppMode()`, remove `KEY_APP_MODE`, and stop removing that key in `clearSessionTokens()`.

In `LoginRepository`, remove the `SellerAppMode` import, omit `sellerAppMode` when mapping `Company`, delete the local `appMode`, and construct `LoggedInUser` without `appMode`.

- [ ] **Step 5: Remove mode from Home state construction**

```kotlin
data class HomeState(
    val companyName: String,
    val companyDocument: String,
    val dispatcherName: String,
    val companyLogoKey: String?,
    val salesmen: List<Salesman>,
)
```

Remove the `SellerAppMode` import and `sellerAppMode` argument from `HomeViewModel`. Delete `SellerAppMode.kt`, `HomeModeRouter.kt`, `LoggedInUserTest.kt`, and `HomeModeRouterTest.kt`.

- [ ] **Step 6: Rewrite the login repository test around canonical user mapping**

Keep the existing mocks, then replace the three mode tests and their fixture arguments with:

```kotlin
@Test
fun `login maps authenticated user without application mode`() = runTest {
    coEvery { remoteDataSource.login("04685620000162", "crasa04685620") } returns
        Result.Success(authResponse())
    coEvery { usersDao.insertUser(any()) } returns 1L
    coEvery { authRepository.saveLoginSession(any(), any()) } just Runs

    val result = repository.login("04685620000162", "crasa04685620")

    assertTrue(result is Result.Success)
    val user = (result as Result.Success).data
    assertEquals("user-1", user.id)
    assertEquals("CRASA", user.companies.single().name)
}

private fun authResponse() = AuthResponse(
    token = "legacy-token",
    accessToken = "access-token",
    refreshToken = "refresh-token",
    expiresIn = 3600,
    expiresAt = 999999,
    tokenType = "Bearer",
    user = userResponse(),
    companies = listOf(LoginCompanyResponse(id = 37, name = "CRASA")),
    dispatchers = listOf(DispatcherResponse(id = 35, name = "Despachante")),
    salesmen = emptyList(),
)

private fun userResponse() = UserResponse(
    id = "user-1",
    documentId = "doc-1",
    username = "terminal",
    name = "Terminal",
    email = "terminal@example.com",
    phoneNumber = null,
    cpf_cnpj = "04685620000162",
    blocked = false,
    role = RoleResponse(id = 1, name = "pos_terminal", type = "pos_terminal"),
)
```

- [ ] **Step 7: Run focused and full unit tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.architecture.ModeFreeDomainArchitectureTest" --tests "com.detrapay.data.repositories.LoginRepositoryTest"
.\gradlew.bat testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [ ] **Step 8: Verify no domain/session mode tokens remain**

Run:

```powershell
rg -n "SellerAppMode|appMode|sellerAppMode|HomeModeRouter" app/src/main app/src/test
```

Expected: exit code 1 and no matches.

- [ ] **Step 9: Commit the domain cleanup**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: remove seller app modes"
```

---

### Task 2: Canonicalize order presentation and ViewModel support

**Files:**
- Create: `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/orders/OrdersViewModel.kt`
- Create: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`
- Modify: current files under `app/src/main/java/com/detrapay/ui/home/direct_checkout/` to consume the new names temporarily
- Delete: `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutOrderPresentation.kt`
- Delete: `app/src/main/java/com/detrapay/ui/home/simplified/SimplifiedReceivableListViewModel.kt`
- Delete: `app/src/test/java/com/detrapay/ui/home/simplified/DirectCheckoutOrderPresentationTest.kt`

**Interfaces:**
- Consumes: `OrderRepository`, `RegistrationRepository`, `SalesmanRepository`, `PaymentTypeRules`, and `OrderPaymentTotals`.
- Produces: `OrderPresentation`, `OrdersViewModel`, `PendingOrderPayment`, `OrderSummary`, `SellerCardSummary`, and `WaitingPresentation` under `com.detrapay.ui.home.orders`.

- [ ] **Step 1: Move the presentation test to the canonical package first**

Create `OrderPresentationTest.kt` by copying the existing test body and applying exactly:

```text
package com.detrapay.ui.home.simplified
  -> package com.detrapay.ui.home.orders
DirectCheckoutOrderPresentationTest
  -> OrderPresentationTest
DirectCheckoutOrderPresentation
  -> OrderPresentation
```

- [ ] **Step 2: Run the moved test and verify the target object is missing**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
```

Expected: compilation FAIL with `Unresolved reference 'OrderPresentation'`.

- [ ] **Step 3: Create the canonical presentation file without behavior changes**

Copy every function body from `DirectCheckoutOrderPresentation.kt` and apply this declaration map:

```text
DirectCheckoutOrderPresentation -> OrderPresentation
DirectCheckoutOrderSummary      -> OrderSummary
SellerCardSummary               -> SellerCardSummary
WaitingPresentation             -> WaitingPresentation
```

Delete the unused duplicate declarations `DirectCheckoutSellerCardSummary` and `DirectCheckoutWaitingPresentation`. The target summary signature is:

```kotlin
data class OrderSummary(
    val registeredAmount: Double,
    val missingAmount: Double,
    val progress: Int,
    val registeredLabel: String,
    val pendingValueLabel: String,
    val missingLabel: String,
    val hasPendingBalance: Boolean,
)
```

- [ ] **Step 4: Create the canonical Orders ViewModel**

Copy only the behavior used by the current Pedidos flow. Remove `receivableListState` and the old receivables-only `loadScreenContent`. Apply this exact public API:

```kotlin
data class PendingOrderPayment(
    val order: Order,
    val receivable: OrderReceivableItem,
    val paymentType: String,
    val amount: Double,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val registrationRepository: RegistrationRepository,
    private val salesmanRepository: SalesmanRepository,
) : ViewModel() {
    val orderListState: LiveData<UIState<List<Order>>>
    val paymentMethodsState: LiveData<UIState<List<PaymentMethod>>>
    val calculateFeesState: LiveData<UIState<CalculateFeesResponse>>
    val pendingPaymentState: LiveData<UIState<PendingOrderPayment>>
    val manualPaymentState: LiveData<UIState<Order>>

    fun loadOrders(forceRefresh: Boolean = false)
    fun loadPaymentMethods(forceRefresh: Boolean = false)
    fun availablePaymentTypes(): List<String>
    fun calculateFees(value: Double, paymentType: String)
    fun clearFeesState()
    fun addPendingPayment(order: Order, paymentType: String, amount: Double, installments: Int)
    fun confirmManualPayment(pendingPayment: PendingOrderPayment, paymentData: PaymentData)
    fun clearPaymentState()
    fun consumePendingPayment(): PendingOrderPayment?
    fun prefetchRegistrationData()
}
```

The implementation is the existing ViewModel logic with these exact symbol changes:

```text
SimplifiedReceivableListViewModel       -> OrdersViewModel
DirectCheckoutPendingPayment            -> PendingOrderPayment
directOrderListState                    -> orderListState
_directOrderListState                   -> _orderListState
loadDirectCheckoutOrders                -> loadOrders
addDirectCheckoutPendingPayment         -> addPendingPayment
confirmDirectCheckoutManualPayment      -> confirmManualPayment
clearDirectCheckoutPaymentState         -> clearPaymentState
consumeDirectCheckoutPendingPayment     -> consumePendingPayment
DirectCheckoutOrderPresentation         -> OrderPresentation
```

- [ ] **Step 5: Bridge current consumers to the canonical support API**

In the existing fragment, route, contract, reducer, payment router, screens, and previews, replace imports and calls using this map:

```text
com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
  -> com.detrapay.ui.home.orders.OrderPresentation
com.detrapay.ui.home.simplified.SimplifiedReceivableListViewModel
  -> com.detrapay.ui.home.orders.OrdersViewModel
com.detrapay.ui.home.simplified.DirectCheckoutPendingPayment
  -> com.detrapay.ui.home.orders.PendingOrderPayment
```

Apply the ViewModel method/property map from Step 4 to `DirectCheckoutFragment.kt` and `DirectCheckoutRoute.kt`.

- [ ] **Step 6: Run presentation tests and compile all current consumers**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
.\gradlew.bat compileDebugKotlin
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [ ] **Step 7: Remove the old simplified package and prove it is gone**

Delete the two old production files and the old test. Then run:

```powershell
rg -n -i "simplified" app/src/main app/src/test
```

Expected: exit code 1 and no matches.

- [ ] **Step 8: Commit the canonical support layer**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: name orders support code canonically"
```

---

### Task 3: Make the canonical Orders flow the Home root

**Files:**
- Create: all target files under `app/src/main/java/com/detrapay/ui/home/orders/` listed in Target file structure
- Create: `app/src/test/java/com/detrapay/ui/home/HomeNavigationContractTest.kt`
- Create: `app/src/test/java/com/detrapay/ui/home/orders/OrderFlowReducerTest.kt`
- Create: `app/src/test/java/com/detrapay/ui/home/orders/OrderPaymentRouterTest.kt`
- Create: `app/src/test/java/com/detrapay/ui/home/orders/TestOrderFixtures.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/HomeActivity.kt`
- Modify: `app/src/main/res/layout/activity_home.xml`
- Modify: `app/src/main/res/navigation/home_navigation.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Delete: `app/src/main/java/com/detrapay/ui/home/direct_checkout/`
- Delete: `app/src/test/java/com/detrapay/ui/home/direct_checkout/`
- Delete: `app/src/main/res/layout/bottom_sheet_direct_checkout_order_details.xml`
- Delete: `app/src/main/res/layout/direct_checkout_order_list_item.xml`

**Interfaces:**
- Consumes: `OrdersViewModel`, `OrderPresentation`, `HomeViewModel`, and `PaymentDialogViewModel`.
- Produces: `OrdersFragment`, `OrdersRoute`, `OrdersScreen`, `OrderFlowContract`, `OrderFlowReducer`, `OrderPaymentRouter`, and `ordersFragment` as the Home start destination.

- [ ] **Step 1: Add the failing Home navigation contract test**

```kotlin
package com.detrapay.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNavigationContractTest {
    private val graph = File("src/main/res/navigation/home_navigation.xml").readText()
    private val layout = File("src/main/res/layout/activity_home.xml").readText()

    @Test
    fun `orders is the home start destination`() {
        assertTrue(graph.contains("app:startDestination=\"@id/ordersFragment\""))
        assertTrue(graph.contains("android:id=\"@+id/ordersFragment\""))
        assertTrue(graph.contains("com.detrapay.ui.home.orders.OrdersFragment"))
    }

    @Test
    fun `home host contains no bottom navigation`() {
        assertFalse(layout.contains("BottomNavigationView"))
        assertFalse(layout.contains("bottomAppBar"))
    }
}
```

- [ ] **Step 2: Move reducer and payment-router tests to their canonical names**

Copy the existing tests and fixtures with these exact replacements:

```text
com.detrapay.ui.home.direct_checkout -> com.detrapay.ui.home.orders
DirectCheckoutReducerTest            -> OrderFlowReducerTest
DirectCheckoutReducer                -> OrderFlowReducer
DirectCheckoutLocalState             -> OrderFlowLocalState
DirectCheckoutStep                   -> OrderFlowStep
DirectCheckoutFeeRequestTarget       -> OrderFeeRequestTarget
DirectCheckoutPaymentRouterTest      -> OrderPaymentRouterTest
DirectCheckoutPaymentRouter          -> OrderPaymentRouter
DirectCheckoutPaymentRoute           -> OrderPaymentRoute
TestDirectCheckoutFixtures           -> TestOrderFixtures
DirectCheckoutPendingPayment         -> PendingOrderPayment
```

- [ ] **Step 3: Run the new tests and verify both navigation and symbols fail**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.HomeNavigationContractTest" --tests "com.detrapay.ui.home.orders.OrderFlowReducerTest" --tests "com.detrapay.ui.home.orders.OrderPaymentRouterTest"
```

Expected: FAIL because `ordersFragment`, `OrderFlowReducer`, and `OrderPaymentRouter` do not yet exist.

- [ ] **Step 4: Create the canonical flow contract**

Copy `DirectCheckoutContract.kt` and apply the canonical types below while retaining all existing properties and actions:

```kotlin
enum class OrderFlowStep { Orders, Detail, Keypad, Method, Credit, Debit, Waiting }

enum class OrderFeeRequestTarget { CheckoutCredit, Simulator }

data class OrderFlowLocalState(
    val step: OrderFlowStep = OrderFlowStep.Orders,
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
    val feeRequestTarget: OrderFeeRequestTarget? = null,
    val feeRequestInFlight: Boolean = false,
    val activePendingPayment: PendingOrderPayment? = null,
)

data class OrdersUiState(
    val companyName: String,
    val companyDocument: String,
    val orders: List<Order>,
    val isLoading: Boolean,
    val isRefreshing: Boolean = false,
    val errorMessage: String?,
    val availablePaymentTypes: List<String>,
    val local: OrderFlowLocalState,
    val inPagePaymentState: UIState<PaymentData> = UIState.Idle(),
)

sealed interface OrderFlowAction {
    data object Logout : OrderFlowAction
    data object Reload : OrderFlowAction
    data object NewOrder : OrderFlowAction
    data class OrderPay(val order: Order) : OrderFlowAction
    data class OrderDetail(val order: Order) : OrderFlowAction
    data object Back : OrderFlowAction
    data class Key(val value: String) : OrderFlowAction
    data object OpenMethods : OrderFlowAction
    data class SelectPaymentType(val paymentType: String) : OrderFlowAction
    data class SelectInstallment(val installment: Int) : OrderFlowAction
    data object ContinueCredit : OrderFlowAction
    data object ContinueDebit : OrderFlowAction
    data object RetryInPagePayment : OrderFlowAction
    data object FinishInPagePayment : OrderFlowAction
    data class CopyPaymentCode(val text: String) : OrderFlowAction
    data object OpenSimulator : OrderFlowAction
    data object CloseSimulator : OrderFlowAction
    data class SimulatorAmountChange(val raw: String) : OrderFlowAction
    data object ConsultSimulator : OrderFlowAction
    data class SelectSimulatorInstallment(val installment: Int) : OrderFlowAction
    data class CopySimulator(val text: String) : OrderFlowAction
    data class ShareSimulator(val text: String) : OrderFlowAction
}

sealed interface OrderFlowEffect {
    data object ShowLogoutConfirmation : OrderFlowEffect
    data object NavigateToRegistration : OrderFlowEffect
    data class ConfirmManualPayment(
        val pendingPayment: PendingOrderPayment,
        val paymentData: PaymentData,
    ) : OrderFlowEffect
    data class CopyPaymentText(val text: String) : OrderFlowEffect
    data class CopySimulatorText(val text: String) : OrderFlowEffect
    data class ShareSimulatorText(val text: String) : OrderFlowEffect
    data class ShowToast(val message: String, val long: Boolean = true) : OrderFlowEffect
    data class ShowSessionExpired(val exception: Exception) : OrderFlowEffect
}
```

- [ ] **Step 5: Move the remaining production flow using the exact rename map**

Create target files, copy behavior byte-for-byte, update packages/imports, then delete the source files:

```text
DirectCheckoutFragment.kt       -> OrdersFragment.kt / OrdersFragment
DirectCheckoutRoute.kt          -> OrdersRoute.kt / OrdersRoute
DirectCheckoutScreen.kt         -> OrdersScreen.kt / OrdersScreen
DirectCheckoutContract.kt       -> OrderFlowContract.kt / declarations from Step 4
DirectCheckoutReducer.kt        -> OrderFlowReducer.kt / OrderFlowReducer
DirectCheckoutPaymentRouter.kt  -> OrderPaymentRouter.kt / OrderPaymentRouter + OrderPaymentRoute
DirectCheckoutPreviewFixtures.kt-> OrderPreviewFixtures.kt / OrderPreviewFixtures
DirectCheckoutScreenPreview.kt  -> OrdersScreenPreview.kt
DirectCheckoutBlocks.kt         -> components/OrderFlowBlocks.kt
DirectCheckoutColors.kt         -> components/OrderFlowColors.kt
screens/OrdersScreen.kt         -> screens/OrdersListScreen.kt / OrdersListScreen
screens/*.kt                    -> orders/screens/*.kt with canonical imports
```

Apply these symbol replacements throughout the new package:

```text
DirectCheckoutAction            -> OrderFlowAction
DirectCheckoutEffect            -> OrderFlowEffect
DirectCheckoutLocalState        -> OrderFlowLocalState
DirectCheckoutUiState           -> OrdersUiState
DirectCheckoutStep              -> OrderFlowStep
DirectCheckoutFeeRequestTarget  -> OrderFeeRequestTarget
DirectCheckoutReducer           -> OrderFlowReducer
DirectCheckoutPaymentRoute      -> OrderPaymentRoute
DirectCheckoutPaymentRouter     -> OrderPaymentRouter
DirectCheckoutColors            -> OrderFlowColors
```

Rename parameters `directCheckoutErrorMessage` to `ordersErrorMessage`; keep every user-visible string unchanged.

- [ ] **Step 6: Make Orders the static Home root**

Replace `HomeActivity` with the same back/logout behavior but no mode observer or navigation mutation:

```kotlin
@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityHomeBinding.inflate(layoutInflater).root)
        viewModel.loadScreenContent()
        addOnBackPressedCallback()
    }

    private fun addOnBackPressedCallback() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) { showLogoutDialog() }
        } else {
            onBackPressedDispatcher.addCallback(this) { showLogoutDialog() }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_dialog_title)
            .setMessage(R.string.logout_dialog_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .show()
    }
}
```

Use this complete graph shape:

```xml
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/home_navigation"
    app:startDestination="@id/ordersFragment">

    <fragment
        android:id="@+id/ordersFragment"
        android:name="com.detrapay.ui.home.orders.OrdersFragment"
        android:label="Pedidos" />

    <activity
        android:id="@+id/registrationActivity"
        android:name="com.detrapay.ui.registration.RegistrationActivity"
        tools:layout="@layout/activity_registration" />
</navigation>
```

`activity_home.xml` must contain only the root `ConstraintLayout` and a `FragmentContainerView` constrained to all four parent edges; remove the bottom constraint to `bottomAppBar` and delete the `BottomNavigationView`.

- [ ] **Step 7: Rename the two used string resources and remove obsolete direct-checkout resources**

```xml
<string name="orders_error">Nao foi possivel carregar os pedidos.</string>
<string name="orders_installments_error">Nao foi possivel obter as parcelas para esse pagamento.</string>
```

Update `OrdersFragment` to use these names. Remove all `direct_checkout_*` strings/plurals and delete the two unused direct-checkout XML layouts.

- [ ] **Step 8: Run canonical flow and navigation tests**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.HomeNavigationContractTest" --tests "com.detrapay.ui.home.orders.OrderFlowReducerTest" --tests "com.detrapay.ui.home.orders.OrderPaymentRouterTest" --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
.\gradlew.bat compileDebugKotlin
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [ ] **Step 9: Prove direct-checkout naming is gone from app code and resources**

```powershell
rg -n -i "direct[_ -]?checkout|DirectCheckout" app/src/main app/src/test
```

Expected: exit code 1 and no matches.

- [ ] **Step 10: Commit the canonical Home flow**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: make orders the canonical home flow"
```

---

### Task 4: Remove legacy Home surfaces and resources

**Files:**
- Modify: `app/src/test/java/com/detrapay/ui/home/HomeNavigationContractTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/styles.xml`
- Delete: `app/src/main/java/com/detrapay/ui/home/registration/`
- Delete: `app/src/main/java/com/detrapay/ui/home/order_list/`
- Delete: `app/src/main/java/com/detrapay/ui/home/profile/`
- Delete: `app/src/main/java/com/detrapay/ui/home/payment_history/`
- Delete: `app/src/main/java/com/detrapay/ui/notification/`
- Delete: `app/src/main/res/menu/home_navigation_menu.xml`
- Delete: legacy layouts and bottom-navigation resources listed below

**Interfaces:**
- Consumes: canonical Home graph from Task 3.
- Produces: no alternate Home implementation, menu, Activity, or resource set.

- [ ] **Step 1: Extend the architecture test to fail while legacy sources exist**

Add to `HomeNavigationContractTest`:

```kotlin
@Test
fun `legacy home surfaces do not exist`() {
    val removedPaths = listOf(
        "src/main/java/com/detrapay/ui/home/registration",
        "src/main/java/com/detrapay/ui/home/order_list",
        "src/main/java/com/detrapay/ui/home/profile",
        "src/main/java/com/detrapay/ui/home/payment_history",
        "src/main/java/com/detrapay/ui/notification",
        "src/main/res/menu/home_navigation_menu.xml",
    )
    removedPaths.forEach { path ->
        assertFalse("Legacy Home path still exists: $path", File(path).exists())
    }
}
```

- [ ] **Step 2: Run the test and verify it reports the first legacy directory**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.HomeNavigationContractTest"
```

Expected: FAIL with `Legacy Home path still exists`.

- [ ] **Step 3: Delete the legacy Kotlin packages and notification registration**

Delete every file under the five directories listed in Step 1. Remove this manifest entry:

```xml
<activity
    android:name=".ui.notification.NotificationActivity"
    android:exported="false" />
```

- [ ] **Step 4: Delete resources owned only by the removed surfaces**

Delete exactly:

```text
app/src/main/res/menu/home_navigation_menu.xml
app/src/main/res/layout/fragment_registration.xml
app/src/main/res/layout/fragment_order_list.xml
app/src/main/res/layout/home_recent_sale_card.xml
app/src/main/res/layout/home_recent_sale_card_pending.xml
app/src/main/res/layout/order_shimmer_item.xml
app/src/main/res/layout/fragment_profile.xml
app/src/main/res/layout/profile_salesman_card_item.xml
app/src/main/res/layout/fragment_payment_history.xml
app/src/main/res/layout/payment_list_item.xml
app/src/main/res/layout/activity_notification.xml
app/src/main/res/drawable/home_bottom_nav_background.xml
app/src/main/res/color/bottom_nav_item_color.xml
```

Do not delete `fragment_registration_order_*`, `fragment_registration_payment_detail.xml`, `order_payment_list_item.xml`, `employee_list_item.xml`, or `employee_shimmer_item.xml`; the current new-order and employee-selection flows use them.

- [ ] **Step 5: Remove now-unused legacy strings and styles**

Remove `home_tab_home`, `home_tab_sales`, `home_tab_profile`, all `payment_history_*` strings, and the two `TextAppearance.Detrapay.BottomNav.*` styles. Do not remove any resource beyond the exact lists in Steps 4 and 5.

- [ ] **Step 6: Run the architecture test, resource linking, and full unit suite**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.HomeNavigationContractTest"
.\gradlew.bat processDebugResources compileDebugKotlin testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit legacy surface removal**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: remove legacy home surfaces"
```

---

### Task 5: Verify the single flow and deploy to the connected device

**Files:**
- Verify: all modified production, test, resource, and manifest files
- Reference: `docs/superpowers/specs/2026-07-27-single-orders-flow-design.md`

**Interfaces:**
- Consumes: completed canonical flow from Tasks 1-4.
- Produces: passing build/tests plus device and log evidence that Pedidos is the only Home.

- [ ] **Step 1: Run the forbidden-token audit**

```powershell
rg -n -i "direct[_ -]?checkout|simplified|SellerAppMode|HomeModeRouter|appMode|sellerAppMode|APP_MODE_" app/src/main app/src/test
```

Expected: exit code 1 and no matches.

- [ ] **Step 2: Confirm the Home graph contains only canonical destinations**

```powershell
Get-Content -Raw app\src\main\res\navigation\home_navigation.xml
```

Expected: `ordersFragment` is the start destination; the only destinations are `ordersFragment` and `registrationActivity`.

- [ ] **Step 3: Run clean unit and debug build verification**

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`, with all unit tests passing and `app-debug.apk` generated.

- [ ] **Step 4: Confirm a device is connected and clear current logs**

```powershell
adb devices -l
adb logcat -c
```

Expected: device `0123abcd` is listed with state `device`; log clear exits successfully.

- [ ] **Step 5: Install and immediately open the app**

```powershell
.\gradlew.bat installDebug
adb shell am start -n com.detrapay/.ui.splash.SplashActivity
```

Expected: `BUILD SUCCESSFUL`, installation succeeds on `0123abcd`, and Activity Manager reports the splash Activity started.

- [ ] **Step 6: Capture and visually inspect the current Home**

```powershell
& 'C:\Users\gerbs\AppData\AndroidCLI\android.exe' layout --device 0123abcd --pretty
& 'C:\Users\gerbs\AppData\AndroidCLI\android.exe' screen capture --device 0123abcd -o "$env:TEMP\detrapay-single-orders-flow.png"
```

Expected: the layout contains `Pedidos`, dealership name/document, order cards, `Sair`, `Buscar pedidos`, and `Abrir acoes`; visual inspection matches the ASCII Pedidos wireframe and shows no bottom navigation.

- [ ] **Step 7: Exercise representative current-flow examples**

Using `android layout` for coordinates and `adb shell input tap`, verify without completing a real charge unless test payment authorization is explicitly available:

```text
1. Open and clear search.
2. Open order #544 and return.
3. Open the + menu and confirm Novo Pedido and Simular Parcelas.
4. Open the simulator, enter R$ 2.570,18, and close it.
5. Open Novo Pedido and cancel/back to Pedidos.
6. Confirm root back opens the logout confirmation.
```

Expected: every transition follows the reference flow, returns to Pedidos, and never reveals an alternate Home.

- [ ] **Step 8: Inspect filtered logs after the installed run**

```powershell
adb logcat -d | Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"
```

Expected: app lifecycle/network logs may appear; no `FATAL EXCEPTION`, process crash, or new stack trace caused by the migration.

- [ ] **Step 9: Review final repository state**

```powershell
git status --short
git log -6 --oneline
```

Expected: only intentional user-owned untracked artifacts may remain; the four implementation commits appear after the plan commit.
