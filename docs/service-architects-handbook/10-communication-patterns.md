# Section 10: Communication Patterns

**Part III: Building Microservices**

---

## Services That Can't Talk Are Just Isolated Monoliths

You've split your monolith into services. You've separated databases. Now comes the **hardest architectural decision**: **How do services communicate?**

Choose wrong, and you'll build a distributed monolith where every deploy breaks three other services. Choose right, and you get independent, scalable services that can fail gracefully.

In this section, I'll show you **every communication pattern** using Bibby's actual flows, and teach you **when to use each one**.

---

## Bibby's Current "Communication" Pattern

**Spoiler:** Bibby doesn't have inter-service communication. It's a monolith. But let's examine what **would become** network calls.

From `BookCommands.java:333-364`:

```java
public void searchByTitle() throws InterruptedException {
    // Step 1: Search for book (Catalog domain)
    String title = getUserInput("Enter book title");
    BookEntity bookEntity = bookService.findBookByTitle(title);  // DB query

    if (bookEntity == null) {
        System.out.println("Book not found");
    } else if (bookEntity.getShelfId() == null) {
        System.out.println("Book found without location");
    } else {
        // Step 2: Get shelf info (Library domain)
        Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(
            bookEntity.getShelfId()
        );  // DB query

        // Step 3: Get bookcase info (Library domain)
        Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(
            shelfEntity.get().getBookcaseId()
        );  // DB query

        System.out.println("Book Was Found\n" +
            "Bookcase: " + bookcaseEntity.get().getBookcaseLabel() + "\n" +
            "Shelf: " + shelfEntity.get().getShelfLabel());
    }
}
```

**Current reality:** Three method calls, three DB queries, ~4ms total.

**Microservices reality:** Three HTTP requests across network boundaries, ~74ms total.

**The question:** Is REST the right choice here? Or should we use messaging? Events? Let's explore.

---

## Pattern 1: Synchronous REST Calls

**When to use:** Service A needs an immediate response from Service B to continue processing.

### Bibby Example: Book Search

If we split Bibby into **Catalog Service** and **Library Service**, the search flow becomes:

```java
// In Catalog Service
@RestController
public class BookSearchController {

    private final BookRepository bookRepository;
    private final LibraryServiceClient libraryClient;  // HTTP client

    @GetMapping("/api/v1/books/search")
    public BookSearchResult searchByTitle(@RequestParam String title) {
        // Step 1: Local DB query (fast)
        Book book = bookRepository.findByTitle(title);
        if (book == null) {
            return new BookSearchResult(null, "NOT_FOUND");
        }

        // Step 2: Call Library Service over network (slow)
        try {
            LocationInfo location = libraryClient.getLocation(book.getShelfId());
            return new BookSearchResult(book, location);

        } catch (FeignException.ServiceUnavailable e) {
            // Library Service is down
            return new BookSearchResult(book, "LOCATION_UNKNOWN");
        }
    }
}
```

**Library Service API:**
```java
// In Library Service
@RestController
public class LocationController {

    @GetMapping("/api/v1/locations/by-shelf/{shelfId}")
    public LocationInfo getLocation(@PathVariable Long shelfId) {
        Shelf shelf = shelfRepository.findById(shelfId)
            .orElseThrow(() -> new ShelfNotFoundException(shelfId));

        Bookcase bookcase = bookcaseRepository.findById(shelf.getBookcaseId())
            .orElseThrow(() -> new BookcaseNotFoundException(shelf.getBookcaseId()));

        return new LocationInfo(
            bookcase.getLabel(),
            shelf.getLabel()
        );
    }
}
```

**HTTP Client (using Spring Cloud OpenFeign):**
```java
@FeignClient(name = "library-service", url = "${library.service.url}")
public interface LibraryServiceClient {

    @GetMapping("/api/v1/locations/by-shelf/{shelfId}")
    LocationInfo getLocation(@PathVariable("shelfId") Long shelfId);
}
```

### Latency Analysis

```
Monolith:
  bookService.findBookByTitle()         → 2ms   (DB query)
  shelfService.findShelfById()          → 1ms   (DB query)
  bookcaseService.findBookCaseById()    → 1ms   (DB query)
  ─────────────────────────────────────────────
  Total:                                  4ms

Microservices (same datacenter):
  HTTP: GET /books/search?title=...     → 2ms   (DB query) + 3ms (network)
  HTTP: GET /locations/by-shelf/7       → 2ms   (2 DB queries) + 3ms (network)
  ─────────────────────────────────────────────
  Total:                                  10ms   (2.5x slower)

Microservices (cross-region):
  HTTP: GET /books/search?title=...     → 2ms   (DB query) + 50ms (network)
  HTTP: GET /locations/by-shelf/7       → 2ms   (DB queries) + 50ms (network)
  ─────────────────────────────────────────────
  Total:                                  104ms  (26x slower!)
```

**Lesson:** Synchronous calls amplify latency. Every hop adds 3-50ms depending on network distance.

### When REST Is the Right Choice

✅ **User-facing reads** - User waiting for response; can't be async
✅ **Validation queries** - "Does shelf X exist?" before creating book
✅ **Low-volume operations** - Admin APIs that run 10 times/day
✅ **Simple request-response** - No complex orchestration needed

❌ **High-volume workflows** - Order processing (1000s/sec)
❌ **Background jobs** - Batch imports, report generation
❌ **Event notifications** - "Book checked out" → update search index

---

## Pattern 2: Asynchronous Messaging

**When to use:** Service A wants Service B to do something, but doesn't need an immediate response.

### Message Brokers: Kafka vs RabbitMQ vs SQS

| Feature | RabbitMQ | Apache Kafka | AWS SQS |
|---------|----------|--------------|---------|
| **Message ordering** | Per queue | Per partition | FIFO queues only |
| **Delivery guarantee** | At-least-once | Exactly-once (with config) | At-least-once |
| **Retention** | Until consumed | Configurable (days/weeks) | 4-14 days |
| **Throughput** | 10K msg/sec | 1M+ msg/sec | Unlimited (throttled) |
| **Use case** | Task queues, RPC | Event streaming, logs | Decoupling, job queues |
| **Latency** | ~5ms | ~10ms | ~100ms |

**Rule of thumb:**
- **RabbitMQ:** Traditional message queue (task distribution, RPC patterns)
- **Kafka:** Event log (audit trail, event sourcing, high throughput)
- **SQS:** AWS-only, serverless integration, cost-effective at scale

### Bibby Example: Checkout Workflow

From `BookCommands.java:458-528`, the checkout flow has these steps:

1. Validate book exists
2. Check if book is available
3. Update book status to CHECKED_OUT
4. (Missing) Send confirmation email
5. (Missing) Update search index
6. (Missing) Log analytics event

**Problem with synchronous approach:**

```java
// ❌ DON'T DO THIS
public void checkOutBook(Long bookId, Long patronId) {
    Book book = bookRepository.findById(bookId);

    if (book.getStatus() == BookStatus.CHECKED_OUT) {
        throw new BookNotAvailableException();
    }

    book.setStatus(BookStatus.CHECKED_OUT);
    bookRepository.save(book);  // Critical transaction

    // Everything below is non-critical, but blocks the user
    emailService.sendCheckoutConfirmation(patronId, book);  // 300ms SMTP call
    searchIndexService.updateBookStatus(bookId, "CHECKED_OUT");  // 50ms HTTP call
    analyticsService.logCheckout(bookId, patronId);  // 20ms HTTP call
    // User waited 370ms for non-critical operations!
}
```

**Better: Publish event, process asynchronously**

```java
// ✅ Publish event immediately
@Transactional
public void checkOutBook(Long bookId, Long patronId) {
    Book book = bookRepository.findById(bookId);

    if (book.getStatus() == BookStatus.CHECKED_OUT) {
        throw new BookNotAvailableException();
    }

    book.setStatus(BookStatus.CHECKED_OUT);
    bookRepository.save(book);

    // Publish event to message broker (non-blocking, < 5ms)
    eventPublisher.publish(new BookCheckedOutEvent(
        bookId,
        patronId,
        book.getTitle(),
        Instant.now()
    ));

    // Return immediately - user sees instant response
}
```

**Event consumers (separate services):**

```java
// Email Service (subscribes to BookCheckedOutEvent)
@KafkaListener(topics = "book-checkout-events")
public void sendCheckoutEmail(BookCheckedOutEvent event) {
    Patron patron = patronRepository.findById(event.getPatronId());
    emailClient.send(patron.getEmail(),
        "Your checkout is ready: " + event.getBookTitle()
    );
    // Runs asynchronously; if it fails, user already got their book
}

// Search Service (subscribes to BookCheckedOutEvent)
@KafkaListener(topics = "book-checkout-events")
public void updateSearchIndex(BookCheckedOutEvent event) {
    elasticsearchClient.update(event.getBookId(), Map.of(
        "status", "CHECKED_OUT",
        "lastCheckout", event.getTimestamp()
    ));
    // Search results eventually consistent (acceptable trade-off)
}

// Analytics Service (subscribes to BookCheckedOutEvent)
@KafkaListener(topics = "book-checkout-events")
public void logCheckout(BookCheckedOutEvent event) {
    analyticsRepository.save(new CheckoutLog(
        event.getBookId(),
        event.getPatronId(),
        event.getTimestamp()
    ));
    // Analytics data can lag by seconds/minutes
}
```

**Benefits:**
- ✅ User response time: 370ms → 6ms (61x faster)
- ✅ Services decoupled (Email Service down? Checkout still works)
- ✅ Easy to add new consumers (e.g., Recommendation Service listens for checkouts)
- ✅ Built-in retry (Kafka retries failed messages automatically)

**Trade-offs:**
- ❌ Eventual consistency (search index lags by ~1 second)
- ❌ Duplicate messages (must handle idempotency)
- ❌ Debugging harder (distributed tracing required)

---

## Pattern 3: The Outbox Pattern

**Problem:** You need **exactly-once** delivery, but message brokers only guarantee **at-least-once**.

**Scenario from BookService.java:22-41:**

```java
@Transactional
public void createNewBook(BookRequestDTO request) {
    // Save to database
    Book book = new Book(request.getTitle());
    bookRepository.save(book);

    // Publish event
    kafkaProducer.send("book-created-events", new BookCreatedEvent(book.getId()));
    // ❌ What if Kafka is down? Transaction rolls back, but event not sent.
    // ❌ What if DB commit fails after Kafka send? Event sent but no book!
}
```

**The dual-write problem:** Can't atomically update DB + send message.

### Solution: Transactional Outbox

**Step 1: Write event to DB in same transaction**

```sql
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    published BOOLEAN DEFAULT FALSE
);
```

**Step 2: Save event atomically with domain change**

```java
@Transactional
public void createNewBook(BookRequestDTO request) {
    // Save book
    Book book = new Book(request.getTitle());
    bookRepository.save(book);

    // Save event to outbox (same transaction, ACID guaranteed)
    OutboxEvent event = new OutboxEvent(
        book.getId(),
        "BookCreated",
        toJson(new BookCreatedEvent(book.getId(), book.getTitle()))
    );
    outboxRepository.save(event);

    // Commit: both book + event are saved atomically
}
```

**Step 3: Background process publishes events**

```java
@Scheduled(fixedDelay = 1000)  // Run every second
public void publishOutboxEvents() {
    List<OutboxEvent> unpublished = outboxRepository.findByPublished(false);

    for (OutboxEvent event : unpublished) {
        try {
            kafkaProducer.send(
                "book-events",
                event.getAggregateId(),
                event.getPayload()
            );

            // Mark as published
            event.setPublished(true);
            outboxRepository.save(event);

        } catch (Exception e) {
            // Log and retry later
            log.error("Failed to publish event {}", event.getId(), e);
        }
    }
}
```

**Alternative: Change Data Capture (CDC)**

Instead of polling, use tools like **Debezium** that tail the database transaction log:

```
PostgreSQL WAL → Debezium → Kafka
```

Debezium reads the `outbox_events` table and publishes changes to Kafka **automatically**. No polling, no scheduled jobs.

**Trade-offs:**
- ✅ Exactly-once semantics (event guaranteed if DB commit succeeds)
- ✅ No lost messages (events survive broker outages)
- ✅ No duplicate events (idempotency key = outbox event ID)
- ❌ Latency (events delayed by polling interval or CDC lag)
- ❌ Storage (outbox table grows; need cleanup job)

---

## Pattern 4: Request-Reply with Messaging

**Problem:** You need async messaging, but also need a response.

**Example:** Catalog Service asks Author Service to validate an author.

**Naive approach (synchronous):**
```java
AuthorValidationResponse response = authorClient.validateAuthor(name);
// Blocks thread, wastes resources if Author Service is slow
```

**Better: Async request-reply**

**Step 1: Send request message**
```java
String correlationId = UUID.randomUUID().toString();

kafkaProducer.send("author-validation-requests", new ValidateAuthorRequest(
    correlationId,
    authorName
));

// Store correlation ID to match response later
pendingRequests.put(correlationId, CompletableFuture<AuthorValidationResponse>());
```

**Step 2: Listen for response**
```java
@KafkaListener(topics = "author-validation-responses")
public void handleValidationResponse(ValidateAuthorResponse response) {
    CompletableFuture<AuthorValidationResponse> future =
        pendingRequests.remove(response.getCorrelationId());

    if (future != null) {
        future.complete(response);  // Unblock waiting caller
    }
}
```

**Step 3: Await response with timeout**
```java
CompletableFuture<AuthorValidationResponse> future = pendingRequests.get(correlationId);

try {
    AuthorValidationResponse response = future.get(5, TimeUnit.SECONDS);
    // Process response
} catch (TimeoutException e) {
    // Author Service didn't respond in 5 seconds
    throw new AuthorServiceUnavailableException();
}
```

**This is essentially RPC over messaging.** RabbitMQ has native support via **RPC pattern**.

**When to use:**
- ✅ Need decoupling (messaging) but also need response
- ✅ Async request allows batching (send 100 requests, collect responses)
- ❌ Don't use for user-facing reads (REST is simpler)

---

## Pattern 5: Event-Driven Architecture

**Philosophy shift:** Instead of "call Service B to do X," publish "X happened" and let subscribers decide what to do.

### Bibby Example: Book Check-In

From `BookCommands.java:532-588`, checking in a book involves:
1. Update book status to AVAILABLE
2. Check if there's a hold/reservation → notify patron
3. Update availability in search index
4. Clear any overdue flags

**Command-driven (tight coupling):**
```java
// ❌ Circulation Service knows about ALL downstream actions
public void checkInBook(String bookTitle) {
    Book book = findBookByTitle(bookTitle);
    book.setStatus(BookStatus.AVAILABLE);
    bookRepository.save(book);

    // Circulation Service must know about Holds
    holdService.checkForHolds(book.getId());

    // Must know about Search
    searchService.updateAvailability(book.getId());

    // Must know about Overdue
    overdueService.clearFlags(book.getId());
}
```

**Event-driven (loose coupling):**
```java
// ✅ Circulation Service publishes event; others listen
public void checkInBook(String bookTitle) {
    Book book = findBookByTitle(bookTitle);
    book.setStatus(BookStatus.AVAILABLE);
    bookRepository.save(book);

    // Publish event, let subscribers decide what to do
    eventBus.publish(new BookCheckedInEvent(
        book.getId(),
        book.getTitle(),
        Instant.now()
    ));
}
```

**Subscribers (independent services):**

```java
// Holds Service (new feature, added 6 months later)
@EventListener
public void onBookCheckedIn(BookCheckedInEvent event) {
    Optional<Hold> hold = holdRepository.findNextForBook(event.getBookId());
    if (hold.isPresent()) {
        emailService.notify(hold.getPatronEmail(),
            "Your hold is ready: " + event.getBookTitle()
        );
    }
}

// Search Service
@EventListener
public void onBookCheckedIn(BookCheckedInEvent event) {
    elasticsearchClient.update(event.getBookId(), Map.of("status", "AVAILABLE"));
}

// Overdue Service
@EventListener
public void onBookCheckedIn(BookCheckedInEvent event) {
    overdueRepository.clearFlags(event.getBookId());
}
```

**Benefits:**
- ✅ **Zero coupling:** Circulation doesn't know Holds Service exists
- ✅ **Extensibility:** Add Recommendation Service listener without changing Circulation
- ✅ **Resilience:** If Overdue Service is down, check-in still succeeds

**Trade-offs:**
- ❌ **Debugging:** "Why wasn't patron notified?" requires tracing events
- ❌ **Ordering:** If BookCheckedOutEvent arrives before BookCreatedEvent, consumers must handle
- ❌ **Discovery:** How do you know who's listening to what?

---

## Resilience Patterns

When services talk over the network, **failures are guaranteed**. Here's how to survive.

### Pattern 6: Timeouts

**Rule:** Every network call must have a timeout.

```java
// ❌ NO TIMEOUT - Thread hangs forever if service is down
LocationInfo location = libraryClient.getLocation(shelfId);

// ✅ Timeout after 2 seconds
@FeignClient(name = "library-service", configuration = TimeoutConfig.class)
public interface LibraryServiceClient { ... }

@Configuration
class TimeoutConfig {
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
            1000,  // Connect timeout: 1 second
            2000   // Read timeout: 2 seconds
        );
    }
}
```

**Choosing timeout values:**

| Operation | Timeout | Reasoning |
|-----------|---------|-----------|
| Read from cache | 50ms | Cache miss = fallback to DB |
| Database query | 500ms | Slow query = likely problem |
| Service API call | 2s | Network + processing |
| External API | 10s | Third-party SLA |
| Batch job | 5min | Long-running operation |

**Netflix rule:** p99 latency × 2 = timeout value. If 99% of requests finish in 800ms, set timeout to 1600ms.

### Pattern 7: Retries with Exponential Backoff

```java
@Retryable(
    value = {ServiceUnavailableException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public LocationInfo getLocationWithRetry(Long shelfId) {
    return libraryClient.getLocation(shelfId);
}
```

**Retry timeline:**
```
Attempt 1: Fails at t=0ms
  Wait 1000ms
Attempt 2: Fails at t=1000ms
  Wait 2000ms (multiplier × delay)
Attempt 3: Fails at t=3000ms
  Throw exception
```

**When NOT to retry:**
- ❌ 400 Bad Request (your request is malformed; retry won't help)
- ❌ 404 Not Found (resource doesn't exist; retry won't help)
- ❌ 409 Conflict (business logic error; retry won't help)
- ✅ 500 Internal Server Error (transient error; retry might work)
- ✅ 503 Service Unavailable (service overloaded; backoff and retry)
- ✅ Connection timeout (network blip; retry might work)

### Pattern 8: Circuit Breaker

**Problem:** If Library Service is down, every request waits for timeout (2s) before failing. With 100 req/sec, you have 200 threads blocked waiting.

**Solution:** After N failures, **stop trying** for a cooldown period.

```java
@CircuitBreaker(name = "library-service", fallbackMethod = "getLocationFallback")
public LocationInfo getLocation(Long shelfId) {
    return libraryClient.getLocation(shelfId);
}

public LocationInfo getLocationFallback(Long shelfId, Exception e) {
    // Circuit is open; return cached or default value
    return new LocationInfo("UNKNOWN", "UNKNOWN");
}
```

**Circuit states:**

```
CLOSED (normal operation)
    ↓
  Failures exceed threshold (e.g., 5 in 10 seconds)
    ↓
OPEN (fail fast, no requests sent)
    ↓
  Wait cooldown period (e.g., 30 seconds)
    ↓
HALF-OPEN (send 1 test request)
    ↓
  If success → CLOSED
  If failure → OPEN
```

**Configuration (Resilience4j):**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      library-service:
        failure-rate-threshold: 50          # Open if 50% fail
        wait-duration-in-open-state: 30s    # Stay open for 30s
        sliding-window-size: 10             # Track last 10 requests
```

**Real-world:** Netflix Hystrix (retired) → Resilience4j (current standard).

---

## Choosing the Right Pattern

Here's the decision tree using Bibby flows:

### searchByTitle() - User waiting for result

**Requirement:** User types title, expects results immediately.

**Pattern:** Synchronous REST
- ✅ User can't proceed without results
- ✅ Low volume (humans typing)
- ✅ Simple request-response
- ❌ Messaging would require polling for results

**Code:**
```java
LocationInfo location = libraryClient.getLocation(shelfId);  // REST call
```

### checkOutBook() - User initiated, but has side effects

**Requirement:** Mark book as checked out, send email, update search.

**Pattern:** Synchronous update + async events
- ✅ Book status update must be synchronous (user needs confirmation)
- ✅ Email can be async (user doesn't wait for SMTP)
- ✅ Search update can be eventual (not critical)

**Code:**
```java
book.setStatus(CHECKED_OUT);
bookRepository.save(book);  // Sync

eventBus.publish(new BookCheckedOutEvent(...));  // Async
```

### Nightly batch: Update book popularity scores

**Requirement:** Calculate checkout frequency, update search ranking.

**Pattern:** Batch job + messaging
- ✅ No user waiting
- ✅ Process millions of records
- ✅ Kafka handles backpressure

**Code:**
```java
books.forEach(book -> {
    int score = calculatePopularity(book.getId());
    kafkaProducer.send("book-score-updates",
        new BookScoreUpdate(book.getId(), score)
    );
});
```

### New feature: Real-time book availability notifications

**Requirement:** Patron subscribes to notifications when book becomes available.

**Pattern:** WebSockets or Server-Sent Events
- ✅ Real-time updates
- ✅ Long-lived connection
- ❌ REST would require polling

**Code (SSE):**
```java
@GetMapping(value = "/books/{id}/availability-stream", produces = "text/event-stream")
public Flux<ServerSentEvent<String>> streamAvailability(@PathVariable Long id) {
    return eventBus.subscribe(BookCheckedInEvent.class)
        .filter(event -> event.getBookId().equals(id))
        .map(event -> ServerSentEvent.builder("AVAILABLE").build());
}
```

---

## Anti-Patterns to Avoid

### 1. The Distributed Monolith via Sync Calls

**Symptom:** Every service calls 5 others synchronously.

```java
// ❌ Call chain 6 levels deep
Catalog → Library → Circulation → Patron → Notification → Email
```

**Problem:** If Email Service is slow (300ms), Catalog response time = 300ms. One slow service kills all services.

**Fix:** Use async events for non-critical paths.

### 2. Chatty APIs (N+1 Problem)

From `BookcaseCommands.java:60-74`:

```java
// ❌ N+1 queries in a monolith become N+1 HTTP calls in microservices
List<BookcaseEntity> bookcases = bookcaseService.getAllBookcases();
for (BookcaseEntity bookcase : bookcases) {
    List<ShelfEntity> shelves = shelfService.findByBookcaseId(bookcase.getId());
    for (ShelfEntity shelf : shelves) {
        List<BookEntity> books = bookService.findBooksByShelf(shelf.getId());
        // If you have 10 bookcases × 20 shelves = 200 HTTP calls!
    }
}
```

**Fix:** Batch API (get all shelves in one call):
```java
GET /api/v1/bookcases?include=shelves,books
```

Or use GraphQL (see Section 6).

### 3. The Event Notification Firehose

**Symptom:** 50 event types, 200 listeners, no one knows what depends on what.

**Fix:**
- Document events in a schema registry (Confluent Schema Registry)
- Version events (`BookCheckedOutV2` when adding fields)
- Monitor consumers (Kafka consumer lag alerts)

### 4. No Idempotency

**Symptom:** Event delivered twice → book charged twice.

```java
// ❌ Not idempotent
@EventListener
public void onBookCheckedOut(BookCheckedOutEvent event) {
    fineRepository.save(new Fine(event.getPatronId(), 2.00));
    // If event replays, patron charged again!
}
```

**Fix:** Use idempotency key
```java
// ✅ Idempotent
@EventListener
public void onBookCheckedOut(BookCheckedOutEvent event) {
    String idempotencyKey = event.getEventId();  // Unique per event
    if (!processedEvents.contains(idempotencyKey)) {
        fineRepository.save(new Fine(event.getPatronId(), 2.00));
        processedEvents.add(idempotencyKey);
    }
}
```

---

## Real-World War Story: Uber's Stacked Events

**Problem (2015):** Uber used event-driven architecture for trip lifecycle. Events: `TripRequested`, `DriverAssigned`, `TripStarted`, `TripCompleted`.

**Failure:** Kafka consumer lag spiked during a traffic surge. Events arrived **out of order**:

```
Expected:  TripRequested → DriverAssigned → TripStarted → TripCompleted
Actual:    TripRequested → TripStarted → DriverAssigned → TripCompleted
```

Result: Drivers marked as "on trip" when they were actually free. Dispatch system broke.

**Solution:**
1. **Partition by trip ID:** All events for Trip #42 go to same Kafka partition (guaranteed order)
2. **Event versioning:** Each event has sequence number; consumers reject out-of-order events
3. **State machines:** Validate transitions (can't start trip before driver assigned)

**Lesson:** Event ordering matters. Design for it.

---

## Action Items

**For Bibby:**

1. **Add Kafka** (1 hour)
   ```bash
   docker-compose up -d kafka
   ```
   Add Spring Kafka dependency

2. **Implement checkout event** (2 hours)
   - Publish `BookCheckedOutEvent` after status update
   - Create Email Service listener (logs to console for now)
   - Verify event delivery

3. **Add circuit breaker** (1 hour)
   - Simulate Library Service downtime
   - Wrap `getLocation()` with `@CircuitBreaker`
   - Test fallback behavior

4. **Implement outbox pattern** (3 hours)
   - Create `outbox_events` table
   - Save events in same transaction as book updates
   - Background job to publish events

**For your project:**

1. **Audit sync calls** - Grep for `@FeignClient`, `RestTemplate`, HTTP clients
2. **Identify async candidates** - Which calls don't need immediate response?
3. **Add timeouts** - Every HTTP call must have a timeout
4. **Instrument messaging** - If using Kafka/RabbitMQ, add consumer lag metrics

---

## Further Reading

- **"Designing Data-Intensive Applications"** by Martin Kleppmann (Chapter 11: Stream Processing)
- **Enterprise Integration Patterns:** Gregor Hohpe (messaging patterns bible)
- **Kafka: The Definitive Guide:** Narkhede, Shapira, Palino (O'Reilly)
- **Resilience4j documentation:** https://resilience4j.readme.io/
- **Outbox pattern:** https://microservices.io/patterns/data/transactional-outbox.html

---

## Next Section Preview

**Section 11: Reliability, Resilience & Fault Tolerance** will teach you:
- Bulkheads and thread pool isolation
- Rate limiting and backpressure
- Graceful degradation strategies
- Chaos engineering (breaking things on purpose)
- SLA/SLO/SLI definitions
- Distributed tracing for debugging

We'll analyze what happens when Bibby's Library Service crashes during a search, and how to design for failure.

Ready? Let's build systems that **survive** production.

---

**Word count:** ~4,800 words
