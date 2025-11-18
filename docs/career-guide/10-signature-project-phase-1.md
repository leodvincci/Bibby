# Section 10: Signature Project (Phase 1)
**Choosing and Building Your Differentiator**

**Week 10-12: From Competent to Memorable**

---

## Overview

You've built a solid foundation: clean code, REST APIs, database migrations, caching, async processing. Bibby now demonstrates **professional competency**—you can build enterprise-grade systems.

But competency isn't enough to stand out in competitive job markets. When 50 candidates have "Spring Boot CRUD app" on their resume, **what makes yours memorable?**

The answer: **A signature feature** that showcases advanced thinking and connects directly to your target domain.

This section is different from previous ones. Instead of prescribing exactly what to build, we'll:
1. Analyze three signature feature options
2. Help you choose based on your background and target roles
3. Provide detailed implementation guidance for your choice
4. Show how to present it in interviews

By the end of Sections 10-12 (3 weeks), you'll have a feature that makes interviewers say: *"This isn't just another bootcamp project. This person thinks like an engineer."*

**This Week's Focus:**
- Understanding what makes a feature "signature-worthy"
- Evaluating three options: Search, Recommendations, Analytics
- Choosing your signature feature
- Beginning implementation (Phase 1 of 3)

---

## Part 1: What Makes a Feature "Signature-Worthy"?

### The Junior Developer Portfolio Problem

**Typical bootcamp project:**
- User authentication
- Basic CRUD operations
- Simple UI
- Maybe deployed to Heroku

**Problem:** Every candidate has this. It demonstrates *baseline competency*, not *differentiation*.

**What interviewers look for:**
- **Technical depth:** Did you tackle a hard problem?
- **Domain thinking:** Does this connect to real-world use cases?
- **Trade-off analysis:** Can you explain *why* you chose this approach vs alternatives?
- **Production mindset:** Is this scalable? How would you monitor it?

### Signature Feature Criteria

A good signature feature:

✅ **1. Solves a Real Problem**
Not a toy feature. Something production systems actually need.

✅ **2. Demonstrates Advanced Concepts**
Goes beyond CRUD: search algorithms, machine learning, data pipelines, complex state management.

✅ **3. Tells a Story**
You can explain in an interview: "Here's the problem, here's why I chose this solution, here's what I learned."

✅ **4. Connects to Your Background**
Leverages your operational experience. Shows you understand industrial software needs.

✅ **5. Has Measurable Impact**
You can say: "Reduced query time from 500ms to 50ms" or "Improved recommendation accuracy to 78%."

### Industrial Analogy: Signature Projects in Operations

In your Navy and Kinder Morgan roles, you likely had projects that stood out:
- **Not signature:** "Completed routine equipment inspections" (everyone does this)
- **Signature:** "Identified recurring failure pattern in pump seals, implemented predictive maintenance schedule, reduced unplanned downtime by 40%"

The signature project demonstrates:
- Problem identification (pattern recognition)
- Solution design (predictive maintenance)
- Measurable impact (40% reduction)
- Initiative beyond baseline duties

Your Bibby signature feature serves the same purpose.

---

## Part 2: Three Signature Feature Options

### Option 1: Advanced Search with Elasticsearch

**The Problem:**
Basic SQL `LIKE` queries don't scale. Can't search across multiple fields efficiently. No relevance ranking. No fuzzy matching (typos).

**The Solution:**
Integrate Elasticsearch for full-text search with relevance scoring, filtering, and autocomplete.

**Technical Depth:**
- Distributed search architecture
- Inverted index data structure
- Text analysis (tokenization, stemming)
- Relevance scoring algorithms (TF-IDF, BM25)
- Sync strategy between PostgreSQL and Elasticsearch

**Industrial Connection:**
Search mirrors **SCADA alarm filtering**:
- Thousands of alarms in industrial systems
- Operators need: free-text search, filter by severity/equipment/timeframe
- Fast retrieval critical (can't wait 10 seconds during emergency)
- Relevance ranking (show most critical alarms first)

**Interview Story:**
> "Library patrons need to find books quickly, even with typos or partial information. SQL `LIKE '%clean%'` scans entire table and doesn't handle 'Clean Code' vs 'Kleen Kode'. I implemented Elasticsearch to enable sub-50ms full-text search with fuzzy matching. The inverted index structure reminded me of alarm database indexing in SCADA systems—both optimize for fast retrieval of frequently-queried data."

**Pros:**
- Widely used in industry (Elastic Stack = ELK)
- Directly applicable to log search, monitoring dashboards
- Impressive demo (autocomplete, instant results)
- Complexity appropriate for mid-level roles

**Cons:**
- Adds infrastructure dependency (Elasticsearch cluster)
- Data sync complexity (keep PostgreSQL and Elasticsearch consistent)
- Overkill for small datasets (Bibby might have 1000 books)

**Best for:** Candidates targeting observability, monitoring, or data-heavy platforms (Splunk, Datadog, Elastic, New Relic)

---

### Option 2: Recommendation Engine

**The Problem:**
Patrons don't know what to read next. Library has thousands of books. Manual browsing is time-consuming.

**The Solution:**
Build recommendation engine using collaborative filtering or content-based filtering.

**Technical Depth:**
- Machine learning algorithms (cosine similarity, matrix factorization)
- Batch processing for model training
- Feature engineering (checkout history → user preferences)
- A/B testing framework for recommendation quality
- Cold start problem handling (new users, new books)

**Industrial Connection:**
Recommendations mirror **predictive maintenance**:
- **Input data:** Equipment sensor readings, maintenance history
- **Algorithm:** Identify patterns in failure data
- **Output:** Predict which equipment needs maintenance soon
- **Feedback loop:** Track prediction accuracy, retrain model

**Interview Story:**
> "I built a recommendation engine to help patrons discover books based on checkout history. Used collaborative filtering—'patrons who checked out Clean Code also liked Refactoring'—similar to how predictive maintenance identifies equipment that fails together. Achieved 78% relevance in testing. The challenge was the cold start problem for new patrons, which I solved with hybrid content-based filtering using book metadata."

**Pros:**
- Touches machine learning (valuable skill)
- Clear business value (increase circulation, patron satisfaction)
- Demonstrates data pipeline skills
- Scalable complexity (start simple, add sophistication)

**Cons:**
- Requires sufficient data (checkout history)
- Complex to explain if interviewer not technical
- Evaluation is subjective (hard to measure "good" recommendations)

**Best for:** Candidates targeting data-driven platforms (Uptake, SparkCognition, C3.ai) or industrial IoT with predictive analytics

---

### Option 3: Analytics & Reporting Dashboard

**The Problem:**
Librarians need operational insights: circulation trends, popular books, overdue patterns, inventory health. No way to visualize this data.

**The Solution:**
Build REST API endpoints for analytics + frontend dashboard with charts/graphs.

**Technical Depth:**
- Aggregation queries (SQL GROUP BY, window functions)
- Caching strategy for expensive analytics
- Scheduled jobs for pre-computing reports
- Data export (CSV, PDF reports)
- Time-series analysis (circulation over time)

**Industrial Connection:**
Dashboard mirrors **SCADA HMI (Human-Machine Interface)**:
- **Real-time metrics:** Active checkouts, overdue count
- **Trends:** Circulation per month, popular genres
- **Alerts:** Books overdue >30 days, inventory below threshold
- **Drill-down:** Click on metric to see details

**Interview Story:**
> "Librarians needed visibility into circulation patterns to make data-driven decisions about acquisitions and inventory management. I built an analytics API with 12 key metrics and a React dashboard for visualization. The dashboard design mirrors SCADA interfaces I used at Kinder Morgan—at-a-glance KPIs, trend graphs, and drill-down capability. Pre-computed daily aggregations with Redis caching reduced query time from 2 seconds to 50ms."

**Pros:**
- Highly visual (impressive in portfolio)
- Directly applicable to industrial dashboards, monitoring tools
- Demonstrates full-stack skills (backend + frontend)
- Easy to explain business value

**Cons:**
- Requires frontend development (React/Vue)
- Less algorithmic depth than search/recommendations
- Dashboard design is subjective

**Best for:** Candidates targeting industrial automation (OSIsoft, Rockwell, Honeywell) or enterprise dashboards (Tableau, PowerBI integrations)

---

## Part 3: Decision Framework

### Choosing Your Signature Feature

**Ask yourself:**

**1. Which aligns with target companies?**
- **Search:** Log analytics, observability platforms, data platforms
- **Recommendations:** Predictive analytics, IoT platforms, data science roles
- **Dashboard:** Industrial automation, SCADA/HMI, monitoring tools

**2. Which leverages your background?**
- **Navy/Kinder Morgan experience:** Dashboard (SCADA familiarity) or Recommendations (predictive maintenance analogy)

**3. Which excites you technically?**
- **Love algorithms:** Recommendations (ML) or Search (indexing, relevance)
- **Love visualization:** Dashboard
- **Love distributed systems:** Search (Elasticsearch cluster)

**4. What skills do you want to learn?**
- **Machine learning:** Recommendations
- **Full-text search:** Elasticsearch
- **Data visualization:** Dashboard

### Recommended Choice for Leo

**Given your profile:**
- Navy petroleum systems + Kinder Morgan pipeline operations
- Target companies: OSIsoft, Uptake, Rockwell, Honeywell
- MBA focus on operational analytics

**I recommend: Analytics & Reporting Dashboard**

**Why:**
1. **Direct connection to operations:** You've used SCADA/HMI systems. You understand what makes a good operational dashboard.
2. **Target company alignment:** OSIsoft PI System, Rockwell FactoryTalk, Honeywell Experion are all dashboard/visualization platforms.
3. **Full-stack demonstration:** Shows backend (aggregation queries, caching) AND frontend (React).
4. **Easier to explain:** Business value is obvious. Interviewers immediately understand "librarian needs to see circulation trends."
5. **Scalable complexity:** Start with basic metrics, add sophistication (predictive alerts, anomaly detection).

**We'll implement the Analytics Dashboard in this section.** If you prefer Search or Recommendations, the framework still applies—just different technical implementation.

---

## Part 4: Analytics Dashboard — Phase 1 Implementation

### Dashboard Requirements

**User Story:**
> As a librarian, I need to see operational metrics for the library so I can make data-driven decisions about acquisitions, staffing, and inventory management.

**Key Metrics (12 total):**

**Inventory Metrics:**
1. Total books in system
2. Books by status (Available, Checked Out, Lost, Archived)
3. Books added this month
4. Shelf utilization (% capacity used)

**Circulation Metrics:**
5. Active checkouts
6. Checkouts this week/month/year
7. Returns this week/month/year
8. Average checkout duration
9. Top 10 most-checked-out books
10. Checkout trend over time (time series)

**Operational Health Metrics:**
11. Overdue books count
12. Books overdue >30 days (critical)

### Backend API Design

**Endpoint structure:**

```
GET /api/v1/analytics/summary          → High-level KPIs
GET /api/v1/analytics/circulation      → Circulation metrics
GET /api/v1/analytics/inventory        → Inventory metrics
GET /api/v1/analytics/trends?period=30 → Time-series data
GET /api/v1/analytics/top-books?limit=10 → Popular books
```

### Create Analytics DTOs

**File:** `src/main/java/com/penrose/bibby/library/analytics/AnalyticsSummaryDTO.java`

```java
package com.penrose.bibby.library.analytics;

import java.time.LocalDateTime;

/**
 * High-level KPIs for dashboard overview.
 */
public record AnalyticsSummaryDTO(
    // Inventory
    long totalBooks,
    long availableBooks,
    long checkedOutBooks,
    long lostBooks,
    long archivedBooks,

    // Circulation
    long activeCheckouts,
    long checkoutsThisMonth,
    long returnsThisMonth,

    // Health
    long overdueBooks,
    long criticalOverdue,  // >30 days

    // Metadata
    LocalDateTime generatedAt
) {}
```

**File:** `src/main/java/com/penrose/bibby/library/analytics/CirculationMetricsDTO.java`

```java
package com.penrose.bibby.library.analytics;

/**
 * Detailed circulation statistics.
 */
public record CirculationMetricsDTO(
    long checkoutsThisWeek,
    long checkoutsThisMonth,
    long checkoutsThisYear,
    long returnsThisWeek,
    long returnsThisMonth,
    long returnsThisYear,
    double averageCheckoutDurationDays,
    long totalCheckoutsAllTime
) {}
```

**File:** `src/main/java/com/penrose/bibby/library/analytics/BookPopularityDTO.java`

```java
package com.penrose.bibby.library.analytics;

/**
 * Book popularity ranking.
 */
public record BookPopularityDTO(
    Long bookId,
    String title,
    String isbn,
    long checkoutCount,
    double averageRating  // Future: if you add ratings
) {}
```

**File:** `src/main/java/com/penrose/bibby/library/analytics/TrendDataPointDTO.java`

```java
package com.penrose.bibby.library.analytics;

import java.time.LocalDate;

/**
 * Single point in time-series data.
 */
public record TrendDataPointDTO(
    LocalDate date,
    long checkoutCount,
    long returnCount,
    long activeCheckouts
) {}
```

### Analytics Service

**File:** `src/main/java/com/penrose/bibby/library/analytics/AnalyticsService.java`

```java
package com.penrose.bibby.library.analytics;

import com.penrose.bibby.library.book.BookRepository;
import com.penrose.bibby.library.book.BookStatus;
import com.penrose.bibby.library.checkout.CheckoutRepository;
import com.penrose.bibby.library.checkout.CheckoutStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating analytics and reports.
 *
 * Industrial analogy: SCADA reporting engine.
 * Pre-computes metrics, caches results, serves to dashboard.
 */
@Service
public class AnalyticsService {

    private final BookRepository bookRepository;
    private final CheckoutRepository checkoutRepository;

    public AnalyticsService(BookRepository bookRepository,
                           CheckoutRepository checkoutRepository) {
        this.bookRepository = bookRepository;
        this.checkoutRepository = checkoutRepository;
    }

    /**
     * Get high-level summary KPIs.
     * Cached for 5 minutes to reduce database load.
     */
    @Cacheable(value = "analyticsSummary", unless = "#result == null")
    public AnalyticsSummaryDTO getSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0);

        return new AnalyticsSummaryDTO(
            // Inventory counts
            bookRepository.count(),
            bookRepository.countByStatus(BookStatus.AVAILABLE),
            bookRepository.countByStatus(BookStatus.CHECKED_OUT),
            bookRepository.countByStatus(BookStatus.LOST),
            bookRepository.countByStatus(BookStatus.ARCHIVED),

            // Circulation
            checkoutRepository.countByStatus(CheckoutStatus.ACTIVE),
            checkoutRepository.countByCheckedOutAtBetween(monthStart, now),
            checkoutRepository.countByReturnedAtBetween(monthStart, now),

            // Health metrics
            checkoutRepository.countOverdue(now),
            checkoutRepository.countCriticalOverdue(now.minusDays(30)),

            // Metadata
            now
        );
    }

    /**
     * Get detailed circulation metrics.
     */
    @Cacheable(value = "circulationMetrics")
    public CirculationMetricsDTO getCirculationMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime monthStart = now.withDayOfMonth(1);
        LocalDateTime yearStart = now.withDayOfYear(1);

        // Calculate average checkout duration
        List<Long> durations = checkoutRepository.findAll().stream()
            .filter(c -> c.getReturnedAt() != null)
            .map(c -> ChronoUnit.DAYS.between(
                c.getCheckedOutAt(), c.getReturnedAt()))
            .collect(Collectors.toList());

        double avgDuration = durations.isEmpty() ? 0.0 :
            durations.stream().mapToLong(Long::longValue).average().orElse(0.0);

        return new CirculationMetricsDTO(
            checkoutRepository.countByCheckedOutAtBetween(weekStart, now),
            checkoutRepository.countByCheckedOutAtBetween(monthStart, now),
            checkoutRepository.countByCheckedOutAtBetween(yearStart, now),
            checkoutRepository.countByReturnedAtBetween(weekStart, now),
            checkoutRepository.countByReturnedAtBetween(monthStart, now),
            checkoutRepository.countByReturnedAtBetween(yearStart, now),
            avgDuration,
            checkoutRepository.count()
        );
    }

    /**
     * Get top N most popular books by checkout count.
     */
    @Cacheable(value = "topBooks", key = "#limit")
    public List<BookPopularityDTO> getTopBooks(int limit) {
        return bookRepository.findTopByCheckoutCount(limit).stream()
            .map(book -> new BookPopularityDTO(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getCheckoutCount(),
                0.0  // Placeholder for future ratings
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get circulation trend data for time series chart.
     *
     * @param days number of days to include
     */
    @Cacheable(value = "trendData", key = "#days")
    public List<TrendDataPointDTO> getTrendData(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        return startDate.datesUntil(endDate.plusDays(1))
            .map(date -> {
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

                long checkouts = checkoutRepository.countByCheckedOutAtBetween(
                    dayStart, dayEnd);
                long returns = checkoutRepository.countByReturnedAtBetween(
                    dayStart, dayEnd);

                // Active checkouts at end of day
                long active = checkoutRepository.countActiveAtDate(dayEnd);

                return new TrendDataPointDTO(date, checkouts, returns, active);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get shelf utilization metrics.
     */
    public List<ShelfUtilizationDTO> getShelfUtilization() {
        // Implementation depends on your shelf structure
        // Return list of shelves with: id, capacity, current count, utilization %
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

### Add Repository Query Methods

**File:** `src/main/java/com/penrose/bibby/library/book/BookRepository.java` (add methods)

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    // Existing methods...

    // Analytics queries
    long countByStatus(BookStatus status);

    @Query("SELECT b FROM BookEntity b ORDER BY b.checkoutCount DESC")
    List<BookEntity> findTopByCheckoutCount(Pageable pageable);

    // Helper method to get top N
    default List<BookEntity> findTopByCheckoutCount(int limit) {
        return findTopByCheckoutCount(PageRequest.of(0, limit));
    }
}
```

**File:** `src/main/java/com/penrose/bibby/library/checkout/CheckoutRepository.java` (add methods)

```java
@Repository
public interface CheckoutRepository extends JpaRepository<CheckoutEntity, Long> {

    // Existing methods...

    // Analytics queries
    long countByStatus(CheckoutStatus status);

    long countByCheckedOutAtBetween(LocalDateTime start, LocalDateTime end);

    long countByReturnedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(c) FROM CheckoutEntity c " +
           "WHERE c.status = 'ACTIVE' AND c.dueDate < :now")
    long countOverdue(LocalDateTime now);

    @Query("SELECT COUNT(c) FROM CheckoutEntity c " +
           "WHERE c.status = 'ACTIVE' AND c.dueDate < :cutoffDate")
    long countCriticalOverdue(LocalDateTime cutoffDate);

    @Query("SELECT COUNT(c) FROM CheckoutEntity c " +
           "WHERE c.status = 'ACTIVE' AND c.checkedOutAt <= :date")
    long countActiveAtDate(LocalDateTime date);
}
```

### Analytics REST Controller

**File:** `src/main/java/com/penrose/bibby/library/analytics/AnalyticsController.java`

```java
package com.penrose.bibby.library.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for analytics and reporting.
 *
 * Industrial analogy: SCADA reporting API.
 * Provides pre-computed metrics for dashboard consumption.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Dashboard metrics and reporting")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get high-level KPI summary.
     *
     * GET /api/v1/analytics/summary
     *
     * Returns key metrics for dashboard overview:
     * - Inventory counts by status
     * - Active checkouts
     * - Overdue books
     */
    @Operation(
        summary = "Get analytics summary",
        description = "High-level KPIs for dashboard overview. Cached for 5 minutes."
    )
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDTO> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }

    /**
     * Get detailed circulation metrics.
     *
     * GET /api/v1/analytics/circulation
     */
    @Operation(
        summary = "Get circulation metrics",
        description = "Detailed circulation statistics: checkouts/returns by week/month/year"
    )
    @GetMapping("/circulation")
    public ResponseEntity<CirculationMetricsDTO> getCirculationMetrics() {
        return ResponseEntity.ok(analyticsService.getCirculationMetrics());
    }

    /**
     * Get top N most popular books.
     *
     * GET /api/v1/analytics/top-books?limit=10
     */
    @Operation(
        summary = "Get popular books",
        description = "Books ranked by checkout count"
    )
    @GetMapping("/top-books")
    public ResponseEntity<List<BookPopularityDTO>> getTopBooks(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopBooks(limit));
    }

    /**
     * Get circulation trend data.
     *
     * GET /api/v1/analytics/trends?days=30
     *
     * Returns time-series data for charting.
     */
    @Operation(
        summary = "Get circulation trends",
        description = "Time-series data for circulation trends over specified period"
    )
    @GetMapping("/trends")
    public ResponseEntity<List<TrendDataPointDTO>> getTrends(
            @RequestParam(defaultValue = "30") int days) {

        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("Days must be between 1 and 365");
        }

        return ResponseEntity.ok(analyticsService.getTrendData(days));
    }
}
```

### Testing Analytics Endpoints

**File:** `src/test/java/com/penrose/bibby/library/analytics/AnalyticsControllerIntegrationTest.java`

```java
package com.penrose.bibby.library.analytics;

import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.book.BookRepository;
import com.penrose.bibby.library.book.BookStatus;
import com.penrose.bibby.library.checkout.CheckoutEntity;
import com.penrose.bibby.library.checkout.CheckoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CheckoutRepository checkoutRepository;

    @BeforeEach
    void setUp() {
        // Create test data
        for (int i = 0; i < 50; i++) {
            BookEntity book = new BookEntity();
            book.setTitle("Book " + i);
            book.setIsbn("978-123456789" + i);
            book.setStatus(i < 30 ? BookStatus.AVAILABLE : BookStatus.CHECKED_OUT);
            book.setCheckoutCount(i * 2);  // Varying popularity
            bookRepository.save(book);
        }

        // Create checkouts
        BookEntity book = bookRepository.findAll().get(0);
        CheckoutEntity checkout = new CheckoutEntity();
        checkout.setBook(book);
        checkout.setPatronName("Test Patron");
        checkout.setPatronEmail("test@example.com");
        checkout.setCheckedOutAt(LocalDateTime.now().minusDays(7));
        checkout.setDueDate(LocalDateTime.now().plusDays(7));
        checkoutRepository.save(checkout);
    }

    @Test
    void getSummary_ReturnsCorrectCounts() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBooks").value(50))
            .andExpect(jsonPath("$.availableBooks").value(30))
            .andExpect(jsonPath("$.checkedOutBooks").value(20))
            .andExpect(jsonPath("$.activeCheckouts").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    void getTopBooks_ReturnsLimitedResults() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/top-books?limit=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[0].checkoutCount",
                greaterThanOrEqualTo(jsonPath("$[1].checkoutCount").value(Long.class))));
    }

    @Test
    void getTrends_Returns30DaysOfData() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends?days=30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(31)))  // Inclusive of start/end
            .andExpect(jsonPath("$[0].date").exists())
            .andExpect(jsonPath("$[0].checkoutCount").exists());
    }

    @Test
    void getTrends_WithInvalidDays_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends?days=500"))
            .andExpect(status().isBadRequest());
    }
}
```

---

## Part 5: Scheduled Report Generation

For expensive analytics, pre-compute and cache results on a schedule instead of on-demand.

**File:** `src/main/java/com/penrose/bibby/library/analytics/AnalyticsScheduler.java`

```java
package com.penrose.bibby.library.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for pre-computing analytics.
 *
 * Industrial analogy: SCADA historian - pre-aggregates data for fast retrieval.
 */
@Component
public class AnalyticsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsScheduler.class);

    private final AnalyticsService analyticsService;
    private final CacheManager cacheManager;

    public AnalyticsScheduler(AnalyticsService analyticsService,
                             CacheManager cacheManager) {
        this.analyticsService = analyticsService;
        this.cacheManager = cacheManager;
    }

    /**
     * Pre-compute analytics summary every 5 minutes.
     * Warms cache before dashboard requests arrive.
     */
    @Scheduled(fixedRate = 300000)  // 5 minutes
    public void preComputeSummary() {
        logger.info("Pre-computing analytics summary");

        // Clear existing cache
        var cache = cacheManager.getCache("analyticsSummary");
        if (cache != null) {
            cache.clear();
        }

        // Compute fresh data (populates cache)
        analyticsService.getSummary();

        logger.info("Analytics summary pre-computed successfully");
    }

    /**
     * Update overdue status daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
    public void updateOverdueStatuses() {
        logger.info("Updating overdue checkout statuses");
        // Implementation: Find active checkouts past due, update status
        logger.info("Overdue status update complete");
    }
}
```

**Enable scheduling:**

**File:** `src/main/java/com/penrose/bibby/config/SchedulingConfig.java`

```java
package com.penrose.bibby.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

---

## Part 6: Export Functionality

Librarians need to export reports for offline analysis.

**File:** `src/main/java/com/penrose/bibby/library/analytics/ExportController.java`

```java
package com.penrose.bibby.library.analytics;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Export analytics data to CSV.
 */
@RestController
@RequestMapping("/api/v1/analytics/export")
public class ExportController {

    private final AnalyticsService analyticsService;

    public ExportController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Export circulation trends as CSV.
     *
     * GET /api/v1/analytics/export/trends?days=30
     */
    @GetMapping("/trends")
    public ResponseEntity<String> exportTrendsCSV(
            @RequestParam(defaultValue = "30") int days) {

        List<TrendDataPointDTO> data = analyticsService.getTrendData(days);

        // Generate CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Checkouts,Returns,Active\n");

        data.forEach(point -> csv.append(String.format("%s,%d,%d,%d\n",
            point.date(),
            point.checkoutCount(),
            point.returnCount(),
            point.activeCheckouts()
        )));

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=circulation-trends-" + LocalDate.now() + ".csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.toString());
    }

    /**
     * Export popular books as CSV.
     */
    @GetMapping("/top-books")
    public ResponseEntity<String> exportTopBooksCSV(
            @RequestParam(defaultValue = "10") int limit) {

        List<BookPopularityDTO> books = analyticsService.getTopBooks(limit);

        StringBuilder csv = new StringBuilder();
        csv.append("Rank,Title,ISBN,Checkout Count\n");

        int rank = 1;
        for (BookPopularityDTO book : books) {
            csv.append(String.format("%d,\"%s\",%s,%d\n",
                rank++,
                book.title().replace("\"", "\"\""),  // Escape quotes
                book.isbn(),
                book.checkoutCount()
            ));
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=top-books-" + LocalDate.now() + ".csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.toString());
    }
}
```

---

## Action Items for Week 10

### Critical (Must Complete)

**1. Implement Analytics Service** (8-10 hours)
- Create all DTOs (Summary, Circulation, Popularity, TrendData)
- Implement `AnalyticsService` with all metrics methods
- Add repository query methods for analytics
- Configure caching for analytics endpoints

**Deliverable:** Working analytics service with 12+ metrics

**2. Build Analytics REST API** (4-5 hours)
- Create `AnalyticsController` with 4 endpoints
- Add OpenAPI documentation
- Test endpoints with Postman/curl

**Deliverable:** Complete REST API for analytics

**3. Write Integration Tests** (4-5 hours)
- Test summary endpoint with varying data
- Test top books ranking
- Test trend data generation
- Test edge cases (empty data, invalid params)

**Deliverable:** 80%+ test coverage on analytics endpoints

**4. Implement Scheduled Pre-computation** (2-3 hours)
- Create `AnalyticsScheduler`
- Configure `@Scheduled` jobs
- Test cache warming

**Deliverable:** Analytics summary pre-computed every 5 minutes

**5. Add CSV Export** (3-4 hours)
- Implement `ExportController`
- Generate CSV for trends and top books
- Test download functionality

**Deliverable:** Working CSV export endpoints

### Important (Should Complete)

**6. Add More Metrics** (3-4 hours)
- Shelf utilization
- Genre distribution
- Author popularity
- Overdue trends

**7. Performance Testing** (2-3 hours)
- Benchmark analytics queries without cache
- Measure improvement with caching
- Document query times

**8. Error Handling** (2 hours)
- Handle empty dataset gracefully
- Validate date ranges
- Return sensible defaults

### Bonus (If Time Permits)

**9. Frontend Dashboard Prep** (3-4 hours)
- Research React + Chart.js
- Design dashboard mockup
- Plan component structure

**10. Advanced Analytics** (5-6 hours)
- Moving averages for trend smoothing
- Anomaly detection (circulation spike/drop)
- Forecast next month's circulation

---

## Success Metrics for Week 10

By the end of this week, you should have:

✅ **Complete Analytics Backend:**
- 12+ metrics available via REST API
- Efficient queries with proper indexing
- Redis caching for expensive operations

✅ **Scheduled Pre-computation:**
- Analytics summary refreshed every 5 minutes
- Overdue status updated daily

✅ **Export Functionality:**
- CSV export for trends and popular books
- Proper headers for browser download

✅ **Testing:**
- Integration tests for all endpoints
- Performance benchmarks documented

✅ **Documentation:**
- OpenAPI docs for all analytics endpoints
- README section explaining analytics features

---

## What's Next

**Section 11: Signature Project (Phase 2) — Frontend Dashboard**

Next week, we'll build the React frontend dashboard to visualize these analytics:
- KPI cards for at-a-glance metrics
- Line charts for circulation trends
- Bar charts for popular books
- Real-time updates with polling/WebSocket

**Section 12: Signature Project (Phase 3) — Advanced Features**

Final week, we'll add sophistication:
- Predictive alerts (circulation dropping, inventory low)
- Drill-down capability (click metric to see details)
- Custom date range filtering
- User preferences (save dashboard configuration)

---

## Industrial Connection: Why Analytics Dashboards Matter

### Operational Visibility = Business Value

In industrial environments, dashboards aren't optional—they're critical:

**SCADA/HMI Dashboards:**
- Real-time equipment status
- KPIs: uptime, throughput, efficiency
- Trend analysis for predictive maintenance
- Alert thresholds

**Bibby Analytics Dashboard:**
- Real-time library status
- KPIs: circulation, inventory health, overdue rate
- Trend analysis for acquisition planning
- Alert thresholds (inventory low, overdue spike)

Both enable **data-driven decision making** instead of gut feel.

### Interview Talking Points

When presenting this feature:

> "I built an analytics dashboard for library operations. It reminded me of SCADA interfaces I used at Kinder Morgan—at-a-glance KPIs, trend visualization, drill-down capability. The challenge was balancing freshness vs performance. I solved it with scheduled pre-computation: expensive aggregations run every 5 minutes and cache results, reducing dashboard load time from 2 seconds to 50ms. The export functionality lets librarians generate reports for monthly reviews, similar to how we generated pipeline operation reports."

**This demonstrates:**
- Connection to your background
- Problem-solving (performance optimization)
- Production thinking (caching strategy)
- Measurable impact (2s → 50ms)

---

**Progress Tracker:** 10/32 sections complete (31%)

**Next Section:** Signature Project (Phase 2) — Frontend dashboard with React and Chart.js
