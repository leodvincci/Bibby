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
//            authorEntity = AuthorMapper.toEntity(new Author(firstName,lastName));
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
        System.out.println("Service Searching for " + title);
        Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
        List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
        for(BookEntity b : bookEntities){
            System.out.println(b.getTitle());
        }
        System.out.println(bookEntity.toString());

        if(bookEntity.isEmpty()){
//            System.out.println("Book Not Found");
            return null;
        }else{
//            System.out.println("Book Found");

        }
        return bookEntity.get();
    }

    public void checkOutBook(BookEntity bookEntity){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }


    public void updateBook(BookEntity bookEntity){
        bookRepository.save(bookEntity);
    }

    public List<AuthorEntity> findAuthorsByBookId(Long bookId) {
        return authorRepository.findByBooks_BookId(bookId);
    }
}
