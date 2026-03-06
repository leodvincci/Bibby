# Devlog: X-Forwarded-For Client IP Resolution for Rate Limiting

**Date:** 2026-03-05  
**Time:** 23:44 CST  
**Branch:** `feat/rate-limiting-and-api-key-auth`  
**Range:** `879bd1c..c5176b3` (1 commit)  
**PR:** #349 (merged to `origin/main`)

**Commits:**

| SHA | Subject |
|-----|---------|
| `c5176b3` | Update BookImportController to use X-Forwarded-For for client IP in rate limiting |

---

## Summary

- **Problem:** The rate limiter used `servletRequest.getRemoteAddr()` to identify clients. Behind a reverse proxy (nginx, Cloudflare, AWS ALB), this returns the *proxy's* IP, not the real client's — meaning all users share one rate-limit bucket.
- **Before:** `String ip = servletRequest.getRemoteAddr()` — correct only for direct connections, broken behind any proxy.
- **Outcomes:**
  - `BookImportController` now parses `X-Forwarded-For` header, extracting the first (leftmost) IP as the real client
  - Falls back to `getRemoteAddr()` when no `X-Forwarded-For` is present (direct connections)
  - Rate limiting is now proxy-aware

---

## Commit Analysis: `c5176b3`

### Intent

Resolve a known risk documented in the previous commit (`a1bc594`): "`Behind a reverse proxy, getRemoteAddr() may return the proxy IP; consider parsing X-Forwarded-For header in that scenario.`" This commit closes that gap.

### Files touched

| File | Reason |
|------|--------|
| `web/controllers/cataloging/book/BookImportController.java` | Replace `getRemoteAddr()` with `X-Forwarded-For`-aware IP extraction |

### Key code changes

**Before (commit `879bd1c`):**
```java
String ip = servletRequest.getRemoteAddr();
log.info("USER IP Address: {} ", ip);
if (!rateLimitService.isAllowed(ip)) {
```

**After (commit `c5176b3`):**
```java
String xff = servletRequest.getHeader("X-Forwarded-For");

String clientIp;
if (xff != null && !xff.isBlank()) {
    clientIp = xff.split(",")[0].trim();
} else {
    clientIp = servletRequest.getRemoteAddr();
}

log.info("USER IP Address: {} ", clientIp);
if (!rateLimitService.isAllowed(clientIp)) {
```

**How it works:**
1. Read the `X-Forwarded-For` header from the incoming request
2. If present and non-blank, split on `,` and take the first entry (the original client IP) — proxies append their addresses, so the leftmost is the real client
3. Trim whitespace (some proxies add spaces after commas)
4. If the header is absent (direct connection, no proxy), fall back to `getRemoteAddr()`

### Architecture notes

- **Layer:** This change is entirely within the web adapter layer (`web.controllers`). No domain or application code touched — correct placement.
- **No new dependencies:** Uses only `jakarta.servlet.http.HttpServletRequest`, already imported.
- **Cross-cutting concern still inline:** The IP resolution logic lives directly in the controller method rather than in a shared utility or the `RateLimitService`. If other controllers need rate limiting, this logic would need duplication. Extracting a `ClientIpResolver` utility or moving IP resolution into `RateLimitService` would be a natural follow-up.
- **Relationship to `server.forward-headers-strategy=framework`:** The app has this property set in `application.properties`, which enables Spring's `ForwardedHeaderFilter`. However, that filter rewrites `getRemoteAddr()` only when the proxy is trusted. Manually parsing `X-Forwarded-For` is a belt-and-suspenders approach that works regardless of Spring's filter configuration.

### Risk / edge cases

1. **IP spoofing:** A malicious client can forge the `X-Forwarded-For` header to bypass rate limiting (e.g., sending a different IP each time). Without a trusted proxy stripping/overwriting the header, this is exploitable. **Mitigation:** Configure the reverse proxy to overwrite (not append to) `X-Forwarded-For`, or use Spring's trusted proxy configuration to only accept the header from known proxy IPs.

2. **Multiple proxies:** With a chain like `client → CDN → nginx → app`, `X-Forwarded-For` looks like `clientIP, cdnIP, nginxIP`. Taking index `[0]` is correct — it's the original client. But if the outermost proxy doesn't strip pre-existing `X-Forwarded-For` from the client request, a spoofed value at index `[0]` would be used.

3. **IPv6 formatting:** IPv6 addresses containing colons (e.g., `::1`, `2001:db8::1`) won't be corrupted by the comma-split logic. However, if any proxy uses bracket notation (`[::1]`), the brackets would be included in the extracted IP. Low risk in practice.

4. **Empty header edge case:** The `!xff.isBlank()` check handles the case where the header exists but is empty or whitespace-only.

### Verification

**No new tests were added.** This is the primary gap.

Recommended tests:
```bash
# Run existing rate-limit tests (still pass — no contract change)
mvn test -Dtest="com.penrose.bibby.ratelimit.**"
```

Suggested new test cases for `BookImportController` (via `@WebMvcTest`):

1. **Request with `X-Forwarded-For` header uses the first IP for rate limiting**
2. **Request without `X-Forwarded-For` falls back to `getRemoteAddr()`**
3. **Multi-proxy `X-Forwarded-For` (comma-separated) extracts only the first IP**
4. **Forged `X-Forwarded-For` with spaces is trimmed correctly**

---

## End-to-End Flow (updated)

```
Client POST /api/v1/books/fetchbookmetadata
  │
  ▼  (may pass through reverse proxy)
BookImportController.importBook(request, servletRequest)
  │
  ├─ Validate ISBN (non-null, non-blank)
  │
  ├─ Extract client IP:
  │    ├─ Read X-Forwarded-For header
  │    ├─ If present → split(",")[0].trim() → clientIp
  │    └─ Else → servletRequest.getRemoteAddr() → clientIp
  │
  ├─ Log clientIp
  │
  ├─ rateLimitService.isAllowed(clientIp)
  │    └─ TokenBucket per-IP check (5 burst, 0.05/sec refill)
  │
  ├─ If blocked → 429 TOO_MANY_REQUESTS
  │
  ├─ isbnLookupService.lookupBook(isbn)
  │    └─ Google Books API with &key= parameter
  │
  └─ Map → GoogleBookResponseBrief → 200 OK
```

## Why this is better

- **Before:** Behind a proxy, every user on the internet shared one rate-limit bucket (the proxy's IP). One user could exhaust the limit for everyone.
- **After:** Each real client gets their own bucket, even behind proxies. Direct connections still work via the `getRemoteAddr()` fallback.
- **Minimal change:** 12 insertions, 3 deletions. Single file. No new dependencies.

---

## What dependencies changed?

None. No new imports, no `pom.xml` changes. The `X-Forwarded-For` header is read via the existing `HttpServletRequest.getHeader()` API.

## Layering violations?

None introduced. The IP resolution logic is correctly in the web adapter layer.

---

## Tests

**Existing tests:** `TokenBucketTest` (5 tests) and `RateLimitServiceTest` (3 tests) still pass — this commit doesn't change the rate-limiting contract.

**Tests that should be added:**

1. `BookImportControllerTest.importBook_usesXForwardedForHeader()` — Mock `servletRequest.getHeader("X-Forwarded-For")` to return `"203.0.113.50, 70.41.3.18"`, verify `rateLimitService.isAllowed("203.0.113.50")` is called.

2. `BookImportControllerTest.importBook_fallsBackToRemoteAddr()` — `X-Forwarded-For` returns `null`, verify `getRemoteAddr()` value is used.

3. `BookImportControllerTest.importBook_trimsWhitespaceFromXff()` — Header returns `" 203.0.113.50 , 70.41.3.18"`, verify trimmed IP is used.

4. `BookImportControllerTest.importBook_ignoresBlankXff()` — Header returns `"   "`, verify falls back to `getRemoteAddr()`.

---

## Lessons

- **Close known risks promptly.** The `getRemoteAddr()` limitation was documented as a risk in commit `a1bc594`. This commit closes it within the same feature branch — no lingering TODO that drifts into the backlog.
- **Leftmost IP in `X-Forwarded-For` is the convention, but trust depends on the proxy chain.** This implementation is correct for standard setups, but production hardening requires verifying that the outermost proxy sanitizes the header.
- **Belt-and-suspenders over relying on framework magic.** The app has `server.forward-headers-strategy=framework`, but manually parsing `X-Forwarded-For` makes the behavior explicit and visible in the controller code. Framework configuration can silently change; explicit code is auditable.

---

## Next Steps

### Immediate
- [ ] Add `@WebMvcTest` for `BookImportController` covering `X-Forwarded-For` parsing (4 test cases above)

### Short-term
- [ ] Extract IP resolution into a reusable `ClientIpResolver` utility or move it into `RateLimitService.isAllowed(HttpServletRequest)` to avoid duplication if other controllers need rate limiting
- [ ] Configure Spring's trusted proxy list if deploying behind a known proxy (to prevent `X-Forwarded-For` spoofing)

### Strategic
- [ ] Move rate limiting to a servlet filter or Spring interceptor so IP resolution and throttling apply globally, not per-controller
