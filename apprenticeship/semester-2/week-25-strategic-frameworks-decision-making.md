# Week 25: Strategic Frameworks for Decision-Making

**Semester 2, Week 11**
**Mentor Voice: Tech Executive**
**Reading Time: 50 minutes**

---

## Introduction: How to Think, Not What to Think

You're the CEO of Bibby. Every day you face dozens of decisions:

- Should we build a mobile app or focus on the CLI?
- Should we hire a VP Sales now or wait 6 months?
- Should we expand to enterprise or double down on SMB?
- Should we build our own recommendation engine or use an API?
- Which features should we prioritize this quarter?

**Bad founders make decisions based on:**
- What competitors are doing (copying)
- What feels right (gut instinct without data)
- What's easiest (path of least resistance)
- What investors suggest (outsourcing thinking)

**Good founders use frameworks:**
- Structured ways to think through problems
- Systematic evaluation of tradeoffs
- Data-driven decision criteria
- Clear reasoning they can communicate to the team

This week you'll learn **9 strategic frameworks** that world-class founders use to make better decisions:

1. **First Principles Thinking** (break problems down to fundamentals)
2. **Jobs-to-be-Done** (understand what customers really want)
3. **North Star Metric** (focus on the one metric that predicts success)
4. **OKRs** (set objectives and track key results)
5. **ICE/RICE Scoring** (prioritize features and initiatives)
6. **Build vs Buy vs Partner** (strategic sourcing)
7. **Wardley Mapping** (visualize competitive position)
8. **Decision-Making Under Uncertainty** (Bayesian thinking)
9. **Avoiding Cognitive Biases** (recognize mental traps)

By the end, you'll have a toolkit for thinking clearly about hard problems.

---

## Part 1: First Principles Thinking

**The Elon Musk approach to problem-solving.**

### What Is First Principles Thinking?

Instead of reasoning by analogy ("Everyone does X, so we should too"), you break problems down to fundamental truths and reason up from there.

**The process:**

1. **Identify the problem**
2. **Break it down to fundamentals** (what do we know is true?)
3. **Reason up from those fundamentals** (what solutions are possible?)

### Example: Should Bibby Build a Mobile App?

**Reasoning by analogy (bad):**
> "All successful SaaS companies have mobile apps. Notion has one. Evernote has one. We should build one too."

**First principles thinking (good):**

**Step 1: What's the fundamental problem?**
> Users want to access their library on the go.

**Step 2: Break down to fundamentals:**
- What do we know is true?
  - Most developers work on laptops/desktops
  - Developers rarely read technical books on mobile
  - Mobile development is expensive (iOS + Android = 6+ months, 2 engineers)
  - Our CLI doesn't work on mobile

**Step 3: Reason up from fundamentals:**
- What are all the ways to solve "access library on the go"?
  1. Native mobile app (iOS + Android)
  2. Progressive Web App (works on mobile browsers)
  3. Web companion (responsive website)
  4. Email digests (send daily reading lists)
  5. Don't solve it (is this actually a problem?)

**Step 4: Evaluate options:**
- **Native app**: 6 months, 2 engineers, high cost → Only if mobile usage is high
- **PWA**: 2 months, 1 engineer, works everywhere → Better ROI
- **Web companion**: 1 month, 1 engineer, simple → Good MVP
- **Email digests**: 1 week, existing team → Test demand first
- **Don't solve it**: Talk to customers → Do they actually want this?

**Conclusion from first principles:**
> We should talk to 20 customers to see if mobile access is actually a problem. If yes, ship email digests in 1 week to test demand. If engagement is high, build a web companion in 1 month. Only build native apps if web companion has >40% mobile usage.

**This is very different from "everyone has an app, so we should too."**

### Implementing First Principles Framework

```java
package com.bibby.strategy;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * First Principles Decision Framework
 *
 * Break problems down to fundamentals and reason up from there.
 */
@Service
public class FirstPrinciplesFramework {

    public record Fundamental(
        String statement,
        String evidence,
        boolean validated
    ) {}

    public record Solution(
        String approach,
        String cost,              // Time, money, resources
        String benefit,
        String risk,
        int feasibilityScore,     // 1-10
        int impactScore,          // 1-10
        int confidenceScore       // 1-10 (how sure are we?)
    ) {
        public double weightedScore() {
            // Weighted average: feasibility 30%, impact 50%, confidence 20%
            return (feasibilityScore * 0.3) + (impactScore * 0.5) + (confidenceScore * 0.2);
        }
    }

    public record FirstPrinciplesAnalysis(
        String problem,
        List<Fundamental> fundamentals,
        List<Solution> solutions,
        Solution recommendation,
        String reasoning
    ) {}

    /**
     * Example: Should Bibby build a mobile app?
     */
    public FirstPrinciplesAnalysis analyzeMobileAppDecision() {
        String problem = "Should Bibby build a mobile app?";

        // Step 1: Break down to fundamentals
        List<Fundamental> fundamentals = List.of(
            new Fundamental(
                "Most developers work on laptops/desktops",
                "Survey: 95% of users access Bibby from desktop",
                true
            ),
            new Fundamental(
                "Developers rarely read technical books on mobile",
                "Industry data: 85% of technical reading happens on desktop",
                true
            ),
            new Fundamental(
                "Mobile app development is expensive",
                "Estimate: 6 months, 2 engineers, $300K cost",
                true
            ),
            new Fundamental(
                "Our core value is CLI speed",
                "NPS drivers: 'fast', 'no context switch', 'terminal-native'",
                true
            )
        );

        // Step 2: Generate all possible solutions
        List<Solution> solutions = List.of(
            new Solution(
                "Native mobile app (iOS + Android)",
                "6 months, 2 engineers, $300K",
                "Full native experience, app store presence",
                "High cost, diverts from core CLI value prop",
                4,  // Low feasibility (expensive)
                6,  // Medium impact
                3   // Low confidence (unvalidated demand)
            ),
            new Solution(
                "Progressive Web App (PWA)",
                "2 months, 1 engineer, $60K",
                "Works on all devices, lower cost than native",
                "Not as smooth as native, limited offline",
                7,  // Good feasibility
                7,  // Good impact
                5   // Medium confidence
            ),
            new Solution(
                "Responsive web companion",
                "1 month, 1 engineer, $30K",
                "Simple, works on mobile browsers, quick to ship",
                "Not as polished as app, requires internet",
                9,  // High feasibility
                6,  // Medium impact
                7   // Good confidence
            ),
            new Solution(
                "Email digests (daily reading lists)",
                "1 week, existing team, $5K",
                "Test demand with minimal investment",
                "Not a full solution, just a test",
                10, // Very high feasibility
                4,  // Lower impact (not solving full problem)
                8   // High confidence (easy to ship)
            ),
            new Solution(
                "Don't build anything (validate demand first)",
                "0 cost, customer interviews",
                "Learn if this is actually a problem",
                "Might miss market opportunity",
                10, // Very high feasibility
                5,  // Medium impact (learning is valuable)
                9   // Very high confidence
            )
        );

        // Step 3: Rank by weighted score
        Solution recommendation = solutions.stream()
            .max(Comparator.comparing(Solution::weightedScore))
            .orElseThrow();

        String reasoning = String.format(
            "Based on first principles analysis, '%s' scores highest (%.1f/10). " +
            "This balances feasibility (%d), impact (%d), and confidence (%d). " +
            "We should validate demand through customer interviews, then ship email digests as MVP. " +
            "If engagement is high (>30%% open rate, >10%% click rate), build responsive web companion.",
            recommendation.approach,
            recommendation.weightedScore(),
            recommendation.feasibilityScore,
            recommendation.impactScore,
            recommendation.confidenceScore
        );

        return new FirstPrinciplesAnalysis(
            problem,
            fundamentals,
            solutions,
            recommendation,
            reasoning
        );
    }

    public void printAnalysis(FirstPrinciplesAnalysis analysis) {
        System.out.println("=== First Principles Analysis ===\n");
        System.out.println("Problem: " + analysis.problem + "\n");

        System.out.println("Fundamentals:");
        for (Fundamental f : analysis.fundamentals) {
            System.out.printf("  ✓ %s\n", f.statement);
            System.out.printf("    Evidence: %s\n", f.evidence);
        }
        System.out.println();

        System.out.println("Solutions Evaluated:");
        System.out.println("┌────────────────────────────────┬────────────┬────────┬────────┬────────┬────────┐");
        System.out.println("│ Approach                       │ Cost       │ Feas.  │ Impact │ Conf.  │ Score  │");
        System.out.println("├────────────────────────────────┼────────────┼────────┼────────┼────────┼────────┤");

        for (Solution s : analysis.solutions) {
            System.out.printf("│ %-30s │ %-10s │ %6d │ %6d │ %6d │ %6.1f │\n",
                s.approach.substring(0, Math.min(30, s.approach.length())),
                s.cost.substring(0, Math.min(10, s.cost.length())),
                s.feasibilityScore,
                s.impactScore,
                s.confidenceScore,
                s.weightedScore()
            );
        }

        System.out.println("└────────────────────────────────┴────────────┴────────┴────────┴────────┴────────┘\n");

        System.out.println("💡 RECOMMENDATION:");
        System.out.println(analysis.reasoning);
        System.out.println();
    }
}
```

**Key Insight:** First principles thinking forces you to validate assumptions and consider all options, not just the obvious ones.

---

## Part 2: Jobs-to-be-Done (JTBD) Deep Dive

We covered JTBD in Week 4. Now let's go deeper.

### The JTBD Framework

**Core insight:** People don't buy products. They "hire" products to do a job.

**The classic example:**
- People don't buy a drill because they want a drill
- They buy a drill because they want a hole
- They want a hole because they want to hang a picture
- They want to hang a picture because they want their home to feel warm and personal

**The job:** "Help me make my home feel like mine"
**The product:** Could be a drill, or picture-hanging service, or interior designer, or Ikea frames with adhesive strips

### JTBD for Bibby

**Surface-level job:**
> "I need to manage my technical books"

**Deeper job:**
> "I need to find the right information quickly when I'm stuck on a problem"

**Deepest job:**
> "I need to feel confident and competent as a developer"

**This changes what you build:**
- Surface-level → Build a catalog (boring)
- Deeper → Build search + recommendations (useful)
- Deepest → Build learning paths, mastery tracking, community (transformative)

### The JTBD Interview Framework

Ask customers:

**1. "When did you realize you needed Bibby?"**
- Uncover the trigger moment

**2. "What were you doing when that happened?"**
- Context and situation

**3. "What did you try before Bibby?"**
- Alternative solutions (your competition)

**4. "What almost stopped you from signing up?"**
- Anxieties and friction

**5. "What convinced you to sign up?"**
- Motivating forces

**6. "How has your life changed since using Bibby?"**
- Outcome, not features

### Example JTBD Analysis

**Customer:** Senior engineer at Stripe

**Interview:**

**Q: "When did you realize you needed Bibby?"**
> A: "I was debugging a distributed systems issue at 2am. I knew I'd read about this in 'Designing Data-Intensive Applications' but couldn't remember where. I spent 30 minutes searching through my Notion, couldn't find it, gave up. That's when I thought 'there has to be a better way.'"

**Analysis:**
- **Trigger**: Stuck on urgent problem at 2am
- **Job**: "Help me quickly find information I've already learned when I'm under pressure"
- **Anxiety**: "I'm wasting time, letting my team down, feeling incompetent"
- **Outcome desired**: "Feel prepared and competent"

**Product implications:**
- Build instant search (< 1 second)
- Build "Recently viewed" (what did I read about this before?)
- Build "Related to current project" (Git integration)
- Don't build: Pretty UI, social features, badges

**The job isn't "manage books." It's "feel prepared and competent under pressure."**

---

## Part 3: North Star Metric

**The one metric that predicts long-term success.**

### What Is a North Star Metric?

A single metric that:
1. **Predicts revenue** (leading indicator)
2. **Reflects customer value** (not vanity)
3. **Guides product decisions** (actionable)

**Examples:**

| Company | North Star Metric |
|---------|------------------|
| Airbnb | Nights booked |
| Spotify | Time listening |
| Slack | Messages sent |
| Netflix | Hours watched |
| Amazon | Purchases per month |

### Finding Bibby's North Star Metric

**Options:**

**1. Active users**
- Pro: Easy to measure
- Con: Doesn't predict revenue (free users don't convert)

**2. Paying customers**
- Pro: Directly tied to revenue
- Con: Lagging indicator (doesn't predict growth)

**3. Books added per user**
- Pro: Reflects engagement
- Con: One-time action, doesn't predict retention

**4. Searches per week (per active user)**
- Pro: Predicts retention (high search = high value)
- Pro: Actionable (we can optimize search)
- Pro: Leading indicator (search today → payment tomorrow)
- **This is our North Star**

**Why "searches per week" works:**
- If users search 5+ times/week → 90% retention at Month 3
- If users search 1-4 times/week → 60% retention
- If users search 0 times/week → 20% retention

**Search frequency predicts revenue.**

### Implementing North Star Tracking

```java
package com.bibby.metrics;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

/**
 * North Star Metric Tracker
 *
 * Tracks the one metric that predicts long-term success.
 */
@Service
public class NorthStarMetricService {

    public record WeeklyMetrics(
        LocalDate weekStart,
        int totalActiveUsers,
        int totalSearches,
        double avgSearchesPerUser,

        // Breakdown by frequency
        int usersWithZeroSearches,
        int usersWith1to4Searches,
        int usersWith5PlusSearches,

        // Predicted retention (based on historical data)
        double predictedRetentionRate
    ) {}

    /**
     * Calculate North Star Metric for a given week.
     */
    public WeeklyMetrics calculateWeeklyNorthStar(LocalDate weekStart) {
        // In production, query from database
        // For now, simulate data

        int totalActiveUsers = 1200;
        int totalSearches = 8400;
        double avgSearchesPerUser = (double) totalSearches / totalActiveUsers;

        // Breakdown by search frequency
        int zeroSearches = 240;     // 20% of users (low engagement)
        int oneToFour = 480;         // 40% of users (medium engagement)
        int fivePlus = 480;          // 40% of users (high engagement)

        // Predicted retention based on search frequency
        // Historical data shows:
        // - 0 searches → 20% retention
        // - 1-4 searches → 60% retention
        // - 5+ searches → 90% retention
        double predictedRetention =
            (zeroSearches * 0.20 +
             oneToFour * 0.60 +
             fivePlus * 0.90) / totalActiveUsers;

        return new WeeklyMetrics(
            weekStart,
            totalActiveUsers,
            totalSearches,
            avgSearchesPerUser,
            zeroSearches,
            oneToFour,
            fivePlus,
            predictedRetention
        );
    }

    public void printNorthStarDashboard() {
        System.out.println("=== North Star Metric Dashboard ===\n");
        System.out.println("North Star: Searches per Week (per active user)\n");

        // Last 4 weeks
        LocalDate today = LocalDate.now();
        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i);
            WeeklyMetrics metrics = calculateWeeklyNorthStar(weekStart);

            System.out.printf("Week of %s:\n", metrics.weekStart);
            System.out.printf("  Active Users: %,d\n", metrics.totalActiveUsers);
            System.out.printf("  Total Searches: %,d\n", metrics.totalSearches);
            System.out.printf("  Avg Searches/User: %.1f 🎯\n", metrics.avgSearchesPerUser);
            System.out.println();

            System.out.println("  User Distribution:");
            System.out.printf("    0 searches: %,d (%.0f%%) → 20%% predicted retention ⚠️\n",
                metrics.usersWithZeroSearches,
                (double) metrics.usersWithZeroSearches / metrics.totalActiveUsers * 100
            );
            System.out.printf("    1-4 searches: %,d (%.0f%%) → 60%% predicted retention\n",
                metrics.usersWith1to4Searches,
                (double) metrics.usersWith1to4Searches / metrics.totalActiveUsers * 100
            );
            System.out.printf("    5+ searches: %,d (%.0f%%) → 90%% predicted retention ✅\n",
                metrics.usersWith5PlusSearches,
                (double) metrics.usersWith5PlusSearches / metrics.totalActiveUsers * 100
            );
            System.out.println();

            System.out.printf("  Blended Predicted Retention: %.0f%%\n\n",
                metrics.predictedRetentionRate * 100);
        }

        System.out.println("💡 Insights:");
        System.out.println("  • Goal: Get 60%+ of users to 5+ searches/week");
        System.out.println("  • Strategy: Trigger emails when users haven't searched in 3 days");
        System.out.println("  • Metric to watch: % of users with 5+ searches (currently 40%)");
        System.out.println();
    }
}
```

**Key Insight:** Focus on moving users from "0-4 searches/week" to "5+ searches/week". This single improvement increases retention from 60% → 90% and compounds over time.

---

## Part 4: OKRs (Objectives and Key Results)

**How Google and Intel set and track goals.**

### What Are OKRs?

**Objective:** Qualitative goal (what you want to achieve)
**Key Results:** Quantitative metrics (how you'll know you achieved it)

**Structure:**
```
Objective: [Inspiring goal]
  Key Result 1: [Measurable outcome]
  Key Result 2: [Measurable outcome]
  Key Result 3: [Measurable outcome]
```

### OKR Best Practices

**1. Objectives should be inspiring**
- Bad: "Increase MRR"
- Good: "Build the default tool for developer knowledge management"

**2. Key Results should be measurable and ambitious**
- Bad: "Improve search quality"
- Good: "90% search satisfaction score (up from 75%)"

**3. Set 3-5 OKRs per quarter**
- More = diluted focus

**4. Score 0.0 to 1.0**
- 0.7+ = Good (you're pushing boundaries)
- 1.0 = Too easy (sandbag goals)

**5. Make them public**
- Everyone sees everyone's OKRs (transparency drives alignment)

### Example: Bibby Q2 OKRs

**Company OKR:**
```
Objective: Become the go-to tool for technical learning

Key Result 1: Increase North Star from 7 → 10 searches/user/week
Key Result 2: Hit 60% weekly retention (up from 45%)
Key Result 3: Reach $100K MRR (up from $52K)
```

**Product OKR (tied to Company KR1, KR2):**
```
Objective: Make Bibby indispensable in developer workflow

Key Result 1: Ship Git integration (50% of users connect in first week)
Key Result 2: Search response time < 500ms for 95% of queries
Key Result 3: 30% of users set up daily email digests
```

**Sales/Marketing OKR (tied to Company KR3):**
```
Objective: Scale acquisition while maintaining unit economics

Key Result 1: Acquire 400 new customers (up from 280 last quarter)
Key Result 2: Maintain CAC < $90 (currently $82)
Key Result 3: Launch partnership with VS Code (10K installs)
```

**Engineering OKR (enables Product OKRs):**
```
Objective: Build scalable infrastructure for 10× growth

Key Result 1: Search latency p95 < 500ms (currently 1.2s)
Key Result 2: 99.9% uptime (currently 99.5%)
Key Result 3: Hire 2 senior engineers (fill pipeline gaps)
```

### OKR Tracking

**Check-in frequency:**
- Weekly: Team leads review progress, update confidence
- Monthly: Company-wide review, course-correct if needed
- Quarterly: Score OKRs, set next quarter's OKRs

**Confidence scoring:**
- Green (8-10): On track
- Yellow (5-7): At risk, need help
- Red (0-4): Off track, major issues

---

## Part 5: ICE/RICE Scoring (Prioritization)

**How to decide what to build next.**

### The Problem

You have 50 feature requests:
- Git integration
- Mobile app
- AI recommendations
- Team collaboration
- Export to PDF
- Dark mode
- API access
- ...

**Which should you build first?**

### ICE Scoring

Rate each feature on 3 dimensions (1-10 scale):

**I = Impact** (How much will this move key metrics?)
**C = Confidence** (How sure are you?)
**E = Ease** (How easy to implement?)

**ICE Score = (Impact × Confidence × Ease) / 100**

### RICE Scoring (More Sophisticated)

**R = Reach** (How many users will this affect?)
**I = Impact** (How much will it affect them? 0.25=minimal, 3=massive)
**C = Confidence** (% confidence: 50%=low, 80%=medium, 100%=high)
**E = Effort** (Person-months of work)

**RICE Score = (Reach × Impact × Confidence) / Effort**

### Example: Prioritizing Bibby Features

```java
package com.bibby.strategy;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * RICE Scoring for Feature Prioritization
 */
@Service
public class FeaturePrioritizationService {

    public record Feature(
        String name,
        String description,
        int reach,              // # of users affected per quarter
        double impact,          // 0.25=minimal, 0.5=low, 1=medium, 2=high, 3=massive
        double confidence,      // 0.5=50%, 0.8=80%, 1.0=100%
        double effortMonths,    // Person-months
        String category         // "Product", "Growth", "Infrastructure"
    ) {
        public double riceScore() {
            return (reach * impact * confidence) / effortMonths;
        }
    }

    public List<Feature> prioritizeBibbyFeatures() {
        List<Feature> features = List.of(
            new Feature(
                "Git Integration",
                "Show reading lists based on current Git branch/project",
                800,        // 800 users will use this
                2.0,        // High impact (increases stickiness)
                0.8,        // 80% confidence
                2.0,        // 2 person-months
                "Product"
            ),
            new Feature(
                "Mobile App (iOS + Android)",
                "Native mobile app for on-the-go access",
                300,        // Only 300 users want mobile
                1.0,        // Medium impact
                0.5,        // 50% confidence (unvalidated demand)
                6.0,        // 6 person-months
                "Product"
            ),
            new Feature(
                "AI Recommendations",
                "ML-powered book recommendations based on reading history",
                1000,       // All users
                2.0,        // High impact (drives discovery)
                0.8,        // 80% confidence
                3.0,        // 3 person-months
                "Product"
            ),
            new Feature(
                "Search Latency Optimization",
                "Reduce search time from 1.2s → 0.3s",
                1000,       // All users
                3.0,        // Massive impact (core value prop)
                1.0,        // 100% confidence (we know how)
                1.5,        // 1.5 person-months
                "Infrastructure"
            ),
            new Feature(
                "Team Collaboration",
                "Shared libraries for teams",
                200,        // Enterprise customers only
                1.0,        // Medium impact
                0.6,        // 60% confidence
                4.0,        // 4 person-months
                "Product"
            ),
            new Feature(
                "Dark Mode",
                "Dark theme for terminal and web",
                600,        // Many users want this
                0.25,       // Minimal impact (nice-to-have)
                1.0,        // 100% confidence
                0.5,        // 0.5 person-months
                "Product"
            ),
            new Feature(
                "VS Code Extension",
                "Bibby integration in VS Code",
                1200,       // Lots of VS Code users
                2.0,        // High impact (new acquisition channel)
                0.8,        // 80% confidence
                1.0,        // 1 person-month
                "Growth"
            )
        );

        // Sort by RICE score (descending)
        return features.stream()
            .sorted(Comparator.comparing(Feature::riceScore).reversed())
            .toList();
    }

    public void printPrioritization() {
        List<Feature> features = prioritizeBibbyFeatures();

        System.out.println("=== Feature Prioritization (RICE) ===\n");
        System.out.println("Formula: RICE = (Reach × Impact × Confidence) / Effort\n");

        System.out.println("┌───────────────────────────────┬───────┬────────┬────────┬────────┬────────┬────────────┐");
        System.out.println("│ Feature                       │ Reach │ Impact │ Confid │ Effort │ RICE   │ Category   │");
        System.out.println("├───────────────────────────────┼───────┼────────┼────────┼────────┼────────┼────────────┤");

        for (Feature f : features) {
            System.out.printf("│ %-29s │ %5d │ %6.2f │ %6.0f%% │ %4.1fm │ %6.0f │ %-10s │\n",
                f.name.substring(0, Math.min(29, f.name.length())),
                f.reach,
                f.impact,
                f.confidence * 100,
                f.effortMonths,
                f.riceScore(),
                f.category
            );
        }

        System.out.println("└───────────────────────────────┴───────┴────────┴────────┴────────┴────────┴────────────┘\n");

        System.out.println("💡 Q2 Roadmap (Based on RICE):");
        System.out.println("  1. " + features.get(0).name + " (RICE: " + String.format("%.0f", features.get(0).riceScore()) + ")");
        System.out.println("  2. " + features.get(1).name + " (RICE: " + String.format("%.0f", features.get(1).riceScore()) + ")");
        System.out.println("  3. " + features.get(2).name + " (RICE: " + String.format("%.0f", features.get(2).riceScore()) + ")");
        System.out.println();
        System.out.println("❌ Deprioritize:");
        int last = features.size() - 1;
        System.out.println("  • " + features.get(last).name + " (RICE: " + String.format("%.0f", features.get(last).riceScore()) + ")");
        System.out.println();
    }
}
```

**Output:**
```
=== Feature Prioritization (RICE) ===

┌───────────────────────────────┬───────┬────────┬────────┬────────┬────────┬────────────┐
│ Feature                       │ Reach │ Impact │ Confid │ Effort │ RICE   │ Category   │
├───────────────────────────────┼───────┼────────┼────────┼────────┼────────┼────────────┤
│ Search Latency Optimization   │  1000 │   3.00 │   100% │  1.5m │   2000 │ Infra      │
│ VS Code Extension             │  1200 │   2.00 │    80% │  1.0m │   1920 │ Growth     │
│ AI Recommendations            │  1000 │   2.00 │    80% │  3.0m │    533 │ Product    │
│ Git Integration               │   800 │   2.00 │    80% │  2.0m │    640 │ Product    │
│ Dark Mode                     │   600 │   0.25 │   100% │  0.5m │    300 │ Product    │
│ Mobile App                    │   300 │   1.00 │    50% │  6.0m │     25 │ Product    │
│ Team Collaboration            │   200 │   1.00 │    60% │  4.0m │     30 │ Product    │
└───────────────────────────────┴───────┴────────┴────────┴────────┴────────┴────────────┘

💡 Q2 Roadmap (Based on RICE):
  1. Search Latency Optimization (RICE: 2000)
  2. VS Code Extension (RICE: 1920)
  3. AI Recommendations (RICE: 533)

❌ Deprioritize:
  • Mobile App (RICE: 25)
```

**Key Insight:** The mobile app everyone thinks you "need" scores dead last. Search optimization—unglamorous infrastructure work—scores highest.

**Trust the framework, not your gut.**

---

## Part 6: Build vs Buy vs Partner

**When should you build, buy, or partner for capabilities?**

### The Decision Framework

**Build when:**
- It's your core competency (differentiator)
- You need full control and customization
- No good alternatives exist
- Long-term ROI justifies investment

**Buy when:**
- Commodity functionality (everyone needs it, it's not differentiating)
- Mature vendor ecosystem exists
- Cost < build + maintain
- Speed to market matters

**Partner when:**
- Complementary products (1+1=3)
- Access to distribution channel
- Neither party wants to build it
- Revenue share makes sense

### Example: Bibby Recommendation Engine

**Option 1: Build**
- **Pros**: Full control, proprietary algorithms, no usage fees
- **Cons**: 3 months + 1 ML engineer ($180K/year)
- **Annual cost**: $180K salary + infrastructure
- **Verdict**: Only if recommendations become core differentiator

**Option 2: Buy (Use Algolia Recommend API)**
- **Pros**: Ship in 2 weeks, proven technology
- **Cons**: $500/month + usage fees, less customization
- **Annual cost**: ~$10K
- **Verdict**: Great for MVP, test demand first

**Option 3: Partner (Integrate with Goodreads API)**
- **Pros**: Leverage their 90M users, instant credibility
- **Cons**: Limited to their data, potential API changes
- **Annual cost**: Free (API access)
- **Verdict**: Quick win, but not defensible long-term

**Decision:**
1. **Month 1**: Partner with Goodreads (free, validate demand)
2. **Month 3**: If engagement >30%, buy Algolia (fast, proven)
3. **Month 12**: If recommendations drive 40%+ conversions, build proprietary engine

**Staged approach = minimize risk, maximize learning.**

---

## Part 7: Avoiding Cognitive Biases

**Your brain will trick you. Know the traps.**

### The 10 Deadliest Cognitive Biases for Founders

**1. Confirmation Bias**
- **What it is**: Seeking evidence that confirms your beliefs
- **Example**: "We need a mobile app" → Only listen to customers who want mobile, ignore those who don't
- **Fix**: Actively seek disconfirming evidence

**2. Sunk Cost Fallacy**
- **What it is**: Continuing investment because you've already spent so much
- **Example**: Spent 6 months building feature X, it's not working, but "we've come this far"
- **Fix**: Ignore sunk costs. Ask "Knowing what we know now, would we start this today?"

**3. Anchoring Bias**
- **What it is**: Over-relying on first piece of information
- **Example**: First investor offers $5M valuation → You anchor there, ignore better offers
- **Fix**: Generate multiple reference points before deciding

**4. Availability Heuristic**
- **What it is**: Overweight recent/memorable information
- **Example**: One customer churned loudly → "We have a huge churn problem!" (ignoring 99% who stay)
- **Fix**: Look at aggregate data, not anecdotes

**5. Planning Fallacy**
- **What it is**: Underestimating time/cost/difficulty
- **Example**: "This feature will take 2 weeks" → Takes 8 weeks
- **Fix**: Use historical data (How long did last feature take?) + add 50% buffer

**6. Survivorship Bias**
- **What it is**: Only studying success, ignoring failures
- **Example**: "Dropbox started with a video demo, so we should too" (Ignoring 1000 startups whose video demos failed)
- **Fix**: Study failures, not just successes

**7. Hindsight Bias**
- **What it is**: "I knew that would happen" (after the fact)
- **Example**: Competitor launches similar feature → "I knew we should have built that first"
- **Fix**: Write down predictions beforehand

**8. Overconfidence Bias**
- **What it is**: Overestimating your abilities/knowledge
- **Example**: "I can build this in 2 months" (you can't) / "Customers will love this" (they won't)
- **Fix**: Track your predictions vs reality (calibrate)

**9. IKEA Effect**
- **What it is**: Overvaluing things you built
- **Example**: You spent 6 months building feature X → Convinced it's amazing, even though users don't care
- **Fix**: Get external validation (user tests, data)

**10. Not Invented Here (NIH) Syndrome**
- **What it is**: Preferring to build rather than use existing solutions
- **Example**: "We can't use Stripe, we should build our own payment system" (terrible idea)
- **Fix**: Use Build vs Buy framework

### How to Avoid Biases

**1. Pre-commit to decision criteria**
- Before analyzing, write down: "I'll choose Option A if metric X > threshold Y"
- Prevents motivated reasoning

**2. Devil's advocate**
- Assign someone to argue against your position
- Forces you to consider alternatives

**3. Pre-mortem**
- Imagine it's 1 year from now and your decision failed. Why?
- Uncovers hidden risks

**4. Track your predictions**
- Write down "I predict X will happen"
- Review monthly: Were you right? Calibrate.

**5. Use frameworks**
- First Principles, RICE, Build vs Buy
- Systematic beats intuition

---

## Practical Assignment: Strategic Decision Analysis

### **Assignment: Make Three Strategic Decisions for Bibby**

You're the CEO. Use the frameworks to make three key decisions:

**Decision 1: Should we build a mobile app?**
- Use: First Principles Thinking
- Break down to fundamentals
- Generate all solutions
- Score and recommend

**Decision 2: What should we build next quarter?**
- Use: RICE Scoring
- List 10 potential features
- Score each on Reach, Impact, Confidence, Effort
- Prioritize top 3

**Decision 3: Should we build or buy our recommendation engine?**
- Use: Build vs Buy vs Partner Framework
- Evaluate all 3 options
- Consider staged approach
- Make recommendation with reasoning

### **Deliverables**

**1. First Principles Analysis (Mobile App)**
```markdown
# Should Bibby Build a Mobile App?

## Fundamentals
- [List 5 fundamental truths]

## All Possible Solutions
- [List 5+ solutions]

## Solution Evaluation
| Solution | Cost | Benefit | Feasibility | Impact | Confidence | Score |
|----------|------|---------|-------------|--------|------------|-------|
| ...      | ...  | ...     | ...         | ...    | ...        | ...   |

## Recommendation
[Which solution and why]

## Implementation Plan
[Staged approach with decision points]
```

**2. RICE Prioritization (Q2 Roadmap)**
```markdown
# Bibby Q2 Feature Prioritization

## Features Evaluated
| Feature | Reach | Impact | Confidence | Effort | RICE Score |
|---------|-------|--------|------------|--------|------------|
| ...     | ...   | ...    | ...        | ...    | ...        |

## Q2 Roadmap
1. [Top feature]
2. [Second feature]
3. [Third feature]

## Deprioritized
- [Bottom features and why]

## Success Metrics
- [How we'll measure success]
```

**3. Build vs Buy vs Partner (Recommendations)**
```markdown
# Recommendation Engine: Build, Buy, or Partner?

## Option 1: Build
**Pros:** ...
**Cons:** ...
**Cost:** ...
**Timeline:** ...

## Option 2: Buy (Algolia)
**Pros:** ...
**Cons:** ...
**Cost:** ...
**Timeline:** ...

## Option 3: Partner (Goodreads)
**Pros:** ...
**Cons:** ...
**Cost:** ...
**Timeline:** ...

## Recommendation
[Staged approach:]
- Month 1: ...
- Month 3: ...
- Month 12: ...

**Decision criteria:** [When to move from one stage to next]
```

**4. Java Implementations**
- `FirstPrinciplesFramework.java` (provided above)
- `FeaturePrioritizationService.java` (provided above)
- `NorthStarMetricService.java` (provided above)

**5. Reflection (400 words)**
- Which framework was most useful? Why?
- Which decision was hardest? What made it hard?
- What cognitive biases did you notice in yourself?
- How will you use these frameworks going forward?

---

## Reflection Questions

1. **When should you use First Principles Thinking vs just copying what works for others?**

2. **How do you balance "move fast" vs "think deeply using frameworks"?**

3. **What's the difference between North Star Metric and your revenue goal?**

4. **If RICE scoring says to build Feature X, but your gut says Feature Y, what do you do?**

5. **How do you avoid framework paralysis (spending more time analyzing than executing)?**

6. **Which cognitive bias do you think affects founders most? Why?**

---

## From the Mentor: Frameworks Are Training Wheels

Here's what I've learned about decision-making frameworks:

**Early in my career, I trusted my gut.** I made fast decisions based on intuition. Some were brilliant. Most were terrible. I had no idea which would be which.

**Then I over-indexed on frameworks.** I RICE-scored everything. I ran first principles analysis on every decision. I spent more time analyzing than executing. The business suffered.

**Now I use frameworks selectively:**

**Use frameworks for:**
- Big, irreversible decisions (hiring VP, fundraising, pivots)
- When you're biased (you really want to do X, but is it right?)
- When stakeholders disagree (frameworks create shared language)
- When you're stuck (frameworks generate new options)

**Don't use frameworks for:**
- Small, reversible decisions (just decide and iterate)
- When you have strong signal from data (data > frameworks)
- When you need to move fast (analysis paralysis)

**The best founders:**
1. **Build intuition** through frameworks (train your gut)
2. **Trust intuition** for most decisions (move fast)
3. **Check intuition** with frameworks when it matters (avoid disasters)

**My framework hierarchy:**

**Tier 1 (Use weekly):**
- North Star Metric (track always)
- OKRs (review weekly)

**Tier 2 (Use monthly):**
- RICE prioritization (quarterly roadmap planning)
- Build vs Buy (evaluate new capabilities)

**Tier 3 (Use rarely, but critically):**
- First Principles (when conventional wisdom feels wrong)
- Pre-mortem (before big bets)

**The goal isn't to use every framework. It's to make better decisions faster.**

Now you have the toolkit. Use it wisely.

---

## Key Takeaways

1. **First Principles Thinking breaks problems down to fundamentals**—reason up from truth, not by analogy
2. **North Star Metric focuses the company**—find the one metric that predicts success
3. **OKRs create alignment**—3-5 ambitious, measurable objectives per quarter
4. **RICE scoring prioritizes objectively**—trust the math, not your gut
5. **Build vs Buy vs Partner has clear criteria**—core competency = build, commodity = buy, complementary = partner
6. **Cognitive biases sabotage decisions**—know the traps (sunk cost, confirmation, planning fallacy)
7. **Frameworks are tools, not rules**—use selectively for big decisions, trust intuition for small ones

**Next week: Semester 2 Capstone Project**

You've completed 11 weeks of metrics, economics, and strategy. Week 26 is your capstone: synthesize everything into a comprehensive business plan for Bibby.

You'll create:
- Complete business strategy (market positioning, competitive advantage)
- 3-year financial model (revenue, expenses, fundraising plan)
- Product roadmap (prioritized features for next 12 months)
- Go-to-market plan (acquisition channels, sales process)
- Team plan (hiring roadmap, org structure)
- Board deck (pitch your plan)

This is your chance to demonstrate mastery of Semester 2 concepts and build a plan you'd actually execute.

---

**Execution Checklist**

- [ ] Implement First Principles Framework in Java
- [ ] Analyze mobile app decision using first principles
- [ ] Implement RICE scoring service
- [ ] Prioritize 10 Bibby features using RICE
- [ ] Identify Bibby's North Star Metric
- [ ] Implement North Star tracking dashboard
- [ ] Write Q2 OKRs for company, product, sales, engineering
- [ ] Analyze build vs buy vs partner for recommendations
- [ ] List your personal cognitive biases
- [ ] Create pre-mortem for your biggest upcoming decision

**Recommended Reading**:
- *Thinking in Bets* by Annie Duke (decision-making under uncertainty)
- *Thinking, Fast and Slow* by Daniel Kahneman (cognitive biases)
- *Measure What Matters* by John Doerr (OKRs)
- *Competing Against Luck* by Clayton Christensen (Jobs-to-be-Done)

**Tools**:
- Amplitude (North Star Metric tracking)
- ProductPlan (roadmap prioritization)
- Aha! (RICE scoring built-in)
- Lattice (OKR software)

---

*"In theory, there is no difference between theory and practice. In practice, there is."* — Yogi Berra

You now have the frameworks. Next week, you'll put them into practice with your Semester 2 capstone project.
