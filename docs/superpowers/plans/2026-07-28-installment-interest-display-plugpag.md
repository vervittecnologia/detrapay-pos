# Installment Interest Display and PlugPag Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show original, interest-bearing total, and interest-bearing installment values everywhere and charge that exact total through PlugPag with seller installments.

**Architecture:** Add one pure shared installment presenter fed by the original amount and backend `InstallmentFee`. Compose and legacy views consume its labels, while `PaymentDialogViewModel` enforces equality between the displayed quote and the prepared backend attempt before building `PlugPagPaymentData`.

**Tech Stack:** Kotlin, Android Views, Jetpack Compose, PagBank PlugPag SDK, JUnit 4, MockK, Gradle, ADB.

## Global Constraints

- Use backend quote values; do not recalculate interest in the app.
- Display original value, total with interest, and installment value with interest on every installment surface.
- Use `PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR` for two or more installments.
- Block PlugPag if the prepared backend total differs in cents from the displayed total.
- After installation, open the app and inspect filtered logcat.

---

## File Structure

- `app/src/main/java/com/detrapay/ui/util/InstallmentQuotePresenter.kt`: parse and format one backend quote for every UI.
- `app/src/test/java/com/detrapay/ui/util/InstallmentQuotePresenterTest.kt`: pure tests for interest/no-interest labels.
- `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`: use the shared parsed quote in payment review.
- `app/src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt`: render checkout cards and shared text.
- `app/src/main/java/com/detrapay/ui/home/orders/screens/InstallmentSimulatorScreen.kt`: render simulator cards.
- `app/src/main/java/com/detrapay/ui/home/orders/screens/ReviewScreen.kt`: use explicit original/interest labels.
- `app/src/main/java/com/detrapay/ui/registration/payment_method/InstallmentsAdapter.kt`: render the shared three-line presentation.
- `app/src/main/java/com/detrapay/ui/registration/payment_method/RegistrationPaymentConfigBottomSheet.kt`: pass the entered original value.
- `app/src/main/java/com/detrapay/ui/registration/payment_method/RegistrationPaymentDetailFragment.kt`: pass the entered original value.
- `app/src/main/java/com/detrapay/ui/order_details/OrderDetailsPaymentConfigBottomSheet.kt`: pass the entered original value.
- `app/src/main/java/com/detrapay/ui/registration/payment_method/InstallmentsBottomSheet.kt`: require and pass the original value.
- `app/src/main/java/com/detrapay/ui/order_details/OrderDetailsPaymentsRecyclerViewAdapter.kt`: show final installment and total for stored card payments.
- `app/src/main/java/com/detrapay/ui/payment/PaymentDialogFragment.kt`: show the three values while processing.
- `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`: validate the prepared total and populate PlugPag.
- `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`: capture and verify PlugPag data and mismatch rejection.

### Task 1: Shared installment presentation

**Files:**
- Create: `app/src/main/java/com/detrapay/ui/util/InstallmentQuotePresenter.kt`
- Create: `app/src/test/java/com/detrapay/ui/util/InstallmentQuotePresenterTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`

**Interfaces:**
- Consumes: `InstallmentQuotePresenter.present(amountOriginal: Double, quote: InstallmentFee)`.
- Produces: `InstallmentQuotePresentation(originalValue, totalValue, installmentValue, originalLabel, totalLabel, installmentLabel)` and an overload for persisted transaction values.

- [ ] **Step 1: Write failing presenter tests**

```kotlin
@Test fun `interest quote shows original total and installment with interest`() {
    val result = InstallmentQuotePresenter.present(100.0, quote("36.00", "108.00", false))
    assertEquals("Valor original: R$ 100,00", result.originalLabel)
    assertEquals("Total com juros: R$ 108,00", result.totalLabel)
    assertEquals("3x de R$ 36,00 com juros", result.installmentLabel)
}

@Test fun `no interest quote is labeled without interest`() {
    val result = InstallmentQuotePresenter.present(100.0, quote("50,00", "100,00", true, installments = 2))
    assertEquals("Total: R$ 100,00", result.totalLabel)
    assertEquals("2x de R$ 50,00 sem juros", result.installmentLabel)
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `./gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.util.InstallmentQuotePresenterTest"`

Expected: compilation fails because the presenter does not exist.

- [ ] **Step 3: Implement the pure presenter and use its numeric values in `paymentReview`**

```kotlin
data class InstallmentQuotePresentation(
    val originalValue: Double,
    val totalValue: Double,
    val installmentValue: Double,
    val originalLabel: String,
    val totalLabel: String,
    val installmentLabel: String,
)

object InstallmentQuotePresenter {
    fun present(amountOriginal: Double, quote: InstallmentFee): InstallmentQuotePresentation {
        val total = parseMoney(quote.totalValue)
        val installment = parseMoney(quote.installmentValue)
        return InstallmentQuotePresentation(
            amountOriginal, total, installment,
            "Valor original: ${currency(amountOriginal)}",
            if (quote.noInterest) "Total: ${currency(total)}" else "Total com juros: ${currency(total)}",
            "${quote.installmentNumber}x de ${currency(installment)} ${if (quote.noInterest) "sem juros" else "com juros"}",
        )
    }

    fun present(amountOriginal: Double, amountFinal: Double, installments: Int) = build(
        amountOriginal = amountOriginal,
        total = amountFinal,
        installment = amountFinal / installments.coerceAtLeast(1),
        installments = installments.coerceAtLeast(1),
        noInterest = amountFinal <= amountOriginal,
    )
}
```

- [ ] **Step 4: Run presenter and order presentation tests and verify GREEN**

Run: `./gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.util.InstallmentQuotePresenterTest" --tests "com.detrapay.ui.home.orders.OrderPresentationTest"`

Expected: PASS.

### Task 2: Apply the shared labels to every installment surface

**Files:**
- Modify all UI files listed in the File Structure section except the ViewModel and its test.

**Interfaces:**
- Consumes: `InstallmentQuotePresenter.present(amountOriginal, quote)`.
- Produces: all installment cards, summaries, share text, processing screens, and stored card rows showing the same three values.

- [ ] **Step 1: Add failing shared-text assertions**

```kotlin
@Test fun `share text includes original total and installments with interest`() {
    val text = simulatorShareText(100.0, listOf(quote("36.00", "108.00", false)))
    assertTrue(text.contains("Valor original: R$ 100,00"))
    assertTrue(text.contains("Total com juros: R$ 108,00"))
    assertTrue(text.contains("3x de R$ 36,00 com juros"))
}
```

- [ ] **Step 2: Run the presentation test and verify RED**

Run: `./gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest"`

Expected: FAIL because the current shared text omits the required labels.

- [ ] **Step 3: Replace per-screen calculations and labels with the shared presenter**

For checkout and simulator rows, use the original input plus the quote and render all labels:

```kotlin
val presentation = InstallmentQuotePresenter.present(amount, installment)
Text(presentation.installmentLabel)
Text(presentation.originalLabel)
Text(presentation.totalLabel)
```

Build each shared-text entry from the same labels:

```kotlin
val presentation = InstallmentQuotePresenter.present(amount, installment)
listOf(presentation.originalLabel, presentation.installmentLabel, presentation.totalLabel)
    .joinToString("\n")
```

Change `InstallmentsAdapter.submitList` and render the same labels in its two available text fields:

```kotlin
fun submitList(newItems: List<InstallmentFee>, amountOriginal: Double)

val presentation = InstallmentQuotePresenter.present(amountOriginal, item)
binding.tvInstallmentName.text = presentation.installmentLabel
binding.tvInstallmentDescription.text =
    "${presentation.originalLabel}\n${presentation.totalLabel}"
```

Pass `Mask.doubleValue(binding.etPaymentValue.text.toString())` from both configuration bottom sheets, `Mask.doubleValue(binding.etCardValue.text.toString())` from `RegistrationPaymentDetailFragment`, and a new `amountOriginal: Double` constructor argument from `InstallmentsBottomSheet`.

For `ReviewScreen`, keep separate summary rows and rename them from the shared semantics:

```kotlin
SummaryRow("Valor original", OrderPresentation.formatCurrency(review.amountOriginal))
SummaryRow("Parcelamento", "${review.installments}x de ${OrderPresentation.formatCurrency(review.installmentValue)} com juros")
SummaryRow("Total com juros", OrderPresentation.formatCurrency(review.amountFinal), strong = true)
```

For persisted receivables in `PaymentDialogFragment` and `OrderDetailsPaymentsRecyclerViewAdapter`, use the overload backed by `amountOriginal`, `amountFinal`, and `installments`:

```kotlin
val presentation = InstallmentQuotePresenter.present(
    item.amountOriginal,
    item.amountFinal,
    item.installments,
)
```

Render `originalLabel`, `installmentLabel`, and `totalLabel` in the existing amount/detail text slots without changing payment behavior.

- [ ] **Step 4: Run focused presentation tests and compile debug sources**

Run: `./gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderPresentationTest" --tests "com.detrapay.ui.util.InstallmentQuotePresenterTest"`

Expected: PASS and production sources compile.

### Task 3: Enforce the displayed total at the PlugPag boundary

**Files:**
- Modify: `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`

**Interfaces:**
- Consumes: `OrderPaymentRequest.amountFinal`, prepared `PaymentAttempt.amountFinal`, and selected installments.
- Produces: `PlugPagPaymentData` with confirmed cents, selected installments, and seller installment type.

- [ ] **Step 1: Replace permissive mismatch tests with failing rejection tests**

```kotlin
@Test fun `prepared total mismatch never opens PlugPag`() {
    coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
        Result.Success(attempt(amountFinal = 99.99))
    viewModel.payOrder(request("credito", true, 3, amountFinal = 108.00), "SER123")
    val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
    assertTrue(state.message.orEmpty().contains("total diferente"))
    verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
}
```

- [ ] **Step 2: Run the ViewModel tests and verify RED**

Run: `./gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest"`

Expected: mismatch tests fail because PlugPag is currently opened.

- [ ] **Step 3: Validate prepared total in cents before terminal setup**

```kotlin
val preparedAmount = prepared.data.amountFinal
if (!preparedAmount.isFinite() || amountInCents(preparedAmount) != confirmedAmountCents) {
    finishWithError("O backend preparou um total diferente do exibido. Atualize a configuracao antes de tentar novamente.")
    return@launch
}
startPagBank(request, prepared.data.id, confirmedAmountCents, serial)
```

Keep `PlugPagPaymentData(paymentType(request), confirmedAmountCents, installmentType(request.installments), request.installments, ...)` and verify the captured fields.

- [ ] **Step 4: Run the ViewModel tests and verify GREEN**

Run: `./gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest"`

Expected: PASS.

### Task 4: Full verification and device deployment

**Files:** Verify only.

**Interfaces:** Produces test and device evidence.

- [ ] **Step 1: Run focused and complete unit tests**

Run: `./gradlew.bat testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Check the patch**

Run: `git diff --check` and `git status --short`.

Expected: no whitespace errors and only scoped changes.

- [ ] **Step 3: Clear logs, install, open, and inspect logs**

Run in order:

```powershell
adb logcat -c
./gradlew.bat installDebug
adb shell am start -n com.detrapay/.ui.splash.SplashActivity
adb logcat -d | Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"
```

Expected: install and launch succeed with no fatal exception.
