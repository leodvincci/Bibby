# Week 4: Problem Discovery (Business Lens)

**Semester 1: Markets, Problems & Value**
**Estimated Time**: 8-10 hours
**Mentor Voice**: Engineering Manager Mentor

---

## Opening: The Problem with Solutions

Most engineers build solutions looking for problems.

You have a great idea. You spend 3 months building it. You launch. Crickets.

Why? Because you fell in love with your solution before validating the problem.

Here's the uncomfortable truth: **Most problems you think exist don't actually exist.** Or they exist, but nobody cares enough to pay to solve them.

Consider:
- **You think**: "Librarians need better cataloging tools"
- **Reality**: Some do, some don't. Which ones? How badly? What would they pay?

- **You think**: "Pipeline operators need real-time monitoring dashboards"
- **Reality**: Maybe. Or maybe they need better shift handoffs. Or predictive maintenance. Or none of the above.

**The only way to know is to ask.** Not to pitch. Not to demo. Just to listen.

This week, you'll learn:
- How to discover problems worth solving
- How to interview customers without biasing them
- How to distinguish real pain from nice-to-haves
- How to validate that people will actually pay

---

## The Jobs-to-be-Done Framework

### What People Actually "Hire" Products to Do

When someone "buys" your product, they're not buying features. They're **hiring** it to do a job.

**Clayton Christensen's Milkshake Example**:

A fast-food chain wanted to sell more milkshakes. They asked:
- "How can we improve the milkshake?" (Wrong question)

Instead, they asked:
- "What job are customers hiring the milkshake to do?" (Right question)

**Findings**:
- **Morning commuters** hired milkshakes to:
  - Keep them full until lunch
  - Be easy to consume while driving
  - Make boring commute more interesting
  - Competitors: Bananas (too quick), donuts (crumbs), coffee (not filling)

- **Parents** hired milkshakes to:
  - Reward kids for good behavior
  - Keep kids occupied/quiet
  - Competitors: Toys, candy

**Same product. Different jobs. Different competition. Different improvements needed.**

---

### Applying Jobs-to-be-Done to Software

**Bad question**: "What features do you want in library management software?"

**Good question**: "When you're trying to [specific task], what are you actually trying to accomplish, and what gets in your way?"

---

**Example: Bibby**

**Bad interview**:
> You: "Would you use a CLI for library management?"
> Librarian: "Uh... maybe? I don't really know what a CLI is."
> You: "Let me show you!" [proceeds to demo]

**You've learned nothing. You pitched, didn't discover.**

**Good interview**:
> You: "Tell me about the last time you needed to find a book in your library."
> Librarian: "Yesterday, a student asked for a specific book. I thought we had it, but couldn't find it on the shelf."
> You: "What did you do?"
> Librarian: "I checked our spreadsheet. It said we had it. So I looked again. Eventually found it mis-shelved."
> You: "How long did that take?"
> Librarian: "About 15 minutes. Happens a few times a week."
> You: "What's the cost of that time?"
> Librarian: "Student leaves frustrated. I waste time. Sometimes I just tell them we don't have it."

**Now you're learning:**
- Job to be done: "Find a specific book quickly and reliably"
- Current solution: Spreadsheet (but unreliable)
- Pain: 15 minutes wasted, multiple times per week
- Consequence: Student frustration, inefficiency
- Workaround: Give up and say "we don't have it"

**This is a real problem.**

---

### The Jobs-to-be-Done Statement

Format:
```
When I ___[situation]___,
I want to ___[motivation]___,
So I can ___[expected outcome]___.
```

**Bibby Examples**:

**Job #1**:
```
When a student requests a specific book,
I want to immediately know if we have it and where it is,
So I can help them in under 1 minute instead of 15.
```

**Job #2**:
```
When I'm buying new books for the library,
I want to know which topics students actually want to read,
So I can allocate my limited budget to books that will be used.
```

**Job #3**:
```
When a book is overdue,
I want to automatically remind the borrower,
So I don't have to manually track and send reminders.
```

**Notice**: These are NOT feature requests. They're situations, motivations, and outcomes.

---

### From Jobs to Solutions

Once you understand the job, you can design solutions:

**Job**: "Find a specific book quickly and reliably"

**Possible Solutions**:
1. Better spreadsheet with search (incremental improvement)
2. Barcode scanning + database (Bibby's approach)
3. RFID tags + location tracking (expensive, over-engineered)
4. AI chatbot that asks questions to narrow search (interesting, might work)
5. Student self-service kiosk (removes librarian bottleneck)

**The job doesn't prescribe the solution.** Multiple solutions can address the same job.

**Your advantage**: Understanding the job deeply lets you design the RIGHT solution, not just A solution.

---

## Problem Space vs Solution Space

### The Critical Distinction

**Problem Space**: The world of customer needs, pains, goals, and constraints

**Solution Space**: The world of features, technologies, architectures, and implementations

```
┌────────────────────────────────────────────────────┐
│                 PROBLEM SPACE                       │
│  "Students can't find books quickly"               │
│  "Librarians waste 5 hours/week searching"         │
│  "Lost books cost $500/year"                       │
└────────────────────────────────────────────────────┘
                      │
                      │ (Discovery)
                      │
                      ▼
┌────────────────────────────────────────────────────┐
│                SOLUTION SPACE                       │
│  "CLI for book management"                         │
│  "Barcode scanning"                                │
│  "PostgreSQL backend"                              │
└────────────────────────────────────────────────────┘
```

**Common Mistake**: Engineers jump to solution space immediately.

**Example**:
- Librarian: "Finding books takes forever"
- Engineer: "I'll build a CLI with search!" ← jumped to solution
- Better: "Tell me more about when this happens and what you've tried"

---

### Staying in Problem Space

**Techniques**:

1. **The 5 Whys**

```
Problem: "Book checkout takes too long"

Why? "I have to write it down manually"
Why? "Our system doesn't track checkouts"
Why? "We're using a spreadsheet for inventory, not circulation"
Why? "We can't afford library management software"
Why? "The cheap options don't work, expensive ones are too complex"

Root cause: Market gap for mid-priced, simple library management
```

2. **Ask About the Last Time**

Generic: "Do you have trouble finding books?"
→ Answer: "Yeah, sometimes" (not actionable)

Specific: "Tell me about the last time you couldn't find a book"
→ Answer: Detailed story with context, emotions, workarounds

3. **Watch Them Work**

Don't ask: "How do you check out books?"
Do: "Can I watch you check out this book to a student?"

**Observation reveals**:
- Steps they didn't mention
- Workarounds they've forgotten they do
- Inefficiencies they've normalized
- Pain points they didn't articulate

---

## Customer Interview Techniques

### The Mom Test

**From Rob Fitzpatrick's "The Mom Test"**:

**Rule**: Talk about their life, not your idea.

**Bad Questions** (get false positives):
- "Would you use a tool that helps you find books faster?" (Yes = politeness, not commitment)
- "Do you think this is a good idea?" (Your mom will say yes)
- "How much would you pay for this?" (Hypothetical, not real)

**Good Questions** (get truth):
- "Tell me about the last time you couldn't find a book"
- "How much time do you spend on cataloging each week?"
- "What have you tried to solve this problem?"
- "How much does this problem cost you?"

---

### The Customer Interview Script

**Structure**:
1. Opening: Set context, get permission
2. Past Behavior: Ask about real experiences
3. Current Solutions: Understand what they do now
4. Pain Quantification: How bad is the problem?
5. Purchasing Process: Who decides, who pays, what's the process?
6. Close: Next steps, ask for intros

---

**Example Interview for Bibby**:

**Opening**:
> "Thanks for talking with me. I'm researching how small libraries manage their collections. I'm NOT trying to sell you anything today — I just want to understand your workflow. Is 20 minutes okay?"

**Past Behavior**:
> "Tell me about the last time a student asked for a book you couldn't immediately find."
> → Listen. Take notes. Don't interrupt.

**Follow-up probes**:
> "How often does that happen?"
> "What did you do?"
> "How long did it take?"
> "How did the student react?"

**Current Solutions**:
> "How do you currently keep track of your books?"
> "What do you like about that system?"
> "What frustrates you about it?"
> "Have you tried other approaches?"

**Pain Quantification**:
> "Roughly how much time per week do you spend on cataloging and tracking?"
> "What would you do with that time if you got it back?"
> "Have you lost any books in the past year? How many? What's the cost?"

**Purchasing Process**:
> "If you decided you needed a better system, what would the process be?"
> "Who would need to approve it?"
> "What's your budget for something like this?"
> "How do you typically evaluate new tools?"

**Close**:
> "This has been really helpful. Can I follow up if I have more questions?"
> "Do you know other librarians I should talk to?"
> "Would you be interested in seeing what I build, even if it's early/rough?"

---

### What You're Listening For

✅ **Good Signals**:
- Specific stories with details
- Emotion (frustration, fear, excitement)
- Current workarounds (they're trying to solve it themselves)
- Mentions money/time costs
- Offers to introduce you to others
- Asks when they can see what you build

⚠️ **Bad Signals**:
- Vague, hypothetical responses
- "That sounds interesting" (polite dismissal)
- "I'd probably use that" (probably = no)
- Talking about the future, not the past
- No clear pain point
- No current attempt to solve

---

## Validating Pain Points

Not all problems are worth solving. You need to distinguish:
- **Hair on fire** (urgent, will pay immediately)
- **Annoying** (aware but not urgent, might pay someday)
- **Nice to have** (would be cool but won't pay)

### The Pain Pyramid

```
        ┌─────────────────┐
        │  HAIR ON FIRE   │ ← Build for these
        │   (Top 10%)     │
        ├─────────────────┤
        │   ANNOYING      │ ← Maybe later
        │   (Middle 30%)  │
        ├─────────────────┤
        │  NICE TO HAVE   │ ← Ignore these
        │   (Bottom 60%)  │
        └─────────────────┘
```

**Hair on Fire Indicators**:
- They're currently paying for a solution (even if bad)
- They've built a workaround themselves
- They mention it unprompted multiple times
- They ask "when can I get this?" before you pitch
- They offer to pay for early access

**Annoying Indicators**:
- They complain when asked but haven't tried to solve it
- "Yeah, that's a pain" but no urgency
- They use free tools and won't upgrade
- Long sales cycles, lots of "let me think about it"

**Nice to Have Indicators**:
- "That would be cool!"
- Hypothetical: "I would use that if..."
- Feature requests unconnected to real pain
- No current solution (because problem isn't real)

---

### The Validation Ladder

**Level 1: Hypothesis**
- "I think librarians struggle with book tracking"

**Level 2: Interview Validation**
- Talked to 10 librarians, 7 mentioned tracking as a top-3 pain point

**Level 3: Observational Validation**
- Watched 3 librarians work, saw them spend 30+ mins searching for books

**Level 4: Behavioral Validation**
- 5 librarians signed up for email updates when tool is ready

**Level 5: Financial Validation**
- 2 librarians pre-paid $50 for first 3 months access

**Each level increases confidence. Most builders stop at Level 2. Winners get to Level 5.**

---

## Exercise 1: Conduct 5 Customer Interviews

**Your Mission**: Interview 5 people in your target domain

**Domains to Choose From**:
1. Small library management (Bibby)
2. Industrial automation (your Kinder Morgan domain)
3. Developer tools (if that's your focus)

---

### Interview Prep

**Before Interviews**:

1. **Identify Target Persona**:
   - Bibby: Small library librarians, school librarians, personal library owners
   - Industrial: Pipeline operators, maintenance schedulers, safety compliance officers
   - Dev Tools: Backend engineers, DevOps engineers, system architects

2. **Write Hypothesis** (What problem do you think exists?):
   ```
   I believe that [persona]
   struggles with [problem]
   when [situation]
   because [root cause].

   This causes [consequence]
   which costs approximately [time/money].
   ```

3. **Draft Interview Questions** (10-15 questions):
   - 3-4 about their role and responsibilities
   - 5-7 about specific pain points
   - 2-3 about current solutions
   - 1-2 about purchasing process

---

### Conducting Interviews

**Logistics**:
- 20-30 minutes per interview
- Video call or in-person (better than phone)
- Record if they permit (for review later)
- Take notes even if recording

**During Interview**:
- Listen 80%, talk 20%
- Ask follow-up questions ("Tell me more about that")
- Probe for specifics ("How many times?" "How long?" "What did it cost?")
- DON'T pitch your solution
- DON'T ask leading questions
- DO ask about the past, not hypothetical future

**After Interview**:
- Write up notes within 24 hours
- Extract quotes (especially emotional ones)
- Rate pain level (hair on fire / annoying / nice to have)
- Identify patterns across interviews

---

### Deliverable: Interview Summary Report

For each interview, document:

```markdown
## Interview #[N] - [Date]

**Interviewee**: [Title/Role, not name for privacy]
**Duration**: [minutes]
**Channel**: [Video/Phone/In-person]

### Key Insights
- Main pain point:
- Current solution:
- Pain intensity (1-10):
- Willingness to pay signals:

### Notable Quotes
> "[Exact quote showing emotion or insight]"

### Jobs to be Done (Identified)
1. When [situation], they want [motivation], so they can [outcome]
2. ...

### Questions for Follow-up
- [ ] Question to validate in next interview
- [ ] Clarification needed

### Pain Level Assessment
[Hair on Fire / Annoying / Nice to Have]

Reasoning: [Why you rated it this way]
```

**After 5 interviews, write synthesis**:

```markdown
## Synthesis Across 5 Interviews

### Patterns Identified
1. [Problem that 3+ people mentioned]
2. [Common workaround across multiple interviews]
3. [Shared consequence/cost]

### Most Validated Pain Points
1. [Top pain, with evidence]
2. [Second pain]
3. [Third pain]

### Jobs to be Done (Prioritized)
1. [Most common/intense job]
2. ...

### Persona Refinement
Based on interviews, our target persona is:
- Role: [specific title]
- Responsibilities: [what they do daily]
- Pain points: [ranked]
- Budget authority: [Yes/No, who decides]
- Current solutions: [what they use now]
- Willingness to pay: [signals observed]

### Next Steps
- [ ] Validate [specific hypothesis] with more interviews
- [ ] Test [specific solution concept] with 2-3 interviewees
- [ ] Talk to [adjacent persona] to compare pain points
```

---

## Exercise 2: Problem Validation Matrix

Create a matrix evaluating potential problems to solve:

```markdown
| Problem | Frequency | Intensity | Current Solution | WTP | Score |
|---------|-----------|-----------|------------------|-----|-------|
| Book tracking | Daily | High (8/10) | Spreadsheet | $50/mo | 32 |
| Cataloging new books | Weekly | Medium (5/10) | Manual entry | $20/mo | 10 |
| Overdue notices | Daily | Low (3/10) | Email manually | $10/mo | 3 |
```

**Scoring Formula**:
```
Score = Frequency × Intensity × WTP

Where:
- Frequency: Daily=4, Weekly=3, Monthly=2, Quarterly=1
- Intensity: 1-10 (how painful?)
- WTP: Willingness to Pay (monthly, in dollars)
```

**Analysis**:
- Focus on highest scoring problems
- These have frequency + intensity + financial validation
- Ignore low scorers (nice-to-haves)

---

## Exercise 3: Jobs-to-be-Done for Bibby

Map all jobs users might hire Bibby to do:

### Library Staff Jobs

**Job Category: Finding Books**
1. When a student asks for a specific book, I want to locate it in under 1 minute, so they don't leave frustrated
2. When I'm looking for books on a topic, I want to browse by genre/subject, so I can make recommendations
3. When a book is reported missing, I want to see last checkout, so I can follow up

**Job Category: Circulation**
1. When a student wants to borrow a book, I want to check it out in 10 seconds, so there's no line
2. When a book is overdue, I want automated reminders sent, so I don't have to manually track
3. When a book comes back, I want to check it in and immediately make it available, so the next student can find it

**Job Category: Collection Management**
1. When buying new books, I want to see checkout data, so I know what students actually want
2. When a book is damaged, I want to mark it for repair/removal, so students don't check out unusable books
3. When doing inventory, I want to scan barcodes and find discrepancies, so I can order replacements

### Administrative Jobs

**Job Category: Reporting**
1. When administration asks for metrics, I want to generate reports in 5 minutes, so I don't waste hours on spreadsheets
2. When planning budget, I want to see collection value and turnover, so I can justify funding

**Job Category: Compliance**
1. When an audit happens, I want complete records, so I can show accountability
2. When a book is challenged/banned, I want to track circulation history, so I can provide data to board

---

### Prioritization

For each job:
1. **How many users have this job?** (All/Most/Some/Few)
2. **How frequently?** (Daily/Weekly/Monthly/Rarely)
3. **How painful is current solution?** (1-10)
4. **What do they pay for current solution?** ($)

**Deliverable**: Prioritized list of 5-10 jobs worth solving

---

## Industrial Domain Deep-Dive

Let's apply this to **your domain expertise** (energy, logistics, industrial automation).

### Example: Pipeline Operations (Kinder Morgan Context)

**Hypothesis**: Pipeline operators struggle with shift handoffs

**Jobs to be Done (Hypothetical - validate with interviews)**:

**Job #1: Shift Handoff**
```
When my shift is ending and the next operator arrives,
I want to communicate current status, anomalies, and action items,
So the next operator knows exactly what's happening without missing critical info.
```

**Current Solution**:
- Paper logbook
- Verbal brief
- Maybe email

**Pains**:
- Information gets lost
- Handwriting illegible
- Critical items buried in noise
- No easy way to track if action items completed
- Auditors want digital records

**Validation Questions**:
- "Tell me about the last shift handoff that went wrong"
- "How much time do you spend on handoff documentation?"
- "Have you ever missed something critical in a handoff?"
- "What's the consequence of a bad handoff?"

**Potential Solution** (ONLY after validating problem):
- Digital shift log app
- Structured templates
- Automatic carry-forward of open items
- Mobile-first (operators on floor, not at desk)
- Audit trail for compliance

---

**Job #2: Equipment Maintenance Scheduling**
```
When planning maintenance for next week,
I want to see equipment usage patterns and predicted failures,
So I can schedule work when it minimizes disruption and prevents unplanned downtime.
```

**Current Solution**:
- Calendar-based (every X weeks)
- Reactive (fix when it breaks)
- Excel spreadsheet

**Pains**:
- Over-maintain (wasting money on unnecessary work)
- Under-maintain (equipment fails unexpectedly, costly)
- Can't optimize for operational windows
- No visibility into actual equipment health

**Validation Questions**:
- "Walk me through how you decide when to schedule maintenance"
- "Tell me about a time equipment failed unexpectedly"
- "What did that unplanned downtime cost?"
- "How do you balance maintenance frequency vs operational needs?"

**Potential Solution**:
- Predictive maintenance dashboard
- Integration with sensor data
- Machine learning for failure prediction
- Scheduler that knows operational constraints

---

### Exercise: Industrial Problem Discovery

Pick ONE system/process from your experience:
- Shift operations
- Equipment maintenance
- Safety compliance
- Logistics coordination
- Inventory management

**Step 1: Write Hypotheses**

```
Hypothesis #1:
I believe that [specific role]
struggles with [specific problem]
when [specific situation]
because [root cause].

This costs approximately [time/money/risk].
```

Write 3-5 hypotheses.

**Step 2: Design Interview**

- Who would you interview? (Be specific: "Day shift pipeline operators" not "workers")
- What questions would you ask?
- How would you observe their work?
- What would validate vs invalidate your hypothesis?

**Step 3: Predict Validation**

For each hypothesis:
- **If validated**: What would you build?
- **If invalidated**: What would you learn?
- **If mixed results**: How would you refine?

**Deliverable**: 3-5 hypotheses + interview plan + validation criteria

---

## Problem vs Feature Requests

### The Trap of Feature Requests

**Customer says**: "I need a dark mode toggle"

**Bad response**: "Okay, I'll add dark mode" ← Taking orders

**Good response**: "Tell me more about when you use the app and what environment you're in"

**Discovery**:
- They work night shifts
- Bright screens cause eye strain
- Reduces productivity
- Security concern (bright screen visible from outside)

**Real problem**: Eye strain and security during night shifts

**Possible solutions**:
- Dark mode (their suggestion)
- Dimmer slider (simpler)
- Auto dark mode based on time (better)
- E-ink display option (overengineered)
- Redesign for lower contrast (different approach)

**Understanding the problem opens up solution space.**

---

### Converting Features to Jobs

**Feature request**: "I want barcode scanning"

**Job to be done**:
```
When I'm adding a new book to the library,
I want to automatically populate title, author, ISBN, etc.,
So I don't spend 5 minutes manually typing metadata.
```

**Alternative solutions**:
- Barcode scanning (their suggestion)
- ISBN lookup API (easier to implement first)
- Bulk CSV import (for initial catalog)
- OCR from book cover photo (future)

---

### From Bibby Feature Requests to Jobs

Let's reverse-engineer Bibby's features into jobs:

**Feature**: `book check-out`

**Underlying Job**:
```
When a student wants to borrow a book,
I want to record who has it and when it's due,
So I can track inventory and send reminders.
```

**Feature**: `browse bookcases`

**Underlying Job**:
```
When I remember a book is on a specific shelf but forget the title,
I want to browse that physical location virtually,
So I can identify the book without walking to the shelf.
```

**Feature**: Multi-author support

**Underlying Job**:
```
When adding books to the catalog,
I want to accurately attribute authorship,
So students can find books by any contributor.
```

**Exercise**: For each Bibby feature, write the underlying job. Are there better solutions to those jobs?

---

## Deliverable: Problem Discovery Report

Synthesize your interviews and analysis into a comprehensive report:

### Structure

**1. Executive Summary** (250 words)
- Domain/Market
- Problem investigated
- Key findings
- Validation level
- Recommendation (build / pivot / abandon)

**2. Research Methodology**
- Interviews conducted (#, personas, duration)
- Observation (if any)
- Other research (competitor analysis, market data)

**3. Problem Statement**
```
[Specific persona]
struggles with [specific problem]
when [specific situation]
which costs [quantified impact].

Current solutions include [alternatives],
but they fail because [why current solutions inadequate].
```

**4. Jobs to be Done** (Top 5, ranked)
For each job:
- JTBD statement
- Frequency (how often this job arises)
- Current solutions (what they use today)
- Pain level (1-10, with evidence)
- Willingness to pay signals

**5. Evidence** (Quotes & Data)
- 5-10 quotes from interviews showing pain
- Quantified costs (time, money, risk)
- Frequency data
- Market size implications

**6. Persona Detail**
- Role and responsibilities
- Technical sophistication
- Budget authority
- Decision-making process
- Purchasing behavior

**7. Validation Level**
- Interview validation: [# of people, consistency]
- Observational validation: [what you saw]
- Behavioral validation: [what they did, not just said]
- Financial validation: [any pre-commitments]

**8. Proposed Solution** (High-Level Only)
- Core job(s) to solve
- Differentiation from current solutions
- Why now? (timing, market shifts, technology enablers)
- Why you? (unfair advantages)

**9. Next Steps**
- Additional validation needed
- Prototype/MVP scope
- Success metrics
- Timeline

**Format**: 2,000-3,000 words, markdown, with appendix of interview notes

---

## Code Integration: Building for Validated Problems

Let's look at Bibby through the lens of validated problems:

### From `BookCommands.java:333-378` — Search Flow

```java
public void searchByTitle() throws InterruptedException {
    System.out.println("\n");
    String title;
    ComponentFlow flow;
    flow = componentFlowBuilder.clone()
            .withStringInput("bookTitle")
            .name("Enter book title:_")
            .and().build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    title = result.getContext().get("bookTitle",String.class);
    System.out.println("\u001B[36m</>\u001B[0m:Hold on, I'm diving into the stacks — Let's see if I can find " + title);

    // ... search logic ...

    BookEntity bookEntity = bookService.findBookByTitle(title);

    if (bookEntity == null) {
        System.out.println("\n\u001B[36m</>\u001B[0m:I just flipped through every shelf — no luck this time.\n");
    } else if(bookEntity.getShelfId() == null){
        System.out.println("\nBook Was Found Without a Location\n");
    } else {
        Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
        Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
        System.out.println("\nBook Was Found \nBookcase: " + bookcaseEntity.get().getBookcaseLabel() + "\nShelf: " + shelfEntity.get().getShelfLabel() + "\n");
    }
}
```

**Job This Solves**:
```
When a student asks for a specific book,
I want to know immediately if we have it and where it is,
So I can help them in under 1 minute instead of wandering shelves for 15 minutes.
```

**Validated Pain**: YES (assuming interviews confirmed this)

**But consider**:
- What if interviews revealed the REAL pain is "students don't know what book they want, they just know the topic"?
- Then you need better browsing/recommendation, not just title search

**Alternative Jobs This Could Solve**:
1. "Find books when I only remember part of the title"
2. "See if a book is available before walking to the shelf"
3. "Help a student who mispronounced the title"

**Each job might need different implementation**:
- Job 1 → Fuzzy search
- Job 2 → Availability status (checked out vs on shelf)
- Job 3 → Phonetic matching or "did you mean?"

**The code you write should map to validated jobs, not assumed needs.**

---

## Reflection Questions

Spend 30-45 minutes journaling:

1. **Think about Bibby.** What problem did you assume it solved? What if you interviewed librarians and discovered they care more about budget planning than cataloging? Would you pivot?

2. **In your Kinder Morgan experience**, what did management THINK operators needed vs what operators ACTUALLY needed? How do you know the difference without asking?

3. **Most engineers hate customer interviews.** Why? Is it fear of invalidation? Preferring to build over talking? How can you make interviews energizing rather than draining?

4. **You've now learned** value creation (Week 1), markets (Week 2), psychology (Week 3), and problem discovery (Week 4). How does this change what you'll build next?

5. **If you spent 1 month interviewing customers** before writing any code, would your projects succeed more often? Why don't more engineers do this?

---

## Interview Talking Points

### For Product Manager Roles:

**Q: "How do you decide what to build next?"**

**Framework**:
1. "I start with customer interviews, not feature requests"
2. "I'm looking for jobs-to-be-done: what are customers actually trying to accomplish?"
3. "I validate through the pain pyramid: is this hair-on-fire urgent, or nice-to-have?"
4. "Example: With Bibby, I interviewed librarians and discovered their biggest pain isn't cataloging — it's [whatever you learn in interviews]"
5. "Only after validating the problem do I design solutions"

---

### For Engineering Roles:

**Q: "Tell me about a time you built something users didn't need."**

**Framework** (Honest + Growth-Oriented):
1. "Early in my career, I built [feature] because I thought users wanted it"
2. "I didn't validate the problem first — I jumped straight to solution"
3. "Outcome: [feature went unused / low adoption / feedback was 'nice but not essential']"
4. "What I learned: Now I do customer discovery first. I interview [X] people, understand their jobs-to-be-done, and validate pain before building"
5. "Recent example: With Bibby, I'm [applying lessons learned]"

---

## Resources for Deeper Learning

### Books (Read 1-2 Chapters)
- **"The Mom Test" by Rob Fitzpatrick** — Entire book (it's short, ~200 pages) on customer interviews
- **"Competing Against Luck" by Clayton Christensen** — Jobs-to-be-Done framework (Chapters 1-3)
- **"Sprint" by Jake Knapp (Google Ventures)** — Monday section on problem framing

### Articles
- **"First Round Review: 12 Things About Product-Market Fit"** — By Superhuman founder
- **"How to Do Customer Development Interviews"** — Lean Startup methodology
- **Jobs-to-be-Done: A Framework for Customer Needs** — HBR article

### Practice
**This Week's Assignment**:
- Conduct 5 customer interviews
- Don't pitch anything
- Just listen
- Document findings
- Identify patterns

**Keep a Discovery Journal**:
- Every day, note one problem you observe
- Ask: Is this your problem, or someone else's?
- If someone else's: Is it hair-on-fire urgent?
- If yes: That's a potential startup idea

---

## Connection to Next Week

**Next Week: Customer Segmentation**

You've learned:
- How to find problems (this week)
- How to validate they're real (this week)

Next: **Not everyone has the same problem the same way.**

We'll cover:
- How to segment customers into meaningful groups
- Demographic vs psychographic vs behavioral segmentation
- Ideal Customer Profile (ICP) definition
- Persona development that actually drives product decisions

**Preparation**:
Look at your interview notes. Do different people describe the same problem differently? Do they have different intensities of pain? Start grouping them by patterns.

---

## Weekly Summary

This week, you learned:

✅ **Jobs-to-be-Done framework** — People hire products to do specific jobs
✅ **Problem space vs solution space** — Stay in problem discovery before jumping to solutions
✅ **Customer interview techniques** — The Mom Test, asking about past behavior, avoiding leading questions
✅ **Pain validation** — Hair-on-fire vs annoying vs nice-to-have
✅ **Validation ladder** — Hypothesis → Interviews → Observation → Behavioral → Financial
✅ **Feature requests → Jobs** — Convert "I want X feature" into "I'm trying to accomplish Y outcome"

**Key Mindset Shift**: Stop building solutions looking for problems. Start discovering problems worth solving, then build the right solution.

---

## Mentor Sign-Off

**From the Engineering Manager:**

Here's what I've learned in 15 years of building products:

**Engineers who skip discovery build features.**
**Engineers who do discovery build businesses.**

The difference?

- Features get used once or ignored
- Businesses solve painful problems people pay for

You've now learned the hardest skill for engineers: **Listening more than building.**

Most engineers hear "do customer interviews" and think:
- "That's not my job" (wrong — it is)
- "I already know what users need" (wrong — you don't)
- "I'll just build it and see" (wrong — wasteful)

You're different now. You have a framework:
1. Hypothesize problems
2. Interview customers
3. Validate pain
4. Map jobs-to-be-done
5. THEN build solutions

This is how senior engineers and founders operate.

Next week, we'll segment customers so you know EXACTLY who to build for.

See you then.

---

**Week 4 Status**: 🟢 Complete
**Next Week**: Customer Segmentation
**Cumulative Progress**: 4/52 weeks | Semester 1: 4/13 weeks

---

*End of Week 4*
