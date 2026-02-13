# Catalog Subdomain

## Purpose

The **Catalog** subdomain exists to define and manage **what a book is** in Bibby: its **identity** and **bibliographic metadata** (title, authors, ISBN, edition, etc.). Catalog is the source of truth for book/author meaning in the **Personal Book Library Management** domain.

Catalog answers questions like: "What book is this?" and "What metadata do we know about it?"

------

## In Scope

- Book identity (e.g., `BookId`, `Isbn`)
- Bibliographic metadata (title, subtitle, authors, edition, publisher, publication date, etc.)
- Author identity and naming
- ISBN lookup / metadata enrichment (e.g., Google Books integration) **as an outbound adapter**
- Validations around bibliographic concepts (ISBN format, required fields, normalization)

------

## Out of Scope

- Physical location and capacity (owned by **Storage/Placement**)
- User-defined categorization like tags and booklists (owned by **Organization**)
- Search/ranking/query UX (owned by **Discovery**)
- Persistence schemas as "truth" (JPA entities are implementation details)
- CLI/UI concerns (handled in adapters)

------

## Ubiquitous Language

- **Book**: a bibliographic item in the user's library (identity + metadata)
- **ISBN**: an industry identifier for an edition of a book (validated/normalized)
- **Title**: the human-readable name of a book
- **Author**: a person or entity credited with creating the work
- **Metadata**: descriptive information about a book (not placement, not categorization)
- **Enrichment**: augmenting metadata from an external source using an identifier (e.g., ISBN)

------

## Key Rules / Invariants

- A **Book** must have a stable identity (`BookId`).
- If `Isbn` is present, it must be **valid and normalized** (format rules enforced in one place).
- Author names should be normalized consistently (e.g., trimming, spacing rules).
- Catalog defines what fields are considered canonical metadata vs optional enrichment.
- Other subdomains must not redefine bibliographic meaning (they may *reference* it).

------

## Public API / Contracts

Catalog should expose a minimal set of contracts for other subdomains/adapters, such as:

- `BookSummary`
  (for lists/search results; stable fields only)
    - `bookId`
    - `title`
    - `primaryAuthor` (or list of authors)
    - `isbn` (optional)
- `BookDetails`
  (for detailed views)
    - `bookId`, `title`, `subtitle`, `authors`, `isbn`, `publisher`, `publishedDate`, …
- `CatalogFacade` (or ports/use cases)
    - `registerBook(...)`
    - `getBookSummary(BookId)`
    - `getBookDetails(BookId)`
    - `findByIsbn(Isbn)` (optional)
    - `enrichMetadataByIsbn(Isbn)` (optional)

**Rule:** Other modules should depend on **contracts**, not Catalog's internal domain objects.

------

## Dependencies

Catalog may have outbound integrations (e.g., Google Books) behind ports such as:

- `BookMetadataLookupPort`

Infrastructure adapters implement those ports (WebClient, HTTP DTOs, etc.).

Catalog should NOT depend on:

- Storage/Placement internals
- Organization internals
- Discovery internals
- shared JPA entities across modules

------

## Suggested Package Layout (inside `catalog/`)

- `contracts/` — public DTOs/views exposed to other modules/adapters
- `application/` — use cases (register, update metadata, lookup by ISBN)
- `domain/` — entities/value objects/invariants (Book, Author, Isbn, BookTitle, etc.)
- `infrastructure/` — persistence + outbound adapters (JPA, WebClient, external payload DTOs)

(Exact naming can match your existing structure; the important part is the boundary.)

------

## Example Flows

- **Add book by ISBN**
    1. Validate/normalize ISBN (`Isbn`)
    2. Lookup metadata via `BookMetadataLookupPort`
    3. Create `Book` with canonical fields + optional enriched metadata
    4. Persist through repository
    5. Return `BookDetails` or `BookSummary` contract
- **Get book summary for display**
    - `CatalogFacade.getBookSummary(bookId)` → `BookSummary`

------

## Notes

Catalog is about **bibliographic truth** for the personal library. It should remain stable as other subdomains evolve. When in doubt, ask:

> "Is this about what the book *is*, or about how the book is *used/organized/placed*?"

If it's not "what the book is," it probably doesn't belong here.
