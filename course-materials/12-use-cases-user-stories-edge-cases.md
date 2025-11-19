# Week 12: Use Cases, User Stories & Edge Cases
## Documenting Complete Scenarios Including All the Ways Things Can Go Wrong

---

## Introduction: The Feature That Worked Perfectly (Until It Didn't)

A development team built a payment processing feature. They tested it thoroughly:
- ✅ Valid credit card → Payment succeeds
- ✅ Invalid card number → Error message shown
- ✅ Insufficient funds → Decline message displayed

**Shipped to production. Worked great for 2 weeks.**

**Then:**

**Customer:** Tries to pay $15,000 invoice with corporate card
**System:** Processes payment successfully
**Payment Processor:** Holds transaction for fraud review (large amount)
**24 hours later:** Payment declined by processor
**System:** Still shows "Payment Successful" (never checked for delayed decline)
**Accounting:** Records payment received
**Customer:** Never receives product (payment actually failed)
**Finance:** Books revenue incorrectly

**What went wrong?**

The team tested the **happy path** (success) and a few **obvious error cases** (invalid card, insufficient funds).

**What they missed:**
- Delayed payment responses (fraud review)
- Partial authorizations (only $5,000 of $15,000 approved)
- Network timeouts during payment
- Payment processor temporary outage
- Duplicate payment prevention
- Refund scenarios
- Currency conversion edge cases
- Tax calculation for international orders
- Payment method expiring mid-transaction

**They documented the main use case. They didn't document the 20+ alternative flows and edge cases.**

**This week, you'll learn to identify and document the complete picture** — all the ways scenarios can succeed, fail, branch, or behave unexpectedly.

---

## Core Concept 1: Use Cases (Detailed Format)

### What Is a Use Case?

**Use Case:** A detailed description of how an actor interacts with the system to achieve a specific goal, including all variations and exceptions.

**Purpose:**
- Capture functional requirements in scenario form
- Show step-by-step interactions
- Document all alternative flows (not just happy path)
- Provide testable specifications

### Use Case Template

```
USE CASE: [UC-ID] [Use Case Name]

Actor: [Primary actor who initiates]
Goal: [What the actor wants to accomplish]
Preconditions: [What must be true before starting]
Success Guarantee (Postconditions): [What's true after successful completion]

MAIN SUCCESS SCENARIO:
1. Actor does [action]
2. System does [response]
3. Actor does [action]
4. System validates [data]
5. System [performs business logic]
6. System confirms [result]

EXTENSIONS (Alternative Flows):
3a. [Condition]: [Alternative sequence]
    3a1. System [does this instead]
    3a2. Resume at step 4
4a. Validation fails:
    4a1. System displays error message
    4a2. Resume at step 3

RELATED INFORMATION:
- Frequency: [how often]
- Business Rules: [BR-ID references]
- Related Use Cases: [UC-ID references]
- Open Issues: [questions]
```

### Example: Process Terminal Truck Loading

```
USE CASE: UC-045 Process Terminal Truck Loading

Actor: Terminal Operator
Goal: Load product onto truck and issue bill of lading
Preconditions:
  - Truck has valid appointment
  - Driver has presented credentials (CDL, TWIC)
  - Product is available in assigned tank

Success Guarantee (Postconditions):
  - Product loaded onto truck
  - Meter readings recorded
  - Bill of lading issued
  - Inventory reduced by loaded quantity
  - Loading operation marked complete

MAIN SUCCESS SCENARIO:
1. Operator scans driver badge
2. System retrieves appointment details and displays assigned bay, product, quantity
3. Operator verifies truck placards match product type
4. System confirms tank has sufficient product
5. Operator connects loading arm to truck
6. Operator records opening meter reading
7. System validates meter reading is greater than previous closing reading
8. Operator starts pump
9. System monitors flow rate and total volume
10. System stops pump when ordered quantity reached
11. Operator records closing meter reading
12. System calculates volume loaded (closing - opening)
13. System adjusts for temperature/pressure (standard conditions)
14. System reduces tank inventory by loaded volume
15. System generates bill of lading with all details
16. Operator disconnects loading arm
17. Operator issues BOL to driver
18. System marks loading operation as complete

EXTENSIONS (Alternative Flows):

1a. Driver badge not recognized:
    1a1. Operator manually enters driver ID
    1a2. Resume at step 2

2a. No appointment found for driver:
    2a1. System alerts operator
    2a2. Operator contacts scheduler
    2a3. If emergency approval granted:
        2a3a. Scheduler creates ad-hoc appointment
        2a3b. Resume at step 2
    2a4. Else: Driver turned away

3a. Placards do not match product:
    3a1. System alerts operator (safety violation)
    3a2. Operator refuses loading
    3a3. Loading operation aborted

4a. Tank does not have sufficient product:
    4a1. System calculates available quantity
    4a2. Operator offers partial load to driver
    4a3. If driver accepts:
        4a3a. Operator adjusts ordered quantity
        4a3b. Resume at step 5
    4a4. Else: Loading cancelled, customer notified

6a. Meter reading invalid (negative or unreasonable):
    6a1. System rejects reading
    6a2. Operator re-enters reading
    6a3. If still invalid after 3 attempts:
        6a3a. Operator calls maintenance
        6a3b. Loading suspended pending meter repair

9a. Flow rate exceeds safe limits:
    9a1. System automatically reduces pump speed
    9a2. Operator notified
    9a3. Continue monitoring

9b. Flow stops unexpectedly:
    9b1. System alerts operator
    9b2. Operator investigates (blockage, pump failure, emergency stop)
    9b3. If resolved: Resume pumping
    9b4. Else: Loading aborted, partial volume recorded

12a. Meter readings show significant discrepancy (> 0.5%):
    12a1. System flags for investigation
    12a2. Operator performs hand measurement
    12a3. If hand measurement confirms meter:
        12a3a. System uses meter reading
    12a4. Else:
        12a4a. Operator calls maintenance
        12a4b. Loading held pending resolution

RELATED INFORMATION:
- Frequency: 20-50 times per day per operator
- Business Rules: BR-030 (bay compatibility), BR-051 (flow rate limits)
- Related Use Cases: UC-044 (Schedule Appointment), UC-046 (Handle Emergency Stop)
- Open Issues: How long to retain meter malfunction records?
```

**Notice:**
- Main scenario is 18 steps (detailed)
- 7 extension points (labeled 1a, 2a, 3a, etc.)
- Each extension has its own numbered sub-steps
- Extensions either resume main flow or abort
- Preconditions and postconditions are explicit

---

## Core Concept 2: User Stories (Agile Format)

### User Story Structure

```
As a [role/persona],
I want to [action/capability],
So that [business value/outcome].

ACCEPTANCE CRITERIA:
- Given [context], When [action], Then [outcome]
- Given [context], When [action], Then [outcome]
- Given [context], When [action], Then [outcome]
```

### Good vs Bad User Stories

**Bad:**
> "As a user, I want a better dashboard."

**Problems:**
- "User" is too vague (which user?)
- "Better" is not specific
- No "so that" (why?)
- No acceptance criteria

**Good:**
> "As a terminal operations manager, I want to see real-time bay utilization metrics on my dashboard, so that I can identify bottlenecks and optimize scheduling."

**ACCEPTANCE CRITERIA:**
- Given I'm viewing the dashboard, When a loading operation starts, Then the bay status updates to "In Use" within 5 seconds
- Given multiple bays are active, When I view the dashboard, Then I see % utilization for each bay over the last hour
- Given a bay is unused for 30+ minutes during business hours, When viewing dashboard, Then that bay is highlighted as potentially available for reassignment

### INVEST Criteria for User Stories

**Independent:** Can be developed in any order
**Negotiable:** Details can be discussed
**Valuable:** Delivers user value
**Estimable:** Team can estimate effort
**Small:** Fits in one sprint
**Testable:** Clear pass/fail criteria

### User Story vs Use Case

| User Story | Use Case |
|------------|----------|
| Brief (1-2 sentences) | Detailed (1-3 pages) |
| Focuses on user value | Focuses on system behavior |
| Conversation starter | Complete specification |
| Agile methodology | Traditional requirements |
| Acceptance criteria (Given/When/Then) | Main scenario + extensions |

**Use both:**
- User stories for backlog and planning
- Use cases for detailed analysis and testing

---

## Core Concept 3: Identifying Edge Cases Systematically

### What Are Edge Cases?

**Edge Case:** A scenario that occurs at an extreme operating parameter, boundary condition, or unusual combination of inputs.

**Why they matter:**
- Rare but impactful
- Often not thought of during design
- Cause production bugs
- Reveal unstated requirements

### Categories of Edge Cases

**1. Boundary Values**
- Zero, one, maximum
- Empty collections
- Null values

**Examples:**
- Order with 0 items
- Cart with 1,000,000 items (max capacity?)
- Customer with empty email field
- Product priced at $0.00

**2. Timing and Concurrency**
- Simultaneous requests
- Race conditions
- Timeouts
- Delays

**Examples:**
- Two users booking last available appointment at exact same time
- Payment processed while order being cancelled
- Session expires mid-transaction
- Response delayed beyond timeout threshold

**3. State-Related**
- Invalid state transitions
- Operations in wrong state
- State corruption

**Examples:**
- Trying to ship an already-shipped order
- Cancelling a delivered order
- Paying for a cancelled order

**4. Data Extremes**
- Very large numbers
- Very small numbers
- Special characters
- Unicode edge cases

**Examples:**
- Customer name with 500 characters
- Product name containing only emojis
- Address with special characters (apostrophes, hyphens)
- Negative quantities (should be prevented)

**5. External System Failures**
- Third-party API down
- Network timeout
- Partial responses
- Malformed data

**Examples:**
- Payment processor returns 500 error
- Shipping API times out
- Credit bureau returns incomplete data
- Currency conversion service unavailable

**6. User Behavior**
- Unexpected sequences
- Repeated actions
- Abandonment
- Back button

**Examples:**
- User submits form twice (double-click)
- User hits "Back" after payment processed
- User abandons checkout, returns 3 days later
- User edits order after payment but before shipping

**7. Security and Abuse**
- Injection attacks
- Privilege escalation
- Data manipulation

**Examples:**
- SQL injection in search field
- User modifying own permissions
- Changing order total in HTTP request
- Accessing another user's data by changing ID in URL

---

## Core Concept 4: The Edge Case Discovery Checklist

### Systematic Questions to Ask

**For every input field:**
- What if it's empty?
- What if it's null?
- What if it's maximum length?
- What if it contains special characters?
- What if it's the wrong data type?

**For every numeric field:**
- What if it's zero?
- What if it's negative?
- What if it's MAX_INT?
- What if it's a decimal when integer expected?

**For every list/collection:**
- What if it's empty?
- What if it has one item?
- What if it has millions of items?
- What if it contains duplicates?

**For every operation:**
- What if it's called twice in a row?
- What if it's called while already running?
- What if prerequisite steps were skipped?
- What if it times out?

**For every external call:**
- What if the service is down?
- What if it returns an error?
- What if it returns partial data?
- What if it never responds?

**For every state transition:**
- What if the entity is in the wrong state?
- What if state was changed by someone else meanwhile?
- What if transition validation fails?

---

## Core Concept 5: Abuse Cases (Security Perspective)

### What Are Abuse Cases?

**Abuse Case:** A scenario where a malicious actor intentionally misuses the system.

**Purpose:**
- Identify security vulnerabilities
- Design defensive measures
- Prevent exploitation

### Abuse Case Format

```
ABUSE CASE: [AC-ID] [Abuse Scenario Name]

Threat Actor: [Who would do this]
Motivation: [Why they would do it]
Attack Scenario: [How they would do it]
Impact: [What damage could be done]
Likelihood: [High/Medium/Low]

MITIGATION:
- Preventive Controls: [Stop the attack]
- Detective Controls: [Detect attack in progress]
- Recovery Controls: [Recover after attack]
```

### Example: Price Manipulation Abuse Case

```
ABUSE CASE: AC-012 Price Manipulation in Shopping Cart

Threat Actor: Malicious customer
Motivation: Get products for less than actual price
Attack Scenario:
1. Add product to cart (price $100)
2. Intercept HTTP request using browser dev tools
3. Modify price parameter in request from $100 to $1
4. Submit modified request
5. Proceed to checkout with manipulated price
6. Complete payment of $1 instead of $100

Impact:
- Revenue loss
- Inventory sold at incorrect prices
- Financial reporting errors

Likelihood: Medium (requires technical knowledge but tools readily available)

MITIGATION:

Preventive Controls:
- Never trust client-side price data
- Calculate price server-side based on product ID
- Use signed/encrypted parameters
- Validate price against product catalog before processing

Detective Controls:
- Monitor for orders where price < 50% of catalog price
- Alert on unusual price fluctuations in orders
- Log all price-related parameters for audit

Recovery Controls:
- Flag suspicious orders for manual review before shipping
- Cancel and refund if fraud detected
- Ban user accounts engaged in price manipulation
```

### Common Abuse Case Categories

**1. Authentication Bypass**
- Brute force password guessing
- Session hijacking
- Credential stuffing

**2. Authorization Escalation**
- Accessing other users' data
- Elevating own permissions
- Bypassing role checks

**3. Data Injection**
- SQL injection
- Cross-site scripting (XSS)
- Command injection

**4. Business Logic Abuse**
- Exploiting promotional codes
- Creating fake accounts for benefits
- Manipulating loyalty points

**5. Denial of Service**
- Resource exhaustion
- Infinite loops
- Malformed inputs causing crashes

---

## Core Concept 6: Alternative Flows vs Exceptions vs Errors

### Terminology Clarification

**Main Success Scenario:**
- The "happy path"
- Everything goes right
- Most common path

**Alternative Flow:**
- Valid variation of the main scenario
- Still achieves the goal (or a modified goal)
- Business-acceptable path

**Exception:**
- Something goes wrong
- Goal may not be achieved
- Requires recovery or workaround

**Error:**
- Technical failure
- System malfunction
- Often outside user control

### Examples

**Use Case: Place Order**

**Main Success Scenario:**
1. Customer adds items to cart
2. Customer proceeds to checkout
3. Customer enters shipping address
4. Customer selects standard shipping
5. Customer enters payment information
6. System processes payment successfully
7. System confirms order

**Alternative Flow 3a: Customer has saved addresses**
3a1. System displays saved addresses
3a2. Customer selects one
3a3. Resume at step 4

**Alternative Flow 5a: Customer uses gift card**
5a1. Customer enters gift card code
5a2. System validates gift card
5a3. System applies gift card balance
5a4. If balance insufficient:
    5a4a. Customer enters additional payment method
5a5. Resume at step 6

**Exception 6a: Payment declined**
6a1. System receives decline from payment processor
6a2. System displays decline reason to customer
6a3. Customer updates payment method
6a4. Resume at step 6 (retry payment)

**Error 6b: Payment processor unavailable**
6b1. System cannot reach payment processor (timeout)
6b2. System displays error message
6b3. System saves cart for later
6b4. System sends email when service restored
6b5. Use case fails gracefully

---

## Practical Framework: Complete Scenario Documentation

### The Three-Level Approach

**Level 1: User Story (Planning)**
```
As a customer,
I want to place an order,
So that I can purchase products.
```

**Level 2: Use Case (Design)**
- Main success scenario (10-20 steps)
- 5-10 alternative flows
- 3-5 exception scenarios

**Level 3: Test Cases (Implementation)**
- 1 test for main scenario
- 1 test per alternative flow
- 1 test per exception
- 10-20 edge case tests

### Traceability

```
USER STORY US-042: Customer places order
  └─ USE CASE UC-089: Process Customer Order
      ├─ Main Scenario → TEST TC-089-001
      ├─ Alt Flow 3a (saved address) → TEST TC-089-002
      ├─ Alt Flow 5a (gift card) → TEST TC-089-003
      ├─ Exception 6a (payment declined) → TEST TC-089-004
      ├─ Exception 6b (processor down) → TEST TC-089-005
      └─ Edge Cases:
          ├─ Empty cart → TEST TC-089-101
          ├─ 1000 items in cart → TEST TC-089-102
          ├─ Concurrent checkouts → TEST TC-089-103
          └─ (10 more edge case tests)
```

---

## Hands-On Exercise: Document Complete Scenarios

### Deliverable: Comprehensive Use Case with All Variations

**Choose ONE complex process from your domain:**
- Patient admission (healthcare)
- Loan application (finance)
- Equipment checkout (logistics/operations)
- Terminal truck scheduling (your domain)
- Purchase order approval (procurement)

Produce:

#### 1. User Story (1 paragraph)

Using proper format:
```
As a [role],
I want to [action],
So that [value].

ACCEPTANCE CRITERIA:
- Given [context], When [action], Then [outcome]
- (3-5 acceptance criteria)
```

#### 2. Detailed Use Case (2-4 pages)

Using template from this week:
- Actor and Goal
- Preconditions and Postconditions
- Main Success Scenario (12-25 steps)
- At least 5 alternative flows (extensions)
- At least 3 exception scenarios
- Related information (frequency, business rules)

#### 3. Edge Case Analysis (1-2 pages)

Identify edge cases in these categories:
- Boundary values (at least 3)
- Timing/concurrency (at least 2)
- State-related (at least 2)
- Data extremes (at least 3)
- External system failures (at least 2)

For each edge case:
```
EDGE CASE: [Description]
Scenario: [What happens]
Current Handling: [How system should respond]
Test: [How to verify]
```

#### 4. Abuse Case (1 page)

Identify at least one abuse case:
- Threat actor and motivation
- Attack scenario
- Impact and likelihood
- Mitigation controls (preventive, detective, recovery)

#### 5. Test Case Mapping (1 page)

Create table showing traceability:

| Test ID | Type | Covers | Expected Result |
|---------|------|--------|-----------------|
| TC-001 | Happy Path | Main scenario | Order placed successfully |
| TC-002 | Alt Flow | Saved address | Address auto-filled |
| TC-003 | Exception | Payment declined | Error shown, retry enabled |
| TC-101 | Edge Case | Empty cart | Cannot proceed to checkout |
| ... | ... | ... | ... |

---

## Real-World Example: Terminal Truck Loading (Expanded)

### User Story

```
As a terminal operator,
I want to safely load product onto customer trucks,
So that customers receive the correct product quantity while maintaining safety and regulatory compliance.

ACCEPTANCE CRITERIA:
- Given a truck with valid appointment, When operator initiates loading, Then system displays correct product and quantity
- Given loading is in progress, When flow rate exceeds limits, Then system automatically adjusts pump speed
- Given loading is complete, When operator records meter readings, Then system generates accurate bill of lading
- Given a safety issue occurs, When operator hits emergency stop, Then system immediately halts loading and records incident
```

### Use Case (Extended from earlier)

Already covered in Core Concept 1, including:
- 18-step main scenario
- 7 alternative flows
- Preconditions and postconditions

### Edge Cases

**Boundary Values:**

```
EDGE CASE: Zero quantity requested
Scenario: Appointment created with quantity = 0
Current Handling:
- System validation should prevent appointment creation with quantity <= 0
- If somehow created, operator should be alerted before starting load
Test: Attempt to create appointment with 0 barrels, expect validation error
```

```
EDGE CASE: Quantity exceeds tank capacity
Scenario: Requested 50,000 barrels but tank only has 30,000
Current Handling:
- System checks availability before starting load (step 4)
- Offers partial load option (extension 4a)
Test: Request quantity > available, expect partial load offered
```

**Timing/Concurrency:**

```
EDGE CASE: Meter reading taken during flow
Scenario: Operator records closing reading while product still flowing
Current Handling:
- System should detect pump is still running
- Prevent closing reading until pump stopped
Test: Attempt to record closing reading while pump active, expect error
```

**State-Related:**

```
EDGE CASE: Attempt to load on cancelled appointment
Scenario: Appointment cancelled but operator tries to proceed
Current Handling:
- System checks appointment status at step 2
- Alerts operator if cancelled
- Prevents loading from proceeding
Test: Cancel appointment, attempt to start loading, expect rejection
```

**Data Extremes:**

```
EDGE CASE: Meter reading overflow
Scenario: Meter resets to zero after 999,999.9 barrels
Current Handling:
- System should handle meter rollover in volume calculation
- If closing < opening, add meter max value
Test: Simulate rollover scenario, expect correct volume calculated
```

**External System Failures:**

```
EDGE CASE: Tank level sensor unavailable
Scenario: SCADA system not responding when checking tank levels
Current Handling:
- System attempts 3 retries with exponential backoff
- If still unavailable, alerts operator
- Operator can manually verify tank levels and override
- Manual override logged for audit
Test: Simulate SCADA timeout, expect retry logic and manual override option
```

### Abuse Case

```
ABUSE CASE: AC-024 Volume Theft via Meter Manipulation

Threat Actor: Dishonest driver or operator
Motivation: Steal product (load more than paid for)
Attack Scenario:
1. Legitimate appointment for 10,000 barrels
2. During loading, collude to under-report closing meter reading
3. Report closing reading 2,000 barrels less than actual
4. Bill of lading shows 10,000 barrels
5. Truck actually loaded with 12,000 barrels
6. 2,000 barrels stolen (worth ~$100,000)

Impact:
- Financial loss (product stolen)
- Inventory discrepancies
- Customer receives more than paid for (fraud)

Likelihood: Low (requires collusion, physically risky) but high impact

MITIGATION:

Preventive Controls:
- Dual verification: Second operator confirms final meter readings
- Flow meter totalizer cross-check (independent measurement)
- Tank level before/after should match loaded volume (± tolerance)
- Video recording of meter readings
- Seal loading arm after closing reading

Detective Controls:
- Automated alerts when tank inventory delta > meter readings + 1% tolerance
- Random audit of video footage vs. recorded readings
- Pattern detection: Same driver/operator showing consistent discrepancies

Recovery Controls:
- Investigation protocol for discrepancies
- Disciplinary action for employees
- Customer billing adjustment if overage detected
- Law enforcement involvement for theft
```

---

## Reflection Questions

1. **Happy path bias:** Think of a feature you built. Did you only test the main scenario? What edge cases did you miss?

2. **Production surprises:** What edge case caused a production bug in your experience? Could it have been identified upfront?

3. **Alternative flows:** For a process you know well, how many alternative flows exist beyond the main path? Did requirements document them all?

4. **Security thinking:** Pick a feature in your domain. What's one way someone could abuse it maliciously?

5. **Coverage:** If you wrote only tests for the main scenario, what % of real-world situations would be covered? 10%? 30%? 50%?

---

## Key Takeaways

1. **Use cases capture complete scenarios:** Main scenario + alternatives + exceptions + edge cases
2. **User stories are conversation starters:** Brief format for planning, expand into use cases for design
3. **Alternative flows are normal:** Valid variations, not errors
4. **Edge cases are requirements:** Rare but important scenarios must be specified
5. **Abuse cases reveal security needs:** Think like an attacker to design defenses
6. **Happy path is not enough:** Most bugs live in alternative flows and edge cases
7. **Systematic discovery prevents surprises:** Use checklists to find edge cases before production

**Next week:** You'll learn **requirements validation & verification** — techniques for ensuring requirements are complete, consistent, and testable, culminating in **PROJECT 3**.

---

## Additional Resources

**Books:**
- *Writing Effective Use Cases* by Alistair Cockburn (definitive guide)
- *User Stories Applied* by Mike Cohn (agile requirements)
- *The Art of Software Security Assessment* by Dowd et al. (abuse cases)

**Templates:**
- Use case template (IEEE/RUP format)
- User story template with acceptance criteria
- Edge case discovery checklist
- Abuse case template

**Tools:**
- UseCase Maker, Visual Paradigm (use case diagrams)
- Jira, Azure DevOps (user story management)
- BDD frameworks (Cucumber, SpecFlow) for Given/When/Then

**Practice:**
- Document 5 use cases for systems you use daily
- Identify 10 edge cases for each
- Write abuse cases for sensitive operations (login, payment, admin functions)
- Review production bugs — how many were edge cases not documented?

---

*The difference between amateur and professional requirements: Amateurs document the happy path. Professionals document every way things can go wrong — and right.*

**End of Week 12**
