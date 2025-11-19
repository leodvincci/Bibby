# Week 21: Human ↔ Software Workflows

**Course 5: Business Process Modeling & Workflows — Week 21 of 26**

---

## The Automation That Made Everything Worse

In 2018, a healthcare insurance company automated their claims approval process. The goal: reduce approval time from 5 days to 24 hours.

**Before automation:**
- Claims processor reviews claim (2 hours)
- If complex, escalates to medical reviewer (4 hours)
- If very complex, escalates to physician reviewer (1 day)
- **Average:** 3 days for straightforward, 5 days for complex

**After automation:**
- System auto-approves simple claims (2 minutes)
- System auto-rejects "suspicious" claims (2 minutes)
- Everything else goes to queue for manual review

**Result:** Disaster.

- **Simple claims:** Blazing fast (24 hours → 2 minutes) ✅
- **Complex claims:** Much slower (5 days → 12 days) ❌
- **"Suspicious" claims:** Auto-rejected incorrectly (40% false positives) ❌

**What went wrong?**

The automation **eliminated human judgment** at the wrong points:

1. **Auto-rejection was too aggressive** — System flagged legitimate claims as suspicious
2. **No escalation path** — When system rejected a claim, humans couldn't override easily
3. **Queue overload** — All borderline cases dumped into manual queue with no prioritization
4. **Lost context** — When humans finally reviewed, they had to start from scratch (system didn't capture processor's notes)
5. **No learning loop** — System never learned from human overrides

**The automation created more work, not less.**

**The lesson:** Successful workflows **blend human judgment with automation**. Fully automated or fully manual are both extremes. The art is knowing **what to automate, what requires human judgment, and how they collaborate**.

Welcome to **Week 21: Human ↔ Software Workflows**.

---

## The Spectrum of Automation

Every task falls somewhere on this spectrum:

```
Fully Manual → Human-Led → Collaborative → System-Led → Fully Automated
```

**Fully Manual:**
- Human does everything
- No system support
- Example: Hand-written notes, verbal approvals

**Human-Led (System-Assisted):**
- Human makes decisions
- System provides information and tools
- Example: Doctor diagnoses with system showing test results, similar cases, drug interactions

**Collaborative:**
- Human and system share responsibility
- System handles routine parts, human handles exceptions
- Example: Expense approval (system auto-approves < $100, human approves > $100)

**System-Led (Human-Supervised):**
- System makes decisions
- Human can override or intervene
- Example: Fraud detection flags transactions, human reviews before blocking account

**Fully Automated:**
- System handles everything
- No human involvement
- Example: Password reset email, simple data validation

### Choosing the Right Level of Automation

**Automate when:**
- ✅ Rules are clear and consistent
- ✅ Volume is high
- ✅ Speed is critical
- ✅ Cost of errors is low
- ✅ Data is structured and available

**Keep human when:**
- ✅ Judgment and nuance are required
- ✅ Context and empathy matter
- ✅ Rules have exceptions
- ✅ Cost of errors is high
- ✅ Regulatory or ethical oversight is needed
- ✅ Creativity or problem-solving is required

**Example: Loan Approval**

| Scenario | Automation Level | Why |
|----------|------------------|-----|
| Credit score > 750, income > 3x payment, DTI < 30% | **Fully Automated** | Clear rules, low risk |
| Credit score 650-750, standard income/DTI | **System-Led** | System recommends, human reviews |
| Credit score < 650 OR unusual income source | **Human-Led** | Requires judgment, exceptions common |
| Large commercial loan (> $1M) | **Collaborative** | System calculates risk, human negotiates terms |

---

## Designing User Tasks in BPMN

**User Tasks** are activities performed by humans in a workflow.

### Anatomy of a User Task

A well-designed user task includes:

1. **Name** — Clear, action-oriented ("Approve invoice," not "Invoice approval")
2. **Assignee** — Who is responsible? (individual, role, group)
3. **Form** — What information does the user see and provide?
4. **Priority** — How urgent is this task?
5. **Due date** — When must it be completed?
6. **Instructions** — What should the user do?
7. **Context** — Why is this task happening? What came before?

**BPMN Notation:**

```
┌──────────────────────┐
│ 👤 Approve Invoice   │
│                      │
│ Assignee: AP Manager │
│ Due: 2 days          │
└──────────────────────┘
```

### User Task Forms

**Forms** are the interface through which users complete tasks.

**Example: Invoice Approval Form**

**Display (Read-Only):**
- Vendor name
- Invoice number
- Invoice date
- Amount
- Line items (table)
- Attached PDF

**Input (User Provides):**
- Approve or Reject (radio button)
- If reject: Reason (text area)
- Comments (optional, text area)

**Camunda Example:**

```xml
<userTask id="approveInvoice" name="Approve Invoice">
  <extensionElements>
    <camunda:formData>
      <camunda:formField id="vendorName" label="Vendor" type="string" readonly="true" />
      <camunda:formField id="amount" label="Amount" type="long" readonly="true" />
      <camunda:formField id="decision" label="Decision" type="enum">
        <camunda:value id="approve" name="Approve" />
        <camunda:value id="reject" name="Reject" />
      </camunda:formField>
      <camunda:formField id="comments" label="Comments" type="string" />
    </camunda:formData>
  </extensionElements>
</userTask>
```

**Best practices:**
- Show **all relevant context** (don't make users hunt for information)
- Make **common actions easy** (approve/reject buttons prominent)
- **Validate input** (required fields, format checks)
- **Provide guidance** (tooltips, help text, examples)
- **Support attachments** (upload documents, screenshots)

---

## Task Assignment and Routing

**Who gets which tasks?**

### Assignment Patterns

**1. Direct Assignment (Specific User)**
- Task assigned to a specific person
- **Example:** "Assign to John Smith"
- **When:** Known individual is responsible

```
┌──────────────────────────┐
│ 👤 Review Contract       │
│ Assignee: john.smith     │
└──────────────────────────┘
```

**2. Role-Based Assignment (Group/Pool)**
- Task assigned to anyone in a role/group
- First available person claims it
- **Example:** "Assign to Customer Service Representatives"
- **When:** Any qualified person can handle it

```
┌──────────────────────────┐
│ 👤 Respond to Ticket     │
│ Candidate Group: CS Reps │
└──────────────────────────┘
```

**3. Rule-Based Assignment**
- System determines assignee based on business rules
- **Example:** "Assign to manager of the employee who submitted the request"
- **When:** Assignment logic is complex

```javascript
// Expression to determine assignee
${employeeService.getManager(request.submitterId)}
```

**4. Load Balancing**
- Distribute tasks evenly across team
- **Example:** Round-robin, least busy, skill-based

```
┌──────────────────────────┐
│ 👤 Process Application   │
│ Assign: Least busy       │
│ in Underwriter group     │
└──────────────────────────┘
```

**5. Skills-Based Assignment**
- Match task requirements to user skills
- **Example:** Spanish-speaking customer service for Spanish inquiries

```
if (inquiry.language == 'Spanish') {
  assignTo(group: 'CS_Spanish_Speakers');
} else {
  assignTo(group: 'CS_General');
}
```

### Task Claiming and Ownership

**Claimed vs Unclaimed Tasks:**

**Unclaimed (in pool):**
```
Task: [Process claim #12345]
Status: Available
Candidate Group: Claims Processors
Action: Any processor can claim it
```

**Claimed (assigned to individual):**
```
Task: [Process claim #12345]
Status: In Progress
Assigned to: Jane Doe
Claimed at: 2024-01-15 10:30
```

**Benefits of claiming:**
- Prevents duplicate work
- Establishes accountability
- Enables tracking (who's working on what)

---

## Escalation and Delegation

### Escalation: When Tasks Are Overdue

**Escalation** automatically reassigns or notifies when a task isn't completed on time.

**Example: Invoice Approval**

```
[👤 Approve Invoice]
│
│ Due: 2 days
│
├──→ (◎ Timer: 2 days) → [Notify manager: "Invoice approval overdue"]
│
├──→ (◎ Timer: 4 days) → [Escalate to director]
│
└──→ (Completed) → [Next step]
```

**BPMN Timer Boundary Event:**

```
┌──────────────────────────┐
│ 👤 Approve Invoice       │ (◎ Timer: P2D)
│ Assignee: AP Manager     │         ↓
└──────────────────────────┘  [Escalate to Director]
```

**Escalation Strategies:**

1. **Notify assignee** — Reminder email/notification
2. **Notify manager** — Alert supervisor
3. **Reassign** — Move task to someone else
4. **Auto-complete** — System takes default action (if safe)
5. **Increase priority** — Move to top of queue

### Delegation: Transferring Responsibility

**Delegation** allows a user to assign their task to someone else.

**Scenarios:**
- User is on vacation
- Task requires specialized expertise
- Workload balancing

**Example:**

```
Original Assignee: Alice (on vacation)
Action: Alice delegates to Bob
New Assignee: Bob
Notification: Bob receives task with note "Delegated by Alice"
```

**Best practices:**
- **Audit trail** — Record who delegated to whom and when
- **Notify both parties** — Delegator and delegate
- **Preserve context** — Don't lose original assignee information
- **Set rules** — Can tasks be re-delegated? Who can delegate to whom?

---

## Work Queues and Task Management

**Work queues** are lists of tasks waiting for users.

### Designing Effective Queues

**1. Personal Queue (My Tasks)**
- Tasks assigned specifically to me
- Sorted by: Priority, Due date, Age

```
My Tasks (12)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔴 [HIGH] Approve invoice #5678 | Due: Today
🟡 [MED] Review contract | Due: Tomorrow
🟢 [LOW] Update customer info | Due: Next week
```

**2. Group Queue (Available Tasks)**
- Tasks available for anyone in my role to claim
- Sorted by: Priority, Age, Estimated effort

```
Available for Customer Service (8)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔴 [HIGH] Angry customer escalation | Age: 2 hours
🟡 [MED] Refund request | Age: 1 day
```

**3. Filtered Queues**
- Custom views based on criteria
- Examples:
  - "High priority tasks"
  - "Overdue tasks"
  - "Tasks I'm watching"
  - "Tasks from VIP customers"

### Queue Management Best Practices

**Prioritization:**
- Use **priority levels** (High, Medium, Low or 1-5)
- Consider **due date** (urgent vs important)
- Factor **age** (how long has it been waiting?)
- Apply **business rules** (VIP customers, large $ amounts)

**Example Priority Calculation:**

```javascript
priority = (basePriority * 2) + daysSinceCreation;

if (customer.isVIP) priority += 10;
if (amount > 10000) priority += 5;
```

**Workload Visibility:**

```
Team Dashboard
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Alice:  8 tasks (2 overdue)
Bob:    3 tasks
Carol: 12 tasks (1 overdue) ← Overloaded
Dave:   5 tasks

Action: Reassign 4 of Carol's tasks to Bob
```

**Metrics:**
- **Average time in queue** — How long before claimed?
- **Average completion time** — How long to complete once claimed?
- **Queue depth** — How many tasks waiting?
- **Overdue rate** — What % miss their deadlines?

---

## Human Decision Points vs Automated Rules

**When should humans decide, and when should systems decide?**

### Pattern 1: Automated Decision with Human Override

**System decides, human can intervene.**

```
[⚙️ Evaluate loan application]
 ↓
◇× Auto-decision confidence?
├─ High (> 95%) → [⚙️ Auto-approve or deny]
│                  │
│                  └──→ (Human can appeal/override within 24 hours)
│
└─ Low (< 95%) → [👤 Manual underwriter review]
```

**When to use:**
- High-volume, low-complexity decisions
- System is accurate most of the time
- Humans need escape hatch for edge cases

### Pattern 2: Human Decision with System Recommendation

**System recommends, human decides.**

```
[⚙️ Calculate risk score]
 ↓
[⚙️ Generate recommendation: "Approve" or "Deny"]
 ↓
[👤 Underwriter reviews recommendation]
 ↓
◇× Human decision?
├─ Approve → [Continue]
└─ Deny → [Send rejection letter]
```

**Display to human:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Loan Application #12345

System Recommendation: APPROVE
Confidence: 87%
Risk Score: 72/100

Reasoning:
✅ Credit score: 720 (Good)
✅ Income: 3.5x monthly payment
⚠️  DTI: 38% (slightly high)
✅ Employment: 5 years stable

Your Decision:
○ Approve  ○ Deny  ○ Request more info
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**When to use:**
- Medium complexity
- System provides value but humans add judgment
- Regulatory requirement for human decision
- Building trust in automation (humans see system's reasoning)

### Pattern 3: Conditional Automation

**Simple cases automated, complex cases to humans.**

```
[⚙️ Evaluate claim]
 ↓
◇× Claim complexity?
├─ Simple → [⚙️ Auto-process]
├─ Moderate → [👤 Claims processor]
└─ Complex → [👤 Senior adjuster]
```

**Complexity criteria:**
- Amount (> $10K = complex)
- Type (property damage = complex, simple illness = simple)
- Customer history (multiple recent claims = complex)
- Data completeness (missing info = complex)

---

## Notifications and Communication Patterns

**How do users know they have work to do?**

### Notification Strategies

**1. Real-Time (Push)**
- Immediate notification when task assigned
- Methods: Email, Slack, SMS, mobile push, desktop notification
- **When:** Urgent tasks, high-priority items

**2. Batch (Daily Digest)**
- Summary email once per day
- "You have 8 tasks due this week"
- **When:** Non-urgent tasks, avoid notification fatigue

**3. Pull (User Checks Dashboard)**
- No notification, user checks queue when ready
- **When:** Low priority, users have dedicated time for task work

**Example: Invoice Approval Notifications**

```
Immediate: [HIGH] Invoice over $50K requires approval
Daily Digest: You have 3 invoices to approve (total: $12K)
Dashboard Only: Low-priority invoice corrections (8 items)
```

### Notification Content Best Practices

**Include:**
- ✅ What (task name)
- ✅ Why (context, reason for assignment)
- ✅ When (due date, urgency)
- ✅ How (direct link to take action)
- ✅ Who (who assigned it, who else is involved)

**Example Email:**

```
Subject: [ACTION REQUIRED] Approve Invoice #5678 - Due Today

Hi Alice,

You have been assigned an invoice approval task.

Vendor: Acme Corp
Amount: $8,500
Invoice #: 5678
Due: Today at 5:00 PM

This invoice is for the Q1 consulting services contract.

[Approve] [Reject] [View Details]

Note: This was escalated from Bob who is on vacation.
```

---

## Petroleum Terminal Example: Driver Check-In Workflow

Let's design a human ↔ software workflow for **driver check-in** at a petroleum terminal.

**Process:**

```
┌──────────────────────────────────────────────────────────────────┐
│ Gate Attendant (Human)                                           │
│                                                                   │
│ (○) Driver arrives at gate                                       │
│  ↓                                                                │
│ [👤 Scan driver license]                                         │
│  │                                                                │
│  │ (System displays)                                             │
│  │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━                       │
│  │ Driver: John Smith                                            │
│  │ License: TX-12345678                                          │
│  │ Certification: Hazmat (Exp: 2025-06-30) ✅                    │
│  │ Background: Clear ✅                                           │
│  │ Recent Violations: None ✅                                     │
│  │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━                       │
│  ↓                                                                │
│ [⚙️ Auto-check: Credentials valid?]                             │
│  ↓                                                                │
│ ◇× Valid?                                                        │
│ ├─ No → [👤 Override or deny?] ─┐                                │
│ │       (Human decision)         │                               │
│ │       ○ Deny entry             │                               │
│ │       ○ Allow with supervisor  │                               │
│ │                                │                               │
│ └─ Yes                           │                               │
│     ↓                             │                               │
│    [👤 Verify load order number] │                               │
│     ↓                             │                               │
│    [⚙️ Look up load order]       │                               │
│     │                             │                               │
│     │ (System displays)           │                               │
│     │ ━━━━━━━━━━━━━━━━━━━━━━━━━ │                               │
│     │ Load Order #: LO-2024-5678  │                               │
│     │ Product: Diesel             │                               │
│     │ Quantity: 8,000 gal         │                               │
│     │ Bay: Assigned (Bay 3)       │                               │
│     │ Time: 2:00 PM - 4:00 PM     │                               │
│     │ Status: SCHEDULED ✅        │                               │
│     │ ━━━━━━━━━━━━━━━━━━━━━━━━━ │                               │
│     ↓                             │                               │
│    ◇× Load order found & active? │                               │
│    ├─ No → [👤 Contact logistics] → [Resolve issue ⊞]            │
│    │                                                               │
│    └─ Yes                                                         │
│        ↓                                                          │
│       [⚙️ Auto-check: On-time?]                                  │
│        ↓                                                          │
│       ◇× Within scheduled window?                                │
│       ├─ No, early (> 30 min) → [👤 Allow early entry?]          │
│       │                         ○ Yes: Proceed                   │
│       │                         ○ No: Ask driver to wait         │
│       │                                                           │
│       ├─ No, late (> 15 min) → [⚙️ Notify operations: Late]      │
│       │                        → [👤 Proceed with check-in]      │
│       │                          (Human can still approve)       │
│       │                                                           │
│       └─ Yes, on time                                            │
│           ↓                                                       │
│          [⚙️ Print gate pass]                                    │
│           ↓                                                       │
│          [👤 Issue pass to driver]                               │
│           ↓                                                       │
│          [👤 Direct driver to Bay 3]                             │
│           ↓                                                       │
│          [⚙️ Notify operations: Driver checked in]               │
│           ↓                                                       │
│          (⦿) Check-in complete                                   │
└──────────────────────────────────────────────────────────────────┘
```

**Human vs System Breakdown:**

| Step | Who | Why |
|------|-----|-----|
| Scan license | Human | Physical action |
| Check credentials valid | **System** | Fast, accurate, rule-based |
| Override invalid credentials | **Human** | Judgment call (maybe cert just renewed, not in system yet) |
| Verify load order number | Human | Driver tells gate attendant the number |
| Look up load order | **System** | Database query |
| Resolve missing load order | **Human** | Problem-solving, communication |
| Check on-time | **System** | Simple time comparison |
| Allow early entry | **Human** | Judgment (is bay free? is it safe?) |
| Print gate pass | **System** | Automated output |
| Issue pass to driver | Human | Physical handoff |
| Direct driver to bay | Human | Communication, safety check |
| Notify operations | **System** | Automated message |

**Form for Gate Attendant:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
DRIVER CHECK-IN

Driver: John Smith
License: TX-12345678 ✅ VALID
Hazmat Cert: ✅ VALID (Exp: 2025-06-30)

Load Order: LO-2024-5678
Product: Diesel | Quantity: 8,000 gal
Bay: 3 | Time: 2:00 PM - 4:00 PM

Current Time: 1:45 PM
Status: ⚠️  15 minutes early

Action:
○ Allow check-in now (Bay 3 is available)
○ Ask driver to wait until 2:00 PM

Comments: [Optional notes]

[Approve Check-In] [Deny Entry]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Notifications:**

```
To: Terminal Operations Team
Subject: Driver Checked In - Bay 3

Driver John Smith has checked in for load order LO-2024-5678.
Assigned to Bay 3.
Expected loading time: 2:00 PM - 4:00 PM.

[View Details] [Contact Driver]
```

---

## Hands-On Exercise: Design a Human ↔ Software Workflow

**Choose ONE process from your domain:**

1. **Petroleum Terminal:** Loading operation execution (human operator + automated systems)
2. **Hospital:** Patient triage (nurse + decision support system)
3. **E-Commerce:** Order exception handling (CS rep + automated tools)
4. **Property Management:** Maintenance request processing (manager + work order system)

### Part 1: Map Automation Levels (30 minutes)

For each step in your process, determine:

| Step | Manual | System-Assisted | Collaborative | System-Led | Automated |
|------|--------|-----------------|---------------|------------|-----------|
| ... | | | | | |

Document **why** each step requires that level of automation.

**Deliverable:** Automation level analysis (1-2 pages)

### Part 2: Design User Tasks (45 minutes)

For 3-5 human tasks, design:
1. Task name (clear, action-oriented)
2. Assignee (individual, role, or assignment rule)
3. Form (what user sees and provides)
4. Priority and due date logic
5. Context provided to user
6. Escalation rules (what happens if overdue)

**Deliverable:** User task specifications

### Part 3: Create BPMN with Human ↔ System Interaction (60 minutes)

Model the complete process showing:
- User tasks (👤) vs Service tasks (⚙️)
- Forms and information displayed to users
- Decision points (human decides vs system decides)
- System recommendations to humans
- Escalation boundary events
- Notifications (when and to whom)

**Deliverable:** Complete BPMN diagram

### Part 4: Design Work Queue and Notifications (30 minutes)

1. **Work queue design:**
   - What tasks appear in the queue?
   - How are they prioritized?
   - How are they assigned?

2. **Notification strategy:**
   - Which tasks trigger immediate notifications?
   - Which go in daily digest?
   - Which are dashboard-only?
   - What does the notification content include?

**Deliverable:** Queue and notification specification

---

## Reflection Questions

1. **Over-automation vs under-automation:** Which is worse? Why? Can you think of examples of each?

2. **The "automation paradox":** Automating easy tasks makes remaining work harder (all the exceptions and edge cases). How do you design for this?

3. **Trust in automation:** When should systems explain their decisions to humans? When is a "black box" acceptable?

4. **From your operational experience:** Think of times when you had to override automated systems. What would have made the override easier?

5. **Work queue psychology:** How does seeing a long queue affect worker behavior? How can queue design encourage productive work patterns?

---

## Key Takeaways

✅ **Blend automation with human judgment** — Neither fully manual nor fully automated is ideal. The art is knowing what to automate.

✅ **Automate the routine, human for exceptions** — Let systems handle high-volume clear-cut cases. Humans handle nuance and edge cases.

✅ **Design user tasks thoughtfully** — Clear forms, complete context, direct links to action. Make it easy for humans to do their job.

✅ **Task assignment matters** — Right task to right person at right time. Use roles, rules, skills, and load balancing.

✅ **Escalation prevents gridlock** — Tasks can't sit forever. Timeouts and escalation ensure flow continues.

✅ **Queues need prioritization** — Not all tasks are equal. Priority, due date, age, business rules determine order.

✅ **System recommendations + human decisions** — Show humans what the system thinks and why, but let them decide.

✅ **Notifications drive action** — Right notification at right time through right channel. Avoid fatigue.

✅ **Measure and optimize** — Track queue depth, completion time, overdue rate. Continuously improve.

---

## Connection to Week 22

This week you learned how to design workflows where **humans and systems collaborate effectively**—balancing automation with judgment, designing great user tasks, managing queues, and handling escalations.

Next week you'll learn **Workflow to Domain Event Integration + PROJECT 5**—how BPMN workflows integrate with domain models (from Course 4), publishing events, subscribing to events, and building complete systems that blend process orchestration with domain logic.

You'll also complete **PROJECT 5**, the capstone for Course 5, creating a complete business process design with workflows, domain integration, and automation strategy.

**Get ready to bring workflows and domain models together.**

---

## Additional Resources

**Books:**
- *Designing with the Mind in Mind* by Jeff Johnson — UI/UX for task interfaces
- *The Lean Startup* by Eric Ries — Automation and validated learning

**Workflow Platforms:**
- **Camunda** — BPMN execution, task management, forms
- **Flowable** — Open-source workflow engine
- **Temporal** — Modern workflow orchestration

**Task Management:**
- Asana, Monday.com — Work queue patterns
- Jira — Ticket routing and escalation

**For Your Context:**
- Design work queues for terminal operations (loading assignments, bay scheduling)
- Model human + SCADA interactions (operator monitors automated systems, intervenes when needed)
- Optimize escalation paths (when does terminal supervisor get involved?)

---

**End of Week 21 — Human ↔ Software Workflows**

**Next:** Week 22 — Workflow to Domain Event Integration + PROJECT 5
