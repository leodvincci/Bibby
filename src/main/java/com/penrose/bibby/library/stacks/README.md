# Stacks Bounded Context

## Purpose

The **Stacks** bounded context manages the **physical organization** of the user's library: bookcases, shelves, and
where books are placed. In library science, "stacks" refers to the shelving area where the collection is physically
stored and arranged.

This context answers questions like "Where is this book?", "How many shelves does this bookcase have?", and "Is there
room on this shelf?"

------

## Modules

### Bookcase (`bookcase/`)

Manages physical bookcase units — their location, labeling, and shelf capacity.

**Domain model:**

- `Bookcase` — domain entity with `bookcaseId`, `bookcaseLabel`, `bookcaseLocation`, and `shelfCapacity` (enforces
  minimum of 1)

**Public API (inbound port):**

- `BookcaseFacade` — contract consumed by the Shelf module and web layer
    - `createNewBookCase(userId, label, zone, zoneIndex, shelfCount, bookCapacity, location)` → `CreateBookcaseResult`
    - `findBookCaseById(Long)` → `Optional<BookcaseDTO>`
    - `getAllBookcases()` → `List<BookcaseDTO>`
    - `getAllBookcaseLocations()` → `List<String>`
    - `getAllBookcasesByLocation(String)` → `List<BookcaseDTO>`
    - `getAllBookcasesByUserId(Long)` → `List<BookcaseDTO>`
    - `deleteBookcase(Long)` — cascade deletes shelves

**DTOs (public contracts):**

- `BookcaseDTO` (record) — `bookcaseId`, `bookcaseLabel`, `shelfCapacity`, `bookCapacity`, `location`; includes
  `fromEntity()` factory method
- `CreateBookcaseRequest` (record) — `location`, `zone`, `indexId`, `shelfCount`, `shelfCapacity`
- `CreateBookcaseResult` (record) — `bookcaseId`

**Infrastructure:**

- `BookcaseEntity` — JPA entity with zone-based label generation (`zone:zoneIndex`), `userId` owner reference, and
  `bookCapacity` (computed from shelf count * per-shelf capacity)
- `BookcaseJpaRepository` — Spring Data JPA with queries by label, location, and user
- `BookcaseRepository` — domain repository interface (outbound port)
- `BookcaseRepositoryImpl` — adapter bridging domain repository to JPA
- `BookcaseMapper` — entity-to-domain mapping (work in progress)

### Shelf (`shelf/`)

Manages individual shelves within bookcases, including capacity tracking and book placement.

**Domain model:**

- `Shelf` — aggregate with `ShelfId` (value object), `shelfPosition`, `bookCapacity`, `books`, and business logic for
  capacity (`isFull()`, `getBookCount()`)
- `ShelfFactory` — creates `ShelfEntity` instances with position, label, and capacity
- `ShelfDomainRepository` — domain repository port (`getById(ShelfId)`, `save(Shelf)`)
- **Value objects:** `ShelfId`

**Public API (inbound port):**

- `ShelfFacade` — contract consumed by the Cataloging context and web layer
    - `findShelfById(Long)` → `Optional<ShelfDTO>`
    - `findByBookcaseId(Long)` → `List<ShelfDTO>`
    - `getAllDTOShelves(Long bookcaseId)` → `List<ShelfDTO>`
    - `findBooksByShelf(Long)` → `List<BookDTO>`
    - `getShelfSummariesForBookcase(Long)` → `List<ShelfSummary>`
    - `deleteAllShelvesInBookcase(Long)` — cascade deletes books on those shelves
    - `isFull(ShelfDTO)` → `Boolean`

**Use cases:**

- `BrowseShelfUseCase` — returns `List<BriefBibliographicRecord>` for a shelf by delegating to `BookFacade` (
  cross-context dependency on Cataloging)

**DTOs (public contracts):**

- `ShelfDTO` (record) — `shelfId`, `shelfLabel`, `bookcaseId`, `shelfPosition`, `bookCapacity`, `shelfDescription`,
  `books`; includes `fromEntity()` and `fromEntityWithBookId()` factory methods
- `ShelfSummary` (record) — `shelfId`, `label`, `bookCount` (designed for JPQL aggregation queries)
- `ShelfOptionResponse` (record) — `shelfId`, `shelfLabel`, `bookcaseLabel`, `bookCapacity`, `bookCount`, `hasSpace` (
  computed availability for UI dropdowns)

**Infrastructure:**

- `ShelfEntity` — JPA entity with `bookcaseId` foreign key, position ordering, and capacity
- `ShelfJpaRepository` — Spring Data JPA with custom JPQL for `ShelfSummary` aggregation
- `ShelfDomainRepositoryImpl` — adapter bridging domain repository to JPA, enriches shelf with book data from Cataloging
  context
- `ShelfMapper` — entity-to-domain mapping with `ShelfId` value object construction

------

## Key Rules / Invariants

- A bookcase must have at least 1 shelf (`shelfCapacity >= 1`)
- Bookcase labels must be unique (duplicate creation returns HTTP 409)
- A shelf is full when its book count reaches `bookCapacity`
- Deleting a bookcase cascades to its shelves; deleting shelves cascades to removing books on those shelves
- Bookcase creation automatically generates the specified number of shelves via `ShelfFactory`

------

## Ubiquitous Language

- **Bookcase** — a physical furniture unit that holds shelves
- **Shelf** — a single level within a bookcase that holds books
- **Shelf Capacity** — the number of shelves a bookcase can hold
- **Book Capacity** — the maximum number of books a shelf can hold
- **Zone** — a named area grouping bookcases (e.g., "Living Room", "Office")
- **Zone Index** — an identifier for a specific bookcase within a zone
- **Shelf Position** — the ordering of a shelf within its bookcase
- **Shelf Summary** — a lightweight view of a shelf with its book count
- **Shelf Option** — a shelf selection choice showing availability (used in UI forms)

------

## Out of Scope

- Book identity and metadata (owned by **Cataloging**)
- User management and authentication

------

## Cross-Context Dependencies

- **Stacks depends on Cataloging:**
    - `BrowseShelfUseCase` uses `BookFacade` to get bibliographic records for books on a shelf
    - `ShelfDomainRepositoryImpl` uses `BookDomainRepository` to enrich shelf domain objects with book IDs
    - `ShelfService` uses `BookJpaRepository` for book counts and cascade deletes
- **Cataloging depends on Stacks:**
    - `ShelfAccessPort` / `ShelfAccessPortAdapter` lets the Book module retrieve shelf information via `ShelfFacade`

------

## Package Layout

```
stacks/
├── bookcase/
│   ├── api/
│   │   ├── CreateBookcaseResult.java
│   │   ├── dtos/              # BookcaseDTO, CreateBookcaseRequest
│   │   └── ports/inbound/     # BookcaseFacade
│   ├── core/
│   │   ├── application/       # BookcaseService
│   │   └── domain/            # Bookcase
│   └── infrastructure/
│       ├── adapter/outbound/  # BookcaseRepositoryImpl
│       ├── entity/            # BookcaseEntity
│       ├── mapping/           # BookcaseMapper
│       └── repository/        # BookcaseJpaRepository, BookcaseRepository
└── shelf/
    ├── api/
    │   ├── dtos/              # ShelfDTO, ShelfSummary, ShelfOptionResponse
    │   └── ports/inbound/     # ShelfFacade
    ├── core/
    │   ├── application/       # ShelfService, BrowseShelfUseCase
    │   └── domain/            # Shelf, ShelfFactory, ShelfDomainRepository
    │       └── valueobject/   # ShelfId
    └── infrastructure/
        ├── adapter/outbound/  # ShelfDomainRepositoryImpl
        ├── entity/            # ShelfEntity
        ├── mapping/           # ShelfMapper
        └── repository/        # ShelfJpaRepository
```

------

## Example Flows

- **Create a bookcase**
    1. `BookcaseFacade.createNewBookCase(...)` — validates label uniqueness, creates `BookcaseEntity`
    2. Automatically generates N shelves via `ShelfFactory` based on `shelfCount`
    3. Returns `CreateBookcaseResult` with the new bookcase ID
- **Browse a shelf**
    1. `BrowseShelfUseCase.browseShelf(shelfId)` — calls `BookFacade.getBriefBibliographicRecordsByShelfId()`
    2. Returns `List<BriefBibliographicRecord>` with title, authors, edition, publisher, ISBN
- **Check if a shelf has room**
    1. `ShelfFacade.isFull(shelfDTO)` — maps to `Shelf.isFull()` which compares book count against capacity
