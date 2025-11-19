# Week 10: Requirements Elicitation Techniques
## The Art and Science of Extracting What Software Must Do From People Who Don't Know How to Tell You

---

## Introduction: The Silent Requirements That Cost $800,000

A hospital hired consultants to build a patient discharge system. After months of interviews with doctors and nurses about the discharge process, they built exactly what was described.

**Launch day:** System rejected by the discharge planning team.

**Why?** Nobody interviewed the **discharge planners** — a separate role that coordinates post-hospital care, insurance approvals, and medical equipment delivery.

The requirements the developers gathered were only 40% of what the system actually needed to do. The remaining 60%:

- **Insurance pre-authorization workflows** (discharge planners spend 2 hours/patient on this)
- **Durable medical equipment ordering** (wheelchairs, oxygen, hospital beds for home)
- **Home health agency coordination** (nurses, physical therapists)
- **Transportation arrangement** (ambulance vs. wheelchair van vs. family)
- **Medication reconciliation** (what prescriptions need to be filled before discharge)
- **Follow-up appointment scheduling** (with primary care or specialists)

**Why were these missed?**

1. **Wrong stakeholders interviewed** (doctors/nurses don't do discharge planning)
2. **Observation didn't include discharge planning department** (different floor, different workflow)
3. **No document review** (discharge checklists existed but weren't analyzed)
4. **No workshop bringing all roles together** (would have revealed the handoffs)

**Cost of rework:** $800,000 and 9 additional months.

**This week, you'll learn systematic techniques for extracting complete requirements** — ensuring you talk to the right people, ask the right questions, and uncover the requirements hiding in documents, workflows, and legacy systems.

---

## Core Concept 1: Stakeholder Interviews (Requirements-Focused)

### How Requirements Interviews Differ From Discovery Interviews

**Week 6 (Discovery):** Broad exploration of problems
- "What's frustrating about your job?"
- "Walk me through a difficult day"
- Open-ended, exploratory

**Week 10 (Requirements):** Specific extraction of system behaviors
- "What must the system do when X happens?"
- "What decision do you make at this step, and what information do you need?"
- Structured, detailed, focused

### The Requirements Interview Framework

**1. Pre-Interview Preparation**

**Research the stakeholder:**
- What's their role?
- What processes do they own?
- What decisions do they make?
- What systems do they currently use?

**Prepare targeted questions:**
- What workflows will they interact with?
- What data do they need to see/enter?
- What decisions require system support?
- What errors or exceptions occur?

**Example prep for terminal scheduler interview:**
```
Role: Terminal Scheduler
Processes: Truck appointment booking, bay assignment, conflict resolution
Current systems: Whiteboard, phone, email
Key questions:
- What information do you need from customers when they request appointments?
- How do you decide which bay to assign?
- What happens when two trucks want the same time?
- What reports do you need at end of day/week/month?
```

**2. Interview Structure**

**Opening (5 minutes):**
- Explain purpose: "I'm gathering requirements for [system]. I want to understand exactly what the system needs to do to support your work."
- Set expectations: "I'll ask about specific tasks, decisions, and information you need. Some questions will be very detailed."
- Request permission for recording/notes

**Context Setting (10 minutes):**
- "Walk me through your typical day"
- "What are your top 3-5 responsibilities?"
- "What systems/tools do you use?"

**Deep Dive (30-45 minutes):**
- Focus on specific workflows
- Extract inputs, outputs, decisions, rules
- Identify exceptions and error cases

**Wrap-Up (5 minutes):**
- "What did I not ask about that I should have?"
- "Who else should I talk to?"
- "Can I follow up if I have clarifications?"

**3. Requirements-Specific Questions**

**For each workflow, ask:**

**Triggers:** "What starts this process?"
- User action? External event? Scheduled time? Another system?

**Inputs:** "What information do you need to complete this task?"
- Where does it come from? Who provides it? In what format?

**Processing:** "What steps do you take?"
- What do you check/validate? What calculations? What decisions?

**Outputs:** "What do you produce?"
- Documents? Data? Notifications? Handoffs to someone else?

**Business Rules:** "What rules govern this?"
- Policies? Regulations? Calculations? Constraints?

**Exceptions:** "What can go wrong?"
- How often? How do you handle it? What information helps you decide?

**Success Criteria:** "How do you know you did it correctly?"
- What confirms success? What checks happen later?

### Example: Terminal Scheduler Interview Extract

**You:** "Walk me through what happens when a customer calls to book an appointment."

**Scheduler:** "They tell me what product they need, how much, and when they want to pick it up."

**You:** "What specific information do you need from them?"

**Scheduler:** "I need the product type — like diesel or unleaded — the volume in barrels, their preferred date and time, and their company name. Oh, and the truck number if they know it."

**You:** "What if they don't know the truck number?"

**Scheduler:** "That's fine, I can leave it blank. The driver gives it to us when they arrive."

**You:** "Once you have that information, what do you do next?"

**Scheduler:** "I check which bays can handle that product. Not all bays work for all products."

**You:** "How do you know which bays can handle which products?" ← *Uncovering business rule*

**Scheduler:** "Bays 1 and 2 are diesel only. Bays 3 and 4 can do anything. Bay 5 is gasoline only."

**You:** "Got it. Then what?"

**Scheduler:** "I look at my whiteboard to see if the time they want is free on one of those bays."

**You:** "What if it's not free?"

**Scheduler:** "I suggest the closest available time, usually within 30 minutes of what they asked for."

**You:** "Do they always accept that?"

**Scheduler:** "Usually. Sometimes they say they'll call back."

**You:** "Once you agree on a time, what happens?"

**Scheduler:** "I write it on the whiteboard with the company name, volume, product, and bay number."

**You:** "Do you confirm anything back to the customer?"

**Scheduler:** "Yeah, I tell them the bay number and time, and remind them to have their paperwork ready."

**You:** "What paperwork?"

**Scheduler:** "BOL — Bill of Lading — and the driver's credentials."

**You:** "What happens if they show up without the BOL?" ← *Exception case*

**Scheduler:** "We can't load them. They have to get it from their office and come back."

**You:** "How often does that happen?"

**Scheduler:** "Maybe once a week. It's annoying."

**Requirements extracted from this short conversation:**

**Functional Requirements:**
- FR-050: System shall capture appointment request including product type, volume, date/time, company name, truck number (optional)
- FR-051: System shall display only bays compatible with requested product type
- FR-052: System shall show bay availability for requested time
- FR-053: System shall suggest alternative times if requested slot unavailable
- FR-054: System shall record confirmed appointments with all details
- FR-055: System shall display required documents (BOL, driver credentials) in confirmation

**Business Rules:**
- BR-030: Bays 1-2 handle diesel only; Bays 3-4 handle all products; Bay 5 handles gasoline only
- BR-031: Alternative appointment suggestions should be within 30 minutes of requested time
- BR-032: BOL and driver credentials required before loading can begin

**Exception Cases:**
- EX-010: Customer calls back later (appointment request not finalized)
- EX-011: Driver arrives without BOL (cannot proceed with loading)

---

## Core Concept 2: Requirements Workshops & Joint Application Design (JAD)

### When to Use Workshops

**Workshops work best when:**
- Multiple stakeholders interact with same process
- Requirements span multiple roles/departments
- Need to resolve conflicting needs
- Want to build consensus quickly
- Have limited time with busy executives

**Example scenarios:**
- Order fulfillment (sales, warehouse, shipping, accounting)
- Patient care coordination (doctors, nurses, pharmacy, lab)
- Loan approval (loan officer, underwriter, compliance, operations)

### Workshop Structure

**Typical 2-3 hour session:**

**1. Opening (15 min)**
- Introduce participants
- State objectives: "Define requirements for [specific scope]"
- Set ground rules: "Everyone's input matters, no idea is bad, focus on needs not solutions"

**2. Process Walkthrough (45 min)**
- Map the end-to-end process on whiteboard
- Each participant adds their steps
- Identify handoffs between roles
- Note pain points and exceptions

**3. Requirements Brainstorming (45 min)**
- "What must the system do at each step?"
- Capture on sticky notes or digital board
- Group similar requirements
- Prioritize (must-have, should-have, nice-to-have)

**4. Validation (30 min)**
- Review requirements list
- Check for gaps
- Identify conflicts
- Assign owners for follow-up

**5. Next Steps (15 min)**
- Document action items
- Schedule follow-ups
- Thank participants

### Facilitation Techniques

**Keep it productive:**

**Use timeboxes:**
- "We'll spend 10 minutes on this topic, then move on"
- Prevents endless debates

**Parking lot for scope creep:**
- "That's important, but out of scope for this session. Let's add it to the parking lot."
- Revisit at end if time allows

**Encourage quiet voices:**
- "Sarah, you haven't spoken yet. What's your perspective on this?"
- Use round-robin technique

**Resolve conflicts:**
- "I'm hearing different needs from sales vs. operations. Let's identify the core requirement that satisfies both."
- Focus on underlying goals, not positions

### Example: Loan Approval Workshop

**Participants:**
- Loan Officer (takes applications)
- Underwriter (evaluates risk)
- Compliance Officer (ensures regulatory adherence)
- Operations Manager (oversees workflow)

**Process mapped on whiteboard:**
```
[Loan Officer] → Application Entry → [System]
     ↓
[System] → Credit Check → [Credit Bureau API]
     ↓
[Underwriter] → Risk Assessment → [System]
     ↓
[Compliance] → Regulatory Review → [System]
     ↓
[System] → Decision Notification → [Customer]
     ↓
[Operations] → Funding → [Bank Core System]
```

**Requirements identified:**

**From Loan Officer:**
- "I need the system to pre-fill customer data if they've applied before"
- "I need to upload supporting documents (pay stubs, tax returns)"
- "I need to see application status so I can answer customer calls"

**From Underwriter:**
- "I need to see complete application plus credit report in one view"
- "I need to document my decision rationale (required for audits)"
- "I need to request additional documentation if needed"

**From Compliance:**
- "I need the system to flag applications that trigger HMDA reporting"
- "I need to ensure Equal Credit Opportunity Act disclosures are provided"
- "I need immutable audit trail of all decisions"

**Conflict identified:**
- Loan Officer wants fast approvals ("can we auto-approve low-risk loans?")
- Compliance wants human review ("regulations require manual review for certain criteria")
- **Resolution:** Auto-approve only for specific criteria (credit score > 750, loan < $50k, existing customer), all others require review

---

## Core Concept 3: Document Analysis

### Sources of Requirements in Documents

**Many requirements already exist in writing** — you just need to extract them.

**Document types:**

**1. Regulatory Requirements**
- Laws, regulations, compliance standards
- Industry certifications (ISO, SOC 2, HIPAA)
- Audit requirements

**2. Existing System Documentation**
- User manuals
- Process procedures
- Training materials
- Help desk tickets (common issues)

**3. Business Process Documentation**
- Standard Operating Procedures (SOPs)
- Workflow diagrams
- Policy manuals
- Quality assurance checklists

**4. Contracts and SLAs**
- Service level agreements
- Vendor contracts
- Customer contracts

**5. Forms and Templates**
- Paper forms currently used
- Spreadsheet templates
- Report templates

### Document Analysis Process

**Step 1: Collect Documents**

Request from stakeholders:
- "Can you share any documentation about how this process currently works?"
- "Are there regulations we need to comply with?"
- "Do you have checklists or forms you use?"

**Step 2: Review and Extract**

**Look for:**
- **Shall statements:** "Employees shall obtain manager approval for..."
- **Required data fields:** Forms show what data is captured
- **Calculations:** Spreadsheets reveal formulas and business rules
- **Decision criteria:** "If X then Y" logic in procedures
- **Validation rules:** "Valid entries are..."
- **Reports:** What outputs are expected

**Step 3: Categorize**

Mark each finding:
- Functional requirement (FR)
- Non-functional requirement (NFR)
- Business rule (BR)
- Constraint (CON)

**Step 4: Validate**

**Don't assume documentation is accurate!**
- People may not follow documented procedures
- Regulations may have changed
- Forms may be outdated

**Validate through interviews or observation.**

### Example: Petroleum Terminal Document Analysis

**Document:** Terminal Safety Procedures Manual (Section 7: Truck Loading)

**Excerpt:**
> "Prior to commencing loading operations, the operator shall verify:
> 1. Driver has valid CDL and TWIC credentials
> 2. Truck placards match product being loaded
> 3. Emergency shutoff is functional
> 4. Grounding cable is attached
> 5. Tank level does not exceed safe fill capacity (95%)
>
> If any verification fails, loading shall not proceed until issue is resolved.
>
> During loading, operator shall monitor flow rate and total volume.
> If flow rate deviates more than 10% from expected, operator shall investigate.
>
> Upon completion, operator shall record:
> - Start time and end time
> - Opening and closing meter readings
> - Total volume loaded
> - Product temperature and density
> - Any anomalies or incidents"

**Requirements extracted:**

**FR-120:** System shall verify driver credentials (CDL and TWIC) before permitting loading operation to begin

**FR-121:** System shall verify truck placard matches product type

**FR-122:** System shall require operator confirmation that emergency shutoff and grounding cable are functional

**FR-123:** System shall check tank capacity and prevent loading if tank level exceeds 95%

**FR-124:** System shall record start time, end time, meter readings, volume, temperature, density for each loading operation

**FR-125:** System shall alert operator if flow rate deviates more than 10% from expected rate

**BR-050:** (Business Rule) Safe fill capacity is defined as 95% of tank maximum capacity

**BR-051:** (Business Rule) Flow rate deviation threshold is 10%

**CON-020:** (Constraint) All loading records must be retained for regulatory compliance (duration TBD)

**Notice:** One page of documentation yielded 8 requirements and business rules.

---

## Core Concept 4: Observation and Ethnographic Studies

### Observation for Requirements (Building on Week 6)

**Week 6:** You observed to discover problems
**Week 10:** You observe to extract requirements

**Different focus:**

**Week 6 questions:**
- What's frustrating?
- Where's the waste?
- What workarounds exist?

**Week 10 questions:**
- What data do they enter/read?
- What decisions do they make and based on what information?
- What validation do they perform?
- What outputs do they generate?
- What system responses would help?

### Requirements-Focused Observation Template

**As you shadow a user, document:**

**1. Task being performed:**
- Name of task
- Frequency (how often)
- Typical duration

**2. Data inputs:**
- What information do they need to start?
- Where does it come from? (system, paper, memory, asking someone)

**3. Processing steps:**
- What do they do with the inputs?
- What do they calculate, validate, look up?

**4. Decisions made:**
- What choices do they make?
- What criteria do they use?
- Where did they learn these criteria?

**5. Outputs produced:**
- What do they create? (document, data entry, notification)
- Who receives it?

**6. Exception handling:**
- What goes wrong during this task?
- How do they handle it?

**7. Unspoken requirements:**
- What do they check that they didn't mention in interviews?
- What workarounds suggest missing system capabilities?

### Example: Observing Hospital Discharge Planner

**Task:** Arranging home oxygen equipment for patient discharge

**Data inputs:**
- Doctor's prescription (oxygen flow rate, duration)
- Patient's insurance information (from intake)
- Patient's home address
- Patient's phone number

**Processing:**
1. Calls insurance to verify home oxygen coverage *(manual, time-consuming)*
2. Waits on hold for insurance rep (observed 15 minute wait)
3. Verifies coverage, notes any copay amount
4. Looks up approved oxygen equipment vendors in binder *(paper-based, outdated)*
5. Calls vendor to arrange delivery
6. Provides vendor with prescription details, patient address, insurance info
7. Confirms delivery date/time with vendor
8. Calls patient to confirm they'll be home for delivery

**Decisions:**
- Which vendor to call first (based on past experience with reliability)
- Whether delivery timeframe is acceptable (patient can't leave hospital until oxygen confirmed)

**Outputs:**
- Note in patient chart: "Home oxygen arranged, delivery [date/time]"
- Discharge hold released once oxygen confirmed

**Exception observed:**
- Insurance denied coverage for specific oxygen concentrator model
- Planner had to call doctor to modify prescription to covered equipment
- Added 45 minutes to process

**Requirements extracted:**

**FR-200:** System shall lookup insurance coverage for prescribed durable medical equipment and display coverage status, copay amount, and approved vendors

**FR-201:** System shall maintain vendor contact information with notes on reliability and service area

**FR-202:** System shall create task for calling patient to confirm delivery acceptance

**FR-203:** System shall prevent discharge workflow from proceeding until home medical equipment delivery is confirmed

**FR-204:** System shall record timeline of discharge planning activities for audit

**NFR-050:** Insurance coverage lookup should complete in under 30 seconds (vs. 15 minute phone wait observed)

**BR-070:** Discharge cannot be approved until all prescribed home medical equipment delivery is confirmed

**Observation revealed:**
- Hidden 15-minute wait times (requirement for automated insurance lookup)
- Paper binder of vendors (requirement for vendor management)
- Exception case insurance representatives never mentioned (need for prescription modification workflow)

---

## Core Concept 5: Prototyping and Mockups

### Show, Don't Just Ask

**People don't know what they want until they see something concrete.**

**Prototyping for requirements** (not solution design):
- Low-fidelity mockups to elicit requirements
- Iterate based on feedback
- Uncover unstated requirements

### Types of Prototypes

**1. Paper Sketches (Lowest Fidelity)**
- Hand-drawn UI sketches
- Fast to create and modify
- Good for early exploration

**2. Wireframes (Low Fidelity)**
- Digital mockups without styling
- Focus on layout and flow
- Tools: Balsamiq, Figma, Draw.io

**3. Clickable Prototypes (Medium Fidelity)**
- Simulated interactions
- User can "use" the system
- Tools: Figma, InVision, Axure

**4. Working Prototype (High Fidelity)**
- Functional but not production-quality
- Real data, limited features
- Rapid development frameworks

### Prototyping Session Structure

**1. Prepare prototype** (based on initial requirements)

**2. Show to stakeholders:** "Here's what we're thinking based on our conversations. React to it — what's wrong? What's missing?"

**3. Watch them interact:** Don't explain. See where they get confused.

**4. Ask specific questions:**
- "What would you do next?"
- "What information is missing here?"
- "Does this match your workflow?"

**5. Iterate:** Make changes on the spot if possible

**6. Extract new requirements** based on feedback

### Example: Terminal Scheduling Prototype

**Initial prototype** (wireframe in Figma):
- Calendar grid showing bays and time slots
- Form to create appointment: Date, Time, Company, Product, Volume
- List of upcoming appointments

**Stakeholder reaction:**

**Scheduler 1:** "I need to see which products are IN the tanks. If someone wants diesel and all our diesel tanks are empty, I can't promise that time."

**→ New Requirement:** FR-160: System shall display current inventory levels by product type when scheduling appointments

**Scheduler 2:** "Some companies have priority because they're big customers. I need to be able to bump a low-priority appointment if a high-priority customer requests that time."

**→ New Requirements:**
- FR-161: System shall support customer priority levels
- FR-162: System shall allow authorized users to override appointments with lower priority

**Scheduler 3:** "I don't see where I note special instructions. Like 'this truck needs two bays because it has two compartments.'"

**→ New Requirement:** FR-163: System shall allow free-text notes on appointments visible to loading operators

**Driver (when shown mobile view):** "I want to see where bay 3 is. I've never been to this terminal before."

**→ New Requirement:** FR-164: Mobile view shall include terminal map showing bay locations

**None of these came up in interviews.** The prototype made them visible.

---

## Core Concept 6: Questionnaires and Surveys (When to Use Them)

### When Surveys Work

**Good for:**
- Large number of similar stakeholders (100+ users)
- Quantifying preferences ("How important is feature X?")
- Validating assumptions across wide audience
- Baseline metrics (current satisfaction, usage patterns)

**Bad for:**
- Uncovering new requirements (open-ended interviews better)
- Understanding complex workflows (observation better)
- Small stakeholder groups (just interview them)

### Survey Design for Requirements

**Question types:**

**1. Current state questions:**
- "How often do you perform [task]?" (Daily / Weekly / Monthly / Rarely)
- "How long does [task] typically take?" (< 5 min / 5-15 min / 15-30 min / > 30 min)
- "What tools do you currently use for [task]?" (Free text)

**2. Pain point quantification:**
- "How frustrating is [problem]?" (Scale 1-5)
- "How much time do you waste on [activity] per week?" (Hours)

**3. Feature prioritization:**
- "Rank these capabilities by importance" (Drag-and-drop ranking)
- "Which of these would you use?" (Multi-select)

**4. Validation questions:**
- "If the system could [do X], would that solve your problem?" (Yes / No / Partially)

**Keep surveys short:** 5-10 minutes max. Higher completion rate.

### Example: Survey for Expense Report System

**Sent to 200 employees who submit expense reports:**

**Q1:** How often do you submit expense reports?
- [ ] Weekly
- [ ] Monthly
- [ ] Quarterly
- [ ] Rarely

**Q2:** On average, how long does it take you to complete an expense report?
- [ ] Less than 15 minutes
- [ ] 15-30 minutes
- [ ] 30-60 minutes
- [ ] More than 1 hour

**Q3:** What causes delays in your expense reporting? (Select all that apply)
- [ ] Finding and attaching receipts
- [ ] Remembering business purpose for each expense
- [ ] Categorizing expenses correctly
- [ ] Getting manager approval
- [ ] System is slow or crashes
- [ ] Other: ___________

**Q4:** How important is each feature? (Scale: Not Important / Somewhat / Very Important)
- Mobile app for photo receipt capture: _____
- Auto-categorization of expenses: _____
- Integration with corporate card transactions: _____
- Real-time approval status: _____

**Results:**
- 78% submit monthly
- 65% spend 30-60 minutes (too long!)
- Top delay: "Finding receipts" (82%)
- Most requested: Mobile receipt capture (92% "Very Important")

**Requirements extracted:**
- FR-300: System shall provide mobile app with camera integration for receipt capture
- FR-301: System shall associate receipts with expenses automatically
- NFR-070: Average time to complete expense report should be under 20 minutes (target 67% reduction)

---

## Core Concept 7: Reverse Engineering Legacy Systems

### When You're Replacing Something That Exists

**Often you're not building from scratch** — you're replacing or modernizing a legacy system.

**The legacy system contains embedded requirements** (though not always documented).

### Reverse Engineering Techniques

**1. Use the system yourself**
- Create test accounts
- Perform real workflows
- Note every feature, validation, message

**2. Review the codebase** (if accessible)
- Look for business logic in code
- Identify validation rules
- Extract calculations and algorithms
- Note error messages and edge cases

**3. Analyze the database schema**
- Tables reveal entities
- Columns reveal data requirements
- Constraints reveal business rules
- Relationships reveal dependencies

**4. Examine reports**
- What reports does the system generate?
- What data must exist to populate those reports?

**5. Review logs and audit trails**
- What events are tracked?
- What compliance requirements are reflected?

**6. Talk to people who built it** (if still available)
- "Why did you build it this way?"
- "What requirements drove this design?"

**7. Analyze support tickets**
- What features are users asking for?
- What bugs reveal unstated requirements?

### Example: Modernizing Legacy Inventory System

**Legacy system:** 20-year-old Access database for warehouse inventory

**Reverse engineering process:**

**1. Exploration:**
- Logged in, added test item, moved it, removed it
- Noted required fields, validation messages, available reports

**2. Database analysis:**
```sql
-- Found these tables:
Items (item_id, sku, description, category, unit_cost, reorder_point, supplier_id)
Locations (location_id, warehouse, aisle, shelf, bin)
Inventory (item_id, location_id, quantity, last_counted)
Transactions (transaction_id, item_id, type, quantity, timestamp, user_id, notes)
```

**3. Requirements extracted from schema:**

**FR-400:** System shall track items with SKU, description, category, unit cost, reorder point, and supplier

**FR-401:** System shall support multi-level location hierarchy (warehouse > aisle > shelf > bin)

**FR-402:** System shall maintain inventory balance by item and location

**FR-403:** System shall record all inventory transactions with type, quantity, timestamp, user, and notes

**BR-100:** (from `reorder_point` column) System must support automatic reorder triggers when quantity falls below threshold

**4. Code analysis found:**

```vb
' VBA code in form validation
If Quantity < 0 Then
   MsgBox "Quantity cannot be negative"
   Exit Sub
End If

If TransactionType = "Transfer" And SourceLocation = DestinationLocation Then
   MsgBox "Cannot transfer to same location"
   Exit Sub
End If
```

**Requirements extracted:**

**BR-101:** Inventory quantities cannot be negative
**BR-102:** Transfers to the same location are prohibited

**5. Report analysis revealed:**

"Monthly Inventory Valuation Report" calculates:
- Total units by item
- Unit cost × quantity = value
- Subtotals by category
- Grand total inventory value

**FR-404:** System shall calculate inventory valuation using unit cost × current quantity

**FR-405:** System shall generate inventory valuation reports by category and warehouse

**6. Support tickets showed:**

"Users want to adjust inventory without creating transaction records (cycle count corrections)"

**FR-406:** System shall support inventory adjustments with separate transaction type and required justification

**By reverse engineering, extracted 10+ requirements that weren't documented anywhere.**

---

## Practical Framework: Elicitation Technique Selection Matrix

### Choosing the Right Technique

| Technique | Best For | Avoid When | Time Investment |
|-----------|----------|------------|-----------------|
| **Interviews** | Deep understanding, complex workflows, expert knowledge | Very large user base | High (1-2 hrs per person) |
| **Workshops** | Cross-functional processes, building consensus, resolving conflicts | Simple single-role processes | Medium (2-3 hrs group) |
| **Document Analysis** | Regulatory compliance, existing procedures, replacing legacy | Greenfield projects with no docs | Low (hours of reading) |
| **Observation** | Tacit knowledge, workarounds, actual vs. stated behavior | Remote users, secure environments | High (4-8 hrs per person) |
| **Prototypes** | UI/UX requirements, uncovering unstated needs | Very early stages before any concept | Medium (hours to create + review) |
| **Surveys** | Quantifying across large groups, prioritizing features | Small groups, complex workflows | Low (create) / Low (respond) |
| **Reverse Engineering** | Replacing legacy systems, understanding current state | Building something completely new | High (days of analysis) |

### Recommended Combinations

**For most projects, use:**
1. **Interviews** (3-5 key stakeholders) → Understand domain
2. **Observation** (1-2 people) → See reality vs. stated
3. **Workshop** (bring stakeholders together) → Resolve conflicts
4. **Prototype** (based on initial requirements) → Validate and refine
5. **Document Analysis** (regulations, procedures) → Ensure compliance

---

## Hands-On Exercise: Multi-Technique Requirements Elicitation

### Deliverable: Comprehensive Requirements Elicitation Plan

**Choose a process you want to understand deeply** (from your work or domain).

Produce:

#### 1. Elicitation Strategy (1 page)

**For your chosen process:**
- Scope: What specific process/workflow?
- Stakeholders: Who's involved? (List 5-8 people/roles)
- Current state: What exists today? (system, manual, hybrid)
- Goal: What are you trying to understand?

**Techniques you'll use:**
- List 3-4 techniques from this week
- For each, explain why it's appropriate
- Sequence them (which order?)

#### 2. Interview Guide (1-2 pages)

**Prepare for one key stakeholder interview:**
- Pre-interview research notes
- 15-20 prepared questions covering:
  - Workflow steps
  - Data inputs/outputs
  - Business rules
  - Decisions and criteria
  - Exceptions and errors
  - Success criteria

#### 3. Workshop Plan (if applicable) (1 page)

**If you're using a workshop:**
- Participants (roles)
- Agenda (time-boxed)
- Materials needed
- Expected outputs

#### 4. Document Analysis Plan (1 page)

**Documents to review:**
- List specific documents (SOPs, regulations, forms, reports)
- What you're looking for in each
- How you'll extract and categorize findings

#### 5. Observation Plan (1 page)

**Who you'll shadow:**
- Role/person
- Tasks to observe
- Duration
- Observation template you'll use

#### 6. Expected Requirements (1-2 pages)

**Before you start, hypothesize:**
- 5-10 requirements you expect to find
- Based on your current knowledge

**After elicitation** (if you actually execute):
- List actual requirements found
- Note which hypotheses were wrong
- Identify surprising discoveries

---

## Reflection Questions

1. **Past requirements gathering:** How have you gathered requirements before? Which techniques did you use? What did you miss?

2. **Stakeholder gaps:** Think of a project that had problems. Were any stakeholders not consulted? What requirements were missed as a result?

3. **Documentation review:** What documents exist in your domain that contain embedded requirements (regulations, procedures, forms)?

4. **Prototyping value:** Have you ever shown a mockup and gotten feedback that changed your understanding? What did you learn?

5. **Legacy systems:** If you had to replace a legacy system tomorrow, how would you extract its requirements? What techniques would you use?

---

## Key Takeaways

1. **Multiple techniques, always:** No single technique captures all requirements. Combine interviews, observation, workshops, and document analysis
2. **Requirements interviews ≠ Discovery interviews:** Week 6 was exploratory; Week 10 is specific and detailed
3. **Workshops build consensus:** Bring cross-functional stakeholders together to surface handoffs and conflicts
4. **Documents contain hidden requirements:** Regulations, procedures, forms, and reports embed requirements — extract them
5. **Observation reveals tacit knowledge:** What people do ≠ what they say. Shadow them to see reality
6. **Prototypes uncover unstated needs:** Show don't tell. Mockups reveal requirements people couldn't articulate
7. **Legacy systems are requirement sources:** Reverse engineering existing systems extracts embedded business logic

**Next week:** You'll learn about **state, events & transitions** — how to model the lifecycle of entities and capture temporal requirements that most engineers miss.

---

## Additional Resources

**Books:**
- *Software Requirements* by Karl Wiegers & Joy Beatty (Chapter 6-9: Elicitation techniques)
- *Exploring Requirements* by Gause & Weinberg (interview and workshop techniques)
- *Rapid Development* by Steve McConnell (JAD sessions)

**Templates:**
- Interview guide templates
- Workshop facilitation guide
- Requirements elicitation plan template
- Document analysis checklist

**Tools:**
- Miro or Mural (virtual workshops)
- Figma or Balsamiq (prototyping)
- Notion or Confluence (documentation)

**Practice:**
- Interview 3 people about the same process — note differences
- Facilitate a small workshop (even with 3 people)
- Analyze one regulatory document for requirements
- Shadow someone for 2 hours and extract 20 requirements

---

*The best requirements come from using multiple techniques. Interview to understand, observe to validate, workshop to resolve, prototype to refine.*

**End of Week 10**
