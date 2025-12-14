package com.penrose.bibby.library.catalog.book.core.application;

import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.catalog.book.contracts.dtos.BookRequestDTO;
import com.penrose.bibby.library.catalog.book.contracts.dtos.BookSummary;
import com.penrose.bibby.library.catalog.book.contracts.ports.outbound.ShelfAccessPort;
import com.penrose.bibby.library.book.contracts.dtos.*;
import com.penrose.bibby.library.catalog.book.contracts.ports.outbound.AuthorAccessPort;
import com.penrose.bibby.library.book.core.domain.*;
import com.penrose.bibby.library.catalog.book.core.domain.Book;
import com.penrose.bibby.library.catalog.book.core.domain.BookDomainRepository;
import com.penrose.bibby.library.catalog.book.core.domain.BookFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;

import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;


import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.catalog.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.catalog.book.infrastructure.repository.BookJpaRepository;

import java.util.*;

@Service
    public class BookService {
    private final BookJpaRepository bookJpaRepository;
    private final AuthorAccessPort authorAccessPort;

    private final BookFactory BookFactory;
    private final BookMapper bookMapper;
    private final IsbnLookupService isbnLookupService;
    private final IsbnEnrichmentService isbnEnrichmentService;
    private final BookDomainRepository bookDomainRepository;
    private final ShelfAccessPort shelfAccessPort;
    Logger logger = org.slf4j.LoggerFactory.getLogger(BookService.class);


    public BookService(IsbnEnrichmentService isbnEnrichmentService, BookJpaRepository bookJpaRepository, BookFactory bookFactory, BookMapper bookMapper, IsbnLookupService isbnLookupService, AuthorAccessPort authorAccessPort, BookDomainRepository bookDomainRepository, ShelfAccessPort shelfAccessPort){
        this.isbnEnrichmentService = isbnEnrichmentService;
        this.bookJpaRepository = bookJpaRepository;
        this.BookFactory = bookFactory;
        this.bookMapper = bookMapper;
        this.isbnLookupService = isbnLookupService;
        this.authorAccessPort = authorAccessPort;
        this.bookDomainRepository = bookDomainRepository;
        this.shelfAccessPort = shelfAccessPort;



    }

    // ============================================================
    //      CREATE Operations
    // ============================================================
//
//    /**
//     * Creates a new book entry in the system. Validates that the book does not already
//     * exist, extracts associated author entities, and persists the book to storage.
//     *
//     * @param bookDTO the data transfer object containing information about the
//     *                       book to be created, including its title and list of authors
//     */
//    @Transactional
//    public void createNewBook(BookRequestDTO bookDTO){
//        bookDomainRepository.registerBook(bookDTO);
//        logger.info("Created new book: {}", bookDTO.title());
//    }

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

//    private Set<AuthorEntity> extractAuthorEntities(BookRequestDTO bookRequestDTO){
//        Set<AuthorEntity> authorEntities = new HashSet<>();
//        for(AuthorDTO author : bookRequestDTO.authors()){
//            authorEntities.add(AuthorDTO.AuthorRefToEntity(authorAccessPort.findOrCreateAuthor(author.firstName(),author.lastName())));
//        }
//        return authorEntities;
//    }


    private void validateBookDoesNotExist(BookRequestDTO bookDTO){
        Optional<BookDTO> bookEntity = findBookByTitleIgnoreCase(bookDTO.title());
        bookEntity.ifPresent(existingBook -> {
            throw new IllegalArgumentException("Book Already Exists: " + existingBook.title());
        });

    }

//    private void validateRequest(BookRequestDTO bookDTO) {
//        if (bookDTO == null) {
//            throw new IllegalArgumentException("Book request cannot be null");
//        }
//        if (bookDTO.title() == null || bookDTO.title().isBlank()) {
//            throw new IllegalArgumentException("Book title cannot be blank");
//        }
//        if (bookDTO.authors() == null || bookDTO.authors().isEmpty()) {
//            throw new IllegalArgumentException("Book must have at least one author");
//        }
//
//        // Validate each author
//        for (AuthorDTO author : bookDTO.authors()) {
//            if (author == null) {
//                throw new IllegalArgumentException("Author cannot be null");
//            }
//            if (author.firstName() == null || author.firstName().isBlank()) {
//                throw new IllegalArgumentException("Author first name cannot be blank");
//            }
//            if (author.lastName() == null || author.lastName().isBlank()) {
//                throw new IllegalArgumentException("Author last name cannot be blank");
//            }
//        }
//    }


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
        if(bookEntity.isEmpty()){
            return null;
        }
        return bookMapper.toDTOfromEntity(bookEntity.orElse(null));

    }

//    @Override
//    public BookMetaDataResponse findBookMetaDataByIsbn(String isbn) {
//        GoogleBooksResponse googleBooksResponse = isbnLookupService.lookupBook(isbn).block();
//        if (googleBooksResponse == null || googleBooksResponse.items() == null || googleBooksResponse.items().isEmpty()) {
//            throw new IllegalArgumentException("No book data found for ISBN: " + isbn);
//        }
//        BookEntity bookEntity = isbnEnrichmentService.enrichIsbn(googleBooksResponse, isbn);
//
//        List<String> authors = bookEntity.getAuthors().stream()
//                .map(author -> author.getFirstName() + " " + author.getLastName())
//                .toList();
//
//        return new BookMetaDataResponse(
//                bookEntity.getBookId(),
//                bookEntity.getTitle(),
//                bookEntity.getIsbn(),
//                authors,
//                bookEntity.getPublisher(),
//                bookEntity.getDescription()
//        );
//    }

//    @Override
//    public void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId) {
//
////        Book book = bookMapper.toDomainFromMetaData(bookMetaDataResponse, isbn, shelfId);
////
////        bookEntity.setTitle(bookMetaDataResponse.title());
////        bookEntity.setIsbn(isbn);
////        bookEntity.setPublisher(bookMetaDataResponse.publisher());
////        bookEntity.setDescription(bookMetaDataResponse.description());
////        bookEntity.setShelfId(shelfId);
////        bookEntity.setAuthors(authorEntities);
////        bookEntity.setCreatedAt(LocalDate.now());
////        bookEntity.setUpdatedAt(LocalDate.now());
////        bookEntity.setAvailabilityStatus(AvailabilityStatus.AVAILABLE.toString());
////        Book book = bookMapper.toDomainFromEntity(bookEntity);
////        registerBook(book);
//        bookDomainRepository.registerBookFromMetaData(bookMetaDataResponse, isbn, shelfId);;
//
//    }

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

//    public BookDetailView getBookDetails(Long bookId){
//        return bookJpaRepository.getBookDetailView(bookId);
//    }


    // ============================================================
    // UPDATE Operations
    // ============================================================




    public BookEntity assignBookToShelf(Long bookId, Long shelfId) {
        BookEntity bookEntity = bookJpaRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
        ShelfDTO shelf = shelfAccessPort.findShelfById(shelfId)
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found: " + shelfId));

        long bookCount = bookJpaRepository.countByShelfId(shelfId);
        if (bookCount >= shelf.bookCapacity()) {
            throw new IllegalStateException("Shelf is full");
        }

        bookEntity.setShelfId(shelfId);
        Book book = bookMapper.toDomainFromEntity(bookEntity);
        bookDomainRepository.updateBook(book);
        return bookEntity;
    }


    public void checkOutBook(BookDTO bookDTO){
//        Set<AuthorDTO> authorEntities = authorAccessPort.findByBookId(bookDTO.id());
//
//        // Create domain object for business logic validation
//        Book book = bookMapper(bookDTO, new HashSet<>(authorEntities));
//        book.checkout(); // This validates and updates the domain object status
//
//        // Update the entity directly instead of converting back
//        book.setAvailabilityStatus(book.getAvailabilityStatus());
//        BookEntity bookEntity = bookMapper.toEntity(book);
//        registerBook(book);
        bookDomainRepository.updateAvailabilityStatus(bookDTO.title());
    }


    public Book bookMapper(BookDTO bookDTO, Set<AuthorDTO> authorDTOs){
        Optional<ShelfDTO> shelfEntity = shelfAccessPort.findShelfById(bookDTO.shelfId());
        return bookMapper.toDomain(bookDTO, authorDTOs, shelfEntity.orElse(null));
    }


    public void checkInBook(String bookTitle){
        bookDomainRepository.updateAvailabilityStatus(bookTitle);
//        BookEntity bookEntity = bookDomainRepository.findBookEntityByTitle(bookTitle);
//        bookEntity.checkIn();
//        Book book = bookMapper.toDomainFromEntity(bookEntity);
//        registerBook(book);
    }

    public BookDTO findBookByIsbn(String isbn) {
        BookEntity bookEntity = bookJpaRepository.findByIsbn(isbn);
        if(bookEntity == null){
            throw new IllegalArgumentException("Book not found with ISBN: " + isbn);
        }
        return bookMapper.toDTOfromEntity(bookEntity);
    }


}




