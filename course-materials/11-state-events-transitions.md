# Week 11: State, Events & Transitions
## Modeling Entity Lifecycles and Temporal Requirements That Most Engineers Miss

---

## Introduction: The $1.2 Million Order That Couldn't Be Cancelled

An e-commerce company built a sophisticated order management system. Clean code. Microservices. Event-driven architecture. The team was proud.

**Then this happened:**

A customer placed a $1.2 million order (bulk industrial equipment). 30 minutes later, they called: "We made a mistake. Cancel that order."

**Customer service rep:** "Let me cancel it for you."

**System response:** "Error: Cannot cancel order in 'Pending Payment' state."

**Rep tries workaround:** Changes status to 'Cancelled' directly in database.

**What happened next:**
- Payment processor still charged the card (system didn't notify payment service of cancellation)
- Warehouse received pick list and started pulling items
- Shipping label generated and sent to logistics partner
- Customer got charged $1.2M for order they thought was cancelled
- Warehouse shipped $800K of product before anyone noticed

**Root cause:** The developers never modeled the **order lifecycle** properly.

**What was missing:**

```
Valid State Transitions (What SHOULD have been designed):

PENDING_PAYMENT → CANCELLED (before payment processes)
PAYMENT_PROCESSING → cannot cancel (payment in flight, must complete)
PAID → CANCELLATION_REQUESTED (requires refund approval)
PAID → CANCELLED (only if refund approved AND inventory not allocated)
ALLOCATED → RETURN_INITIATED (product pulled but not shipped)
SHIPPED → cannot cancel (must process as return)
```

**Instead, they built:**
- Status field with no transition rules
- Anyone could change status to anything
- No validation of state transitions
- No cascading actions when state changes
- No invariants enforcing business rules

**This week, you'll learn to model state, events, and transitions** so your systems enforce valid lifecycles and prevent chaos.

---

## Core Concept 1: State Machines and Lifecycle Modeling

### What Is a State?

**State:** A condition or situation during the lifecycle of an entity that:
- Determines what operations are valid
- Represents progress toward a goal
- Has specific properties that must be true (invariants)
- Persists for some duration

**Examples:**

**Order states:**
- Draft, Submitted, Payment Processing, Paid, Allocated, Picked, Shipped, Delivered, Cancelled, Returned

**Loan states:**
- Applied, Under Review, Approved, Funded, Active, Paid Off, Defaulted

**Equipment states:**
- Available, Reserved, In Use, Maintenance, Out of Service, Decommissioned

**Patient states:**
- Scheduled, Checked In, In Exam, Diagnosed, Treatment, Discharged

### State Machine Components

**1. States (Nodes)**
- Discrete conditions the entity can be in
- Mutually exclusive (can only be in one state at a time)

**2. Events (Triggers)**
- Actions or occurrences that cause transitions
- External (user action) or internal (system action)

**3. Transitions (Edges)**
- Movement from one state to another
- Triggered by specific events
- May have conditions (guards)
- May cause actions (side effects)

**4. Initial State**
- Where the entity starts its lifecycle

**5. Final State(s)**
- Terminal states (no further transitions)

### Simple State Machine Example: Light Switch

```
          ┌─────────────┐
   (ON)   │             │  (OFF)
 ┌────────│     OFF     │◄──────────┐
 │        │             │           │
 │        └─────────────┘           │
 │                                  │
 │        ┌─────────────┐           │
 │        │             │           │
 └───────►│     ON      │───────────┘
          │             │
          └─────────────┘

States: OFF, ON
Events: Switch Flipped
Transitions: OFF → ON (when switch flipped)
             ON → OFF (when switch flipped)
```

Simple toggle behavior.

### Complex State Machine: Order Processing

```
                    ┌──────────┐
              START │          │
                    └────┬─────┘
                         │
                         │ Create Order
                         ▼
                    ┌──────────┐
                    │  DRAFT   │◄──────┐
                    └────┬─────┘       │
                         │              │
                         │ Submit       │ Edit
                         ▼              │
                  ┌────────────┐        │
                  │ SUBMITTED  ├────────┘
                  └─────┬──────┘
                        │
                        │ Process Payment
                        ▼
                  ┌────────────┐
                  │  PAYMENT   │
                  │ PROCESSING │
                  └──┬─────┬───┘
                     │     │
        Payment      │     │ Payment
        Successful   │     │ Failed
                     │     │
                     ▼     ▼
              ┌──────┐   ┌──────────┐
              │ PAID │   │ PAYMENT  │
              └──┬───┘   │ FAILED   │
                 │       └────┬─────┘
                 │            │
    Allocate     │            │ Retry or Cancel
    Inventory    │            │
                 ▼            ▼
           ┌──────────┐  ┌───────────┐
           │ALLOCATED │  │ CANCELLED │ (FINAL)
           └────┬─────┘  └───────────┘
                │
                │ Pick Items
                ▼
           ┌──────────┐
           │  PICKED  │
           └────┬─────┘
                │
                │ Ship
                ▼
           ┌──────────┐
           │ SHIPPED  │
           └────┬─────┘
                │
                │ Confirm Delivery
                ▼
           ┌──────────┐
           │DELIVERED │ (FINAL)
           └──────────┘
```

**Key characteristics:**
- Multiple paths (success and failure)
- Terminal states (CANCELLED, DELIVERED)
- Guards on transitions (can't ship without picking)
- Side effects (allocate inventory, send emails)

---

## Core Concept 2: Events That Trigger Transitions

### Types of Events

**1. User Actions**
- Customer submits order
- Manager approves request
- Operator confirms completion

**2. System Actions**
- Payment processor returns response
- Scheduled job runs
- Timeout expires

**3. External System Events**
- Shipment tracking update received
- Bank notification of payment
- Third-party API callback

**4. Time-Based Events**
- 24 hours elapsed
- Deadline reached
- Scheduled time arrives

### Event Modeling Format

```
EVENT: [EventName]
Source: [User | System | External]
Trigger: [What causes this event]
Data Payload: [Information carried with event]
Applicable States: [Which states can receive this event]
Resulting Transition: [State → State]
Side Effects: [Actions triggered]
```

### Example: Order Events

**EVENT: OrderSubmitted**
```
Source: User (customer clicks "Place Order")
Trigger: Customer completes checkout and confirms
Data Payload:
  - order_id
  - customer_id
  - items[]
  - total_amount
  - payment_method
Applicable States: DRAFT
Resulting Transition: DRAFT → SUBMITTED
Side Effects:
  - Validate inventory availability
  - Initiate payment processing
  - Send order confirmation email
```

**EVENT: PaymentCompleted**
```
Source: External (payment processor callback)
Trigger: Payment successfully processed
Data Payload:
  - order_id
  - transaction_id
  - amount_charged
  - timestamp
Applicable States: PAYMENT_PROCESSING
Resulting Transition: PAYMENT_PROCESSING → PAID
Side Effects:
  - Record transaction
  - Trigger inventory allocation
  - Notify warehouse
```

**EVENT: CancellationRequested**
```
Source: User (customer or CSR)
Trigger: Customer wants to cancel order
Data Payload:
  - order_id
  - reason
  - requested_by
Applicable States: DRAFT, SUBMITTED, PAID (before ALLOCATED)
Resulting Transition:
  - DRAFT → CANCELLED
  - SUBMITTED → CANCELLED (abort payment if in progress)
  - PAID → CANCELLATION_PENDING (requires refund approval)
Side Effects:
  - If PAID: Initiate refund process
  - If SUBMITTED: Cancel payment processing
  - Notify customer
```

**Notice:** Same event (CancellationRequested) behaves differently based on current state.

---

## Core Concept 3: Valid vs Invalid State Transitions

### Transition Rules

**Valid transitions** are explicitly defined and enforced.

**Invalid transitions** must be prevented.

### Example: Loan Application State Machine

**Valid Transitions:**
```
APPLIED → UNDER_REVIEW
UNDER_REVIEW → APPROVED
UNDER_REVIEW → DENIED
APPROVED → FUNDED
FUNDED → ACTIVE
ACTIVE → PAID_OFF
ACTIVE → DEFAULTED
```

**Invalid Transitions (MUST prevent):**
```
APPLIED → FUNDED (cannot fund without review)
DENIED → APPROVED (cannot approve after denying)
ACTIVE → UNDER_REVIEW (cannot go backwards)
PAID_OFF → ACTIVE (loan is closed)
```

### Implementing Transition Validation

**Pseudo-code:**

```java
public class Loan {
    private LoanState state;

    private static final Map<LoanState, Set<LoanState>> VALID_TRANSITIONS = Map.of(
        LoanState.APPLIED, Set.of(LoanState.UNDER_REVIEW),
        LoanState.UNDER_REVIEW, Set.of(LoanState.APPROVED, LoanState.DENIED),
        LoanState.APPROVED, Set.of(LoanState.FUNDED),
        LoanState.FUNDED, Set.of(LoanState.ACTIVE),
        LoanState.ACTIVE, Set.of(LoanState.PAID_OFF, LoanState.DEFAULTED),
        LoanState.PAID_OFF, Set.of(),  // Terminal state
        LoanState.DENIED, Set.of()     // Terminal state
    );

    public void transitionTo(LoanState newState) {
        if (!VALID_TRANSITIONS.get(this.state).contains(newState)) {
            throw new InvalidStateTransitionException(
                "Cannot transition from " + this.state + " to " + newState
            );
        }
        this.state = newState;
        // Publish state change event
        // Trigger side effects
    }
}
```

### Conditional Transitions (Guards)

Sometimes transitions have conditions beyond state:

**Example: Order Cancellation**

```
Event: CancellationRequested
Current State: ALLOCATED

Guard Condition:
  IF items_picked == false:
    TRANSITION → CANCELLED
    SIDE EFFECT: Deallocate inventory
  ELSE:
    TRANSITION → RETURN_INITIATED
    SIDE EFFECT: Create return authorization
```

**Another Example: Loan Approval**

```
Event: UnderwritingCompleted
Current State: UNDER_REVIEW

Guard Conditions:
  IF credit_score >= 700 AND debt_to_income < 0.4:
    TRANSITION → APPROVED
  ELSE:
    TRANSITION → DENIED
```

---

## Core Concept 4: State Invariants (What Must Always Be True)

### What Are Invariants?

**Invariant:** A condition that must always hold true when an entity is in a specific state.

**Purpose:** Protect data integrity and enforce business rules.

### Examples by State

**Order in SHIPPED state:**
- Invariant: `shipping_tracking_number` must be non-null
- Invariant: `shipped_date` must be set
- Invariant: `allocated_items` must match `shipped_items`
- Invariant: Payment must have been completed

**Loan in ACTIVE state:**
- Invariant: `funded_date` must be set
- Invariant: `interest_rate` must be > 0
- Invariant: `remaining_balance` <= `original_amount`
- Invariant: `next_payment_date` must be in the future

**Equipment in IN_USE state:**
- Invariant: `assigned_user` must be non-null
- Invariant: `checkout_time` must be set
- Invariant: `last_maintenance_date` < 90 days ago (safety rule)

### Enforcing Invariants

**Option 1: Validation at State Transition**

```java
public void ship(String trackingNumber) {
    if (this.state != OrderState.PICKED) {
        throw new InvalidOperationException("Cannot ship order in state: " + this.state);
    }

    if (trackingNumber == null || trackingNumber.isEmpty()) {
        throw new ValidationException("Tracking number required to ship");
    }

    this.trackingNumber = trackingNumber;
    this.shippedDate = LocalDateTime.now();
    this.transitionTo(OrderState.SHIPPED);

    // Invariants now satisfied for SHIPPED state
}
```

**Option 2: Defensive Checks in Getters**

```java
public String getTrackingNumber() {
    if (this.state == OrderState.SHIPPED && this.trackingNumber == null) {
        throw new IllegalStateException("Invariant violated: SHIPPED order must have tracking number");
    }
    return this.trackingNumber;
}
```

**Option 3: Database Constraints**

```sql
-- Prevent NULL tracking number for shipped orders
ALTER TABLE orders
ADD CONSTRAINT chk_shipped_has_tracking
CHECK (
    (status != 'SHIPPED')
    OR (status = 'SHIPPED' AND tracking_number IS NOT NULL)
);
```

---

## Core Concept 5: Temporal Requirements (Time-Based Behaviors)

### Types of Temporal Requirements

**1. Time-Based State Transitions**

**Example:** Auto-cancel unpaid orders after 24 hours

```
Event: TimeoutExpired
Current State: SUBMITTED
Condition: created_date + 24 hours < NOW
Transition: SUBMITTED → CANCELLED
Side Effect: Notify customer, release inventory hold
```

**2. State Duration Constraints**

**Example:** Loan cannot remain in UNDER_REVIEW for more than 7 business days

```
Requirement: If loan in UNDER_REVIEW for > 7 business days,
             escalate to senior underwriter
```

**3. Time-Based Validity**

**Example:** Approved loan must be funded within 30 days or approval expires

```
APPROVED state has expiration: approved_date + 30 days

IF NOW > approved_date + 30 days:
  TRANSITION → EXPIRED
```

**4. Scheduled State Changes**

**Example:** Subscription renewal

```
ACTIVE subscription reaches renewal_date:
  IF payment_method valid:
    Charge renewal fee
    Update expiration_date
    STAY in ACTIVE
  ELSE:
    TRANSITION → PAYMENT_FAILED
    Grace period begins
```

**5. Retroactive State** (Effective Dating)

**Example:** Price changes with effective dates

```
Product Price:
  - $100 effective 2024-01-01 to 2024-06-30
  - $120 effective 2024-07-01 to 2024-12-31
  - $110 effective 2025-01-01 onward

Order placed 2024-06-15:
  Use price = $100 (effective at order date)
```

### Temporal State Machine Example: Trial Subscription

```
               ┌──────────┐
        START  │          │
               └────┬─────┘
                    │
                    │ Sign Up
                    ▼
               ┌──────────┐
               │  TRIAL   │ (14 days)
               └────┬─────┘
                    │
                    │ 14 days elapsed OR upgrade
                    ▼
          ┌─────────────────────┐
          │                     │
     (upgrade)           (timeout)
          │                     │
          ▼                     ▼
    ┌──────────┐          ┌──────────┐
    │  ACTIVE  │          │ EXPIRED  │
    └────┬─────┘          └────┬─────┘
         │                     │
         │                     │ (upgrade)
         │                     │
         │                     │
         ▼                     ▼
    ┌──────────┐          ┌──────────┐
    │ PAYMENT  │◄─────────│          │
    │  FAILED  │          └──────────┘
    └────┬─────┘
         │
         │ Grace period (7 days)
         ▼
    ┌──────────┐
    │CANCELLED │ (FINAL)
    └──────────┘

Temporal Rules:
- TRIAL auto-expires after 14 days
- PAYMENT_FAILED enters 7-day grace period
- After grace period → CANCELLED
- ACTIVE subscription renews every 30 days
```

---

## Core Concept 6: Stateless vs Stateful Components

### Understanding the Distinction

**Stateless Component:**
- Doesn't remember previous interactions
- Each request is independent
- Can be replicated freely
- Easier to scale

**Stateful Component:**
- Remembers information between requests
- Session-dependent behavior
- Harder to scale (need session persistence)

### When State Matters

**Stateless Example: Price Calculator**

```java
public class PriceCalculator {
    public Money calculateTotal(List<LineItem> items, TaxRate taxRate) {
        Money subtotal = items.stream()
            .map(LineItem::getPrice)
            .reduce(Money.ZERO, Money::add);
        Money tax = subtotal.multiply(taxRate);
        return subtotal.add(tax);
    }
}
```

No state stored. Same inputs always produce same output.

**Stateful Example: Shopping Cart**

```java
public class ShoppingCart {
    private List<LineItem> items = new ArrayList<>();
    private Customer customer;

    public void addItem(Product product, int quantity) {
        items.add(new LineItem(product, quantity));
        // State changes: items list grows
    }

    public Money getTotal() {
        // Depends on current state (items added so far)
        return items.stream()
            .map(LineItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}
```

State accumulates. Behavior depends on history.

### Managing State in Distributed Systems

**Challenges:**
- User adds item to cart on Server A
- Next request routes to Server B
- Server B doesn't know about item in cart

**Solutions:**

**1. Sticky Sessions**
- Route all requests from same user to same server
- Downside: Load balancing suffers, server failure loses state

**2. Shared State Store**
- Store session data in Redis/Memcached
- All servers read/write to same store
- Downside: Network latency, single point of failure

**3. Client-Side State**
- Store state in browser (cookies, local storage)
- Send with each request
- Downside: Security risk, data size limits

**4. Event Sourcing**
- Store all state changes as events
- Rebuild current state by replaying events
- Downside: Complexity, eventual consistency

---

## Practical Framework: State Machine Design Process

### Step-by-Step Guide

**1. Identify the Entity**
- What thing has a lifecycle? (Order, Loan, Equipment, Patient)

**2. List Possible States**
- Brainstorm all conditions entity can be in
- Include terminal states (final states)

**3. Identify Events**
- What triggers transitions?
- User actions, system actions, external events, timeouts

**4. Map Valid Transitions**
- For each state, what are the valid next states?
- What events cause each transition?

**5. Define Invariants**
- For each state, what must always be true?

**6. Add Guards** (Conditional Transitions)
- When are transitions conditional?
- What determines which path to take?

**7. Specify Side Effects**
- What happens when entering/exiting states?
- Notifications, calculations, integrations

**8. Handle Temporal Aspects**
- Are there timeouts?
- Scheduled transitions?
- Effective dating?

---

## Hands-On Exercise: Model Entity Lifecycles

### Deliverable: Three State Machines from Real Domains

**Model lifecycles for THREE entities from different domains:**

Choose from:
- **E-commerce:** Product, Return, Refund, Wishlist Item
- **Healthcare:** Appointment, Prescription, Lab Order, Patient Admission
- **Finance:** Loan, Transaction, Dispute, Investment Account
- **Logistics:** Shipment, Container, Delivery, Route
- **Manufacturing:** Work Order, Equipment, Quality Inspection, Batch
- **Energy/Terminal (your domain):** Tank, Loading Operation, Delivery, Maintenance Request

For each entity, document:

#### 1. State Diagram (Visual)

Create a state machine diagram showing:
- All states (boxes/circles)
- All transitions (arrows)
- Events that trigger transitions (labels on arrows)
- Initial state (marked)
- Final states (marked)

Use ASCII art, drawing tool, or Mermaid syntax.

#### 2. State Catalog (Table)

| State Name | Description | Entry Actions | Exit Actions | Valid Next States |
|------------|-------------|---------------|--------------|-------------------|
| DRAFT | Order being composed | Create order record | Validate completeness | SUBMITTED, CANCELLED |
| SUBMITTED | Awaiting payment | Initiate payment | — | PAYMENT_PROCESSING, CANCELLED |
| ... | ... | ... | ... | ... |

#### 3. Event Catalog (Table)

| Event Name | Trigger | Applicable States | Resulting State | Conditions/Guards | Side Effects |
|------------|---------|-------------------|-----------------|-------------------|--------------|
| OrderSubmitted | Customer clicks "Place Order" | DRAFT | SUBMITTED | Cart not empty, Address valid | Send confirmation email, Hold inventory |
| ... | ... | ... | ... | ... | ... |

#### 4. State Invariants (List)

For each state, what must be true:

```
State: SHIPPED
Invariants:
- tracking_number IS NOT NULL
- shipped_date IS NOT NULL
- shipped_date >= payment_date
- allocated_items == shipped_items
- payment_status = 'COMPLETED'
```

#### 5. Temporal Requirements (List)

Any time-based behaviors:

```
- Orders in SUBMITTED state for > 24 hours transition to CANCELLED
- Orders in DELIVERED state for > 30 days become eligible for review
- Refunds must be processed within 7 business days of RETURN_RECEIVED
```

#### 6. Validation Rules (List)

Invalid transitions that must be prevented:

```
INVALID:
- DELIVERED → SUBMITTED (cannot go backwards)
- CANCELLED → SHIPPED (cannot ship cancelled order)
- PAYMENT_FAILED → PAID (must retry payment, not skip state)
```

---

## Real-World Example: Terminal Loading Operation State Machine

**Entity:** LoadingOperation

**States:**
- SCHEDULED
- DRIVER_ARRIVED
- PRE_LOAD_INSPECTION
- LOADING_IN_PROGRESS
- LOADING_COMPLETE
- POST_LOAD_VERIFICATION
- DEPARTED
- ABORTED

**State Diagram:**

```
     ┌──────────────┐
START│              │
     └──────┬───────┘
            │
            │ CreateAppointment
            ▼
       ┌──────────┐
       │SCHEDULED │
       └────┬─────┘
            │
            │ DriverArrives
            ▼
       ┌──────────────┐
       │DRIVER_ARRIVED│
       └──────┬───────┘
              │
              │ BeginInspection
              ▼
       ┌─────────────────┐
       │  PRE_LOAD       │
       │  INSPECTION     │
       └────┬─────┬──────┘
            │     │
    (pass)  │     │ (fail)
            │     │
            │     └──────────┐
            ▼                ▼
       ┌──────────┐     ┌─────────┐
       │ LOADING  │     │ ABORTED │ (FINAL)
       │IN_PROGRESS│    └─────────┘
       └────┬─────┘
            │
            │ LoadingComplete
            ▼
       ┌──────────┐
       │ LOADING  │
       │ COMPLETE │
       └────┬─────┘
            │
            │ VerifyLoad
            ▼
       ┌──────────────┐
       │  POST_LOAD   │
       │ VERIFICATION │
       └──────┬───────┘
              │
              │ IssueTicket
              ▼
       ┌──────────┐
       │ DEPARTED │ (FINAL)
       └──────────┘
```

**Invariants:**

```
LOADING_IN_PROGRESS:
- meter_opening_reading IS NOT NULL
- operator_id IS NOT NULL
- tank_id IS NOT NULL
- product_type IS NOT NULL
- started_time IS NOT NULL

DEPARTED:
- meter_closing_reading IS NOT NULL
- volume_loaded = closing_reading - opening_reading
- bill_of_lading_number IS NOT NULL
- departed_time IS NOT NULL
- departed_time > started_time
```

**Temporal Requirements:**

```
- If DRIVER_ARRIVED for > 30 minutes without transitioning to PRE_LOAD_INSPECTION,
  alert dispatcher (possible bottleneck)

- If LOADING_IN_PROGRESS for > 2 × expected_duration_minutes,
  alert operator (possible flow issue)

- Loading operations must complete within scheduled time window
  (scheduled_time ± 30 minutes) to avoid conflicts
```

---

## Reflection Questions

1. **State bugs:** Think of a bug you've encountered. Was it caused by invalid state transition or missing state validation?

2. **Implicit state:** What systems have you worked on that had "hidden" state machines (statuses that weren't formally modeled)?

3. **Temporal complexity:** What time-based behaviors exist in your domain that aren't currently captured in code?

4. **Invariants:** What business rules in your domain are really state invariants? (Things that must be true in certain conditions)

5. **Lifecycle gaps:** For a key entity in your domain, can you draw its full lifecycle? What states might be missing from current implementation?

---

## Key Takeaways

1. **State machines prevent chaos:** Explicitly modeling valid transitions prevents invalid operations
2. **Events drive transitions:** Every state change should be triggered by a named event
3. **Invariants protect integrity:** Each state has conditions that must always hold true
4. **Invalid transitions must be prevented:** Don't allow arbitrary state changes
5. **Temporal requirements are requirements:** Time-based behaviors belong in requirements, not just implementation
6. **Lifecycle modeling reveals gaps:** Drawing state machines exposes missing requirements
7. **State is fundamental:** Whether stateless or stateful, understanding state is critical

**Next week:** You'll learn about **use cases, user stories & edge cases** — how to document complete scenarios including all the exceptions and alternative flows that make systems robust.

---

## Additional Resources

**Books:**
- *UML Distilled* by Martin Fowler (state machine diagrams)
- *Domain-Driven Design* by Eric Evans (aggregates as transactional state boundaries)
- *Implementing Domain-Driven Design* by Vaughn Vernon (state modeling)

**Tools:**
- PlantUML (state diagram generation from code)
- Draw.io or Lucidchart (visual state machine design)
- State machine libraries: Spring State Machine, XState (JavaScript)

**Standards:**
- UML State Machine Diagrams (OMG specification)
- Finite State Automata theory

**Practice:**
- Model 5 entity lifecycles from apps you use daily
- Find a production bug related to state, diagram what should have been
- Implement a state machine with explicit transition validation
- Identify temporal requirements in your current project

---

*Most bugs are state bugs. Most state bugs come from not modeling state explicitly. Master state machines, and you'll prevent entire categories of defects.*

**End of Week 11**
