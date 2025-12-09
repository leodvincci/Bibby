package com.penrose.bibby.library.book.core.application;

import com.penrose.bibby.library.book.contracts.dtos.*;
import com.penrose.bibby.library.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.book.contracts.ports.outbound.AuthorAccessPort;
import com.penrose.bibby.library.book.core.domain.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;

import com.penrose.bibby.library.shelf.contracts.dtos.ShelfDTO;

import com.penrose.bibby.library.shelf.core.application.ShelfService;

import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.book.infrastructure.repository.BookJpaRepository;

import java.time.LocalDate;
import java.util.*;

@Service
    public class BookService implements BookFacade {
    private final ShelfService shelfService;
    private final BookJpaRepository bookJpaRepository;
    private final AuthorAccessPort authorAccessPort;

    private final BookFactory BookFactory;
    private final BookMapper bookMapper;
    private final IsbnLookupService isbnLookupService;
    private final IsbnEnrichmentService isbnEnrichmentService;
    private final BookDomainRepository bookDomainRepository;
    Logger logger = org.slf4j.LoggerFactory.getLogger(BookService.class);


    public BookService(IsbnEnrichmentService isbnEnrichmentService, BookJpaRepository bookJpaRepository, BookFactory bookFactory, ShelfService shelfService, BookMapper bookMapper, IsbnLookupService isbnLookupService, AuthorAccessPort authorAccessPort, BookDomainRepository bookDomainRepository){
        this.isbnEnrichmentService = isbnEnrichmentService;
        this.bookJpaRepository = bookJpaRepository;
        this.BookFactory = bookFactory;
        this.shelfService = shelfService;
        this.bookMapper = bookMapper;
        this.isbnLookupService = isbnLookupService;
        this.authorAccessPort = authorAccessPort;
        this.bookDomainRepository = bookDomainRepository;
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
//        validateRequest(bookDTO);
//        validateBookDoesNotExist(bookDTO);

        Book book = new Book();
        book.setTitle(new Title(bookDTO.title()));
        List<AuthorRef> authorRefs = new ArrayList<>();
        for(AuthorDTO authorDTO : bookDTO.authors()){
            AuthorRef authorRef = new AuthorRef(null, new AuthorName(authorDTO.firstName(), authorDTO.lastName()));
            authorRefs.add(authorRef);
        }
        book.setIsbn(new Isbn(bookDTO.isbn()));
        registerBook(book);
        logger.info("Created new book: {}", bookDTO.title());
    }

// I think I can get rid of this method

//    public BookDTO createBookFromScan(GoogleBooksResponse googleBooksResponse, String isbn, Long shelfId){
//        BookEntity bookEntity = new BookEntity();
//        List<String> authors = new ArrayList<>();
//        Set<AuthorEntity> authorEntities = new HashSet<>();
//        for(String authorName : googleBooksResponse.items().get(0).volumeInfo().authors()) {
//            String [] nameParts = authorName.split(" ", 2);
//            AuthorRef author = authorAccessPort.findOrCreateAuthor(nameParts[0],nameParts[1]);
//            authors.add(nameParts[0] + " " + nameParts[1]);
//            authorEntities.add(AuthorDTO.AuthorRefToEntity(author));
//        }
//
//        BookDTO bookDTO = new BookDTO(null,
//                0,
//                googleBooksResponse.items().get(0).volumeInfo().title(),
//                authors,
//                isbn,
//                null,
//                googleBooksResponse.items().get(0).volumeInfo().publisher(),
//                Integer.parseInt(googleBooksResponse.items().get(0).volumeInfo().publishedDate().split("-")[0]),
//                shelfId,
//                googleBooksResponse.items().get(0).volumeInfo().description(),
//                AvailabilityStatus.AVAILABLE,
//                LocalDate.now(),
//                LocalDate.now(),
//                null
//        );
//
//        bookEntity = bookMapper.toEntityFromDTO(bookDTO,authorEntities);
//        saveBook(bookEntity);
//        return bookDTO;
//    }

    private Set<AuthorEntity> extractAuthorEntities(BookRequestDTO bookRequestDTO){
        Set<AuthorEntity> authorEntities = new HashSet<>();
        for(AuthorDTO author : bookRequestDTO.authors()){
            authorEntities.add(AuthorDTO.AuthorRefToEntity(authorAccessPort.findOrCreateAuthor(author.firstName(),author.lastName())));
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
        BookEntity bookEntity = bookJpaRepository.findById(bookId).orElse(null);
        return Optional.of(BookDTO.fromEntity(bookEntity));
    }

    public List<BookEntity> findBooksByShelf(Long id){
        return bookJpaRepository.findByShelfId(id);
    }

    public Optional<BookDTO> findBookByTitleIgnoreCase(String title){
        Optional<BookEntity> bookEntity = bookJpaRepository.findByTitleIgnoreCase(title);
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
        Optional<BookEntity> bookEntity = bookJpaRepository.findByTitleIgnoreCase(title);
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
            AuthorEntity authorEntity = AuthorDTO.AuthorRefToEntity(authorAccessPort.findOrCreateAuthor(nameParts[0],nameParts[1]));
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
        Book book = bookMapper.toDomainFromEntity(bookEntity);
        registerBook(book);

    }

    public List<BookEntity> findBookByKeyword(String keyword){
        List<BookEntity> bookEntities = bookJpaRepository.findByTitleContaining(keyword);
        for(BookEntity book : bookEntities){
            System.out.println(book.getTitle());
        }
        return bookEntities;
    }

    public List<BookSummary> getBooksForShelf(Long shelfId){
        return bookJpaRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
    }

    public BookDetailView getBookDetails(Long bookId){
        return bookJpaRepository.getBookDetailView(bookId);
    }


    // ============================================================
    // UPDATE Operations
    // ============================================================



    public void registerBook(Book book){
        bookDomainRepository.registerBook(book);
    }



    public BookEntity assignBookToShelf(Long bookId, Long shelfId) {
        BookEntity bookEntity = bookJpaRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
        ShelfDTO shelf = shelfService.findShelfById(shelfId)
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found: " + shelfId));

        long bookCount = bookJpaRepository.countByShelfId(shelfId);
        if (bookCount >= shelf.bookCapacity()) {
            throw new IllegalStateException("Shelf is full");
        }

        bookEntity.setShelfId(shelfId);
        Book book = bookMapper.toDomainFromEntity(bookEntity);
        registerBook(book);
        return bookEntity;
    }


    public void checkOutBook(BookDTO bookDTO){
        Set<AuthorDTO> authorEntities = authorAccessPort.findByBookId(bookDTO.id());

        // Create domain object for business logic validation
        Book book = bookMapper(bookDTO, new HashSet<>(authorEntities));
        book.checkout(); // This validates and updates the domain object status

        // Update the entity directly instead of converting back
        book.setAvailabilityStatus(book.getAvailabilityStatus());
        BookEntity bookEntity = bookMapper.toEntity(book);
        registerBook(book);
    }


    public Book bookMapper(BookDTO bookDTO, Set<AuthorDTO> authorDTOs){
        Optional<ShelfDTO> shelfEntity = shelfService.findShelfById(bookDTO.shelfId());
        return bookMapper.toDomain(bookDTO, authorDTOs, shelfEntity.orElse(null));
    }


    // todo: use bookDomainRepository instead of bookJpaRepository directly
    public void checkInBook(String bookTitle){
        BookEntity bookEntity = bookJpaRepository.findBookEntityByTitle(bookTitle);
        bookEntity.checkIn();
        Book book = bookMapper.toDomainFromEntity(bookEntity);
        registerBook(book);
    }

    public BookDTO findBookByIsbn(String isbn) {
        System.out.println("isbn in service: " + isbn);
        BookEntity bookEntity = bookJpaRepository.findByIsbn(isbn);
        if(bookEntity == null){
            throw new IllegalArgumentException("Book not found with ISBN: " + isbn);
        }
        return bookMapper.toDTOfromEntity(bookEntity);
    }

    @Override
    public void setShelfForBook(Long id, Long shelfId) {
        BookEntity bookEntity = bookJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
        bookEntity.setShelfId(shelfId);
        Book book = bookMapper.toDomainFromEntity(bookEntity);
        registerBook(book);
    }
}




