package com.penrose.bibby.library.author.application;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.author.domain.AuthorRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.Set;

import com.penrose.bibby.library.author.contracts.AuthorDTO;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public Set<Author> findByBookId(Long id){
        return authorRepository.findAuthorsByBookId(id);
    }


   public Optional<Author> findById(Long id){
       return authorRepository.findById(id);
   }


   public Author createAuthor(String authorFirstName, String authorLastName){
        return authorRepository.save(authorFirstName,authorLastName);
   }


   public void updateAuthor(Author author){
        authorRepository.update(author);
   }

   public Author findOrCreateAuthor(String authorFirstName, String authorLastName){
        Optional<Author> existingAuthor = findAuthorByName(authorFirstName,authorLastName);
        return existingAuthor.orElseGet(()-> createAuthor(authorFirstName,authorLastName));
   }

    public Optional<Author> findAuthorByName(String firstName, String lastName){
        return authorRepository.findByFirstNameAndLastName(firstName, lastName);
    }
}
