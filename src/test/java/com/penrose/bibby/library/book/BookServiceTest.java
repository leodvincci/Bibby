package com.penrose.bibby.library.book;
import com.penrose.bibby.library.book.domain.BookEntity;
import com.penrose.bibby.library.book.repository.BookRepository;
import com.penrose.bibby.library.book.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService serviceUnderTest;  // Replace with your actual class name


    @Test
    void findBookByKeyword_WhenBooksExist_ShouldReturnBooks() {
        // Arrange
        String keyword = "Java";

        BookEntity book1 = new BookEntity();
        book1.setTitle("Java Programming");

        BookEntity book2 = new BookEntity();
        book2.setTitle("Effective Java");

        BookEntity book3 = new BookEntity();
        book3.setTitle("The Great Java");

        List<BookEntity> expectedBooks = Arrays.asList(book1, book2, book3);

        when(bookRepository.findByTitleContaining(keyword))
                .thenReturn(expectedBooks);

        // Act
        List<BookEntity> result = serviceUnderTest.findBookByKeyword(keyword);

        // Assert
        assertEquals(3, result.size());
        assertEquals(expectedBooks, result);
        verify(bookRepository).findByTitleContaining(keyword);
    }

    @Test
    void findBookByKeyword_WhenNoBooks_ShouldReturnEmptyList() {
        // Arrange
        String keyword = "NonExistent";
        when(bookRepository.findByTitleContaining(keyword))
                .thenReturn(Collections.emptyList());

        // Act
        List<BookEntity> result = serviceUnderTest.findBookByKeyword(keyword);

        // Assert
        assertTrue(result.isEmpty());
        verify(bookRepository).findByTitleContaining(keyword);
    }



}