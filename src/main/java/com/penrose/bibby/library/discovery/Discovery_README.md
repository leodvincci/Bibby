# Discovery Subdomain

## Purpose

The **Discovery** subdomain exists to help users **find books quickly** using **search, filtering, and sorting**, without leaking persistence details or forcing other subdomains to share internal models.

Discovery is a **read-focused** capability: it answers questions like “show me books that match X.”

---

## In Scope

* Text search (title, author name, ISBN, tags, booklists)
* Filters (e.g., by author, tag, booklist, shelf/bookcase)
* Sorting (title, author, recently added, etc.)
* Query models / view models optimized for read UX (CLI/UI)
* Search result shaping (summary vs detail views)

---

## Out of Scope

* Book identity and metadata rules (owned by **Catalog**)
* Physical placement rules/capacity (owned by **Storage/Placement**)
* Tag and booklist invariants (owned by **Organization**)
* Persistence schemas (JPA entities) as domain truth
* Write workflows (creating/editing books, tagging, placing)

Discovery **consumes** read-friendly projections from other subdomains; it does not own the truth of those concepts.

---

## Ubiquitous Language

* **Query**: a user’s search intent (text + filters + sort)
* **Filter**: a constraint applied to results (e.g., tag = “FP”)
* **Facet**: a filter dimension users can select (tags, authors, shelves)
* **Result**: a matched book entry returned by discovery
* **View / Projection**: a read-optimized representation for display (not a domain object)
* **Index** (optional future): a structure that accelerates search (may be DB or external)

---

## Key Rules / Invariants

* Discovery results must be **consistent in meaning** with the source subdomain contracts.
* Discovery must not depend directly on other subdomains’ **domain** types (Entities/Value Objects).
* Discovery may cache/denormalize **read data**, but must treat it as **derived**, not authoritative.
* A “search result” is a **view**, not a domain model: it can contain merged fields across subdomains.
* Search must be deterministic for the same inputs (unless explicitly using ranking).

---

## Public API / Contracts

Discovery should expose a small, stable contract such as:

* `SearchBooksQuery` (input)

    * `text`
    * `filters` (tags, booklists, author, location)
    * `sort`
    * `page` / `limit`
* `BookSearchResultView` (output)

    * `bookId`
    * `displayTitle`
    * `primaryAuthor`
    * `isbn` (optional)
    * `locationSummary` (optional)
    * `tagNames` (optional)
    * `booklists` (optional)

**Recommended entrypoint**

* `DiscoveryFacade` (or `SearchBooksUseCase` port)

    * `search(SearchBooksQuery) -> Page<BookSearchResultView>`

Adapters (CLI, REST) call Discovery through this contract.

---

## Dependencies

Discovery is allowed to depend on **contracts** from:

* `library.catalog.contracts` (book/author summaries)
* `library.organization.contracts` (tag/booklist summaries)
* `library.storage.contracts` (location summaries)

Discovery should NOT depend on:

* JPA entities from any module
* internal domain objects from any module

---

## Example Queries

* “functional programming” (text search)
* filter: `tag = "Java"`
* filter: `booklist = "System Design"`
* filter: `shelf = "Shelf-3"`
* sort: `recentlyAdded`

---

## Implementation Notes (Guidance)

* Start with a simple DB-backed search (SQL/JPQL) using read DTOs/views.
* Keep query logic in the **application** layer; keep storage-specific concerns in **infrastructure**.
* If discovery grows, consider a dedicated read model or index (still behind the same contracts).

---

If you want, paste your intended discovery commands (CLI: `book search ...`?) and I’ll tailor the **Ubiquitous Language** + **contracts** to match your actual query shapes and naming style.
