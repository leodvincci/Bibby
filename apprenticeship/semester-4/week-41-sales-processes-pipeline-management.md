# Week 41: Sales Processes & Pipeline Management

**Semester 4, Week 41 of 52**
**Focus: Execution, Revenue & Scale**

---

## Overview

Last week, you built the billing infrastructure that captures revenue. This week, you'll learn how to **generate** that revenue through effective sales processes.

Sales is often misunderstood by engineers. Many think sales is:
- Pushy tactics and manipulation
- Something you avoid if you build a great product
- A "soft skill" that can't be systematized

**The reality:** Sales is a system, just like any other part of your business. It has:
- Clear inputs (leads) and outputs (closed deals)
- Measurable conversion rates at each stage
- Optimization opportunities at every step
- Repeatable processes that can be documented and scaled

Whether you're building a self-serve product or selling enterprise software, you need sales processes. Even "product-led growth" companies need sales for expansion deals, enterprise tiers, and strategic accounts.

This week, you'll learn to design, implement, and scale sales processes from first principles.

**By the end of this week, you will:**

- Understand the three sales motions: self-serve, SMB, and enterprise
- Build and manage sales pipelines with clear stages
- Implement CRM systems and lead scoring
- Create sales playbooks with discovery frameworks
- Design compensation structures that drive behavior
- Measure and optimize sales efficiency
- Scale from founder-led to sales team
- Apply sales processes to Bibby's enterprise tier

**Week Structure:**
- **Part 1:** Sales Motion Fundamentals
- **Part 2:** Sales Pipeline Architecture
- **Part 3:** Lead Management & Qualification
- **Part 4:** Sales Process & Playbooks
- **Part 5:** CRM Implementation
- **Part 6:** Sales Team Structure & Scaling
- **Part 7:** Sales Metrics & Optimization
- **Part 8:** Enterprise Sales for Bibby

---

## Part 1: Sales Motion Fundamentals

### The Three Sales Motions

Sales motion = how you acquire and close customers.

**1. Self-Serve (Product-Led Growth)**

**Characteristics:**
- No sales rep involvement
- Customer self-discovers, signs up, and pays
- Price: $0-$500/year
- Sales cycle: Minutes to days
- Volume: Thousands of customers

**Examples:** Canva, Figma (free tier), Notion (personal), Grammarly

**Pros:**
✅ Low CAC (no sales team)
✅ Scalable (no human bottleneck)
✅ Fast growth (frictionless signup)

**Cons:**
❌ Low deal size
❌ Limited customer context
❌ Harder to upsell

**2. SMB Sales (Inside Sales)**

**Characteristics:**
- Sales rep involvement (SDR → AE)
- Phone/video sales, no in-person meetings
- Price: $5K-50K/year
- Sales cycle: 2-8 weeks
- Volume: Hundreds of customers

**Examples:** HubSpot, Zendesk, Monday.com

**Pros:**
✅ Higher ACV than self-serve
✅ Better customer qualification
✅ Opportunity for upselling

**Cons:**
❌ Requires sales team
❌ Higher CAC
❌ Less scalable than self-serve

**3. Enterprise Sales (Field Sales)**

**Characteristics:**
- Multiple stakeholders, complex decision-making
- In-person meetings, executive engagement
- Price: $100K-$1M+/year
- Sales cycle: 3-12 months
- Volume: Tens of customers

**Examples:** Salesforce, Workday, ServiceNow

**Pros:**
✅ Highest ACV
✅ Strategic relationships
✅ Predictable revenue (multi-year contracts)

**Cons:**
❌ Very high CAC
❌ Long sales cycles
❌ Requires specialized talent

### Choosing Your Sales Motion

**Decision framework:**

| Factor | Self-Serve | SMB | Enterprise |
|--------|-----------|-----|------------|
| **ACV** | < $500 | $5K-50K | $100K+ |
| **Product complexity** | Simple, intuitive | Moderate | Complex |
| **Buyer** | Individual | Manager/Director | VP/C-level |
| **Implementation** | Self-service | Guided onboarding | Professional services |
| **Support** | Help docs, chat | Email, phone | Dedicated CSM |
| **Sales cycle** | < 1 week | 2-8 weeks | 3-12 months |

**Most SaaS companies use a hybrid approach:**

```
Self-Serve → SMB → Enterprise
(land)     (expand) (grow)
```

- **Land:** Self-serve freemium for initial adoption
- **Expand:** Inside sales for growing teams
- **Enterprise:** Field sales for large organizations

### Code Example: Sales Motion Classifier

```java
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SalesMotionService {

    public enum SalesMotion {
        SELF_SERVE,
        SMB_INSIDE_SALES,
        ENTERPRISE_FIELD_SALES
    }

    public record LeadProfile(
        String companyName,
        int employeeCount,
        String industry,
        long estimatedACV,
        int numberOfUsers,
        boolean hasComplexRequirements,
        String buyerLevel  // "IC", "Manager", "Director", "VP", "C-Level"
    ) {}

    /**
     * Determine appropriate sales motion for a lead
     */
    public SalesMotion classifySalesMotion(LeadProfile lead) {
        // Enterprise signals
        if (lead.estimatedACV() >= 100_000 ||
            lead.employeeCount() >= 1000 ||
            lead.buyerLevel().equals("VP") ||
            lead.buyerLevel().equals("C-Level") ||
            lead.hasComplexRequirements()) {
            return SalesMotion.ENTERPRISE_FIELD_SALES;
        }

        // SMB signals
        if (lead.estimatedACV() >= 5_000 ||
            lead.numberOfUsers() >= 10 ||
            lead.employeeCount() >= 50) {
            return SalesMotion.SMB_INSIDE_SALES;
        }

        // Default to self-serve
        return SalesMotion.SELF_SERVE;
    }

    /**
     * Route lead to appropriate sales process
     */
    public record SalesRouting(
        SalesMotion motion,
        String assignedTo,
        int priorityScore,
        String recommendedAction
    ) {}

    public SalesRouting routeLead(LeadProfile lead) {
        SalesMotion motion = classifySalesMotion(lead);

        return switch (motion) {
            case SELF_SERVE -> new SalesRouting(
                motion,
                "Product-Led Growth",
                calculatePriorityScore(lead),
                "Send to self-serve signup flow with automated onboarding"
            );

            case SMB_INSIDE_SALES -> new SalesRouting(
                motion,
                "Inside Sales Team",
                calculatePriorityScore(lead),
                "Assign to SDR for qualification call within 24 hours"
            );

            case ENTERPRISE_FIELD_SALES -> new SalesRouting(
                motion,
                "Enterprise Sales Team",
                calculatePriorityScore(lead),
                "Assign to AE for discovery call within 4 hours"
            );
        };
    }

    private int calculatePriorityScore(LeadProfile lead) {
        int score = 0;

        // ACV scoring
        if (lead.estimatedACV() >= 100_000) score += 40;
        else if (lead.estimatedACV() >= 50_000) score += 30;
        else if (lead.estimatedACV() >= 10_000) score += 20;
        else if (lead.estimatedACV() >= 5_000) score += 10;

        // Company size scoring
        if (lead.employeeCount() >= 1000) score += 30;
        else if (lead.employeeCount() >= 500) score += 20;
        else if (lead.employeeCount() >= 100) score += 10;

        // Buyer level scoring
        score += switch (lead.buyerLevel()) {
            case "C-Level" -> 20;
            case "VP" -> 15;
            case "Director" -> 10;
            case "Manager" -> 5;
            default -> 0;
        };

        // Industry fit (example: higher education is good fit for Bibby)
        if (lead.industry().equals("Higher Education") ||
            lead.industry().equals("Publishing")) {
            score += 10;
        }

        return Math.min(score, 100);  // Cap at 100
    }
}
```

---

## Part 2: Sales Pipeline Architecture

### What is a Sales Pipeline?

A **sales pipeline** is the visual representation of where prospects are in your sales process.

**Classic B2B SaaS pipeline stages:**

```
Lead → Qualified → Discovery → Proposal → Negotiation → Closed Won/Lost
```

**Detailed example:**

| Stage | Description | Conversion Rate | Avg Days |
|-------|-------------|-----------------|----------|
| **Lead** | Inbound or outbound contact | - | - |
| **MQL** | Marketing Qualified Lead | 30% | 1 |
| **SQL** | Sales Qualified Lead | 50% | 3 |
| **Discovery** | Qualification call completed | 60% | 7 |
| **Demo** | Product demo delivered | 50% | 10 |
| **Proposal** | Pricing proposal sent | 40% | 14 |
| **Negotiation** | Contract negotiation | 70% | 21 |
| **Closed Won** | Deal signed | - | - |

**Overall conversion rate:** 30% × 50% × 60% × 50% × 40% × 70% = **1.26%**

This means you need **~80 leads** to generate **1 closed deal**.

### Pipeline Velocity

**Pipeline velocity** = how fast deals move through your pipeline.

**Formula:**
```
Pipeline Velocity = (# Opportunities × Average Deal Size × Win Rate) / Sales Cycle Length
```

**Example:**
- 50 opportunities in pipeline
- Average deal size: $20,000
- Win rate: 25%
- Sales cycle: 60 days

```
Velocity = (50 × $20,000 × 0.25) / 60 = $4,167/day
Monthly revenue = $4,167 × 30 = $125,000
```

**Improving pipeline velocity:**

1. **Increase # of opportunities:** Better marketing, more SDRs
2. **Increase average deal size:** Upsell, better packaging
3. **Increase win rate:** Better qualification, stronger playbooks
4. **Decrease sales cycle:** Remove friction, faster decision-making

### Code Example: Pipeline Management Service

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Document(collection = "opportunities")
public record Opportunity(
    @Id String opportunityId,
    String leadId,
    String companyName,
    String contactName,
    String contactEmail,
    PipelineStage stage,
    long estimatedValueCents,
    double winProbability,
    LocalDateTime createdAt,
    LocalDateTime stageChangedAt,
    LocalDateTime expectedCloseDate,
    String ownerId,  // Assigned sales rep
    Map<String, Object> metadata
) {
    public enum PipelineStage {
        LEAD(0.05),
        MQL(0.10),
        SQL(0.20),
        DISCOVERY(0.30),
        DEMO(0.40),
        PROPOSAL(0.50),
        NEGOTIATION(0.70),
        CLOSED_WON(1.00),
        CLOSED_LOST(0.00);

        private final double defaultWinProbability;

        PipelineStage(double defaultWinProbability) {
            this.defaultWinProbability = defaultWinProbability;
        }

        public double getDefaultWinProbability() {
            return defaultWinProbability;
        }
    }

    public long daysInCurrentStage() {
        return ChronoUnit.DAYS.between(stageChangedAt, LocalDateTime.now());
    }

    public long daysInPipeline() {
        return ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }

    public long weightedValue() {
        return (long) (estimatedValueCents * winProbability);
    }
}

@Service
public class PipelineManagementService {

    /**
     * Move opportunity to next stage
     */
    public Opportunity advanceStage(
        String opportunityId,
        Opportunity.PipelineStage newStage
    ) {
        Opportunity opp = opportunityRepository.findById(opportunityId)
            .orElseThrow(() -> new IllegalArgumentException("Opportunity not found"));

        // Validate stage progression (can't skip stages backwards)
        if (newStage.ordinal() < opp.stage().ordinal() &&
            newStage != Opportunity.PipelineStage.CLOSED_LOST) {
            throw new IllegalStateException("Cannot move opportunity backwards");
        }

        // Update win probability based on stage
        double newWinProbability = newStage.getDefaultWinProbability();

        Opportunity updated = new Opportunity(
            opp.opportunityId(),
            opp.leadId(),
            opp.companyName(),
            opp.contactName(),
            opp.contactEmail(),
            newStage,
            opp.estimatedValueCents(),
            newWinProbability,
            opp.createdAt(),
            LocalDateTime.now(),  // Update stageChangedAt
            opp.expectedCloseDate(),
            opp.ownerId(),
            opp.metadata()
        );

        return opportunityRepository.save(updated);
    }

    /**
     * Calculate pipeline metrics
     */
    public record PipelineMetrics(
        int totalOpportunities,
        long totalValueCents,
        long weightedValueCents,
        double averageWinProbability,
        double averageDaysInPipeline,
        Map<Opportunity.PipelineStage, Integer> stageDistribution
    ) {}

    public PipelineMetrics calculateMetrics(List<Opportunity> opportunities) {
        if (opportunities.isEmpty()) {
            return new PipelineMetrics(0, 0, 0, 0, 0, new HashMap<>());
        }

        long totalValue = opportunities.stream()
            .mapToLong(Opportunity::estimatedValueCents)
            .sum();

        long weightedValue = opportunities.stream()
            .mapToLong(Opportunity::weightedValue)
            .sum();

        double avgWinProb = opportunities.stream()
            .mapToDouble(Opportunity::winProbability)
            .average()
            .orElse(0);

        double avgDays = opportunities.stream()
            .mapToLong(Opportunity::daysInPipeline)
            .average()
            .orElse(0);

        Map<Opportunity.PipelineStage, Long> distribution = opportunities.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Opportunity::stage,
                java.util.stream.Collectors.counting()
            ));

        Map<Opportunity.PipelineStage, Integer> distributionInt = new HashMap<>();
        distribution.forEach((k, v) -> distributionInt.put(k, v.intValue()));

        return new PipelineMetrics(
            opportunities.size(),
            totalValue,
            weightedValue,
            avgWinProb,
            avgDays,
            distributionInt
        );
    }

    /**
     * Calculate pipeline velocity
     */
    public record PipelineVelocity(
        double dailyRevenue,
        double monthlyRevenue,
        double quarterlyRevenue,
        String bottleneck
    ) {}

    public PipelineVelocity calculateVelocity(List<Opportunity> opportunities) {
        // Filter to open opportunities only
        List<Opportunity> open = opportunities.stream()
            .filter(o -> o.stage() != Opportunity.PipelineStage.CLOSED_WON &&
                        o.stage() != Opportunity.PipelineStage.CLOSED_LOST)
            .toList();

        if (open.isEmpty()) {
            return new PipelineVelocity(0, 0, 0, "No opportunities in pipeline");
        }

        long weightedPipeline = open.stream()
            .mapToLong(Opportunity::weightedValue)
            .sum();

        double avgCycleLength = open.stream()
            .mapToLong(Opportunity::daysInPipeline)
            .average()
            .orElse(60);  // Default 60 days

        double dailyRevenue = weightedPipeline / avgCycleLength;
        double monthlyRevenue = dailyRevenue * 30;
        double quarterlyRevenue = dailyRevenue * 90;

        // Identify bottleneck (stage with most opportunities stuck)
        String bottleneck = identifyBottleneck(open);

        return new PipelineVelocity(
            dailyRevenue / 100.0,  // Convert to dollars
            monthlyRevenue / 100.0,
            quarterlyRevenue / 100.0,
            bottleneck
        );
    }

    private String identifyBottleneck(List<Opportunity> opportunities) {
        // Find stage with longest average time
        Map<Opportunity.PipelineStage, Double> avgTimeByStage = new HashMap<>();

        for (Opportunity.PipelineStage stage : Opportunity.PipelineStage.values()) {
            List<Opportunity> inStage = opportunities.stream()
                .filter(o -> o.stage() == stage)
                .toList();

            if (!inStage.isEmpty()) {
                double avgDays = inStage.stream()
                    .mapToLong(Opportunity::daysInCurrentStage)
                    .average()
                    .orElse(0);

                avgTimeByStage.put(stage, avgDays);
            }
        }

        // Find stage with highest average time
        return avgTimeByStage.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(e -> e.getKey().name() + " (" + String.format("%.0f days avg)", e.getValue()) + ")")
            .orElse("No bottleneck identified");
    }

    /**
     * Identify at-risk opportunities
     */
    public record AtRiskOpportunity(
        Opportunity opportunity,
        String reason,
        String recommendation
    ) {}

    public List<AtRiskOpportunity> identifyAtRisk(List<Opportunity> opportunities) {
        List<AtRiskOpportunity> atRisk = new ArrayList<>();

        for (Opportunity opp : opportunities) {
            // Stuck too long in stage
            if (opp.daysInCurrentStage() > 21) {
                atRisk.add(new AtRiskOpportunity(
                    opp,
                    "Stuck in " + opp.stage() + " for " + opp.daysInCurrentStage() + " days",
                    "Reach out to re-engage. May need executive involvement."
                ));
            }

            // Expected close date passed
            if (opp.expectedCloseDate() != null &&
                opp.expectedCloseDate().isBefore(LocalDateTime.now())) {
                atRisk.add(new AtRiskOpportunity(
                    opp,
                    "Passed expected close date by " +
                        ChronoUnit.DAYS.between(opp.expectedCloseDate(), LocalDateTime.now()) + " days",
                    "Update close date or close as lost"
                ));
            }

            // Too long in pipeline overall
            if (opp.daysInPipeline() > 90 &&
                opp.stage().ordinal() < Opportunity.PipelineStage.PROPOSAL.ordinal()) {
                atRisk.add(new AtRiskOpportunity(
                    opp,
                    "In pipeline for " + opp.daysInPipeline() + " days but not yet at proposal stage",
                    "Qualify out if not progressing"
                ));
            }
        }

        return atRisk;
    }
}
```

---

## Part 3: Lead Management & Qualification

### Lead Sources

**Inbound:**
- Website form fills
- Demo requests
- Free trial signups
- Content downloads
- Webinar registrations

**Outbound:**
- Cold email
- LinkedIn outreach
- Cold calling
- Conference networking
- Referrals

### Lead Qualification Frameworks

**BANT (Classic):**
- **Budget:** Can they afford it?
- **Authority:** Can they sign the contract?
- **Need:** Do they have a problem we solve?
- **Timeline:** When will they buy?

**MEDDIC (Enterprise Sales):**
- **Metrics:** What is the economic impact?
- **Economic Buyer:** Who controls the budget?
- **Decision Criteria:** How will they evaluate options?
- **Decision Process:** What is the approval process?
- **Identify Pain:** What problem are they solving?
- **Champion:** Who internally advocates for us?

**CHAMP (Modern):**
- **Challenges:** What problems do they have?
- **Authority:** Who makes the decision?
- **Money:** Do they have budget?
- **Prioritization:** Is this a priority now?

### Code Example: Lead Scoring Service

```java
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class LeadScoringService {

    public record Lead(
        String leadId,
        String companyName,
        String contactName,
        String email,
        String phone,
        int employeeCount,
        String industry,
        String jobTitle,
        LeadSource source,
        Map<String, Object> behavioralData,
        LocalDateTime createdAt
    ) {}

    public enum LeadSource {
        WEBSITE_FORM,
        DEMO_REQUEST,
        FREE_TRIAL,
        CONTENT_DOWNLOAD,
        WEBINAR,
        COLD_OUTBOUND,
        REFERRAL,
        CONFERENCE,
        OTHER
    }

    public record LeadScore(
        int totalScore,
        int firmographicScore,
        int behavioralScore,
        String grade,  // A, B, C, D
        boolean isMQL,
        String recommendation
    ) {}

    /**
     * Calculate lead score
     */
    public LeadScore scoreLead(Lead lead) {
        int firmographicScore = calculateFirmographicScore(lead);
        int behavioralScore = calculateBehavioralScore(lead);
        int totalScore = firmographicScore + behavioralScore;

        String grade = assignGrade(totalScore);
        boolean isMQL = totalScore >= 70;  // Threshold for Marketing Qualified Lead

        String recommendation = generateRecommendation(totalScore, lead);

        return new LeadScore(
            totalScore,
            firmographicScore,
            behavioralScore,
            grade,
            isMQL,
            recommendation
        );
    }

    /**
     * Firmographic scoring (who they are)
     */
    private int calculateFirmographicScore(Lead lead) {
        int score = 0;

        // Company size
        if (lead.employeeCount() >= 1000) score += 25;
        else if (lead.employeeCount() >= 500) score += 20;
        else if (lead.employeeCount() >= 100) score += 15;
        else if (lead.employeeCount() >= 50) score += 10;
        else score += 5;

        // Industry fit (example: education is ideal for Bibby)
        if (lead.industry().equals("Higher Education")) score += 15;
        else if (lead.industry().equals("Publishing")) score += 10;
        else if (lead.industry().equals("Library Services")) score += 15;
        else score += 5;

        // Job title/seniority
        if (lead.jobTitle().contains("Director") ||
            lead.jobTitle().contains("VP") ||
            lead.jobTitle().contains("Chief")) {
            score += 10;
        } else if (lead.jobTitle().contains("Manager")) {
            score += 5;
        }

        return Math.min(score, 50);  // Max 50 points for firmographics
    }

    /**
     * Behavioral scoring (what they've done)
     */
    private int calculateBehavioralScore(Lead lead) {
        int score = 0;
        Map<String, Object> behavior = lead.behavioralData();

        // Lead source value
        score += switch (lead.source()) {
            case DEMO_REQUEST -> 20;
            case FREE_TRIAL -> 15;
            case REFERRAL -> 15;
            case WEBINAR -> 10;
            case WEBSITE_FORM -> 10;
            case CONTENT_DOWNLOAD -> 5;
            case COLD_OUTBOUND -> 5;
            case CONFERENCE -> 10;
            case OTHER -> 0;
        };

        // Website engagement
        int pageViews = (int) behavior.getOrDefault("pageViews", 0);
        if (pageViews >= 10) score += 10;
        else if (pageViews >= 5) score += 5;

        // Email engagement
        int emailOpens = (int) behavior.getOrDefault("emailOpens", 0);
        int emailClicks = (int) behavior.getOrDefault("emailClicks", 0);
        score += Math.min(emailOpens * 2 + emailClicks * 3, 10);

        // Product engagement (for trial users)
        boolean triedProduct = (boolean) behavior.getOrDefault("triedProduct", false);
        if (triedProduct) {
            int sessionsCount = (int) behavior.getOrDefault("sessionsCount", 0);
            score += Math.min(sessionsCount * 2, 10);
        }

        // Recency boost
        long daysSinceCreated = java.time.temporal.ChronoUnit.DAYS.between(
            lead.createdAt(), LocalDateTime.now()
        );
        if (daysSinceCreated <= 7) score += 5;  // Hot lead

        return Math.min(score, 50);  // Max 50 points for behavioral
    }

    private String assignGrade(int score) {
        if (score >= 80) return "A";
        if (score >= 60) return "B";
        if (score >= 40) return "C";
        return "D";
    }

    private String generateRecommendation(int totalScore, Lead lead) {
        if (totalScore >= 80) {
            return "High-priority lead. Assign to AE for immediate outreach.";
        } else if (totalScore >= 70) {
            return "Qualified lead (MQL). Pass to SDR for qualification call.";
        } else if (totalScore >= 50) {
            return "Warm lead. Continue nurturing with targeted content.";
        } else {
            return "Low-fit lead. Add to long-term nurture campaign.";
        }
    }

    /**
     * BANT qualification
     */
    public record BANTQualification(
        boolean hasBudget,
        boolean hasAuthority,
        boolean hasNeed,
        boolean hasTimeline,
        boolean isQualified,
        String notes
    ) {}

    public BANTQualification qualifyBANT(
        boolean hasBudget,
        boolean hasAuthority,
        boolean hasNeed,
        boolean hasTimeline,
        String notes
    ) {
        boolean isQualified = hasBudget && hasAuthority && hasNeed && hasTimeline;

        return new BANTQualification(
            hasBudget,
            hasAuthority,
            hasNeed,
            hasTimeline,
            isQualified,
            notes
        );
    }
}
```

---

## Part 4: Sales Process & Playbooks

### Discovery Call Framework

**Discovery call** = first sales conversation with qualified lead.

**Goals:**
1. Understand their situation, problems, goals
2. Qualify fit (BANT/CHAMP/MEDDIC)
3. Build rapport and trust
4. Earn the right to next step (demo/proposal)

**Structure (45-60 min call):**

**1. Intro & Agenda (5 min)**
- Thank them for their time
- Share agenda
- Ask how much time they have

**2. Their Story (15-20 min)**
Ask open-ended questions:
- "Tell me about your current process for [solving problem]"
- "What prompted you to look for a solution now?"
- "What's working well? What's frustrating?"
- "Walk me through a typical day/week"
- "Who else is involved in this process?"

**3. Problem Deep-Dive (15-20 min)**
- "You mentioned [pain point]. Tell me more about that."
- "How is that impacting your team/business?"
- "What have you tried to solve this?"
- "What would success look like?"
- "If you could wave a magic wand, what would change?"

**4. Budget & Authority (5-10 min)**
- "Have you allocated budget for this?"
- "What's the approval process like at your company?"
- "Who else needs to be involved in this decision?"
- "What's your timeline for making a decision?"

**5. Next Steps (5 min)**
- Summarize what you heard
- Propose next step (demo, proposal, etc.)
- Schedule follow-up
- Send recap email

### Discovery Question Bank

**Situation questions:**
- How are you currently solving [problem]?
- How long have you been using [current solution]?
- How many people are involved in [process]?

**Problem questions:**
- What's the biggest challenge with your current approach?
- What happens if you don't solve this?
- How much time/money are you losing to this problem?

**Implication questions:**
- How does this problem affect other parts of your business?
- What would happen if this gets worse?
- Who else is impacted by this?

**Need-payoff questions:**
- What would it mean to your team if you solved this?
- How would success be measured?
- What would be different 6 months from now?

### Demo Best Practices

**Demo structure:**

1. **Recap their needs (5 min)**
   - "When we last talked, you mentioned [pain points]. Did I get that right?"

2. **Show outcome first (10 min)**
   - Start with the "after" state
   - "Here's what it looks like when [problem is solved]"
   - Don't start with login screen

3. **Feature walkthrough mapped to their needs (20-30 min)**
   - For each feature: "Remember you said [pain]? Here's how we solve that."
   - Use their data if possible (customize demo)
   - Let them drive ("What would you like to see?")

4. **Address concerns (10 min)**
   - "What questions do you have?"
   - "What concerns do you have?"
   - "What would prevent you from moving forward?"

5. **Next steps (5 min)**
   - "Based on what you've seen, does this solve your problem?"
   - If yes: "Great, next step is [proposal/trial/contract]"
   - If no: "What's missing?"

### Code Example: Sales Playbook Service

```java
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SalesPlaybookService {

    public enum CallType {
        DISCOVERY,
        DEMO,
        PROPOSAL,
        NEGOTIATION,
        CLOSE
    }

    public record PlaybookStep(
        String title,
        int durationMinutes,
        List<String> objectives,
        List<String> questions,
        List<String> talkingPoints,
        List<String> nextSteps
    ) {}

    /**
     * Get playbook for call type
     */
    public List<PlaybookStep> getPlaybook(CallType callType) {
        return switch (callType) {
            case DISCOVERY -> getDiscoveryPlaybook();
            case DEMO -> getDemoPlaybook();
            case PROPOSAL -> getProposalPlaybook();
            case NEGOTIATION -> getNegotiationPlaybook();
            case CLOSE -> getClosePlaybook();
        };
    }

    private List<PlaybookStep> getDiscoveryPlaybook() {
        return List.of(
            new PlaybookStep(
                "Intro & Agenda",
                5,
                List.of("Set expectations", "Build rapport", "Confirm time available"),
                List.of("How's your day going?", "Is this still a good time?", "How much time do you have?"),
                List.of("Agenda: Learn about your situation, see if we can help, determine next steps"),
                List.of()
            ),
            new PlaybookStep(
                "Their Story",
                20,
                List.of("Understand current state", "Identify problems", "Build context"),
                List.of(
                    "Tell me about your current process for managing books/library",
                    "What prompted you to look for a solution now?",
                    "What's working well? What's frustrating?",
                    "Who else is involved in this process?"
                ),
                List.of(),
                List.of()
            ),
            new PlaybookStep(
                "Problem Deep-Dive",
                20,
                List.of("Quantify pain", "Understand impact", "Identify urgency"),
                List.of(
                    "You mentioned [pain]. Tell me more about that.",
                    "How is that impacting your team/students?",
                    "What have you tried to solve this?",
                    "What would success look like?",
                    "What happens if you don't solve this?"
                ),
                List.of("Listen for: time wasted, money lost, frustration, manual work"),
                List.of()
            ),
            new PlaybookStep(
                "Budget & Authority",
                10,
                List.of("Qualify budget", "Understand decision process", "Identify timeline"),
                List.of(
                    "Have you allocated budget for this?",
                    "What's the approval process like?",
                    "Who else needs to be involved?",
                    "What's your timeline for making a decision?"
                ),
                List.of("If no budget: 'What would need to happen to get budget allocated?'"),
                List.of()
            ),
            new PlaybookStep(
                "Next Steps",
                5,
                List.of("Summarize understanding", "Propose next step", "Get commitment"),
                List.of(
                    "Based on what you've shared, does this sound like something worth exploring?",
                    "What would you like to see next?"
                ),
                List.of(
                    "Recap: You're looking to solve [X, Y, Z problems]",
                    "I think we can help with [solutions]",
                    "Next step: Let's schedule a demo where I'll show you exactly how"
                ),
                List.of("Schedule demo", "Send recap email with notes", "Add to CRM with BANT notes")
            )
        );
    }

    private List<PlaybookStep> getDemoPlaybook() {
        return List.of(
            new PlaybookStep(
                "Recap Needs",
                5,
                List.of("Confirm understanding", "Re-establish context"),
                List.of("When we last talked, you mentioned [pain points]. Did I get that right?"),
                List.of(),
                List.of()
            ),
            new PlaybookStep(
                "Show Outcome First",
                10,
                List.of("Show value immediately", "Create vision of success"),
                List.of(),
                List.of(
                    "Here's what it looks like when your library is fully cataloged and searchable",
                    "Imagine your students finding any book in seconds",
                    "Picture your staff saving 10 hours/week on manual processes"
                ),
                List.of()
            ),
            new PlaybookStep(
                "Feature Walkthrough",
                30,
                List.of("Map features to their needs", "Address each pain point", "Let them drive"),
                List.of(
                    "Would you like to see [feature]?",
                    "Does this solve your problem with [pain]?",
                    "What else would you like to see?"
                ),
                List.of(
                    "For each feature: 'Remember you said [pain]? Here's how we solve that.'",
                    "Use their data if possible (e.g., import sample catalog)",
                    "Pause frequently for questions"
                ),
                List.of()
            ),
            new PlaybookStep(
                "Address Concerns",
                10,
                List.of("Surface objections", "Address concerns", "Build confidence"),
                List.of(
                    "What questions do you have?",
                    "What concerns do you have?",
                    "What would prevent you from moving forward?",
                    "How does this compare to other solutions you're evaluating?"
                ),
                List.of(),
                List.of()
            ),
            new PlaybookStep(
                "Close for Next Step",
                5,
                List.of("Get commitment", "Advance deal", "Set timeline"),
                List.of(
                    "Based on what you've seen, does this solve your problem?",
                    "What would you need to see to feel confident moving forward?",
                    "What are the next steps on your end?"
                ),
                List.of(),
                List.of("If yes: Send proposal", "If maybe: Address remaining concerns", "If no: Understand why and close lost")
            )
        );
    }

    private List<PlaybookStep> getProposalPlaybook() {
        return List.of(
            new PlaybookStep(
                "Proposal Review",
                30,
                List.of("Walk through proposal", "Justify pricing", "Handle objections"),
                List.of("Does this match what we discussed?", "Any surprises?", "What questions do you have?"),
                List.of(
                    "Pricing is based on [users/books/value delivered]",
                    "ROI: You'll save [X hours/week] = $[Y value/year]",
                    "Implementation: We'll have you up and running in [timeline]"
                ),
                List.of("Get verbal agreement", "Identify remaining blockers", "Set timeline for decision")
            )
        );
    }

    private List<PlaybookStep> getNegotiationPlaybook() {
        return List.of(
            new PlaybookStep(
                "Negotiation",
                30,
                List.of("Understand objections", "Find win-win", "Protect margin"),
                List.of("What's preventing you from moving forward?", "What would make this a no-brainer?"),
                List.of(
                    "If price too high: 'Let's talk about value. What's it worth to solve [problem]?'",
                    "If comparing to competitor: 'What do they offer that we don't?'",
                    "If need discount: 'I can do [X] if you can commit to [longer term/more users/case study]'"
                ),
                List.of("Get to 'yes' or 'no' (avoid maybe)", "Set deadline for decision")
            )
        );
    }

    private List<PlaybookStep> getClosePlaybook() {
        return List.of(
            new PlaybookStep(
                "Close",
                15,
                List.of("Get signature", "Confirm next steps", "Set expectations"),
                List.of("Are you ready to move forward?", "Any final questions before we get started?"),
                List.of(
                    "Sign contract",
                    "Kickoff call with implementation team",
                    "You'll receive onboarding email within 24 hours"
                ),
                List.of("Send signed contract", "Introduce to customer success", "Mark opportunity as Closed Won")
            )
        );
    }

    /**
     * Common objection handlers
     */
    public record ObjectionHandler(
        String objection,
        String category,
        List<String> responses,
        List<String> questions
    ) {}

    public List<ObjectionHandler> getCommonObjections() {
        return List.of(
            new ObjectionHandler(
                "Price is too high",
                "Pricing",
                List.of(
                    "I understand. Let's talk about the value you'll get. What's it worth to solve [problem]?",
                    "Compared to [current solution cost + time wasted], this actually saves you money.",
                    "What budget did you have in mind? Let's see if we can find a package that fits."
                ),
                List.of(
                    "What are you currently spending on [problem area]?",
                    "How much time are you losing to manual processes?",
                    "What's the cost of not solving this problem?"
                )
            ),
            new ObjectionHandler(
                "We need to think about it",
                "Stalling",
                List.of(
                    "Of course! What specifically do you need to think about?",
                    "What information is missing that would help you decide?",
                    "Let's schedule a follow-up. When would be a good time?"
                ),
                List.of(
                    "Is there something I haven't addressed?",
                    "What concerns do you have?",
                    "Who else needs to be involved in this decision?"
                )
            ),
            new ObjectionHandler(
                "We're already using [competitor]",
                "Competition",
                List.of(
                    "Great! What do you like about [competitor]? What would you change?",
                    "Interesting. How is that working for you?",
                    "We hear that a lot. Many customers switched from [competitor] because [reasons]."
                ),
                List.of(
                    "What prompted you to take this call if you're happy with [competitor]?",
                    "What would it take to switch?",
                    "Are you evaluating alternatives, or just exploring?"
                )
            ),
            new ObjectionHandler(
                "We don't have budget this year",
                "Budget",
                List.of(
                    "I understand. When does your budget cycle reset?",
                    "What would need to happen to get budget allocated?",
                    "Let's stay in touch. I'll check back in [before next budget cycle]."
                ),
                List.of(
                    "If budget weren't an issue, is this something you'd move forward with?",
                    "Can we start with a pilot/trial to prove value before budget allocation?",
                    "Who controls budget decisions?"
                )
            )
        );
    }
}
```

---

## Part 5: CRM Implementation

### What is a CRM?

**CRM (Customer Relationship Management)** = system to manage all customer interactions.

**Core functions:**
- **Lead management:** Track leads from first touch to close
- **Contact management:** Store contact details, interaction history
- **Opportunity management:** Track deals through pipeline
- **Activity tracking:** Log calls, emails, meetings
- **Reporting:** Pipeline metrics, forecasting, rep performance

**Popular CRMs:**
- **Salesforce:** Enterprise standard, powerful but complex
- **HubSpot:** Great for SMB, free tier available
- **Pipedrive:** Simple, visual pipeline management
- **Close:** Built for inside sales teams
- **Custom:** Build your own (only if CRM is your product!)

### CRM Data Model

```java
// Lead → Contact/Account → Opportunity → Deal
```

**Lead:**
- Not yet qualified
- Single person
- May or may not convert

**Contact:**
- Individual person
- Belongs to an Account

**Account:**
- Company/organization
- Has multiple Contacts

**Opportunity:**
- Potential deal
- Linked to Account and Contact
- Moves through pipeline stages

**Code Example: CRM Service**

```java
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "accounts")
public record Account(
    @Id String accountId,
    String companyName,
    String industry,
    int employeeCount,
    String website,
    String billingAddress,
    AccountStatus status,
    LocalDateTime createdAt,
    Map<String, Object> customFields
) {
    public enum AccountStatus {
        PROSPECT,
        CUSTOMER,
        CHURNED
    }
}

@Document(collection = "contacts")
public record Contact(
    @Id String contactId,
    String accountId,
    String firstName,
    String lastName,
    String email,
    String phone,
    String jobTitle,
    boolean isPrimaryContact,
    LocalDateTime createdAt
) {
    public String fullName() {
        return firstName + " " + lastName;
    }
}

@Document(collection = "activities")
public record Activity(
    @Id String activityId,
    String opportunityId,
    String contactId,
    ActivityType type,
    String subject,
    String description,
    LocalDateTime activityDate,
    String ownerId,
    ActivityStatus status
) {
    public enum ActivityType {
        CALL,
        EMAIL,
        MEETING,
        TASK,
        NOTE
    }

    public enum ActivityStatus {
        PLANNED,
        COMPLETED,
        CANCELED
    }
}

@Service
public class CRMService {

    /**
     * Convert lead to opportunity
     */
    public Opportunity convertLead(String leadId) {
        Lead lead = leadRepository.findById(leadId).orElseThrow();

        // Create account if doesn't exist
        Account account = accountRepository.findByCompanyName(lead.companyName())
            .orElseGet(() -> {
                Account newAccount = new Account(
                    UUID.randomUUID().toString(),
                    lead.companyName(),
                    lead.industry(),
                    lead.employeeCount(),
                    null,
                    null,
                    Account.AccountStatus.PROSPECT,
                    LocalDateTime.now(),
                    new HashMap<>()
                );
                return accountRepository.save(newAccount);
            });

        // Create contact
        Contact contact = new Contact(
            UUID.randomUUID().toString(),
            account.accountId(),
            extractFirstName(lead.contactName()),
            extractLastName(lead.contactName()),
            lead.email(),
            lead.phone(),
            lead.jobTitle(),
            true,  // Primary contact
            LocalDateTime.now()
        );
        contactRepository.save(contact);

        // Create opportunity
        Opportunity opportunity = new Opportunity(
            UUID.randomUUID().toString(),
            leadId,
            account.companyName(),
            contact.fullName(),
            contact.email(),
            Opportunity.PipelineStage.SQL,
            0L,  // Will be set later
            0.20,  // Default SQL win probability
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(60),  // Default 60-day close
            getCurrentUserId(),
            new HashMap<>()
        );

        return opportunityRepository.save(opportunity);
    }

    /**
     * Log activity
     */
    public Activity logActivity(
        String opportunityId,
        String contactId,
        Activity.ActivityType type,
        String subject,
        String description
    ) {
        Activity activity = new Activity(
            UUID.randomUUID().toString(),
            opportunityId,
            contactId,
            type,
            subject,
            description,
            LocalDateTime.now(),
            getCurrentUserId(),
            Activity.ActivityStatus.COMPLETED
        );

        return activityRepository.save(activity);
    }

    /**
     * Get activity timeline for opportunity
     */
    public List<Activity> getActivityTimeline(String opportunityId) {
        return activityRepository
            .findByOpportunityIdOrderByActivityDateDesc(opportunityId);
    }

    /**
     * Schedule follow-up
     */
    public Activity scheduleFollowUp(
        String opportunityId,
        String contactId,
        LocalDateTime followUpDate,
        String notes
    ) {
        Activity activity = new Activity(
            UUID.randomUUID().toString(),
            opportunityId,
            contactId,
            Activity.ActivityType.TASK,
            "Follow-up call",
            notes,
            followUpDate,
            getCurrentUserId(),
            Activity.ActivityStatus.PLANNED
        );

        return activityRepository.save(activity);
    }

    private String extractFirstName(String fullName) {
        return fullName.split(" ")[0];
    }

    private String extractLastName(String fullName) {
        String[] parts = fullName.split(" ");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }

    private String getCurrentUserId() {
        // In real app: get from security context
        return "current-user-id";
    }
}
```

---

## Part 6: Sales Team Structure & Scaling

### Sales Roles

**SDR (Sales Development Representative):**
- **Focus:** Lead qualification and meeting setting
- **Activities:** Cold outreach, qualify inbound leads, book demos
- **Quota:** 20-40 qualified meetings/month
- **Comp:** $40-60K base + $20-30K variable

**AE (Account Executive):**
- **Focus:** Close deals
- **Activities:** Run demos, send proposals, negotiate contracts
- **Quota:** $500K-1M ARR/year
- **Comp:** $60-80K base + $40-80K variable

**CSM (Customer Success Manager):**
- **Focus:** Retention and expansion
- **Activities:** Onboarding, adoption, upsells, renewals
- **Quota:** 95%+ retention, 120%+ net revenue retention
- **Comp:** $60-80K base + $20-40K variable

**Sales Engineer (SE):**
- **Focus:** Technical pre-sales support
- **Activities:** Deep-dive demos, POCs, integrations
- **Quota:** Support deals worth $XM
- **Comp:** $80-120K base + $30-50K variable

### Org Chart Evolution

**Stage 1: Founder-led sales (0-10 customers)**
```
Founder → Prospect → Customer
```

**Stage 2: First sales hire (10-50 customers)**
```
Founder (closes) ← SDR (qualifies) ← Leads
```

**Stage 3: Sales team (50-200 customers)**
```
Head of Sales
  ├── SDR Team (2-3)
  ├── AE Team (2-4)
  └── CSM (1)
```

**Stage 4: Scaled sales (200+ customers)**
```
VP Sales
  ├── SDR Manager → SDRs (5-10)
  ├── Sales Manager → AEs (5-10)
  ├── CSM Manager → CSMs (3-5)
  └── Sales Ops (1-2)
```

### Compensation Structures

**Principles:**
- 50/50 to 60/40 base/variable split
- Variable tied to quota attainment
- Accelerators for over-achievement
- Clear, measurable metrics

**Example AE comp plan:**

| Quota Attainment | Commission Rate |
|------------------|-----------------|
| < 50% | 5% of revenue |
| 50-80% | 8% of revenue |
| 80-100% | 10% of revenue |
| 100-120% | 12% of revenue |
| > 120% | 15% of revenue |

**Annual quota:** $1M ARR
**Base:** $70K
**Target OTE:** $140K (assumes 100% attainment)

**At 100% quota ($1M closed):**
- Base: $70K
- Commission: $1M × 10% = $100K
- **Total: $170K**

**At 120% quota ($1.2M closed):**
- Base: $70K
- Commission: $1M × 10% + $200K × 12% = $100K + $24K = $124K
- **Total: $194K**

### Code Example: Quota & Commission Service

```java
@Service
public class SalesCompensationService {

    public record QuotaAttainment(
        String repId,
        String repName,
        long annualQuotaCents,
        long closedRevenueCents,
        double attainmentPercentage,
        String attainmentTier,
        long baseSalaryCents,
        long commissionCents,
        long totalCompCents
    ) {}

    /**
     * Calculate quota attainment and commission
     */
    public QuotaAttainment calculateCompensation(
        String repId,
        long annualQuotaCents,
        long baseSalaryCents,
        long closedRevenueCents
    ) {
        double attainment = closedRevenueCents / (double) annualQuotaCents;
        String tier = getAttainmentTier(attainment);
        long commission = calculateCommission(closedRevenueCents, attainment);
        long totalComp = baseSalaryCents + commission;

        SalesRep rep = salesRepRepository.findById(repId).orElseThrow();

        return new QuotaAttainment(
            repId,
            rep.name(),
            annualQuotaCents,
            closedRevenueCents,
            attainment,
            tier,
            baseSalaryCents,
            commission,
            totalComp
        );
    }

    private String getAttainmentTier(double attainment) {
        if (attainment >= 1.20) return "Exceptional";
        if (attainment >= 1.00) return "Target";
        if (attainment >= 0.80) return "Near Target";
        if (attainment >= 0.50) return "Below Target";
        return "Significant Gap";
    }

    private long calculateCommission(long revenue, double attainment) {
        // Tiered commission rates
        if (attainment >= 1.20) {
            return (long) (revenue * 0.15);  // 15% for exceptional
        } else if (attainment >= 1.00) {
            return (long) (revenue * 0.12);  // 12% for hitting quota
        } else if (attainment >= 0.80) {
            return (long) (revenue * 0.10);  // 10% for near target
        } else if (attainment >= 0.50) {
            return (long) (revenue * 0.08);  // 8% for below target
        } else {
            return (long) (revenue * 0.05);  // 5% for significant gap
        }
    }

    /**
     * Sales leaderboard
     */
    public record LeaderboardEntry(
        int rank,
        String repName,
        long closedRevenue,
        double quotaAttainment,
        int dealsWon
    ) {}

    public List<LeaderboardEntry> generateLeaderboard(
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<SalesRep> reps = salesRepRepository.findAll();
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (SalesRep rep : reps) {
            List<Opportunity> wonDeals = opportunityRepository
                .findByOwnerIdAndStatusAndCloseDateBetween(
                    rep.repId(),
                    Opportunity.PipelineStage.CLOSED_WON,
                    startDate.atStartOfDay(),
                    endDate.atTime(23, 59)
                );

            long totalRevenue = wonDeals.stream()
                .mapToLong(Opportunity::estimatedValueCents)
                .sum();

            double attainment = totalRevenue / (double) rep.annualQuotaCents();

            entries.add(new LeaderboardEntry(
                0,  // Will set rank after sorting
                rep.name(),
                totalRevenue,
                attainment,
                wonDeals.size()
            ));
        }

        // Sort by revenue desc
        entries.sort(Comparator.comparingLong(LeaderboardEntry::closedRevenue).reversed());

        // Assign ranks
        List<LeaderboardEntry> ranked = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            ranked.add(new LeaderboardEntry(
                i + 1,
                entry.repName(),
                entry.closedRevenue(),
                entry.quotaAttainment(),
                entry.dealsWon()
            ));
        }

        return ranked;
    }
}
```

---

## Part 7: Sales Metrics & Optimization

### Key Sales Metrics

**1. Pipeline Coverage**
```
Pipeline Coverage = Total Pipeline Value / Quarterly Quota
```

**Target:** 3-5x coverage

**Example:**
- Q1 quota: $300K
- Pipeline value: $1.2M
- Coverage: 4x ✅

**2. Win Rate**
```
Win Rate = Closed Won / (Closed Won + Closed Lost)
```

**Benchmarks:**
- Self-serve: N/A (automated)
- SMB: 20-30%
- Enterprise: 15-25%

**3. Average Deal Size**
```
ACV = Total ARR Closed / # of Deals
```

**Track over time:**
- Is ACV growing (moving upmarket)?
- Is ACV shrinking (moving downmarket)?

**4. Sales Cycle Length**
```
Sales Cycle = Days from Lead Created to Closed Won
```

**Benchmarks:**
- Self-serve: < 7 days
- SMB: 30-60 days
- Enterprise: 90-180 days

**5. Conversion Rates by Stage**
```
Lead → MQL → SQL → Demo → Proposal → Closed Won
30%    50%    60%   50%     40%       70%
```

**6. CAC Payback Period**
```
Payback = CAC / (Monthly Revenue × Gross Margin)
```

**Target:** < 12 months

**7. Quota Attainment**
```
% of reps hitting 100%+ quota
```

**Healthy:** 60-70% of reps hit quota

### Sales Forecasting

**Categories:**

- **Commit:** 90%+ likely to close this quarter
- **Best Case:** 70-90% likely
- **Pipeline:** 50-70% likely
- **Omitted:** < 50% likely

**Weighted forecast:**
```
Forecast = (Commit × 1.0) + (Best Case × 0.8) + (Pipeline × 0.5)
```

**Code Example: Sales Forecasting Service**

```java
@Service
public class SalesForecastingService {

    public enum ForecastCategory {
        COMMIT(1.0),
        BEST_CASE(0.8),
        PIPELINE(0.5),
        OMITTED(0.0);

        private final double weight;

        ForecastCategory(double weight) {
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }
    }

    public record ForecastBreakdown(
        long commitValue,
        long bestCaseValue,
        long pipelineValue,
        long weightedForecast,
        long quarterlyQuota,
        double quotaCoverage,
        String forecast Health
    ) {}

    /**
     * Generate quarterly forecast
     */
    public ForecastBreakdown generateForecast(
        LocalDate quarterStart,
        LocalDate quarterEnd,
        long quarterlyQuota
    ) {
        List<Opportunity> opps = opportunityRepository
            .findByExpectedCloseDateBetween(
                quarterStart.atStartOfDay(),
                quarterEnd.atTime(23, 59)
            );

        long commit = 0;
        long bestCase = 0;
        long pipeline = 0;

        for (Opportunity opp : opps) {
            ForecastCategory category = categorizeForecast(opp);
            long value = opp.estimatedValueCents();

            switch (category) {
                case COMMIT -> commit += value;
                case BEST_CASE -> bestCase += value;
                case PIPELINE -> pipeline += value;
                case OMITTED -> {}  // Don't count
            }
        }

        long weighted = (long) (
            commit * ForecastCategory.COMMIT.getWeight() +
            bestCase * ForecastCategory.BEST_CASE.getWeight() +
            pipeline * ForecastCategory.PIPELINE.getWeight()
        );

        double coverage = weighted / (double) quarterlyQuota;
        String health = getForecastHealth(coverage);

        return new ForecastBreakdown(
            commit,
            bestCase,
            pipeline,
            weighted,
            quarterlyQuota,
            coverage,
            health
        );
    }

    private ForecastCategory categorizeForecast(Opportunity opp) {
        double winProb = opp.winProbability();

        if (winProb >= 0.90) return ForecastCategory.COMMIT;
        if (winProb >= 0.70) return ForecastCategory.BEST_CASE;
        if (winProb >= 0.50) return ForecastCategory.PIPELINE;
        return ForecastCategory.OMITTED;
    }

    private String getForecastHealth(double coverage) {
        if (coverage >= 1.0) return "On track to hit quota";
        if (coverage >= 0.80) return "Slightly behind - need to close more deals";
        if (coverage >= 0.60) return "Significant gap - increase pipeline urgently";
        return "Critical gap - quota at risk";
    }
}
```

---

## Part 8: Enterprise Sales for Bibby

Let's apply everything to **Bibby Library Edition** targeting universities and corporations.

### Bibby Enterprise ICP

**Target Account Profile:**
- **Segment:** University libraries, corporate learning departments
- **Size:** 5,000+ students or 1,000+ employees
- **Budget:** $500K+ annual technology budget
- **Pain:** Managing large collections (100K+ books), outdated systems
- **Decision-makers:** Head Librarian, CIO, VP of Learning & Development

### Sales Process for Bibby Enterprise

**Stage 1: Prospecting (Week 1-2)**
- Research top 200 universities
- Identify decision-makers on LinkedIn
- Cold email/LinkedIn outreach
- Attend library conferences (ALA, EDUCAUSE)

**Stage 2: Discovery Call (Week 3)**
- Understand current system (Koha, Evergreen, Symphony)
- Pain points: outdated UX, slow search, poor mobile experience
- Quantify: hours spent on manual processes, student satisfaction scores
- Budget and timeline

**Stage 3: Demo (Week 4-5)**
- Customized demo with sample university data
- Show: modern search, mobile app, analytics dashboard
- Compare to current system side-by-side
- Involve multiple stakeholders (librarians, IT, students)

**Stage 4: Proof of Concept (Week 6-10)**
- 30-day pilot with subset of catalog (10K books)
- Metrics: search speed, user satisfaction, staff time saved
- Success criteria: 50% faster cataloging, 90% user satisfaction

**Stage 5: Proposal (Week 11-12)**
- Pricing: $50K implementation + $20K/year subscription
- ROI: Save 20 staff hours/week = $50K/year savings
- Contract: 3-year commitment with annual price lock

**Stage 6: Negotiation (Week 13-16)**
- Legal review (data privacy, SLAs)
- Procurement process
- Reference calls with existing customers
- Executive sign-off

**Stage 7: Close (Week 17-18)**
- Contract signature
- Kickoff call with implementation team
- Payment terms: 50% upfront, 50% at go-live

**Total sales cycle:** 4-5 months

### Code Example: Enterprise Sales Pipeline for Bibby

```java
@Service
public class BibbyEnterpriseSalesService {

    public record UniversityProspect(
        String universityName,
        int studentCount,
        int catalogSize,
        String currentSystem,
        String decisionMaker,
        String email,
        int priorityScore
    ) {}

    /**
     * Generate prospect list
     */
    public List<UniversityProspect> generateProspectList() {
        // In reality: scrape from databases, enrich with data
        return List.of(
            new UniversityProspect(
                "Stanford University",
                17000,
                250000,
                "Symphony ILS",
                "Jane Smith, Head Librarian",
                "jsmith@stanford.edu",
                95
            ),
            new UniversityProspect(
                "MIT",
                11500,
                180000,
                "Koha",
                "John Doe, Director of Libraries",
                "jdoe@mit.edu",
                92
            )
            // ... more prospects
        );
    }

    /**
     * Qualify enterprise lead
     */
    public boolean qualifyEnterpriseLead(UniversityProspect prospect) {
        // Must meet minimum criteria
        return prospect.studentCount() >= 5000 &&
               prospect.catalogSize() >= 50000 &&
               !prospect.currentSystem().equals("None");
    }

    /**
     * Generate ROI calculation for proposal
     */
    public record ROICalculation(
        long currentAnnualCostCents,
        long bibbyAnnualCostCents,
        long annualSavingsCents,
        double paybackMonths,
        String recommendation
    ) {}

    public ROICalculation calculateROI(
        int catalogSize,
        int staffCount,
        double avgHourlySalary
    ) {
        // Current cost assumptions
        long staffHoursPerWeek = 40L * staffCount;
        long manualHoursPerWeek = (long) (staffHoursPerWeek * 0.30);  // 30% on manual work
        long annualManualHours = manualHoursPerWeek * 52;
        long currentAnnualCost = (long) (annualManualHours * avgHourlySalary * 100);  // to cents

        // Bibby cost
        long bibbyImplementation = 5_000_000L;  // $50K
        long bibbyAnnual = 2_000_000L;  // $20K/year
        long bibbyTotalYear1 = bibbyImplementation + bibbyAnnual;

        // Savings (assume 50% reduction in manual work)
        long savedHours = annualManualHours / 2;
        long annualSavings = (long) (savedHours * avgHourlySalary * 100);

        // Payback period
        double paybackMonths = bibbyImplementation / (double) (annualSavings / 12);

        String recommendation = annualSavings > bibbyTotalYear1 ?
            "Strong ROI - positive return in year 1" :
            "ROI achieved in " + String.format("%.1f", paybackMonths) + " months";

        return new ROICalculation(
            currentAnnualCost,
            bibbyAnnual,
            annualSavings,
            paybackMonths,
            recommendation
        );
    }
}
```

---

## Practical Assignments

### Assignment 1: Build Sales Pipeline

Create a complete sales pipeline with:
- 7 stages (Lead → Closed Won/Lost)
- Conversion rates for each stage
- Average days in each stage
- Overall conversion rate calculation

Test with sample opportunities moving through stages.

### Assignment 2: Implement Lead Scoring

Build lead scoring system with:
- Firmographic scoring (company size, industry, job title)
- Behavioral scoring (website activity, email engagement, product trial)
- MQL threshold (score >= 70)
- Routing logic based on score

Test with various lead profiles.

### Assignment 3: Create Sales Playbook

Document a complete discovery call playbook with:
- Call structure (agenda, timing)
- Question bank (situation, problem, implication, need-payoff)
- BANT qualification checklist
- Next step options

### Assignment 4: Design Compensation Plan

Create compensation structure for:
- SDR (meeting-based quota)
- AE (revenue-based quota)
- Include base/variable split, commission tiers, accelerators

Calculate example payouts at 80%, 100%, 120% attainment.

### Assignment 5: Build CRM System

Implement basic CRM with:
- Lead, Contact, Account, Opportunity entities
- Lead → Opportunity conversion
- Activity logging
- Pipeline metrics dashboard

Use Spring Boot + MongoDB.

---

## Reflection Questions

1. **Sales vs Product:** Should every B2B SaaS have a sales team, or can product-led growth work for everyone?

2. **Founder sales:** How long should founders do sales before hiring their first AE?

3. **Win rate:** If your win rate is 50%, is that good or bad? What does it tell you?

4. **Sales cycle:** What's better: 50 deals at 30 days each, or 10 deals at 180 days each? Why?

5. **Compensation:** Should sales reps be paid on bookings (upfront) or recognized revenue (over time)?

6. **CRM complexity:** At what point does a simple spreadsheet stop working and you need a real CRM?

7. **Enterprise sales:** Is enterprise sales worth it for a small startup, or should you focus on volume?

8. **Quota attainment:** If only 40% of reps hit quota, is that a rep problem or a quota problem?

---

## Key Takeaways

### Sales is a System

- Inputs: Leads
- Process: Pipeline stages with conversion rates
- Outputs: Closed deals
- Optimize at every stage

### Three Sales Motions

- **Self-serve:** PLG, high volume, low touch
- **SMB:** Inside sales, mid-market, video calls
- **Enterprise:** Field sales, strategic accounts, long cycles

### Pipeline Fundamentals

- Clear stages with entry/exit criteria
- Track conversion rates between stages
- Measure velocity (deals × value × win rate / cycle length)
- Identify and fix bottlenecks

### Sales Process

- **Discovery:** Understand before pitching
- **Demo:** Show outcomes, not features
- **Proposal:** Justify with ROI
- **Negotiation:** Protect value, find win-win
- **Close:** Ask for the business

### Sales Team Scaling

- Start with founder-led sales
- First hire: SDR to qualify leads
- Second hire: AE to close deals
- Third hire: CSM to retain and expand
- Build playbooks before scaling

### Sales Metrics

- Pipeline coverage (3-5x quota)
- Win rate (20-30% for SMB, 15-25% for enterprise)
- Sales cycle length (track and reduce)
- Quota attainment (60-70% hit quota)

---

## Looking Ahead: Week 42

Next week: **Customer Success & Account Management**

You'll learn:
- Customer onboarding and activation
- Health scoring and churn prediction
- Expansion and upsell strategies
- Quarterly business reviews (QBRs)
- Customer success team structure
- Retention and NRR metrics

Plus: Building a customer success program for Bibby.

---

## From Your Engineering Manager

"Sales gets a bad rap among engineers. But sales is just a system for creating customer value and capturing a fair share of it.

The best salespeople aren't smooth-talkers or manipulators. They're problem-solvers who listen deeply, understand context, and match solutions to needs.

As a technical founder or engineering leader, you need to understand sales because:

1. **You'll do sales first:** Someone has to close those first 10 customers
2. **You'll hire sales:** You need to know what good looks like
3. **You'll support sales:** Product needs to enable sales success
4. **You'll partner with sales:** Eng + Sales alignment = growth

Learn the system. Build the playbooks. Measure the metrics. Optimize relentlessly.

Sales isn't magic. It's process, data, and continuous improvement.

Just like engineering."

—Your Engineering Manager

---

## Progress Tracker

**Week 41 of 52 complete (79% complete)**

**Semester 4 (Weeks 40-52): Execution, Revenue & Scale**
- ✅ Week 40: Revenue Architecture & Billing Systems
- ✅ Week 41: Sales Processes & Pipeline Management ← **You are here**
- ⬜ Week 42: Customer Success & Account Management
- ⬜ Week 43: Team Building & Hiring
- ⬜ Week 44: Engineering Management & Productivity
- ⬜ Week 45: Organizational Design & Culture
- ⬜ Week 46: Fundraising & Investor Relations
- ⬜ Week 47: Financial Planning & Unit Economics
- ⬜ Week 48: Legal, Compliance & Risk Management
- ⬜ Week 49: Data Analytics & Business Intelligence
- ⬜ Week 50: Experimentation & A/B Testing
- ⬜ Week 51: AI/ML in Product & Operations
- ⬜ Week 52: Final Capstone Project

---

**End of Week 41**
