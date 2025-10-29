package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorRepository;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository){
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    public void addBook(String title, String firstName, String lastName) {
        // Logic to add a new book to the library
        AuthorEntity authorEntity = new AuthorEntity(firstName,lastName);
        authorRepository.save(authorEntity);
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(authorEntity);

//        BookEntity bookEntity = new BookEntity();
//        bookEntity.setTitle(title);
//        bookEntity.setAuthorId(5L);
//        bookRepository.save(bookEntity);
//        System.out.println();
//        System.out.println("Adding book: " + title + " by " + author.getFullName());
        BookEntity myBookE = BookMapper.toEntity(book);
        bookRepository.save(myBookE);
    }

}
