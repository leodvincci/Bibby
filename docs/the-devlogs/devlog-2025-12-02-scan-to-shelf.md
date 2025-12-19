# Dev Log: Scan-to-Shelf Workflow Implementation

**Date:** December 2, 2025  
**Feature:** Complete scan-to-shelf book import workflow  
**Tools Used:** OpenAI Codex (frontend), manual implementation (backend)

---

## Summary

Implemented end-to-end workflow for scanning a book barcode, fetching metadata from Google Books API, persisting to the database, and assigning the book to a shelf. This feature works both via the CLI and the new web-based scanner UI.

---

## Changes by Commit

### 1. refactor(book): rename BookEnrichmentService to IsbnEnrichmentService

**Files:** `IsbnEnrichmentService.java`

The original name was too generic. The service's single responsibility is enriching book data from an ISBN lookup, so the new name reflects that. Also updated the method signature from `enrichBookData()` returning `Book` to `enrichIsbn()` returning `BookEntity` — this makes more sense since the caller typically wants to persist or return the entity, not the domain object.

Added null-check validation on the `GoogleBooksResponse` input and made `wrapText()` null-safe.

---

### 2. feat(api): add endpoint to place book on shelf

**Files:** `BookController.java`, `BookPlacementResponse.java` (new), `BookShelfAssignmentRequest.java` (new)

```
POST api/v1/books/{bookId}/shelf
Body: { "shelfId": 123 }
```

New endpoint to assign a book to a shelf after import. Returns the updated book info plus shelf/bookcase labels for UI confirmation. Error handling maps domain exceptions to appropriate HTTP status codes:

- `IllegalArgumentException` → 404 NOT_FOUND (book or shelf doesn't exist)
- `IllegalStateException` → 409 CONFLICT (shelf is full)

---

### 3. feat(api): implement complete book import workflow

**Files:** `BookImportController.java`, `BookImportResponse.java` (new)

```
POST /import/books
Body: { "isbn": "9780134685991" }
```

Previously this endpoint just logged the ISBN. Now it:

1. Validates input
2. Calls Google Books API via `BookInfoService`
3. Enriches and persists via `IsbnEnrichmentService`
4. Returns full book details including parsed author names

This is the primary endpoint the frontend scanner uses.

---

### 4. feat(api): add shelf options endpoint for UI dropdown

**Files:** `ShelfController.java`, `ShelfService.java`, `BookRepository.java`, `ShelfOptionResponse.java` (new)

```
GET /api/v1/shelves/options
```

Returns all shelves with:
- `shelfId`, `shelfLabel`, `bookcaseLabel`
- `bookCapacity`, `bookCount`, `hasSpace`

The frontend uses this to populate a dropdown and disable full shelves. Added `BookRepository.countByShelfId()` to support the occupancy calculation.

---

### 5. feat(service): add book-to-shelf assignment with capacity check

**Files:** `BookService.java`

New method `assignBookToShelf(Long bookId, Long shelfId)`:

1. Validates book exists
2. Validates shelf exists
3. Checks current occupancy against capacity
4. Throws `IllegalStateException` if full
5. Updates and persists the book

This encapsulates the business logic so the controller stays thin.

---

### 6. feat(domain): add factory and mapper methods for Google Books JSON

**Files:** `BookFactory.java`, `BookMapperTwo.java`, `AuthorService.java`

Added support for creating domain objects directly from Google Books API responses:

- `BookFactory.createBookDomainFromJSON()` — creates `Book` domain object with all fields populated
- `BookMapperTwo.toDomainFromJSON()` — parses `GoogleBooksResponse`, splits author names into first/last, builds domain model

Also added `AuthorService.saveAuthor(AuthorEntity)` convenience method.

**Note:** Author name splitting uses simple `split(" ", 2)` which won't handle all edge cases (suffixes, middle names, etc.). Good enough for now.

---

### 7. feat(cli): add shelf placement command and scan result display

**Files:** `BookCommands.java`, `CliPromptService.java`

Added CLI-side equivalents of the web workflow:

- `scanToShelf(GoogleBooksResponse)` — prompts for bookcase → shelf, validates capacity, assigns book
- `addScanResultCommand()` — formatted display of scanned book metadata
- `promptBookConfirmation()` — yes/no prompt before adding to library

Removed unused `BookController` dependency from `BookCommands`.

---

### 8. feat(ui): redesign barcode scanner interface

**Files:** `index.html`

**Implemented by:** OpenAI Codex

Complete UI overhaul:
- Dark theme with card-based layout
- Two-panel responsive grid: camera feed + book details
- Book details section shows title, authors (as pills), publisher, description
- Shelf selection dropdown with capacity indicators
- "Place book on shelf" action button
- Visual states for empty/loading/populated

---

### 9. feat(ui): implement book import and shelf placement workflow

**Files:** `script.js`

**Implemented by:** OpenAI Codex

Rewrote the scanner JavaScript:

- `importBook(isbn)` — POST to `/import/books`, display returned metadata
- `fetchShelfOptions()` — GET shelf dropdown data, render with availability
- `placeOnShelf()` — POST to assign book to selected shelf
- `showBookDetails()` / `formatAuthors()` — UI helpers
- Updated `handleScan()` to use new import flow
- Added event listeners for shelf selection changes and place button

---

## Architecture Notes

**Request/Response DTOs Added:**
- `BookImportRequest` (existing) — `{ isbn }`
- `BookImportResponse` (new) — `{ bookId, title, isbn, authors[], publisher, description }`
- `BookShelfAssignmentRequest` (new) — `{ shelfId }`
- `BookPlacementResponse` (new) — `{ bookId, title, shelfId, shelfLabel, bookcaseLabel }`
- `ShelfOptionResponse` (new) — `{ shelfId, shelfLabel, bookcaseLabel, bookCapacity, bookCount, hasSpace }`

**Flow:**
```
Scan barcode
    ↓
POST /import/books { isbn }
    → Google Books API lookup
    → Persist book + authors
    ← BookImportResponse
    ↓
GET /api/v1/shelves/options
    ← ShelfOptionResponse[]
    ↓
User selects shelf
    ↓
POST /api/v1/books/{id}/shelf { shelfId }
    → Capacity check
    → Update book.shelfId
    ← BookPlacementResponse
```

---

## What I Learned

1. **Codex for frontend iteration** — Using Codex to rapidly iterate on the UI let me focus on the backend logic. The generated code needed no modifications and integrated cleanly with the new API endpoints.

2. **DTOs as API contracts** — Having explicit request/response DTOs made the frontend-backend integration straightforward. The frontend knows exactly what to send and what it'll receive.

3. **Capacity as derived state** — Shelf capacity is checked by counting books at assignment time rather than storing a "current count" field. This avoids synchronization issues when books are moved or deleted.

4. **Service layer for business rules** — Putting the capacity validation in `BookService.assignBookToShelf()` keeps the controller thin and makes the rule testable in isolation.

---

## Next Steps

- [ ] Add error toasts/notifications in the UI instead of just status text
- [ ] Handle duplicate ISBN imports (currently creates duplicates)
- [ ] Add loading spinners during API calls
- [ ] Write tests for `assignBookToShelf()` capacity logic
- [ ] Consider WebSocket for real-time shelf updates if multiple users scan simultaneously

---

## Commits (9 total)

1. `refactor(book): rename BookEnrichmentService to IsbnEnrichmentService`
2. `feat(api): add endpoint to place book on shelf`
3. `feat(api): implement complete book import workflow`
4. `feat(api): add shelf options endpoint for UI dropdown`
5. `feat(service): add book-to-shelf assignment with capacity check`
6. `feat(domain): add factory and mapper methods for Google Books JSON`
7. `feat(cli): add shelf placement command and scan result display`
8. `feat(ui): redesign barcode scanner interface`
9. `feat(ui): implement book import and shelf placement workflow`
