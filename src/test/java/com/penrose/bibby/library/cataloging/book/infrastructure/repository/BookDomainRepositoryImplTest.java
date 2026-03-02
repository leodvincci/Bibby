package com.penrose.bibby.library.cataloging.book.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDetailView;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorName;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.BookId;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Isbn;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Title;
import com.penrose.bibby.library.cataloging.book.infrastructure.adapter.mapping.BookMapper;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookDomainRepositoryImplTest {

  @Mock private BookJpaRepository bookJpaRepository;
  @Mock private BookMapper bookMapper;

  @InjectMocks private BookDomainRepositoryImpl bookDomainRepositoryImpl;

  @Captor private ArgumentCaptor<BookEntity> entityCaptor;

  // --- Helper methods ---

  private Book createTestBook() {
    return new Book(
        new BookId(1L),
        1,
        new Title("Test Book"),
        List.of(
            new AuthorRef(1L, new AuthorName("Author", "One")),
            new AuthorRef(2L, new AuthorName("Author", "Two"))),
        new Isbn("9781492034025"),
        "Fiction",
        "Test Publisher",
        2020,
        5L,
        "Test Description",
        AvailabilityStatus.AVAILABLE,
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 6, 1),
        "2020-01-01");
  }

  private BookEntity createTestBookEntity() {
    return new BookEntity(
        1L,
        "Test Book",
        "9781492034025",
        "Test Publisher",
        2020,
        "Fiction",
        1,
        "Test Description",
        5L,
        "AVAILABLE",
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 6, 1),
        Set.of(new AuthorEntity("Author", "One"), new AuthorEntity("Author", "Two")));
  }

  // --- getBooksByShelfId ---

  @Test
  void getBooksByShelfId_shouldReturnMappedBookDTOs() {
    // given
    Long shelfId = 5L;
    BookEntity entity1 = createTestBookEntity();
    BookEntity entity2 = createTestBookEntity();
    entity2.setBookId(2L);
    entity2.setTitle("Second Book");
    List<BookEntity> entities = List.of(entity1, entity2);

    BookDTO dto1 =
        new BookDTO(
            1L,
            1,
            "Test Book",
            List.of("Author One"),
            "9781492034025",
            "Fiction",
            "Test Publisher",
            2020,
            5L,
            "Test Description",
            AvailabilityStatus.AVAILABLE,
            LocalDate.now(),
            LocalDate.now(),
            null);
    BookDTO dto2 =
        new BookDTO(
            2L,
            1,
            "Second Book",
            List.of("Author One"),
            "9781492034025",
            "Fiction",
            "Test Publisher",
            2020,
            5L,
            "Test Description",
            AvailabilityStatus.AVAILABLE,
            LocalDate.now(),
            LocalDate.now(),
            null);

    when(bookJpaRepository.findByShelfId(shelfId)).thenReturn(entities);
    when(bookMapper.toDTOfromEntity(entity1)).thenReturn(dto1);
    when(bookMapper.toDTOfromEntity(entity2)).thenReturn(dto2);

    // when
    List<BookDTO> result = bookDomainRepositoryImpl.getBooksByShelfId(shelfId);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).title()).isEqualTo("Test Book");
    assertThat(result.get(1).title()).isEqualTo("Second Book");
    verify(bookJpaRepository).findByShelfId(shelfId);
    verify(bookMapper, times(2)).toDTOfromEntity(any(BookEntity.class));
  }

  @Test
  void getBooksByShelfId_shouldReturnEmptyListWhenNoBooksFound() {
    // given
    Long shelfId = 99L;
    when(bookJpaRepository.findByShelfId(shelfId)).thenReturn(List.of());

    // when
    List<BookDTO> result = bookDomainRepositoryImpl.getBooksByShelfId(shelfId);

    // then
    assertThat(result).isEmpty();
    verify(bookJpaRepository).findByShelfId(shelfId);
    verifyNoInteractions(bookMapper);
  }

  // --- registerBook ---

  @Test
  void registerBook_shouldMapDomainFieldsAndSaveEntity() {
    // given
    Book book = createTestBook();
    Set<AuthorEntity> authorEntities =
        Set.of(new AuthorEntity("Author", "One"), new AuthorEntity("Author", "Two"));
    when(bookMapper.toEntitySetFromAuthorRefs(book.getAuthors())).thenReturn(authorEntities);

    // when
    bookDomainRepositoryImpl.registerBook(book);

    // then
    verify(bookJpaRepository).save(entityCaptor.capture());
    BookEntity captured = entityCaptor.getValue();
    assertThat(captured.getTitle()).isEqualTo("Test Book");
    assertThat(captured.getIsbn()).isEqualTo("9781492034025");
    assertThat(captured.getPublisher()).isEqualTo("Test Publisher");
    assertThat(captured.getShelfId()).isEqualTo(5L);
    assertThat(captured.getGenre()).isEqualTo("Fiction");
    assertThat(captured.getDescription()).isEqualTo("Test Description");
    assertThat(captured.getAvailabilityStatus()).isEqualTo("AVAILABLE");
    assertThat(captured.getPublicationYear()).isEqualTo(2020);
    assertThat(captured.getAuthors()).isEqualTo(authorEntities);
    assertThat(captured.getCreatedAt()).isEqualTo(LocalDate.now());
    assertThat(captured.getUpdatedAt()).isEqualTo(LocalDate.now());
  }

  // --- updateBook ---

  @Test
  void updateBook_shouldFetchExistingEntityAndUpdateFields() {
    // given
    Book book = createTestBook();
    BookEntity existingEntity = createTestBookEntity();
    Set<AuthorEntity> authorEntities = Set.of(new AuthorEntity("Author", "One"));

    when(bookJpaRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
    when(bookMapper.toEntitySetFromAuthorRefs(book.getAuthors())).thenReturn(authorEntities);

    // when
    bookDomainRepositoryImpl.updateBook(book);

    // then
    verify(bookJpaRepository).findById(1L);
    verify(bookJpaRepository).save(existingEntity);
    assertThat(existingEntity.getTitle()).isEqualTo("Test Book");
    assertThat(existingEntity.getIsbn()).isEqualTo("9781492034025");
    assertThat(existingEntity.getShelfId()).isEqualTo(5L);
    assertThat(existingEntity.getAvailabilityStatus()).isEqualTo("AVAILABLE");
    assertThat(existingEntity.getAuthors()).isEqualTo(authorEntities);
    assertThat(existingEntity.getUpdatedAt()).isEqualTo(LocalDate.now());
  }

  // --- getBookById ---

  @Test
  void getBookById_shouldReturnEntityWhenFound() {
    // given
    BookEntity entity = createTestBookEntity();
    when(bookJpaRepository.findById(1L)).thenReturn(Optional.of(entity));

    // when
    BookEntity result = bookDomainRepositoryImpl.getBookById(1L);

    // then
    assertThat(result).isEqualTo(entity);
    assertThat(result.getTitle()).isEqualTo("Test Book");
    verify(bookJpaRepository).findById(1L);
  }

  @Test
  void getBookById_shouldThrowExceptionWhenNotFound() {
    // given
    when(bookJpaRepository.findById(99L)).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> bookDomainRepositoryImpl.getBookById(99L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Book not found with id: 99");
  }

  // --- findBookEntityByTitle ---

  @Test
  void findBookEntityByTitle_shouldDelegateToRepository() {
    // given
    BookEntity entity = createTestBookEntity();
    when(bookJpaRepository.findByTitle("Test Book")).thenReturn(entity);

    // when
    BookEntity result = bookDomainRepositoryImpl.findBookEntityByTitle("Test Book");

    // then
    assertThat(result).isEqualTo(entity);
    verify(bookJpaRepository).findByTitle("Test Book");
  }

  // --- updateTheBooksShelf ---

  @Test
  void updateTheBooksShelf_shouldUpdateShelfIdAndSave() {
    // given
    Long newShelfId = 10L;
    Book book = createTestBook();
    BookEntity bookEntity = createTestBookEntity();
    when(bookJpaRepository.findById(1L)).thenReturn(Optional.of(bookEntity));

    // when
    bookDomainRepositoryImpl.updateTheBooksShelf(book, 1L, newShelfId);

    // then
    assertThat(bookEntity.getShelfId()).isEqualTo(newShelfId);
    verify(bookJpaRepository).save(bookEntity);
  }

  @Test
  void updateTheBooksShelf_shouldThrowExceptionWhenBookNotFound() {
    // given
    Book book = createTestBook();
    when(bookJpaRepository.findById(99L)).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> bookDomainRepositoryImpl.updateTheBooksShelf(book, 99L, 10L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Book not found with id: 99");
  }

  // --- findBookByIsbn ---

  @Test
  void findBookByIsbn_shouldDelegateToRepository() {
    // given
    BookEntity entity = createTestBookEntity();
    when(bookJpaRepository.findByIsbn("9781492034025")).thenReturn(entity);

    // when
    BookEntity result = bookDomainRepositoryImpl.findBookByIsbn("9781492034025");

    // then
    assertThat(result).isEqualTo(entity);
    verify(bookJpaRepository).findByIsbn("9781492034025");
  }

  // --- getBookDetailView ---

  @Test
  void getBookDetailView_shouldDelegateToRepository() {
    // given
    BookDetailView detailView =
        new BookDetailView(1L, "Test Book", "Author One", "A1", "Shelf 1", "AVAILABLE");
    when(bookJpaRepository.getBookDetailView(1L)).thenReturn(detailView);

    // when
    BookDetailView result = bookDomainRepositoryImpl.getBookDetailView(1L);

    // then
    assertThat(result).isEqualTo(detailView);
    assertThat(result.title()).isEqualTo("Test Book");
    verify(bookJpaRepository).getBookDetailView(1L);
  }

  // --- createBookFromMetaData ---

  @Test
  void createBookFromMetaData_shouldMapAndSaveEntity() {
    // given
    BookMetaDataResponse metaData =
        new BookMetaDataResponse(
            null,
            "Meta Book",
            "9781234567890",
            List.of("John Doe"),
            "Meta Publisher",
            Optional.of("A great book"));
    List<Long> authorIds = List.of(1L, 2L);
    String isbn = "9781234567890";
    Long shelfId = 5L;
    BookEntity mappedEntity = createTestBookEntity();

    when(bookMapper.toEntityFromBookMetaDataResponse(metaData, authorIds, isbn, shelfId))
        .thenReturn(mappedEntity);

    // when
    bookDomainRepositoryImpl.createBookFromMetaData(metaData, authorIds, isbn, shelfId);

    // then
    verify(bookMapper).toEntityFromBookMetaDataResponse(metaData, authorIds, isbn, shelfId);
    verify(bookJpaRepository).save(mappedEntity);
  }

  // --- getThreeBooksByAuthorId ---

  @Test
  void getThreeBooksByAuthorId_shouldDelegateToRepository() {
    // given
    BookEntity entity1 = createTestBookEntity();
    BookEntity entity2 = createTestBookEntity();
    entity2.setBookId(2L);
    List<BookEntity> entities = List.of(entity1, entity2);
    when(bookJpaRepository.findByAuthorsAuthorId(1L)).thenReturn(entities);

    // when
    List<BookEntity> result = bookDomainRepositoryImpl.getThreeBooksByAuthorId(1L);

    // then
    assertThat(result).hasSize(2);
    verify(bookJpaRepository).findByAuthorsAuthorId(1L);
  }

  // --- updatePublisher ---

  @Test
  void updatePublisher_shouldUpdatePublisherAndSave() {
    // given
    BookEntity entity = createTestBookEntity();
    when(bookJpaRepository.findByIsbn("9781492034025")).thenReturn(entity);

    // when
    bookDomainRepositoryImpl.updatePublisher("9781492034025", "New Publisher");

    // then
    assertThat(entity.getPublisher()).isEqualTo("New Publisher");
    assertThat(entity.getUpdatedAt()).isEqualTo(LocalDate.now());
    verify(bookJpaRepository).save(entity);
  }

  @Test
  void updatePublisher_shouldThrowExceptionWhenBookNotFound() {
    // given
    when(bookJpaRepository.findByIsbn("0000000000000")).thenReturn(null);

    // when / then
    assertThatThrownBy(() -> bookDomainRepositoryImpl.updatePublisher("0000000000000", "Publisher"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Book not found with ISBN: 0000000000000");
  }

  // --- deleteByShelfId ---

  @Test
  void deleteByShelfId_shouldDeleteBooksMatchingShelfIds() {
    // given
    List<Long> shelfIds = List.of(1L, 2L, 3L);
    BookEntity book1 = createTestBookEntity();
    book1.setShelfId(1L);
    BookEntity book2 = createTestBookEntity();
    book2.setBookId(2L);
    book2.setShelfId(2L);
    List<BookEntity> booksToDelete = List.of(book1, book2);
    when(bookJpaRepository.findByShelfIdIn(shelfIds)).thenReturn(booksToDelete);

    // when
    bookDomainRepositoryImpl.deleteByShelfId(shelfIds);

    // then
    verify(bookJpaRepository).findByShelfIdIn(shelfIds);
    verify(bookJpaRepository).deleteAll(booksToDelete);
  }

  @Test
  void deleteByShelfId_shouldHandleEmptyResult() {
    // given
    List<Long> shelfIds = List.of(99L);
    when(bookJpaRepository.findByShelfIdIn(shelfIds)).thenReturn(List.of());

    // when
    bookDomainRepositoryImpl.deleteByShelfId(shelfIds);

    // then
    verify(bookJpaRepository).findByShelfIdIn(shelfIds);
    verify(bookJpaRepository).deleteAll(List.of());
  }

  // --- getBookDomainById ---

  @Test
  void getBookDomainById_shouldReturnMappedDomainModel() {
    // given
    BookEntity entity = createTestBookEntity();
    Book expectedBook = createTestBook();
    when(bookJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
    when(bookMapper.toDomainFromEntity(entity)).thenReturn(expectedBook);

    // when
    Book result = bookDomainRepositoryImpl.getBookDomainById(1L);

    // then
    assertThat(result).isEqualTo(expectedBook);
    verify(bookJpaRepository).findById(1L);
    verify(bookMapper).toDomainFromEntity(entity);
  }

  @Test
  void getBookDomainById_shouldThrowExceptionWhenNotFound() {
    // given
    when(bookJpaRepository.findById(99L)).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> bookDomainRepositoryImpl.getBookDomainById(99L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Book not found with id: 99");
  }

  // --- placeBookOnShelf ---

  @Test
  void placeBookOnShelf_shouldUpdateShelfIdAndReturnDomainModel() {
    // given
    BookEntity entity = createTestBookEntity();
    Book expectedBook = createTestBook();
    when(bookJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
    when(bookMapper.toDomainFromEntity(entity)).thenReturn(expectedBook);

    // when
    Book result = bookDomainRepositoryImpl.placeBookOnShelf(1L, 10L);

    // then
    assertThat(entity.getShelfId()).isEqualTo(10L);
    assertThat(result).isEqualTo(expectedBook);
    verify(bookJpaRepository).save(entity);
    verify(bookMapper).toDomainFromEntity(entity);
  }

  @Test
  void placeBookOnShelf_shouldThrowExceptionWhenBookNotFound() {
    // given
    when(bookJpaRepository.findById(99L)).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> bookDomainRepositoryImpl.placeBookOnShelf(99L, 10L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Book not found with id: 99");
  }

  // --- getBookIdsByShelfId ---

  @Test
  void getBookIdsByShelfId_shouldReturnListOfBookIds() {
    // given
    BookEntity entity1 = createTestBookEntity();
    entity1.setBookId(1L);
    BookEntity entity2 = createTestBookEntity();
    entity2.setBookId(2L);
    BookEntity entity3 = createTestBookEntity();
    entity3.setBookId(3L);
    when(bookJpaRepository.findByShelfId(5L)).thenReturn(List.of(entity1, entity2, entity3));

    // when
    List<Long> result = bookDomainRepositoryImpl.getBookIdsByShelfId(5L);

    // then
    assertThat(result).containsExactly(1L, 2L, 3L);
    verify(bookJpaRepository).findByShelfId(5L);
  }

  @Test
  void getBookIdsByShelfId_shouldReturnEmptyListWhenNoBooksFound() {
    // given
    when(bookJpaRepository.findByShelfId(99L)).thenReturn(List.of());

    // when
    List<Long> result = bookDomainRepositoryImpl.getBookIdsByShelfId(99L);

    // then
    assertThat(result).isEmpty();
  }
}
