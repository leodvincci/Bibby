# Week 16: Pricing Strategy & Optimization

**Mentor Voice: Business Analyst**

> "Pricing is not a one-time decision—it's a continuous optimization problem. The companies that win are those that treat pricing as a strategic lever, not a set-it-and-forget-it variable." - Inspired by SaaS pricing wisdom
>
> You've chosen your business model (tiered SaaS) and pricing structure ($25/$100/$500). But that's just the beginning. Should you display prices high-to-low or low-to-high? When do you raise prices—and by how much? Should you discount for annual contracts? What about enterprise negotiation? This week, you'll learn the tactical science of pricing optimization: the psychological tricks, the strategic timing, the negotiation frameworks, and the experimentation methodologies that turn good pricing into great pricing. Because a 10% price increase can be worth more than a 20% increase in customers.

---

## Learning Objectives

By the end of this week, you will:

1. Apply psychological pricing principles (anchoring, framing, charm pricing)
2. Design and execute price increases without losing customers
3. Understand grandfather pricing vs. forced migrations
4. Create a strategic discounting policy
5. Run pricing experiments and A/B tests
6. Master value-based pricing negotiation
7. Use pricing as a competitive weapon
8. Implement dynamic pricing in Bibby

---

## Part 1: Psychological Pricing Principles

### Anchoring: The First Number Matters

**Principle:** The first price a customer sees becomes their reference point for all other prices.

**Example 1: High-to-Low Display**

```
Option A (High-to-Low):
┌─────────────────────────────┐
│ Enterprise: $500/month      │ ← Anchor (high)
│ Pro: $100/month             │ ← Looks reasonable
│ Starter: $25/month          │ ← Looks like a steal
└─────────────────────────────┘

Psychological effect: $100 feels cheap compared to $500

Option B (Low-to-High):
┌─────────────────────────────┐
│ Starter: $25/month          │ ← Anchor (low)
│ Pro: $100/month             │ ← Feels expensive (4× more!)
│ Enterprise: $500/month      │ ← Seems outrageous
└─────────────────────────────┘

Psychological effect: $100 feels expensive compared to $25
```

**Best practice:** Display highest price first to anchor high.

**Bibby implementation:**

```java
@Controller
public class PricingPageController {

    @GetMapping("/pricing")
    public String showPricing(Model model) {
        // IMPORTANT: Display in descending price order
        List<PricingTier> tiers = List.of(
            tierService.getEnterprise(),  // $500 (anchor high)
            tierService.getPro(),         // $100 (target tier)
            tierService.getStarter()      // $25 (entry tier)
        );

        model.addAttribute("tiers", tiers);
        return "pricing";
    }
}
```

**Example 2: External Anchors**

```
Without anchor:
"Pro plan: $100/month"
└─ Customer thinks: "Is that expensive?"

With anchor (competitor comparison):
"Pro plan: $100/month
(Competitors charge $150-200/month for similar features)"
└─ Customer thinks: "That's a bargain!"

With anchor (value comparison):
"Pro plan: $100/month
(Saves you $530/month in librarian time)"
└─ Customer thinks: "That's 5× ROI!"
```

### Framing: How You Present Changes Perception

**Principle:** The same price feels different depending on how it's framed.

**Example: Daily vs. Monthly Pricing**

```
Option A (Monthly framing):
"$100 per month"

Option B (Daily framing):
"$3.33 per day"
└─ Feels cheaper (cost of a coffee!)

Option C (Annual framing):
"$1,200 per year"
└─ Feels more expensive (big number!)

Option D (Value framing):
"$100/month saves you 20 hours/month = $600 in time"
└─ Feels like you're making money
```

**When to use which:**
- **Daily:** Consumer products (Netflix: "$15.49/mo = $0.52/day")
- **Monthly:** B2B SaaS (predictable budgets)
- **Annual:** When offering discount (Bibby: "Save $240 with annual billing")
- **Value:** When ROI is clear (saves money/time)

**Bibby example:**

```java
@Service
public class PricingDisplayService {

    public String formatPrice(PricingTier tier, PriceFraming framing) {
        BigDecimal monthlyPrice = tier.getMonthlyPrice();

        return switch (framing) {
            case MONTHLY -> String.format("$%,.0f per month", monthlyPrice);

            case DAILY -> {
                BigDecimal dailyPrice = monthlyPrice
                    .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
                yield String.format("$%.2f per day", dailyPrice);
            }

            case ANNUAL -> {
                BigDecimal annualPrice = monthlyPrice.multiply(BigDecimal.valueOf(10));
                yield String.format("$%,.0f per year (save 2 months!)", annualPrice);
            }

            case VALUE -> {
                // Calculate time savings value
                int hoursSavedPerMonth = 20;
                double hourlyWage = 30.0;
                double monthlySavings = hoursSavedPerMonth * hourlyWage;
                double netValue = monthlySavings - monthlyPrice.doubleValue();

                yield String.format("$%,.0f/month → Saves $%.0f in time = $%.0f net gain",
                    monthlyPrice, monthlySavings, netValue);
            }
        };
    }
}
```

### Charm Pricing: The $99 Effect

**Principle:** Prices ending in 9 are perceived as significantly cheaper than round numbers.

**The research:**
```
$100 vs $99: Perceived as 15-20% cheaper (even though it's only 1%)
$1,000 vs $999: Same effect
$10 vs $9.99: Minimal effect (too obvious)
```

**When to use charm pricing:**
- **Consumer products:** Always (Amazon: $19.99, $49.99)
- **B2B <$100/month:** Sometimes (Mailchimp: $20 → $19)
- **B2B >$100/month:** Rarely (looks unsophisticated)
- **Enterprise:** Never ($999,999/year looks ridiculous)

**Bibby's approach:**

```
Current:
├─ Starter: $25/month (round number - fine for simplicity)
├─ Pro: $100/month (round number - looks professional)
└─ Enterprise: $500/month (round number - enterprise expects this)

Alternative with charm pricing:
├─ Starter: $24/month (marginal benefit, may confuse)
├─ Pro: $99/month (looks consumer-y for B2B)
└─ Enterprise: $499/month (unprofessional)

Decision: Stick with round numbers for B2B
```

**Exception:** If migrating from competitor with odd pricing:

```
Competitor: $149/month
Bibby: $99/month ← Use charm pricing to emphasize savings
```

### The Decoy Effect: Adding a Third Option

**Principle:** A strategically-placed "decoy" tier makes the target tier look better.

**Classic example:**

```
Without decoy:
├─ Small: $5 (most popular)
└─ Large: $8

With decoy:
├─ Small: $5
├─ Medium: $7 (DECOY - bad value)
└─ Large: $8 (looks like great deal vs. medium!)

Result: Large sales increase 40%
```

**Bibby's current structure:**

```
Starter: $25/month (1,000 books, basic)
Pro: $100/month (10,000 books, advanced) ← TARGET
Enterprise: $500/month (unlimited, API)

Question: Is Enterprise a decoy or a real tier?

Analysis:
├─ If few customers buy Enterprise → It's an anchor/decoy
├─ If 10%+ buy Enterprise → It's a real tier
└─ Either way, Enterprise makes Pro look like "best value"
```

**Explicit decoy example:**

```
BAD tier structure:
├─ Starter: $25/mo (1K books)
└─ Pro: $100/mo (10K books) ← Big jump, feels expensive

GOOD tier structure (with decoy):
├─ Starter: $25/mo (1K books)
├─ Basic Pro: $75/mo (5K books) ← DECOY (bad value per book)
├─ Pro: $100/mo (10K books) ← TARGET (great value!)
└─ Enterprise: $500/mo (unlimited)

Psychology: Pro is only $25 more than Basic Pro but gives 2× books
```

---

## Part 2: When and How to Raise Prices

### The Price Increase Playbook

**The uncomfortable truth:** Most SaaS companies undercharge, especially early on. You should raise prices regularly.

**When to raise prices:**

1. **Product maturity:** Added significant value since launch
2. **Market validation:** Customers aren't price-sensitive (low churn)
3. **High NRR:** Existing customers expanding (demonstrates value)
4. **Low churn:** <2% monthly churn (customers love product)
5. **CAC payback:** <12 months (can afford to lose price-sensitive customers)

**How much to raise:**

```
Conservative: 10-15% (test the waters)
Moderate: 20-25% (standard SaaS increase)
Aggressive: 30-50% (when significantly underpriced)
Massive: 2-3× (complete repositioning)
```

**Bibby scenario:**

```
Current pricing (Year 1):
├─ Starter: $25/month
├─ Pro: $100/month
└─ Enterprise: $500/month

Analysis:
├─ LTV:CAC: 8.5:1 (excellent - can afford increases)
├─ Churn: 2.5% monthly (acceptable)
├─ NRR: 105% (expanding)
├─ Value delivered: $530/month in time savings
└─ Current capture: $100 = 19% of value

Decision: We're significantly underpriced

Year 2 pricing (moderate increase):
├─ Starter: $29/month (+16%)
├─ Pro: $125/month (+25%)
└─ Enterprise: $600/month (+20%)

Rationale:
├─ Pro is target tier → larger increase
├─ Still only capturing 24% of value delivered
└─ Modest increase maintains competitive advantage
```

### Grandfather Pricing vs. Forced Migration

**Two strategies for existing customers:**

**Strategy 1: Grandfather Pricing** (Let existing customers keep old prices)

```
Announcement:
"Starting May 1, our Pro plan increases from $100 to $125/month
for new customers.

As a thank you for your early support, you'll stay at $100/month
for as long as you remain a customer."

Pros:
✓ Zero customer backlash
✓ High retention
✓ Goodwill and loyalty

Cons:
✗ Creates pricing complexity (multiple price tiers per plan)
✗ Reduces revenue from existing base
✗ Can last years (early customers = lowest prices forever)
✗ Creates "legacy" customers who resist feature changes
```

**Strategy 2: Forced Migration** (Everyone pays new prices)

```
Announcement:
"Starting May 1, our Pro plan increases from $100 to $125/month
for all customers.

We've added $300/month in value through:
├─ Advanced reporting (saves 2 hrs/week = $240/mo)
├─ Barcode scanner (saves 30 min/week = $60/mo)
└─ API access (enables integrations)

Your new price: $125/month (still saves you $405/month)"

Pros:
✓ Simple pricing (one price per tier)
✓ Revenue increase from existing base
✓ Fair (everyone pays for value received)

Cons:
✗ Customer backlash
✗ Churn risk (price-sensitive customers leave)
✗ Support burden (handling complaints)
```

**Decision framework:**

```java
@Service
public class PriceIncreaseSt rategyService {

    public enum MigrationStrategy {
        GRANDFATHER,
        FORCED,
        HYBRID
    }

    public MigrationStrategy recommendStrategy(BusinessMetrics metrics) {
        // If churn is high, grandfather (can't afford more churn)
        if (metrics.getMonthlyChurn() > 0.05) {
            return MigrationStrategy.GRANDFATHER;
        }

        // If large existing base, grandfather (too much complexity)
        if (metrics.getCustomerCount() > 1000) {
            return MigrationStrategy.FORCED; // Complexity worse than churn
        }

        // If LTV:CAC low, grandfather (can't afford customer loss)
        if (metrics.getLtvCacRatio() < 3.0) {
            return MigrationStrategy.GRANDFATHER;
        }

        // If underpriced significantly, force migration
        if (metrics.getPricingGap() > 0.5) { // Capturing <50% of value
            return MigrationStrategy.FORCED;
        }

        // Default: Hybrid (grandfather for 12 months, then migrate)
        return MigrationStrategy.HYBRID;
    }
}
```

**Hybrid approach (recommended):**

```
Announcement:
"Starting May 1, new customers pay $125/month for Pro.

Existing customers: You'll stay at $100/month through April 2025
(12 months). After that, you'll migrate to $125/month.

This gives you a year to budget for the increase."

Benefits:
✓ Honors existing customers (12-month grace period)
✓ Simple long-term (everyone eventually at same price)
✓ Time to prove additional value
✗ Still creates temporary complexity
```

### Communicating Price Increases

**The email template:**

```
Subject: Bibby Pricing Update – Investing in Your Library

Hi [Name],

When we launched Bibby two years ago, we set out to build the best
library management software for schools. Thanks to your feedback,
we've come a long way:

✓ Barcode scanner integration (saved customers 10 hrs/month)
✓ Advanced reporting dashboard (replaced manual Excel work)
✓ Inter-library loan network (now 500 schools connected)
✓ Reading level filters (helps match students to books)

These improvements represent $300+/month in additional value.

**What's Changing**

Starting May 1, 2025:
├─ Pro plan: $100 → $125/month (new customers)
├─ Your price: Stays at $100/month through April 2026
└─ After April 2026: $125/month

**Why This Matters**

Our pricing still delivers 5× ROI:
├─ Bibby Pro: $125/month
├─ Time saved: 20 hours/month × $30/hour = $600/month
└─ Net value: $475/month

We're committed to making this the best investment you make.

Questions? Reply to this email or schedule a call: [Link]

Thank you for being part of the Bibby community.

[Name]
Founder, Bibby
```

**Key elements:**
1. **Lead with value:** What you've added, not what you're taking
2. **Justify with numbers:** Show ROI still strong
3. **Honor existing customers:** Grandfather period
4. **Make it personal:** Signed by founder, not "Billing Team"
5. **Open dialogue:** Invite questions

---

## Part 3: Strategic Discounting

### The Discounting Dilemma

**The problem with discounts:**

```
Scenario: Customer asks for 20% discount

Without discount:
├─ Price: $100/month
├─ LTV: $4,000 (40 months retention)
└─ Revenue: $4,000

With 20% discount:
├─ Price: $80/month
├─ LTV: $3,200 (same retention)
└─ Revenue: $3,200

Loss: $800 per customer (20% of lifetime value)

If you gave this discount to 100 customers: $80,000 lost revenue
```

**The discount trap:**
- Trains customers to always ask for discounts
- Reduces perceived value ("If you discounted 20%, was it overpriced?")
- Creates pricing inconsistency
- Hard to raise prices later

### When to Discount (and When Not To)

**NEVER discount for these reasons:**
- ❌ "We're a school" (budget constraints aren't your problem)
- ❌ "Your competitor is cheaper" (different product, different value)
- ❌ "It's my first time" (establishes bad precedent)
- ❌ "I'm thinking about it" (creates artificial urgency to discount)

**OK to discount for these reasons:**
- ✅ **Annual commitment:** Discount monthly rate for annual prepayment
- ✅ **Volume:** Bulk deals (districts buying for 10+ schools)
- ✅ **Early adopter:** Beta/pilot customers helping you improve product
- ✅ **Strategic:** High-profile customer (case study, reference, visibility)
- ✅ **Competitive:** Must-win deal against incumbent (rarely)

### Discount Structure

**The discount ladder:**

```
No commitment:
└─ $100/month (standard pricing)

Annual commitment:
└─ $1,000/year = $83.33/month (16.7% discount for prepayment)
   Rationale: Reduces churn risk, improves cash flow

Multi-year commitment:
└─ $2,800 for 3 years = $77.78/month (22% discount for 3-year lock-in)
   Rationale: Extremely low churn risk, predictable revenue

Volume discount (districts):
└─ 10-50 schools: 15% off per school
└─ 50+ schools: 25% off per school
   Rationale: Lower CAC, higher retention, strategic accounts
```

**Implementation:**

```java
@Service
public class DiscountPolicyService {

    public record DiscountEligibility(
        boolean eligible,
        double discountPercent,
        String reason
    ) {}

    /**
     * Determine discount eligibility based on rules
     */
    public DiscountEligibility calculateDiscount(Customer customer, Deal deal) {
        // Rule 1: Annual prepayment discount
        if (deal.getBillingPeriod() == BillingPeriod.ANNUAL) {
            return new DiscountEligibility(
                true,
                16.7,
                "Annual prepayment (2 months free)"
            );
        }

        // Rule 2: Multi-year commitment
        if (deal.getCommitmentYears() >= 3) {
            return new DiscountEligibility(
                true,
                22.0,
                "3-year commitment"
            );
        }

        // Rule 3: Volume discount (districts)
        if (deal.getSchoolCount() >= 50) {
            return new DiscountEligibility(
                true,
                25.0,
                "Volume discount (50+ schools)"
            );
        } else if (deal.getSchoolCount() >= 10) {
            return new DiscountEligibility(
                true,
                15.0,
                "Volume discount (10-49 schools)"
            );
        }

        // Rule 4: Strategic/Beta customer (manual approval required)
        if (deal.isStrategicCustomer() && deal.isApprovedByFounder()) {
            return new DiscountEligibility(
                true,
                deal.getApprovedDiscountPercent(),
                "Strategic customer discount"
            );
        }

        // Default: No discount
        return new DiscountEligibility(
            false,
            0.0,
            "No discount applicable"
        );
    }

    /**
     * Prevent discount abuse
     */
    public boolean validateDiscount(Customer customer, double requestedDiscount) {
        // Check if customer is serial discount seeker
        if (customer.getDiscountRequestCount() > 3) {
            logger.warn("Customer {} requests discounts frequently", customer.getId());
            return false;
        }

        // Check if discount is within policy
        if (requestedDiscount > 30.0) {
            logger.warn("Requested discount {}% exceeds policy max", requestedDiscount);
            return false;
        }

        // Check if customer already has discount
        if (customer.getCurrentDiscountPercent() > 0) {
            logger.warn("Customer already has {}% discount", customer.getCurrentDiscountPercent());
            return false;
        }

        return true;
    }
}
```

### Negotiation Framework

**The price objection conversation:**

```
Customer: "That's too expensive. Can you do $75/month?"

BAD response:
"Sure, I can do $75."
└─ Destroys perceived value, sets bad precedent

GOOD response (value reframing):
"I understand budget is important. Let's look at the ROI:

Bibby Pro: $100/month = $1,200/year
Time saved: 20 hours/month × $30/hour = $600/month = $7,200/year
Net value: $6,000/year

Even at $100/month, you're getting 6:1 return. At $75, you'd get 8:1.
But is that extra 2:1 worth the features we'd have to remove to get there?

Instead of discounting, what if we started you on Starter ($25/month)
and you upgrade to Pro when you've seen the value?"

Alternative (if they have legitimate constraint):
"I can't discount the monthly price, but I can offer:
├─ Annual billing: $1,000/year = $83/month (saves $200/year)
├─ Or: Starter tier at $25/month to start
└─ Or: Extended trial (60 days instead of 30) to prove value

Which makes most sense for you?"
```

**The key:** Never discount without getting something in return (commitment, prepayment, reference).

---

## Part 4: Pricing Experiments & A/B Testing

### What to Test

**High-impact pricing experiments:**

1. **Price points:** $99 vs $100 vs $120
2. **Price display order:** High-to-low vs low-to-high
3. **Framing:** Monthly vs daily vs annual
4. **Tier features:** Which features in which tier?
5. **CTA copy:** "Start trial" vs "Get started" vs "See plans"
6. **Annual discount:** 16% vs 20% vs 2 months free

### Running a Pricing Experiment

**Example: Testing Pro tier price**

```java
@Service
public class PricingExperimentService {

    public record Experiment(
        String name,
        LocalDate startDate,
        LocalDate endDate,
        PriceVariant control,
        PriceVariant variant,
        double variantAllocation // 0.5 = 50/50 split
    ) {}

    public record PriceVariant(
        String name,
        BigDecimal price,
        String displayFormat
    ) {}

    /**
     * Run A/B test on Pro tier pricing
     */
    public Experiment runProPriceTest() {
        PriceVariant control = new PriceVariant(
            "Control",
            new BigDecimal("100.00"),
            "$100/month"
        );

        PriceVariant variant = new PriceVariant(
            "Variant",
            new BigDecimal("120.00"),
            "$120/month"
        );

        Experiment experiment = new Experiment(
            "Pro Tier Price Test",
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            control,
            variant,
            0.5 // 50% see each price
        );

        experimentRepository.save(experiment);
        return experiment;
    }

    /**
     * Assign user to cohort
     */
    public PriceVariant assignCohort(User user, Experiment experiment) {
        // Consistent hashing: same user always sees same price
        int hash = user.getId().hashCode();
        boolean isVariant = (hash % 100) < (experiment.variantAllocation() * 100);

        PriceVariant assigned = isVariant
            ? experiment.variant()
            : experiment.control();

        // Track assignment
        experimentAssignmentRepository.save(new ExperimentAssignment(
            experiment.name(),
            user.getId(),
            assigned.name(),
            LocalDateTime.now()
        ));

        return assigned;
    }

    /**
     * Analyze experiment results
     */
    public ExperimentResults analyzeExperiment(Experiment experiment) {
        List<User> controlUsers = getExperimentCohort(experiment, "Control");
        List<User> variantUsers = getExperimentCohort(experiment, "Variant");

        // Conversion rate
        double controlConversion = calculateConversionRate(controlUsers);
        double variantConversion = calculateConversionRate(variantUsers);

        // Revenue per user
        BigDecimal controlRevenue = calculateRevenuePerUser(controlUsers);
        BigDecimal variantRevenue = calculateRevenuePerUser(variantUsers);

        // Expected value (conversion × price)
        BigDecimal controlEV = experiment.control().price()
            .multiply(BigDecimal.valueOf(controlConversion));

        BigDecimal variantEV = experiment.variant().price()
            .multiply(BigDecimal.valueOf(variantConversion));

        // Statistical significance
        boolean isSignificant = calculateSignificance(
            controlUsers.size(),
            variantUsers.size(),
            controlConversion,
            variantConversion
        );

        return new ExperimentResults(
            experiment,
            controlConversion,
            variantConversion,
            controlEV,
            variantEV,
            isSignificant
        );
    }
}
```

### Interpreting Results

**Example results:**

```
Experiment: Pro Tier Price ($100 vs $120)
Duration: 30 days
Sample size: 500 trials per variant

Control ($100/month):
├─ Trials: 500
├─ Conversions: 125
├─ Conversion rate: 25%
├─ Revenue per trial: $100 × 0.25 = $25
└─ Total revenue: $12,500

Variant ($120/month):
├─ Trials: 500
├─ Conversions: 115
├─ Conversion rate: 23%
├─ Revenue per trial: $120 × 0.23 = $27.60
└─ Total revenue: $13,800

Analysis:
├─ Conversion dropped 2% (25% → 23%)
├─ But revenue per trial increased 10.4% ($25 → $27.60)
├─ Statistical significance: p < 0.05 ✓
└─ Decision: Increase price to $120

Rationale:
We can afford to lose 2% conversion if revenue increases 10%.
With LTV:CAC of 8:1, we have margin to trade conversions for revenue.
```

**When to roll back:**

```
Scenario: Conversion drops significantly

Control ($100): 25% conversion
Variant ($120): 15% conversion (10% absolute drop)

Expected value:
├─ Control: $100 × 0.25 = $25
└─ Variant: $120 × 0.15 = $18

Decision: Roll back to $100
Reason: 20% price increase doesn't justify 40% conversion drop
```

---

## Part 5: Value-Based Pricing Negotiation

### Enterprise Pricing Negotiation

**The enterprise buying process:**

```
Month 1-2: Discovery (understand needs)
Month 3-4: Demo and POC (prove value)
Month 5-6: Proposal and negotiation ← WE ARE HERE
Month 7-8: Legal and security review
Month 9-10: Procurement and signatures
```

**In Month 5, they'll say:**

"Your proposal is $60,000/year for 100 schools. Our budget is $40,000. Can you do $40K?"

### Value-Based Response Framework

**Step 1: Quantify the value**

```
You: "I understand budget constraints. Let's review the value:

Your current solution:
├─ Alexandria: $150/school/year = $15,000/year
├─ Manual processes: 5 hrs/week × 100 librarians × $30/hr × 40 weeks = $600,000/year
└─ Total current cost: $615,000/year

Bibby:
├─ Software: $60,000/year (proposed)
├─ Time saved: 4 hrs/week × 100 librarians × $30/hr × 40 weeks = $480,000/year
└─ Total cost: $60,000/year (88% reduction)

Net value: $615,000 - $60,000 = $555,000/year saved

So our proposal captures $60K of $555K value created (11%).
That's a 9:1 return for you."
```

**Step 2: Reframe the ask**

```
You: "When you ask for $40K instead of $60K, you're asking us to:
├─ Reduce our fee from 11% to 7% of value created
├─ Accept 33% less revenue for the same cost to serve
└─ This makes the economics challenging for us

Instead of discounting, what if we structure this differently?"
```

**Step 3: Offer alternatives (not discounts)**

```
Option A: Phase rollout
├─ Year 1: 50 schools at $30K
├─ Year 2: Add 25 schools at $15K
├─ Year 3: Add 25 schools at $15K
└─ Total: Same $60K, spread over 3 years

Option B: Proof of value
├─ Pilot: 20 schools at $12K for 6 months
├─ Full rollout: 100 schools at $60K after pilot succeeds
└─ Risk: Lower if pilot doesn't prove value

Option C: Multi-year commitment
├─ 3-year contract at $50K/year (discount for commitment)
├─ Total: $150K over 3 years
└─ Gives you budget certainty, gives us predictable revenue

Which structure works best for your budget cycle?"
```

**Step 4: Walk away if needed**

```
If they insist on $40K with no concessions:

You: "I appreciate you being direct about budget. Here's where we are:

At $40K, we'd be capturing 7% of the value we create ($555K).
Our cost to serve 100 schools is ~$30K/year (support, infrastructure).
That leaves $10K margin, which doesn't work for us.

I'd rather be honest: we can't make this work at $40K without
removing features you need (SSO, API, dedicated support).

I have two options:
1. We revisit when your budget allows $50-60K
2. We start with 60 schools at $40K, add more schools later

What makes more sense?"
```

**The key:** Never discount without reason. Always get something in return.

---

## Part 6: Pricing as Competitive Weapon

### Strategic Pricing Moves

**1. Undercut to gain market share (aggressive)**

```
Market leader (Alexandria): $150/school/year
Your price: $50/school/year (67% cheaper)

Strategy: Win on price, build base, raise prices later

Risks:
├─ Low revenue → longer path to profitability
├─ Price-sensitive customers (high churn when you raise prices)
└─ Race to the bottom (competitors match)

When to use: You have funding, incumbents are complacent
```

**2. Premium positioning (defensive moat)**

```
Competitors: $50-100/school/year
Your price: $200/school/year (2-4× more expensive)

Strategy: Signal superior quality, target high-value customers

Benefits:
├─ Higher revenue per customer
├─ Attracts customers who want "best" not "cheapest"
└─ Sustainable margins

When to use: Clear differentiation, strong brand, enterprise focus
```

**3. Freemium to fast growth (expansion)**

```
Your tier: $100/month
Strategy: Add free tier (500 books, 1 user)

Effect:
├─ 10× more signups (free tier)
├─ 2-5% convert to paid
├─ Viral growth (free users share)
└─ Massive top-of-funnel

When to use: Network effects, low marginal cost, VC-backed growth
```

**Bibby's approach: Hybrid**

```
Year 1-2: Competitive pricing ($100/mo = market rate)
└─ Goal: Gain market share, prove value

Year 3-4: Moderate increases ($125/mo = 25% premium)
└─ Goal: Capture more value as product matures

Year 5+: Premium positioning ($150-200/mo)
└─ Goal: Serve high-value segment, maximize profitability
```

---

## Part 7: Implementation

### Dynamic Pricing System

```java
// src/main/java/com/penrose/bibby/pricing/DynamicPricingEngine.java

@Service
public class DynamicPricingEngine {

    public record PricingContext(
        Customer customer,
        PricingTier tier,
        BillingPeriod period,
        Optional<String> campaignCode,
        boolean isExperiment
    ) {}

    public record PricingResult(
        BigDecimal basePrice,
        BigDecimal discount,
        BigDecimal finalPrice,
        List<PriceAdjustment> adjustments
    ) {}

    public record PriceAdjustment(
        String reason,
        BigDecimal amount,
        AdjustmentType type
    ) {}

    public enum AdjustmentType {
        DISCOUNT,
        SURCHARGE,
        EXPERIMENT
    }

    /**
     * Calculate final price with all adjustments
     */
    public PricingResult calculatePrice(PricingContext context) {
        List<PriceAdjustment> adjustments = new ArrayList<>();
        BigDecimal price = context.tier().getBasePrice(context.period());

        // Adjustment 1: Annual discount
        if (context.period() == BillingPeriod.ANNUAL) {
            BigDecimal discount = price.multiply(BigDecimal.valueOf(0.167)); // 16.7% = 2 months free
            adjustments.add(new PriceAdjustment(
                "Annual prepayment discount",
                discount,
                AdjustmentType.DISCOUNT
            ));
            price = price.subtract(discount);
        }

        // Adjustment 2: Volume discount (for districts)
        if (context.customer().getSchoolCount() >= 50) {
            BigDecimal discount = price.multiply(BigDecimal.valueOf(0.25));
            adjustments.add(new PriceAdjustment(
                "Volume discount (50+ schools)",
                discount,
                AdjustmentType.DISCOUNT
            ));
            price = price.subtract(discount);
        }

        // Adjustment 3: Campaign code
        if (context.campaignCode().isPresent()) {
            Campaign campaign = campaignService.findByCode(context.campaignCode().get());
            if (campaign != null && campaign.isActive()) {
                BigDecimal discount = price.multiply(campaign.getDiscountPercent());
                adjustments.add(new PriceAdjustment(
                    "Campaign: " + campaign.getName(),
                    discount,
                    AdjustmentType.DISCOUNT
                ));
                price = price.subtract(discount);
            }
        }

        // Adjustment 4: Pricing experiment
        if (context.isExperiment()) {
            Experiment experiment = experimentService.getActiveExperiment();
            PriceVariant variant = experimentService.assignCohort(context.customer(), experiment);

            BigDecimal experimentAdjustment = variant.price().subtract(context.tier().getBasePrice(context.period()));
            if (experimentAdjustment.compareTo(BigDecimal.ZERO) != 0) {
                adjustments.add(new PriceAdjustment(
                    "Pricing experiment: " + experiment.name(),
                    experimentAdjustment,
                    AdjustmentType.EXPERIMENT
                ));
                price = price.add(experimentAdjustment);
            }
        }

        // Adjustment 5: Grandfather pricing (existing customers on old prices)
        if (context.customer().hasGrandfatherPricing()) {
            BigDecimal grandfatherPrice = context.customer().getGrandfatherPrice();
            BigDecimal adjustment = price.subtract(grandfatherPrice);
            adjustments.add(new PriceAdjustment(
                "Grandfather pricing",
                adjustment,
                AdjustmentType.DISCOUNT
            ));
            price = grandfatherPrice;
        }

        BigDecimal basePrice = context.tier().getBasePrice(context.period());
        BigDecimal totalDiscount = basePrice.subtract(price);

        return new PricingResult(
            basePrice,
            totalDiscount,
            price,
            adjustments
        );
    }

    /**
     * Display pricing breakdown to customer
     */
    public String formatPricingBreakdown(PricingResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pricing Breakdown:\n");
        sb.append(String.format("Base price: $%,.2f\n", result.basePrice()));

        for (PriceAdjustment adjustment : result.adjustments()) {
            String sign = adjustment.type() == AdjustmentType.DISCOUNT ? "-" : "+";
            sb.append(String.format("  %s %s: $%,.2f\n",
                sign, adjustment.reason(), adjustment.amount()));
        }

        sb.append(String.format("\nFinal price: $%,.2f\n", result.finalPrice()));
        return sb.toString();
    }
}
```

---

## Deliverables

### 1. Pricing Psychology Audit

Review Bibby's pricing page:
- Is highest price displayed first? (Anchoring)
- Are prices framed effectively? (Daily? Value?)
- Is there a clear "target" tier? (Visual highlighting)
- Do tiers use psychological principles? (Decoy effect)

### 2. Price Increase Plan

Create a 2-year price increase roadmap:
- Year 1: Current prices ($25/$100/$500)
- Year 2: +20% increase ($29/$125/$600)
- Communication plan (email templates, grandfather policy)
- Expected impact on churn, revenue, LTV

### 3. Discount Policy Document

Write explicit discount policy:
- When to discount (annual, volume, strategic)
- When NOT to discount (individual requests)
- Approval requirements (CEO approval for >25%)
- Tracking system (prevent discount abuse)

### 4. Pricing Experiment Plan

Design 3 pricing experiments:
- Experiment 1: Pro tier price ($100 vs $120)
- Experiment 2: Price display order (high-to-low vs low-to-high)
- Experiment 3: Framing (monthly vs value)

For each: Hypothesis, sample size, duration, metrics, success criteria

### 5. Enterprise Negotiation Playbook

Create scripts for common objections:
- "That's too expensive"
- "Competitor is cheaper"
- "We have budget for $X"
- "Can you give us a discount?"

Include: Value reframing, alternative structures, walk-away points

---

## Reflection Questions

1. **Psychological Pricing:**
   - Does charm pricing ($99) work for B2B? Why or why not?
   - How does anchoring affect perceived value?
   - When should you lead with price vs. hide it?

2. **Price Increases:**
   - Should Bibby grandfather existing customers? Pros/cons?
   - How often should you raise prices? (Annually? Every 2 years?)
   - What churn rate is acceptable after a price increase?

3. **Discounting:**
   - Is an annual discount (2 months free) worth it?
   - How do you handle "we're a school" budget objections?
   - When should you walk away from a deal?

4. **Experiments:**
   - What's the minimum sample size for a pricing experiment?
   - How long should you run an experiment?
   - When should you override data with intuition?

5. **Value-Based Pricing:**
   - How do you quantify "soft" benefits (e.g., librarian happiness)?
   - What if customers don't believe your ROI calculation?
   - How do you negotiate with procurement (who only care about price)?

---

## Week 16 Summary

You've mastered pricing optimization:

1. **Psychological pricing:** Anchoring, framing, charm pricing, decoy effect
2. **Price increases:** When, how much, grandfather vs. forced migration
3. **Discounting:** When to discount (annual, volume), when not to (individual requests)
4. **Experiments:** A/B testing prices, interpreting results
5. **Negotiation:** Value-based enterprise pricing, alternative structures
6. **Competitive weapon:** Pricing strategy as market positioning
7. **Implementation:** Dynamic pricing engine with experiments

**Key Insight:** Pricing is not a one-time decision—it's a continuous optimization problem. Test, measure, iterate.

**For Bibby:**
- Display prices high-to-low (anchor with Enterprise $500)
- Plan 20% increase in Year 2 ($100 → $125 Pro tier)
- Grandfather existing customers for 12 months
- Discount only for annual (16.7%) and volume (15-25%)
- Run experiments on Pro tier price ($100 vs $120)
- Use value-based negotiation for enterprise deals

---

## Looking Ahead: Week 17

Next week: **CAC Optimization & Growth Loops**

You've optimized pricing (revenue per customer). Now you'll optimize CAC (cost per customer):
- Marketing channel analysis
- Viral loops and referral programs
- Content marketing and SEO
- Paid acquisition tactics
- Sales efficiency metrics
- Product-led growth loops
- Cohort-based CAC tracking

Plus: Building self-sustaining growth engines.

---

**Progress:** 16/52 weeks complete (31% of apprenticeship, Week 3 of Semester 2)

**Commit your work:**
```bash
git add apprenticeship/semester-2/week-16-pricing-strategy-optimization.md
git commit -m "Add Week 16: Pricing Strategy & Optimization"
git push
```

Type "continue" when ready for Week 17.
