# Devlog: Ports and Adapters — Package Restructuring

**Date:** 2025-12-07  
**Focus:** Evolving from facades to true hexagonal architecture with inbound/outbound ports  
**Type:** Architecture / Refactoring

---

## Summary

Restructured the Author and Book modules to follow hexagonal architecture with explicit inbound and outbound ports. The key insight: **the Book module should not depend on the Author module's facade**—it should define its own interface for what it needs from authors.

**Before:** Book → AuthorFacade (Book depends on Author's contract)  
**After:** Book → AuthorAccessPort (Book defines its own contract, Author provides adapter)

---

## The Problem

The facade pattern got us most of the way there. CLI imports facades, services implement facades, everyone's happy. But there was a subtle coupling:

```java
// BookService.java (before)
public class BookService implements BookFacade {
    private final AuthorFacade authorFacade;  // ← Book depends on Author's contract
    
    public void createScannedBook(...) {
        AuthorDTO author = authorFacade.findOrCreateAuthor(firstName, lastName);
        //                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        //                 Book is coupled to Author's DTO and Facade
    }
}
```

The Book module had to import:
- `com.penrose.bibby.library.author.contracts.AuthorFacade`
- `com.penrose.bibby.library.author.contracts.AuthorDTO`

That's a dependency arrow pointing the wrong way. If the Author module changes its `AuthorDTO`, Book breaks.

---

## The Solution: Dependency Inversion

The Book module should define what it needs from authors in its own terms:

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BOOK MODULE                                  │
│                                                                      │
│   ┌─────────────────┐         ┌──────────────────────┐             │
│   │   BookService   │────────▶│   AuthorAccessPort   │ (interface) │
│   └─────────────────┘         └──────────────────────┘             │
│                                         ▲                           │
│   ┌─────────────────┐                   │                           │
│   │   AuthorRef     │ (Book's view)     │                           │
│   │   AuthorName    │ (value object)    │                           │
│   └─────────────────┘                   │                           │
└─────────────────────────────────────────┼───────────────────────────┘
                                          │ implements
┌─────────────────────────────────────────┼───────────────────────────┐
│                         AUTHOR MODULE   │                            │
│                                         │                            │
│   ┌─────────────────────────────────────┴────┐                      │
│   │         AuthorAccessAdapter              │                      │
│   │  (implements AuthorAccessPort)           │                      │
│   └─────────────────────────────────────────-┘                      │
│                        │                                             │
│                        ▼                                             │
│   ┌─────────────────────────────────────────┐                       │
│   │            AuthorService                │                       │
│   └─────────────────────────────────────────┘                       │
└─────────────────────────────────────────────────────────────────────┘
```

Now the dependency arrow points **into** the Book module. The Author module depends on Book's port definition, not the other way around.

---

## New Package Structure

### Author Module

```
author/
├── contracts/
│   ├── AuthorDTO.java                    # DTO for external consumers
│   └── ports/
│       └── AuthorFacade.java             # Inbound port (CLI uses this)
├── core/
│   ├── application/
│   │   └── AuthorService.java            # Application service
│   └── domain/
│       ├── Author.java                   # Domain model
│       ├── AuthorFactory.java            # Factory
│       └── AuthorRepository.java         # Domain repository interface
└── infrastructure/
    ├── adapters/
    │   └── AuthorAccessAdapter.java      # Implements Book's port
    ├── entity/
    │   └── AuthorEntity.java
    ├── mapping/
    │   ├── AuthorMapper.java
    │   └── AuthorMapperTwo.java
    └── repository/
        ├── AuthorJpaRepository.java      # JPA interface (renamed)
        └── AuthorRepositoryImpl.java     # Domain repo implementation
```

### Book Module

```
book/
├── AuthorRef.java                        # Book's view of an author
├── AuthorName.java                       # Value object for names
├── contracts/
│   ├── dtos/
│   │   ├── BookDTO.java
│   │   ├── BookDetailView.java
│   │   ├── BookMetaDataResponse.java
│   │   ├── BookPlacementResponse.java
│   │   ├── BookRequestDTO.java
│   │   ├── BookShelfAssignmentRequest.java
│   │   └── BookSummary.java
│   ├── ports/
│   │   ├── inbound/
│   │   │   └── BookFacade.java           # Inbound port (CLI uses this)
│   │   └── outbound/
│   │       └── AuthorAccessPort.java     # Outbound port (Book defines this)
│   └── adapters/
│       └── ...                           # Adapter implementations
├── core/
│   ├── application/
│   │   ├── BookService.java
│   │   ├── IsbnEnrichmentService.java
│   │   └── IsbnLookupService.java
│   └── domain/
│       ├── AvailabilityStatus.java
│       ├── Book.java
│       └── BookFactory.java
└── infrastructure/
    ├── entity/
    │   └── BookEntity.java
    ├── external/
    │   └── GoogleBooksResponse.java
    ├── mapping/
    │   ├── BookMapper.java
    │   └── BookMapperTwo.java
    └── repository/
        ├── BookDomainRepository.java
        └── BookRepository.java
```

---

## Key New Types

### AuthorName (Value Object)

```java
package com.penrose.bibby.library.book;

public class AuthorName {
    private final String firstName;
    private final String lastName;

    public AuthorName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // getters...
}
```

A simple value object that represents an author's name **in Book's vocabulary**. No dependency on the Author module.

### AuthorRef (Reference Type)

```java
package com.penrose.bibby.library.book;

public class AuthorRef {
    private final Long authorId;
    private final String authorFirstName;
    private final String authorLastName;
    
    // constructor, getters...
}
```

Book's view of an author identity. Contains just what Book needs: an ID and a name. Not the full `Author` domain object.

### AuthorAccessPort (Outbound Port)

```java
package com.penrose.bibby.library.book.contracts.ports.outbound;

public interface AuthorAccessPort {
    AuthorRef findOrCreateAuthor(String firstName, String lastName);
    Set<AuthorDTO> findByBookId(Long bookId);
}
```

The interface that Book defines for accessing author data. The Author module provides an adapter that implements this.

---

## BookService Changes

### Before

```java
package com.penrose.bibby.library.book.application;

import com.penrose.bibby.library.author.contracts.AuthorFacade;  // ❌ Depends on Author
import com.penrose.bibby.library.author.contracts.AuthorDTO;    // ❌ Depends on Author

public class BookService implements BookFacade {
    private final AuthorFacade authorFacade;
    
    public void createScannedBook(...) {
        AuthorDTO author = authorFacade.findOrCreateAuthor(firstName, lastName);
        authorEntities.add(AuthorDTO.AuthorDTOtoEntity(author));
    }
}
```

### After

```java
package com.penrose.bibby.library.book.core.application;

import com.penrose.bibby.library.book.AuthorRef;                           // ✓ Book's own type
import com.penrose.bibby.library.book.contracts.ports.outbound.AuthorAccessPort;  // ✓ Book's own port

public class BookService implements BookFacade {
    private final AuthorAccessPort authorAccessPort;
    
    public void createScannedBook(...) {
        AuthorRef author = authorAccessPort.findOrCreateAuthor(firstName, lastName);
        authorEntities.add(AuthorDTO.AuthorRefToEntity(author));
    }
}
```

---

## AuthorService Improvements

Added comprehensive Javadoc to clarify domain contracts:

```java
/**
 * Retrieves all Authors associated with the given Book.
 *
 * <p><strong>Domain Meaning:</strong><br>
 * A Book may have zero, one, or multiple Authors. This method resolves that
 * relationship by querying the underlying persistence store for all Author
 * records linked to the specified Book ID.
 *
 * <p><strong>Contract:</strong>
 * <ul>
 *   <li>Returns an immutable empty Set if the Book has no associated Authors.</li>
 *   <li>Never returns {@code null}.</li>
 *   <li>Always returns domain-level {@code Author} objects, not entities.</li>
 * </ul>
 *
 * @param id the unique identifier of the Book
 * @return a Set of domain {@code Author} objects, or empty Set if none exist
 */
public Set<Author> findAuthorsByBookId(Long id) {
    return authorRepository.findAuthorsByBookId(id);
}
```

Renamed methods for clarity:
- `findByBookId()` → `findAuthorsByBookId()`
- `findById()` → `findAuthorById()`
- `save()` → `createAuthor()`
- `update()` → `updateAuthor()`

---

## AuthorMapper Additions

```java
public static Set<Author> toDomainSet(Set<AuthorEntity> authorEntities) {
    Set<Author> authors = new java.util.HashSet<>();
    for (AuthorEntity authorEntity : authorEntities) {
        authors.add(toDomain(
            authorEntity.getAuthorId(), 
            authorEntity.getFirstName(), 
            authorEntity.getLastName()
        ));
    }
    return authors;
}

public static Set<AuthorDTO> toDTOSet(Set<Author> authors) {
    Set<AuthorDTO> authorDTOs = new java.util.HashSet<>();
    for (Author author : authors) {
        authorDTOs.add(new AuthorDTO(
            author.getAuthorId(),
            author.getFirstName(),
            author.getLastName()
        ));
    }
    return authorDTOs;
}

public static AuthorDTO toDTO(Author author) {
    return new AuthorDTO(
        author.getAuthorId(),
        author.getFirstName(),
        author.getLastName()
    );
}
```

---

## Naming Clarifications

| Before | After | Why |
|--------|-------|-----|
| `AuthorRepository` | `AuthorJpaRepository` | Distinguish JPA interface from domain repository |
| `findByBookId()` | `findAuthorsByBookId()` | Explicit about what's being found |
| `findById()` | `findAuthorById()` | Consistent naming pattern |
| `save()` | `createAuthor()` | Domain language, not persistence language |
| `update()` | `updateAuthor()` | Consistent with createAuthor() |

---

## Port Types Explained

### Inbound Ports (Driving Adapters)
- **Who calls them:** External actors (CLI, REST controllers)
- **Who defines them:** The module being called
- **Examples:** `BookFacade`, `AuthorFacade`, `ShelfFacade`

### Outbound Ports (Driven Adapters)
- **Who calls them:** The module's application services
- **Who defines them:** The module that needs the capability
- **Examples:** `AuthorAccessPort` (Book needs author data)

```
CLI ──▶ BookFacade ──▶ BookService ──▶ AuthorAccessPort ──▶ AuthorModule
        (inbound)                      (outbound)
```

---

## Files Changed

### Documentation
- `docs/engineering/closed-issues/closed-issue-11.md` — path updates
- `docs/the-devlogs/devlog-2025-12-03-*.md` — path updates
- `docs/the-devlogs/devlog-2025-12-05-*.md` — path updates
- `docs/the-devlogs/devlog-2025-12-06-*.md` — path updates
- `docs/systems/architecture/` — new architecture docs

### Author Module
- `author/contracts/AuthorDTO.java` — add AuthorRefToEntity()
- `author/contracts/ports/AuthorFacade.java` — moved, added Javadoc
- `author/core/application/AuthorService.java` — moved, renamed methods, added Javadoc
- `author/core/domain/Author.java` — moved
- `author/core/domain/AuthorFactory.java` — moved
- `author/core/domain/AuthorRepository.java` — moved
- `author/infrastructure/adapters/` — new adapter directory
- `author/infrastructure/mapping/AuthorMapper.java` — added helper methods
- `author/infrastructure/mapping/AuthorMapperTwo.java` — cleaned up
- `author/infrastructure/repository/AuthorJpaRepository.java` — renamed
- `author/infrastructure/repository/AuthorRepositoryImpl.java` — new

### Book Module
- `book/AuthorName.java` — new value object
- `book/AuthorRef.java` — new reference type
- `book/contracts/dtos/*.java` — moved all DTOs
- `book/contracts/ports/inbound/BookFacade.java` — moved
- `book/contracts/ports/outbound/AuthorAccessPort.java` — new
- `book/contracts/adapters/` — new adapter directory
- `book/core/application/BookService.java` — moved, use AuthorAccessPort
- `book/core/application/IsbnEnrichmentService.java` — moved
- `book/core/application/IsbnLookupService.java` — moved
- `book/core/domain/AvailabilityStatus.java` — moved
- `book/core/domain/Book.java` — moved
- `book/core/domain/BookFactory.java` — moved
- `book/infrastructure/entity/BookEntity.java` — import updates
- `book/infrastructure/mapping/BookMapper.java` — import updates
- `book/infrastructure/mapping/BookMapperTwo.java` — import updates
- `book/infrastructure/repository/BookDomainRepository.java` — import updates
- `book/infrastructure/repository/BookRepository.java` — import updates

### Other Modules
- `bookcase/contracts/BookcaseFacade.java` — return type fix
- `shelf/application/ShelfService.java` — import updates
- `shelf/contracts/ShelfFacade.java` — import updates
- `shelf/domain/Shelf.java` — remove unused import
- `shelf/domain/ShelfDomainRepositoryImpl.java` — import updates

### CLI & Infrastructure
- `cli/commands/BookCommands.java` — import updates
- `cli/commands/BookcaseCommands.java` — import updates
- `infrastructure/web/book/BookController.java` — import updates
- `infrastructure/web/book/BookImportController.java` — import updates

### Tests
- `test/.../BookServiceTest.java` — import updates

---

## Interview Talking Points

### "Explain the difference between inbound and outbound ports."

> Inbound ports are the API your module exposes to the outside world—like `BookFacade`. External actors call in through these ports. Outbound ports are interfaces your module defines for capabilities it needs from elsewhere—like `AuthorAccessPort`. My Book module needs author data, so it defines an interface saying "I need something that can find or create authors." The Author module then provides an adapter implementing that interface. The key insight is that the **consumer** defines the interface, not the provider. That's Dependency Inversion.

### "Why create AuthorRef instead of just using AuthorDTO?"

> `AuthorDTO` belongs to the Author module. If Book imports it, Book is coupled to Author's contract. If Author changes its DTO, Book breaks. Instead, Book defines `AuthorRef`—its own minimal view of what an author looks like. The adapter in Author module translates between `Author` domain objects and `AuthorRef`. Now Book and Author can evolve independently. The only coupling is through the port interface that Book defines.

### "What's the value of AuthorName as a value object?"

> `AuthorName` represents a concept in Book's vocabulary—a name with first and last parts. It's immutable, has no identity, and encapsulates the logic for combining names (`getFullName()`). By having this value object, Book doesn't need to know anything about how Author represents names. It's also a natural place to add validation later—like ensuring names aren't empty.

---

## Architecture Comparison

### Before (Facade Pattern)

```
CLI ──▶ BookFacade ──▶ BookService ──▶ AuthorFacade ──▶ AuthorService
                       │                │
                       │                └── Book imports Author's types
                       └── Book knows about Author's contract
```

### After (Ports and Adapters)

```
CLI ──▶ BookFacade ──▶ BookService ──▶ AuthorAccessPort
        (inbound)                      (outbound, defined by Book)
                                              │
                                              ▼
                                       AuthorAccessAdapter
                                       (implemented by Author)
                                              │
                                              ▼
                                       AuthorService
```

The arrows now flow **into** Book. Author depends on Book's port definition.

---

## What's Next

1. **Implement AuthorAccessAdapter** — Bridge between AuthorAccessPort and AuthorService
2. **Apply same pattern to Shelf/Bookcase** — Define outbound ports where needed
3. **Add ArchUnit rules** — Enforce port boundaries at test time
4. **Consider event-driven communication** — Replace some synchronous calls with domain events

---

## Reflection

This refactoring took the facade pattern to its logical conclusion. Facades gave us interface boundaries, but the dependency arrows still pointed in mixed directions. With explicit inbound and outbound ports, every module is a hexagon with well-defined edges.

The key mental shift: **the consumer defines the interface**. When Book needs author data, Book shouldn't ask "what does Author offer?" It should declare "here's what I need" and let Author figure out how to provide it. That's true decoupling.

The package structure now tells the story: `core/` is the domain heart, `contracts/ports/inbound/` is how the world calls in, `contracts/ports/outbound/` is what the module needs from the world, and `infrastructure/adapters/` is where the wiring happens.

Six weeks from "first commit" to hexagonal architecture with proper ports. Not bad.
