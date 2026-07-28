# Single Orders Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the current Pedidos experience the only supported Home flow, remove all application-mode concepts and legacy Home surfaces, and enforce the approved online-versus-record-only payment persistence rules.

**Architecture:** `HomeActivity` remains the Android host and loads shared company state, while `OrdersFragment` becomes the navigation graph start destination. The canonical `com.detrapay.ui.home.orders` package owns the Compose route, immutable flow contract, reducer, payment routing, ViewModel, presentation rules, screens, and Android effects. `PaymentMethod.isOnlinePayment`, mapped only from `is_online_payment`, is the sole classification property used to choose PagBank or immediate record-only persistence.

**Tech Stack:** Kotlin 2.1.10, Android SDK 35, XML Navigation 2.8.9, Jetpack Compose Material 3, Hilt 2.57.1, LiveData/coroutines, JUnit 4, MockK, Gradle wrapper, ADB.

## Global Constraints

- Preserve the current device UI and copy except for the explicitly specified payment-entry correction below; this work is not a general redesign.
- Detail must show `Resumo financeiro`; when the order has pending balance, its action is a plain `Pagar` button with no amount and no `Pagar saldo` label.
- Every payment entry must open at `R$ 0,00`; show the pending balance only through `Text("Valor pendente: $pendingAmountLabel")` and provide the explicit shortcut `Usar valor pendente`.
- Never use the pending balance as the initial primary amount. Enable `Pagar` only after the user enters a value greater than zero or taps the pending-balance shortcut.
- Support exactly one Home surface: Pedidos.
- Do not select, persist, normalize, or branch on `complete`, `simplified`, `direct_checkout`, or any equivalent mode.
- Preserve new-order, detail, payment, installment simulation, refresh, search, report, and logout journeys reachable from Pedidos.
- Add exactly one payment-flow classification property to the Android domain model: non-null `PaymentMethod.isOnlinePayment`. Do not add or route on `requiresTerminal`, `allowsManualConfirmation`, `paymentGateway`, `shouldPersistInMemory`, or any equivalent parallel flag.
- Do not remove or incompatibly change any backend field, model, payload, or endpoint consumed by released app versions. The backend payment contract must evolve additively and remain backward compatible throughout rollout.
- The removal of parallel classification properties applies to the Android decision model and routing code, not to destructive removal of backend response fields. Existing backend properties may remain in the wire contract for older clients; the new Android domain simply does not depend on them.
- Label and group `isOnlinePayment == true` methods as `Pagamentos online`; Credit, Debit, and Pix use PagBank on the device and are persisted only after approval.
- Credit, Debit, and Pix must never be persisted or displayed with status `PENDING`. Cancellation, decline, timeout, or SDK failure performs zero payment-persistence calls.
- Treat `isOnlinePayment == false` methods as record-only payments. Store Credit (`store_credit`), Pix Transfer (`pix_manual`), and Cash (`cash`/`dinheiro`) are persisted immediately after user confirmation, never invoke PagBank, and never enter `WaitingScreen`.
- Keep Pix (`pix`) and Pix Transfer (`pix_manual`) distinct throughout model, selection, routing, and persistence.
- Preserve existing order-read and installment-simulation contracts. The new app uses the new additive payment-write contract only after its backend gate is satisfied; released app versions may continue using the retained legacy endpoints.
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
   +--> [Cartao do pedido] --> [Detalhes] --> [Pagar]
                                                  |
                                                  v
                                          [Digitar valor]
                                                  |
                                                  v
                                        [Forma de pagamento]
                                           |              |
                         [Pagamentos online]              [Pagamentos para registro]
                          |       |       |                  |       |       |
                       Credito  Debito   Pix             Credito  Transf.  Dinheiro
                          |       |       |              Loja     Pix
                          +-------+-------+                  |       |       |
                                  |                          +-------+-------+
                                  v                                  |
                        [PagBank/resultado]                    [Confirmar]
                                  |                                  |
                       aprovado: gravar uma vez              gravar imediatamente
                       falha: nao gravar                             |
                                  +---------------+------------------+
                                                  |
                                                  v
                                               Pedidos
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
+------------------------------------------------+
| [<] Pedido #544                                |
+------------------------------------------------+
| Resumo financeiro                              |
| Total          Pago             Pendente       |
| R$ 2.570,18    R$ 0,00          R$ 2.570,18    |
+------------------------------------------------+
| Pagamentos registrados                    0    |
|          Nenhum pagamento registrado          |
+------------------------------------------------+
|                   [ Pagar ]                    |
+------------------------------------------------+
```

```text
+-----------------------+  +-----------------------+
| [<] PAGAMENTO         |  | [<] R$ 1.000,00       |
| DIGITE O VALOR        |  | Escolha a forma      |
| R$ 0,00               |  | de pagamento         |
| Pedido #544           |  | Pagamentos online    |
| Valor pendente:       |  | [ Credito           ] |
| R$ 2.570,18           |  | [ Debito            ] |
| [Usar valor pendente] |  | [ Pix               ] |
| [1] [2] [3]           |  | Pagamentos p/ registro|
| [4] [5] [6]           |  | [Transf.Pix][Loja][$]|
| [7] [8] [9]           |  |                       |
| [,] [0] [apagar]      |  |                       |
| [   Pagar (inativo) ] |  |                       |
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

- Open order `#544`, confirm `Resumo financeiro` and the plain `Pagar` button, then confirm payment entry starts at `R$ 0,00`.
- Confirm `Valor pendente: R$ 2.570,18` is secondary information; tap `Usar valor pendente` and verify only that explicit action fills the primary amount.
- Clear the amount back to zero and verify `Pagar` becomes disabled; enter `R$ 1.000,00`, continue with credit, select an installment, complete, and return to Pedidos.
- Choose Pix, generate/copy its code, and return to Pedidos without losing the root navigation state.
- Decline or cancel Credit, Debit, and Pix in the PagBank SDK; verify the order receives no new payment and never shows a pending online payment.
- Confirm Store Credit, Pix Transfer, and Cash; verify each is recorded immediately without opening PagBank or `WaitingScreen`.
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
    OrderPaymentRouter.kt         Online versus record-only routing from isOnlinePayment
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

### Task 0: Satisfy the backward-compatible backend gate

**Scope:** Backend coordination and contract verification only. Do not change Android payment behavior in this task and do not remove or mutate any production backend contract.

**Current incompatibility:** The installed app creates an order receivable through `POST /orders/{id}/receivables` before PagBank approval. Card completion then uses `POST /update-split-config` and `POST /receivables/{id}/confirm-payment`; Pix uses `POST /receivables/{id}/generate-pix`. Because all of these paths depend on a persisted receivable, they cannot guarantee that Credit, Debit, and Pix never exist as `PENDING`.

**Required additive contract:**

- Keep every current endpoint and payload available for released app versions during and after the rollout.
- Keep current backend model fields, including any legacy classification or gateway metadata, while older clients may depend on them. The new Android model maps only `is_online_payment` for flow selection.
- Add a new operation that can prepare an online Credit, Debit, or Pix attempt without attaching a receivable to the order.
- Add a new idempotent operation that accepts an approved PagBank transaction and atomically creates the final approved/paid receivable, returning the updated order. Repeating the same PagBank transaction must not duplicate the payment.
- Add or expose an idempotent operation that atomically records Store Credit, Pix Transfer, or Cash as confirmed and returns the updated order.
- Do not implement create-then-delete compensation for rejected online payments.
- Publish the exact new endpoint paths, request DTOs, response DTOs, idempotency key, authentication, and error semantics before Android implementation begins. The Android service/repository files in Tasks 2 and 3 must use that published contract rather than guessed names.
- Keep `GET /payment-methods` backward compatible and return a reliable `is_online_payment` value: `true` for all Credit installments (including 13x-18x), Debit, and Pix; `false` for Store Credit, Pix Transfer (`pix_manual`), and Cash.
- Before correcting shared catalog values, run contract/consumer tests for every production client known to use them. If a released client would change behavior unsafely, expose the corrected catalog through an additive versioned/client-scoped contract and retain the legacy response for that client.

**Exact additive contract candidate (implemented locally; deployment gate still closed):**

- `POST /orders/{orderId}/payment-attempts` accepts `payment_method_id`, `amount_original`, `installments`, and `idempotency_key`. It accepts only `is_online_payment=true`, returns a separate `payment_attempt` with status `prepared`, and never creates an order receivable.
- `POST /update-split-config` additively accepts `{ "payment_attempt_id": "<uuid>", "serial": "<device serial>" }`. The released payload `{ "receivable_id": <id>, "serial": "<device serial>" }` remains supported.
- `POST /payment-attempts/{attemptId}/complete` accepts the approved PagBank `transaction_id` plus optional authorization/card/transaction log fields. It idempotently creates the order receivable directly as `paid` with `payment_origin=pagbank` and returns `{ data, updatedOrder }`.
- `POST /orders/{orderId}/manual-payments` accepts the common payment fields plus optional `transaction_log`. It accepts only `is_online_payment=false`, idempotently creates the order receivable directly as `paid` with `payment_origin=manual`, and returns `{ data, updatedOrder }` without PagBank or a waiting state.
- Every new write requires a nonblank client-generated `idempotency_key`. Repetition for the same order returns the existing result; reuse against another order returns a conflict. Online completion additionally requires the stable PagBank `transaction_id`.
- The full request/response and retry contract is recorded in backend document `docs/MOBILE_ATOMIC_PAYMENT_CONTRACT.md` on branch `codex/single-orders-flow-backend`, commit `5dcb589a`.
- Current gate status (28/07/2026): local unit tests and Deno type-check pass; the additive migration is applied and registered in `detrapay-prod`; the `mobile` Edge Function is published; remote schema/RLS/grant/catalog checks pass; and legacy/new unauthenticated smoke requests reach the function and return the expected `401 TOKEN_INVALID` without writes. End-to-end writes and idempotency remain for the controlled Android/device scenarios with an authorized user and order.

- [x] **Step 1: Verify the existing production contract is retained**

Capture contract tests or API evidence that the current receivable, confirm-payment, generate-pix, and split-config endpoints still accept the payloads used by released Android versions.

Expected: no removed field, renamed field, changed requiredness, removed endpoint, or incompatible response shape.

- [ ] **Step 2: Verify the new atomic operations in a non-production environment**

Exercise all paths with a unique external transaction/idempotency key:

```text
online approved  -> one final approved/paid receivable; updated order returned
same approval x2 -> still one receivable
online declined  -> zero receivables created
online cancelled -> zero receivables created
store_credit     -> one confirmed record; no PagBank dependency
pix_manual       -> one confirmed record; no PagBank dependency
cash             -> one confirmed record; no PagBank dependency
```

- [x] **Step 3: Verify the payment-method catalog**

Expected matrix:

```text
credit 1x-18x  is_online_payment=true
debit          is_online_payment=true
pix            is_online_payment=true
store_credit   is_online_payment=false
pix_manual     is_online_payment=false
cash/dinheiro  is_online_payment=false
```

- [x] **Step 4: Approve the deployment gate**

Deploy the additive backend contract first, run smoke tests for both the released app contract and the new atomic contract, and record the published endpoint/DTO details in this plan before implementing Android network calls.

Expected: old Android versions remain operational. If any check fails or the exact new contract is not published, stop; do not implement a client workaround and do not release the new Android flow.

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

- [x] **Step 1: Write the failing architecture test**

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

- [x] **Step 2: Run the test and verify the existing mode model fails it**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.architecture.ModeFreeDomainArchitectureTest"
```

Expected: FAIL with `Found forbidden mode token: SellerAppMode`.

- [x] **Step 3: Remove mode fields from the domain and DTOs**

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

- [x] **Step 4: Remove mode persistence from repositories**

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

- [x] **Step 5: Remove mode from Home state construction**

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

- [x] **Step 6: Rewrite the login repository test around canonical user mapping**

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

- [x] **Step 7: Run focused and full unit tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.architecture.ModeFreeDomainArchitectureTest" --tests "com.detrapay.data.repositories.LoginRepositoryTest"
.\gradlew.bat testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [x] **Step 8: Verify no domain/session mode tokens remain**

Run:

```powershell
rg -n "SellerAppMode|appMode|sellerAppMode|HomeModeRouter" app/src/main app/src/test
```

Expected: exit code 1 and no matches.

- [x] **Step 9: Commit the domain cleanup**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: remove seller app modes"
```

---

### Task 2: Canonicalize order presentation and ViewModel support

**Files:**
- Modify: `app/src/main/java/com/detrapay/data/model/PaymentMethod.kt`
- Modify: `app/src/main/java/com/detrapay/data/model/remote/PaymentMethodResponse.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/RegistrationRepository.kt`
- Modify: `app/src/main/java/com/detrapay/ui/util/PaymentTypeRules.kt`
- Modify: `app/src/test/java/com/detrapay/data/repositories/RegistrationRepositoryTest.kt`
- Modify: `app/src/test/java/com/detrapay/ui/util/PaymentTypeRulesTest.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/orders/OrdersViewModel.kt`
- Create: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`
- Modify: current files under `app/src/main/java/com/detrapay/ui/home/direct_checkout/` to consume the new names temporarily
- Delete: `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutOrderPresentation.kt`
- Delete: `app/src/main/java/com/detrapay/ui/home/simplified/SimplifiedReceivableListViewModel.kt`
- Delete: `app/src/test/java/com/detrapay/ui/home/simplified/DirectCheckoutOrderPresentationTest.kt`

**Interfaces:**
- Consumes: the backward-compatible `GET /payment-methods`, the additive payment contract approved in Task 0, `OrderRepository`, `RegistrationRepository`, `SalesmanRepository`, `PaymentTypeRules`, and `OrderPaymentTotals`.
- Produces: a domain `PaymentMethod` with one non-null routing property, plus `OrderPresentation`, `OrdersViewModel`, `OrderPaymentRequest`, `OrderSummary`, `SellerCardSummary`, and `WaitingPresentation` under `com.detrapay.ui.home.orders`.

- [x] **Step 0: Add failing tests for the single classification property**

Extend `RegistrationRepositoryTest` to prove that `is_online_payment=true` and `false` are copied to the domain model and that a missing value returns a configuration error instead of silently becoming offline. Replace tests for `requiresTerminalApproval` and `shouldPersistInMemory` in `PaymentTypeRulesTest` with normalization-only coverage proving that `pix` remains `pix` and `pix_manual` remains `pix_manual`.

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.data.repositories.RegistrationRepositoryTest" --tests "com.detrapay.ui.util.PaymentTypeRulesTest"
```

Expected: FAIL because the domain model does not expose `isOnlinePayment`, the repository ignores the field, and the obsolete routing helpers still exist.

- [x] **Step 0.1: Adjust the Android model and mapping without changing the backend response shape**

Keep all existing response DTO fields so deserialization stays backward compatible. Keep `PaymentMethodResponse.isOnlinePayment` nullable only at the transport boundary to detect an absent field. Add exactly this routing property to the domain model:

```kotlin
data class PaymentMethod(
    val id: Int,
    val name: String,
    val installments: Int,
    val interestTax: Double?,
    val paymentType: String? = null,
    val isOnlinePayment: Boolean,
) : Serializable
```

In `RegistrationRepository.loadPaymentMethods`, require `PaymentMethodResponse.isOnlinePayment` while mapping. If it is absent, return `Result.Error` identifying `GET /payment-methods`, the method id/name, and the missing `is_online_payment` field. Never default it to `false`.

Remove `PaymentTypeRules.requiresTerminalApproval` and `PaymentTypeRules.shouldPersistInMemory`. Keep normalization and fee calculation helpers only; they must not decide online versus record-only routing. Add the explicit normalization alias for `pix_manual` without mapping it to `pix`.

Update every `PaymentMethod(...)` construction in production fixtures and tests to provide `isOnlinePayment`, using the catalog semantics rather than inferring from the name.

Run the two focused tests again. Expected: `BUILD SUCCESSFUL`.

- [x] **Step 1: Move the presentation test to the canonical package first**

Create `OrderPresentationTest.kt` by copying the existing test body and applying exactly:

```text
package com.detrapay.ui.home.simplified
  -> package com.detrapay.ui.home.orders
DirectCheckoutOrderPresentationTest
  -> OrderPresentationTest
DirectCheckoutOrderPresentation
  -> OrderPresentation
```

Add these payment-entry assertions to the moved test:

```kotlin
@Test
fun `blank payment input is zero and never falls back to pending balance`() {
    assertEquals(0.0, OrderPresentation.paymentAmount(""), 0.0)
    assertEquals("R$ 0,00", OrderPresentation.paymentDisplayAmount(""))
}

@Test
fun `typed payment input uses only the entered digits`() {
    assertEquals(1_000.0, OrderPresentation.paymentAmount("100000"), 0.0)
    assertEquals("R$ 1.000,00", OrderPresentation.paymentDisplayAmount("100000"))
}
```

- [x] **Step 2: Run the moved test and verify the target object is missing**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
```

Expected: compilation FAIL with `Unresolved reference 'OrderPresentation'`.

- [x] **Step 3: Create the canonical presentation file without behavior changes**

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

Replace the pending-balance fallback functions with:

```kotlin
fun paymentAmount(digits: String): Double {
    return digits.toDoubleOrNull()?.let { it / 100.0 } ?: 0.0
}

fun paymentDisplayAmount(digits: String): String {
    return formatCurrency(paymentAmount(digits))
}
```

There must be no overload that accepts `pendingAmount`; the pending balance is separate presentation data.

- [x] **Step 4: Create the canonical Orders ViewModel**

Copy only the behavior used by the current Pedidos flow. Remove `receivableListState`, the old receivables-only `loadScreenContent`, and the create-pending-then-confirm API. An order payment request is local and contains no persisted receivable:

```kotlin
data class OrderPaymentRequest(
    val order: Order,
    val paymentMethod: PaymentMethod,
    val amount: Double,
    val installments: Int,
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
    val paymentRecordState: LiveData<UIState<Order>>

    fun loadOrders(forceRefresh: Boolean = false)
    fun loadPaymentMethods(forceRefresh: Boolean = false)
    fun availablePaymentMethods(): List<PaymentMethod>
    fun calculateFees(value: Double, paymentType: String)
    fun clearFeesState()
    fun recordApprovedOnlinePayment(request: OrderPaymentRequest, approval: PaymentData)
    fun recordOfflinePayment(request: OrderPaymentRequest)
    fun clearPaymentState()
    fun prefetchRegistrationData()
}
```

`recordApprovedOnlinePayment` and `recordOfflinePayment` use the exact additive repository operations published and approved in Task 0. Neither method may call `addPendingReceivable`. The online method is reachable only after PagBank approval; the offline method is reachable only after the user confirms a method with `isOnlinePayment == false`.

The implementation is the existing ViewModel logic with these exact symbol changes:

```text
SimplifiedReceivableListViewModel       -> OrdersViewModel
directOrderListState                    -> orderListState
_directOrderListState                   -> _orderListState
loadDirectCheckoutOrders                -> loadOrders
clearDirectCheckoutPaymentState         -> clearPaymentState
DirectCheckoutOrderPresentation         -> OrderPresentation
```

Do not mechanically copy `addDirectCheckoutPendingPayment`, `confirmDirectCheckoutManualPayment`, or `consumeDirectCheckoutPendingPayment`; replace those behaviors with the two atomic recording methods above.

- [x] **Step 5: Bridge current consumers to the canonical support API**

In the existing fragment, route, contract, reducer, payment router, screens, and previews, replace imports and calls using this map:

```text
com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
  -> com.detrapay.ui.home.orders.OrderPresentation
com.detrapay.ui.home.simplified.SimplifiedReceivableListViewModel
  -> com.detrapay.ui.home.orders.OrdersViewModel
com.detrapay.ui.home.simplified.DirectCheckoutPendingPayment
  -> com.detrapay.ui.home.orders.OrderPaymentRequest (local request only; no receivable)
```

Apply the ViewModel method/property map from Step 4 to `DirectCheckoutFragment.kt` and `DirectCheckoutRoute.kt`.

- [x] **Step 6: Run presentation tests and compile all current consumers**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.data.repositories.RegistrationRepositoryTest" --tests "com.detrapay.ui.util.PaymentTypeRulesTest" --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
.\gradlew.bat compileDebugKotlin
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [x] **Step 7: Remove the old simplified package and prove it is gone**

Delete the two old production files and the old test. Then run:

```powershell
rg -n -i "simplified" app/src/main app/src/test
```

Expected: exit code 1 and no matches.

- [x] **Step 8: Commit the canonical support layer**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: name orders support code canonically"
```

---

### Task 3: Make the canonical Orders flow the Home root

**Files:**
- Create: all target files under `app/src/main/java/com/detrapay/ui/home/orders/` listed in Target file structure
- Modify: `app/src/main/java/com/detrapay/data/api/DetrapayService.kt` using the exact additive contract approved in Task 0
- Modify: `app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/OrderRepository.kt`
- Modify: `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`
- Modify: corresponding request/response DTO files named by the published backend contract
- Modify: `app/src/test/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSourceTest.kt`
- Modify: `app/src/test/java/com/detrapay/data/repositories/OrderRepositoryTest.kt`
- Modify: `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`
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
- Consumes: `OrdersViewModel`, `OrderPresentation`, `HomeViewModel`, `PaymentDialogViewModel`, and the additive atomic payment operations approved in Task 0.
- Produces: `OrdersFragment`, `OrdersRoute`, `OrdersScreen`, `OrderFlowContract`, `OrderFlowReducer`, `OrderPaymentRouter`, and `ordersFragment` as the Home start destination.

- [x] **Step 1: Add the failing Home navigation contract test**

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

- [x] **Step 2: Move reducer and payment-router tests to their canonical names**

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
DirectCheckoutPendingPayment         -> OrderPaymentRequest
```

Replace the old auto-filled start-payment test and add the explicit shortcut test:

```kotlin
@Test
fun `start payment opens keypad with zero amount`() {
    val order = order(total = 2570.18, paidAmount = 1200.0)

    val state = OrderFlowReducer.startPayment(OrderFlowLocalState(), order)

    assertEquals(order, state.selectedOrder)
    assertEquals(OrderFlowStep.Keypad, state.step)
    assertEquals("", state.paymentDigits)
}

@Test
fun `pending balance is filled only by explicit shortcut`() {
    val order = order(total = 2570.18, paidAmount = 1200.0)
    val initial = OrderFlowReducer.startPayment(OrderFlowLocalState(), order)

    val filled = OrderFlowReducer.usePendingAmount(initial, order)

    assertEquals("137018", filled.paymentDigits)
}
```

Replace type-name routing assertions with property-based cases. Use deliberately misleading names to prove that only the property decides:

```kotlin
@Test
fun `true is online regardless of payment type text`() {
    val request = paymentRequest(method = paymentMethod(paymentType = "cash", isOnlinePayment = true))
    assertTrue(OrderPaymentRouter.routeFor(request) is OrderPaymentRoute.Online)
}

@Test
fun `false is record only regardless of payment type text`() {
    val request = paymentRequest(method = paymentMethod(paymentType = "credit", isOnlinePayment = false))
    assertTrue(OrderPaymentRouter.routeFor(request) is OrderPaymentRoute.RecordOnly)
}
```

Add `PaymentDialogViewModelTest` cases before production changes:

- Credit, Debit, and Pix cancellation/decline/timeout call the PagBank adapter but execute zero repository persistence calls.
- Credit, Debit, and Pix approval execute exactly one atomic approved-payment call using the PagBank transaction id as idempotency key.
- Pix uses the PagBank SDK (`PlugPag.TYPE_PIX`) and never calls the legacy backend-generated Pix endpoint.
- A record-only request never reaches `PaymentDialogViewModel`.

- [x] **Step 3: Run the new tests and verify both navigation and symbols fail**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.HomeNavigationContractTest" --tests "com.detrapay.ui.home.orders.OrderFlowReducerTest" --tests "com.detrapay.ui.home.orders.OrderPaymentRouterTest"
```

Expected: FAIL because `ordersFragment`, `OrderFlowReducer`, and `OrderPaymentRouter` do not yet exist.

- [x] **Step 4: Create the canonical flow contract**

Copy `DirectCheckoutContract.kt` and apply the canonical types below while retaining all existing properties and actions:

```kotlin
enum class OrderFlowStep { Orders, Detail, Keypad, Method, Credit, Debit, Waiting }

enum class OrderFeeRequestTarget { CheckoutCredit, Simulator }

data class OrderFlowLocalState(
    val step: OrderFlowStep = OrderFlowStep.Orders,
    val selectedOrder: Order? = null,
    val paymentDigits: String = "",
    val selectedPaymentMethod: PaymentMethod? = null,
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
    val activePaymentRequest: OrderPaymentRequest? = null,
)

data class OrdersUiState(
    val companyName: String,
    val companyDocument: String,
    val orders: List<Order>,
    val isLoading: Boolean,
    val isRefreshing: Boolean = false,
    val errorMessage: String?,
    val availablePaymentMethods: List<PaymentMethod>,
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
    data object UsePendingAmount : OrderFlowAction
    data object OpenMethods : OrderFlowAction
    data class SelectPaymentMethod(val paymentMethod: PaymentMethod) : OrderFlowAction
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
    data class ConfirmRecordOnlyPayment(val request: OrderPaymentRequest) : OrderFlowEffect
    data class CopyPaymentText(val text: String) : OrderFlowEffect
    data class CopySimulatorText(val text: String) : OrderFlowEffect
    data class ShareSimulatorText(val text: String) : OrderFlowEffect
    data class ShowToast(val message: String, val long: Boolean = true) : OrderFlowEffect
    data class ShowSessionExpired(val exception: Exception) : OrderFlowEffect
}
```

- [x] **Step 5: Move the remaining production flow using the exact rename map**

Create target files, copy the current UI and non-payment behavior, update packages/imports, then delete the source files. Do not copy the old type-name routing or create-pending-then-confirm persistence behavior:

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

Rename parameters `directCheckoutErrorMessage` to `ordersErrorMessage`; keep user-visible strings unchanged except for the explicit `Pagar`, `Text("Valor pendente: $pendingAmountLabel")`, and `Usar valor pendente` requirements.

Implement the payment-entry correction in `OrderFlowReducer` exactly as follows:

```kotlin
fun startPayment(state: OrderFlowLocalState, order: Order): OrderFlowLocalState {
    return state.copy(
        selectedOrder = order,
        paymentDigits = "",
        selectedPaymentMethod = null,
        selectedInstallment = 1,
        creditInstallments = emptyList(),
        feesLoading = false,
        feesError = null,
        showSimulator = false,
        feeRequestTarget = null,
        step = OrderFlowStep.Keypad,
    )
}

fun usePendingAmount(state: OrderFlowLocalState, order: Order): OrderFlowLocalState {
    val pendingAmount = OrderPresentation.summary(order).missingAmount
    return state.copy(paymentDigits = (pendingAmount * 100).roundToLong().toString())
}
```

Implement the router with no lookup by name or normalized type:

```kotlin
sealed interface OrderPaymentRoute {
    data class Online(val request: OrderPaymentRequest) : OrderPaymentRoute
    data class RecordOnly(val request: OrderPaymentRequest) : OrderPaymentRoute
}

object OrderPaymentRouter {
    fun routeFor(request: OrderPaymentRequest): OrderPaymentRoute {
        return if (request.paymentMethod.isOnlinePayment) {
            OrderPaymentRoute.Online(request)
        } else {
            OrderPaymentRoute.RecordOnly(request)
        }
    }
}
```

`MethodScreen` receives `List<PaymentMethod>` and emits the selected object, never only a `String`:

```kotlin
@Composable
fun MethodScreen(
    order: Order,
    amount: Double,
    paymentMethods: List<PaymentMethod>,
    onBack: () -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
)
```

Render `paymentMethods.filter { it.isOnlinePayment }` under `Pagamentos online` and the `false` group under `Pagamentos para registro`. Resolve visual labels from `paymentType`, but retain and submit the complete selected object. Pix must match only normalized `pix`; Pix Transfer must match only `pix_manual`. Never reuse the Pix object or type for Pix Transfer.

When a method is selected, create an in-memory `OrderPaymentRequest`. Route it as follows:

- `Online`: Credit may open installments; Credit, Debit, and Pix then enter the PagBank flow. `WaitingScreen` is allowed only while representing this active SDK operation/result. Do not call `addPendingReceivable`, legacy `generatePixCharge`, or any persistence API before approval.
- `RecordOnly`: ask for the existing user confirmation and emit `ConfirmRecordOnlyPayment`. Call `OrdersViewModel.recordOfflinePayment` immediately after confirmation, show inline loading/error if needed, refresh from the returned order, and return to Pedidos. Do not invoke `PaymentDialogViewModel`, PagBank, or `WaitingScreen`.

Adjust `PaymentDialogViewModel` to start PagBank from the local request instead of an `OrderReceivableItem`. Credit maps to the existing credit transaction type, Debit to the existing debit transaction type, and Pix to `PlugPag.TYPE_PIX`. On decline, cancellation, timeout, or SDK error, publish the failure result and make zero calls to payment persistence. On approval, pass the approved transaction and its stable transaction id to `recordApprovedOnlinePayment` exactly once. Only the backend response may add the final payment to the order; the client must not create a local `PENDING` item.

In `OrdersRoute`, handle the shortcut and calculate payments only from entered digits:

```kotlin
OrderFlowAction.UsePendingAmount -> {
    localState.selectedOrder?.let { order ->
        localState = OrderFlowReducer.usePendingAmount(localState, order)
    }
}

private fun currentPaymentAmount(state: OrderFlowLocalState): Double {
    return OrderPresentation.paymentAmount(state.paymentDigits)
}
```

In `OrdersScreen`, keep `pendingAmount` separate, pass it to the keypad only as formatted secondary information, and use entered digits as the primary amount:

```kotlin
val pendingAmount = currentOrder?.let { OrderPresentation.summary(it).missingAmount } ?: 0.0
val amount = OrderPresentation.paymentAmount(local.paymentDigits)

KeypadScreen(
    order = currentOrder,
    displayAmount = OrderPresentation.paymentDisplayAmount(local.paymentDigits),
    pendingAmountLabel = OrderPresentation.formatCurrency(pendingAmount),
    canPay = amount > 0.0,
    onBack = { onAction(OrderFlowAction.Back) },
    onKey = { onAction(OrderFlowAction.Key(it)) },
    onUsePendingAmount = { onAction(OrderFlowAction.UsePendingAmount) },
    onPay = { onAction(OrderFlowAction.OpenMethods) },
)
```

`DetailScreen` must preserve `Resumo financeiro` and use this button content:

```kotlin
Text("Pagar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
```

`KeypadScreen` must expose these parameters and UI rules:

```kotlin
@Composable
fun KeypadScreen(
    order: Order,
    displayAmount: String,
    pendingAmountLabel: String,
    canPay: Boolean,
    onBack: () -> Unit,
    onKey: (String) -> Unit,
    onUsePendingAmount: () -> Unit,
    onPay: () -> Unit,
)
```

Under the primary `displayAmount`, render `Text("Valor pendente: $pendingAmountLabel")` and an `OutlinedButton(onClick = onUsePendingAmount)` containing `Text("Usar valor pendente")`. Set the primary `Button(enabled = canPay, onClick = onPay, ...)`. Do not render the pending balance inside the primary amount or the Detail action label.

- [x] **Step 6: Make Orders the static Home root**

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

- [x] **Step 7: Rename the two used string resources and remove obsolete direct-checkout resources**

```xml
<string name="orders_error">Nao foi possivel carregar os pedidos.</string>
<string name="orders_installments_error">Nao foi possivel obter as parcelas para esse pagamento.</string>
```

Update `OrdersFragment` to use these names. Remove all `direct_checkout_*` strings/plurals and delete the two unused direct-checkout XML layouts.

- [x] **Step 8: Run canonical flow and navigation tests**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.HomeNavigationContractTest" --tests "com.detrapay.ui.home.orders.OrderFlowReducerTest" --tests "com.detrapay.ui.home.orders.OrderPaymentRouterTest" --tests "com.detrapay.ui.home.orders.OrderPresentationTest" --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest" --tests "com.detrapay.data.repositories.OrderRepositoryTest" --tests "com.detrapay.data.datasources.remote.DetrapayRemoteDataSourceTest"
.\gradlew.bat compileDebugKotlin
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [x] **Step 9: Prove direct-checkout naming is gone from app code and resources**

```powershell
rg -n -i "direct[_ -]?checkout|DirectCheckout" app/src/main app/src/test
```

Expected: exit code 1 and no matches.

- [x] **Step 10: Commit the canonical Home flow**

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

- [x] **Step 1: Extend the architecture test to fail while legacy sources exist**

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

- [x] **Step 2: Run the test and verify it reports the first legacy directory**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.HomeNavigationContractTest"
```

Expected: FAIL with `Legacy Home path still exists`.

- [x] **Step 3: Delete the legacy Kotlin packages and notification registration**

Delete every file under the five directories listed in Step 1. Remove this manifest entry:

```xml
<activity
    android:name=".ui.notification.NotificationActivity"
    android:exported="false" />
```

- [x] **Step 4: Delete resources owned only by the removed surfaces**

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

- [x] **Step 5: Remove now-unused legacy strings and styles**

Remove `home_tab_home`, `home_tab_sales`, `home_tab_profile`, all `payment_history_*` strings, and the two `TextAppearance.Detrapay.BottomNav.*` styles. Do not remove any resource beyond the exact lists in Steps 4 and 5.

- [x] **Step 6: Run the architecture test, resource linking, and full unit suite**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.HomeNavigationContractTest"
.\gradlew.bat processDebugResources compileDebugKotlin testDebugUnitTest
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

- [x] **Step 7: Commit legacy surface removal**

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

**Device verification status (28/07/2026):** clean build, 106 unit tests, APK installation, app launch, Home capture, and crash-log inspection pass on `0123abcd`. The captured Home is the single Pedidos surface with no bottom navigation. Representative order/payment transitions remain blocked because the published `mobile` Edge Function currently returns `503 BOOT_ERROR` even for legacy endpoints; this is a backend deployment startup failure and must be repaired before release. No Android workaround may hide it.

- [x] **Step 0: Recheck the production-safety gate**

Confirm that the additive backend contract from Task 0 is deployed, its legacy compatibility smoke tests pass, and no existing field/model/endpoint was removed or changed incompatibly. Confirm the new Android build targets only the published new payment operations.

Expected: both released app versions and the new contract work. Otherwise stop the Android release.

- [x] **Step 1: Run the forbidden-token audit**

```powershell
rg -n -i "direct[_ -]?checkout|simplified|SellerAppMode|HomeModeRouter|appMode|sellerAppMode|APP_MODE_" app/src/main app/src/test
rg -n "requiresTerminalApproval|shouldPersistInMemory|allowsManualConfirmation|paymentGateway" app/src/main app/src/test
```

Expected: both commands exit with code 1 and no matches. `isOnlinePayment` is the only payment-flow classification property in the Android domain and routing code.

- [x] **Step 2: Confirm the Home graph contains only canonical destinations**

```powershell
Get-Content -Raw app\src\main\res\navigation\home_navigation.xml
```

Expected: `ordersFragment` is the start destination; the only destinations are `ordersFragment` and `registrationActivity`.

- [x] **Step 3: Run clean unit and debug build verification**

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`, with all unit tests passing and `app-debug.apk` generated.

- [x] **Step 4: Confirm a device is connected and clear current logs**

```powershell
adb devices -l
adb logcat -c
```

Expected: device `0123abcd` is listed with state `device`; log clear exits successfully.

- [x] **Step 5: Install and immediately open the app**

```powershell
.\gradlew.bat installDebug
adb shell am start -n com.detrapay/.ui.splash.SplashActivity
```

Expected: `BUILD SUCCESSFUL`, installation succeeds on `0123abcd`, and Activity Manager reports the splash Activity started.

- [x] **Step 6: Capture and visually inspect the current Home**

```powershell
& 'C:\Users\gerbs\AppData\AndroidCLI\android.exe' layout --device 0123abcd --pretty
& 'C:\Users\gerbs\AppData\AndroidCLI\android.exe' screen capture --device 0123abcd -o "$env:TEMP\detrapay-single-orders-flow.png"
```

Expected: the layout contains `Pedidos`, dealership name/document, order cards, `Sair`, `Buscar pedidos`, and `Abrir acoes`; visual inspection matches the ASCII Pedidos wireframe and shows no bottom navigation.

- [ ] **Step 7: Exercise representative current-flow examples**

Using `android layout` for coordinates and `adb shell input tap`, verify without completing a real charge unless test payment authorization is explicitly available:

```text
1. Open and clear search.
2. Open order #544; confirm `Resumo financeiro` and a plain `Pagar` button with no amount.
3. Tap `Pagar`; confirm the primary value is `R$ 0,00`, the hint is `Valor pendente: R$ 2.570,18`, and `Pagar` is disabled.
4. Tap `Usar valor pendente`; confirm the primary value becomes `R$ 2.570,18` only after this action and `Pagar` becomes enabled.
5. Delete the filled amount; confirm it returns to `R$ 0,00` and disables `Pagar`, then return to Pedidos.
6. Enter a positive value and confirm the method screen groups Credit, Debit, and Pix under `Pagamentos online`.
7. Confirm `Pagamentos para registro` contains distinct Store Credit, Pix Transfer, and Cash options; Pix Transfer must carry `pix_manual`, never `pix`.
8. In an authorized test environment, decline/cancel each online PagBank type and verify the order API returns no new receivable and the detail never displays an online `PENDING` payment.
9. In an authorized test environment, approve each online type and verify exactly one final approved/paid receivable appears after SDK approval.
10. Confirm each record-only type and verify it saves immediately without PagBank or `WaitingScreen`.
11. Open the + menu and confirm Novo Pedido and Simular Parcelas.
12. Open the simulator, enter R$ 2.570,18, and close it.
13. Open Novo Pedido and cancel/back to Pedidos.
14. Confirm root back opens the logout confirmation.
```

Expected: every transition follows the reference flow, returns to Pedidos, and never reveals an alternate Home.

- [x] **Step 8: Inspect filtered logs after the installed run**

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
