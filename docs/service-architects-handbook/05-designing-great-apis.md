# Chapter 5: Designing Great APIs

## Introduction: The API That Wouldn't Die

Let me tell you about a company that designed an API in 2012. They made what seemed like innocent decisions:

```java
// v1 of their API (2012)
GET /books
{
  "books": [
    {"id": 1, "name": "Clean Code", "writer": "Robert Martin"}
  ]
}
```

Fast forward to 2020, and they're stuck supporting this API forever because:
- 500+ client applications depend on it
- Field is called `"writer"` instead of `"author"` (inconsistent with other endpoints)
- Field is called `"name"` instead of `"title"` (every other resource uses `"title"`)
- They can't fix it without breaking everyone

**The cost**: Every new engineer asks "why is it called `writer`?" and gets told "historical reasons." Every integration guide needs a note explaining the inconsistency. Every new feature has to maintain backward compatibility with a bad decision from 2012.

**The lesson**: API design is permanent. Once you ship it, clients depend on it. You can add to it, but you can rarely change it.

In this chapter, I'll teach you how to design APIs that evolve gracefully using Bibby as our case study. We'll cover versioning strategies, error schemas, backward compatibility, and the patterns that prevent you from painting yourself into a corner.

## API Design as Interface Design

An API is a contract between your service and its clients. Breaking that contract breaks prod for someone else.

### Bibby's Current API Contracts

Let's audit what Bibby has already committed to:

**BookRequestDTO** (used in `POST /api/v1/books`):
```java
public record BookRequestDTO(String title, String firstName, String lastName) {}
```

**Analysis**:
- ✅ Uses `title` (good, consistent with domain)
- ❌ Splits author into `firstName`/`lastName` (what about books with multiple authors?)
- ❌ No validation annotations
- ❌ Tightly couples input to single-author books

**BookcaseDTO**:
```java
public record BookcaseDTO(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {}
```

**Analysis**:
- ✅ Clean, simple structure
- ✅ Includes ID (good for responses)
- ❌ Uses ID in request DTO (should be separate Request/Response DTOs)
- ❌ Primitive `int` for capacity (what about `null` / unset?)

**BookDetailView**:
```java
public record BookDetailView(
    Long bookId,
    String title,
    String authors,  // ← CSV string!
    String bookcaseLabel,
    String shelfLabel,
    String bookStatus
) {}
```

**Analysis**:
- ✅ Denormalized for read performance (good for queries)
- ❌ `authors` is a CSV string (clients have to parse "David Thomas, Andrew Hunt")
- ❌ `bookStatus` is a string (should be enum/type in API response)
- ❌ Mixes concerns (book data + location data)

### The Request/Response Separation Principle

**Rule**: **Never use the same DTO for requests and responses.**

**Why?** Requests and responses have different requirements:

| **Request DTOs** | **Response DTOs** |
|------------------|-------------------|
| No ID (server generates it) | Has ID |
| Required fields only | All fields (including computed) |
| Validation annotations | No validation needed |
| Minimal (only what client provides) | Rich (includes server-added data) |

**Example from Bibby**: Creating a book

**WRONG** (using same DTO):
```java
public record BookDTO(
    Long bookId,           // ID in request makes no sense
    String title,
    List<String> authors,
    LocalDate createdAt    // Client can't set this!
) {}

@PostMapping("/books")
public ResponseEntity<BookDTO> createBook(@RequestBody BookDTO request) {
    // Client could send: {"bookId": 999, "createdAt": "2099-01-01"}
    // We ignore these, but it's confusing API design
}
```

**RIGHT** (separate DTOs):
```java
// Request: Only what client provides
public record CreateBookRequest(
    @NotBlank String title,
    @NotBlank String isbn,
    @NotEmpty List<CreateAuthorRequest> authors
) {}

public record CreateAuthorRequest(
    @NotBlank String firstName,
    @NotBlank String lastName
) {}

// Response: Full representation
public record BookResponse(
    Long bookId,              // Server-generated
    String title,
    String isbn,
    List<AuthorResponse> authors,  // Structured, not CSV
    BookStatus status,        // Enum, not string
    LocalDate createdAt,      // Server-generated
    LocalDate updatedAt,      // Server-generated
    Links _links              // HATEOAS links
) {}

public record AuthorResponse(
    Long authorId,
    String fullName
) {}
```

**Benefits**:
- Clear what client can/cannot set
- Request can evolve independently of response
- Validation lives in request DTO only
- Response can include computed fields

## API Versioning Strategies

Bibby currently has `api/v1/books`. Good start! But how do you evolve from v1 to v2?

### Strategy 1: URI Versioning (What Bibby Uses)

**Pattern**: Version in the URL path

```java
@RestController
@RequestMapping("/api/v1/books")  // ← Version in path
public class BookControllerV1 {
    @GetMapping
    public List<BookResponse> getBooks() { ... }
}

@RestController
@RequestMapping("/api/v2/books")  // ← New version
public class BookControllerV2 {
    @GetMapping
    public List<BookResponseV2> getBooks() { ... }  // Different response shape
}
```

**Pros**:
- ✅ Explicit and visible
- ✅ Easy to route in load balancers
- ✅ Can deploy different versions independently
- ✅ Simple to understand

**Cons**:
- ❌ URL changes (breaks REST purism)
- ❌ Need to duplicate controllers
- ❌ Can't version individual endpoints

**When to use**: Most of the time. This is the most common pattern.

**Bibby example**: Evolving the books API

```java
// v1: Returns CSV authors
GET /api/v1/books/42
{
  "bookId": 42,
  "title": "Clean Code",
  "authors": "Robert Martin"  // ← CSV string
}

// v2: Returns structured authors
GET /api/v2/books/42
{
  "bookId": 42,
  "title": "Clean Code",
  "authors": [  // ← Array of objects
    {
      "authorId": 7,
      "firstName": "Robert",
      "lastName": "Martin"
    }
  ]
}
```

### Strategy 2: Header Versioning

**Pattern**: Version in Accept header

```java
@RestController
@RequestMapping("/api/books")
public class BookController {
    @GetMapping
    public ResponseEntity<?> getBooks(@RequestHeader("Accept") String acceptHeader) {
        if (acceptHeader.contains("application/vnd.bibby.v2+json")) {
            return ResponseEntity.ok(getBooksV2());
        } else {
            return ResponseEntity.ok(getBooksV1());
        }
    }
}
```

**Request**:
```http
GET /api/books
Accept: application/vnd.bibby.v2+json
```

**Pros**:
- ✅ URL stays the same (REST purity)
- ✅ Can version individual resources
- ✅ Follows HTTP spec (content negotiation)

**Cons**:
- ❌ Hidden from URL (harder to debug)
- ❌ Can't bookmark specific versions
- ❌ More complex routing logic

**When to use**: When URL stability is critical (public APIs, hypermedia-driven apps).

### Strategy 3: Query Parameter Versioning

**Pattern**: Version as query param

```java
GET /api/books?version=2
```

**Pros**:
- ✅ Visible and explicit
- ✅ Easy to test in browser

**Cons**:
- ❌ Easy to forget (what's the default?)
- ❌ Pollutes query parameters
- ❌ Mixing versioning with filtering/pagination

**When to use**: Almost never. Use URI versioning instead.

### The Bibby Versioning Decision

**Recommendation for Bibby**: Stick with URI versioning (`/api/v1/`, `/api/v2/`)

**Migration strategy**:

```java
// v1 Controller (keep for backward compatibility)
@RestController
@RequestMapping("/api/v1/books")
public class BookControllerV1 {
    @GetMapping("/{id}")
    public BookResponseV1 getBook(@PathVariable Long id) {
        BookEntity book = bookService.findBookById(id).orElseThrow();
        return mapToV1Response(book);  // CSV authors
    }
}

// v2 Controller (new and improved)
@RestController
@RequestMapping("/api/v2/books")
public class BookControllerV2 {
    @GetMapping("/{id}")
    public BookResponseV2 getBook(@PathVariable Long id) {
        BookEntity book = bookService.findBookById(id).orElseThrow();
        return mapToV2Response(book);  // Structured authors
    }
}
```

Both controllers use the same service layer, just different response DTOs.

## Error Schemas and Problem Details (RFC 7807)

Bibby's current error handling:

```java
// BookController.java
return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
```

**Problems**:
- Returns `200 OK` even when it should return `201 Created`
- Success message is a plain string (no structure)
- No error handling (what if book already exists?)
- Clients have to parse strings to understand errors

### RFC 7807: Problem Details for HTTP APIs

**Standard format** for error responses:

```json
{
  "type": "https://bibby.com/problems/book-already-exists",
  "title": "Book Already Exists",
  "status": 409,
  "detail": "A book with ISBN 978-0132350884 already exists in the library",
  "instance": "/api/v1/books",
  "bookId": 42,
  "isbn": "978-0132350884"
}
```

**Fields**:
- `type`: URI identifying the problem type (can link to docs)
- `title`: Human-readable summary
- `status`: HTTP status code (repeated for convenience)
- `detail`: Human-readable explanation
- `instance`: URI where the problem occurred
- **Extension fields**: Domain-specific data (`bookId`, `isbn`)

### Implementing RFC 7807 in Bibby

**1. Create ProblemDetail base class**:

```java
public record ProblemDetail(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    Map<String, Object> extensions
) {
    public static ProblemDetailBuilder builder() {
        return new ProblemDetailBuilder();
    }
}
```

**2. Define problem types**:

```java
public class BookProblems {
    public static final String BASE_URI = "https://api.bibby.com/problems";

    public static ProblemDetail bookNotFound(Long bookId, String requestUri) {
        return ProblemDetail.builder()
            .type(BASE_URI + "/book-not-found")
            .title("Book Not Found")
            .status(404)
            .detail("Book with ID " + bookId + " does not exist")
            .instance(requestUri)
            .extension("bookId", bookId)
            .build();
    }

    public static ProblemDetail bookAlreadyExists(String isbn, Long existingBookId, String requestUri) {
        return ProblemDetail.builder()
            .type(BASE_URI + "/book-already-exists")
            .title("Book Already Exists")
            .status(409)
            .detail("A book with ISBN " + isbn + " already exists")
            .instance(requestUri)
            .extension("isbn", isbn)
            .extension("existingBookId", existingBookId)
            .build();
    }

    public static ProblemDetail invalidBookData(List<String> validationErrors, String requestUri) {
        return ProblemDetail.builder()
            .type(BASE_URI + "/invalid-book-data")
            .title("Invalid Book Data")
            .status(400)
            .detail("The book data contains validation errors")
            .instance(requestUri)
            .extension("errors", validationErrors)
            .build();
    }

    public static ProblemDetail shelfFull(Long shelfId, int capacity, String requestUri) {
        return ProblemDetail.builder()
            .type(BASE_URI + "/shelf-full")
            .title("Shelf Is Full")
            .status(422)
            .detail("Shelf " + shelfId + " has reached capacity of " + capacity + " books")
            .instance(requestUri)
            .extension("shelfId", shelfId)
            .extension("capacity", capacity)
            .build();
    }
}
```

**3. Use in controllers**:

```java
@PostMapping("/api/v2/books")
public ResponseEntity<?> createBook(@RequestBody CreateBookRequest request, HttpServletRequest httpRequest) {
    // Validation
    List<String> errors = validateBookRequest(request);
    if (!errors.isEmpty()) {
        ProblemDetail problem = BookProblems.invalidBookData(errors, httpRequest.getRequestURI());
        return ResponseEntity.status(problem.status()).body(problem);
    }

    // Check for duplicates
    if (bookService.existsByIsbn(request.isbn())) {
        BookEntity existing = bookService.findByIsbn(request.isbn());
        ProblemDetail problem = BookProblems.bookAlreadyExists(
            request.isbn(),
            existing.getBookId(),
            httpRequest.getRequestURI()
        );
        return ResponseEntity.status(problem.status()).body(problem);
    }

    // Create book
    BookEntity created = bookService.createBook(request);
    BookResponse response = BookMapper.toResponse(created);

    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.getBookId())
        .toUri();

    return ResponseEntity.created(location).body(response);
}
```

**Client experience**:

```http
POST /api/v2/books
{
  "title": "Clean Code",
  "isbn": "978-0132350884"
}

HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "https://api.bibby.com/problems/book-already-exists",
  "title": "Book Already Exists",
  "status": 409,
  "detail": "A book with ISBN 978-0132350884 already exists",
  "instance": "/api/v2/books",
  "isbn": "978-0132350884",
  "existingBookId": 42
}
```

Now the client can:
- Check `status` for HTTP status code
- Check `type` to programmatically handle error
- Show `detail` to end user
- Link to `existingBookId` ("Did you mean this book?")

## Backward Compatibility Rules

Once you ship an API, clients depend on it. Here are the rules for evolving without breaking them.

### Safe Changes (Non-Breaking)

**✅ ADD optional fields to responses**:

```java
// v1
{
  "bookId": 42,
  "title": "Clean Code"
}

// v1.1 (added fields, still v1)
{
  "bookId": 42,
  "title": "Clean Code",
  "description": "...",  // ← New optional field
  "tags": ["programming", "java"]  // ← New optional field
}
```

Clients ignoring unknown fields will still work.

**✅ ADD optional fields to requests**:

```java
// v1
POST /api/v1/books
{
  "title": "Clean Code"
}

// v1.1 (accepts new optional field)
POST /api/v1/books
{
  "title": "Clean Code",
  "description": "..."  // ← New optional field
}
```

Old clients not sending `description` still work.

**✅ ADD new endpoints**:

```java
// v1 has: GET /books, POST /books
// v1.1 adds: GET /books/search, GET /books/recommended
```

### Breaking Changes (Require New Version)

**❌ REMOVE fields from responses**:

```java
// v1
{
  "bookId": 42,
  "title": "Clean Code",
  "writer": "Robert Martin"
}

// v2 (removed "writer")
{
  "bookId": 42,
  "title": "Clean Code"
  // Clients expecting "writer" will break!
}
```

**❌ RENAME fields**:

```java
// v1
{"writer": "Robert Martin"}

// v2
{"author": "Robert Martin"}  // Clients looking for "writer" break!
```

**❌ CHANGE field types**:

```java
// v1
{"authors": "Robert Martin"}  // String

// v2
{"authors": ["Robert Martin"]}  // Array (breaks parsing)
```

**❌ MAKE optional fields required**:

```java
// v1: description is optional
POST /books {"title": "Clean Code"}  // Works

// v2: description is required
POST /books {"title": "Clean Code"}  // 400 Bad Request (breaks clients!)
```

**❌ CHANGE URL structure**:

```java
// v1
GET /books/42

// v2
GET /books/42/details  // Different URL (breaks bookmarks, cached links)
```

### The Expand-Contract Pattern

When you need to rename a field, use this pattern:

**Phase 1: Expand** (support both old and new)

```java
// v1.5: Add new field, keep old field
{
  "writer": "Robert Martin",  // ← Deprecated but still present
  "author": "Robert Martin"   // ← New field
}
```

**Phase 2: Migrate** (give clients time to update)

- Announce deprecation in docs
- Email API consumers
- Add `Deprecation` header to responses
- Wait 6-12 months

**Phase 3: Contract** (remove old field in new version)

```java
// v2: Remove deprecated field
{
  "author": "Robert Martin"  // Only new field remains
}
```

## Request/Response Modeling for Complex Operations

Let's design Bibby's checkout operation properly.

### The Checkout Flow

**Business logic**:
1. User wants to check out a book
2. Verify book is available (not already checked out)
3. Verify book exists and has a shelf location
4. Update book status to CHECKED_OUT
5. Increment checkout count
6. Record checkout transaction (who, when, due date)

**Naive approach** (what not to do):

```java
@PostMapping("/books/{id}/checkout")
public ResponseEntity<String> checkout(@PathVariable Long id) {
    bookService.checkout(id);
    return ResponseEntity.ok("Book checked out");  // ❌ No structure, no info
}
```

**Problems**:
- Client doesn't know when it's due back
- No checkout ID (how to check it in?)
- Can't extend due date (no resource representing the checkout)

**Proper approach**: Checkout as a first-class resource

**Request**:
```java
public record CheckoutRequest(
    @NotNull Long bookId,
    @NotNull Long userId,
    @Future LocalDate dueDate  // Optional, server can default to +14 days
) {}
```

**Response**:
```java
public record CheckoutResponse(
    Long checkoutId,
    BookSummary book,
    UserSummary user,
    LocalDate checkedOutAt,
    LocalDate dueDate,
    CheckoutStatus status,  // ACTIVE, OVERDUE, RETURNED
    Links _links
) {
    public record Links(
        String self,      // /checkouts/123
        String returnBook, // /checkouts/123/return
        String extend,    // /checkouts/123/extend
        String book       // /books/42
    ) {}
}
```

**API**:

```java
@PostMapping("/api/v2/checkouts")
public ResponseEntity<CheckoutResponse> checkout(
    @RequestBody @Valid CheckoutRequest request,
    HttpServletRequest httpRequest
) {
    // Validate book exists and is available
    BookEntity book = bookService.findBookById(request.bookId())
        .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

    if (book.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
        ProblemDetail problem = BookProblems.bookUnavailable(
            request.bookId(),
            "Already checked out",
            httpRequest.getRequestURI()
        );
        return ResponseEntity.status(problem.status()).body(problem);
    }

    // Create checkout
    Checkout checkout = checkoutService.checkoutBook(request);
    CheckoutResponse response = CheckoutMapper.toResponse(checkout);

    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(checkout.getCheckoutId())
        .toUri();

    return ResponseEntity.created(location).body(response);
}
```

**Response**:

```json
HTTP/1.1 201 Created
Location: /api/v2/checkouts/789
Content-Type: application/json

{
  "checkoutId": 789,
  "book": {
    "bookId": 42,
    "title": "Clean Code"
  },
  "user": {
    "userId": 7,
    "name": "Leo Penrose"
  },
  "checkedOutAt": "2025-11-18T10:00:00Z",
  "dueDate": "2025-12-02",
  "status": "ACTIVE",
  "_links": {
    "self": "/api/v2/checkouts/789",
    "returnBook": "/api/v2/checkouts/789/return",
    "extend": "/api/v2/checkouts/789/extend",
    "book": "/api/v2/books/42"
  }
}
```

Now clients can:
- Know when it's due (`dueDate`)
- Return the book (`POST /checkouts/789/return`)
- Extend the checkout (`POST /checkouts/789/extend`)
- Navigate to the book (`GET /books/42`)

## Common API Anti-Patterns

### Anti-Pattern 1: God Endpoints

**WRONG**:
```java
@PostMapping("/books/action")
public ResponseEntity<?> doAction(@RequestBody ActionRequest request) {
    switch (request.action()) {
        case "checkout": return checkout(request);
        case "checkin": return checkin(request);
        case "reserve": return reserve(request);
        case "rate": return rate(request);
        default: return error();
    }
}
```

**Why it's bad**:
- Can't use HTTP verbs properly
- Can't route to different services
- Hard to version individual actions
- Hard to document

**RIGHT**: Separate, resource-oriented endpoints

```java
POST /checkouts        → Checkout a book
DELETE /checkouts/789  → Return a book
POST /reservations     → Reserve a book
POST /ratings          → Rate a book
```

### Anti-Pattern 2: Leaking Implementation Details

**WRONG**: Bibby's current `BookDetailView`

```java
public record BookDetailView(
    Long bookId,
    String title,
    String authors,  // ← "David Thomas, Andrew Hunt" (CSV string!)
    String bookcaseLabel,
    String shelfLabel,
    String bookStatus
) {}
```

**Why it's bad**:
- Exposes database query format (CSV from `STRING_AGG`)
- Client must parse CSV (what if author name has a comma?)
- Can't evolve to multiple authors easily

**RIGHT**: Structure that hides implementation

```java
public record BookDetailResponse(
    Long bookId,
    String title,
    List<AuthorSummary> authors,  // ← Proper structure
    ShelfLocation location,
    BookStatus status
) {}

public record AuthorSummary(Long authorId, String fullName) {}
public record ShelfLocation(String bookcase, String shelf) {}
```

### Anti-Pattern 3: Chatty APIs (N+1 Problem)

**WRONG**: Require many round trips

```
Client wants: Book + Authors + Shelf + Bookcase

GET /books/42           → {title, authorIds: [1, 2]}
GET /authors/1          → {name: "David Thomas"}
GET /authors/2          → {name: "Andrew Hunt"}
GET /shelves/7          → {label: "A1", bookcaseId: 3}
GET /bookcases/3        → {label: "Main"}

Total: 5 network round trips!
```

**RIGHT**: Aggregate endpoint or field selection

```
GET /books/42?include=authors,location

{
  "bookId": 42,
  "title": "The Pragmatic Programmer",
  "authors": [
    {"authorId": 1, "fullName": "David Thomas"},
    {"authorId": 2, "fullName": "Andrew Hunt"}
  ],
  "location": {
    "shelf": "A1",
    "bookcase": "Main"
  }
}

Total: 1 network round trip
```

Bibby's `BookDetailView` query already does this! It's a read model that joins everything.

## Action Items

Before moving to Chapter 6, refactor Bibby's API design:

### 1. Create Proper Request/Response DTOs

Split `BookcaseDTO` into:

```java
// Request: Creating a bookcase
public record CreateBookcaseRequest(
    @NotBlank String label,
    @Min(1) int shelfCapacity
) {}

// Response: Full bookcase representation
public record BookcaseResponse(
    Long bookcaseId,
    String label,
    int shelfCapacity,
    int currentShelfCount,  // Computed field
    LocalDate createdAt,
    Links _links
) {}
```

### 2. Implement RFC 7807 Error Handling

Create `ProblemDetail` class and `BookProblems` helpers. Refactor all controllers to return structured errors instead of plain strings.

### 3. Design Checkout API

Implement the full checkout resource:
- POST /checkouts (create checkout)
- GET /checkouts/{id} (get checkout details)
- POST /checkouts/{id}/return (check in book)
- POST /checkouts/{id}/extend (extend due date)

### 4. Plan v2 API

Identify what you'd change in Bibby's API:
- Rename `api/v1` fields?
- Change response structures?
- Add new fields?

Design the v2 endpoints and migration strategy.

### 5. Fix BookDetailView

Refactor from CSV string to structured data:

```java
// Current (CSV)
"authors": "David Thomas, Andrew Hunt"

// Fixed (structured)
"authors": [
  {"authorId": 1, "fullName": "David Thomas"},
  {"authorId": 2, "fullName": "Andrew Hunt"}
]
```

## Key Takeaways

1. **API design is permanent** — Once shipped, you must maintain backward compatibility

2. **Separate request and response DTOs** — They have different requirements

3. **URI versioning is the simplest** — `/api/v1/` → `/api/v2/` works for most cases

4. **Use RFC 7807 for errors** — Structured, consistent error responses

5. **Adding is safe, changing is breaking** — You can add optional fields, but can't rename/remove

6. **Expand-contract for migrations** — Support both old and new during transition

7. **Model complex operations as resources** — Checkout is a resource, not just an action

8. **Avoid god endpoints** — Use resource-oriented design instead

9. **Don't leak implementation details** — CSV strings, database columns, etc.

10. **Minimize round trips** — Aggregate related data in responses

## Further Reading

### Specifications
- **RFC 7807**: Problem Details for HTTP APIs
- **OpenAPI Specification 3.0** — API documentation standard
- **JSON:API** — Opinionated JSON API specification

### Books
- **"Web API Design"** by Brian Mulloy — Short, practical guide
- **"REST API Design Rulebook"** by Mark Masse
- **"Designing Web APIs"** by Brenda Jin, Saurabh Sahni

### Real-World Examples
- **Stripe API Documentation** — Gold standard for API design
- **GitHub REST API** — Well-designed, well-documented
- **Twilio API** — Excellent error handling and docs

---

## What's Next?

In **Chapter 6: Beyond REST (GraphQL, gRPC, Webhooks)**, we'll explore alternatives to REST:
- When GraphQL solves real problems (and when it doesn't)
- gRPC and Protocol Buffers for service-to-service communication
- Server-Sent Events and WebSockets for real-time updates
- Webhooks for event-driven callbacks
- Choosing the right protocol for your use case

**Remember**: REST is great, but it's not always the right tool. Next, you'll learn when to reach for alternatives.

You've learned how to design evolvable REST APIs. Now let's explore what comes next.
