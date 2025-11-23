package com.penrose.bibby.library.author;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final AuthorEntityFactory authorEntityFactory;

    public AuthorService(AuthorRepository authorRepository, AuthorEntityFactory authorEntityFactory) {
        this.authorRepository = authorRepository;
        this.authorEntityFactory = authorEntityFactory;
    }

    public Set<AuthorEntity> findByBookId(Long id){
        return authorRepository.findByBooks_BookId(id);
    }

    public List<AuthorEntity> findAll(){
        return authorRepository.findAll();
    }

   public AuthorEntity findById(Long id){
       return authorRepository.findById(id).orElse(null);
   }

   public AuthorEntity createAuthor(String authorFirstName, String authorLastName){
        return authorRepository.save(authorEntityFactory.createEntity(authorFirstName,authorLastName));
   }

   public AuthorEntity findOrCreateAuthor(String authorFirstName, String authorLastName){
        Optional<AuthorEntity> existingAuthor = findAuthorByName(authorFirstName,authorLastName);
        return existingAuthor.orElseGet(()-> createAuthor(authorFirstName,authorLastName));
   }

    public Optional<AuthorEntity> findAuthorByName(String firstName, String lastName){
        return authorRepository.findByFirstNameAndLastName(firstName, lastName);
    }
}
