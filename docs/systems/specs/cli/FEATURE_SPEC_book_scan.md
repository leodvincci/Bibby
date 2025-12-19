# Feature Spec: Book Scan

**Command:** `book new --scan`  
**Module:** CLI / Cataloging  
**Status:** 🟡 Core flow complete, edge cases and hardening in progress  
**Last Updated:** 2025-12-19

---

## Problem

Adding books to a personal library by hand is tedious and error-prone. Users must type title, authors, publisher, publication year, and description — per book. For someone cataloging a full bookshelf (50+ books), this is a significant barrier to adoption.

**Current pain:**
- Manual entry takes 2-3 minutes per book
- Typos and inconsistencies in author names create duplicate author records
- Users abandon cataloging mid-way because it's too slow

---

## Solution

Scan a book's ISBN barcode → fetch metadata automatically → confirm → place on shelf → done.

**User value:** Cataloging drops from 2-3 minutes to ~10 seconds per book.

---

## User Flow

```

┌─────────────────────────────────────────────────────────────────┐
│  1. SCAN/ENTER ISBN                                             │
│     User scans barcode or types ISBN                            │
│     System validates format (ISBN-10 or ISBN-13)                │
│                                                                 │
│         ↓ valid                          ↓ invalid              │
│                                          → Error message        │
│                                          → Re-prompt            │
├─────────────────────────────────────────────────────────────────┤
│  2. FETCH METADATA                                              │
│     System calls Google Books API                               │
│                                                                 │
│         ↓ found                          ↓ not found            │
│                                          → "No results for ISBN"│
│                                          → Exit gracefully      │
├─────────────────────────────────────────────────────────────────┤
│  3. DISPLAY BOOK CARD (pre-save)                                │
│     ┌──────────────────────────────────┐                        │
│     │ 📚 Book Metadata                 │                        │
│     │ ISBN:       978-1234567890       │                        │
│     │ Title:      Domain-Driven Design │                        │
│     │ Authors:    Eric Evans           │                        │
│     │ Published:  2003                 │                        │
│     │ Bookcase:   PENDING              │                        │
│     │ Shelf:      PENDING              │                        │
│     │ Location:   PENDING              │                        │
│     └──────────────────────────────────┘                        │
│     "Add this book? [Y/n]"                                      │
│                                                                 │
│         ↓ confirm                        ↓ cancel               │
│                                          → Exit, no side effects│
├─────────────────────────────────────────────────────────────────┤
│  4. PLACEMENT PROMPTS                                           │
│     → Select location (e.g., "Office", "Living Room")           │
│     → Select bookcase (filtered by location)                    │
│     → Select shelf (filtered by bookcase, shows capacity)       │
│                                                                 │
│         ↓ selected                       ↓ cancel at any step   │
│                                          → Exit, no side effects│
├─────────────────────────────────────────────────────────────────┤
│  5. PERSIST                                                     │
│     → Create missing authors (by name parsing)                  │
│     → Create book record with shelf assignment                  │
│     → Display final book card with actual placement             │
│     ┌──────────────────────────────────┐                        │
│     │ ✓ Book added to library          │                        │
│     │ Bookcase:   Bookcase 1           │                        │
│     │ Shelf:      Shelf 3              │                        │
│     │ Location:   Office               │                        │
│     └──────────────────────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```




---



---





## Acceptance Criteria

### Happy Path
- [ ] User runs `book new --scan`
- [ ] System prompts for ISBN
- [ ] Valid ISBN-10 or ISBN-13 accepted
- [ ] Google Books API called with ISBN
- [ ] Metadata displayed as "book card" with PENDING placement fields
- [ ] User confirms to proceed
- [ ] User selects location → bookcase → shelf
- [ ] Missing authors created automatically (parsed from metadata)
- [ ] Book persisted with correct shelf assignment
- [ ] Final book card displays actual location labels
- [ ] Command exits cleanly

### Cancel Handling (Atomicity)
- [ ] Cancel at confirmation prompt → no records created, clean exit
- [ ] Cancel at location selection → no records created, clean exit
- [ ] Cancel at bookcase selection → no records created, clean exit  
- [ ] Cancel at shelf selection → no records created, clean exit
- [ ] No orphan author records on any cancel path

### Validation & Errors
- [ ] Invalid ISBN format → validation error, re-prompt
- [ ] ISBN not found in Google Books → friendly message, exit
- [ ] Network failure during API call → error message, exit (no crash)
- [ ] Empty/null fields in metadata → handled gracefully (display "Unknown" or skip)

### Duplicate Prevention (Planned)
- [ ] ISBN already exists in library → "Already in library" message with details
- [ ] No new record created for duplicate ISBN
- [ ] User shown existing book's current location

### Shelf Capacity
- [ ] Full shelf indicated in selection UI
- [ ] User can still select full shelf (warning, not blocker) OR
- [ ] User blocked from selecting full shelf (decide on behavior)

---

## Out of Scope (v1)

| Item | Reason | Future? |
|------|--------|---------|
| Multi-scan mode (`--type multi`) | Stub exists, not wired up | v2 |
| AI shelf suggestions | Separate command stub exists | Future feature |
| Manual metadata override | Adds complexity, low value for MVP | Maybe |
| Multiple copies of same ISBN | Requires `BookCopy` concept | If needed |
| Offline mode / cached lookups | Network required for MVP | Future |

---

## Architecture

### Entry Point
```
BookCommands.scanBook(...)
└── src/main/java/com/penrose/bibby/cli/commands/book/BookCommands.java
```

### Collaborators

| Component | Responsibility |
|-----------|----------------|
| `CliPromptService` | ISBN prompt, validation, confirmation prompts |
| `BookFacade` | Metadata lookup, book creation |
| `AuthorFacade` | Author lookup/creation |
| `BookcaseFacade` | Bookcase label lookup |
| `ShelfFacade` | Shelf label lookup, capacity info |
| `PromptOptions` | Generates selection lists (locations, bookcases, shelves) |
| `BookInfoService` | Google Books API integration (WebFlux) |

### Data Flow
```
CLI Layer          Application Layer       Domain Layer         Infrastructure
───────────────────────────────────────────────────────────────────────────────
BookCommands  →    BookFacade         →    Book (domain)    →   BookRepository
     │                  │                       │
     │                  ↓                       │
     │             AuthorFacade        →   Author (domain)  →   AuthorRepository
     │                  │
     ↓                  ↓
CliPromptService   BookInfoService    →   [Google Books API]
```

![Book ISBN Scan to Shelf-2025-12-19-224219](/home/leodpenrose/Downloads/Book ISBN Scan to Shelf-2025-12-19-224219.png)



### Key Technical Decisions

| Decision                            | Rationale                                                                        |
|-------------------------------------|----------------------------------------------------------------------------------|
| Block on reactive call (`.block()`) | Spring Shell is synchronous; blocking is appropriate for CLI context             |
| Author creation as discrete step    | Ensures authors exist before book persistence; cleaner transaction boundaries    |
| Derived placement labels            | Final book card fetches labels via facades rather than storing denormalized data |
| Cancel = first-class outcome        | Prevents orphan records; treats user exit as valid path, not error               |

---

## Open Questions

1. **Shelf full behavior:** Block selection or warn and allow?
2. **Duplicate ISBN behavior:** Show existing book and exit, or offer to update location?
3. **Author name parsing:** Current approach splits on first space — handles "Eric Evans" but not "J.R.R. Tolkien" well. Acceptable for v1?
4. **Multi-scan flow:** What's the UX for scanning 20 books in a row? Batch confirm? Stream placement?

---

## Test Coverage (Target)

| Layer | What to Test | Status |
|-------|--------------|--------|
| Domain | Author name parsing, ISBN validation | 🔴 Not started |
| Application | Facade orchestration, cancel paths | 🔴 Not started |
| Infrastructure | Google Books response mapping | 🔴 Not started |
| Integration | Full scan flow (mocked API) | 🔴 Not started |

---

## Status Tracker

| Milestone | Status |
|-----------|--------|
| Happy path works end-to-end | ✅ Done |
| Book card displays correctly | ✅ Done |
| Placement flow (location → bookcase → shelf) | ✅ Done |
| Author auto-creation | ✅ Done |
| Cancel at any step = no side effects | 🟡 Verify |
| ISBN validation | ✅ Done |
| Google Books not found handling | 🟡 Verify |
| Network error handling | 🔴 Not tested |
| Duplicate ISBN prevention | 🔴 Not started |
| Unit tests | 🔴 Not started |

---

## Interview Talking Points

> **On defining the feature:**  
> "Before building, I wrote a spec with clear acceptance criteria — not just the happy path, but cancel handling, error cases, and architectural boundaries. This let me know when 'done' meant done."

> **On atomicity:**  
> "I treat cancel as a first-class outcome. If a user exits mid-flow, no orphan records get created. This required thinking through transaction boundaries early."

> **On external API integration:**  
> "The scan feature integrates with Google Books using Spring WebFlux. Even though the CLI is synchronous, I used the reactive client for modern Spring alignment — I just block at the CLI layer where it's appropriate."

> **On architectural boundaries:**  
> "The CLI layer talks to facades, never directly to repositories. This keeps the orchestration logic testable and means I could swap the CLI for a REST API without touching domain logic."

---

## Related Documents

- [ ] Devlog: Scan feature implementation
- [x] GitHub Issue: Prevent Duplicate Books by ISBN
- [ ] Technical Debt: Author name parsing improvements
