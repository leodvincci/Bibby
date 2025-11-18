# Section 09: Portfolio Project Architecture (Phase 2)
**Advanced Workflows, Database Migrations & Production Patterns**

**Week 9-10: From Basic CRUD to Enterprise-Grade System**

---

## Overview

Section 08 gave you a solid REST API foundation. Now we'll tackle the patterns that separate junior-level code from production-ready systems: complex business workflows, database migration management, performance optimization with caching, asynchronous processing, and observability.

These aren't theoretical exercises. Every pattern in this section is **standard practice in industrial software**:
- Checkout/return workflows mirror operational state machines (valve sequences, pump startup/shutdown procedures)
- Database migrations are version control for schemas (like pipeline configuration change management)
- Caching reduces database load (like buffering sensor data before persistence)
- Async processing decouples systems (like event-driven SCADA architectures)
- Observability enables production debugging (like telemetry dashboards in control rooms)

By the end of this section, Bibby will demonstrate enterprise patterns that directly translate to your target roles at companies like OSIsoft, Uptake, and Rockwell Automation.

**This Week's Focus:**
- Build checkout/return workflow with complex business rules
- Implement database migrations with Flyway
- Add caching layer with Spring Cache and Redis
- Process events asynchronously with Spring Events
- Add structured logging and observability

---

## Part 1: Checkout/Return Workflow Design

### Current State Analysis

Let's examine the existing checkout logic in `BookService.java:56-62`:

```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**Problems with Current Implementation:**

1. **No audit trail:** Who checked out the book? When is it due back?
2. **No business rule validation:** What if book is LOST or ARCHIVED?
3. **No checkout history:** Can't track circulation patterns or popular books
4. **Primitive status field:** Uses String instead of enum (type-unsafe)
5. **No patron tracking:** Who has the book?
6. **Silent failure:** Returns void even if checkout fails
7. **No events:** Other systems can't react to checkout (e.g., send email notification)

### Industrial Analogy: Checkout = Equipment Transfer

In operational environments, transferring equipment custody involves:

**Navy Tool Checkout:**
- **Sign-out sheet:** Name, date, tool ID, expected return
- **Condition check:** Tool must be in serviceable state
- **Transfer record:** Custody chain documented
- **Accountability:** Person responsible until return

**Pipeline Valve Operation:**
- **Pre-operation checks:** Verify safe to operate (not under maintenance, pressure within limits)
- **State transition:** Document valve position change
- **Event logging:** SCADA records who, when, why
- **Post-operation validation:** Confirm expected system state

**Library Checkout Should Mirror This:**
- **Patron record:** Who's checking out
- **Availability check:** Book must be AVAILABLE
- **Checkout record:** Transaction documented with due date
- **Event notification:** System publishes event for downstream processing (email, analytics)

### Designing the Checkout Entity

We need a new entity to represent checkout transactions.

**File:** `src/main/java/com/penrose/bibby/library/checkout/CheckoutEntity.java`

```java
package com.penrose.bibby.library.checkout;

import com.penrose.bibby.library.book.BookEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a book checkout transaction.
 *
 * Industrial analogy: Equipment transfer record.
 * Captures who, what, when for accountability and audit trail.
 */
@Entity
@Table(name = "checkouts")
public class CheckoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Book being checked out.
     * Many checkouts can reference same book over time.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;

    /**
     * Patron who checked out the book.
     * For now, simple string. Later: reference to Patron entity.
     */
    @Column(nullable = false)
    private String patronName;

    @Column(nullable = false)
    private String patronEmail;

    /**
     * When checkout occurred.
     */
    @Column(name = "checked_out_at", nullable = false)
    private LocalDateTime checkedOutAt;

    /**
     * When book is due back.
     * Default: 14 days from checkout.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    /**
     * When book was actually returned (null if still checked out).
     */
    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    /**
     * Current status of this checkout.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckoutStatus status;

    /**
     * Optional notes (e.g., condition at checkout, renewal reason).
     */
    @Column(length = 1000)
    private String notes;

    // Constructors
    public CheckoutEntity() {
        this.status = CheckoutStatus.ACTIVE;
    }

    public CheckoutEntity(BookEntity book, String patronName, String patronEmail, int loanPeriodDays) {
        this.book = book;
        this.patronName = patronName;
        this.patronEmail = patronEmail;
        this.checkedOutAt = LocalDateTime.now();
        this.dueDate = LocalDateTime.now().plusDays(loanPeriodDays);
        this.status = CheckoutStatus.ACTIVE;
    }

    // Business logic methods

    /**
     * Mark checkout as returned.
     *
     * @return true if book was overdue
     */
    public boolean markReturned() {
        this.returnedAt = LocalDateTime.now();
        this.status = CheckoutStatus.RETURNED;
        return isOverdue();
    }

    /**
     * Check if checkout is currently overdue.
     */
    public boolean isOverdue() {
        if (status == CheckoutStatus.RETURNED) {
            // Check if it was returned late
            return returnedAt != null && returnedAt.isAfter(dueDate);
        }
        // Still active - check if past due date
        return LocalDateTime.now().isAfter(dueDate);
    }

    /**
     * Renew checkout (extend due date).
     * Business rule: Can't renew if already overdue.
     *
     * @param extensionDays days to extend
     * @return true if renewal succeeded
     */
    public boolean renew(int extensionDays) {
        if (isOverdue()) {
            return false;
        }
        if (status != CheckoutStatus.ACTIVE) {
            return false;
        }
        this.dueDate = this.dueDate.plusDays(extensionDays);
        this.status = CheckoutStatus.RENEWED;
        return true;
    }

    /**
     * Calculate days until due (negative if overdue).
     */
    public long getDaysUntilDue() {
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(now, dueDate).toDays();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BookEntity getBook() { return book; }
    public void setBook(BookEntity book) { this.book = book; }

    public String getPatronName() { return patronName; }
    public void setPatronName(String patronName) { this.patronName = patronName; }

    public String getPatronEmail() { return patronEmail; }
    public void setPatronEmail(String patronEmail) { this.patronEmail = patronEmail; }

    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public CheckoutStatus getStatus() { return status; }
    public void setStatus(CheckoutStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
```

**File:** `src/main/java/com/penrose/bibby/library/checkout/CheckoutStatus.java`

```java
package com.penrose.bibby.library.checkout;

/**
 * Status of a checkout transaction.
 */
public enum CheckoutStatus {
    ACTIVE,      // Currently checked out
    RENEWED,     // Extended due date
    RETURNED,    // Returned on time
    OVERDUE      // Not returned by due date (status updated by batch job)
}
```

### Repository Layer

**File:** `src/main/java/com/penrose/bibby/library/checkout/CheckoutRepository.java`

```java
package com.penrose.bibby.library.checkout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckoutRepository extends JpaRepository<CheckoutEntity, Long> {

    /**
     * Find active checkout for a specific book.
     * A book can only have one active checkout at a time.
     */
    Optional<CheckoutEntity> findByBookBookIdAndStatus(Long bookId, CheckoutStatus status);

    /**
     * Find all active checkouts for a patron.
     */
    List<CheckoutEntity> findByPatronEmailAndStatus(String patronEmail, CheckoutStatus status);

    /**
     * Find all overdue checkouts.
     * Used by batch job to send reminders.
     */
    @Query("SELECT c FROM CheckoutEntity c WHERE c.status = 'ACTIVE' AND c.dueDate < :now")
    List<CheckoutEntity> findOverdueCheckouts(LocalDateTime now);

    /**
     * Count active checkouts for a patron.
     * Business rule: Max 5 books checked out at once.
     */
    long countByPatronEmailAndStatus(String patronEmail, CheckoutStatus status);

    /**
     * Find checkout history for a book.
     * Useful for circulation analytics.
     */
    List<CheckoutEntity> findByBookBookIdOrderByCheckedOutAtDesc(Long bookId);
}
```

### Service Layer with Business Rules

**File:** `src/main/java/com/penrose/bibby/library/checkout/CheckoutService.java`

```java
package com.penrose.bibby.library.checkout;

import com.penrose.bibby.exception.BusinessRuleViolationException;
import com.penrose.bibby.exception.ResourceNotFoundException;
import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.book.BookRepository;
import com.penrose.bibby.library.book.BookStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing book checkouts and returns.
 *
 * Implements business rules:
 * - Only AVAILABLE books can be checked out
 * - Patron can have max 5 active checkouts
 * - Book status updated atomically with checkout creation
 * - Events published for downstream processing
 */
@Service
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);
    private static final int DEFAULT_LOAN_PERIOD_DAYS = 14;
    private static final int MAX_CHECKOUTS_PER_PATRON = 5;

    private final CheckoutRepository checkoutRepository;
    private final BookRepository bookRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CheckoutService(CheckoutRepository checkoutRepository,
                          BookRepository bookRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.checkoutRepository = checkoutRepository;
        this.bookRepository = bookRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Check out a book.
     *
     * Business rules enforced:
     * 1. Book must exist
     * 2. Book must be AVAILABLE
     * 3. Patron can't exceed max checkout limit
     * 4. Book status transitions to CHECKED_OUT
     *
     * Industrial analogy: Tool checkout with pre-flight checks.
     *
     * @param bookId ID of book to check out
     * @param patronName patron's full name
     * @param patronEmail patron's email
     * @return created checkout record
     * @throws ResourceNotFoundException if book doesn't exist
     * @throws BusinessRuleViolationException if business rules violated
     */
    @Transactional
    public CheckoutEntity checkoutBook(Long bookId, String patronName, String patronEmail) {
        logger.info("Attempting checkout: bookId={}, patron={}", bookId, patronEmail);

        // Rule 1: Book must exist
        BookEntity book = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        // Rule 2: Book must be AVAILABLE
        if (book.getStatus() != BookStatus.AVAILABLE) {
            throw new BusinessRuleViolationException(
                String.format("Book %d is not available for checkout. Current status: %s",
                    bookId, book.getStatus()),
                "BOOK_NOT_AVAILABLE"
            );
        }

        // Rule 3: Patron checkout limit
        long patronActiveCheckouts = checkoutRepository.countByPatronEmailAndStatus(
            patronEmail, CheckoutStatus.ACTIVE);
        if (patronActiveCheckouts >= MAX_CHECKOUTS_PER_PATRON) {
            throw new BusinessRuleViolationException(
                String.format("Patron %s has reached maximum checkout limit (%d)",
                    patronEmail, MAX_CHECKOUTS_PER_PATRON),
                "MAX_CHECKOUTS_EXCEEDED"
            );
        }

        // Create checkout record
        CheckoutEntity checkout = new CheckoutEntity(
            book, patronName, patronEmail, DEFAULT_LOAN_PERIOD_DAYS);
        checkout = checkoutRepository.save(checkout);

        // Update book status atomically (same transaction)
        book.setStatus(BookStatus.CHECKED_OUT);
        book.setCheckoutCount(book.getCheckoutCount() + 1);
        bookRepository.save(book);

        logger.info("Checkout successful: checkoutId={}, bookId={}, dueDate={}",
            checkout.getId(), bookId, checkout.getDueDate());

        // Publish event for async processing (email notification, analytics)
        eventPublisher.publishEvent(new BookCheckedOutEvent(this, checkout));

        return checkout;
    }

    /**
     * Return a book.
     *
     * Business rules:
     * 1. Book must have an active checkout
     * 2. Book status transitions to AVAILABLE
     * 3. If overdue, publish overdue event
     *
     * @param bookId ID of book being returned
     * @return updated checkout record
     */
    @Transactional
    public CheckoutEntity returnBook(Long bookId) {
        logger.info("Attempting return: bookId={}", bookId);

        // Find active checkout for this book
        CheckoutEntity checkout = checkoutRepository
            .findByBookBookIdAndStatus(bookId, CheckoutStatus.ACTIVE)
            .orElseThrow(() -> new BusinessRuleViolationException(
                String.format("No active checkout found for book %d", bookId),
                "NO_ACTIVE_CHECKOUT"
            ));

        // Mark as returned (calculates if overdue)
        boolean wasOverdue = checkout.markReturned();
        checkout = checkoutRepository.save(checkout);

        // Update book status
        BookEntity book = checkout.getBook();
        book.setStatus(BookStatus.AVAILABLE);
        bookRepository.save(book);

        logger.info("Return successful: checkoutId={}, wasOverdue={}",
            checkout.getId(), wasOverdue);

        // Publish events
        eventPublisher.publishEvent(new BookReturnedEvent(this, checkout, wasOverdue));

        return checkout;
    }

    /**
     * Renew a checkout (extend due date).
     *
     * @param checkoutId checkout to renew
     * @param extensionDays days to extend
     * @return updated checkout
     */
    @Transactional
    public CheckoutEntity renewCheckout(Long checkoutId, int extensionDays) {
        CheckoutEntity checkout = checkoutRepository.findById(checkoutId)
            .orElseThrow(() -> new ResourceNotFoundException("Checkout", checkoutId));

        if (!checkout.renew(extensionDays)) {
            throw new BusinessRuleViolationException(
                "Cannot renew overdue or completed checkouts",
                "RENEWAL_NOT_ALLOWED"
            );
        }

        checkout = checkoutRepository.save(checkout);
        logger.info("Checkout renewed: checkoutId={}, newDueDate={}",
            checkoutId, checkout.getDueDate());

        return checkout;
    }

    /**
     * Find all active checkouts for a patron.
     */
    public List<CheckoutEntity> findActiveCheckoutsForPatron(String patronEmail) {
        return checkoutRepository.findByPatronEmailAndStatus(patronEmail, CheckoutStatus.ACTIVE);
    }

    /**
     * Find checkout history for a book.
     */
    public List<CheckoutEntity> findCheckoutHistory(Long bookId) {
        return checkoutRepository.findByBookBookIdOrderByCheckedOutAtDesc(bookId);
    }

    /**
     * Find all overdue checkouts.
     * Called by scheduled job to send reminders.
     */
    public List<CheckoutEntity> findOverdueCheckouts() {
        return checkoutRepository.findOverdueCheckouts(LocalDateTime.now());
    }
}
```

### Event-Driven Architecture with Spring Events

**Why events?** Decouple checkout logic from notification, analytics, logging. The checkout service shouldn't care *how* notifications are sent—just that checkout happened.

**Industrial analogy:** SCADA systems publish events (valve opened, pressure exceeded threshold). Subscribers react independently: log to database, send alert, update dashboard. Central controller doesn't know about all subscribers.

**File:** `src/main/java/com/penrose/bibby/library/checkout/events/BookCheckedOutEvent.java`

```java
package com.penrose.bibby.library.checkout.events;

import com.penrose.bibby.library.checkout.CheckoutEntity;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a book is checked out.
 * Subscribers can send emails, update analytics, log audit trail.
 */
public class BookCheckedOutEvent extends ApplicationEvent {

    private final CheckoutEntity checkout;

    public BookCheckedOutEvent(Object source, CheckoutEntity checkout) {
        super(source);
        this.checkout = checkout;
    }

    public CheckoutEntity getCheckout() {
        return checkout;
    }
}
```

**File:** `src/main/java/com/penrose/bibby/library/checkout/events/BookReturnedEvent.java`

```java
package com.penrose.bibby.library.checkout.events;

import com.penrose.bibby.library.checkout.CheckoutEntity;
import org.springframework.context.ApplicationEvent;

public class BookReturnedEvent extends ApplicationEvent {

    private final CheckoutEntity checkout;
    private final boolean wasOverdue;

    public BookReturnedEvent(Object source, CheckoutEntity checkout, boolean wasOverdue) {
        super(source);
        this.checkout = checkout;
        this.wasOverdue = wasOverdue;
    }

    public CheckoutEntity getCheckout() {
        return checkout;
    }

    public boolean wasOverdue() {
        return wasOverdue;
    }
}
```

**File:** `src/main/java/com/penrose/bibby/library/checkout/events/CheckoutEventListener.java`

```java
package com.penrose.bibby.library.checkout.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for checkout events and handles async processing.
 *
 * @Async ensures event handling doesn't block checkout transaction.
 */
@Component
public class CheckoutEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutEventListener.class);

    /**
     * Handle book checkout event.
     * In production: send email, update analytics dashboard, notify inventory system.
     */
    @Async
    @EventListener
    public void handleBookCheckedOut(BookCheckedOutEvent event) {
        logger.info("Processing checkout event: bookId={}, patron={}, dueDate={}",
            event.getCheckout().getBook().getId(),
            event.getCheckout().getPatronEmail(),
            event.getCheckout().getDueDate());

        // TODO: Send checkout confirmation email
        // TODO: Update circulation analytics
        // TODO: Check if book was reserved, notify next patron
    }

    /**
     * Handle book return event.
     */
    @Async
    @EventListener
    public void handleBookReturned(BookReturnedEvent event) {
        logger.info("Processing return event: bookId={}, wasOverdue={}",
            event.getCheckout().getBook().getId(),
            event.wasOverdue());

        if (event.wasOverdue()) {
            logger.warn("Book returned late: checkoutId={}, patron={}",
                event.getCheckout().getId(),
                event.getCheckout().getPatronEmail());
            // TODO: Calculate late fee
            // TODO: Send overdue notice
        }

        // TODO: Check if book has reservation queue, notify next patron
    }
}
```

**Enable async processing in configuration:**

**File:** `src/main/java/com/penrose/bibby/config/AsyncConfig.java`

```java
package com.penrose.bibby.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring will process @Async methods in background thread pool
}
```

---

## Part 2: Database Migrations with Flyway

**Problem:** As Bibby evolves, database schema changes. How do you version control schema? How do you ensure dev/staging/prod databases stay in sync?

**Solution:** Flyway—database migration tool that applies SQL scripts in order, tracking which have been applied.

### Why Database Migrations Matter

**Industrial analogy:** Configuration change management for control systems.

When you upgrade SCADA software or change PLC ladder logic:
1. **Version control:** Every change tracked with version number
2. **Rollback plan:** If upgrade fails, revert to previous version
3. **Test in staging:** Never apply directly to production
4. **Audit trail:** Who made change, when, why

Flyway provides the same for databases:
- **Version history:** Schema evolution tracked in code
- **Repeatable:** Spin up new dev environment with all migrations applied
- **Rollback support:** Revert schema changes if needed
- **Team synchronization:** Everyone's database has same schema version

### Add Flyway Dependency

**File:** `pom.xml`

Add inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### Configure Flyway

**File:** `src/main/resources/application.properties`

```properties
# Flyway configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# IMPORTANT: Change Hibernate DDL to validate
# Let Flyway manage schema, not Hibernate
spring.jpa.hibernate.ddl-auto=validate
```

### Migration File Structure

Flyway looks for SQL files in `src/main/resources/db/migration/` with naming pattern:

```
V{version}__{description}.sql
```

Examples:
- `V1__initial_schema.sql`
- `V2__add_checkouts_table.sql`
- `V3__add_checkout_count_to_books.sql`

**Version must be unique and sequential.** Flyway applies migrations in order.

### Create Initial Baseline Migration

**File:** `src/main/resources/db/migration/V1__initial_schema.sql`

```sql
-- Initial schema baseline for Bibby
-- Represents current state before Flyway adoption

CREATE TABLE IF NOT EXISTS books (
    book_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    isbn VARCHAR(20),
    publisher VARCHAR(255),
    publication_year INTEGER,
    genre VARCHAR(100),
    edition INTEGER,
    description TEXT,
    shelf_id BIGINT,
    checkout_count INTEGER DEFAULT 0,
    book_status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at DATE DEFAULT CURRENT_DATE,
    updated_at DATE DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS authors (
    author_id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    biography TEXT,
    created_at DATE DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS book_authors (
    book_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES authors(author_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS shelves (
    shelf_id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(50) NOT NULL UNIQUE,
    capacity INTEGER NOT NULL,
    bookcase_id BIGINT,
    created_at DATE DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS bookcases (
    bookcase_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    location VARCHAR(255),
    total_shelves INTEGER DEFAULT 0,
    created_at DATE DEFAULT CURRENT_DATE
);

-- Indexes for common queries
CREATE INDEX idx_books_status ON books(book_status);
CREATE INDEX idx_books_shelf ON books(shelf_id);
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_authors_name ON authors(last_name, first_name);
```

### Add Checkouts Table Migration

**File:** `src/main/resources/db/migration/V2__add_checkouts_table.sql`

```sql
-- Add checkouts table for tracking book loans

CREATE TABLE checkouts (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    patron_name VARCHAR(255) NOT NULL,
    patron_email VARCHAR(255) NOT NULL,
    checked_out_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,

    CONSTRAINT fk_checkout_book FOREIGN KEY (book_id)
        REFERENCES books(book_id) ON DELETE RESTRICT,

    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'RENEWED', 'RETURNED', 'OVERDUE'))
);

-- Indexes for common queries
CREATE INDEX idx_checkouts_book ON checkouts(book_id);
CREATE INDEX idx_checkouts_patron ON checkouts(patron_email);
CREATE INDEX idx_checkouts_status ON checkouts(status);
CREATE INDEX idx_checkouts_due_date ON checkouts(due_date) WHERE status = 'ACTIVE';

-- Prevent multiple active checkouts for same book
CREATE UNIQUE INDEX idx_checkouts_active_book
    ON checkouts(book_id)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE checkouts IS 'Tracks book checkout/return transactions';
COMMENT ON COLUMN checkouts.status IS 'ACTIVE: currently checked out, RENEWED: extended, RETURNED: returned, OVERDUE: past due';
```

### Improve Audit Trail Migration

**File:** `src/main/resources/db/migration/V3__improve_audit_timestamps.sql`

```sql
-- Upgrade audit timestamps from DATE to TIMESTAMP
-- Add updated_at triggers for automatic updates

-- Books table
ALTER TABLE books
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- Authors table
ALTER TABLE authors
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE authors ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Shelves table
ALTER TABLE shelves
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE shelves ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Bookcases table
ALTER TABLE bookcases
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE bookcases ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Create trigger function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers to all tables
CREATE TRIGGER update_books_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_authors_updated_at
    BEFORE UPDATE ON authors
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shelves_updated_at
    BEFORE UPDATE ON shelves
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bookcases_updated_at
    BEFORE UPDATE ON bookcases
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### Running Migrations

```bash
# Flyway runs automatically on application startup
mvn spring-boot:run

# Check migration status
mvn flyway:info

# Repair failed migration (use carefully!)
mvn flyway:repair

# Clean database (DESTROYS ALL DATA - dev only!)
mvn flyway:clean
```

**Flyway creates `flyway_schema_history` table tracking applied migrations:**

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

Output:
```
installed_rank | version | description              | script                          | checksum    | installed_on
1              | 1       | initial schema           | V1__initial_schema.sql          | -1234567890 | 2025-01-15 10:00:00
2              | 2       | add checkouts table      | V2__add_checkouts_table.sql     | 987654321   | 2025-01-15 10:00:01
3              | 3       | improve audit timestamps | V3__improve_audit_timestamps.sql| 123456789   | 2025-01-15 10:00:02
```

---

## Part 3: Caching with Spring Cache and Redis

**Problem:** Every API call hits database. Popular queries (list all books, get book details) execute repeatedly. Database becomes bottleneck.

**Solution:** Cache frequently-accessed data in memory. Serve from cache instead of database.

### Industrial Analogy: Buffering Sensor Data

In SCADA systems:
- **Raw data:** Sensors produce readings every 100ms
- **Storage:** Can't write every reading to database (I/O bottleneck)
- **Buffer:** Cache recent readings in memory
- **Persistence:** Write aggregated data (every 5 min) to database
- **Queries:** Real-time dashboards read from cache, reports read from database

In web applications:
- **Hot data:** Popular books, active checkouts (read frequently)
- **Cache:** Store in Redis (in-memory database)
- **Eviction:** Data expires after TTL or when changed
- **Queries:** Read from Redis (microseconds) vs PostgreSQL (milliseconds)

### Add Redis Dependencies

**File:** `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### Configure Redis

**File:** `src/main/resources/application.properties`

```properties
# Redis configuration
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}

# Cache configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
# 10 minutes = 600,000 ms
spring.cache.redis.cache-null-values=false
```

### Enable Caching

**File:** `src/main/java/com/penrose/bibby/config/CacheConfig.java`

```java
package com.penrose.bibby.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))  // Default TTL
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
    }
}
```

### Apply Caching to Service Methods

**File:** `src/main/java/com/penrose/bibby/library/book/BookService.java` (enhanced)

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;

@Service
public class BookService {

    // ... existing code

    /**
     * Get book by ID with caching.
     *
     * @Cacheable: Result cached in "books" cache with key = bookId
     * Subsequent calls with same ID return cached value (no DB query)
     */
    @Cacheable(value = "books", key = "#bookId")
    public BookEntity findById(Long bookId) {
        logger.info("Cache miss - querying database for book: {}", bookId);
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
    }

    /**
     * Update book and evict from cache.
     *
     * @CacheEvict: Removes this book from cache after update
     * Forces next read to fetch fresh data from DB
     */
    @CacheEvict(value = "books", key = "#bookId")
    public BookEntity updateBook(Long bookId, BookUpdateRequestDTO dto) {
        logger.info("Updating book {} - will evict from cache", bookId);
        BookEntity book = findById(bookId);

        if (dto.title() != null) book.setTitle(dto.title());
        if (dto.isbn() != null) book.setIsbn(dto.isbn());
        if (dto.status() != null) book.setStatus(dto.status());

        return bookRepository.save(book);
    }

    /**
     * Search books by title.
     * Cache by query string.
     */
    @Cacheable(value = "bookSearchResults", key = "#query")
    public Page<BookEntity> searchByTitle(String query, Pageable pageable) {
        logger.info("Cache miss - searching books: query={}", query);
        return bookRepository.findByTitleContainingIgnoreCase(query, pageable);
    }

    /**
     * Clear all book caches.
     * Use when bulk operations performed.
     */
    @CacheEvict(value = {"books", "bookSearchResults"}, allEntries = true)
    public void clearBookCaches() {
        logger.info("Cleared all book caches");
    }
}
```

### Cache Annotations Reference

**@Cacheable:**
- Caches method return value
- Key generated from parameters
- On subsequent calls with same params, returns cached value (skips method execution)

**@CacheEvict:**
- Removes entry from cache
- Use when data changes (update, delete)
- `allEntries=true`: Clear entire cache

**@CachePut:**
- Always executes method AND updates cache
- Use for update operations where you want to refresh cache

**@Caching:**
- Combine multiple cache operations

### Monitor Cache Performance

**File:** `src/main/java/com/penrose/bibby/library/book/BookCacheStats.java`

```java
package com.penrose.bibby.library.book;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class BookCacheStats {

    private final CacheManager cacheManager;

    public BookCacheStats(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void printCacheStats() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                System.out.println("Cache: " + cacheName);
                // Redis-specific stats require RedisCache cast
            }
        });
    }
}
```

### Testing Cache Behavior

```bash
# Start Redis locally (Docker)
docker run -d -p 6379:6379 redis:7-alpine

# Make request - cache miss (slow)
curl http://localhost:8080/api/v1/books/1
# Check logs: "Cache miss - querying database for book: 1"

# Make same request - cache hit (fast)
curl http://localhost:8080/api/v1/books/1
# No database query in logs!

# Update book - evicts cache
curl -X PATCH http://localhost:8080/api/v1/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "Updated Title"}'

# Next GET is cache miss again (fetches updated data)
curl http://localhost:8080/api/v1/books/1
```

---

## Part 4: Observability — Structured Logging

**Problem:** Production debugging with `System.out.println` is impossible. Need searchable, structured logs.

**Solution:** SLF4J with Logback (industry standard for Java logging).

### Logging Levels

```
TRACE < DEBUG < INFO < WARN < ERROR
```

**TRACE:** Very detailed (method entry/exit)
**DEBUG:** Detailed info for debugging (variable values, flow)
**INFO:** General operational events (server started, checkout completed)
**WARN:** Potential issues (deprecated API called, slow query)
**ERROR:** Failures requiring attention (exception thrown, service down)

**Production:** Usually INFO level (reduce log volume)
**Development:** DEBUG or TRACE (see everything)

### Configure Logback

**File:** `src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console appender for development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File appender for production -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/bibby.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- Daily rollover -->
            <fileNamePattern>logs/bibby-%d{yyyy-MM-dd}.log</fileNamePattern>
            <!-- Keep 30 days of history -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- JSON appender for log aggregation (ELK stack, CloudWatch) -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/bibby-json.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/bibby-json-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
        </encoder>
    </appender>

    <!-- Package-level logging -->
    <logger name="com.penrose.bibby" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### Structured Logging in Code

**Best practices:**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CheckoutService {

    // Create logger (one per class)
    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    public CheckoutEntity checkoutBook(Long bookId, String patronEmail) {
        // ✅ Good: Structured with parameters
        logger.info("Processing checkout: bookId={}, patron={}", bookId, patronEmail);

        // ❌ Bad: String concatenation (expensive, not searchable)
        logger.info("Processing checkout: bookId=" + bookId + ", patron=" + patronEmail);

        try {
            // ... business logic

            logger.debug("Validation passed for bookId={}", bookId);

            CheckoutEntity checkout = checkoutRepository.save(new CheckoutEntity(...));

            logger.info("Checkout successful: checkoutId={}, dueDate={}",
                checkout.getId(), checkout.getDueDate());

            return checkout;

        } catch (BusinessRuleViolationException e) {
            logger.warn("Checkout failed due to business rule: bookId={}, rule={}",
                bookId, e.getRuleViolated());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during checkout: bookId={}", bookId, e);
            // Include exception as last parameter for stack trace
            throw e;
        }
    }
}
```

### MDC (Mapped Diagnostic Context) for Request Tracing

**Problem:** In production with many concurrent requests, how do you trace one request through logs?

**Solution:** MDC—thread-local context for adding request ID to all log statements.

**File:** `src/main/java/com/penrose/bibby/config/RequestLoggingFilter.java`

```java
package com.penrose.bibby.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds request ID to MDC for correlation across logs.
 */
@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Get request ID from header or generate new one
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        // Add to MDC (all subsequent logs include this)
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        try {
            logger.info("Incoming request: method={}, uri={}, requestId={}",
                httpRequest.getMethod(), httpRequest.getRequestURI(), requestId);

            long startTime = System.currentTimeMillis();
            chain.doFilter(request, response);
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Request completed: requestId={}, duration={}ms",
                requestId, duration);
        } finally {
            // Clean up MDC to prevent memory leak
            MDC.clear();
        }
    }
}
```

**Update logback pattern to include requestId:**

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n</pattern>
```

**Log output with request tracing:**

```
2025-01-15 10:30:00.123 [http-nio-8080-exec-1] [a1b2c3d4] INFO  c.p.b.config.RequestLoggingFilter - Incoming request: method=POST, uri=/api/v1/checkouts
2025-01-15 10:30:00.145 [http-nio-8080-exec-1] [a1b2c3d4] INFO  c.p.b.library.checkout.CheckoutService - Processing checkout: bookId=123
2025-01-15 10:30:00.167 [http-nio-8080-exec-1] [a1b2c3d4] DEBUG c.p.b.library.checkout.CheckoutService - Validation passed
2025-01-15 10:30:00.189 [http-nio-8080-exec-1] [a1b2c3d4] INFO  c.p.b.library.checkout.CheckoutService - Checkout successful: checkoutId=456
2025-01-15 10:30:00.200 [http-nio-8080-exec-1] [a1b2c3d4] INFO  c.p.b.config.RequestLoggingFilter - Request completed: duration=77ms
```

All log lines for this request share `[a1b2c3d4]` requestId—easy to grep!

---

## Action Items for Week 9-10

### Critical (Must Complete)

**1. Implement Checkout/Return Workflow** (10-12 hours)
- Create `CheckoutEntity`, `CheckoutStatus` enum, `CheckoutRepository`
- Implement `CheckoutService` with business rules (availability check, patron limit)
- Add REST endpoints: `POST /api/v1/checkouts`, `PUT /api/v1/checkouts/{id}/return`
- Write integration tests for checkout/return flows

**Deliverable:** Working checkout/return API with business rule enforcement

**2. Set Up Flyway Migrations** (4-5 hours)
- Add Flyway dependency
- Create `V1__initial_schema.sql` (baseline)
- Create `V2__add_checkouts_table.sql`
- Create `V3__improve_audit_timestamps.sql`
- Change `ddl-auto` to `validate`
- Verify migrations run on startup

**Deliverable:** Schema managed by Flyway, migrations tracked in `flyway_schema_history`

**3. Implement Event-Driven Processing** (3-4 hours)
- Create `BookCheckedOutEvent` and `BookReturnedEvent`
- Implement `CheckoutEventListener` with `@EventListener` and `@Async`
- Publish events from `CheckoutService`
- Enable async with `@EnableAsync`

**Deliverable:** Checkout events logged asynchronously

**4. Add Caching Layer** (4-5 hours)
- Add Redis dependencies
- Configure Redis connection
- Add `@Cacheable` to `BookService.findById()`
- Add `@CacheEvict` to update/delete methods
- Test cache behavior (hit/miss)

**Deliverable:** Book reads served from Redis cache

**5. Implement Structured Logging** (3-4 hours)
- Configure `logback-spring.xml` with console and file appenders
- Replace `System.out.println` with SLF4J logger
- Add MDC request tracing filter
- Test log output and request correlation

**Deliverable:** Structured logs with request IDs

### Important (Should Complete)

**6. Add Checkout History Endpoints** (2-3 hours)
- `GET /api/v1/books/{id}/checkouts` - History for a book
- `GET /api/v1/patrons/{email}/checkouts` - Active checkouts for patron

**7. Implement Overdue Detection** (3-4 hours)
- Scheduled job (Spring `@Scheduled`) to find overdue checkouts
- Update status to OVERDUE
- Publish `CheckoutOverdueEvent`

**8. Add Metrics with Micrometer** (2-3 hours)
- Add Spring Boot Actuator metrics
- Custom metrics: checkout count, overdue count
- Expose `/actuator/metrics` endpoint

### Bonus (If Time Permits)

**9. Implement Checkout Renewal** (2 hours)
- `PUT /api/v1/checkouts/{id}/renew` endpoint
- Business rules: Can't renew if overdue

**10. Add Redis Monitoring** (2 hours)
- RedisInsight for cache visualization
- Cache hit/miss rate metrics

**11. Distributed Tracing Preparation** (3 hours)
- Research Spring Cloud Sleuth / Micrometer Tracing
- Prepare for future microservices architecture

---

## Success Metrics for Week 9-10

By the end of this section, you should have:

✅ **Complete Checkout Workflow:**
- Checkout/return endpoints with business rule validation
- Event-driven architecture with async processing
- Integration tests covering happy path and edge cases

✅ **Database Migration Management:**
- Flyway configured with 3+ migration scripts
- Schema version controlled in code
- `ddl-auto=validate` (Hibernate doesn't manage schema)

✅ **Performance Optimization:**
- Redis caching on book reads
- Measurable cache hit rate
- Reduced database query load

✅ **Production-Ready Logging:**
- Structured logging with SLF4J
- Request correlation with MDC
- Log files with daily rotation

✅ **Async Event Processing:**
- Spring Events for checkout/return
- Background processing doesn't block API responses

---

## Industrial Connection: Why This Matters

### Workflow Design = Process Design

Checkout/return workflow mirrors industrial process flows:

**Pipeline Startup Sequence:**
1. **Pre-checks:** Verify pressures, temperatures within limits
2. **State transition:** Open isolation valves in sequence
3. **Monitoring:** Track flow rates, detect anomalies
4. **Post-operation:** Update system state, log event

**Book Checkout Sequence:**
1. **Pre-checks:** Verify book available, patron under limit
2. **State transition:** AVAILABLE → CHECKED_OUT atomically
3. **Monitoring:** Track due dates, detect overdue
4. **Post-operation:** Publish event, log transaction

Both require **transactional consistency** (all steps succeed or all fail) and **audit trail** (who did what when).

### Database Migrations = Configuration Management

In industrial systems, configuration changes follow strict protocols:
- **Version control:** Every PLC program version saved
- **Change tracking:** Document what changed and why
- **Rollback plan:** If new config causes problems, revert quickly
- **Testing:** Validate in offline simulator before deploying to live system

Flyway provides the same rigor for database schemas.

### Caching = Data Buffering

SCADA systems buffer data:
- **High-frequency sensors:** 100 readings/second
- **Memory buffer:** Cache recent readings
- **Database writes:** Aggregate to 1 reading/minute (reduce I/O)
- **Dashboards:** Display from buffer (real-time), reports from database (historical)

Web applications cache similarly:
- **Hot data:** Popular books, active checkouts
- **Redis cache:** In-memory (microsecond access)
- **Database:** Full dataset (millisecond access)
- **API responses:** Serve from cache when possible

---

## What's Next

**Section 10: Signature Project (Phase 1)**

You've built the technical foundation. Now we'll focus on **what makes Bibby memorable**:
- Choose signature feature (advanced search, recommendation engine, or reporting dashboard)
- Implement with production-grade patterns
- Document architecture decisions
- Prepare for portfolio showcase

You're approaching 30% completion (9/32 sections). The foundation is enterprise-grade. Next sections focus on differentiation.

---

## Resources

**Spring Events:**
- [Spring Events Guide](https://spring.io/blog/2015/02/11/better-application-events-in-spring-framework-4-2)
- [Async Event Processing](https://www.baeldung.com/spring-events)

**Flyway:**
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Flyway Spring Boot Integration](https://flywaydb.org/documentation/usage/plugins/springboot)

**Spring Cache:**
- [Spring Caching Guide](https://spring.io/guides/gs/caching/)
- [Redis with Spring Boot](https://www.baeldung.com/spring-data-redis-tutorial)

**Logging:**
- [SLF4J Manual](http://www.slf4j.org/manual.html)
- [Logback Configuration](https://logback.qos.ch/manual/configuration.html)
- [MDC for Request Tracing](https://www.baeldung.com/mdc-in-log4j-2-logback)

**Observability:**
- [Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
- [Micrometer Metrics](https://micrometer.io/docs)

---

**Progress Tracker:** 9/32 sections complete (28%)

**Next Section:** Signature Project (Phase 1) — Advanced features that differentiate Bibby
