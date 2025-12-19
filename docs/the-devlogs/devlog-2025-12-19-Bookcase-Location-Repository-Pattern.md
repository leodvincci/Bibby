# Devlog: Bookcase Location Feature & Repository Pattern Refactor

**Date:** December 19, 2025  
**Module:** Stacks (Bookcase, Shelf) + CLI  
**Type:** Feature + Refactor  
**Status:** ✅ Complete

---

## 1. High-Level Summary

- **Added physical location to bookcases** — Bookcases now have a `location` (e.g., "Office"), `zone` (e.g., "NorthWall"), and `zoneIndex` (e.g., "A")
- **Derived bookcase labels** — Labels now auto-generate as `zone:index` (e.g., "NorthWall:A")
- **Extracted BookcaseRepository interface** — Separated domain interface from Spring Data JPA
- **Added location-based bookcase selection** — Users now select location first, then see filtered bookcases
- **Updated book cards** — Now display location information alongside bookcase and shelf

---

## 2. The Underlying Problem or Motivation

### Problem 1: No Physical Location Tracking

The original `Bookcase` model had:
```java
public class Bookcase {
    private Long bookcaseId;
    private String bookcaseLabel;  // User-entered, no structure
    private int shelfCapacity;
}
```

**Issues:**
- No way to group bookcases by physical location (office vs living room)
- Labels were free-form text, inconsistent naming
- As library grows, finding the right bookcase becomes harder

### Problem 2: JPA Leaking into Domain

```java
// Before: Domain interface WAS the JPA interface
@Repository
public interface BookcaseRepository extends JpaRepository<BookcaseEntity, Long> {
    BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);
}
```

**Issues:**
- Domain layer directly coupled to Spring Data
- Can't easily swap implementations for testing
- Violates hexagonal architecture principles

---

## 3. The Solution

### New Domain Model

```java
public class Bookcase {
    private Long bookcaseId;
    private String bookcaseLocation;   // NEW: "Office", "Living Room", "Garage"
    private String bookcaseZone;       // NEW: "NorthWall", "Desk", "Corner"
    private String bookcaseZoneIndex;  // NEW: "A", "B", "C"
    private String bookcaseLabel;      // DERIVED: zone + ":" + index
    private int shelfCapacity;
    private int bookCapacity;
}
```

### Labeling Convention

| Location | Zone | Index | Generated Label |
|----------|------|-------|-----------------|
| Office | NorthWall | A | NorthWall:A |
| Office | NorthWall | B | NorthWall:B |
| Office | Desk | A | Desk:A |
| Living Room | Main | A | Main:A |

### Repository Pattern

```
Before:
┌─────────────────────────────────────────┐
│ BookcaseRepository                      │
│ (JPA interface, used everywhere)        │
└─────────────────────────────────────────┘

After:
┌─────────────────────────────────────────┐
│ BookcaseRepository (Domain Interface)   │
│ - findAll()                             │
│ - findById(Long)                        │
│ - findByLocation(String)                │
│ - getAllBookCaseLocations()             │
│ - save(BookcaseEntity)                  │
└─────────────────────────────────────────┘
                    ▲
                    │ implements
┌─────────────────────────────────────────┐
│ BookcaseRepositoryImpl                  │
│ - Delegates to BookcaseJpaRepository    │
└─────────────────────────────────────────┘
                    │
                    │ uses
                    ▼
┌─────────────────────────────────────────┐
│ BookcaseJpaRepository                   │
│ extends JpaRepository<BookcaseEntity>   │
└─────────────────────────────────────────┘
```

---

## 4. User Flow Changes

### Before: Flat Bookcase Selection

```
User: book scan
System: Select a bookcase:
        [Cancel]
        Bookcase-1
        Bookcase-2
        My Office Shelf
        Living Room Corner
        ...20 more items...
```

### After: Location-Based Selection

```
User: book scan
System: Select a location:
        [Cancel]
        Office
        Living Room
        Garage

User: Office
System: Select a bookcase:
        [Cancel]
        NorthWall:A
        NorthWall:B
        Desk:A

User: NorthWall:A
System: Select a shelf:
        [Cancel]
        Shelf 1
        Shelf 2
```

**Benefits:**
- Fewer options at each step
- Logical grouping by physical space
- Scales better as library grows

---

## 5. Book Card Updates

### Before

```
╭──────────────────────────────────────────────────────────────────────────────╮
│  📖 Building Microservices                                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│  ISBN: 978-1491950357                                                        │
│  Author: Sam Newman                                                          │
│  Publisher: O'Reilly Media                                                   │
│                                                                              │
│  Bookcase: NorthWall:A                                                       │
│  Shelf: Shelf 2                                                              │
╰──────────────────────────────────────────────────────────────────────────────╯
```

### After

```
╭──────────────────────────────────────────────────────────────────────────────╮
│  📖 Building Microservices                                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│  ISBN: 978-1491950357                                                        │
│  Author: Sam Newman                                                          │
│  Publisher: O'Reilly Media                                                   │
│                                                                              │
│  Location: Office                                                            │
│  Bookcase: NorthWall:A                                                       │
│  Shelf: Shelf 2                                                              │
╰──────────────────────────────────────────────────────────────────────────────╯
```

---

## 6. Key Code Changes

### BookcaseEntity — New Fields & Constructor

```java
@Entity
public class BookcaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookcaseId;
    
    private String bookcaseLocation;     // NEW
    private String bookcaseZone;         // NEW
    private String bookcaseZoneIndex;    // NEW
    private String bookcaseLabel;        // Now derived
    private int shelfCapacity;
    private int bookCapacity;
    
    // NEW: Constructor that derives label
    public BookcaseEntity(String location, String zone, String index, 
                          int shelfCapacity, int bookCapacity) {
        this.bookcaseLocation = location;
        this.bookcaseZone = zone;
        this.bookcaseZoneIndex = index;
        this.bookcaseLabel = zone + ":" + index;  // Derived!
        this.shelfCapacity = shelfCapacity;
        this.bookCapacity = bookCapacity;
    }
}
```

### BookcaseDTO — Added Location

```java
public record BookcaseDTO(
    Long bookcaseId,
    String bookcaseLabel,
    int shelfCapacity,
    int bookCapacity,
    String location    // NEW
) {}
```

### BookcaseRepository — Domain Interface

```java
public interface BookcaseRepository {
    BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);
    List<BookcaseEntity> findAll();
    List<String> getAllBookCaseLocations();  // NEW
    void save(BookcaseEntity bookcaseEntity);
    Optional<BookcaseEntity> findById(Long id);
    List<BookcaseEntity> findByLocation(String location);  // NEW
}
```

### PromptOptions — Location-Based Options

```java
public Map<String, String> bookCaseOptionsByLocation(String location) {
    Map<String, String> options = new LinkedHashMap<>();
    options.put("\u001B[38;5;202m[Cancel]\033[36m", "cancel");
    
    List<BookcaseDTO> bookcases = bookcaseFacade.findByLocation(location);
    for (BookcaseDTO b : bookcases) {
        options.put(b.bookcaseLabel(), b.bookcaseId().toString());
    }
    return options;
}

public Map<String, String> locationOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    options.put("\u001B[38;5;202m[Cancel]\033[36m", "cancel");
    
    List<String> locations = bookcaseFacade.getAllLocations();
    for (String loc : locations) {
        options.put(loc, loc);
    }
    return options;
}
```

### BookCommands — Updated Flow

```java
public void scanBook(...) {
    // ... ISBN scan and metadata fetch ...
    
    if (cliPrompt.promptBookConfirmation()) {
        // NEW: Select location first
        String location = cliPrompt.promptForBookcaseLocation();
        if (location == null) return;
        
        // Then select bookcase (filtered by location)
        Long bookcaseId = cliPrompt.promptForBookCase(
            promptOptions.bookCaseOptionsByLocation(location)
        );
        if (bookcaseId == null) return;
        
        Long shelfId = cliPrompt.promptForShelf(bookcaseId);
        // ... rest of flow ...
    }
}
```

### ShelfService — Uses Facade Instead of Repository

```java
// Before
public class ShelfService implements ShelfFacade {
    private final BookcaseRepository bookcaseRepository;
    
    private ShelfOptionResponse toShelfOption(ShelfEntity shelf) {
        BookcaseEntity bookcase = bookcaseRepository.findById(shelf.getBookcaseId()).orElse(null);
        // ...
    }
}

// After
public class ShelfService implements ShelfFacade {
    private final BookcaseFacade bookcaseFacade;  // Through facade, not repository
    
    private ShelfOptionResponse toShelfOption(ShelfEntity shelf) {
        BookcaseEntity bookcase = bookcaseFacade.findById(shelf.getBookcaseId()).orElse(null);
        // ...
    }
}
```

---

## 7. Architecture Implications

### Good: Repository Pattern Applied

```
Domain Layer
    └── BookcaseRepository (interface)
            ↑
            │ implements
Infrastructure Layer
    ├── BookcaseRepositoryImpl
    │       └── uses BookcaseJpaRepository
    └── BookcaseJpaRepository (Spring Data)
```

**Benefits:**
- Domain doesn't know about Spring Data
- Can create `InMemoryBookcaseRepository` for tests
- Follows hexagonal architecture

### Watch: Cross-Module Dependency

```java
// In ShelfService (Shelf module)
private final BookcaseFacade bookcaseFacade;  // Depends on Bookcase module
```

**Concern:** Shelf now depends on Bookcase through the facade.

**Options:**
1. **Accept it** — Shelf legitimately needs bookcase info for display
2. **Create outbound port** — `BookcaseAccessPort` in Shelf module (like you did for Book→Shelf)
3. **Pass data from CLI** — CLI fetches bookcase, passes label to ShelfService

For now, facade dependency is acceptable. Consider port pattern if coupling grows.

---

## 8. Talking Points (Interview / Portfolio)

- **Extended domain model with physical location hierarchy** — Added location, zone, and index fields to enable structured bookcase organization and scalable library management

- **Applied repository pattern properly** — Extracted domain interface from JPA implementation, improving testability and adherence to hexagonal architecture

- **Implemented hierarchical UI flow** — Changed flat bookcase selection to location→bookcase→shelf drill-down, reducing cognitive load as library scales

- **Derived data from components** — Bookcase labels now auto-generate from zone:index, ensuring consistency and reducing user input errors

- **Reduced cross-layer coupling** — ShelfService now uses BookcaseFacade instead of directly accessing BookcaseRepository, respecting module boundaries

---

## 9. Potential Interview Questions

### Domain Modeling

1. Why did you split bookcase naming into zone and index instead of keeping a single label field?
2. What are the tradeoffs of deriving the label vs letting users enter it freely?
3. How would you handle bookcases that move between locations?

### Repository Pattern

4. Why separate BookcaseRepository (interface) from BookcaseJpaRepository?
5. How does this separation improve testability?
6. What would an in-memory implementation look like for testing?

### Architecture

7. Is it appropriate for ShelfService to depend on BookcaseFacade?
8. When would you introduce an outbound port instead of using a facade directly?
9. How does this change affect the dependency graph of your stacks module?

### UX Design

10. Why filter bookcases by location instead of showing all at once?
11. How does the hierarchical selection scale as the library grows?
12. What happens if a user has only one location? Should you skip that step?

---

## 10. Database Considerations

### New Columns

```sql
ALTER TABLE bookcase_entity ADD COLUMN bookcase_location VARCHAR(255);
ALTER TABLE bookcase_entity ADD COLUMN bookcase_zone VARCHAR(255);
ALTER TABLE bookcase_entity ADD COLUMN bookcase_zone_index VARCHAR(255);
```

### Migration Strategy

**If using `create-drop`:** No action needed, schema regenerates.

**If using `update` or production:** Create migration scripts in `src/main/resources/db/`.

### Existing Data

Existing bookcases will have `NULL` for new fields. Consider:
1. Default values in migration
2. Data backfill script
3. Allow null in UI (show "Unassigned" for location)

---

## 11. Files Changed

| File | Type | Description |
|------|------|-------------|
| `Bookcase.java` | Modified | Add location field |
| `BookcaseEntity.java` | Modified | Add location, zone, index + new constructor |
| `BookcaseDTO.java` | Modified | Add location field |
| `BookcaseRepository.java` | Modified | Convert to domain interface |
| `BookcaseJpaRepository.java` | **New** | Spring Data JPA interface |
| `BookcaseRepositoryImpl.java` | **New** | Repository implementation |
| `BookcaseFacade.java` | Modified | Add findById, findByLocation |
| `BookcaseService.java` | Modified | Update DTO mapping |
| `ShelfService.java` | Modified | Use facade instead of repository |
| `PromptOptions.java` | Modified | Add location-based options |
| `CliPromptService.java` | Modified | Add location prompt |
| `BookCommands.java` | Modified | Update flow and book card |
| `BookcaseCommands.java` | Modified | Update create flow |
| `db/` | **New** | Database migrations |

---

## 12. Commit Strategy

```bash
# Commit 1: Domain model changes
git add src/main/java/com/penrose/bibby/library/stacks/bookcase/core/domain/Bookcase.java
git add src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseEntity.java
git add src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/dtos/BookcaseDTO.java
git commit -m "feat(bookcase): add location, zone, and zoneIndex fields"

# Commit 2: Repository pattern
git add src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseRepository.java
git add src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseJpaRepository.java
git add src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseRepositoryImpl.java
git commit -m "refactor(bookcase): extract repository interface from JPA"

# Commit 3: Facade and service updates
git add src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/ports/inbound/BookcaseFacade.java
git add src/main/java/com/penrose/bibby/library/stacks/bookcase/core/application/BookcaseService.java
git add src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java
git commit -m "refactor(stacks): update services for new repository pattern"

# Commit 4: CLI updates
git add src/main/java/com/penrose/bibby/cli/
git commit -m "feat(cli): add location-based bookcase selection flow"

# Commit 5: Database migrations (if applicable)
git add src/main/resources/db/
git commit -m "chore(db): add migration for bookcase location fields"
```

---

## 13. Future Enhancements

1. **Skip location selection** if user has only one location
2. **Default location** preference in user settings
3. **Location management CLI** — add/rename/delete locations
4. **Search by location** — "Show all books in Office"
5. **Location capacity tracking** — "Office is 80% full"
6. **Move bookcase** command — relocate bookcase between locations
