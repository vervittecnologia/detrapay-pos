# Task 3 Report: Route And Thin Fragment Boundary

## Status

Completed.

## Changes

- Added `DirectCheckoutRoute`, which owns route-local Compose state, observes the Home and direct-checkout LiveData streams, maps UI actions to reducer/ViewModel work, and emits Android effects.
- Reduced `DirectCheckoutFragment` to Compose host and Android-effect integration.
- Added `androidx.compose.runtime:runtime-livedata` through the version catalog for `observeAsState` support.

## Scoped Compile Adjustment

`openPaymentDialog` previously updated fragment-owned route state in its payment callback. Since that state now belongs to `DirectCheckoutRoute`, the callback now refreshes direct-checkout orders through the ViewModel. No other contract or payment integration was changed.

## Verification

Passed:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --tests "com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentationTest" --no-watch-fs
```

Result: `BUILD SUCCESSFUL in 2m 38s`.

An earlier invocation was cancelled by the command timeout while compiling unit-test Kotlin sources; the subsequent focused run completed successfully.

## Reviewer Findings To Fix

Critical:
- Terminal payment completion never updates route-local state. Credit/debit moves to Waiting, but the dialog callback only reloads orders and cannot reset the route to Orders/Method.

Important:
- Simulator and checkout-credit fee requests share calculateFeesState and are classified using current UI flags rather than request identity.
- Session-expired handling is only emitted for direct-order-list errors; payment-method, fee, pending-payment, and manual-payment errors should also emit ShowSessionExpired when the exception is UnauthorizedException.

## Review Fixes

### What changed

- Added the `OpenPaymentDialog` result callback owned by `DirectCheckoutRoute`. A completed terminal payment now returns to Orders, a cancelled/failed result returns to Method, and both refresh direct checkout orders.
- Added `DirectCheckoutFeeRequestTarget` to explicitly route fee responses to checkout credit or the simulator. The target is cleared when the response is consumed or the related flow is closed or cleared.
- Emit `ShowSessionExpired` for payment-method, fee, pending-payment, and manual-payment error exceptions.

### Test command/result

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.detrapay.ui.home.direct_checkout.DirectCheckoutReducerTest" --tests "com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentationTest" --no-watch-fs
```

Result: `BUILD SUCCESSFUL`.

### Files changed

- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutContract.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducer.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutRoute.kt`
- `app/src/main/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutFragment.kt`
- `app/src/test/java/com/detrapay/ui/home/direct_checkout/DirectCheckoutReducerTest.kt`
- `.superpowers/sdd/task-3-report.md`

### Concerns

- None.
