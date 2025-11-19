# Week 13: Requirements Validation & Verification
## Ensuring Requirements Are Complete, Consistent, and Ready to Build

---

## Introduction: The $4 Million Requirements Gap

A healthcare company hired a consulting firm to specify requirements for a patient portal. After 6 months and hundreds of pages of requirements, they handed the document to the development team.

**Development started. 9 months and $4 million later:**

**Problem 1:** Requirements contradicted each other
- Requirement 47: "System shall maintain patient data for 7 years"
- Requirement 203: "System shall automatically delete inactive accounts after 2 years"
- **Conflict:** Which rule wins? Nobody knew.

**Problem 2:** Requirements weren't testable
- Requirement 112: "System shall be user-friendly"
- **Question:** How do you test "friendly"? Nobody could say.

**Problem 3:** Requirements were technically infeasible
- Requirement 89: "System shall encrypt all data using quantum encryption"
- **Reality:** Quantum encryption wasn't commercially available

**Problem 4:** Requirements had gaps
- 50 requirements about scheduling appointments
- **Missing:** Who can cancel appointments? How far in advance? What notifications?

**Problem 5:** Requirements weren't traceable
- Requirement 156 existed
- **Question:** Why does this exist? Who needs it? Nobody remembered.

**The consulting firm had gathered requirements (Week 10). They had documented them (Weeks 9, 11, 12). But they never validated or verified them.**

**Result:** 6 months of rework, project delayed 18 months, $2M budget overrun.

**This week, you'll learn to validate and verify requirements** — ensuring they're complete, consistent, feasible, and testable before committing to build.

---

## Core Concept 1: Validation vs Verification

### The Critical Distinction

**Validation:** "Are we building the **right** thing?"
- Do requirements match actual needs?
- Will this solve the real problem?
- Did we understand what stakeholders want?

**Verification:** "Are we building the thing **right**?"
- Are requirements complete?
- Are they consistent?
- Are they testable?
- Can we actually build this?

### Validation Questions

**1. Correctness**
- Do requirements accurately reflect stakeholder needs?
- Did we understand the domain correctly?

**How to validate:**
- Review requirements with stakeholders
- Walk through scenarios together
- Ask: "If we build exactly this, will it solve your problem?"

**2. Necessity**
- Is each requirement actually needed?
- What happens if we don't implement it?

**How to validate:**
- Trace each requirement to business goal (from Week 9)
- Ask: "Why does this exist?"
- Challenge "nice to haves"

**3. Feasibility**
- Can we actually build this?
- With available technology?
- Within constraints (time, budget, skills)?

**How to validate:**
- Technical spike/proof-of-concept for risky requirements
- Architect review
- Vendor capability check for third-party dependencies

### Verification Questions

**1. Completeness**
- Are all necessary requirements captured?
- Are there gaps?

**How to verify:**
- Checklist reviews
- Traceability matrix (every business goal has requirements)
- Coverage analysis (every user role has use cases)

**2. Consistency**
- Do any requirements contradict?
- Are terms used consistently?

**How to verify:**
- Cross-reference analysis
- Glossary/ubiquitous language enforcement
- Automated consistency checks

**3. Unambiguity**
- Is each requirement clear?
- Could it be interpreted multiple ways?

**How to verify:**
- Multiple reviewers interpret independently
- If interpretations differ → ambiguous, must clarify

**4. Testability**
- Can we write a test to verify this requirement?
- What would pass/fail look like?

**How to verify:**
- For each requirement, write acceptance criteria
- If you can't define a test → requirement not testable

---

## Core Concept 2: Requirements Quality Checklist

### The SMART Requirements Framework (Revisited)

From Week 9, but now with verification techniques:

**Specific:**
- ❌ "System shall be fast"
- ✅ "System shall return search results within 500ms for 95% of queries"

**Verification:** Can you point to a specific measurement?

**Measurable:**
- ❌ "System should handle many users"
- ✅ "System shall support 10,000 concurrent users"

**Verification:** Can you count or measure the outcome?

**Achievable:**
- ❌ "System shall have zero bugs"
- ✅ "System shall maintain < 0.1% error rate in production"

**Verification:** Is this physically/technically possible?

**Relevant:**
- ❌ "Login screen shall use Comic Sans font"
- ✅ "Login screen shall meet WCAG 2.1 AA accessibility standards"

**Verification:** Does this trace to a business need or stakeholder request?

**Testable:**
- ❌ "System shall be intuitive"
- ✅ "New users shall complete first task without help within 5 minutes"

**Verification:** Can you write a test case with pass/fail criteria?

### The 8-Point Requirements Quality Checklist

**For each requirement, verify:**

**1. Unique identifier**
- ✅ Has format like REQ-042 or FR-089
- ❌ No way to reference this requirement

**2. Clear statement**
- ✅ Uses "shall" for mandatory, "should" for desired
- ✅ One requirement per statement (not compound)
- ❌ Vague or compound requirement

**3. Rationale**
- ✅ Explains why this requirement exists
- ✅ Traces to business need or stakeholder
- ❌ No justification

**4. Priority**
- ✅ Marked as Must Have, Should Have, or Nice to Have
- ✅ Stakeholders agreed on priority
- ❌ Everything marked "high priority"

**5. Acceptance criteria**
- ✅ Defines how to test this
- ✅ Clear pass/fail conditions
- ❌ No way to verify

**6. Dependencies**
- ✅ Lists related requirements
- ✅ Notes what must be built first
- ❌ Isolated, no context

**7. Source**
- ✅ Attributes to specific stakeholder or document
- ✅ Contact information for clarification
- ❌ Unknown origin

**8. Status**
- ✅ Current state (Proposed, Approved, Implemented, Verified)
- ✅ Change history
- ❌ Unknown if approved

---

## Core Concept 3: Completeness Analysis

### What Does "Complete" Mean?

**Complete requirements cover:**
- All functional behaviors
- All non-functional qualities
- All business rules
- All interfaces (user, system, external)
- All data requirements
- All states and transitions
- All scenarios (happy path + alternatives + exceptions + edge cases)
- All stakeholder needs

### Completeness Checklist

**Functional Coverage:**
- [ ] All user roles have associated use cases
- [ ] All business processes are documented
- [ ] All CRUD operations specified (Create, Read, Update, Delete)
- [ ] All calculations and algorithms defined
- [ ] All validations specified

**Non-Functional Coverage:**
- [ ] Performance requirements (response time, throughput)
- [ ] Scalability requirements (users, data volume, transaction load)
- [ ] Availability requirements (uptime, MTBF, MTTR)
- [ ] Security requirements (authentication, authorization, encryption, audit)
- [ ] Usability requirements (learnability, efficiency, error rates)
- [ ] Maintainability requirements (code quality, documentation, testability)
- [ ] Compliance requirements (regulatory, standards, audit)

**Interface Coverage:**
- [ ] All user interfaces specified (screens, reports, dashboards)
- [ ] All APIs specified (endpoints, request/response formats)
- [ ] All external system integrations specified
- [ ] All data imports/exports specified

**Data Coverage:**
- [ ] All entities identified
- [ ] All attributes specified (name, type, constraints)
- [ ] All relationships defined
- [ ] All business rules on data specified

**Scenario Coverage:**
- [ ] All use cases have main success scenarios
- [ ] All use cases have alternative flows
- [ ] All use cases have exception handling
- [ ] Edge cases identified and handled

### Traceability Matrix for Completeness

**Forward traceability:**
```
Business Goal → User Requirements → Functional Requirements → Use Cases → Test Cases
```

**Backward traceability:**
```
Test Case → Use Case → Functional Requirement → User Requirement → Business Goal
```

**Example Matrix:**

| Business Goal | User Requirement | Functional Requirement | Use Case | Test Case |
|---------------|------------------|------------------------|----------|-----------|
| BG-001: Reduce truck wait times | UR-012: Schedulers prevent conflicts | FR-089: Detect double-booking | UC-045: Process loading | TC-089 |
| BG-001 | UR-012 | FR-090: Send conflict alerts | UC-045 | TC-090 |
| BG-001 | UR-013: Drivers notified of appointments | FR-091: SMS notifications | UC-046: Notify driver | TC-091 |

**Gaps reveal incompleteness:**
- Business goal with no requirements → Incomplete
- Requirement with no use case → Not realized
- Use case with no test cases → Can't verify

---

## Core Concept 4: Consistency Analysis

### Types of Inconsistency

**1. Direct Contradiction**

```
REQ-047: System shall retain patient data for 7 years
REQ-203: System shall delete inactive accounts after 2 years

CONFLICT: Which rule wins? Must clarify.

RESOLUTION:
- REQ-047: Applies to medical records (regulatory requirement)
- REQ-203: Applies to account login credentials only
- Update both requirements to specify scope
```

**2. Terminology Inconsistency**

```
REQ-050: "Operator" records meter readings
REQ-075: "Terminal staff" issues bill of lading
REQ-098: "Employee" confirms product quality

QUESTION: Are these the same role or different?

RESOLUTION:
- Define glossary with clear terms
- "Operator" = Terminal Loading Operator (specific role)
- "Terminal staff" is vague → Replace with specific role
- Standardize all requirements to use consistent terms
```

**3. Temporal Inconsistency**

```
REQ-110: Order status changes to "Shipped" when tracking number assigned
REQ-145: Order must be in "Packed" state before shipping

PROBLEM: REQ-110 doesn't mention "Packed" state

RESOLUTION:
- Update REQ-110: Order transitions PACKED → SHIPPED when tracking number assigned
- Ensure state machine (Week 11) reflects this
```

**4. Numeric Inconsistency**

```
REQ-200: System shall support 5,000 concurrent users
REQ-240: Database connection pool size shall be 50 connections

PROBLEM: 5,000 users with 50 connections = 100 users per connection (likely insufficient)

RESOLUTION:
- Review connection pooling strategy
- Either increase pool size or clarify user activity patterns
```

### Consistency Verification Techniques

**1. Cross-Reference Matrix**

Create matrix showing relationships:

| Requirement | References | Referenced By | Potential Conflicts |
|-------------|------------|---------------|---------------------|
| REQ-047 | BR-020 (retention policy) | REQ-105, REQ-203 | REQ-203 (different retention) |

**2. Glossary Enforcement**

- Maintain single source of truth for terms
- Every requirement uses glossary terms
- Flag requirements using undefined or inconsistent terms

**3. State Machine Validation**

- All requirements about state must match state machine from Week 11
- If requirement mentions state transition, verify it exists in state diagram
- If state diagram has transition, ensure requirement documents it

**4. Automated Checks**

- Parse requirements for conflicting keywords (e.g., "shall" vs "shall not" on same entity/action)
- Detect numeric inconsistencies
- Find duplicate requirement IDs

---

## Core Concept 5: Requirements Reviews and Walkthroughs

### Types of Requirements Reviews

**1. Peer Review**
- One or more colleagues review requirements
- Look for defects, ambiguities, gaps
- Informal, fast

**2. Walkthrough**
- Author presents requirements to team
- Team asks questions, provides feedback
- Semi-formal

**3. Inspection (Fagan Inspection)**
- Formal, structured review
- Defined roles (moderator, reader, recorder, reviewers)
- Checklist-driven
- Most thorough, most time-intensive

### Effective Requirements Review Process

**Before the review:**
- Distribute requirements document 2-3 days in advance
- Provide review checklist
- Ask reviewers to note questions/issues

**During the review:**
- Walk through requirements section by section
- Focus on defects, not solutions
- Record all issues (don't debate solutions during review)
- Timebox (max 2 hours per session)

**Review Checklist:**
- [ ] Is each requirement clear and unambiguous?
- [ ] Is each requirement testable?
- [ ] Are there contradictions?
- [ ] Are there gaps?
- [ ] Is terminology consistent?
- [ ] Are priorities realistic?
- [ ] Is rationale provided?
- [ ] Are acceptance criteria defined?

**After the review:**
- Prioritize issues found
- Author addresses issues
- Re-review if major changes

### Example Review Finding

**Requirement:** REQ-155: "System shall provide notifications"

**Issues identified:**
1. What notifications? (Ambiguous)
2. To whom? (Missing recipient)
3. When? (Missing trigger)
4. How delivered? (Email? SMS? In-app?) (Missing channel)
5. What format? (Missing content specification)
6. How to test? (Not testable as written)

**Revised:**
REQ-155: "System shall send SMS notification to driver 2 hours before scheduled appointment time, containing appointment time, bay number, and required documents."

**Now:**
- Clear (SMS, to driver, 2 hours before, specific content)
- Testable (Create appointment, wait 2 hours before time, verify SMS received with correct content)

---

## Core Concept 6: Acceptance Criteria and Testability

### Writing Testable Acceptance Criteria

**Format: Given-When-Then**

```
GIVEN [initial context/state],
WHEN [action occurs],
THEN [expected outcome].
```

**Example 1:**

**Requirement:** FR-089: System shall prevent double-booking of loading bays

**Acceptance Criteria:**
- GIVEN bay 3 has appointment at 10:00 AM, WHEN scheduler attempts to book bay 3 at 10:00 AM for different truck, THEN system displays error message and prevents booking
- GIVEN bay 3 has appointment at 10:00 AM, WHEN scheduler books bay 3 at 10:30 AM (different time), THEN system allows booking
- GIVEN bay 3 has appointment at 10:00 AM, WHEN that appointment is cancelled, WHEN scheduler books bay 3 at 10:00 AM, THEN system allows booking

**Example 2:**

**Requirement:** NFR-050: System shall respond quickly to user actions

**Problem:** "Quickly" is not testable

**Revised Requirement:** NFR-050: System shall respond to user actions within defined time limits

**Acceptance Criteria:**
- GIVEN user searches for product, WHEN search submitted, THEN results displayed within 500ms for 95% of queries
- GIVEN user clicks "Save" on form, WHEN save action triggered, THEN confirmation message appears within 2 seconds
- GIVEN system under load (1000 concurrent users), WHEN user performs any action, THEN response time < 3 seconds for 90% of requests

### Linking Requirements to Test Cases

**One requirement → Multiple test cases**

**Requirement:** FR-090: System shall validate driver credentials before allowing loading

**Test Cases:**
- TC-090-001: Valid CDL and TWIC → Loading allowed
- TC-090-002: Invalid CDL → Loading blocked, error shown
- TC-090-003: Valid CDL but expired TWIC → Loading blocked
- TC-090-004: Valid credentials but different driver than appointment → Alert operator
- TC-090-005: Credentials expired yesterday → Blocked
- TC-090-006: Credentials expire today → Allowed (still valid)
- TC-090-007: No credentials provided → Blocked

**Coverage:** 1 requirement → 7 test cases (happy path + edge cases)

---

## Practical Framework: Requirements Validation & Verification Workflow

### The 7-Step V&V Process

**Step 1: Completeness Check**
- Use completeness checklist
- Create traceability matrix
- Identify gaps

**Step 2: Consistency Check**
- Cross-reference all requirements
- Check state machines match
- Verify terminology

**Step 3: Quality Check**
- Apply SMART criteria to each requirement
- Apply 8-point quality checklist
- Flag defects

**Step 4: Stakeholder Review**
- Walk through requirements with stakeholders
- Validate correctness (are we building the right thing?)
- Get sign-off

**Step 5: Technical Review**
- Architects verify feasibility
- Identify technical risks
- Estimate complexity

**Step 6: Requirements Inspection**
- Formal review session
- Use checklists
- Document defects

**Step 7: Acceptance Criteria Definition**
- Write Given-When-Then for each requirement
- Ensure testability
- Link to test cases

---

## PROJECT 3: Comprehensive Requirements Document

**This is your capstone for Course 3 (Requirements Engineering & Domain Understanding).**

You'll produce a complete, validated, verified Requirements Document demonstrating mastery of Weeks 9-13.

### Project Requirements

**Choose a real system from your domain:**
- Terminal operations system
- Supply chain/logistics system
- Healthcare system
- Financial services system
- Must be complex enough to warrant thorough requirements (not trivial)

### Deliverables (15-20 pages total)

#### 1. Executive Summary (1 page)

- System overview (what is being built)
- Business context (why)
- Scope (what's in, what's out)
- Key stakeholders
- Success criteria

#### 2. Business Context and Goals (1-2 pages)

- Business requirements (3-5 strategic goals from Week 9)
- Each goal with measurable success metrics
- Stakeholder analysis (primary, secondary, external - from Week 10)

#### 3. Functional Requirements (3-5 pages)

**Organized by feature area, including:**
- At least 20 functional requirements
- Each with format: REQ-ID, statement (shall/should), rationale, priority, acceptance criteria
- Cover all CRUD operations for key entities
- Reference business rules where applicable

**Example:**
```
FR-045: Schedule Truck Appointment
Statement: The system shall allow schedulers to create appointments by specifying truck ID, product type, quantity, date/time, and bay assignment.
Rationale: Supports business goal BG-001 (reduce conflicts). Requested by terminal operations manager.
Priority: Must Have
Acceptance Criteria:
  - GIVEN scheduler has valid credentials, WHEN scheduler enters appointment details, THEN system validates bay availability and creates appointment
  - GIVEN bay is already booked, WHEN scheduler attempts same time/bay, THEN system displays conflict error
Source: Interview with scheduler (Week 10 elicitation)
Dependencies: REQ-030 (bay availability), REQ-040 (product compatibility)
Status: Approved
```

#### 4. Non-Functional Requirements (2-3 pages)

**At least 10 NFRs covering:**
- Performance (response time, throughput)
- Scalability (users, data volume)
- Availability/Reliability
- Security (authentication, authorization, encryption, audit)
- Usability (learnability, efficiency)
- Maintainability
- Compliance/Regulatory

**Each with specific, measurable criteria**

#### 5. Business Rules (1-2 pages)

**At least 10 business rules:**
- Use format from Week 9
- Rule ID, type, source, justification, statement, examples, exceptions

**Example:**
```
BR-030: Bay-Product Compatibility
Type: Constraint
Source: Terminal operations procedures
Justification: Safety and product quality (prevent contamination)
Statement: Loading bays can only handle specific product types based on physical configuration and cleaning requirements. Bays 1-2: Diesel only. Bays 3-4: All products. Bay 5: Gasoline only.
Examples:
  - Diesel appointment → Can use Bay 1, 2, 3, or 4
  - Gasoline appointment → Can only use Bay 5
  - Attempt to assign diesel to Bay 5 → Validation error
Exceptions: None (safety-critical rule)
Related Requirements: FR-045, FR-050
```

#### 6. State Machines (2-3 pages)

**Model at least 3 entity lifecycles (Week 11):**
- State diagram (visual)
- State catalog (table with entry/exit actions, valid transitions)
- Event catalog (triggers, guards, side effects)
- State invariants
- Temporal requirements

**Entities could include:**
- Order, Appointment, Loading Operation, Equipment, Request, Application

#### 7. Use Cases (3-5 pages)

**At least 5 detailed use cases (Week 12):**
- Actor, goal, preconditions, postconditions
- Main success scenario (10-20 steps)
- Alternative flows (3-5 per use case)
- Exception scenarios
- Edge cases identified

**At least one use case must include an abuse case** with mitigations.

#### 8. Data Requirements (1-2 pages)

**Overview of key entities and attributes:**
- Main entities (5-10)
- Key attributes per entity
- Relationships
- Constraints

**Not a full database design, but enough to understand data model.**

#### 9. Assumptions and Constraints (1 page)

**Assumptions:**
- What are you assuming to be true?
- What external dependencies?

**Constraints:**
- Technical constraints
- Budget/timeline constraints
- Regulatory constraints
- Organizational constraints

#### 10. Acceptance Criteria (1-2 pages)

**System-level acceptance criteria:**
- What must be true for the system to be "done"?
- How will stakeholders know it works?

**Link to requirements:**
- Reference key functional requirements
- Reference key NFRs

#### 11. Traceability Matrix (1 page)

**Show traceability from business goals → user requirements → functional requirements → use cases → test cases**

| Business Goal | User Requirement | Functional Requirements | Use Cases | Test Cases (count) |
|---------------|------------------|-------------------------|-----------|-------------------|
| BG-001: Reduce conflicts | UR-012: Prevent double-booking | FR-045, FR-050, FR-089 | UC-045 | 15 |
| ... | ... | ... | ... | ... |

#### 12. Validation & Verification Summary (1-2 pages)

**Document your V&V process:**
- How did you validate with stakeholders?
- What reviews were conducted?
- What defects were found and resolved?
- Completeness check results
- Consistency check results
- Quality metrics (% requirements with acceptance criteria, % testable, % traced)

### Quality Standards

**This is a portfolio piece. Demonstrate:**
- ✅ Completeness (all requirement categories covered)
- ✅ Consistency (no contradictions, terminology standardized)
- ✅ Clarity (every requirement understandable)
- ✅ Testability (all requirements have acceptance criteria)
- ✅ Traceability (every requirement traces to business need)
- ✅ Professional quality (well-organized, formatted, spell-checked)

### Evaluation Criteria

**Your project demonstrates:**
- ✅ Mastery of requirements engineering (Week 9)
- ✅ Effective elicitation (Week 10 techniques applied)
- ✅ State machine modeling (Week 11)
- ✅ Complete scenario documentation (Week 12)
- ✅ Validation & verification rigor (Week 13)
- ✅ Portfolio-ready quality

---

## Reflection Questions

1. **Past requirement defects:** Think of bugs caused by bad requirements. Were they incomplete, inconsistent, ambiguous, or not testable?

2. **Review value:** Have you participated in requirements reviews? What defects were found that would have become bugs?

3. **Testability:** Pick 5 requirements from a project you know. Can you write acceptance criteria for each? If not, how would you fix them?

4. **Traceability:** Can you trace a feature you built back to a business goal? If not, why was it built?

5. **Validation vs verification:** Have you built the wrong thing right? Or the right thing wrong? What could have prevented it?

---

## Key Takeaways

1. **Validation (right thing) ≠ Verification (thing right):** Both are essential
2. **Complete means ALL categories covered:** Functional, non-functional, rules, interfaces, data, states, scenarios
3. **Consistent means no contradictions:** Cross-reference everything, standardize terminology
4. **Testable means acceptance criteria:** If you can't test it, it's not a requirement
5. **Traceable means justified:** Every requirement should answer "why does this exist?"
6. **Reviews find defects early:** Cheaper to fix in requirements than in code
7. **PROJECT 3 synthesizes everything:** Weeks 9-13 come together in comprehensive document

**Course 3 Complete!** You now master requirements engineering:
- Identify requirements types (Week 9)
- Elicit from multiple sources (Week 10)
- Model state and lifecycle (Week 11)
- Document scenarios completely (Week 12)
- Validate and verify quality (Week 13)

**Next:** Course 4 begins — **Domain-Driven Design (DDD)** (Weeks 14-18)

---

## Additional Resources

**Books:**
- *Software Requirements* by Wiegers & Beatty (Chapter 17-18: Validation/Verification)
- *Requirements Engineering Fundamentals* by Pohl & Rupp
- *IEEE 830-1998* (Recommended Practice for SRS)

**Templates:**
- Requirements review checklist
- Traceability matrix template
- Requirements verification matrix
- Acceptance criteria template

**Tools:**
- Requirements management: Jira, Azure DevOps, IBM DOORS
- Traceability: ReqView, Visure Requirements
- Review: Collaborator, Review Board

**Practice:**
- Review 10 requirements from your current project using quality checklist
- Create traceability matrix for one feature
- Write acceptance criteria for 20 requirements
- Conduct requirements review session with team

---

*The best requirements are validated (right thing) and verified (complete, consistent, testable). Master this, and you'll prevent the majority of project failures.*

**End of Week 13 — End of Course 3: Requirements Engineering & Domain Understanding**

---

## 🎓 Course 3 Complete

You've mastered requirements engineering and domain understanding. You can:
- ✅ Distinguish requirements, specifications, and implementation (Week 9)
- ✅ Elicit requirements using multiple techniques (Week 10)
- ✅ Model entity lifecycles with state machines (Week 11)
- ✅ Document complete scenarios with edge cases (Week 12)
- ✅ Validate and verify requirements quality (Week 13)

**PROJECT 3** is your capstone. Spend 15-25 hours. Combine everything from Weeks 9-13. Make it portfolio-quality showing requirements engineering mastery.

**Completed Courses:**
- ✅ Course 1: Systems Thinking & Complex Domains (Weeks 1-4)
- ✅ Course 2: Product Discovery & Problem Identification (Weeks 5-8)
- ✅ Course 3: Requirements Engineering & Domain Understanding (Weeks 9-13)

**When ready, proceed to Course 4: Domain-Driven Design (DDD) (Weeks 14-18)**
