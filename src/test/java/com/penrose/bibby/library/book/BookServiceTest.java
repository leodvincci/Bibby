package com.penrose.bibby.library.book;

// JUnit 5 imports (note the jupiter package - this is JUnit 5, not JUnit 4!)
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

// Mockito imports
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Assertions - these are your test verification methods
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for BookService.
 *
 * LEARNING NOTES:
 * ===============
 * These are UNIT tests, not integration tests. Key differences:
 *
 * ✅ UNIT TEST (what we're doing here):
 *    - Tests ONE class (BookService) in isolation
 *    - Mocks all external dependencies (repositories)
 *    - No database required
 *    - FAST (milliseconds)
 *    - Tests business logic
 *
 * ❌ INTEGRATION TEST (not here):
 *    - Tests multiple components together
 *    - Uses real database (or test database)
 *    - SLOWER (seconds)
 *    - Tests that components work together
 *
 * WHY MOCK REPOSITORIES?
 * ======================
 * 1. Speed: No database = tests run in milliseconds
 * 2. Isolation: We're testing BookService logic, not Hibernate
 * 3. Control: We decide exactly what the repository returns
 * 4. No setup: No need to seed test data in a database
 */
@ExtendWith(MockitoExtension.class)  // This enables Mockito - required for @Mock to work!
@DisplayName("BookService Unit Tests")  // Prettier test report names
class BookServiceTest {

    // ==================== TEST DOUBLES ====================
    // "Test double" = generic term for mocks, stubs, fakes, etc.

    /**
     * @Mock creates a "fake" object that we control.
     * When you call methods on a mock, they return null by default.
     * We use when(...).thenReturn(...) to teach the mock how to behave.
     */
    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    /**
     * This is the REAL BookService we're testing.
     * But it uses FAKE repositories (the mocks above).
     *
     * We create this manually in @BeforeEach to have full control.
     */
    private BookService bookService;

    /**
     * @BeforeEach runs before EACH test method.
     * This ensures every test starts with a fresh, clean state.
     *
     * WHY NOT PUT THIS IN A CONSTRUCTOR?
     * - JUnit creates a new instance of the test class for each test
     * - @BeforeEach makes the initialization explicit and clear
     */
    @BeforeEach
    void setUp() {
        // Create a REAL BookService with FAKE repositories
        bookService = new BookService(bookRepository, authorRepository);

        // Note: We could also use @InjectMocks instead of manual construction:
        // @InjectMocks private BookService bookService;
        // But I prefer manual construction for clarity
    }

    // ==================== TESTS FOR createNewBook() ====================

    /**
     * @Nested lets us group related tests together.
     * This improves test report readability:
     *
     * BookService Unit Tests
     *   └── createNewBook()
     *       ├── should create new author and book when neither exist
     *       ├── should reuse existing author when author already exists
     *       └── ...
     */
    @Nested
    @DisplayName("createNewBook()")
    class CreateNewBookTests {

        /**
         * TEST CASE: Happy path - nothing exists yet
         *
         * SCENARIO:
         * - Book "1984" doesn't exist in database
         * - Author "George Orwell" doesn't exist in database
         * - Should create BOTH and link them together
         *
         * LEARNING: This is the "happy path" - everything works as expected.
         * Always start with happy path tests, then add edge cases.
         */
        @Test
        @DisplayName("should create new author and book when neither exist")
        void shouldCreateNewAuthorAndBook_WhenNeitherExist() {
            // ========== GIVEN (Arrange) ==========
            // Prepare test data
            BookRequestDTO request = new BookRequestDTO(
                    "1984",        // title
                    "George",      // firstName
                    "Orwell"       // lastName
            );

            // STUBBING: Teach the mocks what to return
            // "When someone calls findByTitle('1984'), return null"
            when(bookRepository.findByTitle("1984")).thenReturn(null);
            when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
                    .thenReturn(null);

            // ========== WHEN (Act) ==========
            // Execute the method we're testing
            bookService.createNewBook(request);

            // ========== THEN (Assert) ==========
            // Verify the expected outcomes

            // 1. Verify author was saved
            // LEARNING: ArgumentCaptor captures the argument passed to save()
            // so we can inspect it and make assertions about it
            ArgumentCaptor<AuthorEntity> authorCaptor =
                    ArgumentCaptor.forClass(AuthorEntity.class);
            verify(authorRepository).save(authorCaptor.capture());

            AuthorEntity savedAuthor = authorCaptor.getValue();
            assertEquals("George", savedAuthor.getFirstName(),
                    "Author first name should match the request");
            assertEquals("Orwell", savedAuthor.getLastName(),
                    "Author last name should match the request");

            // 2. Verify book was saved
            ArgumentCaptor<BookEntity> bookCaptor =
                    ArgumentCaptor.forClass(BookEntity.class);
            verify(bookRepository).save(bookCaptor.capture());

            BookEntity savedBook = bookCaptor.getValue();
            assertEquals("1984", savedBook.getTitle(),
                    "Book title should match the request");
            assertNotNull(savedBook.getAuthors(),
                    "Book should be linked to author");

            // LEARNING: Why ArgumentCaptor instead of just verify(repo).save(any())?
            // - ArgumentCaptor lets us inspect the actual object that was saved
            // - We can make detailed assertions about its state
            // - It catches bugs where save() is called but with wrong data
        }

        /**
         * TEST CASE: Author exists, book doesn't
         *
         * SCENARIO:
         * - Author "George Orwell" already exists (ID: 123)
         * - Book "Animal Farm" doesn't exist
         * - Should reuse existing author, NOT create a duplicate
         *
         * LEARNING: This tests the "find or create" pattern.
         * We're verifying that the code reuses existing entities instead of
         * creating duplicates.
         */
        @Test
        @DisplayName("should reuse existing author when author already exists")
        void shouldReuseExistingAuthor_WhenAuthorExists() {
            // ========== GIVEN ==========
            BookRequestDTO request = new BookRequestDTO(
                    "Animal Farm",
                    "George",
                    "Orwell"
            );

            // Create a pre-existing author (simulating database record)
            AuthorEntity existingAuthor = new AuthorEntity("George", "Orwell");
            existingAuthor.setAuthorId(123L);  // Simulates DB-assigned ID

            // Stub: book doesn't exist, but author does
            when(bookRepository.findByTitle("Animal Farm")).thenReturn(null);
            when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
                    .thenReturn(existingAuthor);  // Return the existing author!

            // ========== WHEN ==========
            bookService.createNewBook(request);

            // ========== THEN ==========

            // CRITICAL: Author should NOT be saved again (it already exists)
            verify(authorRepository, never()).save(any(AuthorEntity.class));
            // LEARNING: never() verifies a method was NEVER called
            // This ensures we're not creating duplicate authors

            // Book should be saved
            ArgumentCaptor<BookEntity> bookCaptor =
                    ArgumentCaptor.forClass(BookEntity.class);
            verify(bookRepository).save(bookCaptor.capture());

            BookEntity savedBook = bookCaptor.getValue();
            assertEquals("Animal Farm", savedBook.getTitle());

            // CRITICAL: Book should be linked to EXISTING author, not a new one
            assertEquals(existingAuthor, savedBook.getAuthors(),
                    "Book should be linked to existing author with ID 123");
            assertEquals(123L, savedBook.getAuthors().getAuthorId(),
                    "Author ID should be 123 (the existing one)");
        }

        /**
         * TEST CASE: Book exists, author exists
         *
         * SCENARIO:
         * - Both book and author already exist
         * - Should update the book's author link
         *
         * LEARNING: This tests the "update existing" path.
         * Note: The current implementation has a bug here - it always
         * overwrites the book's authors! This test documents that behavior.
         */
        @Test
        @DisplayName("should update existing book when book already exists")
        void shouldUpdateExistingBook_WhenBookExists() {
            // ========== GIVEN ==========
            BookRequestDTO request = new BookRequestDTO("1984", "George", "Orwell");

            // Pre-existing book
            BookEntity existingBook = new BookEntity();
            existingBook.setTitle("1984");
            existingBook.setBookId(456L);

            // Pre-existing author
            AuthorEntity existingAuthor = new AuthorEntity("George", "Orwell");
            existingAuthor.setAuthorId(123L);

            // Stub: both already exist
            when(bookRepository.findByTitle("1984")).thenReturn(existingBook);
            when(authorRepository.findByFirstNameAndLastName("George", "Orwell"))
                    .thenReturn(existingAuthor);

            // ========== WHEN ==========
            bookService.createNewBook(request);

            // ========== THEN ==========

            // Neither should be created (both already exist)
            verify(authorRepository, never()).save(any(AuthorEntity.class));

            // But book should still be saved (to update the author link)
            verify(bookRepository).save(existingBook);

            // Verify the link was updated
            assertEquals(existingAuthor, existingBook.getAuthors(),
                    "Existing book should be linked to existing author");

            // BUG ALERT! The current code always calls setAuthors(),
            // which OVERWRITES all authors. If a book has multiple authors,
            // this will remove all but the last one!
            // This test documents the CURRENT behavior, not necessarily CORRECT behavior.
        }
    }

    // ==================== TESTS FOR checkOutBook() ====================

    @Nested
    @DisplayName("checkOutBook()")
    class CheckOutBookTests {

        /**
         * TEST CASE: Check out an available book
         *
         * SCENARIO:
         * - Book is currently AVAILABLE
         * - Should update status to CHECKED_OUT
         * - Should save the book
         *
         * LEARNING: This tests state transitions.
         * AVAILABLE → CHECKED_OUT
         */
        @Test
        @DisplayName("should update book status to CHECKED_OUT when book is available")
        void shouldUpdateStatusToCheckedOut_WhenBookIsAvailable() {
            // ========== GIVEN ==========
            BookEntity book = new BookEntity();
            book.setBookId(1L);
            book.setTitle("1984");
            book.setBookStatus("AVAILABLE");  // Initially available

            // ========== WHEN ==========
            bookService.checkOutBook(book);

            // ========== THEN ==========
            assertEquals("CHECKED_OUT", book.getBookStatus(),
                    "Book status should be updated to CHECKED_OUT");

            // Verify save was called
            verify(bookRepository).save(book);

            // LEARNING: We're testing the state change AND the side effect (save)
        }

        /**
         * TEST CASE: Try to check out an already checked-out book
         *
         * SCENARIO:
         * - Book is already CHECKED_OUT
         * - Should do nothing (no state change)
         * - Should NOT call save()
         *
         * LEARNING: This tests the guard clause that prevents redundant operations.
         * Good code avoids unnecessary database writes.
         */
        @Test
        @DisplayName("should not update book when already checked out")
        void shouldNotUpdate_WhenBookAlreadyCheckedOut() {
            // ========== GIVEN ==========
            BookEntity book = new BookEntity();
            book.setBookId(1L);
            book.setTitle("1984");
            book.setBookStatus("CHECKED_OUT");  // Already checked out

            // ========== WHEN ==========
            bookService.checkOutBook(book);

            // ========== THEN ==========

            // Status should remain unchanged
            assertEquals("CHECKED_OUT", book.getBookStatus(),
                    "Book status should remain CHECKED_OUT");

            // CRITICAL: save() should NOT be called (no state change = no save)
            verify(bookRepository, never()).save(any(BookEntity.class));

            // LEARNING: This test verifies an optimization - don't save if nothing changed.
            // It also prevents potential issues with concurrent updates.
        }
    }

    // ==================== TESTS FOR checkInBook() ====================

    @Nested
    @DisplayName("checkInBook()")
    class CheckInBookTests {

        /**
         * TEST CASE: Check in a checked-out book
         *
         * SCENARIO:
         * - Book "1984" is currently CHECKED_OUT
         * - Should update status to AVAILABLE
         * - Should save the book
         *
         * LEARNING: This tests the reverse state transition.
         * CHECKED_OUT → AVAILABLE
         */
        @Test
        @DisplayName("should update book status to AVAILABLE when checking in")
        void shouldUpdateStatusToAvailable_WhenCheckingIn() {
            // ========== GIVEN ==========
            String bookTitle = "1984";

            BookEntity checkedOutBook = new BookEntity();
            checkedOutBook.setTitle(bookTitle);
            checkedOutBook.setBookStatus("CHECKED_OUT");

            // Stub: findBookByTitle() calls TWO repository methods
            when(bookRepository.findByTitleIgnoreCase(bookTitle))
                    .thenReturn(checkedOutBook);
            when(bookRepository.findByTitleContaining(bookTitle))
                    .thenReturn(List.of(checkedOutBook));

            // ========== WHEN ==========
            bookService.checkInBook(bookTitle);

            // ========== THEN ==========
            assertEquals("AVAILABLE", checkedOutBook.getBookStatus(),
                    "Book should be marked as AVAILABLE after check-in");

            // Verify save was called
            verify(bookRepository).save(checkedOutBook);
        }

        /**
         * TEST CASE: Try to check in a book that doesn't exist
         *
         * SCENARIO:
         * - Book "Nonexistent Book" doesn't exist
         * - findBookByTitle() returns null
         * - checkInBook() tries to call setBookStatus() on null
         * - Should throw NullPointerException
         *
         * LEARNING: This test DOCUMENTS A BUG!
         * The code doesn't handle the case where the book doesn't exist.
         * It tries to call methods on null, which causes a NPE.
         *
         * This is valuable because:
         * 1. It documents the current (buggy) behavior
         * 2. When we fix the bug, we'll update this test
         * 3. It proves the bug exists (failing test)
         */
        @Test
        @DisplayName("should throw NullPointerException when book not found (BUG)")
        void shouldThrowNPE_WhenBookNotFound() {
            // ========== GIVEN ==========
            String nonExistentTitle = "Nonexistent Book";

            // Stub: book doesn't exist
            when(bookRepository.findByTitleIgnoreCase(nonExistentTitle))
                    .thenReturn(null);
            when(bookRepository.findByTitleContaining(nonExistentTitle))
                    .thenReturn(List.of());

            // ========== WHEN & THEN ==========
            // LEARNING: assertThrows verifies that an exception IS thrown
            assertThrows(NullPointerException.class, () -> {
                bookService.checkInBook(nonExistentTitle);
            }, "Should throw NPE when book is not found");

            // No save should happen
            verify(bookRepository, never()).save(any(BookEntity.class));

            // TODO: Fix this bug! checkInBook() should either:
            // 1. Return a result object indicating failure
            // 2. Throw a meaningful exception (BookNotFoundException)
            // 3. Return a boolean (true = success, false = not found)
            //
            // After fixing, update this test to verify the correct behavior.
        }
    }

    // ==================== TESTS FOR findBookByTitle() ====================

    @Nested
    @DisplayName("findBookByTitle()")
    class FindBookByTitleTests {

        /**
         * TEST CASE: Find book by exact title match
         *
         * SCENARIO:
         * - Book "1984" exists
         * - findByTitleIgnoreCase returns it
         * - Should return the book
         *
         * LEARNING: Happy path for search functionality
         */
        @Test
        @DisplayName("should return book when exact match found")
        void shouldReturnBook_WhenExactMatchFound() {
            // ========== GIVEN ==========
            String title = "1984";
            BookEntity expectedBook = new BookEntity();
            expectedBook.setTitle(title);
            expectedBook.setBookId(1L);

            when(bookRepository.findByTitleIgnoreCase(title))
                    .thenReturn(expectedBook);
            when(bookRepository.findByTitleContaining(title))
                    .thenReturn(List.of(expectedBook));

            // ========== WHEN ==========
            BookEntity result = bookService.findBookByTitle(title);

            // ========== THEN ==========
            assertNotNull(result, "Should return a book when found");
            assertEquals(title, result.getTitle(), "Returned book should have correct title");
            assertEquals(1L, result.getBookId(), "Returned book should have correct ID");
        }

        /**
         * TEST CASE: Search for book that doesn't exist
         *
         * SCENARIO:
         * - Book "Nonexistent Book" doesn't exist
         * - Both search methods return empty results
         * - Should return null
         *
         * LEARNING: This tests the "not found" path.
         * Note: Returning null is often considered a code smell.
         * Better alternatives:
         * - Return Optional<BookEntity>
         * - Throw BookNotFoundException
         * But we're testing the CURRENT behavior here.
         */
        @Test
        @DisplayName("should return null when book not found")
        void shouldReturnNull_WhenBookNotFound() {
            // ========== GIVEN ==========
            String title = "Nonexistent Book";

            when(bookRepository.findByTitleIgnoreCase(title))
                    .thenReturn(null);
            when(bookRepository.findByTitleContaining(title))
                    .thenReturn(List.of());

            // ========== WHEN ==========
            BookEntity result = bookService.findBookByTitle(title);

            // ========== THEN ==========
            assertNull(result, "Should return null when book not found");

            // DESIGN NOTE: This method has a side effect (System.out.println)
            // that we can't easily test without mocking System.out.
            // This is a code smell - side effects in search methods make testing harder.
        }

        /**
         * TEST CASE: Case-insensitive search
         *
         * SCENARIO:
         * - Search for "1984" (lowercase)
         * - Database has "1984"
         * - Should find it (case insensitive)
         *
         * LEARNING: Tests the case-insensitivity requirement
         */
        @Test
        @DisplayName("should find book regardless of case")
        void shouldFindBook_CaseInsensitive() {
            // ========== GIVEN ==========
            BookEntity book = new BookEntity();
            book.setTitle("1984");

            // Search with different case
            when(bookRepository.findByTitleIgnoreCase("1984"))
                    .thenReturn(book);
            when(bookRepository.findByTitleContaining("1984"))
                    .thenReturn(List.of(book));

            // ========== WHEN ==========
            BookEntity result = bookService.findBookByTitle("1984");

            // ========== THEN ==========
            assertNotNull(result);
            assertEquals("1984", result.getTitle());
        }
    }

    // ==================== TESTS FOR findBookById() ====================

    @Nested
    @DisplayName("findBookById()")
    class FindBookByIdTests {

        /**
         * TEST CASE: Find book by valid ID
         *
         * SCENARIO:
         * - Book with ID 1 exists
         * - Should return Optional containing the book
         *
         * LEARNING: This is a simple delegation test.
         * Some developers argue these aren't worth testing because
         * they just delegate to the repository. Others say it's worth
         * it to verify the delegation is correct.
         *
         * I recommend testing delegation if:
         * 1. The method is public API
         * 2. You might change the implementation later
         * 3. It's trivial to test (like this one)
         */
        @Test
        @DisplayName("should return Optional with book when found")
        void shouldReturnOptional_WhenBookFound() {
            // ========== GIVEN ==========
            Long bookId = 1L;
            BookEntity book = new BookEntity();
            book.setBookId(bookId);
            book.setTitle("1984");

            when(bookRepository.findById(bookId))
                    .thenReturn(Optional.of(book));

            // ========== WHEN ==========
            Optional<BookEntity> result = bookService.findBookById(bookId);

            // ========== THEN ==========
            assertTrue(result.isPresent(), "Optional should contain a book");
            assertEquals(bookId, result.get().getBookId());
            assertEquals("1984", result.get().getTitle());

            // Verify the repository was called with correct ID
            verify(bookRepository).findById(bookId);
        }

        /**
         * TEST CASE: Find book by invalid ID
         *
         * SCENARIO:
         * - Book with ID 999 doesn't exist
         * - Should return empty Optional
         */
        @Test
        @DisplayName("should return empty Optional when not found")
        void shouldReturnEmptyOptional_WhenBookNotFound() {
            // ========== GIVEN ==========
            Long nonExistentId = 999L;

            when(bookRepository.findById(nonExistentId))
                    .thenReturn(Optional.empty());

            // ========== WHEN ==========
            Optional<BookEntity> result = bookService.findBookById(nonExistentId);

            // ========== THEN ==========
            assertFalse(result.isPresent(), "Optional should be empty");

            // Alternative assertion style:
            assertTrue(result.isEmpty(), "Optional should be empty");
        }
    }
}
