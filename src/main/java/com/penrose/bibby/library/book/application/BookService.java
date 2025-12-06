package com.penrose.bibby.library.book.application;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.author.application.AuthorService;
import com.penrose.bibby.library.book.contracts.*;
import com.penrose.bibby.library.book.domain.AvailabilityStatus;
import com.penrose.bibby.library.book.domain.Book;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.book.domain.BookFactory;
import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.book.infrastructure.repository.BookRepository;
import com.penrose.bibby.library.shelf.contracts.ShelfDTO;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.shelf.application.ShelfService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
    public class BookService implements BookFacade {
    private final IsbnEnrichmentService isbnEnrichmentService;
    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final BookFactory BookFactory;
    private final ShelfService shelfService;
    private final BookMapper bookMapper;
    private final IsbnLookupService isbnLookupService;

    public BookService(IsbnEnrichmentService isbnEnrichmentService, BookRepository bookRepository, AuthorService authorService, BookFactory bookFactory, ShelfService shelfService, BookMapper bookMapper, IsbnLookupService isbnLookupService){
        this.isbnEnrichmentService = isbnEnrichmentService;
        this.bookRepository = bookRepository;
        this.authorService = authorService;
        this.BookFactory = bookFactory;
        this.shelfService = shelfService;
        this.bookMapper = bookMapper;
        this.isbnLookupService = isbnLookupService;
    }

    // ============================================================
    //      CREATE Operations
    // ============================================================

    /**
     * Creates a new book entry in the system. Validates that the book does not already
     * exist, extracts associated author entities, and persists the book to storage.
     *
     * @param bookDTO the data transfer object containing information about the
     *                       book to be created, including its title and list of authors
     */
    @Transactional
    public void createNewBook(BookRequestDTO bookDTO){
        validateRequest(bookDTO);
        validateBookDoesNotExist(bookDTO);
        saveBook(BookFactory.createBookEntity(bookDTO.title(), extractAuthorEntities(bookDTO)));
    }

    public BookDTO createScannedBook(GoogleBooksResponse googleBooksResponse, String isbn, Long shelfId){
        BookEntity bookEntity = new BookEntity();
        Set<Long> authorIds = new HashSet<>();
        Set<AuthorEntity> authorEntities = new HashSet<>();
        for(String authorName : googleBooksResponse.items().get(0).volumeInfo().authors()) {
            String [] nameParts = authorName.split(" ", 2);
            AuthorEntity authorEntity = authorService.findOrCreateAuthor(nameParts[0],nameParts[1]);
            authorIds.add(authorEntity.getAuthorId());
            authorEntities.add(authorEntity);
        }

        BookDTO bookDTO = new BookDTO(null,
                0,
                googleBooksResponse.items().get(0).volumeInfo().title(),
                authorIds,
                isbn,
                null,
                googleBooksResponse.items().get(0).volumeInfo().publisher(),
                Integer.parseInt(googleBooksResponse.items().get(0).volumeInfo().publishedDate().split("-")[0]),
                shelfId,
                googleBooksResponse.items().get(0).volumeInfo().description(),
                AvailabilityStatus.AVAILABLE,
                LocalDate.now(),
                LocalDate.now(),
                null
        );

        bookEntity = bookMapper.toEntityFromDTO(bookDTO,authorEntities);
        saveBook(bookEntity);
        return bookDTO;
    }

    private Set<AuthorEntity> extractAuthorEntities(BookRequestDTO bookRequestDTO){
        Set<AuthorEntity> authorEntities = new HashSet<>();
        for(AuthorDTO author : bookRequestDTO.authors()){
            authorEntities.add(authorService.findOrCreateAuthor(author.firstName(),author.lastName()));
        }
        return authorEntities;
    }


    private void validateBookDoesNotExist(BookRequestDTO bookDTO){
        Optional<BookDTO> bookEntity = findBookByTitleIgnoreCase(bookDTO.title());
        bookEntity.ifPresent(existingBook -> {
            throw new IllegalArgumentException("Book Already Exists: " + existingBook.title());
        });

    }

    private void validateRequest(BookRequestDTO bookDTO) {
        if (bookDTO == null) {
            throw new IllegalArgumentException("Book request cannot be null");
        }
        if (bookDTO.title() == null || bookDTO.title().isBlank()) {
            throw new IllegalArgumentException("Book title cannot be blank");
        }
        if (bookDTO.authors() == null || bookDTO.authors().isEmpty()) {
            throw new IllegalArgumentException("Book must have at least one author");
        }

        // Validate each author
        for (AuthorDTO author : bookDTO.authors()) {
            if (author == null) {
                throw new IllegalArgumentException("Author cannot be null");
            }
            if (author.firstName() == null || author.firstName().isBlank()) {
                throw new IllegalArgumentException("Author first name cannot be blank");
            }
            if (author.lastName() == null || author.lastName().isBlank()) {
                throw new IllegalArgumentException("Author last name cannot be blank");
            }
        }
    }


    // ============================================================
    //      READ Operations
    // ============================================================
    public Optional<BookDTO> findBookById(Long bookId){
        BookEntity bookEntity = bookRepository.findById(bookId).orElse(null);
        return Optional.of(BookDTO.fromEntity(bookEntity));
    }

    public List<BookEntity> findBooksByShelf(Long id){
        return bookRepository.findByShelfId(id);
    }

    public Optional<BookDTO> findBookByTitleIgnoreCase(String title){
        Optional<BookEntity> bookEntity = bookRepository.findByTitleIgnoreCase(title);
        return Optional.of(BookDTO.fromEntity(bookEntity.orElse(null)));
    }

    /**
     * Retrieves a book entity based on the given title. The search is case-insensitive.
     * If no book with the specified title is found, the method returns null.
     *
     * @param title the title of the book to search for
     * @return the book entity with the specified title, or null if no such book exists
     */
    public BookDTO findBookByTitle(String title){
        Optional<BookEntity> bookEntity = bookRepository.findByTitleIgnoreCase(title);
        return bookMapper.toDTOfromEntity(bookEntity.orElse(null));

    }

    @Override
    public BookMetaDataResponse findBookMetaDataByIsbn(String isbn) {
        GoogleBooksResponse googleBooksResponse = isbnLookupService.lookupBook(isbn).block();
        if (googleBooksResponse == null || googleBooksResponse.items() == null || googleBooksResponse.items().isEmpty()) {
            throw new IllegalArgumentException("No book data found for ISBN: " + isbn);
        }
        BookEntity bookEntity = isbnEnrichmentService.enrichIsbn(googleBooksResponse, isbn);

        List<String> authors = bookEntity.getAuthors().stream()
                .map(author -> author.getFirstName() + " " + author.getLastName())
                .toList();

        return new BookMetaDataResponse(
                bookEntity.getBookId(),
                bookEntity.getTitle(),
                bookEntity.getIsbn(),
                authors,
                bookEntity.getPublisher(),
                bookEntity.getDescription()
        );
    }

    @Override
    public void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId) {
        BookEntity bookEntity = new BookEntity();
        Set<Long> authorIds = new HashSet<>();
        Set<AuthorEntity> authorEntities = new HashSet<>();
        for(String authorName : bookMetaDataResponse.authors()) {
            String [] nameParts = authorName.split(" ", 2);
            AuthorEntity authorEntity = authorService.findOrCreateAuthor(nameParts[0],nameParts[1]);
            authorIds.add(authorEntity.getAuthorId());
            authorEntities.add(authorEntity);
        }


        bookEntity.setTitle(bookMetaDataResponse.title());
        bookEntity.setIsbn(isbn);
        bookEntity.setPublisher(bookMetaDataResponse.publisher());
        bookEntity.setDescription(bookMetaDataResponse.description());
        bookEntity.setShelfId(shelfId);
        bookEntity.setAuthors(authorEntities);
        bookEntity.setCreatedAt(LocalDate.now());
        bookEntity.setUpdatedAt(LocalDate.now());
        bookEntity.setAvailabilityStatus(AvailabilityStatus.AVAILABLE.toString());
        saveBook(bookEntity);

    }

    public List<BookEntity> findBookByKeyword(String keyword){
        List<BookEntity> bookEntities = bookRepository.findByTitleContaining(keyword);
        for(BookEntity book : bookEntities){
            System.out.println(book.getTitle());
        }
        return bookEntities;
    }

    public List<BookSummary> getBooksForShelf(Long shelfId){
        return bookRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
    }

    public BookDetailView getBookDetails(Long bookId){
        return bookRepository.getBookDetailView(bookId);
    }


    // ============================================================
    // UPDATE Operations
    // ============================================================
    public void saveBook(BookEntity bookEntity){
        bookRepository.save(bookEntity);
    }

    public BookEntity assignBookToShelf(Long bookId, Long shelfId) {
        BookEntity bookEntity = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
        ShelfDTO shelf = shelfService.findShelfById(shelfId)
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found: " + shelfId));

        long bookCount = bookRepository.countByShelfId(shelfId);
        if (bookCount >= shelf.bookCapacity()) {
            throw new IllegalStateException("Shelf is full");
        }

        bookEntity.setShelfId(shelfId);
        saveBook(bookEntity);
        return bookEntity;
    }


    public void checkOutBook(BookDTO bookDTO){
        Set<AuthorDTO> authorEntities = authorService.findByBookId(bookDTO.id());

        // Create domain object for business logic validation
        Book book = bookMapper(bookDTO, new HashSet<>(authorEntities));
        book.checkout(); // This validates and updates the domain object status

        // Update the entity directly instead of converting back
        book.setAvailabilityStatus(book.getAvailabilityStatus());
        BookEntity bookEntity = bookMapper.toEntity(book);
        saveBook(bookEntity);
    }


    public Book bookMapper(BookDTO bookDTO, Set<AuthorDTO> authorDTOs){
        Optional<ShelfDTO> shelfEntity = shelfService.findShelfById(bookDTO.shelfId());
        return bookMapper.toDomain(bookDTO, authorDTOs, shelfEntity.orElse(null));
    }


    public void checkInBook(String bookTitle){
        BookEntity bookEntity = bookRepository.findBookEntityByTitle(bookTitle);
        bookEntity.checkIn();
        saveBook(bookEntity);
    }

    public BookDTO findBookByIsbn(String isbn) {
        BookEntity bookEntity = bookRepository.findByIsbn(isbn);
        return bookMapper.toDTOfromEntity(bookEntity);
    }

    @Override
    public void setShelfForBook(Long id, Long shelfId) {
        BookEntity bookEntity = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
        bookEntity.setShelfId(shelfId);
        saveBook(bookEntity);
    }
}




