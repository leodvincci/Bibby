package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorService;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final BookFactory BookFactory;
    private final ShelfService shelfService;

    public BookService(BookRepository bookRepository, AuthorService authorService, BookFactory bookFactory, ShelfService shelfService){
        this.bookRepository = bookRepository;
        this.authorService = authorService;
        this.BookFactory = bookFactory;
        this.shelfService = shelfService;
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
        saveBook(BookFactory.createBook(bookDTO.title(), extractAuthorEntities(bookDTO)));
    }

    private Set<AuthorEntity> extractAuthorEntities(BookRequestDTO bookRequestDTO){
        Set<AuthorEntity> authorEntities = new HashSet<>();
        for(Author author : bookRequestDTO.authors()){
            authorEntities.add(authorService.findOrCreateAuthor(author.getFirstName(),author.getLastName()));
        }
        return authorEntities;
    }


    private void validateBookDoesNotExist(BookRequestDTO bookDTO){
        Optional<BookEntity> bookEntity = findBookByTitleIgnoreCase(bookDTO.title());
        bookEntity.ifPresent(existingBook -> {
            throw new IllegalArgumentException("Book Already Exists: " + existingBook.getTitle());
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
        for (Author author : bookDTO.authors()) {
            if (author == null) {
                throw new IllegalArgumentException("Author cannot be null");
            }
            if (author.getFirstName() == null || author.getFirstName().isBlank()) {
                throw new IllegalArgumentException("Author first name cannot be blank");
            }
            if (author.getLastName() == null || author.getLastName().isBlank()) {
                throw new IllegalArgumentException("Author last name cannot be blank");
            }
        }
    }


    // ============================================================
    //      READ Operations
    // ============================================================
    public Optional<BookEntity> findBookById(Long bookId){
        return bookRepository.findById(bookId);
    }

    public List<BookEntity> findBooksByShelf(Long id){
        return bookRepository.findByShelfId(id);
    }

    public Optional<BookEntity> findBookByTitleIgnoreCase(String title){
        return bookRepository.findByTitleIgnoreCase(title);
    }

    /**
     * Retrieves a book entity based on the given title. The search is case-insensitive.
     * If no book with the specified title is found, the method returns null.
     *
     * @param title the title of the book to search for
     * @return the book entity with the specified title, or null if no such book exists
     */
    public BookEntity findBookByTitle(String title){
        Optional<BookEntity> bookEntity = bookRepository.findByTitleIgnoreCase(title);
        return bookEntity.orElse(null);
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


    public void checkOutBook(BookEntity bookEntity){
        List<AuthorEntity> authorEntities = authorService.findByBookId(bookEntity.getBookId());

        // Create domain object for business logic validation
        Book book = bookMapper(bookEntity, new HashSet<>(authorEntities));
        book.checkout(); // This validates and updates the domain object status

        // Update the entity directly instead of converting back
        bookEntity.setAvailabilityStatus(book.getAvailabilityStatus().name());
        saveBook(bookEntity);
    }


    public Book bookMapper(BookEntity bookEntity, Set<AuthorEntity> authorEntities){
        Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
        return BookMapper.toDomain(bookEntity, authorEntities, shelfEntity.orElse(null));
    }


    public void checkInBook(String bookTitle){
        BookEntity bookEntity = findBookByTitle(bookTitle);
        bookEntity.checkIn();
        saveBook(bookEntity);
    }

}





