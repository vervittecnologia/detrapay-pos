# Home Direct Checkout Compose Design

## Goal

Migrate the first architecture slice toward Compose by organizing the Home direct checkout flow as a Compose-first surface while keeping `HomeActivity`, `NavHostFragment`, and `home_navigation.xml` as temporary hosts.

This slice covers only the Home shell behavior needed to reach direct checkout and the direct checkout mode itself. It does not migrate the legacy Home tabs (`Registro`, `Meus pedidos`, `Historico`, `Perfil`), registration flow, payment details flow, reports, or login.

## Scope

In scope:

- Keep `HomeActivity` as the Android host for the first migration slice.
- Keep `DirectCheckoutFragment` as a thin Android integration boundary.
- Preserve the current `home_navigation.xml` destination for `directCheckoutFragment`.
- Move the direct checkout UI into smaller Compose contracts, route, screens, and components.
- Preserve the existing visual behavior of direct checkout unless a change is required to split responsibilities safely.
- Keep `PaymentDialogFragment`, `SessionExpiredDialog`, Android share sheet, clipboard, logout confirmation, and navigation to registration behind the fragment/Android boundary.
- Add tests around the new state/action transition layer.
- Preserve existing `DirectCheckoutOrderPresentation` tests and add focused tests only where the migration introduces new logic.

Out of scope:

- Full single-activity migration.
- Navigation Compose adoption for the whole app.
- Migrating legacy Home tabs to Compose.
- Migrating registration/payment/details/report/login flows.
- Replacing the PagBank/payment dialog integration.
- Backend contract workarounds.

## Current State

The project already has Compose enabled in `app/build.gradle.kts` and has an existing direct checkout Compose screen. The current implementation is hybrid:

- `HomeActivity` is an `AppCompatActivity` using ViewBinding and `NavHostFragment`.
- `home_navigation.xml` declares both fragment and activity destinations.
- `DirectCheckoutFragment` creates a `ComposeView`, owns direct checkout UI state with `mutableStateOf`, observes `HomeViewModel` and `SimplifiedReceivableListViewModel`, and directly handles Android-only effects.
- `DirectCheckoutFlowScreen.kt` contains the full direct checkout UI, including multiple screens, shared widgets, formatting helpers, colors, and simulator UI.

This works, but the fragment and the giant Compose file are doing too much. The next migration should make the Compose surface easier to test and easier to move later into a pure Compose host.

## Architecture

Use a progressive migration architecture:

- `HomeActivity` remains the host for now.
- `DirectCheckoutFragment` remains the nav destination and Android integration boundary.
- `DirectCheckoutRoute` connects lifecycle-aware ViewModel observations to Compose state and emits Android effects back to the fragment.
- `DirectCheckoutScreen` renders a stateless Compose UI from `DirectCheckoutUiState`.
- Screen-specific composables live under a direct checkout package split by responsibility.
- Small reusable components live under a direct checkout components package.
- Pure formatting and calculation remains in `DirectCheckoutOrderPresentation`.

The target dependency direction is:

```text
HomeActivity
  -> home_navigation.xml
  -> DirectCheckoutFragment
    -> DirectCheckoutRoute
      -> DirectCheckoutScreen
        -> screens/*
        -> components/*
      -> DirectCheckoutOrderPresentation
      -> HomeViewModel
      -> SimplifiedReceivableListViewModel
```

`DirectCheckoutFragment` may know about Android framework APIs and dialogs. The composable screens should not know about `Fragment`, `Activity`, `Toast`, `Intent`, `ClipboardManager`, or Android navigation IDs.

## Proposed Files

Create:

- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutContract.kt`
  - Defines `DirectCheckoutUiState`, `DirectCheckoutStep`, `DirectCheckoutAction`, and one-shot Android effects.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutRoute.kt`
  - Observes ViewModels, owns route-level mutable state for this migration slice, maps UI actions to ViewModel calls or effects.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutScreen.kt`
  - Stateless screen shell that chooses which direct checkout screen to render.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducer.kt`
  - Pure transition helpers for direct checkout local state where useful and testable.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/components/*`
  - Shared Compose widgets used by multiple direct checkout screens.
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/*`
  - One composable file per step: orders, detail, keypad, method, credit, debit, waiting, simulator.
- `app/src/test/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducerTest.kt`
  - Tests state transitions introduced by this migration.

Modify:

- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutFragment.kt`
  - Make it thin. It should supply ViewModels to the route and handle emitted Android effects.
- `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreen.kt`
  - Split contents into the new direct checkout package. The old file should be removed or reduced to a temporary compatibility wrapper only if needed for an incremental compile.
- `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreenPreview.kt`
  - Move or update previews to target the new stateless screen/components.
- `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutOrderPresentation.kt`
  - Keep as the pure presentation/calculation module unless package movement is required by the implementation plan.

## Data Flow

The Compose screen receives immutable state and emits actions:

```kotlin
@Composable
fun DirectCheckoutScreen(
    state: DirectCheckoutUiState,
    onAction: (DirectCheckoutAction) -> Unit,
)
```

`DirectCheckoutUiState` contains:

- Company name/document.
- Current order list.
- Loading and error states.
- Current step.
- Selected order.
- Payment amount digits.
- Selected payment type.
- Available payment types.
- Credit installments and loading/error state.
- Selected installment.
- Simulator state.

`DirectCheckoutAction` contains user intents:

- Logout.
- Reload orders.
- Open new order flow.
- Open order detail.
- Start order payment.
- Back.
- Keypad input.
- Open method selection.
- Select payment type.
- Select installment.
- Continue credit/debit.
- Open/close simulator.
- Update/consult simulator amount.
- Select/copy/share simulator installment text.

The route maps actions to either local transition, ViewModel calls, or one-shot effects.

## Android Effects

Android-only behavior remains outside stateless composables:

- `ShowLogoutConfirmation`
- `OpenPaymentDialog`
- `NavigateToRegistration`
- `CopySimulatorText`
- `ShareSimulatorText`
- `ShowToast`
- `ShowSessionExpired`

The fragment handles these effects with existing platform APIs. This preserves current behavior and keeps the Compose UI portable for a later Navigation Compose migration.

## Error Handling

Preserve current behavior:

- `UnauthorizedException` opens `SessionExpiredDialog`.
- Order list errors show a reloadable error/empty state.
- Payment methods and payment mutation errors show existing messages through toast or the existing screen error surfaces.
- Credit installment errors show the credit error state.
- Simulator errors show the simulator error state.
- Terminal-required payments continue through `PaymentDialogFragment`.
- Manual payments continue through the ViewModel confirmation path and reload orders after success.
- Missing backend data should not be masked by client workarounds. If a required field or contract is absent, the UI should surface a coherent error/empty state and the backend demand should be documented explicitly.

## Testing

Unit tests:

- Keep existing `DirectCheckoutOrderPresentationTest`.
- Add `DirectCheckoutReducerTest` for pure local transitions:
  - Orders to detail.
  - Orders to keypad with default missing amount.
  - Keypad to method.
  - Method to credit.
  - Method to debit.
  - Method to waiting for manual/non-terminal methods.
  - Back behavior for each step.
  - Simulator open, close, amount update, and selection.

Compose previews:

- Orders loaded.
- Orders empty.
- Orders error.
- Detail.
- Keypad.
- Method.
- Credit loading.
- Credit error.
- Credit with installments.
- Debit.
- Waiting.
- Simulator empty/loading/error/loaded.

Final Android validation:

- Run the standard project flow only at delivery validation or when runtime Android behavior must be verified:
  - `.\scripts\dev-device.ps1`
- After install, verify the app opens and monitor logcat filtered for `com.detrapay`, `AndroidRuntime`, and `FATAL EXCEPTION`, following the workspace instructions.

## Migration Rules

- Do not migrate legacy Home tabs in this slice.
- Do not remove `HomeActivity`, `NavHostFragment`, or `home_navigation.xml`.
- Do not introduce Navigation Compose yet.
- Do not change backend contracts.
- Do not change payment SDK/dialog behavior.
- Keep XML resources that are still used by other flows.
- Keep changes scoped to Home direct checkout unless a small supporting test or import update is required.

## Acceptance Criteria

- Direct checkout behavior matches the current flow.
- `DirectCheckoutFragment` is reduced to Android integration responsibilities.
- Direct checkout UI is split into route, contract, stateless screen, screen files, and shared components.
- Local step/action transitions are covered by unit tests.
- Existing direct checkout presentation tests still pass.
- Compose previews exist for the main direct checkout states.
- No unrelated dirty worktree changes are reverted or overwritten.

