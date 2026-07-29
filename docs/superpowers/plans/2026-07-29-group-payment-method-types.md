# Group Payment Method Types Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show one selectable entry per payment type and describe the order amount as pending.

**Architecture:** Add a pure grouping function to `OrderPresentation` so the behavior is deterministic and unit-testable. `MethodScreen` will render those representatives with labels based on normalized `paymentType`; the existing checkout continues resolving the exact credit method after installment selection.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Gradle.

## Global Constraints

- Keep `pix` and `pix_manual` distinct.
- Display `pix_manual` under manual registration.
- Preserve unrelated local changes.
- Run Gradle offline once after the implementation, then install, open, and inspect logs.

---

### Task 1: Group payment methods

**Files:**
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`

**Interfaces:**
- Produces: `fun paymentMethodTypes(paymentMethods: List<PaymentMethod>): List<PaymentMethod>`.
- Consumes: `PaymentTypeRules.normalize`.

- [ ] Add a failing test with duplicated credit installments plus distinct `pix` and `pix_manual`; assert one deterministic representative per normalized type.
- [ ] Run the targeted test and confirm it fails because `paymentMethodTypes` does not exist.
- [ ] Implement grouping by `paymentType ?: name`, choosing the lowest installment count and then lowest id.
- [ ] Run the targeted test and confirm it passes.

### Task 2: Render type labels and pending copy

**Files:**
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/SellerPaymentFlowSourceTest.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/MethodScreen.kt`

**Interfaces:**
- Consumes: `OrderPresentation.paymentMethodTypes`.
- Produces: one card per type with the labels Crédito, Débito, Pix, Transferência Pix, Dinheiro, and Crédito loja.

- [ ] Add failing source-contract assertions for `Valor pendente`, absence of `Saldo disponível`, use of `paymentMethodTypes`, and `Transferência Pix`.
- [ ] Run the targeted test and confirm the current source fails.
- [ ] Group before splitting online/manual sections and render title/subtitle from normalized type.
- [ ] Run both targeted test classes and confirm they pass.

### Task 3: Verify on device

**Files:**
- Verify: changed Kotlin and test files.

**Interfaces:**
- Produces: fresh build, test, and runtime evidence.

- [ ] Run `.\gradlew.bat --offline testDebugUnitTest assembleDebug`.
- [ ] Run the Impeccable detector on `MethodScreen.kt`.
- [ ] Clear logcat, install the APK, open `SplashActivity`, and inspect filtered logs.
- [ ] Navigate to the payment-method screen, inspect its layout and screenshot, and confirm one card per type plus “Valor pendente”.
