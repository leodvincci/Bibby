# Week 14: Ubiquitous Language & Bounded Contexts

**Course 4: Domain-Driven Design (DDD) — Week 14 of 26**

---

## The $12M Translation Problem

In 2019, a major logistics company launched an ambitious enterprise system to unify their freight management operations. They had excellent requirements (thanks to a rigorous process like you learned in Course 3), talented engineers, and generous budget.

The system failed spectacularly within 6 months.

**The problem?** Not bad code. Not missing requirements. Not technical debt.

**The problem was language.**

The Sales team called their core entity a "Shipment." The Operations team called it a "Load." The Customer Service team called it an "Order." The Warehouse team called it a "Consignment." Engineering, trying to be neutral, called it a "TransportationRequest."

Nobody understood each other.

When Sales said "The shipment is confirmed," they meant the customer agreed to terms. When Operations said "The load is confirmed," they meant the truck was assigned. When the system displayed "TransportationRequest.status = CONFIRMED," nobody knew what it actually meant.

Requirements were translated into technical specs. Technical specs were translated into code. Code behavior was translated back into business terms. Each translation introduced drift. Within months, the system and the business were speaking completely different languages.

**The cost:** $12M in development costs, 18 months of rework, and damaged customer relationships.

**The lesson:** The most important design decision you make isn't your database schema or your microservice boundaries. It's **what you call things** and **where those names apply**.

Welcome to **Domain-Driven Design (DDD)**.

---

## What Is Domain-Driven Design?

**Domain-Driven Design** is a software development approach introduced by Eric Evans in 2003 that focuses on:

1. **Putting the domain first** — The business problem is more important than the technology
2. **Creating a shared language** — Business and engineering speak the same language
3. **Modeling the domain in code** — Your code structure mirrors the business domain
4. **Managing complexity through boundaries** — Large domains are divided into cohesive contexts

You've spent Course 3 learning how to discover and document requirements. **DDD is how you turn those requirements into code that actually models the domain.**

### Why DDD Matters for Your Background

As someone who worked in petroleum terminal operations, you've already seen what happens when systems don't match reality:

- **Inventory systems** that track "barrels" while operators think in "tank levels"
- **Scheduling systems** that book "time slots" while terminal operators manage "loadout queues"
- **Safety systems** that record "incidents" while field teams report "near misses" and "violations"

The gap between how software describes the world and how the domain actually works creates:
- **Translation overhead** (constant mental mapping between business and code)
- **Subtle bugs** (edge cases that emerge from misalignment)
- **Slow feature development** (every change requires re-translation)
- **Knowledge silos** (only a few people understand both sides)

**DDD eliminates these gaps by making code speak the language of the domain.**

---

## Core Concept 1: Ubiquitous Language

**Ubiquitous Language** is a shared, rigorously defined vocabulary used by:
- Domain experts (operations, business, users)
- Developers (backend, frontend, QA)
- Documentation (requirements, design docs, user guides)
- Code (class names, method names, variables)

**One team. One language. Everywhere.**

### The Problem: Translation Layers

Traditional software development looks like this:

```
Business Stakeholder:
"We need to track when a driver checks in at the gate."

Business Analyst:
"Requirement: System shall record driver arrival timestamp."

Developer:
"I'll add a 'user_entry_time' field to the 'persons' table."

Code:
UPDATE persons SET user_entry_time = NOW() WHERE user_type = 'DRIVER';
```

**Four different vocabularies:**
- Business: "checks in at the gate"
- Requirements: "record driver arrival timestamp"
- Design: "user entry time"
- Code: "user_entry_time"

When a bug emerges ("check-in times are wrong"), it takes hours to trace through the translation layers.

### The DDD Solution: Shared Vocabulary in Code

With Ubiquitous Language:

```
Business Stakeholder:
"We need to track when a driver checks in at the gate."

Developer:
"So we'll call checkInAtGate() on the Driver?"

Code:
public class Driver {
    private GateCheckIn currentCheckIn;

    public void checkInAtGate(Gate gate, Instant checkInTime) {
        this.currentCheckIn = new GateCheckIn(gate, checkInTime);
        // Domain events, validation, etc.
    }
}
```

**One vocabulary.** Business experts can read the code. Developers can speak to operators. When someone says "gate check-in," everyone knows exactly what it means—and where it lives in the system.

### Building a Ubiquitous Language

**Step 1: Extract Domain Terms from Conversations**

During requirements elicitation (Week 10), you talk to domain experts. **Listen for the nouns and verbs they use repeatedly:**

**Petroleum Terminal Example:**
- Nouns: Tank, Product, Load, Carrier, Bay, Meter, Ticket, Release
- Verbs: Schedule, Load, Offload, Sample, Seal, Release, Reconcile
- States: Available, Loading, Loaded, Sealed, Departed
- Constraints: "A bay can only load one carrier at a time"
- Rules: "You can't load incompatible products back-to-back without a flush"

**Step 2: Resolve Ambiguities and Conflicts**

When the same word means different things, **make it explicit:**

**Example: "Load" in a terminal**
- Operations team: "Load" = The physical act of transferring product to a truck
- Logistics team: "Load" = The shipment booking (even before truck arrives)
- Inventory team: "Load" = The quantity of product dispensed

**Resolution:**
- "LoadingOperation" = Physical transfer (Operations context)
- "LoadOrder" = Scheduled shipment (Logistics context)
- "DispensingRecord" = Quantity tracking (Inventory context)

**Step 3: Capture Language in a Glossary**

Maintain a **domain glossary** that defines every term:

```
## Terminal Domain Glossary

**LoadOrder**: A scheduled shipment booking that specifies product, quantity,
carrier, and requested time window. Created by logistics, executed by operations.

**LoadingOperation**: The physical process of transferring product from a tank
to a carrier through a loading bay. Includes meter readings, sampling, and sealing.

**Bay**: A physical loading/unloading point with metering equipment. Can be
truck bay, rail bay, or marine berth.

**Product**: A petroleum product stored in tanks (gasoline, diesel, jet fuel, etc.).
Products have compatibility rules that govern sequencing.

**CarrierTicket**: The legal document recording the transaction, including
bill of lading number, seal numbers, net quantity, and signatures.
```

**Step 4: Enforce Language in Code**

Your class names, method names, and package structure must use the exact terms from the glossary:

```java
package com.terminal.loading;

public class LoadingOperation {
    private LoadOrder order;
    private Bay bay;
    private Product product;
    private CarrierTicket ticket;

    public void beginLoading(Driver driver, Truck truck) {
        // Implementation
    }

    public CarrierTicket completeLoadingAndIssueTicket(MeterReading finalReading) {
        // Implementation
    }
}
```

**Engineers and operators can look at this code and understand it.** No translation required.

### Ubiquitous Language in Different Layers

The language must appear **everywhere:**

**1. Domain Model (Code)**
```java
public class Tank {
    public boolean canAccept(Product product) {
        return this.product.isCompatibleWith(product)
            || this.isEmpty();
    }
}
```

**2. Database Schema**
```sql
CREATE TABLE tanks (
    tank_id VARCHAR(50) PRIMARY KEY,
    product_code VARCHAR(20),
    available_capacity_barrels DECIMAL(10,2)
);
```

**3. API Endpoints**
```
POST /loading-operations
GET /loading-operations/{id}/carrier-ticket
PUT /bays/{bayId}/assign-load-order
```

**4. User Interface**
```
Button: "Begin Loading"
Status: "Loading in Progress"
Alert: "Bay 3 requires flush before next product"
```

**5. Documentation**
```
## LoadingOperation Lifecycle

1. LoadOrder is assigned to a Bay
2. Driver checks in and presents credentials
3. Loading begins when operator confirms truck positioning
4. System records meter readings every 10 seconds
5. Loading completes when target quantity reached
6. CarrierTicket is generated and printed
```

**6. Conversations**
```
Developer: "Should we validate the LoadOrder before assigning it to a Bay?"
Operator: "Yes, check if the Bay is compatible with the Product."
Developer: "Got it, I'll add a compatibility check to Bay.canLoad(Product)."
```

### Anti-Patterns: When Language Breaks Down

**❌ Generic Technical Terms**
```java
public class Entity {
    public void process() {
        // What does this mean??
    }
}
```

**❌ Abbreviations That Domain Experts Don't Use**
```java
public class LdOp {  // Nobody calls it "LdOp"
    private CrTkt tkt;  // Incomprehensible
}
```

**❌ Translation in Method Names**
```java
public void updateDatabaseWithDriverArrivalInformation() {
    // Business says "check in", not "update database with arrival information"
}
```

**✅ Ubiquitous Language**
```java
public class Driver {
    public void checkInAtGate(Gate gate) {
        this.checkInTime = Instant.now();
        this.assignedGate = gate;
    }
}
```

---

## Core Concept 2: Bounded Contexts

**Bounded Context** is the boundary within which a particular domain model applies. Inside the boundary, all terms have specific, unambiguous meanings. Outside the boundary, the same terms might mean something completely different.

### The Problem: One Model to Rule Them All

Early in a project, teams often try to create a **single unified model** for the entire enterprise:

"Let's create ONE definition of 'Customer' that works for Sales, Support, Billing, and Shipping."

This fails because:
- **Sales** cares about "Customer" as a lead (company name, decision makers, deal size)
- **Support** cares about "Customer" as a user (login credentials, ticket history, product version)
- **Billing** cares about "Customer" as an account (payment terms, invoice address, credit limit)
- **Shipping** cares about "Customer" as a delivery destination (ship-to address, dock hours, contact phone)

Trying to merge these into ONE "Customer" entity creates a bloated, incoherent model that serves nobody well.

### The DDD Solution: Multiple Bounded Contexts

Instead of one model, create **separate models for separate contexts:**

**Sales Context:**
```java
public class Customer {
    private CompanyName companyName;
    private Industry industry;
    private List<DecisionMaker> decisionMakers;
    private AnnualRevenue estimatedValue;
    private SalesStage stage;
}
```

**Support Context:**
```java
public class Customer {
    private CustomerId id;
    private List<User> users;
    private ProductLicense license;
    private List<SupportTicket> ticketHistory;
    private SupportTier tier;
}
```

**Billing Context:**
```java
public class Customer {
    private CustomerId id;
    private BillingAddress address;
    private PaymentTerms terms;
    private CreditLimit creditLimit;
    private List<Invoice> invoices;
}
```

**Same word. Different contexts. Different models.**

Each context has its own:
- **Ubiquitous Language** (terms mean specific things inside this boundary)
- **Domain Model** (classes, relationships, rules)
- **Data Schema** (tables, fields, constraints)
- **Team ownership** (who maintains this model)

### Identifying Bounded Contexts

**Signals that you need a context boundary:**

1. **Different meanings for the same term**
   - "Order" in e-commerce (shopping cart) vs "Order" in fulfillment (shipping directive)

2. **Different stakeholders with different concerns**
   - Doctors care about patient medical history
   - Billing staff care about patient insurance and payments

3. **Different rates of change**
   - Product catalog changes frequently (marketing, seasonal)
   - Order history is stable (legal, auditing)

4. **Different non-functional requirements**
   - Inventory (real-time, high consistency)
   - Reporting (batch updates, eventual consistency OK)

5. **Natural organizational boundaries**
   - Sales department vs Operations department
   - Legacy system vs new system

### Petroleum Terminal Example: Bounded Contexts

A petroleum terminal operation has at least **four distinct contexts:**

**1. Scheduling Context**
- **Focus:** Booking load orders, managing time slots, optimizing throughput
- **Key entities:** LoadOrder, TimeSlot, Carrier, Product
- **Stakeholders:** Logistics coordinators, dispatchers
- **Ubiquitous Language:** "appointment," "slot," "booking," "capacity"

**2. Operations Context**
- **Focus:** Physical loading, safety, equipment, real-time execution
- **Key entities:** LoadingOperation, Bay, Tank, Meter, Ticket
- **Stakeholders:** Terminal operators, drivers, safety officers
- **Ubiquitous Language:** "loading," "bay assignment," "meter reading," "seal"

**3. Inventory Context**
- **Focus:** Tank levels, product movements, reconciliation, compliance
- **Key entities:** Tank, Product, Receipt, Withdrawal, InventoryPosition
- **Stakeholders:** Inventory analysts, compliance, finance
- **Ubiquitous Language:** "receipt," "withdrawal," "variance," "reconciliation"

**4. Compliance Context**
- **Focus:** Environmental reporting, safety incidents, regulatory submissions
- **Key entities:** IncidentReport, EmissionRecord, InspectionLog
- **Stakeholders:** Safety managers, environmental compliance, regulators
- **Ubiquitous Language:** "incident," "reportable event," "threshold exceedance"

**Notice:** "Product" appears in multiple contexts, but means different things:
- **Scheduling Context:** Product = scheduling constraint (can't mix certain products)
- **Operations Context:** Product = what's flowing through meters right now
- **Inventory Context:** Product = category for accounting and reconciliation
- **Compliance Context:** Product = hazardous material with regulatory properties

### Context Relationships: Context Mapping

Contexts don't exist in isolation. They **integrate**. The pattern of integration is called a **Context Map**.

**Common Context Relationships:**

**1. Upstream/Downstream**
- **Upstream** context produces data that **downstream** context consumes
- Example: Scheduling (upstream) creates LoadOrders → Operations (downstream) executes them

**2. Shared Kernel**
- Two contexts share a small, common model that both teams agree on
- Example: Both Scheduling and Operations share a "Product compatibility matrix"

**3. Customer/Supplier**
- **Supplier** context provides a service; **Customer** context consumes it
- Supplier team considers customer needs but owns the interface
- Example: Inventory (supplier) provides "current tank levels" → Scheduling (customer) uses it for booking

**4. Anti-Corruption Layer (ACL)**
- When integrating with a legacy or external system, create a translation layer
- Prevents the messy external model from corrupting your clean internal model
- Example: Modern Operations system integrates with legacy SCADA via ACL that translates "PLC signals" into "Loading events"

**5. Conformist**
- Downstream team conforms entirely to upstream model (no translation)
- Used when upstream has strong authority or changing it isn't feasible
- Example: Compliance context conforms to EPA reporting format exactly

### Designing Context Boundaries

**Guidelines for drawing boundaries:**

**Keep together:**
- Entities that **change together** (high cohesion)
- Entities that have **strong business invariants** that must be enforced together
- Entities owned by **the same team**

**Separate:**
- Entities with **different rates of change**
- Entities with **different stakeholders** or **different purposes**
- Entities that can **fail independently** without breaking others

**Example: "Ticket" in Terminal Operations**

**Option 1: Ticket in Operations Context**
```java
public class LoadingOperation {
    private CarrierTicket ticket;

    public CarrierTicket completeLoading(MeterReading reading) {
        this.ticket = new CarrierTicket(this, reading);
        return ticket;
    }
}
```
✅ Good if: Ticket is generated immediately during loading and only used operationally

**Option 2: Ticket in Compliance/Legal Context**
```java
public class TicketArchive {
    public void recordCompletedLoading(LoadingCompletedEvent event) {
        CarrierTicket ticket = new CarrierTicket(event);
        this.archive(ticket);
    }
}
```
✅ Good if: Tickets are legal documents with long retention, auditing, retrieval requirements

**The answer depends on business priorities.** If tickets are primarily operational (quick reference, corrections, reprints), keep them in Operations. If they're primarily legal (7-year retention, audit trails, regulatory submission), move them to Compliance.

---

## Connecting to Course 3: From Requirements to Contexts

In **Course 3**, you learned to elicit and document requirements. Now you'll use those requirements to **discover Bounded Contexts**.

### Requirements Analysis → Context Discovery

**Step 1: Group requirements by stakeholder and concern**

From Week 9-13, you documented requirements for different stakeholders. Group them:

**Requirements from Logistics Coordinator:**
- "System shall allow scheduling load orders 48 hours in advance"
- "System shall prevent double-booking of time slots"
- "System shall display available capacity by product and time"

**→ These belong in the Scheduling Context**

**Requirements from Terminal Operator:**
- "System shall record meter start and stop readings"
- "System shall validate truck positioning before allowing loading to begin"
- "System shall print carrier ticket with bill of lading number"

**→ These belong in the Operations Context**

**Step 2: Look for linguistic boundaries**

Different stakeholders use different terms. These linguistic boundaries often reveal context boundaries.

**Step 3: Map state machines to contexts**

From **Week 11**, you modeled entity lifecycles. Each state machine often belongs to one context:

**LoadOrder state machine** → Scheduling Context
```
Draft → Submitted → Confirmed → Scheduled → Completed
```

**LoadingOperation state machine** → Operations Context
```
Assigned → In Progress → Metering → Completed → Ticketed
```

**Step 4: Identify integration points**

Where do contexts need to communicate?

- Scheduling **creates** a LoadOrder → Operations **receives** it as a work assignment
- Operations **completes** loading → Inventory **updates** tank levels
- Operations **issues** ticket → Compliance **archives** it

These integration points become **context boundaries** in your architecture.

---

## Hands-On Exercise: Identifying Contexts and Building Language

**Choose ONE of these domains** (preferably from your experience):

1. **Petroleum terminal operations**
2. **Hospital emergency department**
3. **E-commerce fulfillment center**
4. **Property management (apartments/commercial)**

**Part 1: Build a Ubiquitous Language Glossary (60-90 minutes)**

1. **List 15-20 domain terms** (nouns, verbs, states) that domain experts use daily
2. **Define each term precisely** in 1-3 sentences (ask: "What does this REALLY mean in this business?")
3. **Identify 3-5 ambiguous terms** that mean different things to different stakeholders
4. **Resolve ambiguities** by creating distinct terms for distinct meanings

**Deliverable format:**
```markdown
# [Domain] Ubiquitous Language Glossary

## Core Entities

**[Term]**: [Definition — what it represents, its key attributes, its role in the domain]

**[Term]**: [Definition...]

## Ambiguous Terms & Resolutions

**Original term:** [Term that means different things]
- **Meaning in [Context A]:** [Definition]
- **Meaning in [Context B]:** [Definition]
- **Resolution:** Use "[TermA]" for [Context A], "[TermB]" for [Context B]
```

**Part 2: Identify Bounded Contexts (45-60 minutes)**

1. **List 3-5 different stakeholder groups** in this domain (roles, departments, teams)
2. **For each stakeholder group, describe their primary concerns** (what do they care about?)
3. **Identify 3-4 Bounded Contexts** based on:
   - Different concerns
   - Different meanings for the same terms
   - Natural organizational boundaries
4. **For each context:**
   - Name it
   - Define its purpose (one sentence)
   - List 5-7 key entities that belong in this context
   - Identify which stakeholder group owns it

**Deliverable format:**
```markdown
# [Domain] Bounded Contexts

## Context 1: [Name]

**Purpose:** [One sentence describing what this context is responsible for]

**Stakeholders:** [Who owns/uses this context]

**Key Entities:**
- [Entity]: [Role in this context]
- [Entity]: [Role in this context]
- ...

**Ubiquitous Language in this context:**
- [Term] means: [Definition specific to this context]
- ...

---

## Context 2: [Name]
...
```

**Part 3: Create a Context Map (30 minutes)**

1. **Draw relationships between your contexts:**
   - Which contexts produce data for other contexts? (upstream/downstream)
   - Which contexts need to integrate?
   - Are there shared models? Legacy systems? External systems?

2. **Label each relationship:**
   - Upstream/Downstream
   - Customer/Supplier
   - Shared Kernel
   - Anti-Corruption Layer

**Deliverable:** A simple diagram (hand-drawn or digital) showing contexts as boxes and relationships as arrows with labels.

---

## Reflection Questions

**On Ubiquitous Language:**

1. **Think of a project where business and engineering spoke different languages.** What was the cost? How much time was spent "translating"? Did misunderstandings cause bugs?

2. **Look at code you've written recently.** Can a domain expert read the class and method names and understand what the code does? Or is it full of technical terms like "handler," "processor," "manager," "service"?

3. **How would you introduce Ubiquitous Language to a team that's been using generic technical terms for years?** What would you do with existing code?

**On Bounded Contexts:**

4. **Why is it better to have multiple small models (Bounded Contexts) instead of one large unified model?** What are the trade-offs?

5. **How do organizational boundaries (departments, teams) relate to context boundaries?** Should they always align? When might they diverge?

6. **In your petroleum terminal experience (or another domain), can you identify moments where different departments used the same word to mean different things?** How did that cause confusion?

**On Integration:**

7. **If two Bounded Contexts need to share data, why not just share the same database tables?** What problems does that create?

8. **When would you use an Anti-Corruption Layer vs just conforming to an upstream system's model?**

---

## Key Takeaways

✅ **Ubiquitous Language is a shared vocabulary** used consistently by domain experts, developers, and code. It eliminates translation layers and ensures everyone speaks the same language.

✅ **Bounded Contexts are explicit boundaries** where a particular model applies. The same term can mean different things in different contexts—and that's OK as long as the boundaries are clear.

✅ **Context boundaries follow natural splits:** different stakeholders, different concerns, different meanings for terms, different rates of change.

✅ **Requirements from Course 3 map to Bounded Contexts** — grouping requirements by stakeholder and concern reveals context boundaries.

✅ **Contexts integrate through explicit relationships** (upstream/downstream, customer/supplier, anti-corruption layers). Integration is designed, not accidental.

✅ **DDD isn't just about code—it's about alignment** between how the business thinks and how software is structured.

✅ **Your operational domain knowledge is a competitive advantage** — you already know the real language and natural boundaries. Now you'll encode them in software.

---

## Connection to Week 15

This week you learned the **strategic** foundations of DDD: Ubiquitous Language (what we call things) and Bounded Contexts (where those names apply).

Next week you'll dive into **tactical** DDD patterns: **Entities, Value Objects, and Aggregates**—the building blocks you use to actually MODEL the domain in code.

You'll learn:
- How to distinguish between **Entities** (things with identity) and **Value Objects** (things defined by attributes)
- How to design **Aggregates** that enforce business invariants
- How to translate requirements (from Course 3) into domain models (in DDD)

**Get ready to write domain models that business experts can validate.**

---

## Additional Resources

**Books:**
- *Domain-Driven Design* by Eric Evans (2003) — The foundational text, especially Part II (Strategic Design)
- *Implementing Domain-Driven Design* by Vaughn Vernon (2013) — Practical, detailed, excellent for Java/Spring developers

**Articles:**
- Martin Fowler: "Bounded Context" (martinfowler.com/bliki/BoundedContext.html)
- Eric Evans: "Getting Started with DDD When Surrounded by Legacy Systems"

**For Your Context:**
- Study how industrial automation systems define domains (process control vs scheduling vs inventory)
- Examine API documentation from terminal management systems — do they use domain language or technical jargon?
- Talk to former colleagues about terms they use daily vs terms the software uses

---

**End of Week 14 — Ubiquitous Language & Bounded Contexts**

**Next:** Week 15 — Entities, Value Objects & Aggregates
