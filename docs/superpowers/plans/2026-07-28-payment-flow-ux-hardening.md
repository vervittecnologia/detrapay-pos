# Payment Flow UX Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tornar o fluxo de pagamento mais denso, claro, seguro e acessível na SmartPOS sem alterar contratos financeiros ou a integração PlugPag.

**Architecture:** Preservar o wizard Compose e os reducers atuais. Regras monetárias e validações ficam em funções puras de `OrderPresentation`; estado de simulador, confirmação de cancelamento e resultado persistente ficam em `OrderFlowLocalState`/`OrderFlowReducer`; os composables apenas representam e disparam esses estados.

**Tech Stack:** Kotlin, Android Compose Material 3, JUnit 4, testes Android/Compose existentes, Gradle, ADB e Android CLI.

## Global Constraints

- Preservar o valor final com juros, número de parcelas e `INSTALLMENT_TYPE_SELLER` já enviados ao PlugPag.
- Não alterar endpoints nem improvisar dados ausentes do backend.
- Manter valor original, total com juros e valor das parcelas com juros em todas as opções.
- Textos normais devem atingir WCAG AA de 4,5:1.
- Alvos de toque efetivos devem permanecer com pelo menos 48 dp.
- Depois de instalar, abrir o app e monitorar `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`.

---

### Task 1: Normalizar entrada monetária e validar saldo

**Files:**
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/KeypadScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt`

**Interfaces:**
- Produces: `OrderPresentation.nextPaymentDigits(digits: String, key: String): String`
- Produces: `OrderPresentation.paymentAmountError(amount: Double, pendingAmount: Double): String?`
- Consumes: `OrderPresentation.summary(order).missingAmount`

- [ ] **Step 1: Write failing monetary input tests**

```kotlin
@Test
fun `payment input accepts only digits and delete`() {
    assertEquals("123", OrderPresentation.nextPaymentDigits("12", "3"))
    assertEquals("12", OrderPresentation.nextPaymentDigits("123", "DEL"))
    assertEquals("12", OrderPresentation.nextPaymentDigits("12", ","))
    assertEquals("12", OrderPresentation.nextPaymentDigits("12", "A"))
}

@Test
fun `payment amount validation rejects zero and amount above pending`() {
    assertEquals("Informe um valor maior que zero.", OrderPresentation.paymentAmountError(0.0, 100.0))
    assertEquals(
        "O valor não pode ser maior que o saldo pendente de R$ 100,00.",
        OrderPresentation.paymentAmountError(100.01, 100.0),
    )
    assertEquals(null, OrderPresentation.paymentAmountError(100.0, 100.0))
}
```

- [ ] **Step 2: Run tests and verify expected failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
```

Expected: FAIL because comma is currently appended and `paymentAmountError` does not exist.

- [ ] **Step 3: Implement minimal pure rules**

```kotlin
fun nextPaymentDigits(digits: String, key: String): String = when {
    key == "DEL" -> digits.dropLast(1)
    key.length == 1 && key[0].isDigit() && digits.length < 10 -> digits + key
    else -> digits
}

fun paymentAmountError(amount: Double, pendingAmount: Double): String? = when {
    amount <= 0.0 -> "Informe um valor maior que zero."
    amount > pendingAmount -> "O valor não pode ser maior que o saldo pendente de ${formatCurrency(pendingAmount)}."
    else -> null
}
```

Remove the comma key from `PaymentKeypad`. Pass an inline `amountError` to `KeypadScreen`, use it to disable Continue, and reuse the same validation in `OrdersRoute` before quoting or opening review.

- [ ] **Step 4: Run focused tests**

Run the command from Step 2.

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt app/src/main/java/com/detrapay/ui/home/orders/OrdersScreen.kt app/src/main/java/com/detrapay/ui/home/orders/screens/KeypadScreen.kt app/src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt
git commit -m "fix: harden payment amount input"
```

### Task 2: Tornar o simulador modal, numérico e orientado pela seleção

**Files:**
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderFlowReducerTest.kt`
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderFlowReducer.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/InstallmentSimulatorScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersScreen.kt`

**Interfaces:**
- Produces: `simulatorShareText(amount: Double, installment: InstallmentFee): String`
- Produces: simulator state with `simulatorSelectedInstallment == null` after loading
- Consumes: `OrderFlowAction.SelectSimulatorInstallment`

- [ ] **Step 1: Write failing selection/share tests**

```kotlin
@Test
fun `simulator loaded does not preselect an installment`() {
    val loaded = OrderFlowReducer.simulatorLoaded(
        OrderFlowReducer.startSimulatorLoading(OrderFlowLocalState(showSimulator = true)),
        listOf(installmentFee()),
        "Nenhuma opção disponível.",
    )
    assertNull(loaded.simulatorSelectedInstallment)
}

@Test
fun `simulator share text contains only selected installment`() {
    val selected = installmentFee()
    val text = simulatorShareText(100.0, selected)
    assertTrue(text.contains("2x de R$ 50,00"))
    assertEquals(1, Regex("""\\dx de R\\$""").findAll(text).count())
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderFlowReducerTest" --tests "com.detrapay.ui.home.orders.OrderPresentationTest"
```

Expected: FAIL because the last installment is preselected and the share API accepts a list.

- [ ] **Step 3: Implement simulator behavior**

Set `simulatorSelectedInstallment = null` in `simulatorLoaded`. Change share text to format one `InstallmentFee`. In `InstallmentSimulatorScreen`:

```kotlin
val focusManager = LocalFocusManager.current
val keyboardController = LocalSoftwareKeyboardController.current

OutlinedTextField(
    value = formattedAmount,
    onValueChange = onAmountChange,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    keyboardActions = KeyboardActions(onDone = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }),
)
```

Remove `leadingIcon = { Text("R$") }`. Clear focus/hide keyboard before `onConsult`. Keep one close action. Disable Copy/WhatsApp until a selected installment exists and generate text only for that selection.

Wrap the simulator with Compose `Dialog` using full-screen properties or apply `clearAndSetSemantics`/modal semantics so background descendants are not exposed.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/test/java/com/detrapay/ui/home/orders/OrderFlowReducerTest.kt app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt app/src/main/java/com/detrapay/ui/home/orders/OrderFlowReducer.kt app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt app/src/main/java/com/detrapay/ui/home/orders/screens/InstallmentSimulatorScreen.kt app/src/main/java/com/detrapay/ui/home/orders/OrdersScreen.kt
git commit -m "fix: clarify installment simulator interactions"
```

### Task 3: Compactar lista e esclarecer métodos

**Files:**
- Create: `app/src/test/java/com/detrapay/ui/home/orders/OrdersUxContractTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/OrdersListScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/MethodScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersScreenPreview.kt`
- Modify: `app/src/androidTest/java/com/detrapay/golden/PaymentConfigCreditGoldenTest.kt`
- Update: `app/src/androidTest/assets/goldens/payment_config_credit.png`

**Interfaces:**
- Consumes: existing `onNewOrder`, `onOpenSimulator`, `onSelectPaymentMethod`
- Produces: neutral method rows without implicit Credit selection

- [ ] **Step 1: Add a failing source contract test**

Create `OrdersUxContractTest.kt` with an assertion that reads the source and rejects the hardcoded Credit border condition:

```kotlin
@Test
fun `payment methods do not style credit as preselected`() {
    val source = File("src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt").readText()
    assertFalse(source.contains("""title == "Crédito""""))
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrdersUxContractTest"
```

Expected: FAIL while the Credit-specific border exists.

- [ ] **Step 3: Implement compact hierarchy**

Remove the Credit special border. Rename groups to “Cobrar na maquininha” and “Apenas registrar no pedido”. Use explicit “Indisponível” copy when a method remains visible.

In `OrdersListScreen`, reduce card vertical padding and repeated metrics, keep client/order, total, pending/settled and one contextual action. Replace the unlabeled expanded FAB choice with a visible “Novo pedido” primary action and a secondary “Simular parcelas”.

- [ ] **Step 4: Render/update golden and verify**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.detrapay.golden.PaymentConfigCreditGoldenTest
```

Expected: test records or validates the updated method screen per the project golden workflow.

- [ ] **Step 5: Commit**

```powershell
git add app/src/test/java/com/detrapay/ui/home/orders/OrdersUxContractTest.kt app/src/main/java/com/detrapay/ui/home/orders/screens/OrdersListScreen.kt app/src/main/java/com/detrapay/ui/home/orders/screens/MethodScreen.kt app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt app/src/main/java/com/detrapay/ui/home/orders/OrdersScreenPreview.kt app/src/androidTest
git commit -m "feat: improve payment flow density and clarity"
```

### Task 4: Proteger cancelamento e persistir resultado aprovado

**Files:**
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderFlowReducerTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderFlowContract.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderFlowReducer.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/WaitingScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/orders/screens/PaymentResultScreen.kt`

**Interfaces:**
- Add: `OrderFlowStep.Result`
- Add: `showPaymentCancelConfirmation: Boolean`
- Add actions: `RequestPaymentCancel`, `DismissPaymentCancel`, `ConfirmPaymentCancel`
- Preserve: successful `PaymentData` until `FinishInPagePayment`

- [ ] **Step 1: Write failing reducer tests**

```kotlin
@Test
fun `back during waiting requests confirmation without leaving`() {
    val state = OrderFlowLocalState(step = OrderFlowStep.Waiting)
    val next = OrderFlowReducer.requestPaymentCancel(state)
    assertEquals(OrderFlowStep.Waiting, next.step)
    assertTrue(next.showPaymentCancelConfirmation)
}

@Test
fun `payment success opens persistent result`() {
    val next = OrderFlowReducer.showPaymentResult(
        OrderFlowLocalState(step = OrderFlowStep.Waiting),
    )
    assertEquals(OrderFlowStep.Result, next.step)
}
```

- [ ] **Step 2: Verify RED**

Run focused reducer tests and expect missing APIs/enum failure.

- [ ] **Step 3: Implement state transitions and screens**

Route Back from Waiting to `RequestPaymentCancel`. Show an `AlertDialog` with “Cancelar pagamento?” and “Continuar aguardando”. Only `ConfirmPaymentCancel` calls `abortPayment()`.

On non-pending `UIState.Success`, move to `Result` instead of Orders. `PaymentResultScreen` shows:

```kotlin
PaymentResultScreen(
    total = local.paymentReview?.amountFinal ?: amount,
    paymentMethod = local.selectedPaymentMethod?.name.orEmpty(),
    installments = local.paymentReview?.installments ?: 1,
    transactionId = paymentData?.transactionId,
    onDone = { onAction(OrderFlowAction.FinishInPagePayment) },
)
```

Use `PaymentData.transactionId`; omit the row when it is null or blank.

- [ ] **Step 4: Run reducer and presentation tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/test/java/com/detrapay/ui/home/orders/OrderFlowReducerTest.kt app/src/main/java/com/detrapay/ui/home/orders
git commit -m "feat: add safe payment cancellation and result"
```

### Task 5: Aplicar acessibilidade, contraste e acabamento

**Files:**
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowColors.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/InstallmentSimulatorScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/InstallmentsScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/WaitingScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/PaymentResultScreen.kt`
- Create: `app/src/androidTest/java/com/detrapay/ui/home/orders/OrderFlowAccessibilityTest.kt`

**Interfaces:**
- Selected rows expose role and selected state.
- Loading/error/success expose live-region semantics.
- All informational text uses AA-compliant tokens.

- [ ] **Step 1: Add failing semantic assertions**

Add Compose test tags and assertions:

```kotlin
composeRule
    .onNodeWithTag("installment_2")
    .assertIsSelectable()
    .assertIsSelected()

composeRule
    .onNodeWithTag("payment_result")
    .assertTextContains("Pagamento aprovado")
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.detrapay.ui.home.orders.OrderFlowAccessibilityTest
```

Expected: FAIL because the selection and result semantics/tags do not exist.

- [ ] **Step 3: Implement semantics and contrast**

Replace `Faint` text usage with an AA token such as `Muted #475569` where text is 12–14 sp. Darken WhatsApp button background or use dark content with verified ratio. Apply:

```kotlin
Modifier.selectable(
    selected = isSelected,
    role = Role.RadioButton,
    onClick = onClick,
).semantics {
    stateDescription = if (isSelected) "Selecionado" else "Não selecionado"
}
```

Use `liveRegion = LiveRegionMode.Polite` for loading and result, `Assertive` for errors. Give the QR Code a functional description. Remove the decorative infinite pulse from Waiting.

Normalize Portuguese accents and sentence case in touched files.

- [ ] **Step 4: Run UI tests and inspect previews**

Run the command from Step 2 and expect PASS. Render Compose previews for method, simulator, waiting and result, then visually inspect every image.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/detrapay/ui/home/orders app/src/androidTest/java/com/detrapay/ui/home/orders/OrderFlowAccessibilityTest.kt
git commit -m "fix: improve payment flow accessibility"
```

### Task 6: Full verification and SmartPOS deployment

**Files:**
- Verify all modified files
- No production behavior added in this task

- [ ] **Step 1: Run all unit tests**

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: BUILD SUCCESSFUL with zero failed tests.

- [ ] **Step 2: Run lint and assemble**

```powershell
.\gradlew.bat lintDebug assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run applicable device tests**

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Expected: all tests pass. If the command fails because the SmartPOS lacks a capability required by a golden renderer, record the exact failing class and exception, exclude only that class with the instrumentation runner arguments, and rerun every remaining test.

- [ ] **Step 4: Install, open and capture current logs**

```powershell
adb logcat -c
.\gradlew.bat installDebug
adb shell am start -n com.detrapay/.ui.splash.SplashActivity
adb logcat -d | Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"
```

Expected: install succeeds, app opens, and no `FATAL EXCEPTION` appears.

- [ ] **Step 5: Inspect the final flow**

Use Android CLI/device inspection to capture and immediately view:

- Compact orders list.
- Neutral method selection.
- Numeric amount input.
- Simulator with keyboard hidden and isolated semantics.
- Installments and review values.
- Cancellation confirmation.
- Approved result.

- [ ] **Step 6: Final commit if verification required adjustments**

Stage only files changed by those adjustments and commit with a focused message. Do not stage `PRODUCT.md`, `.impeccable/` or unrelated workspace files unless explicitly intended.
