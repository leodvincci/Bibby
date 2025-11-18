# Week 7: Market Sizing (TAM/SAM/SOM)

**Semester 1: Markets, Problems & Value**
**Estimated Time**: 8-10 hours
**Mentor Voice**: Business/Systems Analyst

---

## Opening: Why Market Size Matters

You've identified your ICP (Week 5), validated the problem (Week 4), and crafted your value prop (Week 6).

Now the critical question:

**"Is this opportunity big enough to build a business?"**

Not "big enough to be interesting" — but specifically:
- Can this support a venture-scale business? ($100M+ revenue potential)
- Can this support a sustainable lifestyle business? ($1M-$10M revenue)
- Or is this a feature, not a company? (< $1M revenue ceiling)

**The answer determines everything**:
- Should you raise venture capital? (VCs need $1B+ markets)
- Should you bootstrap? (Smaller markets, higher margins)
- Should you pivot? (Market too small)
- Should you expand horizontally? (Market big enough to build platform)

This week, you'll learn:
- How to calculate Total Addressable Market (TAM)
- How to define Serviceable Addressable Market (SAM)
- How to estimate Serviceable Obtainable Market (SOM)
- Top-down vs bottom-up sizing methodologies
- How to sanity-check your assumptions
- How to present market size credibly

---

## The Three Market Sizes: TAM, SAM, SOM

### The Pyramid

```
        ┌─────────────────┐
        │       TAM       │  ← Total Addressable Market
        │  (Everyone who  │     (Theoretical maximum)
        │   could use it) │
        ├─────────────────┤
        │       SAM       │  ← Serviceable Addressable Market
        │   (Everyone we  │     (Who we can realistically serve)
        │    can reach)   │
        ├─────────────────┤
        │       SOM       │  ← Serviceable Obtainable Market
        │  (What we can   │     (What we can actually capture)
        │   realistically │
        │     capture)    │
        └─────────────────┘
```

**Key insight**: TAM gets investor attention. SOM determines if you succeed.

---

### Total Addressable Market (TAM)

**Definition**: The total revenue opportunity if you achieved 100% market share in your category.

**Question TAM answers**: "How big could this possibly get?"

**Example: Bibby (Library Management)**

**TAM Calculation**:
- All libraries globally: ~2.8 million (public, academic, school, special)
- Average spending on management software: ~$2,000/year
- **TAM = 2.8M × $2,000 = $5.6 billion**

**Why this matters**: Investors want to see $1B+ TAM for venture scale.

**But be realistic**: You're NOT going to get every library globally.

---

### Serviceable Addressable Market (SAM)

**Definition**: The portion of TAM you can actually serve with your current business model, geography, and capabilities.

**Question SAM answers**: "Who can we realistically reach?"

**Example: Bibby**

**SAM Filters** (from TAM):
- Geography: US only (initially) = ~130,000 libraries
- Segment: Small-to-mid-size (exclude large academic) = ~110,000 libraries
- Tech readiness: Willing to use modern software = ~75,000 libraries
- **SAM = 75,000 × $600/year (our pricing) = $45 million**

**Why this matters**: This is your actual target market.

---

### Serviceable Obtainable Market (SOM)

**Definition**: The portion of SAM you can realistically capture in a specific timeframe (usually 3-5 years).

**Question SOM answers**: "What can we actually achieve?"

**Example: Bibby (5-Year Projection)**

**SOM Calculation** (Year 5):
- Realistic market share of SAM: 5% (3,750 libraries)
- Our average contract value: $600/year
- **SOM (Year 5) = 3,750 × $600 = $2.25 million ARR**

**Why this matters**: This is your business plan target.

**Sanity check**: 5% market share in 5 years is ambitious but achievable for a focused startup.

---

### The Relationship

```
TAM:  Total theoretical market       $5.6 billion (all libraries, global)
  ↓   Filter by: geography, segment
SAM:  What we can serve              $45 million (US small-mid libraries)
  ↓   Filter by: competition, time
SOM:  What we'll capture (Year 5)    $2.25 million (5% of SAM)
```

**Investor pitch**: Lead with TAM (shows big opportunity), back it up with realistic SOM.

---

## Top-Down Market Sizing

### The Methodology

**Approach**: Start with large, published market data and narrow down.

**Formula**:
```
TAM = (Total market participants) × (Average spend per participant)
```

**Data sources**:
- Industry reports (Gartner, Forrester, IDC)
- Government statistics (Census, BLS)
- Public company filings
- Trade associations

---

### Bibby Example: Top-Down

**Step 1: Find total market size**

Source: American Library Association (ALA) data

- Public libraries (US): ~17,000
- Academic libraries (US): ~3,600
- School libraries (US): ~80,000
- Special libraries (corporate, medical, etc.): ~7,000
- **Total: ~107,000 libraries in US**

**Step 2: Find average spending**

Source: Library Journal Annual Survey

- Average library technology budget: $15,000/year
- % spent on management software: ~15%
- **Average spend on library management: ~$2,250/year**

**Step 3: Calculate TAM**

```
TAM = 107,000 libraries × $2,250/year = $240 million (US market)
```

**Step 4: Calculate SAM** (apply filters)

**Our filters**:
- Exclude large academic (too complex): -3,000 = 104,000
- Exclude very small (< 1,000 books): -20,000 = 84,000
- Target tech-ready: ~70% = 58,800

**SAM potential customers**: ~58,800 libraries

**Our pricing**: $600/year (lower than average, targeting underserved)

```
SAM = 58,800 × $600 = $35.3 million
```

**Step 5: Calculate SOM** (realistic capture)

**Year 1**: 0.5% of SAM = 294 libraries = $176K ARR
**Year 3**: 2% of SAM = 1,176 libraries = $706K ARR
**Year 5**: 5% of SAM = 2,940 libraries = $1.76M ARR

---

### Industrial Example: Top-Down

**Market: Predictive Maintenance for Pipeline Operators**

**Step 1: Total market participants**

Source: Pipeline and Hazardous Materials Safety Administration (PHMSA)

- Oil pipeline operators (US): ~2,800 companies
- Gas pipeline operators (US): ~3,000 companies
- **Total: ~5,800 pipeline operators**

**Step 2: Segment by size**

Source: Energy Information Administration (EIA)

- Large operators (> 5,000 miles): ~50 companies
- Mid-market (500-5,000 miles): ~450 companies
- Small operators (< 500 miles): ~5,300 companies

**Our ICP**: Mid-market (500-5,000 miles)

**Step 3: Average spending**

Source: Industry surveys, analyst reports

- Average operations budget: $50M/year
- % spent on monitoring/maintenance software: ~3%
- **Average spend: ~$1.5M/year**

**But we're not replacing entire budget, just predictive maintenance**:
- Typical predictive maintenance budget: ~$200K/year
- **Our pricing**: $60K/year (cloud-based, lower than traditional)

**Step 4: Calculate SAM**

```
SAM = 450 mid-market operators × $60K/year = $27 million
```

**Step 5: Calculate SOM**

**Year 1**: 1% = 4-5 customers = $240K-$300K ARR
**Year 3**: 5% = 22-23 customers = $1.3M ARR
**Year 5**: 15% = 67-68 customers = $4M ARR

---

## Bottom-Up Market Sizing

### The Methodology

**Approach**: Start with your unit economics and build up to market size.

**Formula**:
```
SOM = (Customers you can acquire per month) × (Average contract value) × (Months in period)
```

**Why it's better**: Based on YOUR capabilities, not industry averages.

**Why it's harder**: Requires operational assumptions.

---

### Bibby Example: Bottom-Up

**Step 1: Define acquisition channels**

**Channel 1: Content Marketing (SEO)**
- Target keyword: "library management software for schools"
- Monthly searches: ~2,000
- Click-through rate: 5% = 100 clicks/month
- Trial signup rate: 10% = 10 trials/month
- Trial-to-paid: 20% = 2 customers/month
- **Content channel: 2 customers/month**

**Channel 2: Education Conferences**
- Relevant conferences per year: 12
- Booth cost: $2K per conference
- Conversations at booth: ~100
- Demo requests: 20%  = 20 demos
- Demo-to-trial: 50% = 10 trials
- Trial-to-paid: 30% = 3 customers
- **Conference channel: 36 customers/year (3/month)**

**Channel 3: Partnerships (EdTech platforms)**
- Partner integrations: 3 platforms
- Referrals per month per platform: 5
- Total referrals: 15/month
- Trial rate: 60% = 9 trials/month
- Trial-to-paid: 25% = 2.25 customers/month
- **Partnership channel: 2 customers/month**

**Total acquisition rate**: 7 customers/month = 84 customers/year

**Step 2: Calculate revenue**

**Year 1**:
- New customers: 84
- Average contract value: $600/year
- Churn: 15% (early stage, still finding fit)
- **Year 1 ending ARR: ~$42K** (accounting for mid-year starts + churn)

**Year 2**:
- Base from Year 1: 84 customers (minus 15% churn = 71)
- New customers: 168 (2x growth in acquisition)
- **Year 2 ending ARR: ~$143K**

**Year 3**:
- Base: 203 customers
- New: 250
- **Year 3 ending ARR: ~$384K**

**Year 5** (scaling channels):
- Content: 10 customers/month (improved SEO)
- Conferences: 6 customers/month (more conferences)
- Partnerships: 8 customers/month (more partners)
- Referrals (new channel): 4 customers/month (word of mouth)
- **Total: 28 customers/month = 336/year**
- **Year 5 ARR: ~$1.2M**

**This is bottom-up SOM**: Based on what you can execute, not market theory.

---

### Industrial Example: Bottom-Up

**Market: Predictive Maintenance for Pipeline Operators**

**Step 1: Sales-led approach** (enterprise sales required)

**Sales capacity**:
- Sales reps: 2 (Year 1)
- Meetings per rep per month: 12
- Total meetings/month: 24

**Funnel**:
- Meetings → Pilots: 25% = 6 pilots/month
- Pilot duration: 90 days
- Pilot → Paid: 40% = 2.4 customers/month (but lagged by 90 days)

**Realistic Year 1**:
- First 3 months: Building pipeline (0 customers)
- Months 4-12: 2 customers/month average = 18 customers
- **Year 1: 18 customers × $60K = $1.08M ARR**

**Year 2** (scaling):
- Sales reps: 4
- Improved conversion (better case studies): 45% pilot-to-paid
- **Year 2 addition: 36 new customers**
- **Year 2 ending ARR: (18 + 36) × $60K = $3.24M**

**Year 5**:
- Sales reps: 10
- Improved conversion: 50%
- **Year 5 ARR: ~$8M**

**This bottom-up SOM (~$8M) is higher than top-down SOM (~$4M) in this case**:
- Top-down assumed 15% market share of 450 companies
- Bottom-up shows we could actually capture 133 customers (30% market share)
- **Lesson**: When sales-driven, execution can beat market share assumptions

---

## Sanity Checking Your Numbers

### Common Mistakes

**Mistake 1: "TAM = Everyone on Earth × $X"**

Example:
> "There are 7 billion people. If even 1% use our app at $10/month, that's $840M/year!"

**Why this fails**:
- 1% of everyone is not realistic
- Ignores segmentation, accessibility, willingness to pay
- No path to reach "1% of everyone"

**Fix**: Define realistic TAM based on specific segments.

---

**Mistake 2: "We'll get 10% market share"**

**Why this fails**:
- Why 10%? Why not 5% or 20%?
- Assumes equal distribution of market
- Ignores incumbents, competition, network effects

**Fix**: Build bottom-up based on YOUR acquisition capabilities.

---

**Mistake 3: "Market is growing at 50% CAGR"**

**Why this fails**:
- Citing industry reports without context
- High growth rates compress over time
- Your growth ≠ market growth (you might grow faster or slower)

**Fix**: Model YOUR growth based on operational levers, not market tailwinds.

---

### The Sanity Check Framework

For any market sizing claim, ask:

#### 1. **Is the TAM believable?**

**Test**:
- Does it match published industry data?
- Can you name 10 real companies/people in this market?
- What's the largest competitor's revenue? (Your TAM should be bigger)

**Example: Bibby**

Published data: Library management software market ~$500M globally
Our TAM claim: $240M (US only)
**Sanity check: PASS** (US is ~40% of global, so $240M is reasonable)

Largest competitor: SirsiDynix (~$100M revenue)
Our TAM: $240M
**Sanity check: PASS** (TAM > largest player)

---

#### 2. **Is the SAM realistic?**

**Test**:
- What % of TAM did you filter out? (Should be 50-90%)
- Can you name specific exclusion reasons?
- Would someone in the industry agree with your segments?

**Example: Bibby**

TAM: $240M (all US libraries)
SAM: $35M (small-mid libraries, tech-ready)
**Filtered out**: 85% of market
**Reasons**: Too large (enterprise needs), too small (won't pay), not tech-ready

**Sanity check: PASS** (specific, defensible filters)

---

#### 3. **Is the SOM achievable?**

**Test**:
- What market share % is your SOM?
- Can you name a company that achieved similar share in similar time?
- Does your bottom-up model support this number?

**Example: Bibby Year 5**

SOM: $1.76M
SAM: $35M
**Market share**: 5%

**Comparable**: Notion achieved ~8% of note-taking market in 5 years (we're targeting 5%)
**Bottom-up**: Our channel model projects $1.2M (close to $1.76M)

**Sanity check: PASS** (achievable with execution)

---

#### 4. **Does the math work?**

**Test**: Work backwards from SOM

```
SOM (Year 5): $1.76M ARR
÷ Average contract value: $600
= 2,933 customers

Can we acquire 2,933 customers in 5 years?
= 587 customers/year
= 49 customers/month

From current channels:
- Content: 2/month
- Conferences: 3/month
- Partnerships: 2/month
**Current capacity: 7/month = 84/year**

**Gap to close**: Need 503 more customers/year by Year 5
**Implies**: 6x growth in acquisition rate

Is 6x growth realistic in 5 years?
**Answer: Yes, if we scale channels aggressively**
```

**Sanity check: PASS** (ambitious but achievable)

---

## Market Sizing Template

### For Your Domain

Use this template to size your market:

```markdown
# Market Sizing: [Your Product/Domain]

## TAM (Total Addressable Market)

**Market definition**: [Specific market you're in]

**Data sources**:
- [Source 1]: [stat]
- [Source 2]: [stat]

**Total potential customers**: [number]
**Average spend per customer**: $[amount]/year
**TAM**: [total]

**Sanity check**:
- Largest competitor revenue: $[amount]
- Industry reports estimate: $[amount]
- My TAM: $[amount]
- **Passes sanity check**: [Yes/No, why]

---

## SAM (Serviceable Addressable Market)

**Filters applied to TAM**:

1. **Geography**: [which regions] → [number] customers
2. **Segment**: [which customer types] → [number] customers
3. **Tech readiness**: [willingness to adopt] → [number] customers
4. **Other filters**: [additional constraints] → [number] customers

**SAM total customers**: [number]
**Our pricing**: $[amount]/year
**SAM**: $[total]

**Sanity check**:
- % of TAM: [percentage] (should be 10-50%)
- Can name [number] real potential customers: [Yes/No]
- **Passes sanity check**: [Yes/No, why]

---

## SOM (Serviceable Obtainable Market)

### Top-Down Approach

**Realistic market share** (Year 5): [percentage]%
**SOM calculation**: [SAM] × [percentage]% = $[amount]

**Comparable companies**:
- [Company 1] achieved [X]% in [Y] years
- [Company 2] achieved [X]% in [Y] years
- Our target: [X]% in [Y] years

**Sanity check**: [Ambitious/Realistic/Conservative]

### Bottom-Up Approach

**Acquisition channels**:

Channel 1: [Name]
- Capacity: [number] customers/month
- Cost per acquisition: $[amount]
- Expected: [number] customers Year 1

Channel 2: [Name]
- Capacity: [number] customers/month
- Cost per acquisition: $[amount]
- Expected: [number] customers Year 1

**Total Year 1**: [number] customers × $[ACV] = $[ARR]

**Growth trajectory**:
- Year 1: $[amount]
- Year 2: $[amount] ([X]% growth)
- Year 3: $[amount] ([X]% growth)
- Year 5: $[amount] ([X]% growth)

**Sanity check**:
- Bottom-up SOM: $[amount]
- Top-down SOM: $[amount]
- **Difference**: [percentage]% (should be within 2x)

---

## Growth Assumptions

**Market growth rate**: [X]% CAGR
**Our growth rate**: [Y]% (faster/same/slower than market)
**Why we can outpace market**: [specific reasons]

---

## Conclusion

**Is this market big enough?**

- For venture scale (need $1B+ TAM): [Yes/No]
- For lifestyle business ($1-10M): [Yes/No]
- For bootstrapped business: [Yes/No]

**Recommendation**: [Build/Pivot/Expand scope]
```

---

## Exercise 1: Size the Bibby Market

Complete a full TAM/SAM/SOM analysis for Bibby:

### Part A: Top-Down Sizing

1. **Research library statistics**:
   - US public libraries: [number]
   - US school libraries: [number]
   - US academic libraries: [number]
   - US special libraries: [number]

2. **Find average spending data**:
   - Industry reports on library technology budgets
   - Average software spending per library
   - Competitive pricing (Follett, SirsiDynix, etc.)

3. **Calculate TAM, SAM, SOM**:
   - TAM: All US libraries × average spend
   - SAM: Filtered for your ICP × your pricing
   - SOM (Year 5): Realistic market share × SAM

### Part B: Bottom-Up Sizing

1. **Define acquisition channels** (at least 3):
   - Content marketing (SEO, blog)
   - Education conferences
   - Partnerships (EdTech platforms)
   - Direct outreach
   - Referral programs

2. **Model each channel**:
   - Monthly capacity (how many customers?)
   - Cost per acquisition
   - Conversion rates (traffic → trial → paid)

3. **Project 5-year growth**:
   - Year 1 ARR
   - Year 3 ARR
   - Year 5 ARR

### Part C: Sanity Check

- Compare top-down SOM vs bottom-up SOM
- Identify gaps and reconcile differences
- List assumptions that could be wrong
- Define what would need to be true for these numbers

**Deliverable**: Complete market sizing document (2,000 words)

---

## Exercise 2: Size an Industrial Market

Pick your domain expertise (energy, logistics, manufacturing).

### Market to Size

Choose one:
1. Predictive maintenance for pipeline operators
2. Shift management software for 24/7 facilities
3. Compliance tracking for hazardous materials
4. Equipment tracking for distributed facilities

### Research Required

**Top-Down**:
- Industry associations (how many companies?)
- Government data (facility counts, industry size)
- Analyst reports (technology spending)
- Public company filings (comparable software revenue)

**Bottom-Up**:
- Sales cycle length (enterprise deals take 3-12 months)
- Sales team capacity (reps, meetings, conversions)
- Pilot-to-paid conversion rates
- Year-over-year scaling assumptions

**Deliverable**: Complete TAM/SAM/SOM with bottom-up model (2,500 words)

---

## Exercise 3: Investor-Ready Market Slide

Create a single slide (or markdown equivalent) that shows:

**Title**: "[Product] Market Opportunity"

**Content**:
```
TAM: $[X]B
"[Market category] spending globally"

SAM: $[Y]M
"[Specific segment] we can serve in [geography]"

SOM (Year 5): $[Z]M
"[X]% market share, [number] customers at $[ACV]"

[Visual: Pyramid or bar chart showing TAM → SAM → SOM]

Market Growth: [X]% CAGR
Source: [Credible analyst firm]

Key Assumptions:
• [Assumption 1]
• [Assumption 2]
• [Assumption 3]
```

**Deliverable**: One investor-ready slide with clear numbers and sources

---

## Presenting Market Size to Stakeholders

### For Investors (Venture Capital)

**What they care about**:
- TAM > $1B (must be venture-scale)
- High growth rate (20%+ CAGR)
- Clear path to dominance (winner-take-all dynamics)

**How to present**:
1. **Lead with TAM**: "We're going after a $5.6B market"
2. **Show growth**: "Growing at 15% CAGR"
3. **Narrow to SAM**: "We're specifically targeting $45M segment that's underserved"
4. **Show SOM credibility**: "We can capture $2M ARR by Year 5 based on our bottom-up model"
5. **Prove with bottoms-up**: "Here's how we acquire customers month by month"

**Example pitch**:

> "The global library management software market is $5.6 billion and growing at 8% annually. We're initially targeting the 75,000 small-to-mid-size libraries in the US — a $45 million segment that's underserved by enterprise tools and undersupported by free alternatives. Our bottom-up model shows we can reach $2 million ARR by Year 5 by acquiring 50 customers per month through content marketing, education conferences, and strategic partnerships. That's 5% market share in a segment where current solutions have 60% customer dissatisfaction."

---

### For Bootstrappers (Self-Funded)

**What you care about**:
- Can I get to $100K ARR profitably? (validation)
- Can I scale to $1M ARR? (sustainable business)
- What's the ceiling? (lifestyle vs growth business)

**How to think about it**:
1. **Ignore TAM**: Doesn't matter for bootstrapping
2. **Focus on SOM**: Can you get to 200-300 customers?
3. **Model CAC vs LTV**: Does unit economics work at scale?
4. **Check market depth**: Are there enough customers to sustain growth for 3-5 years?

**Example analysis**:

> "I don't need a $1B market. I need 500 customers at $2K/year to hit $1M ARR. There are 75,000 potential customers in my segment. Even at 1% penetration, that's 750 customers — room to grow. My CAC is $800 (conferences + content), LTV is $10K (5-year retention at $2K/year), so LTV:CAC is 12.5:1. This works even without venture scale."

---

### For Employers (Product Manager Role)

**What they care about**:
- Is this worth our team's time?
- How does this compare to other opportunities?
- What's the 3-year revenue potential?

**How to present**:
1. **Show TAM/SAM/SOM clearly**
2. **Compare to adjacent opportunities** (size + strategic fit)
3. **Bottom-up revenue projections** (Year 1, Year 2, Year 3)
4. **Resource requirements** vs potential return

**Example presentation**:

> "The small business accounting software market (TAM) is $12B. We can serve the 500K US small businesses under 10 employees (SAM: $600M at $100/month). In 3 years, we can realistically capture 5,000 customers ($6M ARR, 1% market share). This requires 2 engineers, 1 PM, 1 marketer for 18 months to MVP, then 3 more hires to scale. ROI: $6M revenue vs $3M investment over 3 years."

---

## Common Pitfalls to Avoid

### Pitfall 1: "Trillion Dollar Market"

**Red flag**: "$3 trillion healthcare market"

**Why it's wrong**: Too broad. Healthcare includes hospitals, insurance, pharma, devices. You're not addressing all of healthcare.

**Fix**: Narrow to specific segment. "Hospital scheduling software: $2.5B"

---

### Pitfall 2: "1% of China"

**Red flag**: "If we get just 1% of China's 1.4 billion people..."

**Why it's wrong**:
- 1% of China is 14 million people (massive, not "just")
- No realistic path to reach them
- Ignores competition, distribution, payment infrastructure

**Fix**: Model realistic customer acquisition in markets you can actually access.

---

### Pitfall 3: Hockey Stick Growth

**Red flag**: "Year 1: $100K, Year 2: $500K, Year 3: $5M, Year 4: $50M"

**Why it's wrong**: 50x growth from Year 3 to Year 4 requires explanation. What changes?

**Fix**: Show operational drivers of growth (more reps, more channels, improved conversion).

---

### Pitfall 4: "Market is Growing Fast"

**Red flag**: "Growing at 50% CAGR"

**Why it might be wrong**:
- Small markets grow fast percentages but small absolute dollars
- High growth rates slow down as market matures
- Your growth ≠ market growth

**Fix**: Model YOUR growth based on execution, cite market growth as tailwind (not driver).

---

### Pitfall 5: Ignoring Competition

**Red flag**: "No one else is doing this"

**Why it's concerning**:
- If market is big and obvious, why no competitors?
- Maybe market doesn't exist
- Maybe problem isn't painful enough

**Fix**: Identify competitors (direct, indirect, DIY) and show why there's still room.

---

## Deliverable: Complete Market Sizing Analysis

### Structure

**1. Executive Summary** (250 words)
- Market you're addressing
- TAM, SAM, SOM (Year 5)
- Key assumptions
- Conclusion (is market big enough?)

**2. TAM Analysis** (500 words)
- Market definition
- Data sources (citations)
- Total participants
- Average spending
- TAM calculation
- Sanity checks

**3. SAM Analysis** (500 words)
- Filters applied (geography, segment, tech readiness, other)
- Filtered participant count
- Your pricing model
- SAM calculation
- % of TAM captured
- Sanity checks

**4. SOM Analysis** (750 words)

**Top-Down**:
- Realistic market share assumption (Year 5)
- Comparable companies and their share
- Calculation

**Bottom-Up**:
- Acquisition channels (detail each)
- Monthly/annual capacity
- Year 1, 3, 5 projections
- Reconciliation with top-down

**5. Growth Assumptions** (500 words)
- Market CAGR (with sources)
- Your growth drivers
- Why you can outpace market
- Risks to growth

**6. Sanity Checks** (250 words)
- TAM vs industry data
- SAM as % of TAM
- SOM achievability
- Bottom-up vs top-down reconciliation

**7. Conclusion** (250 words)
- Venture scale? (Yes/No, why)
- Bootstrap viable? (Yes/No, why)
- Recommended approach
- Next validation steps

**Format**: Markdown, 3,000-3,500 words

**Include**: Tables, charts (optional but helpful)

---

## Reflection Questions

Spend 30-45 minutes journaling:

1. **If Bibby's market is only $35M SAM**, is that big enough? For what business model? (Venture-backed? Bootstrapped? Lifestyle?)

2. **In your industrial experience**, have you seen companies pursue small markets successfully? How did they make the economics work?

3. **Most founders overestimate their addressable market.** Why? Is it optimism bias? Lack of understanding? Pitching to investors? How can you stay honest?

4. **If your bottom-up SOM is 3x higher than top-down**, which do you trust? What does the gap tell you?

5. **Market size influences strategy.** A $10M market requires different execution than a $1B market. How would you approach Bibby differently if SAM was $500M vs $35M?

---

## Interview Talking Points

### For Startup Roles:

**Q: "How big is the market opportunity?"**

**Framework**:
1. "The total addressable market for [category] is [TAM], based on [source]"
2. "We're specifically targeting [segment], which represents [SAM]"
3. "In our first 5 years, we can realistically capture [SOM], which is [X]% market share"
4. "I've validated this bottom-up: we can acquire [Y] customers per month through [channels], leading to [Z] ARR by Year 5"
5. "For comparison, [competitor] achieved [similar metrics] in [similar timeframe]"

**Example**:
"The library management software market is $240M in the US. We're targeting small-to-mid-size libraries, which is a $35M segment. In 5 years, we can capture $2M ARR, or 5% market share. I've modeled this bottom-up: acquiring 50 customers per month through content and conferences gets us to 2,900 customers. For comparison, Libib (a competitor) grew to 500K users in 6 years — we're targeting a smaller, higher-value segment."

---

### For Product Management Roles:

**Q: "How do you prioritize which markets to enter?"**

**Framework**:
1. "I evaluate market size alongside strategic fit and competitive positioning"
2. "A $50M market with weak competition beats a $500M market with strong incumbents"
3. "I use TAM/SAM/SOM to assess total opportunity, serviceable opportunity, and obtainable opportunity"
4. "Then I model bottom-up: can we actually execute customer acquisition in this market?"
5. "Example: [specific market analysis]"

---

## Resources for Deeper Learning

### Books
- **"The Startup Owner's Manual" by Steve Blank** — Chapter on market sizing
- **"Venture Deals" by Brad Feld** — What investors look for in market size

### Articles
- **"How to Size a Market" (First Round Review)** — Step-by-step guide
- **"TAM SAM SOM Explained" (Slidebean)** — Visual framework
- **"Bottom-Up Market Sizing" (SaaStr)** — Operational approach

### Data Sources
- **Gartner, Forrester, IDC** — Industry analyst reports (expensive but credible)
- **U.S. Census Bureau** — Free demographic and business data
- **Industry associations** — Trade groups often publish market data
- **Public company filings** — 10-K reports show revenue by segment
- **Similar Web, Ahrefs** — Web traffic data for competitors

### Practice
**This Week**:
1. Size three markets you're considering
2. For each, calculate TAM/SAM/SOM both top-down and bottom-up
3. Compare results and reconcile differences
4. Pick the most promising based on market size + strategic fit

---

## Connection to Next Week

**Next Week: Competitive Landscape**

You now know:
- **How big** the opportunity is (market sizing)
- **Who** your customers are (segmentation)
- **What** value you provide (value props)

Next: **Who else is competing for this market?**

We'll cover:
- Direct, indirect, and substitute competition
- Competitive analysis frameworks
- Differentiation strategies
- Market positioning maps
- How to compete and win

**Preparation**:
Research 5-10 competitors in your space:
- What do they offer?
- What's their pricing?
- Who are their customers?
- What are their strengths/weaknesses?

---

## Weekly Summary

This week, you learned:

✅ **TAM/SAM/SOM framework** — Three levels of market opportunity
✅ **Top-down sizing** — Start with industry data, filter down
✅ **Bottom-up sizing** — Build from your unit economics up
✅ **Sanity checking** — How to validate market size claims
✅ **Presenting to stakeholders** — Different audiences care about different numbers
✅ **Common pitfalls** — Trillion dollar markets, 1% of China, hockey sticks
✅ **Market size influences strategy** — Small vs large markets require different approaches

**Key Mindset Shift**: Market size isn't about impressing investors with big numbers. It's about understanding if there's enough opportunity to build a sustainable business with your capabilities.

---

## Mentor Sign-Off

**From the Business/Systems Analyst:**

Here's what I've learned in 20 years of market analysis:

**Big markets attract competition. Small markets attract focus.**

Most founders chase the biggest TAM they can claim. They want to pitch "$10 billion market" to investors.

But here's the truth:
- A $10B market with 1,000 competitors is harder than a $100M market with 10 competitors
- A $100M market where you can be #1 is better than a $1B market where you're #37
- Market size matters, but market dynamics matter more

**Your job**:
1. Size the market honestly (not optimistically)
2. Find the segment where YOU can win
3. Build bottom-up to prove you can execute
4. Expand from position of strength

You now know:
- How to calculate TAM/SAM/SOM
- How to validate market size
- How to present credibly

Next week: Who are you competing against, and how do you win?

See you then.

---

**Week 7 Status**: 🟢 Complete
**Next Week**: Competitive Landscape
**Cumulative Progress**: 7/52 weeks | Semester 1: 7/13 weeks

---

*End of Week 7*
