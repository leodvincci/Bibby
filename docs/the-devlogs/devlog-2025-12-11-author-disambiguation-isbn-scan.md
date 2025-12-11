# Devlog: Author Disambiguation & ISBN Scan Workflow

**Date:** December 11, 2025  
**Diff Context:** Author name collision handling, ISBN scan implementation, command refactoring

---

## 1. High-Level Summary

- **Implemented author disambiguation workflow** that detects when multiple authors share the same name and prompts users to select an existing author or create a new one
- **Completed ISBN scan functionality** by wiring up `findBookMetaDataByIsbn()` and `createBookFromMetaData()` through BookFacadeAdapter to the Google Books API
- **Refactored command handling** with a `ScanMode` enum replacing string-based conditionals for cleaner control flow
- **Simplified AuthorFacadeImpl** by removing the AuthorService dependency and routing directly through AuthorRepository
- **Expanded facade contracts** with new methods: `authorExistFirstNameLastName()`, `getAllAuthorsByName()`, `getBooksByAuthorId()`
- **Enhanced CLI UX** by displaying author's existing books during disambiguation to help users make informed choices

---

## 2. The Underlying Problem or Friction

This diff addresses a real-world domain complexity: **author name collisions**.

**The Scenario:** The screenshot from earlier showed three "Mike Jones" entries—which were *intentionally* different people. But the system had no way for users to indicate "this is a different Mike Jones" vs "this is the same Mike Jones who wrote my other books."

**Evidence of the fix:**

```java
// BookCommands.java - createAuthors()
if(authorFacade.authorExistFirstNameLastName(authorDTO.firstName(),authorDTO.lastName())){
    log.info("Author already exists: {} {}", authorDTO.firstName(), authorDTO.lastName());
    System.out.println("Multiple Authors with this name.\n");
    Long authorId = cliPrompt.promptMultipleAuthorConfirmation(authorDTO);
    
    if(authorId == 0){
        // User explicitly chose "Create New Author"
        authors.add(authorFacade.saveAuthor(authorDTO));
    }else{
        // User selected an existing author
        AuthorDTO existingAuthor = authorFacade.findById(authorId);
        authors.add(existingAuthor);
    }
}
```

**Additional friction points addressed:**

- ISBN scan workflow was stubbed out (returning `null`) — now fully implemented
- Command logic used messy string conditionals (`if(multi.equalsIgnoreCase("multi"))`)
- AuthorFacadeImpl had unnecessary indirection through AuthorService
- No way to see an author's books when deciding if it's the "right" Mike Jones

---

## 3. The Behavior Change

**User Experience - Before:**
```
Enter author name: Mike Jones
→ System blindly creates new author every time
→ No way to associate book with existing "Mike Jones"
→ Duplicate author records accumulate
```

**User Experience - After:**
```
Enter author name: Mike Jones
→ System detects existing author(s) with this name
→ Displays selection menu:

> [Create New Author]
  Mike Jones (ID: 1) : [Back Then, The Voice, Flossin']
  Mike Jones (ID: 2) : [Fast Fall in Love]
  Mike Jones (ID: 3) : [Call Me Back]

→ User can see each author's books to decide
→ Selection uses existing author's ID in the book-author relationship
→ OR creates genuinely new author if it's a different person
```

**ISBN Scan - Before:**
```java
@Override
public BookMetaDataResponse findBookMetaDataByIsbn(String isbn) {
    return null;  // Stubbed out!
}
```

**ISBN Scan - After:**
```java
@Override
public BookMetaDataResponse findBookMetaDataByIsbn(String isbn) {
    GoogleBooksResponse googleBooksResponse = isbnLookupService.lookupBook(isbn).block();
    log.info("Fetched book metadata for ISBN: {}", isbn);
    return bookMapper.toBookMetaDataResponseFromGoogleBooksResponse(googleBooksResponse, isbn);
}
```

---

## 4. The Architectural Meaning

### 1. Domain Logic Stays in the Right Place

The disambiguation decision happens at the **CLI layer** (user interaction), while the **facade layer** provides the data needed to make that decision:

```
CLI Layer (BookCommands)
    ↓ "Does author exist?"
Facade Layer (AuthorFacade)
    ↓ authorExistFirstNameLastName()
Repository Layer (AuthorRepository)
    ↓ JPA query
Database
```

The CLI doesn't reach into the repository directly — it asks the facade "give me what I need to show the user."

### 2. Facade Simplification

```java
// BEFORE: Facade → Service → Repository (unnecessary hop)
public class AuthorFacadeImpl implements AuthorFacade {
    final AuthorService authorService;
    
    public Set<AuthorDTO> findByBookId(Long id) {
        Set<Author> authors = authorService.findAuthorsByBookId(id);
        return AuthorMapper.toDTOSet(authors);
    }
}

// AFTER: Facade → Repository (direct, since no business logic needed)
public class AuthorFacadeImpl implements AuthorFacade {
    private final AuthorRepository authorRepository;
    
    public Set<AuthorDTO> findByBookId(Long id) {
        Set<Author> authors = authorRepository.findAuthorsByBookId(id);
        return AuthorMapper.toDTOSet(authors);
    }
}
```

When a facade method is pure coordination (fetch, map, return), routing through a service adds no value. The service layer is for **business logic**, not pass-through.

### 3. Contract Expansion for Real Use Cases

The `BookFacade` and `AuthorFacade` interfaces grew to support actual user workflows:

```java
// AuthorFacade - new methods
boolean authorExistFirstNameLastName(String firstName, String lastName);
List<AuthorDTO> getAllAuthorsByName(String firstName, String lastName);

// BookFacade - new methods  
List<String> getBooksByAuthorId(Long id);
```

These aren't speculative — they're driven by the UI need to display "Mike Jones (ID: 1) : [Back Then, The Voice]".

### 4. Clean Command Dispatch with Enum

```java
// BEFORE: String comparison soup
if (scan && multi) {
    multiBookScan();
} else if (scan) {
    scanBook("single");
} else {
    // manual registration
}

// AFTER: Explicit mode enum
ScanMode mode = ScanMode.from(scan, multi);
switch(mode){
    case SINGLE -> scanBook(false);
    case MULTI -> multiBookScan();
    case NONE -> createBookManually();
}
```

This pattern makes command behavior self-documenting and eliminates edge-case bugs from boolean combinations.

---

## 5. The Developer's Thought Process (Reconstructed)

**Phase 1: "The CLI showed duplicate Mike Jones entries — but wait, those were intentional"**

The earlier screenshot showed what looked like a bug (three Mike Jones records), but was actually correct domain behavior. This triggered the realization:

> "If two different people can have the same name, I need to let users *choose* whether they mean an existing author or a new one."

**Phase 2: "How do I show users which Mike Jones is which?"**

The key insight was displaying each author's books:

```java
// CliPromptService.java
options.add(SelectItem.of(
    a.firstName() + " " + a.lastName() + " (ID: " + a.id() + ")" + 
    " : " + bookFacade.getBooksByAuthorId(a.id()),
    String.valueOf(a.id())
));
```

> "If I show their books, users can say 'oh, Mike Jones who wrote Back Then — that's the rapper, not the other guy.'"

**Phase 3: "The ISBN scan was never actually wired up"**

Looking at `BookFacadeAdapter`, the ISBN methods were stubs returning null:

> "I need to connect these to the actual Google Books lookup service I built earlier."

**Phase 4: "AuthorService isn't adding value here"**

While wiring up the new facade methods, the developer noticed:

> "These calls just pass through AuthorService to AuthorRepository. If there's no business logic, why have the extra layer? The facade can talk to the repository directly for simple queries."

**Phase 5: "The command conditionals are getting messy"**

The boolean flag handling (`scan && multi`, `scan && !multi`, etc.) was error-prone:

> "Let me extract this to an enum that encodes the valid states explicitly."

---

## 6. Narrative of the Changes

The author disambiguation feature emerged from a subtle but important domain truth: people can share names. When the earlier database view showed three "Mike Jones" entries, it wasn't a bug — it was a limitation in how the system let users express their intent. A user registering a book by Mike Jones (the rapper) had no way to indicate "this is the same Mike Jones from my other hip-hop books" versus "this is a completely different person named Mike Jones."

The solution introduces a selection workflow at the point of author entry. When a user types an author name that already exists in the system, the CLI now displays all matching authors along with the books they've written. This context — seeing "[Back Then, The Voice, Flossin']" next to one Mike Jones — lets users make an informed choice. They can select an existing author to reuse that identity, or explicitly create a new author record if this is genuinely a different person.

This required new infrastructure: the facade needed methods to check for name existence, retrieve all authors by name, and fetch an author's books for display. The repository layer gained corresponding queries. The `CliPromptService` got a new `promptMultipleAuthorConfirmation()` method that builds a selection menu with Spring Shell's component flow.

While implementing this, the developer noticed that `BookFacadeAdapter`'s ISBN methods were still stubbed out from earlier work. These now properly integrate with the `IsbnLookupService` to fetch book metadata from Google Books, transform it through the mapper, and persist it via the repository. The entire barcode-scan-to-database pipeline is now functional.

Several cleanup improvements accompanied the main feature: `AuthorFacadeImpl` was simplified by removing its dependency on `AuthorService` (which was just passing through to the repository anyway), command handling was refactored to use a `ScanMode` enum instead of boolean conditionals, and logging was upgraded from string concatenation to SLF4J parameterized format.

---

## 7. Key Technical Highlights

- **Implemented author name collision detection** via `authorExistFirstNameLastName()` method chain from CLI → Facade → Repository → JPA
- **Created disambiguation UI** with `promptMultipleAuthorConfirmation()` that displays author options with their book catalogs
- **Wired up ISBN scan pipeline** connecting `BookFacadeAdapter.findBookMetaDataByIsbn()` → `IsbnLookupService` → `GoogleBooksResponse` → `BookMapper` → `BookMetaDataResponse`
- **Implemented `createBookFromMetaData()`** for persisting scanned books with console feedback showing import success
- **Added `getBooksByAuthorId()`** to display an author's existing works during disambiguation
- **Extracted `createAuthors()` helper method** encapsulating the prompt-check-select-or-create workflow
- **Introduced `ScanMode` enum** replacing boolean flag conditionals with explicit mode dispatch
- **Simplified AuthorFacadeImpl** by removing AuthorService dependency and routing directly to AuthorRepository
- **Added mapper methods** `toBookMetaDataResponseFromGoogleBooksResponse()` and `toEntityFromBookMetaDataResponse()` for Google Books API transformation
- **Upgraded logging** from string concatenation (`"text: " + value`) to parameterized format (`"text: {}", value`)

---

## 8. Talking Points (Interview, Portfolio, Devlog)

- **Designed and implemented an author disambiguation workflow** that handles real-world name collisions by prompting users to select from existing authors (displaying their book catalogs for context) or explicitly create new author records, preserving data integrity while respecting domain complexity

- **Completed the ISBN barcode scanning pipeline** by integrating Google Books API lookup with domain mapping and persistence, enabling users to scan physical books and import metadata automatically with a single command

- **Reduced architectural complexity** by identifying and removing unnecessary service layer indirection, simplifying facade implementations to directly coordinate with repositories when no business logic transformation is required

- **Improved command dispatch clarity** by refactoring boolean flag handling into an explicit enum-based mode selection, eliminating edge-case bugs from boolean combinations and making control flow self-documenting

- **Enhanced domain model expressiveness** by expanding facade contracts with workflow-driven methods (`authorExistFirstNameLastName`, `getAllAuthorsByName`, `getBooksByAuthorId`) that support real user decision-making rather than CRUD operations

- **Applied incremental domain modeling** by letting actual use cases (name collisions discovered during testing) drive interface design rather than speculating about future needs

---

## 9. Potential Interview Questions (Based on This Diff)

### Design Rationale & Tradeoffs

1. **Why display books alongside author names in the disambiguation menu? What other approaches did you consider?**
   - *Expected discussion: unique identifiers (middle name, birth year), user-assigned nicknames, showing book count vs titles, performance of fetching books*

2. **Why use `authorId == 0` to signal "create new" instead of a separate boolean or Option type?**
   - *Expected discussion: null safety, sentinel values, making invalid states unrepresentable*

3. **The ScanMode enum only has three states. Was the enum overkill compared to boolean flags?**
   - *Expected discussion: boolean blindness, self-documenting code, extension for future modes*

### Domain-Driven Design Understanding

4. **Is "author name" a value object or just a pair of strings? How would making it a value object change this code?**
   - *Expected discussion: AuthorName VO encapsulating first/last, equality semantics, validation*

5. **Should the "multiple authors with same name" rule be enforced in the domain layer rather than the CLI?**
   - *Expected discussion: domain events, aggregate boundaries, where business rules belong*

6. **How do you handle the case where users accidentally create a duplicate author and want to merge them later?**
   - *Expected discussion: author merge operation, updating book-author relationships, audit trail*

### Architecture Awareness

7. **You removed AuthorService from AuthorFacadeImpl. When IS a service layer appropriate?**
   - *Expected discussion: orchestration logic, transactions spanning multiple repositories, business rules*

8. **The CLI now depends on both AuthorFacade and BookFacade for the author selection display. Is this a smell?**
   - *Expected discussion: facade granularity, composite facades, query objects*

9. **Why does `promptMultipleAuthorConfirmation` live in CliPromptService rather than BookCommands?**
   - *Expected discussion: separation of prompting from command logic, reusability, single responsibility*

### Persistence & Data Concerns

10. **`getBooksByAuthorId` fetches all books then takes 3. What's the problem and how would you fix it?**
    - *Expected discussion: N+1 queries, pagination, LIMIT in JPA, projection queries*

11. **If two users simultaneously create the same author name, what happens? Is there a race condition?**
    - *Expected discussion: unique constraints, optimistic locking, upsert patterns*

12. **The `createAuthorsIfNotExist` method splits author names on space. What are the edge cases?**
    - *Expected discussion: "Mary Jane Watson", "Plato", cultural naming conventions, parsing robustness*

### Code Quality & Maintainability

13. **There's still `System.out.println` mixed with `log.info`. When do you use each?**
    - *Expected discussion: user-facing output vs operational logging, structured logging, log levels*

14. **The `buildAuthorOptions` method uses ANSI color codes inline. How would you improve this?**
    - *Expected discussion: ConsoleColors utility (which exists!), terminal compatibility, testability*

---

## 10. Areas Worth Diving Deeper Into

### Query Optimization & JPA Performance

**Why this matters:** The `getBooksByAuthorId()` method currently fetches all books then limits to 3 in Java. This works for small datasets but won't scale. Understanding JPA's query methods, `@Query` annotations with `LIMIT`, and projection patterns would help you write efficient data access code.

**Resources:**
- Vlad Mihalcea's blog on [JPA pagination and LIMIT](https://vladmihalcea.com)
- *High-Performance Java Persistence* — Vlad Mihalcea

### CLI/Shell Application Patterns

**Why this matters:** You're building sophisticated interactive workflows with Spring Shell's component flows. Understanding patterns for command composition, prompt chains, and state management would help you build more complex CLI features cleanly.

**Resources:**
- Spring Shell Reference Documentation (component flow section)
- *The Pragmatic Programmer* — Hunt & Thomas (chapter on "Domain Languages")

### Name Parsing & Internationalization

**Why this matters:** Your `createAuthorsIfNotExist` method assumes names split cleanly into "first last". Real-world names are complex: "Mary Jane Watson", "Plato", "José García Márquez". Understanding name handling patterns prevents data quality issues.

**Resources:**
- Falsehoods Programmers Believe About Names (article by Patrick McKenzie)
- Unicode Common Locale Data Repository (CLDR) for i18n patterns

### Facade vs Service vs Repository Boundaries

**Why this matters:** You made a judgment call removing AuthorService from the chain. Having a clear mental model for when each layer is appropriate helps you avoid both over-engineering and under-abstraction.

**Resources:**
- *Patterns of Enterprise Application Architecture* — Martin Fowler (Service Layer, Repository patterns)
- *Get Your Hands Dirty on Clean Architecture* — Tom Hombergs

### Making Invalid States Unrepresentable

**Why this matters:** Using `authorId == 0` as a sentinel for "create new" works but is fragile. The functional programming concept of making invalid states unrepresentable (via Option types, sealed classes, etc.) would make this code more robust.

**Resources:**
- "Making Invalid State Unrepresentable" — Yaron Minsky (talk/article)
- *Effective Java* — Joshua Bloch (Item 30: Use enums instead of int constants)

---

## Code Examples: Key Patterns from This Diff

### Author Disambiguation Flow

```java
// BookCommands.java - createAuthors()
public List<AuthorDTO> createAuthors(){
    int numberOfAuthors = cliPrompt.promptForBookAuthorCount();
    List<AuthorDTO> authors = new ArrayList<>();
    
    for (int i = 0; i < numberOfAuthors; i++) {
        AuthorDTO authorDTO = cliPrompt.promptForAuthor();
        
        // Check for name collision
        if(authorFacade.authorExistFirstNameLastName(
                authorDTO.firstName(), authorDTO.lastName())){
            
            System.out.println("Multiple Authors with this name.\n");
            
            // Let user choose existing or create new
            Long authorId = cliPrompt.promptMultipleAuthorConfirmation(authorDTO);
            
            if(authorId == 0){
                // User chose "Create New Author"
                authors.add(authorFacade.saveAuthor(authorDTO));
            } else {
                // User selected existing author
                authors.add(authorFacade.findById(authorId));
            }
        } else {
            // No collision - just create
            authors.add(authorFacade.saveAuthor(authorDTO));
        }
    }
    return authors;
}
```

### Building Selection Options with Context

```java
// CliPromptService.java
private List<SelectItem> buildAuthorOptions(AuthorDTO author) {
    List<SelectItem> options = new ArrayList<>();
    
    // "Create New" is always first, with ID 0 as sentinel
    options.add(SelectItem.of(
        "\u001B[38;5;42m[Create New Author]\u001B[0m", "0"));
    
    // Fetch all authors with matching name
    List<AuthorDTO> authors = authorFacade.getAllAuthorsByName(
        author.firstName(), author.lastName());
    
    // Display each with their books for disambiguation
    for(AuthorDTO a : authors){
        String label = String.format("%s %s (ID: %d) : %s",
            a.firstName(), a.lastName(), a.id(),
            bookFacade.getBooksByAuthorId(a.id()));  // Shows their books!
        options.add(SelectItem.of(label, String.valueOf(a.id())));
    }
    return options;
}
```

### Enum-Based Command Dispatch

```java
// Inferred ScanMode enum (not shown in diff but implied)
public enum ScanMode {
    SINGLE, MULTI, NONE;
    
    public static ScanMode from(boolean scan, boolean multi) {
        if (scan && multi) return MULTI;  // multi takes precedence
        if (scan) return SINGLE;
        return NONE;  // manual entry
    }
}

// Usage in BookCommands.registerBook()
ScanMode mode = ScanMode.from(scan, multi);
switch(mode){
    case SINGLE -> scanBook(false);
    case MULTI -> multiBookScan();
    case NONE -> createBookManually();
}
```

### Google Books to Domain Mapping

```java
// BookMapper.java
public BookMetaDataResponse toBookMetaDataResponseFromGoogleBooksResponse(
        GoogleBooksResponse googleBooksResponse, String isbn) {
    
    if(googleBooksResponse == null || 
       googleBooksResponse.items() == null || 
       googleBooksResponse.items().isEmpty()){
        throw new RuntimeException("No book found for ISBN: " + isbn);
    }
    
    var volumeInfo = googleBooksResponse.items().get(0).volumeInfo();
    
    return new BookMetaDataResponse(
        null,  // ID assigned on persist
        volumeInfo.title(),
        isbn,
        new ArrayList<>(volumeInfo.authors()),
        volumeInfo.publisher(),
        volumeInfo.description()
    );
}
```

---

## What's Next?

Based on this diff, natural follow-up work might include:

1. **Author merge functionality** — When users accidentally create duplicates, let them merge author records
2. **Pagination for author's books** — `getThreeBooksByAuthorId` should use JPA `Pageable` or `LIMIT`
3. **Better name parsing** — Handle "Mary Jane Watson" and single-name authors gracefully
4. **Test coverage** — The disambiguation workflow has multiple branches worth testing
5. **Remove remaining System.out.println** — Route all output through a consistent logging/display layer

---

*This diff demonstrates mature domain thinking — recognizing that the "duplicate author" situation from earlier wasn't a bug but a missing feature, then implementing a user-friendly solution that respects the domain complexity while keeping the architecture clean.*
