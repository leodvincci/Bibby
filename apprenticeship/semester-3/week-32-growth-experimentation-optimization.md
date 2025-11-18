# Week 32: Growth Experimentation & Optimization

**Semester 3: Marketing, Go-to-Market & Growth**
**Week 32 of 52 • Estimated Time: 15-20 hours**

---

## Overview

You've learned multiple growth channels: demand generation, sales, product-led growth, and community. But here's the challenge: **You don't know which tactics will actually work for Bibby until you test them.**

Growth isn't about executing a predetermined playbook—it's about systematically experimenting to discover what works, then doubling down on winners and killing losers. The best growth teams don't have magic instincts; they have rigorous experimentation processes.

This week, you'll learn how to run growth experiments like a scientist: formulate hypotheses, design tests, analyze results with statistical rigor, and build a culture of continuous experimentation.

This week covers:
- The growth experimentation framework
- Generating high-quality growth hypotheses
- Prioritizing experiments with ICE/RICE scoring
- Designing statistically valid A/B tests
- Analyzing results and avoiding false positives
- Scaling winners and killing losers
- Building a growth experimentation culture

By the end of this week, you'll know how to run your first 10 growth experiments for Bibby and build a systematic approach to discovering your highest-leverage growth channels.

---

## Why Growth Experimentation Matters

**The Problem with "Best Practices"**

Most companies copy what worked for others:
- "Dropbox did referrals, so we should too"
- "Slack grew with word-of-mouth, so let's focus on community"
- "Everyone says to run ads, so let's run ads"

Problem: **What worked for them might not work for you.**

Your market, product, audience, and timing are different. The only way to know what works is to **test everything**.

**The Scientific Method Applied to Growth**

```
1. Observe (analyze data, identify bottlenecks)
2. Hypothesize (form testable predictions)
3. Experiment (run controlled tests)
4. Measure (collect data, analyze results)
5. Learn (draw conclusions, iterate)
6. Scale (double down on winners)
```

**Growth Experimentation in Practice**

**Without experimentation**:
- Spend 6 months building a referral program
- Launch it
- Nobody uses it
- Wasted 6 months

**With experimentation**:
- Week 1: Test concept with mockup (fake door test)
- Week 2: Build MVP version (single referral link)
- Week 3: Test with 100 users
- Result: 2% use it → Kill or iterate
- Total investment: 3 weeks

**The Compound Effect**

Running 2 experiments per week × 52 weeks = **104 experiments per year**

If you:
- Win rate: 20% (20 experiments work)
- Each winner improves a metric by 5%
- Compound: 1.05^20 = **2.65× growth**

This is how high-growth companies achieve 3-5× growth year-over-year while competitors struggle to grow 20%.

---

## Part 1: The Growth Experimentation Framework

**The Growth Equation**

```
Growth = (Traffic × Conversion × Retention × Monetization) - Churn
```

Every experiment targets one component:
- **Traffic**: More visitors (SEO, ads, content)
- **Conversion**: Higher signup rate (onboarding, messaging)
- **Retention**: Reduce churn (activation, engagement)
- **Monetization**: Higher ARPU (pricing, upsells)

**The AARRR Pirate Metrics Framework**

Map experiments to funnel stages:

```
Acquisition: How do users find you?
    ↓
Activation: Do they have a great first experience?
    ↓
Retention: Do they come back?
    ↓
Revenue: Do they pay?
    ↓
Referral: Do they tell others?
```

**Example Experiments by Stage**

**Acquisition**:
- Test: Add "Show HN" post vs Product Hunt launch vs Dev.to article
- Hypothesis: Dev.to article will drive 200+ signups
- Metric: Signups from source

**Activation**:
- Test: Guided onboarding vs self-serve
- Hypothesis: Guided onboarding will increase activation rate from 30% → 45%
- Metric: % who reach "aha moment" (5+ searches)

**Retention**:
- Test: Email reminder vs push notification vs no reminder
- Hypothesis: Email reminder will increase 7-day retention from 40% → 50%
- Metric: % active Day 7

**Revenue**:
- Test: $10/mo vs $8/mo vs $12/mo pricing
- Hypothesis: $8/mo will increase conversions by 30% and net revenue by 15%
- Metric: Revenue per 100 visitors

**Referral**:
- Test: "Invite 3 friends, get 1 month free" vs "No incentive"
- Hypothesis: Incentive will increase referral rate from 5% → 15%
- Metric: % who send invites

**The Experiment Template**

Every experiment should have:

1. **Name**: Clear, descriptive title
2. **Hypothesis**: "If we [change], then [metric] will [improve] because [reason]"
3. **Success Metric**: Primary KPI to measure
4. **Target**: Specific, measurable goal
5. **Variants**: Control vs treatment(s)
6. **Sample Size**: How many users needed
7. **Duration**: How long to run
8. **Results**: What actually happened
9. **Decision**: Scale, iterate, or kill

**Example**:

```
Name: Homepage Hero Copy Test
Hypothesis: If we change the hero copy from "Book management for developers"
           to "Never forget what you read", then signup rate will increase
           from 3% to 5% because it's more benefit-focused
Success Metric: Signup rate (signups / homepage visitors)
Target: >4.5% signup rate (50% improvement)
Variants:
  - Control: "Book management for developers"
  - Treatment: "Never forget what you read"
Sample Size: 2,000 visitors per variant
Duration: 2 weeks
Results: Control 3.1%, Treatment 4.8% (+55%)
Decision: SCALE - Implement treatment as new default
```

---

## Part 2: Generating Growth Hypotheses

**Where Do Good Hypotheses Come From?**

### Source 1: Data Analysis

Look at your funnel. Where are the biggest drop-offs?

**Example for Bibby**:
```
Homepage: 10,000 visitors
  ↓ 3% signup rate
Signup: 300 users
  ↓ 50% complete onboarding
Onboarded: 150 users
  ↓ 30% activate (5+ searches)
Activated: 45 users
  ↓ 20% convert to paid
Paid: 9 customers
```

**Biggest bottleneck**: Signup → Onboarding (50% drop)

**Hypothesis**: "If we simplify onboarding from 5 steps to 2 steps, completion rate will increase from 50% → 70%"

### Source 2: User Research

Talk to users. What's confusing? What's delightful?

**Interview Finding**: "I wanted to try Bibby but didn't know how to import my 200 Goodreads books"

**Hypothesis**: "If we add a 'One-click Goodreads import' button on signup, onboarding completion will increase from 50% → 65%"

### Source 3: Competitor Analysis

What are competitors doing? Can you do it better?

**Observation**: Notion has excellent onboarding templates

**Hypothesis**: "If we provide 'Reading List' templates (Startup Books, Leadership Books, etc.), activation rate will increase from 30% → 40%"

### Source 4: Analogies from Other Industries

Growth tactics from unrelated products might work for you.

**Example**: Spotify's "Year in Review" went viral

**Hypothesis**: "If we create a 'Year in Books' shareable card (books read, hours spent, top genres), social shares will increase by 300%"

### Source 5: First Principles Thinking

Break down the problem to fundamentals.

**Question**: Why is signup rate only 3%?

**First Principles Analysis**:
- People don't understand what Bibby is (education problem)
- They understand but don't care (value prop problem)
- They care but don't trust (credibility problem)
- They trust but don't want to commit (friction problem)

**Hypotheses**:
- Add explainer video → +1% signup
- Add social proof (testimonials) → +0.5% signup
- Add "No credit card required" → +1% signup

**The Hypothesis Quality Checklist**

Good hypotheses are:
- ✅ **Specific**: Not "improve activation" but "increase activation from 30% → 40%"
- ✅ **Measurable**: Clear success metric
- ✅ **Testable**: Can run in 1-4 weeks
- ✅ **Impactful**: Targets a high-leverage metric
- ✅ **Based on insight**: Has a "because" (reason)

---

## Part 3: Prioritizing Experiments (ICE/RICE Framework)

You have 50 experiment ideas. Which do you run first?

### The ICE Framework

Score experiments on three dimensions:

**I = Impact** (1-10): How much will this move the needle?
**C = Confidence** (1-10): How sure are you it will work?
**E = Ease** (1-10): How easy is it to implement?

**ICE Score = (I + C + E) / 3**

**Example**:

```
Experiment 1: Redesign entire onboarding flow
Impact: 9 (huge potential)
Confidence: 5 (uncertain)
Ease: 2 (takes 4 weeks)
ICE Score: (9 + 5 + 2) / 3 = 5.3

Experiment 2: Add "No credit card required" badge
Impact: 5 (modest impact)
Confidence: 8 (proven tactic)
Ease: 10 (takes 10 minutes)
ICE Score: (5 + 8 + 10) / 3 = 7.7

Winner: Experiment 2 (higher ICE score)
```

### The RICE Framework (More Rigorous)

**R = Reach** (number): How many users will see this?
**I = Impact** (0.25 - 3): Minimal, Low, Medium, High, Massive
**C = Confidence** (0-100%): How sure are you?
**E = Effort** (person-weeks): How long will it take?

**RICE Score = (Reach × Impact × Confidence) / Effort**

**Example**:

```
Experiment: Add referral program
Reach: 300 active users
Impact: 1.5 (medium)
Confidence: 70%
Effort: 4 person-weeks
RICE Score: (300 × 1.5 × 0.7) / 4 = 78.75

Experiment: Optimize pricing page
Reach: 10,000 visitors/month
Impact: 2 (high - directly affects revenue)
Confidence: 80%
Effort: 1 person-week
RICE Score: (10,000 × 2 × 0.8) / 1 = 16,000

Winner: Optimize pricing page (higher RICE score)
```

**Implementation: Experiment Prioritization**

```java
package com.bibby.growth;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ExperimentPrioritizationService {

    public record Experiment(
        String id,
        String name,
        String hypothesis,
        String successMetric,
        double targetImprovement,
        int impact,           // 1-10
        int confidence,       // 1-10
        int ease,             // 1-10
        int reach,            // Number of users affected
        double impactRICE,    // 0.25, 0.5, 1, 2, 3
        double confidenceRICE, // 0-1
        double effortWeeks,   // Person-weeks
        String stage          // AARRR stage
    ) {
        public double iceScore() {
            return (impact + confidence + ease) / 3.0;
        }

        public double riceScore() {
            return (reach * impactRICE * confidenceRICE) / effortWeeks;
        }
    }

    public List<Experiment> generateSampleExperiments() {
        return List.of(
            new Experiment(
                "exp-001",
                "Add 'No Credit Card Required' badge to signup",
                "If we add trust signal, signup rate will increase from 3% to 4%",
                "Signup rate",
                33.3,
                5, 8, 10,  // ICE: Impact 5, Confidence 8, Ease 10
                10000, 1.0, 0.8, 0.1,  // RICE: 10K reach, medium impact, 80% confidence, 0.1 weeks
                "Acquisition"
            ),
            new Experiment(
                "exp-002",
                "Simplify onboarding from 5 steps to 2",
                "If we reduce friction, onboarding completion will increase from 50% to 70%",
                "Onboarding completion rate",
                40.0,
                9, 7, 3,   // High impact, good confidence, hard to build
                300, 2.0, 0.7, 2.0,  // 300 users, high impact, 70% confidence, 2 weeks
                "Activation"
            ),
            new Experiment(
                "exp-003",
                "Launch referral program: Invite 3, get 1 month free",
                "If we incentivize referrals, referral rate will increase from 5% to 15%",
                "Referral rate",
                200.0,
                7, 6, 4,   // Medium-high impact, moderate confidence, moderate effort
                300, 1.5, 0.6, 4.0,  // 300 active users, medium-high impact, 60% confidence, 4 weeks
                "Referral"
            ),
            new Experiment(
                "exp-004",
                "Test $8/mo vs $10/mo vs $12/mo pricing",
                "If we lower price to $8, conversion will increase enough to offset revenue",
                "Revenue per 100 visitors",
                15.0,
                8, 5, 9,   // High impact, moderate confidence, easy
                500, 2.0, 0.5, 0.5,  // 500 trial users, high impact, 50% confidence, 0.5 weeks
                "Revenue"
            ),
            new Experiment(
                "exp-005",
                "Add explainer video to homepage",
                "If we show product in action, signup rate will increase from 3% to 4.5%",
                "Signup rate",
                50.0,
                6, 6, 5,   // Medium impact, moderate confidence, moderate effort
                10000, 1.0, 0.6, 1.0,  // 10K visitors, medium impact, 60% confidence, 1 week
                "Acquisition"
            ),
            new Experiment(
                "exp-006",
                "Email retention campaign: Day 3, 7, 14 emails",
                "If we remind inactive users, 7-day retention will increase from 40% to 50%",
                "7-day retention rate",
                25.0,
                7, 8, 8,   // Medium-high impact, high confidence, relatively easy
                300, 1.5, 0.8, 1.0,  // 300 signups/month, medium-high impact, 80% confidence, 1 week
                "Retention"
            )
        );
    }

    public void printPrioritizationReport() {
        System.out.println("=== Experiment Prioritization (ICE & RICE) ===\n");

        List<Experiment> experiments = generateSampleExperiments();

        // Sort by RICE score (most rigorous)
        List<Experiment> sortedByRICE = new ArrayList<>(experiments);
        sortedByRICE.sort((a, b) -> Double.compare(b.riceScore(), a.riceScore()));

        System.out.println("Ranked by RICE Score:\n");
        System.out.println("Rank | ID      | Experiment Name                               | RICE Score | ICE Score | Stage");
        System.out.println("-----|---------|-----------------------------------------------|------------|-----------|------------");

        int rank = 1;
        for (Experiment exp : sortedByRICE) {
            System.out.printf("%4d | %-7s | %-45s | %10.0f | %9.1f | %s%n",
                rank++,
                exp.id(),
                truncate(exp.name(), 45),
                exp.riceScore(),
                exp.iceScore(),
                exp.stage()
            );
        }

        System.out.println("\n📊 Top 3 Experiments to Run This Sprint:\n");

        for (int i = 0; i < Math.min(3, sortedByRICE.size()); i++) {
            Experiment exp = sortedByRICE.get(i);
            System.out.println((i + 1) + ". " + exp.name());
            System.out.println("   Hypothesis: " + exp.hypothesis());
            System.out.println("   Metric: " + exp.successMetric());
            System.out.println("   Target: +" + String.format("%.1f%%", exp.targetImprovement()));
            System.out.println("   RICE: " + String.format("%.0f", exp.riceScore()) +
                              " | ICE: " + String.format("%.1f", exp.iceScore()));
            System.out.println("   Effort: " + exp.effortWeeks() + " weeks");
            System.out.println();
        }

        System.out.println("💡 Prioritization Insights:");
        System.out.println("• Quick wins (high ICE): Focus on ease + confidence");
        System.out.println("• Big bets (high RICE): Focus on reach + impact");
        System.out.println("• Balance both: Run 2 quick wins + 1 big bet per sprint");
    }

    private String truncate(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }
}
```

---

## Part 4: Designing Statistically Valid Tests

**The Problem with "Ship It and See"**

Bad approach:
- Launch new feature
- "Looks like signups are up!"
- Ship it to everyone

Problem: Was it the feature, or random variance, or external factors (weekend, press mention)?

**A/B Testing Basics**

1. **Split traffic** randomly: 50% see Control (A), 50% see Treatment (B)
2. **Measure metric**: Track success metric for both groups
3. **Calculate significance**: Is the difference real or random?

**Statistical Significance**

**p-value**: Probability the result is due to chance
- p < 0.05 (5%): Statistically significant (confident it's real)
- p > 0.05: Not significant (could be random)

**Example**:

```
Control: 100 visitors, 3 signups (3%)
Treatment: 100 visitors, 5 signups (5%)

Is +67% improvement real or luck?

Run significance test:
p-value = 0.28 (28% chance it's random)

Conclusion: NOT significant. Need more data.
```

**Sample Size Calculation**

How many users do you need to detect a 20% improvement with 95% confidence?

**Formula** (simplified):
```
n = (16 × baseline_rate × (1 - baseline_rate)) / (min_detectable_effect²)

Example:
Baseline: 3% signup rate
Min effect: 20% improvement (3% → 3.6%)
MDE: 0.6 percentage points = 0.006

n = (16 × 0.03 × 0.97) / (0.006²) ≈ 12,978 per variant

Total needed: ~26,000 visitors (13K control + 13K treatment)
```

**At 1,000 visitors/day, this takes 26 days.**

**Test Duration**

**Too short**: Random daily fluctuations distort results
**Too long**: Opportunity cost of delayed learnings

**Best practice**:
- Minimum: 1-2 weeks (captures weekly cycles)
- Maximum: 4 weeks (if no clear winner by then, effect is too small to matter)

**Common A/B Test Mistakes**

### Mistake 1: Stopping Tests Early

**Problem**: p-value fluctuates. If you check daily and stop when p < 0.05, you'll get false positives.

**Solution**: Pre-commit to sample size and duration. Don't peek.

### Mistake 2: Multiple Testing

**Problem**: Testing 10 variants increases false positive rate to 40% (not 5%).

**Solution**: Use Bonferroni correction (p < 0.05/10 = 0.005) or limit variants to 2-3.

### Mistake 3: Ignoring External Factors

**Problem**: Product Hunt launch during test skews results.

**Solution**: Monitor external events. Restart test if major events occur.

### Mistake 4: Testing Too Small Changes

**Problem**: Testing 3% vs 3.1% signup rate requires 100K+ users.

**Solution**: Focus on tests that could move metrics 10-20%+.

**Implementation: A/B Test Calculator**

```java
package com.bibby.growth;

import org.springframework.stereotype.Service;

@Service
public class ABTestingService {

    public record TestResults(
        int controlVisitors,
        int controlConversions,
        double controlRate,
        int treatmentVisitors,
        int treatmentConversions,
        double treatmentRate,
        double relativeImprovement,
        double pValue,
        boolean isSignificant,
        String recommendation
    ) {}

    /**
     * Calculate A/B test results with statistical significance
     */
    public TestResults analyzeTest(int controlVisitors, int controlConversions,
                                   int treatmentVisitors, int treatmentConversions) {

        double controlRate = (double) controlConversions / controlVisitors;
        double treatmentRate = (double) treatmentConversions / treatmentVisitors;
        double relativeImprovement = ((treatmentRate - controlRate) / controlRate) * 100;

        // Simplified z-test for proportions
        double pooledRate = (double) (controlConversions + treatmentConversions) /
                           (controlVisitors + treatmentVisitors);

        double se = Math.sqrt(pooledRate * (1 - pooledRate) *
                             (1.0/controlVisitors + 1.0/treatmentVisitors));

        double zScore = (treatmentRate - controlRate) / se;
        double pValue = 2 * (1 - normalCDF(Math.abs(zScore)));

        boolean isSignificant = pValue < 0.05;

        String recommendation;
        if (!isSignificant) {
            recommendation = "NOT SIGNIFICANT - Need more data or larger effect";
        } else if (relativeImprovement > 0) {
            recommendation = "WINNER - Scale treatment to 100%";
        } else {
            recommendation = "LOSER - Keep control, kill treatment";
        }

        return new TestResults(
            controlVisitors,
            controlConversions,
            controlRate,
            treatmentVisitors,
            treatmentConversions,
            treatmentRate,
            relativeImprovement,
            pValue,
            isSignificant,
            recommendation
        );
    }

    /**
     * Calculate required sample size
     */
    public int calculateSampleSize(double baselineRate, double minDetectableEffect,
                                   double power, double significance) {
        // Simplified formula (actual calculation is more complex)
        // Using rule of thumb: 16 × p × (1-p) / (MDE²)
        double mde = baselineRate * minDetectableEffect;  // Absolute effect
        return (int) Math.ceil((16 * baselineRate * (1 - baselineRate)) / (mde * mde));
    }

    /**
     * Run sample test scenarios
     */
    public void demonstrateABTesting() {
        System.out.println("=== A/B Testing Examples ===\n");

        // Scenario 1: Clear winner (large sample, big effect)
        System.out.println("Scenario 1: Homepage CTA Copy Test\n");
        TestResults test1 = analyzeTest(
            5000, 150,   // Control: 5000 visitors, 150 signups (3%)
            5000, 225    // Treatment: 5000 visitors, 225 signups (4.5%)
        );
        printTestResults(test1);

        // Scenario 2: Not enough data (small sample)
        System.out.println("\nScenario 2: Onboarding Flow Test (Small Sample)\n");
        TestResults test2 = analyzeTest(
            100, 3,      // Control: 100 users, 3% conversion
            100, 5       // Treatment: 100 users, 5% conversion (+67%)
        );
        printTestResults(test2);

        // Scenario 3: No real difference (random variance)
        System.out.println("\nScenario 3: Pricing Page Test (No Real Effect)\n");
        TestResults test3 = analyzeTest(
            3000, 90,    // Control: 3000 visitors, 90 conversions (3%)
            3000, 95     // Treatment: 3000 visitors, 95 conversions (3.17%)
        );
        printTestResults(test3);

        // Sample size calculation
        System.out.println("\n=== Sample Size Calculator ===\n");
        System.out.println("To detect a 20% improvement from 3% baseline:");
        int sampleSize = calculateSampleSize(0.03, 0.20, 0.8, 0.05);
        System.out.println("Sample size needed per variant: " +
                          String.format("%,d", sampleSize));
        System.out.println("Total visitors needed: " +
                          String.format("%,d", sampleSize * 2));
        System.out.println("At 1,000 visitors/day: " +
                          (sampleSize * 2 / 1000) + " days");
    }

    private void printTestResults(TestResults results) {
        System.out.println("Control:   " + results.controlVisitors() + " visitors, " +
                          results.controlConversions() + " conversions (" +
                          String.format("%.2f%%", results.controlRate() * 100) + ")");
        System.out.println("Treatment: " + results.treatmentVisitors() + " visitors, " +
                          results.treatmentConversions() + " conversions (" +
                          String.format("%.2f%%", results.treatmentRate() * 100) + ")");
        System.out.println("Improvement: " +
                          String.format("%+.1f%%", results.relativeImprovement()));
        System.out.println("p-value: " + String.format("%.4f", results.pValue()) +
                          (results.isSignificant() ? " ✅ SIGNIFICANT" : " ❌ NOT SIGNIFICANT"));
        System.out.println("📊 " + results.recommendation());
    }

    /**
     * Normal CDF approximation (for z-score to p-value conversion)
     */
    private double normalCDF(double z) {
        // Simplified approximation
        return 0.5 * (1 + Math.tanh(Math.sqrt(2/Math.PI) * (z + 0.044715 * Math.pow(z, 3))));
    }
}
```

---

## Part 5: Analyzing Results & Making Decisions

**The Decision Framework**

After running a test, you have 3 options:

### Option 1: Scale (Winner)

**Criteria**:
- Statistically significant (p < 0.05)
- Meaningful improvement (>10% on key metric)
- No negative side effects (doesn't hurt other metrics)

**Action**: Roll out to 100% of users, document learnings

### Option 2: Iterate (Inconclusive or Mixed)

**Criteria**:
- Not significant but promising trend
- Significant but small effect (<5%)
- Positive on one metric, negative on another

**Action**: Run follow-up test with modifications

### Option 3: Kill (Loser)

**Criteria**:
- Statistically significant negative result
- No effect with sufficient sample size
- Positive on vanity metric but negative on revenue

**Action**: Abandon, document learnings, move on

**The Metrics Hierarchy**

Not all metrics are equal. Prioritize:

1. **North Star Metric**: Ultimate business goal (revenue, activation)
2. **Primary Metric**: Direct experiment target (signup rate)
3. **Secondary Metrics**: Related metrics to monitor (time on page)
4. **Guardrail Metrics**: Must not decrease (load time, error rate)

**Example**:

```
Experiment: Simplify signup form (remove 3 fields)

North Star: Activated users (who reach value)
Primary: Signup rate (% who complete form)
Secondary: Time to complete signup, form abandonment rate
Guardrail: Data quality (are we collecting enough info?)

Results:
✅ Primary: Signup rate +22% (15% → 18.3%)
✅ Secondary: Time to complete -30% (60s → 42s)
⚠️  Guardrail: Missing company data for 40% of users

Decision: ITERATE
- Signup rate improved (good!)
- But missing critical data (bad)
- Solution: Add company field back, remove other 2 fields
- Re-test
```

**False Positives vs False Negatives**

- **False Positive**: Thinking a test won when it didn't (5% risk with p < 0.05)
- **False Negative**: Missing a real winner due to insufficient data

**Which is worse?**

- For low-risk tests (copy changes): False negatives worse (miss opportunities)
- For high-risk tests (pricing changes): False positives worse (break revenue)

**Adjust significance threshold accordingly**:
- Low risk: p < 0.10 (10% risk, detect more winners)
- Medium risk: p < 0.05 (standard)
- High risk: p < 0.01 (1% risk, very conservative)

---

## Part 6: Building a Growth Experimentation Culture

**The Growth Team Structure**

**Option 1: Dedicated Growth Team** (larger companies)
- PM, Engineer, Designer, Data Analyst
- Full-time focus on experiments
- 2-4 experiments per week

**Option 2: Growth Pod** (startups)
- 1 person (founder or growth hire)
- Partners with eng/design as needed
- 1-2 experiments per week

**For Bibby (early stage)**:
- You (founder) own growth
- Run 1 experiment per week
- Partner with designer for mockups, developer for implementation

**The Weekly Growth Sprint**

**Monday**: Review last week's results, prioritize new experiments
**Tuesday**: Design and spec new experiments
**Wednesday**: Implement experiments
**Thursday**: Launch experiments
**Friday**: Monitor results, document learnings

**The Growth Meeting Agenda** (30 min weekly)

1. **Review Active Tests** (10 min)
   - What's running? Any interim results?
   - Any tests ready to conclude?

2. **Analyze Completed Tests** (10 min)
   - What were results?
   - Scale, iterate, or kill?
   - Key learnings?

3. **Prioritize Next Tests** (10 min)
   - Review backlog (ICE/RICE scores)
   - Select top 2-3 for next sprint
   - Assign owners and deadlines

**The Experiment Documentation Template**

Every experiment gets a doc:

```markdown
# Experiment: [Name]

## Hypothesis
If we [change], then [metric] will [improve] because [reason]

## Setup
- Variants: Control vs Treatment
- Traffic split: 50/50
- Sample size: 10,000 per variant
- Duration: 2 weeks
- Success metric: Signup rate
- Target: >10% improvement

## Results
- Control: 3.2% (320/10,000)
- Treatment: 3.9% (390/10,000)
- Improvement: +21.9%
- p-value: 0.012 ✅ Significant

## Decision
SCALE - Treatment becomes new default

## Learnings
- Benefit-focused copy (Treatment: "Never forget what you read")
  outperforms feature-focused (Control: "Book management for developers")
- Emotional hook > rational description
- Test 3 more benefit-focused variants to find optimal

## Next Steps
- Roll out to 100%
- Run follow-up test: Test 3 benefit-focused variants
```

**The Growth Playbook**

Document everything you learn:

```
Bibby Growth Playbook

## What Works
✅ Benefit-focused homepage copy (+22% signup)
✅ "No credit card required" badge (+15% signup)
✅ Goodreads one-click import (+40% onboarding completion)
✅ Weekly email retention campaign (+12% 30-day retention)

## What Doesn't Work
❌ Referral program (only 2% participation)
❌ Intercom chatbot (annoyed users, no conversion impact)
❌ $12/mo pricing (conversions dropped 40%)

## Ongoing Tests
🧪 Explainer video on homepage
🧪 Simplified pricing ($8 vs $10)
🧪 Activation email sequence

## Backlog (Top 10 by RICE)
1. SEO content strategy
2. Product Hunt launch
3. Integration with Notion
...
```

**Implementation: Growth Experimentation Tracker**

```java
package com.bibby.growth;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class ExperimentTrackerService {

    public enum ExperimentStatus {
        BACKLOG,
        RUNNING,
        COMPLETED_SCALE,
        COMPLETED_ITERATE,
        COMPLETED_KILL
    }

    public record ExperimentRecord(
        String id,
        String name,
        String hypothesis,
        String metric,
        double targetImprovement,
        LocalDate startDate,
        LocalDate endDate,
        ExperimentStatus status,
        double actualImprovement,
        double pValue,
        String decision,
        String learnings
    ) {}

    private List<ExperimentRecord> experiments = new ArrayList<>();

    /**
     * Log completed experiment
     */
    public void logExperiment(ExperimentRecord experiment) {
        experiments.add(experiment);
        System.out.println("✅ Experiment logged: " + experiment.name());
    }

    /**
     * Generate weekly growth report
     */
    public void printWeeklyReport() {
        System.out.println("=== Weekly Growth Report ===\n");

        long running = experiments.stream()
            .filter(e -> e.status() == ExperimentStatus.RUNNING)
            .count();

        long completed = experiments.stream()
            .filter(e -> e.status() != ExperimentStatus.BACKLOG &&
                        e.status() != ExperimentStatus.RUNNING)
            .count();

        long wins = experiments.stream()
            .filter(e -> e.status() == ExperimentStatus.COMPLETED_SCALE)
            .count();

        System.out.println("📊 Summary:");
        System.out.println("• Experiments Running: " + running);
        System.out.println("• Experiments Completed (All-Time): " + completed);
        System.out.println("• Win Rate: " +
                          (completed > 0 ? String.format("%.0f%%", (wins * 100.0 / completed)) : "N/A"));

        System.out.println("\n🔥 Wins This Quarter:");
        experiments.stream()
            .filter(e -> e.status() == ExperimentStatus.COMPLETED_SCALE)
            .forEach(e -> {
                System.out.println("• " + e.name() +
                                  " (+" + String.format("%.0f%%", e.actualImprovement()) + ")");
            });

        System.out.println("\n🧪 Currently Running:");
        experiments.stream()
            .filter(e -> e.status() == ExperimentStatus.RUNNING)
            .forEach(e -> {
                System.out.println("• " + e.name() + " (Started: " + e.startDate() + ")");
            });

        System.out.println("\n📚 Key Learnings:");
        experiments.stream()
            .filter(e -> e.learnings() != null && !e.learnings().isEmpty())
            .limit(3)
            .forEach(e -> {
                System.out.println("• " + e.learnings());
            });
    }

    /**
     * Sample data for demonstration
     */
    public void loadSampleData() {
        experiments.addAll(List.of(
            new ExperimentRecord(
                "exp-001",
                "Benefit-focused homepage hero copy",
                "If we change hero copy to benefit-focused, signup rate will increase",
                "Signup rate",
                20.0,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 14),
                ExperimentStatus.COMPLETED_SCALE,
                21.9,
                0.012,
                "SCALE - Treatment becomes default",
                "Benefit-focused copy outperforms feature-focused by 22%"
            ),
            new ExperimentRecord(
                "exp-002",
                "Referral program: Invite 3, get 1 month free",
                "If we incentivize referrals, referral rate will increase",
                "Referral rate",
                200.0,
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 2, 15),
                ExperimentStatus.COMPLETED_KILL,
                -10.0,
                0.42,
                "KILL - Low participation, no impact",
                "Referral programs need network effects to work; too early for Bibby"
            ),
            new ExperimentRecord(
                "exp-003",
                "Explainer video on homepage",
                "If we show product in action, signup rate will increase",
                "Signup rate",
                50.0,
                LocalDate.of(2025, 3, 1),
                null,
                ExperimentStatus.RUNNING,
                0.0,
                0.0,
                "TBD",
                null
            )
        ));
    }
}
```

---

## Week 32 Practical Assignment

**Objective**: Design and run your first 10 growth experiments for Bibby.

**Assignment 1: Funnel Analysis**

Analyze Bibby's funnel and identify bottlenecks.

**Deliverables**:
- Current funnel (traffic → signup → activation → retention → revenue)
- Conversion rates at each stage
- Biggest bottleneck (where to focus experiments)

**Assignment 2: Hypothesis Generation**

Generate 20 experiment ideas across AARRR stages.

**Deliverables**:
- 20 hypotheses (use template: "If [change], then [metric] will [improve] because [reason]")
- Mix of acquisition, activation, retention, revenue, referral experiments

**Assignment 3: Prioritization**

Prioritize your 20 experiments using RICE framework.

**Deliverables**:
- RICE score for each experiment
- Top 10 ranked by score
- Selection of 3 experiments for first sprint

**Assignment 4: Experiment Design**

Fully design your top 3 experiments.

**Deliverables**:
- Experiment spec (variants, metric, target, sample size, duration)
- Success criteria (what makes this a win?)
- Implementation plan (what needs to be built?)

**Assignment 5: Run a Test**

Actually run one simple experiment (even if just a mockup or manual test).

**Deliverables**:
- Experiment results (data from test)
- Analysis (significant? winner?)
- Decision (scale, iterate, or kill)
- Learnings document

**Stretch Goal**: Build the experiment tracking system and log all your experiments.

---

## Reflection Questions

1. **Speed vs Rigor**: Moving fast means running lots of tests. Rigor means proper sample sizes and statistical significance. How do you balance velocity with validity?

2. **Intuition vs Data**: Sometimes data says one thing, but your gut says another. When do you trust intuition over data?

3. **Local vs Global Maxima**: Optimization can trap you at a local maximum (incrementally improving a bad strategy). How do you know when to stop optimizing and start reinventing?

4. **Metrics Gaming**: If you optimize for a metric, teams will game it. How do you prevent Goodhart's Law ("When a measure becomes a target, it ceases to be a good measure")?

5. **Experiment Debt**: Every test adds complexity (code branches, tracking logic). How do you clean up after tests?

6. **False Positives**: With 100 tests at p < 0.05, you'll get 5 false positives. How do you avoid building on bad data?

7. **Opportunity Cost**: Running a test means NOT running other tests. How do you manage FOMO (fear of missing out on the test you didn't run)?

8. **Culture of Failure**: 80% of tests fail or are inconclusive. How do you build a culture where failure is learning, not shame?

---

## Key Takeaways

1. **Experimentation is Systematic Discovery**: Growth isn't about executing a playbook—it's about testing everything to find what works for YOU.

2. **Hypotheses Need "Because"**: Good hypotheses have insight ("If X, then Y **because** Z"). The "because" comes from data, research, or first principles.

3. **Prioritize with RICE**: Impact × Reach × Confidence / Effort. Run high-impact, low-effort tests first.

4. **Statistical Rigor Matters**: Don't ship based on "looks good." Calculate significance, hit proper sample sizes, run for full cycles.

5. **Metric Hierarchy**: North Star > Primary > Secondary > Guardrail. Don't sacrifice North Star for primary metric wins.

6. **Document Everything**: Your growth playbook (what works, what doesn't) becomes institutional knowledge. Future you (and your team) will thank you.

7. **Velocity Compounds**: 2 tests/week × 52 weeks = 104 tests. 20% win rate = 20 wins. 1.05^20 = 2.65× growth.

8. **Fail Fast, Learn Faster**: 80% of experiments fail. That's normal. The goal isn't to win every test—it's to learn quickly and iterate.

---

## What's Next?

You now know how to run systematic growth experiments. But experiments alone won't scale Bibby from 100 to 10,000 users. Next week, you'll learn how to think about virality and network effects at scale.

**Next week: Conversion Rate Optimization (CRO)**

You'll learn how to:
- Optimize every step of your funnel
- Run CRO audits and identify friction
- Design high-converting landing pages
- Write persuasive copy that converts
- Build trust and credibility
- Optimize pricing pages and checkout flows

Plus: A complete CRO audit of Bibby's user journey.

---

**Mentor Voice This Week**: **Tech Executive** (Scaled multiple startups through experimentation)

*"Early on, we'd build features for months, then wonder why they didn't move the needle. Now we don't build anything without testing first. Every feature starts as a hypothesis, gets validated with a small test, then scales if it works. This discipline—test before you build, kill what doesn't work, double down on winners—is what separates 10× companies from 1.1× companies. Most teams run 1-2 experiments per quarter. We run 2 per week. That's 100× more learning. And learning compounds."*

---

**Progress Check**: **32/52 weeks complete** (62% of total apprenticeship)

**Semesters**:
- ✅ Semester 1: Systems Thinking & Technical Foundation (Weeks 1-13)
- ✅ Semester 2: Metrics, Economics & Strategy (Weeks 14-26)
- 🔄 Semester 3: Marketing, Go-to-Market & Growth (Weeks 27-39) ← **You are here (Week 32)**
- ⏳ Semester 4: Execution, Revenue & Scale (Weeks 40-52)

You're nearly two-thirds complete. The apprenticeship has transformed from learning fundamentals (systems, metrics) to executing go-to-market (demand gen, sales, PLG, community) to optimizing growth (experimentation). The final 20 weeks will focus on scaling operations and building a sustainable company.
