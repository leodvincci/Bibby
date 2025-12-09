# Devlog: Shelf Module Hexagonal Architecture Restructure

**Date:** 2025-12-09  
**Module:** Shelf  
**Type:** Refactoring  
**Status:** Complete

---

## Summary

Restructured the Shelf module to follow the same hexagonal architecture pattern established in Author and Book modules. This creates consistency across the codebase and enforces clear boundaries between core domain logic, contracts, and infrastructure.

---

## Problem

The Shelf module had a flat package structure that didn't align with the hexagonal architecture being adopted across other modules:

```
shelf/
├── application/
│   └── ShelfService.java
├── contracts/
│   ├── ShelfDTO.java
│   ├── ShelfFacade.java
│   ├── ShelfOptionResponse.java
│   └── ShelfSummary.java
├── domain/
│   ├── Shelf.java
│   ├── ShelfDomainRepository.java
│   ├── ShelfDomainRepositoryImpl.java
│   └── ShelfFactory.java
└── infrastructure/
    ├── entity/
    ├── mapping/
    └── repository/
```

This structure lacked explicit separation between inbound ports (how the world calls us) and DTOs (data carriers), and didn't follow the `core/` convention for domain and application layers.

---

## Solution

Restructured to match the hexagonal pattern:

```
shelf/
├── contracts/
│   ├── dtos/
│   │   ├── ShelfDTO.java
│   │   ├── ShelfOptionResponse.java
│   │   └── ShelfSummary.java
│   └── ports/
│       └── inbound/
│           └── ShelfFacade.java
├── core/
│   ├── application/
│   │   └── ShelfService.java
│   └── domain/
│       ├── Shelf.java
│       ├── ShelfDomainRepository.java
│       ├── ShelfDomainRepositoryImpl.java
│       └── ShelfFactory.java
└── infrastructure/
    ├── entity/
    ├── mapping/
    └── repository/
```

---

## Package Changes

| Before | After |
|--------|-------|
| `shelf.application` | `shelf.core.application` |
| `shelf.domain` | `shelf.core.domain` |
| `shelf.contracts.ShelfFacade` | `shelf.contracts.ports.inbound.ShelfFacade` |
| `shelf.contracts.ShelfDTO` | `shelf.contracts.dtos.ShelfDTO` |
| `shelf.contracts.ShelfSummary` | `shelf.contracts.dtos.ShelfSummary` |
| `shelf.contracts.ShelfOptionResponse` | `shelf.contracts.dtos.ShelfOptionResponse` |

---

## Files Changed

### Shelf Module (moved/updated packages)
- `ShelfService.java` → `shelf.core.application`
- `Shelf.java` → `shelf.core.domain`
- `ShelfFactory.java` → `shelf.core.domain`
- `ShelfDomainRepository.java` → `shelf.core.domain`
- `ShelfDomainRepositoryImpl.java` → `shelf.core.domain`
- `ShelfFacade.java` → `shelf.contracts.ports.inbound`
- `ShelfDTO.java` → `shelf.contracts.dtos`
- `ShelfSummary.java` → `shelf.contracts.dtos`
- `ShelfOptionResponse.java` → `shelf.contracts.dtos`
- `ShelfMapper.java` (updated imports)
- `ShelfJpaRepository.java` (updated imports)

### Consumers (updated imports)
- `BookCommands.java`
- `BookcaseCommands.java`
- `CliPromptService.java`
- `BookController.java`
- `ShelfController.java`
- `BookService.java`
- `BookDomainRepositoryImpl.java`

### Documentation
- `devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md` (fixed import paths in code examples)

---

## Architecture Pattern

The hexagonal structure separates concerns into three areas:

### Core (the hexagon)
- **`core/domain/`** — Pure business logic, entities, value objects, domain repositories
- **`core/application/`** — Use cases, orchestration, implements inbound ports

### Contracts (the ports)
- **`contracts/ports/inbound/`** — Interfaces the world uses to call us (facades)
- **`contracts/ports/outbound/`** — Interfaces we use to call the world (not yet needed for Shelf)
- **`contracts/dtos/`** — Data transfer objects for API boundaries

### Infrastructure (the adapters)
- **`infrastructure/entity/`** — JPA entities
- **`infrastructure/repository/`** — Repository implementations
- **`infrastructure/mapping/`** — Entity ↔ Domain mappers

---

## Consistency Across Modules

All four domain modules now follow the same structure:

| Module | core/ | contracts/ports/inbound/ | contracts/dtos/ | infrastructure/ |
|--------|-------|--------------------------|-----------------|-----------------|
| Author | ✓ | AuthorFacade | AuthorDTO | ✓ |
| Book | ✓ | BookFacade | BookDTO, etc. | ✓ |
| Shelf | ✓ | ShelfFacade | ShelfDTO, etc. | ✓ |
| Bookcase | ✓ | BookcaseFacade | BookcaseDTO | ✓ |

---

## Outstanding Work

1. **BookService imports ShelfService directly** — Should import `ShelfFacade` instead
2. **BookController imports ShelfService directly** — Same issue
3. **Consider ShelfAccessPort** — If Book needs shelf data, should it go through an outbound port?

---

## Key Decision: Consumer Adapts to Provider

During this refactoring, discussed whether modules should use dependency inversion (provider adapts to consumer) or the simpler model (consumer adapts to provider).

For Bibby, the simpler model makes sense:
- Author, Shelf, Bookcase are stable foundation modules
- Book is the primary consumer
- Consumers import provider facades and adapt internally if needed
- No need for outbound ports unless testing isolation becomes painful

This avoids over-engineering while maintaining clean facade boundaries.

---

## Interview Talking Points

**"How did you ensure consistency across modules?"**
> Established a canonical package structure—core/, contracts/ports/, contracts/dtos/, infrastructure/—and applied it systematically to each module. The structure is self-documenting: you can understand the architecture just by looking at package names.

**"Why separate dtos/ from ports/?"**
> Ports are interfaces that define behavior contracts. DTOs are just data shapes. They serve different purposes. A port like ShelfFacade defines *what operations exist*. A DTO like ShelfDTO defines *what data looks like*. Keeping them separate makes each easier to evolve.

---

## Commit

```
refactor(shelf): restructure to hexagonal architecture

- Move ShelfService to shelf.core.application
- Move domain classes (Shelf, ShelfFactory, ShelfDomainRepository) to shelf.core.domain
- Move ShelfFacade to shelf.contracts.ports.inbound
- Move DTOs (ShelfDTO, ShelfSummary, ShelfOptionResponse) to shelf.contracts.dtos
- Update imports across CLI, controllers, and dependent services
- Fix devlog documentation paths to reflect new structure
```
