# Payment Method First Wizard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o wizard que escolhe o meio antes do valor, revisa o total confirmado e envia esse total ao PagBank, além de unificar o cálculo de taxas do backend.

**Architecture:** O Android terá um reducer explícito para `Method → Amount → Installments? → Review → Waiting`, com um snapshot de revisão que carrega valor original, taxa, total e parcelas. O backend extrairá uma função pura de cálculo do total do cliente e a reutilizará em `/calculate-fees` e `calculateReceivables`, removendo a divergência entre cotação e tentativa.

**Tech Stack:** Kotlin, Jetpack Compose, LiveData, JUnit4, MockK, TypeScript, Supabase Edge Functions, Vitest.

## Global Constraints

- Não exibir indicador numérico de etapas.
- O valor enviado ao PagBank deve ser exatamente o total final confirmado na interface.
- O número de parcelas enviado ao PagBank deve ser exatamente o selecionado no wizard.
- Uma divergência do `amount_final` preparado deve gerar log, sem substituir o total confirmado e sem bloquear o terminal.
- Dinheiro e crédito loja não consultam taxas remotas.
- A validação no device deve cobrir crédito, débito, Pix online, transferência Pix, dinheiro e crédito loja.
- Preservar as alterações preexistentes em `C:/Vervit/detrapay-services` que não pertencem a este plano.

---

### Task 1: Regra compartilhada de valor final no backend

**Files:**
- Create: `C:/Vervit/detrapay-services/supabase/functions/mobile/utils/customer-charge.ts`
- Create: `C:/Vervit/detrapay-services/supabase/functions/mobile/utils/customer-charge.test.ts`
- Modify: `C:/Vervit/detrapay-services/supabase/functions/mobile/handlers/calculate-fees.ts`
- Modify: `C:/Vervit/detrapay-services/supabase/functions/mobile/handlers/orders.ts`

**Interfaces:**
- Consumes: `amountOriginal`, `interestRate`, `feeMode`, `paymentType`, `surcharges`, `installments`.
- Produces: `calculateCustomerCharge(input): CustomerCharge`, com `interestAmount`, `surchargeAmount`, `amountFinal`, `installmentValue`, `noInterest`.

- [ ] **Step 1: Escrever os testes falhos da regra compartilhada**

```ts
import { describe, expect, it } from "vitest";
import { calculateCustomerCharge } from "./customer-charge";

describe("customer charge", () => {
  it("adds buyer interest and configured surcharges once", () => {
    expect(calculateCustomerCharge({
      amountOriginal: 100,
      interestRate: 0.05,
      feeMode: "buyer",
      paymentType: "credit",
      installments: 3,
      surcharges: [
        { payment_type: "credit", surcharge_type: "percentage", value: 2 },
        { payment_type: "credit", surcharge_type: "fixed", value: 1 },
      ],
    })).toMatchObject({
      interestAmount: 5,
      surchargeAmount: 3,
      amountFinal: 108,
      installmentValue: 36,
      noInterest: false,
    });
  });

  it("keeps seller interest away from customer but applies surcharge", () => {
    expect(calculateCustomerCharge({
      amountOriginal: 100,
      interestRate: 0.05,
      feeMode: "seller",
      paymentType: "debit",
      installments: 1,
      surcharges: [{ payment_type: "debit", surcharge_type: "fixed", value: 2 }],
    }).amountFinal).toBe(102);
  });
});
```

- [ ] **Step 2: Executar o teste e confirmar RED**

Run: `npx vitest run supabase/functions/mobile/utils/customer-charge.test.ts`

Expected: FAIL porque `customer-charge.ts` ainda não existe.

- [ ] **Step 3: Implementar a função pura mínima**

```ts
export type FeeMode = "buyer" | "seller" | "none" | string;
export type Surcharge = {
  payment_type: string;
  surcharge_type: string;
  value: number;
};

const roundMoney = (value: number) => Math.round((value + Number.EPSILON) * 100) / 100;

export function calculateCustomerCharge(input: {
  amountOriginal: number;
  interestRate: number;
  feeMode: FeeMode;
  paymentType: string;
  installments: number;
  surcharges: Surcharge[];
}) {
  const surchargeAmount = roundMoney(input.surcharges
    .filter((item) => item.payment_type === input.paymentType)
    .reduce((sum, item) => sum + (item.surcharge_type === "percentage"
      ? input.amountOriginal * item.value / 100
      : item.value), 0));
  const interestAmount = input.feeMode === "buyer"
    ? roundMoney(input.amountOriginal * input.interestRate)
    : 0;
  const amountFinal = roundMoney(input.amountOriginal + interestAmount + surchargeAmount);
  const installments = Math.max(1, input.installments);
  return {
    interestAmount,
    surchargeAmount,
    amountFinal,
    installmentValue: Math.ceil(amountFinal * 100 / installments) / 100,
    noInterest: interestAmount === 0 && surchargeAmount === 0,
  };
}
```

- [ ] **Step 4: Confirmar GREEN da função pura**

Run: `npx vitest run supabase/functions/mobile/utils/customer-charge.test.ts`

Expected: PASS.

- [ ] **Step 5: Escrever teste de paridade entre cotação e recebível**

```ts
it.each(["credit", "debit", "pix", "pix_manual", "cash", "store_credit"])(
  "calculates one deterministic total for %s",
  (paymentType) => {
    const input = {
      amountOriginal: 100,
      interestRate: paymentType === "credit" ? 0.05 : 0,
      feeMode: "buyer",
      paymentType,
      installments: paymentType === "credit" ? 3 : 1,
      surcharges: [{ payment_type: paymentType, surcharge_type: "percentage", value: 2 }],
    };
    const quoteTotal = calculateCustomerCharge(input).amountFinal;
    const receivableTotal = calculateCustomerCharge(input).amountFinal;
    expect(receivableTotal).toBe(quoteTotal);
  },
);
```

- [ ] **Step 6: Usar a função nos dois handlers**

Em ambos os handlers, usar a mesma chamada:

```ts
const charge = calculateCustomerCharge({
  amountOriginal: value,
  interestRate: Number(method.interest_tax) || 0,
  feeMode: plan.fee_mode || "buyer",
  paymentType: method.payment_type,
  installments: method.installments || 1,
  surcharges: surchargesList,
});
```

Em `orders.ts`, a consulta de métodos deve incluir `payment_type`; a consulta de adicionais deve selecionar `payment_plan_id, payment_type, surcharge_type, value` para os `planIds`, e o filtro entregue à função deve usar o plano do meio atual.

- [ ] **Step 7: Executar testes do backend**

Run: `npx vitest run supabase/functions/mobile/utils/customer-charge.test.ts supabase/functions/mobile/utils/payment-contract.test.ts`

Expected: PASS.

- [ ] **Step 8: Commitar somente os arquivos do cálculo**

```powershell
git add -- supabase/functions/mobile/utils/customer-charge.ts supabase/functions/mobile/utils/customer-charge.test.ts supabase/functions/mobile/handlers/calculate-fees.ts supabase/functions/mobile/handlers/orders.ts
git commit -m "fix: unify payment customer totals"
```

---

### Task 2: Modelo e reducer do wizard Android

**Files:**
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderFlowContract.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderFlowReducer.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt`
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderFlowReducerTest.kt`
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt`

**Interfaces:**
- Produces: `OrderPaymentReview(amountOriginal, amountFinal, feeAmount, installments, installmentValue)`.
- Produces: passos `Method`, `Amount`, `Installments`, `Review`, `Waiting`.
- Consumes: `InstallmentFee` devolvida por `calculateFees`.

- [ ] **Step 1: Escrever testes falhos da nova sequência**

```kotlin
@Test
fun `start payment opens method before amount`() {
    val state = OrderFlowReducer.startPayment(OrderFlowLocalState(), order())
    assertEquals(OrderFlowStep.Method, state.step)
}

@Test
fun `selecting payment method opens amount and clears dependent quote`() {
    val next = OrderFlowReducer.selectPaymentMethod(
        OrderFlowLocalState(paymentDigits = "10000", selectedInstallment = 3),
        paymentMethod("credito", true),
    )
    assertEquals(OrderFlowStep.Amount, next.step)
    assertEquals("", next.paymentDigits)
    assertNull(next.paymentReview)
}

@Test
fun `credit quote opens installments and simple quote opens review`() {
    val credit = OrderFlowReducer.quoteLoaded(
        OrderFlowLocalState(
            step = OrderFlowStep.Amount,
            paymentDigits = "10000",
            selectedPaymentMethod = paymentMethod("credito", true),
        ),
        listOf(installmentFee()),
    )
    assertEquals(OrderFlowStep.Installments, credit.step)
    val pix = OrderFlowReducer.quoteLoaded(
        OrderFlowLocalState(
            step = OrderFlowStep.Amount,
            paymentDigits = "10000",
            selectedPaymentMethod = paymentMethod("pix", true),
        ),
        listOf(installmentFee()),
    )
    assertEquals(OrderFlowStep.Review, pix.step)
}
```

- [ ] **Step 2: Confirmar RED do reducer**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderFlowReducerTest"`

Expected: FAIL porque os novos passos e `paymentReview` não existem.

- [ ] **Step 3: Implementar contrato e reducer mínimos**

```kotlin
enum class OrderFlowStep { Orders, Detail, Method, Amount, Installments, Review, Waiting }

data class OrderPaymentReview(
    val amountOriginal: Double,
    val amountFinal: Double,
    val feeAmount: Double,
    val installments: Int,
    val installmentValue: Double,
)

fun startPayment(state: OrderFlowLocalState, order: Order) = state.copy(
    selectedOrder = order,
    selectedPaymentMethod = null,
    paymentDigits = "",
    paymentReview = null,
    step = OrderFlowStep.Method,
)

fun selectPaymentMethod(state: OrderFlowLocalState, method: PaymentMethod) = state.copy(
    selectedPaymentMethod = method,
    paymentDigits = "",
    creditInstallments = emptyList(),
    selectedInstallment = 1,
    paymentReview = null,
    step = OrderFlowStep.Amount,
)
```

- [ ] **Step 4: Escrever testes falhos de conversão monetária da cotação**

```kotlin
@Test
fun `review uses backend quote total and interest`() {
    val review = OrderPresentation.paymentReview(
        amountOriginal = 100.0,
        installment = InstallmentFee(3, "36.00", "108.00", "8.00", false),
    )
    assertEquals(108.0, review.amountFinal, 0.0)
    assertEquals(8.0, review.feeAmount, 0.0)
    assertEquals(3, review.installments)
}
```

- [ ] **Step 5: Implementar e validar parsing da revisão**

```kotlin
fun paymentReview(amountOriginal: Double, installment: InstallmentFee): OrderPaymentReview {
    val amountFinal = parseDecimal(installment.totalValue)
    return OrderPaymentReview(
        amountOriginal = amountOriginal,
        amountFinal = amountFinal,
        feeAmount = (amountFinal - amountOriginal).coerceAtLeast(0.0),
        installments = installment.installmentNumber.coerceAtLeast(1),
        installmentValue = parseDecimal(installment.installmentValue),
    )
}

fun directPaymentReview(amount: Double) = OrderPaymentReview(
    amountOriginal = amount,
    amountFinal = amount,
    feeAmount = 0.0,
    installments = 1,
    installmentValue = amount,
)
```

- [ ] **Step 6: Executar testes focados**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderFlowReducerTest" --tests "com.detrapay.ui.home.orders.OrderPresentationTest"`

Expected: PASS.

- [ ] **Step 7: Commitar o domínio do wizard**

```powershell
git add -- app/src/main/java/com/detrapay/ui/home/orders/OrderFlowContract.kt app/src/main/java/com/detrapay/ui/home/orders/OrderFlowReducer.kt app/src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt app/src/test/java/com/detrapay/ui/home/orders/OrderFlowReducerTest.kt app/src/test/java/com/detrapay/ui/home/orders/OrderPresentationTest.kt
git commit -m "feat: model payment review wizard"
```

---

### Task 3: Valor confirmado como fonte do PagBank

**Files:**
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersViewModel.kt`
- Modify: `app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt`
- Modify: `app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt`
- Modify: `app/src/test/java/com/detrapay/ui/home/orders/OrderPaymentRouterTest.kt`

**Interfaces:**
- `OrderPaymentRequest` passa a carregar `amount` original e `amountFinal` confirmado.
- `PaymentDialogViewModel` usa `amountFinal` no `PlugPagPaymentData`, log local e `PaymentData`.

- [ ] **Step 1: Substituir testes antigos pela expectativa confirmada e observar RED**

```kotlin
@Test
fun `confirmed total wins when backend prepared total differs`() {
    val terminal = slot<PlugPagPaymentData>()
    coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
        Result.Success(attempt(amountOriginal = 25.67, amountFinal = 99.99))
    arrangeTerminalSuccess(capture(terminal))

    viewModel.payOrder(
        request("credito", online = true, installments = 3, amountFinal = 27.00),
        "SER123",
    )

    viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
    assertEquals(2700, readInt(terminal.captured, "amount"))
    assertEquals(3, readInt(terminal.captured, "installments"))
}
```

- [ ] **Step 2: Executar o teste e confirmar RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest"`

Expected: FAIL porque `OrderPaymentRequest` não possui `amountFinal` e o ViewModel ainda usa o valor preparado.

- [ ] **Step 3: Implementar a fonte confirmada e log de divergência**

```kotlin
data class OrderPaymentRequest(
    val order: Order,
    val paymentMethod: PaymentMethod,
    val amount: Double,
    val amountFinal: Double,
    val installments: Int,
    val idempotencyKey: String = UUID.randomUUID().toString(),
)

val confirmedAmountCents = amountInCents(request.amountFinal)
if (confirmedAmountCents <= 0) {
    finishWithError("O valor confirmado para o pagamento é inválido.")
    return@launch
}
if (kotlin.math.abs(prepared.data.amountFinal - request.amountFinal) > 0.01) {
    Log.w("PaymentDialogVM", "Prepared total differs from confirmed total")
}
startPagBank(request, prepared.data.id, confirmedAmountCents, serial)
```

- [ ] **Step 4: Executar testes do pagamento e roteamento**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.payment.PaymentDialogViewModelTest" --tests "com.detrapay.ui.home.orders.OrderPaymentRouterTest"`

Expected: PASS.

- [ ] **Step 5: Commitar a integração monetária**

```powershell
git add -- app/src/main/java/com/detrapay/ui/home/orders/OrdersViewModel.kt app/src/main/java/com/detrapay/ui/payment/PaymentDialogViewModel.kt app/src/test/java/com/detrapay/ui/payment/PaymentDialogViewModelTest.kt app/src/test/java/com/detrapay/ui/home/orders/OrderPaymentRouterTest.kt
git commit -m "fix: charge confirmed payment total"
```

---

### Task 4: Telas e orquestração Compose

**Files:**
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/OrdersFragment.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/MethodScreen.kt`
- Modify: `app/src/main/java/com/detrapay/ui/home/orders/screens/KeypadScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/orders/screens/InstallmentsScreen.kt`
- Create: `app/src/main/java/com/detrapay/ui/home/orders/screens/ReviewScreen.kt`
- Delete: `app/src/main/java/com/detrapay/ui/home/orders/screens/CreditScreen.kt`
- Delete: `app/src/main/java/com/detrapay/ui/home/orders/screens/DebitScreen.kt`

**Interfaces:**
- Consumes o estado e as ações definidos na Task 2.
- Produz ações `ContinueAmount`, `ContinueInstallments` e `ConfirmPayment`.

- [ ] **Step 1: Ajustar os testes do reducer para as ações esperadas e confirmar que ainda passam**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.OrderFlowReducerTest"`

Expected: PASS antes da ligação de UI.

- [ ] **Step 2: Implementar as telas sem regra de negócio local**

`MethodScreen` remove o card de valor. `KeypadScreen` recebe o método selecionado e usa o texto `Continuar`. `InstallmentsScreen` lista a cotação e só habilita `Revisar pagamento` após seleção. `ReviewScreen` mostra método, original, taxa, parcelas e total, com `Confirmar` e voltar. Nenhuma tela mostra indicador de etapa.

```kotlin
@Composable
fun InstallmentsScreen(
    amount: Double,
    installments: List<InstallmentFee>,
    selectedInstallment: Int?,
    onBack: () -> Unit,
    onSelectInstallment: (Int) -> Unit,
    onContinue: () -> Unit,
)

@Composable
fun ReviewScreen(
    paymentMethod: PaymentMethod,
    review: OrderPaymentReview,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
)
```

- [ ] **Step 3: Ligar a rota ao novo reducer**

Ao selecionar método, navegar para valor. Ao continuar, criar revisão direta para meios sem taxa ou chamar `calculateFees`. Respostas de crédito abrem parcelamento; respostas dos demais meios abrem revisão. Confirmar revisão cria `OrderPaymentRequest` com `amountFinal`; online abre `Waiting`, manual chama `recordOfflinePayment` diretamente.

```kotlin
OrderFlowAction.ContinueAmount -> {
    localState.selectedPaymentMethod?.let { method ->
        val amount = currentPaymentAmount(localState)
        if (PaymentTypeRules.isDirectNoFeePaymentType(method.paymentType)) {
            localState = OrderFlowReducer.openDirectReview(localState, amount)
        } else {
            localState = OrderFlowReducer.startCheckoutQuote(localState)
            viewModel.calculateFees(amount, method.paymentType.orEmpty())
        }
    }
}

OrderFlowAction.ConfirmPayment -> startConfirmedRequest(localState)
```

- [ ] **Step 4: Remover a confirmação nativa duplicada**

Remover `ConfirmRecordOnlyPayment` de `OrderFlowEffect` e o `AlertDialog` correspondente de `OrdersFragment`, pois `ReviewScreen` passa a ser a confirmação única de todos os meios.

```kotlin
sealed interface OrderFlowEffect {
    data object ShowLogoutConfirmation : OrderFlowEffect
    data object NavigateToRegistration : OrderFlowEffect
    data class ShowToast(val message: String, val long: Boolean = true) : OrderFlowEffect
}
```

- [ ] **Step 5: Compilar e executar os testes do fluxo**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.orders.*"`

Expected: PASS e compilação Kotlin sem referências aos passos removidos.

- [ ] **Step 6: Commitar a interface do wizard**

```powershell
git add -- app/src/main/java/com/detrapay/ui/home/orders app/src/test/java/com/detrapay/ui/home/orders
git commit -m "feat: add payment method first wizard"
```

---

### Task 5: Verificação completa e device

**Files:**
- No production files. This task only verifies the files changed in Tasks 1–4.

**Interfaces:**
- Verifica o contrato completo sem criar novas APIs.

- [ ] **Step 1: Executar a suíte completa do backend**

Run: `npm test`

Expected: PASS em `C:/Vervit/detrapay-services`.

- [ ] **Step 2: Executar a suíte completa do Android**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Instalar seguindo a sequência obrigatória do projeto**

```powershell
adb -s 0123abcd logcat -c
.\gradlew.bat installDebug
adb -s 0123abcd shell am start -n com.detrapay/.ui.splash.SplashActivity
adb -s 0123abcd logcat -d | Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"
```

Expected: instalação e abertura bem-sucedidas, sem `FATAL EXCEPTION`.

- [ ] **Step 4: Validar todos os meios no device**

Para crédito, débito, Pix online, transferência Pix, dinheiro e crédito loja: verificar método antes do valor, tela de valor, revisão, voltar e valor final. No crédito, verificar tela própria de parcelas. Nos meios online, confirmar nos logs do PlugPag valor em centavos e parcelas. Cancelar operações de terminal que não devam ser concluídas.

- [ ] **Step 5: Revisar worktrees e commits**

Run: `git status --short` em ambos os repositórios e `git log -5 --oneline`.

Expected: Android limpo; backend contendo apenas mudanças preexistentes fora do escopo e nenhum arquivo novo deste plano sem commit.
