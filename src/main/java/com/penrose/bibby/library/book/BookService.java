package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorMapper;
import com.penrose.bibby.library.author.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class BookService {

    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository){
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    public void createNewBook(BookRequestDTO bookRequestDTO) {
        String firstName = bookRequestDTO.firstName();
        String lastName = bookRequestDTO.lastName();
        String title = bookRequestDTO.title();
        AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName,lastName);
        if(authorEntity == null){
            authorEntity = new AuthorEntity(firstName,lastName);
//            authorEntity = AuthorMapper.toEntity(new Author(firstName,lastName));
            authorRepository.save(authorEntity);
        }
        Book book = new Book();
        book.setTitle(title);

//        book.setAuthor(authorEntity);
        BookEntity bookEntity = BookMapper.toEntity(book);
        AuthorEntity a1 = new AuthorEntity("Leo", "Penrose");
        AuthorEntity a2 = new AuthorEntity("Ivo", "Murphy");
        authorRepository.save(a1);
        authorRepository.save(a2);
        HashSet<AuthorEntity> aSet = new HashSet<>();
        aSet.add(a1);
        aSet.add(a2);
        bookEntity.setAuthors(aSet);
        bookRepository.save(bookEntity);
    }

}
