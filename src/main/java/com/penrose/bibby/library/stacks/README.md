# Stacks Bounded Context

## Purpose

The **Stacks** bounded context manages the **physical organization** of the user's library: bookcases, shelves, and
where books are placed. In library science, "stacks" refers to the shelving area where the collection is physically
stored and arranged.

This context answers questions like "Where is this book?", "How many shelves does this bookcase have?", and "Is there
room on this shelf?"

------

## Architecture

Both sub-modules follow **hexagonal architecture** (ports & adapters). Each service class implements a facade interface
(inbound port) and delegates every operation to a dedicated use-case class. Domain objects never cross the application
boundary — inbound port models or DTOs are used instead.

------

## Modules

### Bookcase (`bookcase/`)

Manages physical bookcase units — their location, labeling, and shelf capacity.

**Domain model:**

- `Bookcase` — plain domain entity with `bookcaseId`, `userId`, `bookcaseLocation`, `bookcaseZone`, `bookcaseIndex`,
  `shelfCapacity` (clamped to minimum 1), and `bookCapacity` (computed from shelf count * per-shelf capacity)

**Public API (inbound port):**

- `BookcaseFacade` — contract consumed by the Shelf module and web layer
    - `createNewBookCase(userId, label, zone, zoneIndex, shelfCount, bookCapacity, location)` → `CreateBookcaseResult`
    - `findBookCaseById(Long)` → `Bookcase`
    - `findById(Long)` → `Bookcase`
    - `getAllBookcases()` → `List<Bookcase>`
    - `getAllBookcaseLocations()` → `List<String>`
    - `getAllBookcasesByLocation(String)` → `List<Bookcase>`
    - `getAllBookcasesByUserId(Long)` → `List<Bookcase>`
    - `deleteBookcase(Long)` — cascades to shelves

**Use cases:**

- `CreateBookcaseUseCase` — validates label uniqueness (throws HTTP 409 on conflict), persists the bookcase, then
  auto-creates N shelves via `ShelfAccessPort`
- `DeleteBookcaseUseCase` — cascades deletion: shelves first via `ShelfAccessPort`, then the bookcase itself
  (`@Transactional`)
- `QueryBookcaseUseCase` — thin delegation to `BookcaseRepository` for all read operations

**DTOs (public contracts):**

- `BookcaseDTO` (record) — `bookcaseId`, `shelfCapacity`, `bookCapacity`, `location`, `zone`, `index`
- `CreateBookcaseRequest` (record) — `location`, `zone`, `indexId`, `shelfCount`, `shelfCapacity`
- `CreateBookcaseResult` (record) — `bookcaseId`

**Outbound ports:**

- `BookcaseRepository` — domain repository port for bookcase persistence
- `ShelfAccessPort` — allows the bookcase module to create/delete shelves without a direct dependency on the shelf module

**Infrastructure:**

- `BookcaseEntity` — JPA entity (`@Table("bookcases")`) with `GenerationType.AUTO`
- `BookcaseJpaRepository` — Spring Data JPA with queries by label, location, and user
- `BookcaseRepositoryImpl` — adapter bridging `BookcaseRepository` port to JPA
- `ShelfAccessPortAdapter` — bridges `ShelfAccessPort` to `ShelfFacade`
- `BookcaseMapper` (in `core/domain/`) — static methods for entity-to-domain and domain-to-DTO mapping

------

### Shelf (`shelf/`)

Manages individual shelves within bookcases, including capacity tracking and book placement.

**Domain model:**

- `Shelf` — aggregate root with `ShelfId` (value object), `shelfLabel`, `shelfPosition`, `bookCapacity`,
  `books` (list of book IDs), and `bookcaseId`. Business logic: `isFull()`, `getBookCount()`. Setters enforce
  invariants (label not blank, position >= 1, bookCapacity >= 1, bookcaseId not null).
- `Placement` — domain model representing the assignment of a book to a shelf. Fields: `bookId`, `shelfId`.
  Null-safe getters throw `IllegalStateException`; setters throw `IllegalArgumentException` for nulls.
- **Value objects:** `ShelfId` (record wrapping `Long shelfId`)

**Public API (inbound port):**

- `ShelfFacade` — contract consumed by the Bookcase module and web layer
    - `findShelvesByBookcaseId(Long)` → `List<ShelfResponse>`
    - `findShelfById(Long)` → `Optional<ShelfResponse>`
    - `getShelfSummariesForBookcaseByBookcaseId(Long)` → `List<ShelfSummaryResponse>`
    - `createShelfInBookcaseByBookcaseId(bookcaseId, position, label, bookCapacity)` → void
    - `deleteAllShelvesInBookcaseByBookcaseId(Long)` → void
    - `findAll()` → `List<ShelfResponse>`
    - `isFull(Long shelfId)` → boolean (throws `IllegalStateException` if not found)
    - `isEmpty(Long shelfId)` → boolean (throws `IllegalStateException` if not found)
    - `placeBookOnShelf(Long bookId, Long shelfId)` → void

**Inbound port models:**

- `ShelfResponse` (record) — `id`, `shelfPosition`, `shelfLabel`, `bookCapacity`, `bookIds`, `bookcaseId`
- `ShelfSummaryResponse` (record) — `shelfId`, `label`, `bookCount` (lightweight projection)
- `ShelfPortModelMapper` — static utility mapping `Shelf` → `ShelfResponse`

**Use cases:**

- `CreateShelfUseCase` — creates a `Shelf` with a null `ShelfId` (JPA assigns it) and persists via
  `ShelfDomainRepositoryPort`
- `DeleteShelvesUseCase` — `@Transactional`; deletes books on shelves first via `BookAccessPort`, then deletes shelves
  by bookcase ID
- `QueryShelfUseCase` — all read operations; maps domain objects to `ShelfResponse`/`ShelfSummaryResponse` via
  `ShelfPortModelMapper`
- `PlaceBookOnShelfUseCase` — validates the book exists via `BookAccessPort.getBookById()` (throws
  `IllegalArgumentException` if null), then creates and persists a `Placement`

**Outbound ports:**

- `ShelfDomainRepositoryPort` — `getShelfByShelfId(ShelfId)`, `createNewShelfInBookcase(Shelf)`,
  `deleteByBookcaseId(Long)`, `findByBookcaseId(Long)`, `findAll()`
- `BookAccessPort` — interface for querying the Cataloging context: `getBookIdsByShelfId(Long)`,
  `deleteBooksOnShelves(List<Long>)`, `getBookById(Long)`
- `PlacementRepositoryPort` — `placeBookOnShelf(Placement)` for persisting book-to-shelf assignments

**DTOs (public contracts):**

- `ShelfDTO` (record) — `shelfId`, `shelfLabel`, `bookcaseId`, `shelfPosition`, `bookCapacity`, `bookIds`
- `ShelfOptionResponse` (record) — `shelfId`, `shelfLabel`, `bookCapacity`, `bookCount`, `hasSpace` (computed
  availability for UI dropdowns)

**Infrastructure:**

- `ShelfEntity` — JPA entity (`@Table("shelves")`) with `bookcaseId`, `shelfPosition`, `bookCapacity`
- `PlacementEntity` — JPA entity (`@Table("placements")`) with `bookId`, `shelfId`; uses `GenerationType.IDENTITY`
- `ShelfJpaRepository` — Spring Data JPA with `findByBookcaseId`, `deleteByBookcaseId`
- `PlacementJpaRepository` — Spring Data JPA for placements
- `ShelfDomainRepositoryPortImpl` — adapter bridging `ShelfDomainRepositoryPort` to JPA; enriches shelves with book IDs
  from `BookAccessPort`
- `BookAccessPortAdapter` — implements `BookAccessPort` by delegating to Cataloging's `BookFacade`
- `PlacementRepositoryPortImpl` — adapter bridging `PlacementRepositoryPort` to JPA
- `ShelfMapper` (`@Component`) — bidirectional mapping between entity, domain, and DTO
- `PlacementMapper` — static mapping from `Placement` domain model to `PlacementEntity`

------

## Key Rules / Invariants

- A bookcase must have at least 1 shelf (`shelfCapacity >= 1`, silently clamped)
- Bookcase labels must be unique (duplicate creation returns HTTP 409)
- A shelf's position must be >= 1, label must not be blank, and `bookCapacity` must be >= 1
- A shelf is full when its book count reaches `bookCapacity`
- A placement requires both a valid book (verified via `BookAccessPort`) and a shelf
- Deleting a bookcase cascades to its shelves; deleting shelves cascades to removing books on those shelves
- Bookcase creation automatically generates the specified number of shelves via `ShelfAccessPort`

------

## Ubiquitous Language

- **Bookcase** — a physical furniture unit that holds shelves
- **Shelf** — a single level within a bookcase that holds books
- **Placement** — the assignment of a specific book to a specific shelf
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

Dependencies between Stacks and Cataloging are managed through ports to keep the architecture clean:

- **Shelf → Cataloging:** `BookAccessPortAdapter` (in shelf infrastructure) implements `BookAccessPort` by delegating
  to Cataloging's `BookFacade`. Uses lazy resolution (`ObjectProvider`) to break a circular Spring bean dependency.
  Methods: `getBookById`, `getBookIdsByShelfId`, `deleteBooksOnShelves`.
- **Bookcase → Shelf:** `ShelfAccessPortAdapter` (in bookcase infrastructure) implements the bookcase module's
  `ShelfAccessPort` by delegating to `ShelfFacade`. This lets the bookcase module create or delete shelves without a
  hard compile-time coupling.

------

## Package Layout

```
stacks/
├── README.md
├── bookcase/
│   ├── api/
│   │   ├── CreateBookcaseResult.java
│   │   └── dtos/                        # BookcaseDTO, CreateBookcaseRequest
│   ├── core/
│   │   ├── application/
│   │   │   ├── BookcaseService.java     # Facade implementation (@Service)
│   │   │   └── usecases/               # CreateBookcaseUseCase, DeleteBookcaseUseCase, QueryBookcaseUseCase
│   │   ├── domain/
│   │   │   ├── BookcaseMapper.java      # Static domain-layer mapper
│   │   │   └── model/                   # Bookcase
│   │   └── ports/
│   │       ├── inbound/                 # BookcaseFacade
│   │       └── outbound/               # BookcaseRepository, ShelfAccessPort
│   └── infrastructure/
│       ├── adapter/outbound/            # BookcaseRepositoryImpl, ShelfAccessPortAdapter
│       ├── entity/                      # BookcaseEntity
│       ├── mapping/                     # BookcaseMapper (stub — real one is in core/domain/)
│       └── repository/                  # BookcaseJpaRepository
└── shelf/
    ├── api/
    │   └── dtos/                        # ShelfDTO, ShelfOptionResponse
    ├── core/
    │   ├── application/
    │   │   ├── ShelfService.java        # Facade implementation (@Service)
    │   │   └── usecases/               # CreateShelfUseCase, DeleteShelvesUseCase, PlaceBookOnShelfUseCase, QueryShelfUseCase
    │   ├── domain/
    │   │   ├── model/                   # Shelf (aggregate root), Placement
    │   │   └── valueobject/             # ShelfId
    │   └── ports/
    │       ├── inbound/
    │       │   ├── ShelfFacade.java
    │       │   └── inboundPortModels/   # ShelfResponse, ShelfSummaryResponse, ShelfPortModelMapper
    │       └── outbound/               # ShelfDomainRepositoryPort, BookAccessPort, PlacementRepositoryPort
    └── infrastructure/
        ├── adapter/outbound/            # ShelfDomainRepositoryPortImpl, BookAccessPortAdapter, PlacementRepositoryPortImpl
        ├── entity/                      # ShelfEntity, PlacementEntity
        ├── mapping/                     # ShelfMapper, PlacementMapper
        └── repository/                  # ShelfJpaRepository, PlacementJpaRepository
```

------

## Example Flows

- **Create a bookcase**
    1. `BookcaseFacade.createNewBookCase(...)` → `CreateBookcaseUseCase` validates label uniqueness, persists the
       bookcase
    2. Automatically creates N shelves via `ShelfAccessPort.createShelf()` based on `shelfCount`
    3. Returns `CreateBookcaseResult` with the new bookcase ID

- **Place a book on a shelf**
    1. `ShelfFacade.placeBookOnShelf(bookId, shelfId)` → `PlaceBookOnShelfUseCase`
    2. Validates the book exists via `BookAccessPort.getBookById()` (throws `IllegalArgumentException` if not found)
    3. Creates a `Placement` domain object and persists it via `PlacementRepositoryPort`

- **Check if a shelf has room**
    1. `ShelfFacade.isFull(shelfId)` → `QueryShelfUseCase` fetches the shelf, maps to domain
    2. `Shelf.isFull()` compares `books.size()` against `bookCapacity`

- **Delete a bookcase**
    1. `BookcaseFacade.deleteBookcase(bookcaseId)` → `DeleteBookcaseUseCase`
    2. Cascades: deletes shelves first via `ShelfAccessPort.deleteAllShelvesInBookcase()`
    3. `DeleteShelvesUseCase` deletes books on those shelves via `BookAccessPort`, then deletes the shelves
    4. Finally deletes the bookcase itself