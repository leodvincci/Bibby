
# Domain: Booklist

## What is a Booklist?
A **Booklist** is an aggregate root representing a named collection of books. It gives the collection a stable identity, a human-friendly name, and lifecycle metadata (creation and last update times). In the domain model, you change a booklist via domain operations (e.g., rename), not by “patching fields” arbitrarily.

---

## Fields

### `listId` (`BooklistId`)
* **Meaning:** Stable identity of the booklist.
* **Rule:** Required and immutable once the booklist is created.

### `booklistName` (`BooklistName`)
* **Meaning:** Display name for the list (what users see).
* **Rules (enforced by `BooklistName`):**
  * Must not be `null` or blank.
  * Must not exceed the configured max length.
  * Must contain only allowed characters: letters, digits, space, and `-_,.!?()`.
  * Whitespace is normalized (multiple spaces collapse to one) and trimmed.

### `bookIdentifier` (`Set<BookIdentifier>`)
* **Meaning:** The set of books currently included in the list.
* **Rules:**
  * Semantically a set (no duplicates).
  * Should not contain `null` elements.
  * Prefer treating it as the single source of truth for membership (add/remove operations should update `updatedAt`).

### `createdAt` (`Instant`)
* **Meaning:** When the booklist was created.
* **Rules:** Set at creation time and should not change.

### `updatedAt` (`Instant`)
* **Meaning:** When the booklist was last modified.
* **Rules:**
  * Must be `>= createdAt`.
  * Should be updated whenever the booklist is meaningfully changed (e.g., rename, membership change).

---

## Domain operations / rules

### Rename
**Operation:** `renameBooklist(newName)`
* Rejects `null` new names.
* If the new name equals the current name: no-op (does not update timestamps).
* Otherwise:
  * updates `booklistName`
  * updates `updatedAt` to “now”

---

## Notes / implementation guidance
* Prefer domain methods (rename, add/remove book) over exposing setters that can break invariants.
* If persistence/mapping needs setters, keep them package-private and validate inputs, or enforce invariants in a factory/constructor layer.
