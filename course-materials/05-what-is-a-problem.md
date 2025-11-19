# Week 5: What a "Problem" Actually Is
## Beyond Feature Requests: Understanding the Jobs People Hire Solutions to Do

---

## Introduction: The $2 Million Feature Nobody Used

A software company spent 18 months building a sophisticated analytics dashboard. Beautiful visualizations. Real-time data. Configurable widgets. The works.

**Cost:** $2 million in engineering time.

**Usage after 6 months:** 3% of customers accessed it more than once.

**What went wrong?**

The product manager asked customers: "What features do you want?" Customers said: "We want better analytics and reporting."

So they built it. But here's what the PM didn't ask:

- **What job are you trying to do?**
- **What outcome are you seeking?**
- **Why do you need analytics?**
- **What decision are you trying to make?**
- **What happens when you can't make that decision?**
- **How do you solve this today?**
- **What's inadequate about the current solution?**

It turned out: Customers didn't actually want analytics. They wanted to **know which customers were at risk of churning so they could intervene**.

The analytics dashboard showed lots of data. But it didn't answer the question: **"Who should I call today?"**

A simple daily email with a list of at-risk customers would have solved the real problem in 2 weeks for $20,000.

**This week you'll learn to identify real problems, not just collect feature requests.**

---

## COURSE 2: Product Discovery & Problem Identification

**Course Goal:** Learn to identify valuable problems worth solving through systematic discovery, research, and validation — before writing code.

**Why this matters for software engineers:**
- Most failed projects solve the wrong problem
- Understanding problems deeply = better architecture
- Problem identification skills differentiate great engineers from code monkeys
- Companies pay for problem-solving, not code-writing

**Course structure:**
- **Week 5:** What a problem actually is (this week)
- **Week 6:** Interviews, research & observation
- **Week 7:** Validating problems without code
- **Week 8:** Opportunity docs and problem briefs (PROJECT 2)

---

## Core Concept 1: Problem vs Symptom vs Root Cause

### The Three Levels

**1. Symptom:** What you observe (surface level)

**2. Problem:** The underlying obstacle preventing desired outcome

**3. Root Cause:** The fundamental reason the problem exists

### Example: Hospital Emergency Room

**Symptom:**
- "Wait times are too long"
- "Patients are angry"
- "Complaint forms filled out"

**Problem (dig deeper):**
- Bottleneck in triage? (Capacity issue)
- Patients don't know how long they'll wait? (Information issue)
- Seriously ill patients waiting behind minor cases? (Prioritization issue)

**Root Cause (dig deeper still):**
- Triage nurses overwhelmed (staffing/training problem)
- No way to communicate wait estimates (systems problem)
- Patients with non-emergencies coming to ER (upstream problem — lack of alternative care options)

**Different root causes → Different solutions:**
- Staffing problem → Hire/train more triage nurses
- Systems problem → Implement queue management and waiting time displays
- Upstream problem → Partner with urgent care clinics, educate community

**If you solve the symptom without finding root cause, the problem returns.**

### Example: Terminal Truck Delays (Your Domain)

**Symptom:**
- "Trucks are waiting too long"
- "Drivers complaining"
- "Demurrage costs increasing"

**Problem (what's actually broken):**
Could be:
1. Loadout capacity insufficient (bottleneck)
2. Scheduling system inefficient (poor coordination)
3. Paperwork delays at gate (process issue)
4. Quality testing takes too long (lab capacity)
5. Trucks arriving at wrong times (information/communication)

**Root Cause analysis:**

Let's say it's #2 (scheduling). Why is scheduling inefficient?

- Manual phone calls instead of automated booking?
- No visibility into available time slots?
- First-come-first-served creating conflicts?
- No differentiation between quick vs. complex loads?
- Customers booking without actual trucks ready?

**Each root cause suggests different solutions:**
- Automation → Online booking system
- Visibility → Shared calendar system
- Prioritization → Slot reservation with penalties for no-shows
- Differentiation → Different appointment types (standard/express/bulk)

**The Five Whys Technique:**

```
Problem: Trucks waiting too long

Why? → Loadout takes longer than scheduled
Why? → Paperwork issues delaying start
Why? → Documents incomplete when truck arrives
Why? → Customers don't know what documents are needed
Why? → No standardized communication of requirements

ROOT CAUSE: Information gap in customer onboarding
SOLUTION: Standardized checklist, driver training, pre-arrival verification
```

---

## Core Concept 2: Jobs-to-be-Done (JTBD) Framework

### The Central Insight

> **People don't want products. They want to make progress in their lives.**

Products are "hired" to do a job. If a better way to do the job comes along, the product is "fired."

### The JTBD Statement Format

```
When ___[situation]___,
I want to ___[motivation]___,
So I can ___[outcome]___.
```

**NOT:** "I want a drill."
**YES:** "When I need to hang a picture, I want to make a hole in the wall, so I can display my artwork."

**Insight:** You don't want the drill. You want the hole. (You don't want the hole. You want the artwork displayed.)

### Example 1: Netflix

**Bad understanding:**
- "Customers want to stream movies"

**Good understanding (JTBD):**
- "When I have 30 minutes to relax after work, I want to quickly find something entertaining to watch, so I can unwind without spending 20 minutes browsing."

**Job:** Help me decide what to watch (quickly)

**Solution implications:**
- Recommendation engine (primary feature)
- Auto-play (reduce friction)
- "Continue watching" (remember where I was)
- Curated categories (browse faster)

**Notice:** "Stream movies" is the mechanism. "Help me relax quickly" is the job.

### Example 2: Uber

**Bad understanding:**
- "Customers want to book a taxi through an app"

**Good understanding (JTBD):**
- "When I need to get somewhere in the city, I want reliable transportation without the hassle of finding, hailing, or paying for a cab, so I can arrive on time with minimal stress."

**Jobs (multiple):**
1. Get somewhere reliably
2. Know when ride will arrive (reduce uncertainty)
3. Know how much it will cost (no surprises)
4. Pay seamlessly (no cash/card fumbling)

**Solution implications:**
- Driver tracking (know when arrival happens)
- Upfront pricing (reduce uncertainty)
- In-app payment (remove friction)
- Driver ratings (ensure quality)

### Example 3: Terminal Scheduling Software (Your Domain)

**Bad understanding:**
- "Customers want a web portal to book loadout appointments"

**Good understanding (JTBD):**

**Multiple jobs for multiple actors:**

**Trucking company dispatcher:**
- "When I have 20 drivers on the road, I want to efficiently schedule pickups across multiple terminals, so I can maximize driver utilization and meet customer delivery commitments."

**Truck driver:**
- "When I arrive at a terminal, I want to get loaded quickly and get back on the road, so I can make my next delivery on time and earn my per-load pay."

**Terminal scheduler:**
- "When managing 100+ daily loadouts, I want to optimize bay utilization and prevent congestion, so I can maximize throughput while meeting customer SLAs."

**Terminal operator:**
- "When a truck arrives, I want all paperwork and quality specs confirmed in advance, so I can load quickly without delays or errors."

**Each job suggests different features:**
- Dispatcher → Multi-terminal visibility, batch scheduling, optimization algorithms
- Driver → Mobile app, real-time queue position, estimated load time
- Scheduler → Capacity planning tools, conflict detection, priority management
- Operator → Pre-arrival document verification, automated quality lookup, quick loading instructions

**If you only build "a web portal," you miss most of these jobs.**

---

## Core Concept 3: Outcome Statements (Not Feature Requests)

### The Problem with Feature Requests

**Customer says:** "I need a CRM system."

**What they mean:** One of these outcomes:
1. "I need to remember customer interactions so I don't ask the same questions twice"
2. "I need to know which customers haven't bought recently so I can reach out"
3. "I need my team to coordinate so we don't all call the same customer"
4. "I need to track sales pipeline so I can forecast revenue"

**Different outcomes → Different solutions:**
- Outcome 1 → Simple note-taking tool might suffice
- Outcome 2 → Automated alert system based on purchase history
- Outcome 3 → Shared calendar + communication tool
- Outcome 4 → Spreadsheet with pipeline stages might work

**A full CRM addresses all of these, but might be overkill if you only need one outcome.**

### How to Extract Outcomes from Requests

**Customer says:** "Make it faster."

**You ask:**
- Faster than what? (baseline)
- How fast is fast enough? (target)
- What happens when it's slow? (consequence)
- What would you do with the time saved? (value)

**Customer says:** "Add a dashboard."

**You ask:**
- What decision are you trying to make? (purpose)
- What question are you trying to answer? (information need)
- How often do you need this information? (frequency)
- What do you do with the answer? (action)

### Outcome-Driven Statement Format

```
[Actor] needs to [accomplish outcome]
in order to [realize value],
currently hindered by [obstacle].
```

**Example:**

"Terminal schedulers need to balance loadout capacity across multiple products in order to prevent tank overfills and customer wait times, currently hindered by manual spreadsheet-based allocation that doesn't account for real-time demand changes."

**This statement tells you:**
- Who has the problem (schedulers)
- What they're trying to achieve (balance capacity, prevent issues)
- Why it matters (operational risk + customer satisfaction)
- What's broken today (manual process, no real-time updates)

**Now you can design a solution that addresses the actual need.**

---

## Core Concept 4: The Economic Value of Problems

### Not All Problems Are Worth Solving

**A problem is valuable when:**
1. **It's felt frequently** (happens often)
2. **It's felt intensely** (causes significant pain)
3. **People are willing to pay to solve it** (economic value)
4. **It affects many people** (market size)
5. **Current solutions are inadequate** (opportunity gap)

### The Problem Value Matrix

```
                    HIGH FREQUENCY
                          │
           ┌──────────────┼──────────────┐
           │              │              │
           │   NUISANCE   │   HIGH       │
 LOW       │              │   PRIORITY   │
INTENSITY  │              │              │
           ├──────────────┼──────────────┤
           │              │              │
           │   IGNORE     │   NICE TO    │
           │              │   SOLVE      │
           │              │              │
           └──────────────┴──────────────┘
                    LOW FREQUENCY
```

**High Priority (High Frequency × High Intensity):**
- Worth significant investment
- People will pay premium prices
- Venture capital interested

**Nice to Solve (Low Frequency × High Intensity):**
- Important when it happens
- May be worth solving if no alternatives exist
- Example: Hospital emergency room (infrequent for individual, critical when needed)

**Nuisance (High Frequency × Low Intensity):**
- Annoying but tolerable
- Low willingness to pay
- May be worth solving if very easy solution

**Ignore (Low Frequency × Low Intensity):**
- Not worth solving
- Focus elsewhere

### Quantifying Problem Value

**Framework:**

```
Annual Cost of Problem =
  (Frequency per year) ×
  (Cost per occurrence) ×
  (Number of people affected)
```

**Example: Truck Delay Problem**

**Metrics:**
- 50 trucks per day delayed
- Average delay: 30 minutes
- Demurrage cost: $100/hour
- 250 operating days per year

**Calculation:**
```
Annual Cost =
  (50 trucks/day) ×
  (0.5 hours/truck) ×
  ($100/hour) ×
  (250 days/year)

= $312,500 per year
```

**Willingness to pay:** If software reduces delays by 60%, annual savings = $187,500. Customer might pay $50,000/year for the solution (26% of savings).

**Now you can justify development investment.**

### Example: Spreadsheet Errors in Financial Analysis

**Problem:** Financial analysts spend 2 hours per week fixing spreadsheet errors.

**Metrics:**
- 20 analysts in department
- 2 hours per week per analyst
- $75/hour fully-loaded cost
- 50 work weeks per year

**Calculation:**
```
Annual Cost =
  (20 analysts) ×
  (2 hours/week) ×
  ($75/hour) ×
  (50 weeks/year)

= $150,000 per year
```

**Plus hidden costs:**
- Incorrect analysis leading to bad decisions (hard to quantify but potentially huge)
- Delayed reports missing decision windows
- Reputation damage when errors discovered

**Total value of solving: $150k–$500k per year.**

**Solution investment justified:** Up to $100k development cost with 1-year payback.

---

## Core Concept 5: Problem Statements That Attract Investment

### What Makes a Good Problem Statement?

**1. Specific actors** (not "users" or "people")
**2. Clear context** (when/where does problem occur)
**3. Concrete obstacle** (what's blocking progress)
**4. Measurable impact** (cost, time, frequency)
**5. Current inadequate solution** (why existing approaches fail)

### Template

```
[Specific actor/persona]
struggles to [accomplish specific outcome]
in the context of [situation/constraint],
resulting in [measurable consequence],
because [current solution] is inadequate due to [specific failure mode].
```

### Example 1: Healthcare

**Bad:** "Patients have trouble getting appointments."

**Good:**
"Primary care patients with chronic conditions (e.g., diabetes, hypertension) struggle to schedule regular follow-up appointments within their required 90-day window, resulting in 30% of patients missing recommended checkups and experiencing preventable health complications, because the current phone-based scheduling system operates only during business hours (when patients are at work), requires 15+ minute hold times, and doesn't integrate with insurance pre-authorization requirements."

**Why this is good:**
- ✅ Specific actors (chronic condition patients)
- ✅ Clear outcome (schedule within 90-day window)
- ✅ Measurable impact (30% miss checkups)
- ✅ Real consequence (preventable complications)
- ✅ Current solution described (phone system)
- ✅ Why it fails (timing, friction, integration gaps)

### Example 2: Supply Chain (Your Domain)

**Bad:** "Logistics is complicated."

**Good:**
"Regional supply chain managers coordinating multi-modal shipments (rail → terminal → truck → customer) struggle to maintain real-time visibility of product location and custody status across 10+ independent systems (railroad SCADA, terminal management, trucking GPS, customer receiving), resulting in 4-6 hours per day spent manually reconciling conflicting data sources and fielding customer 'Where is my order?' calls, because each system uses different product identifiers, timestamps, and measurement units with no automated translation layer."

**Why this is good:**
- ✅ Specific actor (regional managers)
- ✅ Specific job (maintain visibility across multi-modal)
- ✅ Context (10+ independent systems)
- ✅ Measurable impact (4-6 hours/day wasted)
- ✅ Current approach (manual reconciliation)
- ✅ Why it fails (no data translation/integration)

### Example 3: Software Development

**Bad:** "Code quality is poor."

**Good:**
"Backend engineers working in a 50+ microservice architecture struggle to understand the downstream impact of API contract changes before deploying to production, resulting in an average of 3 breaking changes per week that cause customer-facing incidents and require emergency rollbacks, because the current testing strategy relies on manually maintained integration tests that cover only 20% of service dependencies and become outdated within days of being written."

**Why this is good:**
- ✅ Specific actor (backend engineers)
- ✅ Context (microservice complexity)
- ✅ Specific outcome (understand impact before deploy)
- ✅ Measurable consequence (3 breaks/week)
- ✅ Current solution (manual integration tests)
- ✅ Why it fails (low coverage, maintenance burden)

---

## Practical Framework: The Problem Discovery Canvas

Use this to systematically analyze any problem:

### Problem Discovery Template

**1. WHO experiences this problem?**
- Primary actor/persona:
- Secondary actors affected:
- How many people total?

**2. WHAT are they trying to accomplish?**
- Desired outcome:
- Success criteria:
- Why does this outcome matter?

**3. WHEN/WHERE does the problem occur?**
- Context/situation:
- Frequency (per day/week/month):
- Triggers that cause the problem:

**4. WHY can't they accomplish it today?**
- Current approach/workaround:
- What breaks down?
- Root cause (use Five Whys):

**5. WHAT are the consequences?**
- Time wasted:
- Money lost:
- Opportunities missed:
- Emotional impact (frustration, stress):

**6. HOW MUCH would solving this be worth?**
- Quantified cost of problem:
- Willingness to pay:
- Market size (if product):

**7. WHY hasn't this been solved already?**
- Technical barriers?
- Economic barriers?
- Organizational barriers?
- Awareness barriers?

---

## Hands-On Exercise: Reframe Feature Requests as Problems

### Deliverable: Problem Translation Practice

Take these 5 common "requests" and translate them into proper problem statements using JTBD and outcome thinking.

**For each, document:**
1. **Surface request** (what they said)
2. **Underlying job** (what they're trying to accomplish)
3. **Desired outcome** (what success looks like)
4. **Current inadequacy** (why existing solution fails)
5. **Proper problem statement** (using template)
6. **Alternative solutions** (3+ ways to solve, not just the requested feature)

### Request 1: "I need a CRM system"

**Analyze:**
- Who is asking? (Sales manager? Support rep? Marketing?)
- What job are they trying to do?
- What outcome would make them happy?
- How do they solve this today?
- What breaks down?

**Your deliverable:** Full problem statement + 3 alternative solutions

### Request 2: "Make the app faster"

**Analyze:**
- What specific flow feels slow?
- How slow is "slow"? (baseline measurement)
- What is the user trying to accomplish in that moment?
- What happens when it's slow? (abandon? frustration? error?)
- What would "fast enough" look like?

**Your deliverable:** Full problem statement + 3 alternative solutions

### Request 3: "Add real-time notifications"

**Analyze:**
- What information do they need?
- When do they need it?
- What decision or action does this information enable?
- What happens if they don't get notified?
- How do they get this information today?

**Your deliverable:** Full problem statement + 3 alternative solutions

### Request 4: "Build a mobile app" (for an existing web app)

**Analyze:**
- What job can't they do on mobile web?
- Where are they when they need this?
- What device constraints matter? (offline? location? camera?)
- How frequently do they need mobile access?
- What's the cost of not having mobile?

**Your deliverable:** Full problem statement + 3 alternative solutions

### Request 5: "Integrate with [Third-Party System]"

**Analyze:**
- What data needs to flow between systems?
- What workflow is broken without integration?
- How do they bridge the gap today?
- What errors or inefficiencies result?
- Who benefits from integration?

**Your deliverable:** Full problem statement + 3 alternative solutions

---

## Connection to Systems Thinking (Course 1)

**Everything you learned in Course 1 applies to problem discovery:**

### Stocks and Flows
- Problems often manifest as unwanted stock accumulation (backlog, inventory, technical debt)
- Or inadequate stock levels (shortage, burnout, capacity)

### Bottlenecks
- Many problems are actually bottleneck problems disguised as feature requests
- "We need more people" might really be "We have a bottleneck that adding people won't fix"

### Feedback Loops
- Problems exist within systems with feedback structures
- Solving symptoms without understanding loops creates policy resistance
- Example: "Hire more support staff" doesn't work if the real problem is product quality (reinforcing loop)

### Causal Loop Diagrams
- Map the problem in a CLD to see the system structure
- Find where interventions would break vicious cycles or strengthen virtuous cycles

**Example: Customer Churn Problem**

```
           ┌────────────────────────────────────┐
           │                                    │
           │      (R) Churn Spiral              │
           ▼                                    │
    Product Bugs ──(+)──> Customer    ──(-)──> Revenue  ──(-)──> Engineering
                         Frustration                              Resources
           ▲                                                           │
           │                                                           │
           └────────────────────(+)─────────────────────────────────────┘
                                Technical Debt / Rush Fixes
```

**Problem statement:** "We need better customer support."

**System view:** Support is a symptom. Real problem is reinforcing loop: bugs → churn → less revenue → fewer engineers → more rushed fixes → more bugs.

**Better intervention:** Break the loop by investing in quality (refactoring, testing, slower releases) even though it feels wrong in the short term.

---

## Reflection Questions

1. **Feature requests you've heard:** Think of 3 feature requests from users or stakeholders. What was the underlying job they were trying to do? What outcome did they actually want?

2. **Symptom vs root cause:** Describe a time when you solved a symptom but the problem came back. What was the root cause you missed?

3. **Problem value:** Pick a problem in your current work. Calculate its annual cost (frequency × impact × people affected). Is it worth solving?

4. **Jobs you hire products for:** Think of 3 products you use regularly. What job do you hire each one to do? What outcome are you seeking? Could a different solution do the job better?

5. **Systems connection:** Choose one problem from your domain. Draw a simple CLD showing the feedback loops that keep the problem in place. Where would you intervene?

---

## Key Takeaways

1. **Problems ≠ Feature requests:** Don't build what people ask for; solve what they need
2. **JTBD is the lens:** People hire solutions to make progress; understand the job
3. **Symptoms hide root causes:** Use Five Whys to dig deeper
4. **Outcomes > Mechanisms:** Focus on what people want to accomplish, not how
5. **Value is quantifiable:** Calculate frequency × intensity × affected population
6. **Problem statements sell:** Specific, measurable, compelling problem descriptions attract investment
7. **Systems thinking applies:** Problems exist in feedback structures; see the whole system

**Next week:** You'll learn **interview and observation techniques** to uncover problems that people don't articulate directly — because the best problems are often hidden in workflows, workarounds, and frustrated sighs.

---

## Additional Resources

**Books:**
- *The Mom Test* by Rob Fitzpatrick (essential — best book on customer interviews)
- *Competing Against Luck* by Clayton Christensen (JTBD framework deep dive)
- *Intercom on Jobs-to-be-Done* (free ebook from Intercom)

**Frameworks:**
- Jobs-to-be-Done templates (JTBD.info)
- Lean Canvas (focus on problem/solution fit)
- Value Proposition Canvas (Strategyzer)

**Practice:**
- Next time someone requests a feature, ask "What job are you trying to do?"
- Interview 3 people about a workflow they struggle with
- Write problem statements for 5 frustrations in your own life

---

*The most valuable skill in product development: **See the real problem beneath the stated request.** Master this, and you'll build things people actually need.*

**End of Week 5**
