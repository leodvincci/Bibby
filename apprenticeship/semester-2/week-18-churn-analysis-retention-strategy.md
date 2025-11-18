# Week 18: Churn Analysis & Retention Strategy

**Mentor Voice: Engineering Manager**

> "Acquiring a new customer is 5-7× more expensive than retaining an existing one. Yet most companies spend 80% of their resources on acquisition and 20% on retention. The best companies flip this ratio." - Fred Reichheld, loyalty economics pioneer
>
> Last week you optimized acquisition (lower CAC). This week, you'll optimize retention (lower churn). Churn is the silent killer of SaaS businesses—a small leak that compounds into a flood. A 5% churn rate means you lose half your customers every 14 months. But churn isn't inevitable. The best SaaS companies have retention rates above 95%, not because they have better products, but because they architect retention into every customer touchpoint. This week, you'll learn to measure, predict, and prevent churn before it happens. Because growth minus churn equals actual growth.

---

## Learning Objectives

By the end of this week, you will:

1. Understand different types of churn and their causes
2. Build cohort-based retention analysis
3. Predict churn using early warning signals
4. Design retention interventions that work
5. Build habit-forming product experiences
6. Create reactivation campaigns for churned users
7. Calculate retention ROI and opportunity cost
8. Implement churn prediction models in Bibby

---

## Part 1: Understanding Churn

### The Churn Equation

**From Week 14, but worth revisiting:**

```
Revenue Churn Rate = Churned MRR / Starting MRR × 100%

Example:
Starting MRR: $10,000
Churned MRR: $400

Monthly Churn = 4%
Annual Churn = 1 - (1 - 0.04)^12 = 38.6%

Translation: You lose 39% of customers per year
```

**The compounding problem:**

```
Year 1: Start with 100 customers
├─ 4% monthly churn = 38.6% annual churn
└─ End with 61 customers

Year 2: Start with 61 customers
└─ End with 37 customers

Year 3: Start with 37 customers
└─ End with 23 customers

In 3 years, you've lost 77% of your customer base
```

**Contrast with low churn:**

```
Year 1: Start with 100 customers
├─ 1% monthly churn = 11.4% annual churn
└─ End with 89 customers

Year 2: Start with 89 customers
└─ End with 79 customers

Year 3: Start with 79 customers
└─ End with 70 customers

In 3 years, you've lost only 30% of your customer base
```

**Key insight:** Reducing churn from 4% to 1% is worth **2.5× more customers retained** after 3 years.

### Types of Churn

**1. Voluntary Churn** (Customer chooses to leave)

```
Reasons:
├─ Product doesn't solve problem (poor fit)
├─ Product too complex (bad UX)
├─ Price too high (value mismatch)
├─ Switched to competitor (better alternative)
├─ Changed needs (no longer relevant)
└─ Poor support experience (service failure)

Bibby example:
"We switched to Alexandria because it integrates with our student system"
└─ Actionable: Build integration
```

**2. Involuntary Churn** (Customer didn't choose to leave)

```
Reasons:
├─ Credit card expired (payment failure)
├─ Budget cuts (external factor)
├─ Business closed (customer went out of business)
├─ Staff turnover (champion left, successor doesn't know product)
└─ Technical issues (couldn't log in, gave up)

Bibby example:
"Credit card expired and we forgot to update it"
└─ Actionable: Automated dunning emails
```

**3. Good Churn vs. Bad Churn**

```
Good churn (don't fight it):
├─ Free users who never intended to pay
├─ Customers who outgrew you (startup → enterprise)
├─ Poor-fit customers (reduce support burden)
└─ Non-responsive zombie accounts

Bad churn (fight hard):
├─ High-value customers (Enterprise tier)
├─ Happy customers hit by external factors (budget cuts)
├─ Power users who could become advocates
└─ Customers you could have saved with intervention
```

### Measuring Churn Precisely

```java
// src/main/java/com/penrose/bibby/retention/ChurnAnalyzer.java

@Service
public class ChurnAnalyzer {

    public enum ChurnType {
        VOLUNTARY,
        INVOLUNTARY,
        GOOD_CHURN,
        BAD_CHURN
    }

    public enum ChurnReason {
        // Voluntary
        SWITCHED_TO_COMPETITOR,
        TOO_EXPENSIVE,
        TOO_COMPLEX,
        POOR_SUPPORT,
        FEATURE_GAP,
        CHANGED_NEEDS,

        // Involuntary
        PAYMENT_FAILURE,
        BUSINESS_CLOSED,
        BUDGET_CUTS,
        STAFF_TURNOVER,
        TECHNICAL_ISSUES,

        // Other
        UNKNOWN
    }

    public record ChurnEvent(
        Customer customer,
        LocalDate churnDate,
        ChurnType type,
        ChurnReason reason,
        String notes,
        BigDecimal mrrLost,
        boolean saveable
    ) {}

    /**
     * Calculate detailed churn metrics for a month
     */
    public ChurnReport analyzeMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // Get all churn events this month
        List<ChurnEvent> events = churnEventRepository
            .findByChurnDateBetween(start, end);

        // Categorize by type
        Map<ChurnType, List<ChurnEvent>> byType = events.stream()
            .collect(Collectors.groupingBy(ChurnEvent::type));

        // Categorize by reason
        Map<ChurnReason, Long> byReason = events.stream()
            .collect(Collectors.groupingBy(
                ChurnEvent::reason,
                Collectors.counting()
            ));

        // Calculate financial impact
        BigDecimal totalMRRLost = events.stream()
            .map(ChurnEvent::mrrLost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Identify saveable churn
        long saveableCount = events.stream()
            .filter(ChurnEvent::saveable)
            .count();

        BigDecimal saveableMRR = events.stream()
            .filter(ChurnEvent::saveable)
            .map(ChurnEvent::mrrLost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ChurnReport(
            month,
            events.size(),
            byType,
            byReason,
            totalMRRLost,
            saveableCount,
            saveableMRR
        );
    }

    /**
     * Determine if churn was saveable
     */
    public boolean isSaveable(ChurnEvent event) {
        return switch (event.reason()) {
            case PAYMENT_FAILURE, STAFF_TURNOVER, TECHNICAL_ISSUES -> true; // Fixable
            case TOO_EXPENSIVE, FEATURE_GAP, POOR_SUPPORT -> true; // Could have intervened
            case BUSINESS_CLOSED, SWITCHED_TO_COMPETITOR -> false; // Hard to save
            case CHANGED_NEEDS -> false; // Natural lifecycle
            default -> false;
        };
    }

    /**
     * Display churn analysis
     */
    public void printChurnReport(YearMonth month) {
        ChurnReport report = analyzeMonth(month);

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📉 Churn Analysis - " + month);
        System.out.println("═══════════════════════════════════════════════\n");

        System.out.printf("Total Churned: %d customers, $%,.2f MRR\n\n",
            report.totalCount(),
            report.totalMRRLost()
        );

        System.out.println("By Type:");
        report.byType().forEach((type, events) ->
            System.out.printf("  %s: %d (%.1f%%)\n",
                type,
                events.size(),
                100.0 * events.size() / report.totalCount()
            )
        );

        System.out.println("\nBy Reason:");
        report.byReason().entrySet().stream()
            .sorted(Map.Entry.<ChurnReason, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry ->
                System.out.printf("  %s: %d\n", entry.getKey(), entry.getValue())
            );

        System.out.println("\n⚠️  Saveable Churn:");
        System.out.printf("  %d customers ($%,.2f MRR) could have been saved\n",
            report.saveableCount(),
            report.saveableMRR()
        );
        System.out.printf("  That's %.1f%% of total churn!\n",
            100.0 * report.saveableCount() / report.totalCount()
        );
    }
}
```

---

## Part 2: Cohort-Based Retention Analysis

### The Retention Curve

**Question:** What % of customers from each cohort are still active?

```
Jan 2024 Cohort (100 customers):
├─ Month 0 (Jan): 100 customers (100% retention)
├─ Month 1 (Feb): 98 customers (98% retention, 2% churn)
├─ Month 2 (Mar): 94 customers (94% retention, 6% cumulative churn)
├─ Month 3 (Apr): 90 customers (90% retention, 10% cumulative churn)
├─ Month 6 (Jul): 82 customers (82% retention)
└─ Month 12 (Jan 2025): 73 customers (73% retention, 27% annual churn)
```

**The retention curve shape matters:**

```
Good retention curve (flattens):
100% → 95% → 92% → 90% → 89% → 88% (flattening → stable)

Bad retention curve (linear):
100% → 95% → 90% → 85% → 80% → 75% (linear → death spiral)
```

### Implementation

```java
@Service
public class RetentionAnalyzer {

    public record CohortRetention(
        YearMonth cohortMonth,
        int initialSize,
        Map<Integer, RetentionSnapshot> monthlySnapshots
    ) {}

    public record RetentionSnapshot(
        int monthNumber,
        int activeCustomers,
        double retentionRate,
        BigDecimal activeMRR,
        double revenueRetention
    ) {}

    /**
     * Calculate retention curve for a cohort
     */
    public CohortRetention analyzeCohort(YearMonth cohortMonth) {
        List<Customer> cohort = customerRepository
            .findByAcquisitionMonth(cohortMonth);

        int initialSize = cohort.size();
        BigDecimal initialMRR = cohort.stream()
            .map(c -> mrrCalculator.getMRR(c, cohortMonth))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Integer, RetentionSnapshot> snapshots = new HashMap<>();

        // Track cohort for 24 months
        for (int month = 0; month <= 24; month++) {
            YearMonth analysisMonth = cohortMonth.plusMonths(month);

            // Count still-active customers
            long activeCount = cohort.stream()
                .filter(c -> isActive(c, analysisMonth))
                .count();

            double retentionRate = (double) activeCount / initialSize;

            // Calculate current MRR from this cohort
            BigDecimal activeMRR = cohort.stream()
                .filter(c -> isActive(c, analysisMonth))
                .map(c -> mrrCalculator.getMRR(c, analysisMonth))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            double revenueRetention = initialMRR.compareTo(BigDecimal.ZERO) > 0
                ? activeMRR.divide(initialMRR, 4, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

            snapshots.put(month, new RetentionSnapshot(
                month,
                (int) activeCount,
                retentionRate,
                activeMRR,
                revenueRetention
            ));
        }

        return new CohortRetention(
            cohortMonth,
            initialSize,
            snapshots
        );
    }

    /**
     * Display retention curves for multiple cohorts
     */
    public void printRetentionCurves(List<YearMonth> cohorts) {
        System.out.println("📊 Retention Curves (Customer %)\n");
        System.out.printf("%-12s | M0    M1    M2    M3    M6    M12   M24\n", "Cohort");
        System.out.println("─".repeat(60));

        for (YearMonth cohort : cohorts) {
            CohortRetention analysis = analyzeCohort(cohort);
            System.out.printf("%-12s | ", cohort);

            for (int month : List.of(0, 1, 2, 3, 6, 12, 24)) {
                if (analysis.monthlySnapshots().containsKey(month)) {
                    double retention = analysis.monthlySnapshots().get(month)
                        .retentionRate() * 100;
                    System.out.printf("%4.0f%% ", retention);
                } else {
                    System.out.print("  -   ");
                }
            }
            System.out.println();
        }

        System.out.println("\n📊 Retention Curves (Revenue %)\n");
        System.out.printf("%-12s | M0    M1    M2    M3    M6    M12   M24\n", "Cohort");
        System.out.println("─".repeat(60));

        for (YearMonth cohort : cohorts) {
            CohortRetention analysis = analyzeCohort(cohort);
            System.out.printf("%-12s | ", cohort);

            for (int month : List.of(0, 1, 2, 3, 6, 12, 24)) {
                if (analysis.monthlySnapshots().containsKey(month)) {
                    double retention = analysis.monthlySnapshots().get(month)
                        .revenueRetention() * 100;
                    System.out.printf("%4.0f%% ", retention);
                } else {
                    System.out.print("  -   ");
                }
            }
            System.out.println();
        }
    }
}
```

**Example output:**

```
📊 Retention Curves (Customer %)

Cohort       | M0    M1    M2    M3    M6    M12   M24
────────────────────────────────────────────────────────
2024-01      | 100%  98%   96%   94%   90%   78%   68%
2024-02      | 100%  97%   95%   92%   87%   74%   65%
2024-03      | 100%  95%   91%   87%   79%   65%   55%  ← Problem cohort
2024-04      | 100%  96%   93%   90%   85%   72%    -

📊 Retention Curves (Revenue %)

Cohort       | M0    M1    M2    M3    M6    M12   M24
────────────────────────────────────────────────────────
2024-01      | 100%  102%  105%  107%  108%  110%  115%  ← Excellent!
2024-02      | 100%  101%  103%  104%  105%  106%  108%
2024-03      | 100%  98%   95%   93%   88%   82%   76%   ← Problem cohort
2024-04      | 100%  99%   98%   97%   95%   93%    -
```

**Analysis:**

- March 2024 cohort has worse retention (65% vs 74-78% for other cohorts)
- Revenue retention is even worse (82% vs 106-115%)
- Something changed in March → investigate product changes, marketing channels, customer segment

---

## Part 3: Predicting Churn Before It Happens

### Early Warning Signals

**Churn doesn't happen suddenly—there are always leading indicators.**

**Usage decline:**

```
Healthy customer:
├─ Month 1: 50 logins
├─ Month 2: 55 logins
├─ Month 3: 60 logins (engaged!)

At-risk customer:
├─ Month 1: 50 logins
├─ Month 2: 30 logins (↓40%)
├─ Month 3: 10 logins (↓67%) ← Churn warning!
```

**Support tickets:**

```
Churn predictor:
├─ 3+ support tickets in 30 days → 60% churn rate
├─ Unresolved ticket > 7 days → 45% churn rate
└─ Multiple issues with same feature → 55% churn rate
```

**Engagement signals:**

```
At-risk behaviors:
├─ Haven't logged in 14+ days
├─ Haven't used core feature in 30 days
├─ Team size decreased (removed users)
├─ Downgraded tier
├─ Disabled email notifications
└─ Searched for "cancel" or "export data"
```

### Churn Prediction Model

```java
@Service
public class ChurnPredictionService {

    public record ChurnScore(
        Customer customer,
        double churnProbability,      // 0.0 to 1.0
        ChurnRisk riskLevel,
        List<RiskFactor> factors,
        LocalDateTime calculatedAt
    ) {}

    public enum ChurnRisk {
        LOW,      // < 20% probability
        MEDIUM,   // 20-50%
        HIGH,     // 50-75%
        CRITICAL  // > 75%
    }

    public record RiskFactor(
        String factor,
        int points,
        String description
    ) {}

    /**
     * Calculate churn probability for a customer
     */
    public ChurnScore calculateChurnScore(Customer customer) {
        List<RiskFactor> factors = new ArrayList<>();
        int totalPoints = 0;

        // Factor 1: Usage decline
        UsageStats current = usageService.getStats(customer, Period.ofDays(30));
        UsageStats previous = usageService.getStats(customer, Period.ofDays(30), 30);

        if (current.loginCount() < previous.loginCount() * 0.5) {
            int points = 25;
            factors.add(new RiskFactor(
                "Usage decline",
                points,
                String.format("Logins dropped %d%% in last 30 days",
                    (int)((1 - current.loginCount() / (double)previous.loginCount()) * 100))
            ));
            totalPoints += points;
        }

        // Factor 2: Days since last login
        long daysSinceLogin = ChronoUnit.DAYS.between(
            customer.getLastLoginAt(),
            LocalDateTime.now()
        );

        if (daysSinceLogin > 14) {
            int points = (int)Math.min(30, daysSinceLogin * 2);
            factors.add(new RiskFactor(
                "Inactive",
                points,
                daysSinceLogin + " days since last login"
            ));
            totalPoints += points;
        }

        // Factor 3: Support issues
        long recentTickets = supportTicketRepository
            .countByCustomerAndCreatedAfter(
                customer,
                LocalDateTime.now().minusDays(30)
            );

        if (recentTickets >= 3) {
            int points = (int)(recentTickets * 5);
            factors.add(new RiskFactor(
                "Support issues",
                points,
                recentTickets + " tickets in last 30 days"
            ));
            totalPoints += points;
        }

        // Factor 4: Unresolved issues
        long unresolvedTickets = supportTicketRepository
            .countByCustomerAndStatus(customer, TicketStatus.OPEN);

        if (unresolvedTickets > 0) {
            long oldestTicketDays = supportTicketRepository
                .findOldestOpenTicket(customer)
                .map(ticket -> ChronoUnit.DAYS.between(
                    ticket.getCreatedAt(),
                    LocalDateTime.now()
                ))
                .orElse(0L);

            if (oldestTicketDays > 7) {
                int points = 20;
                factors.add(new RiskFactor(
                    "Unresolved issues",
                    points,
                    "Ticket open for " + oldestTicketDays + " days"
                ));
                totalPoints += points;
            }
        }

        // Factor 5: Feature usage
        boolean usesCoreFe ature = current.catalogedBooks() > 0 ||
                                   current.checkouts() > 0;

        if (!usesCoreFe ature) {
            int points = 30;
            factors.add(new RiskFactor(
                "Not using core features",
                points,
                "No cataloging or checkouts in 30 days"
            ));
            totalPoints += points;
        }

        // Factor 6: Downgrade or team shrinkage
        if (customer.wasDowngradedRecently()) {
            int points = 15;
            factors.add(new RiskFactor(
                "Downgraded tier",
                points,
                "Downgraded from " + customer.getPreviousTier()
            ));
            totalPoints += points;
        }

        // Factor 7: Payment issues
        if (customer.hasPaymentFailures()) {
            int points = 25;
            factors.add(new RiskFactor(
                "Payment failures",
                points,
                customer.getPaymentFailureCount() + " failed payments"
            ));
            totalPoints += points;
        }

        // Convert points to probability (max 100 points = 100% churn risk)
        double probability = Math.min(1.0, totalPoints / 100.0);

        ChurnRisk riskLevel = getRiskLevel(probability);

        return new ChurnScore(
            customer,
            probability,
            riskLevel,
            factors,
            LocalDateTime.now()
        );
    }

    private ChurnRisk getRiskLevel(double probability) {
        if (probability >= 0.75) return ChurnRisk.CRITICAL;
        if (probability >= 0.50) return ChurnRisk.HIGH;
        if (probability >= 0.20) return ChurnRisk.MEDIUM;
        return ChurnRisk.LOW;
    }

    /**
     * Identify all at-risk customers
     */
    @Scheduled(cron = "0 9 * * 1") // Every Monday at 9am
    public void identifyAtRiskCustomers() {
        List<Customer> activeCustomers = customerRepository.findActive();

        List<ChurnScore> atRisk = activeCustomers.stream()
            .map(this::calculateChurnScore)
            .filter(score -> score.riskLevel() != ChurnRisk.LOW)
            .sorted(Comparator.comparing(ChurnScore::churnProbability).reversed())
            .toList();

        logger.info("Identified {} at-risk customers", atRisk.size());

        // Alert customer success team
        for (ChurnScore score : atRisk) {
            if (score.riskLevel() == ChurnRisk.CRITICAL) {
                alertService.sendCriticalChurnAlert(score);
                retentionService.initiateIntervention(score);
            }
        }

        // Save for tracking
        atRisk.forEach(score ->
            churnScoreRepository.save(score)
        );
    }
}
```

---

## Part 4: Retention Interventions

### The Intervention Ladder

**When churn risk is detected, intervene based on severity:**

```
Risk Level → Intervention

LOW (< 20%):
└─ Automated email: "Tips to get more value from Bibby"

MEDIUM (20-50%):
└─ Personalized email: "We noticed you haven't logged in. Need help?"
└─ In-app message: "3 features you haven't tried yet"

HIGH (50-75%):
└─ Customer success outreach: Phone call or video chat
└─ Offer: Free training session or setup help
└─ Incentive: Credit for next month

CRITICAL (> 75%):
└─ Executive outreach: Founder or VP reaches out personally
└─ Custom solution: Dedicated onboarding, feature customization
└─ Win-back offer: 3 months free if they stay
```

### Implementation

```java
@Service
public class RetentionInterventionService {

    /**
     * Initiate intervention based on churn score
     */
    public void initiateIntervention(ChurnScore score) {
        Intervention intervention = switch (score.riskLevel()) {
            case LOW -> createLowRiskIntervention(score);
            case MEDIUM -> createMediumRiskIntervention(score);
            case HIGH -> createHighRiskIntervention(score);
            case CRITICAL -> createCriticalRiskIntervention(score);
        };

        interventionRepository.save(intervention);
        executeIntervention(intervention);
    }

    private Intervention createLowRiskIntervention(ChurnScore score) {
        return Intervention.builder()
            .customer(score.customer())
            .riskLevel(score.riskLevel())
            .type(InterventionType.AUTOMATED_EMAIL)
            .template("tips-for-success")
            .parameters(Map.of(
                "features", suggestUnusedFeatures(score.customer()),
                "tips", generatePersonalizedTips(score.customer())
            ))
            .build();
    }

    private Intervention createMediumRiskIntervention(ChurnScore score) {
        return Intervention.builder()
            .customer(score.customer())
            .riskLevel(score.riskLevel())
            .type(InterventionType.PERSONALIZED_EMAIL)
            .template("check-in")
            .assignedTo(customerSuccessService.assignCSM(score.customer()))
            .parameters(Map.of(
                "topRiskFactor", score.factors().get(0).factor(),
                "unusedFeatures", suggestUnusedFeatures(score.customer())
            ))
            .build();
    }

    private Intervention createHighRiskIntervention(ChurnScore score) {
        return Intervention.builder()
            .customer(score.customer())
            .riskLevel(score.riskLevel())
            .type(InterventionType.PERSONAL_OUTREACH)
            .assignedTo(customerSuccessService.assignCSM(score.customer()))
            .dueDate(LocalDate.now().plusDays(2)) // Urgent
            .parameters(Map.of(
                "offer", "Free training session",
                "incentive", "50% off next month"
            ))
            .build();
    }

    private Intervention createCriticalRiskIntervention(ChurnScore score) {
        // Escalate to executive team
        User executive = score.customer().getAccountValue() > 10000
            ? userService.findByRole(Role.CEO)
            : userService.findByRole(Role.VP_CUSTOMER_SUCCESS);

        return Intervention.builder()
            .customer(score.customer())
            .riskLevel(score.riskLevel())
            .type(InterventionType.EXECUTIVE_OUTREACH)
            .assignedTo(executive)
            .dueDate(LocalDate.now().plusDays(1)) // Very urgent
            .parameters(Map.of(
                "offer", "Custom onboarding + 3 months free",
                "escalation", "Founder will call personally"
            ))
            .build();
    }

    /**
     * Execute the intervention
     */
    private void executeIntervention(Intervention intervention) {
        switch (intervention.type()) {
            case AUTOMATED_EMAIL -> {
                emailService.send(
                    intervention.customer().getEmail(),
                    intervention.template(),
                    intervention.parameters()
                );
            }

            case PERSONALIZED_EMAIL -> {
                emailService.send(
                    intervention.customer().getEmail(),
                    intervention.template(),
                    enrichWithPersonalization(intervention)
                );

                // Create task for CSM to follow up
                taskService.create(Task.builder()
                    .assignee(intervention.assignedTo())
                    .title("Follow up with " + intervention.customer().getName())
                    .description("Churn risk: " + intervention.riskLevel())
                    .dueDate(intervention.dueDate())
                    .build()
                );
            }

            case PERSONAL_OUTREACH -> {
                // Create high-priority task
                taskService.create(Task.builder()
                    .assignee(intervention.assignedTo())
                    .title("[URGENT] Call " + intervention.customer().getName())
                    .description(String.format(
                        "Critical churn risk (%.0f%%)\n" +
                        "Top issues:\n%s\n\n" +
                        "Offer: %s",
                        intervention.getChurnScore().churnProbability() * 100,
                        formatRiskFactors(intervention.getChurnScore().factors()),
                        intervention.parameters().get("offer")
                    ))
                    .priority(Priority.CRITICAL)
                    .dueDate(intervention.dueDate())
                    .build()
                );

                // Send calendar invite
                calendarService.scheduleCall(
                    intervention.assignedTo(),
                    intervention.customer(),
                    "Bibby Check-in Call"
                );
            }

            case EXECUTIVE_OUTREACH -> {
                // Notify executive immediately
                slackService.send(
                    intervention.assignedTo().getSlackId(),
                    String.format(
                        "🚨 CRITICAL CHURN ALERT\n" +
                        "Customer: %s (%s)\n" +
                        "MRR: $%,.0f\n" +
                        "Churn risk: %.0f%%\n" +
                        "Top issues: %s\n\n" +
                        "Action required within 24 hours.",
                        intervention.customer().getName(),
                        intervention.customer().getCompanyName(),
                        intervention.customer().getMRR(),
                        intervention.getChurnScore().churnProbability() * 100,
                        intervention.getChurnScore().factors().get(0).description()
                    )
                );

                // Create executive task
                taskService.create(Task.builder()
                    .assignee(intervention.assignedTo())
                    .title("[CEO ACTION] Save " + intervention.customer().getName())
                    .priority(Priority.CRITICAL)
                    .dueDate(intervention.dueDate())
                    .build()
                );
            }
        }

        intervention.setExecutedAt(LocalDateTime.now());
        interventionRepository.save(intervention);
    }

    /**
     * Track intervention effectiveness
     */
    public InterventionEffectiveness analyzeInterventionEffectiveness() {
        List<Intervention> completedInterventions = interventionRepository
            .findByExecutedAtNotNull();

        Map<InterventionType, Double> saveRateByType = new HashMap<>();

        for (InterventionType type : InterventionType.values()) {
            List<Intervention> ofType = completedInterventions.stream()
                .filter(i -> i.type() == type)
                .toList();

            if (ofType.isEmpty()) continue;

            long saved = ofType.stream()
                .filter(i -> !i.customer().isChurned())
                .count();

            double saveRate = (double) saved / ofType.size();
            saveRateByType.put(type, saveRate);
        }

        return new InterventionEffectiveness(saveRateByType);
    }
}
```

---

## Part 5: Building Habit-Forming Products

### The Hooked Model (Nir Eyal)

**Four steps to creating habits:**

```
1. TRIGGER (Cue)
   ↓
2. ACTION (Behavior)
   ↓
3. REWARD (Variable reinforcement)
   ↓
4. INVESTMENT (Commitment)
   ↓
(Loop back to TRIGGER)
```

**Bibby example:**

```
1. TRIGGER:
   └─ Email: "5 new books added to your collection"
   └─ Push notification: "Sarah checked out a book"

2. ACTION:
   └─ Open Bibby
   └─ Check what books were added/checked out

3. REWARD:
   └─ See interesting new book
   └─ Feel productive (library is growing)
   └─ Social validation (students are reading!)

4. INVESTMENT:
   └─ Catalog another book (adds to collection)
   └─ Create reading list (builds value)
   └─ Configure preferences (personalizes experience)

Next TRIGGER:
   └─ More invested → more triggers → more likely to engage
```

### Designing Habit Loops

```java
@Service
public class HabitFormationService {

    /**
     * Create triggers that bring users back
     */
    @Scheduled(cron = "0 9 * * *") // Daily at 9am
    public void sendDailyTriggers() {
        List<Customer> activeCustomers = customerRepository.findActive();

        for (Customer customer : activeCustomers) {
            DailyDigest digest = buildDigest(customer);

            if (digest.hasContent()) {
                emailService.send(customer.getEmail(), EmailTemplate.builder()
                    .subject(String.format(
                        "Good morning! %d updates in your library",
                        digest.totalUpdates()
                    ))
                    .body(String.format(
                        "New checkouts: %d\n" +
                        "Books returned: %d\n" +
                        "Popular today: %s\n\n" +
                        "[View Dashboard]",
                        digest.newCheckouts(),
                        digest.returns(),
                        digest.popularBook().getTitle()
                    ))
                    .build()
                );

                habitTracker.recordTrigger(customer, "DAILY_DIGEST");
            }
        }
    }

    /**
     * Reward engagement with positive reinforcement
     */
    public void celebrateMilestones(Customer customer) {
        LibraryStats stats = statsService.getStats(customer);

        // Milestone: 100 books cataloged
        if (stats.totalBooks() == 100) {
            notificationService.show(customer, Notification.builder()
                .title("🎉 Milestone: 100 Books!")
                .message("Your library has reached 100 books! You're building something amazing.")
                .cta("Share your progress")
                .ctaAction(() -> shareService.createShareableStats(customer))
                .build()
            );

            habitTracker.recordReward(customer, "MILESTONE_100_BOOKS");
        }

        // Milestone: 7-day streak
        if (stats.currentStreak() == 7) {
            notificationService.show(customer, Notification.builder()
                .title("🔥 7-Day Streak!")
                .message("You've logged in 7 days in a row. Keep it going!")
                .build()
            );

            habitTracker.recordReward(customer, "STREAK_7_DAYS");
        }
    }

    /**
     * Encourage investment (builds commitment)
     */
    public void encourageInvestment(Customer customer) {
        ProfileCompleteness profile = profileService.analyze(customer);

        if (!profile.hasProfilePhoto()) {
            notificationService.show(customer, Notification.builder()
                .title("Personalize your profile")
                .message("Add a photo to make your library feel like home")
                .cta("Add photo")
                .build()
            );
        }

        if (!profile.hasCustomCategories()) {
            notificationService.show(customer, Notification.builder()
                .title("Organize your way")
                .message("Create custom categories to match your library's needs")
                .cta("Create category")
                .build()
            );
        }

        // More investment = more likely to stay
        habitTracker.recordInvestment(customer, profile.completenessScore());
    }
}
```

---

## Part 6: Reactivation Campaigns

### Winback Strategy

**Not all churn is permanent. Some customers can be won back.**

**Winback timeline:**

```
Day 0: Customer cancels
├─ Immediate: "We're sorry to see you go" (ask why)
└─ Offer: "Want to pause instead of cancel?"

Day 7: First winback attempt
├─ Email: "We've made improvements based on your feedback"
└─ Offer: "Come back for 50% off for 3 months"

Day 30: Second winback attempt
├─ Email: "Here's what you're missing"
└─ Content: Show new features, customer success stories

Day 90: Final winback attempt
├─ Email: "We'd love to have you back"
└─ Offer: "Free month + setup help"

Day 180: Archive (stop outreach)
```

**Implementation:**

```java
@Service
public class WinbackCampaignService {

    @Scheduled(cron = "0 10 * * *") // Daily at 10am
    public void executeWinbackCampaigns() {
        LocalDate today = LocalDate.now();

        // Day 7 winback
        List<Customer> day7Churned = customerRepository
            .findByChurnDateAndDaysAgo(7);

        for (Customer customer : day7Churned) {
            sendWinbackEmail(customer, WinbackStage.DAY_7);
        }

        // Day 30 winback
        List<Customer> day30Churned = customerRepository
            .findByChurnDateAndDaysAgo(30);

        for (Customer customer : day30Churned) {
            sendWinbackEmail(customer, WinbackStage.DAY_30);
        }

        // Day 90 winback (final attempt)
        List<Customer> day90Churned = customerRepository
            .findByChurnDateAndDaysAgo(90);

        for (Customer customer : day90Churned) {
            sendWinbackEmail(customer, WinbackStage.DAY_90_FINAL);
        }
    }

    private void sendWinbackEmail(Customer customer, WinbackStage stage) {
        EmailTemplate template = switch (stage) {
            case DAY_7 -> EmailTemplate.builder()
                .subject("We've been improving Bibby")
                .body(String.format(
                    "Hi %s,\n\n" +
                    "Since you left, we've:\n" +
                    "- Added barcode scanning\n" +
                    "- Improved search speed 3×\n" +
                    "- Built inter-library loan network\n\n" +
                    "We'd love to have you back.\n" +
                    "Come back now: 50%% off for 3 months\n\n" +
                    "[Reactivate Account]",
                    customer.getName()
                ))
                .build();

            case DAY_30 -> EmailTemplate.builder()
                .subject("See what you're missing")
                .body(String.format(
                    "Hi %s,\n\n" +
                    "2,000+ libraries are now on Bibby.\n" +
                    "Here's what they're saying:\n\n" +
                    "\"Bibby saves me 10 hours/week\" - Michelle D.\n" +
                    "\"Best library software I've used\" - Sarah M.\n\n" +
                    "[Read More Success Stories]\n\n" +
                    "Ready to give us another try?\n" +
                    "[Reactivate Account]",
                    customer.getName()
                ))
                .build();

            case DAY_90_FINAL -> EmailTemplate.builder()
                .subject("One last offer from Bibby")
                .body(String.format(
                    "Hi %s,\n\n" +
                    "We respect your decision to leave, but wanted\n" +
                    "to make one final offer:\n\n" +
                    "Come back and get:\n" +
                    "- 1 month free\n" +
                    "- Free setup help\n" +
                    "- Personal onboarding call\n\n" +
                    "No pressure—we just think you'll love what\n" +
                    "we've built since you left.\n\n" +
                    "[Reactivate Account]\n\n" +
                    "If not, we wish you the best!\n\n" +
                    "Thanks,\n" +
                    "[Founder Name]",
                    customer.getName()
                ))
                .build();
        };

        emailService.send(customer.getEmail(), template);
        winbackRepository.recordAttempt(customer, stage);
    }

    /**
     * Track winback effectiveness
     */
    public WinbackEffectiveness analyzeWinback() {
        List<WinbackAttempt> attempts = winbackRepository.findAll();

        long day7Sent = attempts.stream()
            .filter(a -> a.stage() == WinbackStage.DAY_7)
            .count();

        long day7Reactivated = attempts.stream()
            .filter(a -> a.stage() == WinbackStage.DAY_7)
            .filter(a -> a.customer().isReactivated())
            .count();

        double day7Rate = day7Sent > 0 ? (double)day7Reactivated / day7Sent : 0.0;

        // Similar for day 30 and day 90...

        return new WinbackEffectiveness(
            day7Rate,
            day30Rate,
            day90Rate
        );
    }
}
```

---

## Part 7: Retention ROI

### Why Retention is 5× More Valuable Than Acquisition

**The math:**

```
Scenario 1: Focus on acquisition
├─ CAC: $500
├─ Monthly churn: 5%
├─ Customer lifetime: 20 months (1 / 0.05)
├─ LTV: $100/mo × 20 months × 80% margin = $1,600
├─ LTV:CAC: 3.2:1
└─ Profit per customer: $1,100

Scenario 2: Reduce churn from 5% to 2%
├─ CAC: $500 (same)
├─ Monthly churn: 2%
├─ Customer lifetime: 50 months (1 / 0.02)
├─ LTV: $100/mo × 50 months × 80% margin = $4,000
├─ LTV:CAC: 8:1
└─ Profit per customer: $3,500

Result: 3× more profit per customer just from retention!
```

**The compounding effect:**

```
Year 1: 100 customers
├─ 5% churn: End with 61 customers
├─ 2% churn: End with 89 customers
└─ Difference: 46% more customers

Year 3: Start with 100 customers
├─ 5% churn: End with 23 customers
├─ 2% churn: End with 70 customers
└─ Difference: 3× more customers!
```

**Retention ROI calculation:**

```java
@Service
public class RetentionROICalculator {

    public record RetentionROI(
        double currentChurnRate,
        double targetChurnRate,
        double currentLTV,
        double targetLTV,
        double ltvGainPerCustomer,
        double investmentRequired,
        double roi,
        int paybackMonths
    ) {}

    /**
     * Calculate ROI of churn reduction initiative
     */
    public RetentionROI calculateROI(
            double currentChurn,
            double targetChurn,
            double monthlyRevenue,
            double grossMargin,
            double investmentRequired) {

        // Current LTV
        double currentLifetime = 1.0 / currentChurn; // months
        double currentLTV = monthlyRevenue * currentLifetime * grossMargin;

        // Target LTV
        double targetLifetime = 1.0 / targetChurn;
        double targetLTV = monthlyRevenue * targetLifetime * grossMargin;

        // Gain per customer
        double ltvGain = targetLTV - currentLTV;

        // How many customers do we need to retain to break even?
        double customersToBreakEven = investmentRequired / ltvGain;

        // ROI over 12 months
        int newCustomers = 100; // Assume 100 new customers
        double additionalValue = newCustomers * ltvGain;
        double roi = (additionalValue - investmentRequired) / investmentRequired;

        // Payback period
        double monthlyBenefit = (monthlyRevenue * newCustomers * grossMargin) *
                                (targetLifetime - currentLifetime) / currentLifetime;
        int paybackMonths = monthlyBenefit > 0
            ? (int) Math.ceil(investmentRequired / monthlyBenefit)
            : Integer.MAX_VALUE;

        return new RetentionROI(
            currentChurn,
            targetChurn,
            currentLTV,
            targetLTV,
            ltvGain,
            investmentRequired,
            roi,
            paybackMonths
        );
    }
}
```

**Example:**

```
Investment: Hire dedicated customer success manager ($80K/year)
Goal: Reduce churn from 3% to 2%

ROI calculation:
├─ Current LTV: $100 × (1/0.03) × 0.8 = $2,667
├─ Target LTV: $100 × (1/0.02) × 0.8 = $4,000
├─ Gain per customer: $1,333
├─ New customers per year: 500
├─ Total value gain: $666,500
├─ Investment: $80,000
├─ ROI: 733%
└─ Payback: 1.4 months

Decision: ABSOLUTELY do this!
```

---

## Deliverables

### 1. Churn Analysis Dashboard

Build comprehensive churn tracking:
- Churn by type (voluntary, involuntary)
- Churn by reason (top 5)
- Cohort retention curves
- Revenue retention vs customer retention
- Saveable churn identification

### 2. Churn Prediction Model

Implement early warning system:
- Calculate churn scores for all customers
- Identify risk factors (usage decline, support issues, etc.)
- Alert customer success team for high-risk accounts
- Track prediction accuracy

### 3. Retention Intervention Playbook

Create intervention workflows:
- Low risk: Automated tips email
- Medium risk: Personalized CSM outreach
- High risk: Phone call + offer
- Critical risk: Executive involvement

Track effectiveness by intervention type.

### 4. Habit Formation Features

Add habit loops to Bibby:
- Daily digest emails (trigger)
- Milestone celebrations (reward)
- Profile customization (investment)
- Streak tracking (gamification)

### 5. Winback Campaign

Design 90-day winback sequence:
- Day 0: Exit survey
- Day 7: 50% off offer
- Day 30: Success stories
- Day 90: Final offer

Track reactivation rate by stage.

---

## Reflection Questions

1. **Churn Types:**
   - What % of Bibby's churn is saveable?
   - Which churn reasons should you address first?
   - Is voluntary or involuntary churn worse?

2. **Prediction:**
   - What's the earliest signal of churn risk?
   - Can you predict churn 90 days out?
   - How accurate are your predictions?

3. **Interventions:**
   - At what churn probability should you intervene?
   - Should founders call critical-risk customers personally?
   - Can you automate too much?

4. **Retention vs. Acquisition:**
   - Should Bibby spend more on retention or acquisition?
   - What's the ROI of reducing churn from 3% to 2%?
   - When does acquisition matter more than retention?

5. **Habits:**
   - What's the core habit loop in Bibby?
   - How often should users engage to form a habit?
   - Can you over-notify users?

---

## Week 18 Summary

You've mastered retention and churn prevention:

1. **Types of churn:** Voluntary, involuntary, saveable vs unsaveable
2. **Cohort retention:** Track curves, identify problem cohorts
3. **Churn prediction:** Early warning signals, scoring model
4. **Interventions:** Ladder from automated emails to executive outreach
5. **Habit formation:** Hooked model (trigger, action, reward, investment)
6. **Winback campaigns:** 90-day sequence, reactivation offers
7. **Retention ROI:** Why reducing churn 3% → 2% = 3× more profit

**Key Insight:** Retention compounds. Reducing churn from 5% to 2% means 3× more customers after 3 years—worth far more than marginal acquisition gains.

**For Bibby:**
- **Current:** 3% monthly churn (31% annually)
- **Target:** 2% monthly churn (22% annually)
- **Strategy:**
  - Implement churn prediction (catch at-risk customers early)
  - Build intervention ladder (automated → personal → executive)
  - Add habit loops (daily triggers, milestone rewards)
  - Launch winback campaign (50% off at day 7)
- **Expected impact:** LTV increases from $2,667 to $4,000 (+50%)

---

## Looking Ahead: Week 19

Next week: **Financial Modeling & Forecasting**

You've optimized metrics (MRR, CAC, churn). Now you'll model the business:
- Building 5-year financial models
- Revenue forecasting methodologies
- Cohort-based projections
- Scenario planning (best/worst/likely)
- Unit economics at scale
- Break-even analysis
- Runway calculations

Plus: How to build financial models that impress investors.

---

**Progress:** 18/52 weeks complete (35% of apprenticeship, Week 5 of Semester 2)

**Commit your work:**
```bash
git add apprenticeship/semester-2/week-18-churn-analysis-retention-strategy.md
git commit -m "Add Week 18: Churn Analysis & Retention Strategy"
git push
```

Type "continue" when ready for Week 19.
