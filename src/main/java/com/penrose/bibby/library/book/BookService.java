package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final BookFactory BookFactory;

    public BookService(BookRepository bookRepository, AuthorService authorService,BookFactory bookFactory){
        this.bookRepository = bookRepository;
        this.authorService = authorService;
        this.BookFactory = bookFactory;
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
        if (bookEntity.isPresent()) {
            throw new IllegalArgumentException("Book Already Exists: " + bookRequestDTO.title());
        }
        AuthorEntity authorEntity = authorService.findOrCreateAuthor(bookRequestDTO.firstName(),bookRequestDTO.lastName());
        saveBook(BookFactory.createBook(bookRequestDTO.title(), authorEntity));
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
        if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
            bookEntity.setBookStatus("CHECKED_OUT");
            saveBook(bookEntity);
        }
    }

    public void checkInBook(String bookTitle) {
        BookEntity bookEntity = findBookByTitle(bookTitle);
        bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());
        saveBook(bookEntity);
    }

}





