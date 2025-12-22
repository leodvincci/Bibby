# Devlog: December 21, 2025

## Summary

Today's work centered on CLI workflow refinement across four major themes: separating scan-based and manual book creation workflows, implementing CSV bulk import, adding shelf-targeted commands, and introducing optional placement prompts. The throughline is making book intake more flexible—users can now catalog books without immediately placing them, import entire collections via CSV, or assign books to shelves in the same workflow.

---

## 1. Scan/Manual Workflow Separation

### The Problem

`BookCreateCommands` had grown into a 7-dependency behemoth handling two distinct user workflows:

```java
// Before: Everything in one class
public BookCreateCommands(
    CliPromptService cliPrompt, 
    BookcardRenderer bookcardRenderer,      // scan-only
    BookFacade bookFacade, 
    BookcaseFacade bookcaseFacade,           // scan-only
    ShelfFacade shelfFacade,                 // scan-only
    AuthorFacade authorFacade, 
    PromptOptions promptOptions)             // scan-only
```

Scanning needs location selection, book card rendering, and API metadata parsing. Manual entry just needs prompts and the ability to save books. Coupling them meant every change touched unrelated code.

### The Solution

Created `BookCreateScanCommands` to own the scan workflow:

```java
@ShellComponent
@Command(command = "book", group = "Book Create Commands")
public class BookCreateScanCommands {
    private final CliPromptService cliPrompt;
    private final BookcardRenderer bookcardRenderer;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;
    private final ShelfFacade shelfFacade;
    private final AuthorFacade authorFacade;
    private final PromptOptions promptOptions;
    // ... scan-specific methods
}
```

Slimmed `BookCreateCommands` to just 3 dependencies:

```java
// After: Clean separation
public BookCreateCommands(
    CliPromptService cliPrompt, 
    BookFacade bookFacade, 
    AuthorFacade authorFacade)
```

**Methods that moved to `BookCreateScanCommands`:**

| Method | Purpose |
|--------|---------|
| `createBookScan()` | Main entry point for `book scan` |
| `scanBook()` | ISBN prompt → API lookup → display card |
| `createAuthorsFromMetaData()` | Parse author names from Google Books response |
| `mapAuthorStringToDTO()` | "FirstName LastName" → `AuthorDTO` |
| `createNewAuthorOrAddExisting()` | Author disambiguation when names collide |
| `saveNewAuthor()` | Facade wrapper |
| `authorExists()` | Facade wrapper |

The package initially had a `book/create/` subpackage, but I flattened it back to `book/`—subdirectories didn't add value for two files.

### Commits

- `bf6347c` — Create `BookCreateScanCommands` with full scan workflow
- `26ac6cb` — Remove 242 lines and 4 dependencies from `BookCreateCommands`
- `4ec9e6f` — Add section comment headers and improve Javadoc
- `f018b4f` — Refine Javadoc for `createNewAuthorOrAddExisting`

---

## 2. CSV Import Feature

### Implementation

Added bulk import capability through `LibraryCommands`:

```java
@Command(command = "import", description = "Import CSV of ISBNs to add multiple books at once.")
public void createBooksFromCsv(@ShellOption String filePath) throws IOException {
    log.info("Importing books from CSV...");
    
    try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
        String line;
        while ((line = reader.readLine()) != null) {
            BookMetaDataResponse bookMetaDataResponse = bookCreateScanCommands.importBook(line);
        }
    } catch (IOException exception) {
        log.error("Error reading CSV file: {}", exception.getMessage());
        throw new IOException("Failed to read CSV file", exception);
    }
    
    System.out.println("\033[38;5;42mBooks imported successfully from CSV.\033[0m");
}
```

The `importBook()` method is a headless variant of `scanBook()` that skips the ISBN prompt:

```java
public BookMetaDataResponse importBook(String isbn){
    log.info("Initiating scanBook for Import.");
    BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
    System.out.println(
            bookcardRenderer.bookImportCard(
                bookMetaDataResponse.title(),
                bookMetaDataResponse.isbn(),
                bookMetaDataResponse.authors().toString(),
                bookMetaDataResponse.publisher())
    );
    return bookMetaDataResponse;
}
```

### New Book Card for Imports

Created `bookImportCard()` in `BookcardRenderer`—a compact card without location fields since imported books aren't placed immediately:

```java
public String bookImportCard(String title, String isbn, String author, String publisher) {
    return """
            ╭──────────────────────────────────────────────────────────────────────────────────────╮
            │  📖 \033[38;5;63m%-73s\033[0m        │     
            ├──────────────────────────────────────────────────────────────────────────────────────┤
            │  \033[38;5;42mISBN\033[0m:      %-31s                                          │
            │  \033[38;5;42mAuthor\033[0m:    %-31.31s%-3.3s                                       │                                                              
            │  \033[38;5;42mPublisher\033[0m: %-31.31s                                          │
            ╰──────────────────────────────────────────────────────────────────────────────────────╯
    """.formatted(title, isbn, formater(author), 
                  author.length() > 42 ? "..." : " ", publisher);
}
```

### Commits

- `7b7b212` — Initial CSV file reading with `Files.newBufferedReader`
- `fd3f352` — Wire up `importBook()` to actually fetch metadata per ISBN
- `5662cde` — Create `BookCreateImportCommands` class (later consolidated)
- `0b693d0` — Update command group to "Library Commands"

---

## 3. Command Naming Evolution

### The Semantic Shift

Renamed commands to reflect how librarians naturally describe actions:

| Old Command | New Command | Rationale |
|-------------|-------------|-----------|
| `book add` | `book register` | Manual entry is "filling out a registration form" |
| `book scan` | `book add` | Scanning is the quick/common path for adding |
| (new) | `book shelve` | Explicitly placing a cataloged book |
| (new) | `book place` | Synonym for `shelve` |

The insight: "add" should map to the most common action. Most books enter via barcode scan, not manual entry. Reserve verbose names like "register" for the less common manual workflow.

### Command Descriptions

Added detailed descriptions with ANSI color hints:

```java
@Command(command = "add", description =
    """
    Add books to your library by ISBN (scan/type/paste) or manual entry.
    If an ISBN is provided, Bibby fetches metadata and creates a new book record. 
    Supports single entry, batch entry, or file import.
    """)
public void createBookScan(@ShellOption(defaultValue = "single") boolean multi) {
    // ...
}
```

### New Placement Commands

Stubbed out `shelve` and `place` for future implementation:

```java
@Command(command = "shelve", description =
    """
    \u001B[38;5;185mAssign an existing book in your library to a shelf (or move it to a new shelf). 
    You can identify books by ISBN (single, batch entry, or file input) or select from 
    your list of unplaced books. Updates shelf location only—does not create new book records.
    \u001B[0m""",
    group = "Book Placement Commands")
public void shelveBook() {
    System.out.println("Shelve command executed.");
}
```

### Commits

- `d7886a0` — Rename `add` → `register` in `BookCreateCommands`, add `add` to `BookCreateScanCommands`
- `b459b17` — Add `shelve` and `place` commands with descriptions
- `88b814d` — Rename command group to "Book Circulation Commands"
- `9215121` — Remove orphaned `scan` command
- `c639e01` — Remove unused import command from `BookCreateImportCommands`

---

## 4. Shelf Scan Command

Created a new command class for shelf-targeted operations:

```java
@ShellComponent
@Command(command = "shelf", group = "Shelf Commands")
public class ShelfScanCommands {

    @Command(command = "scan", description = "Scan books on a shelf.", group = "Shelf Commands")
    public void shelfScan() {
        System.out.println("Shelf scan command executed.");
    }
}
```

This is currently a stub, but the intent is clear: `shelf scan` will let you select a shelf first, then scan multiple books directly onto it. The inverse of the current flow where you add a book then optionally place it.

### Commit

- `4de27f0` — Add `ShelfScanCommands` class

---

## 5. Optional Placement Prompt

### The Problem

Previously, adding a book via scan *required* immediate shelf placement. But sometimes you're cataloging books for later organization, or the physical books aren't near their eventual home.

### The Solution

Added a placement decision prompt after book confirmation:

```java
public void createBookScan(@ShellOption(defaultValue = "single") boolean multi) {
    BookMetaDataResponse bookMetaDataResponse = scanBook();
    String isbn = bookMetaDataResponse.isbn();
    Long shelfId = null;
    Long bookcaseId = null;

    if (cliPrompt.promptToConfirmBookAddition()) {
        
        if (cliPrompt.promptForPlacementDecision()) {
            String location = cliPrompt.promptForBookcaseLocation();
            bookcaseId = cliPrompt.promptForBookcaseSelection(
                promptOptions.bookCaseOptionsByLocation(location));
            if (bookcaseId != null) {
                shelfId = cliPrompt.promptForShelfSelection(bookcaseId);
                if (shelfId == null) return;
                
                // Show updated card with location info
                String updatedBookCard = bookcardRenderer.createBookCard(
                    bookMetaDataResponse.title(),
                    // ... full location details
                );
                System.out.println(updatedBookCard);
            }
        }
        
        List<Long> authorIds = createAuthorsFromMetaData(bookMetaDataResponse.authors());
        bookFacade.createBookFromMetaData(bookMetaDataResponse, authorIds, isbn, shelfId);
        
        // Show card with "Not Set" if no placement
        if (shelfId == null) {
            String updatedBookCard = bookcardRenderer.createBookCard(
                bookMetaDataResponse.title(),
                bookMetaDataResponse.isbn(),
                bookMetaDataResponse.authors().toString(),
                bookMetaDataResponse.publisher(),
                "Not Set", "Not Set", "Not Set"
            );
            System.out.println(updatedBookCard);
        }
    }
}
```

The `promptForPlacementDecision()` method:

```java
public boolean promptForPlacementDecision() {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withSingleItemSelector("placementDecision")
            .name("Assign a shelf now?")
            .selectItems(promptOptions.yesNoOptions())
            .and().build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("placementDecision", String.class)
            .equalsIgnoreCase("Yes");
}
```

### UX Polish

Simplified the yes/no options from verbose to minimal:

```java
// Before
options.put("Yes  — \u001B[32mLet's Do It\u001B[0m", "Yes");
options.put("No  —  \u001B[32mNot this time\u001B[0m", "No");

// After
options.put("Yes", "Yes");
options.put("No", "No");
```

Also simplified the confirmation message from "Book Successfully Added" to just "Saved":

```java
// Before
System.out.println("\u001B[32mBook Successfully Added\u001B[0m\n");

// After
System.out.println("\u001B[32mSaved\u001B[0m\n");
```

### Commits

- `6cf3718` — Add `promptForPlacementDecision()` method and wire into flow
- `b8cb6b4` — Simplify yes/no options, fix control flow for optional placement
- `d6e7617` — Update output messages, switch to `bookImportCard()` for scan display

---

## 6. Prompt Method Renaming

Standardized method names across `CliPromptService` for consistency:

| Old Name | New Name |
|----------|----------|
| `promptForIsbnScan()` | `promptForIsbn()` |
| `promptForAuthor()` | `promptForAuthorDetails()` |
| `promptForShelf()` | `promptForShelfSelection()` |
| `promptForBookCase()` | `promptForBookcaseSelection()` |
| `promptBookConfirmation()` | `promptToConfirmBookAddition()` |

The pattern: verb + noun (+ optional qualifier). Clearer, more consistent, and self-documenting.

### ISBN Validation Change

Changed invalid ISBN handling from silent failure to exception:

```java
// Before
if (!isValidIsbn(isbn)) {
    System.out.println("Invalid ISBN...");
    return false;  // Swallowed the error
}

// After
if (!isValidIsbn(isbn)) {
    System.out.println("Invalid ISBN...");
    throw new IllegalArgumentException("Invalid ISBN");
}
```

This surfaces the error properly rather than returning a confusing `false` that gets ignored.

**Update**: Later in the day, this was reverted back to returning `false` (commit `9a1dfae`) to support the re-prompting flow. Throwing an exception broke the loop-until-valid pattern. The new flow shows an error message, clears the input, and re-prompts with `m` as the default—much friendlier than crashing.

---

## Architectural Observations

### Dependency Count as Code Smell

The refactoring from 7 → 3 dependencies in `BookCreateCommands` wasn't just about cleanliness. High dependency counts often indicate a class doing too much. When you find yourself injecting `BookcaseFacade` and `ShelfFacade` into a "book create" class, that's a hint the class has location concerns that don't belong there.

### Command Composition Pattern

`LibraryCommands` now delegates to `BookCreateScanCommands` for CSV import:

```java
public LibraryCommands(BookCreateScanCommands bookCreateScanCommands, ...) {
    this.bookCreateScanCommands = bookCreateScanCommands;
    // ...
}
```

This is composition over inheritance—`LibraryCommands` uses scan functionality without being a scan command itself. The alternative (inheritance) would tangle the command group hierarchy.

### Optional Null as Missing Shelf

Using `null` for `shelfId` when placement is skipped is pragmatic:

```java
bookFacade.createBookFromMetaData(bookMetaDataResponse, authorIds, isbn, shelfId);
// shelfId may be null
```

This works because the persistence layer already handles nullable shelf references. An alternative would be a dedicated "unplaced" shelf, but that adds ceremony without benefit.

---

## Metrics

- **Commits**: 22 (excluding merge commits)
- **PRs merged**: 8 (#157, #158, #160, #161, #163, #165, #166, #167)
- **New classes**: 3 (`BookCreateScanCommands`, `BookCreateImportCommands`, `ShelfScanCommands`)
- **Lines removed from `BookCreateCommands`**: 242
- **Dependencies reduced**: 7 → 3 in `BookCreateCommands`
- **New escape hatches**: 2 (`:q` for quit, `m` for manual)

---

## 7. Flexible ISBN Input with Manual Fallback

### The Problem

When scanning a book, if the barcode doesn't read correctly or the book doesn't have one, users had to exit the scan workflow entirely and run a separate manual entry command. This breaks flow, especially when cataloging a stack of books where most scan fine but a few don't.

### The Solution

Reworked `promptForIsbn()` to accept special commands alongside ISBN input:

```java
public String promptForIsbn() {
    ComponentFlow flow;
    flow = componentFlowBuilder.clone()
            .withStringInput("isbn")
            .name("ISBN Number (or 'm' for manual ':q' to abort):_")
            .defaultValue("m")
            .next(ctx -> {
                String value = ctx.getResultValue();
                if (value.equalsIgnoreCase("m")) {
                    System.out.println("\u001B[33mManual ISBN entry selected.\u001B[0m");
                    ctx.setResultValue("m");
                    ctx.setInput("m");
                    ctx.setDefaultValue("m");
                    return "m";
                } else if (value.equalsIgnoreCase(":q")) {
                    System.out.println("\u001B[31mISBN entry cancelled by user.\u001B[0m");
                    return ":q";
                } else if (!isbnValidator(value)) {
                    ctx.setResultValue(null);
                    ctx.setInput("");
                    ctx.setDefaultValue("m");
                    return "isbn";  // Re-prompt
                }
                return null;  // Valid ISBN, continue
            })
            .and()
            .build();
    // ...
}
```

Key changes:
- **Default value is now `"m"`** — pressing Enter without typing goes straight to manual entry
- **`"m"` triggers manual entry** — seamless fallback when barcode won't scan
- **`:q` aborts** — consistent with vi-style quit pattern used elsewhere
- **Invalid ISBN re-prompts** — no longer throws exception, just loops

### Wiring Up the Manual Entry Branch

In `BookCreateScanCommands.scanBook()`:

```java
public BookMetaDataResponse scanBook() {
    log.info("Initiating scanBook for Add Book (ISBN)");
    System.out.println("\n\u001B[95mAdd Book (ISBN)");
    String isbn = cliPrompt.promptForIsbn();
    
    if (isbn.equals("m")) {
        bookCreateCommands.createBookManually();
        return null;
    } else if (isbn.equals(":q")) {
        System.out.println("Aborting book addition.");
        return null;
    }
    
    BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
    // ...
}
```

And the caller now handles null returns:

```java
public void createBookScan(@ShellOption(defaultValue = "single") boolean multi) {
    BookMetaDataResponse bookMetaDataResponse = scanBook();
    if (bookMetaDataResponse == null) return;  // User chose manual or quit
    // ...
}
```

### Dependency Addition

To call `createBookManually()` from the scan workflow, needed to inject `BookCreateCommands`:

```java
public class BookCreateScanCommands {
    private final BookCreateCommands bookCreateCommands;
    
    public BookCreateScanCommands(..., BookCreateCommands bookCreateCommands) {
        // ...
        this.bookCreateCommands = bookCreateCommands;
    }
}
```

Same pattern applied to `BookCreateImportCommands` and `BookSearchCommands`.

### Manual Entry Also Gets Quit

Added `:q` support to `createBookManually()` as well:

```java
public void createBookManually() {
    System.out.println("\n\u001B[95mCreate New Book\u001B[0m (':q' to quit)");
    
    String title = cliPrompt.promptForBookTitle();
    if (title.equals(":q")) {
        log.info("Book creation process aborted by user.");
        System.out.println("Book creation aborted.\n");
        return;
    }
    // ...
}
```

### UX Flow

The new flow looks like:

```
bibby> book add

Add Book (ISBN)
ISBN Number (or 'm' for manual ':q' to abort):_ [user presses Enter]
Manual ISBN entry selected.

Create New Book (':q' to quit)
Title:_ The Art of Computer Programming
...
```

Or if they have an ISBN:

```
bibby> book add

Add Book (ISBN)
ISBN Number (or 'm' for manual ':q' to abort):_ 9780201633610
[fetches metadata, displays book card]
...
```

### Commits

- `9a1dfae` — Enhance ISBN prompt with `m`/`:q` options and default to `m`
- `4adcfc5` — Wire up manual entry branch, add null handling in callers
- `b9fc114` — Add `:q` quit option to manual book creation

---

## What's Next

- Implement actual logic for `shelf scan` (select shelf first, then batch scan)
- Add progress indicator for CSV import (currently silent during API calls)
- Consider `library import --dry-run` to preview what would be imported
- Wire up `shelve` and `place` commands to actual placement logic
- Add `:q` support to remaining prompts (author entry, shelf selection, etc.)
