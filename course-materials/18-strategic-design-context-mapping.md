# Week 18: Strategic Design & Context Mapping + PROJECT 4

**Course 4: Domain-Driven Design (DDD) — Week 18 of 26**

---

## The $25M Integration Nightmare

In 2015, a major retail company embarked on a modernization effort. They had identified six bounded contexts (from Event Storming workshops, no less):

- **Catalog** — Product listings, pricing, inventory
- **Shopping** — Cart, checkout, promotions
- **Fulfillment** — Warehouse, shipping, tracking
- **Customer** — Accounts, preferences, history
- **Payment** — Billing, refunds, fraud detection
- **Analytics** — Reporting, forecasting, insights

Each context was assigned to a different team. Each team built their service. Six months later, they tried to integrate.

**Disaster.**

- Catalog team expected Shopping to "just use our database directly"
- Shopping team built their own product cache but didn't tell Catalog
- Fulfillment team needed order data but Shopping's API changed constantly
- Customer team refused to share data ("security concerns")
- Payment team wanted synchronous confirmations, but Shopping needed async
- Analytics wanted real-time data, but nobody designed for that

**The problems:**
- No one designed the **integration patterns**
- Teams had conflicting expectations about **who's upstream/downstream**
- No shared understanding of **dependencies**
- No agreement on **data ownership**
- No plan for **versioning and evolution**

**The result:** $25M spent on rewrites, architectural revisions, and mediation meetings. Three teams quit in frustration. Launch delayed 18 months.

**The lesson:** Identifying Bounded Contexts (Week 14) is necessary but **not sufficient**. You must also design **how contexts relate to each other**.

That's **Strategic Design and Context Mapping**.

Welcome to **Week 18**, the capstone of Course 4: Domain-Driven Design.

---

## Strategic Design vs Tactical Design

DDD has two levels:

### Tactical Design

**What you've learned in Weeks 15-16:**
- Entities, Value Objects, Aggregates (Week 15)
- Domain Events, Commands (Week 16)
- Event Storming (Week 17)

**Focus:** Building blocks **inside** a Bounded Context.

**Questions:** "How do I model this concept?" "Is it an Entity or Value Object?" "Where's the aggregate boundary?"

### Strategic Design

**What you'll learn this week:**
- Bounded Contexts (Week 14 introduced them)
- Context relationships and integration patterns
- Team topology and ownership
- Architecture at scale

**Focus:** Relationships **between** Bounded Contexts.

**Questions:** "How do contexts integrate?" "Who depends on whom?" "How do teams collaborate?" "What happens when requirements conflict?"

### Why Strategic Design Matters

**You can have perfect tactical design** (beautiful entities, value objects, aggregates) **but fail at the system level** if contexts don't integrate well.

**Strategic design prevents:**
- Tight coupling between teams
- Accidental dependencies
- Integration chaos
- Conflicting data models leaking across boundaries
- Teams blocking each other

**Strategic design enables:**
- Independent team evolution
- Clear ownership and responsibility
- Intentional integration patterns
- Scalable architecture
- Managed complexity

---

## Core Concept: Context Mapping

A **Context Map** is a visual representation showing:

1. **All Bounded Contexts** in the system
2. **Relationships between contexts** (who depends on whom)
3. **Integration patterns** (how contexts communicate)
4. **Team ownership** (who's responsible for what)

**Example: Petroleum Terminal Context Map**

```
┌─────────────────┐
│   Scheduling    │─────────┐
│    Context      │         │
│  (Logistics)    │         │ LoadOrderScheduled
└─────────────────┘         │ (event)
                            ↓
                   ┌─────────────────┐
                   │   Operations    │
                   │    Context      │
                   │   (Terminal)    │
                   └─────────────────┘
                            │
                            │ LoadingCompleted
                            │ (event)
         ┌──────────────────┼──────────────────┐
         ↓                  ↓                  ↓
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│  Inventory  │   │   Billing   │   │ Compliance  │
│   Context   │   │   Context   │   │   Context   │
│ (Analysis)  │   │  (Finance)  │   │  (Safety)   │
└─────────────┘   └─────────────┘   └─────────────┘
```

**What the map shows:**
- **Scheduling** is upstream to **Operations**
- **Operations** is upstream to **Inventory**, **Billing**, and **Compliance**
- Events flow downstream
- Teams can be identified by context ownership

---

## Context Relationship Patterns

DDD identifies several patterns for how Bounded Contexts relate:

### 1. Partnership

**Definition:** Two contexts have a **mutual dependency**—success requires coordinated development.

**Characteristics:**
- ✅ Teams coordinate closely
- ✅ Joint planning and integration
- ✅ Shared success criteria
- ❌ Can't evolve independently

**When to use:**
- Both contexts change together frequently
- Strong business coupling
- Same organizational unit

**Example:**

```
┌─────────────────┐      Partnership      ┌─────────────────┐
│   Scheduling    │ ←─────────────────→  │    Capacity     │
│    Context      │   (joint planning)    │    Planning     │
└─────────────────┘                       └─────────────────┘
```

**Petroleum Terminal:** Scheduling and Capacity Planning must coordinate—can't schedule loads that exceed capacity. Teams work together on planning algorithms.

**Integration:**
- Shared planning sessions
- Coordinated releases
- Direct API calls or shared database (acceptable because tightly coupled anyway)

### 2. Customer-Supplier

**Definition:** **Upstream** context (supplier) provides services to **downstream** context (customer). Customer's needs influence supplier's priorities, but supplier decides the interface.

**Characteristics:**
- ✅ Clear upstream/downstream relationship
- ✅ Customer can request features
- ✅ Supplier maintains control of interface
- ✅ Some independence

**When to use:**
- Clear dependency direction
- Different teams with coordinated priorities
- Downstream needs influence upstream roadmap

**Example:**

```
┌─────────────────┐
│   Operations    │ (Supplier / Upstream)
│    Context      │
└────────┬────────┘
         │ LoadingCompleted events
         ↓
┌─────────────────┐
│    Billing      │ (Customer / Downstream)
│    Context      │
└─────────────────┘
```

**Petroleum Terminal:** Operations (upstream) provides loading completion data to Billing (downstream). Billing team can request additional data fields ("Can you include carrier ID in the event?"), and Operations team considers it.

**Integration:**
- Published events or APIs
- Versioning and backward compatibility
- SLAs and support agreements

### 3. Conformist

**Definition:** Downstream context **conforms entirely** to upstream's model. No translation, no negotiation.

**Characteristics:**
- ✅ Simple integration (no translation layer)
- ✅ Fast to implement
- ❌ Downstream team has no control
- ❌ Upstream changes break downstream

**When to use:**
- Upstream has strong authority (regulatory, standard, legacy)
- Downstream team is small and can adapt
- Cost of translation layer > cost of conforming

**Example:**

```
┌──────────────────────┐
│  EPA Reporting API   │ (Upstream / External)
│   (Government)       │
└──────────┬───────────┘
           │ Conform to EPA format
           ↓
┌─────────────────┐
│   Compliance    │ (Downstream / Conformist)
│    Context      │
└─────────────────┘
```

**Petroleum Terminal:** Compliance context must submit reports in EPA's exact format. No negotiation, no translation—just conform.

**Integration:**
- Use upstream's data model directly
- No Anti-Corruption Layer
- Accept upstream's evolution

### 4. Anti-Corruption Layer (ACL)

**Definition:** Downstream context builds a **translation layer** to protect itself from upstream's model.

**Characteristics:**
- ✅ Downstream maintains clean model
- ✅ Isolation from upstream changes
- ✅ Can integrate with messy/legacy systems
- ❌ More complexity (translation code)

**When to use:**
- Upstream model is messy or doesn't match domain
- Upstream is legacy system or third-party
- Downstream needs stability despite upstream volatility

**Example:**

```
┌──────────────────────┐
│   Legacy SCADA       │ (Upstream / Messy)
│   (20-year-old)      │
└──────────┬───────────┘
           │ PLC signals, cryptic codes
           ↓
     ┌─────────────┐
     │     ACL     │ (Translation Layer)
     └─────┬───────┘
           │ Clean domain events
           ↓
┌─────────────────┐
│   Operations    │ (Downstream / Protected)
│    Context      │
└─────────────────┘
```

**Petroleum Terminal:** Legacy SCADA system sends cryptic PLC signals. ACL translates them into clean domain events like `TankLevelChanged`, `ValveOpened`, `PumpStarted`.

**Integration:**
- Adapter/Facade pattern
- Translates upstream model to downstream model
- Shields downstream from upstream changes

**Code Example:**

```java
// ACL translates SCADA signals into domain events
@Component
public class ScadaToOperationsACL {

    @Scheduled(fixedRate = 5000)  // Poll SCADA every 5 seconds
    public void pollScadaAndTranslate() {
        List<ScadaSignal> signals = scadaClient.readSignals();

        for (ScadaSignal signal : signals) {
            if (signal.getCode().equals("TL_CHG")) {
                // Translate to domain event
                TankLevelChanged event = new TankLevelChanged(
                    parseTankId(signal.getData()),
                    parseVolume(signal.getData()),
                    Instant.now()
                );
                eventPublisher.publish(event);
            }
        }
    }

    private TankId parseTankId(String data) {
        // Extract tank ID from cryptic format
    }

    private Volume parseVolume(String data) {
        // Extract volume from cryptic format
    }
}
```

### 5. Shared Kernel

**Definition:** Two contexts share a **small, common model** that both teams agree to maintain together.

**Characteristics:**
- ✅ Reduces duplication
- ✅ Ensures consistency in shared concepts
- ❌ Requires coordination for changes
- ❌ Creates coupling

**When to use:**
- Two contexts genuinely share a core concept
- Teams are willing to coordinate
- Shared model is small and stable

**Example:**

```
┌─────────────────┐       Shared Kernel      ┌─────────────────┐
│   Operations    │ ←────────────────────→  │    Inventory    │
│    Context      │  (Product, Volume)       │    Context      │
└─────────────────┘                          └─────────────────┘
```

**Petroleum Terminal:** Operations and Inventory both use `Product` and `Volume`. Instead of each maintaining their own version, they share a kernel module.

**Integration:**
- Shared library or module
- Coordinated changes (both teams approve)
- Keep shared kernel minimal

**Code Structure:**

```
shared-kernel/
  Product.java
  Volume.java
  ProductCode.java

operations-context/
  (imports shared-kernel)
  LoadingOperation.java

inventory-context/
  (imports shared-kernel)
  Tank.java
```

**Warning:** Don't let Shared Kernel grow too large. Only share what's truly common and stable.

### 6. Separate Ways

**Definition:** Two contexts have **no connection**—they solve their problems independently.

**Characteristics:**
- ✅ Maximum independence
- ✅ No integration complexity
- ✅ Fast development
- ❌ Potential duplication
- ❌ No data sharing

**When to use:**
- Contexts genuinely don't need each other's data
- Integration cost > duplication cost
- Different domains entirely

**Example:**

```
┌─────────────────┐                    ┌─────────────────┐
│   Operations    │                    │   HR/Payroll    │
│    Context      │    No relationship │    Context      │
└─────────────────┘                    └─────────────────┘
```

**Petroleum Terminal:** Operations (loading terminals) and HR (employee payroll) are separate. Operations doesn't need payroll data. HR doesn't need loading data. They go their separate ways.

**Integration:**
- None

### 7. Open Host Service / Published Language

**Definition:** Upstream context provides a **public API** using a **well-documented, stable model** that many downstream consumers can use.

**Characteristics:**
- ✅ One-to-many integration
- ✅ Stable, versioned interface
- ✅ Upstream defines clear contract
- ✅ Downstream teams can self-serve

**When to use:**
- One upstream serves many downstreams
- Upstream team can invest in good API design
- Need for stability and documentation

**Example:**

```
                    ┌─────────────────┐
                    │   Operations    │ (Open Host Service)
                    │    Context      │
                    └────────┬────────┘
                             │ Published API with
                             │ stable event schema
         ┌───────────────────┼───────────────────┐
         ↓                   ↓                   ↓
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Inventory  │     │   Billing   │     │ Compliance  │
└─────────────┘     └─────────────┘     └─────────────┘
```

**Petroleum Terminal:** Operations publishes `LoadingCompleted` events using a stable, documented schema. Any downstream context (Inventory, Billing, Compliance, Analytics, Reporting) can subscribe without negotiating with Operations team.

**Integration:**
- RESTful API with OpenAPI spec
- Event schema with versioning (JSON Schema, Avro, Protobuf)
- Changelog and migration guides

---

## Conway's Law and Team Topology

**Conway's Law:** "Organizations design systems that mirror their communication structure."

**Translation:** Your architecture will reflect your org chart.

**Implications for DDD:**

### Align Bounded Contexts with Teams

**Good:**
- One Bounded Context = One team
- Team owns the entire context (code, data, deployments)
- Clear responsibility

**Bad:**
- Multiple teams share a context (who makes decisions?)
- One team spans multiple contexts (divided attention, conflicting priorities)

**Example:**

```
Team 1: Operations Context (Terminal Operations Team)
Team 2: Scheduling Context (Logistics Coordination Team)
Team 3: Inventory Context (Inventory Analysis Team)
Team 4: Billing Context (Finance Team)
Team 5: Compliance Context (Safety & Regulatory Team)
```

Each team can evolve their context independently, subject to agreed integration contracts.

### Minimize Team Dependencies

**Context Map reveals dependencies.** If Team A depends heavily on Team B, and Team B's priorities don't align, Team A is blocked.

**Strategies:**
- Use **asynchronous integration** (events) to reduce blocking
- Design **stable interfaces** so upstream changes don't break downstream
- Use **Anti-Corruption Layers** to insulate from volatile upstreams

### Team Interaction Modes (from Team Topologies)

**Collaboration:** Teams work closely together (Partnership pattern)
- Use temporarily during high-uncertainty phases
- Transition to more independent modes once boundaries stabilize

**X-as-a-Service:** Upstream provides service, downstream consumes (Customer-Supplier, Open Host)
- Clear interface, SLAs, versioning
- Enables independence

**Facilitating:** One team helps another build capability
- Example: Platform team helps Operations team set up event publishing infrastructure

---

## Architecture Patterns for DDD

How do you implement Bounded Contexts in actual architecture?

### Option 1: Modular Monolith

**Structure:** Single deployable application with internal modules for each Bounded Context.

```
monolith-app/
  src/
    operations/        ← Operations Context
      domain/
      api/
      events/
    inventory/         ← Inventory Context
      domain/
      api/
      events/
    scheduling/        ← Scheduling Context
      domain/
      api/
      events/
  shared-kernel/
```

**Pros:**
- ✅ Simple deployment (one artifact)
- ✅ Easy local development
- ✅ Strong compile-time boundaries (packages)
- ✅ Good for small-to-medium teams

**Cons:**
- ❌ All contexts scale together
- ❌ Deployment coupling (change in one context redeploys all)
- ❌ Technology coupling (same language/framework for all)

**When to use:**
- Team size < 50 people
- Moderate complexity
- Contexts don't have radically different scaling needs

### Option 2: Microservices

**Structure:** Each Bounded Context is a separate deployable service.

```
operations-service/
  (Operations Context)

inventory-service/
  (Inventory Context)

scheduling-service/
  (Scheduling Context)

billing-service/
  (Billing Context)
```

**Pros:**
- ✅ Independent deployment
- ✅ Independent scaling
- ✅ Technology diversity (each service can use different stack)
- ✅ Clear ownership boundaries

**Cons:**
- ❌ Operational complexity (many services to deploy/monitor)
- ❌ Distributed system challenges (network, consistency, debugging)
- ❌ Higher infrastructure cost

**When to use:**
- Large teams (50+ people)
- Contexts with different scaling needs
- Need for technology diversity
- Mature DevOps capability

### Option 3: Event-Driven Architecture

**Structure:** Contexts communicate exclusively via events on a shared event bus (Kafka, RabbitMQ, EventStoreDB).

```
┌─────────────┐       ┌─────────────────┐       ┌─────────────┐
│ Operations  │──────→│   Event Bus     │──────→│  Inventory  │
└─────────────┘  pub  │ (Kafka/RabbitMQ)│  sub  └─────────────┘
                      └─────────────────┘
                             ↓ sub
                      ┌─────────────┐
                      │   Billing   │
                      └─────────────┘
```

**Pros:**
- ✅ Maximum decoupling
- ✅ Asynchronous (fast, non-blocking)
- ✅ Scalable (add subscribers without changing publishers)
- ✅ Audit trail (event log)

**Cons:**
- ❌ Eventual consistency
- ❌ Harder to debug (distributed flows)
- ❌ Requires event infrastructure

**When to use:**
- Lots of integration points
- Need for asynchronous processing
- Audit/compliance requirements (event sourcing)

### Hybrid Approach

**Most real systems combine approaches:**

- **Modular monolith** for tightly coupled contexts (Scheduling + Capacity Planning)
- **Microservices** for independently scaling contexts (Billing, Analytics)
- **Event bus** for cross-context integration

---

## Hands-On Exercise: Context Mapping

**Use the domain from Weeks 14-17** (petroleum terminal, hospital, e-commerce, property management).

### Part 1: Identify All Bounded Contexts (30 minutes)

From your Event Storming (Week 17), list all Bounded Contexts.

**For each context, document:**
- Name
- Purpose (one sentence)
- Key aggregates
- Owning team (hypothetical if necessary)

### Part 2: Draw Context Map (45 minutes)

Create a visual map showing:
- All contexts as boxes
- Arrows showing dependencies (upstream → downstream)
- Labels on arrows indicating relationship pattern (Partnership, Customer-Supplier, ACL, etc.)

**Deliverable:** Context map diagram (hand-drawn or digital)

### Part 3: Design Integration for One Relationship (60 minutes)

Pick one relationship from your map and design the integration in detail:

**If event-based:**
- Event schema (JSON or Java class)
- Publisher code (aggregate publishes event)
- Subscriber code (event handler reacts)

**If API-based:**
- API endpoint signature
- Request/response formats
- Error handling

**If ACL:**
- Translation logic (how to convert upstream model to downstream model)

**Deliverable:** Code examples showing integration

### Part 4: Identify Risks and Hotspots (30 minutes)

For your Context Map:
- **Which relationships are risky?** (High coupling, frequent changes, unclear ownership)
- **Which contexts have too many dependencies?** (Bottlenecks)
- **Where might Conway's Law create problems?** (Team structure doesn't match context boundaries)

**Deliverable:** Risk assessment document

---

## PROJECT 4: Complete Domain-Driven Design (DDD) Design

**This is the capstone project for Course 4.** Integrate everything you've learned from Weeks 14-18.

### Project Scope

**Choose ONE of the following domains:**

1. **Petroleum Terminal Operations** (scheduling, loading, inventory, compliance)
2. **Hospital Patient Flow** (admissions, appointments, treatments, billing)
3. **E-Commerce Platform** (catalog, shopping, fulfillment, payments)
4. **Property Management** (leasing, maintenance, rent collection, inspections)
5. **A domain from your professional experience** (get instructor approval)

### Deliverables

**Produce a comprehensive DDD design document (25-35 pages) including:**

#### 1. Executive Summary (1-2 pages)
- Domain overview
- Business context and goals
- Why DDD is appropriate for this domain
- Key challenges addressed

#### 2. Ubiquitous Language Glossary (3-4 pages)
- 30-40 domain terms with precise definitions
- Organized by context
- Ambiguity resolutions documented
- Examples of usage

#### 3. Event Storming Outputs (4-5 pages)
- 40-60 domain events
- Timeline visualization
- Commands, actors, policies identified
- Hotspots documented
- Photographs/diagrams of Event Storming session (if conducted)

#### 4. Bounded Contexts (5-6 pages)
- Identify 4-6 Bounded Contexts
- For each context:
  - Name and purpose
  - Key aggregates (3-5 per context)
  - Key domain events
  - Ubiquitous language specific to this context
  - Owning team/stakeholder group
  - Responsibilities and boundaries

#### 5. Context Map (2-3 pages)
- Visual diagram showing all contexts and relationships
- For each relationship:
  - Pattern (Partnership, Customer-Supplier, ACL, etc.)
  - Integration mechanism (events, API, shared kernel)
  - Rationale for chosen pattern
- Team topology and ownership

#### 6. Tactical Design: Domain Models (6-8 pages)
- For 2-3 key Bounded Contexts, provide detailed domain models:
  - **Entities:** Identity, lifecycle, key behaviors
  - **Value Objects:** Attributes, validation, domain operations
  - **Aggregates:** Boundaries, invariants, root entity
  - **Domain Events:** Schema, triggers, subscribers
  - **Commands:** Parameters, validation, target aggregates
- Include Java class signatures or pseudocode

#### 7. Integration Design (3-4 pages)
- Detail 3-5 integration points between contexts
- For each:
  - Event schemas or API contracts
  - Publisher and subscriber implementations
  - Error handling and retry logic
  - Versioning strategy

#### 8. Architecture Overview (2-3 pages)
- Deployment model (modular monolith, microservices, hybrid)
- Technology stack (Java/Spring Boot, event bus, database per context)
- Rationale for architectural choices
- Scalability and resilience considerations

#### 9. Risks and Mitigations (1-2 pages)
- Identified risks from Context Map
- Hotspots from Event Storming
- Mitigation strategies
- Open questions requiring further investigation

#### 10. Appendix: Code Samples (Optional, 3-5 pages)
- Complete implementations of key aggregates
- Event publishing and handling
- Command handlers
- ACL translation logic

### Evaluation Criteria

Your PROJECT 4 will be evaluated on:

**Completeness (30%):**
- All sections present and thorough
- Sufficient detail in domain models
- Context map covers full domain

**Correctness (25%):**
- Appropriate use of DDD patterns
- Correct Entity vs Value Object distinctions
- Aggregates enforce stated invariants
- Context relationships well-chosen

**Clarity (20%):**
- Ubiquitous language consistently used
- Clear explanations and rationale
- Diagrams are readable and accurate

**Depth (15%):**
- Non-trivial domain with real complexity
- Thoughtful analysis of trade-offs
- Risks identified and addressed

**Integration Design (10%):**
- Well-designed integration points
- Practical event schemas or API contracts
- Consideration of failure modes

### Portfolio Value

This project demonstrates to employers:
- Deep understanding of DDD strategic and tactical patterns
- Ability to model complex business domains
- Collaborative discovery techniques (Event Storming)
- Architectural thinking at enterprise scale
- Communication of technical design to diverse audiences
- Real-world application of advanced software design principles

**Display this prominently in your portfolio.** It shows you can tackle complex domain modeling—a skill that sets senior engineers apart.

---

## Reflection Questions

1. **Think about a large system you've worked with.** Can you identify Bounded Contexts retroactively? How were they integrated?

2. **Context Maps reveal dependencies.** If Context A depends on Context B, and they're owned by different teams, what governance/communication is needed?

3. **When would you choose Conformist over Anti-Corruption Layer?** What are the trade-offs?

4. **Conway's Law:** Have you seen systems where the architecture clearly mirrors the org chart? Was it good or bad?

5. **From your operational experience:** If petroleum terminals were redesigned from scratch using DDD, how would Bounded Contexts align with organizational departments?

---

## Key Takeaways

✅ **Strategic Design focuses on relationships between Bounded Contexts** — Not just what's inside each context, but how they integrate.

✅ **Context Maps visualize dependencies** — Show upstream/downstream relationships, integration patterns, team ownership.

✅ **Seven relationship patterns:** Partnership, Customer-Supplier, Conformist, Anti-Corruption Layer, Shared Kernel, Separate Ways, Open Host Service.

✅ **Conway's Law:** Align contexts with teams for independence and clear ownership.

✅ **Architecture options:** Modular monolith (simple, monolithic deployment), Microservices (independent scaling), Event-driven (maximum decoupling).

✅ **Integration patterns matter** — Events for async decoupling, APIs for sync requests, ACLs to protect clean models.

✅ **PROJECT 4 integrates all DDD concepts** — Ubiquitous Language, Bounded Contexts, Entities/Value Objects/Aggregates, Events/Commands, Event Storming, Context Mapping, Strategic Design.

---

## Connection to Course 5

**Course 4 Complete!** You've mastered Domain-Driven Design:
- **Week 14:** Ubiquitous Language & Bounded Contexts
- **Week 15:** Entities, Value Objects & Aggregates
- **Week 16:** Domain Events & Commands
- **Week 17:** Event Storming & Domain Modeling
- **Week 18:** Strategic Design & Context Mapping + PROJECT 4

**Next: Course 5 — Business Process Modeling & Workflows (Weeks 19-22)**

You've learned to model **domains** (what the business does). Now you'll learn to model **processes** (how the business does it).

**Week 19** introduces **BPMN (Business Process Model and Notation)**—a standard visual language for mapping end-to-end business processes, human tasks, automated steps, decisions, and handoffs.

You'll learn:
- BPMN elements: tasks, gateways, events, flows
- Modeling workflows from start to finish
- Integrating BPMN with domain events
- Process automation and orchestration
- Human ↔ Software collaboration patterns

**Get ready to map and optimize business processes.**

---

## Additional Resources

**Books:**
- *Domain-Driven Design* by Eric Evans — Part IV (Strategic Design)
- *Implementing Domain-Driven Design* by Vaughn Vernon — Chapters on Context Mapping and Integration
- *Team Topologies* by Matthew Skelton & Manuel Pais — Team interaction patterns

**Articles:**
- Martin Fowler: "BoundedContext" (martinfowler.com)
- Eric Evans: "Strategic Design with Context Mapping"

**Architectural Patterns:**
- Sam Newman: *Building Microservices* — Decomposition and integration
- Chris Richardson: Microservices Patterns (microservices.io)

**For Your Context:**
- Study integration patterns in petroleum terminal systems (how do scheduling, operations, and inventory systems integrate?)
- Examine legacy system integration—often requires ACLs
- Consider how your operational experience reveals natural context boundaries

---

**End of Week 18 — End of Course 4: Domain-Driven Design**

**Next:** Week 19 — Introduction to BPMN (Course 5 begins)
