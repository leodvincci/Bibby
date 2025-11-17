# Section 24: Real-World Scenarios & Design Patterns
## Clean Code + Spring Framework Mentorship

**Focus:** Applying design patterns to real scenarios in Bibby

**Estimated Time:** 2-3 hours to read; apply during refactoring

---

## Overview

This section demonstrates how to apply design patterns to solve **real problems** in your Bibby codebase, not just theoretical examples.

---

## Scenario 1: Multiple Book Formats

**Business Requirement:** Support physical books, e-books, and audiobooks.

### Problem

All book types have different properties:
- Physical: shelf location, condition
- E-book: file format, file size
- Audiobook: duration, narrator

### Bad Solution (Type Codes)

```java
@Entity
public class BookEntity {
    private String bookType;  // "PHYSICAL", "EBOOK", "AUDIOBOOK"
    private String shelfLocation;  // Only for physical
    private String fileFormat;  // Only for ebook
    private Integer duration;  // Only for audiobook

    public void checkOut() {
        if (bookType.equals("PHYSICAL")) {
            // Check shelf availability
        } else if (bookType.equals("EBOOK")) {
            // Generate download link
        } else if (bookType.equals("AUDIOBOOK")) {
            // Check license limits
        }
    }
}
```

**Problems:**
- Violates Single Responsibility Principle
- if/else chains everywhere
- Null fields for each type

### Good Solution (Strategy Pattern + Inheritance)

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "book_type")
public abstract class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToMany
    private Set<Author> authors;

    public abstract void checkOut(User user);
    public abstract boolean isAvailable();
}
```

```java
@Entity
@DiscriminatorValue("PHYSICAL")
public class PhysicalBook extends Book {

    private String shelfLocation;
    private BookCondition condition;

    @Override
    public void checkOut(User user) {
        if (!isAvailable()) {
            throw new BusinessException("Book is not available");
        }
        // Physical checkout logic
    }

    @Override
    public boolean isAvailable() {
        return getStatus() == BookStatus.AVAILABLE;
    }
}
```

```java
@Entity
@DiscriminatorValue("EBOOK")
public class EBook extends Book {

    private String fileFormat;
    private Long fileSizeBytes;
    private Integer downloadLimit;

    @Override
    public void checkOut(User user) {
        // Generate time-limited download link
    }

    @Override
    public boolean isAvailable() {
        return true;  // Always available
    }
}
```

---

## Scenario 2: Multiple Notification Channels

**Business Requirement:** Send notifications via email, SMS, and push notifications.

### Problem

How to send notifications without coupling to specific channels?

### Bad Solution (Hardcoded)

```java
@Service
public class NotificationService {

    public void sendCheckoutNotification(User user, Book book) {
        // Send email
        emailService.send(user.getEmail(), "Book checked out", "You checked out: " + book.getTitle());

        // Send SMS
        smsService.send(user.getPhone(), "Book checked out: " + book.getTitle());

        // Send push
        pushService.send(user.getDeviceToken(), "Book checked out", book.getTitle());
    }
}
```

**Problems:**
- Coupled to all notification types
- Can't add new channels easily
- Can't disable channels per user

### Good Solution (Observer Pattern)

```java
public interface NotificationChannel {
    void sendNotification(User user, NotificationMessage message);
    boolean supports(User user);
}
```

```java
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailService emailService;

    @Override
    public void sendNotification(User user, NotificationMessage message) {
        emailService.send(
            user.getEmail(),
            message.getSubject(),
            message.getBody()
        );
    }

    @Override
    public boolean supports(User user) {
        return user.getEmail() != null && user.isEmailNotificationsEnabled();
    }
}
```

```java
@Component
public class SmsNotificationChannel implements NotificationChannel {

    private final SmsService smsService;

    @Override
    public void sendNotification(User user, NotificationMessage message) {
        smsService.send(user.getPhone(), message.getBody());
    }

    @Override
    public boolean supports(User user) {
        return user.getPhone() != null && user.isSmsNotificationsEnabled();
    }
}
```

```java
@Service
public class NotificationService {

    private final List<NotificationChannel> channels;

    public NotificationService(List<NotificationChannel> channels) {
        this.channels = channels;  // Spring auto-injects all beans
    }

    public void sendNotification(User user, NotificationMessage message) {
        channels.stream()
            .filter(channel -> channel.supports(user))
            .forEach(channel -> channel.sendNotification(user, message));
    }
}
```

**Benefits:**
- ✅ Add new channels without modifying existing code
- ✅ User preferences control which channels are used
- ✅ Easy to test each channel independently

---

## Scenario 3: Complex Book Creation

**Business Requirement:** Create books with various optional configurations.

### Problem

Book creation has many optional fields:
- Cover image
- Publisher details
- Series information
- Tags
- Related books

### Bad Solution (Telescoping Constructors)

```java
public class Book {
    public Book(String title, Author author) { }
    public Book(String title, Author author, String isbn) { }
    public Book(String title, Author author, String isbn, String publisher) { }
    public Book(String title, Author author, String isbn, String publisher, String coverImage) { }
    // 10 more constructors...
}
```

### Good Solution (Builder Pattern)

```java
public class Book {

    private final String title;
    private final Set<Author> authors;
    private final String isbn;
    private final String publisher;
    private final String coverImageUrl;
    private final Series series;
    private final Set<String> tags;

    private Book(Builder builder) {
        this.title = builder.title;
        this.authors = builder.authors;
        this.isbn = builder.isbn;
        this.publisher = builder.publisher;
        this.coverImageUrl = builder.coverImageUrl;
        this.series = builder.series;
        this.tags = builder.tags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private Set<Author> authors = new HashSet<>();
        private String isbn;
        private String publisher;
        private String coverImageUrl;
        private Series series;
        private Set<String> tags = new HashSet<>();

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(Author author) {
            this.authors.add(author);
            return this;
        }

        public Builder isbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public Builder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder coverImage(String url) {
            this.coverImageUrl = url;
            return this;
        }

        public Builder series(Series series) {
            this.series = series;
            return this;
        }

        public Builder tag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Book build() {
            if (title == null || title.isBlank()) {
                throw new IllegalStateException("Title is required");
            }
            if (authors.isEmpty()) {
                throw new IllegalStateException("At least one author is required");
            }
            return new Book(this);
        }
    }
}
```

**Usage:**
```java
Book book = Book.builder()
    .title("Clean Code")
    .author(robertMartin)
    .isbn("978-0132350884")
    .publisher("Prentice Hall")
    .coverImage("https://example.com/cover.jpg")
    .tag("programming")
    .tag("craftsmanship")
    .build();
```

---

## Scenario 4: Calculating Late Fees

**Business Requirement:** Different late fee calculations based on membership type.

### Problem

Premium members get waived fees, regular members pay $0.50/day, guests pay $1/day.

### Bad Solution (Conditional Logic)

```java
public BigDecimal calculateLateFee(User user, int daysOverdue) {
    if (user.getMembershipType().equals("PREMIUM")) {
        return BigDecimal.ZERO;
    } else if (user.getMembershipType().equals("REGULAR")) {
        return BigDecimal.valueOf(daysOverdue * 0.50);
    } else if (user.getMembershipType().equals("GUEST")) {
        return BigDecimal.valueOf(daysOverdue * 1.00);
    }
    return BigDecimal.ZERO;
}
```

### Good Solution (Strategy Pattern)

```java
public interface LateFeeStrategy {
    BigDecimal calculateFee(int daysOverdue);
}
```

```java
public class PremiumLateFeeStrategy implements LateFeeStrategy {
    @Override
    public BigDecimal calculateFee(int daysOverdue) {
        return BigDecimal.ZERO;  // Waived
    }
}
```

```java
public class RegularLateFeeStrategy implements LateFeeStrategy {
    private static final BigDecimal RATE_PER_DAY = BigDecimal.valueOf(0.50);

    @Override
    public BigDecimal calculateFee(int daysOverdue) {
        return RATE_PER_DAY.multiply(BigDecimal.valueOf(daysOverdue));
    }
}
```

```java
public class GuestLateFeeStrategy implements LateFeeStrategy {
    private static final BigDecimal RATE_PER_DAY = BigDecimal.ONE;

    @Override
    public BigDecimal calculateFee(int daysOverdue) {
        return RATE_PER_DAY.multiply(BigDecimal.valueOf(daysOverdue));
    }
}
```

```java
@Component
public class LateFeeStrategyFactory {

    public LateFeeStrategy getStrategy(MembershipType membershipType) {
        return switch (membershipType) {
            case PREMIUM -> new PremiumLateFeeStrategy();
            case REGULAR -> new RegularLateFeeStrategy();
            case GUEST -> new GuestLateFeeStrategy();
        };
    }
}
```

**Usage:**
```java
@Service
public class CheckoutService {

    private final LateFeeStrategyFactory strategyFactory;

    public BigDecimal calculateLateFee(Checkout checkout) {
        int daysOverdue = calculateDaysOverdue(checkout);
        LateFeeStrategy strategy = strategyFactory.getStrategy(
            checkout.getUser().getMembershipType()
        );
        return strategy.calculateFee(daysOverdue);
    }
}
```

---

## Scenario 5: Database Migrations

**Business Requirement:** Add new book metadata without breaking existing data.

### Problem

Need to add "edition" field, but 10,000 existing books don't have it.

### Bad Solution (Nullable Everywhere)

```java
@Entity
public class Book {
    private Integer edition;  // null for old books

    public Integer getEdition() {
        return edition;  // NPE risk!
    }
}
```

### Good Solution (Null Object Pattern)

```java
@Entity
public class Book {

    private Integer edition;

    public Edition getEdition() {
        return edition != null ?
            new Edition(edition) :
            Edition.UNKNOWN;
    }
}
```

```java
public class Edition {

    public static final Edition UNKNOWN = new Edition(null);
    public static final Edition FIRST = new Edition(1);

    private final Integer number;

    private Edition(Integer number) {
        this.number = number;
    }

    public boolean isKnown() {
        return number != null;
    }

    public String getDisplayName() {
        return number != null ?
            getOrdinal(number) + " Edition" :
            "Edition Unknown";
    }

    private String getOrdinal(int n) {
        if (n >= 11 && n <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
```

**Usage:**
```java
// No null checks needed!
String displayName = book.getEdition().getDisplayName();

if (book.getEdition().isKnown()) {
    // Use edition number
}
```

---

## Scenario 6: Search with Multiple Filters

**Business Requirement:** Search by title, author, genre, status, publication year.

### Bad Solution (Method Explosion)

```java
List<Book> findByTitle(String title);
List<Book> findByAuthor(String authorName);
List<Book> findByTitleAndAuthor(String title, String authorName);
List<Book> findByTitleAndGenre(String title, Genre genre);
List<Book> findByAuthorAndGenre(String authorName, Genre genre);
// 20 more methods...
```

### Good Solution (Specification Pattern)

Already covered in Section 23, but here's the usage:

```java
@Service
public class BookSearchService {

    private final BookRepository bookRepository;

    public Page<Book> search(BookSearchCriteria criteria, Pageable pageable) {
        Specification<Book> spec = Specification.where(null);

        if (criteria.getTitle() != null) {
            spec = spec.and(BookSpecifications.titleContains(criteria.getTitle()));
        }

        if (criteria.getAuthor() != null) {
            spec = spec.and(BookSpecifications.authorNameContains(criteria.getAuthor()));
        }

        if (criteria.getGenre() != null) {
            spec = spec.and(BookSpecifications.hasGenre(criteria.getGenre()));
        }

        if (criteria.getStatus() != null) {
            spec = spec.and(BookSpecifications.hasStatus(criteria.getStatus()));
        }

        if (criteria.getPublishedAfter() != null) {
            spec = spec.and(BookSpecifications.publishedAfter(criteria.getPublishedAfter()));
        }

        return bookRepository.findAll(spec, pageable);
    }
}
```

---

## Scenario 7: Audit Logging

**Business Requirement:** Track who did what and when.

### Solution (Decorator Pattern + Events)

```java
public record AuditLog(
    Long id,
    String entityType,
    Long entityId,
    String action,
    String username,
    LocalDateTime timestamp,
    String details
) {}
```

```java
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditLogRepository auditLogRepository;

    @EventListener
    public void onBookCreated(BookCreatedEvent event) {
        AuditLog auditLog = new AuditLog(
            null,
            "Book",
            event.getBookId(),
            "CREATED",
            event.getUsername(),
            LocalDateTime.now(),
            "Book created: " + event.getBookTitle()
        );
        auditLogRepository.save(auditLog);
        log.info("Audit: Book created by {}: {}", event.getUsername(), event.getBookTitle());
    }

    @EventListener
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        AuditLog auditLog = new AuditLog(
            null,
            "Book",
            event.getBookId(),
            "CHECKED_OUT",
            event.getUsername(),
            LocalDateTime.now(),
            "Book checked out: " + event.getBookTitle()
        );
        auditLogRepository.save(auditLog);
        log.info("Audit: Book checked out by {}: {}", event.getUsername(), event.getBookTitle());
    }
}
```

---

## Design Patterns Summary

### Patterns You've Used in Bibby

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Dependency Injection** | Entire app | Loose coupling |
| **Repository** | Data access | Abstract database |
| **Service Layer** | Business logic | Transaction boundary |
| **DTO** | API layer | Data transfer |
| **Builder** | Test data | Fluent object creation |
| **Facade** | CLI commands | Simplify complex subsystems |
| **Template Method** | AbstractShellComponent | Define algorithm skeleton |

### Patterns to Consider Adding

| Pattern | Use Case | Priority |
|---------|----------|----------|
| **Strategy** | Late fees, notifications | High |
| **Observer** | Events, notifications | High |
| **Specification** | Dynamic queries | Medium |
| **Null Object** | Missing data | Medium |
| **Decorator** | Audit logging | Low |

---

## Anti-Patterns to Avoid

### 1. God Object

```java
// ❌ One class does everything
public class BookManager {
    public void createBook() { }
    public void updateBook() { }
    public void deleteBook() { }
    public void searchBooks() { }
    public void checkoutBook() { }
    public void returnBook() { }
    public void calculateLateFees() { }
    public void sendNotifications() { }
    public void generateReports() { }
    // 50 more methods...
}
```

**Solution:** Split into focused services.

### 2. Anemic Domain Model

```java
// ❌ Entity with only getters/setters
public class Book {
    private String title;
    private BookStatus status;

    // Only getters and setters, no behavior
}

// Business logic scattered in services
public class BookService {
    public void checkOut(Book book) {
        if (book.getStatus() == BookStatus.AVAILABLE) {
            book.setStatus(BookStatus.CHECKED_OUT);
        }
    }
}
```

**Solution:** Put behavior in entities.

```java
// ✅ Rich domain model
public class Book {
    private String title;
    private BookStatus status;

    public void checkOut() {
        if (status != BookStatus.AVAILABLE) {
            throw new BusinessException("Book not available");
        }
        this.status = BookStatus.CHECKED_OUT;
    }

    public boolean isAvailable() {
        return status == BookStatus.AVAILABLE;
    }
}
```

### 3. Primitive Obsession

```java
// ❌ Using primitives everywhere
public class Book {
    private String isbn;  // No validation

    public void setIsbn(String isbn) {
        this.isbn = isbn;  // What if it's invalid?
    }
}
```

**Solution:** Create value objects.

```java
// ✅ Value object
public class ISBN {
    private final String value;

    public ISBN(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid ISBN: " + value);
        }
        this.value = value;
    }

    private boolean isValid(String isbn) {
        // Validation logic
        return isbn.matches("^978-\\d{1,5}-\\d{1,7}-\\d{1,7}-\\d$");
    }

    public String getValue() {
        return value;
    }
}
```

---

## Action Items

**Apply patterns where they solve real problems:**

1. **Add Strategy Pattern for Notifications**
   - [ ] Create NotificationChannel interface
   - [ ] Implement Email/SMS channels
   - [ ] Use Spring's auto-injection

2. **Add Builder for Test Data**
   - [ ] Create BookTestDataBuilder
   - [ ] Use in all tests

3. **Consider Specification for Search**
   - [ ] Implement if you have complex search requirements

---

## Summary

**Key Takeaways:**
1. Patterns solve **specific problems**, not theoretical exercises
2. Don't force patterns where they don't fit
3. Start simple, refactor to patterns when complexity justifies it
4. The best code uses patterns invisibly

---

## Resources

### Books
- **"Design Patterns"** by Gang of Four (classic)
- **"Head First Design Patterns"** by Freeman (beginner-friendly)
- **"Refactoring to Patterns"** by Joshua Kerievsky

### Online
- [Refactoring Guru](https://refactoring.guru/design-patterns)
- [SourceMaking](https://sourcemaking.com/design_patterns)

---

## Mentor's Note

Leo, design patterns are **tools, not goals**. Don't add patterns just because they're "best practice."

**When to use patterns:**
- ✅ You have a clear problem
- ✅ The pattern solves that specific problem
- ✅ The benefits outweigh the complexity

**When NOT to use patterns:**
- ❌ "Because enterprise apps use it"
- ❌ "To show I know patterns"
- ❌ "It might be useful someday"

Your current code is **fine** without most of these patterns. Add them during refactoring when you hit the specific problems they solve.

---

**Next Section:** Implementation Roadmap

**Last Updated:** 2025-11-17
**Status:** Complete ✅
