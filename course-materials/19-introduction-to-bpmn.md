# Week 19: Introduction to BPMN (Business Process Model and Notation)

**Course 5: Business Process Modeling & Workflows — Week 19 of 26**

---

## The $18M Process That Nobody Understood

In 2017, an insurance company decided to automate their claims processing workflow. They had been processing claims manually for 30 years with great success—but it was slow and expensive.

They hired consultants who interviewed the claims processors, documented the process in a 400-page PDF, and handed it to the development team.

**Eighteen months and $18M later**, the system went live.

**It was a disaster.**

- Claims that took 3 days manually took 2 weeks in the new system
- 40% of claims got stuck in infinite loops
- Fraud detection (which worked perfectly for humans) caught nothing
- Exceptions that humans handled easily crashed the system

**What went wrong?**

The consultants documented **what people said they did**, not **what actually happened**. The PDF described the "happy path" but missed:
- The **12 different exception handling paths** that experienced processors knew by heart
- The **implicit decision rules** ("If it's over $50K and involves property damage, escalate to legal")
- The **human workarounds** for system limitations
- The **parallel activities** (investigation happens while waiting for documents)
- The **handoffs between teams** (claims → fraud detection → legal → payout)

When developers built exactly what the PDF described, they automated **an incomplete understanding** of the process.

**What was needed:** A **visual process map** that captured:
- All paths (happy path + exceptions)
- Decision points with explicit rules
- Parallel vs sequential activities
- Handoffs and responsibilities
- Integration with external systems

**That's BPMN: Business Process Model and Notation.**

Welcome to **Course 5: Business Process Modeling & Workflows**.

---

## What Is BPMN?

**BPMN (Business Process Model and Notation)** is an **international standard** (ISO/IEC 19510) for modeling business processes using a simple, visual language that both business people and technical people can understand.

### Why BPMN Exists

**Business processes are complex:**
- Multiple actors (human and automated)
- Sequential and parallel activities
- Decisions and branching
- Exceptions and error handling
- Handoffs between teams
- Integration with systems

**Traditional documentation fails:**
- Text descriptions are ambiguous ("then the manager reviews the claim" — but what if the manager is on vacation?)
- Spreadsheets can't show parallel flows
- Flowcharts are inconsistent (everyone draws them differently)

**BPMN provides:**
- **Standard notation** — Everyone uses the same symbols with the same meanings
- **Precision** — Unambiguous representation of complex flows
- **Communication** — Business experts and developers see the same diagram
- **Executability** — BPMN can be executed by workflow engines (Camunda, Activiti, jBPM)

### BPMN vs Domain Models (From Course 4)

**DDD (Course 4)** taught you to model **what the business is**:
- Entities, Value Objects, Aggregates
- Domain concepts and their relationships
- Business rules and invariants

**BPMN (Course 5)** teaches you to model **how the business operates**:
- Processes and workflows
- Sequences of activities
- Decision points and branching
- Handoffs and responsibilities

**They work together:**

```
Domain Model (DDD):          Process Model (BPMN):
- LoadOrder (entity)    →    "Load Scheduling Process"
- LoadingOperation      →    "Loading Execution Workflow"
- Tank (aggregate)      →    "Inventory Reconciliation Process"

Domain Events (Week 16): →   Trigger process steps
- LoadOrderScheduled    →    Starts "Prepare for Loading" task
- LoadingCompleted      →    Triggers "Update Inventory" subprocess
```

**Example:** A **LoadOrder** (domain model) goes through a **Load Scheduling Process** (BPMN) involving multiple human tasks, automated validations, and decision gates.

---

## Core BPMN Elements

BPMN has about **100 different symbols**, but you can model **90% of processes** with just **10-15 core elements**.

### 1. Events

**Events** are things that **happen** (instantaneous, zero duration).

**Three types:**

#### Start Event (○)
- **When:** Process begins
- **Example:** "Customer places order," "Driver arrives at gate," "Month-end closes"

```
(○) Customer Places Order
```

#### Intermediate Event (◎)
- **When:** Something happens during the process
- **Example:** "Timer expires," "Message received," "Error occurs"

```
→ [Wait for approval] → (◎ 2-day timer) → [Escalate to manager]
```

#### End Event (⦿)
- **When:** Process completes
- **Example:** "Order fulfilled," "Claim paid," "Loading completed"

```
[Issue ticket] → (⦿) Loading Complete
```

**Event types (can be start, intermediate, or end):**
- **None** — Generic event
- **Message** — Receiving/sending a message
- **Timer** — Time-based trigger (after 2 hours, every Monday at 9am)
- **Error** — Error occurred
- **Signal** — Broadcast signal (multiple processes can listen)
- **Conditional** — Condition became true

### 2. Activities (Tasks)

**Activities** are **work** that happens in the process.

#### Task (rounded rectangle)
- Generic work unit
- Can be human or automated

```
┌──────────────────┐
│  Review Claim    │
└──────────────────┘
```

**Task types:**

**User Task** (👤)
- Performed by a human
- **Example:** "Approve invoice," "Verify driver credentials," "Inspect tank"

```
┌──────────────────┐
│ 👤 Approve Load  │
└──────────────────┘
```

**Service Task** (⚙️)
- Automated by a system
- **Example:** "Check inventory level," "Send email notification," "Calculate price"

```
┌──────────────────────┐
│ ⚙️ Check Inventory   │
└──────────────────────┘
```

**Script Task** (📜)
- Execute a script (JavaScript, Groovy, etc.)

**Send Task / Receive Task** (✉️)
- Send or receive a message

**Business Rule Task** (📋)
- Execute business rules (decision table, DMN)

**Manual Task**
- Work done outside the system (physical task)
- **Example:** "Load product onto truck," "Take sample for testing"

```
┌──────────────────────┐
│ 🔧 Load Product      │
└──────────────────────┘
```

#### Subprocess (Collapsed: ⊞)
- A process within a process
- Can be expanded to show detail

```
┌──────────────────────┐
│  Handle Exception ⊞  │
└──────────────────────┘
```

### 3. Gateways (Decision Points)

**Gateways** control the **flow** of the process—where decisions are made and paths split or merge.

#### Exclusive Gateway (XOR) — ◇×
- **ONE path** is taken based on a condition
- **Example:** "If claim < $10K, auto-approve; otherwise, manual review"

```
         ◇× Amount < $10K?
        ╱  ╲
   Yes ╱    ╲ No
      ╱      ╲
[Auto-approve] [Manual review]
```

#### Parallel Gateway (AND) — ◇+
- **ALL paths** are taken simultaneously
- Use to **split** into parallel activities or **merge** parallel flows

```
         ◇+ (split)
        ╱  ╲
       ╱    ╲
[Investigate] [Request docs]
      ╲      ╱
       ╲    ╱
        ◇+ (merge)
```

**Example:** After load order is scheduled, **both** "Verify product availability" and "Confirm driver assignment" happen in parallel.

#### Inclusive Gateway (OR) — ◇○
- **ONE OR MORE** paths are taken
- **Example:** "If high-value claim: notify legal; if property damage: notify adjuster; if fraud suspected: notify security"

```
         ◇○ Claim characteristics?
        ╱  |  ╲
[Legal] [Adjuster] [Security]
      ╲    |    ╱
        ◇○ (merge)
```

### 4. Sequence Flows (Arrows)

**Sequence flows** show the **order** in which activities happen.

```
[Task A] ──→ [Task B] ──→ [Task C]
```

**Conditional flow** (diamond on arrow):
- Flow is taken only if condition is true

```
[Evaluate] ──→ ◇ Approved? ──→ [Process]
                |
                | Rejected
                ↓
              [Deny]
```

### 5. Message Flows (Dashed Arrows)

**Message flows** show **communication** between participants (different organizations, systems, or roles).

```
[Customer]
    │
    ├──→ (message: Order placed)
    │
[System]
    │
    ├──→ (message: Order confirmed)
    │
[Customer]
```

### 6. Pools and Lanes (Swimlanes)

**Pools** represent **participants** (organizations, systems, or major actors).

**Lanes** divide a pool by **role** or **department**.

```
┌──────────────────────────────────────────┐
│ Customer (Pool)                          │
│ [Place order] → [Receive confirmation]  │
└──────────────────────────────────────────┘
           ↓ (message flow)
┌──────────────────────────────────────────┐
│ Vendor (Pool)                            │
│ ┌────────────────────────────────────┐  │
│ │ Sales (Lane)                       │  │
│ │ [Receive order] → [Create invoice] │  │
│ └────────────────────────────────────┘  │
│ ┌────────────────────────────────────┐  │
│ │ Warehouse (Lane)                   │  │
│ │ [Pick items] → [Ship]              │  │
│ └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

---

## Petroleum Terminal Example: Load Scheduling Process

Let's model a real-world process using BPMN.

**Process:** Schedule a load order at a petroleum terminal

**Actors:**
- **Customer** (external)
- **Logistics Coordinator** (schedules loads)
- **Terminal Operator** (executes loads)
- **System** (automated checks)

**BPMN Diagram:**

```
┌──────────────────────────────────────────────────────────────────┐
│ Customer                                                         │
│ (○) Request Load → [Submit load request] ──(message)──→         │
└──────────────────────────────────────────────────────────────────┘
                                                  ↓
┌──────────────────────────────────────────────────────────────────┐
│ Logistics Coordinator                                            │
│                                                                   │
│  (○) Load Request Received                                       │
│   │                                                               │
│   ↓                                                               │
│  [⚙️ Validate request]                                           │
│   │                                                               │
│   ↓                                                               │
│  ◇× Valid?                                                       │
│  ├─ No → [Notify customer of error] → (⦿) Request Rejected       │
│  │                                                                │
│  └─ Yes                                                           │
│      ↓                                                            │
│     ◇+ (parallel split)                                          │
│     ├─→ [⚙️ Check product availability]                          │
│     │                                                             │
│     └─→ [⚙️ Check bay availability]                              │
│          ╲                                                        │
│           ◇+ (parallel merge)                                    │
│            │                                                      │
│            ↓                                                      │
│           ◇× Capacity available?                                 │
│           ├─ No → [Propose alternative time] ──(message)──→      │
│           │                                   Customer           │
│           │                                                       │
│           └─ Yes                                                  │
│               ↓                                                   │
│              [👤 Assign bay and time slot]                       │
│               │                                                   │
│               ↓                                                   │
│              [⚙️ Create load order]                              │
│               │                                                   │
│               ↓                                                   │
│              [✉️ Send confirmation] ──(message)──→ Customer       │
│               │                                                   │
│               ↓                                                   │
│              (⦿) Load Scheduled                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Key features of this process:**

1. **Start event:** Load request received
2. **Automated validation:** System checks request format
3. **Exclusive gateway:** Valid or invalid?
4. **Parallel gateway:** Check product AND bay availability simultaneously
5. **User task:** Human assigns bay and time slot
6. **Service tasks:** Automated inventory and scheduling checks
7. **Message flows:** Communication with customer
8. **End events:** Request rejected OR load scheduled

**Connection to DDD (Course 4):**
- "Create load order" task → Creates `LoadOrder` aggregate (Week 15)
- "Load Scheduled" end event → Publishes `LoadOrderScheduled` domain event (Week 16)
- Logistics Coordinator → Actor from Event Storming (Week 17)
- Process operates within **Scheduling Context** (Week 14)

---

## Reading BPMN Diagrams

**How to interpret a BPMN process:**

1. **Find the start event(s)** — Where does the process begin?
2. **Follow the sequence flows** — What happens next?
3. **Look for gateways** — Where are decisions made? What are the conditions?
4. **Identify parallel flows** — What happens simultaneously?
5. **Check swimlanes** — Who is responsible for each task?
6. **Find the end event(s)** — How does the process complete? Are there multiple outcomes?
7. **Look for message flows** — How do participants communicate?

**Example: Invoice Processing**

```
(○) Invoice Received
 │
 ↓
[⚙️ Extract invoice data]
 │
 ↓
[👤 Review invoice]
 │
 ↓
◇× Approved?
├─ No → [Send back to vendor] → (⦿) Invoice Rejected
└─ Yes
    ↓
   ◇× Amount > $10,000?
   ├─ Yes → [👤 Get VP approval] → ◇× VP Approved?
   │                               ├─ No → (⦿) Rejected
   │                               └─ Yes ↓
   └─ No ─────────────────────────────────┘
       ↓
      [⚙️ Schedule payment]
       │
       ↓
      (⦿) Invoice Processed
```

**Reading this:**
1. Starts when invoice is received
2. System extracts data automatically
3. Human reviews
4. If not approved → rejected (end)
5. If approved → check amount
6. If > $10K → VP approval needed
7. If VP rejects → rejected (end)
8. If approved (or < $10K) → schedule payment
9. Process completes with invoice processed

**Decision points:** Two exclusive gateways (approval? amount?)
**Human tasks:** Review, VP approval
**Automated tasks:** Extract data, schedule payment
**Multiple end states:** Rejected OR processed

---

## Common BPMN Patterns

### Pattern 1: Sequential Activities

Tasks happen **one after another**.

```
[Task A] → [Task B] → [Task C] → (⦿)
```

**Example:** Load execution
```
[Check in driver] → [Assign bay] → [Begin loading] → [Complete loading] → (⦿)
```

### Pattern 2: Parallel Activities (AND Split/Join)

Tasks happen **at the same time**.

```
         ◇+
        ╱  ╲
[Task A] [Task B]
      ╲  ╱
       ◇+
        │
       (⦿)
```

**Example:** Onboarding new employee
```
         ◇+ Day 1 begins
        ╱  |  ╲
[IT setup] [HR docs] [Manager intro]
      ╲    |    ╱
        ◇+ All complete
         │
        (⦿)
```

**All paths must complete** before continuing.

### Pattern 3: Exclusive Choice (XOR Split)

**Exactly one** path is taken based on a condition.

```
      ◇× Condition?
     ╱  ╲
[Path A] [Path B]
```

**Example:** Loan application
```
      ◇× Credit score?
     ╱  ╲
[< 650: Deny] [>= 650: Manual review]
```

### Pattern 4: Loop (Redo Until Correct)

Go back and repeat a task.

```
[Task] → ◇× Correct?
         ├─ No ──┐
         │       │
         │       ↓
         │    [Fix errors] ──→ (back to Task)
         │
         └─ Yes → (⦿)
```

**Example:** Document review
```
[Submit document] → ◇× Approved?
                    ├─ No → [Revise document] ──┐
                    │                            │
                    │ ←──────────────────────────┘
                    │
                    └─ Yes → (⦿)
```

### Pattern 5: Timeout / Escalation

If something doesn't happen within a time limit, escalate.

```
[Wait for approval]
      │
      ├──→ (◎ Timer: 2 days) → [Escalate to manager]
      │
      └──→ (◎ Approval received) → [Continue process]
```

**Example:** Approval workflow
```
[Request approval] → [Wait]
                      │
                      ├──→ (◎ 2 days) → [Auto-escalate to VP]
                      │
                      └──→ (◎ Approved) → [Proceed]
```

### Pattern 6: Error Handling

If an error occurs, handle it differently.

```
[Task] → (successful) → [Next task]
  │
  └──→ (◎ Error) → [Handle error] → (⦿) Error handled
```

**Example:** Payment processing
```
[Process payment] → (success) → (⦿) Complete
      │
      └──→ (◎ Payment failed) → [Notify customer] → [Retry or cancel]
```

---

## BPMN + Domain Events (Integration with Week 16)

**BPMN processes** and **domain events** work together:

**Domain events trigger process steps:**

```
(○) LoadOrderScheduled event received
 │
 ↓
[Notify terminal operators]
 │
 ↓
[Prepare loading bay]
 │
 ↓
(⦿) Ready for loading
```

**Process steps publish domain events:**

```
[Complete loading] → [Publish LoadingCompleted event] → (⦿)
```

**Complete integration:**

```
Domain Model (DDD):
- LoadOrder.schedule() → publishes LoadOrderScheduled event

Process (BPMN):
(○) LoadOrderScheduled event
 ↓
[👤 Assign bay]
 ↓
[⚙️ Notify driver]
 ↓
[👤 Prepare equipment]
 ↓
(⦿) Ready for loading

When loading is done:
[Complete loading] → LoadingOperation.complete() → publishes LoadingCompleted event

Which triggers another process:
(○) LoadingCompleted event
 ↓
◇+ (parallel)
├─→ [⚙️ Update inventory]
└─→ [⚙️ Generate invoice]
```

**Benefits:**
- Domain models stay clean (no process logic)
- Processes orchestrate domain operations
- Events create loose coupling between processes

---

## Hands-On Exercise: Model a Process with BPMN

**Choose ONE process from your domain** (petroleum terminal, hospital, e-commerce, property management):

**Options:**
1. **Petroleum Terminal:** Driver check-in and loading execution
2. **Hospital:** Patient admission and room assignment
3. **E-Commerce:** Order fulfillment (from order placed to delivery)
4. **Property Management:** Lease application and approval

### Part 1: Identify Process Elements (30 minutes)

For your chosen process, list:

1. **Start event(s)** — What triggers the process?
2. **End event(s)** — How does it complete? (multiple outcomes?)
3. **Activities (tasks)** — What work happens? (list 8-12 tasks)
   - Mark which are **user tasks** (human) vs **service tasks** (automated)
4. **Decision points** — Where are choices made? What are the conditions?
5. **Parallel activities** — What happens simultaneously?
6. **Actors** — Who is involved? (create swimlanes)
7. **Messages** — What communication happens between actors?

**Deliverable:** Structured list of process elements

### Part 2: Draw BPMN Diagram (60 minutes)

Create a complete BPMN diagram showing:
- Start and end events
- All tasks in sequence
- Gateways for decisions and parallel flows
- Swimlanes showing actor responsibilities
- Message flows between actors

**Tools:**
- Hand-drawn (paper and pencil)
- **draw.io** (free, web-based, has BPMN shapes)
- **Camunda Modeler** (free desktop app, industry-standard)
- **Lucidchart** (web-based with BPMN template)

**Deliverable:** BPMN diagram (photo or digital file)

### Part 3: Identify Integration with Domain Model (30 minutes)

For your process:

1. **Which domain events trigger process steps?**
   - Example: `LoadOrderScheduled` event → starts "Prepare for Loading" process

2. **Which process steps create/modify aggregates?**
   - Example: "Create load order" task → creates `LoadOrder` aggregate

3. **Which process steps publish domain events?**
   - Example: "Complete loading" task → publishes `LoadingCompleted` event

4. **Which bounded contexts does this process span?**
   - Example: Starts in Scheduling context, executes in Operations context

**Deliverable:** Integration mapping document (1-2 pages)

### Part 4: Identify Exception Paths (20 minutes)

Processes rarely go perfectly. List:

1. **Error conditions** — What can go wrong?
2. **Exception handling** — How should errors be handled?
3. **Timeout scenarios** — What if someone doesn't respond?
4. **Compensating actions** — How to undo if something fails partway through?

**Add exception paths to your BPMN diagram.**

**Deliverable:** Updated BPMN diagram with exception handling

---

## Reflection Questions

1. **Compare BPMN to requirements documentation (Course 3).** How is a BPMN diagram different from a use case or user story? What information does each capture?

2. **Think about a manual process you've participated in** (expense approval, hiring, maintenance requests). How would you model it in BPMN? Where are the decision points? What happens in parallel?

3. **Exclusive vs Parallel gateways:** Why does the distinction matter? What goes wrong if you model parallel activities as sequential?

4. **From your petroleum terminal experience:** If you modeled the complete loadout process in BPMN, how many swimlanes would you need? What actors are involved?

5. **Automation potential:** Looking at a BPMN diagram, how can you identify which tasks could be automated vs which require human judgment?

---

## Key Takeaways

✅ **BPMN is a standard visual language** for modeling business processes that both business and technical people can understand.

✅ **Core elements:** Events (start, end, intermediate), Activities (tasks, subprocesses), Gateways (XOR, AND, OR), Sequence flows, Message flows, Swimlanes (pools and lanes).

✅ **Complements DDD:** Domain models (what) + Process models (how) = complete picture.

✅ **Precision over prose:** BPMN captures complexity (parallel flows, exceptions, decisions) that text descriptions miss.

✅ **Executable:** BPMN diagrams can be run by workflow engines (Camunda, Activiti), not just documentation.

✅ **Common patterns:** Sequential, parallel, exclusive choice, loops, timeouts, error handling.

✅ **Integration with domain events:** Events trigger processes, processes publish events, loose coupling between domain and workflow.

✅ **Swimlanes show responsibility:** Who does what, when handoffs occur, where dependencies exist.

---

## Connection to Week 20

This week you learned the **fundamentals of BPMN**—the core elements and how to read and create basic process diagrams.

Next week you'll learn **End-to-End Business Processes**—how to model complete workflows from start to finish, including:
- Complex decision logic
- Subprocesses and reusable components
- Error handling and compensation
- Integration with multiple systems
- Real-world petroleum terminal example (complete loadout process)
- Optimizing processes for efficiency

**Get ready to model real enterprise workflows.**

---

## Additional Resources

**Books:**
- *BPMN Method and Style* by Bruce Silver — Comprehensive BPMN guide with best practices
- *Real-Life BPMN* by Jakob Freund & Bernd Rücker — Practical examples from Camunda

**Standards:**
- Official BPMN 2.0 Specification (OMG)
- BPMN Quick Reference Guide

**Tools:**
- **Camunda Modeler** (free, desktop, industry-standard)
- **draw.io** (free, web-based, simple)
- **bpmn.io** (open-source JavaScript library)

**Online:**
- Camunda BPMN Tutorial
- BPMN.io interactive playground
- freund & rücker BPMN blog

**For Your Context:**
- Model petroleum terminal processes (loadout, inventory reconciliation, safety inspection)
- Compare BPMN to existing SOPs (Standard Operating Procedures)
- Identify automation opportunities in manual workflows

---

**End of Week 19 — Introduction to BPMN**

**Next:** Week 20 — End-to-End Business Processes
