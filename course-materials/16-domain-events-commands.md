# Week 16: Domain Events & Commands

**Course 4: Domain-Driven Design (DDD) — Week 16 of 26**

---

## The $6M Coupling Disaster

In 2018, a major e-commerce company decided to add a loyalty points program to their platform. Simple requirement: "When an order is completed, award points to the customer."

The developers made what seemed like a reasonable choice—they modified the `Order` class:

```java
public class Order {
    public void complete() {
        this.status = OrderStatus.COMPLETED;

        // Direct call to loyalty system
        loyaltyService.awardPoints(this.customerId, this.calculatePoints());

        // Direct call to inventory system
        inventoryService.reserveStock(this.items);

        // Direct call to shipping system
        shippingService.createShipment(this.shippingAddress, this.items);

        // Direct call to analytics
        analyticsService.trackOrderCompletion(this);
    }
}
```

**What could go wrong?**

Everything.

**Month 1:** The loyalty service had a database deadlock. Orders couldn't complete. Revenue stopped.

**Month 3:** Marketing wanted to send email notifications when orders complete. Developers added another line to `Order.complete()`. Now orders depend on the email service being available.

**Month 6:** A new regulation required archiving completed orders to a compliance system. Another dependency added to `Order.complete()`.

**Month 9:** The `Order.complete()` method had **17 direct dependencies**. If any one failed, orders couldn't complete. The method took 4+ seconds to execute. Deployment required coordinating 8 teams.

**Month 12:** The company abandoned the monolith and started a $6M rewrite to microservices.

**The problem:** **Tight coupling through direct method calls.** Every time something needed to react to an order completion, developers modified the `Order` class. The core order domain became entangled with loyalty, shipping, inventory, email, analytics, compliance...

**The solution:** **Domain Events.**

Instead of `Order` calling everyone directly, it **publishes an event**: "OrderCompleted." Other systems **subscribe** to that event and react independently. The order domain knows nothing about loyalty, shipping, or email.

**Decoupled. Scalable. Extensible.**

Welcome to **Week 16: Domain Events & Commands**.

---

## Core Concept 1: Domain Events

A **Domain Event** is a record of something significant that happened in the domain. It's a **fact**—something that already occurred.

### Characteristics of Domain Events

1. **Named in past tense** — "OrderCompleted," "PaymentReceived," "TankFilledToCapacity"
2. **Immutable** — Events describe history, which can't be changed
3. **Include relevant data** — Enough information for subscribers to react
4. **Timestamped** — When did this happen?
5. **Sourced from aggregates** — Events are published when aggregates change state

### Examples from Different Domains

**E-commerce:**
- `OrderPlaced`, `OrderCompleted`, `OrderCancelled`
- `PaymentReceived`, `PaymentFailed`
- `ItemShipped`, `ItemDelivered`

**Petroleum Terminal:**
- `LoadOrderScheduled`, `LoadingStarted`, `LoadingCompleted`
- `TankLevelChanged`, `ProductReceived`, `ProductWithdrawn`
- `SafetyIncidentReported`, `EmergencyShutdownTriggered`

**Healthcare:**
- `PatientAdmitted`, `PatientDischarged`
- `PrescriptionIssued`, `MedicationAdministered`
- `LabResultsAvailable`, `AppointmentScheduled`

**Banking:**
- `AccountOpened`, `DepositMade`, `WithdrawalMade`
- `LoanApproved`, `LoanFunded`, `PaymentReceived`
- `OverdraftDetected`, `FraudAlertTriggered`

### Domain Events in Code

**Basic structure:**

```java
public class LoadingCompleted {
    private final LoadingOperationId loadingOperationId;
    private final LoadOrderId loadOrderId;
    private final Volume quantityLoaded;
    private final Product product;
    private final Instant completedAt;
    private final Instant occurredAt;  // Event timestamp

    public LoadingCompleted(
        LoadingOperationId loadingOperationId,
        LoadOrderId loadOrderId,
        Volume quantityLoaded,
        Product product,
        Instant completedAt
    ) {
        this.loadingOperationId = loadingOperationId;
        this.loadOrderId = loadOrderId;
        this.quantityLoaded = quantityLoaded;
        this.product = product;
        this.completedAt = completedAt;
        this.occurredAt = Instant.now();  // When event was created
    }

    // Only getters, no setters (immutable)
    public LoadingOperationId getLoadingOperationId() { return loadingOperationId; }
    public LoadOrderId getLoadOrderId() { return loadOrderId; }
    public Volume getQuantityLoaded() { return quantityLoaded; }
    public Product getProduct() { return product; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getOccurredAt() { return occurredAt; }
}
```

**Notice:**
- All fields are `final` (immutable)
- Past tense name (`LoadingCompleted`, not `LoadingComplete` or `CompleteLoading`)
- Includes enough data for subscribers to react (operation ID, order ID, quantity, product)
- Timestamp of when it occurred

### How Aggregates Publish Events

**Week 15** showed you aggregates. Now they'll publish events when they change state:

```java
public class LoadingOperation {  // Aggregate Root
    private final LoadingOperationId id;
    private LoadingOperationStatus status;
    private Volume quantityLoaded;
    private List<DomainEvent> domainEvents = new ArrayList<>();  // Event collection

    public CarrierTicket complete(Instant completionTime) {
        if (this.status != LoadingOperationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete operations in progress");
        }

        this.status = LoadingOperationStatus.COMPLETED;

        // Create and record the domain event
        domainEvents.add(new LoadingCompleted(
            this.id,
            this.loadOrderId,
            this.quantityLoaded,
            this.product,
            completionTime
        ));

        return generateCarrierTicket();
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

**When you save the aggregate**, the repository publishes the events:

```java
public class LoadingOperationRepository {
    private final EntityManager entityManager;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public void save(LoadingOperation operation) {
        entityManager.persist(operation);  // Save to database

        // Publish all domain events
        operation.getDomainEvents().forEach(eventPublisher::publish);

        operation.clearDomainEvents();  // Clear after publishing
    }
}
```

### Event Subscribers: Reacting to Events

Other parts of the system **subscribe** to events and react:

```java
@Component
public class InventoryEventHandler {

    private final InventoryService inventoryService;

    @EventListener
    public void handleLoadingCompleted(LoadingCompleted event) {
        // When loading completes, update tank levels
        inventoryService.recordWithdrawal(
            event.getProduct(),
            event.getQuantityLoaded(),
            event.getCompletedAt()
        );
    }
}
```

```java
@Component
public class BillingEventHandler {

    private final BillingService billingService;

    @EventListener
    public void handleLoadingCompleted(LoadingCompleted event) {
        // When loading completes, create invoice
        billingService.createInvoice(
            event.getLoadOrderId(),
            event.getQuantityLoaded(),
            event.getProduct()
        );
    }
}
```

```java
@Component
public class ComplianceEventHandler {

    private final ComplianceArchive complianceArchive;

    @EventListener
    public void handleLoadingCompleted(LoadingCompleted event) {
        // When loading completes, archive for regulatory compliance
        complianceArchive.recordTransaction(
            event.getLoadingOperationId(),
            event.getProduct(),
            event.getQuantityLoaded(),
            event.getCompletedAt()
        );
    }
}
```

**Notice what changed:**

**Before (tight coupling):**
```java
public void complete() {
    this.status = COMPLETED;
    inventoryService.update(...);  // Direct dependency
    billingService.invoice(...);   // Direct dependency
    complianceArchive.record(...); // Direct dependency
}
```

**After (decoupled):**
```java
public void complete() {
    this.status = COMPLETED;
    domainEvents.add(new LoadingCompleted(...));  // Just record the event
}
```

**The aggregate doesn't know or care who's listening.** Subscribers can be added, removed, or changed without touching the aggregate.

### Benefits of Domain Events

1. **Decoupling** — Aggregates don't depend on other systems
2. **Extensibility** — Add new subscribers without changing existing code
3. **Auditability** — Events are a complete history of what happened
4. **Scalability** — Subscribers can process events asynchronously
5. **Integration** — Different bounded contexts communicate via events
6. **Testing** — Test aggregates without worrying about side effects

---

## Core Concept 2: Commands

A **Command** is a request to change the system's state. It represents **intent**—something someone wants to happen.

### Characteristics of Commands

1. **Named in imperative mood** — "ScheduleLoadOrder," "CompleteLoading," "ApproveLoad"
2. **Can be rejected** — Unlike events (which are facts), commands can fail
3. **Include intent and parameters** — What do you want to do, and with what data?
4. **Directed to an aggregate** — Commands are handled by specific aggregates
5. **May result in events** — Successful command → state change → event published

### Command Examples

**Petroleum Terminal:**
- `ScheduleLoadOrder`, `CancelLoadOrder`, `RescheduleLoadOrder`
- `BeginLoading`, `CompleteLoading`, `AbortLoading`
- `ReceiveProductToTank`, `TransferProductBetweenTanks`

**E-commerce:**
- `PlaceOrder`, `CancelOrder`, `UpdateShippingAddress`
- `AddItemToCart`, `RemoveItemFromCart`, `ApplyCoupon`
- `ProcessPayment`, `RefundPayment`

**Healthcare:**
- `ScheduleAppointment`, `CancelAppointment`, `RescheduleAppointment`
- `AdmitPatient`, `DischargePatient`
- `PrescribeMedication`, `AdministerMedication`

### Commands in Code

```java
public class CompleteLoading {
    private final LoadingOperationId loadingOperationId;
    private final Instant completionTime;
    private final Volume finalQuantity;

    public CompleteLoading(
        LoadingOperationId loadingOperationId,
        Instant completionTime,
        Volume finalQuantity
    ) {
        this.loadingOperationId = Objects.requireNonNull(loadingOperationId);
        this.completionTime = Objects.requireNonNull(completionTime);
        this.finalQuantity = Objects.requireNonNull(finalQuantity);
    }

    // Getters
    public LoadingOperationId getLoadingOperationId() { return loadingOperationId; }
    public Instant getCompletionTime() { return completionTime; }
    public Volume getFinalQuantity() { return finalQuantity; }
}
```

### Command Handlers

**Commands are handled by application services** that load aggregates, execute the command, and save results:

```java
@Service
public class LoadingOperationCommandHandler {

    private final LoadingOperationRepository repository;

    @Transactional
    public void handle(CompleteLoading command) {
        // Load the aggregate
        LoadingOperation operation = repository.findById(
            command.getLoadingOperationId()
        ).orElseThrow(() -> new LoadingOperationNotFoundException());

        // Execute the command on the aggregate
        operation.complete(command.getCompletionTime());

        // Save the aggregate (which publishes events)
        repository.save(operation);
    }
}
```

### Command → Aggregate → Event Flow

**Complete flow:**

1. **User intent** → Create command
2. **Command handler** → Load aggregate
3. **Aggregate** → Validate and change state
4. **Aggregate** → Record domain event
5. **Repository** → Save aggregate to database
6. **Repository** → Publish domain events
7. **Event subscribers** → React to events

**Example:**

```java
// 1. User intent (from API controller)
CompleteLoading command = new CompleteLoading(
    loadingOperationId,
    Instant.now(),
    new Volume(new BigDecimal("5000"))
);

// 2. Command handler loads aggregate
LoadingOperation operation = repository.findById(command.getLoadingOperationId());

// 3. Aggregate validates and changes state
operation.complete(command.getCompletionTime());  // Throws exception if invalid

// 4. Aggregate records event
// (inside operation.complete():)
//   domainEvents.add(new LoadingCompleted(...))

// 5. Repository saves aggregate
repository.save(operation);

// 6. Repository publishes events
// (inside repository.save():)
//   operation.getDomainEvents().forEach(eventPublisher::publish)

// 7. Event subscribers react
// InventoryEventHandler.handleLoadingCompleted(event)
// BillingEventHandler.handleLoadingCompleted(event)
// ComplianceEventHandler.handleLoadingCompleted(event)
```

**Notice:** The command handler is thin—it just orchestrates. The **aggregate** contains the business logic.

---

## Integrating Bounded Contexts with Events

From **Week 14**, you learned about Bounded Contexts. **Events are how contexts integrate.**

### Context Integration Pattern

**Example: Petroleum Terminal System**

**Scheduling Context** (upstream) publishes events:
- `LoadOrderScheduled`
- `LoadOrderCancelled`
- `LoadOrderRescheduled`

**Operations Context** (downstream) subscribes:
- When `LoadOrderScheduled` → Create work assignment for terminal operators
- When `LoadOrderCancelled` → Remove work assignment
- When `LoadOrderRescheduled` → Update assignment time

**Operations Context** (now upstream) publishes events:
- `LoadingCompleted`
- `LoadingAborted`

**Inventory Context** (downstream) subscribes:
- When `LoadingCompleted` → Update tank levels
- When `LoadingAborted` → Log discrepancy

**Billing Context** (downstream) subscribes:
- When `LoadingCompleted` → Create invoice

```
┌─────────────────┐
│   Scheduling    │ ─┐
│    Context      │  │ LoadOrderScheduled
└─────────────────┘  │
                     ↓
                ┌─────────────────┐
                │   Operations    │ ─┐
                │    Context      │  │ LoadingCompleted
                └─────────────────┘  │
                     ↓               ↓
         ┌───────────────────┬───────────────────┐
         ↓                   ↓                   ↓
  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
  │  Inventory  │   │   Billing   │   │ Compliance  │
  │   Context   │   │   Context   │   │   Context   │
  └─────────────┘   └─────────────┘   └─────────────┘
```

### Event Translation at Context Boundaries

Sometimes the **internal event** from one context needs to be **translated** for consumption by another context.

**Operations Context (internal event):**
```java
public class LoadingOperationCompleted {
    private LoadingOperationId operationId;
    private Bay bay;
    private MeterReadings readings;
    private CarrierTicket ticket;
    // Lots of operational details...
}
```

**Public event for other contexts:**
```java
public class ProductWithdrawn {  // Inventory Context's perspective
    private TankId tankId;
    private Product product;
    private Volume quantity;
    private Instant timestamp;
    // Only what Inventory Context needs
}
```

**Event translation:**

```java
@Component
public class OperationsToInventoryEventTranslator {

    @EventListener
    @Async
    public void translateLoadingCompleted(LoadingOperationCompleted event) {
        // Translate from Operations model to Inventory model
        ProductWithdrawn inventoryEvent = new ProductWithdrawn(
            event.getSourceTankId(),
            event.getProduct(),
            event.getQuantityLoaded(),
            event.getCompletedAt()
        );

        eventPublisher.publish(inventoryEvent);
    }
}
```

This **Anti-Corruption Layer** (Week 14) prevents the internal model of one context from leaking into another.

---

## Eventual Consistency vs Immediate Consistency

### Immediate Consistency (Traditional)

**Everything happens in one transaction:**

```java
@Transactional
public void completeOrder(OrderId orderId) {
    Order order = orderRepository.find(orderId);
    order.complete();
    orderRepository.save(order);

    // All in same transaction
    loyaltyService.awardPoints(order.getCustomerId(), order.getPoints());
    inventoryService.decrementStock(order.getItems());
    shippingService.createShipment(order);
}
```

**Pros:**
- ✅ Immediate consistency—everything updates at once
- ✅ Easy to reason about

**Cons:**
- ❌ Tight coupling (all services must be available)
- ❌ Long transactions (higher chance of deadlocks)
- ❌ Slow (sequential operations)
- ❌ Hard to scale

### Eventual Consistency (Event-Driven)

**Core action in one transaction, side effects happen asynchronously:**

```java
@Transactional
public void completeOrder(OrderId orderId) {
    Order order = orderRepository.find(orderId);
    order.complete();  // Changes state, records event
    orderRepository.save(order);  // Publishes OrderCompleted event
}

// Separate handlers (possibly in different services)
@EventListener
@Async
public void handleOrderCompleted(OrderCompleted event) {
    loyaltyService.awardPoints(event.getCustomerId(), event.getPoints());
}

@EventListener
@Async
public void handleOrderCompleted(OrderCompleted event) {
    inventoryService.decrementStock(event.getItems());
}
```

**Pros:**
- ✅ Decoupled (services are independent)
- ✅ Fast (main operation completes immediately)
- ✅ Resilient (failures in side effects don't block the main flow)
- ✅ Scalable (subscribers can process events in parallel)

**Cons:**
- ❌ Eventual consistency—there's a delay before everything is consistent
- ❌ Harder to reason about (distributed state)
- ❌ Requires handling failures and retries

### When to Use Each

**Use Immediate Consistency when:**
- The business requires **all changes together or none** (true ACID transaction)
- Example: Transferring money between two bank accounts (must debit and credit atomically)

**Use Eventual Consistency when:**
- Side effects are **secondary** to the main action
- Delays are acceptable
- You need **scalability and decoupling**
- Example: Awarding loyalty points after order (nice to have, but not critical to order completion)

### Handling Failures in Eventual Consistency

**What if an event handler fails?**

**Option 1: Retry**
```java
@EventListener
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
public void handleLoadingCompleted(LoadingCompleted event) {
    inventoryService.updateTankLevel(event.getTankId(), event.getQuantity());
}
```

**Option 2: Dead Letter Queue**
- If event processing fails after retries, move to a dead letter queue for manual review

**Option 3: Compensating Action**
- If you can't undo an event, emit a **compensating event**
- Example: `LoadingCompleted` → `LoadingCorrected` (if a mistake was found)

---

## Practical Example: Complete Flow

**Scenario:** Schedule and complete a load order at a petroleum terminal.

### Step 1: Command to Schedule

```java
ScheduleLoadOrder command = new ScheduleLoadOrder(
    customerId,
    product,
    new Volume(new BigDecimal("5000")),
    LocalDateTime.of(2024, 1, 15, 14, 0)
);

loadOrderCommandHandler.handle(command);
```

### Step 2: Aggregate Handles Command

```java
@Service
public class LoadOrderCommandHandler {
    @Transactional
    public LoadOrderId handle(ScheduleLoadOrder command) {
        LoadOrder order = LoadOrder.schedule(
            command.getCustomerId(),
            command.getProduct(),
            command.getQuantity(),
            command.getRequestedTime()
        );

        loadOrderRepository.save(order);  // Publishes LoadOrderScheduled event
        return order.getId();
    }
}
```

### Step 3: Aggregate Records Event

```java
public class LoadOrder {
    public static LoadOrder schedule(...) {
        LoadOrder order = new LoadOrder(...);
        order.domainEvents.add(new LoadOrderScheduled(
            order.getId(),
            order.getCustomerId(),
            order.getProduct(),
            order.getQuantity(),
            order.getRequestedTime()
        ));
        return order;
    }
}
```

### Step 4: Event Published

```java
public class LoadOrderRepository {
    @Transactional
    public void save(LoadOrder order) {
        entityManager.persist(order);
        order.getDomainEvents().forEach(eventPublisher::publish);
        order.clearDomainEvents();
    }
}
```

### Step 5: Subscribers React

```java
@Component
public class OperationsEventHandler {
    @EventListener
    @Async
    public void handleLoadOrderScheduled(LoadOrderScheduled event) {
        // Create work assignment for terminal operators
        WorkAssignment assignment = new WorkAssignment(
            event.getLoadOrderId(),
            event.getProduct(),
            event.getRequestedTime()
        );
        workAssignmentRepository.save(assignment);
    }
}

@Component
public class CapacityPlanningHandler {
    @EventListener
    @Async
    public void handleLoadOrderScheduled(LoadOrderScheduled event) {
        // Update capacity forecasts
        capacityPlanner.recordScheduledLoad(
            event.getProduct(),
            event.getQuantity(),
            event.getRequestedTime()
        );
    }
}
```

### Step 6: Complete the Loading

```java
CompleteLoading command = new CompleteLoading(
    loadingOperationId,
    Instant.now(),
    new Volume(new BigDecimal("4987.5"))
);

loadingOperationCommandHandler.handle(command);
```

### Step 7: More Events Published

```java
public class LoadingOperation {
    public void complete(Instant completionTime) {
        this.status = LoadingOperationStatus.COMPLETED;
        this.domainEvents.add(new LoadingCompleted(
            this.id,
            this.loadOrderId,
            this.quantityLoaded,
            this.product,
            completionTime
        ));
    }
}
```

### Step 8: Multiple Contexts React

```java
// Inventory Context
@EventListener
public void handleLoadingCompleted(LoadingCompleted event) {
    inventoryService.recordWithdrawal(event.getProduct(), event.getQuantity());
}

// Billing Context
@EventListener
public void handleLoadingCompleted(LoadingCompleted event) {
    billingService.createInvoice(event.getLoadOrderId(), event.getQuantity());
}

// Compliance Context
@EventListener
public void handleLoadingCompleted(LoadingCompleted event) {
    complianceArchive.record(event);
}

// Analytics Context
@EventListener
public void handleLoadingCompleted(LoadingCompleted event) {
    analyticsService.trackThroughput(event.getProduct(), event.getQuantity());
}
```

**Result:** The core domain (LoadOrder, LoadingOperation) is completely decoupled from inventory, billing, compliance, and analytics.

---

## Hands-On Exercise: Design Events and Commands

**Use the same domain from Week 14-15** (petroleum terminal, hospital, e-commerce, property management).

### Part 1: Identify Domain Events (45 minutes)

1. **List 10-15 significant events** that occur in your domain
2. **Name them in past tense** (what happened?)
3. **For each event, specify:**
   - What aggregate publishes it?
   - What data does it include?
   - Who might subscribe to it? (other contexts, other aggregates)

**Deliverable format:**

```markdown
## Domain Events

**Event:** LoadOrderScheduled
- **Source Aggregate:** LoadOrder
- **Data Included:** LoadOrderId, CustomerId, Product, RequestedQuantity, RequestedTime, ScheduledAt
- **Potential Subscribers:**
  - Operations Context (create work assignment)
  - Capacity Planning (update forecasts)
  - Customer Notifications (send confirmation email)
```

### Part 2: Identify Commands (30 minutes)

1. **List 8-12 commands** that users/systems can issue
2. **Name them in imperative mood** (do this!)
3. **For each command, specify:**
   - What aggregate handles it?
   - What parameters does it require?
   - What event(s) might result from successful execution?

**Deliverable format:**

```markdown
## Commands

**Command:** ScheduleLoadOrder
- **Target Aggregate:** LoadOrder
- **Parameters:** CustomerId, Product, RequestedQuantity, RequestedTime
- **Resulting Event(s):** LoadOrderScheduled (on success)
- **Possible Failures:** InsufficientCapacity, InvalidProduct, InvalidTime
```

### Part 3: Design Event Flow (60 minutes)

1. **Pick one end-to-end scenario** from your domain
2. **Map the complete flow:**
   - Initial command
   - Aggregate that handles it
   - Event published
   - Subscribers that react
   - Any subsequent commands/events

**Deliverable:** Flow diagram showing:
```
[User] → [Command] → [Aggregate] → [Event] → [Subscriber 1]
                                           → [Subscriber 2]
                                           → [Subscriber 3]
```

### Part 4: Implement Events and Commands in Java (90 minutes)

1. **Implement 3 domain events** as immutable Java classes
2. **Implement 3 commands** as Java classes
3. **Modify one aggregate from Week 15** to:
   - Accept commands via methods
   - Record domain events
   - Provide `getDomainEvents()` and `clearDomainEvents()`
4. **Implement 2 event handlers** that react to the events

**Deliverable:** Working Java code with tests.

---

## Reflection Questions

1. **Think about systems you've used.** Can you identify tight coupling where one action directly calls many others? What problems did that create?

2. **Events vs Commands:** Why is it important that events are immutable facts (past tense) while commands are mutable requests (imperative)?

3. **Eventual consistency:** In your domain, what operations require immediate consistency (everything together or nothing)? What operations can tolerate eventual consistency?

4. **Event naming:** Why does naming matter? How does "LoadingCompleted" (event) differ from "CompleteLoading" (command) in meaning and usage?

5. **From your operational experience:** What events were critical to track? What happened when those events weren't properly recorded or communicated?

---

## Key Takeaways

✅ **Domain Events are facts** — Immutable records of significant things that happened. Named in past tense.

✅ **Commands are requests** — Can be rejected. Named in imperative mood. Directed to aggregates.

✅ **Events decouple** — Aggregates publish events without knowing who's listening. Subscribers react independently.

✅ **Command → Aggregate → Event** — Commands change state, aggregates validate and record events, repositories publish events.

✅ **Bounded Contexts integrate via events** — Each context publishes its events, other contexts subscribe as needed.

✅ **Eventual consistency enables scale** — Core operations complete quickly, side effects happen asynchronously.

✅ **Events create audit trails** — Complete history of what happened in the domain.

✅ **Anti-Corruption Layers translate events** — Internal events stay internal, public events cross context boundaries.

---

## Connection to Week 17

This week you learned **Domain Events & Commands**—how domain objects communicate changes and how different parts of the system react.

Next week you'll learn **Event Storming & Domain Modeling**—a collaborative workshop technique for discovering events, commands, aggregates, and bounded contexts with domain experts.

You'll learn:
- How to run Event Storming workshops
- How to discover the domain model collaboratively
- How to identify aggregates, events, commands, policies, and read models
- How to translate workshop outputs into executable domain models
- How to involve business experts in domain modeling

**Get ready to discover your domain model with sticky notes.**

---

## Additional Resources

**Books:**
- *Domain-Driven Design* by Eric Evans — Chapter on Domain Events
- *Implementing Domain-Driven Design* by Vaughn Vernon — Chapter 8 (Domain Events)
- *Versioning in an Event Sourced System* by Greg Young

**Articles:**
- Martin Fowler: "Event Sourcing" (martinfowler.com/eaaDev/EventSourcing.html)
- Udi Dahan: "Domain Events - Salvation"

**For Spring Boot:**
- Spring Framework `@EventListener` annotation
- Spring Data's `@DomainEvents` and `@AfterDomainEventPublication`
- Spring Integration / Spring Cloud Stream for distributed events

**Event Stores:**
- EventStoreDB (purpose-built event store)
- Kafka (distributed event streaming)
- RabbitMQ / ActiveMQ (message queues)

---

**End of Week 16 — Domain Events & Commands**

**Next:** Week 17 — Event Storming & Domain Modeling
