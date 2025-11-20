package com.penrose.bibby.library.bookcase;

import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookcaseService.
 *
 * EXERCISE FOR YOU:
 * =================
 * I've started this test class for you as practice. Try to complete it!
 *
 * TIPS:
 * - Look at BookServiceTest for examples
 * - Follow the Given/When/Then pattern
 * - Test both happy paths and edge cases
 * - Use ArgumentCaptor to inspect saved entities
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookcaseService Unit Tests")
class BookcaseServiceTest {

    @Mock
    private BookcaseRepository bookcaseRepository;

    @Mock
    private ShelfRepository shelfRepository;

    private BookcaseService bookcaseService;

    @BeforeEach
    void setUp() {
        bookcaseService = new BookcaseService(bookcaseRepository, shelfRepository);
    }

    @Nested
    @DisplayName("createNewBookCase()")
    class CreateNewBookCaseTests {

        @Test
        @DisplayName("should create bookcase with shelves when label is unique")
        void shouldCreateBookcaseWithShelves_WhenLabelIsUnique() {
            // ========== GIVEN ==========
            String label = "Fiction";
            int capacity = 5;

            // Stub: no existing bookcase with this label
            when(bookcaseRepository.findBookcaseEntityByBookcaseLabel(label))
                    .thenReturn(null);

            // IMPORTANT: When save() is called, we need to set the ID
            // because the real code uses it to create shelves
            when(bookcaseRepository.save(any(BookcaseEntity.class)))
                    .thenAnswer(invocation -> {
                        BookcaseEntity entity = invocation.getArgument(0);
                        entity.setBookcaseId(123L);  // Simulate DB-assigned ID
                        return entity;
                    });

            // ========== WHEN ==========
            String result = bookcaseService.createNewBookCase(label, capacity);

            // ========== THEN ==========

            // TODO: Complete these assertions

            // 1. Verify bookcase was saved
            // verify(...).save(...);

            // 2. Verify the correct number of shelves were created
            // Hint: Use times(N) to verify save() was called N times on shelfRepository
            // verify(shelfRepository, times(?)).save(any(ShelfEntity.class));

            // 3. Verify the return message is correct
            // assertEquals("...", result);

            // 4. BONUS: Use ArgumentCaptor to verify shelf labels
            // ArgumentCaptor<ShelfEntity> captor = ArgumentCaptor.forClass(ShelfEntity.class);
            // verify(shelfRepository, times(5)).save(captor.capture());
            // List<ShelfEntity> shelves = captor.getAllValues();
            // assertEquals("Shelf 0", shelves.get(0).getShelfLabel());
            // assertEquals("Shelf 1", shelves.get(1).getShelfLabel());
            // ... etc
        }

        @Test
        @DisplayName("should throw ResponseStatusException when bookcase label already exists")
        void shouldThrowException_WhenLabelExists() {
            // ========== GIVEN ==========
            String existingLabel = "Fiction";
            int capacity = 5;

            // Stub: bookcase with this label already exists
            BookcaseEntity existingBookcase = new BookcaseEntity(existingLabel, 10);
            existingBookcase.setBookcaseId(1L);
            when(bookcaseRepository.findBookcaseEntityByBookcaseLabel(existingLabel))
                    .thenReturn(existingBookcase);

            // ========== WHEN & THEN ==========

            // TODO: Complete this test
            // Use assertThrows to verify ResponseStatusException is thrown
            // assertThrows(ResponseStatusException.class, () -> {
            //     bookcaseService.createNewBookCase(existingLabel, capacity);
            // });

            // Verify nothing was saved (because exception was thrown)
            // verify(bookcaseRepository, never()).save(any());
            // verify(shelfRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle zero capacity gracefully")
        void shouldHandleZeroCapacity() {
            // ========== GIVEN ==========
            String label = "Empty";
            int capacity = 0;

            when(bookcaseRepository.findBookcaseEntityByBookcaseLabel(label))
                    .thenReturn(null);
            when(bookcaseRepository.save(any(BookcaseEntity.class)))
                    .thenAnswer(invocation -> {
                        BookcaseEntity entity = invocation.getArgument(0);
                        entity.setBookcaseId(456L);
                        return entity;
                    });

            // ========== WHEN ==========
            String result = bookcaseService.createNewBookCase(label, capacity);

            // ========== THEN ==========

            // TODO: Complete this test
            // 1. Verify bookcase was saved
            // 2. Verify NO shelves were created (capacity is 0)
            // 3. Verify return message
        }

        @Test
        @DisplayName("should create correct shelf labels and positions")
        void shouldCreateCorrectShelfLabelsAndPositions() {
            // ========== GIVEN ==========
            String label = "Science";
            int capacity = 3;

            when(bookcaseRepository.findBookcaseEntityByBookcaseLabel(label))
                    .thenReturn(null);
            when(bookcaseRepository.save(any(BookcaseEntity.class)))
                    .thenAnswer(invocation -> {
                        BookcaseEntity entity = invocation.getArgument(0);
                        entity.setBookcaseId(789L);
                        return entity;
                    });

            // ========== WHEN ==========
            bookcaseService.createNewBookCase(label, capacity);

            // ========== THEN ==========

            // TODO: Use ArgumentCaptor to inspect all saved shelves
            // ArgumentCaptor<ShelfEntity> captor = ArgumentCaptor.forClass(ShelfEntity.class);
            // verify(shelfRepository, times(3)).save(captor.capture());
            //
            // List<ShelfEntity> shelves = captor.getAllValues();
            // assertEquals(3, shelves.size());
            //
            // // Verify first shelf
            // assertEquals("Shelf 0", shelves.get(0).getShelfLabel());
            // assertEquals(0, shelves.get(0).getShelfPosition());
            // assertEquals(789L, shelves.get(0).getBookcaseId());
            //
            // // Verify second shelf
            // assertEquals("Shelf 1", shelves.get(1).getShelfLabel());
            // assertEquals(1, shelves.get(1).getShelfPosition());
            //
            // // Verify third shelf
            // assertEquals("Shelf 2", shelves.get(2).getShelfLabel());
            // assertEquals(2, shelves.get(2).getShelfPosition());
        }
    }

    @Nested
    @DisplayName("findBookCaseById()")
    class FindBookCaseByIdTests {

        @Test
        @DisplayName("should return Optional with bookcase when found")
        void shouldReturnOptional_WhenFound() {
            // TODO: Implement this test
            // Look at BookServiceTest.findBookById() for reference
        }

        @Test
        @DisplayName("should return empty Optional when not found")
        void shouldReturnEmptyOptional_WhenNotFound() {
            // TODO: Implement this test
        }
    }

    @Nested
    @DisplayName("getAllBookcases()")
    class GetAllBookcasesTests {

        @Test
        @DisplayName("should return all bookcases from repository")
        void shouldReturnAllBookcases() {
            // TODO: Implement this test
            //
            // GIVEN: Multiple bookcases in repository
            // WHEN: getAllBookcases() is called
            // THEN: Should return list from repository
        }

        @Test
        @DisplayName("should return empty list when no bookcases exist")
        void shouldReturnEmptyList_WhenNoBookcasesExist() {
            // TODO: Implement this test
        }
    }

    // ==================== HINTS & TIPS ====================

    /*
     * HINT 1: Testing loops
     * When testing createNewBookCase(), pay attention to the loop:
     *
     * for(int i = 0; i < capacity; i++) {
     *     addShelf(bookcaseEntity, i, i);
     * }
     *
     * You need to verify:
     * - The loop runs the correct number of times (verify save() called N times)
     * - Each shelf gets the correct label ("Shelf 0", "Shelf 1", etc.)
     * - Each shelf gets the correct position (0, 1, 2, etc.)
     *
     * Use ArgumentCaptor.getAllValues() to get all the saved shelves!
     */

    /*
     * HINT 2: Mocking save() to return an ID
     * The code does this:
     *
     * bookcaseRepository.save(bookcaseEntity);
     * for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++) {
     *     addShelf(bookcaseEntity, i, i);
     * }
     *
     * The shelves need the bookcase ID. So when you stub save(), return
     * an entity with an ID set:
     *
     * when(bookcaseRepository.save(any())).thenAnswer(invocation -> {
     *     BookcaseEntity entity = invocation.getArgument(0);
     *     entity.setBookcaseId(123L);  // Simulate DB
     *     return entity;
     * });
     */

    /*
     * HINT 3: Testing exceptions
     * When testing the duplicate label scenario, use assertThrows:
     *
     * ResponseStatusException exception = assertThrows(
     *     ResponseStatusException.class,
     *     () -> bookcaseService.createNewBookCase("Duplicate", 5)
     * );
     *
     * You can also verify the exception message:
     * assertTrue(exception.getReason().contains("already exist"));
     */

    /*
     * HINT 4: Edge cases to test
     * - Zero capacity (no shelves created)
     * - Large capacity (100 shelves)
     * - Null label (should it throw?)
     * - Empty string label (should it be allowed?)
     * - Negative capacity (is this validated?)
     */
}
