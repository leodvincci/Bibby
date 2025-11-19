# Week 17: Event Storming & Domain Modeling

**Course 4: Domain-Driven Design (DDD) — Week 17 of 26**

---

## The Meeting That Took 6 Months

In 2016, a healthcare company tried to build a new patient scheduling system. They followed "best practices":

- Business analysts interviewed stakeholders
- Requirements were documented in 200-page Word documents
- Architects designed the system in isolation
- Developers built what the documents specified

**Six months later**, when they demoed the system to doctors and nurses:

"This isn't how appointments work."

"You can't schedule an MRI without checking equipment availability."

"What happens when a patient is late? This doesn't handle that."

"Why does it ask for insurance before we even confirm the appointment time?"

**The problem:** Everyone worked in silos. Business analysts **translated** what doctors said into requirements. Architects **translated** requirements into designs. Developers **translated** designs into code. Each translation introduced drift.

By the time the system was built, it bore little resemblance to how the domain actually worked.

**The project was scrapped.** $3.5M lost.

**What if**, instead of months of translation, everyone—doctors, nurses, schedulers, IT staff, developers—spent **one day in a room with sticky notes**, mapping out **exactly** how appointments work?

That's **Event Storming**.

Welcome to **Week 17: Event Storming & Domain Modeling**.

---

## What Is Event Storming?

**Event Storming** is a collaborative workshop technique invented by Alberto Brandolini for rapidly discovering and modeling complex business domains.

### Core Principles

1. **Bring everyone together** — Domain experts, developers, product owners, designers, operations, support—anyone who touches the domain
2. **Use a simple visual language** — Colored sticky notes represent different domain concepts
3. **Focus on events** — What happens in the domain? (Not what the system does, but what occurs)
4. **Start chaotic, then organize** — Brain dump first, structure later
5. **Move fast** — Discover the entire domain in 4-8 hours, not 6 months

### Why Event Storming Works

**Traditional requirements gathering:**
- Business analyst interviews stakeholders one-on-one
- Each stakeholder sees only their piece
- Contradictions emerge months later
- Translation from business → analyst → architect → developer

**Event Storming:**
- Everyone in the same room at the same time
- Everyone sees the full picture
- Contradictions surface immediately ("Wait, I thought accounting handled that!")
- No translation—domain experts and developers use the same language

### What You'll Discover

Through Event Storming, you'll identify:

- **Domain Events** — What happens in the domain (Week 16)
- **Commands** — What triggers those events (Week 16)
- **Aggregates** — What enforces business rules (Week 15)
- **Actors** — Who or what initiates commands
- **Policies** — Reactive logic ("Whenever X happens, do Y")
- **Read Models** — Information needed to make decisions
- **External Systems** — Third-party integrations
- **Hotspots** — Problems, bottlenecks, conflicts, unknowns
- **Bounded Contexts** — Natural boundaries in the domain (Week 14)

All in **one day**.

---

## Materials Needed

Event Storming is deliberately low-tech:

**Required:**
- **Large wall space** or **paper roll** (8-15 feet long)
- **Sticky notes in different colors:**
  - Orange — Domain Events
  - Blue — Commands
  - Yellow — Aggregates/Entities
  - Small yellow — Actors/Users
  - Lilac/Purple — Policies (reactive logic)
  - Green — Read Models
  - Pink — External Systems
  - Red — Hotspots (problems, questions)
- **Markers** (Sharpies or similar)
- **Tape** (to attach paper roll to wall)

**Why physical, not digital?**
- **Tactile engagement** — People think better when moving sticky notes
- **Spatial arrangement** — Physical space reveals temporal flows and relationships
- **No technology barriers** — Everyone can participate
- **Energy** — Standing, moving, collaborating is more engaging than sitting at computers

*Note: Remote Event Storming is possible with tools like Miro or Mural, but loses some of the energy and spontaneity.*

---

## The Event Storming Process

### Phase 1: Chaotic Exploration (60-90 minutes)

**Goal:** Brain dump all domain events onto the wall. No structure yet, just discovery.

**Instructions to participants:**

> "Think about everything that happens in this domain. Write each significant event on an orange sticky note. Use past tense: 'Order Placed,' 'Invoice Sent,' 'Tank Filled.' Don't worry about order or organization—just get all the events out."

**Facilitator role:**
- Encourage breadth (cover the whole domain)
- Ask triggering questions: "What happens when...?" "What comes before...?" "What comes after...?"
- Don't organize yet—let chaos happen

**Example: Petroleum Terminal Domain**

Participants write orange sticky notes:
- "Load Order Scheduled"
- "Driver Checked In"
- "Bay Assigned"
- "Loading Started"
- "Meter Reading Recorded"
- "Loading Completed"
- "Ticket Printed"
- "Driver Departed"
- "Product Received"
- "Tank Level Changed"
- "Inventory Reconciled"
- "Safety Incident Reported"
- "Emergency Shutdown Triggered"
- ...and 50+ more

**Key insight:** Different people contribute different events. Operations staff know events that logistics never sees. Safety knows events nobody else thinks about.

### Phase 2: Enforce Timeline (30-60 minutes)

**Goal:** Arrange events in rough chronological order from left to right.

**Instructions:**

> "Now let's arrange these events in time order. What happens first? What happens next? Move your sticky notes to show the flow."

**What emerges:**
- **Happy path** — The ideal flow when everything goes right
- **Variations** — Alternative paths ("What if the driver is late?")
- **Parallels** — Events that happen simultaneously in different parts of the domain

**Petroleum Terminal Example (Simplified Timeline):**

```
[Product Received] → [Tank Level Changed] → [Load Order Scheduled] →
[Driver Checked In] → [Bay Assigned] → [Loading Started] →
[Meter Reading Recorded] → [Loading Completed] → [Ticket Printed] →
[Driver Departed] → [Inventory Reconciled]
```

But also parallel flows:
- Safety inspections happen independently
- Maintenance schedules overlap with operations
- Billing processes run in parallel

### Phase 3: Add Commands (30 minutes)

**Goal:** Identify **what triggers** each event. Add blue sticky notes for commands.

**Instructions:**

> "For each event, what causes it to happen? Write a command—an action someone takes—on a blue sticky note and place it just before the event."

**Example:**

```
[Schedule Load Order] → (Load Order Scheduled)
[Check In Driver] → (Driver Checked In)
[Assign Bay] → (Bay Assigned)
[Begin Loading] → (Loading Started)
[Record Meter Reading] → (Meter Reading Recorded)
[Complete Loading] → (Loading Completed)
```

**Key questions:**
- Who executes this command?
- What information do they need to execute it?
- Can this command fail? Under what conditions?

### Phase 4: Add Actors (20 minutes)

**Goal:** Identify **who or what** executes each command.

**Instructions:**

> "For each command, who performs it? Write the actor on a small yellow sticky note."

**Example:**

```
[Logistics Coordinator] → [Schedule Load Order] → (Load Order Scheduled)
[Gate Attendant] → [Check In Driver] → (Driver Checked In)
[Terminal Operator] → [Assign Bay] → (Bay Assigned)
[Terminal Operator] → [Begin Loading] → (Loading Started)
[Automated System] → [Record Meter Reading] → (Meter Reading Recorded)
[Terminal Operator] → [Complete Loading] → (Loading Completed)
```

**Insights:**
- **Human actors** — Roles, not specific people ("Terminal Operator," not "Bob")
- **Automated systems** — Timers, sensors, batch processes
- **External systems** — Third-party APIs, partner systems

### Phase 5: Add Policies (30 minutes)

**Goal:** Identify **reactive logic**—rules that trigger automatically when events occur.

**Instructions:**

> "Look at each event. Does anything happen **automatically** as a result? Write a policy on a lilac sticky note: 'Whenever [Event], then [Action]'."

**Example:**

```
(Loading Completed) → [Policy: Whenever Loading Completes, Update Tank Level] → [Update Tank Level]

(Tank Level Changed) → [Policy: If Level Below Threshold, Trigger Reorder] → [Create Reorder Request]

(Driver Departed) → [Policy: Whenever Driver Departs, Archive Ticket] → [Archive Ticket in Compliance System]
```

**Policies represent:**
- Business rules that execute automatically
- Integration points (event in one context triggers action in another)
- Eventual consistency (Week 16)

### Phase 6: Add Aggregates (30 minutes)

**Goal:** Identify **what enforces business rules** and **what changes state** when commands are executed.

**Instructions:**

> "For each command, what domain object validates the command and updates state? Write it on a yellow sticky note and place it near the command."

**Example:**

```
[Terminal Operator] → [Assign Bay] → {LoadOrder} → (Bay Assigned)
[Terminal Operator] → [Begin Loading] → {LoadingOperation} → (Loading Started)
[Terminal Operator] → [Complete Loading] → {LoadingOperation} → (Loading Completed)
[Automated System] → [Update Tank Level] → {Tank} → (Tank Level Changed)
```

**Aggregates from Week 15:**
- LoadOrder, LoadingOperation, Tank, Driver, Product, etc.
- Enforce invariants (e.g., "Can't complete loading if not started")
- Publish events when state changes

### Phase 7: Add Read Models (20 minutes)

**Goal:** Identify **information needed** to make decisions.

**Instructions:**

> "When someone executes a command, what information do they need? Write it on a green sticky note."

**Example:**

```
[Available Bays by Time Slot] → [Logistics Coordinator] → [Schedule Load Order]
[Current Tank Levels] → [Logistics Coordinator] → [Schedule Load Order]
[Driver Check-In Status] → [Terminal Operator] → [Assign Bay]
[Loading Progress Dashboard] → [Terminal Operator] → [Monitor Loading]
```

**Read models answer queries:**
- "Which bays are available tomorrow between 2-4pm?"
- "What's the current level of Tank #5?"
- "How many loads are scheduled this week?"

### Phase 8: Identify Hotspots (Throughout)

**Goal:** Mark **problems, conflicts, questions, bottlenecks**.

**Instructions:**

> "Whenever you discover a problem, a conflict between stakeholders, a question, or a bottleneck, put a red sticky note there."

**Examples of Hotspots:**

**Conflict:**
```
Red note: "Sales says 'confirmed' means customer agreed.
Operations says 'confirmed' means truck is assigned.
Which is it??"
```
→ Indicates need for different terms or different contexts (Week 14)

**Unknown:**
```
Red note: "What happens if the meter fails during loading?
Nobody knows the current process."
```
→ Reveals gap in knowledge—need to investigate

**Bottleneck:**
```
Red note: "Only 2 bays available but 8 loads scheduled.
Can't physically handle the volume."
```
→ Capacity constraint (Week 2)

**Technical Risk:**
```
Red note: "SCADA system is 20 years old.
Integration may not be possible."
```
→ Technical constraint

**Key insight:** Hotspots surface problems **early**, when they're cheap to fix, instead of during development when they're expensive.

### Phase 9: Identify Bounded Contexts (30-45 minutes)

**Goal:** Find **natural boundaries** where the language, concerns, or team ownership changes.

**Instructions:**

> "Look at the timeline. Where do you see clusters of events that belong together? Where does the vocabulary change? Where would you draw boundaries?"

**Petroleum Terminal Example:**

**Scheduling Context:**
- Events: Load Order Scheduled, Load Order Cancelled, Load Order Rescheduled
- Actors: Logistics Coordinator, Dispatch Manager
- Aggregates: LoadOrder
- Concern: Planning and capacity management

**Operations Context:**
- Events: Loading Started, Meter Reading Recorded, Loading Completed, Ticket Printed
- Actors: Terminal Operator, Driver
- Aggregates: LoadingOperation, Bay, Ticket
- Concern: Physical execution and safety

**Inventory Context:**
- Events: Product Received, Tank Level Changed, Inventory Reconciled
- Actors: Inventory Analyst, Automated System
- Aggregates: Tank, Product, InventoryPosition
- Concern: Stock levels and reconciliation

**Compliance Context:**
- Events: Ticket Archived, Incident Reported, Emission Recorded
- Actors: Compliance Officer, Automated Regulatory System
- Aggregates: ComplianceRecord, IncidentReport
- Concern: Regulatory requirements

**Mark boundaries** with vertical lines or different sections of the wall.

---

## Facilitating an Event Storming Workshop

### Before the Workshop

**1. Define the Scope**
- What domain are we exploring? (e.g., "Petroleum terminal load scheduling and execution")
- What's out of scope? (e.g., "Refinery operations, marine shipping")

**2. Invite the Right People**
- Domain experts (people who DO the work daily)
- Technical people (developers, architects)
- Product owners
- Ops/Support
- **Critical:** Include people with different perspectives (front office, back office, IT, field operations)

**3. Prepare the Space**
- Book a large room with wall space
- Get sticky notes and markers
- Print a simple legend of sticky note colors
- Bring snacks and coffee (workshops are intense!)

**4. Set Expectations**
- Duration: 4-8 hours (usually one full day)
- Goal: Understand the domain, not design software
- Everyone's input is valuable
- No laptops—full engagement

### During the Workshop

**Facilitator Responsibilities:**

**1. Set the Stage (15 minutes)**
- Explain Event Storming: "We're going to map out how this domain works using sticky notes."
- Show the legend (orange = events, blue = commands, etc.)
- Emphasize: "No idea is wrong. If you think it, write it."

**2. Keep Energy High**
- Stand, don't sit
- Take breaks every 90 minutes
- Encourage movement ("Go look at that section over there")
- Celebrate discoveries ("Great question!")

**3. Ask Triggering Questions**
- "What happens **before** that event?"
- "What happens **after**?"
- "What if the customer is late?"
- "What if the system is down?"
- "Who needs to know when this happens?"

**4. Surface Conflicts**
- When two people disagree, **make it visible** (hotspot!)
- Don't resolve immediately—acknowledge and continue
- "Interesting! We have two different understandings. Let's mark that."

**5. Manage Scope**
- If tangents emerge: "Great point. Let's park that for now and come back."
- Use a "parking lot" area for out-of-scope items

**6. Translate to Ubiquitous Language (Week 14)**
- When you hear generic terms ("process," "handle"), push for specifics
- "What do you call this in your daily work?"
- Capture the real language on the sticky notes

### After the Workshop

**1. Photograph Everything**
- Take high-resolution photos of the entire wall
- Close-ups of each section
- Document hotspots

**2. Digitize**
- Transcribe sticky notes into a structured document or digital board
- Preserve the spatial relationships (timeline, groupings)

**3. Follow Up on Hotspots**
- Schedule meetings to resolve conflicts
- Research unknowns
- Escalate bottlenecks

**4. Translate to Code**
- Events → Domain Events (Week 16)
- Commands → Command objects (Week 16)
- Aggregates → Aggregate classes (Week 15)
- Bounded Contexts → Microservices or modules (Week 14)

---

## From Sticky Notes to Code

**How to translate Event Storming outputs into executable domain models:**

### Step 1: Events → Domain Event Classes

**Sticky note:**
```
Orange: "Loading Completed"
```

**Java code:**
```java
public class LoadingCompleted {
    private final LoadingOperationId loadingOperationId;
    private final Volume quantityLoaded;
    private final Instant completedAt;
    // Constructor, getters...
}
```

### Step 2: Commands → Command Classes

**Sticky note:**
```
Blue: "Complete Loading"
```

**Java code:**
```java
public class CompleteLoading {
    private final LoadingOperationId loadingOperationId;
    private final Instant completionTime;
    // Constructor, getters, validation...
}
```

### Step 3: Aggregates → Aggregate Root Classes

**Sticky note:**
```
Yellow: "LoadingOperation"
```

**Java code:**
```java
public class LoadingOperation {
    private final LoadingOperationId id;
    private LoadingOperationStatus status;

    public void complete(Instant completionTime) {
        // Validate
        if (status != LoadingOperationStatus.IN_PROGRESS) {
            throw new IllegalStateException("...");
        }
        // Change state
        this.status = LoadingOperationStatus.COMPLETED;
        // Record event
        domainEvents.add(new LoadingCompleted(id, quantityLoaded, completionTime));
    }
}
```

### Step 4: Policies → Event Handlers

**Sticky note:**
```
Lilac: "Whenever Loading Completes, Update Tank Level"
```

**Java code:**
```java
@Component
public class TankInventoryPolicy {
    @EventListener
    public void handleLoadingCompleted(LoadingCompleted event) {
        tankService.recordWithdrawal(
            event.getTankId(),
            event.getQuantityLoaded()
        );
    }
}
```

### Step 5: Read Models → Query Endpoints

**Sticky note:**
```
Green: "Available Bays by Time Slot"
```

**Java code:**
```java
@RestController
public class BayAvailabilityController {
    @GetMapping("/bays/available")
    public List<Bay> getAvailableBays(
        @RequestParam LocalDateTime startTime,
        @RequestParam LocalDateTime endTime
    ) {
        return bayService.findAvailable(startTime, endTime);
    }
}
```

### Step 6: Bounded Contexts → Modules or Services

**Sticky notes grouped:**
```
Section 1: Scheduling Context (Load Order events)
Section 2: Operations Context (Loading events)
Section 3: Inventory Context (Tank events)
```

**Code structure:**
```
src/
  scheduling/        ← Scheduling Context
    domain/
      LoadOrder.java
      LoadOrderScheduled.java
  operations/        ← Operations Context
    domain/
      LoadingOperation.java
      LoadingCompleted.java
  inventory/         ← Inventory Context
    domain/
      Tank.java
      TankLevelChanged.java
```

Or separate microservices:
```
scheduling-service/
operations-service/
inventory-service/
```

---

## Benefits of Event Storming

**1. Shared Understanding**
- Everyone sees the full domain
- Developers understand business context
- Business experts see technical constraints

**2. Rapid Discovery**
- 1 day vs 6 months of requirements gathering
- Immediate feedback loop
- Early conflict resolution

**3. Ubiquitous Language Emerges Naturally**
- Real terms used by domain experts
- Captured directly on sticky notes
- No translation layer

**4. Identifies Bounded Contexts**
- Natural boundaries become visible
- Language shifts are obvious
- Ownership is clarified

**5. Surfaces Unknowns Early**
- Hotspots reveal gaps in knowledge
- Conflicts surface before development
- Risks identified upfront

**6. Engages Everyone**
- Domain experts feel heard
- Developers see the big picture
- Cross-functional collaboration

**7. Creates Shared Artifacts**
- Wall of sticky notes becomes reference
- Photos serve as documentation
- Living model that evolves

---

## Hands-On Exercise: Plan and Run an Event Storming Workshop

**Choose the same domain from Weeks 14-16** (petroleum terminal, hospital, e-commerce, property management).

### Part 1: Workshop Planning (60 minutes)

**Design your Event Storming workshop:**

1. **Define Scope**
   - What specific area of the domain will you explore?
   - What's explicitly out of scope?
   - What's the goal? (Understand scheduling? Operations? End-to-end?)

2. **Identify Participants**
   - List 6-10 people who should attend
   - What role/perspective does each bring?
   - Who are the critical domain experts?

3. **Prepare Materials**
   - List sticky note colors and their purposes
   - Create a simple legend/reference card
   - Identify room/space needs

4. **Create Facilitator Guide**
   - Phase-by-phase agenda with time allocations
   - Key questions to ask during each phase
   - How you'll handle conflicts or tangents

**Deliverable:** Workshop plan document (2-3 pages)

### Part 2: Solo Event Storming (90 minutes)

**Since you might not have 10 people available, do a "solo" Event Storming:**

1. **Brain dump events** (30 minutes)
   - Write 30-50 domain events on orange sticky notes (or digital equivalent)
   - Cover the full breadth of your domain
   - Don't organize yet

2. **Organize timeline** (20 minutes)
   - Arrange events left to right chronologically
   - Identify parallel flows
   - Mark happy path vs variations

3. **Add commands and actors** (20 minutes)
   - For each event, what command caused it?
   - Who/what executed that command?

4. **Add aggregates and policies** (20 minutes)
   - Which aggregate enforces rules for each command?
   - What reactive logic (policies) connects events?

**Deliverable:** Photograph or digital board showing complete Event Storming output

### Part 3: Identify Bounded Contexts and Hotspots (45 minutes)

1. **Mark 3-5 Bounded Contexts**
   - Draw boundaries where language/concerns change
   - Name each context
   - List key aggregates and events in each

2. **Identify 5-8 Hotspots**
   - Conflicts (same term, different meanings)
   - Unknowns (gaps in knowledge)
   - Bottlenecks (capacity or process constraints)
   - Technical risks

**Deliverable:** Annotated Event Storming board with boundaries and hotspots marked

### Part 4: Translate to Code Structure (60 minutes)

1. **Pick one Bounded Context**
2. **Translate Event Storming to code:**
   - List 3-5 events → Create domain event classes
   - List 3-5 commands → Create command classes
   - List 2-3 aggregates → Create aggregate root classes
   - List 2-3 policies → Create event handler classes

**Deliverable:** Code structure outline or partial implementation

---

## Reflection Questions

1. **Compared to traditional requirements gathering, what are the advantages of getting everyone in one room?** What are the disadvantages?

2. **Event Storming uses physical sticky notes instead of digital tools.** Why might that matter? When would digital be preferable?

3. **Think about a project where requirements were misunderstood.** How would Event Storming have helped? What conflicts would have surfaced?

4. **Hotspots reveal problems early.** What's the cost of discovering a conflict in a 1-day workshop vs discovering it in production 6 months later?

5. **From your operational experience:** If you ran an Event Storming workshop with terminal operators, logistics coordinators, and IT staff, what conflicts do you think would emerge?

6. **Event Storming focuses on events (what happened), not features (what the system should do).** Why is this perspective valuable?

---

## Key Takeaways

✅ **Event Storming discovers domains collaboratively** — Everyone in one room mapping out how the domain works using sticky notes.

✅ **Use a visual language** — Orange = events, Blue = commands, Yellow = aggregates, Lilac = policies, Green = read models, Red = hotspots.

✅ **Start chaotic, then organize** — Brain dump all events first, structure later (timeline, contexts, aggregates).

✅ **Surfaces conflicts early** — Hotspots reveal problems during the workshop, not in production.

✅ **Creates shared understanding** — Developers see business context, business experts see technical constraints.

✅ **Discovers Ubiquitous Language** — Real terms used by domain experts captured directly on sticky notes.

✅ **Identifies Bounded Contexts** — Natural boundaries become visible where language and concerns shift.

✅ **Translates to code** — Events → domain events, Commands → command classes, Aggregates → aggregate roots, Policies → event handlers.

✅ **Fast** — Discover entire domain in 1 day instead of 6 months of requirements docs.

---

## Connection to Week 18

This week you learned **Event Storming**—how to collaboratively discover domain models with domain experts using sticky notes and workshops.

Next week you'll learn **Strategic Design & Context Mapping**—how to design the architecture of complex systems with multiple Bounded Contexts, defining integration patterns, and managing complexity at scale.

You'll learn:
- Strategic vs tactical design in DDD
- Context mapping patterns (Partnership, Customer-Supplier, Conformist, Anti-Corruption Layer, Separate Ways)
- How to design integration between contexts
- Managing dependencies between teams and systems
- Architecture patterns for DDD (modular monolith, microservices, event-driven)
- PROJECT 4: Complete DDD design for a complex domain

**Get ready to design systems at scale.**

---

## Additional Resources

**Books:**
- *Introducing EventStorming* by Alberto Brandolini (the creator)
- *Domain-Driven Design Distilled* by Vaughn Vernon — Chapter on Event Storming

**Articles:**
- Alberto Brandolini: "Introducing Event Storming" (ziobrando.blogspot.com)
- Mariusz Gil: "EventStorming Glossary & Cheat Sheet"

**Videos:**
- Alberto Brandolini: Event Storming talks on YouTube
- Various conference talks showing Event Storming in action

**Tools (for Remote):**
- Miro (digital whiteboard)
- Mural (collaborative canvas)
- EventStorming.com (resources and templates)

**For Your Context:**
- Consider running a mini Event Storming with former petroleum terminal colleagues
- Map out loadout operations, scheduling, or inventory reconciliation
- Compare Event Storming discoveries to current system design

---

**End of Week 17 — Event Storming & Domain Modeling**

**Next:** Week 18 — Strategic Design & Context Mapping + PROJECT 4
