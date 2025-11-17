# SECTION 27: TEST-DRIVEN DEVELOPMENT (TDD)

## Introduction: Tests First, Code Second

**"Write a test. Make it fail. Make it pass. Refactor. Repeat."**

Everything you've learned so far - unit tests, mocking, integration tests - you've been writing **after** the code exists. But what if you wrote the **test first**?

**Test-Driven Development (TDD)** flips the traditional approach:

**Traditional**:
1. Write code
2. Write tests for the code
3. Hope the tests pass

**TDD**:
1. Write a failing test
2. Write **just enough code** to make it pass
3. Refactor
4. Repeat

In this section, we'll master TDD by building a complete Bibby feature from scratch, test-first.

**Learning Objectives**:
- Understand the Red-Green-Refactor cycle
- Write tests before writing code
- Use tests to drive design
- Apply TDD to build a Bibby feature
- Know when to use TDD vs. test-after
- Avoid common TDD pitfalls

**Time Investment**: 4-5 hours

---

## Part 1: The TDD Cycle - Red, Green, Refactor

### The Three Laws of TDD

From Robert C. Martin (Uncle Bob):

1. **You may not write production code** until you have written a failing test
2. **You may not write more of a test** than is sufficient to fail
3. **You may not write more production code** than is sufficient to make the test pass

**Translation**: Tiny iterations. Write one failing test, write minimal code to pass it, refactor, repeat.

### The Red-Green-Refactor Cycle

```
   ┌──────────────────────┐
   │                      │
   │   🔴 RED             │
   │   Write failing test │
   │                      │
   └──────┬───────────────┘
          │
          ▼
   ┌──────────────────────┐
   │                      │
   │   🟢 GREEN           │
   │   Make test pass     │
   │   (minimal code)     │
   │                      │
   └──────┬───────────────┘
          │
          ▼
   ┌──────────────────────┐
   │                      │
   │   🔵 REFACTOR        │
   │   Clean up code      │
   │   All tests still    │
   │   pass               │
   │                      │
   └──────┬───────────────┘
          │
          └───► Back to RED
```

### Example: Simple Calculator (Learning TDD)

**Iteration 1: RED**

Write a failing test:
```java
@Test
void shouldAddTwoNumbers() {
    Calculator calc = new Calculator();

    int result = calc.add(2, 3);

    assertThat(result).isEqualTo(5);
}
```

**Run test**: ❌ FAILS (Calculator class doesn't exist)

**Iteration 1: GREEN**

Write **minimal** code to pass:
```java
public class Calculator {
    public int add(int a, int b) {
        return 5;  // Hardcoded! But test passes!
    }
}
```

**Run test**: ✅ PASSES

**Wait, hardcoding 5 is cheating!**

Not yet. The test doesn't prove we need general addition. Let's write another test to force it.

**Iteration 2: RED**

Write another failing test:
```java
@Test
void shouldAddDifferentNumbers() {
    Calculator calc = new Calculator();

    int result = calc.add(10, 7);

    assertThat(result).isEqualTo(17);
}
```

**Run tests**: ❌ FAILS (returns 5, not 17)

**Iteration 2: GREEN**

Now we **must** implement proper addition:
```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;  // Actual implementation
    }
}
```

**Run tests**: ✅ BOTH PASS

**Iteration 3: REFACTOR**

Code is already clean. Nothing to refactor yet.

**This is TDD**: Small steps, always driven by failing tests.

---

## Part 2: TDD in Practice - Building a Bibby Feature

Let's build a **real Bibby feature** using TDD from scratch: **Loan Management**.

**Feature**: Track when books are loaned to friends, with due dates and overdue calculations.

### Iteration 1: Create a Loan

**RED: Write failing test**

```java
package com.penrose.bibby.library.loan;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LoanTest {

    @Test
    void shouldCreateLoanWithBookAndBorrower() {
        BookId bookId = BookId.generate();
        String borrower = "Alice";
        LocalDate loanDate = LocalDate.of(2024, 1, 1);

        Loan loan = Loan.create(bookId, borrower, loanDate);

        assertThat(loan.getBookId()).isEqualTo(bookId);
        assertThat(loan.getBorrower()).isEqualTo(borrower);
        assertThat(loan.getLoanDate()).isEqualTo(loanDate);
    }
}
```

**Run**: ❌ FAILS (Loan class doesn't exist)

**GREEN: Minimal code to pass**

```java
package com.penrose.bibby.library.loan;

import com.penrose.bibby.library.book.BookId;
import java.time.LocalDate;

public class Loan {
    private BookId bookId;
    private String borrower;
    private LocalDate loanDate;

    private Loan(BookId bookId, String borrower, LocalDate loanDate) {
        this.bookId = bookId;
        this.borrower = borrower;
        this.loanDate = loanDate;
    }

    public static Loan create(BookId bookId, String borrower, LocalDate loanDate) {
        return new Loan(bookId, borrower, loanDate);
    }

    public BookId getBookId() { return bookId; }
    public String getBorrower() { return borrower; }
    public LocalDate getLoanDate() { return loanDate; }
}
```

**Run**: ✅ PASSES

**REFACTOR**: Extract Borrower value object?

Not yet. Let's see if we need it.

### Iteration 2: Calculate Due Date

**RED: Write failing test**

```java
@Test
void shouldHaveDueDateTwoWeeksAfterLoan() {
    BookId bookId = BookId.generate();
    LocalDate loanDate = LocalDate.of(2024, 1, 1);

    Loan loan = Loan.create(bookId, "Alice", loanDate);

    LocalDate expectedDueDate = LocalDate.of(2024, 1, 15);  // 14 days later
    assertThat(loan.getDueDate()).isEqualTo(expectedDueDate);
}
```

**Run**: ❌ FAILS (getDueDate() doesn't exist)

**GREEN: Add due date calculation**

```java
public class Loan {
    private static final int LOAN_PERIOD_DAYS = 14;

    private BookId bookId;
    private String borrower;
    private LocalDate loanDate;
    private LocalDate dueDate;

    private Loan(BookId bookId, String borrower, LocalDate loanDate) {
        this.bookId = bookId;
        this.borrower = borrower;
        this.loanDate = loanDate;
        this.dueDate = loanDate.plusDays(LOAN_PERIOD_DAYS);  // Calculate due date
    }

    // ... existing methods ...

    public LocalDate getDueDate() {
        return dueDate;
    }
}
```

**Run**: ✅ PASSES

**REFACTOR**: Code is clean. Continue.

### Iteration 3: Determine if Loan is Overdue

**RED: Write failing test**

```java
@Test
void shouldNotBeOverdueWhenBeforeDueDate() {
    LocalDate loanDate = LocalDate.of(2024, 1, 1);
    Loan loan = Loan.create(BookId.generate(), "Alice", loanDate);

    LocalDate today = LocalDate.of(2024, 1, 10);  // 5 days before due

    assertThat(loan.isOverdue(today)).isFalse();
}

@Test
void shouldBeOverdueWhenAfterDueDate() {
    LocalDate loanDate = LocalDate.of(2024, 1, 1);
    Loan loan = Loan.create(BookId.generate(), "Alice", loanDate);

    LocalDate today = LocalDate.of(2024, 1, 20);  // 5 days after due

    assertThat(loan.isOverdue(today)).isTrue();
}

@Test
void shouldNotBeOverdueOnDueDate() {
    LocalDate loanDate = LocalDate.of(2024, 1, 1);
    Loan loan = Loan.create(BookId.generate(), "Alice", loanDate);

    LocalDate today = LocalDate.of(2024, 1, 15);  // Exact due date

    assertThat(loan.isOverdue(today)).isFalse();
}
```

**Run**: ❌ FAILS (isOverdue() doesn't exist)

**GREEN: Add overdue check**

```java
public boolean isOverdue(LocalDate today) {
    return today.isAfter(dueDate);
}
```

**Run**: ✅ ALL PASS

**REFACTOR**: Method is simple and clear. No refactoring needed.

### Iteration 4: Calculate Days Overdue

**RED: Write failing test**

```java
@Test
void shouldCalculateDaysOverdue() {
    LocalDate loanDate = LocalDate.of(2024, 1, 1);
    Loan loan = Loan.create(BookId.generate(), "Alice", loanDate);

    LocalDate today = LocalDate.of(2024, 1, 20);  // 5 days overdue

    assertThat(loan.daysOverdue(today)).isEqualTo(5);
}

@Test
void shouldReturnZeroWhenNotOverdue() {
    LocalDate loanDate = LocalDate.of(2024, 1, 1);
    Loan loan = Loan.create(BookId.generate(), "Alice", loanDate);

    LocalDate today = LocalDate.of(2024, 1, 10);  // Before due date

    assertThat(loan.daysOverdue(today)).isEqualTo(0);
}
```

**Run**: ❌ FAILS (daysOverdue() doesn't exist)

**GREEN: Add calculation**

```java
public long daysOverdue(LocalDate today) {
    if (!isOverdue(today)) {
        return 0;
    }
    return ChronoUnit.DAYS.between(dueDate, today);
}
```

**Run**: ✅ ALL PASS

**REFACTOR**: Code is clean.

### Iteration 5: Return the Loan

**RED: Write failing test**

```java
@Test
void shouldMarkLoanAsReturned() {
    Loan loan = Loan.create(BookId.generate(), "Alice", LocalDate.now());

    loan.returnBook(LocalDate.now().plusDays(5));

    assertThat(loan.isReturned()).isTrue();
}

@Test
void shouldRecordReturnDate() {
    Loan loan = Loan.create(BookId.generate(), "Alice", LocalDate.of(2024, 1, 1));
    LocalDate returnDate = LocalDate.of(2024, 1, 10);

    loan.returnBook(returnDate);

    assertThat(loan.getReturnDate()).isEqualTo(returnDate);
}

@Test
void shouldThrowWhenReturningAlreadyReturnedLoan() {
    Loan loan = Loan.create(BookId.generate(), "Alice", LocalDate.now());
    loan.returnBook(LocalDate.now());

    assertThatThrownBy(() -> loan.returnBook(LocalDate.now()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already returned");
}
```

**Run**: ❌ FAILS (methods don't exist)

**GREEN: Add return functionality**

```java
public class Loan {
    // ... existing fields ...
    private LocalDate returnDate;

    public void returnBook(LocalDate returnDate) {
        if (isReturned()) {
            throw new IllegalStateException("Loan already returned");
        }
        this.returnDate = Objects.requireNonNull(returnDate, "Return date required");
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }
}
```

**Run**: ✅ ALL PASS

**REFACTOR**: Code looks good.

### Iteration 6: Validate Inputs

**RED: Write failing tests for edge cases**

```java
@Test
void shouldThrowWhenBookIdIsNull() {
    assertThatThrownBy(() ->
        Loan.create(null, "Alice", LocalDate.now())
    ).isInstanceOf(NullPointerException.class);
}

@Test
void shouldThrowWhenBorrowerIsNull() {
    assertThatThrownBy(() ->
        Loan.create(BookId.generate(), null, LocalDate.now())
    ).isInstanceOf(NullPointerException.class);
}

@Test
void shouldThrowWhenBorrowerIsBlank() {
    assertThatThrownBy(() ->
        Loan.create(BookId.generate(), "  ", LocalDate.now())
    ).isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("Borrower cannot be blank");
}

@Test
void shouldThrowWhenLoanDateIsNull() {
    assertThatThrownBy(() ->
        Loan.create(BookId.generate(), "Alice", null)
    ).isInstanceOf(NullPointerException.class);
}
```

**Run**: ❌ FAILS (no validation)

**GREEN: Add validation**

```java
private Loan(BookId bookId, String borrower, LocalDate loanDate) {
    this.bookId = Objects.requireNonNull(bookId, "Book ID required");
    this.borrower = validateBorrower(borrower);
    this.loanDate = Objects.requireNonNull(loanDate, "Loan date required");
    this.dueDate = loanDate.plusDays(LOAN_PERIOD_DAYS);
}

private String validateBorrower(String borrower) {
    Objects.requireNonNull(borrower, "Borrower cannot be null");
    if (borrower.isBlank()) {
        throw new IllegalArgumentException("Borrower cannot be blank");
    }
    return borrower.trim();
}
```

**Run**: ✅ ALL PASS

**REFACTOR**: Extract Borrower value object

```java
public record Borrower(String name) {
    public Borrower {
        Objects.requireNonNull(name, "Borrower name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Borrower name cannot be blank");
        }
        name = name.trim();
    }

    public static Borrower of(String name) {
        return new Borrower(name);
    }
}

// Update Loan
public class Loan {
    private Borrower borrower;  // Changed from String

    private Loan(BookId bookId, Borrower borrower, LocalDate loanDate) {
        this.bookId = Objects.requireNonNull(bookId);
        this.borrower = Objects.requireNonNull(borrower);
        this.loanDate = Objects.requireNonNull(loanDate);
        this.dueDate = loanDate.plusDays(LOAN_PERIOD_DAYS);
    }

    public static Loan create(BookId bookId, String borrowerName, LocalDate loanDate) {
        return new Loan(bookId, Borrower.of(borrowerName), loanDate);
    }

    public String getBorrower() {
        return borrower.name();
    }
}
```

**Run**: ✅ ALL STILL PASS (tests didn't need to change!)

**Result**: We built a complete `Loan` domain model with **tests first**, adding features incrementally.

---

## Part 3: Benefits of TDD

### 1. Better Design

**TDD forces you to think about the API first.**

When you write the test first:
```java
@Test
void test() {
    Loan loan = Loan.create(bookId, "Alice", loanDate);  // How you WANT to use it
    assertThat(loan.isOverdue(today)).isFalse();
}
```

You design the **client perspective** first, leading to better APIs.

### 2. High Test Coverage

**By definition, every line of production code has a test.**

You can't write production code without a failing test first.

**Result**: 95%+ coverage naturally.

### 3. Immediate Feedback

**Tests run every 30 seconds.**

In TDD:
- Write test (30 sec)
- Run test, see it fail (5 sec)
- Write code (1 min)
- Run test, see it pass (5 sec)
- Refactor (1 min)
- Run tests again (5 sec)

**Total cycle**: 3-4 minutes per iteration.

Traditional:
- Write code for 2 hours
- Write tests
- Find bug
- Debug for 1 hour

**TDD catches bugs in minutes, not hours.**

### 4. Refactoring Confidence

**Tests are a safety net.**

```java
// Refactor from if-else to polymorphism
// Before:
if (loan.isOverdue(today)) {
    // ...
}

// After:
loan.status().handleOverdue();  // Refactored!

// Run tests: ✅ Still pass! Refactoring safe!
```

### 5. Living Documentation

**Tests document how code works.**

```java
@Test
void shouldCalculateDaysOverdue() {
    LocalDate loanDate = LocalDate.of(2024, 1, 1);
    Loan loan = Loan.create(BookId.generate(), "Alice", loanDate);
    LocalDate today = LocalDate.of(2024, 1, 20);

    assertThat(loan.daysOverdue(today)).isEqualTo(5);
}
```

**This test documents**: "Loan due date is 14 days after loan date. Days overdue = days past due date."

### 6. Less Debugging

**When a test fails, you know exactly what broke.**

New test fails? The code you just wrote is the problem.

Existing test fails? The code you just changed broke something.

**Debugging time drops dramatically.**

---

## Part 4: When to Use TDD

### Use TDD When:

✅ **Building new features from scratch**
- Clean slate, no existing code
- Example: Loan management (above)

✅ **Complex business logic**
- Calculations, validations, state machines
- Example: Late fee calculation with tiers

✅ **Bug fixing**
- Write test that reproduces bug
- Fix bug until test passes
- Now bug can't reoccur

✅ **Learning a new technology**
- Write tests to understand API
- Safe experimentation

### Don't Use TDD When:

❌ **Prototyping / Spike solutions**
- Exploring unknowns
- Code will be thrown away

❌ **UI work**
- Visual design, layout
- Better to see it running

❌ **Simple CRUD**
- Spring Data repositories
- Already tested by framework

❌ **Under tight deadlines** (controversial!)
- TDD seems slower initially
- But prevents debugging later
- Your call

### Hybrid Approach

**Most projects use a mix**:
- TDD for core domain logic
- Test-after for infrastructure code
- Integration tests for database
- Manual testing for UI

---

## Part 5: Common TDD Pitfalls

### Pitfall 1: Writing Too Much Test at Once

**Bad**:
```java
@Test
void shouldCreateLoanAndCalculateDueDateAndCheckOverdueAndReturnAndCalculateFee() {
    // 50 lines of test setup
    // Testing 5 different things at once!
}
```

**Good**:
```java
@Test
void shouldCreateLoan() { /* ... */ }

@Test
void shouldCalculateDueDate() { /* ... */ }

@Test
void shouldCheckOverdue() { /* ... */ }
```

**Rule**: One test, one concept.

### Pitfall 2: Writing Too Much Production Code

**Bad**: Implement entire feature before running test.

**Good**: Write **just enough** to pass current test.

**Example**:
```java
// Test says: shouldReturnTrue
// Don't implement: full return logic
// Do implement: return true;
```

Then write more tests to force proper implementation.

### Pitfall 3: Not Refactoring

**TDD is Red-Green-**REFACTOR**, not Red-Green.**

After tests pass, **clean up the code**:
- Extract methods
- Remove duplication
- Improve names

**Then run tests** to ensure nothing broke.

### Pitfall 4: Testing Implementation Details

**Bad**:
```java
@Test
void shouldCallRepositorySaveMethod() {
    verify(mockRepo).save(any());  // Testing HOW, not WHAT
}
```

**Good**:
```java
@Test
void shouldPersistLoan() {
    service.createLoan(command);

    Loan saved = repository.findById(loanId).orElseThrow();
    assertThat(saved.getBorrower()).isEqualTo("Alice");  // Testing WHAT
}
```

**Test behavior, not implementation.**

### Pitfall 5: Skipping the Red

**Always see the test FAIL first.**

Why? To ensure the test actually tests something.

**Story**: Developer writes test, it passes immediately. "Great!"

**Problem**: Test had a typo - wasn't actually asserting anything!

**Solution**: See it fail (RED), then make it pass (GREEN).

---

## Part 6: TDD in Bibby - Complete Example

Let's add a **complete feature** to Bibby using TDD: **Book Reservations**.

**Feature**: Users can reserve an available book. When a checked-out book is returned, reserved user gets notified.

### Step-by-Step TDD

**Iteration 1: Create Reservation**

```java
// RED
@Test
void shouldCreateReservationForAvailableBook() {
    BookId bookId = BookId.generate();
    UserId userId = UserId.of("alice");

    Reservation reservation = Reservation.create(bookId, userId);

    assertThat(reservation.getBookId()).isEqualTo(bookId);
    assertThat(reservation.getUserId()).isEqualTo(userId);
    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
}

// GREEN
public class Reservation {
    private BookId bookId;
    private UserId userId;
    private ReservationStatus status;

    private Reservation(BookId bookId, UserId userId) {
        this.bookId = bookId;
        this.userId = userId;
        this.status = ReservationStatus.ACTIVE;
    }

    public static Reservation create(BookId bookId, UserId userId) {
        return new Reservation(bookId, userId);
    }

    // Getters
}

// ✅ PASS
```

**Iteration 2: Cancel Reservation**

```java
// RED
@Test
void shouldCancelActiveReservation() {
    Reservation reservation = Reservation.create(BookId.generate(), UserId.of("alice"));

    reservation.cancel();

    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
}

// GREEN
public void cancel() {
    this.status = ReservationStatus.CANCELLED;
}

// ✅ PASS
```

**Iteration 3: Fulfill Reservation**

```java
// RED
@Test
void shouldFulfillReservationWhenBookAvailable() {
    Reservation reservation = Reservation.create(BookId.generate(), UserId.of("alice"));

    reservation.fulfill();

    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
}

@Test
void shouldRecordFulfillmentDate() {
    Reservation reservation = Reservation.create(BookId.generate(), UserId.of("alice"));
    LocalDateTime before = LocalDateTime.now();

    reservation.fulfill();

    LocalDateTime after = LocalDateTime.now();
    assertThat(reservation.getFulfilledAt()).isBetween(before, after);
}

// GREEN
private LocalDateTime fulfilledAt;

public void fulfill() {
    this.status = ReservationStatus.FULFILLED;
    this.fulfilledAt = LocalDateTime.now();
}

public LocalDateTime getFulfilledAt() {
    return fulfilledAt;
}

// ✅ PASS
```

**Continue with TDD** for edge cases, validation, etc.

---

## Action Items: Practicing TDD with Bibby

### Action Item 1: Build Loan Management with TDD (3-4 hours)

**Task**: Implement the Loan feature shown in Part 2 using strict TDD.

**Steps**:
1. Start with empty `LoanTest.java`
2. Write first failing test
3. Make it pass with minimal code
4. Refactor
5. Repeat for all Loan features

**Features to implement**:
- ✅ Create loan
- ✅ Calculate due date
- ✅ Check if overdue
- ✅ Calculate days overdue
- ✅ Return book
- ✅ Validation

### Action Item 2: Add Reservation Feature with TDD (2-3 hours)

**Task**: Build book reservation system using TDD.

**Features**:
- Create reservation
- Cancel reservation
- Fulfill reservation
- Expire old reservations
- Limit reservations per user

**Follow strict TDD**: Red → Green → Refactor

### Action Item 3: Fix a Bug with TDD (30 min)

**Task**: Find a bug in Bibby, use TDD to fix it.

**Process**:
1. Write test that reproduces bug (fails)
2. Fix bug
3. Test passes
4. Refactor if needed

### Action Item 4: Measure TDD Impact (ongoing)

**Track**:
- Time to implement feature with TDD
- Number of bugs found in testing
- Refactoring confidence (subjective)
- Test coverage achieved

**Compare** with test-after approach.

### Action Item 5: Practice Kata with TDD (1 hour)

**Code Kata**: Small practice exercise.

**Classic katas**:
- Roman numeral converter
- FizzBuzz
- String calculator
- Bowling game

**Practice TDD** in small, focused sessions.

---

## Key Takeaways

### 1. Red, Green, Refactor

The TDD cycle is:
1. **Red**: Write failing test
2. **Green**: Make it pass (minimal code)
3. **Refactor**: Clean up
4. **Repeat**: Next test

### 2. Tests Drive Design

Writing tests first forces you to think about the API from the client perspective, leading to better design.

### 3. Small Steps

Don't implement entire feature at once. One test, one small piece of functionality.

### 4. Refactor with Confidence

Tests are your safety net. Refactor freely, knowing tests will catch breakage.

### 5. When to Use TDD

Use for:
- New features
- Complex logic
- Bug fixes

Skip for:
- Prototypes
- Simple CRUD
- UI layout

---

## Study Resources

### Books
1. **"Test Driven Development: By Example"** by Kent Beck
   - **THE** TDD book
   - Step-by-step examples
   - Essential reading

2. **"Growing Object-Oriented Software, Guided by Tests"** by Freeman & Pryce
   - TDD for entire applications
   - Mocking strategies

3. **"Clean Code" by Robert C. Martin**
   - Chapter on TDD

### Videos
1. **"TDD: Where Did It All Go Wrong"** by Ian Cooper
   - Common TDD mistakes
   - How to do it right

2. **Kent Beck's TDD screencasts**
   - Watch the creator of TDD work

### Practice
1. **Coding Dojos** - Group TDD practice
2. **Cyber-Dojo** - Online kata platform
3. **Codewars** - TDD challenges

---

## Coming Next

**Section 28: Testing Best Practices & Patterns** - Patterns for maintainable tests

We'll cover:
- Test data builders
- Object mothers
- Test fixtures
- Parameterized test patterns
- Test organization strategies

---

**Section 27 Complete** | **Time Invested**: 4-5 hours

You now understand Test-Driven Development - writing tests first to drive design. TDD isn't just about testing; it's a design methodology that leads to better code, better tests, and fewer bugs.

Practice TDD on small features first. It feels awkward initially, but once you experience the flow of Red-Green-Refactor, you'll find it hard to code any other way.