package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorService;
import com.penrose.bibby.library.shelf.Shelf;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * Creates a new book with the specified details. If a book with the same title already exists,
     * an IllegalArgumentException is thrown. The method also ensures that the author of the book
     * is retrieved or created and associates the book with the author.
     *
     * @param bookRequestDTO the data transfer object containing the title of the book, the author's first name,
     *                       and the author's last name
     * @throws IllegalArgumentException if a book with the specified title already exists
     */
    @Transactional
    public void createNewBook(BookRequestDTO bookRequestDTO) {
        Optional<BookEntity> bookEntity = findBookByTitleIgnoreCase(bookRequestDTO.title());
        System.out.println("DEBUG: " + bookEntity.isPresent() + "?");
        if (bookEntity.isPresent()) {
            throw new IllegalArgumentException("Book Already Exists: " + bookRequestDTO.title());
        }
        Set<AuthorEntity> authorEntities = new HashSet<>();
        for(Author author : bookRequestDTO.authors()){
            authorEntities.add(authorService.findOrCreateAuthor(author.getFirstName(),author.getLastName()));
        }
        saveBook(BookFactory.createBook(bookRequestDTO.title(), authorEntities));
    }


    // ============================================================
    //      READ Operations
    // ============================================================
    public Optional<BookEntity> findBookById(Long bookId) {
        return bookRepository.findById(bookId);
    }

    public List<BookEntity> findBooksByShelf(Long id) {
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

    public List<BookSummary> getBooksForShelf(Long shelfId) {
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
//        List<AuthorEntity> authorEntities = authorService.findByBookId(bookEntity.getBookId());

        Book book = bookMapper(bookEntity, (HashSet<AuthorEntity>) bookEntity.getAuthors());
        book.checkout();
        if(!bookEntity.getBookStatus().equals(AvailabilityStatus.CHECKED_OUT.toString())){
            bookEntity.setBookStatus("CHECKED_OUT");
            saveBook(bookEntity);
        }
    }

    public Book bookMapper(BookEntity bookEntity, HashSet<AuthorEntity> authorEntities){
        Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
        return BookMapper.toDomain(bookEntity, (HashSet<AuthorEntity>) bookEntity.getAuthors(), shelfEntity.orElse(null));
    }


    public void checkInBook(String bookTitle) {
        BookEntity bookEntity = findBookByTitle(bookTitle);
        bookEntity.setBookStatus(AvailabilityStatus.AVAILABLE.toString());
        saveBook(bookEntity);
    }

}





