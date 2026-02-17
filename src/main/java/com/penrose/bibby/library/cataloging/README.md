# Cataloging Bounded Context

## Purpose

The **Cataloging** bounded context is the source of truth for **what a book is** in Bibby: its identity, bibliographic metadata, authorship, and availability status. It answers questions like "What book is this?", "Who wrote it?", and "What metadata do we know about it?"

This context also owns ISBN-based metadata enrichment via the Google Books API.

------

## Modules

### Book (`book/`)

The primary module. Manages book identity, metadata, availability status, and ISBN enrichment.

**Domain model:**
- `Book` — aggregate root with identity (`BookId`), `Title`, `Isbn`, `AvailabilityStatus`, author references (`AuthorRef`), and bibliographic fields (publisher, edition, genre, publication year, description)
- **Value objects:** `BookId`, `Isbn` (with format validation), `Title`, `AuthorRef`, `AuthorName`, `BookMetaData`, `AvailabilityStatus` (enum: `AVAILABLE`, `CHECKED_OUT`, `RESERVED`, `LOST`, `ARCHIVED`)
- `BookFactory` — creates `Book` domain objects and `BookEntity` persistence objects

**Public API (inbound port):**
- `BookFacade` — the contract other bounded contexts depend on
  - `createNewBook(BookRequestDTO)`, `createBookFromMetaData(...)`
  - `findBookById(Long)`, `findBookByIsbn(String)`, `findBookByTitle(String)`
  - `getBookDetails(Long)` → `BookDetailView`
  - `getBooksForShelf(Long)` → `List<BookSummary>`
  - `getBriefBibliographicRecordsByShelfId(Long)` → `List<BriefBibliographicRecord>`
  - `checkOutBook(BookDTO)`, `checkInBook(String)`
  - `updateTheBooksShelf(BookDTO, Long)`, `updatePublisher(String, String)`
  - `findBookMetaDataByIsbn(String)` → `BookMetaDataResponse`
  - `isDuplicate(String isbn)`
  - `getBooksByAuthorId(Long)` → `List<String>`

**Outbound ports (cross-context communication):**
- `AuthorAccessPort` — find/create authors via the Author module's `AuthorFacade`
- `ShelfAccessPort` — retrieve shelf info via the Stacks context's `ShelfFacade`

**DTOs (public contracts):**
- `BookDTO`, `BookRequestDTO`, `BookSummary`, `BookDetailView`, `BriefBibliographicRecord`, `BookMetaDataResponse`, `BookLocationResponse`, `BookPlacementResponse`, `BookReference`, `BookShelfAssignmentRequest`

**Infrastructure:**
- `BookEntity` — JPA entity (`@ManyToMany` with `AuthorEntity` via `book_authors` join table)
- `BookJpaRepository` — Spring Data JPA with custom native SQL for `BookDetailView`
- `BookDomainRepositoryImpl` — bridges `BookDomainRepository` to JPA
- `BookMapper`, `BookMapperTwo` — map between domain, entity, and DTO layers
- `IsbnLookupService` — reactive (`Mono<GoogleBooksResponse>`) Google Books API client
- `IsbnEnrichmentService` — enriches book data from API responses
- External DTOs: `GoogleBooksResponse`, `GoogleBookItems`, `VolumeInfo`, `BookImportRequest`

### Author (`author/`)

Manages author identity and naming. Authors are linked to books via a many-to-many relationship.

**Domain model:**
- `Author` — aggregate root with `AuthorId` and `AuthorName` (first/last, validated non-blank)
- `AuthorFactory` — creates `Author` domain objects and `AuthorEntity` persistence objects

**Public API (inbound port):**
- `AuthorFacade` — contract consumed by the Book module and web layer
  - `findOrCreateAuthor(String, String)` — find-or-create pattern
  - `registerAuthor(AuthorDTO)`, `updateAuthor(AuthorDTO)`
  - `saveAuthor(AuthorDTO)`, `saveAllAuthors(List<AuthorDTO>)`
  - `findById(Long)`, `getAuthorById(Long)`
  - `findByBookId(Long)` → `Set<AuthorDTO>`
  - `getAuthorsById(List<String>)` → `Set<AuthorEntity>`
  - `authorExistFirstNameLastName(String, String)`
  - `getAllAuthorsByName(String, String)`

**Infrastructure:**
- `AuthorEntity` — JPA entity (`@ManyToMany(mappedBy = "authors")` inverse side)
- `AuthorJpaRepository` — Spring Data JPA
- `AuthorRepositoryImpl` — bridges `AuthorRepository` to JPA
- `AuthorMapper`, `AuthorMapperTwo` — map between layers
- `AuthorDTO` (record) — includes static factory methods for entity/ref conversion

### Core (`core/`)

Shared domain concepts for the cataloging context, currently housing the Booklist aggregate.

**Domain model:**
- `Booklist` — aggregate root for named book lists, with `BooklistId`, `BooklistName` (validated: max 60 chars, allowed characters), creation/update timestamps, and a `Set<BookIdentifier>` for membership
- **Value objects:** `BooklistId`, `BooklistName` (with validation and whitespace normalization), `BookIdentifier`

------

## Key Rules / Invariants

- A `Book` must have a stable identity (`BookId`)
- If `Isbn` is present, it must be valid: only digits and hyphens, 10-17 characters, max 4 hyphens, no consecutive/leading/trailing hyphens
- `AuthorName` requires non-null, non-blank first and last names
- `BooklistName` must be non-blank, max 60 characters, and only contain allowed characters
- `Booklist.listId` and `createdAt` are immutable after creation
- Other bounded contexts access cataloging through facade ports, not internal domain objects

------

## Ubiquitous Language

- **Book** — a bibliographic item in the user's library (identity + metadata)
- **ISBN** — an industry identifier for an edition of a book (validated/normalized)
- **Title** — the human-readable name of a book
- **Author** — a person credited with creating the work (first name + last name)
- **AuthorRef** — a lightweight reference to an author (ID + name) used within the Book aggregate
- **Enrichment** — augmenting metadata from Google Books using an ISBN
- **AvailabilityStatus** — the current circulation state of a book
- **Booklist** — a user-curated named collection of book references
- **BriefBibliographicRecord** — a compact summary of a book's bibliographic data

------

## Out of Scope

- Physical location and capacity (owned by **Stacks**)
- CLI/UI concerns (handled in the `web` package)

------

## Package Layout

```
cataloging/
├── book/
│   ├── api/
│   │   ├── adapters/          # AuthorAccessPortAdapter, BookFacadeAdapter, ShelfAccessPortAdapter
│   │   ├── dtos/              # BookDTO, BookSummary, BookDetailView, etc.
│   │   └── ports/
│   │       ├── inbound/       # BookFacade
│   │       └── outbound/      # AuthorAccessPort, ShelfAccessPort
│   ├── core/
│   │   ├── application/       # BookService, IsbnEnrichmentService, IsbnLookupService
│   │   └── domain/            # Book, BookFactory, BookDomainRepository, value objects
│   └── infrastructure/
│       ├── entity/            # BookEntity
│       ├── external/          # GoogleBooksResponse, VolumeInfo, etc.
│       ├── mapping/           # BookMapper, BookMapperTwo
│       └── repository/        # BookJpaRepository, BookDomainRepositoryImpl
├── author/
│   ├── api/
│   │   ├── AuthorDTO.java
│   │   └── ports/inbound/     # AuthorFacade
│   ├── core/
│   │   ├── application/       # AuthorService
│   │   └── domain/            # Author, AuthorFactory, AuthorRepository, AuthorId, AuthorName
│   └── infrastructure/
│       ├── adapters/          # AuthorFacadeImpl
│       ├── entity/            # AuthorEntity
│       ├── mapping/           # AuthorMapper, AuthorMapperTwo
│       └── repository/        # AuthorJpaRepository, AuthorRepositoryImpl
└── core/
    └── domain/
        ├── Booklist.java
        └── valueobject/       # BookIdentifier, BooklistId, BooklistName
```

------

## Example Flows

- **Add book by ISBN**
    1. `BookFacade.findBookMetaDataByIsbn(isbn)` — calls `IsbnLookupService` (reactive Google Books API)
    2. `BookFacade.createBookFromMetaData(metadata, authorIds, isbn, shelfId)` — persists via `BookDomainRepositoryImpl`
- **Check out a book**
    1. `BookFacade.checkOutBook(bookDTO)` — delegates to `Book.checkout()` which validates the state transition
- **Get book details for display**
    1. `BookFacade.getBookDetails(bookId)` → `BookDetailView` (native SQL joining books, authors, shelves, bookcases)
