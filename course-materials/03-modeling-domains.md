# Week 3: Modeling Real-World Domains
## How to See Complex Systems and Turn Messy Reality Into Clear Diagrams

---

## Introduction: The Art of Seeing What's Really There

You've been asked to build software for a business process you don't fully understand. Maybe it's loan origination. Maybe it's hospital patient flow. Maybe it's supply chain logistics.

**Most developers make this mistake:** They immediately ask "What features do you want?" and start designing database schemas.

**The expert does this first:** "Show me how it works today. Walk me through a typical day. Let me watch."

This week is about **domain modeling** — the skill of observing complex real-world systems and creating useful representations of them. Not the software. Not the data structures. **The actual domain.**

Because here's the truth: **You cannot build good software for a domain you don't understand.**

---

## Core Concept 1: The Three Lenses for Seeing Domains

Every domain has multiple flows happening simultaneously. You need to see all three:

### 1. Physical Flows (Atoms)

**What moves through space?**

Examples:
- **Energy sector:** Crude oil flows from wellhead → pipeline → terminal → refinery
- **Healthcare:** Patients move through waiting room → exam room → treatment → discharge
- **Manufacturing:** Raw materials → work-in-progress → finished goods → shipping
- **Logistics:** Packages move through sorting center → trucks → delivery → customer
- **Food service:** Ingredients → prep → cooking → plating → customer

**Why this matters:**
- Physical flows have real constraints (capacity, speed, distance)
- They take time and cost money
- Software often tracks, optimizes, or coordinates physical flows

### 2. Information Flows (Bits)

**What data moves between actors?**

Examples:
- **Energy sector:** Meter readings, custody transfer documents, delivery schedules, tank levels
- **Healthcare:** Patient records, lab results, prescriptions, insurance approvals
- **Manufacturing:** Work orders, quality reports, inventory counts, shipping manifests
- **Logistics:** Tracking numbers, delivery confirmations, route updates
- **Food service:** Orders, recipes, inventory alerts, payment confirmation

**Why this matters:**
- Information flows often precede or follow physical flows
- Delays in information cause delays in physical execution
- Software IS the information flow system

### 3. Value Flows (Money/Utility)

**Where does value get created, transferred, or destroyed?**

Examples:
- **Energy sector:** Value created when crude refined into gasoline; transferred at custody points; destroyed by losses/shrinkage
- **Healthcare:** Value created when patient healed; transferred via insurance payments; destroyed by wait times
- **Manufacturing:** Value added at each transformation step; transferred at sale; destroyed by defects
- **Logistics:** Value created by moving things to where they're needed; transferred at delivery; destroyed by delays/damage
- **Food service:** Value created in preparation; transferred at consumption; destroyed by waste

**Why this matters:**
- Value flows determine where money comes from
- They reveal who has power and incentives in the system
- Software should enhance value creation, not just track transactions

---

## Core Concept 2: Domain Mapping Process

### Step 1: Identify the Actors

**Who participates in this domain?**

**Categories:**
- **Primary actors:** Core participants who drive value (customers, patients, drivers)
- **Supporting actors:** Enable primary actors (warehouse staff, nurses, dispatchers)
- **External systems:** Adjacent systems that interact (banks, regulators, suppliers)
- **Governance actors:** Set rules and policies (compliance, quality assurance, management)

**Example: Petroleum Terminal Domain**

**Primary Actors:**
- Suppliers (pipeline operators, ship operators)
- Terminal operators
- Customers (trucking companies, refineries)

**Supporting Actors:**
- Meter technicians
- Lab analysts (quality control)
- Loadout operators
- Schedulers

**External Systems:**
- Pipeline SCADA systems
- Customer ordering systems
- Accounting/billing systems
- Regulatory reporting systems

**Governance Actors:**
- Terminal manager
- Safety officer
- Compliance team

### Step 2: Trace the Primary Flow

**What's the "happy path" — the most common, successful scenario?**

**Questions to ask:**
1. What triggers the process? (event, request, scheduled activity)
2. What's the first step?
3. Who does what, in what order?
4. What decisions or approvals are needed?
5. Where do handoffs occur?
6. What's the end state? (completion criteria)

**Example: Crude Oil Delivery Flow**

```
┌─────────────────────────────────────────────────────────────────┐
│                     CRUDE OIL DELIVERY FLOW                     │
└─────────────────────────────────────────────────────────────────┘

TRIGGER: Pipeline batch arrives (scheduled)

1. RECEIPT NOTIFICATION
   Pipeline → Terminal: "Batch arriving, 50,000 bbls"

2. TANK ASSIGNMENT
   Scheduler → System: Assign Tank 5 (has capacity)

3. PHYSICAL FLOW
   Pipeline → Meter → Tank 5
   Duration: 5 hours @ 10,000 bbls/hr

4. MEASUREMENT & SAMPLING
   Meter Technician → System: Record opening/closing meter readings
   Lab Analyst → System: Record quality specs (gravity, sulfur, water)

5. CUSTODY TRANSFER
   System → Supplier: Invoice for 49,950 bbls received (metered quantity)
   System → Internal: Update inventory in Tank 5

6. AVAILABLE FOR SALE
   System → Customers: Product available for loadout

END STATE: Product in storage, measured, quality-verified, invoiced
```

### Step 3: Identify Decision Points

**Where do choices get made?**

**Types of decisions:**
- **Routing:** Which path? (which tank, which truck, which facility)
- **Approval:** Go/no-go? (credit check, quality acceptance, safety clearance)
- **Prioritization:** What order? (which order first, which shipment urgent)
- **Allocation:** How much to whom? (limited supply, multiple customers)
- **Escalation:** When to involve higher authority? (exceptions, emergencies)

**Example: Tank Assignment Decision**

```
DECISION: Which tank receives incoming batch?

INPUTS:
- Incoming volume (50,000 bbls)
- Product grade (WTI Crude, 40° API)
- Available tank capacities
- Current tank contents (can't mix grades)
- Downstream demand (which customers need this grade?)

DECISION LOGIC:
1. Filter tanks by: compatible grade, sufficient capacity
2. Prefer: tanks already containing same grade (avoid contamination)
3. Prefer: tanks with upcoming loadout scheduled (minimize storage time)
4. Assign: Tank 5

ACTORS:
- Automated system (routine cases)
- Scheduler (complex cases, overrides)
```

### Step 4: Map Information Flows

**What data moves where, and when?**

**Key questions:**
- What information is created at each step?
- Who needs to know what, when?
- What happens if information is delayed?
- What information is used for decisions?

**Example: Information Flows in Crude Delivery**

```
┌──────────────┐
│   Pipeline   │
│    Operator  │
└──────────────┘
       │
       │ (1) Batch notification (24 hrs advance)
       ▼
┌──────────────┐
│   Terminal   │
│   Scheduler  │──────┐
└──────────────┘      │
       │              │ (2) Tank assignment
       │              ▼
       │         ┌──────────────┐
       │         │   Terminal   │
       │         │    System    │
       │         └──────────────┘
       │              │
       │              │ (3) Receipt instructions
       ▼              ▼
┌──────────────┐  ┌──────────────┐
│    Meter     │  │   Loadout    │
│  Technician  │  │   Operator   │
└──────────────┘  └──────────────┘
       │              │
       │ (4) Meter    │ (5) Quality
       │  readings    │  test results
       ▼              ▼
┌──────────────────────────────┐
│      Terminal System         │
│  (Inventory, Billing, QC)    │
└──────────────────────────────┘
       │
       │ (6) Custody transfer docs, Invoices
       ▼
┌──────────────┐
│   Supplier   │
│   & Customer │
└──────────────┘
```

### Step 5: Identify Handoffs and Boundaries

**Where does responsibility transfer?**

Handoffs are critical because they're where:
- Information gets lost
- Delays occur
- Errors happen
- Finger-pointing starts

**Example: Custody Transfer Point**

```
BEFORE HANDOFF:
- Supplier owns product
- Supplier bears loss risk
- Supplier responsible for quality

HANDOFF EVENT: Meter reading at terminal inlet

AFTER HANDOFF:
- Terminal owns product
- Terminal bears loss risk
- Terminal responsible for quality

DOCUMENTATION REQUIRED:
- Meter readings (opening/closing)
- Temperature/pressure corrections
- Quality certificate (lab results)
- Signed transfer document
```

---

## Core Concept 3: Physical vs Information vs Value Flow Separation

### Why Separate These?

**Because they don't always align.**

**Example: Food Delivery**

**Physical flow:**
```
Restaurant → Driver picks up → Drives to customer → Customer receives
```

**Information flow:**
```
Customer orders (app) → Restaurant receives → Restaurant confirms →
Driver assigned → Customer notified → Driver updates location →
Driver marks delivered → Customer confirms
```

**Value flow:**
```
Customer pays (upfront via app) → Platform holds payment →
Restaurant receives share (after delivery) → Driver receives share (after delivery) →
Platform keeps fee
```

**Notice:** Value transfers BEFORE physical delivery. Risk implications!

### Example: Healthcare Patient Journey

**Physical flow:**
```
Patient in waiting room → Exam room → Lab → Imaging → Back to exam room →
Discharge → Pharmacy
```

**Information flow:**
```
Registration → EHR updated → Physician orders tests → Lab results to EHR →
Imaging results to EHR → Diagnosis entered → Prescription sent to pharmacy
```

**Value flow:**
```
Insurance pre-authorization → Services rendered → Claims submitted →
Insurance pays 80% → Patient billed 20% → Payment collected (eventually)
```

**Misalignments:**
- Information (diagnosis) may lag physical flow (patient waiting)
- Value (payment) lags everything by weeks/months
- Insurance denial (value flow) can happen after service (physical flow)

### Example: Petroleum Terminal (Your Domain)

**Physical flow:**
```
Pipeline → Storage tank → Loadout bay → Truck → Customer facility
```

**Information flow:**
```
Pipeline nomination → Schedule approval → Receipt confirmation →
Meter readings → Quality test → Custody transfer doc → Invoice
```

**Value flow:**
```
Customer credit check → Product reservation → Custody transfer (value exchanges) →
Invoice generated → Payment (30 days later)
```

**Key insight:** Physical product leaves terminal (day 1), but payment arrives (day 30+). Working capital implications!

---

## Core Concept 4: How Domain Complexity Shapes Software Architecture

### Domain Complexity ≠ Technical Complexity

**Simple domain, complex tech:**
- Todo list app with microservices, event sourcing, CQRS
- Over-engineered for the problem

**Complex domain, simple tech:**
- Hospital patient management with CRUD and relational DB
- Under-engineered, will fail at scale or edge cases

**The right approach:**
- **Match architectural complexity to domain complexity**
- Simple domains: monolith, CRUD, straightforward
- Complex domains: bounded contexts, event-driven, careful modeling

### Indicators of Domain Complexity

**You need sophisticated architecture when:**

1. **Multiple bounded contexts** with different languages/rules
   - Example: "Order" means different things in sales vs warehouse vs accounting

2. **Long-running processes** with many states
   - Example: Loan origination takes weeks, many approvals, can pause/resume

3. **Complex business rules** that change frequently
   - Example: Insurance underwriting with 100+ rules that vary by state

4. **External system dependencies** with eventual consistency
   - Example: Payment processed externally, order must wait for confirmation

5. **Temporal complexity** (effective dates, versioning, audit)
   - Example: Contract amendments that take effect in the future

6. **Workflow orchestration** across multiple actors
   - Example: Multi-party approval chains with escalations

### Domain Complexity in Your Experience

**Low complexity: Shift scheduling**
- Actors: Employees, managers
- Flow: Manager creates schedule → Employees view → Trade shifts
- Rules: Coverage requirements, labor law limits
- **Architecture:** Simple CRUD app, monolith fine

**Medium complexity: Inventory management**
- Actors: Suppliers, warehouse, sales, customers
- Flow: Receive → Store → Pick → Ship
- Rules: Reorder points, lot tracking, FIFO/LIFO
- **Architecture:** Bounded contexts (receiving, storage, shipping), event-driven for stock updates

**High complexity: Terminal operations (your domain)**
- Actors: Pipeline operators, terminal operators, trucking companies, labs, accounting, regulators
- Flow: Multi-step with custody transfers, quality gates, scheduling
- Rules: Grade compatibility, tank allocation, measurement standards, safety protocols, regulatory compliance
- **Architecture:** Multiple bounded contexts (scheduling, operations, measurement, custody transfer, billing), event-driven for state changes, strong consistency at custody points

---

## Practical Framework: Domain Mapping Canvas

Use this template for ANY domain:

### Domain Mapping Template

**1. Domain Name & Scope**
- What is this domain? (1-2 sentence description)
- What's in scope? What's out of scope?

**2. Actors**
- Primary actors: [List]
- Supporting actors: [List]
- External systems: [List]
- Governance: [List]

**3. Primary Flow (Happy Path)**
```
[Trigger] → [Step 1] → [Step 2] → ... → [End State]
```

**4. Decision Points**
| Decision | Who Decides | Inputs | Logic | Edge Cases |
|----------|-------------|--------|-------|------------|
| [Name] | [Actor] | [Data] | [Rules] | [Exceptions] |

**5. Physical Flow Diagram**
```
[Start] → [Location 1] → [Location 2] → ... → [End]
```

**6. Information Flow Diagram**
```
[Actor A] → (sends data) → [Actor B] → (sends data) → [Actor C]
```

**7. Value Flow**
- When is value created?
- When does value transfer?
- Where does money flow?

**8. Handoffs & Boundaries**
| Handoff | From | To | Documentation | Risk |
|---------|------|----|--------------|----- |
| [Name] | [Actor] | [Actor] | [Required docs] | [What can go wrong] |

**9. Exceptions & Error Cases**
- What goes wrong commonly?
- How are exceptions handled?

**10. Timing & Volumes**
- How often does this happen? (per day/week/month)
- How long does it take?
- What's typical? Peak? Minimum?

---

## Hands-On Exercise: Map Three Different Domains

### Deliverable Requirements

Create high-level flow maps for **THREE different domains**:

1. **One from your operational experience** (energy, logistics, supply chain)
2. **One financial services domain** (loan origination, payment processing, trading)
3. **One healthcare or education domain**

For each domain, produce:

#### Required Elements

**1. Domain Overview (1 paragraph)**
- What is this domain?
- Why does it exist?
- Who are the primary actors?

**2. Primary Flow Diagram**

Show the happy path from trigger to completion. Include:
- Major steps (boxes)
- Actors responsible (swimlanes or labels)
- Flow direction (arrows)
- Decision points (diamonds)

Example format:
```
[Customer] ──> (Places Order) ──> [System] ──> (Checks Inventory)
                                      │
                        ┌─────────────┴─────────────┐
                        │                           │
                    [In Stock]                 [Out of Stock]
                        │                           │
                        ▼                           ▼
                  (Ship Order)                (Backorder)
```

**3. Three-Flow Separation**

Describe each:
- **Physical flow:** What moves through space?
- **Information flow:** What data moves between whom?
- **Value flow:** Where is value created and transferred?

**4. Key Handoffs (at least 2)**

For each handoff:
- What transfers?
- From whom to whom?
- What documentation is required?
- What can go wrong?

**5. Bottleneck Identification**

Based on your understanding:
- Where is the likely bottleneck?
- Why?
- What evidence would confirm it?

**6. Software Implications (3-5 bullet points)**

If you were building software for this domain:
- What would the system need to track?
- What decisions need automation vs human judgment?
- Where are the critical integration points?
- What's the appropriate architectural complexity?

---

## Example: Financial Services — Loan Origination

**Domain Overview:**

Consumer applies for personal loan from bank. Bank evaluates creditworthiness, approves/denies, and disburses funds. Domain involves customer, loan officer, underwriting team, credit bureau, and disbursement system.

**Primary Flow Diagram:**

```
┌────────────────────────────────────────────────────────────────┐
│                    LOAN ORIGINATION FLOW                       │
└────────────────────────────────────────────────────────────────┘

[Customer] → Application → [Loan Officer] → Pre-Screening → [System]
                                                                │
                                                    ┌───────────┴───────────┐
                                                    │                       │
                                              [Pre-Qualified]         [Declined]
                                                    │                       │
                                                    ▼                       ▼
                                            Credit Check              (Notify Customer)
                                            (Credit Bureau)
                                                    │
                                        ┌───────────┴───────────┐
                                        │                       │
                                  [Score > 650]           [Score < 650]
                                        │                       │
                                        ▼                       ▼
                                  Underwriting             (Decline)
                                  [Underwriter]
                                        │
                            ┌───────────┴───────────┐
                            │                       │
                      [Approved]               [Denied]
                            │                       │
                            ▼                       ▼
                    Loan Documents            (Notify)
                    [Customer Signs]
                            │
                            ▼
                      Disbursement
                    [Funds Transfer]
                            │
                            ▼
                      [COMPLETED]
```

**Three-Flow Separation:**

**Physical flow:**
- Minimal (mostly digital)
- Exception: Paper documents (if required) mailed to customer
- Signed documents scanned/returned

**Information flow:**
1. Customer → System: Application data (personal info, income, debt)
2. System → Credit Bureau: Request credit report
3. Credit Bureau → System: Credit report + score
4. System → Underwriter: Complete application package
5. Underwriter → System: Approval decision + terms
6. System → Customer: Approval notification + document package
7. Customer → System: Signed documents
8. System → Bank: Transfer instruction
9. Bank → Customer: Funds deposited

**Value flow:**
- Customer seeks value: liquidity (cash now)
- Bank seeks value: interest income over time
- Value exchange: Bank gives $10,000 now, customer repays $12,000 over 3 years
- Risk: Customer may default (bank loses value)

**Key Handoffs:**

| Handoff | From | To | Documentation | Risk |
|---------|------|----|--------------| ------|
| Credit decision | Underwriter | System | Approval memo, terms | Manual error, inconsistent criteria |
| Loan agreement | Bank | Customer | Loan contract, disclosures | Customer doesn't understand terms |
| Disbursement authorization | System | Bank core | Transfer instruction | Wrong account, wrong amount |

**Bottleneck Identification:**

**Likely bottleneck:** Underwriting (manual review)

**Why:**
- Requires human judgment (can't fully automate)
- Limited number of underwriters
- Each loan takes 30-60 minutes to review
- Capacity: ~10 loans per underwriter per day

**Evidence:**
- Applications queue up waiting for underwriter
- Automated steps (credit check, document generation) are instant
- Customer complaints about "waiting for approval"

**Software Implications:**

1. **Workflow management:** System must track loan state (applied, credit-checked, under review, approved, disbursed)
2. **Integration points:** Credit bureau API, document signing service, core banking system
3. **Rules engine:** Pre-screening rules (minimum income, debt-to-income ratio) should be configurable
4. **Audit trail:** Every decision, state change, and actor must be logged (regulatory requirement)
5. **Work queue:** Underwriters need prioritized queue (oldest first, urgent cases flagged)
6. **Architectural complexity:** Medium — bounded contexts (application, credit, underwriting, disbursement), event-driven for state changes

---

## Reflection Questions

1. **Pattern recognition:** Across the three domains you mapped, what patterns did you notice? (handoffs, approvals, state transitions?)

2. **Information gaps:** Where in your domain maps do you NOT know what really happens? How would you learn?

3. **Value creation:** For each domain, identify the EXACT moment value is created vs transferred. Are they the same? If not, what are the implications?

4. **Software boundaries:** If you were splitting each domain into microservices, where would you draw the boundaries? What defines a "bounded context"?

5. **Your experience:** Think of a software project that failed or struggled. Did the team understand the domain? What was misunderstood?

---

## Key Takeaways

1. **Three flows always:** Physical, information, value — map all three
2. **Actors before actions:** Know who participates before diagramming what happens
3. **Handoffs hide problems:** Responsibility transfers are where errors and delays occur
4. **Value flow reveals incentives:** Follow the money to understand why people behave as they do
5. **Domain complexity drives architecture:** Match your technical approach to domain reality
6. **Observe first, design later:** Never start coding before understanding the domain

**Next week:** We'll learn **Causal Loop Diagrams (CLDs)** — a powerful technique for revealing hidden feedback structures that drive system behavior.

---

## Additional Resources

**Books:**
- *Domain-Driven Design* by Eric Evans (Chapters 1-4 for context)
- *Business Model Generation* by Osterwalder (Business Model Canvas)
- *Value Stream Mapping* by Karen Martin (Lean approach to flow)

**Tools:**
- Draw.io, Lucidchart, Miro (diagramming)
- BPMN.io (business process modeling)
- Physical paper and sticky notes (seriously — best for initial mapping)

**Practice:**
- Next time you use a service (bank, doctor, delivery), map the flows
- Shadow someone at work for a day, diagram what they do
- Interview a domain expert, draw the process while they talk

---

*The greatest leverage in software engineering is understanding the domain better than your competitors. Master this skill, and you'll build software that actually solves real problems.*

**End of Week 3**
