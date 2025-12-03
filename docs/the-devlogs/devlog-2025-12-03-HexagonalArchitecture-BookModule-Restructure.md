# Devlog: Hexagonal Architecture Refactoring - Book Module

**Date:** 2025-12-03  
**Focus:** Restructuring the Book module into a Hexagonal (Ports & Adapters) architecture  
**Commit Type:** `refactor(book)`

---

## Summary

Reorganized the entire `book` module from a flat/layered structure into a proper hexagonal architecture with clearly defined boundaries between API contracts, application logic, domain core, and infrastructure concerns. Also extracted web controllers into a dedicated `web` package outside the library modules.

---

## What Changed

### New Package Structure

```
com.penrose.bibby.library.book/
├── api/                          # Inbound ports (DTOs, Facades)
│   ├── BookDetailView.java
│   ├── BookFacade.java
│   ├── BookPlacementResponse.java
│   ├── BookRequestDTO.java
│   ├── BookShelfAssignmentRequest.java
│   └── BookSummary.java
├── application/                  # Use cases (Services)
│   ├── BookInfoService.java
│   ├── BookService.java
│   └── IsbnEnrichmentService.java
├── domain/                       # Core business logic
│   ├── AvailabilityStatus.java
│   ├── Book.java
│   └── BookFactory.java
└── infrastructure/               # Outbound adapters
    ├── entity/
    │   └── BookEntity.java
    ├── external/
    │   ├── BookImportRequest.java
    │   ├── BookImportResponse.java
    │   ├── GoogleBookItems.java
    │   ├── GoogleBooksResponse.java
    │   └── VolumeInfo.java
    ├── mapping/
    │   ├── BookMapper.java
    │   └── BookMapperTwo.java
    └── repository/
        ├── BookDomainRepository.java
        ├── BookDomainRepositoryImpl.java
        └── BookRepository.java

com.penrose.bibby.web/             # Extracted web layer
├── author/
│   └── AuthorController.java
└── book/
    ├── BookController.java
    └── BookImportController.java
```

### Key Moves

| Original Location | New Location | Rationale |
|-------------------|--------------|-----------|
| `book.dto.*` | `book.api.*` | DTOs are inbound port contracts |
| `book.service.*` | `book.application.*` | Services are use cases/orchestrators |
| `book.domain.BookEntity` | `book.infrastructure.entity` | JPA entity is persistence concern |
| `book.domain.GoogleBooksResponse` | `book.infrastructure.external` | External API types aren't domain |
| `book.mapping.*` | `book.infrastructure.mapping` | Mappers bridge infra↔domain |
| `book.repository.*` | `book.infrastructure.repository` | Data access is infrastructure |
| `book.controller.*` | `web.book.*` | Controllers are delivery mechanism |

### ShelfFacade Evolution

Converted `ShelfFacade` from an empty placeholder class to a proper interface:

```java
public interface ShelfFacade {
    Optional<ShelfEntity> getShelfEntityById(Long shelfId);
}
```

This establishes the contract for cross-domain communication without exposing internal shelf implementation details.

---

## Why Hexagonal Architecture?

### The Core Insight

Hexagonal architecture (Ports & Adapters) inverts the traditional dependency direction. Instead of:

```
Controller → Service → Repository → Database
```

We get:

```
[Adapters] → [Ports] ← [Application] ← [Domain]
              ↑
         Dependency direction points INWARD
```

### Practical Benefits I'm Experiencing

1. **Domain stays pure**: `Book.java` and `BookFactory.java` have zero infrastructure imports. They don't know about JPA, Spring, or HTTP.

2. **Testability**: I can unit test domain logic without mocking Spring contexts or databases.

3. **Flexibility**: If I wanted to swap PostgreSQL for MongoDB, only `infrastructure/repository` changes. The domain and application layers are untouched.

4. **Clear mental model**: When I'm in `application/`, I know I'm writing orchestration logic. When I'm in `domain/`, I know I'm writing business rules.

### The "Aha" Moment

The Google Books API types (`GoogleBooksResponse`, `VolumeInfo`, etc.) were sitting in `domain/`. That felt wrong but I couldn't articulate why until this refactor.

**The realization**: These are **external data shapes** dictated by Google's API contract. They're not MY domain concepts—they're infrastructure for integrating with an external system. Moving them to `infrastructure/external` makes the boundary crystal clear.

---

## Struggle Journal

### Challenge: Where Do DTOs Belong?

Initial instinct was to put DTOs in `application/` since services use them. But that creates a problem: controllers would need to depend on the application layer just to access request/response types.

**Resolution**: DTOs are **inbound port contracts**. They define the shape of data entering the system. Putting them in `api/` means:
- Controllers can import from `api/` 
- Services can import from `api/`
- Neither depends on the other

### Challenge: BookEntity Imports in Domain

After moving `BookEntity` to `infrastructure/entity`, the `BookFactory` still needed to reference it for the `fromEntity()` method.

**Current state**: `BookFactory` imports from infrastructure, which technically violates pure hexagonal principles.

**Future consideration**: Could introduce a `BookEntityPort` interface in `domain/` that `BookEntity` implements. But that feels like over-engineering for the current scale. Noting it for when/if it becomes painful.

### Challenge: Two Mappers?

The codebase has both `BookMapper` and `BookMapperTwo`. During this refactor, I moved both to `infrastructure/mapping` but didn't consolidate. 

**TODO**: Investigate whether these can be unified or if they serve distinct purposes that justify separate classes.

---

## Interview Talking Points

### "Why did you choose hexagonal over other architectural patterns?"

> For a library management system that integrates with external APIs (Google Books) and might need different interfaces (CLI, REST, potentially a web UI), hexagonal gives me flexibility without over-engineering. The ports define stable contracts while adapters can evolve independently.

### "How do you decide what goes in the domain layer vs application layer?"

> Domain contains the **what**—business rules, invariants, core entities. Application contains the **how**—use case orchestration, transaction boundaries, calling multiple domain objects to achieve a goal. If I can explain it without mentioning technology, it's probably domain. If I'm coordinating multiple things or talking to infrastructure, it's application.

### "What's the practical benefit you've seen from this structure?"

> When the Google Books API changes, I know exactly where to look: `infrastructure/external`. When I need to add a new business rule about book availability, I know it goes in `domain/`. The architecture provides a mental roadmap.

---

## Dependency Flow Verification

Quick sanity check that dependencies point inward:

```
✓ api/ imports nothing from book module
✓ application/ imports from api/ and domain/
✓ domain/ imports nothing from book module (except BookEntity for now)
✓ infrastructure/ imports from domain/
✓ web/ imports from api/ and application/
```

---

## What's Next

1. **Apply pattern to other modules**: Bookcase is partially restructured, but Author and Shelf still need the same treatment.

2. **Consolidate mappers**: Investigate `BookMapper` vs `BookMapperTwo` situation.

3. **ShelfFacade implementation**: The interface exists but needs an implementing class that `BookService` can use instead of directly accessing `ShelfService`.

4. **Consider module boundaries**: As this grows, might want to enforce boundaries more strictly with Java modules (module-info.java) or ArchUnit tests.

---

## Files Changed

- 28 files modified
- Package declarations updated
- Import statements corrected across CLI, controllers, services, and tests
- ShelfFacade converted from class to interface

---

## References

- "Hexagonal Architecture Explained" - Alistair Cockburn
- "Clean Architecture" - Robert Martin (Chapter 22: The Clean Architecture)
- Previous devlog on Factory Pattern and Entity/Domain separation
