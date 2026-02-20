package com.penrose.bibby.library.cataloging.book.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.BookDomainRepository;
import java.time.LocalDate;
import java.util.List;

import com.penrose.bibby.library.cataloging.book.infrastructure.adapter.outbound.BookAccessAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookAccessAdapterTest {

  @Mock private BookDomainRepository bookDomainRepository;

  @InjectMocks private BookAccessAdapter bookAccessAdapter;

  @Test
  void getBookIdsByShelfId_shouldReturnBookIdsWhenBooksExist() {
    Long shelfId = 1L;
    List<BookDTO> books =
        List.of(
            new BookDTO(
                101L,
                1,
                "Book A",
                List.of("Author A"),
                "1234567890",
                "Fiction",
                "Publisher A",
                2020,
                shelfId,
                "Description A",
                AvailabilityStatus.AVAILABLE,
                LocalDate.now(),
                LocalDate.now(),
                "2020-01-01"),
            new BookDTO(
                102L,
                1,
                "Book B",
                List.of("Author B"),
                "0987654321",
                "Non-Fiction",
                "Publisher B",
                2021,
                shelfId,
                "Description B",
                AvailabilityStatus.AVAILABLE,
                LocalDate.now(),
                LocalDate.now(),
                "2021-01-01"));
    when(bookDomainRepository.getBooksByShelfId(shelfId)).thenReturn(books);

    List<Long> bookIds = bookAccessAdapter.getBookIdsByShelfId(shelfId);

    assertThat(bookIds).containsExactly(101L, 102L);
  }

  @Test
  void getBookIdsByShelfId_shouldReturnEmptyListWhenNoBooksExist() {
    Long shelfId = 1L;
    when(bookDomainRepository.getBooksByShelfId(shelfId)).thenReturn(List.of());

    List<Long> bookIds = bookAccessAdapter.getBookIdsByShelfId(shelfId);

    assertThat(bookIds).isEmpty();
  }

  @Test
  void getBookIdsByShelfId_shouldHandleNullShelfId() {
    when(bookDomainRepository.getBooksByShelfId(null)).thenReturn(List.of());

    List<Long> bookIds = bookAccessAdapter.getBookIdsByShelfId(null);

    assertThat(bookIds).isEmpty();
  }
}
