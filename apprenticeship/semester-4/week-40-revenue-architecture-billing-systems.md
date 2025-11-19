# Week 40: Revenue Architecture & Billing Systems

**Semester 4, Week 40 of 52**
**Focus: Execution, Revenue & Scale**

---

## Overview

Welcome to **Semester 4: Execution, Revenue & Scale**.

You've learned systems thinking, built technical foundations, and mastered go-to-market strategy. Now it's time to execute—to build the operational infrastructure that turns strategy into sustainable, scalable revenue.

This week focuses on **revenue architecture and billing systems**—the plumbing that makes money flow through your business.

Most engineers treat billing as an afterthought. They bolt on Stripe, write a few webhooks, and hope for the best. This works until it doesn't. Then you discover:

- Customers charged twice (or not at all)
- Failed payments you never retried
- Revenue recognition errors that anger your CFO
- Subscription states that drift out of sync
- Tax calculations that are completely wrong
- Invoices that don't match what customers paid

**Billing is deceptively complex.** A single subscription involves:
- Payment processing (capture, refunds, disputes)
- Subscription lifecycle (trial, active, past_due, canceled, paused)
- Proration calculations (upgrades, downgrades, mid-cycle changes)
- Tax calculation and remittance (sales tax, VAT, GST)
- Revenue recognition (accrual vs cash accounting)
- Dunning workflows (retry failed payments, notify customers)
- Invoicing and receipts
- Credit and refund handling
- Usage-based billing (metered vs tiered)
- Multi-currency support

This week, you'll learn how to build production-grade revenue architecture that scales from $0 to $100M ARR without breaking.

**By the end of this week, you will:**

- Understand subscription billing fundamentals and state machines
- Implement payment processing with Stripe
- Build subscription management workflows
- Handle proration, upgrades, and downgrades correctly
- Design dunning strategies that maximize recovery
- Implement revenue recognition and reporting
- Build invoicing and receipt generation
- Handle tax calculation and compliance
- Prevent revenue leakage and billing errors
- Apply revenue architecture to Bibby

**Week Structure:**
- **Part 1:** Revenue Architecture Fundamentals
- **Part 2:** Subscription Management & State Machines
- **Part 3:** Payment Processing with Stripe
- **Part 4:** Billing Workflows & Proration
- **Part 5:** Dunning & Failed Payment Recovery
- **Part 6:** Revenue Recognition & Reporting
- **Part 7:** Tax, Compliance & Invoicing
- **Part 8:** Building Bibby's Billing System

---

## Part 1: Revenue Architecture Fundamentals

### The Billing Stack

A complete billing system has 7 layers:

```
┌─────────────────────────────────────┐
│  7. Analytics & Reporting           │
│     (MRR, churn, cohorts)           │
├─────────────────────────────────────┤
│  6. Tax & Compliance                │
│     (sales tax, VAT, invoices)      │
├─────────────────────────────────────┤
│  5. Revenue Recognition             │
│     (accrual accounting, deferred)  │
├─────────────────────────────────────┤
│  4. Dunning & Recovery              │
│     (retry failed payments)         │
├─────────────────────────────────────┤
│  3. Subscription Management         │
│     (plans, upgrades, cancels)      │
├─────────────────────────────────────┤
│  2. Payment Processing              │
│     (Stripe, cards, ACH)            │
├─────────────────────────────────────┤
│  1. Customer & Account Management   │
│     (users, orgs, billing contacts) │
└─────────────────────────────────────┘
```

**Layer 1: Customer & Account Management**
- Who is being billed?
- Separate user identity from billing entity
- Support for organizations with multiple users

**Layer 2: Payment Processing**
- How do we collect money?
- Payment method storage (cards, ACH, wire)
- PCI compliance via payment processor (Stripe, Braintree)

**Layer 3: Subscription Management**
- What are they paying for?
- Plans, add-ons, usage tiers
- Lifecycle: trial → active → past_due → canceled

**Layer 4: Dunning & Recovery**
- What happens when payment fails?
- Retry schedules, customer notifications
- Maximize recovery, minimize churn

**Layer 5: Revenue Recognition**
- When do we recognize revenue?
- Deferred revenue for annual plans
- Revenue waterfall reporting

**Layer 6: Tax & Compliance**
- What taxes must we collect?
- Sales tax (US), VAT (EU), GST (other)
- Invoice generation and storage

**Layer 7: Analytics & Reporting**
- How is the business performing?
- MRR, ARR, churn, LTV
- Cohort analysis, revenue retention

### Build vs Buy vs Hybrid

**Build everything in-house:**
- ✅ Full control and customization
- ✅ No vendor lock-in
- ❌ Months of development time
- ❌ Ongoing maintenance burden
- ❌ Compliance risk (PCI, tax)
- **Best for:** Enterprise products with complex billing requirements

**Buy (use billing platform like Stripe Billing, Chargebee):**
- ✅ Fast to implement
- ✅ Handles compliance and edge cases
- ✅ Battle-tested infrastructure
- ❌ Limited customization
- ❌ Vendor lock-in
- ❌ Higher transaction fees
- **Best for:** Standard SaaS subscription models

**Hybrid (Stripe for payments + custom subscription logic):**
- ✅ Balance of control and speed
- ✅ Use Stripe for PCI compliance
- ✅ Custom logic for complex scenarios
- ❌ More code to maintain than pure "buy"
- ❌ Integration complexity
- **Best for:** Most SaaS products (recommended)

**This week, we'll build the hybrid approach.**

### Key Billing Concepts

**Subscription vs One-Time Payment:**
- **Subscription:** Recurring revenue, charged automatically (monthly/annually)
- **One-time:** Single charge, no recurrence (lifetime access, consulting)

**Billing Cycle:**
- The period between charges (monthly, quarterly, annual)
- Anchor date: When in month does billing occur?

**Proration:**
- Adjusting charges when customer changes plans mid-cycle
- Example: Upgrade from $10/mo to $20/mo on day 15
- Credit unused portion of old plan, charge new plan

**Trial Period:**
- Free access for limited time (7, 14, 30 days)
- Requires payment method upfront or at trial end?
- Convert to paid automatically or require manual action?

**Grace Period:**
- Time after failed payment before canceling service
- Allow customer to update payment method
- Typically 7-14 days

**MRR (Monthly Recurring Revenue):**
- Normalized monthly revenue from subscriptions
- Annual plan ($120/year) = $10 MRR
- Key metric for SaaS health

**ARR (Annual Recurring Revenue):**
- MRR × 12
- Used for larger businesses

**Churn:**
- % of customers who cancel
- Revenue churn vs customer churn
- Negative churn (expansion > contraction)

### Code Example: Core Billing Entities

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Customer entity - represents a billing account
 */
@Document(collection = "customers")
public record Customer(
    @Id String customerId,
    String email,
    String name,
    String stripeCustomerId,
    LocalDateTime createdAt,
    Map<String, String> metadata
) {}

/**
 * Subscription entity - represents an active subscription
 */
@Document(collection = "subscriptions")
public record Subscription(
    @Id String subscriptionId,
    String customerId,
    String planId,
    SubscriptionStatus status,
    LocalDateTime currentPeriodStart,
    LocalDateTime currentPeriodEnd,
    LocalDateTime trialEnd,
    LocalDateTime canceledAt,
    LocalDateTime cancelAt,  // Schedule cancellation for future
    boolean cancelAtPeriodEnd,
    String stripeSubscriptionId,
    Map<String, Object> metadata
) {
    public enum SubscriptionStatus {
        TRIALING,       // In trial period
        ACTIVE,         // Paid and active
        PAST_DUE,       // Payment failed, in grace period
        CANCELED,       // Canceled by user or system
        UNPAID,         // Payment failed beyond grace period
        PAUSED,         // Temporarily paused (feature for some products)
        INCOMPLETE      // Initial payment not yet succeeded
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE ||
               status == SubscriptionStatus.TRIALING;
    }

    public boolean needsPayment() {
        return status == SubscriptionStatus.PAST_DUE ||
               status == SubscriptionStatus.INCOMPLETE;
    }
}

/**
 * Billing plan - defines pricing and features
 */
@Document(collection = "plans")
public record BillingPlan(
    @Id String planId,
    String name,
    String description,
    long amountCents,      // Price in cents (e.g., 999 = $9.99)
    String currency,
    BillingInterval interval,
    int intervalCount,     // How many intervals between charges (1 = monthly, 12 = annually)
    int trialPeriodDays,
    Map<String, Object> features,
    boolean active
) {
    public enum BillingInterval {
        DAY, WEEK, MONTH, YEAR
    }

    public double monthlyEquivalent() {
        return switch (interval) {
            case DAY -> (amountCents / 100.0) * 30;
            case WEEK -> (amountCents / 100.0) * 4.33;
            case MONTH -> amountCents / 100.0;
            case YEAR -> (amountCents / 100.0) / 12;
        };
    }
}

/**
 * Invoice - represents a billing event
 */
@Document(collection = "invoices")
public record Invoice(
    @Id String invoiceId,
    String customerId,
    String subscriptionId,
    long amountCents,
    long taxCents,
    long totalCents,
    String currency,
    InvoiceStatus status,
    LocalDateTime periodStart,
    LocalDateTime periodEnd,
    LocalDateTime dueDate,
    LocalDateTime paidAt,
    String stripeInvoiceId,
    List<InvoiceLineItem> lineItems
) {
    public enum InvoiceStatus {
        DRAFT,
        OPEN,       // Sent to customer, awaiting payment
        PAID,
        VOID,       // Canceled
        UNCOLLECTIBLE  // Failed to collect after dunning
    }

    public record InvoiceLineItem(
        String description,
        long quantity,
        long unitAmountCents,
        long amountCents,
        LocalDateTime periodStart,
        LocalDateTime periodEnd
    ) {}
}

/**
 * Payment - represents a payment attempt
 */
@Document(collection = "payments")
public record Payment(
    @Id String paymentId,
    String customerId,
    String invoiceId,
    long amountCents,
    String currency,
    PaymentStatus status,
    PaymentMethod method,
    LocalDateTime createdAt,
    LocalDateTime succeededAt,
    LocalDateTime failedAt,
    String failureReason,
    String stripePaymentIntentId
) {
    public enum PaymentStatus {
        PENDING,
        SUCCEEDED,
        FAILED,
        REFUNDED,
        DISPUTED
    }

    public enum PaymentMethod {
        CARD,
        BANK_TRANSFER,
        ACH,
        WIRE,
        PAYPAL,
        OTHER
    }
}
```

---

## Part 2: Subscription Management & State Machines

### The Subscription Lifecycle

A subscription moves through states:

```
                    ┌──────────┐
                    │          │
              ┌────→│ TRIALING │────┐
              │     │          │    │
              │     └──────────┘    │
              │                     │ (trial ends)
              │                     ↓
    ┌─────────┴────┐         ┌──────────┐
    │              │         │          │
    │ INCOMPLETE   │────────→│  ACTIVE  │←───────┐
    │              │         │          │        │
    └──────────────┘         └────┬─────┘        │
         │                        │              │
         │ (payment fails)        │ (payment     │ (payment
         │                        │  fails)      │  succeeds)
         ↓                        ↓              │
    ┌──────────┐           ┌──────────┐         │
    │          │           │          │         │
    │  UNPAID  │           │ PAST_DUE │─────────┘
    │          │           │          │
    └──────────┘           └────┬─────┘
                                │
                                │ (grace period ends)
                                ↓
                          ┌──────────┐
                          │          │
                          │ CANCELED │
                          │          │
                          └──────────┘
```

**State descriptions:**

- **INCOMPLETE:** Subscription created, initial payment not yet succeeded
- **TRIALING:** In free trial period
- **ACTIVE:** Paid and working
- **PAST_DUE:** Payment failed, in grace period, retrying
- **UNPAID:** Payment failed beyond grace period, no retries
- **CANCELED:** Subscription ended (user or system initiated)
- **PAUSED:** Temporarily paused (optional feature, not all products support)

### State Transition Rules

**Allowed transitions:**

```
INCOMPLETE → ACTIVE (payment succeeds)
INCOMPLETE → UNPAID (payment fails, no retry)

TRIALING → ACTIVE (trial ends, payment succeeds)
TRIALING → PAST_DUE (trial ends, payment fails)
TRIALING → CANCELED (user cancels during trial)

ACTIVE → PAST_DUE (payment fails)
ACTIVE → CANCELED (user cancels)
ACTIVE → PAUSED (user pauses, if supported)

PAST_DUE → ACTIVE (payment retry succeeds)
PAST_DUE → UNPAID (all retries fail)
PAST_DUE → CANCELED (user cancels or grace period ends)

PAUSED → ACTIVE (user resumes)
PAUSED → CANCELED (user cancels)

UNPAID → ACTIVE (user updates payment method and pays)
UNPAID → CANCELED (final cancellation)
```

### Code Example: Subscription State Machine

```java
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SubscriptionStateMachine {

    /**
     * Transition subscription to new state with validation
     */
    public Subscription transition(
        Subscription subscription,
        Subscription.SubscriptionStatus newStatus,
        String reason
    ) {
        // Validate transition is allowed
        if (!isTransitionAllowed(subscription.status(), newStatus)) {
            throw new IllegalStateException(
                "Cannot transition from " + subscription.status() +
                " to " + newStatus
            );
        }

        // Perform side effects based on transition
        handleStateTransition(subscription, newStatus, reason);

        // Update subscription
        return new Subscription(
            subscription.subscriptionId(),
            subscription.customerId(),
            subscription.planId(),
            newStatus,
            subscription.currentPeriodStart(),
            subscription.currentPeriodEnd(),
            subscription.trialEnd(),
            newStatus == Subscription.SubscriptionStatus.CANCELED ?
                LocalDateTime.now() : subscription.canceledAt(),
            subscription.cancelAt(),
            subscription.cancelAtPeriodEnd(),
            subscription.stripeSubscriptionId(),
            subscription.metadata()
        );
    }

    private boolean isTransitionAllowed(
        Subscription.SubscriptionStatus from,
        Subscription.SubscriptionStatus to
    ) {
        return switch (from) {
            case INCOMPLETE -> to == Subscription.SubscriptionStatus.ACTIVE ||
                              to == Subscription.SubscriptionStatus.UNPAID;

            case TRIALING -> to == Subscription.SubscriptionStatus.ACTIVE ||
                            to == Subscription.SubscriptionStatus.PAST_DUE ||
                            to == Subscription.SubscriptionStatus.CANCELED;

            case ACTIVE -> to == Subscription.SubscriptionStatus.PAST_DUE ||
                          to == Subscription.SubscriptionStatus.CANCELED ||
                          to == Subscription.SubscriptionStatus.PAUSED;

            case PAST_DUE -> to == Subscription.SubscriptionStatus.ACTIVE ||
                            to == Subscription.SubscriptionStatus.UNPAID ||
                            to == Subscription.SubscriptionStatus.CANCELED;

            case PAUSED -> to == Subscription.SubscriptionStatus.ACTIVE ||
                          to == Subscription.SubscriptionStatus.CANCELED;

            case UNPAID -> to == Subscription.SubscriptionStatus.ACTIVE ||
                          to == Subscription.SubscriptionStatus.CANCELED;

            case CANCELED -> false;  // Terminal state
        };
    }

    private void handleStateTransition(
        Subscription subscription,
        Subscription.SubscriptionStatus newStatus,
        String reason
    ) {
        // Log state change
        System.out.println(String.format(
            "Subscription %s: %s → %s (reason: %s)",
            subscription.subscriptionId(),
            subscription.status(),
            newStatus,
            reason
        ));

        // Trigger side effects based on new state
        switch (newStatus) {
            case ACTIVE -> {
                // Grant access to product
                // Send welcome/reactivation email
                System.out.println("✅ Granting product access");
            }
            case PAST_DUE -> {
                // Start dunning workflow
                // Send payment failure notification
                System.out.println("⚠️ Starting dunning workflow");
            }
            case CANCELED -> {
                // Revoke access (immediate or at period end)
                // Send cancellation confirmation
                System.out.println("❌ Revoking product access");
            }
            case UNPAID -> {
                // Revoke access
                // Send final notice
                System.out.println("❌ Subscription unpaid - access revoked");
            }
            default -> {}
        }
    }

    /**
     * Check if subscription should transition based on time/state
     */
    public Optional<Subscription.SubscriptionStatus> checkAutoTransition(
        Subscription subscription
    ) {
        LocalDateTime now = LocalDateTime.now();

        // Trial ending?
        if (subscription.status() == Subscription.SubscriptionStatus.TRIALING &&
            subscription.trialEnd() != null &&
            now.isAfter(subscription.trialEnd())) {
            // Would transition to ACTIVE or PAST_DUE based on payment
            return Optional.of(Subscription.SubscriptionStatus.ACTIVE);
        }

        // Scheduled cancellation?
        if (subscription.cancelAt() != null &&
            now.isAfter(subscription.cancelAt())) {
            return Optional.of(Subscription.SubscriptionStatus.CANCELED);
        }

        // Cancel at period end?
        if (subscription.cancelAtPeriodEnd() &&
            now.isAfter(subscription.currentPeriodEnd())) {
            return Optional.of(Subscription.SubscriptionStatus.CANCELED);
        }

        return Optional.empty();
    }
}
```

### Subscription Operations

**Create subscription:**
```java
@Service
public class SubscriptionService {

    public Subscription createSubscription(
        String customerId,
        String planId,
        boolean startTrial
    ) {
        BillingPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime trialEnd = startTrial ?
            now.plusDays(plan.trialPeriodDays()) : null;

        // Calculate first period
        LocalDateTime periodStart = trialEnd != null ? trialEnd : now;
        LocalDateTime periodEnd = calculatePeriodEnd(periodStart, plan);

        Subscription subscription = new Subscription(
            UUID.randomUUID().toString(),
            customerId,
            planId,
            startTrial ? Subscription.SubscriptionStatus.TRIALING :
                        Subscription.SubscriptionStatus.INCOMPLETE,
            periodStart,
            periodEnd,
            trialEnd,
            null,  // canceledAt
            null,  // cancelAt
            false, // cancelAtPeriodEnd
            null,  // stripeSubscriptionId (will be set after Stripe creation)
            new HashMap<>()
        );

        return subscriptionRepository.save(subscription);
    }

    private LocalDateTime calculatePeriodEnd(
        LocalDateTime start,
        BillingPlan plan
    ) {
        return switch (plan.interval()) {
            case DAY -> start.plusDays(plan.intervalCount());
            case WEEK -> start.plusWeeks(plan.intervalCount());
            case MONTH -> start.plusMonths(plan.intervalCount());
            case YEAR -> start.plusYears(plan.intervalCount());
        };
    }
}
```

**Cancel subscription:**
```java
public Subscription cancelSubscription(
    String subscriptionId,
    boolean immediately
) {
    Subscription sub = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

    if (immediately) {
        // Cancel now
        return stateMachine.transition(
            sub,
            Subscription.SubscriptionStatus.CANCELED,
            "User requested immediate cancellation"
        );
    } else {
        // Cancel at period end
        return new Subscription(
            sub.subscriptionId(),
            sub.customerId(),
            sub.planId(),
            sub.status(),
            sub.currentPeriodStart(),
            sub.currentPeriodEnd(),
            sub.trialEnd(),
            sub.canceledAt(),
            sub.currentPeriodEnd(),  // Set cancelAt to period end
            true,                     // cancelAtPeriodEnd
            sub.stripeSubscriptionId(),
            sub.metadata()
        );
    }
}
```

**Upgrade/downgrade:**
```java
public Subscription changePlan(
    String subscriptionId,
    String newPlanId,
    boolean prorate
) {
    Subscription sub = subscriptionRepository.findById(subscriptionId)
        .orElseThrow();
    BillingPlan newPlan = planRepository.findById(newPlanId)
        .orElseThrow();

    // Calculate proration if applicable
    if (prorate) {
        long prorationAmount = calculateProration(sub, newPlan);
        // Create invoice for proration
    }

    // Update subscription
    return new Subscription(
        sub.subscriptionId(),
        sub.customerId(),
        newPlanId,  // New plan
        sub.status(),
        sub.currentPeriodStart(),
        sub.currentPeriodEnd(),
        sub.trialEnd(),
        sub.canceledAt(),
        sub.cancelAt(),
        sub.cancelAtPeriodEnd(),
        sub.stripeSubscriptionId(),
        sub.metadata()
    );
}

private long calculateProration(Subscription sub, BillingPlan newPlan) {
    // Time remaining in current period
    LocalDateTime now = LocalDateTime.now();
    long totalSeconds = java.time.Duration.between(
        sub.currentPeriodStart(), sub.currentPeriodEnd()
    ).getSeconds();
    long remainingSeconds = java.time.Duration.between(
        now, sub.currentPeriodEnd()
    ).getSeconds();

    double fractionRemaining = remainingSeconds / (double) totalSeconds;

    // Credit for unused time on old plan
    BillingPlan oldPlan = planRepository.findById(sub.planId()).orElseThrow();
    long credit = (long) (oldPlan.amountCents() * fractionRemaining);

    // Charge for new plan (prorated)
    long charge = (long) (newPlan.amountCents() * fractionRemaining);

    // Net amount due
    return charge - credit;
}
```

---

## Part 3: Payment Processing with Stripe

### Stripe Integration Architecture

```
Your Backend         Stripe API          Webhooks
─────────────       ───────────        ─────────

Customer signs up
      │
      ├─────────→ Create Customer
      │
      ├─────────→ Attach Payment Method
      │
      ├─────────→ Create Subscription
      │
      │                                  ←────── invoice.created
      │                                  ←────── invoice.payment_succeeded
      │                                  ←────── customer.subscription.created
      │
      └─────────→ Update local DB
```

### Stripe Setup

**Add dependencies (Maven):**

```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.0.0</version>
</dependency>
```

**Configuration:**

```java
import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}
```

**application.yml:**
```yaml
stripe:
  api:
    key: sk_test_... # Use environment variable in production
  webhook:
    secret: whsec_... # Webhook signing secret
```

### Code Example: Stripe Payment Service

```java
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class StripePaymentService {

    /**
     * Create Stripe customer
     */
    public String createStripeCustomer(Customer customer) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
            .setEmail(customer.email())
            .setName(customer.name())
            .putMetadata("customer_id", customer.customerId())
            .build();

        com.stripe.model.Customer stripeCustomer =
            com.stripe.model.Customer.create(params);

        return stripeCustomer.getId();
    }

    /**
     * Attach payment method to customer
     */
    public void attachPaymentMethod(
        String stripeCustomerId,
        String paymentMethodId
    ) throws StripeException {
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

        PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
            .setCustomer(stripeCustomerId)
            .build();

        paymentMethod.attach(params);

        // Set as default payment method
        CustomerUpdateParams customerParams = CustomerUpdateParams.builder()
            .setInvoiceSettings(
                CustomerUpdateParams.InvoiceSettings.builder()
                    .setDefaultPaymentMethod(paymentMethodId)
                    .build()
            )
            .build();

        com.stripe.model.Customer.retrieve(stripeCustomerId)
            .update(customerParams);
    }

    /**
     * Create subscription in Stripe
     */
    public String createStripeSubscription(
        String stripeCustomerId,
        String stripePriceId,
        int trialPeriodDays
    ) throws StripeException {
        SubscriptionCreateParams.Builder paramsBuilder =
            SubscriptionCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(stripePriceId)
                        .build()
                )
                .setPaymentBehavior(
                    SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE
                );

        if (trialPeriodDays > 0) {
            paramsBuilder.setTrialPeriodDays((long) trialPeriodDays);
        }

        com.stripe.model.Subscription subscription =
            com.stripe.model.Subscription.create(paramsBuilder.build());

        return subscription.getId();
    }

    /**
     * Update subscription (upgrade/downgrade)
     */
    public void updateSubscription(
        String stripeSubscriptionId,
        String newPriceId,
        boolean prorate
    ) throws StripeException {
        com.stripe.model.Subscription subscription =
            com.stripe.model.Subscription.retrieve(stripeSubscriptionId);

        String subscriptionItemId = subscription.getItems().getData().get(0).getId();

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
            .addItem(
                SubscriptionUpdateParams.Item.builder()
                    .setId(subscriptionItemId)
                    .setPrice(newPriceId)
                    .build()
            )
            .setProrationBehavior(
                prorate ?
                    SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS :
                    SubscriptionUpdateParams.ProrationBehavior.NONE
            )
            .build();

        subscription.update(params);
    }

    /**
     * Cancel subscription
     */
    public void cancelSubscription(
        String stripeSubscriptionId,
        boolean immediately
    ) throws StripeException {
        com.stripe.model.Subscription subscription =
            com.stripe.model.Subscription.retrieve(stripeSubscriptionId);

        if (immediately) {
            subscription.cancel();
        } else {
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();
            subscription.update(params);
        }
    }

    /**
     * Create one-time payment
     */
    public String createPaymentIntent(
        String stripeCustomerId,
        long amountCents,
        String currency,
        String description
    ) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amountCents)
            .setCurrency(currency)
            .setCustomer(stripeCustomerId)
            .setDescription(description)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return intent.getClientSecret();
    }

    /**
     * Refund payment
     */
    public void refundPayment(
        String paymentIntentId,
        Long amountCents  // null = full refund
    ) throws StripeException {
        RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
            .setPaymentIntent(paymentIntentId);

        if (amountCents != null) {
            paramsBuilder.setAmount(amountCents);
        }

        Refund.create(paramsBuilder.build());
    }
}
```

### Webhook Handling

**Stripe sends webhooks for events:**
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.payment_succeeded`
- `invoice.payment_failed`
- `payment_intent.succeeded`
- `payment_intent.payment_failed`

**Code Example: Webhook Controller**

```java
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final StripeWebhookHandler webhookHandler;

    public StripeWebhookController(StripeWebhookHandler webhookHandler) {
        this.webhookHandler = webhookHandler;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;

        try {
            // Verify webhook signature
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid signature");
        }

        // Handle event
        try {
            webhookHandler.handleEvent(event);
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            // Log error but return 200 to prevent Stripe from retrying
            System.err.println("Webhook processing error: " + e.getMessage());
            return ResponseEntity.ok("Webhook received but processing failed");
        }
    }
}

@Service
public class StripeWebhookHandler {

    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;

    public StripeWebhookHandler(
        SubscriptionService subscriptionService,
        InvoiceService invoiceService
    ) {
        this.subscriptionService = subscriptionService;
        this.invoiceService = invoiceService;
    }

    public void handleEvent(Event event) {
        switch (event.getType()) {
            case "customer.subscription.created" -> handleSubscriptionCreated(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "invoice.payment_succeeded" -> handlePaymentSucceeded(event);
            case "invoice.payment_failed" -> handlePaymentFailed(event);
            default -> System.out.println("Unhandled event type: " + event.getType());
        }
    }

    private void handleSubscriptionCreated(Event event) {
        com.stripe.model.Subscription stripeSubscription =
            (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        System.out.println("Subscription created: " + stripeSubscription.getId());
        // Update local database
    }

    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription stripeSubscription =
            (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        System.out.println("Subscription updated: " + stripeSubscription.getId());

        // Sync status changes
        String status = stripeSubscription.getStatus();
        subscriptionService.syncSubscriptionStatus(
            stripeSubscription.getId(),
            status
        );
    }

    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSubscription =
            (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        System.out.println("Subscription deleted: " + stripeSubscription.getId());
        subscriptionService.handleCancellation(stripeSubscription.getId());
    }

    private void handlePaymentSucceeded(Event event) {
        com.stripe.model.Invoice stripeInvoice =
            (com.stripe.model.Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        System.out.println("Payment succeeded for invoice: " + stripeInvoice.getId());
        invoiceService.markPaid(stripeInvoice.getId());
    }

    private void handlePaymentFailed(Event event) {
        com.stripe.model.Invoice stripeInvoice =
            (com.stripe.model.Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        System.out.println("Payment failed for invoice: " + stripeInvoice.getId());
        invoiceService.handlePaymentFailure(stripeInvoice.getId());
    }
}
```

---

## Part 4: Billing Workflows & Proration

### Proration Calculation

When a customer upgrades or downgrades mid-cycle, you need to calculate the difference.

**Example:**
- Current plan: $10/month
- New plan: $30/month
- Change happens 10 days into 30-day billing cycle
- 20 days remaining

**Calculation:**
```
Unused credit = $10 × (20/30) = $6.67
New plan charge = $30 × (20/30) = $20.00
Amount due = $20.00 - $6.67 = $13.33
```

**Code Example:**

```java
@Service
public class ProrationService {

    public record ProrationResult(
        long creditCents,
        long chargeCents,
        long netDueCents,
        String explanation
    ) {}

    public ProrationResult calculateProration(
        Subscription subscription,
        BillingPlan currentPlan,
        BillingPlan newPlan
    ) {
        LocalDateTime now = LocalDateTime.now();

        // Calculate time fractions
        long totalSeconds = java.time.Duration.between(
            subscription.currentPeriodStart(),
            subscription.currentPeriodEnd()
        ).getSeconds();

        long elapsedSeconds = java.time.Duration.between(
            subscription.currentPeriodStart(),
            now
        ).getSeconds();

        long remainingSeconds = totalSeconds - elapsedSeconds;
        double fractionRemaining = remainingSeconds / (double) totalSeconds;

        // Calculate credit for unused time on current plan
        long creditCents = (long) (currentPlan.amountCents() * fractionRemaining);

        // Calculate charge for new plan (prorated)
        long chargeCents = (long) (newPlan.amountCents() * fractionRemaining);

        // Net amount due (positive = customer owes, negative = credit)
        long netDueCents = chargeCents - creditCents;

        String explanation = String.format(
            "%.1f%% of billing period remaining. " +
            "Credit: $%.2f (%s). Charge: $%.2f (%s). Net due: $%.2f",
            fractionRemaining * 100,
            creditCents / 100.0, currentPlan.name(),
            chargeCents / 100.0, newPlan.name(),
            netDueCents / 100.0
        );

        return new ProrationResult(creditCents, chargeCents, netDueCents, explanation);
    }
}
```

### Handling Upgrades & Downgrades

**Upgrade (e.g., $10 → $30):**
1. Calculate proration
2. Create invoice for net amount due
3. Charge immediately
4. Update subscription to new plan
5. Grant access to new features

**Downgrade (e.g., $30 → $10):**
1. Calculate proration (customer has credit)
2. Apply credit to next invoice
3. Schedule downgrade for end of current period (optional)
4. Or downgrade immediately and issue refund/credit

**Code Example:**

```java
@Service
public class BillingWorkflowService {

    private final SubscriptionService subscriptionService;
    private final ProrationService prorationService;
    private final InvoiceService invoiceService;
    private final StripePaymentService stripePaymentService;

    public void handlePlanChange(
        String subscriptionId,
        String newPlanId,
        boolean immediate,
        boolean prorate
    ) throws StripeException {
        Subscription subscription = subscriptionService.getById(subscriptionId);
        BillingPlan currentPlan = planRepository.findById(subscription.planId()).orElseThrow();
        BillingPlan newPlan = planRepository.findById(newPlanId).orElseThrow();

        boolean isUpgrade = newPlan.amountCents() > currentPlan.amountCents();

        if (isUpgrade) {
            handleUpgrade(subscription, currentPlan, newPlan, prorate);
        } else {
            handleDowngrade(subscription, currentPlan, newPlan, immediate);
        }
    }

    private void handleUpgrade(
        Subscription subscription,
        BillingPlan currentPlan,
        BillingPlan newPlan,
        boolean prorate
    ) throws StripeException {
        if (prorate) {
            // Calculate proration
            ProrationResult proration = prorationService.calculateProration(
                subscription, currentPlan, newPlan
            );

            // Create invoice for difference
            if (proration.netDueCents() > 0) {
                Invoice invoice = invoiceService.createProrationInvoice(
                    subscription.customerId(),
                    subscription.subscriptionId(),
                    proration
                );

                // Charge immediately
                invoiceService.finalizeAndPay(invoice.invoiceId());
            }
        }

        // Update subscription in Stripe and locally
        stripePaymentService.updateSubscription(
            subscription.stripeSubscriptionId(),
            newPlan.stripePriceId(),
            prorate
        );

        subscriptionService.updatePlan(subscription.subscriptionId(), newPlanId);

        System.out.println("✅ Upgrade complete: " + currentPlan.name() +
                          " → " + newPlan.name());
    }

    private void handleDowngrade(
        Subscription subscription,
        BillingPlan currentPlan,
        BillingPlan newPlan,
        boolean immediate
    ) throws StripeException {
        if (immediate) {
            // Downgrade now, issue credit
            ProrationResult proration = prorationService.calculateProration(
                subscription, currentPlan, newPlan
            );

            // Net due will be negative (customer has credit)
            if (proration.netDueCents() < 0) {
                // Apply credit to customer account
                customerService.addCredit(
                    subscription.customerId(),
                    Math.abs(proration.netDueCents())
                );
            }

            // Update subscription
            stripePaymentService.updateSubscription(
                subscription.stripeSubscriptionId(),
                newPlan.stripePriceId(),
                true
            );

            subscriptionService.updatePlan(subscription.subscriptionId(), newPlanId);
        } else {
            // Schedule downgrade for end of period
            subscriptionService.scheduleDowngrade(
                subscription.subscriptionId(),
                newPlanId,
                subscription.currentPeriodEnd()
            );

            System.out.println("📅 Downgrade scheduled for " +
                              subscription.currentPeriodEnd());
        }
    }
}
```

---

## Part 5: Dunning & Failed Payment Recovery

### What is Dunning?

**Dunning** is the process of recovering failed payments through:
1. Automatic payment retries
2. Customer notifications
3. Grace period before cancellation

**Why it matters:**
- 20-40% of payment failures are temporary (expired card, insufficient funds)
- Good dunning can recover 40-70% of failed payments
- Each recovered payment = retained customer = higher LTV

### Dunning Strategy

**Retry schedule:**

```
Payment fails → Retry immediately
              ↓
       Wait 3 days → Retry
              ↓
       Wait 5 days → Retry
              ↓
       Wait 7 days → Final retry
              ↓
    No more retries → Cancel subscription
```

**Customer communication:**

| Day | Action | Message |
|-----|--------|---------|
| 0 | Payment fails | "Payment declined. Please update your card." |
| 1 | First retry fails | "We'll try again in 3 days. Update card to avoid disruption." |
| 4 | Second retry fails | "Still having trouble. Service may be interrupted soon." |
| 9 | Third retry fails | "Final attempt in 7 days. Update payment method now." |
| 16 | All retries exhausted | "Subscription canceled. We'd love to have you back!" |

### Code Example: Dunning Service

```java
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DunningService {

    public record DunningState(
        String subscriptionId,
        int attemptNumber,
        LocalDateTime lastAttempt,
        LocalDateTime nextAttempt,
        List<String> failureReasons
    ) {}

    /**
     * Handle initial payment failure
     */
    public void handlePaymentFailure(
        String subscriptionId,
        String failureReason
    ) {
        // Transition subscription to PAST_DUE
        Subscription subscription = subscriptionService.getById(subscriptionId);
        subscription = stateMachine.transition(
            subscription,
            Subscription.SubscriptionStatus.PAST_DUE,
            "Payment failed: " + failureReason
        );

        // Initialize dunning state
        DunningState state = new DunningState(
            subscriptionId,
            1,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(3),  // Next retry in 3 days
            List.of(failureReason)
        );

        dunningStateRepository.save(state);

        // Send customer notification
        emailService.sendPaymentFailureNotification(
            subscription.customerId(),
            failureReason
        );

        // Schedule immediate retry for some failure types
        if (isRetriableImmediately(failureReason)) {
            scheduleRetry(subscriptionId, 0);  // Retry now
        }
    }

    private boolean isRetriableImmediately(String failureReason) {
        // Some failures are worth immediate retry
        return failureReason.contains("card_declined") &&
               !failureReason.contains("insufficient_funds");
    }

    /**
     * Scheduled job to process retry queue
     */
    @Scheduled(fixedDelay = 3600000)  // Run every hour
    public void processRetryQueue() {
        List<DunningState> dueDunning = dunningStateRepository
            .findByNextAttemptBefore(LocalDateTime.now());

        for (DunningState state : dueDunning) {
            retryPayment(state);
        }
    }

    private void retryPayment(DunningState state) {
        Subscription subscription = subscriptionService.getById(state.subscriptionId());

        try {
            // Attempt to charge payment method
            boolean success = stripePaymentService.retrySubscriptionPayment(
                subscription.stripeSubscriptionId()
            );

            if (success) {
                // Payment succeeded!
                handleRetrySuccess(subscription, state);
            } else {
                // Payment failed again
                handleRetryFailure(subscription, state);
            }
        } catch (Exception e) {
            System.err.println("Retry error: " + e.getMessage());
            handleRetryFailure(subscription, state);
        }
    }

    private void handleRetrySuccess(Subscription subscription, DunningState state) {
        // Transition back to ACTIVE
        subscription = stateMachine.transition(
            subscription,
            Subscription.SubscriptionStatus.ACTIVE,
            "Payment retry succeeded"
        );

        // Clean up dunning state
        dunningStateRepository.delete(state);

        // Send success notification
        emailService.sendPaymentRecoveredNotification(subscription.customerId());

        System.out.println("✅ Payment recovered for " + subscription.subscriptionId() +
                          " after " + state.attemptNumber() + " attempts");
    }

    private void handleRetryFailure(Subscription subscription, DunningState state) {
        int nextAttempt = state.attemptNumber() + 1;

        if (nextAttempt > 4) {
            // All retries exhausted - cancel subscription
            handleDunningExhausted(subscription);
        } else {
            // Schedule next retry
            LocalDateTime nextRetryDate = calculateNextRetryDate(nextAttempt);

            DunningState updatedState = new DunningState(
                state.subscriptionId(),
                nextAttempt,
                LocalDateTime.now(),
                nextRetryDate,
                state.failureReasons()
            );

            dunningStateRepository.save(updatedState);

            // Send escalating notification
            emailService.sendPaymentRetryFailedNotification(
                subscription.customerId(),
                nextAttempt,
                nextRetryDate
            );
        }
    }

    private LocalDateTime calculateNextRetryDate(int attemptNumber) {
        return switch (attemptNumber) {
            case 1 -> LocalDateTime.now().plusDays(3);
            case 2 -> LocalDateTime.now().plusDays(5);
            case 3 -> LocalDateTime.now().plusDays(7);
            default -> LocalDateTime.now().plusDays(7);
        };
    }

    private void handleDunningExhausted(Subscription subscription) {
        // Cancel subscription
        subscription = stateMachine.transition(
            subscription,
            Subscription.SubscriptionStatus.UNPAID,
            "All dunning attempts exhausted"
        );

        // Send final notice
        emailService.sendSubscriptionCanceledNotification(
            subscription.customerId(),
            "Payment failure"
        );

        System.out.println("❌ Subscription canceled due to payment failure: " +
                          subscription.subscriptionId());
    }

    /**
     * Allow customer to manually resolve payment issue
     */
    public void resolvePaymentIssue(String subscriptionId, String newPaymentMethodId)
            throws StripeException {
        Subscription subscription = subscriptionService.getById(subscriptionId);

        // Update payment method
        Customer customer = customerService.getById(subscription.customerId());
        stripePaymentService.attachPaymentMethod(
            customer.stripeCustomerId(),
            newPaymentMethodId
        );

        // Retry payment immediately
        boolean success = stripePaymentService.retrySubscriptionPayment(
            subscription.stripeSubscriptionId()
        );

        if (success) {
            // Clean up dunning state
            dunningStateRepository.deleteBySubscriptionId(subscriptionId);

            // Reactivate subscription
            stateMachine.transition(
                subscription,
                Subscription.SubscriptionStatus.ACTIVE,
                "Customer updated payment method"
            );
        }
    }
}
```

### Dunning Best Practices

✅ **Start gentle:** First email is helpful, not accusatory
✅ **Escalate gradually:** Later emails more urgent
✅ **Make it easy:** One-click link to update payment method
✅ **Offer help:** Human support for confused customers
✅ **Track success rate:** Monitor recovery rate by email/retry
✅ **A/B test:** Test different messaging and timing

❌ **Don't spam:** Respect customer with reasonable frequency
❌ **Don't cancel too fast:** Give adequate grace period (14+ days)
❌ **Don't be accusatory:** Payment failures are often mistakes
❌ **Don't make it hard:** Updating payment should be simple

---

## Part 6: Revenue Recognition & Reporting

### Revenue Recognition Basics

**Cash vs Accrual Accounting:**

- **Cash accounting:** Recognize revenue when payment received
- **Accrual accounting:** Recognize revenue when service delivered

**SaaS uses accrual accounting:**
- Customer pays $120 for annual subscription upfront
- You recognize $10/month over 12 months
- The $110 not yet earned is "deferred revenue" (liability)

**Example timeline:**

```
Jan 1: Customer pays $120 for annual subscription

Balance Sheet:
  Cash: +$120
  Deferred Revenue (liability): +$120

Income Statement:
  Revenue: $0  (nothing earned yet)

---

Each month (Jan, Feb, Mar...):

Income Statement:
  Revenue: +$10

Balance Sheet:
  Deferred Revenue: -$10
```

### Code Example: Revenue Recognition Service

```java
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class RevenueRecognitionService {

    public record RevenueSchedule(
        String invoiceId,
        long totalAmountCents,
        LocalDate startDate,
        LocalDate endDate,
        List<RevenueEntry> entries
    ) {}

    public record RevenueEntry(
        YearMonth month,
        long amountCents,
        boolean recognized
    ) {}

    /**
     * Create revenue recognition schedule for an invoice
     */
    public RevenueSchedule createSchedule(Invoice invoice, Subscription subscription) {
        List<RevenueEntry> entries = new ArrayList<>();

        LocalDate start = subscription.currentPeriodStart().toLocalDate();
        LocalDate end = subscription.currentPeriodEnd().toLocalDate();

        long monthsBetween = ChronoUnit.MONTHS.between(start, end);

        if (monthsBetween <= 1) {
            // Single month - recognize immediately
            entries.add(new RevenueEntry(
                YearMonth.from(start),
                invoice.totalCents(),
                false
            ));
        } else {
            // Multi-month - spread evenly
            long monthlyAmount = invoice.totalCents() / monthsBetween;
            long remainder = invoice.totalCents() % monthsBetween;

            for (int i = 0; i < monthsBetween; i++) {
                YearMonth month = YearMonth.from(start.plusMonths(i));
                long amount = monthlyAmount + (i == 0 ? remainder : 0);  // Put remainder in first month

                entries.add(new RevenueEntry(month, amount, false));
            }
        }

        return new RevenueSchedule(
            invoice.invoiceId(),
            invoice.totalCents(),
            start,
            end,
            entries
        );
    }

    /**
     * Recognize revenue for a given month
     */
    public long recognizeRevenueForMonth(YearMonth month) {
        List<RevenueSchedule> schedules = revenueScheduleRepository
            .findByMonth(month);

        long totalRecognized = 0;

        for (RevenueSchedule schedule : schedules) {
            for (RevenueEntry entry : schedule.entries()) {
                if (entry.month().equals(month) && !entry.recognized()) {
                    // Mark as recognized
                    revenueScheduleRepository.markRecognized(
                        schedule.invoiceId(),
                        month
                    );

                    totalRecognized += entry.amountCents();
                }
            }
        }

        return totalRecognized;
    }

    /**
     * Calculate deferred revenue (liability)
     */
    public long calculateDeferredRevenue(LocalDate asOf) {
        List<RevenueSchedule> schedules = revenueScheduleRepository.findAll();

        long deferred = 0;

        for (RevenueSchedule schedule : schedules) {
            for (RevenueEntry entry : schedule.entries()) {
                // If entry is in the future and not yet recognized
                if (entry.month().atDay(1).isAfter(asOf.withDayOfMonth(1)) ||
                    !entry.recognized()) {
                    deferred += entry.amountCents();
                }
            }
        }

        return deferred;
    }
}
```

### MRR & ARR Calculation

**MRR (Monthly Recurring Revenue):**

```java
@Service
public class MRRCalculationService {

    public record MRRBreakdown(
        long newMRR,         // From new customers
        long expansionMRR,   // From upgrades
        long contractionMRR, // From downgrades
        long churnMRR,       // From cancellations
        long netNewMRR,      // Net change
        long totalMRR        // Total MRR
    ) {}

    public MRRBreakdown calculateMRR(YearMonth month) {
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByStatusAndMonth(Subscription.SubscriptionStatus.ACTIVE, month);

        long totalMRR = 0;
        long newMRR = 0;
        long expansionMRR = 0;
        long contractionMRR = 0;
        long churnMRR = 0;

        for (Subscription sub : activeSubscriptions) {
            BillingPlan plan = planRepository.findById(sub.planId()).orElseThrow();
            long mrr = (long) (plan.monthlyEquivalent() * 100);  // Convert to cents

            totalMRR += mrr;

            // Categorize MRR
            if (isNewSubscription(sub, month)) {
                newMRR += mrr;
            } else if (wasUpgradedThisMonth(sub, month)) {
                long previousMRR = getPreviousMonthMRR(sub, month);
                expansionMRR += (mrr - previousMRR);
            } else if (wasDowngradedThisMonth(sub, month)) {
                long previousMRR = getPreviousMonthMRR(sub, month);
                contractionMRR += (previousMRR - mrr);
            }
        }

        // Calculate churn MRR (subscriptions that were active last month but canceled)
        churnMRR = calculateChurnMRR(month);

        long netNewMRR = newMRR + expansionMRR - contractionMRR - churnMRR;

        return new MRRBreakdown(
            newMRR,
            expansionMRR,
            contractionMRR,
            churnMRR,
            netNewMRR,
            totalMRR
        );
    }

    private boolean isNewSubscription(Subscription sub, YearMonth month) {
        LocalDate subStart = sub.currentPeriodStart().toLocalDate();
        YearMonth subMonth = YearMonth.from(subStart);
        return subMonth.equals(month);
    }

    private boolean wasUpgradedThisMonth(Subscription sub, YearMonth month) {
        // Check subscription change history
        return subscriptionHistoryRepository
            .findUpgradeInMonth(sub.subscriptionId(), month)
            .isPresent();
    }

    private boolean wasDowngradedThisMonth(Subscription sub, YearMonth month) {
        return subscriptionHistoryRepository
            .findDowngradeInMonth(sub.subscriptionId(), month)
            .isPresent();
    }

    private long getPreviousMonthMRR(Subscription sub, YearMonth month) {
        // Get subscription's MRR from previous month
        return mrrHistoryRepository
            .findBySubscriptionAndMonth(sub.subscriptionId(), month.minusMonths(1))
            .orElse(0L);
    }

    private long calculateChurnMRR(YearMonth month) {
        YearMonth previousMonth = month.minusMonths(1);

        List<Subscription> churned = subscriptionRepository
            .findCanceledInMonth(month);

        long churnedMRR = 0;
        for (Subscription sub : churned) {
            churnedMRR += getPreviousMonthMRR(sub, previousMonth);
        }

        return churnedMRR;
    }
}
```

**ARR = MRR × 12**

---

## Part 7: Tax, Compliance & Invoicing

### Tax Basics for SaaS

**US Sales Tax:**
- Nexus: Physical or economic presence in a state
- Rates vary by state (0-10%)
- Some states exempt SaaS, others tax it
- Threshold: ~$100K sales or 200 transactions

**EU VAT:**
- Required for B2C sales in EU
- Rate varies by country (15-27%)
- Reverse charge for B2B (customer pays VAT)
- Threshold varies by country

**Handling tax:**

**Option 1: Manual**
- Track nexus, calculate rates, file returns
- Only feasible for very early stage

**Option 2: Use tax service**
- Stripe Tax, TaxJar, Avalara
- Automatic calculation and filing
- Cost: 0.5-1% of revenue + fixed fee

**Recommended: Stripe Tax**

```java
// Enable Stripe Tax
SubscriptionCreateParams params = SubscriptionCreateParams.builder()
    .setCustomer(stripeCustomerId)
    .addItem(/* ... */)
    .setAutomaticTax(
        SubscriptionCreateParams.AutomaticTax.builder()
            .setEnabled(true)
            .build()
    )
    .build();
```

### Invoice Generation

**Invoice must include:**
- Invoice number (unique, sequential)
- Issue date
- Due date
- Seller information (company name, address, tax ID)
- Buyer information
- Line items (description, quantity, unit price, total)
- Subtotal
- Tax (broken down by jurisdiction)
- Total
- Payment terms
- Payment methods accepted

**Code Example: Invoice Generator**

```java
@Service
public class InvoiceGeneratorService {

    public String generateInvoiceHTML(Invoice invoice) {
        Customer customer = customerRepository.findById(invoice.customerId()).orElseThrow();

        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .invoice-header { margin-bottom: 30px; }
                    .invoice-number { font-size: 24px; font-weight: bold; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 20px; }
                    th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
                    .total { font-size: 20px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="invoice-header">
                    <h1>Invoice</h1>
                    <p class="invoice-number">Invoice #%s</p>
                    <p>Date: %s</p>
                    <p>Due: %s</p>
                </div>

                <div class="customer-info">
                    <h3>Bill To:</h3>
                    <p>%s<br>%s</p>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Description</th>
                            <th>Period</th>
                            <th>Amount</th>
                        </tr>
                    </thead>
                    <tbody>
            """.formatted(
                invoice.invoiceId(),
                invoice.periodStart(),
                invoice.dueDate(),
                customer.name(),
                customer.email()
            ));

        // Add line items
        for (Invoice.InvoiceLineItem item : invoice.lineItems()) {
            html.append("""
                        <tr>
                            <td>%s</td>
                            <td>%s - %s</td>
                            <td>$%.2f</td>
                        </tr>
                """.formatted(
                    item.description(),
                    item.periodStart(),
                    item.periodEnd(),
                    item.amountCents() / 100.0
                ));
        }

        // Add totals
        html.append("""
                    </tbody>
                </table>

                <div class="totals" style="margin-top: 30px; text-align: right;">
                    <p>Subtotal: $%.2f</p>
                    <p>Tax: $%.2f</p>
                    <p class="total">Total: $%.2f</p>
                </div>

                <div class="footer" style="margin-top: 50px; color: #666;">
                    <p>Thank you for your business!</p>
                    <p>Questions? Contact support@bibby.dev</p>
                </div>
            </body>
            </html>
            """.formatted(
                invoice.amountCents() / 100.0,
                invoice.taxCents() / 100.0,
                invoice.totalCents() / 100.0
            ));

        return html.toString();
    }

    /**
     * Generate PDF invoice (requires library like iText or Flying Saucer)
     */
    public byte[] generateInvoicePDF(Invoice invoice) {
        String html = generateInvoiceHTML(invoice);
        // Convert HTML to PDF
        // Implementation depends on PDF library chosen
        return new byte[0];  // Placeholder
    }
}
```

---

## Part 8: Building Bibby's Billing System

Let's apply everything to **Bibby**.

### Bibby Pricing (from Week 39):

- **Free:** Up to 100 books
- **Bibliophile:** $8/month, unlimited books
- **Book Club:** $20/month, shared libraries
- **Enterprise:** Custom pricing

### Implementation Plan

**Step 1: Set up Stripe**

```java
@Configuration
public class BibbyStripeConfig {
    // Configure Stripe as shown in Part 3
}
```

**Step 2: Create billing entities**

```java
// Customer, Subscription, BillingPlan, Invoice, Payment
// Use code from Part 1
```

**Step 3: Implement subscription flow**

```java
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @PostMapping("/subscribe")
    public ResponseEntity<SubscribeResponse> subscribe(
        @RequestBody SubscribeRequest request
    ) {
        // 1. Create/get customer
        // 2. Attach payment method
        // 3. Create subscription
        // 4. Return client secret for confirmation
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription(
        @RequestParam String subscriptionId,
        @RequestParam boolean immediately
    ) {
        subscriptionService.cancel(subscriptionId, immediately);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/upgrade")
    public ResponseEntity<Void> upgradePlan(
        @RequestParam String subscriptionId,
        @RequestParam String newPlanId
    ) {
        billingWorkflowService.handlePlanChange(
            subscriptionId, newPlanId, true, true
        );
        return ResponseEntity.ok().build();
    }
}
```

**Step 4: Webhook handling**

```java
@RestController
@RequestMapping("/api/webhooks/stripe")
public class BibbyStripeWebhookController {
    // Implement webhook handler from Part 3
    // Handle subscription events and payment status changes
}
```

**Step 5: Feature gating based on subscription**

```java
@Service
public class FeatureAccessService {

    public boolean canAddBook(String userId) {
        User user = userRepository.findById(userId).orElseThrow();

        if (user.subscriptionTier() == Tier.FREE) {
            int bookCount = bookRepository.countByUserId(userId);
            return bookCount < 100;  // Free tier limit
        }

        return true;  // Paid tiers = unlimited
    }

    public boolean canAccessAnalytics(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.subscriptionTier() != Tier.FREE;
    }

    public boolean canUseAPI(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.subscriptionTier() == Tier.BIBLIOPHILE ||
               user.subscriptionTier() == Tier.BOOK_CLUB ||
               user.subscriptionTier() == Tier.ENTERPRISE;
    }
}
```

**Step 6: Dunning workflow**

```java
// Implement DunningService from Part 5
// Configure retry schedule and customer notifications
```

**Step 7: Revenue reporting dashboard**

```java
@RestController
@RequestMapping("/api/admin/revenue")
public class RevenueReportingController {

    @GetMapping("/mrr")
    public MRRBreakdown getMRR(@RequestParam YearMonth month) {
        return mrrCalculationService.calculateMRR(month);
    }

    @GetMapping("/deferred")
    public long getDeferredRevenue() {
        return revenueRecognitionService.calculateDeferredRevenue(
            LocalDate.now()
        );
    }
}
```

---

## Practical Assignments

### Assignment 1: Build Subscription State Machine

Implement a complete subscription state machine with:
- All 7 states
- Transition validation
- Side effects (emails, access control)
- Automated transitions based on time

Test with JUnit:
```java
@Test
public void testSubscriptionLifecycle() {
    // Create subscription in TRIALING
    // Transition to ACTIVE when trial ends
    // Simulate payment failure → PAST_DUE
    // Successful retry → ACTIVE
    // User cancels → CANCELED
}
```

### Assignment 2: Implement Proration Logic

Build a proration calculator that handles:
- Upgrades mid-cycle
- Downgrades mid-cycle
- Plan changes with different billing intervals
- Edge cases (same-day changes, end-of-month changes)

Test with various scenarios:
```java
@Test
public void testProrationUpgrade() {
    // $10/mo plan, 10 days into 30-day cycle, upgrade to $30/mo
    // Expected: Credit $6.67, Charge $20, Net $13.33
}
```

### Assignment 3: Design Dunning Strategy

Create a dunning workflow with:
- Custom retry schedule (configurable)
- Email templates for each stage
- Success metrics tracking
- Manual resolution flow

Document:
- Retry timing and frequency
- Email messaging for each attempt
- Grace period before cancellation
- Expected recovery rate

### Assignment 4: Revenue Recognition Schedule

Implement revenue recognition for:
- Monthly subscriptions (recognize immediately)
- Annual subscriptions (spread over 12 months)
- Prorated charges (partial month recognition)

Build a report showing:
- Revenue recognized this month
- Deferred revenue (liability)
- Revenue forecast for next 12 months

### Assignment 5: Integrate Stripe

Build a working Stripe integration:
- Create customer
- Attach payment method
- Create subscription
- Handle webhooks
- Display subscription status in UI

Test in Stripe test mode with test cards.

---

## Reflection Questions

1. **Build vs Buy:** When should you build custom billing vs use a platform like Stripe Billing or Chargebee?

2. **Failed payments:** What's the right balance between aggressive dunning (maximizing recovery) and respecting customer experience?

3. **Proration:** Should you prorate downgrades immediately or apply at end of period? What are trade-offs?

4. **Free trials:** Should trials require a credit card upfront, or only at conversion? How does this affect conversion vs fraud?

5. **Revenue recognition:** Why is deferred revenue a liability? How does it affect company valuation?

6. **Tax complexity:** At what revenue level should you invest in automated tax calculation? Is it worth it at $10K ARR? $100K? $1M?

7. **Subscription state:** Should you revoke access immediately when payment fails, or give a grace period? How long?

8. **Annual vs monthly:** How much discount should you offer for annual plans to make them attractive while preserving margin?

---

## Key Takeaways

### Billing is Critical Infrastructure

- Handle with care—errors cost real money
- Test thoroughly—double-charging or under-charging destroys trust
- Monitor closely—billing bugs are silent killers
- Plan for scale—refactoring billing at scale is painful

### The Subscription Lifecycle is a State Machine

- Define clear states and allowed transitions
- Handle edge cases (what if user cancels during dunning?)
- Automate state transitions based on time and events
- Log all state changes for debugging and auditing

### Proration is Complex

- Daily proration is most fair but complex
- Consider simplifying (no proration for downgrades)
- Always explain proration to customers
- Test edge cases thoroughly

### Dunning Saves Revenue

- 40-70% of failed payments are recoverable
- Balance retry frequency with customer experience
- Make payment update easy (one-click link)
- Track dunning success rate and optimize

### Revenue Recognition Matters

- SaaS uses accrual accounting
- Deferred revenue is a liability (you owe service)
- Recognize revenue as service is delivered
- Critical for financial reporting and fundraising

### Tax is Unavoidable

- Use automated tax services (Stripe Tax, TaxJar)
- Don't try to DIY beyond very early stage
- Factor tax into pricing and unit economics
- Failure to collect tax = personal liability

---

## Looking Ahead: Week 41

Next week: **Sales Processes & Pipeline Management**

You'll learn how to:
- Design sales processes for different customer segments (self-serve, SMB, enterprise)
- Build and manage sales pipelines
- Implement CRM systems and lead scoring
- Create sales playbooks and training materials
- Measure sales efficiency and conversion rates
- Scale from founder-led sales to sales team

Plus: Building a sales pipeline for Bibby's enterprise tier.

---

## From Your Senior Architect

"Billing is where the rubber meets the road.

You can have the best product, the best marketing, the best growth strategy—but if billing breaks, revenue stops. I've seen companies:

- Double-charge customers and face chargebacks
- Under-charge and discover months later
- Lose thousands in failed payments they never retried
- Fail audits due to incorrect revenue recognition
- Owe massive tax penalties for missing filings

Billing is not glamorous. It's plumbing. But it's the plumbing that makes the whole house work.

Invest the time to build it right:
- Test every state transition
- Handle every edge case
- Monitor every metric
- Audit every calculation

Your finance team will thank you. Your investors will thank you. Your customers will thank you.

And when you're processing millions in ARR without a single billing error? You'll thank yourself.

Now go build rock-solid revenue infrastructure."

—Your Senior Architect

---

## Progress Tracker

**Week 40 of 52 complete (77% complete)**

**Semester 4 (Weeks 40-52): Execution, Revenue & Scale**
- ✅ Week 40: Revenue Architecture & Billing Systems ← **You are here**
- ⬜ Week 41: Sales Processes & Pipeline Management
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

**End of Week 40**
