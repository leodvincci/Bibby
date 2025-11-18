# Week 12: Problem Prioritization

**Mentor Voice: Tech Executive**

> "The art of leadership is saying no, not saying yes. It is very easy to say yes." - Tony Blair
>
> You've discovered 100 customer problems. Each one is real, valuable, and solvable. Your backlog is overflowing. Engineers are eager to build. But you have finite resources: one team, one quarter, one shot at market traction. Which problem do you solve first? This is the make-or-break decision that separates successful products from failed ones. Great products aren't built by doing more—they're built by doing less, but doing it right. This week, you'll learn the frameworks top product leaders use to ruthlessly prioritize, make hard tradeoffs, and master the most important word in product management: "No."

---

## Learning Objectives

By the end of this week, you will:

1. Apply RICE scoring to quantify opportunity value
2. Use the Kano Model to classify feature types
3. Plot features on Value vs. Complexity matrices
4. Implement ICE scoring for rapid triage
5. Learn to say no effectively (without burning bridges)
6. Build a prioritization system into Bibby
7. Apply these frameworks to industrial automation projects

---

## Part 1: The Prioritization Problem

### The Tyranny of Good Ideas

```
Bibby's Product Backlog (Week 12):

High-Priority (according to someone):
├─ Inter-library loan network (Sales says this closes enterprise deals)
├─ Mobile app (Customers have been asking for 2 years)
├─ Advanced reporting (Top feature request in surveys)
├─ AI book recommendations (CEO's pet project)
├─ Barcode scanner integration (Schools need this)
├─ Patron self-checkout (Reduces librarian workload)
├─ MARC record export (Compliance requirement)
├─ Multi-language support (International expansion)
├─ API for third-party integrations (Developers keep asking)
├─ Performance optimization (App is slow with 10K+ books)
└─ ... 90 more "critical" features

Resources:
├─ 2 engineers
├─ 1 designer
├─ 13 weeks until end of quarter
└─ Reality: Can build 2-3 major features well, or 10 features poorly
```

**The Question:** Which 2-3 features will transform your business?

**The Stakes:**
- **Right choice:** 10× growth, market leadership, happy customers
- **Wrong choice:** Wasted quarter, competitor gains ground, team morale tanks

### Why Prioritization is Hard

**1. Everything Seems Important**
- Sales: "We'll close 10 deals if we build X"
- Support: "We're getting 50 tickets/week about Y"
- Engineering: "Technical debt Z is slowing us down"
- CEO: "Strategic initiative W aligns with our vision"

**Who's right?** Everyone. And no one.

**2. Hidden Costs**
- Building feature X blocks features Y and Z
- Opportunity cost: What could you have built instead?
- Maintenance cost: Every feature adds complexity forever
- Focus cost: More features = less polish on each

**3. Vocal Minority Bias**
- 5 enterprise customers loudly demand feature
- 5,000 silent users would prefer different feature
- Squeaky wheel gets the grease (but shouldn't always)

**4. Sunk Cost Fallacy**
- "We already spent 3 weeks on this feature"
- "But the CEO promised it to a customer"
- Past investment clouds future decisions

### The Framework Mindset

> "Without a framework, you're just arguing opinions. With a framework, you're making decisions."

Good prioritization frameworks:
1. **Quantifiable:** Turn gut feelings into numbers
2. **Consistent:** Same inputs always produce same outputs
3. **Transparent:** Everyone understands why decisions were made
4. **Fast:** Don't spend 3 weeks prioritizing a 2-week project

---

## Part 2: RICE Scoring

**RICE** = **R**each × **I**mpact × **C**onfidence / **E**ffort

Developed by Intercom, RICE forces you to quantify all dimensions of value.

### The Four Components

**Reach:** How many people will this affect in a given time period?
```
Examples:
- "1,000 users/quarter will use this feature"
- "100% of trial users will see this onboarding flow"
- "50 schools/month will benefit from this integration"

Measurement: Count of people affected per time period
```

**Impact:** How much will this affect each person?
```
Scale:
3 = Massive impact (fundamentally changes their workflow)
2 = High impact (significantly improves their experience)
1 = Medium impact (noticeable improvement)
0.5 = Low impact (nice to have)
0.25 = Minimal impact (barely noticeable)

Be honest: Most features are 0.5-1.0, not 3.0
```

**Confidence:** How sure are you about your estimates?
```
Scale:
100% = High confidence (strong data, proven approach)
80% = Medium confidence (some data, reasonable assumptions)
50% = Low confidence (gut feeling, untested hypothesis)

Use this to discount speculative projects
```

**Effort:** How much work will this take?
```
Measurement: Person-months
- 0.5 = One engineer, 2 weeks
- 1.0 = One engineer, 1 month
- 3.0 = One engineer, 3 months OR 3 engineers, 1 month

Include design, QA, deployment, documentation
```

### RICE Formula

```
RICE Score = (Reach × Impact × Confidence) / Effort

Example: Inter-library loan network

Reach: 500 schools/quarter will use this
Impact: 2.0 (significantly improves library value)
Confidence: 80% (we've talked to 20 schools who confirmed they'd use it)
Effort: 6 person-months (complex feature, backend + frontend + testing)

RICE = (500 × 2.0 × 0.80) / 6
     = 800 / 6
     = 133.3
```

### Comparing Features with RICE

```java
// src/main/java/com/penrose/bibby/prioritization/RICEScore.java

public record RICEScore(
    String featureName,
    int reach,              // People affected per quarter
    double impact,          // 0.25, 0.5, 1.0, 2.0, 3.0
    double confidence,      // 0.5, 0.8, 1.0
    double effort,          // Person-months
    double score            // Calculated
) {
    public static RICEScore calculate(
            String name,
            int reach,
            double impact,
            double confidence,
            double effort) {

        double score = (reach * impact * confidence) / effort;
        return new RICEScore(name, reach, impact, confidence, effort, score);
    }

    public String getRationale() {
        return String.format(
            "%s affects %d users with %.1fx impact (%.0f%% confident) for %.1f PM of effort → RICE: %.1f",
            featureName, reach, impact, confidence * 100, effort, score
        );
    }
}
```

**Using RICE in Bibby:**

```java
@Service
public class FeaturePrioritizationService {

    public List<RICEScore> prioritizeQ1Features() {
        List<RICEScore> features = List.of(
            // Inter-library loan network
            RICEScore.calculate(
                "Inter-library loan network",
                500,    // 500 schools/quarter
                2.0,    // High impact (new capability)
                0.8,    // Medium-high confidence
                6.0     // 6 person-months
            ), // Score: 133.3

            // Mobile app
            RICEScore.calculate(
                "Mobile app",
                2000,   // All users would use mobile
                1.0,    // Medium impact (convenience)
                0.5,    // Low confidence (haven't validated)
                12.0    // 12 person-months (iOS + Android)
            ), // Score: 83.3

            // Advanced reporting
            RICEScore.calculate(
                "Advanced reporting dashboard",
                1500,   // Most schools need better reports
                1.5,    // High-medium impact (saves time)
                1.0,    // High confidence (clear requirements)
                2.0     // 2 person-months
            ), // Score: 1125.0 ← WINNER

            // AI recommendations
            RICEScore.calculate(
                "AI book recommendations",
                800,    // Schools with 1000+ books
                0.5,    // Low impact (nice-to-have)
                0.5,    // Low confidence (untested ML model)
                4.0     // 4 person-months
            ), // Score: 50.0

            // Barcode scanner
            RICEScore.calculate(
                "Barcode scanner integration",
                1200,   // Schools doing physical inventory
                2.0,    // High impact (major workflow improvement)
                0.8,    // Medium-high confidence
                1.5     // 1.5 person-months
            ), // Score: 1280.0 ← SECOND WINNER

            // Performance optimization
            RICEScore.calculate(
                "Performance optimization (10K+ books)",
                300,    // Only large libraries affected
                2.0,    // High impact for those affected
                1.0,    // High confidence (clear problem)
                3.0     // 3 person-months
            )  // Score: 200.0
        );

        // Sort by RICE score descending
        return features.stream()
            .sorted(Comparator.comparing(RICEScore::score).reversed())
            .toList();
    }
}
```

**Q1 Priority:**
```
1. Advanced reporting: 1125.0 (Build this first)
2. Barcode scanner: 1280.0 (Build this second)
3. Performance optimization: 200.0 (Maybe Q2)
4. Inter-library loan: 133.3 (Defer to Q2)
5. Mobile app: 83.3 (Not yet)
6. AI recommendations: 50.0 (Say no)
```

**Key Insight:** The two "unsexy" features (reporting, barcode scanner) beat the "exciting" features (mobile app, AI) because they have better reach-to-effort ratios.

---

## Part 3: The Kano Model

**Problem with RICE:** It treats all features the same. But customers react differently to different feature types.

**The Kano Model** categorizes features by how they affect customer satisfaction.

### Three Feature Types

**1. Must-Haves (Basic Expectations)**
- **Definition:** If missing, customers are dissatisfied. If present, customers are neutral.
- **Examples:**
  - Library software must let you catalog books
  - Cars must have brakes
  - Hotels must have clean sheets
- **Characteristic:** Absence kills you. Presence doesn't differentiate you.
- **Investment strategy:** Do the minimum to meet expectations, don't over-invest

**2. Performance (Satisfiers)**
- **Definition:** More is better. Linear relationship between performance and satisfaction.
- **Examples:**
  - Faster search results
  - More storage capacity
  - Better uptime
- **Characteristic:** Direct ROI. Every improvement increases satisfaction.
- **Investment strategy:** Invest until diminishing returns

**3. Delighters (Excitement)**
- **Definition:** If missing, customers don't care. If present, customers are delighted.
- **Examples:**
  - Bibby automatically orders books trending in your genre
  - Car plays your favorite song when you start it
  - Hotel leaves personalized welcome note
- **Characteristic:** Absence doesn't hurt. Presence creates wow moment.
- **Investment strategy:** Pick 1-2 strategic delighters, skip the rest

### Kano Classification for Bibby

```java
public enum KanoCategory {
    MUST_HAVE,      // Table stakes
    PERFORMANCE,    // More is better
    DELIGHTER       // Unexpected wow
}

@Service
public class KanoClassifier {

    public record KanoFeature(
        String name,
        KanoCategory category,
        String reasoning
    ) {}

    public List<KanoFeature> classifyBibbyFeatures() {
        return List.of(
            // Must-haves (don't differentiate, but required)
            new KanoFeature(
                "Basic book cataloging",
                KanoCategory.MUST_HAVE,
                "Can't sell library software without this"
            ),
            new KanoFeature(
                "Search by title/author",
                KanoCategory.MUST_HAVE,
                "Customers expect this in any library system"
            ),
            new KanoFeature(
                "Checkout/check-in",
                KanoCategory.MUST_HAVE,
                "Core workflow must work"
            ),
            new KanoFeature(
                "99% uptime",
                KanoCategory.MUST_HAVE,
                "SaaS baseline expectation"
            ),

            // Performance (linear satisfaction)
            new KanoFeature(
                "Search speed (< 100ms)",
                KanoCategory.PERFORMANCE,
                "Faster = better. Diminishing returns after 100ms."
            ),
            new KanoFeature(
                "Report customization options",
                KanoCategory.PERFORMANCE,
                "More options = more use cases covered"
            ),
            new KanoFeature(
                "Number of concurrent users",
                KanoCategory.PERFORMANCE,
                "Larger schools need more capacity"
            ),
            new KanoFeature(
                "Storage capacity",
                KanoCategory.PERFORMANCE,
                "More books supported = more customer segments"
            ),

            // Delighters (unexpected wow)
            new KanoFeature(
                "AI auto-categorization",
                KanoCategory.DELIGHTER,
                "Nobody expects this. Would love it if present."
            ),
            new KanoFeature(
                "Reading level predictor",
                KanoCategory.DELIGHTER,
                "Helps teachers, not expected in library software"
            ),
            new KanoFeature(
                "Automatic book club suggestions",
                KanoCategory.DELIGHTER,
                "Goes beyond library management into programming"
            ),
            new KanoFeature(
                "Parent notification integration",
                KanoCategory.DELIGHTER,
                "Unexpected value-add for school librarians"
            )
        );
    }
}
```

### Kano Strategy

**Resource allocation:**
```
Must-haves: 40% of engineering time
├─ Goal: Meet baseline expectations reliably
├─ Don't over-invest (diminishing returns)
└─ Focus on stability, not features

Performance: 50% of engineering time
├─ Goal: Be measurably better than competitors
├─ Invest in top 3 performance dimensions
└─ Example: Search speed, report quality, uptime

Delighters: 10% of engineering time
├─ Goal: Create 1-2 memorable wow moments
├─ Pick strategically (not random fun)
└─ Example: AI auto-categorization (saves hours)
```

**Common Mistake:** Spending 50% of time on delighters, 20% on must-haves. This creates a buggy product with cool features nobody can rely on.

---

## Part 4: Value vs. Complexity Matrix

**Simple 2×2 matrix:** Plot features by value (to customer) and complexity (to build).

```
High Value │
           │   Do Next        Do First
           │   (Plan)         (Quick Wins)
           │
           │─────────────┼─────────────
           │
           │   Don't Do       Do Later
           │   (Avoid)        (Fill-ins)
           │
Low Value  │
           └───────────────────────────
              High             Low
              Complexity    Complexity
```

### Four Quadrants

**Q1: Do First (High Value, Low Complexity)**
- **Strategy:** Start here. Quick wins build momentum.
- **Examples:**
  - Keyboard shortcuts for power users
  - Export to CSV
  - Email notifications for holds
- **Timeline:** This sprint

**Q2: Do Next (High Value, High Complexity)**
- **Strategy:** Plan carefully. Worth the investment if you can resource it.
- **Examples:**
  - Inter-library loan network
  - Mobile app (if validated)
  - Advanced analytics
- **Timeline:** Next quarter

**Q3: Do Later (Low Value, Low Complexity)**
- **Strategy:** Fill-in work when you have spare cycles. Don't prioritize.
- **Examples:**
  - Dark mode
  - Custom themes
  - Fun Easter eggs
- **Timeline:** Whenever

**Q4: Don't Do (Low Value, High Complexity)**
- **Strategy:** Kill these. Opportunity cost too high.
- **Examples:**
  - Build your own authentication system (use Auth0)
  - Custom email server (use SendGrid)
  - Blockchain integration (why?)
- **Timeline:** Never

### Implementation

```java
@Service
public class ValueComplexityMatrix {

    public record MatrixFeature(
        String name,
        int value,       // 1-10
        int complexity,  // 1-10
        Quadrant quadrant
    ) {}

    public enum Quadrant {
        DO_FIRST,    // High value, low complexity
        DO_NEXT,     // High value, high complexity
        DO_LATER,    // Low value, low complexity
        DONT_DO      // Low value, high complexity
    }

    public Quadrant classify(int value, int complexity) {
        boolean highValue = value >= 6;
        boolean lowComplexity = complexity <= 5;

        if (highValue && lowComplexity) return Quadrant.DO_FIRST;
        if (highValue && !lowComplexity) return Quadrant.DO_NEXT;
        if (!highValue && lowComplexity) return Quadrant.DO_LATER;
        return Quadrant.DONT_DO;
    }

    public List<MatrixFeature> plotBibbyFeatures() {
        return List.of(
            new MatrixFeature("Barcode scanner", 9, 3, classify(9, 3)),      // DO FIRST
            new MatrixFeature("Export to CSV", 7, 2, classify(7, 2)),        // DO FIRST
            new MatrixFeature("Email notifications", 8, 2, classify(8, 2)),  // DO FIRST

            new MatrixFeature("Mobile app", 8, 9, classify(8, 9)),           // DO NEXT
            new MatrixFeature("Advanced analytics", 9, 8, classify(9, 8)),   // DO NEXT
            new MatrixFeature("API platform", 7, 7, classify(7, 7)),         // DO NEXT

            new MatrixFeature("Dark mode", 4, 3, classify(4, 3)),            // DO LATER
            new MatrixFeature("Custom themes", 3, 4, classify(3, 4)),        // DO LATER

            new MatrixFeature("Build own auth", 2, 9, classify(2, 9)),       // DON'T DO
            new MatrixFeature("Blockchain ledger", 1, 10, classify(1, 10))   // DON'T DO
        );
    }

    public void printMatrix() {
        Map<Quadrant, List<MatrixFeature>> grouped = plotBibbyFeatures().stream()
            .collect(Collectors.groupingBy(MatrixFeature::quadrant));

        System.out.println("📊 Value vs. Complexity Matrix\n");

        System.out.println("🚀 DO FIRST (High Value, Low Complexity):");
        grouped.getOrDefault(Quadrant.DO_FIRST, List.of())
            .forEach(f -> System.out.println("  ✓ " + f.name()));

        System.out.println("\n📅 DO NEXT (High Value, High Complexity):");
        grouped.getOrDefault(Quadrant.DO_NEXT, List.of())
            .forEach(f -> System.out.println("  • " + f.name()));

        System.out.println("\n⏳ DO LATER (Low Value, Low Complexity):");
        grouped.getOrDefault(Quadrant.DO_LATER, List.of())
            .forEach(f -> System.out.println("  - " + f.name()));

        System.out.println("\n🚫 DON'T DO (Low Value, High Complexity):");
        grouped.getOrDefault(Quadrant.DONT_DO, List.of())
            .forEach(f -> System.out.println("  ✗ " + f.name()));
    }
}
```

---

## Part 5: ICE Scoring (Rapid Triage)

**ICE** = **I**mpact × **C**onfidence × **E**ase

Simpler than RICE. Use for rapid prioritization when you have 50+ ideas and need to triage fast.

### The Three Components

**Impact:** How much will this move the needle?
- Scale: 1-10
- 10 = Transform the business
- 5 = Noticeable improvement
- 1 = Minimal impact

**Confidence:** How sure are you?
- Scale: 1-10
- 10 = Proven, strong data
- 5 = Reasonable hypothesis
- 1 = Wild guess

**Ease:** How easy to build?
- Scale: 1-10
- 10 = One day of work
- 5 = One week of work
- 1 = One quarter of work

### ICE Formula

```
ICE Score = (Impact + Confidence + Ease) / 3

Simple average. No multiplication (unlike RICE).
```

**Example:**

```java
public record ICEScore(
    String name,
    int impact,     // 1-10
    int confidence, // 1-10
    int ease,       // 1-10
    double score    // Average
) {
    public static ICEScore calculate(String name, int impact, int confidence, int ease) {
        double score = (impact + confidence + ease) / 3.0;
        return new ICEScore(name, impact, confidence, ease, score);
    }
}

// Quick triage of 10 ideas
List<ICEScore> ideas = List.of(
    ICEScore.calculate("Add undo button", 6, 10, 9),              // 8.3
    ICEScore.calculate("Rebuild entire UI", 8, 7, 1),             // 5.3
    ICEScore.calculate("Keyboard shortcuts", 7, 9, 9),            // 8.3
    ICEScore.calculate("Multi-language support", 9, 6, 2),        // 5.7
    ICEScore.calculate("Fix slow search", 9, 10, 8),              // 9.0 ← Winner
    ICEScore.calculate("Add dark mode", 4, 8, 7),                 // 6.3
    ICEScore.calculate("AI chatbot", 5, 3, 2),                    // 3.3
    ICEScore.calculate("Improve error messages", 7, 10, 9),       // 8.7
    ICEScore.calculate("Add bulk import", 8, 9, 7),               // 8.0
    ICEScore.calculate("Build recommendation engine", 6, 4, 3)    // 4.3
);

// Top 3: Fix slow search (9.0), Improve error messages (8.7), Keyboard shortcuts (8.3)
```

**When to use ICE vs RICE:**
- **ICE:** Brainstorming, early triage, hundreds of ideas
- **RICE:** Quarterly planning, major features, resource allocation

---

## Part 6: The Art of Saying No

**The hardest skill:** Every yes to one feature is a no to ten others.

### Why Saying No is Hard

**1. Social pressure:**
- Don't want to disappoint stakeholders
- Fear of seeming uncooperative
- Desire to be liked

**2. FOMO (Fear of Missing Out):**
- What if this was the winning feature?
- Competitor might build it
- Customer might leave

**3. Optimism bias:**
- "We can do both!"
- "It won't take that long"
- "We'll find a way"

**Reality:** You can't do both. It will take longer. Resources are finite.

### How to Say No Effectively

**1. No, because [framework]:**

❌ **Bad:** "No, I don't think that's a good idea."
✅ **Good:** "I ran this through our RICE framework. It scores 45, while our cutoff for this quarter is 100. Here's the scoring breakdown..."

**Why this works:** Framework removes personal opinion. You're not rejecting the person, the framework is rejecting the idea.

**2. No, but later:**

❌ **Bad:** "We're not building that."
✅ **Good:** "Great idea. I'm adding it to the Q3 backlog. For Q1, we're focused on [higher priority items]. Let's revisit in June."

**Why this works:** Acknowledges the idea's value, sets expectations, provides timeline.

**3. No, with alternatives:**

❌ **Bad:** "Mobile app won't happen."
✅ **Good:** "Mobile app would take 12 months and $500K. Instead, we made the web app responsive—works on mobile browsers. This delivered 80% of the value in 2 weeks. Can we try this first?"

**Why this works:** Shows you understood the underlying need, proposed cheaper solution.

**4. No, but you can:**

❌ **Bad:** "That's not on our roadmap."
✅ **Good:** "We're not building that, but our API lets you build it yourself. Here's documentation. If you build something useful, we might acquire it from you."

**Why this works:** Empowers them, turns potential detractor into potential partner.

### The "No Template"

```java
@Service
public class RequestResponseService {

    public String generateNoResponse(FeatureRequest request) {
        RICEScore score = riceCalculator.calculate(request);
        double cutoff = 100.0;

        return String.format("""
            Hi %s,

            Thanks for the suggestion: "%s"

            I evaluated it using our prioritization framework:
            • Reach: %d users/quarter
            • Impact: %.1fx
            • Confidence: %.0f%%
            • Effort: %.1f person-months
            • RICE Score: %.1f

            Our Q1 cutoff is %.1f (based on team capacity).

            This scores %.1f, which means it's not making the cut for Q1.

            However, I'm adding it to our Q2 backlog. We'll re-evaluate
            in April when we do Q2 planning.

            If circumstances change (higher demand, lower effort estimate),
            we can reprioritize. Let me know if you have additional data
            that might affect the scoring.

            Thanks for thinking about how to improve Bibby!

            Best,
            [Your Name]
            """,
            request.requesterName(),
            request.featureName(),
            score.reach(),
            score.impact(),
            score.confidence() * 100,
            score.effort(),
            score.score(),
            cutoff,
            score.score()
        );
    }
}
```

### Saying No to Leadership

**Scenario:** CEO wants to build AI features because "everyone is doing AI."

❌ **Bad:** "That's a bad idea."
✅ **Good:**

```
"I love the strategic thinking around AI. Let me show you what I found:

[Present analysis]
AI book recommendations:
├─ Reach: 800 schools (only large libraries benefit)
├─ Impact: 0.5x (nice-to-have, not must-have)
├─ Effort: 4 person-months
├─ RICE: 50
├─ Risk: ML model might not work well (low confidence)

vs.

Barcode scanner integration:
├─ Reach: 1,200 schools (most do physical inventory)
├─ Impact: 2.0x (major workflow improvement)
├─ Effort: 1.5 person-months
├─ RICE: 1,280
├─ Risk: Low (proven technology)

Barcode scanner is 25× higher priority by our framework.

Here's my proposal:
- Q1: Build barcode scanner (high ROI, proven)
- Q2: Pilot AI with 10 beta schools (validate assumptions)
- Q3: Roll out AI if pilot succeeds

This de-risks AI while delivering immediate value with barcode scanner.

Thoughts?"
```

**Why this works:**
- Acknowledges CEO's vision
- Uses data, not opinion
- Proposes alternative that addresses concern
- De-risks with pilot approach
- Asks for input (collaborative, not confrontational)

---

## Part 7: Industrial Automation Prioritization

### The Capital Expenditure Problem

Industrial projects compete for CapEx budget. Different prioritization dynamics.

**Key differences:**
1. **Safety > Everything:** Safety improvements jump to top of queue
2. **Payback period:** Must show < 2-year payback (often < 1 year)
3. **Risk aversion:** Proven tech beats cutting-edge tech
4. **Uptime obsession:** Downtime costs overwhelm other factors

### Industrial RICE

Modified RICE for industrial automation:

```java
public record IndustrialRICE(
    String projectName,
    double annualSavings,      // Dollars saved per year
    double downtimePrevented,  // Hours of downtime prevented
    double safetyImprovement,  // 0-10 scale
    double confidence,         // 0-1 (0.5, 0.8, 1.0)
    double capEx,              // Capital expenditure (dollars)
    double score
) {
    public static IndustrialRICE calculate(
            String name,
            double savings,
            double downtime,
            double safety,
            double confidence,
            double capEx) {

        // Downtime is extremely expensive ($50K/hour)
        double downtimeValue = downtime * 50000;

        // Safety multiplier (regulatory requirement)
        double safetyMultiplier = safety >= 8 ? 2.0 : 1.0;

        // Total value
        double totalValue = (savings + downtimeValue) * safetyMultiplier * confidence;

        // Score = NPV over 5 years / CapEx
        double npv5yr = totalValue * 5 - capEx;
        double score = npv5yr / capEx;

        return new IndustrialRICE(name, savings, downtime, safety, confidence, capEx, score);
    }
}
```

**Example: Kinder Morgan Compressor Station**

```java
List<IndustrialRICE> projects = List.of(
    // Predictive maintenance ML model
    IndustrialRICE.calculate(
        "Predictive maintenance ML",
        1_600_000,  // $1.6M/year savings (prevented failures)
        32,         // 32 hours downtime prevented/year
        7,          // Moderate safety improvement
        0.8,        // Medium-high confidence
        250_000     // $250K CapEx (sensors + software)
    ), // Score: 54.4

    // Automated valve control
    IndustrialRICE.calculate(
        "Automated emergency valve control",
        200_000,    // $200K/year savings (faster response)
        5,          // 5 hours downtime prevented
        9,          // Major safety improvement (gas leak prevention)
        1.0,        // High confidence (proven tech)
        150_000     // $150K CapEx
    ), // Score: 23.7 (but safety multiplier makes it high priority)

    // Advanced scheduling software
    IndustrialRICE.calculate(
        "Advanced maintenance scheduling",
        300_000,    // $300K/year efficiency gains
        0,          // No downtime impact
        3,          // Minimal safety impact
        0.8,        // Medium-high confidence
        100_000     // $100K CapEx
    ), // Score: 11.0

    // Remote monitoring dashboard
    IndustrialRICE.calculate(
        "Remote monitoring dashboard",
        100_000,    // $100K/year (faster issue detection)
        8,          // 8 hours downtime prevented
        6,          // Moderate safety (earlier warnings)
        1.0,        // High confidence
        80_000      // $80K CapEx
    )  // Score: 30.0
);

// Priority order (with safety override):
// 1. Predictive maintenance (54.4 score)
// 2. Remote monitoring (30.0 score)
// 3. Automated valves (23.7 score, but safety critical → may jump to #1)
// 4. Scheduling software (11.0 score)
```

**Key Insight:** Safety projects with scores > 20 often get automatic approval regardless of ranking.

---

## Part 8: Building the Prioritization System

Let's implement a complete prioritization system in Bibby:

```java
// src/main/java/com/penrose/bibby/prioritization/PrioritizationService.java

@Service
public class PrioritizationService {

    public record FeatureAnalysis(
        String name,
        RICEScore rice,
        KanoCategory kano,
        Quadrant quadrant,
        ICEScore ice,
        Priority priority,
        String recommendation
    ) {}

    public enum Priority {
        P0_CRITICAL,    // This quarter, must do
        P1_HIGH,        // This quarter, should do
        P2_MEDIUM,      // Next quarter
        P3_LOW,         // Backlog
        P4_WONT_DO      // Reject
    }

    /**
     * Comprehensive prioritization using multiple frameworks
     */
    public FeatureAnalysis analyze(FeatureRequest request) {
        // Calculate RICE
        RICEScore rice = calculateRICE(request);

        // Classify Kano
        KanoCategory kano = classifyKano(request);

        // Plot on matrix
        Quadrant quadrant = plotMatrix(request);

        // Calculate ICE
        ICEScore ice = calculateICE(request);

        // Determine priority
        Priority priority = determinePriority(rice, kano, quadrant);

        // Generate recommendation
        String recommendation = generateRecommendation(
            rice, kano, quadrant, priority
        );

        return new FeatureAnalysis(
            request.name(),
            rice,
            kano,
            quadrant,
            ice,
            priority,
            recommendation
        );
    }

    private Priority determinePriority(
            RICEScore rice,
            KanoCategory kano,
            Quadrant quadrant) {

        // Must-haves with high RICE → P0
        if (kano == KanoCategory.MUST_HAVE && rice.score() > 100) {
            return Priority.P0_CRITICAL;
        }

        // Do First quadrant + high RICE → P0
        if (quadrant == Quadrant.DO_FIRST && rice.score() > 200) {
            return Priority.P0_CRITICAL;
        }

        // Do First quadrant → P1
        if (quadrant == Quadrant.DO_FIRST) {
            return Priority.P1_HIGH;
        }

        // Do Next with very high RICE → P1
        if (quadrant == Quadrant.DO_NEXT && rice.score() > 500) {
            return Priority.P1_HIGH;
        }

        // Do Next → P2
        if (quadrant == Quadrant.DO_NEXT) {
            return Priority.P2_MEDIUM;
        }

        // Do Later → P3
        if (quadrant == Quadrant.DO_LATER) {
            return Priority.P3_LOW;
        }

        // Don't Do → P4
        return Priority.P4_WONT_DO;
    }

    private String generateRecommendation(
            RICEScore rice,
            KanoCategory kano,
            Quadrant quadrant,
            Priority priority) {

        StringBuilder rec = new StringBuilder();

        rec.append("Priority: ").append(priority).append("\n\n");

        rec.append("Analysis:\n");
        rec.append("• RICE Score: ").append(String.format("%.1f", rice.score())).append("\n");
        rec.append("• Kano: ").append(kano).append("\n");
        rec.append("• Quadrant: ").append(quadrant).append("\n\n");

        switch (priority) {
            case P0_CRITICAL -> rec.append(
                "🚨 BUILD NOW\n" +
                "This is critical for business success. Allocate resources immediately."
            );
            case P1_HIGH -> rec.append(
                "✅ BUILD THIS QUARTER\n" +
                "High value, fits in current roadmap. Schedule in next sprint."
            );
            case P2_MEDIUM -> rec.append(
                "📅 NEXT QUARTER\n" +
                "Good idea, but lower priority than current work. Add to Q2 backlog."
            );
            case P3_LOW -> rec.append(
                "⏳ BACKLOG\n" +
                "Low priority. Build when you have spare cycles."
            );
            case P4_WONT_DO -> rec.append(
                "🚫 DON'T BUILD\n" +
                "Low value relative to effort. Opportunity cost too high."
            );
        }

        return rec.toString();
    }
}
```

### CLI Command for Prioritization

```java
@Command(command = "prioritize", description = "Analyze feature priority")
public void prioritizeFeature(
        @Option(longNames = "name") String name,
        @Option(longNames = "reach") int reach,
        @Option(longNames = "impact") double impact,
        @Option(longNames = "confidence") double confidence,
        @Option(longNames = "effort") double effort) {

    FeatureRequest request = new FeatureRequest(
        name, reach, impact, confidence, effort
    );

    FeatureAnalysis analysis = prioritizationService.analyze(request);

    System.out.println("📊 Feature Prioritization Analysis");
    System.out.println("─────────────────────────────────\n");
    System.out.println("Feature: " + analysis.name());
    System.out.println();
    System.out.println(analysis.recommendation());
    System.out.println();
    System.out.println("Detailed Scoring:");
    System.out.println("  RICE: " + analysis.rice().getRationale());
    System.out.println("  Kano: " + analysis.kano());
    System.out.println("  Matrix: " + analysis.quadrant());
}
```

**Usage:**

```bash
bibby> prioritize --name "Barcode scanner" --reach 1200 --impact 2.0 --confidence 0.8 --effort 1.5

📊 Feature Prioritization Analysis
─────────────────────────────────

Feature: Barcode scanner

Priority: P0_CRITICAL

Analysis:
• RICE Score: 1280.0
• Kano: PERFORMANCE
• Quadrant: DO_FIRST

🚨 BUILD NOW
This is critical for business success. Allocate resources immediately.

Detailed Scoring:
  RICE: Barcode scanner affects 1200 users with 2.0x impact (80% confident) for 1.5 PM of effort → RICE: 1280.0
  Kano: PERFORMANCE
  Matrix: DO_FIRST
```

---

## Deliverables

### 1. Prioritization Framework Document

Create a one-page framework guide:
- When to use RICE vs ICE vs Matrix
- Scoring guidelines (what's a 2.0 impact? What's 0.8 confidence?)
- Decision thresholds (what RICE score is "build now"?)
- Review cadence (weekly triage? quarterly planning?)

### 2. Feature Scoring Sheet

Build a spreadsheet or CLI tool:
- Input: Feature request details
- Output: RICE, ICE, Kano, Quadrant, Priority
- Historical log: All features scored (for audit trail)

### 3. Roadmap Based on Prioritization

Apply frameworks to Bibby's backlog:
- Score all 20+ feature ideas
- Create Q1/Q2/Q3 roadmap based on scores
- Document why each feature made cut (or didn't)

### 4. "No" Response Templates

Create 5 templates for saying no:
- To customers
- To leadership
- To engineers
- To sales team
- To yourself (killing your own ideas)

### 5. Industrial Case Study

Pick 5 industrial automation projects:
- Score each using modified RICE
- Prioritize them
- Justify the ranking
- Identify which would get funding and why

---

## Reflection Questions

1. **RICE Scoring:**
   - Which component is hardest to estimate accurately?
   - How do you avoid optimism bias in effort estimates?
   - When should you update RICE scores for in-flight features?

2. **Kano Model:**
   - Can a feature move from delighter to must-have over time? (Yes: Tesla autopilot)
   - Should you ever skip must-haves to build delighters?
   - How do you identify delighters customers don't know they want?

3. **Saying No:**
   - Why is saying no harder than saying yes?
   - How do you say no to a feature the CEO is personally excited about?
   - What's the difference between "not now" and "never"?

4. **Opportunity Cost:**
   - If you build feature X, what are you NOT building?
   - How do you quantify the cost of distraction?
   - Can you recover from building the wrong thing?

5. **Industrial Context:**
   - Why do industrial projects prioritize differently than SaaS?
   - How does safety override other factors?
   - What's the role of regulatory compliance in prioritization?

---

## Common Mistakes

**1. Not Using a Framework**
- Prioritizing by whoever yells loudest
- Building "cool" features instead of valuable ones
- Let politics override data

**2. Analysis Paralysis**
- Spending 2 weeks scoring a 1-week project
- Waiting for perfect data before deciding
- Over-engineering the prioritization system

**3. Ignoring Effort**
- Only looking at value, not effort
- Assuming everything takes "about a week"
- Not including QA, docs, deployment in effort

**4. Confusing Urgent with Important**
- Customer escalation feels urgent (build now!)
- But might be edge case affecting 1 user
- Important = high reach, high impact

**5. Can't Say No**
- Trying to build everything
- Roadmap has 50 "P0" items
- Team burns out, nothing ships

---

## Week 12 Summary

You've learned how to ruthlessly prioritize:

1. **RICE Scoring:** Reach × Impact × Confidence / Effort (quantifies everything)
2. **Kano Model:** Must-haves vs Performance vs Delighters (different satisfaction curves)
3. **Value vs Complexity:** 2×2 matrix (Do First, Do Next, Do Later, Don't Do)
4. **ICE Scoring:** Impact × Confidence × Ease (rapid triage)
5. **Saying No:** Use frameworks, not opinions. Acknowledge value, explain tradeoffs.

**Key Insight:** Great products are built by saying no to 90% of ideas, so you can say yes to the right 10%.

**For Bibby:** Barcode scanner (RICE: 1280) and reporting (RICE: 1125) beat mobile app (RICE: 83) and AI (RICE: 50). Build the "boring" high-value features first.

**For Industrial Automation:** Safety and downtime prevention override pure ROI. A project preventing one catastrophic failure (insurance value) may rank higher than steady efficiency gains.

---

## Looking Ahead: Week 13

Next week: **Opportunity Thesis (Semester 1 Capstone)**

You've spent 12 weeks learning markets, customers, problems, value, and prioritization. Now you'll synthesize everything into an **Opportunity Thesis**—a single document that makes the case for why Bibby should exist, who will pay for it, and how you'll win.

This is your investor pitch, your strategic plan, and your north star for execution.

---

**Progress:** 12/52 weeks complete (SEMESTER 1 COMPLETE!)

**Commit your work:**
```bash
git add apprenticeship/semester-1/week-12-problem-prioritization.md
git commit -m "Add Week 12: Problem Prioritization"
git push
```

Type "continue" when ready for Week 13 (Semester 1 Capstone).
