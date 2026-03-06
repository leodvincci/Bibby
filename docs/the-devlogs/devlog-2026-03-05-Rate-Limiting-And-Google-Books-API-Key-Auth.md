# Devlog: Token Bucket Rate Limiting & Google Books API Key Authentication

**Date:** 2026-03-05  
**Time:** 22:28 CST  
**Branch:** `feat/rate-limiting-and-api-key-auth`  
**Range:** `c21a64c..879bd1c` (base: merge of PR #348 → HEAD)  
**PR:** #349 (merged to `origin/main` at `909c07f`)

**Commits:**
| SHA | Subject |
|-----|---------|
| `5563815` | Add token bucket rate limiting for API endpoints |
| `f134935` | Add Google Books API key to ISBN lookup requests |
| `a1bc594` | Fix rate limiter to use client IP instead of server local address |
| `93ab16a` | Add unit tests for TokenBucket and RateLimitService |
| `6d1249a` | Document GOOGLE_BOOKS_API_KEY in README environment variables |
| `879bd1c` | Add google.books.api.key to test application.properties |

---

## TL;DR

- **Problem:** The `/api/v1/books/fetchbookmetadata` endpoint was publicly accessible with no abuse protection, and Google Books API calls were unauthenticated (hitting anonymous rate limits).
- **Before:** Any client could spam ISBN lookups without throttling. Google Books requests had no API key, relying on anonymous quota.
- **Outcomes:**
  - Per-IP token bucket rate limiting on the book metadata fetch endpoint (5 burst, 0.05 tokens/sec refill)
  - Google Books API calls now authenticate with a configurable API key via `GOOGLE_BOOKS_API_KEY` environment variable
  - 8 new unit tests covering the rate limiting subsystem
  - Bug fix: rate limiter was accidentally using `getLocalAddr()` (server IP) instead of `getRemoteAddr()` (client IP)
  - Test configuration and README updated for the new env var

---

## Per-Commit Analysis

### Commit 1: `5563815` — Add token bucket rate limiting for API endpoints

**Intent:** Introduce a reusable rate limiting mechanism and wire it into the book import controller.

**Files touched:**
| File | Reason |
|------|--------|
| `ratelimit/RateLimitService.java` | **New.** Per-IP bucket registry using `ConcurrentHashMap` |
| `ratelimit/tokenbucket/TokenBucket.java` | **New.** Token bucket algorithm implementation |
| `web/controllers/cataloging/book/BookImportController.java` | Wire rate limiter; return 429 on exhaustion |

**Key code changes:**

`TokenBucket` implements the classic token bucket algorithm:
- Constructor takes `maxTokens` (burst capacity) and `refillRate` (tokens per second)
- `refill()` calculates elapsed time since last call, adds `elapsed * refillRate` tokens, caps at `maxTokens`
- `isAllowed()` calls `refill()`, then consumes 1 token if available, returning `true`/`false`
- Uses `new Date().getTime()` for timestamps (not `System.currentTimeMillis()` — functional but slightly unconventional)

`RateLimitService` is a Spring `@Service` that manages a `ConcurrentHashMap<String, TokenBucket>`:
```java
public boolean isAllowed(String ipAddress) {
    buckets.putIfAbsent(ipAddress, new TokenBucket(5, .05));
    return buckets.get(ipAddress).isAllowed();
}
```
Each IP gets a bucket with **5 max tokens** and **0.05 tokens/sec refill** (~1 token every 20 seconds).

`BookImportController` now injects `RateLimitService` via constructor and checks rate limit before processing:
```java
String ip = servletRequest.getRemoteAddr();
if (!rateLimitService.isAllowed(ip)) {
    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
}
```

**Architecture notes:**
- `ratelimit` is a new top-level package at `com.penrose.bibby.ratelimit` — sits outside the hexagonal domain modules (`library.cataloging`, `library.stacks`). This is appropriate: rate limiting is cross-cutting infrastructure, not domain logic.
- The controller (`web.controllers`) depends on `ratelimit` — acceptable since both are infrastructure/adapter layer. No domain boundary is crossed.
- `TokenBucket` is a plain POJO (no Spring annotations) — good. `RateLimitService` is a Spring `@Service` — fine for the registry role.
- **No port/adapter abstraction** for rate limiting yet. The controller couples directly to `RateLimitService`. If rate limiting grows (e.g., Redis-backed, configurable per-endpoint), an interface would be warranted.

**Risk / edge cases:**
- **In-memory only:** Buckets reset on app restart. No persistence across deploys.
- **Unbounded map growth:** `ConcurrentHashMap` never evicts entries. A sustained attack from many IPs will leak memory. Needs an eviction policy (TTL-based cleanup or bounded cache like Caffeine).
- **Not thread-safe at the `TokenBucket` level:** `refill()` and `isAllowed()` read/write `currentTokens` and `lastRefilledTime` without synchronization. Under concurrent requests from the same IP, token counts could drift. The `ConcurrentHashMap` ensures safe bucket lookup, but the bucket itself has race conditions on its mutable fields.
- The initial commit used `servletRequest.getLocalAddr()` (server address) — all clients shared one bucket. Fixed in commit `a1bc594`.

**Verification:**
```bash
mvn test -pl . -Dtest="com.penrose.bibby.ratelimit.**"
```

---

### Commit 2: `f134935` — Add Google Books API key to ISBN lookup requests

**Intent:** Authenticate Google Books API calls with a project API key to get better quota and reliability.

**Files touched:**
| File | Reason |
|------|--------|
| `library/cataloging/book/core/application/IsbnLookupService.java` | Inject `@Value("${google.books.api.key}")`, append `&key=` to URL |
| `src/main/resources/application.properties` | Add `google.books.api.key=${GOOGLE_BOOKS_API_KEY}`, clean up blank lines |

**Key code changes:**

Before:
```java
String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + (isbn);
```

After:
```java
@Value("${google.books.api.key}")
private String apiKey;
// ...
String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + (isbn) + "&key=" + apiKey;
```

Also reformatted the file from 4-space indentation to 2-space (project convention alignment) and collapsed the fluent `WebClient` chain to a single line.

**Architecture notes:**
- `IsbnLookupService` lives in `library.cataloging.book.core.application` — the **application layer** of the book cataloging module. It imports from `book.infrastructure.external` (`GoogleBooksResponse`). This is technically a **layering concern**: a core application service importing an infrastructure type. The `GoogleBooksResponse` DTO represents the external API shape and arguably belongs behind an outbound port. This predates this PR, but worth noting.
- The `@Value` injection of the API key is fine for now but could be extracted to a configuration record/class if more external API settings accumulate.

**Risk / edge cases:**
- **Startup failure:** If `GOOGLE_BOOKS_API_KEY` is not set, Spring will throw `IllegalArgumentException` on context load. This is fail-fast (good), but developers may be surprised. The README update in commit `6d1249a` addresses discoverability.
- **Key exposed in logs:** The URL (with key) is not explicitly logged, but WebClient tracing at DEBUG level could expose it. Ensure production logging doesn't leak the key.
- String concatenation for URL building is fragile. A `UriComponentsBuilder` would be safer (handles encoding, avoids accidental double-`?`). Low risk here since the ISBN is a simple alphanumeric string.

**Verification:**
```bash
# Confirm app context loads with test key
mvn test -Dtest="com.penrose.bibby.BibbyApplicationTests"
```

---

### Commit 3: `a1bc594` — Fix rate limiter to use client IP instead of server local address

**Intent:** Bug fix. The initial implementation used `servletRequest.getLocalAddr()` which returns the server's own address, causing all clients to share one rate limit bucket.

**Files touched:**
| File | Reason |
|------|--------|
| `web/controllers/cataloging/book/BookImportController.java` | `getLocalAddr()` → `getRemoteAddr()` |

**Key code change:**
```java
// Before (commit 5563815)
String ip = servletRequest.getLocalAddr();
// After
String ip = servletRequest.getRemoteAddr();
```

One-line fix. Caught quickly and committed separately — good commit hygiene to isolate the bug fix from the feature.

**Risk / edge cases:**
- **Reverse proxy:** Behind nginx/Cloudflare/load balancer, `getRemoteAddr()` returns the proxy IP, not the real client. The app already has `server.forward-headers-strategy=framework` in `application.properties`, which means Spring will respect `X-Forwarded-For`. However, `getRemoteAddr()` on `HttpServletRequest` does **not** automatically use `X-Forwarded-For` — you'd need Spring's `ForwardedHeaderFilter` to rewrite the request, or manually parse `X-Forwarded-For`. This is a **latent issue** for production behind a reverse proxy.

**Verification:**
```bash
# Manual test: hit endpoint twice rapidly, confirm different IPs get separate buckets
curl -X POST http://localhost:8080/api/v1/books/fetchbookmetadata \
  -H "Content-Type: application/json" \
  -d '{"isbn":"9780134685991"}'
```

---

### Commit 4: `93ab16a` — Add unit tests for TokenBucket and RateLimitService

**Intent:** Add test coverage for the new rate limiting subsystem.

**Files touched:**
| File | Reason |
|------|--------|
| `test/.../ratelimit/tokenbucket/TokenBucketTest.java` | **New.** 5 test cases for `TokenBucket` |
| `test/.../ratelimit/RateLimitServiceTest.java` | **New.** 3 test cases for `RateLimitService` |

**Test cases — `TokenBucketTest`:**
1. `newBucket_allowsRequestsUpToMaxTokens` — 3 tokens, all 3 allowed
2. `bucket_blocksWhenTokensExhausted` — 2 tokens with 0 refill, 3rd blocked
3. `bucket_refillsTokensOverTime` — 1 token with 10/sec refill, exhaust → sleep 200ms → allowed again
4. `bucket_doesNotExceedMaxTokensAfterRefill` — Fast refill, still capped at max
5. `bucket_withZeroRefillRate_neverRefills` — 1 token, never recovers

**Test cases — `RateLimitServiceTest`:**
1. `isAllowed_allowsInitialRequests` — First request for an IP is allowed
2. `isAllowed_blocksAfterBurstExhausted` — 5 allowed, 6th blocked
3. `isAllowed_tracksSeparateBucketsPerIp` — Exhausted IP1 doesn't affect IP2

**Architecture notes:**
- Tests are pure unit tests (no Spring context) — fast and focused. Good practice.
- The `Thread.sleep(200)` in the refill test is a minor flakiness risk on slow CI, but 200ms with a 10 tokens/sec rate is generous enough margin.

**Verification:**
```bash
mvn test -Dtest="com.penrose.bibby.ratelimit.tokenbucket.TokenBucketTest,com.penrose.bibby.ratelimit.RateLimitServiceTest"
```

---

### Commit 5: `6d1249a` — Document GOOGLE_BOOKS_API_KEY in README environment variables

**Intent:** Make the new required env var discoverable for other developers.

**Files touched:**
| File | Reason |
|------|--------|
| `README.md` | Add row to environment variables table |

Single-line addition to the existing env var table. Clean and minimal.

---

### Commit 6: `879bd1c` — Add google.books.api.key to test application.properties

**Intent:** Fix `BibbyApplicationTests` (`@SpringBootTest`) failure caused by unresolved `${google.books.api.key}` placeholder in test context.

**Files touched:**
| File | Reason |
|------|--------|
| `src/test/resources/application.properties` | Add `google.books.api.key=test-api-key` |

**Key insight:** This is the classic "added a required property in main but forgot to update test config" issue. Caught by the integration test suite failing to boot.

---

## End-to-End Flow

Here's how a book metadata fetch request flows through the system now:

```
Client POST /api/v1/books/fetchbookmetadata { "isbn": "9780134685991" }
  │
  ▼
BookImportController.importBook(request, servletRequest)
  │
  ├─ Extract client IP via servletRequest.getRemoteAddr()
  ├─ Log IP address
  │
  ├─ rateLimitService.isAllowed(ip)
  │    │
  │    ├─ ConcurrentHashMap.putIfAbsent(ip, new TokenBucket(5, 0.05))
  │    └─ tokenBucket.isAllowed()
  │         ├─ refill() → calculate elapsed time, add tokens, cap at max
  │         └─ currentTokens >= 1 ? consume & return true : return false
  │
  ├─ If blocked → throw ResponseStatusException(429 TOO_MANY_REQUESTS)
  │
  ├─ isbnLookupService.lookupBook(isbn)
  │    │
  │    └─ WebClient GET https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}&key={apiKey}
  │         └─ bodyToMono(GoogleBooksResponse.class)
  │
  ├─ If null/empty → throw ResponseStatusException(404)
  │
  └─ Map to GoogleBookResponseBrief → ResponseEntity.ok(brief)
```

**Why this design is better than before:**
- **Before:** Zero protection against abuse. A single client could hammer Google Books API, exhaust anonymous quota, and degrade service for everyone.
- **After:** Per-IP throttling at 5 burst / 0.05 refill rate means a client gets 5 rapid lookups, then must wait ~20 seconds per additional request. Legitimate users are unaffected; abusers hit 429.
- Authenticated Google Books API calls get higher quota and better reliability than anonymous calls.

---

## Dependency Analysis

**New dependencies added:** None (no `pom.xml` changes). The token bucket is hand-rolled using `java.util.concurrent.ConcurrentHashMap`, `java.util.Date`, and SLF4J.

**New package introduced:** `com.penrose.bibby.ratelimit` with sub-package `tokenbucket`.

**Dependency direction:**
```
web.controllers.cataloging.book.BookImportController
  ├── depends on → ratelimit.RateLimitService  (cross-cutting → OK)
  └── depends on → library.cataloging.book.core.application.IsbnLookupService  (adapter → core, OK)

ratelimit.RateLimitService
  └── depends on → ratelimit.tokenbucket.TokenBucket  (internal, OK)
```

No circular dependencies. No domain layer violations introduced.

**Pre-existing layering concern (not introduced by this PR):**
- `IsbnLookupService` (core/application) imports `GoogleBooksResponse` from `infrastructure.external`. The external API response DTO should ideally be mapped at the infrastructure boundary through an outbound port.

---

## Risks & Suggested Micro-Fixes

1. **Thread safety in `TokenBucket`:** `currentTokens` and `lastRefilledTime` are mutated without synchronization. Under concurrent requests from the same IP, race conditions can cause token over-consumption or under-consumption. **Fix:** Add `synchronized` to `isAllowed()` or use `AtomicLong`/`ReentrantLock`.

2. **Unbounded bucket map growth:** `ConcurrentHashMap<String, TokenBucket>` never evicts. A distributed scan or botnet hitting many IPs will grow the map unboundedly. **Fix:** Use Caffeine cache with TTL-based expiry (e.g., 10 minutes after last access).

3. **`new Date().getTime()` in `TokenBucket`:** Use `System.currentTimeMillis()` directly — avoids unnecessary `Date` object allocation. Or better, inject a `Clock` for testability (eliminates `Thread.sleep` in tests).

4. **Hardcoded rate limit constants:** `new TokenBucket(5, .05)` is hardcoded in `RateLimitService`. Extract to `@ConfigurationProperties` for environment-specific tuning.

5. **Reverse proxy IP resolution:** `getRemoteAddr()` behind a load balancer returns the proxy IP. The app has `server.forward-headers-strategy=framework` but should verify `ForwardedHeaderFilter` is active or parse `X-Forwarded-For` manually.

---

## Test Coverage

**Tests added (8 total):**
- `TokenBucketTest` — 5 tests (burst, exhaustion, refill, cap, zero-refill)
- `RateLimitServiceTest` — 3 tests (initial allow, burst exhaustion, per-IP isolation)

**Tests that should be added:**

1. **`BookImportController` integration test** — Verify the full 429 response when rate limit is exhausted. Use `@WebMvcTest` with a mocked `RateLimitService`:
   ```java
   @Test
   void importBook_returns429WhenRateLimited() { ... }
   ```

2. **`TokenBucket` concurrent access test** — Spin up N threads, all calling `isAllowed()`, verify total allowed count ≤ maxTokens (exposes the thread-safety issue).

3. **`IsbnLookupService` test with API key** — Verify the constructed URL includes `&key=` parameter. Mock `WebClient` to capture the URI.

4. **`RateLimitService` eviction/cleanup test** — Once an eviction strategy is added, verify stale buckets are removed.

5. **`BookImportController` test with missing ISBN** — Verify 400 response (may already exist but not in this diff).

**Run all tests:**
```bash
mvn test
```

**Run only the new rate-limit tests:**
```bash
mvn test -Dtest="com.penrose.bibby.ratelimit.**"
```

---

## Lessons From This Diff

1. **Catch API misuse early with fail-fast config:** Making `google.books.api.key` a required property (not defaulted) means missing configuration is caught at startup, not at runtime when a user tries to scan an ISBN. The tradeoff is you must remember to update test properties too (commit `879bd1c`).

2. **Separate bug fixes from features in commits:** The `getLocalAddr()` → `getRemoteAddr()` fix (`a1bc594`) was isolated into its own commit despite being a one-liner. This makes the bug traceable and revertable independently.

3. **Token bucket is simple but has sharp edges:** The algorithm is easy to implement but concurrency, memory growth, and clock-dependence are non-obvious pitfalls. A production-grade rate limiter (Bucket4j, Resilience4j, or even a Spring Cloud Gateway filter) handles these. For a solo project at this stage, the hand-rolled version is a great learning exercise.

4. **Cross-cutting concerns deserve their own package:** Placing `ratelimit` at the top level (`com.penrose.bibby.ratelimit`) rather than inside `library.cataloging` was the right call — rate limiting is not domain logic for any specific bounded context.

5. **Infrastructure types leaking into core is a recurring pattern:** `IsbnLookupService` (core application) depending on `GoogleBooksResponse` (infrastructure) is a pre-existing smell. This PR didn't make it worse, but every touch to that service is a reminder to introduce an outbound port + mapper.

6. **Test properties drift is a silent killer:** When a new required Spring property is added in `main/resources`, the test `application.properties` must be updated too. A CI smoke test (`@SpringBootTest` loading context) catches this, which is exactly what happened here.

7. **`server.forward-headers-strategy=framework` is necessary but not sufficient:** The property exists in `application.properties`, which is good. But `getRemoteAddr()` behavior depends on whether `ForwardedHeaderFilter` is actually registered and the proxy is trusted. Worth verifying in a deployed environment.

---

## Next Steps

### Immediate (today)
- [ ] Add `synchronized` to `TokenBucket.isAllowed()` to prevent race conditions under concurrent requests
- [ ] Replace `new Date().getTime()` with `System.currentTimeMillis()` in `TokenBucket`
- [ ] Verify `ForwardedHeaderFilter` is active by testing behind a proxy (or in Docker)

### Short-term hardening (this week)
- [ ] Replace `ConcurrentHashMap` in `RateLimitService` with a Caffeine cache (TTL-based eviction, bounded size)
- [ ] Extract rate limit constants (`maxTokens`, `refillRate`) to `application.properties` via `@ConfigurationProperties`
- [ ] Add `@WebMvcTest` integration test for `BookImportController` covering the 429 path
- [ ] Add a concurrent stress test for `TokenBucket`

### Strategic refactors (later)
- [ ] Extract an outbound port for `IsbnLookupService` to decouple core from `GoogleBooksResponse` infrastructure type
- [ ] Consider a `RateLimitFilter` (servlet filter or Spring interceptor) to apply rate limiting globally instead of per-controller
- [ ] Evaluate Bucket4j or Resilience4j if rate limiting needs grow (distributed, per-endpoint config, Redis-backed)
- [ ] Inject a `Clock` into `TokenBucket` for deterministic testing (eliminates `Thread.sleep` flakiness)
