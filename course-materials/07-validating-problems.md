# Week 7: Validating Problems Without Code
## How to Test if Problems Are Worth Solving Before Building Anything

---

## Introduction: The $500,000 Lesson in Validation

A startup spent 18 months building a sophisticated terminal scheduling platform. Beautiful UI. Real-time optimization algorithms. Mobile apps for drivers. Integration with 15 different systems.

**Total investment:** $500,000 in engineering, $200,000 in sales and marketing.

**Launch day:** 5 terminals signed up for pilots.

**3 months later:** 2 terminals still using it. Usage dropping.

**6 months later:** 0 terminals paying. Product shut down.

**What went wrong?**

The founders interviewed terminal managers who said: "Yes, scheduling is a pain. We'd love a better system."

But they never tested whether terminals would:
1. **Actually change their workflows** to use new software
2. **Pay enough** to sustain the business
3. **Overcome integration hurdles** with existing systems
4. **Get buy-in from operators** who liked the old way
5. **Prioritize this problem** over other urgent issues

**What they should have done:**

**Week 1:** Create a landing page describing the solution. Drive traffic. Measure signup rate.
- **Cost:** $500
- **Learning:** Do people even click "I'm interested"?

**Week 2:** For signups, offer a manual concierge service (no software yet).
- **Cost:** Founder's time
- **Learning:** Will terminals follow a new scheduling process if we do the work for them?

**Week 3:** After proving manual demand, build a simple spreadsheet-based tool.
- **Cost:** 1 week of engineering
- **Learning:** Can we deliver value without the fancy features?

**Week 4:** Get 3 terminals to pay $100/month for the spreadsheet version.
- **Cost:** Sales effort
- **Learning:** Is this problem worth paying for?

**Then and only then:** Build the real platform.

**Total validation cost:** $5,000 and 4 weeks, instead of $700,000 and 18 months.

**This week you'll learn to validate problems cheaply and quickly, before writing production code.**

---

## Core Concept 1: The Validation Hierarchy

### Not All Evidence Is Equal

**Weakest evidence → Strongest evidence:**

```
1. "I think this would be useful" (opinion)
   ↓
2. "Yes, I'd probably use this" (hypothetical intent)
   ↓
3. "Here's my email, notify me when ready" (low-commitment signal)
   ↓
4. "I'll pay a deposit to reserve my spot" (money down)
   ↓
5. "Here's my credit card, charge me monthly" (ongoing commitment)
   ↓
6. "I'm using it every day and referring others" (proven value)
```

**Most failed products stop at level 2 or 3.** They mistake interest for commitment.

### The Validation Mindset

**Before building software, answer these questions:**

1. **Problem existence:** Does this problem actually exist? (Interviews, observation)
2. **Problem frequency:** How often does it occur? (Daily? Weekly? Monthly?)
3. **Problem intensity:** How painful is it? (Annoying? Costly? Mission-critical?)
4. **Current solutions:** What do people do today? How much does it cost?
5. **Willingness to change:** Will people adopt a new approach?
6. **Willingness to pay:** Will they pay enough to sustain a solution?
7. **Solution feasibility:** Can we actually solve this?

**You've covered #1-4 in Weeks 5-6. This week focuses on #5-7.**

---

## Core Concept 2: Smoke Tests and Landing Pages

### What Is a Smoke Test?

**Definition:** An experiment to gauge demand before building the product.

**Core idea:** Pretend the product exists, offer it to potential customers, measure response.

**Named after:** "Smoke testing" in electronics — turn it on, see if smoke comes out. If no smoke, probably works.

### The Classic Landing Page Smoke Test

**How it works:**

1. **Create a landing page** describing your solution
2. **Add a call-to-action** ("Sign up for early access", "Get notified", "Pre-order now")
3. **Drive traffic** (ads, social media, direct outreach)
4. **Measure conversion rate** (visitors → signups)
5. **Follow up** with signups to learn more

**What you're testing:**
- Do people understand the value proposition?
- Is the problem compelling enough for them to take action?
- What conversion rate indicates real demand?

### Example: Terminal Scheduling Landing Page

**Headline:** "Stop Playing Phone Tag with Truck Drivers"

**Subhead:** "Terminal scheduling software that reduces wait times by 40% and eliminates surprise arrivals."

**Value props:**
- Drivers book appointments online (no more phone calls)
- Real-time bay availability
- Automated notifications
- Integration with your terminal management system

**CTA:** "Start Your Free Trial" → Leads to waitlist signup form

**Metrics to track:**
- **Traffic:** 500 terminal managers visit the page
- **Signups:** 75 leave their email (15% conversion)
- **Follow-up:** You email them a survey, 40 respond (53% engagement)
- **Qualified:** 10 say "Yes, I'd pay $200/month for this" (25% of respondents)

**Interpretation:**
- 15% landing page conversion is solid (typical range: 5-25%)
- 53% email engagement is high (they're interested)
- 10 qualified leads from 500 visitors = 2% qualified conversion
- **Verdict:** Worth pursuing, but need to validate if they'll actually pay

### Advanced Smoke Test: The "Pay Before It Exists" Test

**Even better than email signups:** Ask for payment upfront.

**Example: Pre-order Campaign**

Landing page: "Reserve your spot in the beta. First 20 terminals get 50% off forever."

CTA: "Pay $100 deposit (refundable if we don't deliver)"

**Why this works:**
- Separates genuine interest from curiosity
- Validates willingness to pay, not just interest
- Creates urgency and scarcity

**Results:**
- 500 visitors → 15 pay deposit (3% conversion)
- **Verdict:** Strong signal. People are putting money down.

**If nobody pays:** The problem isn't painful enough, or your solution isn't compelling. Pivot or kill the idea.

### Smoke Test for Internal Tools

**Scenario:** You want to build an internal tool for your company (e.g., automated test reporting dashboard).

**Smoke test:**
1. **Create a mockup** in PowerPoint or Figma
2. **Show it to potential users**: "If this existed, would you use it?"
3. **Measure enthusiasm**: Are they excited or polite?
4. **Test behavior**: "I can manually generate this report for you weekly. Would you want that?"
5. **Track usage**: If they don't use the manual version, they won't use the automated one

**Example outcome:**
- Show dashboard mockup to 10 engineers
- 8 say "Yeah, that'd be nice"
- 2 say "Oh wow, I need this now"
- Offer manual version to the 2 enthusiastic ones
- 1 actually uses it, 1 forgets
- **Verdict:** Only 1 person really has this problem. Not worth building for whole team. Build for that one person first, see if others copy the behavior.

---

## Core Concept 3: Concierge MVPs (Manual First)

### The Concierge Approach

**Core idea:** Deliver the outcome manually before automating it.

**Why?**
1. **Validate the outcome matters** (not just the idea)
2. **Learn the nuances** of what customers actually need
3. **Iterate quickly** without rebuilding software
4. **Get paid while learning** (charge for the service, even if manual)

**The term "concierge":** You're personally delivering white-glove service to understand what "good" looks like.

### Example 1: Food Delivery (DoorDash Origin Story)

**The problem:** Restaurants want delivery but can't afford dedicated drivers.

**The validation approach:**

**2013: The concierge MVP**
- Founders created a simple website listing local restaurants
- When orders came in, founders personally drove to restaurants, picked up food, delivered it
- **No driver network, no routing algorithms, no mobile app**
- Just founders in a car with Google Maps

**What they learned:**
- Restaurants were willing to pay 15-20% commission
- Customers were willing to pay delivery fees + tip
- 30-45 min delivery was acceptable
- Communication during delivery was critical ("Where's my order?")
- Some restaurants had packaging issues (food arrived cold/messy)

**After 3 months:** Proven demand, real revenue, deep understanding of operational challenges.

**Then:** Built software to scale (driver marketplace, routing, real-time tracking).

**Key insight:** They didn't build software until they proved people would pay and use the service.

### Example 2: Terminal Load Optimization (Your Domain)

**The problem:** Terminals want to optimize which product goes in which tank to minimize ship-to-truck time.

**Traditional approach:** Build complex optimization algorithm first.

**Concierge approach:**

**Month 1: Manual optimization service**
- Offer to manually create optimized tank assignments for one terminal
- Every morning, you analyze:
  - Incoming pipeline schedules
  - Outgoing truck orders
  - Tank capacities and current contents
  - Product compatibility rules
- Create recommended tank assignment plan
- Send to terminal scheduler via email/spreadsheet
- Track: Do they follow your recommendations? What do they override? Why?

**What you learn:**
- Scheduler overrides 30% of recommendations (you learn their implicit rules)
- Tank assignments change mid-day due to surprise orders (your daily plan isn't enough)
- Real constraint isn't optimization algorithm — it's data entry burden
- They'd pay $500/month for this service (validates value)

**Month 2: Refine the service**
- Move from daily email to shared spreadsheet with real-time updates
- Add notes field for schedulers to explain overrides
- Learn their actual decision logic (not the "official" rules)

**Month 3: Start building software**
- Now you know: Real-time updates matter more than perfect optimization
- You know: Scheduler judgment can't be fully automated (build "suggest + approve" workflow)
- You know: Integration with truck order system is critical (not tank monitoring)

**Result:** You build the right thing because you did it manually first.

### Example 3: Automated Code Review Tool

**The problem:** Team wants automated code review to catch common mistakes.

**Concierge approach:**

**Week 1-2: Manual review**
- Personally review every PR for 2 weeks
- Document every issue you catch (security, performance, style, logic errors)
- Categorize issues: automatable vs. requires human judgment

**What you learn:**
- 60% of issues are simple patterns (could automate)
- 30% require domain context (can't automate)
- 10% are subjective style debates (not worth automating)
- Developers appreciate fast feedback more than perfect feedback

**Week 3-4: Simple automation**
- Write grep scripts to catch the top 10 automatable patterns
- Run manually on each PR, post results as comments
- Measure: Does team fix issues faster? Do they appreciate it?

**Outcome:** You've validated the need, refined the scope, and have a simple tool that delivers 80% of the value. Now decide if more sophistication is worth it.

---

## Core Concept 4: Solution Interviews (Show, Don't Tell)

### The Problem with Pitching Ideas

**Bad interview:** "Would you use a dashboard that shows X, Y, and Z?"

**Why it fails:**
- People say yes to be polite
- They can't visualize what you're describing
- They imagine their ideal version, not your actual version

**Better approach:** Show them something concrete, even if it's fake.

### The Prototype Interview

**Tools you can use:**
1. **Sketches** (paper, whiteboard)
2. **Wireframes** (Balsamiq, Figma)
3. **Clickable mockups** (InVision, Figma prototypes)
4. **Fake-o demos** (PowerPoint clicking through screens)
5. **Manual demo** (you operate a Wizard of Oz version)

**Goal:** Get them to react to something specific, not abstract.

### Example: Dashboard Solution Interview

**Setup:**
- Create mockup of analytics dashboard in Figma (2 hours of work)
- Show it to 5 potential users
- Watch their reaction, don't explain yet

**Script:**
"I'm going to show you something. Don't be polite — I want honest reactions. Tell me what you think this is, what you'd use it for, what's missing."

**Show mockup silently. Wait.**

**What you learn from their reactions:**

**User 1:** "Oh cool, so this shows... wait, how do I drill down into the details?"
→ **Learning:** They want interactivity, not just static charts

**User 2:** "This is nice, but I'd only look at it once a month."
→ **Learning:** Not a daily problem, wrong use case for dashboard

**User 3:** "Hmm, I don't see [X metric]."
→ **Learning:** You missed a critical metric they care about

**User 4:** "Oh this is great! When can I have it?"
→ **Learning:** Strong genuine interest (rare and valuable)

**User 5:** "This looks like [existing tool]. How is it different?"
→ **Learning:** You have competition you didn't know about

### The "Wizard of Oz" Demo

**Technique:** Show them a "working" system that's actually you behind the curtain.

**Example: Smart scheduling assistant**

**What the user sees:**
- Web interface to request truck appointment
- "AI" suggests optimal time slot
- Confirmation email sent automatically

**What's actually happening:**
- You're watching requests come in via form submission
- You're manually checking availability in spreadsheet
- You're manually emailing confirmations

**Why this works:**
- User experiences the workflow (tests usability)
- You learn what information they provide vs. what's missing
- You see if they come back and use it repeatedly
- You can iterate on the "rules" without rewriting code

**When to reveal the truth:** After they've used it 3-5 times and you've learned what you need. Most users don't care if it was manual at first if the outcome was valuable.

---

## Core Concept 5: Measuring Problem Severity (Frequency × Intensity)

### The Problem Severity Matrix

You saw this briefly in Week 5. Now let's operationalize it.

```
INTENSITY
   ↑
   │
 5 │    [  LOW  ] [  MEDIUM  ] [  HIGH  ]
   │    Annoying   Profitable   Mission
 4 │    but low    Sweet spot   Critical
   │     value                   (niche)
 3 │
   │    [IGNORE ] [  MEDIUM  ] [  HIGH  ]
 2 │   Not worth  Consider if  Worth
   │   attention   easy fix     solving
 1 │
   │    [IGNORE ] [  IGNORE  ] [ MEDIUM ]
   │   Background  Not worth    Nice to
   │     noise     effort       have
   └───────────────────────────────────→
       1          3            5    FREQUENCY
    (Rare)    (Monthly)     (Daily)
```

### How to Score Problems

**Frequency Scale (1-5):**
- **1 = Rare:** Once a year or less
- **2 = Occasional:** Few times per year
- **3 = Regular:** Monthly
- **4 = Common:** Weekly
- **5 = Constant:** Daily or multiple times per day

**Intensity Scale (1-5):**
- **1 = Minor inconvenience:** Barely noticeable, no real cost
- **2 = Annoyance:** Noticeable, small cost (< 15 minutes or < $50)
- **3 = Significant pain:** Meaningful cost (1 hour or $100-500)
- **4 = Major problem:** Large cost (half day or $1,000+)
- **5 = Critical/urgent:** Mission-critical, company-threatening cost

### Example: Scoring Terminal Problems

**Problem 1: Truck driver can't find loading bay**
- Frequency: 2-3 times per week = **4**
- Intensity: Driver calls, operator walks out to guide them, 10 min delay = **2**
- **Score: 4 × 2 = 8** (Medium priority)
- **Solution value:** Low. Simple signage fix ($500 one-time).

**Problem 2: Tank overfill alarm**
- Frequency: Once a quarter = **2**
- Intensity: Emergency shutdown, potential spill, regulatory incident = **5**
- **Score: 2 × 5 = 10** (High priority despite rarity)
- **Solution value:** High. Prevents catastrophic cost.

**Problem 3: Manual data entry of meter readings**
- Frequency: 50 times per day = **5**
- Intensity: 2 minutes per reading, occasional errors = **2**
- **Score: 5 × 2 = 10** (High priority due to frequency)
- **Solution value:** High. Automation saves 100 min/day.

**Problem 4: Scheduling conflicts between overlapping orders**
- Frequency: 2-3 times per day = **5**
- Intensity: 30 min to resolve, customer frustration, may delay trucks = **3**
- **Score: 5 × 3 = 15** (Highest priority)
- **Solution value:** Very high. Scheduling optimization has huge ROI.

### Quantifying the Opportunity

**For each high-scoring problem, calculate annual cost:**

**Problem 4 calculation:**
```
Frequency: 2.5 times per day
Impact per occurrence:
  - 30 min scheduler time @ $40/hr = $20
  - 30 min truck delay @ $100/hr = $50
  - Customer friction (harder to quantify, but real)

Total: $70 per occurrence

Annual cost:
  $70 × 2.5 times/day × 250 working days = $43,750 per year

Multiply by number of terminals:
  $43,750 × 50 terminals = $2,187,500 total market pain
```

**This tells you:**
- Individual terminal would save $43k/year
- They might pay $10k/year for solution (23% of savings)
- With 50 terminals, that's $500k/year revenue potential
- Justifies significant investment in solution

---

## Core Concept 6: When to Kill an idea Early

### The Discipline of No

**Most ideas should be killed.** That's not failure — that's smart validation.

**Kill signals:**

1. **Low interest in smoke test** (< 2% conversion from landing page)
2. **Nobody uses concierge MVP** (offered manual version, crickets)
3. **Polite responses but no action** ("That's nice, let me think about it")
4. **Low problem severity score** (< 6 on frequency × intensity)
5. **No willingness to pay** (after directly asking)
6. **Existing solutions are "good enough"** (people complain but don't switch)
7. **Can't reach target market** (don't know how to find customers)
8. **Regulatory or technical blockers** (can't actually build it)

### The Kill Decision Framework

**Ask these questions:**

**1. Problem validation:**
- Have we confirmed the problem exists? (Yes/No)
- Is it frequent and intense enough? (Score ≥ 10?)

**2. Solution validation:**
- Do people want OUR solution? (Not just any solution)
- Will they change behavior to use it?
- Have we proven this with actions, not words?

**3. Economic validation:**
- Will people/companies pay enough?
- Is the market large enough?
- Can we reach customers affordably?

**4. Feasibility:**
- Can we actually build this?
- Do we have the skills/resources?
- Can we deliver before market moves on?

**Decision matrix:**

```
If Problem = NO:  Kill immediately
If Problem = YES, Solution = NO:  Pivot solution approach
If Problem = YES, Solution = YES, Economic = NO:  Kill or pivot to different market
If Problem = YES, Solution = YES, Economic = YES, Feasibility = NO:  Kill or find different approach
If all YES:  Proceed to build
```

### Example: Killing a "Good" Idea

**Idea:** Automated terminal reporting system

**Problem validation:**
- Terminals spend 2 hours/day on regulatory reports ✓
- Happens daily ✓
- Frequency = 5, Intensity = 3, Score = 15 ✓

**Solution validation:**
- Built concierge version (manual report generation)
- 3 terminals signed up
- 2 stopped using it after 2 weeks
- Reason: "Our auditor wants it in our format, not yours" ✗

**Economic validation:**
- Remaining 1 terminal willing to pay $100/month
- Need $5k/month to sustain development
- Would need 50 terminals
- Only reached 3 after 3 months of sales ✗

**Feasibility:**
- Technically feasible ✓
- But every terminal wants custom format ✗
- Professional services business, not software ✗

**Decision: KILL**

**Reason:** Solution doesn't fit the problem (customization requirement breaks scalability). Economic model doesn't work (can't reach enough customers). Would become consulting, not product.

**Alternative pivot:** Sell spreadsheet templates instead of software. One-time $500 purchase. Lower revenue but fits the reality.

---

## Practical Framework: The Validation Roadmap

### Phase 1: Problem Discovery (Weeks 5-6)
- ✓ Interviews
- ✓ Observation
- ✓ Problem statements

### Phase 2: Demand Testing (Week 7)

**Step 1: Smoke test (Days 1-7)**
- Create landing page or mockup
- Drive 100-500 potential users to it
- Measure conversion (goal: > 5%)
- Follow up with signups

**Step 2: Solution interview (Days 8-14)**
- Show mockup/prototype to top prospects
- Watch reactions
- Iterate based on feedback
- Identify must-have features

**Step 3: Concierge MVP (Days 15-30)**
- Deliver outcome manually to 3-5 customers
- Charge money (even small amount)
- Track actual usage
- Learn operational nuances

**Decision point: Proceed or pivot?**
- If 3+ customers using and paying → Build software
- If 1-2 customers using → Need more validation
- If 0 customers using → Kill or major pivot

### Phase 3: Minimum Viable Product (Next stage)
- Build simplest software version
- Focus on core workflow only
- Ship to concierge customers first
- Measure retention and expansion

---

## Hands-On Exercise: Generate and Validate Problem Hypotheses

### Deliverable: 5 Problem Hypotheses with Validation Plans

**From your domain experience, generate 5 problem hypotheses.**

For each one, document:

#### 1. Problem Statement

Use Week 5 format:
```
[Actor] struggles to [accomplish outcome]
in context of [situation],
resulting in [consequence],
because [current solution] fails due to [root cause].
```

#### 2. Problem Severity Score

- **Frequency (1-5):** How often?
- **Intensity (1-5):** How painful?
- **Score (F × I):** Multiply
- **Interpretation:** Ignore (< 6), Medium (6-10), High (11-15), Critical (16-25)

#### 3. Annual Cost Calculation

```
Cost per occurrence × Frequency per year × People affected = Annual cost
```

#### 4. Validation Method

**Choose one:**

**A. Smoke Test**
- Landing page copy (headline, value props, CTA)
- Traffic source (how you'll reach 100+ people)
- Success criteria (what conversion rate = proceed?)

**B. Concierge MVP**
- Manual service description (what you'll deliver)
- How you'll deliver it (process, tools)
- Pricing (what you'll charge)
- Success criteria (X customers using after Y weeks)

**C. Solution Interview**
- Mockup description (what you'll show)
- Interview questions (what you'll ask)
- Number of interviews (at least 5)
- Success criteria (how many need to be enthusiastic?)

#### 5. Kill Criteria

**Define upfront:**
- If < X people sign up → Kill
- If < Y people use concierge version → Kill
- If < Z people willing to pay → Kill
- If [specific deal-breaker] discovered → Kill

### Example Template Filled Out

**Problem 1: Terminal truck scheduling conflicts**

**Problem Statement:**
Terminal schedulers managing 20+ daily truck appointments struggle to prevent scheduling conflicts and minimize wait times in context of customers booking via phone/email without visibility into availability, resulting in 3-5 conflicts per day requiring 30 minutes each to resolve and frustrated drivers waiting 45+ minutes, because current methods (whiteboard, phone log, scheduler's memory) fail due to lack of real-time shared visibility and automated conflict detection.

**Severity Score:**
- Frequency: 5 (multiple times daily)
- Intensity: 3 (30 min + customer friction)
- Score: 15 (Critical)

**Annual Cost:**
- $70 per conflict (scheduler + driver time)
- 4 conflicts/day × 250 days = 1,000 conflicts/year
- $70,000 per terminal per year
- 50 terminals = $3.5M total market

**Validation Method: Concierge MVP**

**Manual Service:**
- I'll manage scheduling for one terminal for 30 days
- Customers call/email me instead of terminal
- I maintain shared Google Calendar
- I confirm appointments and send reminders
- I handle conflicts proactively

**Pricing:** $500 for the month (test willingness to pay)

**Success Criteria:**
- Terminal agrees to try it
- 80% of customers use the system (vs. calling terminal directly)
- Conflicts drop by 50%
- Terminal wants to continue after trial

**Kill Criteria:**
- If terminal won't try even for free → Kill (no pain)
- If customers won't use new booking method → Kill (behavior change too hard)
- If conflicts don't decrease → Kill (wrong solution)
- If terminal won't pay $500/month after trial → Kill (insufficient value)

---

## Reflection Questions

1. **Past projects:** Think of something you built that didn't get used. Could you have validated the problem earlier? What smoke test or concierge MVP would have revealed the issue?

2. **Current backlog:** Look at your team's feature backlog. Which items have been validated with real evidence (money, usage) vs. just requested? What would proper validation look like?

3. **Your domain:** What's a problem you think exists in your domain? How would you validate it without building software? What's the cheapest test you could run?

4. **Risk tolerance:** How do you feel about killing ideas early? Do you tend to build first and validate later? Why?

5. **Manual delivery:** Think of a software feature request you've heard. Could you deliver that outcome manually first? What would that look like?

---

## Connection to Systems Thinking

**Everything connects back to Course 1:**

### Validation Prevents Reinforcing Loops

**Bad loop (build before validating):**
```
Build features → Low usage → Pressure to add more features →
More complexity → Even lower usage → Death spiral
```

**Good loop (validate first):**
```
Validate problem → Build minimum solution → Prove usage →
Build next feature → More usage → Virtuous cycle
```

### Bottleneck Thinking in Validation

**Question:** What's the constraint on adoption?
- Is it awareness? (Do people know the problem exists?)
- Is it behavior change? (Will they adopt new workflow?)
- Is it value? (Does it save enough time/money?)

**Exploit before elevate:** Test cheapest constraint first (landing page), before expensive ones (building software).

### Feedback Loops in Usage

**Monitor these loops:**
- **Value realization loop:** Does usage create value that drives more usage?
- **Friction loop:** Do adoption barriers create abandonment?

**Early warning signs:** If concierge MVP has high friction, automated version will be worse (more friction, less flexibility).

---

## Key Takeaways

1. **Validate demand before building:** Smoke tests, concierge MVPs, and solution interviews separate interest from commitment
2. **Strongest evidence = money + usage:** Email signups are weak signals; paying customers using your manual version is strong
3. **Manual delivery first:** Concierge MVPs teach you what customers actually need vs. what they think they want
4. **Show, don't tell:** Solution interviews with mockups reveal reactions words can't capture
5. **Measure problem severity:** Frequency × Intensity scoring identifies highest-value problems
6. **Kill decisively:** Most ideas should die early. That's not failure — it's smart capital allocation
7. **Validation is fast and cheap:** Weeks and thousands, not years and millions

**Next week:** You'll synthesize everything into **Opportunity Docs and Problem Briefs** — comprehensive documents that communicate problems and opportunities to stakeholders, culminating in **PROJECT 2**.

---

## Additional Resources

**Books:**
- *The Lean Startup* by Eric Ries (MVP concepts, validated learning)
- *Sprint* by Jake Knapp (5-day validation process from Google Ventures)
- *Testing Business Ideas* by Strategyzer (validation playbook)

**Tools:**
- Unbounce, Carrd, or Webflow (landing page builders)
- Figma or Sketch (mockup tools)
- Google Forms or Typeform (smoke test signups)
- Loom (demo videos without building product)

**Case Studies:**
- Dropbox explainer video (validated demand before building product)
- Zappos (manual fulfillment before building inventory system)
- Buffer (landing page with pricing before writing code)

**Practice:**
- Create a landing page for a problem you think exists (can be hypothetical)
- Offer to manually solve a problem for 3 people for free
- Show 5 people a mockup of a solution and watch their reactions
- Calculate the annual cost of 3 problems you observe this week

---

*The best engineers don't just build well — they build the right things. Master validation, and you'll never waste months building something nobody wants.*

**End of Week 7**
