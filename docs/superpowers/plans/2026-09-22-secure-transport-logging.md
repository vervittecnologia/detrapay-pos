# Secure Transport and Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure external media never receives Detrapay credentials and diagnostic logging never exposes authentication, identity, or raw terminal data.

**Architecture:** Public media uses a dedicated unauthenticated OkHttp client plus explicit URL/response validation. API diagnostics use metadata-only logging with redacted headers. Manager audit sanitization is recursive and size-bounded before persistence.

**Tech Stack:** Kotlin, Retrofit/OkHttp, Hilt, MockK/JUnit, TypeScript, Vitest, Supabase Edge Functions.

**Spec:** `docs/superpowers/specs/2026-09-22-payment-session-security-hardening-design.md`

## Global Constraints

- Authenticated Detrapay and Supabase API clients keep their current base URLs and auth behavior.
- Absolute external URLs use no AuthInterceptor, SessionAuthenticator, device serial, access token, or refresh token.
- Media accepts HTTPS only, validates image content type, and rejects oversized content before persistence.
- Logs must not include passwords, access/refresh tokens, CPF/CNPJ, or raw PlugPag transaction JSON.
- Do not add a permissive network-security exception.
- Do not deploy or install the APK.
- Use TDD and narrow commits.

## Review Focus

- Redirects from an allowed HTTPS logo to another host must still remain credential-free; Task 1 tests the dedicated client path.
- Missing or forged `Content-Length` must not bypass the decoded-byte limit; Task 2 adds a streaming limit test.
- Nested arrays/objects containing `access_token` or `password` must be redacted recursively; Task 4 adds nested tests.
- A huge error response must be truncated before database audit insertion; Task 4 adds the size-bound test.
- Debug builds must remain diagnosable through method/path/status while never logging bodies or Authorization; Task 3 adds logger-policy tests.

---

### Task 1: Separate public media from authenticated API traffic

**Files:**
- Create: `app/src/main/java/com/detrapay/data/api/PublicMediaService.kt`
- Create: `app/src/main/java/com/detrapay/data/api/PublicMediaUrlPolicy.kt`
- Create: `app/src/main/java/com/detrapay/data/api/PublicMediaClientFactory.kt`
- Create: `app/src/test/java/com/detrapay/data/api/PublicMediaUrlPolicyTest.kt`
- Create: `app/src/test/java/com/detrapay/data/api/PublicMediaClientFactoryTest.kt`
- Modify: `app/src/main/java/com/detrapay/di/RetrofitModule.kt`
- Modify: `app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt`
- Modify: `app/src/main/java/com/detrapay/data/api/DetrapayService.kt`

**Interfaces:**
- Consumes: company logo absolute URL.
- Produces: `PublicMediaService.download(@Url url)` on a named no-auth client and `PublicMediaUrlPolicy.validate(raw): HttpUrl`.

- [ ] **Step 1: Write failing URL-policy tests**

```kotlin
@Test fun `accepts https image URL`() {
    assertEquals("https", policy.validate("https://cdn.example/logo.png").scheme)
}

@Test fun `rejects http and non web schemes`() {
    assertFailsWith<IllegalArgumentException> { policy.validate("http://cdn.example/logo.png") }
    assertFailsWith<IllegalArgumentException> { policy.validate("file:///data/logo.png") }
}
```

- [ ] **Step 2: Run the focused test and verify missing policy failure**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.api.PublicMediaUrlPolicyTest"`

Expected: FAIL at compilation.

- [ ] **Step 3: Implement the policy and dedicated client**

Use `raw.toHttpUrl()` and require `scheme == "https"` plus a nonblank host. `PublicMediaClientFactory.create(timeoutMs)` returns a plain OkHttp client with `Authenticator.NONE` and no application interceptors. Create a named `PublicMediaRetrofit` from that client. Move `@GET downloadFile(@Url)` out of `DetrapayService` into `PublicMediaService` and inject it into the remote data source.

- [ ] **Step 4: Add an interceptor-chain regression test**

Assert `PublicMediaClientFactory.create()` has an empty application-interceptor list and `Authenticator.NONE`, while the authenticated client still installs `AuthInterceptor` and `SessionAuthenticator`. Because redirect requests inherit only headers present on the original request, the dedicated client cannot forward `Authorization`, `x-device-serial`, or refresh headers to a redirect host.

- [ ] **Step 5: Run tests and commit**

Run the policy and remote-data-source tests; expected PASS.

```powershell
git add app/src/main/java/com/detrapay/data/api/PublicMediaService.kt app/src/main/java/com/detrapay/data/api/PublicMediaUrlPolicy.kt app/src/main/java/com/detrapay/data/api/PublicMediaClientFactory.kt app/src/test/java/com/detrapay/data/api/PublicMediaUrlPolicyTest.kt app/src/test/java/com/detrapay/data/api/PublicMediaClientFactoryTest.kt app/src/main/java/com/detrapay/di/RetrofitModule.kt app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt app/src/main/java/com/detrapay/data/api/DetrapayService.kt
git commit -m "fix: isolate public media downloads from auth"
```

### Task 2: Bound and validate logo downloads

**Files:**
- Create: `app/src/main/java/com/detrapay/data/api/PublicImageValidator.kt`
- Create: `app/src/test/java/com/detrapay/data/api/PublicImageValidatorTest.kt`
- Modify: `app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/LoginRepository.kt`

**Interfaces:**
- Consumes: HTTP headers and `ResponseBody.source()`.
- Produces: `readValidatedImage(responseBody, contentType, maxBytes = 2_097_152): ByteArray`.

- [ ] **Step 1: Add failing MIME and streaming-limit tests**

```kotlin
@Test fun `rejects non image content`() {
    assertFailsWith<IOException> { validator.read(body("<html>"), "text/html") }
}

@Test fun `rejects body larger than two MiB without content length`() {
    val bytes = ByteArray(2_097_153)
    assertFailsWith<IOException> { validator.read(streamingBody(bytes), "image/png") }
}
```

- [ ] **Step 2: Run tests and verify the validator is missing**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.api.PublicImageValidatorTest"`

Expected: FAIL at compilation.

- [ ] **Step 3: Implement streaming validation**

Allow `image/png`, `image/jpeg`, `image/webp`, and `image/svg+xml`. Reject declared lengths above 2 MiB, then read at most `maxBytes + 1` through Okio and reject when the actual byte count exceeds the limit. Return bytes only after both checks pass.

- [ ] **Step 4: Make login consume validated bytes**

Change `downloadFile` to return the validated `ByteArray`; `LoginRepository` passes those bytes to `ImageUtils.saveImage` and never calls unbounded `ResponseBody.bytes()`.

- [ ] **Step 5: Run tests and commit**

Run the image-validator and LoginRepository tests; expected PASS.

```powershell
git add app/src/main/java/com/detrapay/data/api/PublicImageValidator.kt app/src/test/java/com/detrapay/data/api/PublicImageValidatorTest.kt app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt app/src/main/java/com/detrapay/data/repositories/LoginRepository.kt
git commit -m "fix: validate and bound public images"
```

### Task 3: Replace body logging with redacted metadata diagnostics

**Files:**
- Create: `app/src/main/java/com/detrapay/data/api/SafeHttpLogger.kt`
- Create: `app/src/test/java/com/detrapay/data/api/SafeHttpLoggerTest.kt`
- Modify: `app/src/main/java/com/detrapay/di/RetrofitModule.kt`
- Modify: `app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt`
- Modify: `app/src/main/java/com/detrapay/data/repositories/OrderRepository.kt`

**Interfaces:**
- Consumes: OkHttp request/response metadata.
- Produces: one debug line containing method, relative path, status, and duration; never headers or bodies.

- [ ] **Step 1: Add failing redaction-policy tests**

```kotlin
@Test fun `login diagnostics omit credentials and token response`() {
    val lines = logger.capture(requestWithPassword("secret"), responseWithToken("jwt-value"))
    assertTrue(lines.none { it.contains("secret") || it.contains("jwt-value") || it.contains("Authorization") })
    assertTrue(lines.any { it.contains("POST") && it.contains("/auth/local") && it.contains("200") })
}
```

- [ ] **Step 2: Run the focused test and verify failure**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.api.SafeHttpLoggerTest"`

Expected: FAIL because the safe logger does not exist.

- [ ] **Step 3: Implement metadata-only logging**

Remove BODY/HEADERS `HttpLoggingInterceptor` instances. In debug builds, add `SafeHttpLogger`; in release, add no diagnostic interceptor. Log only method, `encodedPath`, response code, and elapsed milliseconds. Never include query values.

- [ ] **Step 4: Remove manual full-body logging**

Delete `Logger.d(result.body().toString())`, `Logger.d(errorBody().toString())`, request-object logging, and raw backend response logging. Keep short endpoint/status/error-category messages. `unauthorizedError` must not copy response bodies into logs or exception messages.

- [ ] **Step 5: Run logger, remote, and repository tests; commit**

Run the SafeHttpLogger, DetrapayRemoteDataSource, LoginRepository, and OrderRepository tests; expected PASS.

```powershell
git add app/src/main/java/com/detrapay/data/api/SafeHttpLogger.kt app/src/test/java/com/detrapay/data/api/SafeHttpLoggerTest.kt app/src/main/java/com/detrapay/di/RetrofitModule.kt app/src/main/java/com/detrapay/data/datasources/remote/DetrapayRemoteDataSource.kt app/src/main/java/com/detrapay/data/repositories/OrderRepository.kt
git commit -m "fix: redact mobile network diagnostics"
```

### Task 4: Sanitize and bound Manager audit bodies recursively

**Files:**
- Create: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/audit-sanitizer.ts`
- Create: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/utils/audit-sanitizer.test.ts`
- Modify: `C:/Users/gerbs/Documents/GitHub/detrapay-services/supabase/functions/mobile/index.ts`

**Interfaces:**
- Consumes: arbitrary request/response JSON.
- Produces: `sanitizeAuditValue(value, maxSerializedBytes = 32768): unknown`.

- [ ] **Step 1: Add failing nested and size-limit tests**

```ts
it("redacts sensitive keys recursively", () => {
  const sanitized = sanitizeAuditValue({ data: [{ password: "x", nested: { access_token: "y", cpf_cnpj: "123" } }] });
  expect(JSON.stringify(sanitized)).not.toContain("x");
  expect(JSON.stringify(sanitized)).not.toContain("y");
  expect(JSON.stringify(sanitized)).not.toContain("123");
});

it("bounds oversized audit data", () => {
  const sanitized = sanitizeAuditValue({ value: "a".repeat(100_000) }, 1024);
  expect(new TextEncoder().encode(JSON.stringify(sanitized)).byteLength).toBeLessThanOrEqual(1024);
});
```

- [ ] **Step 2: Run the focused test and verify missing-module failure**

Run from `detrapay-services`: `npm test -- supabase/functions/mobile/utils/audit-sanitizer.test.ts`

Expected: FAIL because the sanitizer does not exist.

- [ ] **Step 3: Implement recursive sanitization and truncation**

Redact case-insensitive keys `password`, `token`, `secret`, `authorization`, `jwt`, `access_token`, `refresh_token`, `cpf`, `cpf_cnpj`, and `transaction_log` at every object depth, including objects inside arrays. Limit recursion depth to 20, avoid mutating input, and return `{ truncated: true }` plus a bounded preview when serialized output exceeds the byte limit.

- [ ] **Step 4: Replace shallow `sanitizeBody` usage**

Use `sanitizeAuditValue` for request and response bodies before inserting `api_request_logs`. Keep status, method, path without query values, duration, device model, and relational IDs. Do not persist raw GET query contents.

- [ ] **Step 5: Run tests and commit**

Run: `npm test -- supabase/functions/mobile/utils/audit-sanitizer.test.ts`

Expected: PASS for nested values, arrays, input immutability, recursion depth, and maximum bytes.

```powershell
git add supabase/functions/mobile/utils/audit-sanitizer.ts supabase/functions/mobile/utils/audit-sanitizer.test.ts supabase/functions/mobile/index.ts
git commit -m "fix: sanitize mobile API audit logs"
```

### Task 5: Verify secure transport and diagnostics

**Files:**
- Verify only.

**Interfaces:**
- Consumes: Tasks 1-4.
- Produces: local evidence without deployment or installation.

- [ ] **Step 1: Run focused POS security tests**

Run: `.\gradlew.bat --offline testDebugUnitTest --tests "com.detrapay.data.api.PublicMediaUrlPolicyTest" --tests "com.detrapay.data.api.PublicImageValidatorTest" --tests "com.detrapay.data.api.SafeHttpLoggerTest" --tests "com.detrapay.data.repositories.LoginRepositoryTest" --tests "com.detrapay.data.datasources.remote.DetrapayRemoteDataSourceTest" --max-workers=1`

Expected: PASS.

- [ ] **Step 2: Run focused Manager security tests**

Run from `detrapay-services`: `npm test -- supabase/functions/mobile/utils/audit-sanitizer.test.ts`

Expected: PASS.

- [ ] **Step 3: Run final repository checks if not already run at the same revisions**

Manager: `npm run lint`, `npm test`, `npm run build`.

POS: `.\gradlew.bat --offline testDebugUnitTest lintDebug assembleDebug --max-workers=1`.

Expected: checks pass or unrelated pre-existing failures are captured separately with exact output; do not install or deploy.
