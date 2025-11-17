# Section 17: Web API Design & Best Practices
## Clean Code + Spring Framework Mentorship

**Focus:** RESTful API design, HTTP semantics, validation, error handling, and API contracts

**Estimated Time:** 3-4 hours to read and understand; 12-16 hours to implement recommendations

---

## Overview

Welcome to the Web API section! While Bibby is primarily a CLI application, you've begun adding REST endpoints. This is **excellent** for your learning and career growth—most Spring Boot jobs involve building APIs.

However, your current API implementation has fundamental issues that would fail code review in any professional setting. This section will teach you industry-standard REST API design.

---

## Your Current API Implementation

### What You Have

```java
// BookController.java
@RestController
public class BookController {

    @PostMapping("api/v1/books")
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.createNewBook(requestDTO);
        return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
    }

    @GetMapping("api/v1/books")
    public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
        System.out.println("Controller Search For " + requestDTO.title());
        bookService.findBookByTitle(requestDTO.title());
    }
}
```

```java
// BookCaseController.java
@RestController
public class BookCaseController {

    @PostMapping("/create/bookcase")
    public ResponseEntity<String> createBookCase(@RequestBody BookcaseDTO bookcaseDTO){
        String message = bookCaseService.createNewBookCase(
            bookcaseDTO.bookcaseLabel(),
            bookcaseDTO.shelfCapacity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}
```

```java
// BookRequestDTO.java
public record BookRequestDTO(String title, String firstName, String lastName) {}

// BookcaseDTO.java
public record BookcaseDTO(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {}
```

### Empty Controllers
```java
// AuthorController.java
public class AuthorController {
}

// CatalogController.java
public class CatalogController {
}

// ShelfController.java
@RestController
public class ShelfController {
    ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }
}
```

---

## Critical Issues Found

### 🚨 SEVERITY: CRITICAL (Fix Immediately)

#### 1. **GET Request with Request Body** ⚠️ HTTP VIOLATION
**Location:** BookController.java:24-28

```java
@GetMapping("api/v1/books")
public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
    System.out.println("Controller Search For " + requestDTO.title());
    bookService.findBookByTitle(requestDTO.title());
}
```

**Problems:**
1. **HTTP Specification Violation:** GET requests MUST NOT have request bodies per RFC 7231
2. **Unpredictable Behavior:** Many HTTP clients/proxies strip bodies from GET requests
3. **Caching Issues:** GET with body breaks HTTP caching mechanisms
4. **Returns void:** No response means client can't see results
5. **System.out.println:** Debugging code left in production controller

**Why This Matters:**
- This would be **rejected immediately** in code review
- Violates HTTP standards that have existed for 25+ years
- Some clients (curl, Postman, browsers) will silently drop the body
- **This endpoint literally doesn't work** as intended

**Correct Approaches:**

**Option A: Query Parameter (for simple searches)**
```java
@GetMapping("api/v1/books")
public ResponseEntity<List<BookResponseDTO>> findBooksByTitle(
    @RequestParam String title
) {
    List<Book> books = bookService.findBooksByTitle(title);
    return ResponseEntity.ok(toResponseDTOs(books));
}
// Usage: GET /api/v1/books?title=Clean%20Code
```

**Option B: Path Variable (for single resource)**
```java
@GetMapping("api/v1/books/{id}")
public ResponseEntity<BookResponseDTO> getBookById(
    @PathVariable Long id
) {
    Book book = bookService.findById(id);
    return ResponseEntity.ok(toResponseDTO(book));
}
// Usage: GET /api/v1/books/123
```

**Option C: POST to search endpoint (for complex searches)**
```java
@PostMapping("api/v1/books/search")
public ResponseEntity<List<BookResponseDTO>> searchBooks(
    @Valid @RequestBody BookSearchCriteria criteria
) {
    List<Book> books = bookService.search(criteria);
    return ResponseEntity.ok(toResponseDTOs(books));
}
// Usage: POST /api/v1/books/search with JSON body
```

---

#### 2. **No Input Validation** ⚠️ SECURITY RISK
**Location:** All controllers

**Current State:**
```java
public record BookRequestDTO(String title, String firstName, String lastName) {}
```

**Problems:**
- What if `title` is null? Empty string? 1000 characters?
- What if `firstName` is `"<script>alert('xss')</script>"`?
- What if `shelfCapacity` is -1? 0? 2,147,483,647?
- Your service will crash or create invalid data

**Professional Solution:**
```java
public record BookRequestDTO(
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    String title,

    @NotBlank(message = "Author first name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    String firstName,

    @NotBlank(message = "Author last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    String lastName
) {}
```

**In Controller:**
```java
@PostMapping("api/v1/books")
public ResponseEntity<BookResponseDTO> addBook(
    @Valid @RequestBody BookRequestDTO requestDTO
) {
    Book book = bookService.createNewBook(requestDTO);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(toResponseDTO(book));
}
```

**Add to pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

#### 3. **No Error Handling** ⚠️ USER EXPERIENCE DISASTER
**Location:** All controllers

**Current Behavior:**
When something goes wrong:
```
500 Internal Server Error
{
  "timestamp": "2025-11-17T10:23:45.123+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/v1/books"
}
```

**What the client actually needs:**
- What went wrong?
- Which field was invalid?
- How to fix it?

**Professional Solution: Global Exception Handler**

Create `GlobalExceptionHandler.java`:

```java
package com.penrose.bibby.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            fieldErrors,
            Instant.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex
    ) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            null,
            Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex
    ) {
        // Log the full exception for debugging
        // logger.error("Unexpected error", ex);

        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            null,
            Instant.now()
        );

        return ResponseEntity.internalServerError().body(response);
    }
}

record ErrorResponse(
    int status,
    String message,
    Map<String, String> fieldErrors,
    Instant timestamp
) {}
```

**Create Custom Exception:**
```java
package com.penrose.bibby.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

**Use in Service:**
```java
public Book findById(Long id) {
    return bookRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Book not found with id: " + id
        ));
}
```

---

### 🔶 SEVERITY: HIGH (Fix This Week)

#### 4. **Inconsistent URL Patterns**

**Current State:**
```java
@PostMapping("api/v1/books")           // ✅ Good
@PostMapping("/create/bookcase")       // ❌ Inconsistent
```

**Problems:**
- `/create/bookcase` is not versioned (what about breaking changes?)
- `/create/` is redundant (POST already means "create")
- No leading slash on books endpoint (works but inconsistent)

**RESTful Standard:**

| Operation | Method | URL | Status Code |
|-----------|--------|-----|-------------|
| List all books | GET | `/api/v1/books` | 200 OK |
| Get single book | GET | `/api/v1/books/{id}` | 200 OK |
| Create book | POST | `/api/v1/books` | 201 CREATED |
| Update book | PUT | `/api/v1/books/{id}` | 200 OK |
| Partial update | PATCH | `/api/v1/books/{id}` | 200 OK |
| Delete book | DELETE | `/api/v1/books/{id}` | 204 NO CONTENT |

**Correct BookCaseController:**
```java
@RestController
@RequestMapping("/api/v1/bookcases")  // Base path
public class BookCaseController {

    private final BookcaseService bookcaseService;

    public BookCaseController(BookcaseService bookcaseService) {
        this.bookcaseService = bookcaseService;
    }

    @PostMapping
    public ResponseEntity<BookcaseResponseDTO> createBookcase(
        @Valid @RequestBody BookcaseRequestDTO request
    ) {
        Bookcase bookcase = bookcaseService.createBookcase(
            request.label(),
            request.shelfCapacity()
        );

        BookcaseResponseDTO response = toResponseDTO(bookcase);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookcaseResponseDTO> getBookcase(
        @PathVariable Long id
    ) {
        Bookcase bookcase = bookcaseService.findById(id);
        return ResponseEntity.ok(toResponseDTO(bookcase));
    }

    // Add more endpoints...
}
```

---

#### 5. **Return Types Expose Internal Structure**

**Current Issue:**
```java
return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
```

**Problems:**
- Returns a string message (not structured data)
- Client can't access the created book
- No way to get the book's ID
- Can't build client-side navigation

**Professional Pattern: Response DTOs**

**Separate Request and Response DTOs:**
```java
// Request DTO (what client sends)
public record BookRequestDTO(
    @NotBlank String title,
    @NotBlank String firstName,
    @NotBlank String lastName
) {}

// Response DTO (what server returns)
public record BookResponseDTO(
    Long id,
    String title,
    AuthorDTO author,
    Instant createdAt,
    Instant updatedAt
) {}

public record AuthorDTO(
    Long id,
    String firstName,
    String lastName,
    String fullName
) {}
```

**Why Separate?**
1. **Request DTO** = What user can send (no ID, no timestamps)
2. **Response DTO** = What server returns (includes ID, timestamps, computed fields)
3. **Never expose entity directly** = Changes to DB don't break API

**Updated Controller:**
```java
@PostMapping
public ResponseEntity<BookResponseDTO> addBook(
    @Valid @RequestBody BookRequestDTO request
) {
    Book book = bookService.createNewBook(request);
    BookResponseDTO response = BookMapper.toResponseDTO(book);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .header("Location", "/api/v1/books/" + book.getId())
        .body(response);
}
```

**Client receives:**
```json
{
  "id": 123,
  "title": "Clean Code",
  "author": {
    "id": 45,
    "firstName": "Robert",
    "lastName": "Martin",
    "fullName": "Robert Martin"
  },
  "createdAt": "2025-11-17T10:23:45.123Z",
  "updatedAt": "2025-11-17T10:23:45.123Z"
}
```

---

#### 6. **Missing Resource Mapping Layer**

**Current Problem:**
Your controllers directly use DTOs from requests and return strings. No mapping between:
- DTOs ↔️ Entities
- Entities ↔️ Response DTOs

**Professional Solution: Mapper Pattern**

Create `BookMapper.java`:
```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;

public class BookMapper {

    public static BookResponseDTO toResponseDTO(Book book) {
        if (book == null) return null;

        return new BookResponseDTO(
            book.getId(),
            book.getTitle(),
            toAuthorDTO(book.getAuthor()),
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
    }

    public static List<BookResponseDTO> toResponseDTOs(List<Book> books) {
        return books.stream()
            .map(BookMapper::toResponseDTO)
            .toList();
    }

    private static AuthorDTO toAuthorDTO(Author author) {
        if (author == null) return null;

        return new AuthorDTO(
            author.getId(),
            author.getFirstName(),
            author.getLastName(),
            author.getFirstName() + " " + author.getLastName()
        );
    }

    // No toEntity method - that's in the service layer
}
```

**Alternative: MapStruct (Industry Standard)**

Add dependency:
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

Define mapper interface:
```java
@Mapper(componentModel = "spring")
public interface BookMapper {

    BookResponseDTO toResponseDTO(Book book);

    List<BookResponseDTO> toResponseDTOs(List<Book> books);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Book toEntity(BookRequestDTO dto);
}
```

MapStruct generates implementation at compile time!

---

### 🔷 SEVERITY: MEDIUM (Fix This Month)

#### 7. **No API Documentation**

Your API has zero documentation. New developers (or you in 3 months) won't know:
- What endpoints exist?
- What fields are required?
- What are valid values?
- What errors can occur?

**Solution: SpringDoc OpenAPI**

Add dependency:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

Add annotations:
```java
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Book management APIs")
public class BookController {

    @Operation(
        summary = "Create a new book",
        description = "Creates a new book with the specified details"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Book created successfully",
            content = @Content(schema = @Schema(implementation = BookResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<BookResponseDTO> createBook(
        @Valid @RequestBody BookRequestDTO request
    ) {
        // implementation
    }
}
```

**Access documentation:**
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

---

#### 8. **No Pagination for List Endpoints**

**Future Problem:**
When you have 10,000 books, this query will:
```java
@GetMapping
public ResponseEntity<List<BookResponseDTO>> getAllBooks() {
    return ResponseEntity.ok(bookService.findAll());
}
```

- Return 10,000 books in one response (megabytes of JSON)
- Crash mobile clients
- Take 30+ seconds to render
- Kill your database

**Solution: Spring Data Pagination**

```java
@GetMapping
public ResponseEntity<Page<BookResponseDTO>> getAllBooks(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "title") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir
) {
    Sort sort = sortDir.equalsIgnoreCase("desc")
        ? Sort.by(sortBy).descending()
        : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Book> booksPage = bookService.findAll(pageable);
    Page<BookResponseDTO> response = booksPage.map(BookMapper::toResponseDTO);

    return ResponseEntity.ok(response);
}
```

**Client usage:**
```
GET /api/v1/books?page=0&size=20&sortBy=title&sortDir=asc
```

**Response:**
```json
{
  "content": [ /* 20 books */ ],
  "totalElements": 10000,
  "totalPages": 500,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

## REST API Principles

### 1. **Resource-Oriented Design**

URLs represent **resources** (nouns), not **actions** (verbs):

❌ **Wrong:**
```
POST /api/v1/createBook
GET  /api/v1/getBookById/123
POST /api/v1/deleteBook
```

✅ **Correct:**
```
POST   /api/v1/books          (create)
GET    /api/v1/books/123      (read)
PUT    /api/v1/books/123      (update)
DELETE /api/v1/books/123      (delete)
```

---

### 2. **HTTP Status Codes Matter**

Use semantically correct status codes:

| Code | Meaning | When to Use |
|------|---------|-------------|
| 200 OK | Success | GET, PUT, PATCH successful |
| 201 Created | Resource created | POST successful |
| 204 No Content | Success, no body | DELETE successful |
| 400 Bad Request | Client error | Validation failed |
| 401 Unauthorized | Not authenticated | Missing/invalid auth |
| 403 Forbidden | Not authorized | Authenticated but no permission |
| 404 Not Found | Resource missing | GET /books/999 (doesn't exist) |
| 409 Conflict | Business rule violation | Duplicate email, etc. |
| 500 Internal Server Error | Server error | Unexpected exception |

**Your Current Mistake:**
```java
return ResponseEntity.ok("Book Added Successfully");  // ❌ 200
```

Should be:
```java
return ResponseEntity.status(HttpStatus.CREATED).body(response);  // ✅ 201
```

---

### 3. **Idempotency**

| Method | Idempotent? | Meaning |
|--------|-------------|---------|
| GET | ✅ Yes | Multiple calls return same result |
| PUT | ✅ Yes | Multiple updates with same data = same state |
| DELETE | ✅ Yes | Deleting deleted resource = still deleted |
| POST | ❌ No | Each call creates new resource |
| PATCH | ⚠️ Depends | Depends on implementation |

**Why it matters:**
- Network issues cause retries
- If POST is retried → duplicate books created
- If DELETE is retried → no harm, already deleted

---

### 4. **API Versioning**

You correctly use `/api/v1/`. **Never remove this.** Here's why:

**Without versioning:**
```java
// Version 1 (initial)
GET /api/books returns { "title": "...", "author": "..." }

// Version 2 (breaking change)
GET /api/books returns { "title": "...", "authors": [...] }
// 💥 All mobile apps crash
```

**With versioning:**
```java
GET /api/v1/books  // Old clients keep working
GET /api/v2/books  // New clients use enhanced version
```

**Versioning strategies:**

1. **URL Versioning** (your approach - GOOD)
```
/api/v1/books
/api/v2/books
```

2. **Header Versioning**
```
GET /api/books
Accept: application/vnd.bibby.v1+json
```

3. **Query Parameter**
```
GET /api/books?version=1
```

**Recommendation:** Stick with URL versioning—it's the most explicit and widely understood.

---

## Complete Example: Professional BookController

Here's how your BookController should look:

```java
package com.penrose.bibby.library.book;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Book management APIs")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @Operation(summary = "Create a new book")
    public ResponseEntity<BookResponseDTO> createBook(
        @Valid @RequestBody BookRequestDTO request
    ) {
        Book book = bookService.createBook(request);
        BookResponseDTO response = BookMapper.toResponseDTO(book);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("Location", "/api/v1/books/" + book.getId())
            .body(response);
    }

    @GetMapping
    @Operation(summary = "Get all books with pagination")
    public ResponseEntity<Page<BookResponseDTO>> getAllBooks(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "title") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Book> booksPage = bookService.findAll(pageable);
        Page<BookResponseDTO> response = booksPage.map(BookMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<BookResponseDTO> getBookById(
        @PathVariable Long id
    ) {
        Book book = bookService.findById(id);
        return ResponseEntity.ok(BookMapper.toResponseDTO(book));
    }

    @GetMapping("/search")
    @Operation(summary = "Search books by title")
    public ResponseEntity<Page<BookResponseDTO>> searchBooksByTitle(
        @RequestParam String title,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> booksPage = bookService.searchByTitle(title, pageable);
        Page<BookResponseDTO> response = booksPage.map(BookMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing book")
    public ResponseEntity<BookResponseDTO> updateBook(
        @PathVariable Long id,
        @Valid @RequestBody BookRequestDTO request
    ) {
        Book book = bookService.updateBook(id, request);
        return ResponseEntity.ok(BookMapper.toResponseDTO(book));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Action Items

### 🚨 Critical (Do First - 2-3 hours)

1. **Fix GET with Request Body**
   - [ ] Change `findBookByTitle` to use `@RequestParam` or `@PathVariable`
   - [ ] Return `ResponseEntity<BookResponseDTO>` instead of `void`
   - [ ] Remove `System.out.println`
   - **File:** BookController.java:24-28

2. **Add Input Validation**
   - [ ] Add `spring-boot-starter-validation` dependency
   - [ ] Add `@Valid`, `@NotBlank`, `@Size` to BookRequestDTO
   - [ ] Add validation to BookcaseDTO
   - **Files:** BookRequestDTO.java, BookcaseDTO.java, pom.xml

3. **Create Global Exception Handler**
   - [ ] Create `GlobalExceptionHandler.java`
   - [ ] Handle `MethodArgumentNotValidException`
   - [ ] Create `ResourceNotFoundException`
   - [ ] Create `ErrorResponse` record
   - **New files:** web/GlobalExceptionHandler.java, exception/ResourceNotFoundException.java

### 🔶 High Priority (This Week - 4-6 hours)

4. **Standardize URL Patterns**
   - [ ] Add `@RequestMapping("/api/v1/bookcases")` to BookCaseController
   - [ ] Change `/create/bookcase` to just `@PostMapping`
   - [ ] Ensure all controllers use `/api/v1/` prefix

5. **Create Response DTOs**
   - [ ] Create `BookResponseDTO` (separate from request DTO)
   - [ ] Create `AuthorDTO` for nested author data
   - [ ] Create `BookcaseResponseDTO`
   - [ ] Update controllers to return response DTOs

6. **Implement Mapper Layer**
   - [ ] Create `BookMapper.java` with `toResponseDTO` methods
   - [ ] Create `BookcaseMapper.java`
   - [ ] Update controllers to use mappers

7. **Fix HTTP Status Codes**
   - [ ] POST endpoints return 201 CREATED
   - [ ] DELETE endpoints return 204 NO CONTENT
   - [ ] Add `Location` header to POST responses

### 🔷 Medium Priority (This Month - 4-5 hours)

8. **Add API Documentation**
   - [ ] Add `springdoc-openapi-starter-webmvc-ui` dependency
   - [ ] Add `@Tag` to controllers
   - [ ] Add `@Operation` to endpoints
   - [ ] Add `@ApiResponses` to complex endpoints
   - [ ] Test Swagger UI at http://localhost:8080/swagger-ui.html

9. **Implement Pagination**
   - [ ] Add pagination to `GET /api/v1/books`
   - [ ] Update BookService to accept `Pageable`
   - [ ] Return `Page<BookResponseDTO>` instead of `List<BookResponseDTO>`

10. **Complete CRUD Operations**
    - [ ] Add PUT endpoint for updating books
    - [ ] Add DELETE endpoint for deleting books
    - [ ] Add GET by ID endpoint
    - [ ] Test all endpoints with Postman or curl

### ⚪ Low Priority (Nice to Have)

11. **Advanced Features**
    - [ ] Add HATEOAS links to responses
    - [ ] Implement ETags for caching
    - [ ] Add rate limiting
    - [ ] Add API authentication (next section)

---

## Testing Your API

### Manual Testing with curl

**Create a book:**
```bash
curl -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "firstName": "Robert",
    "lastName": "Martin"
  }'
```

**Get all books:**
```bash
curl http://localhost:8080/api/v1/books?page=0&size=10
```

**Get book by ID:**
```bash
curl http://localhost:8080/api/v1/books/1
```

**Update book:**
```bash
curl -X PUT http://localhost:8080/api/v1/books/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code (Updated)",
    "firstName": "Robert",
    "lastName": "Martin"
  }'
```

**Delete book:**
```bash
curl -X DELETE http://localhost:8080/api/v1/books/1
```

### Automated Testing (Section 20)

```java
@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createBook_withValidData_returns201() throws Exception {
        String requestBody = """
            {
                "title": "Clean Code",
                "firstName": "Robert",
                "lastName": "Martin"
            }
            """;

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Clean Code"))
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createBook_withBlankTitle_returns400() throws Exception {
        String requestBody = """
            {
                "title": "",
                "firstName": "Robert",
                "lastName": "Martin"
            }
            """;

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").exists());
    }
}
```

---

## Career Perspective: Why This Matters

### Interview Question You'll Face
**"Design a REST API for a library management system."**

**Bad Answer (Your Current Code):**
> "I'd create a BookController with POST and GET methods..."

**Good Answer (After This Section):**
> "I'd design resource-oriented endpoints following REST principles:
> - POST /api/v1/books for creation (201 status)
> - GET /api/v1/books with pagination
> - Separate request/response DTOs to protect internal structure
> - Bean validation for input
> - Global exception handler for consistent errors
> - OpenAPI documentation
> - Proper HTTP semantics and idempotency"

### Code Review Comments You'll Receive

Your current code would receive these comments in professional review:

1. ❌ "GET requests cannot have request bodies per RFC 7231"
2. ❌ "Why does findBookByTitle return void?"
3. ❌ "No input validation—what if title is null?"
4. ❌ "Inconsistent URL patterns (/api/v1/books vs /create/bookcase)"
5. ❌ "POST should return 201, not 200"
6. ❌ "No API documentation—how do clients know what to send?"
7. ❌ "System.out.println in controller—use logger"

**All of these would block your PR from merging.**

---

## Common Mistakes to Avoid

### 1. **Returning Entities Directly**
```java
@GetMapping("/{id}")
public Book getBook(@PathVariable Long id) {  // ❌ DON'T
    return bookService.findById(id);
}
```

**Why it's bad:**
- Exposes database structure to clients
- Changes to entity break API
- Lazy-loading causes JSON serialization errors
- Can't control what fields are returned

### 2. **Generic Exception Messages**
```java
catch (Exception e) {
    return ResponseEntity.badRequest().body("Error");  // ❌ Useless
}
```

### 3. **Using POST for Everything**
```java
@PostMapping("/getBook")  // ❌ Wrong HTTP method
@PostMapping("/updateBook")  // ❌ Wrong HTTP method
@PostMapping("/deleteBook")  // ❌ Wrong HTTP method
```

### 4. **Ignoring HTTP Status Codes**
```java
if (book == null) {
    return ResponseEntity.ok("Book not found");  // ❌ Should be 404
}
```

---

## Resources

### Books
- **"REST API Design Rulebook"** by Mark Massé
- **"RESTful Web Services"** by Leonard Richardson

### Online
- [Spring REST Tutorial](https://spring.io/guides/tutorials/rest)
- [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html)
- [HTTP Status Dogs](https://httpstatusdogs.com/) (fun way to learn status codes)

### Tools
- **Postman:** API testing GUI
- **Swagger UI:** Auto-generated from your code
- **curl:** Command-line testing
- **HTTPie:** Better curl (https://httpie.io)

---

## Summary

### Your Current State
- ❌ GET with request body (HTTP violation)
- ❌ No validation
- ❌ No error handling
- ❌ Inconsistent URLs
- ❌ Returns strings instead of structured data
- ❌ No documentation
- ❌ No pagination

### After This Section
- ✅ RESTful endpoints following HTTP standards
- ✅ Input validation with meaningful errors
- ✅ Global exception handling
- ✅ Consistent URL patterns (`/api/v1/resource`)
- ✅ Response DTOs protecting internal structure
- ✅ Proper HTTP status codes
- ✅ API documentation with Swagger
- ✅ Pagination for large datasets

---

## Mentor's Note

Leo, your API code shows you're learning Spring Boot, which is great! But right now, **this code would not pass code review at any company I've worked at.**

The good news? These are **all fixable** in a weekend. Unlike architectural issues that require weeks to refactor, REST API issues are mostly about:
1. Following HTTP standards
2. Adding validation annotations
3. Returning correct status codes
4. Creating DTOs

**Priority Order:**
1. Fix the GET with request body (30 minutes)
2. Add validation (1 hour)
3. Add exception handler (1 hour)
4. Fix status codes (30 minutes)
5. Everything else (4-6 hours)

After fixing these, your API will be **production-ready** and **interview-worthy**.

---

**Next Section:** Spring Data JPA & Database Best Practices

**Last Updated:** 2025-11-17
**Status:** Complete ✅
