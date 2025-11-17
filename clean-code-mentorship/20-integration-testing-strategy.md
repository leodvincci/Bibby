# Section 20: Integration Testing Strategy
## Clean Code + Spring Framework Mentorship

**Focus:** Unit testing, integration testing, test data builders, and test coverage

**Estimated Time:** 3-4 hours to read and understand; 12-16 hours to implement comprehensive tests

---

## Overview

Your current test suite is **essentially non-existent**:
- 1 smoke test (context loads)
- 1 empty test stub

For a codebase with 42 Java files, this is **a disaster waiting to happen**. Every refactoring is dangerous. Every feature addition risks breaking existing functionality.

This section will teach you how to build a **comprehensive, maintainable test suite** for your Spring Boot application.

---

## Your Current Testing Situation

### What Exists

**BibbyApplicationTests.java:**
```java
@SpringBootTest
class BibbyApplicationTests {
    @Test
    void contextLoads() {  // ✅ This works but tests almost nothing
    }
}
```

**BookCommandsTest.java:**
```java
class BookCommandsTest {
    @Test
    public void searchByTitleTest(){
        BookEntity bookEntity = null;  // ❌ Empty test!
    }
}
```

**Test Coverage:** ~0%

**Problems:**
1. No repository tests
2. No service tests
3. No controller tests
4. No validation that business logic works
5. Can't refactor safely
6. Can't prevent regressions

---

## The Testing Pyramid

```
           ╱╲
          ╱  ╲
         ╱ E2E ╲        <- Few: Test complete user flows
        ╱────────╲
       ╱          ╲
      ╱ Integration╲    <- Some: Test layers together
     ╱──────────────╲
    ╱                ╲
   ╱   Unit Tests     ╲  <- Many: Test individual components
  ╱────────────────────╲
```

**Distribution for your project:**
- **70%**: Unit tests (services, mappers, utilities)
- **25%**: Integration tests (repositories, controllers)
- **5%**: End-to-end tests (full application flows)

---

## Unit Testing

### Testing Services (Business Logic)

**BookServiceTest.java:**
```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.exception.ResourceNotFoundException;
import com.penrose.bibby.exception.DuplicateBookException;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    private AuthorEntity testAuthor;
    private BookEntity testBook;

    @BeforeEach
    void setUp() {
        testAuthor = new AuthorEntity("Robert", "Martin");
        testAuthor.setAuthorId(1L);

        testBook = new BookEntity("Clean Code", "978-0132350884");
        testBook.setBookId(1L);
        testBook.addAuthor(testAuthor);
    }

    @Test
    void createNewBook_whenAuthorExists_shouldReuseAuthor() {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "Clean Code",
            "Robert",
            "Martin",
            "978-0132350884"
        );

        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(Optional.of(testAuthor));
        when(bookRepository.findByTitleAndAuthor(anyString(), any()))
            .thenReturn(Optional.empty());
        when(bookRepository.save(any(BookEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BookEntity result = bookService.createNewBook(request);

        // Then
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthors()).contains(testAuthor);

        verify(authorRepository, never()).save(any());  // Didn't create new author
        verify(bookRepository).save(any(BookEntity.class));
    }

    @Test
    void createNewBook_whenAuthorDoesNotExist_shouldCreateAuthor() {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "Clean Code",
            "Robert",
            "Martin",
            "978-0132350884"
        );

        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(Optional.empty());
        when(authorRepository.save(any(AuthorEntity.class)))
            .thenReturn(testAuthor);
        when(bookRepository.findByTitleAndAuthor(anyString(), any()))
            .thenReturn(Optional.empty());
        when(bookRepository.save(any(BookEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BookEntity result = bookService.createNewBook(request);

        // Then
        verify(authorRepository).save(any(AuthorEntity.class));  // Created new author
        verify(bookRepository).save(any(BookEntity.class));
    }

    @Test
    void createNewBook_whenBookAlreadyExists_shouldThrowException() {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "Clean Code",
            "Robert",
            "Martin",
            "978-0132350884"
        );

        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(Optional.of(testAuthor));
        when(bookRepository.findByTitleAndAuthor("Clean Code", testAuthor))
            .thenReturn(Optional.of(testBook));

        // When / Then
        assertThatThrownBy(() -> bookService.createNewBook(request))
            .isInstanceOf(DuplicateBookException.class)
            .hasMessageContaining("already exists");

        verify(bookRepository, never()).save(any());  // Didn't save duplicate
    }

    @Test
    void findById_whenBookExists_shouldReturnBook() {
        // Given
        when(bookRepository.findById(1L))
            .thenReturn(Optional.of(testBook));

        // When
        BookEntity result = bookService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void findById_whenBookDoesNotExist_shouldThrowException() {
        // Given
        when(bookRepository.findById(999L))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> bookService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book not found");
    }

    @Test
    void checkOutBook_whenBookIsAvailable_shouldCheckOut() {
        // Given
        testBook.setStatus(BookStatus.AVAILABLE);
        testBook.setCheckoutCount(0);

        when(bookRepository.findById(1L))
            .thenReturn(Optional.of(testBook));

        // When
        bookService.checkOutBook(1L);

        // Then
        assertThat(testBook.getStatus()).isEqualTo(BookStatus.CHECKED_OUT);
        assertThat(testBook.getCheckoutCount()).isEqualTo(1);
    }

    @Test
    void checkOutBook_whenBookAlreadyCheckedOut_shouldThrowException() {
        // Given
        testBook.setStatus(BookStatus.CHECKED_OUT);

        when(bookRepository.findById(1L))
            .thenReturn(Optional.of(testBook));

        // When / Then
        assertThatThrownBy(() -> bookService.checkOutBook(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("already checked out");
    }
}
```

**Key Patterns:**
1. ✅ `@ExtendWith(MockitoExtension.class)` - Enables Mockito
2. ✅ `@Mock` - Creates mock dependencies
3. ✅ `@InjectMocks` - Injects mocks into service
4. ✅ `@BeforeEach` - Sets up test data
5. ✅ `when(...).thenReturn(...)` - Stubbing
6. ✅ `verify(...)` - Verifying interactions
7. ✅ `assertThat(...)` - AssertJ assertions (more readable)

---

## Integration Testing

### Testing Repositories (Data Access Layer)

**BookRepositoryTest.java:**
```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    private AuthorEntity robertMartin;
    private AuthorEntity martinFowler;

    @BeforeEach
    void setUp() {
        robertMartin = new AuthorEntity("Robert", "Martin");
        martinFowler = new AuthorEntity("Martin", "Fowler");

        entityManager.persist(robertMartin);
        entityManager.persist(martinFowler);
        entityManager.flush();
    }

    @Test
    void findByIsbn_whenExists_shouldReturnBook() {
        // Given
        BookEntity book = new BookEntity("Clean Code", "978-0132350884");
        book.addAuthor(robertMartin);
        entityManager.persist(book);
        entityManager.flush();

        // When
        Optional<BookEntity> found = bookRepository.findByIsbn("978-0132350884");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Code");
        assertThat(found.get().getAuthors()).contains(robertMartin);
    }

    @Test
    void findByIsbn_whenNotExists_shouldReturnEmpty() {
        // When
        Optional<BookEntity> found = bookRepository.findByIsbn("999-9999999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByTitleIgnoreCase_shouldBeCaseInsensitive() {
        // Given
        BookEntity book = new BookEntity("Clean Code", "978-0132350884");
        book.addAuthor(robertMartin);
        entityManager.persist(book);
        entityManager.flush();

        // When
        Optional<BookEntity> found = bookRepository.findByTitleIgnoreCase("CLEAN CODE");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void findByTitleContainingIgnoreCase_shouldReturnMatchingBooks() {
        // Given
        BookEntity cleanCode = new BookEntity("Clean Code", "978-0132350884");
        cleanCode.addAuthor(robertMartin);

        BookEntity cleanArchitecture = new BookEntity("Clean Architecture", "978-0134494166");
        cleanArchitecture.addAuthor(robertMartin);

        BookEntity refactoring = new BookEntity("Refactoring", "978-0201485677");
        refactoring.addAuthor(martinFowler);

        entityManager.persist(cleanCode);
        entityManager.persist(cleanArchitecture);
        entityManager.persist(refactoring);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<BookEntity> results = bookRepository.findByTitleContainingIgnoreCase("clean", pageable);

        // Then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
            .extracting(BookEntity::getTitle)
            .containsExactlyInAnyOrder("Clean Code", "Clean Architecture");
    }

    @Test
    void findByStatus_shouldReturnOnlyMatchingStatus() {
        // Given
        BookEntity available = new BookEntity("Clean Code", "978-0132350884");
        available.setStatus(BookStatus.AVAILABLE);
        available.addAuthor(robertMartin);

        BookEntity checkedOut = new BookEntity("Clean Architecture", "978-0134494166");
        checkedOut.setStatus(BookStatus.CHECKED_OUT);
        checkedOut.addAuthor(robertMartin);

        entityManager.persist(available);
        entityManager.persist(checkedOut);
        entityManager.flush();

        // When
        List<BookEntity> availableBooks = bookRepository.findByStatus(BookStatus.AVAILABLE);

        // Then
        assertThat(availableBooks).hasSize(1);
        assertThat(availableBooks.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void save_shouldPersistBook() {
        // Given
        BookEntity book = new BookEntity("Clean Code", "978-0132350884");
        book.addAuthor(robertMartin);
        book.setStatus(BookStatus.AVAILABLE);
        book.setCheckoutCount(0);

        // When
        BookEntity saved = bookRepository.save(book);
        entityManager.flush();

        // Then
        assertThat(saved.getBookId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();  // Auditing works
        assertThat(saved.getUpdatedAt()).isNotNull();

        // Verify persisted
        BookEntity found = entityManager.find(BookEntity.class, saved.getBookId());
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void save_withDuplicateIsbn_shouldThrowException() {
        // Given
        BookEntity book1 = new BookEntity("Clean Code", "978-0132350884");
        book1.addAuthor(robertMartin);
        entityManager.persist(book1);
        entityManager.flush();

        BookEntity book2 = new BookEntity("Different Title", "978-0132350884");  // Same ISBN
        book2.addAuthor(martinFowler);

        // When / Then
        assertThatThrownBy(() -> {
            bookRepository.save(book2);
            entityManager.flush();
        }).hasMessageContaining("unique");  // Unique constraint violation
    }
}
```

**Key Annotations:**
- `@DataJpaTest` - Configures in-memory database (H2), scans entities, configures JPA
- `@Autowired TestEntityManager` - JPA test utilities
- Tests run in transactions and rollback automatically

**Add H2 for testing:**
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Controller Testing (REST API)

**BookControllerTest.java:**
```java
package com.penrose.bibby.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penrose.bibby.exception.ResourceNotFoundException;
import com.penrose.bibby.library.book.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @Test
    void createBook_withValidData_shouldReturn201() throws Exception {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "Clean Code",
            "Robert",
            "Martin",
            "978-0132350884"
        );

        BookEntity savedBook = new BookEntity("Clean Code", "978-0132350884");
        savedBook.setBookId(1L);

        when(bookService.createBook(any(BookRequestDTO.class)))
            .thenReturn(savedBook);

        // When / Then
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/v1/books/1")))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Clean Code"))
            .andExpect(jsonPath("$.isbn").value("978-0132350884"));

        verify(bookService).createBook(any(BookRequestDTO.class));
    }

    @Test
    void createBook_withBlankTitle_shouldReturn400() throws Exception {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "",  // Blank title
            "Robert",
            "Martin",
            "978-0132350884"
        );

        // When / Then
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").exists())
            .andExpect(jsonPath("$.fieldErrors.title").value(containsString("required")));

        verify(bookService, never()).createBook(any());
    }

    @Test
    void getBookById_whenExists_shouldReturn200() throws Exception {
        // Given
        BookEntity book = new BookEntity("Clean Code", "978-0132350884");
        book.setBookId(1L);

        when(bookService.findById(1L)).thenReturn(book);

        // When / Then
        mockMvc.perform(get("/api/v1/books/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void getBookById_whenNotExists_shouldReturn404() throws Exception {
        // Given
        when(bookService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Book not found with id: 999"));

        // When / Then
        mockMvc.perform(get("/api/v1/books/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(containsString("Book not found")));
    }

    @Test
    void getAllBooks_withPagination_shouldReturn200() throws Exception {
        // Given
        BookEntity book1 = new BookEntity("Clean Code", "978-0132350884");
        book1.setBookId(1L);

        BookEntity book2 = new BookEntity("Clean Architecture", "978-0134494166");
        book2.setBookId(2L);

        Page<BookEntity> booksPage = new PageImpl<>(
            List.of(book1, book2),
            PageRequest.of(0, 20),
            2
        );

        when(bookService.findAll(any(PageRequest.class)))
            .thenReturn(booksPage);

        // When / Then
        mockMvc.perform(get("/api/v1/books")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void deleteBook_whenExists_shouldReturn204() throws Exception {
        // Given
        doNothing().when(bookService).deleteBook(1L);

        // When / Then
        mockMvc.perform(delete("/api/v1/books/1"))
            .andExpect(status().isNoContent());

        verify(bookService).deleteBook(1L);
    }

    @Test
    void updateBook_withValidData_shouldReturn200() throws Exception {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "Clean Code (Updated)",
            "Robert",
            "Martin",
            "978-0132350884"
        );

        BookEntity updatedBook = new BookEntity("Clean Code (Updated)", "978-0132350884");
        updatedBook.setBookId(1L);

        when(bookService.updateBook(eq(1L), any(BookRequestDTO.class)))
            .thenReturn(updatedBook);

        // When / Then
        mockMvc.perform(put("/api/v1/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Clean Code (Updated)"));
    }
}
```

**Key Annotations:**
- `@WebMvcTest(BookController.class)` - Only loads web layer
- `@MockBean` - Mocks Spring beans
- `mockMvc.perform(...)` - Simulates HTTP requests

---

## Testing Shell Commands

**BookManagementCommandsTest.java:**
```java
package com.penrose.bibby.cli.commands;

import com.penrose.bibby.library.book.*;
import com.penrose.bibby.cli.ui.ShellUIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookManagementCommandsTest {

    @Mock
    private BookFacade bookFacade;

    @Mock
    private ShellUIService ui;

    @InjectMocks
    private BookManagementCommands commands;

    @Test
    void addBook_withValidInput_shouldCreateBook() {
        // Given
        when(ui.promptForString("title", "Enter book title:"))
            .thenReturn("Clean Code");
        when(ui.promptForString("firstName", "Author first name:"))
            .thenReturn("Robert");
        when(ui.promptForString("lastName", "Author last name:"))
            .thenReturn("Martin");
        when(ui.promptForString("isbn", "ISBN:"))
            .thenReturn("978-0132350884");

        BookEntity savedBook = new BookEntity("Clean Code", "978-0132350884");
        when(bookFacade.createBook(any(BookRequestDTO.class)))
            .thenReturn(savedBook);

        // When
        commands.addBook();

        // Then
        verify(bookFacade).createBook(any(BookRequestDTO.class));
        verify(ui).printSuccess(anyString());
    }

    @Test
    void searchBooks_shouldDisplayResults() {
        // Given
        when(ui.promptForString("title", "Search for book:"))
            .thenReturn("Clean");

        BookEntity book1 = new BookEntity("Clean Code", "978-0132350884");
        BookEntity book2 = new BookEntity("Clean Architecture", "978-0134494166");

        when(bookFacade.searchBooks("Clean"))
            .thenReturn(List.of(book1, book2));

        // When
        commands.searchBooks();

        // Then
        verify(bookFacade).searchBooks("Clean");
        verify(ui, times(2)).printInfo(anyString());  // 2 books printed
    }
}
```

---

## Test Data Builders

Instead of creating entities manually in every test, use builders:

**BookTestDataBuilder.java:**
```java
package com.penrose.bibby.testutil;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.book.BookStatus;

public class BookTestDataBuilder {

    private String title = "Default Title";
    private String isbn = "978-0000000000";
    private BookStatus status = BookStatus.AVAILABLE;
    private Integer checkoutCount = 0;
    private AuthorEntity author;

    public static BookTestDataBuilder aBook() {
        return new BookTestDataBuilder();
    }

    public BookTestDataBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public BookTestDataBuilder withIsbn(String isbn) {
        this.isbn = isbn;
        return this;
    }

    public BookTestDataBuilder withStatus(BookStatus status) {
        this.status = status;
        return this;
    }

    public BookTestDataBuilder checkedOut() {
        this.status = BookStatus.CHECKED_OUT;
        this.checkoutCount = 1;
        return this;
    }

    public BookTestDataBuilder withAuthor(AuthorEntity author) {
        this.author = author;
        return this;
    }

    public BookEntity build() {
        BookEntity book = new BookEntity(title, isbn);
        book.setStatus(status);
        book.setCheckoutCount(checkoutCount);

        if (author != null) {
            book.addAuthor(author);
        }

        return book;
    }
}
```

**Usage:**
```java
@Test
void exampleTest() {
    // Fluent, readable test data creation
    BookEntity book = aBook()
        .withTitle("Clean Code")
        .withIsbn("978-0132350884")
        .checkedOut()
        .withAuthor(authorRepository.findById(1L).get())
        .build();

    // Use book in test...
}
```

---

## Test Configuration

**application-test.properties:**
```properties
# Use H2 in-memory database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Disable shell in tests
spring.shell.interactive.enabled=false
spring.main.web-application-type=none

# Fast logging
logging.level.root=WARN
logging.level.com.penrose.bibby=DEBUG
```

**Test Base Class (Optional):**
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected BookRepository bookRepository;

    @Autowired
    protected AuthorRepository authorRepository;

    @BeforeEach
    void baseSetUp() {
        // Clear data before each test
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}

// Usage
class MyIntegrationTest extends IntegrationTestBase {
    @Test
    void myTest() {
        // bookRepository is available
    }
}
```

---

## Test Coverage

### Measuring Coverage

**Add JaCoCo plugin to pom.xml:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>jacoco-check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Run tests with coverage:**
```bash
mvn clean test

# View report
open target/site/jacoco/index.html
```

**Coverage Goals:**
- **Services:** 90%+ (critical business logic)
- **Controllers:** 85%+ (API contracts)
- **Repositories:** 70%+ (mostly framework code)
- **Entities:** 50%+ (getters/setters less important)
- **Overall:** 80%+

---

## Action Items

### 🚨 Critical (Do First - 8-10 hours)

1. **Create Service Tests**
   - [ ] Write `BookServiceTest` with 10+ test cases
   - [ ] Write `AuthorServiceTest`
   - [ ] Write `BookcaseServiceTest`
   - [ ] Cover happy paths and error cases

2. **Create Repository Tests**
   - [ ] Write `BookRepositoryTest` with @DataJpaTest
   - [ ] Write `AuthorRepositoryTest`
   - [ ] Test custom queries
   - [ ] Add H2 test dependency

3. **Create Controller Tests**
   - [ ] Write `BookControllerTest` with @WebMvcTest
   - [ ] Test all endpoints (POST, GET, PUT, DELETE)
   - [ ] Test validation errors
   - [ ] Test 404 responses

### 🔶 High Priority (This Week - 6-8 hours)

4. **Add Test Data Builders**
   - [ ] Create `BookTestDataBuilder`
   - [ ] Create `AuthorTestDataBuilder`
   - [ ] Create `BookcaseTestDataBuilder`
   - [ ] Use builders in all tests

5. **Setup Test Configuration**
   - [ ] Create `application-test.properties`
   - [ ] Configure H2 for tests
   - [ ] Disable shell in tests
   - [ ] Add test logging configuration

6. **Add Coverage Reporting**
   - [ ] Add JaCoCo plugin to pom.xml
   - [ ] Run coverage report
   - [ ] Aim for 80%+ coverage
   - [ ] Fix uncovered critical paths

### 🔷 Medium Priority (This Month - 4-6 hours)

7. **Create Integration Tests**
   - [ ] Test complete book creation flow (service + repository)
   - [ ] Test checkout flow
   - [ ] Test search with pagination

8. **Test Shell Commands**
   - [ ] Write tests for command classes
   - [ ] Mock `ShellUIService`
   - [ ] Test user input flows

9. **Add Performance Tests**
   - [ ] Test pagination with 1000+ books
   - [ ] Test N+1 query fixes
   - [ ] Measure query counts

### ⚪ Low Priority (Nice to Have)

10. **Advanced Testing**
    - [ ] Add contract tests (Spring Cloud Contract)
    - [ ] Add mutation testing (PIT)
    - [ ] Add architecture tests (ArchUnit)
    - [ ] Add load tests (Gatling/JMeter)

---

## Common Testing Patterns

### Testing Exceptions

```java
@Test
void methodName_whenCondition_shouldThrowException() {
    // Given
    when(repository.findById(999L)).thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> service.findById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("not found");
}
```

### Testing Void Methods

```java
@Test
void methodName_shouldDoSomething() {
    // Given
    doNothing().when(service).deleteBook(1L);

    // When
    service.deleteBook(1L);

    // Then
    verify(service).deleteBook(1L);
    verify(repository, never()).save(any());
}
```

### Testing Collections

```java
@Test
void findAll_shouldReturnAllBooks() {
    // When
    List<Book> books = service.findAll();

    // Then
    assertThat(books)
        .hasSize(3)
        .extracting(Book::getTitle)
        .containsExactlyInAnyOrder("Book 1", "Book 2", "Book 3");
}
```

### Testing Pagination

```java
@Test
void findAll_withPagination_shouldReturnPage() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Book> page = repository.findAll(pageable);

    // Then
    assertThat(page.getTotalElements()).isEqualTo(25);
    assertThat(page.getTotalPages()).isEqualTo(3);
    assertThat(page.getNumber()).isEqualTo(0);
    assertThat(page.getSize()).isEqualTo(10);
    assertThat(page.getContent()).hasSize(10);
}
```

---

## Summary

### Your Current State
- ❌ No service tests
- ❌ No repository tests
- ❌ No controller tests
- ❌ 1 empty test stub
- ❌ ~0% test coverage

### After This Section
- ✅ Comprehensive service tests
- ✅ Repository integration tests
- ✅ Controller API tests
- ✅ Test data builders
- ✅ 80%+ test coverage
- ✅ Confidence to refactor

---

## Resources

### Official Docs
- [Spring Boot Testing](https://docs.spring.io/spring-boot/reference/testing/index.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)

### Books
- **"Growing Object-Oriented Software, Guided by Tests"** by Steve Freeman
- **"Test Driven Development: By Example"** by Kent Beck

---

## Mentor's Note

Leo, you currently have **zero meaningful tests**. This is the single biggest risk to your project.

**Why testing matters:**
1. **Confidence:** Change code without fear
2. **Documentation:** Tests show how code should work
3. **Design:** Good tests force good design
4. **Debugging:** Failing tests pinpoint exact problem
5. **Interviews:** "Do you write tests?" = instant credibility

**Your situation:**
- 42 Java files
- 0 tests
- Every refactoring = Russian roulette

**Action plan:**
1. Start with service tests (highest ROI)
2. Add repository tests
3. Add controller tests
4. Aim for 80% coverage

**Time investment:**
- 8-10 hours for initial test suite
- Then 10-15 minutes per new feature

This is **not optional** for professional development. Every company I've worked at requires tests before merging code.

---

**Previous Section:** Spring Boot + Spring Shell Architecture

**Last Updated:** 2025-11-17
**Status:** Complete ✅
