# Devlog: Extract BookcardRenderer from BookCommands

**Date:** December 20, 2025  
**Module:** CLI (UI)  
**Type:** Refactor  
**Status:** ✅ Complete

---

## 1. High-Level Summary

- **Extracted UI rendering logic into dedicated `BookcardRenderer` class** — book card display and "not found" messages now live outside of command handlers
- **Renamed package `cli.commands` → `cli.command`** — consistency fix (singular)
- **Removed dead code** — placeholder `suggestBookShelf()` and unused `addScanResultCommand()`
- **Improved separation of concerns** — commands handle flow, renderer handles display

---

## 2. The Underlying Problem

`BookCommands` was doing too much:

```java
public class BookCommands {
    // Flow logic ✓
    public void scanBook() { ... }
    public void searchByTitle() { ... }
    
    // UI rendering ✗ (doesn't belong here)
    public String createBookCard(...) { ... }
    public void printNotFound(String title) { ... }
    private String formater(String authors) { ... }
    private int countAuthors(String authors) { ... }
    
    // Dead code ✗
    public void suggestBookShelf() { System.out.println("G-16"); }
    public boolean addScanResultCommand(...) { /* commented out */ }
}
```

**Problems:**
1. **Mixed responsibilities** — Command class handling both orchestration AND presentation
2. **Hard to reuse** — Other commands (e.g., `BookcaseCommands`) couldn't use the same book card
3. **Hard to test** — Testing rendering required instantiating the entire command with all its dependencies
4. **Dead code accumulation** — Placeholder methods and commented code cluttering the class

---

## 3. The Solution

### New Class: `BookcardRenderer`

**Location:** `com.penrose.bibby.cli.ui.BookcardRenderer`

```java
@Component
public class BookcardRenderer {
    
    public String createBookCard(String title, String isbn, String author, 
                                  String publisher, String bookcase, 
                                  String shelf, String location) {
        return """
            ╭──────────────────────────────────────────────────────────────────╮
            │  📖 %-60s │
            ├──────────────────────────────────────────────────────────────────┤
            │  ISBN:      %-31s                                    │
            │  Author:    %-31s                                    │
            │  Publisher: %-31s                                    │
            │                                                                  │
            │  Location:  %-35s                                │
            │  Bookcase:  %-35s                                │
            │  Shelf:     %-35s                                │
            ╰──────────────────────────────────────────────────────────────────╯
        """.formatted(title, isbn, formatAuthors(author), publisher, 
                      location, bookcase, shelf);
    }
    
    public void printNotFound(String title) {
        String msg = """
            ╭──────────────────────────────────────────────╮
            │  🚫 No Results Found                         │
            ├──────────────────────────────────────────────┤
            │  Query:  %-34s  │
            │                                              │
            │  Status: Not in library.                     │
            │  Action: Check spelling or add new book.     │
            ╰──────────────────────────────────────────────╯
        """.formatted(truncate(title, 34));
        
        System.out.println(ConsoleColors.RED + msg + ConsoleColors.RESET);
    }
    
    private String formatAuthors(String authors) {
        String normalized = authors.replaceAll("[\\[\\]]", "");
        return normalized.replaceAll(",\\s*", ", ");
    }
    
    private String truncate(String text, int maxLength) {
        return text.length() > maxLength 
            ? text.substring(0, maxLength - 3) + "..." 
            : text;
    }
}
```

### BookCommands — Now Focused on Flow

```java
@Component
public class BookCommands {
    private final BookcardRenderer bookcardRenderer;  // NEW dependency
    
    public void scanBook() {
        // ... flow logic ...
        
        String bookcard = bookcardRenderer.createBookCard(
            response.title(),
            response.isbn(),
            response.authors().toString(),
            response.publisher(),
            bookcaseLabel,
            shelfLabel,
            location
        );
        System.out.println(bookcard);
    }
    
    public void searchByTitle() {
        // ...
        if (bookDTO == null) {
            bookcardRenderer.printNotFound(title);  // Delegated!
            return;
        }
        // ...
    }
}
```

---

## 4. What Moved

| Method | From | To |
|--------|------|----|
| `createBookCard()` | BookCommands | BookcardRenderer |
| `printNotFound()` | BookCommands | BookcardRenderer |
| `formater()` | BookCommands | BookcardRenderer.formatAuthors() |
| `countAuthors()` | BookCommands | BookcardRenderer (or removed if unused) |

---

## 5. What Was Removed

| Method | Reason |
|--------|--------|
| `suggestBookShelf()` | Placeholder with hardcoded `"G-16"` output — no real implementation |
| `addScanResultCommand()` | Unused method with commented-out code |

**Lesson:** Don't commit placeholders. If a feature isn't ready, don't add a stub command that does nothing useful.

---

## 6. Package Rename

```
com.penrose.bibby.cli.commands  →  com.penrose.bibby.cli.command
```

Minor consistency fix. Both conventions are valid, but singular (`command`) is more common in Spring projects.

**Files affected:**
- `BookCommands.java`
- `ScanMode.java`
- `BookcaseCommands.java`
- `LibraryCommands.java`

---

## 7. New Package Structure

```
com.penrose.bibby.cli
├── command/
│   ├── book/
│   │   ├── BookCommands.java
│   │   └── ScanMode.java
│   ├── bookcase/
│   │   └── BookcaseCommands.java
│   └── library/
│       └── LibraryCommands.java
├── prompt/
│   ├── application/
│   │   └── CliPromptService.java
│   ├── contracts/
│   │   └── PromptFacade.java
│   └── domain/
│       └── PromptOptions.java
└── ui/                          ← NEW
    └── BookcardRenderer.java    ← NEW
```

---

## 8. The Architectural Meaning

### Separation of Concerns

| Layer | Responsibility | Example |
|-------|----------------|---------|
| Commands | User intent, flow orchestration | `scanBook()`, `searchByTitle()` |
| Prompts | User interaction, input gathering | `promptForIsbn()`, `promptForBookcase()` |
| Renderer | Output formatting, display | `createBookCard()`, `printNotFound()` |
| Options | Menu construction | `bookCaseOptions()`, `yesNoOptions()` |

### Before

```
BookCommands
├── Flow logic
├── User prompts (via CliPromptService)
├── Menu options (via PromptOptions)
└── Rendering (embedded) ← Wrong place
```

### After

```
BookCommands
├── Flow logic
├── User prompts (via CliPromptService)
├── Menu options (via PromptOptions)
└── Rendering (via BookcardRenderer) ← Proper delegation
```

---

## 9. Benefits

### 1. Reusability

Other commands can now use the same book card:

```java
// In BookcaseCommands
bookcardRenderer.createBookCard(...);

// In future SearchCommands
bookcardRenderer.createBookCard(...);
```

### 2. Testability

Can test rendering in isolation:

```java
@Test
void createBookCard_truncatesLongAuthors() {
    BookcardRenderer renderer = new BookcardRenderer();
    String card = renderer.createBookCard(
        "Test Book",
        "123-456",
        "Author One, Author Two, Author Three, Author Four, Author Five",
        "Publisher",
        "Office",
        "NorthWall:A",
        "Shelf 1"
    );
    
    assertThat(card).contains("Author One, Author Two");
    assertThat(card).contains("...");
}
```

### 3. Maintainability

Need to change card styling? One place:

```java
// Before: Find and update in BookCommands (700+ line file)
// After: Update BookcardRenderer (< 100 lines)
```

### 4. Readability

`BookCommands` is now ~150 lines shorter and focused on its job.

---

## 10. Talking Points (Interview / Portfolio)

- **Applied Extract Class refactoring** to separate rendering concerns from command orchestration

- **Improved single responsibility** — commands handle user intent and flow, renderer handles display formatting

- **Enabled reusability** — book card rendering can now be used by any command without duplication

- **Removed dead code** — eliminated placeholder commands and commented-out methods that added no value

- **Improved testability** — rendering logic can now be unit tested without instantiating command dependencies

---

## 11. Potential Interview Questions

1. Why extract rendering into its own class instead of keeping it in the command?

2. How does this extraction improve testability?

3. What's the difference between `cli.ui` and `cli.prompt`?

4. Should `BookcardRenderer` know about domain objects like `BookDTO`, or should it only accept primitives?

5. How would you handle different card formats (compact vs detailed)?

6. Why remove the placeholder `suggestBookShelf()` instead of implementing it?

---

## 12. Future Improvements

1. **Accept DTOs instead of primitives** — `createBookCard(BookDisplayData data)` instead of 7 string parameters

2. **Add card variants** — `createCompactCard()`, `createDetailedCard()`, `createListItem()`

3. **Extract color constants** — Move ANSI codes to a `ConsoleColors` or `Theme` class

4. **Add AuthorRenderer** — If author formatting gets complex, extract further

5. **Template engine** — For complex UIs, consider a template approach

---

## 13. Commit Message

```
refactor(cli): extract BookcardRenderer and remove dead code

- Rename package cli.commands → cli.command (singular)
- Extract BookcardRenderer for book card display logic
  - Move createBookCard() to BookcardRenderer
  - Move printNotFound() to BookcardRenderer
  - Move formatAuthors() and truncate() helpers
- Inject BookcardRenderer into BookCommands
- Remove unused suggestBookShelf() placeholder command
- Remove unused addScanResultCommand() method
- Remove unused imports from BookcaseCommands

Improves separation of concerns: commands handle flow, renderer handles display.
```
