# Week 22: Workflow to Domain Event Integration + PROJECT 5

**Course 5: Business Process Modeling & Workflows — Week 22 of 26**

---

## The System That Couldn't Stay Consistent

In 2019, a financial services company built separate teams to handle different parts of their loan processing:

- **Team A (Workflow Team):** Built BPMN workflows in Camunda for loan application processing
- **Team B (Domain Team):** Built domain models in Spring Boot for loan management (from Course 4)

Both teams worked independently. Both systems launched successfully.

**Week 1 of production:** Data inconsistencies everywhere.

**The problems:**

1. **Workflow completed a loan approval** → But didn't update the domain model's `Loan.status`
2. **Domain model changed loan amount** → But workflow still had the old amount
3. **Workflow sent approval email** → But domain model hadn't persisted the approval yet
4. **Two systems had different "sources of truth"** → Nobody knew which was correct

**The teams thought they were integrating:**
- Workflow called domain service endpoints
- Domain published events... but workflow didn't listen

**But they weren't really integrated:**
- No shared transaction boundaries
- No event-driven communication both ways
- No consistency guarantees
- No single source of truth

**The fix cost $2M and took 6 months:** Redesigning how workflows and domain models communicate through events, with proper integration patterns.

**The lesson:** Workflows (BPMN) and domain models (DDD) must be **properly integrated**. They're not separate systems—they're complementary views of the same business reality.

Welcome to **Week 22: Workflow to Domain Event Integration + PROJECT 5**.

---

## Integration Patterns: Workflows ↔ Domain Models

There are three main patterns for integrating BPMN workflows with domain models:

### Pattern 1: Workflow Orchestrates Domain Operations

**Workflow is the orchestrator.** It calls domain operations at each step.

```
BPMN Workflow:
(○) Start
 ↓
[Service Task: Create loan]
 ↓ (calls)
Domain Model:
  Loan loan = Loan.create(customerId, amount);
  loanRepository.save(loan);
  eventPublisher.publish(new LoanCreated(loan.getId()));
 ↓
[User Task: Review application]
 ↓
[Service Task: Approve loan]
 ↓ (calls)
Domain Model:
  loan.approve(underwriterId);
  loanRepository.save(loan);
  eventPublisher.publish(new LoanApproved(loan.getId()));
 ↓
(⦿) End
```

**Benefits:**
- ✅ Workflow has complete visibility into process state
- ✅ Clear sequencing of operations
- ✅ Easy to add parallel activities

**Drawbacks:**
- ❌ Tight coupling (workflow depends on domain model)
- ❌ Workflow must know about domain operations

**When to use:**
- Workflow is the primary driver
- Process orchestration is more important than domain logic
- Acceptable for workflows to depend on domain

### Pattern 2: Domain Events Trigger Workflow Steps

**Domain model is independent.** When domain events occur, they trigger workflow steps.

```
Domain Model:
  loan.approve(underwriterId);
  loanRepository.save(loan);
  eventPublisher.publish(new LoanApproved(loan.getId()));
 ↓
Event Bus:
  LoanApproved event
 ↓
BPMN Workflow (listening):
(○ Message start event: LoanApproved)
 ↓
[Send approval letter]
 ↓
[Schedule funding]
 ↓
(⦿) End
```

**Benefits:**
- ✅ Domain model is decoupled from workflow
- ✅ Domain can exist without workflow
- ✅ Multiple workflows can react to same event

**Drawbacks:**
- ❌ Workflow doesn't drive the process—it reacts
- ❌ Harder to see end-to-end flow

**When to use:**
- Domain model is the source of truth
- Multiple workflows react to domain changes
- Domain should be independent of process orchestration

### Pattern 3: Hybrid (Recommended)

**Workflows orchestrate, domain publishes events, workflows listen.**

```
BPMN Workflow:
(○) Start
 ↓
[Service Task: Create loan]
 ↓ (calls domain)
Domain Model:
  Loan loan = Loan.create(customerId, amount);
  loanRepository.save(loan);
  eventPublisher.publish(new LoanCreated(loan.getId()));
 ↓ (workflow continues)
[User Task: Review application]
 ↓
[Service Task: Approve loan]
 ↓ (calls domain)
Domain Model:
  loan.approve(underwriterId);
  loanRepository.save(loan);
  eventPublisher.publish(new LoanApproved(loan.getId()));
 ↓ (event published)
Other Workflows (listening):
(○ Message: LoanApproved) → [Update credit bureau]
(○ Message: LoanApproved) → [Notify sales team]
```

**Benefits:**
- ✅ Best of both worlds
- ✅ Workflow orchestrates main flow
- ✅ Domain events enable side effects
- ✅ Decoupled reactions to domain changes

**When to use:**
- Most enterprise scenarios
- Clear main process + side effects

---

## Publishing Domain Events from Workflows

### Service Tasks Call Domain Operations

**BPMN Service Task:**

```xml
<serviceTask id="approveLoan" name="Approve Loan"
             camunda:delegateExpression="${approveLoanDelegate}">
</serviceTask>
```

**Java Delegate:**

```java
@Component("approveLoanDelegate")
public class ApproveLoanDelegate implements JavaDelegate {

    private final LoanRepository loanRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public void execute(DelegateExecution execution) {
        // Get data from workflow
        String loanId = (String) execution.getVariable("loanId");
        String underwriterId = (String) execution.getVariable("underwriterId");

        // Load aggregate
        Loan loan = loanRepository.findById(new LoanId(loanId))
            .orElseThrow(() -> new LoanNotFoundException(loanId));

        // Execute domain operation
        loan.approve(new UnderwriterId(underwriterId));

        // Save aggregate
        loanRepository.save(loan);

        // Publish domain events
        loan.getDomainEvents().forEach(eventPublisher::publish);
        loan.clearDomainEvents();

        // Store result in workflow
        execution.setVariable("approvalDate", loan.getApprovalDate());
        execution.setVariable("approvedAmount", loan.getApprovedAmount());
    }
}
```

**Key points:**
1. Workflow variable → Domain operation
2. Domain aggregate executes business logic
3. Domain aggregate records events
4. Events are published
5. Domain results → Workflow variables

### Workflow Variables vs Domain State

**Workflow variables:**
- Process-specific data
- "Where are we in the process?"
- Example: `currentStep`, `assignedUnderwriter`, `daysInReview`

**Domain state:**
- Business entity data
- "What is the state of this business object?"
- Example: `loan.status`, `loan.amount`, `loan.approvalDate`

**Don't duplicate!**

❌ **Bad:**
```java
// Duplicating state
execution.setVariable("loanStatus", "APPROVED");
loan.setStatus(LoanStatus.APPROVED);
```

✅ **Good:**
```java
// Domain is source of truth
loan.approve(underwriter);
// Workflow just tracks process state
execution.setVariable("approvedBy", underwriter.getId());
execution.setVariable("approvalTaskCompletedAt", Instant.now());
```

---

## Listening to Domain Events in Workflows

### Message Start Events

**Start a new workflow when an event occurs:**

```xml
<startEvent id="loanApprovedStart" name="Loan Approved">
  <messageEventDefinition messageRef="LoanApproved" />
</startEvent>
```

**Event listener publishes to workflow engine:**

```java
@Component
public class LoanApprovedEventListener {

    private final RuntimeService runtimeService; // Camunda

    @EventListener
    public void handleLoanApproved(LoanApproved event) {
        // Start new workflow instance
        runtimeService.createMessageCorrelation("LoanApproved")
            .setVariable("loanId", event.getLoanId().getValue())
            .setVariable("approvedAmount", event.getAmount().getValue())
            .correlateStartMessage();
    }
}
```

**Result:** When `LoanApproved` domain event occurs, a new "Post-Approval Process" workflow starts.

### Message Intermediate Events

**Wait for an event during the workflow:**

```
[Send document request]
 ↓
(◎ Message: DocumentsReceived)
 ↓
[Review documents]
```

**BPMN:**

```xml
<intermediateCatchEvent id="waitForDocs" name="Wait for Documents">
  <messageEventDefinition messageRef="DocumentsReceived" />
</intermediateCatchEvent>
```

**Event listener correlates to waiting instance:**

```java
@EventListener
public void handleDocumentsReceived(DocumentsReceived event) {
    runtimeService.createMessageCorrelation("DocumentsReceived")
        .processInstanceBusinessKey(event.getLoanId().getValue())
        .setVariable("documentCount", event.getDocuments().size())
        .correlate();
}
```

**Result:** Workflow instance waits at the message catch event. When `DocumentsReceived` event arrives, workflow continues.

### Signal Events (Broadcast)

**One event triggers multiple workflow instances:**

```java
@EventListener
public void handleEmergencyShutdown(EmergencyShutdown event) {
    // Broadcast signal to ALL active loading operations
    runtimeService.signalEventReceived("EmergencyShutdown");
}
```

**All workflows listening for "EmergencyShutdown" signal react immediately.**

---

## Transaction Boundaries

**Critical question:** What happens if workflow step succeeds but domain operation fails? Or vice versa?

### Scenario 1: Same Transaction

**Workflow and domain in one transaction:**

```java
@Transactional
public void processLoanApproval(String loanId) {
    // Workflow step
    runtimeService.completeTask(taskId);

    // Domain operation
    Loan loan = loanRepository.findById(loanId);
    loan.approve(underwriter);
    loanRepository.save(loan);

    // Both commit or both rollback
}
```

**Benefits:**
- ✅ Strong consistency
- ✅ Either both succeed or both fail

**Drawbacks:**
- ❌ Tight coupling
- ❌ Long transactions (if workflow takes time)
- ❌ Can't use async messaging

**When to use:**
- Short-lived operations
- Strong consistency required
- Same database for workflow and domain

### Scenario 2: Separate Transactions with Events

**Workflow and domain in separate transactions, events ensure consistency:**

```java
// Transaction 1: Domain operation
@Transactional
public void approveLoan(String loanId) {
    Loan loan = loanRepository.findById(loanId);
    loan.approve(underwriter);
    loanRepository.save(loan);
    eventPublisher.publish(new LoanApproved(loan.getId()));
}

// Transaction 2: Workflow reacts
@EventListener
@Transactional
public void handleLoanApproved(LoanApproved event) {
    runtimeService.createMessageCorrelation("LoanApproved")
        .processInstanceBusinessKey(event.getLoanId())
        .correlate();
}
```

**Benefits:**
- ✅ Decoupled
- ✅ Can use async messaging
- ✅ Scales better

**Drawbacks:**
- ❌ Eventual consistency (brief delay)
- ❌ Must handle "domain succeeded but workflow failed"

**When to use:**
- Different databases (domain DB ≠ workflow DB)
- Async processing acceptable
- Scalability important

### Idempotency: Handling Duplicate Events

**Problem:** Event published twice (network retry, at-least-once delivery). Workflow shouldn't process twice.

**Solution: Idempotent event handling**

```java
@EventListener
public void handleLoanApproved(LoanApproved event) {
    // Check if already processed
    if (eventProcessedRepository.exists(event.getEventId())) {
        log.info("Event {} already processed, skipping", event.getEventId());
        return;
    }

    // Process
    runtimeService.createMessageCorrelation("LoanApproved")
        .processInstanceBusinessKey(event.getLoanId())
        .correlate();

    // Mark as processed
    eventProcessedRepository.save(new ProcessedEvent(event.getEventId()));
}
```

---

## Complete Example: Petroleum Terminal Loading Process

### Domain Model (From Course 4)

**Aggregates:**
- `LoadOrder` (Scheduling Context)
- `LoadingOperation` (Operations Context)
- `Tank` (Inventory Context)

**Domain Events:**
- `LoadOrderScheduled`
- `LoadingStarted`
- `LoadingCompleted`
- `TankLevelChanged`

### BPMN Workflow

**Process: Execute Scheduled Load**

```
(○ Message Start: LoadOrderScheduled event)
 │
 │ Variables: loadOrderId, product, quantity, bay
 │
 ↓
[⚙️ Assign terminal operator]
 │ (service task: looks up available operator)
 ↓
[👤 Operator: Verify bay readiness]
 │ (user task: operator confirms bay is safe and ready)
 ↓
[⚙️ Wait for driver check-in]
 │
 ↓
(◎ Message: DriverCheckedIn event)
 │
 ↓
[⚙️ Begin loading]
 │ (calls domain: LoadingOperation.begin())
 │ (publishes: LoadingStarted event)
 ↓
[⚙️ Monitor loading]
 │ (subprocess: polls meter readings)
 │
 ↓
(◎ Message: LoadingCompleted event)
 │ (domain publishes when quantity reached)
 │
 ↓
[👤 Operator: Inspect and seal]
 │ (user task: visual inspection, apply seal)
 ↓
[⚙️ Generate ticket]
 │ (calls domain: LoadingOperation.complete())
 │ (publishes: LoadingCompleted event)
 ↓
[⚙️ Update inventory]
 │ (calls domain: Tank.withdraw(quantity))
 │ (publishes: TankLevelChanged event)
 ↓
(⦿) Loading process complete
```

### Integration Code

**Service Task: Begin Loading**

```java
@Component("beginLoadingDelegate")
public class BeginLoadingDelegate implements JavaDelegate {

    private final LoadingOperationRepository loadingOpRepository;
    private final LoadOrderRepository loadOrderRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public void execute(DelegateExecution execution) {
        String loadOrderId = (String) execution.getVariable("loadOrderId");
        String bayId = (String) execution.getVariable("bayId");
        String operatorId = (String) execution.getVariable("operatorId");

        // Load aggregate
        LoadOrder order = loadOrderRepository.findById(new LoadOrderId(loadOrderId))
            .orElseThrow();

        // Create loading operation
        LoadingOperation operation = LoadingOperation.create(
            order,
            new BayId(bayId),
            new OperatorId(operatorId)
        );

        // Begin loading
        operation.begin(Instant.now());

        // Save
        loadingOpRepository.save(operation);

        // Publish events
        operation.getDomainEvents().forEach(eventPublisher::publish);
        operation.clearDomainEvents();

        // Store loading operation ID in workflow
        execution.setVariable("loadingOperationId", operation.getId().getValue());
    }
}
```

**Event Listener: LoadingCompleted → Workflow**

```java
@Component
public class LoadingEventListener {

    private final RuntimeService runtimeService;

    @EventListener
    public void handleLoadingCompleted(LoadingCompleted event) {
        // Correlate to waiting workflow instance
        runtimeService.createMessageCorrelation("LoadingCompleted")
            .processInstanceBusinessKey(event.getLoadOrderId().getValue())
            .setVariable("actualQuantityLoaded", event.getQuantity().toBarrels())
            .setVariable("completedAt", event.getCompletedAt())
            .correlate();

        log.info("Workflow notified of loading completion for order {}",
                 event.getLoadOrderId());
    }
}
```

**Separate Workflow: Update Compliance (Triggered by LoadingCompleted)**

```
(○ Message Start: LoadingCompleted event)
 ↓
[⚙️ Archive carrier ticket]
 ↓
[⚙️ Update regulatory reporting]
 ↓
◇× Quantity > 5000 gallons?
├─ Yes → [⚙️ Submit to EPA reporting system]
└─ No → (skip)
 ↓
(⦿) Compliance updated
```

**Result:** One domain event (`LoadingCompleted`) triggers:
1. Main workflow continues (to ticket generation)
2. Compliance workflow starts (separate process)
3. Inventory context reacts (updates tank levels)

---

## Error Handling Across Workflows and Domain

**What if domain operation fails during workflow execution?**

### Pattern: BPMN Error Boundary Event

```
┌────────────────────────┐
│ ⚙️ Approve Loan        │ (◎ Error: InsufficientFunds)
└────────────────────────┘         ↓
                                [Notify customer]
                                    ↓
                                [Offer alternative]
```

**Domain throws exception:**

```java
public void approve(Underwriter underwriter) {
    if (this.amount.isGreaterThan(underwriter.getApprovalLimit())) {
        throw new InsufficientApprovalAuthorityException(
            "Underwriter cannot approve amounts over " + underwriter.getApprovalLimit()
        );
    }
    this.status = LoanStatus.APPROVED;
    this.domainEvents.add(new LoanApproved(this.id));
}
```

**Workflow delegate catches and throws BPMN error:**

```java
@Override
public void execute(DelegateExecution execution) {
    try {
        loan.approve(underwriter);
        loanRepository.save(loan);
    } catch (InsufficientApprovalAuthorityException e) {
        throw new BpmnError("InsufficientFunds", e.getMessage());
    }
}
```

**BPMN catches error:**

```xml
<boundaryEvent id="insufficientFundsError" attachedToRef="approveLoan">
  <errorEventDefinition errorRef="InsufficientFunds" />
</boundaryEvent>

<sequenceFlow sourceRef="insufficientFundsError" targetRef="escalateToSeniorUnderwriter" />
```

**Result:** When domain throws exception, workflow catches it and takes alternate path (escalate to senior underwriter).

---

## Testing Workflow + Domain Integration

### Unit Testing Domain Operations

```java
@Test
public void loanApproval_publishesEvent() {
    Loan loan = Loan.create(customerId, amount);
    loan.approve(underwriterId);

    List<DomainEvent> events = loan.getDomainEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(LoanApproved.class);
}
```

### Integration Testing Workflow Delegates

```java
@Test
public void approveLoanDelegate_callsDomainAndPublishesEvent() {
    // Setup
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable("loanId")).thenReturn("loan-123");

    // Execute
    approveLoanDelegate.execute(execution);

    // Verify domain operation
    Loan loan = loanRepository.findById(new LoanId("loan-123")).get();
    assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPROVED);

    // Verify event published
    verify(eventPublisher).publish(any(LoanApproved.class));
}
```

### End-to-End Testing Complete Flow

```java
@Test
@Deployment(resources = "loan-approval-process.bpmn")
public void completeLoanApprovalFlow() {
    // Start process
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(
        "loanApproval",
        Variables.putValue("loanId", "loan-123")
    );

    // Domain event published by first step
    verify(eventPublisher).publish(argThat(event ->
        event instanceof LoanCreated
    ));

    // Complete user task
    Task reviewTask = taskService.createTaskQuery()
        .processInstanceId(instance.getId())
        .singleResult();
    taskService.complete(reviewTask.getId(),
        Variables.putValue("decision", "APPROVE"));

    // Verify domain state changed
    Loan loan = loanRepository.findById(new LoanId("loan-123")).get();
    assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPROVED);

    // Verify process completed
    assertProcessEnded(instance.getId());
}
```

---

## PROJECT 5: Complete Business Process Design with Workflow and Domain Integration

**This is the capstone for Course 5.** Create a comprehensive business process design that integrates BPMN workflows with domain models.

### Project Scope

**Choose ONE domain (same as PROJECT 4 if possible):**

1. **Petroleum Terminal Operations** (complete loadout process)
2. **Hospital Patient Flow** (admission through discharge)
3. **E-Commerce Platform** (order through delivery)
4. **Property Management** (lease application through move-in)
5. **Your professional domain** (with approval)

### Deliverables

**Produce a comprehensive design document (20-30 pages) including:**

#### 1. Executive Summary (1-2 pages)
- Process overview
- Business value of automation
- Integration approach (workflow + domain)
- Key metrics (expected cycle time, throughput, error rate reduction)

#### 2. Process Definition (3-4 pages)
- Complete BPMN diagram (from Week 20)
- All swimlanes, paths, subprocesses
- Human tasks and automation strategy (from Week 21)
- Exception handling and compensation

#### 3. Domain Model Integration (4-5 pages)
- Key aggregates involved in the process
- Domain events published and consumed
- Integration points between workflow and domain
- Transaction boundaries and consistency guarantees

#### 4. Workflow Steps with Domain Operations (6-8 pages)

For 8-10 key workflow steps, document:

**Service Task Example:**
```markdown
### Step 3: Begin Loading

**Type:** Service Task (Automated)

**Trigger:** After operator confirms bay readiness

**Domain Operation:**
- Calls: `LoadingOperation.begin(Instant startTime)`
- Aggregate: LoadingOperation
- Business Rules Enforced:
  - Bay must be in READY state
  - Product must be available in source tank
  - Operator must be certified for product type

**Domain Events Published:**
- `LoadingStarted(loadingOperationId, loadOrderId, bayId, startTime)`

**Workflow Variables Updated:**
- `loadingOperationId`: ID of created LoadingOperation
- `loadingStartTime`: Timestamp

**Error Handling:**
- Exception: `BayNotReadyException` → BPMN Error → Escalate to supervisor
- Exception: `InsufficientInventoryException` → BPMN Error → Cancel load order

**Integration Code:**
```java
@Component("beginLoadingDelegate")
public class BeginLoadingDelegate implements JavaDelegate {
    // Full implementation...
}
```
```

#### 5. Event Integration Map (2-3 pages)

Visual diagram + table showing:

| Domain Event | Published By | Consumed By | Action Triggered |
|--------------|--------------|-------------|------------------|
| LoadOrderScheduled | Scheduling Context | Loading Workflow | Start new workflow instance |
| LoadingCompleted | LoadingOperation | Loading Workflow | Continue to ticket generation |
| LoadingCompleted | LoadingOperation | Compliance Workflow | Start compliance subprocess |
| LoadingCompleted | LoadingOperation | Inventory Context | Update tank levels |

#### 6. Human Task Design (3-4 pages)

For 4-6 user tasks, design:
- Task form (mockup or description)
- Assignment strategy (role, rule, skill-based)
- Priority and escalation rules
- Notification strategy
- Context provided to user

**Example:**
```markdown
### User Task: Operator Verify Bay Readiness

**Assignee:** Terminal Operator (role-based, any certified operator can claim)

**Form Fields:**
- Bay Number (read-only, pre-filled)
- Product (read-only, pre-filled)
- Checklist:
  - ☐ Bay equipment operational
  - ☐ Safety equipment in place
  - ☐ No leaks detected
  - ☐ Truck properly positioned
- Comments (optional, text area)
- Action: ○ Confirm Ready  ○ Report Issue

**Priority:** High (loading scheduled)

**Due:** 15 minutes before scheduled load time

**Escalation:** If not completed by scheduled load time, notify supervisor

**Notification:** Push notification to operator's mobile device + dashboard alert
```

#### 7. Error Handling and Recovery (2-3 pages)
- Error scenarios identified
- BPMN error boundary events
- Compensation logic
- Recovery procedures

#### 8. Testing Strategy (2-3 pages)
- Unit tests for domain operations
- Integration tests for workflow delegates
- End-to-end workflow tests
- Test data and scenarios

#### 9. Deployment and Operations (2-3 pages)
- Deployment architecture (workflow engine, domain services, databases, event bus)
- Monitoring and metrics
- SLAs for process execution
- Operational runbook

#### 10. Appendix: Complete Code (Optional, 3-5 pages)
- Full workflow delegate implementations
- Event listener code
- Domain aggregate methods called by workflow

### Evaluation Criteria

**Completeness (30%):**
- All workflow steps documented with domain integration
- All domain events mapped to workflow reactions
- Error handling comprehensive

**Correctness (25%):**
- Appropriate use of BPMN patterns
- Correct integration patterns (orchestration, events, transactions)
- Sound error handling and recovery

**Clarity (20%):**
- Clear documentation and diagrams
- Code examples are readable and well-commented
- Integration points well-explained

**Depth (15%):**
- Non-trivial process with realistic complexity
- Thoughtful transaction boundary decisions
- Practical error handling

**Feasibility (10%):**
- Can actually be implemented
- Realistic technology choices
- Operational considerations addressed

### Portfolio Value

This project demonstrates to employers:
- End-to-end process design and implementation skills
- Integration of workflow orchestration with domain models
- Enterprise architecture thinking (transactions, events, consistency)
- Practical error handling and recovery design
- Ability to bridge business processes and technical implementation
- Documentation and communication skills

**This is a senior-level deliverable showing mastery of both business process modeling (Course 5) and domain-driven design (Course 4).**

---

## Reflection Questions

1. **Transaction boundaries:** When would you use a single transaction for workflow + domain vs separate transactions with events?

2. **Source of truth:** In an integrated system, is the workflow or the domain model the source of truth? Does it depend?

3. **Event vs API calls:** When should a workflow listen for domain events vs directly calling domain services?

4. **From your operational experience:** Think of processes where manual steps and system steps alternated. How would you design the integration?

5. **Testing:** What's harder to test—the domain model alone, the workflow alone, or the integration between them? Why?

---

## Key Takeaways

✅ **Workflows and domain models are complementary** — Workflows orchestrate, domain models enforce rules and maintain state.

✅ **Three integration patterns:** Workflow orchestrates domain, domain events trigger workflows, or hybrid (recommended).

✅ **Domain events enable decoupling** — One domain event can trigger multiple workflow reactions.

✅ **Transaction boundaries matter** — Same transaction (strong consistency) vs separate transactions (eventual consistency). Choose based on requirements.

✅ **Idempotency is critical** — Events may be delivered twice. Handle gracefully.

✅ **Error handling crosses boundaries** — Domain exceptions → BPMN errors → workflow alternate paths.

✅ **Testing requires multiple levels** — Unit (domain), integration (delegates), end-to-end (complete flow).

✅ **PROJECT 5 integrates everything** — BPMN (Weeks 19-21) + DDD (Weeks 14-18) + Integration (Week 22).

---

## Course 5 Complete Summary

**Weeks 19-22 Summary:**

You've completed a comprehensive journey through business process modeling and workflows:

**Week 19: Introduction to BPMN**
- Core BPMN elements (events, tasks, gateways, flows, swimlanes)
- Reading and creating process diagrams
- Common patterns (sequential, parallel, exclusive, loops, timeouts, errors)

**Week 20: End-to-End Business Processes**
- Modeling complete processes with all exception paths
- Subprocesses for managing complexity
- Error handling and compensation
- Process optimization

**Week 21: Human ↔ Software Workflows**
- Spectrum of automation (manual → automated)
- User task design and forms
- Task assignment and escalation
- Work queues and notifications

**Week 22: Workflow to Domain Event Integration + PROJECT 5**
- Integration patterns (orchestration, events, hybrid)
- Publishing and consuming domain events in workflows
- Transaction boundaries and consistency
- **PROJECT 5: Complete business process design (20-30 pages)**

**Skills Portfolio:**
- BPMN process modeling
- End-to-end workflow design with exception handling
- Human-automation collaboration design
- **Complete integrated workflow + domain design (PROJECT 5)**

---

## Connection to Course 6

**Course 5 taught you to model processes** (how work flows through the organization).

**Course 6 will teach you to model data** (how information is structured, stored, and evolves over time).

**Week 23** begins Course 6 with **Conceptual Data Modeling**—understanding entities, relationships, and attributes at a high level before diving into database design.

You'll learn:
- Entity-Relationship Diagrams (ERD)
- Identifying entities from domain models
- Relationships and cardinality
- Attributes and data types
- Normalization concepts
- Translating domain models to data models

**Get ready to design how your domain data is stored and queried.**

---

## Additional Resources

**Books:**
- *Enterprise Integration Patterns* by Hohpe & Woolf — Integration patterns including process orchestration
- *Implementing Domain-Driven Design* by Vaughn Vernon — Chapter on integrating bounded contexts

**Platforms:**
- **Camunda Platform** — Complete BPMN + DMN + events
- **Temporal** — Modern workflow orchestration with strong consistency
- **Apache Kafka** — Event streaming for workflow-domain integration

**Articles:**
- Bernd Rücker: "Workflow Engines vs. Business Rules" (Camunda blog)
- Martin Fowler: "Event-Driven Architecture"

**For Your Context:**
- Design integration between terminal management workflows and SCADA systems
- Model event flows from physical operations (loading) to business systems (billing, compliance)
- Consider transactional boundaries in operational processes (when can steps fail independently?)

---

**End of Week 22 — End of Course 5: Business Process Modeling & Workflows**

**Next:** Week 23 — Conceptual Data Modeling (Course 6 begins)
