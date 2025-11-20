package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import com.penrose.bibby.library.author.AuthorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository, AuthorService authorService){
        this.bookRepository = bookRepository;
        this.authorService = authorService;
        this.authorRepository = authorRepository;
    }

    @Transactional
    public void createNewBook(BookRequestDTO bookRequestDTO) {
        String firstName = bookRequestDTO.firstName();
        String lastName = bookRequestDTO.lastName();
        String title = bookRequestDTO.title();

        BookEntity bookEntity = findBookByTitleIgnoreCase(title);
        AuthorEntity authorEntity = authorService.findByAuthorFirstNameLastName(firstName, lastName);

        if (authorEntity == null) {
            authorEntity = authorService.createNewAuthor(firstName,lastName);
        }

        if (bookEntity == null) {
            bookEntity = BookFactory.createBook(title,authorEntity);
            saveBook( bookEntity );
        }else{
            System.out.println("Book Already Exists");
        }
    }

    public BookEntity findBookByTitle(String title){
        Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
        List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
        for(BookEntity b : bookEntities){
            System.out.println(b.getTitle());
        }

        if(bookEntity.isEmpty()){
            return null;
        }
        return bookEntity.get();
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

    public BookEntity findBookByTitleIgnoreCase(String title){
        return bookRepository.findByTitleIgnoreCase(title);
    }

    public void saveBook(BookEntity bookEntity){
        bookRepository.save(bookEntity);
    }
}





