## Devlog Entry

**Date**: 2025-11-22

### Feature: Add Capacity Management to Shelf Domain Model

**Context**

The library system needed to track how many books are on each shelf and enforce capacity limits. Previously, shelves had no concept of capacity or current book count, making it impossible to prevent overfilling shelves or query shelf availability.

**Motivation**

Adding capacity management enables:

- Preventing shelves from being overfilled
- Querying available shelf space
- Making informed decisions about book placement
- Reporting on shelf utilization

**Initial Implementation**

#### 1. Added Capacity Fields to Domain Model

**File:** `Shelf.java`

**Added fields:**

```java
private int bookCount;
private int shelfCapacity;
private boolean isFull;
```

**Added constructor:**

```java
public Shelf(Bookcase bookCase, String shelfLabel, int shelfPosition, int shelfCapacity) {
    this.bookCase = bookCase;
    this.shelfLabel = shelfLabel;
    this.shelfPosition = shelfPosition;
    this.shelfCapacity = shelfCapacity;
}
```

**Added getters/setters:**

```java
public int getBookCount() { return bookCount; }
public void setBookCount(int bookCount) { this.bookCount = bookCount; }
public int getShelfCapacity() { return shelfCapacity; }
public void setShelfCapacity(int shelfCapacity) { this.shelfCapacity = shelfCapacity; }
public boolean isFull() { return isFull; }
public void setFull(boolean full) { isFull = full; }
```

#### 2. Mirrored Fields in Entity

**File:** `ShelfEntity.java`

**Added matching fields:**

```java
private int bookCount;
private int shelfCapacity;
private boolean isFull;
private String shelfDescription;  // Additional field
```

**Added corresponding getters/setters** for all new fields.

#### 3. Code Cleanup

**File:** `BookCommands.java`

Removed debug output:

```java
// Removed:
System.out.println("BOOK CASE ID: " + bookCaseId);
```

**File:** `ShelfSummary.java`

Cleaned up formatting:

```java
// Before:
public record ShelfSummary(Long shelfId, String label, long bookCount) {


}

// After:
public record ShelfSummary(Long shelfId, String label, long bookCount) { }
```

**Issues Discovered During Code Review**

#### Issue 1: Storing Derived State

**Problem:**

```java
private boolean isFull;  // ❌ This can be computed from bookCount and shelfCapacity
```

**Root Cause:** Treating `isFull` as data to store rather than a question to ask about the shelf's state.

**Impact:** Creates synchronization problem - if `bookCount` changes but `isFull` isn't updated, the object is in an inconsistent state.

**Example of the bug:**

```java
shelf.setShelfCapacity(10);
shelf.setBookCount(10);
shelf.setFull(true);

// Later...
shelf.setBookCount(5);  // Removed 5 books
// But isFull is still true! 💥
```

#### Issue 2: Missing Default Constructor

**Problem:**

```java
public Shelf(Bookcase bookCase, String shelfLabel, int shelfPosition, int shelfCapacity) {
    // ...
}
// No default constructor - breaks any code doing: new Shelf()
```

**Root Cause:** Not understanding that adding any constructor removes Java's implicit default constructor.

**Impact:**

- Breaks existing code that uses `new Shelf()`
- Breaks frameworks that need default constructor (JPA, Jackson, MapStruct)
- Compilation errors in mapper classes

#### Issue 3: No Validation

**Problem:**

```java
public void setBookCount(int bookCount) {
    this.bookCount = bookCount;  // Accepts any value, including negatives!
}

public void setShelfCapacity(int shelfCapacity) {
    this.shelfCapacity = shelfCapacity;  // No validation
}
```

**Root Cause:** Thinking of setters as "just setting values" rather than enforcing business rules.

**Impact:**

- Allows negative book counts: `shelf.setBookCount(-5)`
- Allows book count to exceed capacity: `shelf.setBookCount(1000)` when capacity is 20
- Allows zero or negative capacity: `shelf.setShelfCapacity(0)`

#### Issue 4: Entity-Domain Model Mismatch

**Problem:**

```java
// ShelfEntity has:
private String shelfDescription;

// But Shelf doesn't have it
```

**Root Cause:** Adding field to one representation without updating the other.

**Impact:**

- Mapper will lose `shelfDescription` when converting Entity → Domain
- Domain model can't use description field in business logic
- Data exists in database but isn't accessible in application layer

#### Issue 5: Business Logic in Wrong Layer

**Problem:** Using raw setters instead of meaningful business operations.

**Better approach:**

```java
// Instead of:
shelf.setBookCount(shelf.getBookCount() + 1);

// Provide:
shelf.addBook();
```

**Corrected Implementation**

#### Shelf.java (Domain Model)

```java
public class Shelf {
    private Long id;
    private Bookcase bookCase;
    private String shelfLabel;
    private int shelfPosition;
    private int bookCount;
    private int shelfCapacity;
    private String shelfDescription;
    
    // Default constructor for frameworks/mappers
    public Shelf() {
    }
    
    // Application constructor with validation
    public Shelf(Bookcase bookCase, String shelfLabel, int shelfPosition, int shelfCapacity) {
        if (shelfLabel == null || shelfLabel.isBlank()) {
            throw new IllegalArgumentException("Shelf label cannot be blank");
        }
        if (shelfPosition < 0) {
            throw new IllegalArgumentException("Shelf position cannot be negative");
        }
        if (shelfCapacity <= 0) {
            throw new IllegalArgumentException("Shelf capacity must be positive");
        }
        
        this.bookCase = bookCase;
        this.shelfLabel = shelfLabel;
        this.shelfPosition = shelfPosition;
        this.shelfCapacity = shelfCapacity;
        this.bookCount = 0;  // Explicit initialization
    }
    
    // Derived state - computed, not stored
    public boolean isFull() {
        return bookCount >= shelfCapacity;
    }
    
    public boolean isEmpty() {
        return bookCount == 0;
    }
    
    public int getRemainingCapacity() {
        return shelfCapacity - bookCount;
    }
    
    public boolean canAccommodate(int numberOfBooks) {
        return bookCount + numberOfBooks <= shelfCapacity;
    }
    
    // Business logic methods
    public void addBook() {
        if (isFull()) {
            throw new IllegalStateException(
                String.format("Cannot add book: shelf '%s' is full (%d/%d)", 
                    shelfLabel, bookCount, shelfCapacity)
            );
        }
        bookCount++;
    }
    
    public void removeBook() {
        if (isEmpty()) {
            throw new IllegalStateException(
                String.format("Cannot remove book: shelf '%s' is empty", shelfLabel)
            );
        }
        bookCount--;
    }
    
    public void addBooks(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Must add at least one book");
        }
        if (!canAccommodate(count)) {
            throw new IllegalStateException(
                String.format("Cannot add %d books: only %d spaces remaining", 
                    count, getRemainingCapacity())
            );
        }
        bookCount += count;
    }
    
    // Validated setters
    public void setBookCount(int bookCount) {
        if (bookCount < 0) {
            throw new IllegalArgumentException("Book count cannot be negative");
        }
        if (bookCount > shelfCapacity) {
            throw new IllegalArgumentException(
                String.format("Book count (%d) exceeds capacity (%d)", 
                    bookCount, shelfCapacity)
            );
        }
        this.bookCount = bookCount;
    }
    
    public void setShelfCapacity(int shelfCapacity) {
        if (shelfCapacity <= 0) {
            throw new IllegalArgumentException("Shelf capacity must be positive");
        }
        if (shelfCapacity < this.bookCount) {
            throw new IllegalArgumentException(
                String.format("Cannot reduce capacity to %d when shelf has %d books",
                    shelfCapacity, this.bookCount)
            );
        }
        this.shelfCapacity = shelfCapacity;
    }
    
    // Standard getters
    public int getBookCount() { return bookCount; }
    public int getShelfCapacity() { return shelfCapacity; }
    public String getShelfDescription() { return shelfDescription; }
    public void setShelfDescription(String description) { 
        this.shelfDescription = description; 
    }
    
    // ... other getters/setters
}
```

#### ShelfEntity.java (Persistence Layer)

```java
@Entity
@Table(name = "shelves")
public class ShelfEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;
    
    private String shelfLabel;
    private Long bookcaseId;
    private int shelfPosition;
    private int bookCount;
    private int shelfCapacity;
    private String shelfDescription;
    
    // Remove isFull field - it's derived, doesn't belong in database
    
    // Default constructor for JPA
    public ShelfEntity() {
    }
    
    // Standard getters and setters
    // (Validation happens in domain model, entity is just for persistence)
    
    public Long getShelfId() { return shelfId; }
    public void setShelfId(Long shelfId) { this.shelfId = shelfId; }
    
    public String getShelfLabel() { return shelfLabel; }
    public void setShelfLabel(String shelfLabel) { this.shelfLabel = shelfLabel; }
    
    public Long getBookcaseId() { return bookcaseId; }
    public void setBookcaseId(Long bookcaseId) { this.bookcaseId = bookcaseId; }
    
    public int getShelfPosition() { return shelfPosition; }
    public void setShelfPosition(int shelfPosition) { this.shelfPosition = shelfPosition; }
    
    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }
    
    public int getShelfCapacity() { return shelfCapacity; }
    public void setShelfCapacity(int shelfCapacity) { this.shelfCapacity = shelfCapacity; }
    
    public String getShelfDescription() { return shelfDescription; }
    public void setShelfDescription(String shelfDescription) { 
        this.shelfDescription = shelfDescription; 
    }
}
```

**Key Changes in Corrected Version**

1. **Removed `isFull` field** - converted to computed method
2. **Added default constructor** to both Entity and Domain Model
3. **Added validation** to all setters that modify state
4. **Added `shelfDescription`** to Domain Model for consistency
5. **Added business logic methods** (`addBook()`, `removeBook()`, `canAccommodate()`)
6. **Added derived query methods** (`isEmpty()`, `getRemainingCapacity()`)
7. **Constructor validation** to prevent invalid object creation

**Usage Examples**

**Before (problematic):**

```java
// Easy to make mistakes
shelf.setBookCount(shelf.getBookCount() + 1);  // Can exceed capacity
shelf.setFull(shelf.getBookCount() >= shelf.getShelfCapacity());  // Manual sync

// Allows invalid state
shelf.setBookCount(-5);  // No error!
shelf.setBookCount(1000);  // Exceeds capacity, no error!
```

**After (safe):**

```java
// Clear intent, enforced rules
shelf.addBook();  // Validates capacity automatically

// Queries are always accurate
if (shelf.isFull()) {  // Computed from actual data
    // Find another shelf
}

// Invalid operations are prevented
shelf.setBookCount(-5);  // ❌ IllegalArgumentException
shelf.addBook();  // ❌ IllegalStateException if full
```

**Migration Impact**

**Breaking Changes:**

1. `Shelf()` default constructor must be explicitly used where needed
2. Code using `shelf.isFull()` as getter must be updated (now a computed method)
3. Code setting `isFull` directly will fail (setter removed)

**Database Migration Required:**

```sql
-- Add new columns
ALTER TABLE shelves ADD COLUMN book_count INTEGER DEFAULT 0;
ALTER TABLE shelves ADD COLUMN shelf_capacity INTEGER DEFAULT 20;
ALTER TABLE shelves ADD COLUMN shelf_description VARCHAR(500);

-- Remove derived column if it exists
ALTER TABLE shelves DROP COLUMN IF EXISTS is_full;
```

**Testing Considerations**

New test cases needed:

```java
@Test
void shouldPreventNegativeBookCount() {
    Shelf shelf = new Shelf(null, "Shelf 1", 1, 20);
    assertThrows(IllegalArgumentException.class, 
        () -> shelf.setBookCount(-1));
}

@Test
void shouldPreventExceedingCapacity() {
    Shelf shelf = new Shelf(null, "Shelf 1", 1, 10);
    shelf.setBookCount(10);
    assertThrows(IllegalStateException.class, 
        () -> shelf.addBook());
}

@Test
void shouldCalculateIsFullCorrectly() {
    Shelf shelf = new Shelf(null, "Shelf 1", 1, 5);
    assertFalse(shelf.isFull());
    
    shelf.setBookCount(5);
    assertTrue(shelf.isFull());
    
    shelf.removeBook();
    assertFalse(shelf.isFull());
}

@Test
void shouldPreventReducingCapacityBelowBookCount() {
    Shelf shelf = new Shelf(null, "Shelf 1", 1, 20);
    shelf.setBookCount(15);
    
    assertThrows(IllegalArgumentException.class,
        () -> shelf.setShelfCapacity(10));
}
```

**Lessons Learned**

1. **Derived State Should Be Computed**: `isFull` should be a method, not a field. Storing derived state creates synchronization bugs.
2. **Constructors Remove Default Constructor**: Adding any constructor requires explicitly adding default constructor if needed by frameworks.
3. **Validation Is Not Optional**: All setters must validate. Business rules should be enforced in code, not just documentation.
4. **Entity-Domain Sync**: Fields must exist in both layers. Adding to one requires adding to the other.
5. **Rich Domain Models**: Provide business methods (`addBook()`) instead of exposing raw setters.
6. **Fail Fast**: Validate immediately on input, don't wait until usage to discover invalid state.
7. **Single Source of Truth**: Any data that can be computed from other data should be computed, not stored.

**Files Modified**

- `Shelf.java` - added capacity fields, constructor, business logic methods
- `ShelfEntity.java` - added capacity fields, description field
- `BookCommands.java` - removed debug statement
- `ShelfSummary.java` - formatting cleanup

**Next Steps**

1. **Update ShelfMapper** to handle new fields
2. **Write comprehensive tests** for validation and business logic
3. **Create database migration** to add new columns
4. **Update ShelfService** to use new business methods instead of setters
5. **Add capacity checks** to book placement operations
6. **Create ShelfFactory** method to include capacity parameters

------

## Git Commit Message (Initial - Before Review)

```
feat: add capacity management to shelf domain model

- Add bookCount, shelfCapacity, and isFull fields to Shelf
- Add corresponding fields to ShelfEntity
- Add shelfDescription to ShelfEntity
- Add parameterized constructor to Shelf
- Add getters and setters for new fields
- Remove debug output from BookCommands
- Clean up ShelfSummary formatting

This enables tracking shelf capacity and prevents overfilling shelves.
```

## Git Commit Message (Corrected - After Review)

```
feat: add capacity management with validation and business logic

- Add bookCount and shelfCapacity fields to Shelf and ShelfEntity
- Add shelfDescription to both Entity and Domain Model
- Convert isFull from field to computed method (derived state)
- Add default constructor to maintain framework compatibility
- Add validated setters with business rule enforcement
- Add business logic methods: addBook(), removeBook(), canAccommodate()
- Add query methods: isEmpty(), getRemainingCapacity()
- Validate constructor parameters for data integrity
- Remove debug output from BookCommands

This change adds shelf capacity management with proper encapsulation,
validation, and business logic enforcement to prevent invalid states.

BREAKING CHANGE: isFull is now a method instead of field with setter
```