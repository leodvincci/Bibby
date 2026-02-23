package com.penrose.bibby.web.controllers.cataloging.book;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.penrose.bibby.library.cataloging.author.core.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.core.application.BookService;
import com.penrose.bibby.library.cataloging.book.core.application.IsbnLookupService;
import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.BookId;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Title;
import com.penrose.bibby.library.stacks.bookcase.core.application.BookcaseService;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = BookController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EnableWebSecurity.class))
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean BookService bookService;
  @MockitoBean IsbnLookupService isbnLookupService;
  @MockitoBean ShelfFacade shelfFacade;
  @MockitoBean BookcaseService bookcaseService;
  @MockitoBean AuthorFacade authorFacade;

  @Test
  void placeBookOnShelf_whenShelfNotFound_returns404() throws Exception {
    Long bookId = 1L;
    Long shelfId = 99L;

    Book book = new Book(new BookId(bookId), new Title("Clean Code"), Collections.emptyList());

    when(bookService.assignBookToShelf(bookId, shelfId)).thenReturn(book);
    when(shelfFacade.findShelfById(shelfId)).thenReturn(Optional.empty());

    String payload =
        """
        { "shelfId": 99 }
        """;

    mockMvc
        .perform(
            post("/api/v1/books/{bookId}/shelf", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNotFound());
  }
}
