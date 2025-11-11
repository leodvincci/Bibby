package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        BookEntity bookEntity = bookRepository.findByTitleIgnoreCase(title);
        if(bookEntity == null){
//            System.out.println("Book Not Found");
            return null;
        }else{
//            System.out.println("Book Found");

        }
        return bookEntity;
    }


    public void updateBook(BookEntity bookEntity){
        bookRepository.save(bookEntity);
    }

}
