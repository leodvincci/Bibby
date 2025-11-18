# Section 28: Validation

**Learning objective:** Master Bean Validation API (`@NotNull`, `@Size`, `@Pattern`), add validation to Bibby's DTOs and entities, handle validation errors properly, and create custom validators for business rules.

**Why this matters:** Your `BookRequestDTO` accepts `null` title, empty strings, and invalid data with ZERO validation. This allows corrupt data into your database, causes NullPointerExceptions at runtime, and provides terrible user experience. Validation catches bad data at the API boundary.

**Duration:** 60 min read

---

## The Critical Question

**Your current DTO:**

```java
// BookRequestDTO.java:3
public record BookRequestDTO(String title, String firstName, String lastName) {}
```

**Question:** What happens if a client sends this?

```json
{
  "title": "",
  "firstName": null,
  "lastName": "    "
}
```

**Answer:** Your service accepts it, tries to save a book with empty title, crashes with `NullPointerException` when accessing `firstName`, and saves an author with whitespace-only name.

**Without validation:** Garbage in, garbage out.

**With validation:** Request rejected instantly with clear error message.

This section reveals how to add validation to Bibby.

---

## What You'll Learn From Your Code

**Shocking discovery:**
- **ZERO validation annotations** in entire codebase
- **ZERO validation dependency** in pom.xml
- **No `@Valid` or `@Validated`** anywhere
- **No error handling** for invalid input
- **Current grade: F** (no validation at all)

**After this section:** **Grade: A** (complete validation coverage)

---

## Part 1: What is Bean Validation?

### Definition

**Bean Validation (JSR 303/380):** Java specification for declarative validation using annotations.

**Purpose:** Validate object state (fields) using annotations instead of manual if-checks.

---

### Without Validation (Your Current Code)

```java
// BookService.java:23-28 (CURRENT - no validation)
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    String firstName = bookRequestDTO.firstName();  // Could be null!
    String lastName = bookRequestDTO.lastName();    // Could be empty!
    String title = bookRequestDTO.title();          // Could be blank!

    // NO CHECKS! Just use the data...
    AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName, lastName);
    // 💥 NullPointerException if firstName is null!
}
```

**Problems:**
1. ❌ No null checks
2. ❌ No empty string checks
3. ❌ No length validation
4. ❌ No format validation
5. ❌ Crashes at runtime
6. ❌ Cryptic error messages

---

### With Validation (What You Should Have)

```java
// BookRequestDTO.java - WITH VALIDATION
public record CreateBookRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    String title,

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    String lastName
) {}

// BookController.java - TRIGGER VALIDATION
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@Valid @RequestBody CreateBookRequest request) {
    //                                 ^^^^^^ Triggers validation!
    bookService.createNewBook(request);  // Only called if validation passes
    return ResponseEntity.ok("Book added: " + request.title());
}
```

**Benefits:**
1. ✅ Validation automatic (no manual if-checks)
2. ✅ Clear error messages
3. ✅ Fails fast (at API boundary)
4. ✅ Self-documenting (annotations show constraints)
5. ✅ Reusable (same DTO validated everywhere)
6. ✅ Standard (Jakarta Bean Validation spec)

---

## Part 2: Adding Validation to Bibby

### Step 1: Add Dependency

**Your pom.xml currently has NO validation dependency.**

```xml
<!-- pom.xml - ADD THIS DEPENDENCY -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**What this includes:**
- **Hibernate Validator** (reference implementation of Bean Validation)
- **Jakarta Bean Validation API** (annotations like `@NotNull`, `@Size`)
- **Spring integration** (`@Valid`, `@Validated`, error handling)

**After adding:** Run `mvn clean install` to download.

---

### Step 2: Annotate Your DTOs

**Let's fix all your DTOs:**

#### CreateBookRequest (Renamed from BookRequestDTO)

```java
// CreateBookRequest.java - FIXED VERSION
package com.penrose.bibby.library.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBookRequest(

    @NotBlank(message = "Title is required and cannot be blank")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    String title,

    @NotBlank(message = "Author first name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    String firstName,

    @NotBlank(message = "Author last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    String lastName

) {}
```

**Annotations explained:**
- `@NotBlank` - String must not be null, empty, or whitespace-only
- `@Size(min, max)` - String length constraints
- `message` - Error message shown to user

---

#### BookcaseDTO (Should Split into Request/Response)

```java
// CreateBookcaseRequest.java - NEW (input DTO, no ID)
package com.penrose.bibby.library.bookcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateBookcaseRequest(

    @NotBlank(message = "Bookcase label is required")
    @Size(min = 1, max = 100, message = "Label must be between 1 and 100 characters")
    String bookcaseLabel,

    @Positive(message = "Shelf capacity must be positive")
    int shelfCapacity

) {}
```

**New annotations:**
- `@Positive` - Number must be > 0
- No ID field (ID is generated by database)

---

#### UpdateBookRequest (NEW - for updating books)

```java
// UpdateBookRequest.java - NEW
package com.penrose.bibby.library.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateBookRequest(

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255)
    String title,

    @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$", message = "Invalid ISBN format")
    String isbn,

    @Size(max = 255)
    String publisher,

    @Positive(message = "Publication year must be positive")
    Integer publicationYear,

    @Size(max = 100)
    String genre,

    @Positive
    Integer edition,

    @Size(max = 1000)
    String description

) {}
```

**New annotations:**
- `@Pattern(regexp)` - String must match regex (ISBN format)
- `Integer` instead of `int` - Nullable fields (optional)

---

### Step 3: Enable Validation in Controllers

**Add `@Valid` to trigger validation:**

```java
// BookController.java - ENABLE VALIDATION
@RestController
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService){
        this.bookService = bookService;
    }

    @PostMapping("api/v1/books")
    public ResponseEntity<String> addBook(@Valid @RequestBody CreateBookRequest request) {
        //                                 ^^^^^^ Triggers validation before method is called
        bookService.createNewBook(request);
        return ResponseEntity.ok("Book added: " + request.title());
    }

    @PutMapping("api/v1/books/{id}")
    public ResponseEntity<Void> updateBook(
        @PathVariable Long id,
        @Valid @RequestBody UpdateBookRequest request  // ← Validation here too
    ) {
        bookService.updateBook(id, request);
        return ResponseEntity.noContent().build();
    }
}
```

**What happens:**
1. Client sends JSON request
2. Spring deserializes JSON → DTO object
3. **Spring runs validation** (checks all constraints)
4. If validation fails → **400 Bad Request** with error details
5. If validation passes → Controller method is called

---

## Part 3: Bean Validation Annotations Reference

### String Constraints

| Annotation | Validates | Example |
|------------|-----------|---------|
| `@NotNull` | Field is not null | `@NotNull String title` |
| `@NotEmpty` | String/Collection is not null or empty | `@NotEmpty String title` |
| `@NotBlank` | String is not null, empty, or whitespace | `@NotBlank String title` (BEST) |
| `@Size(min, max)` | String/Collection length | `@Size(min=1, max=255) String title` |
| `@Pattern(regexp)` | String matches regex | `@Pattern(regexp="^[A-Z].*") String title` |
| `@Email` | Valid email format | `@Email String email` |

**Use `@NotBlank` for strings** (most restrictive - rejects null, "", and "   ").

---

### Number Constraints

| Annotation | Validates | Example |
|------------|-----------|---------|
| `@Min(value)` | Number >= value | `@Min(0) int year` |
| `@Max(value)` | Number <= value | `@Max(9999) int year` |
| `@Positive` | Number > 0 | `@Positive int count` |
| `@PositiveOrZero` | Number >= 0 | `@PositiveOrZero int count` |
| `@Negative` | Number < 0 | `@Negative int debt` |
| `@Digits(integer, fraction)` | Decimal precision | `@Digits(integer=5, fraction=2) BigDecimal price` |
| `@DecimalMin(value)` | Decimal >= value | `@DecimalMin("0.01") BigDecimal price` |

---

### Boolean & Other

| Annotation | Validates | Example |
|------------|-----------|---------|
| `@AssertTrue` | Boolean is true | `@AssertTrue boolean termsAccepted` |
| `@AssertFalse` | Boolean is false | `@AssertFalse boolean deleted` |
| `@Null` | Field is null | `@Null Long id` (for create requests) |
| `@NotNull` | Field is not null | `@NotNull LocalDate date` |
| `@Past` | Date is in past | `@Past LocalDate birthdate` |
| `@PastOrPresent` | Date is past or today | `@PastOrPresent LocalDate publishedDate` |
| `@Future` | Date is in future | `@Future LocalDate dueDate` |
| `@FutureOrPresent` | Date is future or today | `@FutureOrPresent LocalDate eventDate` |

---

### Nested & Collection

| Annotation | Validates | Example |
|------------|-----------|---------|
| `@Valid` | Validates nested object | `@Valid Address address` |
| `@Size(min, max)` | Collection size | `@Size(min=1) List<String> authors` |

---

## Part 4: Validation Error Handling

### Default Error Response (400 Bad Request)

**When validation fails, Spring returns:**

```json
{
  "timestamp": "2025-11-18T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='createBookRequest'. Error count: 2",
  "errors": [
    {
      "field": "title",
      "rejectedValue": "",
      "message": "Title is required and cannot be blank"
    },
    {
      "field": "firstName",
      "rejectedValue": null,
      "message": "Author first name is required"
    }
  ],
  "path": "/api/v1/books"
}
```

**This is GOOD** - client knows exactly what's wrong.

---

### Custom Error Response (Better UX)

**Create custom exception handler:**

```java
// GlobalExceptionHandler.java - NEW FILE
package com.penrose.bibby.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}

// ValidationErrorResponse.java - NEW FILE
package com.penrose.bibby.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ValidationErrorResponse(
    LocalDateTime timestamp,
    int status,
    String message,
    Map<String, String> errors
) {}
```

**Custom response:**
```json
{
  "timestamp": "2025-11-18T10:30:00",
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "title": "Title is required and cannot be blank",
    "firstName": "Author first name is required"
  }
}
```

**Cleaner, more user-friendly!**

---

## Part 5: Validation in Entities

### Should You Validate Entities?

**Debate:** Some say validate DTOs only, others say validate entities too.

**Best practice:** Validate BOTH.

- **DTO validation:** Protects API boundary (external input)
- **Entity validation:** Protects database integrity (internal bugs)

---

### Adding Validation to Entities

```java
// BookEntity.java - ADD VALIDATION
@Entity
@Table(name = "books")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)  // Database constraint
    private String title;

    @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$")
    @Column(unique = true)
    private String isbn;

    @Size(max = 255)
    private String publisher;

    @Positive
    private Integer publicationYear;

    @Size(max = 100)
    private String genre;

    @PositiveOrZero
    private Integer checkoutCount;

    // Relationships...
}
```

**Benefits:**
1. ✅ Database integrity (JPA validates before INSERT/UPDATE)
2. ✅ Double protection (DTO + Entity)
3. ✅ Catches bugs (if you bypass DTO validation in code)

**When JPA validates entities:**
- Before `entityManager.persist()` (INSERT)
- Before `entityManager.merge()` (UPDATE)
- **Only if** `validation-mode` is enabled (default in Spring Boot)

---

## Part 6: Custom Validators

### Problem: Business Rules Beyond Simple Constraints

**Example:** ISBN must be valid according to ISBN checksum algorithm.

**Simple validation:**
```java
@Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$")
String isbn;  // Checks format, NOT checksum
```

**Custom validator:** Checks format AND checksum.

---

### Creating a Custom Validator

**Step 1: Create Annotation**

```java
// ValidISBN.java - NEW FILE
package com.penrose.bibby.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ISBNValidator.class)
@Documented
public @interface ValidISBN {

    String message() default "Invalid ISBN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

---

**Step 2: Create Validator Logic**

```java
// ISBNValidator.java - NEW FILE
package com.penrose.bibby.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ISBNValidator implements ConstraintValidator<ValidISBN, String> {

    @Override
    public boolean isValid(String isbn, ConstraintValidatorContext context) {
        if (isbn == null || isbn.isBlank()) {
            return true;  // Let @NotBlank handle null/blank
        }

        // Remove hyphens and spaces
        isbn = isbn.replaceAll("[-\\s]", "");

        // Check length (ISBN-10 or ISBN-13)
        if (isbn.length() != 10 && isbn.length() != 13) {
            return false;
        }

        // Validate ISBN-13
        if (isbn.length() == 13) {
            return isValidISBN13(isbn);
        }

        // Validate ISBN-10
        return isValidISBN10(isbn);
    }

    private boolean isValidISBN13(String isbn) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(isbn.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == Character.getNumericValue(isbn.charAt(12));
    }

    private boolean isValidISBN10(String isbn) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(isbn.charAt(i)) * (10 - i);
        }
        char lastChar = isbn.charAt(9);
        int checkDigit = (lastChar == 'X') ? 10 : Character.getNumericValue(lastChar);
        sum += checkDigit;
        return sum % 11 == 0;
    }
}
```

---

**Step 3: Use Custom Validator**

```java
// UpdateBookRequest.java - USE CUSTOM VALIDATOR
public record UpdateBookRequest(
    @NotBlank String title,

    @ValidISBN(message = "Invalid ISBN checksum")  // ← Custom validator!
    String isbn,

    // ... other fields
) {}
```

**Now validation checks:**
1. ✅ Format (digits only)
2. ✅ Length (10 or 13 digits)
3. ✅ **Checksum** (validates algorithm)

---

## Part 7: Validation Groups (Advanced)

### Problem: Different Validation for Create vs Update

**Example:**
- **Create:** ID must be `null` (database generates it)
- **Update:** ID must be `@NotNull` (required to identify entity)

**Solution:** Validation groups.

---

### Using Validation Groups

**Step 1: Define Groups**

```java
// ValidationGroups.java - NEW FILE
package com.penrose.bibby.validation;

public class ValidationGroups {
    public interface Create {}
    public interface Update {}
}
```

---

**Step 2: Assign Constraints to Groups**

```java
// BookDTO.java - GROUPS EXAMPLE
public record BookDTO(

    @Null(groups = Create.class, message = "ID must be null for new books")
    @NotNull(groups = Update.class, message = "ID is required for updates")
    Long id,

    @NotBlank(groups = {Create.class, Update.class})
    String title,

    @ValidISBN(groups = {Create.class, Update.class})
    String isbn

) {}
```

---

**Step 3: Specify Group in Controller**

```java
// BookController.java - USE GROUPS
@PostMapping("api/v1/books")
public ResponseEntity<String> createBook(
    @Validated(Create.class) @RequestBody BookDTO dto  // ← Only Create.class constraints
) {
    bookService.createNewBook(dto);
    return ResponseEntity.ok("Created");
}

@PutMapping("api/v1/books/{id}")
public ResponseEntity<Void> updateBook(
    @PathVariable Long id,
    @Validated(Update.class) @RequestBody BookDTO dto  // ← Only Update.class constraints
) {
    bookService.updateBook(id, dto);
    return ResponseEntity.noContent().build();
}
```

**Result:**
- **Create:** ID must be null, title required, ISBN valid
- **Update:** ID required, title required, ISBN valid

---

## Part 8: Complete Validation Strategy for Bibby

### DTOs to Validate

**1. CreateBookRequest**

```java
public record CreateBookRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255)
    String title,

    @NotBlank(message = "Author first name is required")
    @Size(min = 1, max = 100)
    String firstName,

    @NotBlank(message = "Author last name is required")
    @Size(min = 1, max = 100)
    String lastName
) {}
```

---

**2. UpdateBookRequest**

```java
public record UpdateBookRequest(
    @NotBlank @Size(min = 1, max = 255)
    String title,

    @ValidISBN
    String isbn,

    @Size(max = 255)
    String publisher,

    @Positive
    Integer publicationYear,

    @Size(max = 100)
    String genre,

    @Positive
    Integer edition,

    @Size(max = 1000)
    String description
) {}
```

---

**3. CreateBookcaseRequest**

```java
public record CreateBookcaseRequest(
    @NotBlank @Size(min = 1, max = 100)
    String bookcaseLabel,

    @Positive(message = "Shelf capacity must be at least 1")
    int shelfCapacity
) {}
```

---

**4. CreateShelfRequest (NEW)**

```java
public record CreateShelfRequest(
    @NotBlank @Size(min = 1, max = 100)
    String shelfLabel,

    @NotNull(message = "Bookcase ID is required")
    @Positive
    Long bookcaseId,

    @PositiveOrZero(message = "Shelf position cannot be negative")
    int shelfPosition
) {}
```

---

**5. CreateAuthorRequest (NEW)**

```java
public record CreateAuthorRequest(
    @NotBlank @Size(min = 1, max = 100)
    String firstName,

    @NotBlank @Size(min = 1, max = 100)
    String lastName
) {}
```

---

### Entities to Validate

**Add validation to ALL entities:**

```java
// BookEntity.java
@NotBlank @Size(max = 255) private String title;
@ValidISBN private String isbn;
@Size(max = 255) private String publisher;
@Positive private Integer publicationYear;

// AuthorEntity.java
@NotBlank @Size(max = 100) private String firstName;
@NotBlank @Size(max = 100) private String lastName;

// ShelfEntity.java
@NotBlank @Size(max = 100) private String shelfLabel;
@PositiveOrZero private int shelfPosition;

// BookcaseEntity.java
@NotBlank @Size(max = 100) private String bookcaseLabel;
@Positive private int shelfCapacity;
```

---

## Part 9: Testing Validation

### Unit Test for Validation

```java
// CreateBookRequestTest.java - NEW FILE
package com.penrose.bibby.library.book;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateBookRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidation_whenAllFieldsValid() {
        // Given
        CreateBookRequest request = new CreateBookRequest(
            "1984",
            "George",
            "Orwell"
        );

        // When
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidation_whenTitleIsBlank() {
        // Given
        CreateBookRequest request = new CreateBookRequest(
            "   ",  // Blank title
            "George",
            "Orwell"
        );

        // When
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Title is required");
    }

    @Test
    void shouldFailValidation_whenFirstNameIsNull() {
        // Given
        CreateBookRequest request = new CreateBookRequest(
            "1984",
            null,  // Null firstName
            "Orwell"
        );

        // When
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
            .isEqualTo("firstName");
    }

    @Test
    void shouldFailValidation_whenTitleTooLong() {
        // Given
        String longTitle = "a".repeat(256);  // 256 characters (max is 255)
        CreateBookRequest request = new CreateBookRequest(
            longTitle,
            "George",
            "Orwell"
        );

        // When
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("255 characters");
    }
}
```

---

### Integration Test for Controller Validation

```java
// BookControllerValidationTest.java - NEW FILE
package com.penrose.bibby.library.book;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void shouldReturn400_whenTitleIsBlank() throws Exception {
        // Given
        String invalidJson = """
            {
                "title": "",
                "firstName": "George",
                "lastName": "Orwell"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void shouldReturn200_whenAllFieldsValid() throws Exception {
        // Given
        String validJson = """
            {
                "title": "1984",
                "firstName": "George",
                "lastName": "Orwell"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
            .andExpect(status().isOk());
    }
}
```

---

## Summary: Your Validation Situation - Before & After

### Before (Current State):

| Issue | Grade | Problem |
|-------|-------|---------|
| NO validation dependency | F | Can't use Bean Validation |
| ZERO validation annotations | F | No protection against bad data |
| No `@Valid` in controllers | F | Validation never triggered |
| No error handling | F | Cryptic error messages |
| Accept null/empty strings | F | Corrupt data in database |

**Overall Grade: F** (no validation at all)

---

### After (All Fixes Applied):

| Improvement | Grade | Benefit |
|-------------|-------|---------|
| spring-boot-starter-validation added | A | Bean Validation enabled |
| DTOs annotated (@NotBlank, @Size, etc.) | A | Input validated |
| Entities annotated (double protection) | A | Database integrity |
| @Valid in controllers | A | Validation automatic |
| Custom validators (ISBN checksum) | A | Business rules enforced |
| Global error handler | A | Clear error messages |
| Validation tests | A | Validation correctness proven |

**Overall Grade: A** (comprehensive validation coverage)

---

## Action Items

### Priority 1: Add Validation Dependency

```xml
<!-- pom.xml - ADD THIS -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Then run:** `mvn clean install`

---

### Priority 2: Annotate DTOs

**CreateBookRequest:**
```java
public record CreateBookRequest(
    @NotBlank @Size(min = 1, max = 255) String title,
    @NotBlank @Size(min = 1, max = 100) String firstName,
    @NotBlank @Size(min = 1, max = 100) String lastName
) {}
```

**CreateBookcaseRequest:**
```java
public record CreateBookcaseRequest(
    @NotBlank @Size(min = 1, max = 100) String bookcaseLabel,
    @Positive int shelfCapacity
) {}
```

---

### Priority 3: Enable Validation in Controllers

```java
// BookController.java - ADD @Valid
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@Valid @RequestBody CreateBookRequest request) {
    //                                 ^^^^^^ Add this!
    bookService.createNewBook(request);
    return ResponseEntity.ok("Book added: " + request.title());
}
```

---

### Priority 4: Add Custom Error Handler

```java
// Create GlobalExceptionHandler.java
// Create ValidationErrorResponse.java
// (Full code in Part 4)
```

---

### Priority 5: Add Validation to Entities

```java
// BookEntity.java - ADD VALIDATION
@NotBlank @Size(max = 255) private String title;
@ValidISBN private String isbn;
@Positive private Integer publicationYear;
```

---

### Priority 6: Create Custom Validators (Optional)

```java
// Create @ValidISBN annotation
// Create ISBNValidator class
// (Full code in Part 6)
```

---

### Priority 7: Write Validation Tests

```java
// CreateBookRequestTest.java
// BookControllerValidationTest.java
// (Full code in Part 9)
```

---

## What You Learned

1. **Bean Validation basics:**
   - Declarative validation with annotations
   - `@NotNull`, `@NotBlank`, `@Size`, `@Positive`, `@Pattern`, `@Email`
   - Triggers automatically with `@Valid`

2. **Bibby's critical gaps:**
   - NO validation dependency in pom.xml
   - ZERO validation annotations
   - Services accept garbage data
   - No error handling

3. **Where to validate:**
   - **DTOs** (API boundary, user input)
   - **Entities** (database integrity, internal bugs)
   - **Both** (defense in depth)

4. **Custom validators:**
   - Create annotation (`@ValidISBN`)
   - Implement `ConstraintValidator`
   - Use in DTOs/entities

5. **Validation groups:**
   - Different rules for Create vs Update
   - Use `@Validated(Create.class)` in controller

6. **Error handling:**
   - Default: Spring returns 400 with errors
   - Custom: `@RestControllerAdvice` + `@ExceptionHandler`
   - Clean JSON error response

---

**Next:** Section 29 - Transaction Management will explore `@Transactional`, propagation levels, rollback rules, and fixing Bibby's transaction boundaries.

**Your APIs currently accept garbage data.** These fixes transform Bibby from **Grade F to Grade A**, protecting your database, improving user experience, and preventing runtime crashes.

You're building production-grade software. Keep going!

---

*Section 28 complete. 28 of 33 sections finished (85%).*
*Next up: Section 29 - Transaction Management*
