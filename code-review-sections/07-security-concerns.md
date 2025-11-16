# Section 7: Security Concerns

## Overview
This section covers critical security vulnerabilities that could expose the application to attacks, data breaches, or unauthorized access. Security should be a top priority, especially for applications handling user data.

**Issues Found: 4**
- Critical: 2
- High: 2
- Medium: 0
- Low: 0

---

## Issue 7.1: Hardcoded Database Credentials in Version Control

**Priority:** 🔴 **CRITICAL**
**Effort:** 30 minutes
**Files:** `src/main/resources/application.properties`

### Current Code (WRONG ❌)

```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/library_db
spring.datasource.username=postgres
spring.datasource.password=mySecretPassword123
```

### Why This Is Problematic

1. **Credentials in Git History**: Database passwords are committed to version control and visible to anyone with repository access
2. **Hard to Rotate**: Changing passwords requires code changes and redeployment
3. **Environment-Specific**: Cannot use different credentials for dev/staging/prod
4. **Security Risk**: If repository is leaked or accessed by unauthorized users, attackers gain full database access

**Real-World Impact:**
- Attacker gains read/write access to all library data
- Can delete records, modify user information, or exfiltrate data
- Violates compliance standards (GDPR, SOC 2, PCI-DSS if handling payments)

### Correct Approach

Use **environment variables** or **external configuration files** (not in version control) to manage secrets.

### Fixed Code (RIGHT ✅)

**Step 1: Update application.properties**

```properties
# application.properties (committed to Git)
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/library_db}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# Optional: Show clear error if env vars are missing
spring.config.import=optional:file:.env[.properties]
```

**Step 2: Create .env file (NOT committed to Git)**

```properties
# .env (add to .gitignore!)
DB_URL=jdbc:postgresql://localhost:5432/library_db
DB_USERNAME=postgres
DB_PASSWORD=mySecretPassword123
```

**Step 3: Update .gitignore**

```gitignore
# .gitignore
.env
.env.local
.env.*.local
application-local.properties
```

**Step 4: Document in README**

```markdown
## Setup

1. Copy `.env.example` to `.env`:
   ```
   cp .env.example .env
   ```

2. Update `.env` with your database credentials:
   ```
   DB_URL=jdbc:postgresql://localhost:5432/library_db
   DB_USERNAME=your_username
   DB_PASSWORD=your_password
   ```
```

**Step 5: Create .env.example (safe to commit)**

```properties
# .env.example
DB_URL=jdbc:postgresql://localhost:5432/library_db
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
```

**For Production Deployment:**

```bash
# Set environment variables in your deployment platform
export DB_URL=jdbc:postgresql://prod-db.example.com:5432/library_prod
export DB_USERNAME=prod_user
export DB_PASSWORD=super_secure_password_from_vault

# Or use cloud-native secret management
# AWS: Secrets Manager or Systems Manager Parameter Store
# Azure: Key Vault
# GCP: Secret Manager
# Kubernetes: Secrets
```

### Learning Principle

**Never commit secrets to version control.** Use environment variables, secret management services, or external configuration files that are explicitly excluded from Git. This follows the **Twelve-Factor App** methodology's principle of storing config in the environment.

---

## Issue 7.2: No Input Validation on DTOs

**Priority:** 🔴 **CRITICAL**
**Effort:** 2 hours
**Files:** `BookRequestDTO.java`, `CheckoutRequestDTO.java`, and others

### Current Code (WRONG ❌)

```java
public record BookRequestDTO(
    String bookTitle,
    String authorFirstName,
    String authorLastName
) {}

// In BookCommands.java:
public void createBook() {
    System.out.print("Enter book title: ");
    String title = scanner.nextLine();
    System.out.print("Enter author first name: ");
    String firstName = scanner.nextLine();
    System.out.print("Enter author last name: ");
    String lastName = scanner.nextLine();

    // No validation - accepts empty strings, SQL injection attempts, XSS payloads
    BookRequestDTO request = new BookRequestDTO(title, firstName, lastName);
    bookService.createNewBook(request);
}
```

**Attack Scenarios:**

1. **Empty/Null Input:**
   ```
   Title: [press enter]
   First Name: [press enter]
   Last Name: [press enter]
   → Creates book with blank fields
   ```

2. **Malicious Input:**
   ```
   Title: <script>alert('XSS')</script>
   First Name: '; DROP TABLE books; --
   Last Name: ../../../../etc/passwd
   ```

### Why This Is Problematic

1. **Data Integrity**: Allows invalid data into the database (empty strings, whitespace-only)
2. **SQL Injection**: Malicious input could exploit database queries
3. **XSS Attacks**: If displayed in a web UI, could execute malicious scripts
4. **Business Logic Errors**: Causes NPEs or unexpected behavior downstream
5. **No Length Limits**: Could store gigabytes of text in fields designed for names

### Correct Approach

Use **Jakarta Bean Validation** (JSR-380) annotations on DTOs to enforce validation rules declaratively.

### Fixed Code (RIGHT ✅)

**Step 1: Add dependency**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Step 2: Annotate DTOs**

```java
import jakarta.validation.constraints.*;

public record BookRequestDTO(

    @NotBlank(message = "Book title is required")
    @Size(min = 1, max = 255, message = "Book title must be between 1 and 255 characters")
    String bookTitle,

    @NotBlank(message = "Author first name is required")
    @Size(min = 1, max = 100, message = "Author first name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Author first name contains invalid characters")
    String authorFirstName,

    @NotBlank(message = "Author last name is required")
    @Size(min = 1, max = 100, message = "Author last name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Author last name contains invalid characters")
    String authorLastName

) {}
```

**Step 3: Validate in Service Layer**

```java
import jakarta.validation.*;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final Validator validator;  // Injected by Spring

    public BookEntity createNewBook(BookRequestDTO request) {
        // Validate the DTO
        Set<ConstraintViolation<BookRequestDTO>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + errors);
        }

        // Proceed with sanitized input
        String sanitizedTitle = sanitizeInput(request.bookTitle());
        String sanitizedFirstName = sanitizeInput(request.authorFirstName());
        String sanitizedLastName = sanitizeInput(request.authorLastName());

        // Rest of creation logic...
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;

        // Remove leading/trailing whitespace
        String sanitized = input.trim();

        // Remove any HTML/script tags (defense in depth)
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        // Remove SQL-like patterns (defense in depth - use parameterized queries as primary defense)
        sanitized = sanitized.replaceAll("([';\"\\-\\-]|/\\*|\\*/)", "");

        return sanitized;
    }
}
```

**Step 4: Create Validation Utility for Console Input**

```java
public class InputValidationHelper {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public <T> T validateAndGet(T dto, Class<T> clazz) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            System.err.println("❌ Validation Errors:");
            violations.forEach(v ->
                System.err.println("  - " + v.getPropertyPath() + ": " + v.getMessage())
            );
            throw new IllegalArgumentException("Invalid input");
        }

        return dto;
    }
}
```

**Step 5: Update Commands**

```java
public class BookCommands {

    private final InputValidationHelper validationHelper = new InputValidationHelper();

    public void createBook() {
        try {
            System.out.print("Enter book title: ");
            String title = scanner.nextLine();

            System.out.print("Enter author first name: ");
            String firstName = scanner.nextLine();

            System.out.print("Enter author last name: ");
            String lastName = scanner.nextLine();

            // Create and validate DTO
            BookRequestDTO request = new BookRequestDTO(title, firstName, lastName);
            validationHelper.validateAndGet(request, BookRequestDTO.class);

            // If validation passes, create the book
            BookEntity book = bookService.createNewBook(request);
            System.out.println("✅ Book created successfully: " + book.getBookTitle());

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Failed to create book: " + e.getMessage());
        }
    }
}
```

**Additional Validation Examples:**

```java
// For checkout requests
public record CheckoutRequestDTO(

    @NotNull(message = "Book ID is required")
    @Positive(message = "Book ID must be positive")
    Long bookId,

    @NotBlank(message = "Member name is required")
    @Size(min = 2, max = 100, message = "Member name must be between 2 and 100 characters")
    String memberName,

    @Email(message = "Invalid email format")
    String memberEmail,

    @Future(message = "Due date must be in the future")
    LocalDate dueDate

) {}
```

### Learning Principle

**Never trust user input.** Always validate, sanitize, and use parameterized queries. This is the foundation of secure coding. Follow the principle of **defense in depth** - validate at multiple layers (DTO, service, database constraints).

---

## Issue 7.3: No Authentication or Authorization

**Priority:** 🟠 **HIGH**
**Effort:** 8 hours
**Files:** All controller classes (when REST endpoints are added)

### Current Code (WRONG ❌)

```java
// Future REST API (currently no controllers, but planning ahead)
@RestController
@RequestMapping("/api/books")
public class BookController {

    @GetMapping
    public List<BookEntity> getAllBooks() {
        // Anyone can access - no authentication required
        return bookService.findAllBooks();
    }

    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable Long id) {
        // Anyone can delete books - no authorization check
        bookRepository.deleteById(id);
    }
}
```

### Why This Is Problematic

1. **No Access Control**: Anyone can view, modify, or delete data
2. **No Audit Trail**: Cannot track who performed what actions
3. **Data Breach Risk**: Sensitive library data exposed to unauthorized users
4. **Compliance Violations**: GDPR, HIPAA, and other regulations require access controls

**Attack Scenario:**
```bash
# Attacker can:
curl http://localhost:8080/api/books           # View all books
curl -X DELETE http://localhost:8080/api/books/1   # Delete any book
curl -X POST http://localhost:8080/api/books -d '...'  # Create fake records
```

### Correct Approach

Implement **Spring Security** with role-based access control (RBAC).

### Fixed Code (RIGHT ✅)

**Step 1: Add Spring Security Dependency**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Step 2: Create Security Configuration**

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @Secured
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/login", "/error").permitAll()

                // Read-only access for authenticated users
                .requestMatchers("/api/books/**").hasAnyRole("USER", "LIBRARIAN", "ADMIN")
                .requestMatchers("/api/authors/**").hasAnyRole("USER", "LIBRARIAN", "ADMIN")

                // Modify operations require LIBRARIAN role
                .requestMatchers("/api/books/checkout").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers("/api/books/return").hasAnyRole("LIBRARIAN", "ADMIN")

                // Delete operations require ADMIN role
                .requestMatchers("/api/books/delete/**").hasRole("ADMIN")
                .requestMatchers("/api/authors/delete/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")  // Configure appropriately for REST APIs
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Step 3: Create User Entity and Service**

```java
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;  // BCrypt hashed

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    // Getters and setters
}

public enum Role {
    ROLE_USER,       // Can view books
    ROLE_LIBRARIAN,  // Can checkout/return books
    ROLE_ADMIN       // Full access including delete
}
```

**Step 4: Implement UserDetailsService**

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .roles(user.getRole().name().replace("ROLE_", ""))
            .disabled(!user.isEnabled())
            .build();
    }
}
```

**Step 5: Protect Controller Endpoints**

```java
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'LIBRARIAN', 'ADMIN')")
    public List<BookEntity> getAllBooks() {
        return bookService.findAllBooks();
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<String> checkoutBook(@RequestBody @Valid CheckoutRequestDTO request) {
        bookService.checkoutBook(request);
        return ResponseEntity.ok("Book checked out successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-checkouts")
    @PreAuthorize("isAuthenticated()")
    public List<BookEntity> getMyCheckouts(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return bookService.findBooksByMember(username);
    }
}
```

**Step 6: Add Audit Logging**

```java
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Around("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object auditDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        String username = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();

        String method = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("🔒 AUDIT: User '{}' is deleting via {} with args: {}",
            username, method, Arrays.toString(args));

        Object result = joinPoint.proceed();

        log.info("✅ AUDIT: Delete operation completed by '{}'", username);

        return result;
    }
}
```

### Learning Principle

**Implement authentication and authorization from the start.** Use Spring Security's battle-tested mechanisms rather than rolling your own. Follow the principle of **least privilege** - grant only the minimum permissions needed for each role.

---

## Issue 7.4: Potential SQL Injection with Native Queries

**Priority:** 🟠 **HIGH**
**Effort:** 1 hour
**Files:** Custom repository implementations

### Current Code (WRONG ❌)

```java
// If you were to add native queries in the future:
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    // DANGEROUS - String concatenation in native query
    @Query(value = "SELECT * FROM books WHERE book_title LIKE '%" +
                   "?1" + "%'", nativeQuery = true)
    List<BookEntity> searchByTitle(String title);
}

// Or in custom implementation:
public List<BookEntity> searchBooks(String searchTerm) {
    String sql = "SELECT * FROM books WHERE book_title LIKE '%" + searchTerm + "%'";
    return entityManager.createNativeQuery(sql, BookEntity.class).getResultList();
}
```

**Attack Scenario:**
```java
String maliciousInput = "'; DROP TABLE books; --";
bookRepository.searchByTitle(maliciousInput);

// Resulting SQL:
// SELECT * FROM books WHERE book_title LIKE '%'; DROP TABLE books; --%'
// → First query returns nothing, second query DELETES THE TABLE!
```

### Why This Is Problematic

1. **SQL Injection**: Attacker can execute arbitrary SQL commands
2. **Data Loss**: Can delete or modify database records
3. **Data Theft**: Can extract sensitive information
4. **Authentication Bypass**: Can bypass login checks
5. **Privilege Escalation**: Can modify user roles/permissions

### Correct Approach

**Always use parameterized queries or JPA's built-in query methods.**

### Fixed Code (RIGHT ✅)

**Option 1: Use JPA Query Methods (Safest)**

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    // JPA automatically creates parameterized query
    List<BookEntity> findByBookTitleContainingIgnoreCase(String title);

    // Multiple conditions
    List<BookEntity> findByBookTitleContainingOrAuthorLastNameContaining(
        String title, String authorName
    );
}
```

**Option 2: Use @Query with Named Parameters**

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    // JPQL with named parameter (safe)
    @Query("SELECT b FROM BookEntity b WHERE LOWER(b.bookTitle) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<BookEntity> searchByTitle(@Param("searchTerm") String searchTerm);

    // Native query with named parameter (safe)
    @Query(value = "SELECT * FROM books WHERE LOWER(book_title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))",
           nativeQuery = true)
    List<BookEntity> searchByTitleNative(@Param("searchTerm") String searchTerm);
}
```

**Option 3: Use EntityManager with Parameters**

```java
@Repository
@RequiredArgsConstructor
public class CustomBookRepositoryImpl implements CustomBookRepository {

    private final EntityManager entityManager;

    public List<BookEntity> searchBooks(String searchTerm) {
        String sql = """
            SELECT * FROM books
            WHERE LOWER(book_title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            """;

        return entityManager.createNativeQuery(sql, BookEntity.class)
            .setParameter("searchTerm", searchTerm)  // Safe - parameterized
            .getResultList();
    }

    public List<BookEntity> complexSearch(String title, String author, String status) {
        // Use CriteriaBuilder for dynamic queries (type-safe)
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BookEntity> query = cb.createQuery(BookEntity.class);
        Root<BookEntity> book = query.from(BookEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            predicates.add(cb.like(
                cb.lower(book.get("bookTitle")),
                "%" + title.toLowerCase() + "%"
            ));
        }

        if (author != null && !author.isBlank()) {
            predicates.add(cb.like(
                cb.lower(book.get("author").get("lastName")),
                "%" + author.toLowerCase() + "%"
            ));
        }

        if (status != null) {
            predicates.add(cb.equal(book.get("bookStatus"), BookStatus.valueOf(status)));
        }

        query.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(query).getResultList();
    }
}
```

**Defense in Depth: Input Sanitization**

```java
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public List<BookEntity> searchBooks(String searchTerm) {
        // Validate input before passing to repository
        if (searchTerm == null || searchTerm.isBlank()) {
            return Collections.emptyList();
        }

        // Sanitize: remove SQL-like characters (defense in depth)
        String sanitized = searchTerm
            .replaceAll("[';\"\\-\\-]", "")  // Remove quotes, semicolons, comment markers
            .trim();

        if (sanitized.length() < 2) {
            throw new IllegalArgumentException("Search term must be at least 2 characters");
        }

        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Search term too long (max 100 characters)");
        }

        // Even though we sanitize, ALWAYS use parameterized queries
        return bookRepository.findByBookTitleContainingIgnoreCase(sanitized);
    }
}
```

### Learning Principle

**SQL Injection is the #1 web application vulnerability** (OWASP Top 10). NEVER concatenate user input into SQL queries. Always use parameterized queries, prepared statements, or ORM query builders. This is non-negotiable for production code.

---

## Summary

| Issue | Priority | Effort | Impact | Fix |
|-------|----------|--------|--------|-----|
| Hardcoded credentials in Git | 🔴 Critical | 30 min | Prevents unauthorized database access | Use environment variables + .env files |
| No input validation on DTOs | 🔴 Critical | 2 hours | Prevents SQL injection, XSS, data corruption | Add Jakarta Bean Validation annotations |
| No authentication/authorization | 🟠 High | 8 hours | Controls who can access/modify data | Implement Spring Security with RBAC |
| SQL injection risk | 🟠 High | 1 hour | Prevents malicious database queries | Use parameterized queries exclusively |

**Total Estimated Effort:** ~12 hours

**Expected Impact After Fixes:**
- ✅ Zero exposed credentials in version control
- ✅ All user input validated and sanitized
- ✅ Role-based access control protecting sensitive operations
- ✅ Zero SQL injection vulnerabilities
- ✅ Compliance with security best practices (OWASP Top 10)
- ✅ Audit trail of all operations

**Security Checklist:**
- [ ] Move all credentials to environment variables
- [ ] Add .env to .gitignore
- [ ] Add validation annotations to all DTOs
- [ ] Implement Spring Security
- [ ] Create user roles (USER, LIBRARIAN, ADMIN)
- [ ] Protect all controller endpoints with @PreAuthorize
- [ ] Review all queries to ensure parameterization
- [ ] Add security audit logging
- [ ] Enable HTTPS in production
- [ ] Implement rate limiting to prevent brute force attacks

---

**Next Steps:** Proceed to Section 8 (Modern Spring Boot Practices) or Section 10 (Key Takeaways).
