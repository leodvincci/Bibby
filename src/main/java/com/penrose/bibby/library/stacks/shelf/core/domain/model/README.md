# Shelf Domain Model

This package contains the core domain models for the **Shelf** bounded context within the Stacks module.

These classes represent pure domain concepts with no infrastructure dependencies. They enforce their own invariants through self-validating setters, keeping business rules centralized in the domain layer.

## Models

### `Shelf`

The aggregate root representing a physical shelf within a bookcase. A shelf holds a bounded collection of books and knows its position within its parent bookcase.

**Fields:**

| Field           | Type          | Description                                      |
|-----------------|---------------|--------------------------------------------------|
| `shelfId`       | `ShelfId`     | Identity value object (nullable for new shelves)  |
| `shelfLabel`    | `String`      | Human-readable label (non-blank)                  |
| `shelfPosition` | `int`         | Ordinal position within the bookcase (>= 1)       |
| `bookCapacity`  | `int`         | Maximum number of books the shelf can hold (>= 1)  |
| `books`         | `List<Long>`  | IDs of books currently placed on the shelf        |
| `bookcaseId`    | `Long`        | ID of the parent bookcase (non-null)              |

**Invariants:**

- `shelfLabel` must not be null or blank.
- `shelfPosition` must be >= 1.
- `bookCapacity` must be >= 1.
- `bookcaseId` must not be null.
- `books` must not be null.

**Domain behavior:**

- `isFull()` — returns `true` when the number of books meets or exceeds `bookCapacity`.
- `getBookCount()` — returns the current number of books on the shelf.

### `Placement`

A domain model representing the association between a book and a shelf. It captures *where* a specific book has been placed.

**Fields:**

| Field     | Type   | Description                        |
|-----------|--------|------------------------------------|
| `bookId`  | `Long` | ID of the placed book              |
| `shelfId` | `Long` | ID of the shelf the book is on     |

**Invariants:**

- `bookId` must not be null when read (`getBookId()` throws `IllegalStateException` if null).
- `bookId` must not be null when set via `setBookId()`.
- `shelfId` must not be null when set via `setShelfId()`.

## Related

- **Value object:** [`ShelfId`](../valueobject/ShelfId.java) — identity wrapper used by `Shelf`.
- **Tests:** `ShelfTest` and `PlacementTest` cover all invariants and domain behavior.
