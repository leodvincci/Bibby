# Section 6: OOP - Inheritance & Polymorphism

Welcome to Section 6! We're covering the remaining two pillars of Object-Oriented Programming: **Inheritance** and **Polymorphism**. These are powerful tools that, when used correctly, enable code reuse and flexibility. When misused, they create rigid, fragile code.

Your Bibby application uses inheritance primarily through **interfaces**, which is generally the right approach. Let's explore why!

---

## What Is Inheritance?

**Inheritance** is a mechanism where one class acquires the properties and behaviors of another class.

**Types of Inheritance in Java:**
1. **Class Inheritance** - `class Child extends Parent`
2. **Interface Implementation** - `class Impl implements Interface`
3. **Interface Extension** - `interface Child extends Parent`

**The "IS-A" Relationship:**
- A Dog **is-a** Animal (class inheritance)
- A BookRepository **is-a** JpaRepository (interface extension)
- A BookCommands **is-a** AbstractShellComponent (class inheritance)

**Java's Restriction:**
- **Single inheritance** for classes (can only extend one class)
- **Multiple inheritance** for interfaces (can implement many interfaces)

---

## What Is Polymorphism?

**Polymorphism** means "many forms" - the ability of different objects to respond to the same message in different ways.

**Two Types:**

1. **Compile-Time (Static) Polymorphism** - Method overloading
```java
public BookEntity findBook(Long id) { ... }
public BookEntity findBook(String title) { ... }
// Same method name, different parameters - compiler chooses
```

2. **Runtime (Dynamic) Polymorphism** - Method overriding
```java
interface Repository {
    void save(Object obj);
}

class BookRepository implements Repository {
    @Override
    public void save(Object obj) {
        // BookRepository's implementation
    }
}

Repository repo = new BookRepository();
repo.save(book); // Calls BookRepository's save at runtime
```

---

## In Your Code: Inheritance Examples

### ✅ Interface Implementation (Best Practice)

📁 File: `src/main/java/com/penrose/bibby/util/StartupRunner.java`
📍 Lines: 7-17

```java
@Component
public class StartupRunner implements CommandLineRunner {
    @Autowired
    ShelfService shelfService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> Bibby is awake. Seeding data now...");
        // TODO: Add data seeding logic here if needed
    }
}
```

**Why This Is Excellent:**

1. **Interface Contract** - `CommandLineRunner` defines: "You must have a `run(String... args)` method"
2. **@Override Annotation** - Compiler verifies you're actually overriding (catches typos!)
3. **Spring Integration** - Spring automatically calls `run()` at startup
4. **Polymorphism** - Spring treats all CommandLineRunner implementations the same

**How It Works:**

```java
// Inside Spring Framework (simplified):
List<CommandLineRunner> runners = findAll(); // Finds all implementations

for (CommandLineRunner runner : runners) {
    runner.run(args); // Polymorphism! Calls the right implementation
}
```

Your `StartupRunner` is one of potentially many CommandLineRunner implementations. Spring doesn't need to know about your specific class - it just calls the interface method!

---

### ✅ Interface Implementation with @Override

📁 File: `src/main/java/com/penrose/bibby/cli/CustomPromptProvider.java`
📍 Lines: 8-19

```java
@Component
public class CustomPromptProvider implements PromptProvider {

    @Override
    public AttributedString getPrompt(){
        return new AttributedString(
            "Guest </>\uD835\uDC01\uD835\uDC08\uD835\uDC01\uD835\uDC01\uD835\uDC18:_ ",
            AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold()
        );
    }
}
```

**Why @Override Matters:**

```java
// WITHOUT @Override (dangerous):
public AttributedString getprompt() { // ← Typo! lowercase 'p'
    // This creates a NEW method, doesn't override!
    // Spring Shell won't call it - uses default prompt instead
    // No compiler error!
}

// WITH @Override (safe):
@Override
public AttributedString getprompt() { // ← Typo!
    // Compiler error: "Method does not override method from its superclass"
    // You catch the error immediately!
}
```

**The Rule:** **ALWAYS** use `@Override` when overriding methods. It's not optional - it's a safety net!

---

### ✅ Class Inheritance from Framework

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 24-26

```java
@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {
```

📁 File: `src/main/java/com/penrose/bibby/cli/BookcaseCommands.java`
📍 Lines: 20-22

```java
@Component
@Command(command = "bookcase", group = "Bookcase Commands")
public class BookcaseCommands extends AbstractShellComponent {
```

**Why This Works:**

`AbstractShellComponent` is a Spring Shell framework class that provides:
- Terminal utilities
- Component flow builders
- Shell context access

**Benefits:**
- ✅ Code reuse (don't reimplement terminal utilities)
- ✅ Framework integration (Spring Shell expects this)
- ✅ Common behavior (all commands get same capabilities)

**When to Extend Classes:**
- Framework base classes (like AbstractShellComponent)
- You control both parent and child
- Clear "IS-A" relationship
- Parent provides useful default behavior

---

### ✅ Interface Extension (Repository Pattern)

📁 File: `src/main/java/com/penrose/bibby/library/book/BookRepository.java`
📍 Lines: 11-12

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {
```

**This Is Interface Extension:**

```java
interface JpaRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    // ... many more methods
}

interface BookRepository extends JpaRepository<BookEntity, Long> {
    // Inherits all JpaRepository methods!
    // Plus custom methods:
    BookEntity findByTitle(String title);
    List<BookEntity> findByTitleContaining(String title);
}
```

**Polymorphism in Action:**

```java
// Service can use JpaRepository interface:
private final JpaRepository<BookEntity, Long> repository; // Generic interface

// Spring injects BookRepository (specific implementation):
public BookService(BookRepository bookRepository) {
    this.repository = bookRepository; // Polymorphism!
}

// Works with any JpaRepository<BookEntity, Long>:
repository.save(book); // Calls BookRepository's save
```

**Benefits:**
- ✅ Inherits ~20 methods from JpaRepository
- ✅ Add custom query methods
- ✅ Spring Data generates implementation at runtime
- ✅ Easy to test (mock the interface)

---

## Composition vs Inheritance

**The Principle:** "Favor composition over inheritance"

### What's Composition?

**Composition** = "HAS-A" relationship instead of "IS-A"

**Example in Your Code:**

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 14-20

```java
@Service
public class BookService {
    final BookRepository bookRepository;  // HAS-A repository
    private final AuthorRepository authorRepository;  // HAS-A repository

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository){
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }
}
```

**BookService HAS-A BookRepository, not IS-A BookRepository!**

This is **composition** - the service is composed of repositories.

---

### When to Use Inheritance vs Composition

| Use Inheritance When... | Use Composition When... |
|------------------------|-------------------------|
| Clear "IS-A" relationship | "HAS-A" or "USES-A" relationship |
| Child truly specializes parent | Need multiple behaviors |
| Parent designed for extension | Combining different capabilities |
| Framework requires it | More flexibility needed |

**Examples:**

```java
// GOOD INHERITANCE: BookCommands IS-A AbstractShellComponent
public class BookCommands extends AbstractShellComponent {
    // Makes sense - a book command is a type of shell component
}

// GOOD COMPOSITION: BookService HAS-A BookRepository
public class BookService {
    private final BookRepository repository; // Makes sense - service uses repository
}

// BAD INHERITANCE (if you did this):
public class BookService extends BookRepository {
    // BookService IS-A BookRepository? No! Doesn't make sense
    // Should use composition instead
}
```

---

### ⚠️ The Fragile Base Class Problem

**Danger of Class Inheritance:**

If you extend a class, you're tightly coupled to its implementation:

```java
// Parent class (version 1)
public class BaseService {
    public void save(Object obj) {
        validate(obj);
        persist(obj);
    }

    protected void validate(Object obj) {
        // Basic validation
    }
}

// Your class
public class BookService extends BaseService {
    @Override
    protected void validate(Object obj) {
        // Custom validation
        super.validate(obj); // Calls parent's validate
    }
}

// Parent class (version 2 - updated)
public class BaseService {
    public void save(Object obj) {
        // Changed! Now calls validateAndLog instead of validate
        validateAndLog(obj);
        persist(obj);
    }

    protected void validate(Object obj) {
        // This is no longer called!
    }

    protected void validateAndLog(Object obj) {
        validate(obj);
        log(obj);
    }
}

// Your class is now BROKEN!
// You override validate(), but it's no longer called by save()
// This is the "fragile base class" problem
```

**Solution:** Favor composition or interfaces!

---

## Method Overriding Rules

### ✅ @Override Annotation

**ALWAYS use it:**

```java
@Component
public class CustomPromptProvider implements PromptProvider {

    @Override  // ← REQUIRED!
    public AttributedString getPrompt() {
        return new AttributedString("...");
    }
}
```

**What @Override Does:**
1. Compiler verifies method actually overrides something
2. Catches typos in method names
3. Catches signature mismatches
4. Documents intent (this is an override, not a new method)

---

### Overriding Rules

**Access Modifiers:**
- Cannot reduce visibility
- Can increase visibility

```java
interface PromptProvider {
    AttributedString getPrompt(); // Implicitly public
}

class CustomPromptProvider implements PromptProvider {
    @Override
    public AttributedString getPrompt() { ... } // ✅ Must be public

    // @Override
    // protected AttributedString getPrompt() { ... } // ❌ Error! Can't reduce visibility
}
```

**Return Types:**
- Must be same type or **covariant** (subtype)

```java
class Parent {
    public Object getValue() { ... }
}

class Child extends Parent {
    @Override
    public String getValue() { ... } // ✅ String is subtype of Object (covariant return)
}
```

**Exceptions:**
- Can throw fewer exceptions
- Cannot throw more checked exceptions

```java
interface Runner {
    void run() throws IOException;
}

class StartupRunner implements Runner {
    @Override
    public void run() { ... } // ✅ Throws no exceptions (fewer than IOException)

    // @Override
    // public void run() throws Exception { ... } // ❌ Error! Exception is broader than IOException
}
```

---

## Polymorphism in Action

### Runtime Polymorphism Example

Your repositories demonstrate polymorphism:

```java
// Service depends on interface:
@Service
public class BookService {
    private final BookRepository bookRepository;

    public void saveBook(BookEntity book) {
        bookRepository.save(book); // Which save()? Determined at runtime!
    }
}

// At runtime, Spring injects actual implementation:
// bookRepository might be:
// - JPA implementation (default)
// - Mock implementation (in tests)
// - Cached implementation (with caching layer)
// - Remote implementation (distributed database)

// Your code doesn't care! Polymorphism!
```

**How JVM Decides Which Method to Call:**

1. **Static Binding (Compile-Time):**
   - private methods
   - final methods
   - static methods
   - Constructor calls

2. **Dynamic Binding (Runtime):**
   - Instance methods (virtual methods)
   - Interface methods
   - Uses vtable (virtual method table)

```java
Repository repo = new BookRepository(); // Compile-time: Type is Repository
                                        // Runtime: Object is BookRepository
repo.save(book); // Runtime looks up BookRepository's save() in vtable
```

---

## Common Inheritance Patterns in Spring

### 1. Interface Implementation (Most Common)

```java
@Service
public class BookService {
    // Implements business logic
    // No inheritance needed!
}

@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    // Interface extension, not class inheritance
}
```

### 2. Framework Base Classes

```java
@Component
public class BookCommands extends AbstractShellComponent {
    // Framework provides base functionality
    // You add specific commands
}
```

### 3. Marker Interfaces

```java
// Spring's own patterns:
public interface Serializable { } // No methods - just a marker

// Your entities could implement:
public class BookEntity implements Serializable {
    // Now BookEntity can be serialized
}
```

---

## When NOT to Use Inheritance

### ❌ Anti-Pattern: Inheritance for Code Reuse Only

```java
// BAD: Inheritance just to reuse utility methods
public class BookService extends StringUtils {
    // BookService IS-A StringUtils? No!
    // Should use composition or static imports instead
}

// GOOD: Composition
public class BookService {
    private final StringUtils stringUtils; // HAS-A StringUtils
}

// OR: Static imports
import static com.util.StringUtils.*;

public class BookService {
    public void process(String s) {
        String clean = trim(s); // Static method
    }
}
```

### ❌ Anti-Pattern: Deep Inheritance Hierarchies

```java
// BAD: Too many levels
class Entity { }
class DomainEntity extends Entity { }
class PersistentEntity extends DomainEntity { }
class BookEntity extends PersistentEntity { } // 4 levels deep!

// GOOD: Flat with interfaces
interface Identifiable { Long getId(); }
interface Timestamped { LocalDate getCreatedAt(); }

class BookEntity implements Identifiable, Timestamped {
    // Implement multiple interfaces instead of deep hierarchy
}
```

**Why Deep Hierarchies Are Bad:**
- Hard to understand (must read entire chain)
- Fragile (changes ripple through hierarchy)
- Inflexible (can't easily change parent)
- Multiple inheritance of behavior is impossible in Java

---

## Abstract Classes vs Interfaces

**When to Use Each:**

| Abstract Class | Interface |
|----------------|-----------|
| Has state (fields) | No state (constants only) |
| Mix of concrete and abstract methods | All methods abstract (pre-Java 8) |
| Single inheritance | Multiple inheritance |
| "IS-A" relationship | "CAN-DO" capability |

**Modern Java (8+):**

Interfaces can now have:
- Default methods (concrete implementation)
- Static methods
- Private methods (Java 9+)

This makes interfaces more powerful and reduces need for abstract classes!

**Example Decision:**

```java
// Abstract class - has shared state and behavior
public abstract class BaseEntity {
    protected Long id; // Shared state
    protected LocalDate createdAt;

    public Long getId() { return id; } // Concrete method

    public abstract void validate(); // Abstract method
}

// Interface - defines capabilities
public interface Auditable {
    LocalDate getCreatedAt();
    LocalDate getUpdatedAt();
    String getCreatedBy();
}

// Entity can extend one abstract class, implement many interfaces:
public class BookEntity extends BaseEntity implements Auditable, Serializable {
    // Best of both worlds!
}
```

---

## Your Code: Analysis and Recommendations

### ✅ What You're Doing Right

1. **Interface-First Design** - All repositories are interfaces ✅
2. **@Override Annotations** - Present in StartupRunner and CustomPromptProvider ✅
3. **Composition in Services** - Services compose repositories ✅
4. **Framework Base Classes** - Extending AbstractShellComponent makes sense ✅

### ⚠️ Opportunities for Improvement

**1. Missing @Override in Entity toString/equals**

📁 File: `src/main/java/com/penrose/bibby/library/book/Book.java`
📍 Lines: 149-178

```java
@Override
public String toString() { ... } // ✅ Has @Override

@Override
public boolean equals(Object o) { ... } // ✅ Has @Override

@Override
public int hashCode() { ... } // ✅ Has @Override
```

**Actually, you DO have @Override here! Great job!**

**2. Could Use Interface for Common Entity Behavior**

Your entities could implement common interfaces:

```java
// Define capability interfaces
public interface Identifiable<ID> {
    ID getId();
    void setId(ID id);
}

public interface Timestamped {
    LocalDate getCreatedAt();
    LocalDate getUpdatedAt();
}

// Entities implement
@Entity
public class BookEntity implements Identifiable<Long>, Timestamped {
    // Now services can work with any Identifiable entity
}

// Service can be generic:
public <T extends Identifiable<Long>> void logEntity(T entity) {
    System.out.println("Entity ID: " + entity.getId());
}
```

---

## Key Takeaways

**Inheritance:**
1. **Single class inheritance** - Can extend only one class
2. **Multiple interface inheritance** - Can implement many interfaces
3. **IS-A relationship** - Child IS-A Parent
4. **Favor composition over inheritance** - HAS-A is often better
5. **Always use @Override** - Compiler catches errors
6. **Fragile base class problem** - Inheritance creates tight coupling

**Polymorphism:**
1. **Compile-time** - Method overloading (same name, different parameters)
2. **Runtime** - Method overriding (same signature, different implementation)
3. **Program to interfaces** - Use interface types, not concrete classes
4. **Dynamic binding** - JVM chooses method at runtime based on actual object type

**Composition vs Inheritance:**
```
Prefer THIS:          Over THIS:
HAS-A (composition)   IS-A (inheritance)
Interface             Abstract class
Multiple interfaces   Deep hierarchies
Dependency injection  Extending for reuse
```

**Spring Patterns:**
- Interface-based repositories (JpaRepository)
- Composition in services (inject dependencies)
- Framework base classes when needed (AbstractShellComponent)
- Marker interfaces (Serializable)

---

## Practice Exercise: Create Auditable Interface

**Your Task:**

Create an `Auditable` interface that entities can implement to track creation/update metadata.

**Part 1: Define the Interface**

Create: `src/main/java/com/penrose/bibby/library/common/Auditable.java`

```java
package com.penrose.bibby.library.common;

import java.time.LocalDate;

public interface Auditable {
    LocalDate getCreatedAt();
    void setCreatedAt(LocalDate createdAt);

    LocalDate getUpdatedAt();
    void setUpdatedAt(LocalDate updatedAt);

    // Default method (Java 8+)
    default boolean isNew() {
        return getCreatedAt() == null;
    }

    default void markCreated() {
        setCreatedAt(LocalDate.now());
    }

    default void markUpdated() {
        setUpdatedAt(LocalDate.now());
    }
}
```

**Part 2: Implement in BookEntity**

```java
@Entity
@Table(name = "books")
public class BookEntity implements Auditable {
    // ... existing fields ...

    private LocalDate createdAt;
    private LocalDate updatedAt;

    // Implement interface methods
    @Override
    public LocalDate getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

**Part 3: Use Polymorphism**

```java
public class AuditLogger {
    public void logCreation(Auditable entity) {
        // Works with ANY Auditable entity!
        System.out.println("Entity created at: " + entity.getCreatedAt());
    }
}

// Usage:
BookEntity book = new BookEntity();
book.markCreated(); // From interface default method
auditLogger.logCreation(book); // Polymorphism!
```

**Benefits:**
- ✅ Reusable across all entities
- ✅ DRY (Don't Repeat Yourself)
- ✅ Type-safe (compile-time checking)
- ✅ Polymorphic (works with any Auditable)

---

## Action Items for This Week

**1. Add @Override to All Overriding Methods**
**Priority:** MEDIUM
**Estimated Time:** 15 minutes

Search for methods that override but lack @Override:
```bash
grep -A 3 "public.*toString\|public.*equals\|public.*hashCode" src/**/*.java
```

Verify all have `@Override` annotation above them.

**2. Create Identifiable Interface**
**Priority:** LOW
**Estimated Time:** 30 minutes

Create: `src/main/java/com/penrose/bibby/library/common/Identifiable.java`

```java
public interface Identifiable<ID> {
    ID getId();
    void setId(ID id);
}
```

Have all entities implement it:
```java
@Entity
public class BookEntity implements Identifiable<Long> {
    // Already has getId/setId!
}
```

**3. Review Inheritance vs Composition**
**Priority:** LOW
**Estimated Time:** 20 minutes

Audit your codebase:
- List all `extends` relationships - are they justified?
- List all `implements` relationships - correct usage?
- Could any inheritance be replaced with composition?

Create a document with findings.

---

## Further Study

**Books:**
- *Effective Java* (3rd Ed) by Joshua Bloch - Item 18: "Favor composition over inheritance"
- *Effective Java* - Item 19: "Design and document for inheritance or else prohibit it"
- *Effective Java* - Item 20: "Prefer interfaces to abstract classes"
- *Head First Design Patterns* - Strategy Pattern (composition over inheritance)

**Articles:**
- Martin Fowler: "Replace Inheritance with Delegation"
- Baeldung: "Composition vs Inheritance in Java" - https://www.baeldung.com/java-composition-vs-inheritance
- Oracle Java Tutorials: "Inheritance" and "Polymorphism"

**Principles:**
- Liskov Substitution Principle (LSP) - SOLID principles
- Favor Composition Over Inheritance
- Program to Interfaces, Not Implementations

---

## Summary

You've learned:
- ✅ Inheritance creates IS-A relationships (child IS-A parent)
- ✅ Polymorphism allows different implementations of same interface
- ✅ Java has single class inheritance, multiple interface inheritance
- ✅ @Override annotation is mandatory (catches errors)
- ✅ Composition (HAS-A) is often better than inheritance (IS-A)
- ✅ Interface-first design is Spring best practice
- ✅ Fragile base class problem makes deep hierarchies dangerous
- ✅ Dynamic binding happens at runtime (polymorphism)

**Your code showed:**
- **Strengths:** Interface-based repositories, @Override usage, composition in services, sensible framework inheritance
- **Opportunities:** Could add common interfaces (Auditable, Identifiable) for shared behavior

**Next Up:** Section 7 - Constructors & Object Lifecycle (examining object creation, initialization, builder patterns, and immutability in your entities)

---

*Section created: 2025-11-17*
*Files analyzed: BookCommands, StartupRunner, CustomPromptProvider, BookRepository, BookService*
*Inheritance patterns identified: 6*
*Polymorphism examples: 4*
