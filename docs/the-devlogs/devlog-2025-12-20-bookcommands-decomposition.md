# Devlog: Decomposing BookCommands into Domain-Aligned Command Classes

**Date:** 2025-12-20  
**PRs:** #152, #153, #154  
**Theme:** Single Responsibility / Command Cohesion / Library Domain Modeling

## Context

`BookCommands` had become a "god class" accumulating four distinct responsibilities: book creation/cataloging, physical shelf placement, search/discovery, and circulation (check-in/check-out). While the code worked, navigating a 400+ line file with mixed concerns made it harder to reason about, test, and extend.

This refactoring session decomposed `BookCommands` into focused command classes aligned with library domain concepts.

## The Extractions

### PR #152: BookCreateCommands

**What moved:**
- `scanBook()` — ISBN barcode scanning workflow with Google Books metadata lookup
- `registerBook()` — entry point with `ScanMode` dispatch (scan vs manual)
- `createBookManually()` — manual title/author/ISBN entry
- Author handling methods: `createAuthors()`, `createAuthorsFromMetaData()`, `mapAuthorStringToDTO()`, `authorExists()`, `createNewAuthorOrAddExisting()`, `saveNewAuthor()`

**Lines:** -203 from BookCommands, +274 in new class

**Why it coheres:** All of this logic serves one job—getting a new book into the catalog. The author disambiguation workflow (`promptMultipleAuthorConfirmation` when names collide) belongs here because it's part of the intake process, not a standalone author management concern.

Initially placed in `cli/command/book/intake/` subpackage.

---

### PR #153: BookPlacementCommands

**What moved:**
- `addToShelf()` — placing books on shelves, including capacity checking

**Lines:** -45 from BookCommands, +64 in new class

**Why it coheres:** Physical organization is distinct from cataloging. A book can exist in the catalog without being shelved (status: "PENDING / NOT SET"). This command class owns the stacks domain—bookcase selection, shelf assignment, capacity validation.

Initially placed in `cli/command/book/organize/` subpackage.

**Notable detail:** The shelf capacity check throws `IllegalStateException("Shelf is full")`. There's a TODO to handle multiple copies of the same title—currently assumes unique titles.

---

### PR #154: BookSearchCommands + Rename + Package Flattening

This PR did the most structural work:

**What moved to BookSearchCommands:**
- `searchForBook()` — dispatcher prompting for search type (title/author/ISBN)
- `searchByTitle()` — title lookup with `BookcardRenderer` output
- `searchByAuthor()` — author-based search (currently delegates to checkout prompt)
- `searchByIsbn()` — ISBN barcode scan search

**Additional changes:**
1. **Renamed `BookCommands` → `BookCirculationCommands`** — What remains is purely circulation logic (`checkOutBook`, `askBookCheckOut`). The name now reflects the actual responsibility.

2. **Flattened package structure:**
   - `book/intake/BookCreateCommands.java` → `book/BookCreateCommands.java`
   - `book/organize/BookPlacementCommands.java` → `book/BookPlacementCommands.java`

3. **Cleaned up dead imports** — Removed `BookcardRenderer`, `Logger`, `ShellOption`, `BookMetaDataResponse`, `BookRequestDTO` from the now-slim `BookCirculationCommands`.

**Lines:** -181 from BookCommands, +184 across new files

---

## Final Structure

```
cli/command/book/
├── BookCirculationCommands.java   # check-in/check-out (what remained)
├── BookCreateCommands.java        # cataloging, ISBN scan, author handling
├── BookPlacementCommands.java     # shelf assignment, capacity checks
├── BookSearchCommands.java        # title/author/ISBN lookup
└── ScanMode.java                  # enum for scan vs manual entry
```

Each class now maps to a library domain concept:
- **Cataloging** → intake of new materials
- **Stacks** → physical organization
- **Discovery** → patron search
- **Circulation** → borrowing/returning

## Decisions Worth Noting

**Subpackages → Flat structure:** Initially I created `intake/` and `organize/` subpackages, thinking the hierarchy would help. By PR #154, I flattened them. The subdirectories added navigation friction without meaningful benefit—four files in one package is manageable, and the class names are already descriptive.

**Incremental PRs:** Each extraction was a separate PR. This kept diffs reviewable and made rollback trivial if something broke. The merge commits show the progression: #152 → #153 → #154.

**Rename last:** I held off renaming `BookCommands` until the final extraction. Renaming mid-stream would have created confusing diffs and potential merge conflicts between PRs.

## Dependency Slimming

Comparing constructors before and after:

**BookCommands (before):** 8 dependencies
```java
public BookCommands(ComponentFlow.Builder, AuthorFacade, ShelfFacade, 
    CliPromptService, BookFacade, BookcaseFacade, PromptOptions, BookcardRenderer)
```

**BookCirculationCommands (after):** 7 dependencies (dropped `BookcardRenderer`)
```java
public BookCirculationCommands(ComponentFlow.Builder, AuthorFacade, ShelfFacade,
    CliPromptService, BookFacade, BookcaseFacade, PromptOptions)
```

**BookPlacementCommands:** 4 dependencies
```java
public BookPlacementCommands(BookFacade, ShelfFacade, CliPromptService, PromptOptions)
```

Smaller dependency sets = easier testing, clearer responsibilities.

## Reflection

This felt like preventive maintenance. The class wasn't broken, but it was accumulating mass. Every new book-related feature would have added more methods to an already crowded file.

The domain alignment emerged naturally—library science already has vocabulary for these distinctions (cataloging, stacks, circulation, discovery). Leaning on that made the decomposition feel obvious rather than arbitrary.

**Interview angle:** This is a clean example of "I recognized code smell X (god class), applied refactoring pattern Y (extract class), and validated through Z (smaller dependency sets, domain alignment)."

## Technical Debt Noted

- `searchByAuthor()` currently just delegates to `askBookCheckOut()`—incomplete implementation
- `addToShelf()` has a TODO for handling multiple copies of the same title
- Magic number `0` for "create new author" in disambiguation flow needs constant extraction
- The commented-out `multiBookScan()` method got moved to `BookCreateCommands` but is still dead code
