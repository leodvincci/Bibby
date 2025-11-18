# Week 17: CAC Optimization & Growth Loops

**Mentor Voice: Senior Architect**

> "The best growth loops are architectural—they're built into the product itself, not bolted on through marketing campaigns. Every user action should create value for the next user." - Growth engineering wisdom
>
> Last week you optimized revenue per customer (pricing). This week, you'll optimize cost per customer (CAC). But CAC optimization isn't just about spending less—it's about building self-sustaining growth engines. The companies that win are those that architect growth into the product: Dropbox's referral storage, Slack's team invitations, Zoom's meeting participants. This week, you'll learn to analyze channels, build viral loops, create content flywheels, and design product-led growth systems that make acquisition cheaper and faster over time. Because the best marketing is a product that markets itself.

---

## Learning Objectives

By the end of this week, you will:

1. Calculate CAC by channel and cohort
2. Build marketing channel attribution models
3. Design viral loops and referral programs
4. Create content marketing flywheels
5. Optimize paid acquisition channels
6. Implement product-led growth loops
7. Measure sales efficiency metrics
8. Build self-sustaining growth engines
9. Track CAC trends over time

---

## Part 1: Understanding CAC by Channel

### CAC: The Full Picture

**Customer Acquisition Cost (CAC)** = All costs to acquire a customer / Number of customers acquired

**Full CAC formula:**
```
CAC = (Marketing Spend + Sales Spend + Tools + Overhead) / New Customers
```

**Example breakdown:**

```
Bibby - Month of January:
├─ Marketing spend: $10,000
│   ├─ Google Ads: $4,000
│   ├─ Conference booth: $3,000
│   ├─ Content marketing: $2,000
│   └─ Social ads: $1,000
├─ Sales spend: $8,000
│   ├─ Sales salaries: $6,000
│   └─ Sales tools (CRM): $2,000
├─ Overhead allocation: $2,000
│   └─ Marketing team salaries
└─ Total spend: $20,000

New customers acquired: 40

Blended CAC = $20,000 / 40 = $500/customer
```

**But blended CAC hides the truth.** You need CAC by channel.

### CAC by Channel

**The reality:**

```
Bibby - CAC by Channel (January):

Google Ads:
├─ Spend: $4,000
├─ Customers: 10
└─ CAC: $400

Conference:
├─ Spend: $3,000
├─ Customers: 5
└─ CAC: $600

Content/SEO:
├─ Spend: $2,000 (content creation)
├─ Customers: 20
└─ CAC: $100 ← Best channel!

Social Ads:
├─ Spend: $1,000
├─ Customers: 2
└─ CAC: $500

Referrals:
├─ Spend: $0 (product feature)
├─ Customers: 3
└─ CAC: $0 ← Even better!

Insight: Content/SEO and referrals are 4-6× more efficient than paid channels
```

**Decision:** Double down on content, build referral program, reduce social ads.

### Implementation: CAC Tracker

```java
// src/main/java/com/penrose/bibby/analytics/CACTracker.java

@Service
public class CACTracker {

    public enum Channel {
        ORGANIC_SEARCH,
        PAID_SEARCH,
        SOCIAL_PAID,
        SOCIAL_ORGANIC,
        REFERRAL,
        DIRECT,
        CONFERENCE,
        CONTENT,
        EMAIL,
        PARTNERSHIP
    }

    public record ChannelCAC(
        Channel channel,
        YearMonth month,
        BigDecimal spend,
        int customersAcquired,
        BigDecimal cacPerCustomer,
        BigDecimal ltv,
        double ltvCacRatio
    ) {}

    /**
     * Calculate CAC by channel for a given month
     */
    public List<ChannelCAC> calculateMonthlyCAC(YearMonth month) {
        List<ChannelCAC> results = new ArrayList<>();

        for (Channel channel : Channel.values()) {
            // Get spend for this channel
            BigDecimal spend = marketingSpendRepository
                .getTotalSpend(channel, month);

            // Get customers acquired from this channel
            List<Customer> customers = customerRepository
                .findByAcquisitionChannelAndMonth(channel, month);

            int count = customers.size();

            BigDecimal cacPerCustomer = count > 0
                ? spend.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            // Calculate average LTV for this cohort
            BigDecimal avgLTV = customers.stream()
                .map(c -> ltvCalculator.calculateLTV(c))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(count, 1)), 2, RoundingMode.HALF_UP);

            double ltvCacRatio = cacPerCustomer.compareTo(BigDecimal.ZERO) > 0
                ? avgLTV.divide(cacPerCustomer, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

            results.add(new ChannelCAC(
                channel,
                month,
                spend,
                count,
                cacPerCustomer,
                avgLTV,
                ltvCacRatio
            ));
        }

        return results.stream()
            .sorted(Comparator.comparing(ChannelCAC::cacPerCustomer))
            .toList();
    }

    /**
     * Display CAC dashboard
     */
    public void printCACDashboard(YearMonth month) {
        List<ChannelCAC> channelData = calculateMonthlyCAC(month);

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📊 CAC by Channel - " + month);
        System.out.println("═══════════════════════════════════════════════\n");

        System.out.printf("%-20s | %8s | %5s | %8s | %8s\n",
            "Channel", "Spend", "Count", "CAC", "LTV:CAC");
        System.out.println("─".repeat(70));

        BigDecimal totalSpend = BigDecimal.ZERO;
        int totalCustomers = 0;

        for (ChannelCAC data : channelData) {
            System.out.printf("%-20s | $%,7.0f | %5d | $%,7.0f | %7.1f:1\n",
                data.channel(),
                data.spend(),
                data.customersAcquired(),
                data.cacPerCustomer(),
                data.ltvCacRatio()
            );

            totalSpend = totalSpend.add(data.spend());
            totalCustomers += data.customersAcquired();
        }

        System.out.println("─".repeat(70));

        BigDecimal blendedCAC = totalCustomers > 0
            ? totalSpend.divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        System.out.printf("%-20s | $%,7.0f | %5d | $%,7.0f\n",
            "TOTAL (Blended)",
            totalSpend,
            totalCustomers,
            blendedCAC
        );
    }
}
```

---

## Part 2: Marketing Channel Attribution

### The Attribution Problem

**Scenario:** A customer's journey to signup:

```
Week 1: Sees blog post from Google search (Organic)
Week 2: Clicks on LinkedIn ad (Paid Social)
Week 3: Receives email from conference follow-up (Conference)
Week 4: Signs up for trial (which channel gets credit?)
```

### Attribution Models

**1. Last-Touch Attribution** (Most common)

```
Credit: Conference (100%)
└─ Whichever touchpoint immediately preceded signup gets all credit

Pros: Simple to implement
Cons: Ignores earlier touchpoints that built awareness
```

**2. First-Touch Attribution**

```
Credit: Organic Search (100%)
└─ Whichever touchpoint first introduced customer gets all credit

Pros: Values awareness/discovery
Cons: Ignores nurture and conversion touchpoints
```

**3. Linear Attribution**

```
Credit: Organic (33%), Paid Social (33%), Conference (33%)
└─ Every touchpoint gets equal credit

Pros: Recognizes all touchpoints
Cons: Gives too much credit to minor touchpoints
```

**4. Time-Decay Attribution**

```
Credit: Organic (20%), Paid Social (30%), Conference (50%)
└─ More recent touchpoints get more credit

Pros: Values conversion-driving touchpoints
Cons: Can undervalue awareness
```

**5. U-Shaped Attribution** (Best for B2B)

```
Credit: Organic (40%), Paid Social (20%), Conference (40%)
└─ First and last touch get 40% each, middle gets 20%

Pros: Values discovery and conversion
Cons: More complex to implement
```

**Bibby's approach: U-Shaped**

```java
@Service
public class AttributionEngine {

    public record Touchpoint(
        LocalDateTime timestamp,
        Channel channel,
        String campaignId,
        TouchpointType type
    ) {}

    public enum TouchpointType {
        FIRST_TOUCH,
        MIDDLE_TOUCH,
        LAST_TOUCH
    }

    /**
     * Calculate U-shaped attribution for a customer
     */
    public Map<Channel, Double> calculateAttribution(Customer customer) {
        List<Touchpoint> journey = touchpointRepository
            .findByCustomerId(customer.getId())
            .stream()
            .sorted(Comparator.comparing(Touchpoint::timestamp))
            .toList();

        if (journey.isEmpty()) {
            return Map.of();
        }

        Map<Channel, Double> attribution = new HashMap<>();

        if (journey.size() == 1) {
            // Single touch: 100% attribution
            attribution.put(journey.get(0).channel(), 1.0);
        } else if (journey.size() == 2) {
            // Two touches: 50/50
            attribution.put(journey.get(0).channel(), 0.5);
            attribution.put(journey.get(1).channel(), 0.5);
        } else {
            // U-shaped: First = 40%, Last = 40%, Middle = 20% split
            Touchpoint first = journey.get(0);
            Touchpoint last = journey.get(journey.size() - 1);
            List<Touchpoint> middle = journey.subList(1, journey.size() - 1);

            attribution.put(first.channel(), 0.4);
            attribution.put(last.channel(),
                attribution.getOrDefault(last.channel(), 0.0) + 0.4);

            // Split 20% among middle touches
            double middleCredit = 0.2 / middle.size();
            for (Touchpoint touch : middle) {
                attribution.put(touch.channel(),
                    attribution.getOrDefault(touch.channel(), 0.0) + middleCredit);
            }
        }

        return attribution;
    }

    /**
     * Calculate attributed CAC by channel
     */
    public Map<Channel, BigDecimal> calculateAttributedCAC(YearMonth month) {
        List<Customer> customers = customerRepository
            .findByAcquisitionMonth(month);

        Map<Channel, Double> totalAttribution = new HashMap<>();
        Map<Channel, BigDecimal> totalSpend = new HashMap<>();

        // Aggregate attribution across all customers
        for (Customer customer : customers) {
            Map<Channel, Double> customerAttribution = calculateAttribution(customer);
            customerAttribution.forEach((channel, credit) ->
                totalAttribution.merge(channel, credit, Double::sum)
            );
        }

        // Get spend by channel
        for (Channel channel : Channel.values()) {
            BigDecimal spend = marketingSpendRepository.getTotalSpend(channel, month);
            totalSpend.put(channel, spend);
        }

        // Calculate attributed CAC
        Map<Channel, BigDecimal> attributedCAC = new HashMap<>();
        for (Channel channel : Channel.values()) {
            double customers = totalAttribution.getOrDefault(channel, 0.0);
            BigDecimal spend = totalSpend.getOrDefault(channel, BigDecimal.ZERO);

            if (customers > 0) {
                BigDecimal cac = spend.divide(
                    BigDecimal.valueOf(customers),
                    2,
                    RoundingMode.HALF_UP
                );
                attributedCAC.put(channel, cac);
            }
        }

        return attributedCAC;
    }
}
```

---

## Part 3: Viral Loops & Referral Programs

### The Power of Virality

**Viral Coefficient (k):** Average number of new users each user brings

```
k = (% of users who invite) × (average invitations sent) × (% who accept)

Example:
k = 20% × 5 invitations × 10% acceptance
k = 0.20 × 5 × 0.10
k = 0.10

Interpretation: Each user brings 0.10 new users
```

**Why k matters:**

```
If k > 1: Exponential growth (each user brings >1 user → viral!)
If k = 1: Linear growth (sustains itself but doesn't accelerate)
If k < 1: Dependent on acquisition channels

Most products: k = 0.1-0.4 (not viral, but helpful)
Truly viral products: k > 1 (rare: Dropbox, Clubhouse early days)
```

### Designing Viral Loops

**The viral loop formula:**

```
1. User experiences value
2. Value creation triggers sharing
3. Sharing exposes non-users
4. Non-users experience value
5. Loop repeats
```

**Example: Dropbox**

```
1. User stores files in Dropbox (value)
2. User needs to share folder with colleague (trigger)
3. Colleague receives invitation link (exposure)
4. Colleague signs up to access folder (conversion)
5. Colleague stores files and shares with others (loop repeats)

k = 0.6 (each user brings 0.6 users on average)
```

### Bibby's Viral Loops

**Loop 1: Book Recommendation Sharing**

```java
@Service
public class BookRecommendationViralLoop {

    /**
     * Librarian creates shareable book list
     */
    @Transactional
    public ShareableList createShareableList(User librarian, List<Book> books, String title) {
        ShareableList list = new ShareableList();
        list.setCreator(librarian);
        list.setTitle(title); // "Ms. Johnson's Summer Reading List 2024"
        list.setBooks(books);
        list.setShareCode(generateShareCode());
        list.setShareUrl(generateShareUrl(list.getShareCode()));

        shareableListRepository.save(list);

        // Track viral loop initiation
        viralLoopTracker.trackShareCreated(librarian, list);

        return list;
    }

    /**
     * Non-user clicks shared link
     */
    public ShareableListView viewSharedList(String shareCode) {
        ShareableList list = shareableListRepository.findByShareCode(shareCode)
            .orElseThrow(() -> new NotFoundException("List not found"));

        // Increment view count
        list.incrementViews();

        // Track viral loop impression
        viralLoopTracker.trackShareViewed(list);

        // Build view with Bibby branding
        return ShareableListView.builder()
            .list(list)
            .books(list.getBooks())
            .creatorName(list.getCreator().getName())
            .footer("Powered by Bibby • Get your free trial at bibby.com")
            .ctaButton("Create Your Own List with Bibby")
            .ctaUrl("https://bibby.com/signup?ref=shared_list")
            .build();
    }

    /**
     * Non-user signs up after viewing
     */
    public void trackViralConversion(String shareCode, User newUser) {
        ShareableList list = shareableListRepository.findByShareCode(shareCode)
            .orElseThrow();

        // Track conversion
        viralLoopTracker.trackShareConverted(list, newUser);

        // Credit referring user with referral bonus
        referralService.creditReferral(list.getCreator(), newUser, "SHARED_LIST");

        // Calculate viral coefficient
        double k = viralCoefficientCalculator.calculate(list.getCreator());
        logger.info("Viral coefficient for user {}: {}", list.getCreator().getId(), k);
    }
}
```

**Loop 2: Inter-Library Loan Network**

```java
@Service
public class InterLibraryLoanViralLoop {

    /**
     * Library A searches network for book not in their collection
     */
    public List<BookAvailability> searchNetwork(String isbn, Library library) {
        List<BookAvailability> availability = networkService
            .findLibrariesWithBook(isbn)
            .stream()
            .filter(lib -> lib.isWithin(50, library)) // 50 mile radius
            .map(lib -> new BookAvailability(lib, lib.getCopyCount(isbn)))
            .toList();

        // If libraries found but user's library not on Bibby network:
        if (!availability.isEmpty() && !library.isOnBibbyNetwork()) {
            // Show value proposition
            return availability.stream()
                .peek(avail -> avail.setMessage(
                    "Join Bibby network to request this book from " + avail.library().getName()
                ))
                .toList();
        }

        return availability;
    }

    /**
     * Library B receives loan request from Library A
     */
    public void handleLoanRequest(LoanRequest request) {
        Library requestingLibrary = request.getRequestingLibrary();
        Library fulfillLibrary = request.getFulfillingLibrary();

        // If fulfilling library NOT on Bibby:
        if (!fulfillLibrary.isOnBibbyNetwork()) {
            // Send invitation email
            emailService.send(fulfillLibrary.getLibrarian(), EmailTemplate.builder()
                .subject(requestingLibrary.getName() + " wants to borrow from you!")
                .body(String.format(
                    "%s library (on Bibby) wants to borrow '%s' from your library.\n\n" +
                    "Join Bibby's inter-library loan network to:\n" +
                    "- Fulfill loan requests with 1 click\n" +
                    "- Borrow books from 500+ libraries\n" +
                    "- Track all loans in one dashboard\n\n" +
                    "Start free trial: [Link]",
                    requestingLibrary.getName(),
                    request.getBook().getTitle()
                ))
                .build()
            );

            // Track viral loop exposure
            viralLoopTracker.trackNetworkInvitation(fulfillLibrary);
        }
    }
}
```

**Loop 3: Parent Notifications**

```
Flow:
1. Student checks out book from library (using Bibby)
2. Librarian has "Parent notifications" enabled
3. Parent receives email: "Your child checked out [Book Title]"
4. Email footer: "Powered by Bibby • bibby.com"
5. Parent (who works at another school) clicks link
6. Parent signs up for trial at their school

Viral coefficient:
- 30% of librarians enable parent notifications
- 5% of parents work at other schools
- 10% of those sign up

k = 0.30 × 0.05 × 0.10 = 0.0015 (small but non-zero)
```

### Measuring Viral Growth

```java
@Service
public class ViralCoefficientCalculator {

    public record ViralMetrics(
        double invitationRate,      // % users who invite
        double invitationsPerInviter, // Avg invitations sent
        double conversionRate,      // % invitations that convert
        double viralCoefficient,    // k value
        String interpretation
    ) {}

    /**
     * Calculate viral coefficient for a cohort
     */
    public ViralMetrics calculateForCohort(YearMonth cohortMonth) {
        List<User> cohort = userRepository.findByAcquisitionMonth(cohortMonth);
        int cohortSize = cohort.size();

        // Count users who invited others
        long inviters = cohort.stream()
            .filter(u -> invitationRepository.countByInviter(u) > 0)
            .count();

        double invitationRate = (double) inviters / cohortSize;

        // Average invitations per inviter
        double totalInvitations = cohort.stream()
            .mapToInt(u -> invitationRepository.countByInviter(u))
            .sum();

        double invitationsPerInviter = inviters > 0
            ? totalInvitations / inviters
            : 0.0;

        // Conversion rate
        long conversions = cohort.stream()
            .mapToLong(u -> invitationRepository.countConversions(u))
            .sum();

        double conversionRate = totalInvitations > 0
            ? conversions / totalInvitations
            : 0.0;

        // Viral coefficient
        double k = invitationRate * invitationsPerInviter * conversionRate;

        String interpretation = k >= 1.0
            ? "VIRAL! Each user brings " + String.format("%.2f", k) + " new users"
            : k >= 0.5
            ? "Strong viral effect (" + String.format("%.2f", k) + " users per user)"
            : k >= 0.2
            ? "Moderate viral effect (" + String.format("%.2f", k) + " users per user)"
            : "Weak viral effect (" + String.format("%.2f", k) + " users per user)";

        return new ViralMetrics(
            invitationRate,
            invitationsPerInviter,
            conversionRate,
            k,
            interpretation
        );
    }
}
```

### Referral Program Design

**The referral incentive:**

```
Bad: "Refer a friend, get $10"
└─ Too small, not motivating

Good: "Refer a school, get 1 month free"
└─ Valuable ($100 value), aligned with product

Best: "Refer 3 schools, get 1 year free"
└─ Progressive rewards, creates game mechanics
```

**Bibby's referral program:**

```java
@Entity
public class ReferralProgram {

    @Id
    private Long userId;

    private String referralCode; // "SARAH_JOHNSON_2024"
    private int referralCount;

    // Reward tiers
    private List<ReferralTier> tiers = List.of(
        new ReferralTier(1, "1 month free", 1),
        new ReferralTier(3, "3 months free", 3),
        new ReferralTier(5, "6 months free", 5),
        new ReferralTier(10, "1 year free", 10)
    );

    public ReferralTier getCurrentTier() {
        return tiers.stream()
            .filter(tier -> referralCount >= tier.threshold())
            .max(Comparator.comparing(ReferralTier::threshold))
            .orElse(null);
    }

    public ReferralTier getNextTier() {
        return tiers.stream()
            .filter(tier -> referralCount < tier.threshold())
            .min(Comparator.comparing(ReferralTier::threshold))
            .orElse(null);
    }
}

@Service
public class ReferralService {

    public void displayReferralProgress(User user) {
        ReferralProgram program = referralRepository.findByUserId(user.getId());

        System.out.println("🎁 Your Referral Progress\n");
        System.out.println("Referrals: " + program.getReferralCount());
        System.out.println("Your code: " + program.getReferralCode());
        System.out.println("Share: https://bibby.com/signup?ref=" + program.getReferralCode());
        System.out.println();

        ReferralTier current = program.getCurrentTier();
        if (current != null) {
            System.out.println("✅ Unlocked: " + current.reward());
        }

        ReferralTier next = program.getNextTier();
        if (next != null) {
            int needed = next.threshold() - program.getReferralCount();
            System.out.println("🎯 Next reward: " + next.reward());
            System.out.println("   (" + needed + " more referrals needed)");
        } else {
            System.out.println("🏆 You've unlocked all rewards!");
        }
    }
}
```

---

## Part 4: Content Marketing & SEO

### The Content Flywheel

**How content compounds:**

```
Month 1: Publish "How to Catalog Books: Complete Guide"
├─ Traffic: 100 visitors
├─ Signups: 5 (5% conversion)
└─ CAC: $200/customer ($1,000 content cost / 5 signups)

Month 6: Same article (now ranked #1 on Google)
├─ Traffic: 1,000 visitors/month
├─ Signups: 50/month
└─ CAC: $20/customer ($1,000 / 50)

Month 12: Same article
├─ Traffic: 2,000 visitors/month
├─ Signups: 100/month
└─ CAC: $10/customer ($1,000 / 100)

Total over 12 months:
├─ Traffic: 12,000+ visitors
├─ Signups: 600+
└─ Lifetime CAC: $1.67/customer

Compare to Google Ads:
└─ CAC: $400/customer (and stops when you stop paying)
```

**The key:** Content has **increasing returns over time**, while ads have **diminishing returns**.

### Content Strategy for Bibby

**Three content types:**

**1. Problem-Aware Content** (Top of funnel)

```
"How to Organize a School Library"
"MARC Records Explained for Beginners"
"Dewey Decimal System Guide"

Goal: Attract librarians researching their problems
Volume: 80% of content
Conversion: 1-3%
```

**2. Solution-Aware Content** (Middle of funnel)

```
"Best Library Management Software for Schools"
"Bibby vs. Alexandria vs. Destiny: Comparison"
"How to Choose Library Software in 2024"

Goal: Capture librarians evaluating solutions
Volume: 15% of content
Conversion: 5-10%
```

**3. Product Content** (Bottom of funnel)

```
"Bibby Pricing Guide"
"How to Migrate from Alexandria to Bibby"
"Bibby API Documentation"

Goal: Convert ready-to-buy prospects
Volume: 5% of content
Conversion: 15-25%
```

### SEO Implementation

```java
@Service
public class ContentStrategyService {

    public record ContentPiece(
        String title,
        String slug,
        ContentType type,
        List<String> keywords,
        int estimatedMonthlySearchVolume,
        int keywordDifficulty,
        double expectedConversionRate,
        int wordCount
    ) {}

    public enum ContentType {
        PROBLEM_AWARE,
        SOLUTION_AWARE,
        PRODUCT
    }

    /**
     * Prioritize content topics by potential CAC reduction
     */
    public List<ContentPiece> prioritizeContent() {
        List<ContentPiece> ideas = List.of(
            new ContentPiece(
                "How to Catalog Books: Complete Guide for School Librarians",
                "how-to-catalog-books",
                ContentType.PROBLEM_AWARE,
                List.of("how to catalog books", "book cataloging", "library cataloging"),
                1200, // monthly searches
                35,   // difficulty (0-100, lower is easier)
                0.02, // 2% conversion
                3000  // word count
            ),
            new ContentPiece(
                "Best Library Management Software for Schools (2024)",
                "best-library-software",
                ContentType.SOLUTION_AWARE,
                List.of("library management software", "library software for schools"),
                800,
                55,
                0.08, // 8% conversion
                4000
            ),
            new ContentPiece(
                "Bibby vs Alexandria: Which is Better for Elementary Schools?",
                "bibby-vs-alexandria",
                ContentType.SOLUTION_AWARE,
                List.of("bibby vs alexandria", "alexandria alternative"),
                200,
                20,
                0.15, // 15% conversion
                2500
            )
        );

        // Score each piece by expected customer acquisition
        return ideas.stream()
            .sorted(Comparator.comparing(this::calculateContentScore).reversed())
            .toList();
    }

    private double calculateContentScore(ContentPiece content) {
        // Expected monthly traffic (assuming rank #3)
        double traffic = content.estimatedMonthlySearchVolume() * 0.10; // 10% CTR at rank #3

        // Expected signups per month
        double signups = traffic * content.expectedConversionRate();

        // Difficulty penalty (harder to rank = longer to payback)
        double difficultyPenalty = 1.0 - (content.keywordDifficulty() / 100.0);

        // Content cost estimate ($0.15/word for quality content)
        double contentCost = content.wordCount() * 0.15;

        // Expected 12-month signups
        double yearSignups = signups * 12 * difficultyPenalty;

        // Expected CAC
        double expectedCAC = yearSignups > 0 ? contentCost / yearSignups : Double.MAX_VALUE;

        // Score = inverse of expected CAC (lower CAC = higher score)
        return yearSignups > 0 ? 1.0 / expectedCAC : 0.0;
    }
}
```

### Content ROI Tracking

```java
@Service
public class ContentROITracker {

    public record ContentPerformance(
        String slug,
        LocalDate publishDate,
        int monthsSincePublish,
        int totalVisits,
        int totalSignups,
        BigDecimal totalRevenue,
        BigDecimal contentCost,
        BigDecimal roi,
        BigDecimal cac
    ) {}

    public ContentPerformance analyze(String slug) {
        ContentPiece content = contentRepository.findBySlug(slug);

        LocalDate published = content.getPublishDate();
        long months = ChronoUnit.MONTHS.between(published, LocalDate.now());

        // Get all visits and signups from this content
        List<Visit> visits = analyticsService.getVisitsByLandingPage(slug);
        List<User> signups = visits.stream()
            .filter(Visit::convertedToSignup)
            .map(Visit::getUser)
            .toList();

        int totalVisits = visits.size();
        int totalSignups = signups.size();

        // Calculate revenue from these signups
        BigDecimal totalRevenue = signups.stream()
            .map(user -> revenueService.getTotalRevenue(user))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal contentCost = content.getProductionCost();

        BigDecimal roi = contentCost.compareTo(BigDecimal.ZERO) > 0
            ? totalRevenue.divide(contentCost, 2, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        BigDecimal cac = totalSignups > 0
            ? contentCost.divide(BigDecimal.valueOf(totalSignups), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new ContentPerformance(
            slug,
            published,
            (int) months,
            totalVisits,
            totalSignups,
            totalRevenue,
            contentCost,
            roi,
            cac
        );
    }

    public void printContentDashboard() {
        List<ContentPiece> topContent = contentRepository.findAll().stream()
            .map(c -> analyze(c.getSlug()))
            .sorted(Comparator.comparing(ContentPerformance::totalSignups).reversed())
            .limit(10)
            .toList();

        System.out.println("📝 Top 10 Content Pieces by Signups\n");
        System.out.printf("%-40s | %7s | %7s | %8s | %8s\n",
            "Title", "Visits", "Signups", "CAC", "ROI");
        System.out.println("─".repeat(90));

        for (ContentPerformance perf : topContent) {
            System.out.printf("%-40s | %,7d | %,7d | $%,7.0f | %,7.0f%%\n",
                truncate(perf.slug(), 40),
                perf.totalVisits(),
                perf.totalSignups(),
                perf.cac(),
                perf.roi()
            );
        }
    }
}
```

---

## Part 5: Product-Led Growth Loops

### PLG Loop Architecture

**The self-sustaining loop:**

```
1. User signs up (free trial)
2. User experiences value (catalogs books in 5 min)
3. User invites team member (needs help cataloging)
4. Team member experiences value
5. Team member invites another
6. Loop compounds
```

**Bibby's PLG loops:**

**Loop 1: Team Expansion**

```java
@Service
public class TeamExpansionLoop {

    /**
     * Detect when user hits single-person capacity
     */
    @Scheduled(cron = "0 9 * * *") // Daily at 9am
    public void detectExpansionOpportunities() {
        List<User> soloUsers = userRepository.findSingleUserAccounts();

        for (User user : soloUsers) {
            WorkloadAnalysis analysis = analyzeWorkload(user);

            // If workload is high, suggest adding team member
            if (analysis.isOverloaded()) {
                emailService.send(user, EmailTemplate.builder()
                    .subject("Your library is growing! Time to add help?")
                    .body(String.format(
                        "Hi %s,\n\n" +
                        "We noticed you've:\n" +
                        "- Cataloged %d books this month\n" +
                        "- Processed %d checkouts\n" +
                        "- Generated %d reports\n\n" +
                        "That's a lot for one person! " +
                        "Add a team member to share the work:\n\n" +
                        "[Invite Team Member Button]\n\n" +
                        "Team members get their own login and can help with:\n" +
                        "- Cataloging new books\n" +
                        "- Managing checkouts\n" +
                        "- Running reports\n\n" +
                        "No extra cost on your current plan.",
                        user.getName(),
                        analysis.booksThisMonth(),
                        analysis.checkoutsThisMonth(),
                        analysis.reportsThisMonth()
                    ))
                    .build()
                );

                plgLoopTracker.trackExpansionPrompt(user, "TEAM_INVITATION");
            }
        }
    }

    /**
     * When team member is invited
     */
    public void handleTeamInvitation(User inviter, String inviteeEmail) {
        TeamInvitation invitation = new TeamInvitation();
        invitation.setInviter(inviter);
        invitation.setInviteeEmail(inviteeEmail);
        invitation.setToken(generateSecureToken());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        teamInvitationRepository.save(invitation);

        // Send invitation email
        emailService.send(inviteeEmail, EmailTemplate.builder()
            .subject(inviter.getName() + " invited you to Bibby")
            .body(String.format(
                "%s has invited you to join their library on Bibby.\n\n" +
                "Bibby makes library management simple:\n" +
                "- Catalog books in seconds (ISBN auto-lookup)\n" +
                "- Manage checkouts with barcode scanning\n" +
                "- Generate reports instantly\n\n" +
                "[Accept Invitation]\n\n" +
                "Used by 2,000+ school libraries nationwide.",
                inviter.getName()
            ))
            .build()
        );

        plgLoopTracker.trackTeamInvitationSent(inviter, inviteeEmail);
    }

    /**
     * When team member accepts
     */
    public User acceptInvitation(String token) {
        TeamInvitation invitation = teamInvitationRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("Invitation not found"));

        if (invitation.isExpired()) {
            throw new ExpiredInvitationException();
        }

        // Create new user account
        User newUser = userService.createFromInvitation(invitation);

        // Add to inviter's team
        teamService.addMember(invitation.getInviter().getTeam(), newUser);

        // Track PLG loop success
        plgLoopTracker.trackTeamInvitationAccepted(invitation.getInviter(), newUser);

        // Calculate team expansion rate
        double k = calculateTeamExpansionRate(invitation.getInviter());
        logger.info("Team expansion k for user {}: {}", invitation.getInviter().getId(), k);

        return newUser;
    }
}
```

**Loop 2: Usage-Triggered Upgrades**

```java
@Service
public class UsageTriggeredUpgradeLoop {

    /**
     * Monitor usage and prompt upgrades at natural breaking points
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void detectUpgradeOpportunities() {
        List<User> starterUsers = userRepository.findByTier(Tier.STARTER);

        for (User user : starterUsers) {
            UsageLimits limits = checkUsageLimits(user);

            // Approaching book limit
            if (limits.booksUsed() >= limits.booksLimit() * 0.9) {
                notificationService.show(user, Notification.builder()
                    .title("You're almost at your 1,000 book limit!")
                    .message(String.format(
                        "You've cataloged %d of 1,000 books.\n\n" +
                        "Upgrade to Pro for 10,000 books + advanced features:\n" +
                        "- Barcode scanner\n" +
                        "- Advanced reporting\n" +
                        "- Priority support\n\n" +
                        "Only $75 more per month.",
                        limits.booksUsed()
                    ))
                    .cta("Upgrade to Pro")
                    .ctaAction(() -> upgradeService.initiateUpgrade(user, Tier.PRO))
                    .build()
                );

                plgLoopTracker.trackUpgradePrompt(user, "BOOK_LIMIT");
            }

            // High search usage (power user)
            if (limits.searchesThisWeek() > 100) {
                notificationService.show(user, Notification.builder()
                    .title("You're a power user!")
                    .message(String.format(
                        "You've searched %d times this week.\n\n" +
                        "Pro tier unlocks:\n" +
                        "- Advanced search filters\n" +
                        "- Saved searches\n" +
                        "- Search history\n\n" +
                        "Upgrade to Pro for $100/month.",
                        limits.searchesThisWeek()
                    ))
                    .cta("Upgrade to Pro")
                    .ctaAction(() -> upgradeService.initiateUpgrade(user, Tier.PRO))
                    .build()
                );

                plgLoopTracker.trackUpgradePrompt(user, "POWER_USER");
            }
        }
    }
}
```

---

## Deliverables

### 1. CAC Dashboard

Build a comprehensive CAC tracking system:
- CAC by channel (last 6 months)
- CAC by cohort (compare cohorts over time)
- Attribution model (U-shaped)
- LTV:CAC ratio by channel
- Blended CAC trend

### 2. Viral Loop Analysis

Measure viral coefficient:
- Calculate k for last 3 cohorts
- Identify which features drive virality
- Design 2 new viral loops
- Project impact on CAC

### 3. Content Strategy Roadmap

Plan 12 months of content:
- 20 problem-aware articles
- 5 solution-aware articles
- 3 product articles
- Prioritize by expected CAC
- Track ROI per article

### 4. Referral Program Launch

Design and implement referral program:
- Reward structure (progressive tiers)
- Landing page and emails
- Referral code system
- Tracking dashboard
- Success metrics

### 5. PLG Loop Implementation

Build 3 product-led growth loops:
- Team expansion loop
- Usage-triggered upgrade loop
- Network effect loop (inter-library)

Track: Loop activation rate, conversion rate, k value

---

## Reflection Questions

1. **CAC by Channel:**
   - Which channel has lowest CAC? Can you scale it?
   - Should you cut high-CAC channels or optimize them?
   - How does CAC change by cohort over time?

2. **Viral Loops:**
   - What's Bibby's viral coefficient? How can you increase it?
   - Which features naturally create sharing?
   - Is k = 0.3 good enough, or should you aim for k > 1?

3. **Content vs. Paid:**
   - When does content become more efficient than paid ads?
   - How long until content "pays back" its creation cost?
   - Should you do both, or pick one?

4. **Product-Led Growth:**
   - Can Bibby be 100% PLG (no sales team)?
   - What triggers should prompt upgrade prompts?
   - How do you avoid annoying users with prompts?

5. **Sustainable Growth:**
   - At what point is growth self-sustaining (CAC < payback period)?
   - Can you grow without paid acquisition?
   - What's the optimal CAC/LTV mix?

---

## Week 17 Summary

You've learned to build growth engines:

1. **CAC by channel:** Track every acquisition source separately
2. **Attribution:** U-shaped model for multi-touch journeys
3. **Viral loops:** Design product features that create sharing
4. **Referral programs:** Progressive rewards, gamification
5. **Content marketing:** Compounds over time, decreasing CAC
6. **PLG loops:** Team expansion, usage-triggered upgrades
7. **Sustainable growth:** Build self-perpetuating engines

**Key Insight:** The best growth is architectural—built into the product, not bolted on through marketing campaigns.

**For Bibby:**
- **Current CAC:** $500 blended (Content: $100, Referrals: $0, Paid: $400-600)
- **Viral coefficient:** k = 0.15 (not viral but helpful)
- **Strategy:** Double down on content (decreasing CAC over time) + build viral loops (shared book lists, inter-library network)
- **Goal:** Reduce blended CAC from $500 → $250 over 12 months through content + virality

---

## Looking Ahead: Week 18

Next week: **Churn Analysis & Retention Strategy**

You've optimized acquisition (lower CAC). Now you'll optimize retention (lower churn):
- Types of churn and why they happen
- Cohort-based retention analysis
- Predicting churn before it happens
- Retention tactics and interventions
- Building habit-forming products
- Reactivation campaigns
- Calculating retention ROI

Plus: Why preventing churn is 5× more valuable than acquiring new customers.

---

**Progress:** 17/52 weeks complete (33% of apprenticeship, Week 4 of Semester 2)

**Commit your work:**
```bash
git add apprenticeship/semester-2/week-17-cac-optimization-growth-loops.md
git commit -m "Add Week 17: CAC Optimization & Growth Loops"
git push
```

Type "continue" when ready for Week 18.
