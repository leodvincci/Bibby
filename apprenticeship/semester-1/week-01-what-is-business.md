# Week 1: What Is a Business?

**Semester 1: Markets, Problems & Value**
**Estimated Time**: 8-10 hours
**Mentor Voice**: Technical Founder/Startup CEO

---

## Opening: The Question That Changes Everything

You've built Bibby. It works. The architecture is clean. The domain model is thoughtful. The CLI flows are delightful. You've even automated code quality improvements with AI.

But here's the question that separates hobbyist developers from founders:

**"Who would pay for this, and why?"**

Not "could someone pay?" or "would it be nice if someone paid?" — but specifically: What problem does this solve that's painful enough for someone to exchange money to make it go away?

This is the foundational question of business. And for the next 52 weeks, we're going to train you to ask — and answer — this question for every system you build.

---

## The Core Economic Machine

### What Is a Business, Really?

Strip away the jargon, the pitch decks, the org charts, the mission statements. At its core, a business is a machine with three gears:

1. **Value Creation** — You make something people want
2. **Value Delivery** — You get it to the people who want it
3. **Value Capture** — You extract some portion of that value as revenue

That's it. Everything else — marketing, sales, product development, operations, finance — exists to optimize one of these three gears.

**If any gear stops turning, you don't have a business. You have a hobby, a charity, or a failed venture.**

### The Flow of Value

```
┌──────────────────────────────────────────────────────┐
│                   THE BUSINESS MACHINE                │
└──────────────────────────────────────────────────────┘

    INPUTS                 PROCESS              OUTPUTS
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│              │      │              │      │              │
│   Resources  │──────▶   Transform  │──────▶   Customers  │
│              │      │              │      │              │
│ • Capital    │      │ • Labor      │      │ • Problem    │
│ • Time       │      │ • Systems    │      │   solved     │
│ • Expertise  │      │ • IP         │      │ • Need met   │
│ • Materials  │      │ • Process    │      │ • Desire     │
│              │      │              │      │   fulfilled  │
└──────────────┘      └──────────────┘      └──────────────┘
                                                     │
                                                     │
                                                     ▼
                                            ┌──────────────┐
                                            │   Value      │
                                            │   Captured   │
                                            │              │
                                            │ • Revenue    │
                                            │ • Profit     │
                                            │ • Equity     │
                                            └──────────────┘
```

**The critical insight**: The value you create must exceed the cost of creating it. Otherwise, you're destroying value, not creating it.

---

## Value Creation: Making Something People Want

### The Four Types of Value

Software businesses typically create value in one of four ways:

#### 1. **Save Time**
- Automate manual processes
- Reduce decision-making overhead
- Eliminate waiting or coordination delays

**Example**: Calendly saves ~15 minutes per meeting scheduled. For an executive booking 20 meetings/month, that's 5 hours saved monthly — worth $500+ if their time is valued at $100/hour.

#### 2. **Save Money**
- Reduce operational costs
- Eliminate waste
- Optimize resource allocation

**Example**: AWS saves companies from buying/maintaining physical servers. A startup might save $50K-$200K in upfront infrastructure costs by using cloud services.

#### 3. **Make Money**
- Generate new revenue streams
- Increase conversion rates
- Enable price optimization

**Example**: Stripe enables anyone to accept payments online. For a creator earning $10K/month in courses, Stripe doesn't just "facilitate" — it creates $120K/year in revenue that wouldn't exist otherwise.

#### 4. **Reduce Risk**
- Prevent errors or failures
- Ensure compliance
- Increase reliability

**Example**: Datadog monitors infrastructure and alerts on failures. For an e-commerce site doing $1M/month, preventing even one hour of downtime (potentially $1,400 lost) pays for the monitoring service.

### Let's Talk About Bibby

Right now, Bibby creates value for **you**:

- **Saves time**: You can find books on your shelves without physically searching
- **Reduces friction**: The CLI makes cataloging and tracking delightful
- **Organizes chaos**: Your physical library has a digital twin

But here's the key question: **Is the value you're creating for yourself valuable enough that someone else would pay for it?**

Let's explore this:

```java
// From BookCommands.java:458
@Command(command = "check-out", description = "Check-Out a book from the library")
public void checkOutBook(){
    // ... user flow to checkout a book
}
```

This command solves a real problem: **tracking which books are in use vs available**.

**But who else has this problem?**

- Home libraries (like yours) — probably won't pay much
- **Small independent libraries** — might pay $20-50/month for a simple system
- **School libraries** — might pay $100-500/month for something that integrates with student accounts
- **Corporate knowledge libraries** — might pay $1,000+/month for enterprise features, compliance, reporting

Same core functionality. Wildly different value creation (and therefore value capture potential).

---

## Value Delivery: Getting It to Customers

Creating value is only half the battle. You must also **deliver** that value to customers in a way that's:

1. **Discoverable** — They can find your solution
2. **Accessible** — They can try/buy it easily
3. **Usable** — They can actually get value from it

### Distribution Channels for Software

```
┌─────────────────────────────────────────────────────────┐
│              SOFTWARE DISTRIBUTION MODELS                │
└─────────────────────────────────────────────────────────┘

Self-Serve (Product-Led Growth)
├── Website + Free Trial
├── Freemium Model
└── App Store / Marketplace

Sales-Led (Human Touch)
├── Outbound Sales Team
├── Inbound Demo Requests
└── Partner/Reseller Networks

Community-Led
├── Open Source → Paid Features
├── Community Edition → Enterprise
└── Developer Evangelism → Adoption
```

### Bibby's Delivery Challenge

Right now, Bibby is delivered via:
- Manual installation
- Local PostgreSQL setup
- Command-line interface

**For you**: This is perfect. You're technical, you own the machine, you control the data.

**For a school librarian**: This is a non-starter. They need:
- Cloud-hosted (nothing to install)
- Web interface (not everyone loves CLIs)
- Import tools (for existing catalogs)
- Mobile access (check books in/out from anywhere)

**The delivery mechanism shapes who can extract value from what you've built.**

This is why great technology often fails in the market — not because it doesn't create value, but because the delivery mechanism doesn't match the customer's context.

---

## Value Capture: Extracting Revenue

This is the part engineers often find uncomfortable. You've created value. You've delivered it. Now: **how do you get paid?**

### Common Software Pricing Models

#### 1. **Subscription (SaaS)**
- **Structure**: Monthly/annual recurring fee
- **Examples**: Netflix ($15/month), GitHub ($4-21/user/month)
- **Pros**: Predictable revenue, compound growth
- **Cons**: Requires continuous value delivery, churn risk

#### 2. **Usage-Based (Consumption)**
- **Structure**: Pay for what you use
- **Examples**: AWS (pay per compute hour), Twilio (pay per SMS sent)
- **Pros**: Aligns cost with value, scales naturally
- **Cons**: Unpredictable revenue, harder to forecast

#### 3. **Transaction Fee (Take Rate)**
- **Structure**: Percentage of money flowing through platform
- **Examples**: Stripe (2.9% + $0.30), Shopify (2%), Airbnb (~15%)
- **Pros**: Revenue scales with customer success
- **Cons**: Only works for platforms facilitating transactions

#### 4. **License (Perpetual or Annual)**
- **Structure**: One-time or annual fee for software
- **Examples**: Microsoft Office (perpetual), JetBrains IDEs (annual)
- **Pros**: Large upfront payment, customer "owns" it
- **Cons**: No recurring revenue, hard to justify upgrades

#### 5. **Freemium**
- **Structure**: Free base product, paid premium features
- **Examples**: Notion (free up to X blocks), Slack (free with limits)
- **Pros**: Low friction adoption, viral growth
- **Cons**: Most users never convert (~2-5% conversion is normal)

### The Value Capture Formula

```
Revenue = (Number of Customers) × (Average Revenue Per Customer)

But actually:
Sustainable Business = Revenue - (Cost of Delivery + Cost of Acquisition) > 0
```

**Key insight**: You can only capture a fraction of the value you create. The customer must get more value than they pay, or they won't buy.

### What Could Bibby Charge?

Let's play with some hypothetical pricing:

**Scenario 1: Individual Home Libraries**
- Target: Book lovers with 500+ books
- Value created: Save ~30 min/month organizing, never lose a book
- Willingness to pay: $5-10/month (comparable to Spotify/Netflix)
- Conversion rate: ~3% of free users
- **Reality check**: Hard to scale, low margins

**Scenario 2: Small Independent Libraries**
- Target: Community libraries, church libraries, small school libraries
- Value created: Replace $2,000 library management software, save 5 hours/week on admin
- Willingness to pay: $50-100/month
- Conversion rate: ~10-20% after demo
- **Reality check**: Better margins, but need sales motion

**Scenario 3: Enterprise Knowledge Management**
- Target: Companies with internal "libraries" of books, equipment, resources
- Value created: Asset tracking, compliance, utilization reporting
- Willingness to pay: $500-2,000/month (competing with enterprise asset management systems)
- Conversion rate: ~5-10% after sales cycle
- **Reality check**: High value, but long sales cycles, feature-heavy

**Notice**: Same software. Different customer. Different value creation. Different pricing. Different business viability.

---

## The Compounding Effect: Network Effects and Flywheels

Some businesses create value that **compounds** as they grow:

### Network Effects
- More users = more value for all users
- Examples:
  - Facebook (more friends = more useful)
  - Uber (more drivers = shorter wait times = more riders)
  - GitHub (more repos = more contributions = better tools)

### Data Effects
- More usage = better product = more usage
- Examples:
  - Google Search (more searches = better ranking = more accurate)
  - Spotify (more listening = better recommendations = more engagement)

### Platform Effects
- More producers = more consumers = more producers
- Examples:
  - Shopify (more stores = better apps = more stores)
  - AWS (more usage = better services = more usage)

**Question for reflection**: Could Bibby benefit from network effects?

Imagine: If 1,000 libraries used Bibby and opted to share their catalogs, could you create a "universal book finding network"? ("I need this book" → "Library 12 miles away has it, available now")

Now you're not just a catalog tool — you're a **book discovery and lending network**. Different business. Different value creation. Different monetization.

---

## Case Study: Examining Real Software Businesses

Let's analyze three companies through the Value Creation / Delivery / Capture lens:

### 1. **GitHub** (Now Microsoft, ~$1B ARR)

**Value Creation:**
- **Saves time**: No need to set up git servers, manage access controls, host wikis
- **Reduces risk**: Never lose code, easy rollback, audit trail
- **Makes money**: Open source projects attract contributors → better code → more adoption

**Value Delivery:**
- Web-based (accessible anywhere)
- Freemium (public repos free, private repos paid)
- Marketplace (extensions and integrations)

**Value Capture:**
- $4/user/month (personal)
- $21/user/month (teams)
- $231/user/year (enterprise)
- **Key metric**: 100M+ developers, ~10M paying

**Lesson**: Started with individual developers (small capture), scaled to enterprises (large capture). Same product, different pricing tiers.

---

### 2. **Datadog** (Public, ~$2B ARR)

**Value Creation:**
- **Reduces risk**: Catch infrastructure failures before customers do
- **Saves money**: Prevent downtime (Downtime for Amazon = $5,000/second)
- **Saves time**: Unified view across distributed systems

**Value Delivery:**
- Agent-based installation (easy to deploy)
- Pre-built integrations (AWS, GCP, Kubernetes, etc.)
- API-first (programmable)

**Value Capture:**
- $15/host/month (base)
- Usage-based pricing (more logs/metrics = higher bill)
- Average customer spends $50K-$500K/year

**Lesson**: Charged based on infrastructure scale — as customers grow, Datadog's revenue grows automatically.

---

### 3. **Notion** (Private, ~$300M ARR)

**Value Creation:**
- **Saves time**: One tool instead of 5 (wiki + docs + tasks + databases + notes)
- **Makes money**: (Indirect) — better documentation = better team coordination = more productivity

**Value Delivery:**
- Web + mobile + desktop
- Freemium (generous free tier)
- Templates and community

**Value Capture:**
- Free (personal use, limited blocks)
- $10/user/month (pro)
- $18/user/month (team)
- **Key insight**: Keep individuals free → they bring Notion to work → companies pay

**Lesson**: Let users experience value for free, monetize when they bring it to organizations.

---

## Your Turn: Analyzing Three Software Businesses

For this week's core exercise, you'll analyze three software companies and map their business mechanics.

**Choose three companies from different categories:**
1. One B2B SaaS (sells to businesses)
2. One B2C product (sells to consumers)
3. One platform/marketplace (connects two sides)

For each, document:

### Value Creation
- What specific problem does this solve?
- Which type of value? (Save time / save money / make money / reduce risk)
- What would customers do if this didn't exist?
- How much value (in $ or hours) do they create per customer?

### Value Delivery
- How do customers find this product?
- How do they start using it?
- What's the activation energy? (High friction? Low friction?)
- What distribution channels do they use?

### Value Capture
- What's the pricing model?
- How much do they charge?
- What's the pricing metric? (Per user? Per usage? Per transaction?)
- Who pays? (End user? Department? Company?)

### The Economics
- Estimate: Cost to acquire a customer
- Estimate: Average revenue per customer per year
- Estimate: How long does a customer typically stay?
- Does the math work? (Revenue > Costs?)

---

## Exercise 1: Map Value Flows in Three Real Companies

**Instructions:**

1. Choose three companies:
   - One B2B SaaS (e.g., Salesforce, Slack, HubSpot, Figma)
   - One B2C product (e.g., Spotify, Duolingo, Strava, Headspace)
   - One platform/marketplace (e.g., Uber, Airbnb, Shopify, Stripe)

2. For each company, create a document with:
   ```
   # [Company Name] Business Analysis

   ## Value Creation
   - Problem solved:
   - Value type (save time/money, make money, reduce risk):
   - Quantified value per customer:
   - Alternative if this didn't exist:

   ## Value Delivery
   - Discovery channels (how people find it):
   - Onboarding friction (low/medium/high):
   - Primary distribution method:
   - Platform(s):

   ## Value Capture
   - Pricing model:
   - Price point(s):
   - Who pays:
   - Pricing metric:

   ## The Economics (Research + Estimate)
   - Estimated CAC (Customer Acquisition Cost):
   - Estimated ARPU (Average Revenue Per User/Year):
   - Estimated retention (how long do customers stay):
   - Does the business model work? Why or why not?

   ## Key Insight
   - What's the one thing this company does brilliantly?
   ```

3. **Deep dive**: Pick one of the three and research:
   - How did they start? (initial product, first customers)
   - How have they evolved? (feature expansion, pricing changes)
   - What would you do differently?

---

## Exercise 2: Bibby Business Model Exploration

Now let's get practical with **your actual code**.

**Part A: Identify Value Creation Opportunities**

Review your `BookCommands.java` file. For each major command, answer:

1. **What problem does this solve?**
2. **For you personally — how much time/frustration does this save?**
3. **Who else might have this problem?**
4. **How much would solving this problem be worth to them?**

Commands to analyze:
- `book add` — Interactive book cataloging
- `book search` — Finding books by title/author
- `book check-out` — Tracking borrowed books
- `browse bookcases` — Physical location navigation
- `book shelf` — Assigning physical locations

**Example analysis format:**

```markdown
### Command: `book check-out`

**Problem Solved**:
Need to track which books are currently in use vs available on shelf

**Personal Value**:
Saves ~5 minutes per book when I'm looking for something and wondering "did I lend this out?"

**Other Potential Users**:
- Home libraries (low volume, low pain)
- Small community libraries (medium volume, medium pain)
- School libraries (high volume, high pain)
- Corporate office libraries (medium volume, high pain — need reporting for asset tracking)

**Value Quantification**:
- School librarian spends ~10 hours/week on book tracking
- At $25/hour labor cost = $250/week = $13K/year
- If Bibby could save 5 hours/week = $6,500/year saved
- Probably worth $100-300/month subscription ($1,200-$3,600/year)
- Value capture potential: ~20% of value created

**Code that creates this value**:
BookCommands.java:458 - check-out flow
BookService.java - status management
BookEntity.bookStatus - state tracking
```

**Deliverable**: Complete this analysis for all five major command groups in Bibby.

---

**Part B: Design Three Business Models for Bibby**

Create three different business models, each targeting a different customer segment:

```markdown
## Business Model 1: [Segment Name]

**Target Customer**:
(Be specific: "Public libraries with 10,000-50,000 books and 2-5 staff members")

**Value Proposition**:
(One sentence: what problem do you solve and why do you win)

**Value Creation**:
- Save time: [specific hours/week]
- Save money: [specific $ amount]
- Make money: [if applicable]
- Reduce risk: [if applicable]

**Value Delivery**:
- Distribution channel:
- Platform:
- Onboarding:

**Value Capture**:
- Pricing model:
- Price point:
- Target monthly revenue per customer:

**Required Features** (map to existing code where possible):
- [ ] Feature 1 (already exists: BookCommands.java:XX)
- [ ] Feature 2 (needs to be built)
- [ ] Feature 3 (needs to be built)

**Economics**:
- TAM (Total Addressable Market): [# of potential customers]
- Estimated CAC:
- Estimated ARPU:
- Estimated LTV (Customer Lifetime Value):
```

Create this for three segments:
1. Small independent/community libraries
2. School/university libraries
3. Corporate office libraries or resource rooms

---

## Exercise 3: Your Domain — Industrial Automation Value Mapping

You have experience in industrial automation, energy (Kinder Morgan), and logistics domains. Let's apply business thinking there.

Pick **one system or process** from your operational experience:
- Pipeline monitoring
- Energy flow optimization
- Equipment tracking
- Shift scheduling
- Maintenance routing
- Safety compliance reporting

**Map the value flows:**

1. **Current State**:
   - How is this done today?
   - What's manual vs automated?
   - What breaks or goes wrong?
   - What's the cost of failure?

2. **Value Creation Opportunity**:
   - What could software do better?
   - What value type? (time/money/risk)
   - Quantify: Hours saved? Costs reduced? Revenue increased?

3. **Value Delivery Challenge**:
   - Who would need to approve buying this?
   - What's the procurement process in industrial settings?
   - On-premise vs cloud? (Safety, compliance, connectivity)

4. **Value Capture Reality**:
   - What do existing solutions cost?
   - How much could you charge?
   - What's the sales cycle? (Probably 6-18 months)

**Deliverable**: A 2-page "Opportunity Brief" for one industrial automation software idea, including:
- Problem statement
- Value quantification
- Competitive landscape
- Pricing strategy
- Why you're uniquely positioned to build this

---

## Deliverable: Personal Thesis on Software Businesses

Synthesize everything you've learned this week into a **1,500-word essay**:

**Title**: "What Makes Software Businesses Unique?"

**Required sections:**

### 1. The Three Gears
Explain value creation, delivery, and capture in your own words. Use examples from your research.

### 2. Why Software Is Different
Compare software businesses to physical product businesses. Consider:
- Marginal cost of production (near zero for software)
- Distribution costs (internet vs physical logistics)
- Iteration speed (deploy new version in minutes vs retool factory)
- Scalability (serve 10 vs 10M users)
- Network effects and compounding

### 3. The Bibby Paradox
Reflect on this tension:
- Bibby took you ~100+ hours to build (educated guess based on scope)
- You'd probably pay $0-10/month for a tool like this
- But a school library might pay $100-300/month
- **Why the 10-30x difference in willingness to pay for the same core functionality?**

### 4. The Industrial Software Opportunity
Based on your domain expertise:
- Why is software in industrial/energy sectors often terrible?
- What makes building software for these domains hard?
- What's the opportunity for someone who understands both the domain AND modern software?

### 5. Your North Star
Answer this:
- Do you want to build products (build once, sell many times)?
- Or services (build custom for each client)?
- Or platforms (enable others to build)?
- Why? What aligns with your strengths and interests?

**Submission format**: Markdown file: `week-01-software-business-thesis.md`

---

## Reflection Questions

Spend 30 minutes journaling on these:

1. **Before this week, how did you think about "business"?** Has that changed?

2. **Look at Bibby's codebase.** Which architectural decisions create the most value? Which are "nice to have" but don't directly solve user problems?

3. **Think about your career trajectory.** You started as a Navy operations specialist, moved to Kinder Morgan (energy logistics), and now you're transitioning to software. What unique insights do you have that pure software engineers don't?

4. **Value capture makes engineers uncomfortable.** Why do you think that is? Do you feel differently after mapping out value creation → delivery → capture?

5. **If you had to pick one domain** (industrial automation, energy, logistics, developer tools, or something else) **to build a software business in**, what would it be and why?

---

## Code Integration: Business Thinking in Your Architecture

Let's look at a specific architectural decision in Bibby through a business lens:

### From `BookCommands.java:213-254` — The `addToShelf` Flow

```java
@Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
public void addToShelf(){
    ComponentFlow flow;
    flow = componentFlowBuilder.clone()
            .withStringInput("bookTitle")
            .name("What book are you shelving?:_")
            .and()
            .withSingleItemSelector("bookcase")
            .name("Choose a Bookcase:_")
            .selectItems(bookCaseOptions())
            .and().build();
    // ...
}
```

**The Technical Decision**: Use cascading component flows (title → bookcase → shelf)

**The Business Implications**:

1. **Value Creation**: This mirrors physical library organization, reducing cognitive load
2. **Value Delivery**: CLI is perfect for power users, terrible for casual librarians
3. **Value Capture Constraint**: This UX limits your addressable market to technical users

**Alternative Architectures With Different Business Outcomes**:

| Architecture Choice | Target Customer | Price Point | Market Size |
|---------------------|----------------|-------------|-------------|
| **Current: Spring Shell CLI** | Developers, tech-savvy individuals | $0-10/month | Small (thousands) |
| **Web UI + REST API** | Small libraries, schools | $50-200/month | Medium (tens of thousands) |
| **Mobile App + Cloud** | Home users, casual tracking | $3-10/month | Large (millions) |
| **Enterprise Platform + Integrations** | University systems, corporate asset mgmt | $500-5K/month | Small but high-value (hundreds) |

**The Insight**: Your technical architecture isn't just "how you build it" — it determines **who can use it**, which determines **who will pay**, which determines **business viability**.

---

## Interview Talking Points

Practice explaining these concepts as if in a technical interview or founder pitch:

### For Engineering Roles:
*"Walk me through a product you've built and the business impact."*

**Your answer framework**:
1. "I built Bibby, a library management system using Spring Boot..."
2. **Value Creation**: "It solves the problem of tracking books across physical locations..."
3. **Design Decisions**: "I architected it with a service layer because I was thinking about future API monetization..."
4. **Business Thinking**: "While it's a personal project now, I've mapped out three business models targeting different segments, from home users to enterprise asset management..."

### For Startup Interviews:
*"Tell me about a time you identified a business opportunity from a technical problem."*

**Your answer framework**:
1. "In my time at Kinder Morgan, I noticed [specific operational inefficiency]..."
2. **Value Quantification**: "This cost approximately [X hours/week] or [$X/year]..."
3. **Solution Thinking**: "A software solution could [specific intervention]..."
4. **Business Model**: "I'd price this at [pricing strategy] targeting [customer segment]..."

---

## Resources for Deeper Learning

### Books
- **"The Lean Startup" by Eric Ries** — Chapters 1-3 (build-measure-learn fundamentals)
- **"Zero to One" by Peter Thiel** — Chapter 3 (value creation vs value capture)
- **"The Mom Test" by Rob Fitzpatrick** — Entire book (understanding customer problems)

### Articles
- **Paul Graham**: ["How to Get Startup Ideas"](http://www.paulgraham.com/startupideas.html)
- **Ben Thompson (Stratechery)**: ["Aggregation Theory"](https://stratechery.com/aggregation-theory/)
- **Sam Altman**: ["Startup = Growth"](http://www.paulgraham.com/growth.html)

### Podcasts (Listen to 1-2 Episodes)
- **Indie Hackers** — Listen to any founder story episode
- **Acquired** — Pick any company deep-dive (Stripe, GitHub, AWS)
- **The SaaS Podcast** — Learn how B2B software companies price and sell

### Analysis Exercise
Pick one company from your research. Find their:
- Pricing page (how do they frame value?)
- About page (what problem do they highlight?)
- Case studies (how do customers describe value?)

Notice: How do they **talk about value** vs **talk about features**?

---

## Connection to Next Week

Next week: **Markets as Systems**

We'll explore:
- How supply and demand create equilibrium (and opportunity)
- Where market inefficiencies hide (your future business ideas)
- Network effects and winner-take-all dynamics
- How to spot underserved markets

**Preparation**: Start paying attention to frustrations in your daily work. Every time you think "this is annoying" or "why doesn't X exist?" — write it down. Those are potential market opportunities.

---

## Weekly Summary

This week, you learned:

✅ **The three gears of business**: Value creation, delivery, capture
✅ **Four types of value**: Save time, save money, make money, reduce risk
✅ **Common pricing models**: Subscription, usage, transaction, license, freemium
✅ **The compounding effects**: Network effects, data effects, platform effects
✅ **How architecture shapes business viability**: Technical decisions determine addressable market

**Key Mindset Shift**: Every line of code you write either creates value for someone or doesn't. Start asking "who would pay for this, and why?" before you build, not after.

---

## Mentor Sign-Off

**From the Technical Founder:**

You've spent years learning *how* to build software. Now you're learning *what* to build and *why*.

This isn't about turning you into a sales person or a business school graduate. It's about teaching you to see systems — technical systems, but also economic systems, human systems, market systems.

Founders who understand this build better products. Engineers who understand this get promoted faster. Operators who understand this spot opportunities others miss.

You're uniquely positioned: You understand operations (Navy, Kinder Morgan), you understand systems (Bibby architecture), and now you're learning business thinking.

That combination is rare. And valuable.

See you next week.

---

**Week 1 Status**: 🟢 Complete
**Next Week**: Markets as Systems
**Cumulative Progress**: 1/52 weeks | Semester 1: 1/13 weeks

---

*End of Week 1*
