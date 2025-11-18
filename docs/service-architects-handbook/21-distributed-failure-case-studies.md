# Section 21: Distributed Failure Case Studies

**Previous:** [Section 20: Performance Engineering](20-performance-engineering.md)
**Next:** [Section 22: Economics of Microservice Architectures](22-economics-of-microservice-architectures.md)

---

## Learning from Disaster

The best teacher is experience. The second-best is **other people's experience.**

This section analyzes the biggest distributed system failures in tech history — not to mock the engineers who built them, but to **learn from their mistakes** so you don't repeat them.

Every case study follows this structure:
1. **What happened** — The timeline of the outage
2. **Root cause** — The technical reason for failure
3. **What made it worse** — Why it wasn't caught earlier or recovered faster
4. **How to prevent it** — Specific techniques and code examples for Bibby
5. **Key takeaway** — The one lesson you must remember

**These are true stories. The code examples are simplified for clarity, but the lessons are real.**

Let's dive in.

---

## Case Study 1: AWS S3 Outage (February 28, 2017)

### What Happened

**Duration:** 4 hours
**Impact:** Major internet outage, affecting hundreds of thousands of websites
**Financial loss:** Estimated $150 million across all affected companies

**Timeline:**

- **9:37 AM PT:** Engineer runs command to remove a small number of servers for billing system debugging
- **9:37 AM PT:** Typo in command removes critical S3 subsystems instead
- **9:37-1:54 PM:** S3 in us-east-1 completely down
- **1:54 PM:** S3 restored, but many services still broken due to cascading failures

### Root Cause

**A typo in a maintenance command.**

The engineer intended to remove a few servers:

```bash
# Intended command (remove 5% of servers from billing subsystem)
aws-cli decommission --subsystem=s3-billing --percentage=5

# Actual command (typo removed critical subsystems)
aws-cli decommission --subsystem=s3-index --percentage=100
```

**Result:** S3's indexing subsystem was completely removed. Without the index, S3 couldn't locate any objects.

### What Made It Worse

**1. Slow restart process:**
S3 hadn't been fully restarted in years. The restart process took longer than expected because the index had grown so large.

**2. Status dashboard was down:**
AWS's status page was hosted on S3. When S3 went down, the status page couldn't update to tell users there was an outage.

**3. Cascading failures:**
Many AWS services depend on S3. When S3 failed:
- CloudWatch metrics stopped working (stored in S3)
- Lambda functions failed (code stored in S3)
- EC2 couldn't provision new instances (AMIs stored in S3)

### How to Prevent This in Bibby

**Problem 1: Dangerous commands can be run in production**

```java
// Bibby analogy: Admin endpoint with no safeguards
@RestController
@RequestMapping("/admin")
public class AdminController {

    @DeleteMapping("/books/all")
    public ResponseEntity<String> deleteAllBooks() {
        bookRepository.deleteAll();  // EXTREMELY DANGEROUS
        return ResponseEntity.ok("All books deleted");
    }
}
```

**Solution: Multi-step confirmation for destructive operations**

```java
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final Map<String, DestructiveOperation> pendingOperations = new ConcurrentHashMap<>();

    @PostMapping("/books/delete-all/initiate")
    public ResponseEntity<ConfirmationToken> initiateDeleteAll(@RequestHeader("X-Admin-User") String adminUser) {

        // Step 1: Create pending operation
        String confirmationCode = generateRandomCode(6);
        DestructiveOperation op = new DestructiveOperation(
            "DELETE_ALL_BOOKS",
            adminUser,
            Instant.now(),
            confirmationCode
        );

        pendingOperations.put(confirmationCode, op);

        // Send confirmation code to admin's email/Slack
        notificationService.sendConfirmationCode(adminUser, confirmationCode);

        return ResponseEntity.ok(new ConfirmationToken(
            confirmationCode,
            "Check your email for confirmation code. Operation expires in 5 minutes."
        ));
    }

    @DeleteMapping("/books/delete-all/confirm")
    public ResponseEntity<String> confirmDeleteAll(
            @RequestParam String confirmationCode,
            @RequestParam String confirmationPhrase) {

        DestructiveOperation op = pendingOperations.get(confirmationCode);

        if (op == null) {
            return ResponseEntity.badRequest().body("Invalid or expired confirmation code");
        }

        // Require typing exact phrase
        if (!"DELETE ALL BOOKS PERMANENTLY".equals(confirmationPhrase)) {
            return ResponseEntity.badRequest().body("Incorrect confirmation phrase");
        }

        // Check expiration (5 minutes)
        if (op.getCreatedAt().isBefore(Instant.now().minus(Duration.ofMinutes(5)))) {
            pendingOperations.remove(confirmationCode);
            return ResponseEntity.badRequest().body("Confirmation code expired");
        }

        // Execute destructive operation
        int deletedCount = bookRepository.deleteAllAndReturnCount();
        pendingOperations.remove(confirmationCode);

        log.warn("DESTRUCTIVE OPERATION: User {} deleted {} books", op.getAdminUser(), deletedCount);

        return ResponseEntity.ok(String.format("Deleted %d books", deletedCount));
    }
}
```

**Problem 2: Status page depends on the system it monitors**

```yaml
# WRONG: Grafana dashboard hosted in same cluster as services
apiVersion: apps/v1
kind: Deployment
metadata:
  name: grafana
  namespace: bibby  # Same namespace as app services
```

**Solution: Status page on separate infrastructure**

```yaml
# RIGHT: Status page on external hosting (Netlify, GitHub Pages, or separate cluster)
# Use a dedicated status page service like:
# - Statuspage.io (Atlassian)
# - Better Uptime
# - Self-hosted on separate cloud provider
```

**Problem 3: No gradual rollout for infrastructure changes**

```bash
# WRONG: Apply change to all pods at once
kubectl apply -f deployment.yaml

# RIGHT: Gradual rollout with canary
kubectl set image deployment/catalog-service catalog=catalog:v2 --record
kubectl rollout status deployment/catalog-service

# Monitor metrics, rollback if issues detected
kubectl rollout undo deployment/catalog-service
```

### Key Takeaway

**"Make it hard to make mistakes, and easy to recover from them."**

- Require explicit confirmation for destructive operations
- Keep your status page separate from systems it monitors
- Use gradual rollouts, not big-bang deployments

---

## Case Study 2: GitHub Outage (October 21, 2018)

### What Happened

**Duration:** 24 hours 11 minutes
**Impact:** GitHub.com completely unavailable
**Affected users:** 30+ million developers

**Timeline:**

- **10:52 PM:** Network partition between East Coast and West Coast data centers (43 seconds)
- **10:53 PM:** Both data centers think they're primary (split-brain)
- **10:54 PM:** Data written to both databases diverges
- **Next 24 hours:** Engineers manually reconcile inconsistent data

### Root Cause

**Network partition caused database split-brain.**

GitHub uses MySQL with primary-replica replication across two data centers:

```
[Primary MySQL - East Coast] <---> [Replica MySQL - West Coast]
```

When the network split for 43 seconds:

1. West Coast replica couldn't reach East Coast primary
2. West Coast **promoted itself to primary** (automatic failover)
3. Developers on West Coast started writing to West Coast database
4. **Network recovered**
5. Now there are **two primaries** with conflicting data

**Example of conflicting writes:**

```
East Coast DB: User creates PR #12345: "Fix login bug"
West Coast DB: User creates PR #12345: "Update README"

# When network recovers, which PR is #12345?
```

### What Made It Worse

**1. No split-brain detection:**
The system didn't detect that both databases thought they were primary.

**2. Automatic writes continued:**
Both databases kept accepting writes for several minutes before detection.

**3. No automated reconciliation:**
Merging divergent data required manual SQL queries and business logic decisions.

### How to Prevent This in Bibby

**Problem: No split-brain protection in database failover**

```java
// Naive failover logic (DANGEROUS)
@Service
public class DatabaseFailoverService {

    public void checkDatabaseHealth() {
        if (!primaryDatabase.isHealthy()) {
            // Promote replica to primary immediately
            replicaDatabase.promoteToPrimary();
        }
    }
}
```

**Solution 1: Use distributed consensus (etcd, ZooKeeper, Consul)**

```java
@Service
public class SafeDatabaseFailoverService {

    @Autowired
    private EtcdClient etcd;

    public void checkDatabaseHealth() {
        if (!primaryDatabase.isHealthy()) {

            // Try to acquire distributed lock (only one datacenter can hold it)
            boolean acquired = etcd.tryLock(
                "/bibby/db-primary-lock",
                Duration.ofSeconds(10)
            );

            if (acquired) {
                // We won the election, safe to promote
                replicaDatabase.promoteTorimary();
                log.warn("Database failover: Promoted replica to primary");
            } else {
                // Another datacenter already promoted their replica
                log.info("Database failover already handled by another datacenter");
            }
        }
    }
}
```

**Solution 2: Fencing tokens to prevent split-brain writes**

```java
@Service
public class DatabaseService {

    private AtomicLong fencingToken = new AtomicLong(0);

    public void write(String query, Object... params) {
        long currentToken = fencingToken.get();

        // Include fencing token in write
        jdbcTemplate.update(
            "INSERT INTO books (title, author, fencing_token) VALUES (?, ?, ?)",
            params[0], params[1], currentToken
        );
    }

    public void promoteToPromary() {
        // Increment fencing token on promotion
        long newToken = fencingToken.incrementAndGet();

        // Database rejects writes with lower token
        jdbcTemplate.update(
            "UPDATE cluster_metadata SET fencing_token = ? WHERE fencing_token < ?",
            newToken, newToken
        );

        log.warn("Promoted to primary with fencing token {}", newToken);
    }
}
```

**Solution 3: Read-only mode during network partition**

```java
@Component
public class NetworkPartitionDetector {

    @Scheduled(fixedRate = 5000)
    public void checkNetworkPartition() {
        boolean canReachOtherDatacenter = pingOtherDatacenter();

        if (!canReachOtherDatacenter) {
            log.error("Network partition detected! Entering read-only mode.");

            // Stop accepting writes
            applicationContext.getBean(DataSource.class).setReadOnly(true);

            // Return 503 for write requests
            healthIndicator.setStatus(Status.OUT_OF_SERVICE);
        }
    }
}
```

### Key Takeaway

**"In a network partition, you can't have both availability and consistency. Choose one."**

- Use distributed consensus to elect a single primary
- Implement fencing tokens to reject stale writes
- Consider entering read-only mode during partitions

---

## Case Study 3: Knight Capital (August 1, 2012)

### What Happened

**Duration:** 45 minutes
**Impact:** $440 million loss (company went bankrupt)
**Root cause:** Deployment error activated old, dormant code

**Timeline:**

- **9:30 AM:** Stock market opens
- **9:30 AM:** New trading software deploys to 7 of 8 servers
- **9:30 AM:** Old code on 8th server activates (dead code brought back to life)
- **9:30-10:15 AM:** Old code executes millions of erroneous trades
- **10:15 AM:** Trading halted manually
- **Result:** $440 million loss in 45 minutes

### Root Cause

**Partial deployment + reused feature flag = dead code resurrection.**

**What happened:**

1. Engineers repurposed an old feature flag (Power Peg) for new code
2. New code deployed to 7 servers successfully
3. Deployment failed on 8th server (still had old code)
4. Feature flag enabled globally
5. On 7 servers: New code executed (correct behavior)
6. On 8th server: **Old Power Peg code executed** (hadn't been used in 8 years)

**The old code had a bug: It treated test orders as real orders.**

```java
// Old code (dormant for 8 years, supposed to be deleted)
@Service
public class PowerPegTradingService {

    public void processTrade(Order order) {
        if (POWER_PEG_ENABLED) {
            // BUG: This was for testing, but executes real trades
            for (int i = 0; i < 1000; i++) {
                executeMarketOrder(order.getSymbol(), order.getQuantity());
            }
        }
    }
}
```

**Result:** 8th server executed 1,000 trades per incoming order, flooding the market.

### How to Prevent This in Bibby

**Problem 1: Dead code not removed**

```java
// Bibby has old checkout logic that was "disabled"
@Service
public class BookService {

    @Value("${feature.legacy-checkout:false}")
    private boolean useLegacyCheckout;

    public void checkOutBook(Long bookId, Long patronId) {
        if (useLegacyCheckout) {
            // OLD CODE (thought to be disabled, but what if flag is accidentally enabled?)
            legacyCheckoutService.checkout(bookId, patronId);
        } else {
            // NEW CODE
            modernCheckoutService.checkout(bookId, patronId);
        }
    }
}
```

**Solution: Delete dead code, don't disable it**

```java
// After migration is complete, DELETE the old code
@Service
public class BookService {

    public void checkOutBook(Long bookId, Long patronId) {
        // Only one code path exists - no feature flag, no old code
        checkoutService.checkout(bookId, patronId);
    }
}
```

**If you must keep feature flags:**

```java
@Service
public class BookService {

    public void checkOutBook(Long bookId, Long patronId) {
        String checkoutStrategy = featureFlags.getString("checkout-strategy");

        switch (checkoutStrategy) {
            case "modern":
                modernCheckoutService.checkout(bookId, patronId);
                break;
            case "legacy":
                // ONLY allow in dev/staging
                if (environment.isProduction()) {
                    throw new IllegalStateException("Legacy checkout disabled in production");
                }
                legacyCheckoutService.checkout(bookId, patronId);
                break;
            default:
                throw new IllegalArgumentException("Unknown checkout strategy: " + checkoutStrategy);
        }
    }
}
```

**Problem 2: Partial deployments not detected**

```yaml
# Deployment succeeded on 7/8 servers, but Kubernetes didn't notice
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 8
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1  # Only 1 pod can be down during rollout
```

**Solution: Strict readiness checks and deployment validation**

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 8
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0  # Zero downtime
      maxSurge: 1
  minReadySeconds: 30  # Wait 30s after pod is ready before continuing
  progressDeadlineSeconds: 600  # Fail deployment if not done in 10 minutes
  template:
    spec:
      containers:
      - name: catalog
        image: catalog:v2
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
          failureThreshold: 3
          successThreshold: 2  # Must pass twice before considered ready
```

**Problem 3: No version verification**

```java
// No way to check what version is actually running
@GetMapping("/health")
public String health() {
    return "OK";
}
```

**Solution: Include version in health check**

```java
@GetMapping("/actuator/info")
public Map<String, Object> info() {
    return Map.of(
        "version", "1.2.3",
        "gitCommit", gitProperties.getCommitId(),
        "buildTime", buildProperties.getTime(),
        "features", Map.of(
            "checkout-strategy", featureFlags.getString("checkout-strategy"),
            "cache-enabled", featureFlags.getBoolean("cache-enabled")
        )
    );
}
```

**Deployment script verifies all pods have same version:**

```bash
#!/bin/bash
# verify-deployment.sh

EXPECTED_VERSION="1.2.3"

echo "Verifying all pods are running version $EXPECTED_VERSION..."

PODS=$(kubectl get pods -n bibby -l app=catalog-service -o jsonpath='{.items[*].metadata.name}')

for POD in $PODS; do
    ACTUAL_VERSION=$(kubectl exec -n bibby $POD -- curl -s http://localhost:8080/actuator/info | jq -r '.version')

    if [ "$ACTUAL_VERSION" != "$EXPECTED_VERSION" ]; then
        echo "ERROR: Pod $POD is running version $ACTUAL_VERSION (expected $EXPECTED_VERSION)"
        echo "ROLLING BACK DEPLOYMENT"
        kubectl rollout undo deployment/catalog-service -n bibby
        exit 1
    fi

    echo "✓ Pod $POD is running correct version $ACTUAL_VERSION"
done

echo "✓ All pods running version $EXPECTED_VERSION"
```

### Key Takeaway

**"Delete dead code. Don't leave time bombs in your codebase."**

- Remove feature flags after migration completes
- Verify all pods deployed successfully before releasing
- Include version metadata in health checks

---

## Case Study 4: Cloudflare Outage (July 2, 2019)

### What Happened

**Duration:** 27 minutes
**Impact:** 50% of HTTP requests returned 502 errors globally
**Root cause:** Regular expression denial of service (ReDoS)

### Root Cause

**A single regex pattern caused catastrophic backtracking.**

Cloudflare's Web Application Firewall (WAF) uses regexes to detect malicious requests:

```regex
# Simplified version of the problematic regex
(?:(?:\"|'|\]|\}|\\|\d|(?:nan|infinity|true|false|null|undefined|symbol|math)|\`|\-|\+)+[)]*;?((?:\s|-|~|!|{}|\|\||\+)*.*(?:.*=.*)))
```

**When evaluated against certain strings, this regex caused CPU to spike to 100% for 20+ seconds per request.**

**Example catastrophic backtracking:**

```
Input: "x=x"
Regex tries:
  - Match "x" as identifier? No
  - Match "x=" as assignment? No
  - Match "x=x" as expression? Maybe...
  - Backtrack and try different grouping...
  - (repeats exponentially based on input length)

For input length 20: ~2^20 = 1 million attempts
For input length 30: ~2^30 = 1 billion attempts
```

**One user sent a request that triggered this regex. Result:**
- CPU hit 100% processing the regex
- All other requests queued behind it
- Entire data center became unresponsive

### How to Prevent This in Bibby

**Problem: Unvalidated regex in user input validation**

```java
@PostMapping("/api/v1/books/search")
public List<BookEntity> searchBooks(@RequestParam String query) {
    // DANGEROUS: User-provided regex pattern
    Pattern pattern = Pattern.compile(query);

    return bookRepository.findAll().stream()
        .filter(book -> pattern.matcher(book.getTitle()).matches())
        .collect(Collectors.toList());
}
```

**Attack:**

```bash
curl "http://localhost:8080/api/v1/books/search?query=(a%2B)%2B%24"
# Regex: (a+)+$
# Input: "aaaaaaaaaaaaaaaaaX"
# Result: Exponential backtracking, CPU hangs
```

**Solution 1: Never allow user-provided regex**

```java
@PostMapping("/api/v1/books/search")
public List<BookEntity> searchBooks(@RequestParam String query) {
    // Use simple substring matching, not regex
    String sanitized = query.toLowerCase().trim();

    return bookRepository.findByTitleContainingIgnoreCase(sanitized);
}
```

**Solution 2: If regex is necessary, add timeout**

```java
public boolean matchesPattern(String input, String regex) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(input);

    // Timeout after 100ms
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Boolean> future = executor.submit(() -> matcher.matches());

    try {
        return future.get(100, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
        log.warn("Regex match timed out: pattern={}, input={}", regex, input);
        return false;
    } finally {
        executor.shutdownNow();
    }
}
```

**Solution 3: Validate regex before using it**

```java
public void validateRegex(String regex) {
    // Check for known ReDoS patterns
    List<String> dangerousPatterns = List.of(
        "(a+)+",           // Nested quantifiers
        "(a*)*",
        "(a+|b+)+",
        "([a-zA-Z]+)*"
    );

    for (String dangerous : dangerousPatterns) {
        if (regex.contains(dangerous)) {
            throw new IllegalArgumentException("Regex contains dangerous pattern: " + dangerous);
        }
    }

    // Limit regex complexity
    if (regex.length() > 100) {
        throw new IllegalArgumentException("Regex too complex (max 100 chars)");
    }
}
```

### Key Takeaway

**"Input validation is security. Always sanitize user input, especially regex."**

- Never allow user-provided regex patterns
- Add timeouts to regex matching
- Test regex against malicious inputs

---

## Case Study 5: Tarsnap (2011) - The Danger of rm -rf

### What Happened

**Impact:** Entire production database deleted
**Root cause:** Typo in cleanup script
**Recovery time:** 24 hours (from backups)

### Root Cause

**A space in the wrong place:**

```bash
#!/bin/bash
# cleanup.sh - Remove old backup files

BACKUP_DIR="/var/backups/tarsnap"

# Intended: rm -rf /var/backups/tarsnap/*
# Actual (typo): rm -rf /var/backups/tarsnap /*

rm -rf $BACKUP_DIR /*
```

**What happened:**
- `$BACKUP_DIR` expanded to `/var/backups/tarsnap`
- Space caused two separate arguments: `/var/backups/tarsnap` and `/*`
- `/*` = entire filesystem

**Result:** Production database, application code, logs — everything deleted.

### How to Prevent This in Bibby

**Problem: Dangerous shell commands**

```bash
# Kubernetes CronJob that cleans up old book covers
apiVersion: batch/v1
kind: CronJob
spec:
  schedule: "0 2 * * *"  # 2 AM daily
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: cleanup
            image: alpine
            command:
            - /bin/sh
            - -c
            - |
              COVER_DIR="/data/book-covers"
              find $COVER_DIR -type f -mtime +30 -delete
```

**If `COVER_DIR` is empty:**

```bash
find  -type f -mtime +30 -delete
# Deletes ALL files older than 30 days in current directory (/)
```

**Solution 1: Fail if variables are unset**

```bash
#!/bin/bash
set -euo pipefail  # Exit on error, undefined variable, pipe failure

COVER_DIR="${COVER_DIR:?COVER_DIR must be set}"

if [ ! -d "$COVER_DIR" ]; then
    echo "ERROR: Directory does not exist: $COVER_DIR"
    exit 1
fi

echo "Cleaning up files in $COVER_DIR older than 30 days..."
find "$COVER_DIR" -type f -mtime +30 -delete
```

**Solution 2: Dry-run first**

```bash
#!/bin/bash
set -euo pipefail

COVER_DIR="${COVER_DIR:?COVER_DIR must be set}"

# Dry run: List files that would be deleted
echo "Files that would be deleted:"
find "$COVER_DIR" -type f -mtime +30

# Require manual confirmation in production
if [ "$ENVIRONMENT" = "production" ]; then
    read -p "Proceed with deletion? (yes/no): " CONFIRMATION
    if [ "$CONFIRMATION" != "yes" ]; then
        echo "Aborted"
        exit 0
    fi
fi

# Actual deletion
find "$COVER_DIR" -type f -mtime +30 -delete
echo "Cleanup complete"
```

**Solution 3: Use safer alternatives to rm**

```bash
# Instead of: rm -rf /var/data/*
# Use: Move to trash first, delete after verification

TRASH_DIR="/var/trash/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$TRASH_DIR"

# Move instead of delete
mv /var/data/* "$TRASH_DIR"

# Verify application still works
if curl -f http://localhost:8080/health; then
    echo "Application healthy after cleanup"
    # Delete trash after 7 days (gives time to recover)
    find /var/trash -type d -mtime +7 -exec rm -rf {} +
else
    echo "ERROR: Application unhealthy! Restoring from trash..."
    mv "$TRASH_DIR"/* /var/data/
fi
```

### Key Takeaway

**"There is no undo for rm -rf. Use safeguards."**

- Set `set -euo pipefail` in all bash scripts
- Require confirmation for destructive operations
- Move to trash instead of deleting directly

---

## Lessons Applied to Bibby: The Defense Checklist

### 1. Deployment Safety

```yaml
# .github/workflows/deploy.yml
jobs:
  deploy:
    steps:
      - name: Deploy to staging first
        run: kubectl apply -f k8s/staging/

      - name: Wait for staging health
        run: |
          kubectl wait --for=condition=ready pod -l app=catalog-service -n staging --timeout=300s

      - name: Run smoke tests
        run: ./smoke-tests.sh staging

      - name: Verify version consistency
        run: ./verify-deployment.sh staging

      - name: Manual approval required
        uses: trstringer/manual-approval@v1
        with:
          approvers: engineering-leads

      - name: Deploy to production
        run: kubectl apply -f k8s/production/

      - name: Monitor for 10 minutes
        run: |
          sleep 600
          if ./check-error-rate.sh > 1%; then
            echo "Error rate spike detected! Rolling back..."
            kubectl rollout undo deployment/catalog-service -n production
          fi
```

### 2. Database Safety

```java
// Prevent split-brain writes
@Service
public class SafeBookService {

    @Autowired
    private DistributedLockService lockService;

    @Transactional
    public void checkOutBook(Long bookId, Long patronId) {
        // Acquire distributed lock before write
        Lock lock = lockService.acquire("checkout:" + bookId, Duration.ofSeconds(5));

        try {
            BookEntity book = bookRepository.findById(bookId).orElseThrow();

            if (book.isCheckedOut()) {
                throw new BookAlreadyCheckedOutException(bookId);
            }

            book.setStatus(BookStatus.CHECKED_OUT);
            book.setPatronId(patronId);
            bookRepository.save(book);

        } finally {
            lock.release();
        }
    }
}
```

### 3. Input Validation

```java
@RestController
public class BookController {

    @PostMapping("/api/v1/books/search")
    public ResponseEntity<List<Book>> search(@RequestBody @Valid SearchRequest request) {
        // Validation enforced by @Valid
        return ResponseEntity.ok(bookService.search(request.getQuery()));
    }
}

public class SearchRequest {

    @NotBlank(message = "Query cannot be blank")
    @Size(max = 100, message = "Query too long")
    @Pattern(regexp = "^[a-zA-Z0-9\\s-]+$", message = "Query contains invalid characters")
    private String query;
}
```

### 4. Circuit Breakers for External Services

```java
@Service
public class NotificationClient {

    @CircuitBreaker(name = "notification-service", fallbackMethod = "fallbackSendEmail")
    @Retry(name = "notification-service")
    @Timeout(duration = 2000)  // 2 second timeout
    public void sendEmail(String recipientEmail, String subject, String body) {
        restTemplate.postForObject(
            "http://notification-service:8081/api/v1/notifications",
            new EmailRequest(recipientEmail, subject, body),
            Void.class
        );
    }

    // Fallback: Log failure, queue for retry later
    private void fallbackSendEmail(String recipientEmail, String subject, String body, Throwable t) {
        log.error("Failed to send email to {}: {}", recipientEmail, t.getMessage());
        emailRetryQueue.add(new EmailRequest(recipientEmail, subject, body));
    }
}
```

---

## Summary: How to Avoid Being a Case Study

**The pattern in every outage:**
1. Small mistake (typo, config change, partial deployment)
2. No safeguards caught it
3. Cascading failure amplified the impact
4. Recovery took longer than expected

**Your defense:**

- **Deployment:** Gradual rollouts, version verification, manual approval for production
- **Database:** Distributed locks, fencing tokens, read-only mode during partitions
- **Code:** Delete dead code, validate inputs, add timeouts to risky operations
- **Scripts:** Set `set -euo pipefail`, dry-run mode, move instead of delete
- **Monitoring:** Independent status page, alerts on error rate spikes

**Remember:** The engineers at AWS, GitHub, Knight Capital, and Cloudflare are brilliant. They built systems serving billions of users. **And they still had outages.**

**You will have outages too. The question is: Will you learn from theirs first?**

---

## Further Reading

**Post-mortems:**
- **AWS S3 Outage:** https://aws.amazon.com/message/41926/
- **GitHub Outage:** https://github.blog/2018-10-30-oct21-post-incident-analysis/
- **Cloudflare Outage:** https://blog.cloudflare.com/details-of-the-cloudflare-outage-on-july-2-2019/

**Books:**
- **"Site Reliability Engineering"** by Google (2016) — Postmortems and lessons learned
- **"The Phoenix Project"** by Gene Kim (2013) — Fictionalized IT disasters
- **"Chaos Engineering"** by Casey Rosenthal (2020) — Breaking things on purpose

---

**Next:** [Section 22: Economics of Microservice Architectures](22-economics-of-microservice-architectures.md) — Microservices cost more. Here's when they're worth it.
