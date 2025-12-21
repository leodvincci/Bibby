# Devlog: Splitting Scan vs Manual Book Creation Workflows

**Date:** 2025-12-20  
**PR:** #157  
**Theme:** Workflow Separation / Dependency Reduction

## Context

Following the earlier decomposition of `BookCommands` into focused command classes, `BookCreateCommands` was still doing double-duty: handling both ISBN barcode scanning (with Google Books API integration) and manual book entry. These are two distinct user workflows with different dependency requirements.

Scan-based creation needs:
- `BookcardRenderer` to display fetched metadata
- `BookcaseFacade` and `ShelfFacade` for immediate shelf placement
- `PromptOptions` for location selection
- Metadata-to-author mapping logic

Manual creation needs:
- Prompts for title, ISBN, author count
- Interactive author entry with disambiguation

This PR separates them.

---

## Commit 1: `bf6347c` — Create BookCreateScanCommands

**Added:** `BookCreateScanCommands` class in `cli/command/book/create/` subpackage

This new class encapsulates the entire scan-based book intake workflow:

```java
@ShellComponent
@Command(command = "book", group = "Book Create Commands")
public class BookCreateScanCommands {
    // 7 dependencies for scan workflow
    private final CliPromptService cliPrompt;
    private final BookcardRenderer bookcardRenderer;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;
    private final ShelfFacade shelfFacade;
    private final AuthorFacade authorFacade;
    private final PromptOptions promptOptions;
```

**What moved here:**

| Method | Purpose |
|--------|---------|
| `createBookScan()` | Main `@Command` endpoint for `book scan` |
| `scanBook()` | Prompts for ISBN, fetches metadata, displays book card |
| `createAuthorsFromMetaData()` | Parses author names from API response |
| `mapAuthorStringToDTO()` | Converts "FirstName LastName" string to DTO |
| `createNewAuthorOrAddExisting()` | Handles author disambiguation when names collide |
| `saveNewAuthor()` | Wrapper around `authorFacade.saveAuthor()` |
| `authorExists()` | Wrapper around `authorFacade.authorExistFirstNameLastName()` |

The scan workflow has a specific shape: ISBN → API lookup → display card → confirm → select location → select bookcase → select shelf → create authors → create book. All of that logic now lives together.

---

## Commit 2: `26ac6cb` — Slim Down BookCreateCommands

**Removed:** 242 lines and 4 dependencies from `BookCreateCommands`

**Before:**
```java
public BookCreateCommands(
    CliPromptService cliPrompt, 
    BookcardRenderer bookcardRenderer,      // removed
    BookFacade bookFacade, 
    BookcaseFacade bookcaseFacade,           // removed
    ShelfFacade shelfFacade,                 // removed
    AuthorFacade authorFacade, 
    PromptOptions promptOptions)             // removed
```

**After:**
```java
public BookCreateCommands(
    CliPromptService cliPrompt, 
    BookFacade bookFacade, 
    AuthorFacade authorFacade)
```

**7 dependencies → 3 dependencies.**

What remains in `BookCreateCommands`:
- `registerBook()` — the entry point that dispatches based on `ScanMode`
- `createBookManually()` — prompts for title, authors, ISBN
- `createAuthors()` — interactive author entry with disambiguation

The class name still makes sense: it handles book creation, just the manual variant. The scan variant lives in `BookCreateScanCommands`.

**Also in this commit:** Flattened the package structure. `BookCreateScanCommands` moved from `book/create/` subpackage back to `book/`. Same pattern as the earlier refactoring—subdirectories weren't adding value for just two files.

---

## Commit 3: `346007b` — Merge PR #157

Clean merge, no conflicts.

---

## Final Structure

```
cli/command/book/
├── BookCirculationCommands.java    # check-in/check-out
├── BookCreateCommands.java         # manual entry (3 deps)
├── BookCreateScanCommands.java     # ISBN scan flow (7 deps)
├── BookPlacementCommands.java      # shelf assignment
├── BookSearchCommands.java         # title/author/ISBN lookup
└── ScanMode.java                   # enum for routing
```

---

## Why This Matters

**Dependency honesty.** The original `BookCreateCommands` claimed to need 7 collaborators, but manual entry only actually uses 3. The constructor was lying about the class's true dependencies. Now each class declares exactly what it needs.

**Testability.** Testing manual book creation no longer requires mocking `BookcardRenderer`, `BookcaseFacade`, or `ShelfFacade`. Those are irrelevant to that workflow.

**Workflow clarity.** A developer reading `BookCreateScanCommands` knows they're looking at the barcode/API flow. A developer reading `BookCreateCommands` knows they're looking at manual entry. No mental filtering required.

---

## Technical Debt Noted

The `registerBook()` switch statement now has a commented-out case:

```java
switch(mode){
    // case SINGLE -> createBookScan(false);  // commented out
    // case MULTI -> multiBookScan();
    case NONE -> createBookManually();
}
```

This breaks the routing—`book new --scan` won't dispatch to the new class. The fix is either:
1. Inject `BookCreateScanCommands` and delegate, or
2. Remove the `--scan` flag from `registerBook()` since `book scan` is now a separate command

Also, the `multiBookScan()` method is still commented out and dead. Should be implemented or deleted.

---

## Reflection

This felt like finishing a thought. The earlier refactoring separated creation from search/placement/circulation, but left two distinct workflows (scan vs manual) sharing a class. That worked, but the dependency list was a signal that something was still bundled together.

The pattern I'm learning: **if a class needs dependencies for only some of its methods, those methods might want their own class.**
