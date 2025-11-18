# Section 9: Collections Framework

**Estimated Time:** 75 minutes
**Prerequisites:** Sections 1-8 (especially Section 5: Encapsulation)
**Complexity:** ⭐⭐⭐⭐ (Intermediate-Advanced)

---

## Learning Objectives

By the end of this section, you will:

1. ✅ Understand List vs Set vs Map - when to use each collection type
2. ✅ Fix the exposed mutable collection problem in BookEntity and AuthorEntity
3. ✅ Master collection initialization patterns (ArrayList, HashSet, LinkedHashMap)
4. ✅ Learn why Set is used for @ManyToMany JPA relationships
5. ✅ Apply defensive copying to protect internal collections
6. ✅ Use Collections utility methods (unmodifiableSet, emptyList)
7. ✅ Choose appropriate collection return types from services

---

## The Collections Framework Hierarchy

Java provides a rich set of collection types organized in a hierarchy:

```
                    Iterable<E>
                        |
                  Collection<E>
                   /    |    \
                  /     |     \
              List<E>  Set<E>  Queue<E>
                |       |
         ArrayList  HashSet
         LinkedList TreeSet
         Vector     LinkedHashSet
```

**Map is separate** (doesn't extend Collection):

```
           Map<K, V>
          /    |    \
    HashMap  TreeMap  LinkedHashMap
```

---

## List vs Set vs Map: Which to Use?

| Collection Type | Characteristics | Use When | Example in Bibby |
|-----------------|-----------------|----------|------------------|
| **List** | Ordered, allows duplicates, indexed access | You need order or duplicates | `List<BookEntity>` from repository |
| **Set** | Unordered, NO duplicates, no index | You need uniqueness | `Set<AuthorEntity>` in BookEntity |
| **Map** | Key-value pairs, unique keys | You need lookups by key | `LinkedHashMap` for menu options |

---

## Issue #1: Set for @ManyToMany Relationships

### Current Code - BookEntity.java:38-44

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "books")
public class BookEntity {

    @ManyToMany
    @JoinTable(
            name = "book_authors", // the middle box
            joinColumns = @JoinColumn(name = "book_id"), // link to books
            inverseJoinColumns = @JoinColumn(name = "author_id") // link to authors
    )
    private Set<AuthorEntity> authors = new HashSet<>();  // ✅ Correctly using Set!

    public Set<AuthorEntity> getAuthors() {
        return authors;  // ❌ But this exposes the internal Set!
    }

    public void setAuthors(AuthorEntity authors) {  // ❌ Confusing name (should be addAuthor)
        this.authors.add(authors);
    }
}
```

### Why Set Instead of List?

**JPA @ManyToMany relationships should use `Set`, not `List`.**

Here's why:

#### 1. No Duplicates

A book can't have the same author twice. Sets enforce uniqueness:

```java
// With Set (correct):
Set<AuthorEntity> authors = new HashSet<>();
AuthorEntity jkRowling = new AuthorEntity("J.K.", "Rowling");
authors.add(jkRowling);
authors.add(jkRowling);  // Ignored! Set prevents duplicates
System.out.println(authors.size());  // Output: 1 ✅

// With List (wrong):
List<AuthorEntity> authors = new ArrayList<>();
authors.add(jkRowling);
authors.add(jkRowling);  // Both added! List allows duplicates
System.out.println(authors.size());  // Output: 2 ❌
```

#### 2. Database Join Table Constraint

The `book_authors` join table has a **composite primary key**:

```sql
CREATE TABLE book_authors (
    book_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id)  -- Prevents duplicates at DB level
);
```

If you use `List<AuthorEntity>`, Hibernate would try to add duplicate rows, causing a **constraint violation**.

#### 3. Bidirectional Consistency

AuthorEntity also has the relationship:

```java
// AuthorEntity.java:41-42
@ManyToMany(mappedBy = "authors")
private Set<BookEntity> books = new HashSet<>();  // ✅ Also using Set
```

**Both sides must use Set for bidirectional @ManyToMany.**

#### 4. Performance

Sets use **hashing** for fast lookups:

```java
// Checking if a book has a specific author:
Set<AuthorEntity> authors = book.getAuthors();
boolean hasRowling = authors.contains(rowling);  // O(1) with HashSet ✅

// With List:
List<AuthorEntity> authors = book.getAuthors();
boolean hasRowling = authors.contains(rowling);  // O(n) with ArrayList ❌
```

**HashSet.contains() is O(1) vs ArrayList.contains() which is O(n).**

---

## Issue #2: Exposed Mutable Collections (Critical Bug!)

### The Problem

**Your current code exposes internal collections, allowing callers to modify them directly.**

#### Current Code - BookEntity.java:50-52

```java
public Set<AuthorEntity> getAuthors() {
    return authors;  // ❌ Returns the ACTUAL internal Set!
}
```

**This is dangerous!** Here's what can go wrong:

```java
// Somewhere in your CLI or service:
BookEntity book = bookRepository.findByTitle("Harry Potter");
Set<AuthorEntity> authors = book.getAuthors();  // Gets internal Set

// ❌ BUG: Caller can modify the book's authors WITHOUT going through the entity!
authors.clear();  // DELETES ALL AUTHORS from the book!
bookRepository.save(book);  // Saves the modified book (now has zero authors)
```

**The caller bypassed your entity's encapsulation!**

### Same Problem in AuthorEntity.java:33-35

```java
public Set<BookEntity> getBooks() {
    return books;  // ❌ Exposes internal Set
}

public void setBooks(Set<BookEntity> books) {
    this.books = books;  // ❌ Allows replacing the entire Set
}
```

**Caller can do this:**

```java
AuthorEntity author = authorRepository.findByFullName("J.K. Rowling");
Set<BookEntity> books = author.getBooks();
books.clear();  // ❌ Removes all books from the author!
```

### The Solution: Defensive Copying

**Return an unmodifiable view of the collection:**

```java
import java.util.Collections;
import java.util.Set;

public class BookEntity {

    private Set<AuthorEntity> authors = new HashSet<>();

    // BEFORE (BAD):
    public Set<AuthorEntity> getAuthors() {
        return authors;  // ❌ Exposes internal Set
    }

    // AFTER (GOOD):
    public Set<AuthorEntity> getAuthors() {
        return Collections.unmodifiableSet(authors);  // ✅ Returns unmodifiable view
    }
}
```

**Now if a caller tries to modify it:**

```java
BookEntity book = bookRepository.findByTitle("Harry Potter");
Set<AuthorEntity> authors = book.getAuthors();
authors.clear();  // ❌ THROWS UnsupportedOperationException!
```

**The exception prevents accidental modification.**

### Why Not Copy the Set?

You might think: "Why not `return new HashSet<>(authors)`?"

```java
// Option 1: Copy the Set
public Set<AuthorEntity> getAuthors() {
    return new HashSet<>(authors);  // Creates a new Set with same elements
}
```

**Problems with copying:**

1. **Performance** - Creates a new Set every time you call getAuthors()
2. **Memory** - Wastes memory with duplicate collections
3. **Confusion** - Caller gets a copy, so modifications won't affect the entity (but they expect it to)

**Using `Collections.unmodifiableSet()` is better:**

```java
public Set<AuthorEntity> getAuthors() {
    return Collections.unmodifiableSet(authors);  // ✅ Returns a VIEW (no copying)
}
```

**Benefits:**

- ✅ **Fast** - No copying, just wraps the existing Set
- ✅ **Safe** - Prevents modification
- ✅ **Clear** - Caller knows they can't modify it (exception makes it obvious)

### For Adding Authors: Provide a Method

Instead of exposing the Set, provide methods to manage it:

```java
public class BookEntity {

    private Set<AuthorEntity> authors = new HashSet<>();

    // ✅ Read-only access
    public Set<AuthorEntity> getAuthors() {
        return Collections.unmodifiableSet(authors);
    }

    // ✅ Add an author (controlled)
    public void addAuthor(AuthorEntity author) {
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        this.authors.add(author);
        author.getBooks().add(this);  // Maintain bidirectional relationship
    }

    // ✅ Remove an author (controlled)
    public void removeAuthor(AuthorEntity author) {
        this.authors.remove(author);
        author.getBooks().remove(this);  // Maintain bidirectional relationship
    }

    // ❌ DELETE THIS (it's confusing and bypasses validation):
    public void setAuthors(AuthorEntity authors) {
        this.authors.add(authors);
    }
}
```

**Now the entity controls access to its authors collection.**

---

## Issue #3: LinkedHashMap for Ordered Menus

### Current Code - BookCommands.java:405-413

```java
private Map<String, String> buildSearchOptions() {
    // LinkedHashMap keeps insertion order so the menu shows in the order you add them
    Map<String, String> options = new LinkedHashMap<>();  // ✅ Correct choice!
    options.put("Show all books  — \u001B[32mView the complete library\n\u001B[0m", "all");
    options.put("Title or keyword  —  \u001B[32mSearch by words in the title\n\u001B[0m", "title");
    options.put("Author  —  \u001B[32mSearch by author name\n\u001B[0m", "author");
    options.put("Genre  —  \u001B[32mSearch by genre or category\n\u001B[0m", "genre");
    options.put("Publisher  —  \u001B[32mSearch by publisher\n\u001B[0m", "publisher");
    return options;
}
```

### Why LinkedHashMap?

**You're using `LinkedHashMap` to preserve insertion order for menu display.**

Let's compare Map implementations:

| Map Type | Ordering | Performance | Use Case |
|----------|----------|-------------|----------|
| **HashMap** | No order (random) | Fastest (O(1)) | General purpose key-value storage |
| **LinkedHashMap** | Insertion order | Slightly slower (O(1)) | When you need predictable iteration order |
| **TreeMap** | Sorted by key | Slower (O(log n)) | When you need sorted keys |

**Example showing the difference:**

```java
// HashMap (random order):
Map<String, String> hashMap = new HashMap<>();
hashMap.put("Option 1", "one");
hashMap.put("Option 2", "two");
hashMap.put("Option 3", "three");
System.out.println(hashMap.keySet());
// Output: [Option 2, Option 1, Option 3]  ❌ Random order!

// LinkedHashMap (insertion order):
Map<String, String> linkedHashMap = new LinkedHashMap<>();
linkedHashMap.put("Option 1", "one");
linkedHashMap.put("Option 2", "two");
linkedHashMap.put("Option 3", "three");
System.out.println(linkedHashMap.keySet());
// Output: [Option 1, Option 2, Option 3]  ✅ Preserves order!

// TreeMap (sorted order):
Map<String, String> treeMap = new TreeMap<>();
treeMap.put("Charlie", "c");
treeMap.put("Alice", "a");
treeMap.put("Bob", "b");
System.out.println(treeMap.keySet());
// Output: [Alice, Bob, Charlie]  ✅ Alphabetically sorted!
```

**Your use of LinkedHashMap is correct** because menu options should appear in the order you defined them.

### Same Pattern in BookcaseCommands.java:60-62

```java
private Map<String, String> bookCaseOptions() {
    // LinkedHashMap keeps insertion order so the menu shows in the order you add them
    Map<String, String> options = new LinkedHashMap<>();  // ✅ Good!
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();
    for (BookcaseEntity b : bookcaseEntities) {
        // ... build options ...
    }
    return options;
}
```

**Excellent use of LinkedHashMap for consistent menu ordering.**

---

## Issue #4: List Initialization Patterns

### Current Code - BookCommands.java:46-50

```java
List<String> bibbySearchResponses = new ArrayList<>(List.of(
        "Got it — searching the stacks for books by",
        "Sure thing — I'll take a quick look through the shelves for",
        "Looking it up — just a second while I scan the catalog for",
        "On it — I'll search the library for"
));
```

**This is a bit redundant.**

### The Problem

You're using `List.of()` to create an **immutable list**, then wrapping it in an `ArrayList` to make it **mutable again**.

```java
List.of("a", "b", "c")           // Immutable list (can't add/remove)
   ↓
new ArrayList<>(...)              // Convert to mutable ArrayList
```

**Why do this?**

1. If you DON'T need to modify the list → use `List.of()` directly
2. If you DO need to modify the list → use this pattern

### Let's Check: Do You Modify This List?

Looking at the usage... I don't see any `.add()` or `.remove()` calls on `bibbySearchResponses`. You only seem to read from it (likely with `get(random index)`).

**If you're only reading (not modifying), you should use `List.of()` directly:**

```java
// BEFORE (mutable ArrayList):
List<String> bibbySearchResponses = new ArrayList<>(List.of(
        "Got it — searching the stacks for books by",
        "Sure thing — I'll take a quick look through the shelves for",
        "Looking it up — just a second while I scan the catalog for",
        "On it — I'll search the library for"
));

// AFTER (immutable List):
List<String> bibbySearchResponses = List.of(
        "Got it — searching the stacks for books by",
        "Sure thing — I'll take a quick look through the shelves for",
        "Looking it up — just a second while I scan the catalog for",
        "On it — I'll search the library for"
);
```

**Benefits:**

- ✅ **Immutable** - Can't be accidentally modified
- ✅ **Memory efficient** - `List.of()` uses less memory than ArrayList
- ✅ **Thread-safe** - Immutable collections are inherently thread-safe
- ✅ **Clearer intent** - "This is a fixed list of responses"

### When to Use ArrayList vs List.of()

```java
// Use List.of() when the list is FIXED (never changes):
List<String> validStatuses = List.of("AVAILABLE", "CHECKED_OUT", "RESERVED");  // ✅

// Use ArrayList when you WILL modify the list:
List<String> searchResults = new ArrayList<>();  // ✅
searchResults.add("Book 1");
searchResults.add("Book 2");

// Use ArrayList when you need a mutable copy of an immutable list:
List<String> fixedList = List.of("A", "B", "C");
List<String> mutableCopy = new ArrayList<>(fixedList);  // ✅ Now I can modify it
mutableCopy.add("D");
```

### Empty Collections: Use Collections Utility

```java
// ❌ BAD: Creating empty ArrayList for no reason
public List<BookEntity> findBooksByGenre(String genre) {
    if (genre == null) {
        return new ArrayList<>();  // Wastes memory
    }
    // ... query logic ...
}

// ✅ GOOD: Use Collections.emptyList()
public List<BookEntity> findBooksByGenre(String genre) {
    if (genre == null) {
        return Collections.emptyList();  // Singleton instance, immutable
    }
    // ... query logic ...
}
```

**`Collections.emptyList()` returns a reusable immutable empty list** (more efficient than creating new ArrayLists).

---

## Issue #5: Repository Return Types

### Current Code - BookRepository.java:20-24

```java
package com.penrose.bibby.library.book;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface BookRepository extends JpaRepository <BookEntity, Long> {

    List<BookEntity> findByTitleContaining(String title);  // ✅ Returns List

    List<BookEntity> findByShelfId(Long id);  // ✅ Returns List

    List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);  // ✅ Returns List
}
```

### Why List Instead of Set?

**Spring Data JPA query methods return `List` by default.**

This is intentional because:

1. **Ordering** - Queries often return ordered results (e.g., `OrderByTitleAsc`)
2. **Duplicates** - Some queries might return duplicates (projections, joins)
3. **Indexed access** - Lists support `get(index)` for pagination

**If you want a Set from a repository:**

```java
@Repository
public interface BookRepository extends JpaRepository <BookEntity, Long> {

    Set<BookEntity> findByGenre(String genre);  // ✅ You CAN return Set

    List<BookEntity> findByShelfId(Long id);  // ✅ Or List (more common)
}
```

**General rule:**
- Use `List` for most repository methods (ordering, duplicates possible)
- Use `Set` only when uniqueness is critical and order doesn't matter

### Converting List to Set (When Needed)

```java
// Repository returns List:
List<BookEntity> bookList = bookRepository.findByGenre("Fiction");

// Convert to Set if you need uniqueness:
Set<BookEntity> uniqueBooks = new HashSet<>(bookList);  // ✅ Removes duplicates
```

---

## Issue #6: Traditional For Loop vs Enhanced For

### Current Code - BookcaseService.java:35-37

```java
for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
    addShelf(bookcaseEntity, i, i);
}
```

**This is CORRECT usage of a traditional for loop.**

### When to Use Traditional For Loop

Use `for (int i = 0; i < n; i++)` when:

1. ✅ **You need the index** (like here - `i` is used as the shelf position)
2. ✅ **You're not iterating over a collection** (here you're just looping `capacity` times)
3. ✅ **You need to skip elements** (e.g., `i += 2` for every other element)

### When to Use Enhanced For Loop

Use `for (Type item : collection)` when:

1. ✅ **You're iterating over every element** in a collection
2. ✅ **You don't need the index**
3. ✅ **Readability matters** (enhanced for is clearer)

**Example from BookcaseCommands.java:63-71:**

```java
List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();
for (BookcaseEntity b : bookcaseEntities) {  // ✅ Enhanced for (don't need index)
    int shelfBookCount = 0;
    List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());
    for(ShelfEntity s : shelves){  // ✅ Enhanced for (don't need index)
        List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
        shelfBookCount += bookList.size();
    }
    options.put(bookcaseRowFormater(b,shelfBookCount), b.getBookcaseId().toString());
}
```

**This is correct - enhanced for loops when you don't need indices.**

---

## Collections Utility Class

**Java provides `java.util.Collections` with helpful static methods.**

### Immutable Wrappers

```java
import java.util.Collections;

Set<AuthorEntity> authors = new HashSet<>();
authors.add(rowling);
authors.add(tolkien);

// Wrap in unmodifiable Set:
Set<AuthorEntity> readOnly = Collections.unmodifiableSet(authors);  // ✅

readOnly.add(new AuthorEntity("George", "Orwell"));  // ❌ Throws UnsupportedOperationException
```

### Empty Collections

```java
// Instead of:
return new ArrayList<>();  // ❌ Creates new object every time

// Use:
return Collections.emptyList();  // ✅ Returns singleton instance (more efficient)
return Collections.emptySet();   // ✅
return Collections.emptyMap();   // ✅
```

### Singleton Collections

```java
// Create a Set with ONE element:
Set<String> singleton = Collections.singleton("FICTION");  // ✅ Immutable, 1 element

// Useful for queries:
List<BookEntity> fictionBooks = bookRepository.findByGenreIn(Collections.singleton("Fiction"));
```

---

## Collection Choice Decision Tree

```
Do you need key-value pairs?
├─ YES → Map
│   ├─ Need insertion order? → LinkedHashMap ✅ (menu options in Bibby)
│   ├─ Need sorted keys? → TreeMap
│   └─ Just need fast lookup? → HashMap
│
└─ NO → Collection
    │
    ├─ Need duplicates allowed?
    │   ├─ YES → List
    │   │   ├─ Frequent add/remove at ends? → LinkedList
    │   │   └─ Random access needed? → ArrayList ✅ (most common)
    │   │
    │   └─ NO → Set
    │       ├─ JPA @ManyToMany relationship? → HashSet ✅ (BookEntity.authors)
    │       ├─ Need sorted order? → TreeSet
    │       └─ Need insertion order? → LinkedHashSet
    │
    └─ Just iterating in order? → List ✅ (repository results)
```

---

## Bibby Collection Usage Summary

### ✅ Good Patterns

1. **Set for @ManyToMany** (BookEntity.authors, AuthorEntity.books)
   ```java
   private Set<AuthorEntity> authors = new HashSet<>();  // ✅
   ```

2. **LinkedHashMap for menus** (BookCommands, BookcaseCommands)
   ```java
   Map<String, String> options = new LinkedHashMap<>();  // ✅ Preserves order
   ```

3. **List for repository results** (BookRepository, ShelfRepository)
   ```java
   List<BookEntity> findByShelfId(Long id);  // ✅ Supports ordering
   ```

4. **Enhanced for loops** (when index not needed)
   ```java
   for (BookcaseEntity b : bookcaseEntities) {  // ✅
   ```

5. **Traditional for loop** (when index IS needed)
   ```java
   for(int i = 0; i < capacity; i++){  // ✅ Need index for shelf position
   ```

### ❌ Issues to Fix

1. **Exposed mutable collections** (BookEntity.getAuthors(), AuthorEntity.getBooks())
   ```java
   return authors;  // ❌ Should return Collections.unmodifiableSet(authors)
   ```

2. **Confusing method name** (BookEntity.setAuthors)
   ```java
   public void setAuthors(AuthorEntity authors) {  // ❌ Should be addAuthor
       this.authors.add(authors);
   }
   ```

3. **Unnecessary ArrayList wrapping** (BookCommands.bibbySearchResponses)
   ```java
   new ArrayList<>(List.of(...))  // ❌ Should use List.of() directly (if immutable)
   ```

---

## Refactoring Examples

### Example 1: Fix BookEntity Collection Exposure

**BEFORE:**

```java
// BookEntity.java
package com.penrose.bibby.library.book;

import java.util.HashSet;
import java.util.Set;

public class BookEntity {

    private Set<AuthorEntity> authors = new HashSet<>();

    public Set<AuthorEntity> getAuthors() {
        return authors;  // ❌ Exposes internal Set
    }

    public void setAuthors(AuthorEntity authors) {  // ❌ Confusing name
        this.authors.add(authors);
    }
}
```

**AFTER:**

```java
// BookEntity.java
package com.penrose.bibby.library.book;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BookEntity {

    private Set<AuthorEntity> authors = new HashSet<>();

    // ✅ Returns unmodifiable view
    public Set<AuthorEntity> getAuthors() {
        return Collections.unmodifiableSet(authors);
    }

    // ✅ Clear, descriptive name
    public void addAuthor(AuthorEntity author) {
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        this.authors.add(author);
        // Maintain bidirectional relationship:
        if (!author.getBooks().contains(this)) {
            author.addBook(this);
        }
    }

    // ✅ Remove author
    public void removeAuthor(AuthorEntity author) {
        this.authors.remove(author);
        author.getBooks().remove(this);
    }

    // ❌ DELETE the old setAuthors method
}
```

### Example 2: Fix AuthorEntity Collection Exposure

**BEFORE:**

```java
// AuthorEntity.java
public class AuthorEntity {

    private Set<BookEntity> books = new HashSet<>();

    public Set<BookEntity> getBooks() {
        return books;  // ❌ Exposes internal Set
    }

    public void setBooks(Set<BookEntity> books) {
        this.books = books;  // ❌ Allows replacing the entire Set
    }
}
```

**AFTER:**

```java
// AuthorEntity.java
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AuthorEntity {

    private Set<BookEntity> books = new HashSet<>();

    // ✅ Returns unmodifiable view
    public Set<BookEntity> getBooks() {
        return Collections.unmodifiableSet(books);
    }

    // ✅ Controlled addition
    public void addBook(BookEntity book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        this.books.add(book);
        // Maintain bidirectional relationship:
        if (!book.getAuthors().contains(this)) {
            book.addAuthor(this);
        }
    }

    // ✅ Controlled removal
    public void removeBook(BookEntity book) {
        this.books.remove(book);
        book.getAuthors().remove(this);
    }

    // ❌ DELETE the old setBooks method
}
```

### Example 3: Simplify List Initialization

**BEFORE:**

```java
// BookCommands.java
List<String> bibbySearchResponses = new ArrayList<>(List.of(
        "Got it — searching the stacks for books by",
        "Sure thing — I'll take a quick look through the shelves for"
));
```

**AFTER (if you DON'T modify the list):**

```java
// BookCommands.java
List<String> bibbySearchResponses = List.of(  // ✅ Immutable, more efficient
        "Got it — searching the stacks for books by",
        "Sure thing — I'll take a quick look through the shelves for"
);
```

**OR AFTER (if you DO need to modify the list later):**

```java
// Keep the ArrayList wrapper if you add/remove items later
List<String> searchResponses = new ArrayList<>(List.of(
        "Response 1",
        "Response 2"
));
searchResponses.add("Response 3");  // ✅ Can modify because it's ArrayList
```

---

## Advanced: Collection Immutability Levels

Java offers different levels of immutability:

### 1. Modifiable (Standard Collections)

```java
List<String> list = new ArrayList<>();
list.add("A");  // ✅ Can add
list.remove(0); // ✅ Can remove
list.set(0, "B");  // ✅ Can modify
```

### 2. Unmodifiable View (Collections.unmodifiable*)

```java
List<String> original = new ArrayList<>();
original.add("A");

List<String> unmodifiable = Collections.unmodifiableList(original);
unmodifiable.add("B");  // ❌ Throws UnsupportedOperationException

// BUT: Original can still be modified!
original.add("B");  // ✅ Works (original is still mutable)
System.out.println(unmodifiable);  // Output: [A, B] (reflects changes to original!)
```

**`Collections.unmodifiableList()` is a VIEW, not a copy.**

### 3. Truly Immutable (List.of, Set.of, Map.of)

```java
List<String> immutable = List.of("A", "B", "C");
immutable.add("D");  // ❌ Throws UnsupportedOperationException
immutable.set(0, "Z");  // ❌ Throws UnsupportedOperationException

// There's NO underlying mutable list - this IS the data
```

**`List.of()` creates a truly immutable list** (not a view).

### When to Use Which?

| Scenario | Use | Example |
|----------|-----|---------|
| Need to protect internal collection | `Collections.unmodifiableSet()` | `return Collections.unmodifiableSet(authors);` |
| Need a fixed constant list | `List.of()` | `List<String> VALID_STATUSES = List.of("AVAILABLE", "RESERVED");` |
| Need to build and then freeze | Create → wrap | `List<String> list = new ArrayList<>(); list.add(...); return Collections.unmodifiableList(list);` |

---

## Common Collection Mistakes

### Mistake #1: Returning Null Instead of Empty Collection

```java
// ❌ BAD:
public List<BookEntity> findBooksByGenre(String genre) {
    if (genre == null) {
        return null;  // Forces caller to null-check
    }
    return bookRepository.findByGenre(genre);
}

// Caller has to do this:
List<BookEntity> books = service.findBooksByGenre("Fiction");
if (books != null) {  // ❌ Annoying null check
    for (BookEntity book : books) {
        // ...
    }
}

// ✅ GOOD:
public List<BookEntity> findBooksByGenre(String genre) {
    if (genre == null) {
        return Collections.emptyList();  // Returns empty list (not null)
    }
    return bookRepository.findByGenre(genre);
}

// Caller doesn't need null check:
List<BookEntity> books = service.findBooksByGenre("Fiction");
for (BookEntity book : books) {  // ✅ Works even if empty
    // ...
}
```

**Never return null from methods that return collections. Return empty collections instead.**

### Mistake #2: Using Wrong Collection Type

```java
// ❌ BAD: Using List for unique items
List<String> genres = new ArrayList<>();
genres.add("Fiction");
genres.add("Fiction");  // Oops, duplicate!
System.out.println(genres.size());  // Output: 2 (but genres should be unique!)

// ✅ GOOD: Use Set for unique items
Set<String> genres = new HashSet<>();
genres.add("Fiction");
genres.add("Fiction");  // Ignored (Set prevents duplicates)
System.out.println(genres.size());  // Output: 1 ✅
```

### Mistake #3: Modifying Collection While Iterating

```java
// ❌ BAD: ConcurrentModificationException!
List<BookEntity> books = new ArrayList<>();
books.add(book1);
books.add(book2);

for (BookEntity book : books) {
    if (book.getStatus().equals("ARCHIVED")) {
        books.remove(book);  // ❌ CRASH! ConcurrentModificationException
    }
}

// ✅ GOOD: Use iterator.remove()
Iterator<BookEntity> iterator = books.iterator();
while (iterator.hasNext()) {
    BookEntity book = iterator.next();
    if (book.getStatus().equals("ARCHIVED")) {
        iterator.remove();  // ✅ Safe removal
    }
}

// ✅ ALSO GOOD: Use removeIf (Java 8+)
books.removeIf(book -> book.getStatus().equals("ARCHIVED"));  // ✅ Clean and safe
```

### Mistake #4: Using HashMap When Order Matters

```java
// ❌ BAD: HashMap doesn't preserve order
Map<String, String> options = new HashMap<>();
options.put("Option 1", "one");
options.put("Option 2", "two");
options.put("Option 3", "three");

for (String key : options.keySet()) {
    System.out.println(key);
}
// Output: Option 2, Option 1, Option 3  ❌ Random order!

// ✅ GOOD: LinkedHashMap preserves insertion order
Map<String, String> options = new LinkedHashMap<>();
options.put("Option 1", "one");
options.put("Option 2", "two");
options.put("Option 3", "three");

for (String key : options.keySet()) {
    System.out.println(key);
}
// Output: Option 1, Option 2, Option 3  ✅ Correct order!
```

---

## Testing Your Understanding

### Question 1: Collection Type Selection

For each scenario, choose the appropriate collection type:

1. Store unique author IDs from a query → **Answer:** `Set<Long>` (uniqueness)
2. Store menu options in display order → **Answer:** `LinkedHashMap<String, String>` (order + key-value)
3. Store search results that might have duplicates → **Answer:** `List<BookEntity>` (allows duplicates)
4. Store book genres sorted alphabetically → **Answer:** `TreeSet<String>` (sorted + unique)
5. Store temporary search terms → **Answer:** `ArrayList<String>` (order, allows duplicates)

### Question 2: Spot the Bug

```java
public class BookEntity {
    private Set<AuthorEntity> authors = new HashSet<>();

    public Set<AuthorEntity> getAuthors() {
        return authors;  // ❌ What's wrong?
    }
}
```

**Answer:** Exposes internal mutable collection. Callers can modify it directly. Should return `Collections.unmodifiableSet(authors)`.

### Question 3: Immutability Question

What's the difference between these two approaches?

```java
// Approach 1:
List<String> list1 = Collections.unmodifiableList(new ArrayList<>(List.of("A", "B")));

// Approach 2:
List<String> list2 = List.of("A", "B");
```

**Answer:**
- Both are unmodifiable from the caller's perspective
- `List.of()` (Approach 2) is more efficient (no ArrayList creation, no wrapping)
- `List.of()` is truly immutable; `Collections.unmodifiableList()` is just a view

**Use Approach 2** (simpler and more efficient).

---

## Action Items for Your Codebase

### Priority 1: Critical Fixes (Encapsulation Violations)

- [ ] **Fix BookEntity collection exposure** (BookEntity.java:50)
  ```java
  // Change:
  public Set<AuthorEntity> getAuthors() {
      return authors;  // ❌
  }

  // To:
  public Set<AuthorEntity> getAuthors() {
      return Collections.unmodifiableSet(authors);  // ✅
  }
  ```

- [ ] **Fix AuthorEntity collection exposure** (AuthorEntity.java:33)
  ```java
  // Change:
  public Set<BookEntity> getBooks() {
      return books;  // ❌
  }

  // To:
  public Set<BookEntity> getBooks() {
      return Collections.unmodifiableSet(books);  // ✅
  }
  ```

- [ ] **Rename confusing method** (BookEntity.java:54)
  ```java
  // Change:
  public void setAuthors(AuthorEntity authors) {
      this.authors.add(authors);
  }

  // To:
  public void addAuthor(AuthorEntity author) {
      if (author == null) {
          throw new IllegalArgumentException("Author cannot be null");
      }
      this.authors.add(author);
  }
  ```

### Priority 2: Improvements

- [ ] **Add author removal method** to BookEntity
  ```java
  public void removeAuthor(AuthorEntity author) {
      this.authors.remove(author);
      author.getBooks().remove(this);  // Maintain bidirectional relationship
  }
  ```

- [ ] **Add book management methods** to AuthorEntity
  ```java
  public void addBook(BookEntity book) { /* ... */ }
  public void removeBook(BookEntity book) { /* ... */ }
  ```

- [ ] **Simplify List initialization** (BookCommands.java:46)
  ```java
  // If you DON'T modify the list later:
  List<String> bibbySearchResponses = List.of(
      "Got it — searching the stacks for books by",
      "Sure thing — I'll take a quick look through the shelves for"
  );
  ```

### Priority 3: Best Practices

- [ ] **Use Collections.emptyList()** instead of `new ArrayList<>()` for empty returns
- [ ] **Use Collections.singleton()** for single-element sets in queries
- [ ] **Consider immutable collections** for constants (List.of, Set.of, Map.of)

---

## Key Concepts Recap

| Concept | What It Means | Example in Bibby |
|---------|---------------|------------------|
| **List** | Ordered, allows duplicates, indexed | `List<BookEntity>` from repositories |
| **Set** | Unordered, NO duplicates, fast lookup | `Set<AuthorEntity>` in BookEntity |
| **Map** | Key-value pairs, unique keys | `LinkedHashMap` for menu options |
| **ArrayList** | Resizable array, fast random access | General-purpose list |
| **HashSet** | Hash-based set, O(1) contains() | JPA @ManyToMany relationships |
| **LinkedHashMap** | Preserves insertion order | Menu options in CLI commands |
| **Defensive Copy** | Protecting internal collections | `Collections.unmodifiableSet()` |
| **List.of()** | Immutable list factory (Java 9+) | Fixed constants |
| **Collections Utilities** | Helper methods for collections | `emptyList()`, `unmodifiableSet()` |
| **Enhanced For Loop** | `for (Type item : collection)` | Iterating without needing index |
| **Traditional For Loop** | `for (int i = 0; i < n; i++)` | When you need the index |

---

## Summary

### What You Learned

1. ✅ **List vs Set vs Map** - When to use each collection type
2. ✅ **HashSet for @ManyToMany** - Why Set is required for JPA bidirectional relationships
3. ✅ **Defensive copying** - Using `Collections.unmodifiableSet()` to protect internal collections
4. ✅ **LinkedHashMap for order** - Preserving insertion order for menu options
5. ✅ **List.of() vs ArrayList** - Immutable vs mutable initialization patterns
6. ✅ **Enhanced vs traditional for loops** - When to use each
7. ✅ **Collections utilities** - `emptyList()`, `unmodifiableSet()`, `singleton()`

### Critical Issues Found

| Issue | Location | Severity | Fix |
|-------|----------|----------|-----|
| Exposed mutable Set | BookEntity.getAuthors() | **CRITICAL** | Return `Collections.unmodifiableSet(authors)` |
| Exposed mutable Set | AuthorEntity.getBooks() | **CRITICAL** | Return `Collections.unmodifiableSet(books)` |
| Confusing method name | BookEntity.setAuthors() | Medium | Rename to `addAuthor()` |
| Unnecessary ArrayList wrap | BookCommands:46 | Low | Use `List.of()` if immutable |

### Good Patterns Identified

✅ Set for @ManyToMany relationships (BookEntity, AuthorEntity)
✅ LinkedHashMap for ordered menus (BookCommands, BookcaseCommands)
✅ List return types from repositories (supports ordering)
✅ Enhanced for loops when index not needed
✅ Traditional for loops when index IS needed (BookcaseService:35)

---

## What's Next?

In **Section 10: Generics & Type Safety**, we'll examine:
- Generic type parameters in `JpaRepository<BookEntity, Long>`
- Creating type-safe generic methods
- Bounded type parameters (`<T extends Comparable<T>>`)
- Type erasure and its implications
- Wildcard types (`? extends`, `? super`)

**Your Progress:**
- ✅ Sections 1-9 Complete (27% of mentorship guide)
- ⏳ 24 sections remaining

---

**Section 9 Complete! 🎉**

**Time to refactor:** Start with Priority 1 fixes (defensive copying for BookEntity and AuthorEntity), then rename `setAuthors` to `addAuthor`.

**Questions?** Review the "Testing Your Understanding" section above.

**Ready for Section 10?** Reply **"yes"** or **"continue"**.
