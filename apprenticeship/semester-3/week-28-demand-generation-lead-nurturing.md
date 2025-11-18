# Week 28: Demand Generation & Lead Nurturing

**Semester 3: Marketing, Go-to-Market & Growth**
**Week 28 of 52 • Estimated Time: 15-20 hours**

---

## Overview

You've learned how to create content that ranks and attracts organic traffic. But traffic without conversion is just vanity metrics. This week, you'll learn how to build demand generation systems that turn awareness into qualified pipeline—the lifeblood of predictable revenue growth.

Demand generation isn't just "marketing." It's the systematic process of creating awareness, capturing intent, nurturing interest, and qualifying prospects until they're ready to buy. For B2B SaaS like Bibby, this means building a machine that consistently generates qualified leads at predictable cost.

This week covers:
- Lead magnet strategy and high-converting offers
- Email nurture sequences that move prospects through the funnel
- Webinar and event marketing that builds authority and pipeline
- Retargeting and remarketing to re-engage warm prospects
- Marketing automation that scales personalization
- Lead scoring and qualification models that align marketing and sales

By the end of this week, you'll understand how to build a demand generation engine that feeds your sales team with qualified, educated, ready-to-buy prospects.

---

## Why Demand Generation Matters

**The Problem with Just "Awareness"**

Many companies confuse awareness with demand. They celebrate traffic, followers, and impressions—but struggle to convert any of it into revenue.

Awareness without intent is useless:
- 100,000 website visitors → 500 email signups → 10 qualified leads → 2 customers
- That's a 0.002% conversion rate from traffic to customer

Demand generation solves this by building systems that:
1. **Capture intent signals** (downloads, webinar registrations, demo requests)
2. **Nurture interest systematically** (email sequences, content progression)
3. **Qualify prospects automatically** (lead scoring, behavioral tracking)
4. **Hand off sales-ready leads** (smooth marketing → sales transition)

**The Demand Gen Flywheel**

Great demand generation creates a compounding flywheel:

```
Content SEO → Traffic → Lead Magnet → Email List → Nurture Sequence
    → Trial Signup → Product Qualified Lead → Sales Conversation
    → Customer → Case Study → Content SEO (cycle repeats)
```

Each customer becomes a case study that strengthens your content, which attracts more traffic, which feeds the funnel again.

**Key Metrics**

Demand gen success is measured by pipeline generation:

- **MQLs (Marketing Qualified Leads)**: Prospects who match ICP and show intent
- **SQLs (Sales Qualified Leads)**: MQLs verified and accepted by sales
- **Pipeline Generated**: Total contract value of SQLs in the funnel
- **Cost per MQL**: How much you spend to generate each qualified lead
- **MQL → SQL Conversion Rate**: Quality of your lead qualification
- **Pipeline Velocity**: How fast leads move from MQL → SQL → Opportunity → Closed Won

**Example: SaaS Demand Gen Benchmarks**

For a B2B SaaS with $100 ACV (like Bibby):
- **Cost per MQL**: $50-80 (blend of inbound/outbound)
- **MQL → SQL Rate**: 30-40% (good qualification)
- **SQL → Opportunity Rate**: 50-60%
- **Opportunity → Customer Rate**: 20-30%
- **Overall MQL → Customer**: 3-7%

This means: For every 100 MQLs, you get 3-7 customers. At $100 ACV, that's $300-700 revenue from 100 MQLs costing $5,000-8,000 (first-month revenue, but LTV is much higher).

---

## Part 1: Lead Magnet Strategy

**What Makes a Great Lead Magnet?**

A lead magnet is a free offer in exchange for contact information. But not all lead magnets are created equal.

**Bad Lead Magnets** (generate quantity, not quality):
- Generic ebooks: "The Ultimate Guide to Books" (too broad)
- Checklists with no depth: "10 Tips for Reading Faster" (low value)
- Newsletters: "Subscribe for updates!" (no specific promise)

**Great Lead Magnets** (generate qualified, high-intent leads):
- **High specificity**: "The CLI App Developer's Guide to Building Personal Knowledge Systems"
- **Immediate value**: "7-Day Email Course: Build Your First Library Management System"
- **Tool or template**: "Notion Template: Book Tracking System vs Bibby Comparison"
- **Calculator or assessment**: "Library ROI Calculator: Is Your Reading System Working?"

**The Lead Magnet Ladder**

Different lead magnets attract different buying stages:

```
AWARENESS STAGE (Top of Funnel):
├─ Educational ebooks
├─ Blog post upgrades
├─ Checklists and templates
└─ Industry reports

CONSIDERATION STAGE (Middle of Funnel):
├─ Comparison guides
├─ Email courses
├─ Case studies
└─ Webinars

DECISION STAGE (Bottom of Funnel):
├─ ROI calculators
├─ Free trials
├─ Product demos
└─ Consultation calls
```

Match your lead magnet to the buyer's stage. Someone researching "personal knowledge management" (awareness) needs different content than someone searching "Bibby vs Readwise comparison" (decision).

**Lead Magnet Ideas for Bibby**

Let's design lead magnets for each stage:

**Top of Funnel** (Awareness):
1. **"The Reader's Guide to Personal Knowledge Management"** (40-page ebook)
   - Target: Knowledge workers, lifelong learners
   - Topics: Note-taking systems, book tracking, retention techniques
   - CTA: "Start organizing your library with Bibby free trial"

2. **"50 Books Every Engineer Should Read"** (curated list + Notion template)
   - Target: Software engineers, technical readers
   - Value: Curated list with summaries, organized by topic
   - CTA: "Import this list into Bibby with one command"

**Middle of Funnel** (Consideration):
3. **"7-Day Email Course: Build a Second Brain from Your Reading"**
   - Day 1: Why most reading doesn't stick
   - Day 2: The capture → organize → retrieve system
   - Day 3: How to take better notes
   - Day 4: Building a personal library
   - Day 5: The retrieval practice method
   - Day 6: Tools comparison (Notion vs Goodreads vs Bibby)
   - Day 7: Building the habit
   - CTA: "Try Bibby free for 14 days"

4. **"The Tool Comparison Matrix: 10 Book Tracking Systems Analyzed"**
   - Compare: Goodreads, Notion, Obsidian, Readwise, LibraryThing, StoryGraph, Bibby, etc.
   - Dimensions: Features, ease of use, privacy, export, price, CLI vs GUI
   - CTA: "See why developers choose Bibby"

**Bottom of Funnel** (Decision):
5. **"Reading ROI Calculator: How Much Knowledge Are You Losing?"**
   - Input: Books read per year, retention rate, average book price
   - Output: "You spent $800 on books but retained only 15% → $120 value"
   - Insight: "Bibby's spaced repetition increases retention to 60% → $480 value"
   - CTA: "Get a personalized reading system audit"

6. **"The Bibby Onboarding Playbook"** (for trial users)
   - Week 1 checklist: Import your library, configure CLI, set reading goals
   - Week 2: Add notes, tag books, build collections
   - Week 3: Advanced features (search, recommendations, exports)
   - CTA: "Book a 1:1 onboarding call"

**Implementation: Lead Magnet Delivery System**

```java
package com.bibby.marketing;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeadMagnetService {

    public enum LeadMagnetType {
        EBOOK,
        EMAIL_COURSE,
        TEMPLATE,
        CALCULATOR,
        WEBINAR,
        CHECKLIST,
        CASE_STUDY,
        COMPARISON_GUIDE
    }

    public enum FunnelStage {
        AWARENESS,      // Top of funnel
        CONSIDERATION,  // Middle of funnel
        DECISION        // Bottom of funnel
    }

    public record LeadMagnet(
        String id,
        String title,
        LeadMagnetType type,
        FunnelStage funnelStage,
        String landingPageUrl,
        String deliveryUrl,
        String followUpSequenceId,
        int estimatedMinutesToConsume,
        List<String> targetKeywords,
        String ctaText,
        String ctaDestination
    ) {}

    public record Lead(
        String email,
        String firstName,
        String lastName,
        LocalDateTime capturedAt,
        String leadMagnetId,
        String source,          // organic, paid-search, referral, etc.
        String utmCampaign,
        Map<String, String> customFields,
        LeadScore score
    ) {}

    public record LeadScore(
        int demographicScore,   // Job title, company size, industry fit
        int behavioralScore,    // Engagement with emails, content, product
        int totalScore
    ) {
        public String grade() {
            if (totalScore >= 80) return "A - Hot";
            if (totalScore >= 60) return "B - Warm";
            if (totalScore >= 40) return "C - Cool";
            return "D - Cold";
        }
    }

    /**
     * When someone downloads a lead magnet, capture the lead
     * and trigger appropriate nurture sequence
     */
    public Lead captureLead(String email, String firstName, String lastName,
                           String leadMagnetId, String source, String utmCampaign) {

        // 1. Create lead record
        Lead lead = new Lead(
            email,
            firstName,
            lastName,
            LocalDateTime.now(),
            leadMagnetId,
            source,
            utmCampaign,
            new HashMap<>(),
            new LeadScore(0, 0, 0) // Will be calculated later
        );

        // 2. Find the lead magnet to determine follow-up sequence
        LeadMagnet magnet = getLeadMagnet(leadMagnetId);

        // 3. Deliver the lead magnet immediately (email with download link)
        deliverLeadMagnet(lead, magnet);

        // 4. Enroll in appropriate nurture sequence
        enrollInNurtureSequence(lead, magnet.followUpSequenceId());

        // 5. Calculate initial lead score
        LeadScore score = calculateLeadScore(lead);

        // 6. If high score, notify sales immediately
        if (score.totalScore() >= 80) {
            notifySalesTeam(lead, "High-intent lead captured");
        }

        System.out.println("✅ Lead captured: " + email);
        System.out.println("   Lead Magnet: " + magnet.title());
        System.out.println("   Funnel Stage: " + magnet.funnelStage());
        System.out.println("   Score: " + score.grade());
        System.out.println("   Next: Enrolled in " + magnet.followUpSequenceId());

        return lead;
    }

    private LeadMagnet getLeadMagnet(String id) {
        // In reality, fetch from database
        // Hardcoded example:
        return new LeadMagnet(
            "ebook-pkm-guide",
            "The Reader's Guide to Personal Knowledge Management",
            LeadMagnetType.EBOOK,
            FunnelStage.AWARENESS,
            "https://bibby.dev/guides/pkm",
            "https://bibby.dev/downloads/pkm-guide.pdf",
            "nurture-seq-awareness-general",
            45, // minutes to read
            List.of("personal knowledge management", "book tracking",
                   "second brain", "reading system"),
            "Start Your Free Trial",
            "https://bibby.dev/signup"
        );
    }

    private void deliverLeadMagnet(Lead lead, LeadMagnet magnet) {
        // Send immediate email with download link
        System.out.println("📧 Sending lead magnet to " + lead.email());
    }

    private void enrollInNurtureSequence(Lead lead, String sequenceId) {
        // Enroll in drip email sequence
        System.out.println("📨 Enrolled in sequence: " + sequenceId);
    }

    private LeadScore calculateLeadScore(Lead lead) {
        // Will implement scoring logic in Part 6
        return new LeadScore(50, 20, 70);
    }

    private void notifySalesTeam(Lead lead, String reason) {
        System.out.println("🔔 Sales notification: " + reason + " - " + lead.email());
    }
}
```

**Lead Magnet Performance Tracking**

```java
@Service
public class LeadMagnetAnalytics {

    public record LeadMagnetMetrics(
        String leadMagnetId,
        String title,
        int landingPageViews,
        int conversions,
        double conversionRate,
        int mqls,               // Marketing Qualified Leads
        int sqls,               // Sales Qualified Leads
        int customers,
        double costPerLead,
        double costPerMql,
        double costPerCustomer,
        double roi             // Customer LTV / Total Cost
    ) {
        public String verdict() {
            if (roi >= 5.0) return "EXCELLENT - Scale up";
            if (roi >= 3.0) return "GOOD - Optimize and scale";
            if (roi >= 1.5) return "MARGINAL - Improve conversion";
            return "UNPROFITABLE - Pause or kill";
        }
    }

    public LeadMagnetMetrics analyzeLeadMagnet(String leadMagnetId,
                                               int landingPageViews,
                                               int conversions,
                                               int mqls,
                                               int sqls,
                                               int customers,
                                               double totalCost) {

        double conversionRate = (conversions * 100.0) / landingPageViews;
        double costPerLead = totalCost / conversions;
        double costPerMql = totalCost / mqls;
        double costPerCustomer = totalCost / customers;

        // Assume $300 LTV (3 years at $100/year)
        double customerValue = customers * 300;
        double roi = customerValue / totalCost;

        return new LeadMagnetMetrics(
            leadMagnetId,
            "PKM Guide",
            landingPageViews,
            conversions,
            conversionRate,
            mqls,
            sqls,
            customers,
            costPerLead,
            costPerMql,
            costPerCustomer,
            roi
        );
    }

    public void printMonthlyReport() {
        System.out.println("=== Lead Magnet Performance Report (March 2025) ===\n");

        // Example: 3 lead magnets with different performance
        var ebook = analyzeLeadMagnet(
            "ebook-pkm-guide",
            5000,  // landing page views
            750,   // conversions (15% conversion rate - good for ebook)
            225,   // MQLs (30% of leads)
            90,    // SQLs (40% of MQLs)
            18,    // Customers (20% of SQLs)
            3000   // Total cost (mostly paid ads to landing page)
        );

        var emailCourse = analyzeLeadMagnet(
            "email-course-second-brain",
            2000,  // landing page views
            600,   // conversions (30% - higher intent)
            300,   // MQLs (50% - email course attracts better quality)
            150,   // SQLs (50%)
            30,    // Customers (20%)
            2000   // Total cost
        );

        var calculator = analyzeLeadMagnet(
            "reading-roi-calculator",
            1000,  // landing page views
            400,   // conversions (40% - very high intent)
            280,   // MQLs (70% - calculator = strong intent signal)
            168,   // SQLs (60%)
            42,    // Customers (25% - bottom of funnel converts better)
            1500   // Total cost
        );

        printMagnet(ebook);
        printMagnet(emailCourse);
        printMagnet(calculator);

        System.out.println("\n💡 Insights:");
        System.out.println("1. Calculator has highest ROI (8.4×) despite lowest traffic");
        System.out.println("   → Bottom-of-funnel magnets attract buyers, not browsers");
        System.out.println("2. Email course has best MQL quality (50% MQL rate)");
        System.out.println("   → Multi-day engagement builds qualification");
        System.out.println("3. Ebook has scale but lower quality");
        System.out.println("   → Good for awareness, but need better qualification");
        System.out.println("\n🎯 Action Items:");
        System.out.println("• Double budget on calculator (highest ROI)");
        System.out.println("• Create more bottom-funnel magnets (tools, templates, audits)");
        System.out.println("• Add qualification quiz to ebook flow to improve MQL rate");
    }

    private void printMagnet(LeadMagnetMetrics m) {
        System.out.println(m.title());
        System.out.println("  Funnel: " + m.landingPageViews() + " views → " +
                          m.conversions() + " leads (" +
                          String.format("%.1f%%", m.conversionRate()) + ") → " +
                          m.mqls() + " MQLs → " + m.sqls() + " SQLs → " +
                          m.customers() + " customers");
        System.out.println("  Economics: $" + String.format("%.0f", m.costPerLead()) +
                          " per lead, $" + String.format("%.0f", m.costPerMql()) +
                          " per MQL, $" + String.format("%.0f", m.costPerCustomer()) +
                          " per customer");
        System.out.println("  ROI: " + String.format("%.1f", m.roi()) + "× → " +
                          m.verdict());
        System.out.println();
    }
}
```

**Key Insight**: Bottom-of-funnel lead magnets (calculators, comparisons, audits) have lower traffic but dramatically higher conversion rates and ROI.

---

## Part 2: Email Nurture Sequences

**What is Nurture?**

Nurture is the systematic process of educating, building trust, and moving prospects closer to purchase through a series of relevant, timely emails.

Most people aren't ready to buy immediately. The nurture sequence bridges the gap between "interested" and "ready to buy."

**The Anatomy of a Nurture Sequence**

A great nurture sequence has:
1. **Clear goal**: Trial signup, demo booking, purchase, etc.
2. **Logical progression**: Each email builds on the previous
3. **Value-first**: Teach, don't sell (until the end)
4. **Behavioral triggers**: Adjust based on engagement
5. **Multiple paths**: Different sequences for different segments

**Example: 7-Email Nurture Sequence for Bibby**

**Target**: Someone who downloaded "The Reader's Guide to PKM" ebook
**Goal**: Get them to start a free trial
**Duration**: 14 days (every 2 days)

---

**Email 1: Immediate Delivery + Welcome** (Day 0 - sent immediately)

```
Subject: Here's your PKM guide + one quick tip

Hi {{firstName}},

Thanks for downloading "The Reader's Guide to Personal Knowledge Management."

📥 Download your guide here: [LINK]

One thing before you dive in:

Most people who read books never return to them. The average retention rate for
reading is about 10-20%. That means 80% of what you learn... disappears.

The fix isn't reading more. It's building a system to capture, organize, and
retrieve what you've already learned.

That's what this guide is about—and it's what Bibby was built for.

Over the next two weeks, I'll send you practical tips for building your own
knowledge system. No fluff, just actionable advice.

Tomorrow: The #1 mistake people make with book tracking systems.

Best,
Leo
Bibby Team

P.S. Already have a reading system? Try Bibby free for 14 days: [LINK]
```

**Why this works**:
- Delivers value immediately (the ebook)
- Sets expectations (2-week email series)
- Introduces the problem Bibby solves
- Soft CTA at the end (P.S.)

---

**Email 2: Problem Awareness** (Day 2)

```
Subject: The book tracking trap (and how to avoid it)

Hi {{firstName}},

I see this all the time:

Someone decides to "get serious about reading." They pick a tool (Goodreads,
Notion, a spreadsheet) and start tracking their books religiously.

For a while, it feels great. You're logging books, rating them, writing little notes.

But then... nothing happens. You never look at your notes again. The "Currently
Reading" list grows stale. The whole system becomes a chore.

Here's why:

**Tracking ≠ Knowledge Management**

Most tools are designed for tracking (what you've read), not for retrieval
(finding what you need when you need it).

Think about it:
• When was the last time you searched your book notes?
• Can you quickly find "that thing you read about X"?
• Do you revisit and reinforce what you've learned?

If not, you're maintaining a database, not building a knowledge system.

Tomorrow I'll share the 3 components every knowledge system needs.

Best,
Leo

P.S. Bibby was built specifically for retrieval, not just tracking. See how: [LINK]
```

**Why this works**:
- Calls out the common failure pattern (tracking without retrieval)
- Shows deep understanding of reader's problem
- Teases next email (3 components)
- Links Bibby to the solution

---

**Email 3: Framework/Solution** (Day 4)

```
Subject: The 3 components of a knowledge system

Hi {{firstName}},

A real knowledge system has three parts:

**1. Capture** → Save ideas worth remembering
**2. Organize** → Structure so you can find things later
**3. Retrieve** → Actually use what you've captured

Most tools nail #1 and #2. Almost none nail #3.

Here's what good retrieval looks like:

You're writing a proposal and need to reference "that pricing framework from
the book on SaaS metrics."

Bad system: Scroll through Notion. Check Goodreads. Give up. Google it.

Good system: `bibby search "saas pricing framework"` → Instant results with
context from your notes.

Retrieval is the difference between:
• A graveyard of forgotten notes
• A living knowledge base that makes you smarter over time

The ROI is massive: If one insight saves you 2 hours of research, and you spent
$100/year on Bibby, you've paid for 3 years in one search.

Tomorrow: How to actually organize books so you can find them later.

Best,
Leo

P.S. See Bibby's retrieval engine in action: [DEMO VIDEO]
```

**Why this works**:
- Introduces simple framework (Capture, Organize, Retrieve)
- Paints vivid before/after picture
- Quantifies ROI
- Includes demo video (high engagement)

---

**Email 4: Social Proof** (Day 6)

```
Subject: How Sarah went from "I never remember what I read" to citation machine

Hi {{firstName}},

Meet Sarah. Engineering manager. Reads 40 books/year. Remembered almost nothing.

Her problem: "I'd read a great book on leadership, get inspired, then completely
forget the frameworks when I actually needed them in 1:1s."

She tried:
• Goodreads (just tracking, no retrieval)
• Notion (too much friction to add notes)
• Obsidian (over-complicated for her needs)

Then she tried Bibby. Here's what changed:

**Week 1**: Imported her library (200 books) with one command
**Week 2**: Started adding notes with `bibby note --quick`
**Week 3**: First "aha" moment—searched for "difficult conversations" and found
           notes from 3 books she'd read over 2 years
**Month 3**: Using `bibby recommend` to surface relevant books based on current challenges

Now she searches her library 2-3 times per week. "It's like having a conversation
with past-me," she said.

The difference? A system built for how she actually works (CLI, fast, simple).

Could Bibby work for you? Only one way to find out: [START FREE TRIAL]

Best,
Leo

P.S. Read Sarah's full story here: [CASE STUDY]
```

**Why this works**:
- Relatable persona (engineering manager, reads a lot)
- Shows specific journey (Week 1, Week 2, etc.)
- Direct quote adds authenticity
- Strong CTA (trial)

---

**Email 5: Objection Handling** (Day 8)

```
Subject: "Do I really need another tool?"

Hi {{firstName}},

Fair question. Tool fatigue is real.

You probably already have:
• Goodreads (for social book tracking)
• Notion or Evernote (for notes)
• Maybe Readwise (for highlights)

So why add Bibby?

Here's how I think about it:

**Goodreads**: Social network for readers (great for discovery, bad for retrieval)
**Notion**: General-purpose database (powerful but requires setup/maintenance)
**Readwise**: Highlight syncing (good for Kindle, doesn't handle non-Kindle books)

**Bibby**: Purpose-built CLI for developers who want fast, powerful book search
          without the overhead

The key question isn't "Do I need another tool?"

It's "Will this tool save me more time than it costs?"

For Bibby:
• Cost: $100/year, ~5 minutes setup
• Savings: 2-3 searches/week × 10 minutes saved per search = ~2 hours/month

If your time is worth $50/hour, that's $100/month in value for $8/month cost.

Plus: You can use Bibby alongside your existing tools. Import from Goodreads,
      export to Notion. No lock-in.

Try it free for 14 days (no credit card): [START TRIAL]

Best,
Leo
```

**Why this works**:
- Addresses real objection head-on
- Positions against alternatives (not competitors, but alternatives)
- Quantifies ROI clearly
- Shows interoperability (no lock-in)

---

**Email 6: Urgency/Scarcity** (Day 11)

```
Subject: Your guide to getting started (if you're ready)

Hi {{firstName}},

Over the past two weeks, we've covered:
• Why tracking ≠ knowledge management
• The 3 components of a real knowledge system
• How readers like Sarah use Bibby to retrieve insights
• Whether another tool is worth it (spoiler: depends on ROI)

If you've read this far, you probably fall into one of two camps:

**Camp 1**: "Interesting, but I'm sticking with my current system"
→ Totally fair. Keep the PKM guide and best of luck!

**Camp 2**: "I want to try this, but I keep putting it off"
→ I get it. Setup friction is real.

If you're in Camp 2, here's my offer:

Start your free trial today, and I'll send you the "Bibby Quick Start Playbook":
• 5-minute setup guide (import your library with one command)
• 10 most useful commands (90% of what you'll use)
• 3 workflows for different use cases (student, professional, researcher)

No credit card required. If you don't love it in 14 days, just stop using it.

[START FREE TRIAL + GET QUICK START PLAYBOOK]

This is the last email in this series. Thanks for reading.

Best,
Leo

P.S. Questions? Just reply to this email. I read every response.
```

**Why this works**:
- Recaps the journey
- Acknowledges both camps (respectful)
- Adds bonus incentive (Quick Start Playbook)
- Clear final CTA
- Invites reply (engagement)

---

**Email 7: Last Chance** (Day 14 - only if no trial signup)

```
Subject: [LAST CALL] Your PKM guide + trial

Hi {{firstName}},

Quick check-in: Did you get a chance to download the PKM guide?

If you did and found it helpful, I'd love to hear what you learned.
If you didn't, here's the link one more time: [DOWNLOAD]

And if you want to put the concepts into practice:
→ Start your free Bibby trial: [START TRIAL]

Either way, thanks for being part of the Bibby community.

Best,
Leo

P.S. Not interested in Bibby? No worries. Click here to unsubscribe from future emails.
```

**Why this works**:
- Friendly, low-pressure tone
- Gives exit option (unsubscribe)
- One final CTA
- Respects recipient's time

---

**Nurture Sequence Implementation**

```java
package com.bibby.marketing;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class NurtureSequenceService {

    public enum SequenceType {
        AWARENESS_GENERAL,      // Downloaded top-funnel content
        CONSIDERATION_TRIAL,    // Started trial but not activated
        DECISION_DEMO,          // Booked demo
        ONBOARDING_NEW_USER,    // Signed up for paid plan
        WINBACK_CHURNED         // Churned customer
    }

    public record EmailStep(
        int dayOffset,          // Days after enrollment
        String subject,
        String body,
        String ctaText,
        String ctaUrl,
        List<String> tags       // For tracking performance
    ) {}

    public record NurtureSequence(
        String id,
        String name,
        SequenceType type,
        List<EmailStep> steps,
        String goalConversion    // What success looks like
    ) {}

    public record Enrollment(
        String email,
        String sequenceId,
        LocalDateTime enrolledAt,
        int currentStep,
        LocalDateTime lastEmailSent,
        boolean completed,
        boolean converted,
        List<EmailInteraction> interactions
    ) {}

    public record EmailInteraction(
        int stepNumber,
        LocalDateTime sentAt,
        LocalDateTime openedAt,
        LocalDateTime clickedAt,
        List<String> linksClicked
    ) {}

    /**
     * Enroll a lead in a nurture sequence
     */
    public Enrollment enrollInSequence(String email, String sequenceId) {
        Enrollment enrollment = new Enrollment(
            email,
            sequenceId,
            LocalDateTime.now(),
            0,  // Start at step 0
            null,
            false,  // not completed
            false,  // not converted
            new ArrayList<>()
        );

        // Send first email immediately (Day 0)
        sendNextEmail(enrollment);

        System.out.println("✅ Enrolled " + email + " in sequence: " + sequenceId);

        return enrollment;
    }

    /**
     * Send the next email in the sequence
     */
    public void sendNextEmail(Enrollment enrollment) {
        NurtureSequence sequence = getSequence(enrollment.sequenceId());

        if (enrollment.currentStep() >= sequence.steps().size()) {
            System.out.println("✅ Sequence completed for " + enrollment.email());
            return;
        }

        EmailStep step = sequence.steps().get(enrollment.currentStep());

        // Send email
        System.out.println("\n📧 Sending Email " + (enrollment.currentStep() + 1) +
                          " to " + enrollment.email());
        System.out.println("   Subject: " + step.subject());
        System.out.println("   CTA: " + step.ctaText());
        System.out.println("   Scheduled: Day " + step.dayOffset());

        // Track interaction
        EmailInteraction interaction = new EmailInteraction(
            enrollment.currentStep(),
            LocalDateTime.now(),
            null,  // Not opened yet
            null,  // Not clicked yet
            new ArrayList<>()
        );

        enrollment.interactions().add(interaction);
    }

    /**
     * Process email opens, clicks, conversions
     */
    public void trackEmailOpen(String email, int stepNumber) {
        System.out.println("👀 Email opened: " + email + " (Step " + stepNumber + ")");
        // Update interaction record
    }

    public void trackEmailClick(String email, int stepNumber, String linkUrl) {
        System.out.println("🖱️  Link clicked: " + email + " → " + linkUrl);
        // Update interaction record
    }

    public void trackConversion(String email, String conversionType) {
        System.out.println("🎉 CONVERSION: " + email + " → " + conversionType);
        // Mark enrollment as converted
        // Remove from nurture sequence
        // Enroll in onboarding sequence
    }

    /**
     * Get sequence by ID
     */
    private NurtureSequence getSequence(String sequenceId) {
        // In reality, fetch from database
        // Hardcoded example:
        return new NurtureSequence(
            "nurture-seq-awareness-general",
            "PKM Guide Follow-Up (7 emails, 14 days)",
            SequenceType.AWARENESS_GENERAL,
            List.of(
                new EmailStep(0, "Here's your PKM guide + one quick tip",
                             "[Email 1 body]", "Start Free Trial",
                             "https://bibby.dev/trial",
                             List.of("welcome", "delivery")),
                new EmailStep(2, "The book tracking trap (and how to avoid it)",
                             "[Email 2 body]", "See How Bibby Works",
                             "https://bibby.dev/demo",
                             List.of("problem-aware")),
                new EmailStep(4, "The 3 components of a knowledge system",
                             "[Email 3 body]", "Watch Demo",
                             "https://bibby.dev/demo-video",
                             List.of("framework", "solution")),
                new EmailStep(6, "How Sarah went from 'I never remember' to citation machine",
                             "[Email 4 body]", "Start Free Trial",
                             "https://bibby.dev/trial",
                             List.of("social-proof", "case-study")),
                new EmailStep(8, "Do I really need another tool?",
                             "[Email 5 body]", "Try It Free (No CC)",
                             "https://bibby.dev/trial",
                             List.of("objection-handling")),
                new EmailStep(11, "Your guide to getting started (if you're ready)",
                             "[Email 6 body]", "Start Trial + Get Playbook",
                             "https://bibby.dev/trial?bonus=playbook",
                             List.of("urgency", "bonus")),
                new EmailStep(14, "[LAST CALL] Your PKM guide + trial",
                             "[Email 7 body]", "Start Free Trial",
                             "https://bibby.dev/trial",
                             List.of("last-chance"))
            ),
            "trial_signup"
        );
    }
}
```

**Sequence Performance Tracking**

```java
@Service
public class SequenceAnalytics {

    public record SequenceMetrics(
        String sequenceId,
        String sequenceName,
        int totalEnrolled,
        int[] emailsSent,        // Count for each step
        int[] emailsOpened,      // Count for each step
        int[] emailsClicked,     // Count for each step
        double[] openRates,      // % for each step
        double[] clickRates,     // % for each step
        int conversions,
        double conversionRate,
        int unsubscribes
    ) {}

    public void printSequenceReport() {
        System.out.println("=== Nurture Sequence Performance (March 2025) ===\n");
        System.out.println("Sequence: PKM Guide Follow-Up (7 emails, 14 days)\n");

        int totalEnrolled = 750;
        int[] sent = {750, 735, 720, 695, 670, 640, 610};
        int[] opened = {600, 550, 520, 485, 450, 400, 320};
        int[] clicked = {120, 100, 95, 110, 85, 130, 75};

        System.out.println("Email | Sent | Opened | Open% | Clicked | Click% | Subject");
        System.out.println("------|------|--------|-------|---------|--------|--------");

        String[] subjects = {
            "Here's your PKM guide + one quick tip",
            "The book tracking trap (and how to avoid it)",
            "The 3 components of a knowledge system",
            "How Sarah went from 'I never remember' to citation machine",
            "Do I really need another tool?",
            "Your guide to getting started (if you're ready)",
            "[LAST CALL] Your PKM guide + trial"
        };

        for (int i = 0; i < sent.length; i++) {
            double openRate = (opened[i] * 100.0) / sent[i];
            double clickRate = (clicked[i] * 100.0) / opened[i];

            System.out.printf("  %d   | %4d | %5d  | %4.1f%% | %6d  | %5.1f%% | %s%n",
                i + 1, sent[i], opened[i], openRate, clicked[i], clickRate,
                subjects[i].substring(0, Math.min(50, subjects[i].length())));
        }

        System.out.println("\n📊 Overall Metrics:");
        System.out.println("• Total Enrolled: " + totalEnrolled);
        System.out.println("• Completed Sequence: 610 (81%)");
        System.out.println("• Unsubscribed: 140 (19%)");
        System.out.println("• Converted to Trial: 105 (14% of enrolled)");
        System.out.println("• Trial → Paid: 21 (20% of trials)");
        System.out.println("• Overall ROI: 21 customers × $300 LTV = $6,300 from $3,000 spend");

        System.out.println("\n💡 Insights:");
        System.out.println("1. Email 4 (social proof) has highest click rate (22.7%)");
        System.out.println("   → Case studies resonate strongly");
        System.out.println("2. Email 6 (urgency + bonus) drives most conversions");
        System.out.println("   → Bonus offer (Quick Start Playbook) is effective");
        System.out.println("3. Open rates decline steadily (80% → 52%)");
        System.out.println("   → Normal attrition, but consider shortening sequence to 5 emails");
        System.out.println("4. 14% enrollment → trial rate is solid for top-funnel magnet");
        System.out.println("   → Could improve by adding qualification earlier");
    }
}
```

**Key Takeaway**: A well-crafted nurture sequence converts 10-15% of leads to trials. The magic is in the progression: Problem → Framework → Proof → Objection Handling → Urgency.

---

## Part 3: Webinar & Event Marketing

**Why Webinars Work for B2B SaaS**

Webinars are one of the highest-converting demand gen channels:
- **30-50% registration → attendance rate** (vs 2-5% for cold email)
- **20-30% attendee → MQL rate** (high engagement signal)
- **10-15% MQL → SQL rate** (qualified by self-selection)

The reason: Webinars combine education (valuable content) with interaction (Q&A, polls, chat), building trust and authority in 45 minutes.

**The Webinar Formula**

**1. Topic Selection** (most important decision)

Bad topics:
- "Introduction to Bibby" (too product-focused)
- "How to Read More Books" (too generic, no urgency)

Good topics:
- "How to Build a Second Brain from Your Reading (Live Workshop)"
- "The Knowledge Retention System Used by Top Performers"
- "From 200 Books to Zero Recall: Fixing Your Reading System"

**Rule**: The topic should solve a specific problem your ICP has, with your product as the natural solution (but not the focus).

**2. Webinar Structure** (45-60 minutes total)

```
00:00-05:00  Welcome & Hook
             - Introduce yourself
             - State the problem clearly
             - Promise the solution
             - Set expectations

05:00-35:00  Teaching/Demo
             - Framework/concept (10 min)
             - Live demonstration (15 min)
             - Deep dive into one tactic (10 min)

35:00-50:00  Q&A
             - Answer pre-submitted questions
             - Live questions from chat
             - Transition to offer

50:00-55:00  The Offer
             - Recap what you covered
             - Present CTA (trial, demo, download)
             - Exclusive bonus for attendees
             - Show how to take next step

55:00-60:00  Wrap-up
             - Thank attendees
             - Remind of replay availability
             - One more CTA reminder
```

**3. Example: "Build a Second Brain from Your Reading" Webinar**

**Hook** (0-5 min):
```
"Quick poll: How many books have you read in the past year?"
[Attendees answer in chat: 10... 25... 40... 60]

"Great! Now, how many of those books can you recall specific insights from
right now, without looking them up?"

[Awkward silence, few answers]

"Exactly. Most people read constantly but remember almost nothing. The average
retention rate is 10-20%. If you read 30 books last year, you retained insights
from maybe 3-6 of them.

That's not a reading problem. That's a SYSTEM problem.

Today, I'm going to show you the exact system I use to capture, organize, and
retrieve insights from every book I read—and how you can build the same system
in 30 minutes."
```

**Teaching** (5-35 min):

**Part 1: The Framework** (5-15 min)
- The 3 components: Capture, Organize, Retrieve
- Why most systems fail (optimize for input, not output)
- The "retrieval practice" principle

**Part 2: Live Demo** (15-30 min)
- Show your own knowledge system (screenshare Bibby CLI)
- Walk through: Adding a book, capturing notes, searching, retrieving
- Demonstrate real search: "Show me everything I've learned about pricing"
- Highlight how fast it is (CLI = instant)

**Part 3: Deep Dive** (30-35 min)
- The note-taking template: Key ideas, Actionable insights, Related concepts
- How to organize by themes, not books
- Setting up spaced retrieval reminders

**Q&A** (35-50 min):
- "How is this different from Notion?" → Optimized for speed, CLI, no setup overhead
- "Do I need to be technical?" → No, basic command line is enough
- "How long does setup take?" → 5 minutes to install, 30 minutes to import library

**The Offer** (50-55 min):
```
"Alright, here's what I want you to do:

I've created a 'Second Brain Starter Kit' that includes:
✓ The note-taking template we used today
✓ A 30-minute quick-start guide
✓ My personal CLI cheat sheet (10 most-used commands)
✓ Bonus: 1:1 setup call with me (first 20 people only)

Normally I charge $50 for this kit. But since you showed up live today, it's
free when you start a Bibby trial.

Here's the link: [LINK]

No credit card required. 14-day free trial. If it doesn't work for you, no hard feelings.

But if you're serious about retaining what you read, this is the system.

Who's ready to try it? Drop a '✓' in the chat if you're signing up."
```

**4. Webinar Promotion Strategy**

You need attendees. Here's how to fill seats:

**4 Weeks Before**:
- Create landing page with registration form
- Set up email sequence for registrants (3 reminders)
- Design promotional graphics

**3 Weeks Before**:
- Email your list (existing leads, customers)
- Post on social (LinkedIn, Twitter, relevant communities)
- Partner with complementary tools (cross-promote)

**2 Weeks Before**:
- Run paid ads (LinkedIn, Google, Reddit)
- Reach out to influencers/communities for promotion
- Send "Last Chance to Register" email

**1 Week Before**:
- Email registrants: "Webinar is this Thursday!"
- Post countdown on social
- Prep Q&A doc with common questions

**Day Before**:
- Email: "Tomorrow at 2pm PT: Build Your Second Brain"
- Test tech setup (Zoom, slides, demo environment)

**Day Of**:
- Email: "Starting in 1 hour"
- Post live link on social
- Join 15 minutes early for tech check

**After Webinar**:
- Send replay to registrants (including no-shows)
- Send thank-you email to attendees with resources
- Follow up with non-converters (nurture sequence)

**5. Webinar Funnel Metrics**

```java
@Service
public class WebinarAnalytics {

    public record WebinarMetrics(
        String webinarTitle,
        LocalDateTime date,
        int landingPageViews,
        int registrations,
        double registrationRate,
        int attendees,
        double attendanceRate,
        int mqls,
        double mqlRate,
        int trials,
        int customers,
        double cost,
        double costPerMql,
        double costPerCustomer,
        double roi
    ) {}

    public void analyzeWebinar() {
        WebinarMetrics webinar = new WebinarMetrics(
            "Build a Second Brain from Your Reading",
            LocalDateTime.of(2025, 3, 15, 14, 0),
            2000,   // Landing page views (from ads + email + social)
            500,    // Registrations (25% conversion - good for webinar)
            25.0,
            225,    // Attendees (45% show-up rate - excellent)
            45.0,
            157,    // MQLs (70% of attendees - very high intent)
            70.0,
            47,     // Trials started (30% of MQLs - strong)
            12,     // Customers (25% of trials - great for webinar leads)
            2000,   // Total cost (ads $1500, tools $200, time $300)
            12.74,  // Cost per MQL
            166.67, // Cost per customer
            1.8     // ROI (12 customers × $300 LTV = $3,600 / $2,000 cost)
        );

        System.out.println("=== Webinar Performance Report ===\n");
        System.out.println("Webinar: " + webinar.webinarTitle());
        System.out.println("Date: " + webinar.date());
        System.out.println();

        System.out.println("📊 Funnel Breakdown:");
        System.out.println(webinar.landingPageViews() + " landing page views");
        System.out.println("  ↓ " + String.format("%.1f%%", webinar.registrationRate()));
        System.out.println(webinar.registrations() + " registrations");
        System.out.println("  ↓ " + String.format("%.1f%%", webinar.attendanceRate()));
        System.out.println(webinar.attendees() + " attendees");
        System.out.println("  ↓ " + String.format("%.1f%%", webinar.mqlRate()));
        System.out.println(webinar.mqls() + " MQLs (started trial or booked demo)");
        System.out.println("  ↓");
        System.out.println(webinar.trials() + " trials started");
        System.out.println("  ↓");
        System.out.println(webinar.customers() + " customers");
        System.out.println();

        System.out.println("💰 Economics:");
        System.out.println("• Total Cost: $" + String.format("%.0f", webinar.cost()));
        System.out.println("• Cost per MQL: $" + String.format("%.2f", webinar.costPerMql()));
        System.out.println("• Cost per Customer: $" + String.format("%.2f", webinar.costPerCustomer()));
        System.out.println("• Revenue: $" + (webinar.customers() * 300) + " (LTV)");
        System.out.println("• ROI: " + String.format("%.1f", webinar.roi()) + "×");
        System.out.println();

        System.out.println("💡 Insights:");
        System.out.println("1. 45% attendance rate is exceptional (industry avg: 30-35%)");
        System.out.println("   → Great topic selection + effective reminders");
        System.out.println("2. 70% of attendees became MQLs");
        System.out.println("   → Webinar content strongly aligned with product value");
        System.out.println("3. 25% trial → customer rate is higher than email nurture (20%)");
        System.out.println("   → Live interaction builds trust faster");
        System.out.println("4. ROI is positive but lower than bottom-funnel magnets");
        System.out.println("   → Webinars are resource-intensive; good for brand + pipeline");
    }
}
```

**Key Insight**: Webinars have lower ROI than automated funnels, but higher absolute pipeline and stronger brand building. Use quarterly for big pushes.

---

## Part 4: Retargeting & Remarketing

**The Problem: 95% of Visitors Don't Convert**

Typical SaaS website:
- 10,000 monthly visitors
- 500 sign up for trial (5%)
- 9,500 leave forever (95%)

Retargeting brings that 95% back.

**What is Retargeting?**

Retargeting = Showing ads to people who visited your site but didn't convert.

**Example flows**:
1. Someone reads your blog post → Leaves → Sees ad for related ebook → Downloads → Enters nurture sequence
2. Someone starts trial signup → Abandons form → Sees ad: "Finish Your Signup in 30 Seconds" → Returns → Completes signup
3. Someone visits pricing page → Leaves → Sees ad: "See Bibby in Action (2-Min Demo)" → Clicks → Watches demo → Starts trial

**Retargeting Segments**

Not all visitors are equal. Segment by intent:

```
HIGH INTENT:
├─ Visited pricing page (didn't sign up)
├─ Started trial signup (abandoned form)
├─ Watched demo video (didn't start trial)
└─ Searched for "[competitor] vs Bibby"

MEDIUM INTENT:
├─ Read 3+ blog posts
├─ Downloaded lead magnet
├─ Visited product/features page
└─ Spent 5+ minutes on site

LOW INTENT:
├─ Single blog post visit (bounced)
├─ Visited homepage only
└─ Spent <30 seconds on site
```

Match ad spend to intent. High-intent visitors are worth 10× more, so bid 10× more.

**Retargeting Ad Examples**

**For "Abandoned Trial Signup" Segment**:
```
Headline: Forgot to finish signing up for Bibby?
Body: No credit card required. Start your 14-day free trial in 30 seconds.
CTA: Complete Signup →
Visual: Screenshot of Bibby CLI in action
```

**For "Visited Pricing Page" Segment**:
```
Headline: See Bibby in Action (2-Minute Demo)
Body: See how developers are building second brains from their reading.
CTA: Watch Demo →
Visual: Demo video thumbnail
```

**For "Downloaded PKM Guide" Segment**:
```
Headline: Loved the PKM guide? Try the system.
Body: Bibby is the CLI tool that makes the guide actionable. Free for 14 days.
CTA: Start Free Trial →
Visual: Person using terminal
```

**Retargeting Economics**

```java
@Service
public class RetargetingAnalytics {

    public record RetargetingCampaign(
        String segmentName,
        int audienceSize,
        double bidPerClick,
        int impressions,
        int clicks,
        double ctr,
        int conversions,
        double conversionRate,
        double spend,
        double costPerConversion,
        double roi
    ) {}

    public void analyzeRetargetingCampaigns() {
        System.out.println("=== Retargeting Campaign Performance (March 2025) ===\n");

        // High-intent segment: Abandoned trial signups
        var abandonedTrial = new RetargetingCampaign(
            "Abandoned Trial Signup",
            1200,    // People who started signup but didn't complete
            2.50,    // Bid per click (high intent = higher bid)
            36000,   // Impressions (30 per person)
            480,     // Clicks (1.3% CTR - good for retargeting)
            1.3,
            144,     // Conversions (30% - very high because they already showed intent)
            30.0,
            1200,    // Total spend
            8.33,    // Cost per conversion
            5.4      // ROI (144 customers × $300 LTV / $1200 spend)
        );

        // Medium-intent: Visited pricing page
        var pricingVisitors = new RetargetingCampaign(
            "Pricing Page Visitors",
            3500,    // Visited pricing but didn't sign up
            1.50,    // Lower bid than abandoned signup
            105000,  // Impressions
            1050,    // Clicks (1% CTR)
            1.0,
            105,     // Conversions (10% - lower than abandoned trial)
            10.0,
            1575,    // Total spend
            15.00,   // Cost per conversion
            19.0     // ROI (105 × $300 / $1575) - excellent
        );

        // Low-intent: Blog readers (1 post)
        var blogReaders = new RetargetingCampaign(
            "Blog Readers (Single Visit)",
            15000,   // Lots of volume, low intent
            0.50,    // Low bid
            300000,  // Impressions
            1500,    // Clicks (0.5% CTR - low)
            0.5,
            45,      // Conversions (3% - very low)
            3.0,
            750,     // Total spend
            16.67,   // Cost per conversion
            18.0     // ROI (45 × $300 / $750)
        );

        printCampaign(abandonedTrial);
        printCampaign(pricingVisitors);
        printCampaign(blogReaders);

        System.out.println("\n💡 Insights:");
        System.out.println("1. Abandoned trial segment has highest conversion rate (30%)");
        System.out.println("   → These people already decided to try Bibby, just didn't finish");
        System.out.println("   → Simple friction removal (ad reminds them) drives conversion");
        System.out.println("2. Pricing visitors have best ROI (19×)");
        System.out.println("   → High intent + larger audience = scalable");
        System.out.println("   → Opportunity: Double budget on this segment");
        System.out.println("3. Blog readers are marginal but still profitable");
        System.out.println("   → Keep running but cap budget");
        System.out.println("   → Consider filtering to 3+ post readers only");
    }

    private void printCampaign(RetargetingCampaign c) {
        System.out.println(c.segmentName() + ":");
        System.out.println("  Audience: " + c.audienceSize() + " people");
        System.out.println("  Funnel: " + c.impressions() + " imp → " + c.clicks() +
                          " clicks (" + String.format("%.1f%%", c.ctr()) + " CTR) → " +
                          c.conversions() + " conv (" +
                          String.format("%.1f%%", c.conversionRate()) + " rate)");
        System.out.println("  Economics: $" + String.format("%.2f", c.spend()) +
                          " spent, $" + String.format("%.2f", c.costPerConversion()) +
                          " per conv, " + String.format("%.1f", c.roi()) + "× ROI");
        System.out.println();
    }
}
```

**Retargeting Best Practices**

1. **Frequency Cap**: Don't burn out your audience. Show the same ad max 3-5 times per week.
2. **Exclude Converters**: Once someone signs up, remove them from retargeting immediately.
3. **Recency Matters**: Retarget within 30 days of visit (intent decays fast).
4. **Sequential Messaging**: Show different ads over time (awareness → consideration → decision).
5. **Test Creative**: Rotate 3-5 ad variations to find what resonates.

---

## Part 5: Marketing Automation

**What is Marketing Automation?**

Marketing automation = Using software to automatically perform marketing tasks based on triggers and conditions.

**Without automation**:
- Someone downloads ebook → You manually add to email list → You manually send welcome email → You manually check if they opened it → Repeat for 500 people

**With automation**:
- Someone downloads ebook → Automatically added to list → Welcome email sends automatically → If opened, send follow-up #2 in 2 days; if not, send reminder in 1 day → Continues for 500 people without manual work

**Marketing Automation Use Cases**

1. **Lead Nurture Sequences** (covered in Part 2)
2. **Lead Scoring** (covered in Part 6)
3. **Behavioral Triggers**
4. **Lifecycle Campaigns**
5. **Re-engagement Campaigns**

**Behavioral Trigger Example: Trial Activation**

```
TRIGGER: User signs up for trial but doesn't import any books (Day 1)
ACTION: Send email: "Getting Started with Bibby (3-Minute Setup)"

TRIGGER: User imports books but doesn't add any notes (Day 3)
ACTION: Send email: "How to Capture Your First Insight"

TRIGGER: User adds notes but hasn't searched yet (Day 5)
ACTION: Send email: "The Power of Instant Retrieval"

TRIGGER: User has searched 5+ times (Day 7)
ACTION: Tag as "Activated User", send email: "You're getting it! Here are advanced tips"
```

This sequence is personalized based on behavior, not a one-size-fits-all timeline.

**Implementation: Behavioral Automation**

```java
package com.bibby.marketing;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MarketingAutomationService {

    public enum UserAction {
        TRIAL_SIGNUP,
        IMPORTED_BOOKS,
        ADDED_NOTE,
        PERFORMED_SEARCH,
        CREATED_COLLECTION,
        EXPORTED_DATA,
        UPGRADED_TO_PAID
    }

    public record AutomationRule(
        String ruleId,
        String name,
        String trigger,         // What starts this rule
        List<String> conditions, // What must be true
        String action,          // What to do
        int delayHours          // How long to wait before action
    ) {}

    public record UserBehavior(
        String email,
        LocalDateTime trialStartDate,
        int booksImported,
        int notesAdded,
        int searchesPerformed,
        int collectionsCreated,
        boolean hasExported,
        boolean isPaid,
        List<UserAction> actionHistory,
        LocalDateTime lastActive
    ) {}

    /**
     * Check all automation rules and trigger actions
     */
    public void processAutomation(UserBehavior user) {
        List<AutomationRule> rules = getAutomationRules();

        for (AutomationRule rule : rules) {
            if (shouldTriggerRule(rule, user)) {
                executeAction(rule, user);
            }
        }
    }

    private boolean shouldTriggerRule(AutomationRule rule, UserBehavior user) {
        return switch (rule.trigger()) {
            case "trial_signup_no_import" -> {
                long daysSinceSignup = ChronoUnit.DAYS.between(
                    user.trialStartDate(), LocalDateTime.now());
                yield daysSinceSignup >= 1 && user.booksImported() == 0;
            }
            case "imported_no_notes" -> {
                long daysSinceSignup = ChronoUnit.DAYS.between(
                    user.trialStartDate(), LocalDateTime.now());
                yield daysSinceSignup >= 3 && user.booksImported() > 0 &&
                      user.notesAdded() == 0;
            }
            case "notes_no_search" -> {
                long daysSinceSignup = ChronoUnit.DAYS.between(
                    user.trialStartDate(), LocalDateTime.now());
                yield daysSinceSignup >= 5 && user.notesAdded() > 0 &&
                      user.searchesPerformed() == 0;
            }
            case "activated_user" -> {
                yield user.searchesPerformed() >= 5 && user.notesAdded() >= 10;
            }
            case "trial_ending_not_activated" -> {
                long daysSinceSignup = ChronoUnit.DAYS.between(
                    user.trialStartDate(), LocalDateTime.now());
                yield daysSinceSignup >= 12 && user.searchesPerformed() < 5 &&
                      !user.isPaid();
            }
            default -> false;
        };
    }

    private void executeAction(AutomationRule rule, UserBehavior user) {
        System.out.println("🤖 AUTOMATION TRIGGERED");
        System.out.println("   Rule: " + rule.name());
        System.out.println("   User: " + user.email());
        System.out.println("   Action: " + rule.action());
        System.out.println("   Delay: " + rule.delayHours() + " hours");

        // In reality, this would:
        // - Send email via email service
        // - Create task for sales team
        // - Update user tags/segments
        // - Log the automation event
    }

    private List<AutomationRule> getAutomationRules() {
        return List.of(
            new AutomationRule(
                "rule-001",
                "Trial Signup - No Import (Day 1)",
                "trial_signup_no_import",
                List.of("Days since signup >= 1", "Books imported = 0"),
                "Send email: 'Getting Started with Bibby (3-Minute Setup)'",
                0
            ),
            new AutomationRule(
                "rule-002",
                "Imported Books - No Notes (Day 3)",
                "imported_no_notes",
                List.of("Days since signup >= 3", "Books imported > 0", "Notes = 0"),
                "Send email: 'How to Capture Your First Insight'",
                0
            ),
            new AutomationRule(
                "rule-003",
                "Has Notes - No Search (Day 5)",
                "notes_no_search",
                List.of("Days since signup >= 5", "Notes > 0", "Searches = 0"),
                "Send email: 'The Power of Instant Retrieval'",
                0
            ),
            new AutomationRule(
                "rule-004",
                "Activated User (5+ Searches)",
                "activated_user",
                List.of("Searches >= 5", "Notes >= 10"),
                "Tag as 'Activated', send email with advanced tips, notify sales",
                0
            ),
            new AutomationRule(
                "rule-005",
                "Trial Ending - Not Activated (Day 12)",
                "trial_ending_not_activated",
                List.of("Days since signup >= 12", "Searches < 5", "Not paid"),
                "Send email: 'Need help? Book 1:1 onboarding call'",
                0
            )
        );
    }
}
```

**Lifecycle Campaign Example: Re-engagement**

Target: Users who were active but haven't logged in for 30 days

```java
@Service
public class ReengagementCampaign {

    public record InactiveUser(
        String email,
        LocalDateTime lastActive,
        int daysSinceActive,
        String userTier,        // free, trial, paid
        int totalSearches,
        int booksInLibrary
    ) {}

    public void triggerReengagement(InactiveUser user) {
        if (user.daysSinceActive() == 30) {
            sendEmail(user, "We miss you! Here's what's new in Bibby");
        } else if (user.daysSinceActive() == 45) {
            sendEmail(user, "Your library is waiting: " + user.booksInLibrary() + " books ready to search");
        } else if (user.daysSinceActive() == 60 && user.userTier().equals("paid")) {
            sendEmail(user, "Still using Bibby? Let's make sure you're getting value");
            notifyCustomerSuccess(user, "At-risk customer: 60 days inactive");
        }
    }

    private void sendEmail(InactiveUser user, String subject) {
        System.out.println("📧 Re-engagement: " + user.email() + " - " + subject);
    }

    private void notifyCustomerSuccess(InactiveUser user, String reason) {
        System.out.println("🚨 CS Alert: " + reason + " - " + user.email());
    }
}
```

**Key Insight**: Automation doesn't replace strategy—it scales it. Bad emails automated are just bad emails at scale.

---

## Part 6: Lead Scoring & Qualification

**The Problem: Not All Leads Are Equal**

You generate 1,000 leads per month. Your sales team can only handle 200 conversations. How do you decide who to call first?

Answer: **Lead scoring**.

**What is Lead Scoring?**

Lead scoring assigns points to each lead based on:
1. **Demographic fit** (job title, company size, industry)
2. **Behavioral engagement** (email opens, website visits, feature usage)

**Example Scoring Model**

**Demographic Points** (out of 50):
- **Job Title**:
  - Software Engineer, Tech Lead: +15
  - Engineering Manager, CTO: +20
  - Product Manager, Designer: +10
  - Student: +5
  - Other: +0

- **Company Size**:
  - 1-10 employees (startup): +10
  - 11-50: +15
  - 51-200: +10
  - 201+: +5

- **Industry**:
  - Tech, SaaS, Consulting: +15
  - Education, Research: +10
  - Other: +5

**Behavioral Points** (out of 50):
- Downloaded lead magnet: +5
- Opened 3+ emails: +10
- Clicked email link: +5
- Visited pricing page: +15
- Watched demo video: +10
- Started trial: +20
- Used product 3+ days: +15
- Performed 5+ searches: +10
- Replied to email: +10

**Total Score**:
- **80-100**: A - Hot Lead (call immediately)
- **60-79**: B - Warm Lead (prioritize)
- **40-59**: C - Cool Lead (nurture)
- **0-39**: D - Cold Lead (long-term nurture or disqualify)

**Implementation**

```java
package com.bibby.marketing;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class LeadScoringService {

    public enum JobTitle {
        SOFTWARE_ENGINEER(15),
        ENGINEERING_MANAGER(20),
        CTO(20),
        PRODUCT_MANAGER(10),
        DESIGNER(10),
        STUDENT(5),
        OTHER(0);

        public final int points;
        JobTitle(int points) { this.points = points; }
    }

    public enum CompanySize {
        MICRO_1_10(10),
        SMALL_11_50(15),
        MEDIUM_51_200(10),
        LARGE_201_PLUS(5);

        public final int points;
        CompanySize(int points) { this.points = points; }
    }

    public enum Industry {
        TECH_SAAS(15),
        EDUCATION_RESEARCH(10),
        OTHER(5);

        public final int points;
        Industry(int points) { this.points = points; }
    }

    public record LeadDemographics(
        JobTitle jobTitle,
        CompanySize companySize,
        Industry industry
    ) {
        public int demographicScore() {
            return jobTitle.points + companySize.points + industry.points;
        }
    }

    public record LeadBehavior(
        boolean downloadedLeadMagnet,
        int emailsOpened,
        boolean clickedEmailLink,
        boolean visitedPricingPage,
        boolean watchedDemoVideo,
        boolean startedTrial,
        int daysActive,
        int searchesPerformed,
        boolean repliedToEmail
    ) {
        public int behavioralScore() {
            int score = 0;
            if (downloadedLeadMagnet) score += 5;
            if (emailsOpened >= 3) score += 10;
            if (clickedEmailLink) score += 5;
            if (visitedPricingPage) score += 15;
            if (watchedDemoVideo) score += 10;
            if (startedTrial) score += 20;
            if (daysActive >= 3) score += 15;
            if (searchesPerformed >= 5) score += 10;
            if (repliedToEmail) score += 10;
            return Math.min(score, 50); // Cap at 50
        }
    }

    public record ScoredLead(
        String email,
        String name,
        LeadDemographics demographics,
        LeadBehavior behavior,
        int totalScore,
        String grade,
        String recommendation
    ) {}

    public ScoredLead scoreLead(String email, String name,
                                LeadDemographics demographics,
                                LeadBehavior behavior) {

        int demoScore = demographics.demographicScore();
        int behavScore = behavior.behavioralScore();
        int total = demoScore + behavScore;

        String grade;
        String recommendation;

        if (total >= 80) {
            grade = "A - Hot";
            recommendation = "CALL IMMEDIATELY - High fit + high engagement";
        } else if (total >= 60) {
            grade = "B - Warm";
            recommendation = "Prioritize for outreach within 24 hours";
        } else if (total >= 40) {
            grade = "C - Cool";
            recommendation = "Continue nurturing, check back in 7 days";
        } else {
            grade = "D - Cold";
            recommendation = "Long-term nurture or disqualify";
        }

        return new ScoredLead(email, name, demographics, behavior, total,
                             grade, recommendation);
    }

    public void printLeadScoreReport() {
        System.out.println("=== Lead Scoring Report ===\n");

        // Example: 5 leads with different profiles
        var lead1 = scoreLead(
            "alex@startup.com",
            "Alex Chen",
            new LeadDemographics(JobTitle.CTO, CompanySize.SMALL_11_50,
                                Industry.TECH_SAAS),
            new LeadBehavior(true, 5, true, true, true, true, 5, 8, true)
        );

        var lead2 = scoreLead(
            "jamie@corp.com",
            "Jamie Rodriguez",
            new LeadDemographics(JobTitle.SOFTWARE_ENGINEER,
                                CompanySize.LARGE_201_PLUS, Industry.OTHER),
            new LeadBehavior(true, 2, false, false, false, false, 0, 0, false)
        );

        var lead3 = scoreLead(
            "sam@consulting.com",
            "Sam Patel",
            new LeadDemographics(JobTitle.PRODUCT_MANAGER,
                                CompanySize.MEDIUM_51_200, Industry.TECH_SAAS),
            new LeadBehavior(true, 4, true, true, false, true, 2, 3, false)
        );

        var lead4 = scoreLead(
            "riley@university.edu",
            "Riley Kim",
            new LeadDemographics(JobTitle.STUDENT, CompanySize.MICRO_1_10,
                                Industry.EDUCATION_RESEARCH),
            new LeadBehavior(true, 6, true, false, true, true, 4, 12, true)
        );

        var lead5 = scoreLead(
            "morgan@agency.com",
            "Morgan Taylor",
            new LeadDemographics(JobTitle.DESIGNER, CompanySize.MICRO_1_10,
                                Industry.OTHER),
            new LeadBehavior(true, 1, false, false, false, false, 0, 0, false)
        );

        List<ScoredLead> leads = List.of(lead1, lead2, lead3, lead4, lead5);

        // Sort by score (highest first)
        leads.stream()
            .sorted((a, b) -> Integer.compare(b.totalScore(), a.totalScore()))
            .forEach(this::printLead);

        System.out.println("\n💡 Sales Priority:");
        System.out.println("1. Alex Chen (95 pts) - Call NOW");
        System.out.println("2. Riley Kim (70 pts) - Call today");
        System.out.println("3. Sam Patel (62 pts) - Email to schedule call");
        System.out.println("4. Jamie Rodriguez (35 pts) - Keep nurturing");
        System.out.println("5. Morgan Taylor (20 pts) - Consider disqualifying");
    }

    private void printLead(ScoredLead lead) {
        System.out.println(lead.name() + " (" + lead.email() + ")");
        System.out.println("  Demographic: " + lead.demographics().demographicScore() +
                          " pts (" + lead.demographics().jobTitle() + ", " +
                          lead.demographics().companySize() + ", " +
                          lead.demographics().industry() + ")");
        System.out.println("  Behavioral: " + lead.behavior().behavioralScore() + " pts");
        System.out.println("  TOTAL: " + lead.totalScore() + " pts → " + lead.grade());
        System.out.println("  Action: " + lead.recommendation());
        System.out.println();
    }
}
```

**Output**:
```
=== Lead Scoring Report ===

Alex Chen (alex@startup.com)
  Demographic: 50 pts (CTO, SMALL_11_50, TECH_SAAS)
  Behavioral: 45 pts
  TOTAL: 95 pts → A - Hot
  Action: CALL IMMEDIATELY - High fit + high engagement

Riley Kim (riley@university.edu)
  Demographic: 20 pts (STUDENT, MICRO_1_10, EDUCATION_RESEARCH)
  Behavioral: 50 pts
  TOTAL: 70 pts → B - Warm
  Action: Prioritize for outreach within 24 hours

Sam Patel (sam@consulting.com)
  Demographic: 35 pts (PRODUCT_MANAGER, MEDIUM_51_200, TECH_SAAS)
  Behavioral: 27 pts
  TOTAL: 62 pts → B - Warm
  Action: Prioritize for outreach within 24 hours

Jamie Rodriguez (jamie@corp.com)
  Demographic: 20 pts (SOFTWARE_ENGINEER, LARGE_201_PLUS, OTHER)
  Behavioral: 15 pts
  TOTAL: 35 pts → D - Cold
  Action: Long-term nurture or disqualify

Morgan Taylor (morgan@agency.com)
  Demographic: 15 pts (DESIGNER, MICRO_1_10, OTHER)
  Behavioral: 5 pts
  TOTAL: 20 pts → D - Cold
  Action: Long-term nurture or disqualify
```

**MQL Qualification Criteria**

Not every lead is an MQL. Use score + fit:

**Minimum criteria for MQL**:
1. Score ≥ 60 (B grade or higher)
2. AND matches ICP (job title, industry)
3. AND shown intent (trial signup, pricing page visit, or demo request)

**SQL Qualification**

MQLs become SQLs when sales verifies:
1. Budget: Can they afford $100/year?
2. Authority: Can they make purchase decision?
3. Need: Do they have the problem Bibby solves?
4. Timeline: When do they need a solution?

This is **BANT qualification** (Budget, Authority, Need, Timeline).

---

## Week 28 Practical Assignment

**Objective**: Build a complete demand generation system for Bibby that generates qualified pipeline at predictable cost.

**Assignment 1: Lead Magnet Strategy**

Create 3 lead magnets (one for each funnel stage):
1. **Top of Funnel**: Awareness-stage offer (ebook, guide, template)
2. **Middle of Funnel**: Consideration-stage offer (email course, webinar, comparison guide)
3. **Bottom of Funnel**: Decision-stage offer (calculator, assessment, trial bonus)

**Deliverables**:
- Lead magnet titles and descriptions
- Landing page outlines (headline, subhead, bullets, CTA)
- Estimated conversion rates and cost per lead
- Follow-up nurture sequence (1-2 sentences per email)

**Assignment 2: Email Nurture Sequence**

Design a 5-email nurture sequence for one of your lead magnets.

**Deliverables**:
- Email subject lines
- Email purpose (what each email accomplishes)
- CTAs for each email
- Send schedule (Day 0, Day 2, Day 5, etc.)
- Expected open rates, click rates, conversion rate

**Assignment 3: Webinar Plan**

Plan a 60-minute educational webinar that positions Bibby as the solution.

**Deliverables**:
- Webinar title and description
- Target audience
- Outline (5-10 key sections with timestamps)
- The "offer" slide (what you're asking attendees to do)
- Promotion plan (how you'll get 100+ registrants)

**Assignment 4: Retargeting Strategy**

Design a retargeting campaign for high-intent visitors.

**Deliverables**:
- 3 retargeting segments (with audience size estimates)
- Ad creative for each segment (headline, body, CTA, visual)
- Bid strategy (how much you'll pay per click for each segment)
- Expected conversions and ROI

**Assignment 5: Lead Scoring Model**

Build a lead scoring model for Bibby using the `LeadScoringService` code above.

**Deliverables**:
- Demographic scoring criteria (adjusted for Bibby ICP)
- Behavioral scoring criteria (actions that signal intent)
- Grade thresholds (A/B/C/D score ranges)
- Sales handoff criteria (when does marketing pass to sales?)

**Assignment 6: Marketing Automation Workflows**

Design 3 automated workflows:
1. Trial activation workflow (behavioral triggers based on product usage)
2. Re-engagement workflow (for inactive users)
3. Upgrade workflow (moving free users to paid)

**Deliverables**:
- Trigger conditions for each workflow
- Email/action sequence
- Expected impact (e.g., "increase trial → paid by 10%")

**Stretch Goal**: Implement one of the workflows in code using the `MarketingAutomationService` above.

---

## Reflection Questions

1. **Lead Magnet Quality vs Quantity**: Would you rather have 1,000 low-quality leads or 100 high-quality leads? Why? How does this impact your lead magnet strategy?

2. **Nurture Timing**: The example sequence sends 7 emails over 14 days. Is that too fast? Too slow? How would you decide the right cadence for Bibby?

3. **Webinar ROI**: Webinars are time-intensive (10-20 hours prep + delivery + follow-up). Given that ROI is lower than automated funnels, when would you use webinars?

4. **Retargeting Ethics**: Is it creepy to follow people around the internet with ads? Where's the line between helpful and invasive?

5. **Automation vs Personalization**: Marketing automation scales efficiency but can feel robotic. How do you balance automation with genuine, personal connection?

6. **Lead Scoring Bias**: If your scoring model prioritizes CTOs at tech startups, you'll miss individual engineers at big companies. How do you avoid over-optimizing for one segment?

7. **MQL → SQL Conversion**: If only 30-40% of your MQLs convert to SQLs, that means 60-70% don't. Is that acceptable? How would you improve it?

8. **Demand Gen vs Brand**: This week focused on measurable, performance-driven demand gen. But brand awareness (podcasts, conferences, thought leadership) is harder to measure. How much would you invest in brand vs demand gen?

---

## Key Takeaways

1. **Demand generation is a system, not a tactic**: Lead magnet → Nurture → Qualification → Sales handoff. Each part must work together.

2. **Bottom-of-funnel magnets have higher ROI than top-of-funnel**: Calculators, comparisons, and assessments attract buyers. Ebooks attract browsers.

3. **Email nurture converts 10-15% of leads to trials**: The key is progressive value (teach → prove → offer) and respecting the buyer's timeline.

4. **Webinars build trust faster than email**: Live interaction drives higher conversion rates but requires more resources.

5. **Retargeting brings back 95% of visitors who left**: Segment by intent and match ad spend to likelihood of conversion.

6. **Marketing automation scales personalization**: Behavioral triggers enable "right message, right time" without manual work.

7. **Lead scoring aligns marketing and sales**: A shared definition of "qualified" prevents wasted sales effort on bad-fit leads.

8. **Not all leads are created equal**: A CTO at a 20-person startup who visited your pricing page is worth 100× more than a student who read one blog post.

---

## What's Next?

You now have a demand generation machine that systematically turns awareness into qualified pipeline. But pipeline isn't revenue—you still need to close deals.

**Next week: Sales Processes & Methodologies**

You'll learn how to:
- Design a repeatable sales process (from first touch to closed-won)
- Implement proven sales methodologies (MEDDIC, Sandler, Challenger)
- Run discovery calls that uncover true needs
- Handle objections and negotiate pricing
- Forecast revenue accurately
- Build a sales playbook that scales

Plus: How to close your first 10 Bibby customers without a sales team.

---

**Mentor Voice This Week**: **Business Analyst** (Systems thinking, process design, funnel optimization)

*"Demand generation isn't magic—it's math. Every lead magnet, email, and retargeting ad is an input that produces a predictable output. Your job is to instrument the system, measure what works, and double down on high-ROI channels. The companies that win aren't the ones with the flashiest campaigns—they're the ones with the tightest feedback loops."*

---

**Progress Check**: **28/52 weeks complete** (54% of total apprenticeship)

**Semesters**:
- ✅ Semester 1: Systems Thinking & Technical Foundation (Weeks 1-13)
- ✅ Semester 2: Metrics, Economics & Strategy (Weeks 14-26)
- 🔄 Semester 3: Marketing, Go-to-Market & Growth (Weeks 27-39) ← **You are here (Week 28)**
- ⏳ Semester 4: Execution, Revenue & Scale (Weeks 40-52)

You're past the halfway point. The next 11 weeks will transform you from someone who understands metrics and strategy into someone who can actually execute a complete go-to-market motion. Keep going—the hardest part is behind you.
