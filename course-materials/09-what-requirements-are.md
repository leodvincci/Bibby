# Week 9: What Requirements Actually Are
## Beyond "Build Me a Login Screen" — Understanding What Software Must Do and Why

---

## Introduction: The $3 Million Misunderstanding

A financial services company hired a development team to build a "transaction processing system." Six months and $3 million later, the software was rejected.

**What the business stakeholders thought they asked for:**
"A system that processes customer transactions with proper audit trails for regulatory compliance."

**What the developers built:**
A technically impressive system that:
- Processed transactions at 10,000 TPS
- Had beautiful microservices architecture
- Used event sourcing and CQRS
- Had 95% test coverage

**What was missing:**
- The system couldn't reverse erroneous transactions (regulatory requirement)
- It didn't generate the specific audit reports regulators required
- It couldn't handle the 47 different transaction types the business actually processed
- It didn't enforce credit limits before processing
- It couldn't pause suspicious transactions for fraud review

**The problem:** Nobody wrote down **actual requirements**.

The developers heard "transaction processing" and built what they thought that meant. The business assumed developers would "figure out the details." Neither side asked the fundamental questions:

- **What** must the system do? (Functional requirements)
- **How well** must it do it? (Non-functional requirements)
- **What rules** govern behavior? (Business rules)
- **Why** does each requirement exist? (Justification and traceability)
- **Who** cares about each requirement? (Stakeholders)

**This week, you'll learn what requirements actually are** — and how to capture them so software meets real needs instead of imagined ones.

---

## COURSE 3: Requirements Engineering & Domain Understanding

**Course Goal:** Learn to gather, analyze, document, and validate requirements that accurately capture what software must do in complex real-world domains.

**Why this matters:**
- Most project failures come from requirement problems, not technical problems
- Requirements are the contract between business needs and technical implementation
- Good requirements enable accurate estimation, testing, and delivery
- Domain understanding prevents building the wrong thing well

**Course structure:**
- **Week 9:** What requirements actually are (this week)
- **Week 10:** Requirements elicitation techniques
- **Week 11:** State, events & transitions
- **Week 12:** Use cases, user stories & edge cases
- **Week 13:** Requirements validation & verification (PROJECT 3)

---

## Core Concept 1: Requirements vs Specifications vs Implementation

### The Three Levels

**Requirement (WHAT):** What the system must accomplish from a user/business perspective.

**Specification (HOW - High Level):** How the system will satisfy the requirement (design decisions).

**Implementation (HOW - Low Level):** The actual code, architecture, and technology choices.

### Example: User Authentication

**Requirement:**
> "The system shall allow only authorized users to access customer financial data."

**Specification:**
> "The system will implement multi-factor authentication using username/password plus SMS or authenticator app. Sessions will timeout after 15 minutes of inactivity. Failed login attempts will lock the account after 5 tries."

**Implementation:**
> Uses Spring Security with BCrypt password hashing, JWT tokens for session management, integration with Twilio for SMS, PostgreSQL for user storage, Redis for session tracking.

**Key distinction:**
- Requirement = **business need** (must protect customer data)
- Specification = **design choice** (how we'll achieve it)
- Implementation = **technical details** (technologies and code)

**Why this matters:** Requirements should be relatively stable. Specifications and implementation can change as you learn more or technology evolves.

### Example: Terminal Truck Scheduling

**Requirement:**
> "Terminal schedulers shall be able to prevent double-booking of loading bays to avoid truck conflicts and wait times."

**Specification:**
> "The system will maintain a reservation calendar showing 30-minute time slots for each loading bay. When a scheduler assigns a truck to a bay/time, the system will mark that slot as unavailable. The system will alert the scheduler if they attempt to book an occupied slot."

**Implementation:**
> Calendar data stored in PostgreSQL with unique constraints on (bay_id, timeslot). Backend API validates availability before confirming bookings. Frontend uses React calendar component with real-time WebSocket updates.

**Notice:** The requirement doesn't specify calendars, databases, or WebSockets. It specifies the **outcome** (prevent double-booking). The specification and implementation are how we deliver that outcome.

### The Danger of Over-Specifying

**Bad requirement (actually a specification in disguise):**
> "The system shall use a PostgreSQL database with a bay_schedule table having columns: bay_id (integer FK), truck_id (integer FK), scheduled_time (timestamp), duration (interval)."

**Problem:** This dictates **how**, not **what**. It prevents exploring better solutions.

**Good requirement:**
> "The system shall maintain scheduling data with sufficient integrity to prevent double-bookings and provide audit history of all schedule changes."

**Now** you're free to use PostgreSQL, MongoDB, or even a blockchain if that's somehow optimal. The requirement focuses on **outcomes**, not implementation.

---

## Core Concept 2: Functional vs Non-Functional Requirements

### Functional Requirements (WHAT the system does)

**Definition:** Specific behaviors or functions the system must perform.

**Characteristics:**
- Describes interactions between system and users/external systems
- Usually triggered by user actions or events
- Produces observable outputs or state changes
- Can be directly tested ("Does it do X?")

**Examples:**

**E-commerce:**
- The system shall allow customers to add items to a shopping cart
- The system shall calculate total price including applicable taxes
- The system shall send order confirmation emails within 5 minutes of purchase

**Terminal Operations:**
- The system shall record meter readings for each product loadout
- The system shall generate custody transfer documents with timestamp, product, quantity, and quality specs
- The system shall alert operators when tank levels reach 90% capacity

**Healthcare:**
- The system shall allow physicians to prescribe medications from a formulary
- The system shall check for drug-drug interactions before confirming prescriptions
- The system shall maintain complete medication history for each patient

### Non-Functional Requirements (HOW WELL it does it)

**Definition:** Constraints and qualities that affect how the system delivers functionality.

**Categories:**

**1. Performance**
- Response time
- Throughput
- Resource usage (CPU, memory, disk)

**Example:**
> "The system shall process payment transactions with 95th percentile response time under 2 seconds."

**2. Scalability**
- Concurrent users
- Data volume
- Transaction load

**Example:**
> "The system shall support 50 concurrent terminal schedulers managing up to 1,000 daily appointments across 100 terminals."

**3. Availability / Reliability**
- Uptime requirements
- Mean time between failures (MTBF)
- Mean time to recover (MTTR)

**Example:**
> "The system shall maintain 99.9% uptime during business hours (6 AM - 8 PM local time)."

**4. Security**
- Authentication / authorization
- Data encryption
- Audit logging

**Example:**
> "The system shall encrypt all customer data at rest using AES-256 and in transit using TLS 1.3."

**5. Usability**
- Learnability (time to competence)
- Efficiency (time per task)
- Error rates

**Example:**
> "Terminal operators with 5+ years experience shall be able to complete standard truck loadout workflow in under 3 minutes."

**6. Maintainability**
- Code quality
- Documentation
- Testability

**Example:**
> "The system shall maintain automated test coverage above 80% for all business logic."

**7. Compliance / Regulatory**
- Legal requirements
- Industry standards
- Audit requirements

**Example:**
> "The system shall maintain immutable audit logs of all financial transactions for 7 years per Sarbanes-Oxley requirements."

### Functional vs Non-Functional Examples

**Scenario: Search feature**

**Functional:**
- Users can search products by name, category, or SKU
- Search results display product name, image, price, and availability
- Users can filter results by price range and category

**Non-Functional:**
- Search results shall return in under 500ms for 95% of queries
- Search shall handle 10,000 concurrent search requests
- Search relevance ranking shall use industry-standard algorithms
- Search shall be accessible per WCAG 2.1 AA standards

---

## Core Concept 3: Business Rules vs System Behaviors

### Business Rules

**Definition:** Policies, regulations, and constraints that govern how the business operates — independent of any software system.

**Business rules exist whether or not you build software.** Software enforces them.

**Characteristics:**
- Come from domain experts, not developers
- Change based on business decisions, not technical decisions
- May be regulatory (can't change) or internal policy (can change)
- Often expressed as conditional logic ("if X then Y")

**Examples:**

**Banking:**
- **Rule:** Customers under 18 cannot open accounts without a parent/guardian co-signer
- **Rule:** Wire transfers over $10,000 require manager approval
- **Rule:** Overdraft fees are waived for accounts with balance over $5,000

**Petroleum Terminal:**
- **Rule:** Product grades cannot be mixed in the same storage tank
- **Rule:** Trucks must be loaded in order of scheduled appointment unless priority override approved by terminal manager
- **Rule:** Drivers must present commercial driver's license (CDL) and valid TWIC card

**Healthcare:**
- **Rule:** Controlled substances require DEA license verification before prescribing
- **Rule:** Patients under 18 require parental consent for non-emergency treatment
- **Rule:** Medication allergies must be checked before administering any drug

### System Behaviors

**Definition:** How the software system responds to inputs, events, or state changes.

**System behaviors implement business rules** (and other requirements).

**Example: Enforcing a Business Rule**

**Business Rule:**
> "Orders over $10,000 require manager approval before processing."

**System Behaviors (how software enforces the rule):**
- When user submits order, system calculates total
- IF total > $10,000 THEN:
  - System changes order status to "Pending Approval"
  - System sends notification to manager queue
  - System prevents order from moving to fulfillment
  - System displays "Awaiting manager approval" message to user
- IF total <= $10,000 THEN:
  - System changes order status to "Confirmed"
  - System triggers fulfillment process

**Notice:** The rule is one sentence. The system behaviors are multiple steps.

### Capturing Business Rules

**Use a consistent format:**

```
RULE [ID]: [Name]
Type: [Validation / Computation / Inference / Constraint]
Source: [Who defined this rule]
Justification: [Why this rule exists]
Statement: [Precise rule definition]
Examples: [Concrete scenarios]
Exceptions: [When rule doesn't apply]
```

**Example:**

```
RULE BR-047: Manager Approval for High-Value Orders
Type: Constraint
Source: Finance department policy (2019)
Justification: Fraud prevention and cash flow management
Statement: Orders with total value exceeding $10,000 (before tax)
           require approval from a manager with budget authority
           before processing payment or fulfillment.
Examples:
  - $9,999 order → No approval needed
  - $10,001 order → Requires approval
  - $15,000 order from government agency (pre-approved account) → Exempt
Exceptions:
  - Pre-approved corporate accounts (account.preapproved = true)
  - Emergency orders authorized by VP or above
Related Requirements: REQ-103, REQ-104
```

---

## Core Concept 4: Stakeholder Identification

### Who Cares About Requirements?

**Stakeholders** are people who:
- Use the system directly
- Are affected by the system
- Make decisions about the system
- Pay for the system
- Maintain the system
- Regulate the system

**Missing a stakeholder = Missing requirements.**

### Stakeholder Categories

**1. Primary Users (Direct Interaction)**
- Use the system daily
- Their workflow depends on it
- Most visible beneficiaries

**Example (Terminal system):**
- Terminal schedulers
- Loading bay operators
- Drivers (via mobile app)

**2. Secondary Users (Indirect Interaction)**
- Use the system occasionally
- Consume data from the system
- Affected by system outputs

**Example:**
- Terminal managers (reports)
- Accounting (billing integration)
- Customer service (status inquiries)

**3. Decision Makers (Approval Authority)**
- Control budget
- Approve scope
- Set priorities

**Example:**
- Operations director
- IT director
- Finance VP

**4. Subject Matter Experts (Domain Knowledge)**
- Understand business rules
- Know edge cases
- Validate requirements

**Example:**
- Senior terminal scheduler
- Compliance officer
- Safety manager

**5. Technical Stakeholders (Build/Maintain)**
- Architects
- Developers
- QA engineers
- DevOps

**6. External Stakeholders**
- Regulatory bodies
- Integration partners
- End customers (may not use system directly)

### Stakeholder Mapping Exercise

**For any system, create a table:**

| Stakeholder | Type | Interest | Influence | Key Requirements |
|-------------|------|----------|-----------|------------------|
| Terminal Scheduler | Primary User | High | Medium | Easy scheduling, conflict prevention |
| Operations Manager | Decision Maker | High | High | Reports, KPIs, cost savings |
| Driver | Primary User | Medium | Low | Mobile access, real-time updates |
| Finance | Secondary User | Medium | High | Billing integration, audit trails |
| Compliance Officer | SME | Medium | Medium | Regulatory reporting, data retention |

**Use this to:**
- Prioritize who to interview first (high interest + high influence)
- Ensure all perspectives represented
- Identify potential conflicts between stakeholder needs
- Plan communication strategy

---

## Core Concept 5: Requirements Hierarchy

### The Pyramid of Requirements

```
                        ┌─────────────────┐
                        │    Business     │
                        │   Requirements  │  (Why build this?)
                        └────────┬────────┘
                                 │
                    ┌────────────┴────────────┐
                    │       User / System     │
                    │      Requirements       │  (What must it do?)
                    └────────────┬────────────┘
                                 │
                  ┌──────────────┴──────────────┐
                  │        Functional           │
                  │       Requirements          │  (Specific behaviors)
                  └──────────────┬──────────────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         │              Component / Module                │
         │               Requirements                     │  (Implementation details)
         └────────────────────────────────────────────────┘
```

### Business Requirements (Strategic Level)

**Definition:** High-level goals and objectives. Why are we building this?

**Example:**
> "Increase terminal throughput by 15% while reducing truck wait times to improve customer satisfaction and competitive positioning."

**Characteristics:**
- Tied to business strategy
- Measurable outcomes
- Not specific about how

### User Requirements (Tactical Level)

**Definition:** What users need to accomplish their goals.

**Example:**
> "Terminal schedulers need to efficiently manage truck appointments to prevent conflicts and optimize bay utilization."

**Characteristics:**
- User/stakeholder-centric
- Describes needs, not features
- Still relatively high-level

### Functional Requirements (Detailed Level)

**Definition:** Specific system capabilities.

**Example:**
> "The system shall allow schedulers to create appointments by specifying truck ID, product type, quantity, requested date/time, and assigned loading bay."

**Characteristics:**
- Specific and testable
- Describes system behavior
- Forms the actual contract for development

### Component Requirements (Implementation Level)

**Definition:** Specific technical requirements for subsystems or modules.

**Example:**
> "The appointment validation module shall query the bay availability service via REST API and return conflict status within 100ms."

**Characteristics:**
- Technical and detailed
- Architecture/design decisions embedded
- Developer-facing

### Traceability Through the Hierarchy

**Every functional requirement should trace back to a business need.**

**Example:**

```
BUSINESS REQUIREMENT BR-001:
Reduce truck wait times to improve customer satisfaction

  ↓ (supports)

USER REQUIREMENT UR-012:
Schedulers need to prevent appointment conflicts

  ↓ (realizes)

FUNCTIONAL REQUIREMENT FR-089:
System shall detect and prevent double-booking of loading bays

  ↓ (implemented by)

COMPONENT REQUIREMENT CR-234:
Bay availability service shall enforce unique constraint on (bay_id, timeslot)
```

**Why traceability matters:**
- **Justification:** "Why does this requirement exist?" → Traces to business need
- **Impact analysis:** "What if we change this rule?" → See what depends on it
- **Scope management:** "Is this feature necessary?" → Does it trace to business requirement?
- **Testing:** "How do we validate this?" → Test functional requirements that implement user needs

---

## Core Concept 6: Writing Good Requirements

### Characteristics of Good Requirements

**SMART criteria (adapted for requirements):**

**Specific:** Clear, unambiguous language
- ❌ "The system should be fast"
- ✅ "The system shall return search results in under 500ms for 95% of queries"

**Measurable:** Can verify if requirement is met
- ❌ "The system should be user-friendly"
- ✅ "New users shall complete their first appointment booking in under 5 minutes"

**Achievable:** Technically and economically feasible
- ❌ "The system shall predict equipment failures with 100% accuracy"
- ✅ "The system shall predict equipment failures with 85% accuracy (validated against historical data)"

**Relevant:** Tied to business/user needs
- ❌ "The system shall display data in Comic Sans font"
- ✅ "The system shall display critical alerts in high-contrast colors for visibility"

**Testable:** Can create test case to verify
- ❌ "The system shall be secure"
- ✅ "The system shall prevent unauthorized access by requiring authentication for all endpoints"

### Requirements Language Patterns

**Use "shall" for mandatory requirements:**
> "The system **shall** validate truck CDL numbers before confirming appointments."

**Use "should" for desired but not critical:**
> "The system **should** pre-fill driver information from previous visits."

**Use "may" for optional:**
> "The system **may** offer appointment suggestions based on historical patterns."

**Avoid weak language:**
- ❌ "The system will probably..."
- ❌ "The system might..."
- ❌ "The system could potentially..."

### Avoiding Ambiguity

**Ambiguous words to avoid:**

| Ambiguous | Better |
|-----------|--------|
| "fast" | "within 2 seconds" |
| "recent" | "within the last 7 days" |
| "large" | "exceeding 100 MB" |
| "frequently" | "at least 10 times per day" |
| "appropriate" | "meeting WCAG 2.1 AA standards" |
| "if needed" | "under the following conditions: ..." |
| "as much as possible" | "minimum of X, target of Y" |

---

## Practical Framework: Requirements Taxonomy

### Organize Requirements by Type

**Template:**

```
[REQ-ID]: [Requirement Statement]
Type: [Functional | Non-Functional | Business Rule | Constraint]
Category: [Security | Performance | Usability | etc.]
Priority: [Must Have | Should Have | Nice to Have]
Source: [Stakeholder name or document]
Rationale: [Why this requirement exists]
Acceptance Criteria: [How we test this]
Dependencies: [Related requirements]
Status: [Proposed | Approved | Implemented | Verified]
```

---

## Hands-On Exercise: Requirement Identification

### Deliverable: Requirements for Common Systems

**Choose ONE of these systems:**

1. **E-commerce checkout process**
2. **Appointment scheduling system** (doctor, salon, service center)
3. **Document management system**
4. **Expense report submission and approval**

For your chosen system, produce:

#### 1. Stakeholder List (10+ stakeholders)

Identify:
- Primary users (who uses it directly)
- Secondary users (affected but don't use directly)
- Decision makers
- Subject matter experts
- Technical stakeholders
- External stakeholders

#### 2. Business Requirements (3 high-level goals)

**Format:**
> "Increase/Reduce/Improve [metric] by [amount] to achieve [business outcome]"

**Example:**
> "Reduce checkout abandonment rate from 35% to under 25% to increase completed purchases and revenue."

#### 3. Functional Requirements (10 minimum)

List specific behaviors the system must perform.

**Format:**
> "The system shall [action] [object] [conditions/constraints]"

**Example:**
> "The system shall display itemized pricing breakdown including subtotal, tax, shipping, and total before payment confirmation."

#### 4. Non-Functional Requirements (5 minimum)

Cover at least 3 different categories (performance, security, usability, etc.)

**Example:**
> "The system shall process payments with 99.99% reliability (max 1 failure per 10,000 transactions)."

#### 5. Business Rules (5 minimum)

Identify domain-specific rules that constrain behavior.

**Example:**
> "Orders over $5,000 require phone verification before shipping to prevent fraud."

#### 6. Requirement Traceability (for 3 functional requirements)

Show how 3 of your functional requirements trace back to business goals.

**Format:**
```
FR-005: System shall save cart contents for 30 days
  ← Supports UR-002: Users need to resume shopping across sessions
    ← Supports BR-001: Reduce cart abandonment from 35% to 25%
```

---

## Real-World Example: Terminal Truck Scheduling Requirements

### Sample Requirements Document Extract

**Business Requirement BR-001:**
> Improve terminal operational efficiency by reducing scheduling conflicts by 70% and average truck wait times from 47 minutes to under 30 minutes, resulting in increased throughput and customer satisfaction.

**User Requirements:**

**UR-001:** Terminal schedulers need to view real-time bay availability
**UR-002:** Drivers need to know their appointment time and bay assignment in advance
**UR-003:** Terminal managers need visibility into scheduling efficiency metrics

**Functional Requirements:**

**FR-010:** The system shall display a calendar view showing all loading bays and their availability in 30-minute increments for rolling 7-day window.

**FR-011:** The system shall allow schedulers to create appointments by selecting bay, date/time, and entering truck/customer information.

**FR-012:** The system shall validate appointment requests against bay availability and product compatibility rules before confirming.

**FR-013:** The system shall send SMS notification to driver 24 hours and 2 hours before scheduled appointment time.

**FR-014:** The system shall prevent double-booking by enforcing unique constraint on (bay_id, timeslot).

**FR-015:** The system shall allow authorized users to override appointments with mandatory justification note.

**Non-Functional Requirements:**

**NFR-020:** The system shall respond to appointment booking requests within 2 seconds under normal load (50 concurrent users).

**NFR-021:** The system shall maintain 99.5% uptime during business hours (6 AM - 8 PM local time, 7 days/week).

**NFR-022:** The system shall support 100 concurrent terminals with up to 50 daily appointments each (5,000 appointments/day system-wide).

**NFR-023:** The system shall encrypt all data in transit using TLS 1.3 and at rest using AES-256.

**Business Rules:**

**BR-010:** Appointments cannot be scheduled less than 2 hours in advance (emergency overrides require terminal manager approval).

**BR-011:** Loading bays can only handle one product grade at a time (must be cleaned between different grades).

**BR-012:** Trucks must arrive within 30-minute window of appointment time or appointment is forfeit (grace period for documented delays).

**BR-013:** Appointments can be rescheduled up to 4 hours before scheduled time without penalty.

### Traceability Example

```
BR-001: Reduce scheduling conflicts by 70%
  ↓
UR-001: Schedulers need real-time visibility
  ↓
FR-010: Display calendar with bay availability
FR-012: Validate appointments against availability
FR-014: Prevent double-booking
  ↓
NFR-020: Respond within 2 seconds (real-time feel)
```

---

## Reflection Questions

1. **Past projects:** Think of a project that failed or struggled. Were requirements clearly documented? What was missing or ambiguous?

2. **Business rules:** In your domain, what are 5 business rules that must be enforced? Are they documented anywhere?

3. **Stakeholders:** For a system you've worked on, who were the stakeholders? Did you talk to all of them? Who was missed?

4. **Non-functional requirements:** Think of a system that's slow or unreliable. Were performance/reliability requirements specified upfront?

5. **Traceability:** Can you trace any feature you've built back to a business need? What was the justification?

---

## Key Takeaways

1. **Requirements ≠ Specifications ≠ Implementation:** Separate WHAT (requirement), HOW (specification), HOW EXACTLY (implementation)
2. **Functional = WHAT, Non-Functional = HOW WELL:** Both are critical; missing non-functionals leads to performance/security problems
3. **Business rules come from the domain:** They exist independent of software; software enforces them
4. **Identify all stakeholders:** Each stakeholder brings unique requirements; missing one = missing requirements
5. **Requirements hierarchy:** Trace from business goals → user needs → functional requirements → components
6. **Write SMART requirements:** Specific, Measurable, Achievable, Relevant, Testable
7. **Traceability enables justification:** Every requirement should answer "Why does this exist?"

**Next week:** You'll learn **requirements elicitation techniques** — the specific methods for extracting requirements from stakeholders, documents, and observations.

---

## Additional Resources

**Books:**
- *Software Requirements* by Karl Wiegers & Joy Beatty (comprehensive reference)
- *Mastering the Requirements Process* by Suzanne & James Robertson (practical guide)
- *User Stories Applied* by Mike Cohn (agile requirements)

**Standards:**
- IEEE 830: Recommended Practice for Software Requirements Specifications
- ISO/IEC 29148: Systems and software engineering — Life cycle processes — Requirements engineering

**Templates:**
- IEEE 830 SRS template
- Agile user story template (with acceptance criteria)
- Business requirements document (BRD) template

**Practice:**
- Pick 5 apps you use daily; write 10 requirements for each
- Read open source project docs; identify what requirements are documented
- Interview a colleague about their workflow; extract 20 requirements

---

*The best code perfectly implements clear requirements. The worst code perfectly implements unclear requirements. Master requirements, and you'll build the right things right.*

**End of Week 9**
