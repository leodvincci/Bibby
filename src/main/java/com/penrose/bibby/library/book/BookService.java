package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository){
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    @Transactional
    public void createNewBook(BookRequestDTO bookRequestDTO) {
        String firstName = bookRequestDTO.firstName();
        String lastName = bookRequestDTO.lastName();
        String title = bookRequestDTO.title();

        BookEntity bookEntity = bookRepository.findByTitle(title);
        AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName, lastName);

        if (authorEntity == null) {
            authorEntity = new AuthorEntity(firstName, lastName);
            authorRepository.save(authorEntity);
        }

        if (bookEntity == null) {
            bookEntity = new BookEntity();
            bookEntity.setTitle(title);
        }
            bookEntity.setAuthors(authorEntity);
            bookRepository.save(bookEntity);
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
            bookRepository.save(bookEntity);
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
}





