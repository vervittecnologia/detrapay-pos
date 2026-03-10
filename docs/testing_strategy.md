# Android Testing Strategy

This project now has a layered Android test setup aimed at stable CI and device execution.

## Layers

- Local unit tests in `app/src/test` for pure logic and formatting helpers.
- Instrumented smoke tests in `app/src/androidTest` for app shell validation on a real device or emulator.
- Reusable UI testing helpers under `com.detrapay.testing` for runner, screenshots, waiting and page objects.

## Foundation

- Custom runner: `com.detrapay.testing.DetrapayTestRunner`
- Dependency injection in tests: `HiltTestApplication`
- Isolated instrumented execution: Android Test Orchestrator
- Device stability: animations disabled during test execution
- Failure evidence: automatic screenshot capture on instrumented test failure

## Recommended conventions

- Keep page objects in `app/src/androidTest/java/com/detrapay/testing/pages`
- Keep broad startup and navigation checks in `app/src/androidTest/java/com/detrapay/smoke`
- Prefer local unit tests for parsing, mapping and formatting logic
- Avoid network-coupled instrumented tests unless the backend is mocked or controlled

## Commands

```powershell
./gradlew testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew connectedDebugAndroidTest
```

## Artifacts

- Instrumented test failure screenshots are written under the app external files directory:
  `Android/data/com.detrapay/files/test-artifacts/screenshots`
