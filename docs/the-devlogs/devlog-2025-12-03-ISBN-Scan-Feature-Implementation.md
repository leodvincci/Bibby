# Devlog: ISBN Scan-to-Database Feature Implementation

**Date:** 2025-12-03  
**Focus:** Building the CLI workflow for scanning book ISBNs and persisting enriched metadata  
**Commit Type:** `feat(cli)`

---

## Summary

Implemented end-to-end ISBN scanning functionality in the CLI. Users can now scan a book's ISBN (or enter it manually), the system fetches metadata from the Google Books API, displays it for confirmation, and persists the enriched book record to the database.

---

## What Was Built

### The User Flow

```
1. User runs: book scan
2. CLI prompts: "ISBN Number:_"
3. User enters/scans ISBN
4. System calls Google Books API
5. CLI displays formatted metadata
6. User confirms: "Add this book?"
7. System persists book + authors to database
8. Success message displayed
```

### Components Modified

| File | Changes |
|------|---------|
| `BookCommandLine.java` | Added `scanBook()` command, injected `BookInfoService` |
| `CliPromptService.java` | Added `promptForIsbnScan()` method |
| `BookService.java` | Added `createScannedBook()` for processing API response |

---

## Implementation Details

### Phase 1: Scaffolding (First Commit)

Started with the basic structure—command registration and ISBN input prompt.

**BookCommandLine.java:**
```java
@Command(command = "scan", description = "Scan a book's ISBN to add it to your library database", group = "Book Commands")
public void scanBook() {
    String isbn = cliPrompt.promptForIsbnScan();
    System.out.println("Scanned ISBN: " + isbn);
}
```

**CliPromptService.java:**
```java
public String promptForIsbnScan(){
    ComponentFlow flow = componentFlowBuilder.clone()
            .withStringInput("isbn")
            .name("ISBN Number:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("isbn", String.class);
}
```

This established the skeleton—a working command that accepts input but doesn't do anything with it yet.

### Phase 2: Complete Integration (Second Commit)

Wired up the full pipeline: API lookup → display → confirm → persist.

**The Scan Command (Complete):**
```java
@Command(command = "scan", description = "Scan a book's ISBN to add it to your library database", group = "Book Commands")
public void scanBook() {
    String isbn = cliPrompt.promptForIsbnScan();
    System.out.println("Scanned ISBN: " + isbn);
    
    GoogleBooksResponse googleBooksResponse = bookInfoService.lookupBook(isbn).block();
    
    if(addScanResultCommand(googleBooksResponse, isbn)){
        bookService.createScannedBook(googleBooksResponse, isbn);
        System.out.println("\n\u001B[36m</>\033[0m: Book added to the library database successfully!");
    }
}
```

**Metadata Display:**
```java
public boolean addScanResultCommand(GoogleBooksResponse bookMetaData, String isbn) {
    String title = bookMetaData.items().get(0).volumeInfo().title();
    String authors = bookMetaData.items().get(0).volumeInfo().authors().toString();
    String publishingDate = bookMetaData.items().get(0).volumeInfo().publishedDate();
    String categories = String.valueOf(bookMetaData.items().get(0).volumeInfo().categories());
    String description = bookMetaData.items().get(0).volumeInfo().description();

    System.out.printf(""
            + "========================================\n"
            + "📚  Book Metadata\n"
            + "========================================\n"
            + "\n"
            + "ISBN:              %s\n"
            + "Title:             %s\n"
            + "Authors:           %s\n"
            + "Published:         %s\n"
            + "Categories:        %s\n"
            + "\n"
            + "Description:\n"
            + "%s\n"
            + "\n"
            + "========================================\n",
            isbn, title, authors, publishingDate, categories, description);
    
    return cliPrompt.promptBookConfirmation();
}
```

**Book Persistence:**
```java
public void createScannedBook(GoogleBooksResponse googleBooksResponse, String isbn){
    BookEntity bookEntity = new BookEntity();
    Set<AuthorEntity> authors = new HashSet<>();
    
    for(String authorName : googleBooksResponse.items().get(0).volumeInfo().authors()) {
        String[] nameParts = authorName.split(" ", 2);
        AuthorEntity authorEntity = authorService.findOrCreateAuthor(nameParts[0], nameParts[1]);
        authors.add(authorEntity);
    }

    bookEntity.setIsbn(isbn);
    bookEntity.setTitle(googleBooksResponse.items().get(0).volumeInfo().title());
    bookEntity.setPublisher(googleBooksResponse.items().get(0).volumeInfo().publisher());
    bookEntity.setPublicationYear(Integer.parseInt(
        googleBooksResponse.items().get(0).volumeInfo().publishedDate().split("-")[0]
    ));
    bookEntity.setDescription(googleBooksResponse.items().get(0).volumeInfo().description());
    bookEntity.setAuthors(authors);
    bookEntity.setCreatedAt(LocalDate.now());
    bookEntity.setAvailabilityStatus("AVAILABLE");
    
    saveBook(bookEntity);
}
```

---

## Technical Decisions

### 1. Blocking on Reactive Call

```java
GoogleBooksResponse googleBooksResponse = bookInfoService.lookupBook(isbn).block();
```

**Why block?** Spring Shell commands run synchronously. The reactive `Mono<GoogleBooksResponse>` from `BookInfoService` needs to be unwrapped for the CLI context. This is intentional—we're in a blocking CLI, not a web server.

**Trade-off**: If this were a web controller, we'd return the `Mono` and let the framework handle it reactively. For CLI, blocking is appropriate.

### 2. Author Name Parsing

```java
String[] nameParts = authorName.split(" ", 2);
AuthorEntity authorEntity = authorService.findOrCreateAuthor(nameParts[0], nameParts[1]);
```

**Current assumption**: "FirstName LastName" format.

**Known edge cases not yet handled**:
- "LastName, FirstName" (some Google Books entries)
- Single names ("Plato", "Madonna")
- Multiple middle names ("Mary Jane Watson-Parker")
- Suffixes ("John Smith Jr.")

**TODO**: Add defensive parsing or a name normalization utility.

### 3. Publication Year Extraction

```java
Integer.parseInt(googleBooksResponse.items().get(0).volumeInfo().publishedDate().split("-")[0])
```

**Current assumption**: ISO format "YYYY-MM-DD" or at least "YYYY-...".

**Known edge cases not yet handled**:
- Just year: "2024"
- Text format: "March 2024"
- Null/missing dates

**TODO**: Add date parsing utility with fallback handling.

### 4. Direct Entity Creation (Not Using Factory)

`createScannedBook()` directly instantiates `BookEntity` rather than using `BookFactory`.

**Why it happened**: Quick path to working feature. The factory pattern wasn't designed with the Google Books response shape in mind.

**Architectural debt**: This bypasses the domain layer. The cleaner approach would be:
1. Map `GoogleBooksResponse` → `Book` (domain model)
2. Use `BookFactory.toEntity(Book)` for persistence

**TODO**: Refactor to route through factory for consistency.

---

## Struggle Journal

### Challenge: Return Type for Confirmation

Initially `addScanResultCommand()` was `void`. But I needed to know whether the user confirmed or cancelled to decide whether to persist.

**Solution**: Changed return type to `boolean`, returning `cliPrompt.promptBookConfirmation()` which already returns the user's yes/no choice.

**Lesson**: Think through the full flow before implementing. The confirmation step was always planned, but I didn't wire up the return value initially.

### Challenge: Deep Nesting in API Response

Accessing data required repeated chains like:
```java
googleBooksResponse.items().get(0).volumeInfo().title()
```

This appears 10+ times in the code. It's verbose and fragile (what if `items()` is empty?).

**Deferred solution**: Could extract to a helper method or create a simplified DTO that flattens the structure. Left as-is for now since it works, but noted for refactoring.

### Challenge: Character Encoding in CLI Output

The formatted output includes emoji (📚) and box-drawing characters. These render correctly in most modern terminals but might break in limited environments.

**Accepted trade-off**: The CLI is for personal use on modern systems. If portability becomes an issue, can fall back to ASCII-only formatting.

---

## Interview Talking Points

### "Walk me through how the scan feature works."

> When the user runs `book scan`, the CLI prompts for an ISBN. We take that ISBN and call the Google Books API through our `BookInfoService`, which returns a reactive `Mono`. Since we're in a CLI context, we block on that call to get the response synchronously.
>
> We then display the metadata—title, authors, publication date, description—in a formatted view and ask the user to confirm. If they confirm, we parse the response, create or find the author entities, build the book entity, and persist it. The user gets immediate feedback that their book was added.

### "What would you improve about this implementation?"

> Three things come to mind. First, the author name parsing is naive—it assumes "FirstName LastName" format, but Google Books data isn't always consistent. I'd add a name parsing utility with fallback handling.
>
> Second, the `createScannedBook()` method bypasses my factory pattern. To maintain architectural consistency, I should map the API response to a domain model first, then use the factory to create the entity.
>
> Third, there's no error handling for API failures or missing data. In production, I'd add try-catch blocks, handle null fields gracefully, and provide user-friendly error messages.

### "Why did you choose to block on the reactive call?"

> Spring Shell commands are inherently synchronous—the user is waiting at a prompt for the result. Unlike a web request where we can return a `Mono` and let the framework handle the async response, the CLI needs the data immediately to display it. Blocking is the right choice here because it matches the execution model of the environment.

---

## What's Next

1. **Error handling**: Wrap API call in try-catch, handle empty results
2. **Author name parsing**: Build robust name normalization
3. **Date parsing**: Handle various date formats from Google Books
4. **Factory refactor**: Route through `BookFactory` for consistency
5. **Duplicate detection**: Check if ISBN already exists before adding
6. **Shelf assignment**: After adding book, prompt to place on shelf (complete scan-to-shelf flow)

---

## Files Changed

| Commit | Files | Purpose |
|--------|-------|---------|
| Scaffolding | 2 | Command + prompt structure |
| Integration | 2 | API wiring + persistence |

---

## Feature Status

```
[✓] ISBN input via CLI prompt
[✓] Google Books API integration
[✓] Metadata display with formatting
[✓] User confirmation flow
[✓] Book + Author persistence
[ ] Error handling for API failures
[ ] Edge case handling (names, dates)
[ ] Duplicate ISBN detection
[ ] Immediate shelf assignment option
```

The happy path works end-to-end. Edge cases and error handling are the next priority before this is production-ready.
