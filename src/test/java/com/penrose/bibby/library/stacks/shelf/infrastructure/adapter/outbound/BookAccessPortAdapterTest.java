package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import java.util.List;
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
  void getBookIdsByShelfId_shouldDelegateToBookFacade() {
    Long shelfId = 1L;
    when(bookFacade.getBookIdsByShelfId(shelfId)).thenReturn(List.of(101L, 102L));

    List<Long> result = bookAccessPortAdapter.getBookIdsByShelfId(shelfId);

    assertThat(result).containsExactly(101L, 102L);
    verify(bookFacade).getBookIdsByShelfId(shelfId);
  }

  @Test
  void getBookIdsByShelfId_shouldReturnEmptyListWhenNoBooksExist() {
    Long shelfId = 1L;
    when(bookFacade.getBookIdsByShelfId(shelfId)).thenReturn(List.of());

    List<Long> result = bookAccessPortAdapter.getBookIdsByShelfId(shelfId);

    assertThat(result).isEmpty();
  }

  @Test
  void deleteBooksOnShelves_shouldDelegateToBookFacade() {
    List<Long> shelfIds = List.of(1L, 2L, 3L);

    bookAccessPortAdapter.deleteBooksOnShelves(shelfIds);

    verify(bookFacade).deleteByShelfId(shelfIds);
  }
}
