👨‍💻 Dev Log: Centralizing Author Lookup/Creation Logic

**Date:** 2025-11-20
 **Author:** @leodvincci
 **Commit:** Refactor: centralize author lookup/creation in AuthorService
 **Context:** Follow-up to PR #46

------

## 📝 Context

After the previous refactor extracted author operations into `AuthorService`, there was still a lingering issue: **the "find or create" pattern was still living in `BookService`**.

java

```java
// Before this commit - BookService doing author orchestration
AuthorEntity author = authorService.findByAuthorFirstNameLastName(firstName, lastName);
if (author == null) {
    author = authorService.createNewAuthor(firstName, lastName);
}
```

This violated the service boundary we'd just established. If `AuthorService` owns author management, it should own *all* author management—including the decision logic for when to create vs. reuse.

This commit completes that extraction.

------

## 🛠 Technical Changes

### 1. **Introduced `findOrCreateAuthor()` in AuthorService**

Added a single method that encapsulates the entire lookup/creation flow:

java

```java
public AuthorEntity findOrCreateAuthor(String authorFirstName, String authorLastName) {
    AuthorEntity authorEntity = findByAuthorFirstNameLastName(authorFirstName, authorLastName);
    if (authorEntity == null) {
        authorEntity = createNewAuthor(authorFirstName, authorLastName);
    }
    return authorEntity;
}
```

**Why this matters:**

- The decision logic now lives in the service that owns the domain
- Callers don't need to know *how* authors are resolved—they just get one
- Reduces duplication if multiple places need this behavior

------

### 2. **Simplified `BookService.createNewBook()`**

**Before:**

java

```java
public void createNewBook(BookRequestDTO dto) {
    String firstName = dto.firstName();
    String lastName = dto.lastName();
    String title = dto.title();
    
    BookEntity book = findBookByTitleIgnoreCase(title);
    AuthorEntity author = authorService.findByAuthorFirstNameLastName(firstName, lastName);
    
    if (author == null) {
        author = authorService.createNewAuthor(firstName, lastName);
    }
    
    if (book == null) {
        book = BookFactory.createBook(title, author);
        saveBook(book);
    } else {
        System.out.println("Book Already Exists");
    }
}
```

**After:**

java

```java
@Transactional
public void createNewBook(BookRequestDTO dto) {
    BookEntity book = findBookByTitleIgnoreCase(dto.title());
    AuthorEntity author = authorService.findOrCreateAuthor(dto.firstName(), dto.lastName());
    
    if (book == null) {
        book = BookFactory.createBook(dto.title(), author);
        saveBook(book);
    } else {
        System.out.println("Book Already Exists");
    }
}
```

**Changes:**

- **-4 lines of variable extraction** (now using DTO fields directly)
- **-4 lines of author null-checking logic** (delegated to AuthorService)
- **Clearer intent**: The method now reads as "get or create the author, then handle the book"
- **Reduced cyclomatic complexity**: One less branching path to test

------

## 🔍 Reflection

### ✅ **What Improved**

**Better Encapsulation**
 `AuthorService` now truly owns its domain. Callers don't need to understand author creation semantics—they just ask for an author by name and get one.

**Reduced Duplication**
 If another part of the system needs "find or create an author" logic, it's now available as a single method call instead of copy-pasting the pattern.

**Easier to Test**
 `BookService.createNewBook()` now has one less conditional branch, making unit tests simpler. The author resolution is mockable at the service level.

**More Readable**
 The method now reads almost like plain English:

1. Find the book
2. Find or create the author
3. If no book exists, create it

------

### ⚠️ **What Still Needs Work**

**1. AuthorRepository Still in BookService**
 Even though we're no longer *using* it in `createNewBook()`, the dependency is still injected into the constructor. This should be removed:

java

```java
// Current (incorrect)
public BookService(BookRepository bookRepository, 
                   AuthorRepository authorRepository, 
                   AuthorService authorService) { ... }

// Should be
public BookService(BookRepository bookRepository, 
                   AuthorService authorService) { ... }
```

**2. Silent Failure on Duplicate Books**
 `System.out.println("Book Already Exists")` is still a code smell. This should:

- Throw a domain exception (`BookAlreadyExistsException`)
- Return a result object (`BookCreationResult`)
- Or at minimum, log properly

**3. Static Factory Pattern**
 `BookFactory` is still static, which limits testability and violates Spring's component model. Should be:

java

```java
@Component
public class BookFactory { ... }
```

------

## 📊 Impact Analysis

```
MetricBeforeAfterChange
Lines in createNewBook()~15~8-47%
Conditional branches2 (author + book)1 (book only)-50%
Service responsibilitiesMixedClear✓
Author logic locationScatteredCentralized✓
```

------

## 🎯 Key Takeaway

**Refactoring is iterative.**

The previous commit extracted `AuthorService`. This commit *completes* that extraction by moving the orchestration logic where it belongs.

Good refactoring isn't about perfection in one shot—it's about:

1. Identifying a problem
2. Making an improvement
3. Reflecting on what's left
4. Repeating

Each commit should leave the codebase **slightly better** than before, even if not perfect.

------

## 📚 Lessons for the Textbook

This demonstrates **progressive encapsulation**:

- **First refactor**: Extract the pieces (methods for find/create)
- **Second refactor**: Extract the pattern (find-or-create logic)
- **Next refactor**: Remove unused dependencies, add error handling

Students often think refactoring means "rewrite it perfectly." But real-world refactoring is incremental. Each step should:

- Be small enough to review easily
- Be safe enough to merge quickly
- Leave the code measurably better

This is what **sustainable improvement** looks like in production codebases.

------

## 🔄 Next Steps

-  Remove `AuthorRepository` dependency from `BookService`
-  Replace `System.out.println()` with proper exception handling
-  Convert `BookFactory` to a Spring `@Component`
-  Add unit tests for `AuthorService.findOrCreateAuthor()`
-  Add integration test for the full book creation flow

------

**Status:** ✅ Committed
 **Files Changed:** 2 (+11, -11)
 **Net Impact:** Cleaner service boundaries, better encapsulation