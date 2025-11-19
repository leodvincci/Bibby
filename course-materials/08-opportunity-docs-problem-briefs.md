# Week 8: Opportunity Docs & Problem Briefs
## Communicating Problems and Opportunities That Get Funded

---

## Introduction: The Document That Changed a Career

A software engineer wanted to build an internal tool for better test reporting. They went to their manager with:

**"Hey, I think we should build a test dashboard. Can I spend two weeks on it?"**

Manager: "We have other priorities. Maybe later."

**Result:** Idea dies.

---

**A different engineer, same company, same problem, different approach:**

They wrote a 3-page opportunity brief:

**Page 1: The Problem**
- Engineering team spends 45 min per day investigating test failures
- Root cause: test logs scattered across 5 systems
- Annual cost: 200 engineers × 45 min/day × $75/hr × 250 days = $1,406,250

**Page 2: The Opportunity**
- Centralized test reporting dashboard
- Reduces investigation time by 60% (to 18 min/day)
- Annual savings: $843,750
- Payback period: 3 weeks of engineering effort

**Page 3: Validation & Next Steps**
- Surveyed 20 engineers: 85% say this is their top pain point
- Built prototype, 10 engineers using daily
- Requesting 1 engineer for 6 weeks to productionize
- ROI: 141x first year (savings / cost)

**Manager:** "This is excellent. You have approval. Start Monday."

**Result:** Tool built, widely adopted, engineer promoted to tech lead.

---

**What was different?**

The second engineer:
1. **Quantified the problem** (not just described it)
2. **Showed validation evidence** (not just an opinion)
3. **Made it easy to say yes** (clear ask, clear ROI)
4. **Communicated like a business person** (cost/benefit, not just features)

**This week, you'll learn to write documents that get funded.**

---

## Core Concept 1: The Purpose of Opportunity Documents

### Why Write Formal Documents?

**Reason 1: Force yourself to think clearly**
- Writing exposes fuzzy thinking
- If you can't explain the problem clearly, you don't understand it
- Documents force you to confront gaps in your research

**Reason 2: Make decisions asynchronous**
- Stakeholders can read on their schedule
- Multiple decision-makers can review independently
- Avoids "you had to be in the meeting" syndrome

**Reason 3: Create institutional memory**
- Future teams can understand why decisions were made
- "Why did we build X?" → Read the original opportunity doc
- Prevents re-litigating old decisions

**Reason 4: Enable delegation**
- Executive reads brief, delegates decision to manager
- Manager has context to make informed choice
- Reduces bottlenecks in decision-making

**Reason 5: Practice professional communication**
- Engineers who write well advance faster
- Business stakeholders judge you by written communication
- GitHub repos and code aren't enough — you need to explain *why*

### Types of Documents

**1. Problem Brief**
- Focus: Articulate the problem deeply
- Audience: Teams deciding what to work on
- Outcome: "Yes, this problem is worth solving"

**2. Opportunity Document**
- Focus: Business case for solving the problem
- Audience: Budget holders, executives
- Outcome: Funding and resources allocated

**3. Product Requirements Document (PRD)**
- Focus: What to build (comes after problem validated)
- Audience: Engineering, design, QA teams
- Outcome: Shared understanding of scope

**4. Architecture Decision Record (ADR)**
- Focus: Technical approach and tradeoffs
- Audience: Engineering team
- Outcome: Document why technical choices were made

**This week focuses on #1 and #2** — Problem Briefs and Opportunity Docs.

---

## Core Concept 2: Problem Brief Structure

### The Essential Components

**A problem brief answers 7 questions:**

1. **What is the problem?** (Clear, specific statement)
2. **Who experiences it?** (Personas, roles, stakeholder map)
3. **How do we know it exists?** (Evidence: interviews, data, observations)
4. **How often and how painful?** (Frequency × intensity, quantified cost)
5. **What do people do today?** (Current workarounds, why they fail)
6. **Why hasn't this been solved?** (Barriers: technical, economic, organizational)
7. **What would success look like?** (Outcomes, not solutions)

### Problem Brief Template

```
PROBLEM BRIEF: [Descriptive Title]

EXECUTIVE SUMMARY (1 paragraph)
- One-sentence problem statement
- Who it affects and how often
- Current cost/impact
- Why it's urgent/important now

PROBLEM STATEMENT (1-2 pages)
- Detailed description of the problem
- Context and background
- Why this matters (business impact)

AFFECTED STAKEHOLDERS (1 page)
- Primary personas (who feels the pain directly)
- Secondary stakeholders (who's affected indirectly)
- Stakeholder map showing relationships

EVIDENCE (1-2 pages)
- Interview findings (key quotes, patterns)
- Observational data (workflow analysis)
- Quantitative data (frequency, cost, impact)
- Validation experiments (smoke tests, concierge results)

CURRENT STATE (1 page)
- How people solve this today
- Workarounds and their limitations
- Why current solutions are inadequate
- What's been tried and failed

IMPACT & URGENCY (1 page)
- Frequency × Intensity score
- Annual cost calculation
- Opportunity cost (what's not happening because of this problem)
- Urgency factors (why now vs. later)

SUCCESS CRITERIA (1 page)
- What outcomes would indicate the problem is solved
- How we'd measure success
- What wouldn't solve the problem (out of scope)

OPEN QUESTIONS & RISKS (1 page)
- What we still don't know
- Assumptions that need testing
- Risks and unknowns

RECOMMENDATION (1 paragraph)
- Should we pursue this? (Yes/No/More research)
- If yes: Suggested next steps
```

### Example: Terminal Truck Scheduling Problem Brief

**EXECUTIVE SUMMARY**

Terminal schedulers at petroleum distribution facilities struggle to prevent scheduling conflicts and minimize truck wait times when managing 20+ daily appointments. Customers book via phone/email without visibility into availability, resulting in 3-5 conflicts per day requiring 30 minutes each to resolve manually, costing an estimated $70,000 per terminal annually. With increasing truck volumes and driver shortages making wait times more expensive, solving this problem is critical for operational efficiency and customer satisfaction.

**PROBLEM STATEMENT**

Petroleum terminals operate as logistics hubs where product flows from pipelines and railcars to trucks for final delivery to customers. Efficient scheduling of truck loadouts is essential for minimizing wait times, maximizing throughput, and meeting customer service agreements.

Currently, scheduling is managed manually through a combination of phone calls, emails, whiteboards, and the scheduler's institutional knowledge. Customers call or email to request appointment times, and the scheduler must mentally track:
- Bay availability across multiple loading bays
- Product availability across multiple storage tanks
- Compatibility constraints (which products can be loaded where)
- Typical load times for different products and quantities
- Driver preferences and customer SLAs

This manual approach breaks down when:
- Multiple customers request the same time slot
- Customers show up without appointments
- Schedulers are unavailable (lunch, meetings, end of shift)
- Load times run longer than expected, cascading delays
- Product availability changes (unexpected pipeline receipts or outbound orders)

The result is scheduling conflicts that require reactive firefighting: calling customers to reschedule, negotiating priority, managing frustrated drivers waiting at the gate, and sometimes paying demurrage for excessive wait times.

**AFFECTED STAKEHOLDERS**

**Primary Personas:**

1. **Terminal Scheduler**
   - Manages all inbound appointment requests
   - Juggles competing demands and constraints
   - Held accountable for wait times and conflicts
   - Pain: Constant interruptions, stress from angry customers

2. **Truck Driver**
   - Relies on scheduled appointment times to plan routes
   - Earns per-load (wait time = lost income)
   - Faces pressure from dispatchers and customers
   - Pain: Unpredictable wait times, wasted hours

3. **Trucking Company Dispatcher**
   - Schedules drivers across multiple pickups/deliveries
   - Optimizes routes and driver utilization
   - Must meet customer delivery commitments
   - Pain: Schedule disruptions cascade across entire fleet

**Secondary Stakeholders:**

4. **Terminal Operations Manager**
   - Accountable for throughput and customer satisfaction
   - Tracks KPIs (average wait time, conflicts per day)
   - Pain: Poor metrics, customer complaints

5. **Customer (End Purchaser)**
   - Expects on-time delivery of product
   - May face penalties for late delivery to their customers
   - Pain: Unpredictability, unmet SLAs

6. **Loading Bay Operators**
   - Execute the physical loading process
   - Depend on accurate scheduling to prepare
   - Pain: Rush jobs, unclear priorities when conflicts occur

**EVIDENCE**

**Interview Findings (8 interviews conducted):**

*Terminal Scheduler, 15 years experience:*
> "I get probably 30 calls a day about scheduling. Half of them I have to tell 'that time's taken, how about X instead?' Then they call back later wanting to change it again. It's like playing Tetris all day while people yell at you."

*Truck Driver, local hauler:*
> "Some days I'm in and out in 30 minutes. Other days I sit for two hours. I never know which it'll be. I get paid by the load, not the hour, so waiting costs me money."

*Terminal Operations Manager:*
> "Our average wait time is 45 minutes, but that's misleading. 60% of trucks wait less than 30 minutes, but the other 40% wait over an hour. It's the conflicts that kill us. When three trucks show up at the same time, someone's going to wait."

**Observational Data:**

Shadowed terminal scheduler for one full day (8 AM - 5 PM):
- 27 phone calls about scheduling (avg 5 min each = 135 min)
- 18 emails (avg 3 min each = 54 min)
- 5 walk-ins from drivers at gate (avg 10 min = 50 min)
- **Total time on scheduling: 239 minutes (4 hours)**
- Actual scheduling conflicts resolved: 4
- Time per conflict: 30-40 minutes (calls, negotiations, updates)

**Quantitative Data:**

From 30-day analysis at one terminal:
- Average trucks per day: 22
- Scheduled appointments: 45% (rest are "show up and hope")
- Conflicts per day: 3.2 average
- Average wait time (all trucks): 47 minutes
- Wait time for scheduled trucks: 28 minutes
- Wait time for unscheduled trucks: 72 minutes

**Validation:**

Created simple Google Calendar shared scheduling system and offered to manage appointments for one terminal for 2 weeks:
- 75% of customers used the online booking vs. calling
- Conflicts dropped from 3.2 to 1.1 per day (66% reduction)
- Average wait time dropped from 47 to 32 minutes (32% reduction)
- Scheduler reported 2 hours per day time savings

**CURRENT STATE**

**Current Solutions & Limitations:**

**1. Whiteboard + Phone Log**
- Scheduler writes appointments on office whiteboard
- Maintains paper log of calls
- Limitation: Not accessible to customers or drivers; easily gets out of date; no conflict detection

**2. Personal Memory + Experience**
- Experienced schedulers "just know" how to juggle constraints
- Limitation: Knowledge not documented; doesn't scale; vulnerable when scheduler absent

**3. "First Come First Served" Default**
- When in doubt, honor order of calls/emails
- Limitation: Ignores business priorities, doesn't optimize for throughput

**4. Reactive Firefighting**
- When conflicts occur, call customers to negotiate changes
- Limitation: Wastes time, damages relationships, doesn't prevent future conflicts

**Previous Failed Attempts:**

**Shared Google Calendar (tried 2 years ago):**
- Scheduler created shared calendar, sent link to customers
- Customers didn't use it (still called to confirm)
- Scheduler abandoned it after 2 months
- Why it failed: No integration with operations; required double-entry; customers didn't trust it was real-time

**Terminal Management System (TMS) Scheduling Module:**
- Company purchased TMS with scheduling feature
- Scheduler tried using it for 1 week, gave up
- Why it failed: Too complex, required 15 minutes to enter an appointment; no customer-facing interface; didn't account for terminal-specific rules

**IMPACT & URGENCY**

**Frequency × Intensity Score:**
- Frequency: 5 (multiple scheduling conflicts daily)
- Intensity: 3 (30 min to resolve, customer frustration, occasional demurrage)
- **Total Score: 15 (High Priority)**

**Annual Cost Calculation:**

*Direct Costs:*
- Scheduler time: 4 hrs/day × $30/hr × 250 days = $30,000
- Driver wait time: 22 trucks/day × 19 min excess wait × $100/hr × 250 days = $173,250
- Demurrage payments: ~$5,000/year (occasional penalties)
- **Total Direct: $208,250 per terminal**

*Indirect Costs:*
- Customer dissatisfaction → lost business (hard to quantify)
- Scheduler stress → turnover costs (~$40k to replace and train)
- Suboptimal bay utilization → lost throughput (5-10%)

**Total Annual Impact: $250,000+ per terminal**

**Across 50 terminals in network: $12.5M annual problem**

**Urgency Factors:**

1. **Driver shortage:** Wait times increasingly expensive as drivers become scarce
2. **Customer expectations:** Competitors offering better visibility
3. **Volume growth:** 15% YoY increase in truck volumes straining manual system
4. **Compliance:** Increasing regulatory scrutiny on safety (overcrowded yards)

**SUCCESS CRITERIA**

**Problem would be considered "solved" if:**

1. **Conflicts reduced by 70%+** (from 3.2 to < 1.0 per day)
2. **Average wait time < 30 minutes** (from 47 minutes)
3. **Scheduler time on scheduling reduced by 50%+** (from 4 hrs to < 2 hrs per day)
4. **Customer adoption > 80%** (use new system vs. calling)
5. **Driver satisfaction improved** (measured via quarterly survey)

**What would NOT solve the problem:**

- Generic scheduling software that doesn't account for terminal constraints
- System that requires more work than current manual approach
- Solution that only works for scheduled appointments (doesn't handle walk-ins)
- Overly rigid system that doesn't allow scheduler overrides

**OPEN QUESTIONS & RISKS**

**Unknowns:**
- Will customers actually use self-service booking, or will they still call?
- How much customization needed per terminal (different constraints, rules)?
- Integration requirements with existing terminal management systems?
- What happens when bays run behind schedule (cascading updates)?

**Assumptions to Test:**
- Schedulers are willing to adopt new software (change management risk)
- Customers have internet access and basic tech literacy
- Terminal operations teams will trust automated system

**Risks:**
- Low adoption → Parallel systems → More work, not less
- Integration complexity → Project timeline extends
- Regional variability → Solution doesn't fit all terminals

**RECOMMENDATION**

**Proceed to Solution Design Phase**

The evidence strongly supports this is a high-value problem worth solving:
- Validated through interviews, observation, and manual prototype
- Quantified annual cost of $250k per terminal ($12.5M across network)
- Manual concierge test proved 66% conflict reduction is achievable
- Customer willingness to adopt validated (75% used online booking)

**Suggested Next Steps:**
1. Expand concierge test to 3 terminals for 60 days (validate scalability)
2. Document detailed requirements (workflows, constraints, integrations)
3. Evaluate build vs. buy options
4. Create implementation plan with phased rollout

**Estimated Investment:** $200k development + $50k annual maintenance
**Expected Payback:** < 3 months at scale

---

## Core Concept 3: Opportunity Document Structure

### Opportunity Doc vs. Problem Brief

**Problem Brief:** Answers "Is this problem worth solving?"

**Opportunity Doc:** Answers "Should we invest in solving this?"

**Additional elements in Opportunity Doc:**
- Market sizing (TAM, SAM, SOM)
- Competitive landscape
- Business model / monetization
- Go-to-market strategy
- Resource requirements
- Financial projections
- Risk assessment

### Opportunity Document Template

```
OPPORTUNITY DOCUMENT: [Title]

EXECUTIVE SUMMARY (1 page)
- The opportunity in one paragraph
- Market size and growth
- Competitive advantage
- Key metrics (TAM, revenue potential, ROI)
- Ask (budget, headcount, timeline)

PROBLEM & SOLUTION (2 pages)
- Problem description (from Problem Brief)
- Proposed solution approach (high-level)
- Why now? (timing factors)

MARKET ANALYSIS (2-3 pages)
- TAM (Total Addressable Market)
- SAM (Serviceable Addressable Market)
- SOM (Serviceable Obtainable Market)
- Market trends and growth drivers
- Customer segments

COMPETITIVE LANDSCAPE (1-2 pages)
- Direct competitors
- Indirect competitors / substitutes
- Our differentiation
- Competitive advantages / moats

BUSINESS MODEL (1-2 pages)
- Revenue streams
- Pricing strategy
- Unit economics
- Customer acquisition cost (CAC)
- Lifetime value (LTV)

GO-TO-MARKET STRATEGY (1-2 pages)
- Target customer segments (priority order)
- Distribution channels
- Marketing approach
- Sales strategy
- Partnership opportunities

RESOURCE REQUIREMENTS (1 page)
- Team (roles, headcount)
- Technology / infrastructure
- Budget (development, operations, marketing)
- Timeline

FINANCIAL PROJECTIONS (1-2 pages)
- Revenue forecast (3-5 years)
- Cost structure
- Path to profitability
- Key assumptions

RISKS & MITIGATION (1 page)
- Technical risks
- Market risks
- Competitive risks
- Execution risks
- Mitigation strategies for each

DECISION FRAMEWORK (1 page)
- Go / No-go criteria
- Success metrics
- Milestones & checkpoints
- What would cause us to pivot or kill

RECOMMENDATION & ASK (1 paragraph)
- Clear recommendation
- Specific resource request
- Expected outcomes
```

---

## Core Concept 4: Market Sizing Frameworks

### TAM, SAM, SOM Explained

**TAM (Total Addressable Market):**
- The total market demand if you had 100% market share
- Usually calculated globally or at national level
- "If everyone who could use this, did use it"

**SAM (Serviceable Addressable Market):**
- The portion of TAM you can actually serve (geographic, technical, or regulatory constraints)
- "Who we can realistically reach with our distribution"

**SOM (Serviceable Obtainable Market):**
- The portion of SAM you can realistically capture in near term (3-5 years)
- "Who we can win given competition, resources, and execution"

### Example: Terminal Scheduling Software

**TAM Calculation (Bottom-Up):**
```
Petroleum terminals in North America: 1,500
Average price per terminal: $10,000/year
TAM = 1,500 × $10,000 = $15M annual recurring revenue

(This is conservative — includes only petroleum, not chemical, grain, etc.)
```

**SAM Calculation:**
```
Terminals we can serve (exclude very small < 5 trucks/day): 800
Average price: $10,000/year
SAM = 800 × $10,000 = $8M ARR
```

**SOM Calculation (Year 3):**
```
Realistic market share in Year 3: 10% of SAM
SOM = 80 terminals × $10,000 = $800k ARR

(Assumes we're one of several competitors, not dominant player)
```

### Top-Down vs. Bottom-Up

**Top-Down (Analyst Reports):**
"The global logistics software market is $12B and growing at 8% annually..."

**Problem:** Usually too broad, not specific to your solution

**Bottom-Up (First Principles):**
"There are 1,500 terminals, each with 20 trucks/day, at $X per transaction..."

**Better:** More credible, shows you understand the market deeply

---

## Core Concept 5: Communicating to Different Audiences

### Stakeholder Communication Matrix

| Stakeholder | What They Care About | Document Focus |
|-------------|----------------------|----------------|
| **Engineers** | Is this technically interesting? | Problem complexity, technical challenges |
| **Product Managers** | Does this fit our strategy? | User needs, competitive positioning |
| **Executives** | What's the ROI? | Market size, revenue potential, costs |
| **Finance** | Do the numbers make sense? | Unit economics, payback period, risk |
| **Customers** | Does this solve my problem? | Outcomes, ease of use, pricing |
| **Sales** | Can I sell this? | Differentiation, pricing, objection handling |

### Tailoring Your Document

**For Engineers (Technical Audience):**
- Lead with technical challenge and interesting problems
- Show how solution pushes boundaries
- Emphasize learning opportunities

**For Business Stakeholders:**
- Lead with market opportunity and financial upside
- Emphasize competitive positioning
- Show clear path to revenue

**For Executives:**
- Lead with one-page executive summary
- Focus on strategic fit and ROI
- Make the ask crystal clear

**For Mixed Audiences:**
- Use layered structure (exec summary → details)
- Provide "skip to" guidance ("If you want technical details, see Appendix B")
- Use visuals to communicate quickly

---

## Practical Framework: Writing Process

### Step-by-Step Guide

**1. Start with Research (Don't Write Yet)**

Gather:
- Interview notes
- Observation logs
- Quantitative data
- Validation experiment results
- Competitive research
- Market data

**2. Create Outline (Skeleton First)**

Use templates from this week, but adapt:
- What sections does YOUR audience need?
- What questions must you answer?
- What would make this compelling?

**3. Write Rough Draft (Fast and Messy)**

- Get everything down quickly
- Don't self-edit yet
- Use bullet points, fragments
- Mark [TODO: research X] for gaps

**4. Fill Gaps (Research the Unknowns)**

- Go back and research the [TODOs]
- Run additional calculations
- Conduct follow-up interviews if needed

**5. Refine and Structure**

- Turn bullets into paragraphs
- Add transitions between sections
- Ensure logical flow
- Check that each section answers its question

**6. Add Visuals**

- Workflow diagrams
- Market size charts
- Financial projections
- Comparison tables

**7. Write Executive Summary (Last)**

- Synthesize entire document into 1 page
- Can be read standalone
- Includes key numbers and recommendation

**8. Get Feedback**

- Share with trusted colleague
- Ask: "What's unclear? What's missing? What's unconvincing?"
- Revise based on feedback

**9. Polish**

- Spell check
- Formatting consistency
- Professional appearance
- Clear headings and navigation

---

## PROJECT 2: Problem Discovery Brief

**This is your capstone for Course 2 (Product Discovery & Problem Identification).**

You'll produce a comprehensive Problem Discovery Brief that demonstrates mastery of Weeks 5-8.

### Project Requirements

**Choose ONE problem from a domain you know deeply:**
- Use your operational experience (petroleum, logistics, energy, supply chain)
- Or choose a domain where you have access to users/stakeholders
- Must be a real problem, not hypothetical

### Deliverables (2,000–3,000 words total)

#### 1. Executive Summary (1 page)

- One-sentence problem statement
- Who it affects, how often, and impact
- Key evidence (interviews, observations, data)
- Recommendation (pursue / more research / pass)

#### 2. Problem Statement (1-2 pages)

- Detailed description using Week 5 framework
- Context and background
- Why this matters
- When and where it occurs

#### 3. Affected Personas (1 page)

- 3-5 personas identified
- For each: role, goals, how they experience the problem
- Stakeholder map showing relationships

#### 4. Evidence (1-2 pages)

**From Week 6 work:**
- Interview findings (3-5 interviews minimum)
- Key quotes (most compelling)
- Observational data (if you shadowed)
- Patterns across interviews

**From Week 7 work:**
- Validation experiments conducted
- Results (signups, usage, willingness to pay)
- What you learned

#### 5. Current Workarounds (1 page)

- How people solve this today
- Why current solutions are inadequate
- Cost of current approach
- What's been tried and failed

#### 6. Problem Severity & Impact (1 page)

- Frequency × Intensity score
- Annual cost calculation (show your math)
- Quantified impact on business/users
- Opportunity cost

#### 7. Success Criteria (1 page)

- What would "solved" look like?
- How would we measure success?
- What wouldn't solve the problem?

#### 8. Open Questions & Risks (1 page)

- What we still don't know
- Assumptions requiring validation
- Risks if we proceed
- Risks if we don't

#### 9. Recommendation & Next Steps (1 paragraph)

- Should we pursue this? Why or why not?
- If yes: What are the next steps?
- If no: What would change your mind?

### Quality Standards

**This is a portfolio piece. Make it professional:**

- ✅ Clear, concise writing (no jargon without explanation)
- ✅ Proper structure (headings, sections, flow)
- ✅ Visual elements (diagrams, tables, charts where helpful)
- ✅ Citations (reference interviews, data sources)
- ✅ Spell-checked and proofread
- ✅ GitHub-ready (markdown or PDF)
- ✅ Table of contents (for easy navigation)

### Evaluation Criteria

Your project demonstrates:

- ✅ **Deep problem understanding** (not surface-level)
- ✅ **Strong evidence** (interviews + observation + validation)
- ✅ **Clear communication** (executive could read and understand)
- ✅ **Quantified impact** (specific numbers, not vague claims)
- ✅ **Honest assessment** (acknowledges unknowns and risks)
- ✅ **Professional quality** (portfolio-ready)

### Example Projects

**Strong project ideas:**
- Terminal truck scheduling inefficiency
- Healthcare patient handoff communication breakdown
- Software deployment pipeline bottlenecks
- Supply chain visibility gaps
- Manufacturing quality control pain points

**Weak project ideas (avoid):**
- "Users want better dashboards" (too vague)
- "We need to modernize our tech stack" (solution, not problem)
- "AI could improve X" (technology in search of problem)

---

## Reflection Questions

1. **Past proposals:** Think of a time you proposed an idea (feature, project, tool). How did you communicate it? Would you do it differently now?

2. **Rejected ideas:** Have you had good ideas rejected? Was it the idea, or how you communicated it? What document would have helped?

3. **Reading documents:** When you read opportunity docs or proposals, what makes them compelling? What makes them weak?

4. **Your domain:** If you were pitching an investment committee on solving a problem in your domain, what would you emphasize? Market size? Pain severity? Competitive advantage?

5. **Portfolio:** What does PROJECT 2 demonstrate about you as an engineer? How is this different from showing code on GitHub?

---

## Key Takeaways

1. **Documents force clear thinking:** Writing exposes gaps in your understanding
2. **Different audiences need different emphasis:** Engineers care about complexity, executives care about ROI
3. **Quantification is credibility:** "Saves time" is weak; "Saves 45 min/day for 200 people = $1.4M annually" is strong
4. **Evidence beats opinion:** Quotes, data, and validation results are persuasive
5. **Problem Briefs come before solutions:** Validate the problem is worth solving before designing the solution
6. **Opportunity Docs connect to business:** Market size, revenue potential, and competitive landscape matter
7. **Professional communication advances careers:** Engineers who write well get promoted faster

**Course 2 Complete!** You now know how to:
- Identify real problems (vs. feature requests)
- Interview and observe to uncover hidden pain
- Validate problems before building
- Communicate opportunities that get funded

**Next:** Course 3 begins — **Requirements Engineering & Domain Understanding** (Weeks 9-13)

---

## Additional Resources

**Books:**
- *The Pyramid Principle* by Barbara Minto (structured communication)
- *Made to Stick* by Chip & Dan Heath (compelling communication)
- *Crossing the Chasm* by Geoffrey Moore (go-to-market strategy)

**Templates:**
- Amazon "Working Backwards" press release + FAQ
- Y Combinator application (concise opportunity framing)
- Sequoia Capital pitch deck template

**Examples:**
- Stripe's early pitch deck (problem → solution → market)
- Airbnb's original investor deck (storytelling + data)
- Buffer's transparent revenue dashboard

**Practice:**
- Write one-page briefs for 5 problems you observe this month
- Review 10 startup pitch decks — what makes them compelling?
- Get feedback on your writing from non-technical colleagues

---

*The best engineers aren't just problem solvers — they're problem identifiers and communicators. Master this, and you'll shape what gets built, not just how.*

**End of Week 8 — End of Course 2: Product Discovery & Problem Identification**

---

## 🎓 Course 2 Complete

You've now mastered product discovery and problem identification. You can:
- Distinguish real problems from feature requests
- Interview and observe to uncover unstated needs
- Validate problems before writing code
- Communicate opportunities that attract investment

**PROJECT 2** is your capstone. Spend 10-20 hours on it. Combine everything from Weeks 5-8. Make it portfolio-quality that shows business acumen, user research skills, and analytical thinking.

**When ready, proceed to Course 3: Requirements Engineering & Domain Understanding (Weeks 9-13).**
