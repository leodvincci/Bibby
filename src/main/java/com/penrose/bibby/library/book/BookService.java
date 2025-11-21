package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import com.penrose.bibby.library.author.AuthorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final AuthorRepository authorRepository;
    private final BookFactory BookFactory;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository, AuthorService authorService,BookFactory bookFactory){
        this.bookRepository = bookRepository;
        this.authorService = authorService;
        this.authorRepository = authorRepository;
        this.BookFactory = bookFactory;
    }

    @Transactional
    public void createNewBook(BookRequestDTO bookRequestDTO) {
        Optional<BookEntity> bookEntity = findBookByTitleIgnoreCase(bookRequestDTO.title());

        if (bookEntity.isPresent()) {
            throw new IllegalArgumentException("Book Already Exists: " + bookRequestDTO.title());
        }

        AuthorEntity authorEntity = authorService.findOrCreateAuthor(bookRequestDTO.firstName(),bookRequestDTO.lastName());
        saveBook(BookFactory.createBook(bookRequestDTO.title(), authorEntity));

    }


    public BookEntity findBookByTitle(String title){
        Optional<BookEntity> bookEntity = bookRepository.findByTitleIgnoreCase(title);
        List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);

        for(BookEntity b : bookEntities){
            System.out.println(b.getTitle());
        }

        return bookEntity.orElse(null);
    }

    public void checkOutBook(BookEntity bookEntity){
        if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
            bookEntity.setBookStatus("CHECKED_OUT");
            saveBook(bookEntity);
        }

    }


    public void updateBook(BookEntity bookEntity){
        bookRepository.save(bookEntity);
    }

    public List<AuthorEntity> findAuthorsByBookId(Long bookId) {
        return authorRepository.findByBooks_BookId(bookId);
    }

    public void checkInBook(String bookTitle) {
        BookEntity bookEntity = findBookByTitle(bookTitle);
        bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());
        updateBook(bookEntity);
    }

    public List<BookEntity> findBooksByShelf(Long id) {
        return bookRepository.findByShelfId(id);
    }

    public List<BookSummary> getBooksForShelf(Long shelfId) {
        return bookRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
    }


    public BookDetailView getBookDetails(Long bookId){
        return bookRepository.getBookDetailView(bookId);
    }

    public Optional<BookEntity> findBookById(Long bookId) {
        return bookRepository.findById(bookId);
    }

    public Optional<BookEntity> findBookByTitleIgnoreCase(String title){
        return bookRepository.findByTitleIgnoreCase(title);
    }

    public void saveBook(BookEntity bookEntity){
        bookRepository.save(bookEntity);
    }
}





