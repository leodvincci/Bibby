# Week 15: Business Model Patterns

**Mentor Voice: Technical Founder**

> "Your business model is the architecture of your business. Like software architecture, there are patterns that work and patterns that don't. Copy proven patterns, avoid anti-patterns, and innovate carefully." - Inspired by software architecture wisdom
>
> Last week you learned to measure a SaaS business. This week, you'll learn the different ways to structure one. Should Bibby be freemium or free trial? Seat-based or usage-based pricing? Product-led growth or sales-led? These aren't arbitrary choices—each pattern has tradeoffs in economics, growth rate, and organizational complexity. Pick the wrong model and you'll fight uphill. Pick the right one and growth compounds naturally. This week, you'll learn the proven patterns, their tradeoffs, and how to choose the right architecture for your business.

---

## Learning Objectives

By the end of this week, you will:

1. Understand core business model patterns (SaaS, PaaS, Marketplace, Platform)
2. Compare pricing models (usage-based, seat-based, tiered, flat-rate)
3. Differentiate go-to-market motions (PLG vs sales-led vs hybrid)
4. Evaluate freemium vs free trial vs demo-first approaches
5. Understand horizontal vs vertical SaaS tradeoffs
6. Choose the right business model for Bibby
7. Implement business model flexibility in code

---

## Part 1: The Business Model Landscape

### What is a Business Model?

**Definition:** The system by which you create, deliver, and capture value.

**Three questions every business model answers:**
1. **Create:** What value do you create? (Product)
2. **Deliver:** How do customers access that value? (Distribution)
3. **Capture:** How do you extract economic value? (Monetization)

**Example: Bibby**
```
Create: Library management software (eliminates manual work)
Deliver: Cloud SaaS (self-service signup, instant access)
Capture: Tiered subscription pricing (monthly/annual)
```

### The Four Software Business Model Archetypes

**1. Software-as-a-Service (SaaS)**
- **What:** Access to software via subscription
- **Examples:** Salesforce, Slack, Zoom, Bibby
- **Revenue:** Recurring subscription fees
- **Customer:** Typically businesses (B2B) or consumers (B2C)
- **Unit economics:** High gross margin (75-85%), predictable revenue

**2. Platform-as-a-Service (PaaS)**
- **What:** Infrastructure/tools for developers to build on
- **Examples:** AWS, Heroku, Twilio, Stripe
- **Revenue:** Usage-based pricing (API calls, compute hours)
- **Customer:** Developers and technical teams
- **Unit economics:** Variable margin (depends on infra costs), high scale potential

**3. Marketplace**
- **What:** Connect buyers and sellers, take transaction fee
- **Examples:** Airbnb, Uber, Etsy, Upwork
- **Revenue:** Take rate (% of transaction value)
- **Customer:** Two-sided (supply + demand)
- **Unit economics:** High gross margin once scaled, but cold-start problem

**4. Open Source + Commercial**
- **What:** Free core product, charge for enterprise features/support
- **Examples:** MongoDB, GitLab, Elastic
- **Revenue:** Enterprise licenses, support contracts, hosting
- **Customer:** Developers (free), enterprises (paid)
- **Unit economics:** Low CAC (community), but high sales cost for enterprise

**Bibby is pure SaaS** (archetype #1).

---

## Part 2: Pricing Model Patterns

### The Five Core Pricing Models

**1. Flat-Rate Pricing**

**Structure:** One price, unlimited usage

```
Example: Basecamp
├─ $99/month for entire company
├─ Unlimited users, projects, storage
└─ Simple, predictable

Pros:
✓ Simple to understand
✓ Easy to sell
✓ Predictable for customer

Cons:
✗ Leaves money on table (power users undercharged)
✗ Hard to justify price increases
✗ Doesn't scale with customer value
```

**When to use:** Simple products, clear value prop, want fast adoption

**2. User-Based (Seat-Based) Pricing**

**Structure:** Price per user/seat

```
Example: Slack
├─ $8/user/month (Standard)
├─ $15/user/month (Plus)
└─ Scales with team size

Pros:
✓ Revenue grows with customer growth
✓ Aligns value with usage (more users = more value)
✓ Easy for customers to calculate cost

Cons:
✗ Incentivizes customers to share logins
✗ Can limit adoption (expensive for large teams)
✗ Doesn't capture power users vs. light users
```

**When to use:** Collaboration tools, team-based software, B2B SaaS

**Bibby could use this:**
```
Pricing:
├─ $25/month for 1 librarian
├─ $40/month for 2-3 librarians
└─ $60/month for 4-10 librarians
```

**3. Usage-Based Pricing**

**Structure:** Pay for what you use (consumption-based)

```
Example: AWS
├─ $0.10 per GB of storage
├─ $0.012 per hour of compute
└─ Scales perfectly with usage

Pros:
✓ Perfect value alignment (pay for what you use)
✓ Lower barrier to entry (start small)
✓ Revenue scales automatically with usage

Cons:
✗ Unpredictable revenue (harder to forecast)
✗ Unpredictable cost for customer
✗ Complex billing systems required
```

**When to use:** Infrastructure products, APIs, data/compute-heavy products

**Bibby example (if we were usage-based):**
```
Pricing:
├─ $0.02 per book cataloged
├─ $0.001 per checkout processed
└─ $5/month minimum
```

**4. Tiered Pricing**

**Structure:** Good-Better-Best tiers with feature gating

```
Example: Bibby (current model)
├─ Starter: $25/month (1,000 books, basic features)
├─ Pro: $100/month (10,000 books, advanced features)
└─ Enterprise: $500/month (unlimited, API, SSO)

Pros:
✓ Captures different customer segments
✓ Clear upgrade path
✓ Psychological pricing (decoy effect)

Cons:
✗ Complexity (which tier do I need?)
✗ Feature gatekeeping can frustrate users
✗ Requires careful tier design
```

**When to use:** Most B2B SaaS (proven pattern)

**5. Freemium**

**Structure:** Free forever tier + paid upgrades

```
Example: Dropbox
├─ Free: 2GB storage
├─ Plus: $12/month (2TB storage)
└─ Professional: $20/month (3TB + features)

Pros:
✓ Viral growth (free users refer others)
✓ Try before buy (low friction)
✓ Large user base for product feedback

Cons:
✗ Hard to convert free → paid (typically 2-5%)
✗ High support costs for non-paying users
✗ "Good enough" problem (free tier meets needs)
```

**When to use:** Consumer products, network effects, low marginal cost

### Hybrid Models

Most successful companies combine models:

**Slack: Seat-based + Freemium**
```
├─ Free: Up to 10K messages (freemium)
├─ Standard: $8/user/month (seat-based)
└─ Plus: $15/user/month (seat-based)
```

**Snowflake: Usage-based + Tiered**
```
├─ Standard tier: Pay per compute/storage (usage-based)
├─ Enterprise tier: + features + SLA (tiered)
└─ Business Critical: + security + support (tiered)
```

**Twilio: Usage-based + Volume Discounts**
```
├─ Pay-as-you-go: $0.0075 per SMS
├─ Volume discount: $0.0065 per SMS (>100K/month)
└─ Enterprise: Custom pricing + SLA
```

---

## Part 3: Comparing Pricing Models for Bibby

### Decision Framework

Let's evaluate each model for Bibby:

```java
// src/main/java/com/penrose/bibby/pricing/PricingModelAnalyzer.java

@Service
public class PricingModelAnalyzer {

    public enum PricingModel {
        FLAT_RATE,
        SEAT_BASED,
        USAGE_BASED,
        TIERED,
        FREEMIUM
    }

    public record ModelAnalysis(
        PricingModel model,
        int alignmentScore,        // 1-10: How well value aligns with price
        int simplicityScore,       // 1-10: How easy to understand
        int forecastabilityScore,  // 1-10: How predictable revenue
        int scalabilityScore,      // 1-10: How well scales with customer growth
        String recommendation
    ) {}

    public List<ModelAnalysis> analyzeBibbyPricingOptions() {
        return List.of(
            // Flat-rate
            new ModelAnalysis(
                PricingModel.FLAT_RATE,
                5,  // Medium alignment (doesn't scale with library size)
                10, // Very simple
                10, // Very predictable
                4,  // Poor scalability
                "Too simple. Leaves money on table with large libraries."
            ),

            // Seat-based
            new ModelAnalysis(
                PricingModel.SEAT_BASED,
                7,  // Good alignment (value grows with team size)
                9,  // Simple to understand
                9,  // Predictable
                7,  // Good scalability
                "Good option. Works for libraries with multiple staff."
            ),

            // Usage-based
            new ModelAnalysis(
                PricingModel.USAGE_BASED,
                10, // Perfect alignment (pay per book, per checkout)
                4,  // Complex to calculate
                5,  // Unpredictable for customer
                10, // Perfect scalability
                "Best alignment but too complex for market. Schools need budgets."
            ),

            // Tiered
            new ModelAnalysis(
                PricingModel.TIERED,
                8,  // Good alignment (tiers based on library size)
                7,  // Moderate complexity
                9,  // Predictable
                9,  // Excellent scalability
                "RECOMMENDED. Proven pattern, captures segments, clear upgrade path."
            ),

            // Freemium
            new ModelAnalysis(
                PricingModel.FREEMIUM,
                6,  // Medium alignment
                8,  // Simple concept
                3,  // Unpredictable conversion
                8,  // Can scale
                "Risky. Schools have budget for tools. Freemium works better for consumer."
            )
        );
    }

    public PricingModel recommend() {
        return PricingModel.TIERED; // Based on analysis
    }
}
```

### The Winner: Tiered Pricing

**Why tiered pricing wins for Bibby:**

1. **Segment capture:** Small schools (Starter), medium schools (Pro), districts (Enterprise)
2. **Clear value metric:** Tiers based on library size (# of books)
3. **Psychological pricing:** Good-Better-Best with decoy effect
4. **Predictable:** Schools budget annually
5. **Proven pattern:** Every successful B2B SaaS uses tiers

**Bibby's optimal tier structure:**

```
Starter ($25/month):
├─ Up to 1,000 books
├─ 1 user
├─ Basic cataloging + checkout
├─ Email support
└─ Target: Small private schools, personal libraries

Pro ($100/month):
├─ Up to 10,000 books
├─ 5 users
├─ Everything in Starter, plus:
├─ Advanced reporting
├─ Barcode scanner
├─ Chat support
└─ Target: K-8 elementary/middle schools

Enterprise ($500/month):
├─ Unlimited books
├─ Unlimited users
├─ Everything in Pro, plus:
├─ API access
├─ SSO/SAML
├─ Dedicated support
├─ SLA (99.9% uptime)
└─ Target: Large schools, multi-school districts
```

**Implementation:**

```java
@Entity
public class PricingTier {
    @Id
    private Long id;

    private String name; // Starter, Pro, Enterprise
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice; // 2 months free

    // Capacity limits
    private Integer maxBooks;
    private Integer maxUsers;
    private Integer maxCheckoutsPerMonth;

    // Feature flags
    private boolean hasAdvancedReporting;
    private boolean hasBarcodeScanner;
    private boolean hasAPIAccess;
    private boolean hasSSO;
    private boolean hasDedicatedSupport;

    // Support SLA
    private String supportResponseTime; // "24 hours", "4 hours", "1 hour"
    private Double uptimeSLA; // null, 0.99, 0.999

    public boolean supports(Feature feature) {
        return switch (feature) {
            case BASIC_CATALOGING -> true; // All tiers
            case ADVANCED_REPORTING -> hasAdvancedReporting;
            case BARCODE_SCANNER -> hasBarcodeScanner;
            case API_ACCESS -> hasAPIAccess;
            case SSO_AUTHENTICATION -> hasSSO;
        };
    }

    public boolean isWithinLimits(LibraryUsage usage) {
        if (maxBooks != null && usage.bookCount() > maxBooks) {
            return false;
        }
        if (maxUsers != null && usage.userCount() > maxUsers) {
            return false;
        }
        return true;
    }
}
```

---

## Part 4: Go-to-Market Motion Patterns

### Product-Led Growth (PLG)

**Definition:** Product drives acquisition, conversion, and expansion (not sales team).

**Characteristics:**
- Self-service signup
- Free trial or freemium
- Instant value (no onboarding required)
- Viral features (sharing, invitations)
- Usage drives conversion

**Examples:**
- Slack: Free tier, teams invite each other
- Zoom: Free 40-min meetings, participants see branding
- Dropbox: Free storage, referral bonuses

**Economics:**
```
PLG Unit Economics:
├─ Low CAC: $50-200 (product markets itself)
├─ High volume: 100,000+ trials
├─ Low ACV: $1,000-10,000 (SMB focus)
├─ Efficient growth: Land-and-expand
└─ Requires: Simple product, instant value
```

**PLG for Bibby:**

```java
@Service
public class ProductLedGrowthEngine {

    /**
     * Viral loop: Librarians share book recommendations
     */
    public void enableViralFeatures() {
        // 1. Shareable book lists
        // "Ms. Johnson's Summer Reading List"
        // Link includes Bibby branding

        // 2. Inter-library recommendations
        // "Schools like yours are reading..."
        // Other schools see Bibby powered this

        // 3. Parent notifications
        // "Your child checked out [Book]"
        // Email includes "Powered by Bibby"
    }

    /**
     * Instant value: Catalog first book in 30 seconds
     */
    public void optimizeTimeToValue() {
        // Onboarding flow:
        // 1. Scan barcode → Book appears → Done!
        // 2. "That took 30 seconds. Manual entry takes 5 minutes."
        // 3. "Add 10 more books to unlock advanced features"
    }

    /**
     * Usage-based conversion triggers
     */
    public void monitorConversionTriggers(User user) {
        Usage usage = usageTracker.getUsage(user);

        if (usage.booksAdded() >= 100) {
            // Power user behavior
            emailService.sendUpgradeOffer(user,
                "You've added 100 books! Unlock bulk import on Pro.");
        }

        if (usage.searchesPerWeek() > 50) {
            // Relying on product
            emailService.sendUpgradeOffer(user,
                "Your library is active! Pro adds advanced search.");
        }

        if (usage.daysActive() >= 30) {
            // Habit formed
            emailService.sendUpgradeOffer(user,
                "You've used Bibby for 30 days! Lock in annual pricing.");
        }
    }
}
```

**When PLG works:**
- Simple product (low learning curve)
- Clear value in < 15 minutes
- Self-service friendly
- Low ACV ($1K-$10K)
- Viral potential

**When PLG doesn't work:**
- Complex product (requires training)
- Slow time-to-value
- Requires customization
- High ACV (>$50K)
- Enterprise buyers (committees, procurement)

### Sales-Led Growth

**Definition:** Sales team drives acquisition, conversion, and expansion.

**Characteristics:**
- Demo-first (no self-service)
- Enterprise focus (high ACV)
- Customization/implementation
- Long sales cycles (3-12 months)
- Relationship-driven

**Examples:**
- Salesforce (enterprise deals)
- Oracle
- SAP
- Workday

**Economics:**
```
Sales-Led Unit Economics:
├─ High CAC: $5,000-50,000 (sales team, demos, travel)
├─ Low volume: 100-1,000 deals/year
├─ High ACV: $50,000-500,000+ (enterprise)
├─ Long payback: 18-36 months
└─ Requires: Complex product, customization, enterprise buyers
```

**When sales-led works:**
- Complex product (requires explanation)
- High ACV (>$50K)
- Enterprise buyers (multi-stakeholder)
- Customization required
- Compliance/security needs

**When sales-led doesn't work:**
- Simple product (overkill)
- Low ACV (<$10K) - economics don't work
- SMB customers (don't want to talk to sales)
- Self-service preferred

### Hybrid (The Best of Both)

**Most successful SaaS companies use hybrid:**

**Slack's hybrid model:**
```
Bottom-up (PLG):
├─ Teams self-serve on Free/Standard ($8/user)
├─ Usage triggers sales outreach at 100+ users
└─ 0-100 users: Pure PLG

Top-down (Sales-led):
├─ Enterprise team sells Plus ($15/user) to IT buyers
├─ Custom contracts, SSO, compliance
└─ 1,000+ users: Pure sales-led

Hybrid zone (100-1,000 users):
├─ Inside sales team
├─ Light-touch demos
└─ Self-service checkout with sales assist
```

**Atlassian's hybrid model:**
```
PLG motion:
├─ Self-serve Jira/Confluence for teams
├─ Free trials, instant value
└─ Land-and-expand within companies

Sales-led overlay:
├─ Enterprise team for 1,000+ user deals
├─ Data center migrations
└─ Strategic accounts
```

**Bibby's hybrid approach:**

```java
@Service
public class HybridGTMEngine {

    public GTMMotion determineMotion(Lead lead) {
        // Segment by size and signals

        if (lead.estimatedSeats() < 5) {
            return GTMMotion.PURE_PLG;
            // Self-serve, no sales touch
        }

        if (lead.estimatedSeats() >= 5 && lead.estimatedSeats() < 50) {
            return GTMMotion.HYBRID;
            // Self-serve with inside sales assist
        }

        if (lead.estimatedSeats() >= 50 || lead.isDistrict()) {
            return GTMMotion.PURE_SALES;
            // Enterprise sales team, custom demos
        }

        return GTMMotion.PURE_PLG; // Default
    }

    public void executeMotion(Lead lead, GTMMotion motion) {
        switch (motion) {
            case PURE_PLG -> {
                // No sales touch
                emailService.sendOnboardingSequence(lead);
                productService.optimizeForSelfServe(lead);
            }

            case HYBRID -> {
                // Light sales touch
                emailService.sendOnboardingSequence(lead);
                if (lead.getTrialDays() >= 7) {
                    salesService.assignInsideSalesRep(lead);
                    // Rep calls to help, not push
                }
            }

            case PURE_SALES -> {
                // Full sales process
                salesService.assignAccountExecutive(lead);
                salesService.scheduleDiscoveryCall(lead);
                // No product access until after demo
            }
        }
    }
}
```

---

## Part 5: Free Trial vs. Freemium vs. Demo-First

### Three Acquisition Models

**1. Free Trial**

**Structure:** Full product access for limited time (14-30 days)

```
Example: Bibby (current)
├─ 30-day free trial
├─ No credit card required
├─ Full Pro features
└─ Convert to paid or lose access

Pros:
✓ Low friction (no payment upfront)
✓ Try full product
✓ Clear conversion point (trial ends)

Cons:
✗ Time pressure (might not evaluate fully)
✗ Support costs for non-buyers
✗ Conversion rates: 10-25% typical
```

**Best practices:**
- 14-day trial for simple products
- 30-day trial for complex products
- Require credit card if ACV > $1,000 (reduces tire-kickers)
- Send conversion emails at days 3, 7, 14, 28

**2. Freemium**

**Structure:** Free forever tier + paid upgrades

```
Example: Notion
├─ Free: Personal use, unlimited pages
├─ Plus: $10/user/month (more features)
└─ Enterprise: Custom pricing

Pros:
✓ Viral growth (free users invite others)
✓ No time pressure (evaluate forever)
✓ Large user base

Cons:
✗ Low conversion (2-5% typical)
✗ High support costs
✗ Cannibalization (free is "good enough")
```

**When freemium works:**
- Consumer products
- Network effects
- Low marginal cost per user
- Viral sharing mechanics

**When freemium fails:**
- B2B products (businesses have budget)
- High support costs
- No viral mechanics
- Free tier is too good

**3. Demo-First**

**Structure:** Must talk to sales to see product

```
Example: Salesforce (enterprise)
├─ Request demo → Sales call → Demo → Proposal
├─ No free trial
└─ High-touch sales process

Pros:
✓ Qualify leads (no tire-kickers)
✓ Control narrative (sales can position)
✓ Relationship building

Cons:
✗ High friction (turns away SMB)
✗ Expensive (sales team required)
✗ Slow (weeks to see product)
```

**When demo-first works:**
- High ACV (>$50K)
- Complex product
- Requires customization
- Enterprise buyers

### Decision Matrix for Bibby

```
Customer Segment → Acquisition Model:

Personal libraries (< 1,000 books):
└─ Free trial (14 days) → Convert to Starter ($25/mo)

Small schools (1,000-5,000 books):
└─ Free trial (30 days) → Convert to Pro ($100/mo)

Large schools/districts (> 5,000 books):
└─ Demo-first → Enterprise sales → Custom pricing
```

**Implementation:**

```java
@Service
public class AcquisitionStrategyService {

    public AcquisitionModel determineModel(SignupRequest request) {
        LibrarySize size = estimateSize(request);

        return switch (size) {
            case PERSONAL -> new FreeTrialModel(14); // 14-day trial
            case SMALL_SCHOOL -> new FreeTrialModel(30); // 30-day trial
            case LARGE_SCHOOL -> new DemoFirstModel(); // Sales-led
            case DISTRICT -> new DemoFirstModel(); // Enterprise sales
        };
    }

    public void executeFreeTrialFlow(User user, int trialDays) {
        Trial trial = new Trial();
        trial.setUser(user);
        trial.setDays(trialDays);
        trial.setStartDate(LocalDate.now());
        trial.setEndDate(LocalDate.now().plusDays(trialDays));
        trial.setStatus(TrialStatus.ACTIVE);
        trialRepository.save(trial);

        // Unlock all Pro features during trial
        featureGateService.unlockTier(user, Tier.PRO);

        // Schedule conversion emails
        scheduleConversionCampaign(user, trialDays);
    }

    private void scheduleConversionCampaign(User user, int trialDays) {
        // Day 3: Quick win email ("Here's what you can do")
        emailScheduler.schedule(user, 3, EmailTemplate.TRIAL_QUICK_WIN);

        // Day 7: Value reinforcement ("You've saved X hours")
        emailScheduler.schedule(user, 7, EmailTemplate.TRIAL_VALUE);

        // Day trialDays - 3: Urgency ("Trial ending soon")
        emailScheduler.schedule(user, trialDays - 3, EmailTemplate.TRIAL_ENDING);

        // Day trialDays - 1: Final push ("Last chance")
        emailScheduler.schedule(user, trialDays - 1, EmailTemplate.TRIAL_FINAL);
    }
}
```

---

## Part 6: Horizontal vs. Vertical SaaS

### The Strategic Choice

**Horizontal SaaS:** Solves same problem across many industries

```
Example: Salesforce (CRM for everyone)
├─ Used by: Healthcare, finance, retail, manufacturing, etc.
├─ Value prop: Generic CRM needs
└─ Go-to-market: Broad appeal

Pros:
✓ Massive TAM (every company needs CRM)
✓ Economies of scale
✓ Platform effects

Cons:
✗ Intense competition
✗ Generic (not optimized for any vertical)
✗ Hard to differentiate
```

**Vertical SaaS:** Solves industry-specific problem

```
Example: Veeva (CRM for pharma)
├─ Used by: Pharmaceutical companies only
├─ Value prop: Pharma-specific workflows (FDA compliance, samples tracking)
└─ Go-to-market: Vertical conferences, industry publications

Pros:
✓ Deep expertise (hard to replicate)
✓ Higher willingness to pay
✓ Less competition
✓ Industry-specific features = defensibility

Cons:
✗ Smaller TAM (only one industry)
✗ Slower growth
✗ Concentration risk (industry downturn)
```

### Bibby: Vertical SaaS

**Bibby is vertical SaaS** for K-12 school libraries.

**Why vertical wins here:**
```
Generic library software (horizontal):
├─ Serves: Schools, public libraries, corporate libraries, personal
├─ Features: Generic cataloging, checkout
└─ Result: Mediocre fit for everyone

Bibby (vertical):
├─ Serves: K-12 school libraries only
├─ Features: Reading level filters, curriculum alignment, parent notifications
└─ Result: Perfect fit for schools, 10× better than horizontal
```

**Vertical advantages for Bibby:**
1. **Deep expertise:** Understand school librarian workflows intimately
2. **Purpose-built features:** Reading levels, curriculum standards (Common Core)
3. **Industry integrations:** Google Classroom, PowerSchool, Canvas
4. **Vertical GTM:** AASL conferences, school library publications
5. **Higher pricing:** Schools pay more for purpose-built tools

**When to go vertical:**
- Industry has specific workflows horizontal tools don't address
- Compliance/regulatory requirements
- Industry jargon and expertise matter
- Industry has buying power and budget
- Underserved by horizontal solutions

**When to go horizontal:**
- Problem is truly universal
- Scale advantages matter
- Can win on distribution
- Network effects across industries

---

## Part 7: Implementing Business Model Flexibility

### Building for Change

Your business model will evolve. Build flexibility into your architecture.

```java
// src/main/java/com/penrose/bibby/model/BusinessModelStrategy.java

/**
 * Strategy pattern for business model flexibility
 */
public interface PricingStrategy {
    BigDecimal calculatePrice(Customer customer, UsageData usage);
    boolean isEligibleForFeature(Customer customer, Feature feature);
    String getDisplayPrice();
}

/**
 * Tiered pricing (current model)
 */
@Component
public class TieredPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(Customer customer, UsageData usage) {
        Tier tier = customer.getCurrentTier();
        BillingPeriod period = customer.getBillingPeriod();

        return switch (period) {
            case MONTHLY -> tier.getMonthlyPrice();
            case ANNUAL -> tier.getAnnualPrice(); // 2 months free
        };
    }

    @Override
    public boolean isEligibleForFeature(Customer customer, Feature feature) {
        return customer.getCurrentTier().supports(feature);
    }

    @Override
    public String getDisplayPrice() {
        return "Starting at $25/month";
    }
}

/**
 * Usage-based pricing (future option)
 */
@Component
public class UsageBasedPricingStrategy implements PricingStrategy {

    private static final BigDecimal PRICE_PER_BOOK = new BigDecimal("0.02");
    private static final BigDecimal PRICE_PER_CHECKOUT = new BigDecimal("0.001");
    private static final BigDecimal MINIMUM_MONTHLY = new BigDecimal("10.00");

    @Override
    public BigDecimal calculatePrice(Customer customer, UsageData usage) {
        BigDecimal bookCost = PRICE_PER_BOOK
            .multiply(BigDecimal.valueOf(usage.booksAdded()));

        BigDecimal checkoutCost = PRICE_PER_CHECKOUT
            .multiply(BigDecimal.valueOf(usage.checkoutsProcessed()));

        BigDecimal total = bookCost.add(checkoutCost);

        // Enforce minimum
        return total.max(MINIMUM_MONTHLY);
    }

    @Override
    public boolean isEligibleForFeature(Customer customer, Feature feature) {
        // All features available in usage-based model
        return true;
    }

    @Override
    public String getDisplayPrice() {
        return "$0.02/book + $0.001/checkout (min $10/mo)";
    }
}

/**
 * Seat-based pricing (future option)
 */
@Component
public class SeatBasedPricingStrategy implements PricingStrategy {

    private static final BigDecimal PRICE_PER_SEAT = new BigDecimal("25.00");

    @Override
    public BigDecimal calculatePrice(Customer customer, UsageData usage) {
        int seats = customer.getActiveSeats();
        return PRICE_PER_SEAT.multiply(BigDecimal.valueOf(seats));
    }

    @Override
    public boolean isEligibleForFeature(Customer customer, Feature feature) {
        return true; // All features for all seats
    }

    @Override
    public String getDisplayPrice() {
        return "$25/user/month";
    }
}

/**
 * Service to switch pricing models
 */
@Service
public class BusinessModelService {

    private final Map<String, PricingStrategy> strategies;
    private PricingStrategy currentStrategy;

    public BusinessModelService(List<PricingStrategy> allStrategies) {
        this.strategies = allStrategies.stream()
            .collect(Collectors.toMap(
                s -> s.getClass().getSimpleName(),
                s -> s
            ));

        // Default to tiered
        this.currentStrategy = strategies.get("TieredPricingStrategy");
    }

    public void switchModel(String strategyName) {
        PricingStrategy newStrategy = strategies.get(strategyName);
        if (newStrategy != null) {
            this.currentStrategy = newStrategy;
            logger.info("Switched to pricing model: {}", strategyName);
        }
    }

    public BigDecimal calculateCustomerPrice(Customer customer, UsageData usage) {
        return currentStrategy.calculatePrice(customer, usage);
    }

    public boolean canAccessFeature(Customer customer, Feature feature) {
        return currentStrategy.isEligibleForFeature(customer, feature);
    }
}
```

### A/B Testing Business Models

```java
@Service
public class BusinessModelExperimentService {

    /**
     * Run experiments on pricing models
     */
    public Experiment runPricingExperiment(
            String experimentName,
            PricingStrategy controlStrategy,
            PricingStrategy variantStrategy,
            double variantPercentage) {

        Experiment experiment = new Experiment();
        experiment.setName(experimentName);
        experiment.setStartDate(LocalDate.now());
        experiment.setVariantPercentage(variantPercentage);

        // Assign new customers to control or variant
        customerRepository.findNewCustomersSince(experiment.getStartDate())
            .forEach(customer -> {
                boolean isVariant = random.nextDouble() < variantPercentage;
                PricingStrategy strategy = isVariant ? variantStrategy : controlStrategy;

                customer.setPricingStrategy(strategy.getClass().getSimpleName());
                customer.setExperimentCohort(isVariant ? "variant" : "control");
                customerRepository.save(customer);
            });

        return experiment;
    }

    /**
     * Analyze experiment results
     */
    public ExperimentResults analyzeExperiment(Experiment experiment) {
        // Compare control vs variant cohorts
        List<Customer> control = customerRepository
            .findByExperimentCohort("control", experiment);

        List<Customer> variant = customerRepository
            .findByExperimentCohort("variant", experiment);

        // Calculate metrics
        double controlConversion = calculateConversionRate(control);
        double variantConversion = calculateConversionRate(variant);

        double controlARPA = calculateARPA(control);
        double variantARPA = calculateARPA(variant);

        double controlLTV = calculateLTV(control);
        double variantLTV = calculateLTV(variant);

        return new ExperimentResults(
            experiment,
            controlConversion,
            variantConversion,
            controlARPA,
            variantARPA,
            controlLTV,
            variantLTV
        );
    }
}
```

---

## Deliverables

### 1. Business Model Canvas

Create a one-page canvas for Bibby:
- **Value Proposition:** What we create
- **Customer Segments:** Who we serve
- **Channels:** How we deliver
- **Revenue Streams:** How we capture value
- **Key Resources:** What we need
- **Key Activities:** What we do
- **Key Partners:** Who we work with
- **Cost Structure:** What we spend

### 2. Pricing Model Comparison

Build a spreadsheet comparing models:
- Flat-rate vs. seat-based vs. usage-based vs. tiered
- Forecast revenue for each under different scenarios
- Recommend the winner with justification

### 3. GTM Motion Strategy

Document Bibby's go-to-market:
- Which segments use PLG?
- Which segments use sales-led?
- Where's the hybrid zone?
- What triggers sales involvement?

### 4. Free Trial Optimization Plan

Design the perfect trial experience:
- Trial length (14 vs. 30 days?)
- Features included
- Conversion email sequence
- Success metrics (activation rate, trial-to-paid)

### 5. Business Model Flexibility Implementation

Implement strategy pattern in code:
- Support multiple pricing models
- Easy to switch or experiment
- A/B testing framework

---

## Reflection Questions

1. **Pricing Model:**
   - Why does tiered pricing work for most B2B SaaS?
   - When would usage-based pricing be better for Bibby?
   - How do you balance simplicity vs. optimization?

2. **GTM Motion:**
   - Can Bibby be pure PLG? Why or why not?
   - At what ACV does sales-led become viable?
   - How do hybrid models avoid channel conflict?

3. **Trial vs. Freemium:**
   - Why does freemium work for Dropbox but not Salesforce?
   - Should Bibby offer a free tier? Pros/cons?
   - How do you prevent trial abuse?

4. **Vertical vs. Horizontal:**
   - Could Bibby expand to public libraries? (Horizontal move)
   - What's the risk of being too vertical?
   - How do you know when to expand beyond your vertical?

5. **Model Evolution:**
   - When should you change your business model?
   - How do you migrate existing customers to new model?
   - What signals indicate your model isn't working?

---

## Week 15 Summary

You've learned the core business model patterns:

1. **Software archetypes:** SaaS, PaaS, Marketplace, Open Source
2. **Pricing models:** Flat-rate, seat-based, usage-based, tiered, freemium
3. **GTM motions:** PLG, sales-led, hybrid
4. **Acquisition:** Free trial, freemium, demo-first
5. **Market focus:** Vertical vs. horizontal SaaS
6. **Implementation:** Strategy pattern for model flexibility

**Key Insight:** Your business model is your business architecture. Pick patterns that align with your market, customer, and product complexity.

**For Bibby:**
- **Model:** Vertical SaaS for K-12 school libraries
- **Pricing:** Tiered (Starter/Pro/Enterprise)
- **GTM:** Hybrid (PLG for SMB, sales-led for enterprise)
- **Acquisition:** 30-day free trial
- **Why:** Schools have budget, product is simple enough for PLG but large deals need sales

---

## Looking Ahead: Week 16

Next week: **Pricing Strategy & Optimization**

You know the pricing models. Now you'll learn how to optimize within those models:
- Price anchoring and framing tactics
- When to raise prices (and how much)
- Grandfather pricing vs. forced migrations
- Discounting strategy (when to discount, when not to)
- Pricing experiments and A/B testing
- Psychological pricing ($99 vs $100)
- Value-based pricing negotiation

Plus: How to use pricing as a competitive weapon.

---

**Progress:** 15/52 weeks complete (29% of apprenticeship, Week 2 of Semester 2)

**Commit your work:**
```bash
git add apprenticeship/semester-2/week-15-business-model-patterns.md
git commit -m "Add Week 15: Business Model Patterns"
git push
```

Type "continue" when ready for Week 16.
