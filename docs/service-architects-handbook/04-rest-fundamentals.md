# Chapter 4: REST Fundamentals

## Introduction: Your First API Design Decision

Bibby has a `BookController.java` with this endpoint:

```java
@GetMapping("api/v1/books")
public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
    System.out.println("Controller Search For " + requestDTO.title());
    bookService.findBookByTitle(requestDTO.title());
}
```

If you're experienced with REST, alarm bells should be going off. This endpoint:
- Uses `GET` but accepts a `@RequestBody` (breaks HTTP spec)
- Returns `void` instead of the search results
- Doesn't use HTTP status codes
- Doesn't follow resource-oriented URL conventions

**This works in development.** You can call it with curl, it runs, it searches for books. But it violates core REST principles, and in production with microservices, these violations cause cascading problems:
- Load balancers strip request bodies from GET requests
- Clients can't cache GET requests safely
- Retry logic breaks (is this safe to retry?)
- API documentation tools fail to understand it

In this chapter, I'll teach you REST fundamentals by refactoring Bibby's controllers into a production-ready API. You'll learn why HTTP semantics matter, how to model resources correctly, and what idempotency actually means in practice.

## What Is REST?

**REST** (Representational State Transfer) is an architectural style for distributed systems, introduced by Roy Fielding in his 2000 PhD dissertation.

**The core idea**: Model your system as resources (nouns) that you interact with using standard HTTP verbs (actions).

```
Resources (Nouns)    +    HTTP Verbs (Actions)    =    API Operations
     Books                      GET                       Get books
     Books                      POST                      Create book
     Book {id}                  PUT                       Update book
     Book {id}                  DELETE                    Delete book
```

**Key principles**:
1. **Resources are identified by URLs** → `/books/42` identifies book with ID 42
2. **Standard methods have defined semantics** → GET is safe, PUT is idempotent
3. **Resources have representations** → Same book as JSON, XML, or HTML
4. **Stateless interactions** → Each request contains all needed information
5. **Hypermedia** → Responses include links to related resources (HATEOAS)

## Resources: The Heart of REST

A **resource** is anything you can name. In Bibby:
- **Books** (collection resource)
- **Book** (individual resource)
- **Shelves**
- **Shelf**
- **Bookcases**
- **Authors**
- **Checkouts** (yes, actions can be resources!)

### Resource Identification with URIs

**Good URIs for Bibby**:

```
/books                         → Collection of all books
/books/42                      → Book with ID 42
/books/42/authors              → Authors of book 42
/shelves                       → Collection of shelves
/shelves/7                     → Shelf with ID 7
/shelves/7/books               → Books on shelf 7
/bookcases                     → Collection of bookcases
/bookcases/3/shelves           → Shelves in bookcase 3
/bookcases/3/shelves/7/books   → Books on shelf 7 in bookcase 3
```

**Bad URIs** (from Bibby's current code):

```java
// BookCaseController.java
@PostMapping("/create/bookcase")  // ❌ Verb in URL ("create")
```

**Why is this bad?** REST uses HTTP verbs to express actions. The URL should identify *what* (the resource), not *what to do* (the action).

**Fixed**:
```java
@PostMapping("/bookcases")  // ✅ Resource name, verb is in HTTP method
```

### URI Design Patterns for Bibby

Let's design proper URIs for all of Bibby's resources:

**Books**:
```
GET    /books                  → List all books
GET    /books?shelf=7          → Filter: books on shelf 7
GET    /books?status=available → Filter: available books
GET    /books?author=Penrose   → Search: books by author
POST   /books                  → Create new book
GET    /books/{id}             → Get book details
PUT    /books/{id}             → Replace book (full update)
PATCH  /books/{id}             → Update book (partial)
DELETE /books/{id}             → Delete book
```

**Book Sub-resources**:
```
GET    /books/{id}/authors     → Authors of this book
POST   /books/{id}/authors     → Add author to book
DELETE /books/{id}/authors/{authorId}  → Remove author
```

**Shelves**:
```
GET    /shelves                → List all shelves
GET    /shelves?bookcase=3     → Shelves in bookcase 3
POST   /shelves                → Create shelf
GET    /shelves/{id}           → Get shelf details
GET    /shelves/{id}/books     → Books on this shelf
PUT    /shelves/{id}           → Update shelf
DELETE /shelves/{id}           → Delete shelf
```

**Bookcases**:
```
GET    /bookcases              → List all bookcases
POST   /bookcases              → Create bookcase
GET    /bookcases/{id}         → Get bookcase details
GET    /bookcases/{id}/shelves → Shelves in this bookcase
PUT    /bookcases/{id}         → Update bookcase
DELETE /bookcases/{id}         → Delete bookcase
```

**Actions as Resources** (Checkouts):
```
GET    /checkouts              → List all checkouts
POST   /checkouts              → Check out a book
GET    /checkouts/{id}         → Get checkout details
PUT    /checkouts/{id}         → Update checkout (extend due date)
DELETE /checkouts/{id}         → Check in (return book)
```

## HTTP Verbs and Their Semantics

Let's refactor Bibby's `BookController` to use HTTP verbs correctly.

### GET: Retrieve Resources

**Definition**: Retrieve a representation of a resource. **Safe** and **Idempotent**.

**Current Bibby code (WRONG)**:
```java
@GetMapping("api/v1/books")
public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){  // ❌ GET with body
    bookService.findBookByTitle(requestDTO.title());
}
```

**Problems**:
1. **GET shouldn't have a request body** (HTTP spec discourages it, many proxies strip it)
2. **Returns `void`** (should return the book!)
3. **No status code** (should return 404 if not found)

**Fixed**:
```java
@GetMapping("/books")
public ResponseEntity<List<BookDto>> getBooks(
    @RequestParam(required = false) String title,
    @RequestParam(required = false) String author,
    @RequestParam(required = false) Long shelfId,
    @RequestParam(required = false) BookStatus status
) {
    List<BookEntity> books;

    if (title != null) {
        books = bookService.findByTitle(title);
    } else if (author != null) {
        books = bookService.findByAuthor(author);
    } else if (shelfId != null) {
        books = bookService.findByShelfId(shelfId);
    } else if (status != null) {
        books = bookService.findByStatus(status);
    } else {
        books = bookService.findAll();
    }

    if (books.isEmpty()) {
        return ResponseEntity.noContent().build();  // 204 No Content
    }

    List<BookDto> dtos = books.stream()
        .map(BookMapper::toDto)
        .collect(Collectors.toList());

    return ResponseEntity.ok(dtos);  // 200 OK
}
```

**Get single book**:
```java
@GetMapping("/books/{id}")
public ResponseEntity<BookDto> getBook(@PathVariable Long id) {
    Optional<BookEntity> book = bookService.findBookById(id);

    return book
        .map(BookMapper::toDto)
        .map(ResponseEntity::ok)              // 200 OK
        .orElse(ResponseEntity.notFound().build());  // 404 Not Found
}
```

**GET is safe**: Calling it multiple times doesn't change server state. It's safe to cache, safe to retry, safe for browsers to prefetch.

### POST: Create Resources

**Definition**: Create a new resource. **NOT idempotent** (calling twice creates two resources).

**Current Bibby code**:
```java
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookService.createNewBook(requestDTO);
    return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
}
```

**What's wrong**:
1. **Returns 200 OK** (should return 201 Created for resource creation)
2. **Doesn't return the created book** (client doesn't know the ID)
3. **Doesn't include Location header** (where to find the new resource)

**Fixed**:
```java
@PostMapping("/books")
public ResponseEntity<BookDto> createBook(@RequestBody BookRequestDTO request) {
    // Validate request
    if (request.title() == null || request.title().isEmpty()) {
        return ResponseEntity.badRequest().build();  // 400 Bad Request
    }

    // Check if book already exists (by ISBN or title)
    if (bookService.existsByIsbn(request.isbn())) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();  // 409 Conflict
    }

    // Create book
    BookEntity created = bookService.createNewBook(request);
    BookDto dto = BookMapper.toDto(created);

    // Return 201 Created with Location header
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.getBookId())
        .toUri();

    return ResponseEntity.created(location).body(dto);
}
```

**Response**:
```http
HTTP/1.1 201 Created
Location: /books/42
Content-Type: application/json

{
  "bookId": 42,
  "title": "Clean Code",
  "isbn": "978-0132350884",
  "authors": [...]
}
```

**POST is NOT idempotent**: Calling it twice creates two books with different IDs.

### PUT: Replace Resources

**Definition**: Replace a resource entirely. **Idempotent** (calling multiple times has same effect).

**Bibby doesn't have this yet. Let's add it**:

```java
@PutMapping("/books/{id}")
public ResponseEntity<BookDto> replaceBook(
    @PathVariable Long id,
    @RequestBody BookRequestDTO request
) {
    Optional<BookEntity> existing = bookService.findBookById(id);

    if (existing.isEmpty()) {
        // PUT can create if resource doesn't exist
        BookEntity created = bookService.createBookWithId(id, request);
        return ResponseEntity.created(URI.create("/books/" + id))
                             .body(BookMapper.toDto(created));  // 201 Created
    }

    // Replace entire book
    BookEntity updated = bookService.replaceBook(id, request);
    return ResponseEntity.ok(BookMapper.toDto(updated));  // 200 OK
}
```

**PUT is idempotent**:
```
PUT /books/42 {"title": "Clean Code", "isbn": "978-0132350884"}
Result: Book 42 = {title: "Clean Code", isbn: "978-0132350884"}

PUT /books/42 {"title": "Clean Code", "isbn": "978-0132350884"}  (again)
Result: Book 42 = {title: "Clean Code", isbn: "978-0132350884"}  (same!)
```

Calling it 1 time or 100 times has the same effect.

**Important**: PUT replaces the ENTIRE resource. If you omit a field, it gets cleared:

```java
PUT /books/42 {"title": "Clean Code"}
// Result: Book 42 now has ONLY title, isbn is CLEARED
```

### PATCH: Partial Updates

**Definition**: Update part of a resource. **May or may not be idempotent** (depends on implementation).

**Bibby example: Update book status**:

```java
@PatchMapping("/books/{id}")
public ResponseEntity<BookDto> updateBook(
    @PathVariable Long id,
    @RequestBody Map<String, Object> updates
) {
    Optional<BookEntity> existing = bookService.findBookById(id);

    if (existing.isEmpty()) {
        return ResponseEntity.notFound().build();  // 404 Not Found
    }

    BookEntity book = existing.get();

    // Apply partial updates
    if (updates.containsKey("title")) {
        book.setTitle((String) updates.get("title"));
    }
    if (updates.containsKey("bookStatus")) {
        book.setBookStatus((String) updates.get("bookStatus"));
    }
    if (updates.containsKey("shelfId")) {
        book.setShelfId((Long) updates.get("shelfId"));
    }

    BookEntity updated = bookService.updateBook(book);
    return ResponseEntity.ok(BookMapper.toDto(updated));  // 200 OK
}
```

**Better: JSON Patch (RFC 6902)**:

```java
@PatchMapping("/books/{id}")
public ResponseEntity<BookDto> patchBook(
    @PathVariable Long id,
    @RequestBody JsonPatch patch
) {
    BookEntity book = bookService.findBookById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

    BookDto dto = BookMapper.toDto(book);
    BookDto patched = patch.apply(dto);
    BookEntity updated = bookService.updateBook(id, patched);

    return ResponseEntity.ok(BookMapper.toDto(updated));
}
```

**Request**:
```json
PATCH /books/42
Content-Type: application/json-patch+json

[
  {"op": "replace", "path": "/bookStatus", "value": "CHECKED_OUT"},
  {"op": "replace", "path": "/checkoutCount", "value": 5}
]
```

### DELETE: Remove Resources

**Definition**: Delete a resource. **Idempotent** (deleting something already deleted succeeds).

```java
@DeleteMapping("/books/{id}")
public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
    Optional<BookEntity> existing = bookService.findBookById(id);

    if (existing.isEmpty()) {
        return ResponseEntity.noContent().build();  // 204 No Content (already gone)
    }

    bookService.deleteBook(id);
    return ResponseEntity.noContent().build();  // 204 No Content
}
```

**DELETE is idempotent**:
```
DELETE /books/42  → 204 No Content (deleted)
DELETE /books/42  → 204 No Content (already deleted, still success)
DELETE /books/42  → 204 No Content (still idempotent)
```

**Alternative**: Return 404 after first delete:
```java
if (existing.isEmpty()) {
    return ResponseEntity.notFound().build();  // 404 Not Found
}
```

Both approaches are valid. Choose based on your API's semantics.

## Idempotency and Safety

These are critical concepts for distributed systems.

### Safe Methods

**Definition**: A safe method doesn't change server state. You can call it without fear of side effects.

**Safe methods**: GET, HEAD, OPTIONS

**Example from Bibby**:
```java
// This is SAFE (can call 1000 times, nothing changes)
GET /books/42
GET /books/42
GET /books/42
// Book 42 is unchanged
```

**Why this matters in microservices**:
- **Caching**: Safe methods can be cached aggressively
- **Prefetching**: Browsers/proxies can speculatively call safe methods
- **Retries**: Safe to retry on timeout without checking if previous request succeeded

### Idempotent Methods

**Definition**: An idempotent method produces the same result no matter how many times you call it.

**Idempotent methods**: GET, PUT, DELETE, HEAD, OPTIONS
**NOT idempotent**: POST, PATCH (usually)

**Example from Bibby**:

```java
// Idempotent (PUT): Replace book 42 with this data
PUT /books/42 {"title": "Clean Code", "status": "AVAILABLE"}
PUT /books/42 {"title": "Clean Code", "status": "AVAILABLE"}
PUT /books/42 {"title": "Clean Code", "status": "AVAILABLE"}
// Result is the same every time

// NOT Idempotent (POST): Create a book
POST /books {"title": "Clean Code"}
POST /books {"title": "Clean Code"}
POST /books {"title": "Clean Code"}
// Creates 3 different books with 3 different IDs!
```

**Why this matters in microservices**:

When a network request times out, you don't know if it succeeded:

```
Client → Service: POST /books {"title": "Clean Code"}
        ← (timeout, no response)

Did the book get created? You don't know!

If idempotent: Retry safely.
If NOT idempotent: Can't retry (might create duplicate).
```

### Making POST Idempotent: Idempotency Keys

**Problem**: Checking out a book with POST is not idempotent.

```java
POST /checkouts {"bookId": 42, "userId": 7}
// Network timeout... did it work?
// If you retry, you might check out the book twice!
```

**Solution**: Idempotency key

```java
@PostMapping("/checkouts")
public ResponseEntity<CheckoutDto> checkout(
    @RequestBody CheckoutRequest request,
    @RequestHeader("Idempotency-Key") String idempotencyKey
) {
    // Check if we've already processed this idempotency key
    Optional<Checkout> existing = checkoutService.findByIdempotencyKey(idempotencyKey);

    if (existing.isPresent()) {
        // Already processed, return cached result
        return ResponseEntity.ok(CheckoutMapper.toDto(existing.get()));
    }

    // First time seeing this key, process normally
    Checkout checkout = checkoutService.checkoutBook(request, idempotencyKey);
    return ResponseEntity.created(URI.create("/checkouts/" + checkout.getId()))
                         .body(CheckoutMapper.toDto(checkout));
}
```

**Client usage**:
```java
// Generate unique key
String idempotencyKey = UUID.randomUUID().toString();

// First attempt
POST /checkouts
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
{"bookId": 42, "userId": 7}
→ (timeout)

// Retry with SAME key
POST /checkouts
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
{"bookId": 42, "userId": 7}
→ 200 OK (returns existing checkout, doesn't create duplicate)
```

**Stripe, AWS, and other major APIs use this pattern.**

## HTTP Status Codes That Matter

Bibby's `BookController` returns `void` or generic strings. Let's fix that with proper status codes.

### 2xx Success

**200 OK**: Request succeeded, here's the result.
```java
return ResponseEntity.ok(bookDto);
```

**201 Created**: Resource was created.
```java
return ResponseEntity.created(location).body(bookDto);
```

**204 No Content**: Succeeded, but no body to return.
```java
return ResponseEntity.noContent().build();
```

**Example from Bibby**:
```java
@DeleteMapping("/books/{id}")
public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
    bookService.deleteBook(id);
    return ResponseEntity.noContent().build();  // 204: Deleted successfully, no body
}
```

### 4xx Client Errors

**400 Bad Request**: Invalid input.
```java
if (request.title() == null) {
    return ResponseEntity.badRequest().build();
}
```

**404 Not Found**: Resource doesn't exist.
```java
return book
    .map(ResponseEntity::ok)
    .orElse(ResponseEntity.notFound().build());
```

**409 Conflict**: Request conflicts with current state.
```java
// Trying to check out a book that's already checked out
if (book.getStatus() == BookStatus.CHECKED_OUT) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("Book already checked out"));
}
```

**422 Unprocessable Entity**: Syntax is valid, but semantics are wrong.
```java
// Valid JSON, but business rule violation
if (shelf.getBookCount() >= shelf.getCapacity()) {
    return ResponseEntity.unprocessableEntity()
        .body(new ErrorResponse("Shelf is full"));
}
```

### 5xx Server Errors

**500 Internal Server Error**: Something broke on the server.
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericError(Exception e) {
    logger.error("Unexpected error", e);
    return ResponseEntity.internalServerError()
        .body(new ErrorResponse("Internal server error"));
}
```

**503 Service Unavailable**: Service is down or overloaded.
```java
// If dependent service (e.g., Shelf Service) is down
try {
    ShelfDto shelf = shelfServiceClient.getShelf(shelfId);
} catch (ServiceUnavailableException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ErrorResponse("Shelf service temporarily unavailable"));
}
```

## Pagination, Filtering, Sorting

Bibby has 1000+ books. Returning all of them in one response is slow and wasteful.

### Pagination Patterns

**Pattern 1: Offset-based**

```java
@GetMapping("/books")
public ResponseEntity<Page<BookDto>> getBooks(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String title
) {
    Pageable pageable = PageRequest.of(page, size);
    Page<BookEntity> books = bookRepository.findAll(pageable);

    Page<BookDto> dtos = books.map(BookMapper::toDto);

    return ResponseEntity.ok(dtos);
}
```

**Request**:
```
GET /books?page=0&size=20  → First 20 books
GET /books?page=1&size=20  → Next 20 books
GET /books?page=2&size=20  → Books 41-60
```

**Response**:
```json
{
  "content": [
    {"bookId": 1, "title": "Clean Code"},
    {"bookId": 2, "title": "Refactoring"},
    ...
  ],
  "totalElements": 1000,
  "totalPages": 50,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

**Pattern 2: Cursor-based** (better for large datasets)

```java
@GetMapping("/books")
public ResponseEntity<CursorPage<BookDto>> getBooks(
    @RequestParam(required = false) Long cursor,
    @RequestParam(defaultValue = "20") int limit
) {
    List<BookEntity> books;

    if (cursor == null) {
        books = bookRepository.findFirstNOrderByIdAsc(limit + 1);
    } else {
        books = bookRepository.findNAfterIdOrderByIdAsc(cursor, limit + 1);
    }

    boolean hasMore = books.size() > limit;
    if (hasMore) {
        books = books.subList(0, limit);
    }

    Long nextCursor = hasMore ? books.get(books.size() - 1).getBookId() : null;

    List<BookDto> dtos = books.stream().map(BookMapper::toDto).collect(Collectors.toList());

    return ResponseEntity.ok(new CursorPage<>(dtos, nextCursor, hasMore));
}
```

**Request**:
```
GET /books?limit=20              → First 20 books
GET /books?cursor=20&limit=20    → Next 20 after ID 20
GET /books?cursor=40&limit=20    → Next 20 after ID 40
```

**Why cursor is better**: Offset pagination breaks when data changes (new book inserted on page 1 shifts everything). Cursor pagination is stable.

### Filtering

```java
GET /books?status=AVAILABLE           → Only available books
GET /books?shelf=7                    → Books on shelf 7
GET /books?author=Penrose             → Books by Penrose
GET /books?status=AVAILABLE&shelf=7   → Available books on shelf 7
```

**Implementation**:
```java
@GetMapping("/books")
public ResponseEntity<List<BookDto>> getBooks(
    @RequestParam(required = false) BookStatus status,
    @RequestParam(required = false) Long shelfId,
    @RequestParam(required = false) String author
) {
    List<BookEntity> books = bookService.findBooks(status, shelfId, author);
    return ResponseEntity.ok(books.stream().map(BookMapper::toDto).collect(Collectors.toList()));
}
```

### Sorting

```
GET /books?sort=title,asc          → Sort by title ascending
GET /books?sort=publicationYear,desc  → Sort by year descending
GET /books?sort=title,asc&sort=publicationYear,desc  → Multi-field sort
```

**Spring Data JPA makes this easy**:
```java
@GetMapping("/books")
public ResponseEntity<List<BookDto>> getBooks(
    @RequestParam(required = false) String sort
) {
    Sort sortObj = parseSortParam(sort);  // Helper to parse "title,asc"
    List<BookEntity> books = bookRepository.findAll(sortObj);
    return ResponseEntity.ok(books.stream().map(BookMapper::toDto).collect(Collectors.toList()));
}
```

## Action Items

Before moving to Chapter 5, refactor Bibby's controllers:

### 1. Fix BookController

Refactor the current `BookController.java` to use proper HTTP semantics:

```java
// Current (WRONG)
@GetMapping("api/v1/books")
public void findBookByTitle(@RequestBody BookRequestDTO requestDTO)

// Your refactored version:
@GetMapping("/books")
public ResponseEntity<List<BookDto>> getBooks(@RequestParam String title)
```

Create endpoints for:
- GET /books (with filtering by title, author, status, shelf)
- GET /books/{id}
- POST /books
- PUT /books/{id}
- PATCH /books/{id}
- DELETE /books/{id}

### 2. Design Shelf API

`ShelfController` is empty. Design the full REST API:

```java
@RestController
@RequestMapping("/shelves")
public class ShelfController {
    // Your implementation
}
```

Endpoints:
- GET /shelves (with filtering by bookcase)
- GET /shelves/{id}
- GET /shelves/{id}/books
- POST /shelves
- PUT /shelves/{id}
- DELETE /shelves/{id}

### 3. Fix BookcaseController

```java
// Current
@PostMapping("/create/bookcase")  // ❌ Verb in URL

// Your fixed version
@PostMapping("/bookcases")  // ✅ Resource-oriented
```

Add full CRUD operations for bookcases.

### 4. Add Pagination

Implement pagination for `/books` endpoint:
- Support `?page=X&size=Y`
- Return total count, total pages, current page
- Consider cursor-based pagination for bonus points

### 5. Implement Idempotency

Design a checkout endpoint with idempotency key support:

```java
@PostMapping("/checkouts")
public ResponseEntity<CheckoutDto> checkout(
    @RequestBody CheckoutRequest request,
    @RequestHeader("Idempotency-Key") String key
) {
    // Your implementation
}
```

## Key Takeaways

1. **Resources are nouns, HTTP verbs are actions** — `/books` (noun) + POST (verb) = create book

2. **GET shouldn't have a request body** — Use query parameters instead

3. **Use the right status code** — 200 OK, 201 Created, 404 Not Found, 409 Conflict

4. **Idempotency enables safe retries** — PUT/DELETE are idempotent, POST is not

5. **Safe methods can be cached and retried** — GET, HEAD, OPTIONS don't change state

6. **POST is not idempotent** — Use idempotency keys for duplicate prevention

7. **Pagination is mandatory for large collections** — Don't return all 1000 books

8. **URIs identify resources, not actions** — `/bookcases` not `/create/bookcase`

9. **Return the created resource** — POST should return 201 + Location header + body

10. **Filtering > Multiple endpoints** — `GET /books?status=AVAILABLE` not `GET /available-books`

## Further Reading

### Essential Resources
- **Roy Fielding's Dissertation (2000)** — The original REST definition
- **"REST API Design Rulebook"** by Mark Masse — Practical patterns
- **"RESTful Web Services"** by Richardson & Ruby — Comprehensive guide

### Specifications
- **RFC 7231**: HTTP Semantics (methods, status codes)
- **RFC 6902**: JSON Patch
- **RFC 7807**: Problem Details for HTTP APIs

### Best Practices
- **Microsoft REST API Guidelines**
- **Google Cloud API Design Guide**
- **Stripe API Documentation** — Example of excellent API design

---

## What's Next?

In **Chapter 5: Designing Great APIs**, we'll go deeper into API design:
- Versioning strategies (URL vs header vs content negotiation)
- Error schemas and problem details (RFC 7807)
- Backward compatibility rules
- Common API anti-patterns
- Request/response modeling for Bibby's complex operations

**Remember**: REST fundamentals are about HTTP semantics. Next, we'll learn how to design APIs that evolve gracefully and delight developers.

You've learned the mechanics of REST. Now let's learn the art of great API design.
