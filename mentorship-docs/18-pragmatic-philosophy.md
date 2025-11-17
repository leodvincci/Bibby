# SECTION 18: THE PRAGMATIC PHILOSOPHY

## Introduction: Building Software Craftsmanship

*"The Pragmatic Programmer"* by Andrew Hunt and David Thomas introduced principles that separate good programmers from great ones. These aren't trendy patterns that come and go - they're timeless principles that define software craftsmanship.

This section applies these core philosophical principles to your **Bibby** library management codebase. We'll examine real code from your project, identify "broken windows," and show how to apply pragmatic thinking to create maintainable, professional software.

**Learning Objectives**:
- Understand the 7 core principles of pragmatic programming
- Identify "broken windows" in existing code
- Apply craftsmanship thinking to everyday coding decisions
- Develop a mindset of continuous improvement
- Learn to provide options rather than excuses

**Time Investment**: 3-4 hours for reading, analysis, and exercises

---

## The Seven Core Principles

### 1. Care About Your Craft

**Principle**: *Why spend your life developing software unless you care about doing it well?*

Software development is a craft. Like a carpenter who takes pride in smooth joints and level surfaces, programmers should take pride in clean code, good design, and elegant solutions.

**Your code is communication**:
- With future developers (including yourself in 6 months)
- With stakeholders about what the system does
- About your professionalism and attention to detail

**Current Bibby Example** - Lack of Care Evident:

From `AuthorEntity.java:54-68`:
```java
public void setFirstName(String firstName) {
    this.firstName = firstName;
}

public String getLastName() {
    return lastName;
}

public void setLastName(String lastName) {
    this.lastName = lastName;
}

public String getFullName() {
    return fullName;
}

public void setFullName(String fullName) {
    this.fullName = fullName;
}
```

**Problem**: The constructor (line 27) sets `fullName = String.format("%s %s", firstName, lastName)`, but the setters for `firstName` and `lastName` don't update `fullName`! This is a **broken window** - a bug waiting to happen:

```java
AuthorEntity author = new AuthorEntity("Robert", "Martin");
System.out.println(author.getFullName());  // "Robert Martin" ✓

author.setLastName("C. Martin");
System.out.println(author.getFullName());  // Still "Robert Martin" ❌
// fullName is now WRONG!
```

**Caring means**:
- Noticing this bug during code review
- Fixing it immediately, not "later"
- Preventing it with proper design (make fullName derived, not stored)

**Refactored with Care**:
```java
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long authorId;

    private String firstName;
    private String lastName;

    // No fullName field - it's DERIVED, not stored!

    public AuthorEntity(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, "First name required");
        this.lastName = Objects.requireNonNull(lastName, "Last name required");
    }

    public AuthorEntity() {
        // For JPA only
    }

    public String getFullName() {
        // Always correct - derived from source of truth
        return String.format("%s %s", firstName, lastName);
    }

    public void updateName(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
        // fullName automatically correct via getFullName()
    }

    @Override
    public String toString() {
        return getFullName();  // Use method, not field
    }
}
```

**What changed**:
- ✅ Removed `fullName` field - no duplication of knowledge
- ✅ `getFullName()` derives value - always correct
- ✅ Added validation - care about data integrity
- ✅ No silent bugs - impossible to have inconsistent state

---

### 2. Think! About Your Work

**Principle**: *Turn off the autopilot and take control. Constantly critique and appraise your work.*

Don't code on autopilot. Question requirements, assumptions, and your own solutions. Think about edge cases, failure modes, and long-term maintainability.

**Current Bibby Example** - Coding on Autopilot:

From `ShelfService.java:20-30`:
```java
public List<ShelfEntity> getAllShelves(Long bookCaseId){
    return shelfRepository.findByBookcaseId(bookCaseId);
}

public Optional<ShelfEntity> findShelfById(Long shelfId) {
    return shelfRepository.findById(shelfId);
}

public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {
    return shelfRepository.findByBookcaseId(bookcaseId);
}
```

**Problem - Did you notice?**
Lines 20-22 (`getAllShelves`) and lines 28-30 (`findByBookcaseId`) **do exactly the same thing!**

Someone wrote `getAllShelves` first. Later (maybe the same person, maybe not), they needed to find shelves by bookcase ID. Without thinking, they added `findByBookcaseId` - duplicating functionality that already exists!

**Thinking would reveal**:
1. "Do I already have a method for this?"
2. "What's the difference between 'get all' and 'find by ID'?"
3. "Which method name is clearer?"

**Refactored with Thought**:
```java
@Service
public class ShelfService {

    private final ShelfRepository shelfRepository;  // final - shows care!

    public ShelfService(ShelfRepository shelfRepository) {
        this.shelfRepository = shelfRepository;
    }

    /**
     * Finds all shelves belonging to the specified bookcase.
     * Returns empty list if bookcase has no shelves.
     */
    public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {
        Objects.requireNonNull(bookcaseId, "Bookcase ID cannot be null");
        return shelfRepository.findByBookcaseId(bookcaseId);
    }

    /**
     * Finds a shelf by its unique ID.
     * Returns empty Optional if shelf doesn't exist.
     */
    public Optional<ShelfEntity> findById(Long shelfId) {
        Objects.requireNonNull(shelfId, "Shelf ID cannot be null");
        return shelfRepository.findById(shelfId);
    }

    /**
     * Gets summary view of shelves for display purposes.
     * Includes book counts and availability.
     */
    public List<ShelfSummary> getSummariesForBookcase(Long bookcaseId) {
        Objects.requireNonNull(bookcaseId, "Bookcase ID cannot be null");
        return shelfRepository.findShelfSummariesByBookcaseId(bookcaseId);
    }
}
```

**What changed**:
- ❌ Removed duplicate `getAllShelves` method
- ✅ One method, one responsibility, clear name
- ✅ Added null checks - defensive programming
- ✅ Added Javadoc - communication
- ✅ Made repository field `final` - immutability

---

### 3. Provide Options, Don't Make Lame Excuses

**Principle**: *Instead of excuses, provide options. Don't say things can't be done; explain what CAN be done to salvage the situation.*

Before you approach anyone to tell them why something can't be done, is late, or is broken, stop and listen to yourself. Talk to the rubber duck on your monitor, or the cat. Does your excuse sound reasonable, or stupid? How's it going to sound to your boss?

**Professional Approach**:
- ❌ "I can't add validation - the code is too complicated"
- ✅ "We have three options:
  1. Add validation at the service layer (quick, 2 hours, but not ideal)
  2. Refactor to domain model with built-in validation (proper, 8 hours)
  3. Add database constraints only (1 hour, partial solution)

  I recommend option 2 because it prevents bugs everywhere, not just at entry points."

**Scenario in Bibby**:

Imagine you discover the `AuthorEntity.fullName` bug above. How do you respond?

**Lame Excuse**:
> "We can't fix this now - it would break existing code. Too risky."

**Providing Options**:
> "I found a bug where changing an author's name doesn't update fullName. Here are our options:
>
> **Option 1: Quick Fix (30 min)**
> - Update `setFirstName()` and `setLastName()` to recalculate `fullName`
> - Pros: Fast, backwards compatible
> - Cons: Still storing duplicate data, easy to forget in future
>
> **Option 2: Proper Fix (1 hour)**
> - Remove `fullName` field, make `getFullName()` derived
> - Update database migration to remove column
> - Pros: Bug impossible to reoccur, cleaner design
> - Cons: Need migration, slightly more work
>
> **Option 3: Defer But Track (5 min)**
> - Add TODO and tech debt ticket
> - Add warning comment
> - Pros: Ship now, fix in next sprint
> - Cons: Bug still exists, might bite us
>
> **I recommend Option 2** - it's only 30 minutes more than Option 1, and prevents the entire class of bugs permanently."

**Own your code and decisions**. Don't blame tools, deadlines, or other people. Take responsibility and provide solutions.

---

### 4. Don't Live with Broken Windows

**Principle**: *Fix bad designs, wrong decisions, and poor code when you see them.*

The "Broken Window Theory" comes from urban sociology: a building with broken windows looks abandoned, leading to more vandalism. In software: one piece of bad code encourages more bad code.

"I'll fix it later" = technical debt accumulates = project degradation.

**Current Bibby Example** - Broken Window #1:

From `BookEntity.java:54-56`:
```java
public void setAuthors(AuthorEntity authors) {
    this.authors.add(authors);
}
```

**Broken Windows**:
1. Parameter is named `authors` (plural) but type is `AuthorEntity` (singular)
2. Method name is `setAuthors` (replace) but behavior is ADD
3. No validation - can add null
4. Exposes internal collection mutation

This is sloppy code. It works, but it's confusing and error-prone. A broken window.

**Fixed Immediately**:
```java
// Clear name, correct type, validation
public void addAuthor(AuthorEntity author) {
    Objects.requireNonNull(author, "Author cannot be null");
    this.authors.add(author);
}

// Defensive copy prevents external mutation
public Set<AuthorEntity> getAuthors() {
    return Collections.unmodifiableSet(authors);
}

// Proper "set" if you need to replace all authors
public void setAuthors(Set<AuthorEntity> newAuthors) {
    Objects.requireNonNull(newAuthors, "Authors cannot be null");
    this.authors.clear();
    this.authors.addAll(newAuthors);
}
```

**Current Bibby Example** - Broken Window #2:

From `BookService.java:53-62`:
```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
    // No else - silent failure!
}
```

**Broken Windows**:
1. Mixing enum (`BookStatus.CHECKED_OUT`) with string (`.toString()`)
2. Comparing to "CHECKED_OUT" then setting "CHECKED_OUT" - logic seems backward?
3. Silent failure - no error if book already checked out
4. No event raised, no audit trail
5. Business logic in service instead of entity

**One Broken Window Leads to More**:

Because this code is sloppy, future developers think "I guess quality doesn't matter here" and add more sloppy code. The codebase degrades.

**Fixed - No Broken Windows**:
```java
@Service
public class CheckOutBookService {

    private final BookRepository bookRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public void checkOut(BookId bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        // Business logic in domain model
        book.checkOut();

        bookRepository.save(book);
        eventPublisher.publish(new BookCheckedOutEvent(bookId));
    }
}

// In Book domain model
public void checkOut() {
    // Explicit precondition check
    if (this.status != AvailabilityStatus.AVAILABLE) {
        throw new BookNotAvailableException(
            "Cannot check out book " + id + " - current status: " + status.getDescription()
        );
    }

    // Change state
    this.status = AvailabilityStatus.CHECKED_OUT;
    this.checkoutCount++;
    this.lastCheckoutDate = LocalDateTime.now();

    // Postcondition assertion (design by contract)
    assert this.status == AvailabilityStatus.CHECKED_OUT
        : "Postcondition failed: status should be CHECKED_OUT";
}
```

**What changed**:
- ✅ No string comparisons - use enum directly
- ✅ Explicit failure - throw exception if can't check out
- ✅ Business logic in entity - proper DDD
- ✅ Audit trail - checkoutCount, lastCheckoutDate
- ✅ Events published - other systems can react
- ✅ Assertions - fail fast in development

**Rule**: Fix broken windows the moment you see them. Don't let bad code accumulate.

---

### 5. Be a Catalyst for Change

**Principle**: *Most software disasters start as someone saying "It's not my job." Be the catalyst that starts change.*

It's easier to ask forgiveness than permission. If you see a better way, build a prototype and show people. Actions speak louder than words.

**The Stone Soup Story**:

Three soldiers arrive at a village during a famine. Villagers hide food, won't share. Soldiers set up a pot, fill it with water, add a stone. "Stone soup!" Curious villagers ask about it. "It's good, but could use a carrot..." One villager brings a carrot. "Now maybe a potato..." Another brings potatoes. Soon everyone's contributing. They all eat together.

**Software Application**:

Don't ask for permission to refactor the entire codebase. Start small, show value, attract contributors.

**Bibby Example**:

Instead of: "We should refactor everything to use value objects."

Do this:
```java
// Step 1: Create ONE value object quietly
public record ISBN(String value) {
    public ISBN {
        Objects.requireNonNull(value, "ISBN cannot be null");
        String cleaned = value.replaceAll("[\\s-]", "");
        if (!isValid(cleaned)) {
            throw new IllegalArgumentException("Invalid ISBN: " + value);
        }
        value = cleaned;
    }

    private static boolean isValid(String isbn) {
        // ISBN-10 or ISBN-13 validation
        return isbn.matches("^\\d{9}[\\dX]$") || isbn.matches("^\\d{13}$");
    }
}

// Step 2: Use it in ONE place
public class Book {
    private ISBN isbn;  // Instead of String isbn

    // Others see it works, looks clean
}

// Step 3: Others notice, ask about it
// "Hey, what's this ISBN class? I like it!"

// Step 4: Natural adoption spreads
// Soon you have Title, Publisher, Author value objects
// You didn't mandate it - you catalyzed it
```

**Show, don't tell**. Build the prototype. Prove the concept. Let the quality speak for itself.

---

### 6. Remember the Big Picture

**Principle**: *Constantly review what's happening around you, not just what you personally are doing.*

Don't get so focused on the details that you miss the big picture. Step back regularly. Ask:
- Does this serve the project's goals?
- Am I solving the right problem?
- Is this the simplest solution?

**The Frog in Boiling Water**:

If you drop a frog in boiling water, it jumps out. But if you put it in cold water and slowly heat it, the frog doesn't notice the gradual change and boils to death.

**Software Application**:

Your codebase might be slowly degrading, but you don't notice because you see it every day. Step back regularly and ask:
- Is our architecture still serving our needs?
- Are we accumulating too much technical debt?
- Have we drifted from our original design principles?

**Bibby Big Picture Questions**:

1. **Original Goal**: Personal library CLI tool
   - **Reality Check**: Are we over-engineering? Do we need full DDD for a personal tool?
   - **Answer**: It's a learning project, so yes - but keep it pragmatic

2. **Current State**: Anemic domain model, service layer doing everything
   - **Big Picture**: This doesn't scale. As features grow, services become god objects
   - **Action**: Gradually move logic to domain model

3. **Database Design**: Many-to-many for books and authors
   - **Big Picture**: Do we need this complexity now? Most books have 1-2 authors
   - **Pragmatic**: Start simple (one-to-many), refactor later if needed
   - **Counter**: But migration is harder later...
   - **Decision**: Context-dependent - think about YOUR big picture

**Exercise**: Review Current Architecture

Look at your current `BookService`:
```java
@Service
public class BookService {
    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public void createNewBook(BookRequestDTO bookRequestDTO) { ... }
    public BookEntity findBookByTitle(String title) { ... }
    public void checkOutBook(BookEntity bookEntity) { ... }
    public void updateBook(BookEntity bookEntity) { ... }
    public List<AuthorEntity> findAuthorsByBookId(Long bookId) { ... }
    public void checkInBook(String bookTitle) { ... }
    public List<BookEntity> findBooksByShelf(Long id) { ... }
    public List<BookSummary> getBooksForShelf(Long shelfId) { ... }
    public BookDetailView getBookDetails(Long bookId) { ... }
    public Optional<BookEntity> findBookById(Long bookId) { ... }
}
```

**Big Picture Analysis**:
- 10 public methods - too many responsibilities
- Mix of commands (create, checkout) and queries (find, get)
- Book creation, book search, book checkout, book reporting - all in one class
- As Bibby grows, this becomes a 500-line god class

**Big Picture Solution**:
- Split into focused services: `AddBookService`, `CheckOutBookService`, `BookQueryService`
- Follow CQRS pattern: separate commands from queries
- Move business logic to domain model
- Keep services thin - orchestration only

---

### 7. Make Quality a Requirements Issue

**Principle**: *Great software today is often preferable to perfect software tomorrow. Know when to stop.*

Quality is a requirements issue. You need to understand what "good enough" means in your context. Perfect is the enemy of done.

**The Quality Spectrum**:

```
Low Quality          Pragmatic Quality          Over-Engineering
    |__________________|__________________________|
   "Hacky script"    "Production app"         "Enterprise Framework"
   1 hour            2 days                   2 months
```

**For Bibby** (personal learning project):

**Good Enough**:
- ✅ Value objects for core concepts (ISBN, Title)
- ✅ Domain model with business logic
- ✅ Clean separation of concerns
- ✅ Comprehensive unit tests for domain logic
- ✅ Integration tests for critical paths

**Over-Engineering**:
- ❌ Full event sourcing with Kafka
- ❌ Microservices architecture
- ❌ CQRS with separate read/write databases
- ❌ 100% test coverage including getters/setters
- ❌ Custom ORM framework

**Under-Engineering**:
- ❌ No tests at all
- ❌ No value objects - all primitives
- ❌ No validation
- ❌ One giant class

**Questions to Ask**:

1. **Who will use this?** Just you? Team of 10? Enterprise with 1000 users?
2. **How long will it live?** One-off script? Multi-year project?
3. **What's the cost of failure?** Inconvenience? Money lost? Lives at risk?
4. **What's the team's skill level?** Junior? Senior? Mixed?

**For Bibby**:
- **Users**: Just you (learning)
- **Lifespan**: Educational project, maybe a year
- **Cost of failure**: You re-run a command, no big deal
- **Skill level**: Learning DDD and patterns

**Pragmatic Quality Level**:
- Apply patterns where they teach you something
- Don't over-engineer for scale you won't need
- Do write tests - they teach you design
- Don't aim for 100% coverage
- Do refactor code smells when you see them
- Don't refactor just to use a pattern

**Balance** quality with delivery. Ship something that works, learn from it, iterate.

---

## Pragmatic Mindset: Daily Practices

### Morning Code Review Ritual

Before writing new code, spend 10 minutes reviewing yesterday's work:

```java
// Questions to ask:
// 1. Does this code communicate its intent?
// 2. Are there any broken windows I should fix?
// 3. Can I simplify this?
// 4. Did I apply the Boy Scout Rule? (leave it better than I found it)
// 5. Would I be proud to show this in a code review?
```

### The Boy Scout Rule

**"Always leave the campground cleaner than you found it."**

Applied to code:
- Fix a broken window while you're in the file anyway
- Extract a magic number to a constant
- Rename a confusing variable
- Add a missing null check
- Split a long method

**Small improvements compound over time.**

### Rubber Duck Debugging

Before asking for help, explain the problem to a rubber duck (or any inanimate object). Often, the act of explaining reveals the solution.

**Process**:
1. Describe what the code is supposed to do
2. Explain what it's actually doing
3. Walk through it line by line
4. Notice the discrepancy

---

## Action Items: Applying Pragmatic Philosophy to Bibby

### Action Item 1: Identify Your Broken Windows (1 hour)

**Task**: Audit your Bibby codebase and list 10 broken windows.

**Template**:
```
Broken Window #1: [AuthorEntity.fullName bug]
Location: AuthorEntity.java:15-27
Problem: fullName stored separately, not updated when firstName/lastName change
Impact: Data inconsistency bug
Fix: Remove fullName field, make getFullName() derived
Effort: 30 minutes
Priority: High (correctness bug)
```

Create a `BROKEN_WINDOWS.md` file and track them. Fix one per day.

### Action Item 2: Fix the AuthorEntity.fullName Bug (30 min)

Apply Principle #4 (Don't Live with Broken Windows):

1. Read `AuthorEntity.java`
2. Remove the `fullName` field
3. Make `getFullName()` return `firstName + " " + lastName`
4. Remove the `setFullName()` method
5. Update constructor
6. Write a test that proves it's fixed:

```java
@Test
void fullNameShouldUpdateWhenNameChanges() {
    AuthorEntity author = new AuthorEntity("Robert", "Martin");
    assertThat(author.getFullName()).isEqualTo("Robert Martin");

    author.setLastName("C. Martin");
    assertThat(author.getFullName()).isEqualTo("Robert C. Martin");  // Now correct!
}
```

7. Commit with message: "Fix broken window: make fullName derived instead of stored"

### Action Item 3: Eliminate Duplicate Method in ShelfService (15 min)

Apply Principle #2 (Think! About Your Work):

1. Remove either `getAllShelves` or `findByBookcaseId` (they're identical)
2. Update callers to use the remaining method
3. Add null check: `Objects.requireNonNull(bookcaseId)`
4. Make `shelfRepository` field `final`
5. Commit: "Remove duplicate method in ShelfService"

### Action Item 4: Refactor checkOutBook Method (1 hour)

Apply Principle #1 (Care About Your Craft):

1. Move business logic from `BookService` to `Book` domain model
2. Use proper enum instead of string for status
3. Throw exception on failure instead of silent no-op
4. Add proper validation
5. Write tests that prove correctness

Reference the "Fixed" version shown in Principle #4 above.

### Action Item 5: Be a Catalyst - Create ISBN Value Object (30 min)

Apply Principle #5 (Be a Catalyst for Change):

Don't refactor everything. Create ONE value object and use it in ONE place:

1. Create `ISBN` record with validation
2. Update `Book` class to use `ISBN` instead of `String isbn`
3. Show the improvement in a team meeting (or just appreciate it yourself)
4. Let natural adoption happen

### Action Item 6: Big Picture Review (30 min)

Apply Principle #6 (Remember the Big Picture):

Write a `ARCHITECTURE_REVIEW.md` document answering:

1. What is Bibby's primary purpose?
2. What are the core features we need?
3. What's the current architecture?
4. Does the current architecture serve our needs?
5. What are the biggest pain points?
6. What's the simplest improvement we can make?

Review this document monthly.

### Action Item 7: Define "Good Enough" for Bibby (15 min)

Apply Principle #7 (Make Quality a Requirements Issue):

Create a `QUALITY_STANDARDS.md` file:

```markdown
# Bibby Quality Standards

## Core Principles
- Educational value > Production perfection
- Apply patterns to learn, not to impress
- Write tests for learning, not coverage metrics
- Refactor when it teaches something

## Good Enough Means:
- [ ] Core domain has value objects
- [ ] Business logic in domain model, not services
- [ ] Unit tests for domain logic
- [ ] Integration tests for critical paths
- [ ] No broken windows (bugs fixed immediately)
- [ ] Code is readable and maintainable

## Over-Engineering We Avoid:
- Event sourcing
- Microservices
- Custom frameworks
- 100% coverage
- Premature optimization

## Under-Engineering We Reject:
- No tests
- All primitives
- God classes
- No validation
```

---

## Key Takeaways

### 1. Craftsmanship is a Mindset

Quality isn't something you add at the end - it's how you think about code every day:
- Take pride in your work
- Leave code better than you found it
- Fix broken windows immediately
- Think critically about every line

### 2. Small Actions Compound

You don't need to refactor the entire codebase in one sprint:
- Fix one broken window per day
- Extract one magic number
- Rename one confusing variable
- Write one test

In a year, you'll have a transformed codebase.

### 3. Provide Options, Not Excuses

When you encounter a problem:
- Don't say "can't"
- Provide alternatives
- Recommend a solution
- Take ownership

### 4. Be the Catalyst for Change

Don't wait for permission to improve things:
- Build a prototype
- Show the value
- Let quality speak for itself
- Natural adoption follows

### 5. Balance Quality with Pragmatism

Understand your context:
- What's "good enough" for this project?
- Who are the users?
- What's the cost of failure?
- Don't over-engineer, don't under-engineer

---

## Study Resources

### Books
1. **"The Pragmatic Programmer" by Andrew Hunt and David Thomas**
   - The source of these principles
   - Chapters 1-2 are essential
   - Full of practical wisdom

2. **"Clean Code" by Robert C. Martin**
   - Complements pragmatic philosophy
   - Focus on craftsmanship
   - Chapter 17: "Smells and Heuristics"

3. **"The Clean Coder" by Robert C. Martin**
   - Professional ethics and attitudes
   - What it means to be a craftsman
   - Time management and discipline

### Articles
1. **"The Broken Window Theory"** - Original sociology paper
   - Understanding the metaphor's origins
   - Why small things matter

2. **"Technical Debt" by Ward Cunningham**
   - When debt is acceptable
   - How to manage it

### Videos
1. **"The Pragmatic Programmer" - Dave Thomas keynote** (YouTube)
   - 20 years later reflections
   - What's changed, what hasn't

---

## Coming Next

**Section 19: The DRY Principle** - Don't Repeat Yourself

We'll dive deep into knowledge duplication vs. code duplication, and refactor Bibby to eliminate violations:
- Types of duplication
- How to identify DRY violations
- When duplication is actually okay
- Refactoring techniques

---

**Section 18 Complete** | **Time Invested**: 3-4 hours | **Files to Create**: `BROKEN_WINDOWS.md`, `ARCHITECTURE_REVIEW.md`, `QUALITY_STANDARDS.md`

The pragmatic philosophy isn't about perfection - it's about continuous improvement, professional pride, and thoughtful decision-making. Apply these principles daily, and you'll build software you're proud to show.
