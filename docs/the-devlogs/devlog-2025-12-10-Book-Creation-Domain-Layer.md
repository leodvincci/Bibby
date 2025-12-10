# Devlog: Route Book Creation Through Domain Layer

**Date:** 2025-12-10  
**Modules:** Book, CLI, Web  
**Type:** Refactoring  
**Status:** Complete

---

## Summary

Refactored book creation flow to properly route through the domain layer. Authors are now saved before book creation, BookController uses the facade instead of the service directly, and the BookDomainRepository was expanded with several new methods for persistence operations.

---

## Part 1: CLI Command Changes

### Command Renamed
```java
// Before
@Command(command = "register", description = "register a new book to your library")

// After
@Command(command = "new", description = "register a new book to your library")
```

### Author Saving Fixed

**Before:** Authors were passed to `BookRequestDTO` but not explicitly saved first.

**After:** Each author is saved via the facade before being added to the list:
```java
for (int i = 0; i < authorCount; i++) {
    authors.add(authorFacade.saveAuthor(cliPrompt.promptForAuthor()));
    log.info("Author added: {}", authors.get(i));
}
```

This ensures authors exist in the database with valid IDs before the book references them.

### Unused Dependency Removed
```java
// Removed
private final PromptFacade promptFacade;
```
`CliPromptService` is used directly; `PromptFacade` was redundant.

### Shelf Assignment Clarified
```java
// Before
bookFacade.setShelfForBook(bookDTO.id(), shelfId);

// After
bookFacade.updateTheBooksShelf(bookDTO, newShelfId);
```
The new method name is clearer about intent, and it takes the full DTO rather than just the ID.

---

## Part 2: BookFacade & BookService

### New Method: updateTheBooksShelf()

**BookFacade interface:**
```java
void updateTheBooksShelf(BookDTO bookDTO, Long newShelfId);
```

**BookService implementation:**
```java
@Override
public void updateTheBooksShelf(BookDTO bookDTO, Long newShelfId) {
    Book book = bookMapper.toDomainFromDTO(bookDTO);
    bookDomainRepository.updateTheBooksShelf(book, bookDTO.id(), newShelfId);
}
```

This routes the shelf update through the domain layer rather than directly manipulating entities.

### BookRequestDTO Field Reorder

```java
// Before
public record BookRequestDTO(String title, List<AuthorDTO> authors, String isbn) {}

// After  
public record BookRequestDTO(String title, String isbn, List<AuthorDTO> authors) {}
```

Putting ISBN before authors groups the simple fields together.

---

## Part 3: BookDomainRepository Expansion

### New Methods Added

```java
public interface BookDomainRepository {
    // Existing
    List<Book> getBooksByShelfId(Long shelfId);
    void registerBook(Book book);
    void updateBook(Book book);
    BookEntity findBookEntityByTitle(String bookTitle);
    
    // New
    BookEntity getBookById(Long id);
    void updateTheBooksShelf(Book book, Long bookId, Long newShelfId);
    void updateAvailabilityStatus(String bookTitle);
    void registerBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId);
    BookEntity findBookByIsbn(String isbn);
    BookDetailView getBookDetailView(Long bookId);
}
```

### updateTheBooksShelf() Implementation

```java
@Override
public void updateTheBooksShelf(Book book, Long bookId, Long newShelfId) {
    Optional<BookEntity> bookEntity = bookJpaRepository.findById(bookId);

    if (bookEntity.isEmpty()) {
        log.error("Book with id {} not found", bookId);
        throw new RuntimeException("Book not found with id: " + bookId);
    }

    bookEntity.get().setShelfId(newShelfId);
    log.info("Updated shelf id for book: {} to {}", book.getTitle(), newShelfId);

    bookJpaRepository.save(bookEntity.get());
}
```

### registerBook() Double-Save

```java
@Override
public void registerBook(Book book) {
    BookEntity bookEntity = new BookEntity();
    // ... set fields ...
    bookJpaRepository.save(bookEntity);  // First save - generates ID
    
    Set<AuthorEntity> authorEntities = bookMapper.toEntitySetFromAuthorRefs(book.getAuthors());
    bookEntity.setAuthors(authorEntities);
    bookJpaRepository.save(bookEntity);  // Second save - with authors
}
```

**Why two saves?** The book needs an ID before authors can be associated (JPA relationship requires both entities to be persisted). This is a common pattern for bidirectional relationships.

---

## Part 4: BookMapper Addition

### toDomainFromBookRequestDTO()

```java
public Book toDomainFromBookRequestDTO(BookRequestDTO bookRequestDTO) {
    if (bookRequestDTO == null) {
        return null;
    }
    List<AuthorRef> authors = new ArrayList<>();
    for (AuthorDTO author : bookRequestDTO.authors()) {
        String firstName = author.firstName();
        String lastName = author.lastName();
        Long id = author.id();
        authors.add(new AuthorRef(id, new AuthorName(firstName, lastName)));
    }
    Book book = new Book();
    book.setTitle(new Title(bookRequestDTO.title()));
    book.setIsbn(new Isbn(bookRequestDTO.isbn()));
    book.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
    book.setAuthors(authors);
    return book;
}
```

This creates a domain `Book` from the request DTO, which can then be passed to the domain repository for persistence.

---

## Part 5: BookController Uses Facade

### Before
```java
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookService.createNewBook(requestDTO);  // Direct service call
    return ResponseEntity.ok("Book Added Successfully");
}
```

### After
```java
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookFacade.createNewBook(requestDTO);  // Through facade
    return ResponseEntity.ok("Book Added Successfully");
}
```

Controllers should use facades, not services. The facade is the public API; the service is an implementation detail.

---

## Part 6: Logging Enabled

```properties
# Before
logging.level.root=OFF
logging.level.org.springframework=OFF
logging.level.org.hibernate=OFF

# After
logging.level.root=INFO
logging.level.org.springframework=INFO
logging.level.org.hibernate=INFO
```

Enabled for debugging during development. Remember to tune these down for production.

---

## Architecture Flow

### Book Creation (CLI)

```
BookCommands.registerBook()
    │
    ├── authorFacade.saveAuthor() [for each author]
    │
    ├── new BookRequestDTO(title, isbn, authors)
    │
    └── bookFacade.createNewBook(requestDTO)
            │
            └── bookService.createNewBook(requestDTO)
                    │
                    ├── bookMapper.toDomainFromBookRequestDTO()
                    │
                    └── bookDomainRepository.registerBook(book)
                            │
                            └── bookJpaRepository.save(entity)
```

### Shelf Assignment (CLI)

```
BookCommands.addToShelf()
    │
    ├── bookFacade.findBookByTitle()
    │
    ├── shelfFacade.findShelfById() [capacity check]
    │
    └── bookFacade.updateTheBooksShelf(bookDTO, newShelfId)
            │
            └── bookService.updateTheBooksShelf()
                    │
                    └── bookDomainRepository.updateTheBooksShelf()
                            │
                            └── bookJpaRepository.save()
```

---

## Files Changed

### CLI
- `BookCommands.java` — Removed PromptFacade, renamed command, save authors, use updateTheBooksShelf

### Book Module
- `BookFacade.java` — Added updateTheBooksShelf()
- `BookService.java` — Implemented updateTheBooksShelf()
- `BookDomainRepository.java` — Added 6 new method signatures
- `BookDomainRepositoryImpl.java` — Implemented new methods
- `BookMapper.java` — Added toDomainFromBookRequestDTO()
- `BookRequestDTO.java` — Reordered fields

### Web
- `BookController.java` — Use BookFacade instead of BookService

### Config
- `application.properties` — Enabled logging

---

## Interview Talking Points

**"Why save authors before the book?"**
> JPA needs entities to be persisted before they can be referenced in relationships. By saving authors first via the facade, they get database IDs. Then the book can reference them properly. It's a sequencing requirement of relational persistence.

**"Why does registerBook() save twice?"**
> The first save generates the book's ID. The second save establishes the many-to-many relationship with authors. JPA's relationship management requires both sides to be persisted. It's a common pattern—you could also use cascade settings, but explicit saves are clearer.

**"Why move from setShelfForBook to updateTheBooksShelf?"**
> The old method took an ID and did entity manipulation directly. The new method takes a DTO, converts to domain, and routes through the domain repository. It's more aligned with DDD—business operations should go through the domain layer, not bypass it.

---

## Outstanding Work

1. **Empty method implementations** — `updateAvailabilityStatus()` and `registerBookFromMetaData()` are stubs
2. **Remove debug logging** — Tune down INFO logging before production
3. **Multiple copies handling** — TODO comment about books with same title
4. **Author brackets in display** — Still showing `[Sam Newman]` instead of `Sam Newman`

---

## Commit

```
refactor(book): route book creation through domain layer

- Remove unused PromptFacade dependency from BookCommands
- Rename 'register' command to 'new'
- Save authors before book creation in registration flow
- Add updateTheBooksShelf() to BookFacade and BookService
- Expand BookDomainRepository with new methods
- Update BookController to use BookFacade instead of BookService
- Add toDomainFromBookRequestDTO() mapper method
- Enable logging for debugging
- Reorder BookRequestDTO fields (title, isbn, authors)
```
