# Week 6: Interviews, Research & Observation
## How to Uncover Problems People Don't Know How to Articulate

---

## Introduction: The Interview That Changed Everything

A software team was building inventory management software for warehouse operations. They interviewed the warehouse manager.

**Developer:** "What features do you need?"

**Manager:** "I need real-time inventory tracking and automated reordering."

**Developer:** "Great!" *Builds those features for 6 months.*

**Result:** Software rejected. Manager frustrated.

**What went wrong?**

The developer asked what the manager **thought** they needed, not what **actually happened** in the warehouse.

**A different approach:**

**Developer:** "Can I shadow your team for a day?"

**Observation reveals:**
- Workers manually count inventory twice daily
- Counts written on clipboards
- Data entered into Excel at end of shift
- Manager spends 2 hours reconciling discrepancies every morning
- Real problem: **The morning reconciliation is painful**
- Root cause: **Handwriting is illegible, workers rush counts at shift end, no way to verify counts were done correctly**

**Better solution:** Give workers tablets with barcode scanners, photo capture for damaged goods, and real-time entry during their shift. Reconciliation drops from 2 hours to 15 minutes.

**Cost:** 10% of the original "real-time tracking system."

**This week:** Learn to see what people can't tell you, because they're too close to their own workflows to articulate the problems clearly.

---

## Core Concept 1: The Mom Test (Asking Questions That Don't Lie)

### The Central Rule

> **Talk about their life, not your idea.**

Most interviews fail because people:
1. Pitch their solution and ask "Would you use this?"
2. Ask hypothetical questions ("Would you pay for X?")
3. Lead the witness ("Don't you think Y is annoying?")

**People lie.** Not on purpose — they want to be helpful, so they say what they think you want to hear.

### The Mom Test Rules

**Rule 1: Talk about specifics in the past, not hypotheticals about the future**

**Bad:** "Would you use a tool that helps you schedule better?"
**Good:** "Tell me about the last time you struggled with scheduling."

**Bad:** "How much would you pay for this?"
**Good:** "What do you currently spend on solving this problem?"

**Bad:** "Do you think this is a problem?"
**Good:** "Walk me through what happened yesterday. What took the most time?"

### The Mom Test in Action

**Scenario:** You want to build a terminal scheduling app.

**BAD Interview:**

You: "I'm building a scheduling app for terminals. Would you use it?"
Them: "Oh, sure, sounds great!"
You: "What features would you want?"
Them: "Hmm, maybe real-time updates, mobile access, notifications..."
You: "Would you pay $500/month for it?"
Them: "Yeah, probably."

**Result:** They're being polite. You learn nothing.

**GOOD Interview:**

You: "Tell me about the last time a truck showed up when you didn't expect it."
Them: "Oh man, yesterday. Driver called saying he's at the gate, but I had no idea he was coming. Our scheduler was at lunch. I had to scramble..."
You: "What did you do?"
Them: "I had to dig through emails to find his order, then check if we had the right product in the right tank, then pull someone off another load to handle it..."
You: "How long did that take?"
Them: "Like 45 minutes. The driver was pissed."
You: "How often does this happen?"
Them: "At least 2-3 times a week. Mostly because customers book by phone and sometimes the message doesn't get to the right person."
You: "What have you tried to fix it?"
Them: "We have a whiteboard in the office, but not everyone checks it. We tried a shared Google Calendar, but drivers don't use it. Honestly, nothing has really worked."

**Result:** Now you know:
- Real problem: Communication breakdown between booking and operations
- Frequency: 2-3x per week
- Impact: 45 min scramble + driver frustration
- Current solutions tried: Whiteboard, Google Calendar (both failed)
- Root cause: No system that connects customer booking → scheduler → operator

**This is actionable intelligence.**

### Questions That Work

**About past behavior:**
- "Tell me about the last time [problem] happened."
- "Walk me through your typical [workflow]."
- "What's the most frustrating part of your day?"
- "How did you handle [situation] before?"

**About current solutions:**
- "What do you currently use to solve this?"
- "What does it cost you (time/money)?"
- "What's broken about that approach?"
- "Have you tried anything else?"

**About consequences:**
- "What happens when [problem] occurs?"
- "How much time do you spend on [task]?"
- "What would happen if you did nothing?"
- "What's the cost of getting this wrong?"

**About decisions:**
- "Who else needs to approve this?"
- "What would make you switch from your current solution?"
- "What's your budget for solving this?"
- "When do you need this solved by?"

---

## Core Concept 2: The Five Whys in Practice

### Beyond the Formula

You learned the Five Whys in Week 5 as a root cause technique. Now let's see it in action during interviews.

**Key skill:** Ask "why" without sounding like a broken record.

### Technique: The Curious Follow-Up

**Instead of saying "Why?" repeatedly, use these:**

- "What causes that?"
- "What makes that difficult?"
- "What's behind that decision?"
- "Help me understand..."
- "Say more about that..."
- "What's going on there?"

### Example: Terminal Truck Delays

**You:** Tell me about the last time a truck was delayed.

**Them:** Yesterday we had a truck wait 90 minutes.

**You:** What caused the delay? *(Why #1)*

**Them:** The paperwork wasn't ready when the driver arrived.

**You:** What makes the paperwork not ready? *(Why #2)*

**Them:** We need lab results before we can release product, and the lab was backed up.

**You:** What causes the lab to get backed up? *(Why #3)*

**Them:** We batch all the testing at the end of the day to be efficient.

**You:** Why batch them instead of testing as products arrive? *(Why #4)*

**Them:** Because the lab tech has other duties — they split time between the lab and the control room.

**You:** What drives that split responsibility? *(Why #5)*

**Them:** We only have budget for one lab tech, so they cover multiple roles.

**ROOT CAUSE:** Understaffing in lab function due to budget constraints, leading to batching, which delays results, which delays trucks.

**Solutions revealed:**
1. **Hire another lab tech** (expensive, addresses root cause)
2. **Prioritize samples from trucks that are waiting** (cheap, addresses symptom)
3. **Sample products earlier in the day** (cheap, prevents batching)
4. **Automate some lab tech's control room duties** (moderate cost, frees up capacity)

**Notice:** The truck delay is 5 levels removed from the actual root cause. Most people would stop at "paperwork wasn't ready" and build a paperwork tracking system that doesn't solve anything.

### Warning: Don't Interrogate

**Bad approach:**
- "Why? Why? Why? Why? Why?" (robotic, annoying)

**Good approach:**
- Ask one why, then paraphrase: "So if I understand correctly, the issue is [X]. What's driving that?"
- Ask permission: "Can we dig into that a bit more?"
- Show curiosity: "That's interesting — I wouldn't have expected that."

---

## Core Concept 3: Observational Research in Operations

### Why Observation Beats Interviews

**People are terrible at describing their own workflows.**

Why?
1. **They're too close to it** — they don't see the weird parts anymore
2. **They rationalize** — "We do it this way because it makes sense" (even when it doesn't)
3. **They forget steps** — unconscious competence means they skip "obvious" things
4. **They describe the ideal, not the reality** — "We're supposed to do X" vs what actually happens

**Observation reveals:**
- Workarounds people don't mention
- Interruptions they've normalized
- Emotional reactions (frustration, stress, confusion)
- Time spent on each activity (often shocking)

### How to Shadow Someone

**1. Set expectations**

"I want to learn how you actually work, not how it's supposed to work. I'm looking for pain points, workarounds, anything that slows you down. I won't judge — I'm here to understand."

**2. Be a fly on the wall**

- Don't interrupt during tasks
- Take notes quietly
- Observe without offering solutions
- Let them narrate if they want, but don't force it

**3. Ask questions during downtime**

- Between tasks, not during
- "I noticed you checked that twice — what are you looking for?"
- "You seemed frustrated there — what happened?"
- "Why did you switch to the other system?"

**4. Document everything**

- Workflow steps (with time stamps)
- Tools used
- Information sources
- Handoffs
- Interruptions
- Error corrections
- Workarounds

### Example: Shadowing a Terminal Operator

**Task:** Loading a truck with product

**What they told you in interview:**
"We pull up the order, verify the driver's paperwork, connect the loading arm, pump the product, seal the truck, give the driver their ticket. Takes about 30 minutes."

**What you observed:**

```
8:00 AM - Truck arrives at gate
8:05 AM - Driver walks to office with paperwork
8:10 AM - Operator searches computer for order (struggles with search, tries 3 different screens)
8:15 AM - Can't find order, calls scheduler on cell phone
8:20 AM - Scheduler finds order in email, reads details over phone
8:25 AM - Operator writes order details on paper (computer system won't accept new orders)
8:30 AM - Driver walks to loading bay
8:35 AM - Operator positions truck (driver helps guide)
8:40 AM - Connects loading arm
8:42 AM - Starts pump
8:44 AM - Pump alarm goes off (flow rate issue)
8:50 AM - Troubleshoots with maintenance via radio
8:55 AM - Restarts pump
9:20 AM - Loading complete (product pumped)
9:25 AM - Operator prints ticket from different computer (first computer doesn't have printer access)
9:30 AM - Seals truck, hands driver ticket
9:35 AM - Truck departs

Total time: 95 minutes (not 30)
```

**Insights from observation:**

1. **Order system is broken** (can't find orders, can't enter new ones)
2. **Operator relies on scheduler's memory** (phone call every time)
3. **Two different computers** (data entry vs printing)
4. **Pump maintenance issues** (11 minutes lost to troubleshooting)
5. **Paper backup system** (written notes as workaround)

**Actual bottlenecks:**
- Order system (25 min wasted searching/calling)
- Pump reliability (11 min wasted troubleshooting)
- Dual computer system (5 min extra walking/logging in)

**Total waste:** 41 minutes out of 95 (43% of time is non-value-added)

**Solutions:**
- Fix order system (integrate with scheduler's tool)
- Preventive maintenance on pumps
- Single workstation with all systems

**Notice:** The operator didn't mention any of this in the interview because it's "just how we do things." Observation revealed the hidden waste.

---

## Core Concept 4: Identifying Workflow Interruptions

### The Hidden Productivity Killer

**Context switching** destroys productivity. Every interruption costs 15-25 minutes of focus time.

**What to look for:**
- Phone calls mid-task
- System alerts
- People walking up to ask questions
- Switching between applications
- Searching for information
- Waiting for someone else

### Interruption Tracking Exercise

**During your observation, log every interruption:**

| Time | Interruption Type | Duration | Impact |
|------|------------------|----------|--------|
| 8:15 | Phone call (scheduler) | 5 min | Had to refocus on order entry |
| 8:50 | Radio call (maintenance) | 6 min | Stopped loading process |
| 9:10 | Driver asking for restroom | 2 min | Minor, quick answer |
| 9:25 | Computer froze, reboot needed | 8 min | Lost unsaved work |

**Analysis:**
- 4 interruptions in 95 minutes
- 21 minutes lost to interruptions
- 22% of time spent on interruptions
- 2 interruptions required full context switch

### Example: Software Developer Observation

**You shadow a backend engineer for 4 hours:**

**Interruptions logged:**
- 8 Slack messages requiring response (avg 3 min each = 24 min)
- 2 meetings pulled into (30 min + 15 min = 45 min)
- 1 production issue escalation (40 min)
- 3 code review requests (15 min total)

**Total interruption time:** 124 minutes out of 240 (52% of time)

**Actual coding time:** 116 minutes (48%)

**But effective coding time?** Only 40 minutes of uninterrupted blocks.

**Insight:** "We need more developers" isn't the problem. "We need to protect focus time" is the real problem.

**Solutions:**
- No-meeting blocks (Tue/Thu mornings)
- Async Slack communication norms
- On-call rotation for production issues
- Scheduled code review times (not random interrupts)

---

## Core Concept 5: Listening for Emotion and Frustration

### The Emotional Tells

**Problems worth solving create emotion:**
- Frustration ("Ugh, this again...")
- Stress (tense body language)
- Resignation ("It is what it is...")
- Anger (raising voice, short responses)
- Relief (when problem resolves)

**If there's no emotion, it's probably not a painful problem.**

### What to Listen For

**Verbal cues:**
- "This is the worst part of my day"
- "I hate when this happens"
- "If only we could..."
- "It drives me crazy that..."
- "I waste so much time on..."

**Non-verbal cues:**
- Sighing heavily
- Shaking head
- Eye rolling
- Tense shoulders
- Rushed movements

### Example: Emotional Mapping

**During observation, note emotional moments:**

```
8:15 - Operator SIGHS loudly when order not found
       Says: "This happens every time"

8:50 - Operator TENSE when pump alarm sounds
       Says: "Not again... I knew this would happen"
       Radio voice is sharp, frustrated

9:25 - Operator RELAXES when ticket prints successfully
       Says: "Finally... okay, we're good"

9:30 - Operator SMILES when driver leaves
       Says: "Alright, one down... only 15 more today"
```

**Emotional intensity map:**
- **Highest frustration:** Order search (recurring problem, daily pain)
- **Highest stress:** Pump failure (unpredictable, delays everything)
- **Relief points:** Completion (load finishes, truck departs)

**This tells you:** Fix order search first (daily pain), then pump reliability (stress trigger).

### The "If Only" Exercise

**Ask this question:**
"If you had a magic wand and could fix one thing about your job, what would it be?"

**Answers reveal priorities:**
- What they mention first = what hurts most
- What they elaborate on = what they've thought about
- What they hesitate on = politically sensitive

**Example answers:**

"If only we had real-time tank levels, I wouldn't have to walk to the tank farm three times a day to check."
→ **Problem:** Lack of visibility into inventory

"If only customers would call ahead instead of just showing up, I could actually plan my day."
→ **Problem:** Unpredictable demand

"If only the lab results were ready when I needed them, trucks wouldn't wait."
→ **Problem:** Lab bottleneck (we found this one earlier)

---

## Core Concept 6: Documenting the Current State Honestly

### The As-Is Process Map

**Goal:** Document reality, not the ideal.

**What to capture:**
1. **Every step** (even the "obvious" ones)
2. **Decision points** (when choices are made)
3. **Handoffs** (when responsibility transfers)
4. **Wait times** (idle time between steps)
5. **Rework loops** (when errors force do-overs)
6. **Workarounds** (unofficial solutions)

### Documentation Template

**For each workflow, document:**

#### Process Overview
- Process name:
- Trigger (what starts it):
- End state (what success looks like):
- Frequency (how often):
- Actors involved:

#### Step-by-Step Flow

| Step | Actor | Action | Duration | Tools Used | Pain Points |
|------|-------|--------|----------|------------|-------------|
| 1 | Driver | Arrives at gate | 5 min | Paper ticket | Sometimes no ticket |
| 2 | Operator | Searches for order | 10 min | Terminal system | System often can't find order |
| 3 | Operator | Calls scheduler | 5 min | Phone | Scheduler may be busy |

#### Bottlenecks Identified
- Step 2 (order search) - 40% failure rate, requires step 3

#### Waste Activities
- Searching for misplaced orders
- Calling scheduler for info already entered
- Walking between two computers

#### Workarounds Observed
- Paper notes on whiteboard when system down
- Personal cell phone calls instead of official communication
- Manual spreadsheet to track daily loads

#### Emotional Moments
- Frustration at step 2 (daily occurrence)
- Stress when step 6 alarm triggers (weekly)

---

## Practical Framework: Conducting Your First Interview

### Before the Interview

**1. Define your learning goals**
- What do you want to understand?
- What job are they trying to do?
- What's the scope of the conversation?

**2. Identify the right people**
- Primary users (hands-on daily)
- Secondary users (occasional use)
- Decision makers (approve purchases)
- Influencers (recommend solutions)

**3. Prepare open-ended questions**
- Focus on past behavior
- Avoid leading questions
- Prepare follow-ups for likely answers

**4. Get permission to observe**
- "Can I shadow you for [time period]?"
- "I want to see how you actually work"
- "I'm looking for pain points and opportunities"

### During the Interview

**1. Start with context**
- "Tell me about your role"
- "Walk me through a typical day"
- "What are you responsible for?"

**2. Get specific stories**
- "Tell me about the last time [event] happened"
- "Can you give me an example?"
- "What did you do then?"

**3. Dig for root causes**
- Use the Five Whys technique
- Follow emotion and frustration
- Ask about workarounds

**4. Document current solutions**
- "How do you solve this today?"
- "What have you tried?"
- "What didn't work?"

**5. Understand context**
- "Who else is involved?"
- "What happens before this?"
- "What happens after?"

**6. Avoid pitching**
- Don't mention your solution
- Don't ask "Would you use X?"
- Focus on their problem, not your idea

### After the Interview

**1. Synthesize notes immediately**
- Write up insights while fresh
- Identify patterns across interviews
- Flag contradictions

**2. Extract key insights**
- What jobs are they trying to do?
- What's broken today?
- What are they willing to pay to fix?
- What did they emphasize emotionally?

**3. Validate with observation**
- Does what they said match what you saw?
- What did they not mention?
- What workarounds did they minimize?

---

## Hands-On Exercise: Conduct Interviews & Document Findings

### Deliverable: Interview & Observation Report

**Conduct 1-2 interviews** with domain experts or practitioners.

**Choose one of:**
- Someone in your current/past work domain
- Someone who uses software you've built
- Someone in a domain you want to understand (logistics, healthcare, finance, etc.)

### Required Outputs

#### 1. Interview Guide (Before Interview)

**Document:**
- Who you're interviewing (role, context)
- Your learning goals
- 10-15 open-ended questions prepared
- Why you chose these questions

**Example format:**
```
Interviewee: Terminal Operations Manager
Role: Manages daily loadout operations, 20 trucks/day

Learning Goals:
- Understand scheduling pain points
- Identify communication breakdowns
- Discover workarounds in current process

Questions:
1. Walk me through your typical morning routine
2. Tell me about the last time a truck was delayed...
3. [etc.]
```

#### 2. Raw Notes (During Interview)

**Capture:**
- Direct quotes (especially emotional statements)
- Stories and examples
- Current tools and processes
- Pain points mentioned
- Workarounds described

**Don't:** Clean up or interpret yet. Just capture.

#### 3. Observation Log (If You Shadow)

**Document:**
- Step-by-step workflow with timestamps
- Tools/systems used
- Interruptions (type, duration, impact)
- Emotional moments (frustration, stress, relief)
- Workarounds you observed

Use the table format from earlier:
```
| Time | Activity | Duration | Tools | Observations |
```

#### 4. Key Insights (After Interview)

**Synthesize into:**

**A. Jobs to Be Done**
- What is this person trying to accomplish?
- Write 2-3 JTBD statements: "When ___, I want to ___, so I can ___"

**B. Pain Points Identified**
- List top 3-5 problems uncovered
- For each: frequency, intensity, current solution, why it fails

**C. Workflow Diagrams**
- Draw the as-is process based on observation
- Highlight bottlenecks, wait times, rework loops

**D. Root Causes**
- Pick one major pain point
- Apply Five Whys to find root cause
- Suggest 2-3 potential solutions

**E. Surprising Discoveries**
- What didn't you expect?
- What did they not mention but you observed?
- What contradictions emerged?

#### 5. Problem Statements (Using Week 5 Framework)

**Write 2-3 formal problem statements** based on your findings:

```
[Actor] struggles to [accomplish outcome]
in the context of [situation],
resulting in [measurable consequence],
because [current solution] fails due to [root cause].
```

---

## Reflection Questions

1. **Past interviews:** Think of a time you interviewed someone about requirements. What questions did you ask? Were they Mom Test-compliant? What would you ask differently now?

2. **Observation opportunities:** Where in your current work could you shadow someone for a day? What might you learn that wouldn't come up in an interview?

3. **Emotional tells:** Think of a recent time you were frustrated at work. What was the surface problem? What was the root cause? Did anyone ask you about it?

4. **Workarounds you've built:** What workarounds do you use in your daily work? Why? What do they reveal about broken processes or tools?

5. **Interview practice:** Who can you interview this week about a problem domain? What will your first three questions be?

---

## Key Takeaways

1. **The Mom Test:** Talk about their life, not your idea. Ask about past behavior, not future hypotheticals
2. **Five Whys in practice:** Dig for root causes without sounding like a robot. Use curious follow-ups
3. **Observation > Interviews:** People can't describe what they don't see. Shadowing reveals hidden waste
4. **Interruptions matter:** Context switching is a massive productivity killer. Log and quantify it
5. **Emotion reveals importance:** Frustration, stress, and relief show you what really matters
6. **Document reality, not ideals:** As-is process maps capture workarounds, waste, and pain points
7. **Synthesize immediately:** Insights fade fast. Document while fresh

**Next week:** You'll learn to **validate problems without building software** — using smoke tests, concierge MVPs, and solution interviews to test if problems are worth solving before writing code.

---

## Additional Resources

**Books:**
- *The Mom Test* by Rob Fitzpatrick (ESSENTIAL — read this)
- *Interviewing Users* by Steve Portigal
- *Observing the User Experience* by Goodman, Kuniavsky, Moed

**Tools:**
- Voice recorder (get permission first)
- Note-taking app (Notion, Evernote, simple docs)
- Screen recording (for software observation)
- Time-tracking app (for duration logging)

**Practice:**
- Interview 3 people this week about problems in their work
- Shadow someone for at least 2 hours
- Document one workflow from start to finish
- Apply Five Whys to something frustrating you

---

*The best software engineers are also the best interviewers. They uncover problems others miss because they ask better questions and actually watch people work.*

**End of Week 6**
