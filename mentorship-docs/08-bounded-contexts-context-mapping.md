# SECTION 8: BOUNDED CONTEXTS & CONTEXT MAPPING

## Strategic Design for Growing Systems

Welcome to Section 8 - the final section of Part 1! We've spent Sections 1-7 learning **tactical DDD** (how to model a domain). Now we're moving to **strategic DDD** (how to organize multiple domains).

This section is about thinking bigger: What happens when your application grows? How do you organize multiple teams? When should you split into microservices? These are questions of **bounded contexts** and **context mapping**.

---

## What is a Bounded Context?

A **Bounded Context** is a boundary within which a particular domain model is defined and applicable. Inside the boundary, all terms, definitions, and rules are consistent. Outside the boundary, they may be completely different.

### The Core Insight

The same word can mean different things in different parts of your system.

**Example**: What is a "Book" ?

**In the Library Context (Bibby)**:
- A Book is a physical item with location, status, checkout history
- ISBN identifies an edition
- Author is the person who wrote it
- Book can be checked out, returned, placed on shelf

**In an E-Commerce Context**:
- A Book is a product for sale
- ISBN is a SKU (stock keeping unit)
- Author is metadata for search
- Book can be added to cart, purchased, shipped

**In a Publishing Context**:
- A Book is a manuscript being edited
- ISBN hasn't been assigned yet
- Author is a contracted writer with royalty agreements
- Book can be drafted, reviewed, approved, published

**Same word, completely different models!**

Each context has its own model. Don't try to create one "Book" class that works everywhere. That's a path to madness.

### Bounded Context Definition

A **Bounded Context**:
1. Has its own ubiquitous language
2. Has its own domain model
3. Has clear boundaries (what's inside, what's outside)
4. Can be implemented as separate modules, packages, or services
5. May have different teams responsible for different contexts

---

## Identifying Bounded Contexts

How do you find bounded context boundaries?

### Signal 1: Different Meanings for Same Term

If "Book" means different things to different stakeholders, you likely have multiple contexts.

**Questions to ask**:
- Does "checkout" mean the same thing to librarians and online retailers? No!
- Does "author" mean the same thing to publishers and libraries? Not quite!

### Signal 2: Different Business Rules

If business rules differ dramatically across domains, separate contexts.

**Example**:
- Library context: "A book can be checked out for 2 weeks"
- Sales context: "A book is shipped within 24 hours"
- Publishing context: "A book takes 6 months from draft to publication"

Different lifecycles → different contexts.

### Signal 3: Different Teams

If separate teams work on separate features, they're likely in different contexts.

**Example**:
- Library Management Team → Library context
- E-Commerce Team → Sales context
- Content Team → Publishing context

### Signal 4: Can't Agree on Model

If architects/developers can't agree on how to model something, it's a sign of implicit context boundaries.

**Disagreement**:
- "A Book should track loans!" (library context)
- "A Book should track inventory!" (sales context)
- "A Book should track revisions!" (publishing context)

**Solution**: All are right, in their own context!

---

## Bibby's Bounded Contexts (Current and Future)

Let's analyze Bibby and identify potential bounded contexts.

### Current: Single Bounded Context

Right now, Bibby is one bounded context:

**Personal Library Management Context**

**Ubiquitous Language**:
- Book, Author, Shelf, Bookcase, Collection
- Check Out, Return, Place on Shelf
- Available, Checked Out, Lost

**Boundaries**:
- Everything related to managing a personal physical library
- Tracking location, availability, reading history

**Model**:
- Book entity with physical location
- Bookcase/Shelf hierarchy
- Checkout operations

### Future: Potential Additional Contexts

As Bibby grows, you might add:

**Reading Tracking Context**

**Focus**: Track what you're reading, progress, reviews, ratings

**Ubiquitous Language**:
- Reading Session, Progress, Review, Rating, Reading Goal
- Start Reading, Finish Reading, Rate Book
- Currently Reading, Want to Read, Finished

**Model**:
- ReadingSession (different from Book!)
- Progress (percentage, pages)
- Review (your thoughts)

**Why separate?**: "Book" in this context is about the reading experience, not physical location

**Recommendation Context**

**Focus**: Suggest books based on reading history

**Ubiquitous Language**:
- Recommendation, Preference, Genre Affinity, Similar Books
- Recommend, Rate Similarity

**Model**:
- Recommendation algorithm
- Reading preferences
- Book similarity metrics

**Why separate?**: Completely different domain logic (machine learning, recommendations)

**Social Context (Future)**

**Focus**: Share libraries, loan books to friends

**Ubiquitous Language**:
- Friend, Loan, Borrowed Book, Lender, Borrower
- Loan to Friend, Return from Friend, Overdue

**Model**:
- Friend entity
- Loan entity (different from "checkout"!)
- Loan period and overdue rules

**Why separate?**: Different rules (lending to others vs. personal checkout)

---

## Context Mapping: How Contexts Interact

Once you have multiple bounded contexts, they need to communicate. **Context Mapping** is the pattern for managing relationships between contexts.

### Context Map

A **Context Map** is a diagram showing:
1. All bounded contexts
2. Relationships between them
3. How they integrate

**Example for Bibby (future state)**:
```
┌────────────────────────┐
│   Library Management   │
│      (Core)            │
│  - Book (physical)     │
│  - Shelf, Bookcase     │
│  - Checkout            │
└───────────┬────────────┘
            │
            │ supplies data
            ▼
┌────────────────────────┐         ┌──────────────────────┐
│   Reading Tracker      │◄────────│  Recommendations     │
│  - Reading Session     │ requests│  - Preference Model  │
│  - Progress            │         │  - Similarity        │
│  - Review              │         │  - Algorithm         │
└────────────────────────┘         └──────────────────────┘
            │
            │ publishes events
            ▼
     ┌──────────────┐
     │  Analytics   │
     │  - Stats     │
     │  - Reports   │
     └──────────────┘
```

---

## Context Integration Patterns

How do bounded contexts communicate? Eric Evans defined several patterns.

### Pattern 1: Shared Kernel

**Definition**: Two contexts share a small, carefully managed subset of the domain model.

**Use When**: Two teams are closely coordinated and share core concepts

**Example**: Library and Reading Tracker both share `BookId` and `ISBN`

**Risks**: Changes affect multiple contexts - high coordination needed

```java
// shared-kernel/
public record BookId(Long value) {}
public record ISBN(String value) {}

// Both contexts use these
```

### Pattern 2: Customer/Supplier

**Definition**: One context (supplier) provides data/services to another (customer). Supplier defines the interface.

**Use When**: Clear upstream/downstream relationship

**Example**: Library Management (supplier) provides book data to Reading Tracker (customer)

```java
// Library context exposes API
public interface LibraryBookQuery {
    BookDto findBook(BookId id);
}

// Reading context consumes API
public class ReadingSession {
    private final LibraryBookQuery libraryQuery;

    public void startReading(BookId bookId) {
        BookDto book = libraryQuery.findBook(bookId);
        // ... start reading session
    }
}
```

### Pattern 3: Conformist

**Definition**: Customer conforms entirely to supplier's model (no translation).

**Use When**: Customer has no leverage to change supplier

**Example**: Using external ISBN lookup API as-is

```java
// External API model
public class IsbnApiResponse {
    public String isbn;
    public String title;
    // ... their model
}

// We conform to their model
public class BookMetadataService {
    public IsbnApiResponse lookupIsbn(String isbn) {
        return isbnApi.lookup(isbn);  // Use their model directly
    }
}
```

**Downside**: External model pollutes your domain

### Pattern 4: Anti-Corruption Layer (ACL)

**Definition**: Create a translation layer to protect your domain from external models.

**Use When**: External system has incompatible model you don't want to leak into your domain

**Example**: Translate external ISBN API to your domain model

```java
// Your domain model
public class BookMetadata {
    private final ISBN isbn;
    private final Title title;
    private final Set<AuthorName> authors;
    // ... your model
}

// Anti-Corruption Layer
@Service
public class IsbnLookupAdapter {
    private final ExternalIsbnApi externalApi;

    public BookMetadata lookupBookMetadata(ISBN isbn) {
        // Call external API
        IsbnApiResponse response = externalApi.lookup(isbn.getValue());

        // Translate to domain model
        return new BookMetadata(
            isbn,
            new Title(response.title),
            parseAuthors(response.authors)
        );
    }

    private Set<AuthorName> parseAuthors(String authorsString) {
        // Translation logic
    }
}
```

**Benefit**: External API changes don't affect your domain

### Pattern 5: Separate Ways

**Definition**: No integration. Contexts are completely independent.

**Use When**: No benefit to integration, or cost is too high

**Example**: Library Management and a separate Reading Journal app

**Note**: Not relevant for contexts within Bibby (yet)

### Pattern 6: Open Host Service

**Definition**: Define a public API for other contexts to consume.

**Use When**: Multiple contexts need access to your services

**Example**: Library Management exposes REST API

```java
@RestController
@RequestMapping("/api/library")
public class LibraryBookApi {

    @GetMapping("/books/{id}")
    public BookDto getBook(@PathVariable Long id) {
        // Public API for other contexts
    }

    @GetMapping("/books/search")
    public List<BookDto> searchBooks(@RequestParam String query) {
        // Public search API
    }
}
```

### Pattern 7: Published Language

**Definition**: Well-documented, stable model for integration (often XML, JSON schema).

**Use When**: Need standard format for inter-context communication

**Example**: Standard book DTOs

```java
// Published DTO for all contexts
public class PublishedBookDto {
    public String isbn;
    public String title;
    public List<String> authors;
    public String status;

    // Documented, versioned, stable
}
```

---

## Applying Context Mapping to Bibby

### Current State: Monolith, One Context

Bibby is currently one bounded context (Library Management).

**No context mapping needed yet.**

### Near Future: Modular Monolith

As you add features, organize as modules within one app:

```
bibby/
├── library-management/         (Context 1)
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── reading-tracker/            (Context 2)
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── recommendations/            (Context 3)
│   ├── domain/
│   ├── application/
│   └── infrastructure/
└── shared-kernel/
    ├── BookId.java
    └── ISBN.java
```

**Integration**:
- Shared Kernel: `BookId`, `ISBN`
- Domain Events: Library publishes `BookCheckedOut`, Reading Tracker subscribes
- Internal APIs: `LibraryBookQuery` interface

### Far Future: Microservices

Eventually, separate deployment units:

```
┌─────────────────────┐
│ Library Service     │  (Microservice 1)
│ - Own database      │
│ - REST API exposed  │
└──────────┬──────────┘
           │ REST
           ▼
┌─────────────────────┐
│ Reading Service     │  (Microservice 2)
│ - Own database      │
│ - Consumes library  │
└─────────────────────┘
```

**Integration**:
- Open Host Service: REST APIs
- Anti-Corruption Layer: Translation DTOs
- Published Language: API contracts

---

## Bounded Contexts and Aggregates

**Question**: What's the difference between a bounded context and an aggregate?

**Answer**:
- **Aggregate**: Consistency boundary (technical)
- **Bounded Context**: Semantic boundary (conceptual)

**Scale**:
- Aggregate: 1-10 entities
- Bounded Context: 10-100 entities

**Example**:
- **Aggregate**: Book (with title, ISBN, authors)
- **Bounded Context**: Library Management (with Books, Shelves, Bookcases, Members, Loans)

**Rule**: Aggregates live *within* bounded contexts, never across contexts.

---

## Action Items

### 1. Define Current Bounded Context (2-3 hours)

Document Bibby's current bounded context:
- Name: "Personal Library Management"
- Ubiquitous language: List all terms
- Boundaries: What's included, what's excluded
- Domain model: Main entities and aggregates
- Invariants: Key business rules

Create `bounded-context-library-management.md`

### 2. Identify Future Contexts (2-3 hours)

Brainstorm contexts you might add:
- Reading Tracker?
- Book Reviews?
- Reading Goals?
- Social/Sharing?
- Book Recommendations?

For each:
- What is the focus?
- What is the ubiquitous language?
- How is "Book" modeled differently?
- What integration patterns would you use?

Create `future-contexts.md`

### 3. Draw Context Map (1-2 hours)

Create a diagram showing:
- Current context (Library Management)
- Future contexts (2-3 you might add)
- Relationships and integration patterns
- Data flow between contexts

Use draw.io, Lucidchart, or pen and paper.

### 4. Plan Modular Monolith (2-3 hours)

Design package structure for modular monolith:
- How would you organize multiple contexts in one app?
- What would be shared kernel?
- How would contexts communicate (events, interfaces)?

Create `modular-monolith-design.md`

### 5. Research Microservices (1-2 hours)

Study:
- When to split monolith into microservices
- Benefits and costs
- How Bibby might evolve into microservices

Not to implement now, but understand the path.

---

## Key Takeaways

### 1. One Model Can't Rule Them All
- Same terms mean different things in different contexts
- Don't try to create universal models
- Embrace multiple models with clear boundaries

### 2. Bounded Context = Semantic Boundary
- Clear boundary where ubiquitous language is consistent
- Different contexts have different models
- Can be packages, modules, or microservices

### 3. Context Mapping Manages Integration
- Shared Kernel: Shared code
- Customer/Supplier: Provider/consumer relationship
- Anti-Corruption Layer: Protect domain from external models
- Open Host Service: Public API
- Separate Ways: No integration

### 4. Start Monolith, Evolve to Microservices
- Begin with one bounded context (Bibby today)
- Modularize as features grow (modular monolith)
- Split into microservices only when necessary

### 5. Strategic Design Prevents Big Balls of Mud
- Thinking about contexts early prevents architectural mess
- Clear boundaries enable independent evolution
- Easier to add features without breaking everything

---

## Further Study

**Books**:
- "Domain-Driven Design" by Eric Evans - Part IV (Strategic Design)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Chapter 2 (Bounded Contexts), Chapter 3 (Context Maps)
- "Domain-Driven Design Distilled" by Vaughn Vernon - Very accessible intro

**Articles**:
- Martin Fowler: "Bounded Context"
- "Context Mapping" patterns
- Microsoft: "Identify microservice boundaries"

**Tools**:
- Context Mapper (DSL for context mapping)
- C4 Model for architectural diagrams

---

## Mentor's Final Words for Part 1

Congratulations! You've completed Part 1: **Domain Driven Design**.

Let's recap the journey:

### What You've Learned

**Section 1**: Ubiquitous Language - Speaking the language of the domain
**Section 2**: Entities & Value Objects - Building blocks of domain models
**Section 3**: Aggregates - Consistency boundaries
**Section 4**: Repositories - Persistence abstraction
**Section 5**: Domain Services - Cross-entity logic
**Section 6**: Domain Events - Making implicit concepts explicit
**Section 7**: Architecture - Layering and hexagonal design
**Section 8**: Bounded Contexts - Strategic design

### The Transformation Ahead

When you apply these patterns to Bibby, your code will transform:

**From**:
- God classes doing everything
- Anemic entities with only getters/setters
- Mixed concerns (domain + infrastructure)
- Unclear boundaries and dependencies
- Hard to test, hard to extend

**To**:
- Rich domain models with behavior
- Clear separation of concerns
- Explicit business rules
- Clean architecture
- Testable, maintainable, extensible

### This is Just the Beginning

DDD is a craft you refine over years, not weeks. You'll make mistakes. That's how you learn.

**My advice**:
1. Start with one refactoring (e.g., create ISBN value object)
2. See the benefit firsthand
3. Build momentum
4. Don't try to refactor everything at once

### What's Next

**Part 2**: Gang of Four Design Patterns (Sections 9-17)
**Part 3**: Pragmatic Programmer Principles (Sections 18-23)
**Part 4**: Java Testing Mastery (Sections 24-29)
**Part 5**: Integration & Career Development (Sections 30-34)

You're building a foundation that will serve your entire career.

I'm proud of your commitment to learning. Not many junior engineers invest this deeply in understanding software design.

Ready for Part 2? Let me know when you want to continue!

---

**Section 8 Complete** | **Part 1: Domain Driven Design Complete!**

**Next**: Part 2 - Gang of Four Design Patterns (check in with user first)
