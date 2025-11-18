# Section 13: Secondary Data/Analytics Project
**Building Breadth to Complement Your Signature**

**Week 13: Demonstrating Versatility**

---

## Overview

Your analytics dashboard (Sections 10-12) is a strong signature project showing depth in full-stack development, caching, and performance optimization. But relying on a single project has risks:
- What if the interviewer wants to see different skills?
- What if the role emphasizes data pipelines over dashboards?
- How do you demonstrate breadth beyond web applications?

A **secondary project** addresses this. It should be:
- **Complementary:** Shows different skills than the signature project
- **Quick:** 1 week implementation (40-50 hours max)
- **Targeted:** Fills specific resume gaps or demonstrates niche skills
- **Story-worthy:** Has clear purpose and measurable outcome

This section helps you choose and implement a secondary project that rounds out your portfolio.

**This Week's Focus:**
- Identify skill gaps in your current portfolio
- Choose from 4 secondary project options
- Implement selected project
- Document for portfolio presentation

---

## Part 1: Why a Secondary Project Matters

### The "One-Trick Pony" Problem

**Scenario:**
> **Interviewer:** "Great dashboard! What else have you built?"
>
> **You:** "Well, I also have the book management features in Bibby..."
>
> **Interviewer:** *Thinks: Same project, same tech stack. Does this candidate know anything beyond Spring Boot CRUD?*

**Better scenario:**
> **Interviewer:** "Great dashboard! What else have you built?"
>
> **You:** "I also built a daily book recommendation email service that pulls data from Google Books API, processes it with batch jobs, and sends personalized suggestions. Completely different architecture—scheduled jobs, external API integration, email templating."
>
> **Interviewer:** *Thinks: This person can work across different problem spaces.*

### Portfolio Diversification

Think of your portfolio like an investment portfolio:
- **Signature project = Large cap stock:** Safe, proven, shows mastery
- **Secondary project = Small cap stock:** Shows versatility, risk-taking
- **Combined:** Demonstrates both depth and breadth

### Industrial Analogy: Cross-Training

In Navy and pipeline operations, you weren't just specialized in one system:
- **Primary responsibility:** Petroleum systems / pipeline operations
- **Cross-training:** Electrical systems, safety protocols, emergency response

Why? **Versatility makes you more valuable.** Same in software:
- **Primary strength:** Full-stack web applications (Bibby dashboard)
- **Secondary strength:** Data integration, batch processing, or API design

---

## Part 2: Skill Gap Analysis

### What Does Your Resume Currently Show?

**After Sections 1-12, your portfolio demonstrates:**

✅ **Strong:**
- Full-stack web development (React + Spring Boot)
- REST API design
- Database modeling (PostgreSQL, JPA)
- Caching strategies (Redis)
- Frontend data visualization (Recharts)
- Performance optimization
- Production deployment

❌ **Gaps (potential):**
- External API integration
- Batch/scheduled processing
- Data transformation/ETL
- Asynchronous messaging (Kafka, RabbitMQ)
- File processing (CSV, JSON)
- Email/notification systems
- Command-line tools
- Containerization (Docker)

### Target Company Skill Requirements

**OSIsoft PI System / AVEVA:**
- Real-time data ingestion
- Time-series data processing
- **API integration with IoT devices**
- Batch aggregation

**Uptake / SparkCognition (Predictive Analytics):**
- **Data pipeline processing**
- ML model integration
- **Scheduled batch jobs**
- External data source integration

**Rockwell / Honeywell (Industrial Automation):**
- Protocol integration (OPC, Modbus)
- **Data transformation**
- Alert/notification systems
- Scheduled reporting

**Common theme:** Integration, batch processing, data pipelines

---

## Part 3: Four Secondary Project Options

### Option 1: Book Recommendation Email Service ⭐ **Recommended**

**The Problem:**
Patrons don't know what new books are available. Manual browsing is time-consuming.

**The Solution:**
Automated daily email with personalized book recommendations based on checkout history and new acquisitions.

**Technical Skills Demonstrated:**
- ✅ External API integration (Google Books API)
- ✅ Scheduled batch jobs (Spring @Scheduled)
- ✅ Email templating (Thymeleaf + JavaMailSender)
- ✅ Recommendation algorithm (simple collaborative filtering)
- ✅ Async processing (Spring @Async)

**Architecture:**
```
┌──────────────┐     ┌────────────────┐     ┌──────────────┐
│  Scheduled   │────▶│  Fetch New     │────▶│ Google Books │
│  Job (Daily) │     │  Books from DB │     │     API      │
└──────────────┘     └────────────────┘     └──────────────┘
                              │
                              ▼
                     ┌────────────────┐
                     │  Recommendation│
                     │    Algorithm   │
                     └────────────────┘
                              │
                              ▼
                     ┌────────────────┐     ┌──────────────┐
                     │  Email Template│────▶│  SMTP Server │
                     │    Generator   │     │   (Gmail)    │
                     └────────────────┘     └──────────────┘
```

**Industrial Connection:**
Similar to automated SCADA reports—scheduled job pulls sensor data, analyzes trends, generates PDF report, emails to operations team.

**Implementation Time:** 30-40 hours

---

### Option 2: Library Circulation Data Import Tool

**The Problem:**
Librarians have historical circulation data in CSV files from old system. Need to import into Bibby.

**The Solution:**
CLI tool that reads CSV files, validates data, transforms to Bibby format, and bulk imports to database.

**Technical Skills Demonstrated:**
- ✅ File processing (CSV parsing with OpenCSV)
- ✅ Data validation and transformation
- ✅ Bulk database operations
- ✅ Error handling and rollback
- ✅ Progress reporting

**Architecture:**
```
┌──────────────┐     ┌────────────────┐     ┌──────────────┐
│  CSV File    │────▶│  Parser &      │────▶│  Validation  │
│  (Legacy)    │     │  Transformer   │     │  & Mapping   │
└──────────────┘     └────────────────┘     └──────────────┘
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │  Batch       │
                                            │  Insert      │
                                            │  (JDBC)      │
                                            └──────────────┘
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │  PostgreSQL  │
                                            └──────────────┘
```

**Industrial Connection:**
Data migration from legacy SCADA system to modern platform—same challenges (schema differences, data quality, error handling).

**Implementation Time:** 25-35 hours

---

### Option 3: Overdue Notification System

**The Problem:**
Patrons forget when books are due. Manual reminder calls are time-consuming.

**The Solution:**
Automated reminder system: 3 days before due, on due date, and at 7/14/30 days overdue.

**Technical Skills Demonstrated:**
- ✅ Scheduled batch jobs
- ✅ Email/SMS notification
- ✅ Template rendering (personalized messages)
- ✅ State management (last notification sent)
- ✅ Rate limiting (don't spam patrons)

**Architecture:**
```
┌──────────────┐     ┌────────────────┐
│  Scheduled   │────▶│  Query Due     │
│  Job (Daily) │     │  Checkouts     │
└──────────────┘     └────────────────┘
                              │
                              ▼
                     ┌────────────────┐
                     │  Determine     │
                     │  Notification  │
                     │  Type          │
                     └────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
     ┌────────────────┐              ┌────────────────┐
     │  Email Service │              │  SMS Service   │
     │  (SendGrid)    │              │  (Twilio)      │
     └────────────────┘              └────────────────┘
```

**Industrial Connection:**
Alarm notification system in SCADA—threshold exceeded triggers alert via email/SMS to on-call engineer.

**Implementation Time:** 30-40 hours

---

### Option 4: Book Metadata Enrichment Service

**The Problem:**
Books in Bibby have minimal metadata (title, ISBN). Librarians want cover images, descriptions, ratings.

**The Solution:**
Background job that fetches metadata from Google Books API and enriches book records.

**Technical Skills Demonstrated:**
- ✅ External API integration
- ✅ Rate limiting and retry logic
- ✅ Image download and storage
- ✅ Background job processing
- ✅ Progress tracking

**Architecture:**
```
┌──────────────┐     ┌────────────────┐     ┌──────────────┐
│  Manual      │────▶│  Queue Books   │────▶│ Google Books │
│  Trigger     │     │  Needing       │     │     API      │
└──────────────┘     │  Metadata      │     │ (rate limit) │
                     └────────────────┘     └──────────────┘
                              │                      │
                              │                      ▼
                              │              ┌──────────────┐
                              │              │  Download    │
                              │              │  Cover Image │
                              │              └──────────────┘
                              ▼                      │
                     ┌────────────────┐              │
                     │  Update Book   │◀─────────────┘
                     │  Entity        │
                     └────────────────┘
```

**Industrial Connection:**
Asset enrichment in EAM systems—equipment record has ID/model, but maintenance system fetches detailed specs, manuals, parts lists from vendor APIs.

**Implementation Time:** 25-35 hours

---

## Part 4: Recommended Choice and Implementation

### Recommendation: Book Recommendation Email Service

**Why this option:**
1. **Skills complement dashboard:** Dashboard = web UI, Email service = batch processing
2. **Common enterprise pattern:** Scheduled reports/alerts
3. **Full workflow demonstration:** API integration → data processing → templating → delivery
4. **Interview story:** Easy to explain, clear business value
5. **Aligns with target companies:** Uptake, SparkCognition do batch analytics

---

## Part 5: Implementation Guide — Recommendation Email Service

### Phase 1: Foundation (8-10 hours)

#### Set Up Email Configuration

**Add dependencies to `pom.xml`:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

**Configure email in `application.properties`:**

```properties
# Email configuration (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${GMAIL_USERNAME}
spring.mail.password=${GMAIL_APP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Recommendation service
recommendation.enabled=${RECOMMENDATION_ENABLED:true}
recommendation.cron=${RECOMMENDATION_CRON:0 0 8 * * *}  # Daily at 8 AM
```

**Note:** For Gmail, you need an [App Password](https://support.google.com/accounts/answer/185833), not your regular password.

#### Create Patron Entity

**File:** `src/main/java/com/penrose/bibby/library/patron/PatronEntity.java`

```java
package com.penrose.bibby.library.patron;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patrons")
public class PatronEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "receive_recommendations")
    private boolean receiveRecommendations = true;

    @Column(name = "last_recommendation_sent")
    private LocalDateTime lastRecommendationSent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isReceiveRecommendations() { return receiveRecommendations; }
    public void setReceiveRecommendations(boolean receiveRecommendations) {
        this.receiveRecommendations = receiveRecommendations;
    }

    public LocalDateTime getLastRecommendationSent() { return lastRecommendationSent; }
    public void setLastRecommendationSent(LocalDateTime lastRecommendationSent) {
        this.lastRecommendationSent = lastRecommendationSent;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

**Migration:**

**File:** `src/main/resources/db/migration/V4__add_patrons_table.sql`

```sql
-- Add patrons table for tracking users

CREATE TABLE patrons (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    receive_recommendations BOOLEAN DEFAULT TRUE,
    last_recommendation_sent TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_patrons_email ON patrons(email);
CREATE INDEX idx_patrons_recommendations ON patrons(receive_recommendations)
    WHERE receive_recommendations = TRUE;

-- Update checkouts table to reference patron instead of storing name/email
ALTER TABLE checkouts ADD COLUMN patron_id BIGINT;
ALTER TABLE checkouts ADD CONSTRAINT fk_checkout_patron
    FOREIGN KEY (patron_id) REFERENCES patrons(id);

COMMENT ON TABLE patrons IS 'Library patrons who can check out books and receive recommendations';
```

### Phase 2: Google Books API Integration (8-10 hours)

#### Create Google Books API Client

**File:** `src/main/java/com/penrose/bibby/integration/googlebooks/GoogleBooksClient.java`

```java
package com.penrose.bibby.integration.googlebooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Client for Google Books API.
 *
 * Industrial analogy: SCADA protocol driver for external devices.
 * Handles communication, retries, error handling.
 */
@Component
public class GoogleBooksClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleBooksClient.class);
    private static final String API_BASE_URL = "https://www.googleapis.com/books/v1";

    @Value("${google.books.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GoogleBooksClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Search books by query.
     *
     * @param query search terms (e.g., "Java programming")
     * @param maxResults max number of results (1-40)
     * @return list of book info
     */
    public List<GoogleBookInfo> searchBooks(String query, int maxResults) {
        logger.info("Searching Google Books API: query={}, maxResults={}", query, maxResults);

        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/volumes")
            .queryParam("q", query)
            .queryParam("maxResults", Math.min(maxResults, 40))
            .queryParam("key", apiKey)
            .build()
            .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("items")) {
                logger.warn("No results from Google Books API for query: {}", query);
                return List.of();
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            return items.stream()
                .map(this::parseBookInfo)
                .toList();

        } catch (Exception e) {
            logger.error("Error calling Google Books API", e);
            return List.of();
        }
    }

    /**
     * Get book by ISBN.
     */
    public GoogleBookInfo getBookByISBN(String isbn) {
        logger.info("Fetching book from Google Books API: isbn={}", isbn);

        List<GoogleBookInfo> results = searchBooks("isbn:" + isbn, 1);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Parse Google Books API response into our DTO.
     */
    private GoogleBookInfo parseBookInfo(Map<String, Object> item) {
        Map<String, Object> volumeInfo = (Map<String, Object>) item.get("volumeInfo");

        String title = (String) volumeInfo.get("title");
        List<String> authors = (List<String>) volumeInfo.getOrDefault("authors", List.of());
        String description = (String) volumeInfo.get("description");
        String publisher = (String) volumeInfo.get("publisher");
        String publishedDate = (String) volumeInfo.get("publishedDate");

        // Extract ISBN
        String isbn = extractISBN(volumeInfo);

        // Extract cover image
        String coverImageUrl = null;
        if (volumeInfo.containsKey("imageLinks")) {
            Map<String, String> imageLinks = (Map<String, String>) volumeInfo.get("imageLinks");
            coverImageUrl = imageLinks.getOrDefault("thumbnail", imageLinks.get("smallThumbnail"));
        }

        // Extract categories/genres
        List<String> categories = (List<String>) volumeInfo.getOrDefault("categories", List.of());

        return new GoogleBookInfo(
            title,
            authors,
            isbn,
            description,
            publisher,
            publishedDate,
            coverImageUrl,
            categories
        );
    }

    private String extractISBN(Map<String, Object> volumeInfo) {
        if (!volumeInfo.containsKey("industryIdentifiers")) {
            return null;
        }

        List<Map<String, String>> identifiers =
            (List<Map<String, String>>) volumeInfo.get("industryIdentifiers");

        // Prefer ISBN_13, fall back to ISBN_10
        for (Map<String, String> id : identifiers) {
            if ("ISBN_13".equals(id.get("type"))) {
                return id.get("identifier");
            }
        }

        for (Map<String, String> id : identifiers) {
            if ("ISBN_10".equals(id.get("type"))) {
                return id.get("identifier");
            }
        }

        return null;
    }
}
```

**DTO:**

```java
package com.penrose.bibby.integration.googlebooks;

import java.util.List;

public record GoogleBookInfo(
    String title,
    List<String> authors,
    String isbn,
    String description,
    String publisher,
    String publishedDate,
    String coverImageUrl,
    List<String> categories
) {}
```

**Configuration:**

```properties
# Google Books API
google.books.api.key=${GOOGLE_BOOKS_API_KEY}
```

**Get API key:** https://console.cloud.google.com/apis/credentials

### Phase 3: Recommendation Algorithm (10-12 hours)

#### Simple Collaborative Filtering

**File:** `src/main/java/com/penrose/bibby/recommendation/RecommendationService.java`

```java
package com.penrose.bibby.recommendation;

import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.book.BookRepository;
import com.penrose.bibby.library.checkout.CheckoutRepository;
import com.penrose.bibby.library.patron.PatronEntity;
import com.penrose.bibby.integration.googlebooks.GoogleBooksClient;
import com.penrose.bibby.integration.googlebooks.GoogleBookInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating book recommendations.
 *
 * Algorithm: Simple collaborative filtering
 * - Find books patron checked out
 * - Find other patrons who checked out same books
 * - Recommend books those patrons liked that target patron hasn't read
 *
 * Industrial analogy: Predictive maintenance pattern recognition.
 * "Equipment A and B often fail together. If A fails, inspect B."
 */
@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private static final int MAX_RECOMMENDATIONS = 5;

    private final CheckoutRepository checkoutRepository;
    private final BookRepository bookRepository;
    private final GoogleBooksClient googleBooksClient;

    public RecommendationService(CheckoutRepository checkoutRepository,
                                BookRepository bookRepository,
                                GoogleBooksClient googleBooksClient) {
        this.checkoutRepository = checkoutRepository;
        this.bookRepository = bookRepository;
        this.googleBooksClient = googleBooksClient;
    }

    /**
     * Generate recommendations for a patron.
     *
     * @param patron patron to generate recommendations for
     * @return list of recommended books
     */
    public List<BookRecommendation> getRecommendations(PatronEntity patron) {
        logger.info("Generating recommendations for patron: {}", patron.getEmail());

        // Step 1: Get books this patron has checked out
        Set<Long> patronBookIds = checkoutRepository
            .findByPatronEmailAndStatus(patron.getEmail(), CheckoutStatus.RETURNED)
            .stream()
            .map(checkout -> checkout.getBook().getId())
            .collect(Collectors.toSet());

        if (patronBookIds.isEmpty()) {
            logger.info("Patron has no checkout history, using popular books");
            return getPopularBooks();
        }

        // Step 2: Find similar patrons (who read same books)
        Map<String, Integer> similarPatrons = new HashMap<>();

        for (Long bookId : patronBookIds) {
            List<String> otherPatrons = checkoutRepository
                .findByBookIdAndStatusReturned(bookId)
                .stream()
                .map(CheckoutEntity::getPatronEmail)
                .filter(email -> !email.equals(patron.getEmail()))
                .toList();

            for (String otherPatron : otherPatrons) {
                similarPatrons.merge(otherPatron, 1, Integer::sum);
            }
        }

        // Step 3: Get books those similar patrons read
        Map<Long, Integer> candidateBooks = new HashMap<>();

        similarPatrons.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)  // Top 10 similar patrons
            .forEach(entry -> {
                String similarPatron = entry.getKey();
                int similarity = entry.getValue();

                checkoutRepository
                    .findByPatronEmailAndStatus(similarPatron, CheckoutStatus.RETURNED)
                    .stream()
                    .map(checkout -> checkout.getBook().getId())
                    .filter(bookId -> !patronBookIds.contains(bookId))  // Not already read
                    .forEach(bookId -> candidateBooks.merge(bookId, similarity, Integer::sum));
            });

        // Step 4: Rank and return top recommendations
        List<BookRecommendation> recommendations = candidateBooks.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(MAX_RECOMMENDATIONS)
            .map(entry -> {
                BookEntity book = bookRepository.findById(entry.getKey()).orElse(null);
                if (book == null) return null;

                return new BookRecommendation(
                    book.getId(),
                    book.getTitle(),
                    book.getIsbn(),
                    entry.getValue(),  // Score (number of similar patrons who read it)
                    "Readers like you also enjoyed this book"
                );
            })
            .filter(Objects::nonNull)
            .toList();

        logger.info("Generated {} recommendations for patron {}",
            recommendations.size(), patron.getEmail());

        return recommendations;
    }

    /**
     * Fallback: Popular books for new patrons.
     */
    private List<BookRecommendation> getPopularBooks() {
        return bookRepository.findTopByCheckoutCount(MAX_RECOMMENDATIONS)
            .stream()
            .map(book -> new BookRecommendation(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getCheckoutCount(),
                "Popular in our library"
            ))
            .toList();
    }

    /**
     * Enrich recommendation with external data from Google Books.
     */
    public BookRecommendation enrichRecommendation(BookRecommendation recommendation) {
        if (recommendation.isbn() == null) {
            return recommendation;
        }

        GoogleBookInfo bookInfo = googleBooksClient.getBookByISBN(recommendation.isbn());

        if (bookInfo == null) {
            return recommendation;
        }

        return new BookRecommendation(
            recommendation.bookId(),
            recommendation.title(),
            recommendation.isbn(),
            recommendation.score(),
            recommendation.reason(),
            bookInfo.description(),
            bookInfo.coverImageUrl(),
            bookInfo.categories()
        );
    }
}
```

**DTO:**

```java
package com.penrose.bibby.recommendation;

import java.util.List;

public record BookRecommendation(
    Long bookId,
    String title,
    String isbn,
    int score,
    String reason,
    String description,
    String coverImageUrl,
    List<String> categories
) {
    // Constructor for basic recommendation (without enrichment)
    public BookRecommendation(Long bookId, String title, String isbn, int score, String reason) {
        this(bookId, title, isbn, score, reason, null, null, List.of());
    }
}
```

### Phase 4: Email Templates and Sending (6-8 hours)

#### Email Template with Thymeleaf

**File:** `src/main/resources/templates/email/recommendations.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background-color: #3b82f6;
            color: white;
            padding: 20px;
            text-align: center;
            border-radius: 8px 8px 0 0;
        }
        .content {
            background-color: #f9fafb;
            padding: 20px;
        }
        .book {
            background-color: white;
            border: 1px solid #e5e7eb;
            border-radius: 8px;
            padding: 15px;
            margin-bottom: 15px;
            display: flex;
            gap: 15px;
        }
        .book-cover {
            width: 100px;
            height: 140px;
            object-fit: cover;
            border-radius: 4px;
        }
        .book-info {
            flex: 1;
        }
        .book-title {
            font-size: 18px;
            font-weight: bold;
            color: #1f2937;
            margin-bottom: 5px;
        }
        .book-reason {
            color: #6b7280;
            font-size: 14px;
            font-style: italic;
            margin-bottom: 10px;
        }
        .book-description {
            color: #4b5563;
            font-size: 14px;
            line-height: 1.4;
        }
        .footer {
            text-align: center;
            color: #6b7280;
            font-size: 12px;
            margin-top: 20px;
            padding-top: 20px;
            border-top: 1px solid #e5e7eb;
        }
        .cta-button {
            display: inline-block;
            background-color: #3b82f6;
            color: white;
            padding: 10px 20px;
            text-decoration: none;
            border-radius: 4px;
            margin-top: 10px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>📚 Your Daily Book Recommendations</h1>
        <p>Personalized suggestions just for you</p>
    </div>

    <div class="content">
        <p>Hi <span th:text="${patronName}">Patron</span>,</p>

        <p>Based on your reading history, we think you'll enjoy these books:</p>

        <div th:each="book : ${recommendations}" class="book">
            <img th:if="${book.coverImageUrl}"
                 th:src="${book.coverImageUrl}"
                 alt="Book cover"
                 class="book-cover">

            <div class="book-info">
                <div class="book-title" th:text="${book.title}">Book Title</div>
                <div class="book-reason" th:text="${book.reason}">Recommendation reason</div>
                <div class="book-description" th:text="${book.description}">Description</div>

                <a th:href="@{https://bibby.your-domain.com/books/{id}(id=${book.bookId})}"
                   class="cta-button">
                    Reserve This Book
                </a>
            </div>
        </div>

        <p>Happy reading!</p>
    </div>

    <div class="footer">
        <p>Bibby Library Management System</p>
        <p>
            <a href="#">Unsubscribe</a> |
            <a href="#">Manage Preferences</a>
        </p>
    </div>
</body>
</html>
```

#### Email Service

**File:** `src/main/java/com/penrose/bibby/notification/EmailService.java`

```java
package com.penrose.bibby.notification;

import com.penrose.bibby.recommendation.BookRecommendation;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

/**
 * Service for sending emails.
 *
 * Industrial analogy: SCADA notification dispatcher.
 * Sends alerts via multiple channels (email, SMS, push).
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Send recommendation email.
     * Async to avoid blocking scheduled job.
     *
     * @param patronName patron's name
     * @param patronEmail patron's email
     * @param recommendations list of book recommendations
     */
    @Async
    public void sendRecommendationEmail(String patronName, String patronEmail,
                                       List<BookRecommendation> recommendations) {
        logger.info("Sending recommendation email to: {}", patronEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(patronEmail);
            helper.setSubject("📚 Your Daily Book Recommendations");
            helper.setFrom("noreply@bibby.com");

            // Render HTML template
            Context context = new Context();
            context.setVariable("patronName", patronName);
            context.setVariable("recommendations", recommendations);

            String htmlContent = templateEngine.process("email/recommendations", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            logger.info("Recommendation email sent successfully to: {}", patronEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send email to: " + patronEmail, e);
        }
    }
}
```

### Phase 5: Scheduled Job (4-6 hours)

**File:** `src/main/java/com/penrose/bibby/recommendation/RecommendationScheduler.java`

```java
package com.penrose.bibby.recommendation;

import com.penrose.bibby.library.patron.PatronEntity;
import com.penrose.bibby.library.patron.PatronRepository;
import com.penrose.bibby.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for sending daily book recommendations.
 *
 * Industrial analogy: SCADA automated reporting.
 * Daily report generation + distribution to operations team.
 */
@Component
public class RecommendationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationScheduler.class);

    @Value("${recommendation.enabled}")
    private boolean enabled;

    private final PatronRepository patronRepository;
    private final RecommendationService recommendationService;
    private final EmailService emailService;

    public RecommendationScheduler(PatronRepository patronRepository,
                                  RecommendationService recommendationService,
                                  EmailService emailService) {
        this.patronRepository = patronRepository;
        this.recommendationService = recommendationService;
        this.emailService = emailService;
    }

    /**
     * Send daily recommendations.
     * Runs at 8 AM daily (configurable via cron expression).
     */
    @Scheduled(cron = "${recommendation.cron}")
    public void sendDailyRecommendations() {
        if (!enabled) {
            logger.info("Recommendation job is disabled");
            return;
        }

        logger.info("Starting daily recommendation job");

        List<PatronEntity> eligiblePatrons = patronRepository
            .findByReceiveRecommendationsTrue();

        logger.info("Found {} patrons to send recommendations", eligiblePatrons.size());

        int successCount = 0;
        int failureCount = 0;

        for (PatronEntity patron : eligiblePatrons) {
            try {
                // Generate recommendations
                List<BookRecommendation> recommendations =
                    recommendationService.getRecommendations(patron);

                if (recommendations.isEmpty()) {
                    logger.info("No recommendations for patron: {}", patron.getEmail());
                    continue;
                }

                // Enrich with external data
                List<BookRecommendation> enriched = recommendations.stream()
                    .map(recommendationService::enrichRecommendation)
                    .toList();

                // Send email (async)
                emailService.sendRecommendationEmail(
                    patron.getName(),
                    patron.getEmail(),
                    enriched
                );

                // Update last sent timestamp
                patron.setLastRecommendationSent(LocalDateTime.now());
                patronRepository.save(patron);

                successCount++;

            } catch (Exception e) {
                logger.error("Failed to send recommendations to: " + patron.getEmail(), e);
                failureCount++;
            }
        }

        logger.info("Recommendation job completed: success={}, failures={}",
            successCount, failureCount);
    }
}
```

---

## Part 6: Testing and Documentation

### Integration Test

**File:** `src/test/java/com/penrose/bibby/recommendation/RecommendationServiceTest.java`

```java
@SpringBootTest
@Transactional
class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CheckoutRepository checkoutRepository;

    @Test
    void getRecommendations_ForPatronWithHistory_ReturnsRelevantBooks() {
        // Arrange: Create test data
        PatronEntity patron1 = createPatron("patron1@test.com");
        PatronEntity patron2 = createPatron("patron2@test.com");

        BookEntity book1 = createBook("Clean Code");
        BookEntity book2 = createBook("Refactoring");
        BookEntity book3 = createBook("Design Patterns");

        // Patron1 read book1 and book2
        createCheckout(patron1, book1, CheckoutStatus.RETURNED);
        createCheckout(patron1, book2, CheckoutStatus.RETURNED);

        // Patron2 also read book1 and book2 (similar patron)
        createCheckout(patron2, book1, CheckoutStatus.RETURNED);
        createCheckout(patron2, book2, CheckoutStatus.RETURNED);

        // Patron2 also read book3 (should be recommended to patron1)
        createCheckout(patron2, book3, CheckoutStatus.RETURNED);

        // Act
        List<BookRecommendation> recommendations =
            recommendationService.getRecommendations(patron1);

        // Assert
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations.get(0).title()).isEqualTo("Design Patterns");
    }
}
```

### README Section

```markdown
## Book Recommendation System

Automated daily email service that sends personalized book recommendations to library patrons.

### Features

- **Collaborative Filtering**: Recommends books based on reading patterns of similar patrons
- **External API Integration**: Enriches recommendations with metadata from Google Books API
- **Scheduled Batch Processing**: Daily job runs at 8 AM
- **Email Templates**: Professional HTML emails with book covers and descriptions
- **Async Processing**: Non-blocking email delivery

### Architecture

```
Scheduled Job → Generate Recommendations → Enrich with Google Books API → Send Email
```

### Technologies

- Spring Batch (@Scheduled)
- Google Books API (REST integration)
- JavaMailSender (SMTP)
- Thymeleaf (HTML templating)
- Spring @Async (non-blocking)

### Configuration

Required environment variables:
- `GMAIL_USERNAME`: Gmail account for sending
- `GMAIL_APP_PASSWORD`: Gmail app password
- `GOOGLE_BOOKS_API_KEY`: API key from Google Cloud Console

### Running

```bash
# Enable recommendation job
RECOMMENDATION_ENABLED=true mvn spring-boot:run

# Disable for testing
RECOMMENDATION_ENABLED=false mvn spring-boot:run
```
```

---

## Action Items for Week 13

### Critical (Must Complete)

**1. Implement Core Recommendation Logic** (10-12 hours)
- Create Patron entity and repository
- Implement collaborative filtering algorithm
- Write unit tests for recommendation generation

**Deliverable:** Working recommendation algorithm

**2. Integrate Google Books API** (8-10 hours)
- Create GoogleBooksClient
- Implement search and ISBN lookup
- Add retry logic and error handling
- Test with real API calls

**Deliverable:** Functional API integration

**3. Build Email System** (6-8 hours)
- Configure JavaMailSender
- Create Thymeleaf email template
- Implement EmailService
- Test email sending

**Deliverable:** Emails sent successfully

**4. Create Scheduled Job** (4-6 hours)
- Implement RecommendationScheduler
- Configure cron expression
- Add enable/disable flag
- Test job execution

**Deliverable:** Daily job running on schedule

**5. Document in README** (2-3 hours)
- Add section describing recommendation system
- Include architecture diagram
- Document configuration
- Add to portfolio

**Deliverable:** Clear documentation

### Important (Should Complete)

**6. Add Unsubscribe Functionality** (2-3 hours)
- Create endpoint for unsubscribe
- Update patron preferences
- Add link to email template

**7. Metrics and Monitoring** (2 hours)
- Log recommendation job metrics
- Track email send success/failure rate
- Monitor API rate limits

### Bonus (If Time Permits)

**8. Preference Management** (3-4 hours)
- Let patrons specify favorite genres
- Filter recommendations by preferences

**9. A/B Testing** (4-5 hours)
- Test different recommendation algorithms
- Measure click-through rate

---

## Success Metrics for Week 13

By the end of this week, you should have:

✅ **Complete Secondary Project:**
- Recommendation algorithm with collaborative filtering
- Google Books API integration
- Email system with HTML templates
- Scheduled daily job

✅ **New Skills Demonstrated:**
- External API integration
- Batch/scheduled processing
- Email templating and delivery
- Async processing

✅ **Portfolio Addition:**
- README section with architecture
- Working demo (can trigger manually)
- Interview talking points prepared

---

## Interview Talking Points

### Narrative for Recommendation System

> "Beyond the analytics dashboard, I built an automated recommendation system that runs daily. It uses collaborative filtering—'readers like you also enjoyed these books'—similar to how predictive maintenance identifies equipment that often fails together.
>
> The system pulls metadata from Google Books API—rate limiting and retry logic are critical since we're making 50+ calls per job. I used Spring @Async for email delivery so the batch job doesn't block waiting for SMTP responses.
>
> It demonstrates a completely different skill set than the dashboard: batch processing, external API integration, email templating. Shows I can work across different architectural patterns."

### Technical Deep-Dive

**Q: How does the recommendation algorithm work?**

**A:**
> "It's a simple collaborative filtering approach. For a target patron, I find all books they've checked out. Then I find other patrons who read those same books—those are 'similar patrons.' Finally, I look at what those similar patrons read that the target patron hasn't, and rank by how many similar patrons recommended it.
>
> For example, if Patron A read books 1 and 2, and Patrons B, C, D also read books 1 and 2, but they all also read book 3, then book 3 gets a high score for Patron A.
>
> It's not as sophisticated as matrix factorization, but it's simple to explain and effective for small datasets. For production at scale, I'd consider Apache Mahout or a dedicated recommendation engine."

**Q: How do you handle API rate limits?**

**A:**
> "Google Books API has a quota of 1,000 requests/day for free tier. With 50 patrons and 5 recommendations each, that's 250 requests per day—well under limit.
>
> But I added defensive code: exponential backoff retry logic (3 attempts with 2s, 4s, 8s delays), and fallback to database-only recommendations if API fails. Logs track API call counts.
>
> If we exceeded quota, I'd implement caching: store Google Books data in our database, refresh monthly. Trade-off: slightly stale data for resilience."

---

## What's Next

**Section 14: Git, CI/CD & DevOps Foundations**

Now that you have two complete projects (dashboard + recommendation system), Section 14 covers professional development workflows:
- Git best practices (branching, commit messages, PR workflow)
- GitHub Actions for CI/CD
- Automated testing on every commit
- Deployment automation

---

**Progress Tracker:** 13/32 sections complete (40.6% - Over 40%!)

**Next Section:** Git, CI/CD & DevOps Foundations — Professional development workflows
