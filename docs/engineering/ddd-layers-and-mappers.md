# DDD Layers & Mappers

## Does the Domain Model Need an ID if the Entity Already Has One?

**Short answer: Yes.**

The `ShelfEntity.shelfId` and `Shelf.shelfId` serve different purposes and live in different layers.

| | `ShelfEntity.shelfId` | `Shelf.shelfId` |
|---|---|---|
| **Type** | `Long` | `ShelfId` (value object) |
| **Layer** | Infrastructure | Domain |
| **Owned by** | JPA / database | Domain logic |
| **Purpose** | Auto-generated DB primary key | Domain identity |

### Why the domain model needs its own ID

In DDD, the domain model should be **completely decoupled from persistence concerns**. 
`ShelfEntity` knows about `@Id`, `@GeneratedValue`, and JPA — the domain `Shelf` model should not.

The `ShelfId` value object wraps the same underlying `Long` value but keeps the domain layer clean and ignorant of infrastructure details.

### Typical flow

**Saving:**
```
Shelf → ShelfEntity (infrastructure mapper) → JPA persists, generates ID
                                            → map back: shelf.setShelfId(new ShelfId(entity.getShelfId()))
```

**Loading:**
```
ShelfEntity → new Shelf(..., new ShelfId(entity.getShelfId()))
```

### ⚠️ Watch out
`Shelf.getId()` (which calls `shelfId.shelfId()`) will throw a `NullPointerException` if called before the shelf has been persisted and the ID assigned. Guard against this where needed.

---

## Where Should Mappers Live?

Mappers belong in the layer that **owns the boundary** they translate across. Each layer maps to/from its own types — the domain model stays passive and never maps itself.

### Rule of Thumb

| Mapper | Layer | Boundary |
|---|---|---|
| `Shelf` ↔ `ShelfEntity` | **Infrastructure** | Persistence boundary |
| `Shelf` → `ShelfDTO` | **Application** | API / use-case boundary |

### Infrastructure Mapper

Maps between the domain model and the JPA entity. Lives alongside the entity it maps from.

```
infrastructure/
  entity/
    ShelfEntity.java
  mapper/
    ShelfMapper.java        ← Shelf <-> ShelfEntity
  repository/
    ShelfRepositoryImpl.java
```

Used by the repository implementation to convert before saving and after loading.

### Application Mapper

Maps between the domain model and a DTO for transferring data across the API boundary.

```
application/
  dto/
    ShelfDTO.java
  mapper/
    ShelfDTOMapper.java     ← Shelf -> ShelfDTO
  service/
    ShelfService.java       ← calls ShelfDTOMapper
```

### Key Principle

> **You can have mappers in both layers.** Each layer owns the mapper for its own boundary. The domain model (`Shelf`) never does the mapping itself — it only gets mapped *to* and *from*.
