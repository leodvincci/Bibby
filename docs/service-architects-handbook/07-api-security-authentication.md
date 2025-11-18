# Chapter 7: API Security & Authentication

## Introduction: The Unsecured API

Bibby's current API has no security:

```java
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookService.createNewBook(requestDTO);
    return ResponseEntity.ok("Book Added Successfully");
}
```

**Anyone can call this.** No authentication, no authorization, no rate limiting.

In development, this is fine. In production with microservices, this is a disaster:

```
Attacker:
  POST /api/v1/books {"title": "Spam Book 1"}
  POST /api/v1/books {"title": "Spam Book 2"}
  ... (10,000 times per second)

Result: Database full of spam, service crashes, $1000 cloud bill
```

Or worse:

```
Malicious User:
  GET /api/v1/books → Gets all books
  GET /api/v1/checkouts → Gets all checkout records (PII!)
  DELETE /api/v1/books/42 → Deletes books

Result: Data breach, GDPR violation, legal liability
```

In this chapter, I'll teach you how to secure Bibby's APIs using OAuth2, JWTs, API keys, and rate limiting. You'll learn the difference between authentication (who are you?) and authorization (what can you do?), and how to implement both correctly.

## Authentication vs Authorization

**Authentication**: Proving who you are ("I am Leo Penrose")
**Authorization**: Proving what you can do ("I can check out books")

**Example from Bibby**:

```
User logs in with username/password → Authentication
User tries to check out a book → Authorization (is this user allowed?)
User tries to delete a book → Authorization (only librarians can do this)
```

**Both are required.** Authentication without authorization means anyone can do anything once logged in. Authorization without authentication means you don't know who's making the request.

## OAuth2 Flows and When to Use Each

**OAuth2** is an authorization framework. It answers: "How do third-party applications get limited access to my API?"

### The Four OAuth2 Flows

**1. Authorization Code Flow** (for web apps, mobile apps)

**Use case**: A third-party app wants to access Bibby's API on behalf of a user.

**Scenario**: "LibraryBuddy" mobile app integrates with Bibby. Users want to see their Bibby checkouts in the LibraryBuddy app.

**Flow**:
```
1. User clicks "Connect to Bibby" in LibraryBuddy
2. LibraryBuddy redirects to Bibby's login page
3. User logs in and approves: "Allow LibraryBuddy to view your checkouts?"
4. Bibby redirects back to LibraryBuddy with an authorization code
5. LibraryBuddy exchanges code for access token (server-to-server)
6. LibraryBuddy uses access token to call Bibby API
```

**Implementation**:

```java
// Step 1: User authorization endpoint
@GetMapping("/oauth/authorize")
public String authorize(
    @RequestParam String client_id,
    @RequestParam String redirect_uri,
    @RequestParam String scope,
    @RequestParam String state
) {
    // Show user consent screen: "Allow LibraryBuddy to access your checkouts?"
    return "consent_page";
}

// Step 2: User approves, generate authorization code
@PostMapping("/oauth/authorize")
public ResponseEntity<Void> approveAuthorization(
    @RequestParam String client_id,
    @RequestParam String redirect_uri,
    Principal user
) {
    // Generate short-lived authorization code
    String code = generateAuthorizationCode(client_id, user.getName());

    // Redirect back to client with code
    URI redirectLocation = URI.create(redirect_uri + "?code=" + code);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(redirectLocation)
        .build();
}

// Step 3: Client exchanges code for token
@PostMapping("/oauth/token")
public ResponseEntity<TokenResponse> exchangeToken(
    @RequestParam String grant_type,
    @RequestParam String code,
    @RequestParam String client_id,
    @RequestParam String client_secret,
    @RequestParam String redirect_uri
) {
    // Verify client credentials
    if (!verifyClient(client_id, client_secret)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Verify authorization code
    String userId = validateAndConsumeCode(code);
    if (userId == null) {
        return ResponseEntity.badRequest().build();
    }

    // Generate access token
    String accessToken = generateAccessToken(userId, client_id);
    String refreshToken = generateRefreshToken(userId, client_id);

    TokenResponse response = new TokenResponse(
        accessToken,
        "Bearer",
        3600,  // expires in 1 hour
        refreshToken,
        "checkouts:read books:read"
    );

    return ResponseEntity.ok(response);
}
```

**When to use**: User-facing apps (web, mobile) accessing API on behalf of users.

**2. Client Credentials Flow** (for service-to-service)

**Use case**: Catalog Service needs to call Library Service (no user involved).

**Scenario**: Bibby's Catalog Service queries Library Service for book details.

**Flow**:
```
1. Catalog Service authenticates with client_id + client_secret
2. Receives access token
3. Uses token to call Library Service API
```

**Implementation**:

```java
// Token endpoint
@PostMapping("/oauth/token")
public ResponseEntity<TokenResponse> clientCredentials(
    @RequestParam String grant_type,
    @RequestParam String client_id,
    @RequestParam String client_secret,
    @RequestParam(required = false) String scope
) {
    if (!"client_credentials".equals(grant_type)) {
        return ResponseEntity.badRequest().build();
    }

    // Verify service credentials
    ServiceClient client = serviceClientRepo.findByClientId(client_id);
    if (client == null || !client.verifySecret(client_secret)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Generate access token
    String accessToken = jwtService.generateServiceToken(
        client_id,
        scope != null ? scope : client.getDefaultScopes()
    );

    TokenResponse response = new TokenResponse(
        accessToken,
        "Bearer",
        3600,
        null,  // No refresh token for client credentials
        scope
    );

    return ResponseEntity.ok(response);
}
```

**Client usage** (Catalog Service calling Library Service):

```java
@Service
public class LibraryServiceClient {

    private String accessToken;
    private Instant tokenExpiry;

    private String getAccessToken() {
        // Refresh token if expired
        if (accessToken == null || Instant.now().isAfter(tokenExpiry)) {
            refreshAccessToken();
        }
        return accessToken;
    }

    private void refreshAccessToken() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", "catalog-service");
        params.add("client_secret", System.getenv("LIBRARY_CLIENT_SECRET"));
        params.add("scope", "books:read books:write");

        TokenResponse response = restTemplate.postForObject(
            "https://library-service/oauth/token",
            params,
            TokenResponse.class
        );

        this.accessToken = response.accessToken();
        this.tokenExpiry = Instant.now().plusSeconds(response.expiresIn());
    }

    public BookDto getBook(Long bookId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());

        HttpEntity<?> request = new HttpEntity<>(headers);

        return restTemplate.exchange(
            "https://library-service/api/v2/books/" + bookId,
            HttpMethod.GET,
            request,
            BookDto.class
        ).getBody();
    }
}
```

**When to use**: Service-to-service communication in microservices.

**3. Resource Owner Password Credentials** (legacy, avoid)

**Use case**: First-party apps where you trust the client with user credentials.

**Flow**:
```
1. User enters username/password directly into your app
2. App sends credentials to token endpoint
3. Receives access token
```

**Why to avoid**: App sees user's password (security risk). Use Authorization Code + PKCE instead.

**4. Implicit Flow** (deprecated, DO NOT USE)

This flow returned access tokens in URL fragments. It's insecure (tokens exposed in browser history, logs). Use Authorization Code + PKCE instead.

## OpenID Connect (OIDC): OAuth2 + Identity

**OAuth2** handles authorization. **OIDC** adds authentication (identity).

**Scenario**: Bibby needs to know who the user is, not just authorize access.

**OIDC adds**:
- `id_token`: JWT containing user identity (name, email, etc.)
- UserInfo endpoint: Get full user profile

**Example**:

```java
// Step 1: Authorization (same as OAuth2, but add openid scope)
GET /oauth/authorize?
    client_id=library-buddy&
    scope=openid profile email checkouts:read&  // ← openid scope
    response_type=code&
    redirect_uri=https://librarybuddy.com/callback

// Step 2: Token exchange returns both access_token and id_token
POST /oauth/token
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",  // ← User identity
  "refresh_token": "..."
}
```

**id_token decoded**:

```json
{
  "iss": "https://bibby.com",
  "sub": "user123",
  "aud": "library-buddy",
  "exp": 1732540800,
  "iat": 1732537200,
  "name": "Leo Penrose",
  "email": "leo@example.com",
  "email_verified": true
}
```

**Implementation**:

```java
@PostMapping("/oauth/token")
public ResponseEntity<OIDCTokenResponse> issueTokens(...) {
    // Standard OAuth2 access token
    String accessToken = generateAccessToken(userId, clientId);

    // OIDC ID token
    String idToken = jwtService.generateIdToken(
        userId,
        clientId,
        userService.getUserProfile(userId)
    );

    OIDCTokenResponse response = new OIDCTokenResponse(
        accessToken,
        "Bearer",
        3600,
        refreshToken,
        idToken  // ← ID token
    );

    return ResponseEntity.ok(response);
}
```

**When to use**: When your API needs to know user identity (name, email, profile).

## JWTs and Their Footguns

**JWT** (JSON Web Token) is a common way to represent access tokens.

### JWT Structure

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwibmFtZSI6IkxlbyBQZW5yb3NlIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

HEADER.PAYLOAD.SIGNATURE
```

**Decoded**:

```json
// Header
{
  "alg": "RS256",
  "typ": "JWT"
}

// Payload
{
  "sub": "user123",
  "name": "Leo Penrose",
  "scope": "books:read checkouts:write",
  "exp": 1732540800,
  "iat": 1732537200
}

// Signature (verifies token wasn't tampered with)
```

### Implementing JWT in Bibby

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public String generateAccessToken(String userId, String clientId, List<String> scopes) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
            .setSubject(userId)
            .claim("client_id", clientId)
            .claim("scope", String.join(" ", scopes))
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    public boolean hasScope(String token, String requiredScope) {
        Claims claims = validateToken(token);
        String scopesString = claims.get("scope", String.class);
        Set<String> scopes = Set.of(scopesString.split(" "));
        return scopes.contains(requiredScope);
    }
}
```

### JWT Security Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtService.validateToken(token);

                // Set authentication in context
                String userId = claims.getSubject();
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (UnauthorizedException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### Securing Endpoints

```java
@RestController
@RequestMapping("/api/v2/books")
public class BookController {

    @Autowired
    private JwtService jwtService;

    @GetMapping
    public ResponseEntity<List<BookDto>> getBooks(
        @RequestHeader("Authorization") String authHeader
    ) {
        // Already validated by filter, just check scopes
        String token = authHeader.substring(7);

        if (!jwtService.hasScope(token, "books:read")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<BookDto> books = bookService.findAll();
        return ResponseEntity.ok(books);
    }

    @PostMapping
    public ResponseEntity<BookDto> createBook(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody CreateBookRequest request
    ) {
        String token = authHeader.substring(7);

        if (!jwtService.hasScope(token, "books:write")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookEntity created = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookMapper.toDto(created));
    }
}
```

### JWT Footguns (Security Pitfalls)

**1. Storing JWTs in localStorage (XSS attack vector)**

```javascript
// ❌ WRONG: Vulnerable to XSS
localStorage.setItem('access_token', token);

// ✅ RIGHT: Use httpOnly cookies
// Server sets: Set-Cookie: access_token=...; HttpOnly; Secure; SameSite=Strict
```

**2. Not validating `alg` header (algorithm confusion attack)**

```java
// ❌ WRONG: Attacker can set alg=none
Jwts.parser().setSigningKey(secret).parseClaimsJws(token);

// ✅ RIGHT: Explicitly require algorithm
Jwts.parser()
    .setSigningKey(secret)
    .requireAlgorithm("HS256")  // ← Force specific algorithm
    .parseClaimsJws(token);
```

**3. Long expiration times**

```java
// ❌ WRONG: 30-day expiration
.setExpiration(Date.from(now.plusDays(30)))

// ✅ RIGHT: Short-lived access tokens + refresh tokens
.setExpiration(Date.from(now.plusMinutes(15)))  // 15 minutes
```

**4. Including sensitive data in JWT**

```java
// ❌ WRONG: JWT payload is base64, NOT encrypted
{
  "sub": "user123",
  "ssn": "123-45-6789",  // ← Anyone can decode and see this!
  "credit_card": "4111111111111111"
}

// ✅ RIGHT: Only include non-sensitive data
{
  "sub": "user123",
  "scope": "books:read",
  "exp": 1732540800
}
```

**5. Not revoking JWTs**

JWTs are stateless — once issued, they're valid until expiration. If a user logs out or is banned, their JWT still works!

**Solution**: Token revocation list (blacklist)

```java
@Service
public class TokenRevocationService {

    @Autowired
    private RedisTemplate<String, String> redis;

    public void revokeToken(String token) {
        Claims claims = jwtService.validateToken(token);
        long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();

        // Store token ID in Redis until it expires
        redis.opsForValue().set(
            "revoked:" + claims.getId(),
            "true",
            ttl,
            TimeUnit.MILLISECONDS
        );
    }

    public boolean isRevoked(String token) {
        Claims claims = jwtService.validateToken(token);
        return redis.hasKey("revoked:" + claims.getId());
    }
}
```

## API Keys for Service-to-Service & Partners

**API keys** are simpler than OAuth2 for certain use cases.

### When to Use API Keys

**✅ Use API keys for**:
- Service-to-service (simpler than OAuth2 client credentials)
- Partner integrations (give each partner a key)
- Internal tools (admin scripts, cron jobs)

**❌ Don't use API keys for**:
- User-facing apps (use OAuth2)
- Anything in the browser (keys get exposed)

### Implementing API Keys for Bibby

**1. Generate and store API keys**:

```java
@Entity
@Table(name = "api_keys")
public class ApiKey {
    @Id
    private String keyId;

    private String keyHash;  // ← Store bcrypt hash, not plaintext!
    private String description;  // "Public Library Integration"
    private String[] scopes;  // ["books:read", "checkouts:write"]
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private Integer rateLimitPerMinute;
}
```

**2. Key generation**:

```java
@Service
public class ApiKeyService {

    public ApiKeyResponse generateApiKey(String description, String[] scopes) {
        // Generate cryptographically secure key
        String apiKey = "bibby_" + generateSecureRandom(32);  // e.g., bibby_a1b2c3d4...

        // Store hash only
        String keyHash = BCrypt.hashpw(apiKey, BCrypt.gensalt());

        ApiKey entity = new ApiKey();
        entity.setKeyId(UUID.randomUUID().toString());
        entity.setKeyHash(keyHash);
        entity.setDescription(description);
        entity.setScopes(scopes);
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setRateLimitPerMinute(1000);

        apiKeyRepo.save(entity);

        // Return plain key ONCE (never shown again)
        return new ApiKeyResponse(apiKey, entity.getKeyId());
    }

    public ApiKey validateApiKey(String apiKey) {
        if (!apiKey.startsWith("bibby_")) {
            throw new UnauthorizedException("Invalid API key format");
        }

        // Find all active keys and check hash
        // (In production, use indexed key prefix for faster lookup)
        List<ApiKey> activeKeys = apiKeyRepo.findByActive(true);

        for (ApiKey key : activeKeys) {
            if (BCrypt.checkpw(apiKey, key.getKeyHash())) {
                // Update last used timestamp
                key.setLastUsedAt(LocalDateTime.now());
                apiKeyRepo.save(key);
                return key;
            }
        }

        throw new UnauthorizedException("Invalid API key");
    }
}
```

**3. API key authentication filter**:

```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey != null) {
            try {
                ApiKey key = apiKeyService.validateApiKey(apiKey);

                // Set authentication with scopes
                List<GrantedAuthority> authorities = Arrays.stream(key.getScopes())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

                ApiKeyAuthentication auth = new ApiKeyAuthentication(key.getKeyId(), authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (UnauthorizedException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**4. Client usage**:

```bash
curl -H "X-API-Key: bibby_a1b2c3d4e5f6..." \
     https://api.bibby.com/v2/books
```

## Rate Limiting and Throttling

Without rate limiting, a single client can overwhelm your API.

**Scenario**: Bibby's public search API gets hammered by a scraper bot.

```
Bot sends 10,000 requests/second to /api/v2/books/search
Result: Database saturated, legitimate users get timeouts
```

### Rate Limiting Strategies

**1. Fixed Window** (simple, but has burst problem)

```java
@Service
public class FixedWindowRateLimiter {

    private final ConcurrentHashMap<String, AtomicInteger> requests = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientId, int limit) {
        String key = clientId + ":" + getCurrentMinute();
        AtomicInteger count = requests.computeIfAbsent(key, k -> new AtomicInteger(0));

        return count.incrementAndGet() <= limit;
    }

    private long getCurrentMinute() {
        return System.currentTimeMillis() / 60000;
    }
}
```

**Problem**: Burst at window boundary

```
Minute 0: 1000 requests at 00:59
Minute 1: 1000 requests at 01:00
Total: 2000 requests in 1 second (burst!)
```

**2. Sliding Window** (more accurate)

```java
@Service
public class SlidingWindowRateLimiter {

    @Autowired
    private RedisTemplate<String, String> redis;

    public boolean allowRequest(String clientId, int limit, Duration window) {
        String key = "ratelimit:" + clientId;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        // Remove old requests
        redis.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count requests in window
        Long count = redis.opsForZSet().count(key, windowStart, now);

        if (count < limit) {
            // Add current request
            redis.opsForZSet().add(key, UUID.randomUUID().toString(), now);
            redis.expire(key, window);
            return true;
        }

        return false;
    }
}
```

**3. Token Bucket** (allows bursts but rate-limits over time)

```java
@Service
public class TokenBucketRateLimiter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientId, int capacity, int refillRate) {
        TokenBucket bucket = buckets.computeIfAbsent(
            clientId,
            k -> new TokenBucket(capacity, refillRate)
        );

        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int capacity;
        private final int refillRate;  // tokens per second
        private double tokens;
        private long lastRefill;

        public TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefill = System.nanoTime();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsed = (now - lastRefill) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + (elapsed * refillRate));
            lastRefill = now;
        }
    }
}
```

### Implementing Rate Limiting in Bibby

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String clientId = getClientId(request);

        // Different limits for different clients
        int limit = getRateLimitForClient(clientId);

        if (!rateLimiter.allowRequest(clientId, limit, Duration.ofMinutes(1))) {
            response.setStatus(429);  // Too Many Requests
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientId(HttpServletRequest request) {
        // Prefer API key or user ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }

        // Fall back to IP address
        return request.getRemoteAddr();
    }

    private int getRateLimitForClient(String clientId) {
        // Premium users get higher limits
        if (isPremiumUser(clientId)) {
            return 10000;  // 10k requests/minute
        }
        return 1000;  // 1k requests/minute
    }
}
```

## Zero-Trust Architecture

**Zero-trust principle**: Never trust, always verify.

### Applying Zero-Trust to Bibby

**Traditional security** (perimeter-based):
```
┌─────────────────────────────────────┐
│        Firewall (trust boundary)    │
├─────────────────────────────────────┤
│  Inside = Trusted                   │
│  ┌──────────┐    ┌──────────────┐  │
│  │ Catalog  │───▶│ Library      │  │
│  │ Service  │    │ Service      │  │
│  └──────────┘    └──────────────┘  │
│  (No auth needed between services)  │
└─────────────────────────────────────┘
```

**Problem**: If an attacker compromises Catalog Service, they can access Library Service freely.

**Zero-trust** (verify everything):
```
┌──────────────┐          ┌──────────────────┐
│   Catalog    │──────────│    Library       │
│   Service    │  mTLS    │    Service       │
└──────┬───────┘  +Auth   └────────┬─────────┘
       │                            │
       └─────────────┬──────────────┘
                     │
               ┌─────▼──────┐
               │ Auth Check │
               │ Every Call │
               └────────────┘
```

**Implementation**:

**1. Mutual TLS (mTLS)** between services:

```yaml
# application.yml for Catalog Service
server:
  ssl:
    enabled: true
    client-auth: need  # ← Require client certificate
    key-store: classpath:catalog-service.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    trust-store: classpath:truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
```

**2. Service identity verification**:

```java
@Component
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // Extract client certificate
        X509Certificate[] certs = (X509Certificate[])
            request.getAttribute("javax.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No client certificate");
            return;
        }

        X509Certificate clientCert = certs[0];

        // Verify certificate is valid and from allowed service
        String commonName = getCommonName(clientCert);
        if (!isAllowedService(commonName)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unknown service");
            return;
        }

        // Set service identity in context
        ServiceAuthentication auth = new ServiceAuthentication(commonName);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedService(String serviceName) {
        Set<String> allowedServices = Set.of(
            "catalog-service",
            "checkout-service",
            "notification-service"
        );
        return allowedServices.contains(serviceName);
    }
}
```

**3. Least privilege access**:

```java
@RestController
@RequestMapping("/api/internal/books")
public class InternalBookController {

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBook(
        @PathVariable Long id,
        Authentication auth
    ) {
        // Only catalog-service can read books
        if (!auth.getName().equals("catalog-service")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookEntity book = bookService.findBookById(id).orElseThrow();
        return ResponseEntity.ok(BookMapper.toDto(book));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
        @PathVariable Long id,
        Authentication auth
    ) {
        // Only admin-service can delete books
        if (!auth.getName().equals("admin-service")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Action Items

Before moving to Chapter 8, secure Bibby's APIs:

### 1. Implement JWT Authentication

Add JWT generation and validation to Bibby. Secure the checkout endpoint.

```java
@PostMapping("/api/v2/checkouts")
@PreAuthorize("hasAuthority('checkouts:write')")
public ResponseEntity<CheckoutDto> checkout(...) {
    // Your implementation
}
```

### 2. Add API Key Support

Create an API key generation endpoint. Implement API key authentication filter.

### 3. Implement Rate Limiting

Add rate limiting to Bibby's search endpoint. Set limits:
- Authenticated users: 1000 req/min
- Anonymous users: 100 req/min

### 4. Design OAuth2 Flow

Sketch the authorization code flow for a third-party app integrating with Bibby.

### 5. Security Audit

Review every Bibby endpoint and classify:
- Public (no auth)
- Requires authentication
- Requires specific scopes
- Rate limits

## Key Takeaways

1. **Authentication vs Authorization** — Who you are vs what you can do

2. **OAuth2 Authorization Code for user apps** — Mobile/web apps accessing API on behalf of users

3. **OAuth2 Client Credentials for services** — Service-to-service communication

4. **OIDC adds identity to OAuth2** — Get user profile with id_token

5. **JWTs have security pitfalls** — Don't store in localStorage, validate algorithm, short expiration, no sensitive data

6. **API keys for service-to-service & partners** — Simpler than OAuth2 for certain cases

7. **Rate limiting prevents abuse** — Sliding window better than fixed window

8. **Zero-trust: verify everything** — Don't trust based on network location

9. **mTLS for service-to-service** — Mutual authentication with certificates

10. **Least privilege access** — Only grant minimum required permissions

## Further Reading

### Specifications
- **OAuth 2.0 RFC 6749** — The OAuth2 spec
- **OpenID Connect Core 1.0** — OIDC specification
- **JWT RFC 7519** — JSON Web Token spec

### Books
- **"OAuth 2 in Action"** by Justin Richer & Antonio Sanso
- **"API Security in Action"** by Neil Madden
- **"Zero Trust Networks"** by Evan Gilman & Doug Barth

### Security Resources
- **OWASP API Security Top 10**
- **JWT.io** — Decode and verify JWTs
- **OAuth 2.0 Simplified** by Aaron Parecki

---

## What's Next?

In **Chapter 8: Service Boundaries & Decomposition**, we'll return to architecture and learn how to split Bibby into services correctly:
- Finding natural service boundaries using DDD
- Functional vs domain decomposition
- Service granularity (how "micro" should microservices be?)
- Avoiding the distributed monolith
- When to split vs when to keep together

**Remember**: You've learned how to secure APIs. Next, you'll learn where to draw service boundaries.

Security is solved. Architecture is next. Let's design Bibby's microservices correctly.
