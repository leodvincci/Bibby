# Section 08: Portfolio Project Architecture (Phase 1)
**REST API Design & Implementation**

**Week 8-9 Transition: From CLI Tool to Enterprise-Grade REST API**

---

## Overview

You've built strong fundamentals: clean code principles, domain modeling, JPA best practices, and core algorithms. Now it's time to transform Bibby from a command-line tool into a production-ready REST API that showcases enterprise architecture skills.

This section bridges theory and practice. We'll analyze Bibby's existing REST endpoints (yes, you already have some!), identify critical gaps, and systematically elevate the codebase to professional standards. By the end, you'll have a REST API with proper validation, exception handling, API documentation, and integration tests—skills that directly translate to backend roles in industrial software.

**This Week's Focus:**
- Analyze and refactor existing REST controllers
- Implement comprehensive request validation
- Build centralized exception handling
- Generate OpenAPI documentation
- Write integration tests for REST endpoints
- Prepare for containerization and cloud deployment

---

## Part 1: Current State Analysis — Bibby's REST API

Let's start by examining what already exists in `/src/main/java/com/penrose/bibby/library/book/BookController.java`:

```java
@RestController
public class BookController {
    final BookService bookService;
    final AuthorRepository authorRepository;

    public BookController(BookService bookService, AuthorRepository authorRepository,
                          BookRepository bookRepository){
        this.bookService = bookService;
        this.authorRepository = authorRepository;
    }

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

### Issues Identified

**Issue #1: Inconsistent Path Mapping**
- No `@RequestMapping` at class level
- Hardcoded full paths in each method (`"api/v1/books"`)
- Violates DRY principle

**Issue #2: Incorrect HTTP Semantics**
- `findBookByTitle` uses `@GetMapping` with `@RequestBody` (GET requests shouldn't have bodies per HTTP spec)
- Search should use query parameters or path variables
- No proper response type (returns `void`)

**Issue #3: Primitive Response Types**
- `addBook` returns `ResponseEntity<String>` instead of created resource
- No structured error responses
- Missing HTTP status codes (201 Created, 404 Not Found, etc.)

**Issue #4: No Validation**
- `BookRequestDTO` is just: `record BookRequestDTO(String title, String firstName, String lastName) {}`
- No null checks, length constraints, format validation
- Invalid data can reach service layer

**Issue #5: No Exception Handling**
- If `createNewBook` throws exception, client gets generic 500 error
- No meaningful error messages
- No distinction between client errors (400s) vs server errors (500s)

**Issue #6: Missing RESTful Operations**
- No GET by ID, UPDATE (PUT/PATCH), DELETE endpoints
- Not a complete CRUD API

**Issue #7: Unused Dependency**
- Constructor injects `BookRepository` but never uses it (code smell)

### Industrial Connection: API Design = Operational Interface Design

In your Navy and Kinder Morgan roles, you interfaced with systems through control panels, dashboards, SCADA systems. Each had:
- **Clear inputs/outputs:** What data goes in, what comes out
- **Error codes:** System tells you *why* something failed
- **Documentation:** Manuals explaining each operation
- **Validation:** System prevents invalid operations (e.g., can't open valve if pressure too high)

REST APIs are the same. Your API *is* the operational interface for Bibby. Clients (frontend apps, mobile apps, other services) interact through HTTP requests just like operators interact through control systems.

**Poor API design = poor operational interface:**
- No validation → system accepts garbage input → data corruption
- No error codes → operators can't diagnose failures → downtime
- No documentation → steep learning curve → slow onboarding

**Good API design = professional operational interface:**
- Validation prevents invalid states
- Clear error responses enable quick troubleshooting
- Auto-generated documentation serves as the "operator manual"

---

## Part 2: REST API Design Principles

### Principle 1: Resource-Oriented URLs

REST APIs model *resources* (nouns) and operations (HTTP verbs).

**Bad (RPC-style):**
```
POST /api/v1/createBook
POST /api/v1/deleteBook
GET /api/v1/getBookById
```

**Good (RESTful):**
```
POST   /api/v1/books          → Create book
GET    /api/v1/books/{id}     → Get book by ID
GET    /api/v1/books          → List books (with query params)
PUT    /api/v1/books/{id}     → Update entire book
PATCH  /api/v1/books/{id}     → Partially update book
DELETE /api/v1/books/{id}     → Delete book
```

**URL Structure:**
```
/{version}/{resource-collection}/{resource-id}/{sub-resource}
/api/v1/books/123/checkouts  → Get checkouts for book 123
```

### Principle 2: HTTP Status Codes Matter

Use semantic status codes:

**Success (2xx):**
- `200 OK` - Successful GET, PUT, PATCH, DELETE
- `201 Created` - Successful POST (return Location header with new resource URL)
- `204 No Content` - Successful DELETE with no response body

**Client Errors (4xx):**
- `400 Bad Request` - Validation failure, malformed request
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Authenticated but not authorized
- `404 Not Found` - Resource doesn't exist
- `409 Conflict` - Business rule violation (e.g., book already checked out)

**Server Errors (5xx):**
- `500 Internal Server Error` - Unhandled exception
- `503 Service Unavailable` - Dependency failure (database down)

### Principle 3: Consistent Response Format

**Success Response:**
```json
{
  "id": 123,
  "title": "Clean Code",
  "isbn": "978-0132350884",
  "status": "AVAILABLE",
  "authors": [
    {"id": 45, "firstName": "Robert", "lastName": "Martin"}
  ],
  "shelf": {
    "id": 12,
    "identifier": "A-3"
  },
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

**Error Response (RFC 7807 Problem Details):**
```json
{
  "type": "/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/books",
  "timestamp": "2025-01-15T10:30:00Z",
  "errors": [
    {
      "field": "title",
      "rejectedValue": "",
      "message": "Title cannot be blank"
    },
    {
      "field": "isbn",
      "rejectedValue": "invalid",
      "message": "ISBN must match format XXX-XXXXXXXXXX"
    }
  ]
}
```

### Principle 4: Input Validation at API Boundary

**Defense in Depth:**
1. **Controller layer:** Validate request format/structure (JSR-303 Bean Validation)
2. **Service layer:** Validate business rules (e.g., can't checkout book that's already checked out)
3. **Database layer:** Enforce constraints (NOT NULL, UNIQUE, foreign keys)

Never trust client input. Always validate at the API boundary before processing.

---

## Part 3: Refactoring Bibby's Book Controller

### Step 1: Create Response DTOs

**Current problem:** Returning entities directly exposes internal structure, creates tight coupling.

**Solution:** Separate DTOs for requests and responses.

**File:** `src/main/java/com/penrose/bibby/library/book/BookResponseDTO.java`

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorResponseDTO;
import com.penrose.bibby.library.shelf.ShelfSummaryDTO;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for Book resource.
 * Used in API responses - contains full book details.
 */
public record BookResponseDTO(
    Long id,
    String title,
    String isbn,
    BookStatus status,
    Set<AuthorResponseDTO> authors,
    ShelfSummaryDTO shelf,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Factory method to convert entity to response DTO.
     * This encapsulates the mapping logic.
     */
    public static BookResponseDTO fromEntity(BookEntity entity) {
        return new BookResponseDTO(
            entity.getId(),
            entity.getTitle(),
            entity.getIsbn(),
            entity.getStatus(),
            entity.getAuthors().stream()
                .map(AuthorResponseDTO::fromEntity)
                .collect(Collectors.toSet()),
            entity.getShelf() != null
                ? ShelfSummaryDTO.fromEntity(entity.getShelf())
                : null,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
```

**File:** `src/main/java/com/penrose/bibby/library/book/BookSummaryDTO.java`

```java
package com.penrose.bibby.library.book;

/**
 * Lightweight DTO for book listings.
 * Used when full details aren't needed (e.g., GET /books returns list of summaries).
 */
public record BookSummaryDTO(
    Long id,
    String title,
    String isbn,
    BookStatus status
) {
    public static BookSummaryDTO fromEntity(BookEntity entity) {
        return new BookSummaryDTO(
            entity.getId(),
            entity.getTitle(),
            entity.getIsbn(),
            entity.getStatus()
        );
    }
}
```

**Why two DTOs?**
- `BookResponseDTO`: Full details (includes authors, shelf) for GET /books/{id}
- `BookSummaryDTO`: Lightweight for GET /books (list endpoint)

This prevents over-fetching and improves performance.

### Step 2: Add Validation to Request DTO

**File:** `src/main/java/com/penrose/bibby/library/book/BookCreateRequestDTO.java`

```java
package com.penrose.bibby.library.book;

import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * Request DTO for creating a new book.
 * Validation ensures data quality at API boundary.
 */
public record BookCreateRequestDTO(

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    String title,

    @NotBlank(message = "ISBN is required")
    @Pattern(
        regexp = "^(?:\\d{3}-)?\\d{10}$",
        message = "ISBN must match format XXX-XXXXXXXXXX or XXXXXXXXXX"
    )
    String isbn,

    @NotEmpty(message = "At least one author is required")
    Set<@Positive(message = "Author ID must be positive") Long> authorIds,

    @Positive(message = "Shelf ID must be positive")
    Long shelfId
) {}
```

**File:** `src/main/java/com/penrose/bibby/library/book/BookUpdateRequestDTO.java`

```java
package com.penrose.bibby.library.book;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating an existing book.
 * All fields optional (partial updates allowed).
 */
public record BookUpdateRequestDTO(

    @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    String title,

    @Pattern(
        regexp = "^(?:\\d{3}-)?\\d{10}$",
        message = "ISBN must match format XXX-XXXXXXXXXX or XXXXXXXXXX"
    )
    String isbn,

    BookStatus status,

    @Positive(message = "Shelf ID must be positive")
    Long shelfId
) {}
```

**JSR-303 Validation Annotations:**
- `@NotBlank`: Not null, not empty, not just whitespace
- `@NotEmpty`: Collection must have at least one element
- `@Size`: String/collection size constraints
- `@Pattern`: Regex validation
- `@Positive`: Number > 0
- `@Email`, `@URL`, `@Past`, `@Future`: Common format validations

### Step 3: Refactor Controller with Proper REST Design

**File:** `src/main/java/com/penrose/bibby/library/book/BookController.java`

```java
package com.penrose.bibby.library.book;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for Book resource operations.
 * Handles CRUD operations for library books.
 */
@RestController
@RequestMapping("/api/v1/books")
@Validated  // Enables validation for path variables/params
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Create a new book.
     *
     * POST /api/v1/books
     *
     * Industrial analogy: Adding new asset to inventory system.
     * Must validate all required fields before creation.
     *
     * @param requestDTO validated book creation data
     * @return 201 Created with Location header pointing to new resource
     */
    @PostMapping
    public ResponseEntity<BookResponseDTO> createBook(
            @Valid @RequestBody BookCreateRequestDTO requestDTO) {

        BookEntity created = bookService.createBook(requestDTO);
        BookResponseDTO response = BookResponseDTO.fromEntity(created);

        // Build URI for Location header: /api/v1/books/{id}
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(response);
    }

    /**
     * Get book by ID.
     *
     * GET /api/v1/books/{id}
     *
     * @param id book ID (validated as positive)
     * @return 200 OK with book details, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDTO> getBookById(
            @PathVariable @Positive Long id) {

        BookEntity book = bookService.findById(id);
        return ResponseEntity.ok(BookResponseDTO.fromEntity(book));
    }

    /**
     * List books with pagination and optional filtering.
     *
     * GET /api/v1/books?page=0&size=20&status=AVAILABLE
     *
     * Industrial analogy: Querying inventory system with filters.
     * Pagination prevents loading entire dataset (memory efficiency).
     *
     * @param status optional filter by book status
     * @param pageable pagination params (default: page=0, size=20)
     * @return 200 OK with paginated list
     */
    @GetMapping
    public ResponseEntity<Page<BookSummaryDTO>> listBooks(
            @RequestParam(required = false) BookStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BookEntity> books = status != null
            ? bookService.findByStatus(status, pageable)
            : bookService.findAll(pageable);

        Page<BookSummaryDTO> response = books.map(BookSummaryDTO::fromEntity);
        return ResponseEntity.ok(response);
    }

    /**
     * Search books by title.
     *
     * GET /api/v1/books/search?q=clean+code
     *
     * @param query search query (min 2 characters)
     * @param pageable pagination params
     * @return 200 OK with matching books
     */
    @GetMapping("/search")
    public ResponseEntity<Page<BookSummaryDTO>> searchBooks(
            @RequestParam("q") @Size(min = 2, message = "Query must be at least 2 characters") String query,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BookEntity> books = bookService.searchByTitle(query, pageable);
        Page<BookSummaryDTO> response = books.map(BookSummaryDTO::fromEntity);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing book (partial update).
     *
     * PATCH /api/v1/books/{id}
     *
     * Industrial analogy: Updating asset metadata in tracking system.
     * Only fields provided in request are updated.
     *
     * @param id book ID to update
     * @param requestDTO validated update data (all fields optional)
     * @return 200 OK with updated book
     */
    @PatchMapping("/{id}")
    public ResponseEntity<BookResponseDTO> updateBook(
            @PathVariable @Positive Long id,
            @Valid @RequestBody BookUpdateRequestDTO requestDTO) {

        BookEntity updated = bookService.updateBook(id, requestDTO);
        return ResponseEntity.ok(BookResponseDTO.fromEntity(updated));
    }

    /**
     * Delete a book.
     *
     * DELETE /api/v1/books/{id}
     *
     * Industrial analogy: Removing decommissioned asset from inventory.
     * Returns 204 No Content on success (no response body needed).
     *
     * @param id book ID to delete
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable @Positive Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change book status.
     *
     * PUT /api/v1/books/{id}/status
     *
     * Industrial analogy: State transition (AVAILABLE → CHECKED_OUT).
     * Validates transition is allowed per business rules.
     *
     * @param id book ID
     * @param newStatus target status
     * @return 200 OK with updated book
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<BookResponseDTO> updateBookStatus(
            @PathVariable @Positive Long id,
            @RequestParam BookStatus newStatus) {

        BookEntity updated = bookService.updateStatus(id, newStatus);
        return ResponseEntity.ok(BookResponseDTO.fromEntity(updated));
    }
}
```

### Key Improvements

1. **`@RequestMapping("/api/v1/books")`** at class level - DRY principle
2. **Proper HTTP verbs** - POST, GET, PATCH, DELETE with semantic meaning
3. **`@Valid` annotation** - Triggers JSR-303 validation before method executes
4. **Path variables** for resource IDs - `GET /books/{id}` instead of request body
5. **Query parameters** for filtering - `?status=AVAILABLE&page=0`
6. **Pagination** with `Pageable` - prevents loading all books into memory
7. **Location header** on POST - tells client where to find created resource
8. **Specific response types** - `BookResponseDTO` vs `BookSummaryDTO`
9. **Unused dependency removed** - `BookRepository` not needed in controller

---

## Part 4: Centralized Exception Handling

Currently, when `bookService.findById(999)` throws `BookNotFoundException`, Spring returns generic 500 error. We need structured error responses.

### Exception Hierarchy

**File:** `src/main/java/com/penrose/bibby/exception/ResourceNotFoundException.java`

```java
package com.penrose.bibby.exception;

/**
 * Base exception for resource not found scenarios.
 * Results in 404 Not Found HTTP response.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final Object identifier;

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
```

**File:** `src/main/java/com/penrose/bibby/exception/BookNotFoundException.java`

```java
package com.penrose.bibby.exception;

public class BookNotFoundException extends ResourceNotFoundException {
    public BookNotFoundException(Long id) {
        super("Book", id);
    }
}
```

**File:** `src/main/java/com/penrose/bibby/exception/BusinessRuleViolationException.java`

```java
package com.penrose.bibby.exception;

/**
 * Exception for business rule violations.
 * Results in 409 Conflict HTTP response.
 *
 * Example: Attempting to check out a book that's already checked out.
 */
public class BusinessRuleViolationException extends RuntimeException {

    private final String ruleViolated;

    public BusinessRuleViolationException(String message, String ruleViolated) {
        super(message);
        this.ruleViolated = ruleViolated;
    }

    public String getRuleViolated() {
        return ruleViolated;
    }
}
```

### Global Exception Handler

**File:** `src/main/java/com/penrose/bibby/exception/GlobalExceptionHandler.java`

```java
package com.penrose.bibby.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * Converts exceptions into RFC 7807 Problem Detail responses.
 *
 * Industrial analogy: Centralized error handling = centralized alarm system.
 * All failures route through one handler, ensuring consistent error reporting.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation failures from @Valid annotation.
     * Triggered when request DTO validation fails.
     *
     * Returns 400 Bad Request with detailed field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        // Create RFC 7807 Problem Detail
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed"
        );

        problemDetail.setType(URI.create("/errors/validation-failed"));
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        // Extract field errors
        List<FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldError(
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage()
            ))
            .collect(Collectors.toList());

        problemDetail.setProperty("errors", fieldErrors);

        return problemDetail;
    }

    /**
     * Handle validation failures from @Validated annotation.
     * Triggered when path variable/request param validation fails.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Constraint violation"
        );

        problemDetail.setType(URI.create("/errors/constraint-violation"));
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        List<String> violations = ex.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList());

        problemDetail.setProperty("violations", violations);

        return problemDetail;
    }

    /**
     * Handle resource not found exceptions.
     * Returns 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );

        problemDetail.setType(URI.create("/errors/resource-not-found"));
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("resourceType", ex.getResourceType());
        problemDetail.setProperty("identifier", ex.getIdentifier());

        return problemDetail;
    }

    /**
     * Handle business rule violations.
     * Returns 409 Conflict.
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRuleViolation(
            BusinessRuleViolationException ex,
            WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );

        problemDetail.setType(URI.create("/errors/business-rule-violation"));
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("ruleViolated", ex.getRuleViolated());

        return problemDetail;
    }

    /**
     * Catch-all for unhandled exceptions.
     * Returns 500 Internal Server Error.
     *
     * IMPORTANT: Log full stack trace for debugging, but don't expose to client.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(
            Exception ex,
            WebRequest request) {

        // TODO: Add logging here
        // logger.error("Unhandled exception", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );

        problemDetail.setType(URI.create("/errors/internal-server-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        // Don't expose stack trace to client in production
        return problemDetail;
    }

    /**
     * Helper record for field error details.
     */
    private record FieldError(
        String field,
        Object rejectedValue,
        String message
    ) {}
}
```

### Using Exceptions in Service Layer

**File:** `src/main/java/com/penrose/bibby/library/book/BookService.java` (excerpt)

```java
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final ShelfRepository shelfRepository;
    private final AuthorRepository authorRepository;

    // ... constructor

    public BookEntity findById(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));
        // When thrown, GlobalExceptionHandler converts to 404 response
    }

    public BookEntity updateStatus(Long id, BookStatus newStatus) {
        BookEntity book = findById(id);

        // Business rule: validate state transition
        if (!book.getStatus().allowedTransitions().contains(newStatus)) {
            throw new BusinessRuleViolationException(
                String.format("Cannot transition from %s to %s",
                    book.getStatus(), newStatus),
                "INVALID_STATUS_TRANSITION"
            );
            // Returns 409 Conflict with structured error
        }

        book.setStatus(newStatus);
        return bookRepository.save(book);
    }

    public BookEntity createBook(BookCreateRequestDTO dto) {
        // Validate shelf exists
        ShelfEntity shelf = shelfRepository.findById(dto.shelfId())
            .orElseThrow(() -> new ResourceNotFoundException("Shelf", dto.shelfId()));

        // Validate all authors exist
        Set<AuthorEntity> authors = dto.authorIds().stream()
            .map(authorId -> authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", authorId)))
            .collect(Collectors.toSet());

        BookEntity book = new BookEntity();
        book.setTitle(dto.title());
        book.setIsbn(dto.isbn());
        book.setShelf(shelf);
        book.setAuthors(authors);
        book.setStatus(BookStatus.AVAILABLE);

        return bookRepository.save(book);
    }
}
```

---

## Part 5: API Documentation with SpringDoc OpenAPI

Auto-generated API documentation is critical for:
- **Frontend developers** integrating with your API
- **QA testers** understanding endpoints
- **Future you** remembering what you built 6 months ago
- **Interviewers** seeing professional documentation practices

### Add Dependency

**File:** `pom.xml`

Add inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

This automatically:
- Scans all `@RestController` classes
- Generates OpenAPI 3.0 specification
- Serves interactive Swagger UI at `/swagger-ui.html`
- Exposes spec JSON at `/v3/api-docs`

### Configure OpenAPI Metadata

**File:** `src/main/java/com/penrose/bibby/config/OpenApiConfig.java`

```java
package com.penrose.bibby.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bibbyOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Bibby Library Management API")
                .version("1.0.0")
                .description("""
                    REST API for managing library operations including books, shelves,
                    authors, and checkout workflows. Demonstrates industrial software
                    patterns: state management, hierarchical data modeling, and
                    business rule validation.
                    """)
                .contact(new Contact()
                    .name("Leo [Your Last Name]")
                    .email("your.email@example.com")
                    .url("https://github.com/yourusername/bibby")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local development server"),
                new Server()
                    .url("https://bibby-api.your-domain.com")
                    .description("Production server (future)")
            ));
    }
}
```

### Enhance Controllers with OpenAPI Annotations

Update `BookController.java`:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/books")
@Validated
@Tag(name = "Books", description = "Book resource management operations")
public class BookController {

    // ...

    @Operation(
        summary = "Create a new book",
        description = "Adds a new book to the library inventory. Validates authors and shelf exist."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Book created successfully",
            content = @Content(schema = @Schema(implementation = BookResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Referenced shelf or author not found"
        )
    })
    @PostMapping
    public ResponseEntity<BookResponseDTO> createBook(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Book creation data",
                required = true
            )
            @Valid @RequestBody BookCreateRequestDTO requestDTO) {
        // ... implementation
    }

    @Operation(
        summary = "Get book by ID",
        description = "Retrieves full book details including authors and shelf information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Book found"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDTO> getBookById(
            @Parameter(description = "Book ID", example = "123")
            @PathVariable @Positive Long id) {
        // ... implementation
    }
}
```

**Access documentation:**
1. Start application: `mvn spring-boot:run`
2. Open browser: `http://localhost:8080/swagger-ui.html`
3. Interactive UI lets you test endpoints directly!

---

## Part 6: Integration Testing for REST APIs

Unit tests verify individual methods. **Integration tests** verify the entire request/response cycle through actual HTTP calls.

### Add Test Dependency

Already included in `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Integration Test Structure

**File:** `src/test/java/com/penrose/bibby/library/book/BookControllerIntegrationTest.java`

```java
package com.penrose.bibby.library.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BookController.
 *
 * Uses @SpringBootTest to load full application context.
 * Uses MockMvc to simulate HTTP requests without starting server.
 * Uses @Transactional to rollback database changes after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  // Use application-test.properties
@Transactional  // Rollback after each test
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;  // For JSON serialization

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    private ShelfEntity testShelf;
    private AuthorEntity testAuthor;

    @BeforeEach
    void setUp() {
        // Create test data
        testShelf = new ShelfEntity();
        testShelf.setIdentifier("TEST-A-1");
        testShelf.setCapacity(50);
        testShelf = shelfRepository.save(testShelf);

        testAuthor = new AuthorEntity();
        testAuthor.setFirstName("Robert");
        testAuthor.setLastName("Martin");
        testAuthor = authorRepository.save(testAuthor);
    }

    @Test
    void createBook_WithValidData_Returns201Created() throws Exception {
        // Arrange
        BookCreateRequestDTO request = new BookCreateRequestDTO(
            "Clean Code",
            "978-0132350884",
            Set.of(testAuthor.getId()),
            testShelf.getId()
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Clean Code"))
            .andExpect(jsonPath("$.isbn").value("978-0132350884"))
            .andExpect(jsonPath("$.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.authors", hasSize(1)))
            .andExpect(jsonPath("$.authors[0].firstName").value("Robert"))
            .andExpect(jsonPath("$.shelf.id").value(testShelf.getId()));
    }

    @Test
    void createBook_WithBlankTitle_Returns400BadRequest() throws Exception {
        // Arrange
        BookCreateRequestDTO request = new BookCreateRequestDTO(
            "",  // Blank title (violates @NotBlank)
            "978-0132350884",
            Set.of(testAuthor.getId()),
            testShelf.getId()
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists());
    }

    @Test
    void createBook_WithInvalidISBN_Returns400BadRequest() throws Exception {
        BookCreateRequestDTO request = new BookCreateRequestDTO(
            "Clean Code",
            "invalid-isbn",  // Violates regex pattern
            Set.of(testAuthor.getId()),
            testShelf.getId()
        );

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[?(@.field == 'isbn')]").exists())
            .andExpect(jsonPath("$.errors[?(@.field == 'isbn')].message")
                .value(containsString("ISBN must match format")));
    }

    @Test
    void createBook_WithNonExistentShelf_Returns404NotFound() throws Exception {
        BookCreateRequestDTO request = new BookCreateRequestDTO(
            "Clean Code",
            "978-0132350884",
            Set.of(testAuthor.getId()),
            99999L  // Non-existent shelf ID
        );

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"))
            .andExpect(jsonPath("$.resourceType").value("Shelf"))
            .andExpect(jsonPath("$.identifier").value(99999));
    }

    @Test
    void getBookById_WhenExists_Returns200Ok() throws Exception {
        // Arrange: Create book first
        BookEntity book = new BookEntity();
        book.setTitle("Test Book");
        book.setIsbn("978-1234567890");
        book.setStatus(BookStatus.AVAILABLE);
        book.setShelf(testShelf);
        book.setAuthors(Set.of(testAuthor));
        book = bookRepository.save(book);

        // Act & Assert
        mockMvc.perform(get("/api/v1/books/{id}", book.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(book.getId()))
            .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void getBookById_WhenNotExists_Returns404NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"))
            .andExpect(jsonPath("$.resourceType").value("Book"));
    }

    @Test
    void getBookById_WithNegativeId_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", -1L))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Constraint Violation"));
    }

    @Test
    void listBooks_ReturnsPagedResults() throws Exception {
        // Arrange: Create multiple books
        for (int i = 1; i <= 25; i++) {
            BookEntity book = new BookEntity();
            book.setTitle("Book " + i);
            book.setIsbn("978-123456789" + i);
            book.setStatus(BookStatus.AVAILABLE);
            book.setShelf(testShelf);
            book.setAuthors(Set.of(testAuthor));
            bookRepository.save(book);
        }

        // Act & Assert: Default page size is 20
        mockMvc.perform(get("/api/v1/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(20)))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(0));  // First page

        // Request second page
        mockMvc.perform(get("/api/v1/books?page=1&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))  // Remaining 5 books
            .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void updateBookStatus_ValidTransition_Returns200Ok() throws Exception {
        // Arrange
        BookEntity book = new BookEntity();
        book.setTitle("Test Book");
        book.setIsbn("978-1234567890");
        book.setStatus(BookStatus.AVAILABLE);
        book.setShelf(testShelf);
        book.setAuthors(Set.of(testAuthor));
        book = bookRepository.save(book);

        // Act: Transition AVAILABLE → CHECKED_OUT
        mockMvc.perform(put("/api/v1/books/{id}/status", book.getId())
                .param("newStatus", "CHECKED_OUT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CHECKED_OUT"));
    }

    @Test
    void deleteBook_WhenExists_Returns204NoContent() throws Exception {
        // Arrange
        BookEntity book = new BookEntity();
        book.setTitle("Test Book");
        book.setIsbn("978-1234567890");
        book.setStatus(BookStatus.AVAILABLE);
        book.setShelf(testShelf);
        book.setAuthors(Set.of(testAuthor));
        book = bookRepository.save(book);

        Long bookId = book.getId();

        // Act
        mockMvc.perform(delete("/api/v1/books/{id}", bookId))
            .andExpect(status().isNoContent());

        // Assert: Book deleted
        mockMvc.perform(get("/api/v1/books/{id}", bookId))
            .andExpect(status().isNotFound());
    }
}
```

### Test Configuration

**File:** `src/test/resources/application-test.properties`

```properties
# Use in-memory H2 database for tests (faster than PostgreSQL)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# Show SQL in test logs (helpful for debugging)
spring.jpa.show-sql=true

# Disable unnecessary features for tests
spring.shell.interactive.enabled=false
```

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookControllerIntegrationTest

# Run with coverage report
mvn test jacoco:report
```

**Industrial Connection: Integration Tests = System Acceptance Tests**

In operational systems, you don't just unit test individual components (valves, pumps). You run **system integration tests**: "When I open valve A and start pump B, does pressure gauge C read expected value?"

REST API integration tests are the same:
- **Input:** HTTP request with specific data
- **Processing:** Full application stack (controller → service → repository → database)
- **Output:** HTTP response with expected status code and body

These tests catch issues unit tests miss:
- JSON serialization/deserialization problems
- Validation annotation configuration errors
- Database constraint violations
- Exception handling gaps

---

## Part 7: CORS Configuration for Frontend Integration

**CORS (Cross-Origin Resource Sharing):** Security feature that controls which domains can call your API.

**Scenario:** Your API runs on `localhost:8080`, but React frontend runs on `localhost:3000`. Browser blocks requests unless CORS configured.

**File:** `src/main/java/com/penrose/bibby/config/WebConfig.java`

```java
package com.penrose.bibby.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // Apply to all /api endpoints
            .allowedOrigins(
                "http://localhost:3000",  // React dev server
                "http://localhost:5173",  // Vite dev server
                "https://bibby-ui.your-domain.com"  // Production frontend
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)  // Allow cookies/auth headers
            .maxAge(3600);  // Cache preflight response for 1 hour
    }
}
```

**Security Note:** In production, **never** use `.allowedOrigins("*")`. Always specify exact frontend domains.

---

## Part 8: Preparing for Cloud Deployment

### Externalize Configuration

**Current problem:** Database credentials hardcoded in `application.properties`.

**Solution:** Use environment variables.

**File:** `src/main/resources/application.properties`

```properties
# Database configuration (values from environment)
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/bibby}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:password}

# JPA settings
spring.jpa.hibernate.ddl-auto=${DDL_AUTO:validate}
spring.jpa.show-sql=${SHOW_SQL:false}
spring.jpa.properties.hibernate.format_sql=true

# Server configuration
server.port=${PORT:8080}

# API documentation
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

**Format:** `${ENV_VAR_NAME:default_value}`

**Usage:**
```bash
# Local development (uses defaults)
mvn spring-boot:run

# Production (uses environment variables)
export DATABASE_URL=jdbc:postgresql://prod-db:5432/bibby
export DATABASE_USERNAME=bibby_user
export DATABASE_PASSWORD=secure_password
export DDL_AUTO=validate
export SHOW_SQL=false
java -jar target/bibby-0.0.1-SNAPSHOT.jar
```

### Health Check Endpoint

Add Spring Boot Actuator for health monitoring (needed for cloud platforms):

**File:** `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**File:** `application.properties`

```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
```

**Access health endpoint:**
```bash
curl http://localhost:8080/actuator/health

# Response:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

Cloud platforms (AWS, Render, Railway) use `/actuator/health` to monitor application status.

---

## Action Items for Week 8-9

### Critical (Must Complete)

**1. Refactor BookController** (6-8 hours)
- Implement all 7 endpoints (create, get, list, search, update, delete, update status)
- Add `@RequestMapping` at class level
- Create `BookResponseDTO` and `BookSummaryDTO`
- Use `@PathVariable` for IDs, `@RequestParam` for filters
- Return proper HTTP status codes (201, 200, 404, 204)

**Deliverable:** Working CRUD API for books

**2. Implement Validation** (4-5 hours)
- Create `BookCreateRequestDTO` with JSR-303 annotations
- Create `BookUpdateRequestDTO` for partial updates
- Add `@Valid` to controller methods
- Add `@Validated` to controller class

**Deliverable:** API rejects invalid requests with 400 Bad Request

**3. Build Global Exception Handler** (3-4 hours)
- Create exception hierarchy (`ResourceNotFoundException`, `BusinessRuleViolationException`)
- Implement `GlobalExceptionHandler` with `@RestControllerAdvice`
- Return RFC 7807 Problem Detail responses
- Handle validation errors, not found errors, business rule violations

**Deliverable:** Structured error responses for all failure scenarios

**4. Add OpenAPI Documentation** (2-3 hours)
- Add SpringDoc dependency to `pom.xml`
- Create `OpenApiConfig` with API metadata
- Annotate controller methods with `@Operation`, `@ApiResponse`
- Test Swagger UI at `/swagger-ui.html`

**Deliverable:** Interactive API documentation

**5. Write Integration Tests** (6-8 hours)
- Create `BookControllerIntegrationTest` with 10+ test cases
- Test success scenarios (201, 200, 204)
- Test validation failures (400)
- Test not found scenarios (404)
- Test pagination

**Deliverable:** 80%+ test coverage for BookController, all tests passing

### Important (Should Complete)

**6. Implement Pagination** (2-3 hours)
- Use `Pageable` parameter in list endpoints
- Return `Page<BookSummaryDTO>` instead of `List`
- Add `@PageableDefault(size = 20)` annotation
- Test with `?page=0&size=10` query params

**7. Add CORS Configuration** (1 hour)
- Create `WebConfig` with CORS mappings
- Allow `localhost:3000` for frontend development

**8. Externalize Configuration** (1-2 hours)
- Use environment variables in `application.properties`
- Create `.env.example` file documenting required variables
- Test with different environment variable values

**9. Repeat for Other Controllers** (8-10 hours)
- Apply same patterns to `AuthorController`, `ShelfController`, `BookcaseController`
- Consistency across all endpoints

### Bonus (If Time Permits)

**10. Add Request Logging** (2 hours)
- Log all incoming requests with correlation IDs
- Use SLF4J with Logback
- Add request/response logging filter

**11. Rate Limiting** (3 hours)
- Use Bucket4j library for rate limiting
- Prevent API abuse (max 100 requests/minute per IP)

**12. API Versioning Strategy** (2 hours)
- Document versioning approach (`/api/v1/`, `/api/v2/`)
- Create ADR for versioning decisions

---

## Success Metrics for Week 8-9

By the end of this section, you should have:

✅ **Complete REST API for Books:**
- 7 endpoints (create, get, list, search, update, delete, update status)
- Proper HTTP verbs and status codes
- Structured request/response DTOs

✅ **Comprehensive Validation:**
- JSR-303 annotations on all request DTOs
- Global exception handler with structured error responses
- Validation errors return 400 with field-level details

✅ **API Documentation:**
- OpenAPI 3.0 specification auto-generated
- Swagger UI accessible at `/swagger-ui.html`
- All endpoints annotated with descriptions

✅ **Integration Tests:**
- 10+ test cases covering success and failure paths
- 80%+ code coverage
- All tests passing

✅ **Production-Ready Configuration:**
- Externalized configuration with environment variables
- Health check endpoint at `/actuator/health`
- CORS configured for frontend integration

---

## Industrial Connection: Why This Matters

### APIs as Operational Interfaces

Your REST API is to software what control panels are to industrial systems:

**SCADA System (Navy/Kinder Morgan):**
- Displays pipeline pressure, flow rates, valve states
- Accepts commands (open/close valve, start/stop pump)
- Shows alarms when parameters out of range
- Logs all operations for audit trail

**Bibby REST API:**
- Displays book inventory, status, locations (GET endpoints)
- Accepts commands (create book, update status, delete) (POST/PUT/DELETE)
- Returns error codes when invalid operations attempted (400, 409)
- Audit trail via `createdAt`/`updatedAt` timestamps

**Both require:**
- **Clear contracts:** What inputs are valid? What outputs to expect?
- **Error handling:** System must fail gracefully and explain why
- **Documentation:** New operators need manuals; new developers need API docs
- **Testing:** Can't deploy SCADA without testing; can't deploy API without integration tests

### Validation = Safety Interlocks

In operational systems, **safety interlocks** prevent dangerous operations:
- Can't start pump if upstream valve closed
- Can't exceed max pressure limits
- Can't run conflicting operations simultaneously

In REST APIs, **validation** serves the same role:
- Can't create book with blank title (data integrity)
- Can't transition book from CHECKED_OUT to ARCHIVED (business rule)
- Can't delete book with active checkouts (referential integrity)

Both prevent invalid states that corrupt the system.

---

## What's Next

**Section 09: Portfolio Project Architecture (Phase 2)**

We'll continue building out Bibby's architecture:
- Checkout/return workflow with business rules
- Database migrations with Flyway
- Caching with Redis
- Asynchronous processing with Spring Events
- Observability: logging, metrics, tracing

You're 25% through the curriculum (8/32 sections). The foundation is solid. Now we build the signature features that make Bibby stand out.

---

## Exercises

### Exercise 1: Refactor BookController
**Time:** 6-8 hours

Implement the complete refactored `BookController` with all 7 endpoints shown in Part 3.

**Steps:**
1. Create `BookResponseDTO` and `BookSummaryDTO`
2. Create `BookCreateRequestDTO` and `BookUpdateRequestDTO` with validation
3. Implement all controller methods
4. Update `BookService` to support new methods
5. Test manually with Postman or curl

**Deliverable:** Working endpoints for create, get, list, search, update, delete, update status

---

### Exercise 2: Build Global Exception Handler
**Time:** 3-4 hours

Implement the `GlobalExceptionHandler` from Part 4.

**Steps:**
1. Create exception hierarchy (`ResourceNotFoundException`, `BookNotFoundException`, `BusinessRuleViolationException`)
2. Create `GlobalExceptionHandler` with `@RestControllerAdvice`
3. Add handlers for validation, not found, business rules, generic errors
4. Update service methods to throw custom exceptions
5. Test each exception type with curl/Postman

**Deliverable:** Structured error responses for all failure types

---

### Exercise 3: Add OpenAPI Documentation
**Time:** 2-3 hours

Implement API documentation with SpringDoc.

**Steps:**
1. Add SpringDoc dependency to `pom.xml`
2. Create `OpenApiConfig` with metadata
3. Annotate `BookController` methods with `@Operation`, `@ApiResponse`, `@Tag`
4. Start application and access `/swagger-ui.html`
5. Test endpoints through Swagger UI

**Deliverable:** Interactive API documentation

---

### Exercise 4: Write Integration Tests
**Time:** 6-8 hours

Create comprehensive integration tests for `BookController`.

**Steps:**
1. Create `BookControllerIntegrationTest` class
2. Implement at least 10 test methods covering:
   - Successful create (201)
   - Validation failures (400)
   - Not found scenarios (404)
   - Successful get, list, update, delete
   - Pagination
3. Create `application-test.properties` with H2 database
4. Run tests: `mvn test`

**Deliverable:** 80%+ test coverage, all tests passing

---

### Exercise 5: Implement Author and Shelf APIs
**Time:** 8-10 hours

Apply the same patterns to `AuthorController` and `ShelfController`.

**For each controller:**
1. Create request/response DTOs
2. Implement CRUD endpoints
3. Add validation
4. Add OpenAPI annotations
5. Write integration tests

**Deliverable:** Consistent API patterns across all resources

---

### Exercise 6: Document API in README
**Time:** 2 hours

Update Bibby's README with API documentation.

**Include:**
1. How to run the API locally
2. List of all endpoints with examples
3. Link to Swagger UI
4. Example curl commands for each endpoint
5. Environment variable configuration

**Deliverable:** Updated `README.md` in repository

---

## Resources

**Spring Boot REST:**
- [Spring Boot REST API Tutorial](https://spring.io/guides/tutorials/rest/)
- [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

**Validation:**
- [JSR-303 Bean Validation](https://beanvalidation.org/2.0/spec/)
- [Spring Validation Guide](https://spring.io/guides/gs/validating-form-input/)

**Exception Handling:**
- [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807)
- [Spring @ControllerAdvice](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)

**OpenAPI:**
- [SpringDoc OpenAPI](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)

**Testing:**
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [MockMvc Reference](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-framework.html)

**HTTP Semantics:**
- [MDN HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
- [REST API Best Practices](https://restfulapi.net/)

---

**Progress Tracker:** 8/32 sections complete (25%)

**Next Section:** Portfolio Project Architecture (Phase 2) — Advanced workflows and cloud preparation
