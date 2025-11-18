# Section 17: Patterns for Coordination

**Part VI: Advanced Patterns**

---

## The Distributed Transaction Problem

From `BookService.java:22-41`, Bibby's `createNewBook()` method is simple:

```java
@Transactional
public void createNewBook(BookRequestDTO request) {
    // Find or create author
    AuthorEntity author = authorRepository.findByFirstNameAndLastName(...);
    if (author == null) {
        author = new AuthorEntity(request.getFirstName(), request.getLastName());
        authorRepository.save(author);  // Write 1
    }

    // Create book
    BookEntity book = new BookEntity(request.getTitle());
    book.setAuthors(author);
    bookRepository.save(book);  // Write 2
}
```

**In a monolith:** `@Transactional` guarantees **both writes succeed or both fail**. Database ACID properties handle this.

**In microservices (Author Service + Catalog Service):**

```
┌──────────────────┐         ┌──────────────────┐
│ Author Service   │         │ Catalog Service  │
│                  │         │                  │
│ POST /authors    │         │ POST /books      │
│ (create author)  │         │ (create book)    │
└──────────────────┘         └──────────────────┘
       │                            │
       ▼                            ▼
  ┌─────────┐                 ┌─────────┐
  │Authors  │                 │ Books   │
  │Database │                 │Database │
  └─────────┘                 └─────────┘
```

**Problem:** Author created successfully. Catalog Service call **fails** (network timeout). Now you have an orphaned author with no books.

**You can't use `@Transactional` across services.** This section teaches you how to coordinate distributed workflows correctly.

---

## The Saga Pattern (Deep Dive)

**Definition:** Sequence of local transactions where each service publishes events that trigger the next step. If a step fails, execute **compensating transactions** to undo previous work.

**From Section 9, we covered basics. Now: complete implementation.**

### Saga Example: Book Checkout Workflow

**Business requirement:** When user checks out a book:
1. Mark book as CHECKED_OUT (Catalog Service)
2. Create checkout record (Circulation Service)
3. Send confirmation email (Notification Service)
4. Update search index (Search Service)

**If email fails:** Rollback checkout, mark book AVAILABLE.

### Implementation Pattern 1: Choreography (Event-Driven)

**No central coordinator. Services listen to events and react.**

**Step 1: Catalog Service receives checkout request**

```java
@RestController
public class CheckoutController {

    @PostMapping("/api/v1/checkouts")
    public ResponseEntity<CheckoutResponse> checkoutBook(
        @RequestBody CheckoutRequest request
    ) {
        // Validate book is available
        Book book = bookRepository.findById(request.getBookId());
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        if (book.getStatus() == BookStatus.CHECKED_OUT) {
            return ResponseEntity.status(409).build();  // Conflict
        }

        // Create saga instance (track this workflow)
        String sagaId = UUID.randomUUID().toString();

        // Step 1: Update book status
        book.setStatus(BookStatus.CHECKED_OUT);
        book.setSagaId(sagaId);  // Track which saga owns this
        bookRepository.save(book);

        // Publish event
        eventPublisher.publish(new BookCheckedOutEvent(
            sagaId,
            book.getId(),
            request.getPatronId(),
            book.getTitle(),
            Instant.now()
        ));

        // Return immediately (saga continues async)
        return ResponseEntity.accepted()
            .body(new CheckoutResponse(sagaId, "PENDING"));
    }
}
```

**Step 2: Circulation Service listens for BookCheckedOutEvent**

```java
@KafkaListener(topics = "book-checkout-events")
public void onBookCheckedOut(BookCheckedOutEvent event) {
    try {
        // Create checkout record
        Checkout checkout = new Checkout(
            event.getSagaId(),
            event.getBookId(),
            event.getPatronId(),
            Instant.now(),
            Instant.now().plus(14, ChronoUnit.DAYS)  // Due in 14 days
        );
        checkoutRepository.save(checkout);

        // Publish success event
        eventPublisher.publish(new CheckoutRecordCreatedEvent(
            event.getSagaId(),
            checkout.getId(),
            event.getPatronId(),
            event.getBookTitle()
        ));

    } catch (Exception e) {
        // Publish failure event
        eventPublisher.publish(new CheckoutRecordFailedEvent(
            event.getSagaId(),
            e.getMessage()
        ));
    }
}
```

**Step 3: Notification Service listens for CheckoutRecordCreatedEvent**

```java
@KafkaListener(topics = "checkout-record-events")
public void onCheckoutRecordCreated(CheckoutRecordCreatedEvent event) {
    try {
        // Get patron email
        Patron patron = patronRepository.findById(event.getPatronId());

        // Send email
        emailService.send(
            patron.getEmail(),
            "Book Checked Out",
            "You checked out: " + event.getBookTitle()
        );

        // Publish success event
        eventPublisher.publish(new NotificationSentEvent(
            event.getSagaId()
        ));

    } catch (Exception e) {
        // Publish failure event (triggers rollback!)
        eventPublisher.publish(new NotificationFailedEvent(
            event.getSagaId(),
            e.getMessage()
        ));
    }
}
```

**Step 4: Catalog Service listens for NotificationFailedEvent (compensation)**

```java
@KafkaListener(topics = "notification-events")
public void onNotificationFailed(NotificationFailedEvent event) {
    // COMPENSATE: Undo book checkout
    Book book = bookRepository.findBySagaId(event.getSagaId());
    if (book != null) {
        book.setStatus(BookStatus.AVAILABLE);
        book.setSagaId(null);
        bookRepository.save(book);

        log.warn("Rolled back book checkout for saga {}", event.getSagaId());
    }
}
```

**Step 5: Circulation Service also listens for NotificationFailedEvent**

```java
@KafkaListener(topics = "notification-events")
public void onNotificationFailed(NotificationFailedEvent event) {
    // COMPENSATE: Delete checkout record
    Checkout checkout = checkoutRepository.findBySagaId(event.getSagaId());
    if (checkout != null) {
        checkoutRepository.delete(checkout);
        log.warn("Deleted checkout record for saga {}", event.getSagaId());
    }
}
```

**Event flow (happy path):**

```
1. User → POST /checkouts
2. Catalog → BookCheckedOutEvent
3. Circulation → CheckoutRecordCreatedEvent
4. Notification → NotificationSentEvent
5. Search → (updates index)
6. Saga complete
```

**Event flow (failure path):**

```
1. User → POST /checkouts
2. Catalog → BookCheckedOutEvent
3. Circulation → CheckoutRecordCreatedEvent
4. Notification → NotificationFailedEvent (email service down!)
5. Catalog → (compensate: set book AVAILABLE)
6. Circulation → (compensate: delete checkout)
7. Saga rolled back
```

**Pros:**
- ✅ No single point of failure (no coordinator)
- ✅ Loose coupling (services don't know about each other)
- ✅ Easy to add new participants (just subscribe to events)

**Cons:**
- ❌ Hard to understand (logic scattered across services)
- ❌ No centralized view of saga state
- ❌ Testing is complex (need all services running)

### Implementation Pattern 2: Orchestration (Centralized)

**Central saga orchestrator coordinates all steps.**

**Saga Orchestrator:**

```java
@Service
public class CheckoutSagaOrchestrator {

    private final CatalogClient catalogClient;
    private final CirculationClient circulationClient;
    private final NotificationClient notificationClient;
    private final SagaRepository sagaRepository;

    public CheckoutSagaResult executeCheckoutSaga(CheckoutRequest request) {
        String sagaId = UUID.randomUUID().toString();

        SagaState saga = new SagaState(sagaId, SagaStatus.STARTED);
        sagaRepository.save(saga);

        try {
            // Step 1: Check out book in Catalog
            saga.addStep("CATALOG_CHECKOUT", StepStatus.IN_PROGRESS);
            CatalogCheckoutResponse catalogResponse = catalogClient.checkoutBook(
                request.getBookId(),
                sagaId
            );
            saga.updateStep("CATALOG_CHECKOUT", StepStatus.COMPLETED);

            // Step 2: Create checkout record in Circulation
            saga.addStep("CREATE_CHECKOUT", StepStatus.IN_PROGRESS);
            CirculationCheckoutResponse circulationResponse =
                circulationClient.createCheckout(
                    catalogResponse.getBookId(),
                    request.getPatronId(),
                    sagaId
                );
            saga.updateStep("CREATE_CHECKOUT", StepStatus.COMPLETED);

            // Step 3: Send notification
            saga.addStep("SEND_NOTIFICATION", StepStatus.IN_PROGRESS);
            notificationClient.sendCheckoutEmail(
                request.getPatronId(),
                catalogResponse.getBookTitle(),
                sagaId
            );
            saga.updateStep("SEND_NOTIFICATION", StepStatus.COMPLETED);

            // Success!
            saga.setStatus(SagaStatus.COMPLETED);
            sagaRepository.save(saga);

            return new CheckoutSagaResult(sagaId, SagaStatus.COMPLETED);

        } catch (CatalogServiceException e) {
            // Failure at step 1: No compensation needed (nothing committed)
            saga.setStatus(SagaStatus.FAILED);
            saga.setErrorMessage(e.getMessage());
            sagaRepository.save(saga);
            throw e;

        } catch (CirculationServiceException e) {
            // Failure at step 2: Compensate step 1
            compensateCatalogCheckout(sagaId);
            saga.setStatus(SagaStatus.ROLLED_BACK);
            sagaRepository.save(saga);
            throw e;

        } catch (NotificationServiceException e) {
            // Failure at step 3: Compensate steps 1 and 2
            compensateCirculationCheckout(sagaId);
            compensateCatalogCheckout(sagaId);
            saga.setStatus(SagaStatus.ROLLED_BACK);
            sagaRepository.save(saga);
            throw e;
        }
    }

    private void compensateCatalogCheckout(String sagaId) {
        try {
            catalogClient.rollbackCheckout(sagaId);
        } catch (Exception e) {
            log.error("Failed to compensate catalog checkout for saga {}", sagaId, e);
            // Store in dead letter queue for manual intervention
            deadLetterQueue.add(new CompensationFailure(sagaId, "CATALOG_CHECKOUT", e));
        }
    }

    private void compensateCirculationCheckout(String sagaId) {
        try {
            circulationClient.deleteCheckout(sagaId);
        } catch (Exception e) {
            log.error("Failed to compensate circulation checkout for saga {}", sagaId, e);
            deadLetterQueue.add(new CompensationFailure(sagaId, "CREATE_CHECKOUT", e));
        }
    }
}
```

**Saga state tracking:**

```java
@Entity
public class SagaState {
    @Id
    private String sagaId;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;  // STARTED, COMPLETED, FAILED, ROLLED_BACK

    @OneToMany(cascade = CascadeType.ALL)
    private List<SagaStep> steps;

    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}

@Entity
public class SagaStep {
    @Id
    private Long id;

    private String stepName;

    @Enumerated(EnumType.STRING)
    private StepStatus status;  // IN_PROGRESS, COMPLETED, FAILED, COMPENSATED

    private Instant startedAt;
    private Instant completedAt;
}
```

**Pros:**
- ✅ Clear workflow logic (all in one place)
- ✅ Easy to understand and test
- ✅ Centralized monitoring (saga dashboard)
- ✅ Can retry failed steps

**Cons:**
- ❌ Orchestrator is single point of failure (needs HA)
- ❌ Orchestrator can become god service (knows about all services)
- ❌ Tight coupling (orchestrator depends on all service APIs)

---

## Idempotency: The Foundation

**Problem:** Network failures cause retries. Same saga step might execute twice.

**Example:** Notification service sends email. Request times out. Orchestrator retries. **User gets 2 emails.**

**Solution:** Idempotency keys.

### Idempotent Email Service

```java
@Service
public class EmailService {

    private final EmailClient emailClient;
    private final IdempotencyRepository idempotencyRepo;

    public void sendCheckoutEmail(
        String patronEmail,
        String bookTitle,
        String idempotencyKey  // ← Saga ID or unique request ID
    ) {
        // Check if already processed
        if (idempotencyRepo.exists(idempotencyKey)) {
            log.info("Email already sent for idempotency key {}", idempotencyKey);
            return;  // Skip (idempotent)
        }

        // Send email
        emailClient.send(
            patronEmail,
            "Book Checked Out",
            "You checked out: " + bookTitle
        );

        // Record that we processed this
        idempotencyRepo.save(new IdempotencyRecord(
            idempotencyKey,
            "sendCheckoutEmail",
            Instant.now()
        ));
    }
}
```

**Idempotency record:**

```java
@Entity
@Table(indexes = @Index(name = "idx_key", columnList = "idempotencyKey", unique = true))
public class IdempotencyRecord {
    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    private String operation;
    private Instant processedAt;
}
```

**Now:**
```
Request 1: sendCheckoutEmail(sagaId="abc123")
  → Email sent, record saved

Request 2: sendCheckoutEmail(sagaId="abc123")  ← Retry!
  → Check idempotencyRepo: exists("abc123") = true
  → Skip (no duplicate email)
```

**Idempotency for database writes:**

```java
@Transactional
public void createCheckout(CheckoutRequest request, String sagaId) {
    // Check if already created
    if (checkoutRepository.existsBySagaId(sagaId)) {
        return;  // Already created (idempotent)
    }

    Checkout checkout = new Checkout(sagaId, request.getBookId(), ...);
    checkoutRepository.save(checkout);
}
```

**Key insight:** Use saga ID or request ID as idempotency key. Store in database with unique constraint.

---

## Process Manager Pattern

**When sagas get complex (10+ steps, conditional branches), use a process manager (state machine).**

**Example: Book reservation workflow**

```
User requests book:
  ├─ If AVAILABLE → Check out immediately
  ├─ If CHECKED_OUT → Create hold
  │   ├─ When book returned → Notify patron
  │   └─ If patron doesn't respond in 3 days → Offer to next in queue
  └─ If ON_HOLD → Add to waitlist
```

**State machine:**

```java
@Entity
public class BookReservationProcess {
    @Id
    private String processId;

    @Enumerated(EnumType.STRING)
    private ReservationState state;

    private Long bookId;
    private Long patronId;
    private Instant createdAt;
    private Instant expiresAt;
}

public enum ReservationState {
    REQUESTED,
    BOOK_AVAILABLE_CHECKING_OUT,
    BOOK_CHECKED_OUT_CREATING_HOLD,
    HOLD_CREATED_WAITING_FOR_RETURN,
    BOOK_RETURNED_NOTIFYING_PATRON,
    NOTIFICATION_SENT_WAITING_FOR_PICKUP,
    PICKED_UP,
    EXPIRED,
    CANCELLED
}
```

**State transition logic:**

```java
@Service
public class ReservationProcessManager {

    @Transactional
    public void handleBookReservationRequested(BookReservationRequestedEvent event) {
        // Create process
        BookReservationProcess process = new BookReservationProcess(
            event.getProcessId(),
            ReservationState.REQUESTED,
            event.getBookId(),
            event.getPatronId()
        );
        processRepository.save(process);

        // Check book status
        Book book = catalogClient.getBook(event.getBookId());

        if (book.getStatus() == BookStatus.AVAILABLE) {
            transition(process, ReservationState.BOOK_AVAILABLE_CHECKING_OUT);
            catalogClient.checkoutBook(event.getBookId(), event.getPatronId());

        } else if (book.getStatus() == BookStatus.CHECKED_OUT) {
            transition(process, ReservationState.BOOK_CHECKED_OUT_CREATING_HOLD);
            circulationClient.createHold(event.getBookId(), event.getPatronId());
        }
    }

    @KafkaListener(topics = "book-return-events")
    public void handleBookReturned(BookReturnedEvent event) {
        // Find processes waiting for this book
        List<BookReservationProcess> waiting = processRepository
            .findByBookIdAndState(
                event.getBookId(),
                ReservationState.HOLD_CREATED_WAITING_FOR_RETURN
            );

        if (!waiting.isEmpty()) {
            // Notify first patron in queue
            BookReservationProcess process = waiting.get(0);
            transition(process, ReservationState.BOOK_RETURNED_NOTIFYING_PATRON);

            notificationClient.notifyPatronBookAvailable(
                process.getPatronId(),
                event.getBookId()
            );

            // Set expiration (patron has 3 days to pick up)
            process.setExpiresAt(Instant.now().plus(3, ChronoUnit.DAYS));
            processRepository.save(process);
        }
    }

    @Scheduled(fixedDelay = 60000)  // Run every minute
    public void checkExpiredReservations() {
        List<BookReservationProcess> expired = processRepository
            .findByStateAndExpiresAtBefore(
                ReservationState.NOTIFICATION_SENT_WAITING_FOR_PICKUP,
                Instant.now()
            );

        for (BookReservationProcess process : expired) {
            transition(process, ReservationState.EXPIRED);

            // Offer to next patron in queue
            circulationClient.moveHoldToNextPatron(process.getBookId());
        }
    }

    private void transition(BookReservationProcess process, ReservationState newState) {
        log.info("Process {} transitioning from {} to {}",
            process.getProcessId(), process.getState(), newState);
        process.setState(newState);
        processRepository.save(process);
    }
}
```

**Process manager tracks long-running workflows** (hours, days, weeks). Survives service restarts.

---

## Compensating Transactions

**The hardest part of sagas:** Some actions can't be undone.

**Examples:**
- ❌ Can't un-send an email
- ❌ Can't un-charge a credit card (must issue refund)
- ❌ Can't un-ship a physical book

**Compensating transaction:** Action that semantically undoes a previous action.

### Email Compensation

**Can't un-send email. Compensation: Send cancellation email.**

```java
private void compensateNotification(String sagaId) {
    SagaState saga = sagaRepository.findById(sagaId);

    // Send cancellation email
    emailService.send(
        saga.getPatronEmail(),
        "Checkout Cancelled",
        "Your book checkout was cancelled due to a system error. " +
        "The book has been returned to AVAILABLE status."
    );
}
```

### Payment Compensation

**Can't un-charge. Compensation: Issue refund.**

```java
private void compensatePayment(String sagaId) {
    Payment payment = paymentRepository.findBySagaId(sagaId);

    // Issue refund
    paymentGateway.refund(
        payment.getTransactionId(),
        payment.getAmount(),
        "Checkout saga rollback"
    );

    // Record refund
    Refund refund = new Refund(
        sagaId,
        payment.getTransactionId(),
        payment.getAmount(),
        Instant.now()
    );
    refundRepository.save(refund);
}
```

**Key principle:** Compensations don't restore exact previous state. They **semantically undo** the effect.

---

## Event Sourcing (Advanced)

**From Section 9, we covered basics. Now: production patterns.**

### Event Store Schema

```sql
CREATE TABLE event_store (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INT NOT NULL,
    event_data JSONB NOT NULL,
    metadata JSONB,
    timestamp TIMESTAMPTZ DEFAULT NOW(),

    -- Optimistic concurrency control
    CONSTRAINT unique_version_per_aggregate
        UNIQUE (aggregate_id, event_version)
);

CREATE INDEX idx_aggregate ON event_store(aggregate_id, event_version);
CREATE INDEX idx_type ON event_store(aggregate_type, timestamp);
```

### Appending Events (with optimistic locking)

```java
@Service
public class EventStore {

    public void append(DomainEvent event, int expectedVersion) {
        try {
            jdbcTemplate.update(
                """
                INSERT INTO event_store
                (event_id, aggregate_id, aggregate_type, event_type,
                 event_version, event_data, metadata)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
                """,
                event.getEventId(),
                event.getAggregateId(),
                event.getAggregateType(),
                event.getEventType(),
                expectedVersion + 1,
                toJson(event.getData()),
                toJson(event.getMetadata())
            );
        } catch (DuplicateKeyException e) {
            // Concurrent modification detected
            throw new ConcurrentModificationException(
                "Expected version " + expectedVersion +
                " but aggregate was modified concurrently"
            );
        }
    }
}
```

### Rebuilding Aggregate

```java
public BookAggregate rebuild(UUID bookId) {
    List<DomainEvent> events = eventStore.getEvents(bookId);

    BookAggregate book = new BookAggregate(bookId);
    for (DomainEvent event : events) {
        book.apply(event);
    }

    return book;
}
```

### Snapshots (Performance Optimization)

**Problem:** Rebuilding from 10,000 events is slow.

**Solution:** Snapshot every N events (e.g., every 100).

```sql
CREATE TABLE snapshots (
    aggregate_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100),
    version INT NOT NULL,
    snapshot_data JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

```java
public BookAggregate rebuildWithSnapshot(UUID bookId) {
    // Load latest snapshot
    Snapshot snapshot = snapshotRepository.findByAggregateId(bookId);

    BookAggregate book;
    int fromVersion;

    if (snapshot != null) {
        // Start from snapshot
        book = deserialize(snapshot.getData());
        fromVersion = snapshot.getVersion() + 1;
    } else {
        // Start from scratch
        book = new BookAggregate(bookId);
        fromVersion = 0;
    }

    // Apply events after snapshot
    List<DomainEvent> events = eventStore.getEventsSince(bookId, fromVersion);
    for (DomainEvent event : events) {
        book.apply(event);
    }

    // Create new snapshot if needed (every 100 events)
    if (events.size() > 100) {
        snapshotRepository.save(new Snapshot(
            book.getId(),
            book.getVersion(),
            serialize(book)
        ));
    }

    return book;
}
```

---

## Real-World War Story: Amazon's Order Fulfillment

**Challenge:** Order fulfillment involves 50+ steps across 20+ services (inventory, payment, shipping, warehouse, notifications, fraud detection).

**2002-2008:** Used distributed transactions (2PC). **Constant failures.**

**2009:** Migrated to saga pattern.
- Each step publishes events
- Compensations for every step
- Process manager tracks entire order lifecycle

**Result:**
- Order success rate: 99.999%
- Average compensation rate: 0.01% (rare, but handled gracefully)
- Can process millions of orders/day

**Lesson:** Sagas are hard but necessary for complex distributed workflows.

---

## Action Items

**For Bibby:**

1. **Implement choreography saga** (4 hours)
   - Book checkout → email → search update
   - Use Kafka for events
   - Add compensation handlers

2. **Add idempotency keys** (2 hours)
   - IdempotencyRecord table
   - Check before processing events
   - Test with duplicate events

3. **Track saga state** (2 hours)
   - SagaState table
   - Monitor saga completion rates
   - Alert on stuck sagas

**For your project:**

1. **Identify multi-service workflows** - Which flows span services?
2. **Choose pattern** - Choreography (simple) or orchestration (complex)?
3. **Design compensations** - How to undo each step?
4. **Add idempotency** - All event handlers must be idempotent
5. **Monitor sagas** - Track success/failure rates

---

## Further Reading

- **"Designing Data-Intensive Applications"** by Martin Kleppmann (Chapter 9)
- **Saga pattern:** https://microservices.io/patterns/data/saga.html
- **Event Sourcing:** https://martinfowler.com/eaaDev/EventSourcing.html
- **Temporal.io:** Workflow orchestration platform
- **Axon Framework:** Event sourcing + CQRS framework

---

**Word count:** ~4,000 words
