package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound.cataloging.BookAccessPortAdapter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookAccessPortAdapterTest {

  @Mock private BookFacade bookFacade;
  @InjectMocks private BookAccessPortAdapter bookAccessPortAdapter;

  @Test
  void getBookIdsByShelfId_shouldReturnEmptyList() {
    Long shelfId = 1L;

    List<Long> result = bookAccessPortAdapter.getBookIdsByShelfId(shelfId);

    assertThat(result).isEmpty();
  }

  @Test
  void deleteBooksOnShelves_shouldNotThrow() {
    List<Long> shelfIds = List.of(1L, 2L, 3L);

    bookAccessPortAdapter.deleteBooksOnShelves(shelfIds);
  }

  @Test
  void getBookById_shouldReturnBookIdWhenBookExists() {
    Long bookId = 1L;
    BookDTO bookDTO = mock(BookDTO.class);
    when(bookFacade.findBookById(bookId)).thenReturn(Optional.of(bookDTO));

    Long result = bookAccessPortAdapter.getBookById(bookId);

    assertThat(result).isEqualTo(bookId);
    verify(bookFacade).findBookById(bookId);
  }

  @Test
  void getBookById_shouldReturnNullWhenBookDoesNotExist() {
    Long bookId = 99L;
    when(bookFacade.findBookById(bookId)).thenReturn(Optional.empty());

    Long result = bookAccessPortAdapter.getBookById(bookId);

    assertThat(result).isNull();
    verify(bookFacade).findBookById(bookId);
  }
}
