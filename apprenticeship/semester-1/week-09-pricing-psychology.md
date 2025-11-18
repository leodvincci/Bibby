# Week 9: Pricing Psychology

**Semester 1: Markets, Problems & Value**
**Estimated Time**: 8-10 hours
**Mentor Voice**: Technical Founder/Startup CEO

---

## Opening: Why Engineers Struggle With Pricing

You've built Bibby. It works. It creates value. You understand your competitors (Week 8).

Now: **How much should you charge?**

Most engineers approach pricing like this:

> "Let's see... server costs are $50/month, my time is worth $100/hour, I spent 200 hours building this, so... $20,050 ÷ expected customers = price?"

**This is completely wrong.**

Here's why:
- **Cost-based pricing** ignores the value you create
- **Time-based pricing** penalizes efficiency (faster = less revenue?)
- **Competitor-based pricing** ignores your differentiation

**The right question isn't "What did this cost to build?"**
**It's "What is this worth to the customer?"**

Consider:
- Bibby saves a school librarian 8 hours/week
- At $25/hour, that's $200/week = $10,400/year in labor value
- Your hosting costs: $50/month = $600/year
- **Should you charge $50/month (cost-based) or $200/month (value-based)?**

This week, you'll learn:
- Value-based vs cost-based pricing
- Pricing psychology (anchoring, framing, decoys)
- How to structure tiers
- Choosing the right pricing metric
- How to test and iterate pricing
- Common pricing mistakes to avoid

---

## Cost-Based vs Value-Based Pricing

### Cost-Based Pricing (The Engineer's Instinct)

**Formula**: `Price = Costs + Markup`

**Example**:
```
Development costs: $50,000 (500 hours × $100/hour)
Server costs: $600/year
Support costs: $2,000/year
Total costs: $52,600/year

Expected customers: 100
Cost per customer: $526
Markup: 50%
Price: $789/year ≈ $66/month
```

**Why this fails**:
1. **Ignores value created**: If you save customer $10K/year, charging $66/month leaves money on the table
2. **Penalizes efficiency**: If you build it faster, you'd charge less? Makes no sense.
3. **Ignores willingness to pay**: Customer might pay $200/month happily, but you charged $66
4. **Race to the bottom**: Competitors with lower costs can always undercut you

**When cost-based works**:
- Commodities (gas, electricity, generic goods)
- Low-differentiation products
- Established markets with known pricing

**For software? Almost never.**

---

### Value-Based Pricing (The Right Approach)

**Formula**: `Price = Fraction of Value Created`

**Same Bibby example, value-based**:
```
Value created for school librarian:
- Time saved: 8 hours/week × $25/hour = $200/week
- Annual value: $200 × 50 weeks = $10,000/year
- Lost books prevented: 20 books × $25/book = $500/year
- Total value created: $10,500/year

Price (capture 20% of value): $2,100/year ≈ $175/month

Customer pays: $175/month
Customer saves: $875/month net (still $10,500 value - $2,100 cost)
ROI for customer: 400%
```

**Why this works**:
1. **Aligns with customer value**: They get $10,500, pay $2,100, net $8,400 benefit
2. **Encourages efficiency**: Faster delivery = same price, better margins for you
3. **Rewards differentiation**: If you create 2x value, you can charge 2x price
4. **Sustainable**: Customer is happy (positive ROI), you capture fair share

**Key insight**: You can capture 10-40% of value created and customer is still thrilled.

---

### Finding the Value-Based Price

**Step 1: Quantify value created**

For each customer benefit:
- Time saved × hourly rate
- Money saved directly
- Revenue enabled
- Risk reduced × cost of failure

**Bibby example**:
- **Time savings**: 8 hours/week × 50 weeks × $25/hour = $10,000/year
- **Lost books prevention**: 20 books × $25 = $500/year
- **Stress reduction**: Hard to quantify, but real
- **Total quantified value**: $10,500/year

**Step 2: Determine value capture percentage**

**Rules of thumb**:
- **10-20%**: High-volume, low-touch sales (PLG, self-serve)
- **20-40%**: Mid-market, some sales touch
- **40-60%**: Enterprise, custom implementation, high-touch

**Bibby (self-serve, small schools)**:
- Capture 20% of $10,500 = $2,100/year = $175/month

**Industrial (sales-led, high-touch)**:
- Predictive maintenance prevents $150K/year in downtime
- Capture 30% = $45K/year = $3,750/month

**Step 3: Anchor against alternatives**

**Bibby alternatives**:
- Enterprise tools: $200-$500/month → We're cheaper
- Free tools (Excel): $0 but costs $10K in labor → We're ROI-positive in month 1
- DIY: $5,000 to build + $2,000/year maintenance → We're cheaper and faster

**Our price ($175/month)**:
- 65% cheaper than enterprise
- Positive ROI vs free tools
- 90% cheaper than DIY

**Price passes sanity check.**

---

## Pricing Psychology: How Humans Perceive Price

### Anchoring: The First Number Matters

**Principle**: The first price you see becomes a reference point for all subsequent prices.

**Classic study** (Ariely):
- Group A sees $10 item first, then $100 item → $100 feels expensive
- Group B sees $100 item first, then $10 item → $10 feels like a steal
- **Same items, different perception based on anchor**

---

**Application to pricing pages**:

**Bad (low anchor)**:
```
Basic: $10/month
Pro: $50/month    ← Feels expensive relative to $10
Enterprise: $200/month ← Feels very expensive
```

**Good (high anchor)**:
```
Enterprise: $500/month    ← Anchor high
Pro: $100/month           ← Feels reasonable
Basic: $25/month          ← Feels like a steal
```

**Same products, different anchors, different perception.**

---

**Bibby pricing page (anchored)**:

**Before (no anchor)**:
```
Small Library: $50/month
```

**After (anchored to alternatives)**:
```
Enterprise Solutions: $200-$500/month
    ↓
Bibby: $50/month
    ↓
Save $150-$450/month vs enterprise tools
```

**Or anchor to value created**:
```
Your Current Cost: $875/month (in wasted labor time)
    ↓
Bibby: $50/month
    ↓
Net Savings: $825/month
```

**The anchor changes how $50/month feels.**

---

### Price Framing: How You Present the Number

**Principle**: The same price feels different based on how it's presented.

**Examples**:

**Annual vs Monthly**:
- "$600/year" feels more expensive than "$50/month" (even though identical)
- BUT "$50/month" feels ongoing, "$600/year" feels like one-time

**Daily framing** (makes price feel tiny):
- "$50/month" = "$1.67/day" → "Less than a coffee per day"

**Per-unit framing**:
- "$50/month for library management" vs "$0.01 per book tracked"
- If managing 5,000 books: $50/month = 1 penny per book = feels cheap

**Value framing**:
- "$50/month" vs "Save $825/month in labor costs for just $50"
- Second framing emphasizes ROI

---

**Bibby pricing options**:

**Option A: Monthly**
> $50/month

**Option B: Annual (with savings)**
> $500/year (save $100 vs monthly)

**Option C: Daily**
> Just $1.67/day — less than a coffee

**Option D: Per-book**
> Track 5,000 books for 1¢ per book per month

**Option E: ROI-focused**
> Save $10,000/year in labor costs for just $600/year

**Different framings appeal to different customer psychologies.**

---

### The Decoy Effect: Making Options Look Better

**Principle**: Adding a third option (the "decoy") makes one of the other options look more attractive.

**Classic example** (The Economist):

**Before (two options)**:
```
Web-only: $59
Print + Web: $125
```
**Result**: 68% chose web-only (seemed like better value)

**After (adding decoy)**:
```
Web-only: $59
Print-only: $125    ← DECOY (nobody wants this)
Print + Web: $125   ← Suddenly looks like amazing value
```
**Result**: 84% chose Print + Web (same price as print-only but get web free!)

**The decoy made the expensive option look like a deal.**

---

**Bibby pricing with decoy**:

**Without decoy**:
```
Basic: $25/month (1 library, 5,000 books)
Pro: $100/month (unlimited books, reports, API access)
```
**Problem**: $100 feels expensive compared to $25

**With decoy**:
```
Basic: $25/month (1 library, 5,000 books)
Standard: $75/month (1 library, unlimited books)    ← DECOY
Pro: $100/month (unlimited books + reports + API)  ← Looks amazing now
```

**Result**: Pro at $100 now includes everything Standard has PLUS reports + API for only $25 more. Feels like a bargain.

**Most customers choose Pro (highest value for you).**

---

### Loss Aversion in Pricing

**Principle** (from Week 3): Losses hurt 2x more than equivalent gains feel good.

**Application**: Frame pricing to emphasize what customer LOSES by not buying, not just what they gain.

**Gain-framed (weaker)**:
> "Bibby saves you 8 hours per week"

**Loss-framed (stronger)**:
> "You're currently wasting $10,000/year in labor costs. How much longer can you afford NOT to have Bibby?"

**Free trial framing**:

**Gain-framed**:
> "Try Bibby free for 30 days!"

**Loss-framed**:
> "You're losing $825 every month you wait. Start your free trial today."

**Discounting (loss-framed)**:
> "Save $100 if you pay annually"
>
> vs
>
> "Lose $100 by paying monthly"

**The loss frame is psychologically more compelling.**

---

## Tiered Pricing Strategies

### Why Tiers?

**Benefits**:
1. **Capture more value**: Power users pay more, light users pay less
2. **Self-segmentation**: Customers choose appropriate tier
3. **Upsell path**: Start low, upgrade as they extract more value
4. **Price discrimination**: Same product, different prices for different willingness to pay

**How many tiers?**
- **2 tiers**: Simple, clear choice (basic vs premium)
- **3 tiers**: Most common, allows decoy effect (good, better, best)
- **4+ tiers**: Complex, but works for diverse customer base

**Most companies converge on 3 tiers.**

---

### The Good-Better-Best Framework

**Tier 1: Good (Entry)**
- **Goal**: Low friction entry, capture small customers
- **Pricing**: Affordable, accessible
- **Features**: Core value proposition, nothing fancy
- **Margin**: Low (loss leader or break-even)

**Tier 2: Better (Target)**
- **Goal**: Where you want most customers (highest volume)
- **Pricing**: Sweet spot for your ICP
- **Features**: All the features most customers need
- **Margin**: Healthy (most profitable tier)

**Tier 3: Best (Premium)**
- **Goal**: Capture high-willingness-to-pay customers
- **Pricing**: 3-5x Tier 1
- **Features**: Everything + premium features + white-glove service
- **Margin**: Highest (justifies premium support)

---

**Bibby tiered pricing**:

**Tier 1: Starter ($25/month)**
- **Target**: Very small libraries (< 2,000 books), tight budgets
- **Limits**: 2,000 books, 1 user, basic features
- **Features**: Cataloging, search, checkout tracking
- **Goal**: Entry point, prove value, upsell later

**Tier 2: Professional ($75/month)** ← DECOY
- **Target**: Small libraries (2,000-10,000 books)
- **Limits**: 10,000 books, 3 users
- **Features**: Everything in Starter + reports, barcode scanning
- **Goal**: Make Pro (next tier) look amazing

**Tier 3: Pro ($100/month)** ← TARGET (most customers)
- **Target**: School libraries (our ICP: 2,000-50,000 books)
- **Limits**: 50,000 books, unlimited users
- **Features**: Everything + API access, advanced reports, integrations, priority support
- **Goal**: Where most schools land (best value, best margin)

**Tier 4: Enterprise ($500/month)**
- **Target**: Large libraries, multi-branch, districts
- **Limits**: Unlimited everything
- **Features**: Everything + multi-branch, SSO, custom integrations, dedicated support
- **Goal**: Capture high-value customers willing to pay premium

---

**Psychological design**:

1. **Starter anchors low** ($25 = accessible)
2. **Professional is decoy** ($75 for just more books? Meh)
3. **Pro is the winner** ($100 for unlimited books + API + reports + priority support = amazing value)
4. **Enterprise is premium anchor** ($500 makes $100 feel cheap)

**Result**: 60-70% choose Pro (exactly what you want).

---

### Choosing the Right Pricing Metric

**Question**: What unit do you charge for?

**Common metrics**:

1. **Per user** (Slack, Notion, GitHub)
   - Pro: Scales with value (more users = more value)
   - Con: Discourages collaboration ("Let's share logins to save money")

2. **Per usage** (AWS, Twilio, Stripe)
   - Pro: Aligns cost with value extracted
   - Con: Unpredictable bills, harder to forecast

3. **Per feature** (Tiered packaging)
   - Pro: Simple, predictable
   - Con: Doesn't scale with customer growth

4. **Per value metric** (Revenue-based, volume-based)
   - Pro: Perfect alignment (you win when customer wins)
   - Con: Requires access to customer data

5. **Flat fee** (Netflix, Spotify)
   - Pro: Simplest, most predictable
   - Con: Doesn't capture differential value

---

**Bibby pricing metric options**:

**Option A: Per user**
- $25/month per librarian
- **Problem**: School libraries usually have 1-2 staff → revenue capped at $50/month

**Option B: Per book**
- $0.01 per book per month
- Library with 5,000 books = $50/month
- **Problem**: Penalizes growth (more books cataloged = higher bill, feels bad)

**Option C: Per checkout**
- $0.05 per checkout
- Library with 1,000 checkouts/month = $50/month
- **Problem**: Variable costs (busy month = high bill, slow month = low bill)

**Option D: Tiered by capacity**
- $25 for < 2,000 books
- $50 for 2,001-10,000 books
- $100 for 10,001-50,000 books
- **Winner**: Predictable, scales with library size, aligns with value

**Bibby uses Option D** (capacity-based tiers).

---

**Industrial pricing metric**:

**Predictive Maintenance Product**

**Option A: Per sensor/asset**
- $50/month per monitored equipment piece
- **Problem**: Penalizes comprehensive monitoring

**Option B: Per facility**
- $5,000/month per facility
- **Problem**: Doesn't scale with facility size (small vs huge facility)

**Option C: Per failure prevented** (value-based)
- % of savings from prevented downtime
- **Problem**: Hard to prove causation, requires access to ops data

**Option D: Tiered by infrastructure**
- $2,000/month for 1-10 assets
- $5,000/month for 11-50 assets
- $10,000/month for 51-100 assets
- Custom pricing for 100+ assets
- **Winner**: Predictable, scales with complexity

**Industrial product uses Option D** (tiered by scale).

---

## How to Test and Iterate Pricing

### You Won't Get It Right the First Time

**Acceptance**: Initial pricing is a hypothesis, not a decree.

**Signs you're underpriced**:
- Everyone says "yes" immediately (no hesitation)
- No one negotiates
- Customers say "That's it?" or "Is that per user?"
- You're hitting capacity constraints

**Signs you're overpriced**:
- Everyone says "Let me think about it"
- High drop-off at pricing page
- Lots of negotiation, asking for discounts
- Competitors winning on price

**Sweet spot**:
- ~30% of prospects think it's too expensive
- ~70% convert after seeing value
- Some negotiate, most accept
- You occasionally lose deals to price (but not always)

---

### A/B Testing Pricing

**Method 1: Test different price points**

Split traffic:
- 50% see $50/month
- 50% see $75/month

Measure:
- Conversion rate at each price
- Revenue per visitor
- Customer LTV

**Example results**:
```
$50/month:
- Conversion rate: 10%
- Revenue per 100 visitors: $500/month
- LTV: $600 (12-month retention)
- Total value: $60,000 LTV per 100 visitors

$75/month:
- Conversion rate: 7%
- Revenue per 100 visitors: $525/month
- LTV: $900 (12-month retention)
- Total value: $63,000 LTV per 100 visitors

Winner: $75/month (5% more LTV despite lower conversion)
```

**Key metric**: LTV per visitor, not just conversion rate.

---

**Method 2: Test pricing presentation**

Same price ($50/month), different framing:

**Variant A: Feature-focused**
> $50/month
> - Catalog up to 10,000 books
> - Barcode scanning
> - Checkout tracking
> - Basic reports

**Variant B: Value-focused**
> $50/month
> Save $825/month in labor costs
> - Find any book in 10 seconds (vs 15 minutes)
> - Auto overdue reminders (save 5 hours/week)
> - Never lose a book again

**Variant C: ROI-focused**
> $50/month = $1.67/day
> ROI: 1,600%
> Pays for itself in 2 days

**Test which converts highest.**

---

### Pricing Experiments to Run

**Experiment 1: Anchor testing**

**Control**: Show price alone
**Variant**: Show price anchored to alternative
- "Enterprise tools cost $200-$500/month. Bibby: $50/month"

**Hypothesis**: Anchoring increases conversion by showing value

---

**Experiment 2: Annual vs monthly**

**Control**: $50/month (can pay monthly or annual)
**Variant A**: $50/month or $500/year (save $100)
**Variant B**: $50/month or $450/year (save $150)

**Hypothesis**: Annual discount increases prepayment (better cash flow)

**Measure**: % choosing annual, total cash collected

---

**Experiment 3: Tier naming**

**Control**: Basic, Pro, Enterprise
**Variant A**: Starter, Professional, Business
**Variant B**: Essential, Growth, Scale

**Hypothesis**: Names influence tier selection

**Measure**: Distribution across tiers

---

**Experiment 4: Decoy tier**

**Control**: 2 tiers ($25, $100)
**Variant**: 3 tiers ($25, $75 decoy, $100)

**Hypothesis**: Decoy makes $100 tier look better

**Measure**: % choosing each tier, average revenue per customer

---

## Common Pricing Mistakes

### Mistake 1: Pricing Too Low (The Biggest Mistake)

**Symptom**: Everyone says yes immediately, no pushback

**Why it's bad**:
- Leaves money on the table
- Attracts wrong customers (price-sensitive, high churn)
- Signals low value ("If it's cheap, it must not be good")
- Unsustainable economics (can't support good service)

**Fix**: Raise prices. Test 2x your current price with next 100 customers.

**Bibby example**:
- Current: $50/month
- Test: $100/month
- If conversion rate drops < 50%, you're making more money at higher price

---

### Mistake 2: Cost-Plus Pricing

**Symptom**: "My costs are $X, so I'll charge $X + 20%"

**Why it's bad**:
- Ignores value created
- Penalizes efficiency
- Competitors with lower costs win

**Fix**: Price based on value created, not costs incurred.

---

### Mistake 3: Competitive Parity Pricing

**Symptom**: "Competitor charges $50, so we'll charge $50"

**Why it's bad**:
- Ignores your differentiation
- Commoditizes your product
- If you create more value, charge more

**Fix**: Price based on YOUR value, not competitor's price.

**Exception**: If you're new entrant with no brand, slight discount to get traction is okay (but not 50% discount).

---

### Mistake 4: Too Many Tiers

**Symptom**: 5+ pricing tiers, customers confused

**Why it's bad**:
- Paradox of choice (Week 3)
- Analysis paralysis
- Customers choose cheapest when overwhelmed

**Fix**: 3 tiers maximum for most businesses.

---

### Mistake 5: Unclear Value Differentiation Between Tiers

**Bad example**:
```
Basic: $25/month (includes A, B, C)
Pro: $50/month (includes A, B, C, D)
```

**Problem**: What's D? Is it worth $25 more? Unclear.

**Good example**:
```
Basic: $25/month
- For small libraries (< 2,000 books)
- Cataloging + search

Pro: $50/month
- For school libraries (up to 10,000 books)
- Everything in Basic +
  - Automatic overdue reminders (save 5 hours/week)
  - Usage reports (for budget justification)
  - API access (integrate with student database)
```

**Clear value: $25 more gets you 3 big features with quantified benefits.**

---

### Mistake 6: Discounting Too Easily

**Symptom**: Customer asks for discount, you give 20% off immediately

**Why it's bad**:
- Signals price isn't real ("If they gave 20% off easily, I should've asked for 40%")
- Trains customers to negotiate
- Erodes margins
- Creates precedent (customer expects discount at renewal)

**Fix**:
- Anchor to value, not price
- Offer alternatives instead of discounts:
  - "Can't do 20% off, but I can give you annual plan (saves 15%) or add extra users"
- If discounting, require concession:
  - "I can give 15% off if you pay annually upfront and provide a case study testimonial"

---

### Mistake 7: Ignoring Pricing Tiers in Product Design

**Problem**: Pricing is afterthought, tiers don't map to customer value

**Example**:
```
Tier 1: 1,000 API calls/month
Tier 2: 10,000 API calls/month
Tier 3: 100,000 API calls/month
```

**Why it's bad**: These limits might not align with customer jobs-to-be-done.

**Better**:
```
Tier 1: Small libraries (up to 2,000 books) - aligns with customer segment
Tier 2: School libraries (2,000-10,000 books) - aligns with ICP
Tier 3: Large libraries (unlimited books) - aligns with enterprise segment
```

**Pricing should reflect how customers think about value, not how you think about costs.**

---

## Deliverable: Pricing Strategy Document

Create comprehensive pricing strategy:

### Part 1: Value Quantification

```markdown
# Pricing Strategy: [Your Product]

## Value Created (Per Customer)

### Primary Value Drivers

**1. Time Savings**
- Current process takes: [X hours/week]
- Our solution takes: [Y hours/week]
- Time saved: [X-Y hours/week]
- Annual time saved: [(X-Y) × 50 weeks]
- Hourly rate: $[Z]/hour
- **Annual value from time savings**: $[total]

**2. Cost Savings**
- Current costs: $[X]/year
- Our solution costs: $[Y]/year (including our price)
- **Annual cost savings**: $[X-Y]

**3. Revenue Enabled**
- Additional revenue generated: $[X]/year
- **Annual revenue value**: $[X]

**4. Risk Reduction**
- Cost of failure/incident: $[X]
- Frequency reduced: [Y incidents/year]
- **Annual risk value**: $[X × Y]

### Total Value Created
**Sum of all value drivers**: $[total]/year

### Value Capture Strategy
- Total value created: $[A]/year
- Price: $[B]/year
- **Value capture %**: [B/A × 100]%
- **Customer ROI**: [(A-B)/B × 100]%

**Sanity check**: Customer ROI should be 200-500% for easy sell.
```

---

### Part 2: Pricing Structure

```markdown
## Pricing Tiers

### Tier 1: [Name] - $[X]/month

**Target Customer**: [Specific segment]

**Limits**:
- [Dimension 1]: [limit]
- [Dimension 2]: [limit]

**Features**:
- [Feature 1]
- [Feature 2]
- [Feature 3]

**Value Delivered**: $[Y]/month
**ROI for customer**: [Z]%

**Goal**: [Entry point / volume play / etc.]

---

### Tier 2: [Name] - $[X]/month

[Same structure]

**Differentiation from Tier 1**:
- [Key difference 1] → delivers $[value]
- [Key difference 2] → delivers $[value]

---

### Tier 3: [Name] - $[X]/month

[Same structure]

### Expected Distribution
- Tier 1: [X]% of customers
- Tier 2: [Y]% of customers (TARGET tier)
- Tier 3: [Z]% of customers

**Average revenue per customer**: $[weighted average]
```

---

### Part 3: Pricing Psychology Application

```markdown
## Psychology Tactics

### Anchoring Strategy
**Primary anchor**: [What you anchor to]
- Example: "Enterprise solutions cost $500/month. Ours: $100/month"

**Secondary anchor**: [Value created]
- Example: "Save $10,000/year for just $1,200/year"

### Framing
**Price presentation**: [How you show the price]
- Monthly vs annual
- Daily breakdown
- Per-unit cost
- ROI emphasis

**Chosen framing**: [Which and why]

### Decoy Effect
**Decoy tier**: [Which tier is decoy]
**Purpose**: Make [target tier] look like best value

### Loss Aversion
**Loss-framed messaging**:
- "You're currently losing $[X]/month without this"
- "Every week you wait costs $[Y]"
```

---

### Part 4: Competitive Comparison

```markdown
## Pricing vs Competitors

| Feature | Us | Competitor A | Competitor B | Free Alternative |
|---------|-----|--------------|--------------|------------------|
| Price | $[X]/mo | $[Y]/mo | $[Z]/mo | $0 |
| [Feature 1] | ✅ | ✅ | ❌ | ⚠️ |
| [Feature 2] | ✅ | ⚠️ | ✅ | ❌ |
| Setup time | [X min] | [Y days] | [Z hours] | Immediate |
| **Value/Price Ratio** | [High] | [Medium] | [Low] | [Negative] |

**Our positioning**: [How you position on price]
- Premium to free alternatives (but massive ROI)
- Discount to enterprise (but comparable features)
- Fair value relative to mid-market
```

---

### Part 5: Testing Plan

```markdown
## Pricing Experiments

### Experiment 1: Price Point Testing
**Hypothesis**: [What you're testing]
**Variants**:
- Control: $[X]/month
- Variant A: $[Y]/month
- Variant B: $[Z]/month

**Metrics**:
- Conversion rate
- Revenue per visitor
- LTV

**Decision criteria**: Choose price with highest LTV per visitor

---

### Experiment 2: [Another experiment]

[Same structure]

### Rollout Plan
**Phase 1** (Months 1-3): Test with [X] customers
**Phase 2** (Months 4-6): Analyze and iterate
**Phase 3** (Months 7-12): Scale winning variant
```

**Total deliverable**: 2,000-2,500 words

---

## Reflection Questions

Spend 30-45 minutes journaling:

1. **If you had to 10x Bibby's price** (from $50/month to $500/month), what would you need to add or change to justify it? Or is there a customer segment that would pay $500/month today?

2. **Think about software you pay for.** Do you feel like you're getting good value? Are you under-utilizing it? Are you paying for features you don't use? How does this inform your pricing design?

3. **Engineers often underprice** because they think about costs, not value. Why is this instinct so strong? How can you override it?

4. **In industrial settings**, procurement often says "that's too expensive" as negotiation tactic. How do you respond? (Hint: Anchor to value, not cost)

5. **Would you rather have** 100 customers at $50/month ($5K MRR) or 20 customers at $250/month ($5K MRR)? Why? (Consider: support load, churn risk, expansion revenue)

---

## Interview Talking Points

### For Product/Founder Roles:

**Q: "How did you determine your pricing?"**

**Framework**:
1. "I started by quantifying the value we create for customers"
2. "For [specific customer], we save [X hours/week] and prevent [$Y in costs]"
3. "That's $[Z] in value per year"
4. "We price at [A]% of value created, which gives them [B]× ROI"
5. "We validated this by testing $[price A] vs $[price B], and found [result]"

**Example**:
"For Bibby, I quantified that school librarians waste 8 hours/week on manual tracking, worth $10,000/year. We also prevent $500/year in lost books. That's $10,500 in total value. We price at $600/year (6% of value), giving them 17× ROI. We tested $50/month vs $100/month and found conversion dropped only 20%, so we went with $100/month for new customers."

---

## Resources for Deeper Learning

### Books
- **"Priceless" by William Poundstone** — Pricing psychology (Chapters 1-5)
- **"Monetizing Innovation" by Madhavan Ramanujam** — Value-based pricing
- **"The 1% Windfall" by Rafi Mohammed** — Pricing strategy

### Articles
- **"The Psychology of Pricing" (Neil Patel)** — Anchoring, framing, decoys
- **"SaaS Pricing Strategy" (Price Intelligently)** — Metrics, tiers, testing
- **"Don't Just Roll the Dice" (Neil Davidson)** — Software pricing

### Tools
- **Price Intelligently** — Pricing strategy consulting
- **ProfitWell** — Pricing data and benchmarks
- **Baremetrics** — SaaS metrics and pricing analysis

### Practice
**This Week**:
1. Quantify value Bibby creates for 3 different customer types
2. Design 3-tier pricing based on value
3. Create pricing page with anchoring
4. List 5 pricing experiments to run

---

## Connection to Next Week

**Next Week: Economics of Value Creation**

You now know:
- **How to price** to capture value (this week)
- **How much to charge** based on customer value

Next: **How value flows through economic systems**

We'll cover:
- Producer vs consumer surplus
- Value chains and value capture
- Platform economics
- Economic moats and competitive advantages

**Preparation**:
Think about:
- In a transaction, who captures the value you create? (You? Customer? Intermediaries?)
- How much surplus value exists in the system?
- Where are economic inefficiencies?

---

## Weekly Summary

This week, you learned:

✅ **Value-based pricing** — Price based on value created, not costs incurred
✅ **Pricing psychology** — Anchoring, framing, decoy effect, loss aversion
✅ **Tiered pricing** — Good-Better-Best framework, where to position customers
✅ **Pricing metrics** — Per user, per usage, per value, tiered capacity
✅ **Testing pricing** — A/B tests, experiments, iteration
✅ **Common mistakes** — Underpricing, cost-plus, too many tiers, unclear differentiation

**Key Mindset Shift**: Pricing isn't about covering costs. It's about capturing a fair share of value created. If you create $10,000 in value and charge $1,000, customer gets 9× ROI and is thrilled.

---

## Mentor Sign-Off

**From the Technical Founder:**

Here's what I learned building three companies:

**Underpricing was my biggest mistake early on.**

First startup:
- Created $50K/year value per customer
- Charged $5K/year (10% capture)
- Thought I was being "fair" and "accessible"

**Result**:
- Customers loved it (of course, 10× ROI!)
- But I couldn't afford to support them well
- Attracted price-sensitive customers who churned
- Couldn't build sustainable business

**Second startup**:
- Created $50K/year value per customer
- Charged $20K/year (40% capture)
- Felt "expensive" but...

**Result**:
- Customers still got 2.5× ROI (great!)
- I could hire support team
- Attracted customers who valued the product
- Sustainable, profitable business

**The lesson**: Don't be afraid to capture fair value. Customer getting 3-5× ROI is thrilled. You don't need to give them 10× ROI.

You now know:
- How to quantify value created
- How to price based on value
- Psychology of pricing
- How to test and iterate

Next week: How value flows through economic systems (and how to capture your share).

See you then.

---

**Week 9 Status**: 🟢 Complete
**Next Week**: Economics of Value Creation
**Cumulative Progress**: 9/52 weeks | Semester 1: 9/13 weeks

---

*End of Week 9*
