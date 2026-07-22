# Task 4 Report: Split Compose Screens And Components

## Status

Completed.

## Implementation

- Replaced the compatibility call from `DirectCheckoutScreen` to
  `DirectCheckoutFlowScreen` with direct dispatch to the split Compose screens.
- Moved shared checkout widgets and formatting helpers to
  `ui/home/direct_checkout/components/DirectCheckoutBlocks.kt`.
- Split the checkout UI into:
  - `OrdersScreen.kt`
  - `DetailScreen.kt`
  - `KeypadScreen.kt`
  - `MethodScreen.kt`
  - `CreditScreen.kt`
  - `DebitScreen.kt`
  - `WaitingScreen.kt`
  - `InstallmentSimulatorScreen.kt`
- Deleted `ui/home/simplified/DirectCheckoutFlowScreen.kt`.
- Preserved all existing helper composables. The pre-existing, unused alternate
  orders implementation is retained as `LegacyOrdersScreen` to avoid a name
  collision with the required `SellerOrdersScreen -> OrdersScreen` rename.
- Kept `displayDate` with shared blocks because it is used by both detail and
  retained legacy order-card code.
- Updated the existing preview only as required for compilation after removing
  the compatibility shell; preview relocation remains outside this task.

## Behavioral Scope

- No route, ViewModel, reducer, payment, or action behavior changed.
- Screen text, layout values, click handlers, and presentation calls were moved
  mechanically.

## Verification

Executed:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --tests "com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentationTest" --no-watch-fs
```

Result: `BUILD SUCCESSFUL` (36 actionable tasks up-to-date).

## Notes

- Two intermediate compilation failures exposed truncated line-range extraction
  during the mechanical move. The missing function terminator and three missing
  `@Composable` annotations were restored, then the required verification
  command completed successfully.
- One transient Windows file lock on
  `LoginRepositoryTest.class` occurred during an earlier verification attempt;
  the final serial rerun passed.

## Reviewer Findings To Fix

Important:
- The split was not text-preserving: strings changed from the removed source's replacement/mojibake text to corrected accented text. Preserve the exact text from the monolithic source for Task 4.
- The task diff added a non-minimal preview file. Exclude preview changes from Task 4; if the file exists locally, keep it untracked for Task 5 rather than tracked in the Task 4 diff.

Minor:
- Split files retain unused imports. Clean if low-risk while fixing the blocking items.

## Review Fix Report

### What changed

- Restored Compose delegate imports removed during low-risk import cleanup (`getValue`/`setValue`) so the split screens compile.
- Verified the pre-split source at `977053e` used the corrected accented strings; kept the split text matching that source rather than the mojibake representation shown in the first review package.
- Removed `DirectCheckoutFlowScreenPreview.kt` from Git tracking while preserving the local file for Task 5.

### Test command/result

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --tests "com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentationTest" --no-watch-fs
```

Result: `BUILD SUCCESSFUL in 3m 17s`.

### Files changed

- `app/src/main/java/com/detrapay/ui/home/direct_checkout/components/DirectCheckoutBlocks.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/CreditScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/DebitScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/DetailScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/InstallmentSimulatorScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/KeypadScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/MethodScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/OrdersScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/screens/WaitingScreen.kt`
- `app/src/main/java/com/detrapay/ui/home/simplified/DirectCheckoutFlowScreenPreview.kt` (untracked for Task 5)
- `.superpowers/sdd/task-4-report.md`

### Concerns

- The first review package displayed pre-split text with mojibake/replacement encoding, but direct inspection of commit `977053e` showed the source strings are accented. The split files now match the real pre-split source.
