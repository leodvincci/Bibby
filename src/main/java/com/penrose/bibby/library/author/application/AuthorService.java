package com.penrose.bibby.library.author.application;

import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.author.domain.AuthorFactory;
import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final AuthorFactory authorFactory;

    public AuthorService(AuthorRepository authorRepository, AuthorFactory authorFactory) {
        this.authorRepository = authorRepository;
        this.authorFactory = authorFactory;
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
        return authorRepository.save(authorFactory.createEntity(authorFirstName,authorLastName));
   }

   public void saveAuthor(AuthorEntity authorEntity){
        authorRepository.save(authorEntity);
   }

   public AuthorEntity findOrCreateAuthor(String authorFirstName, String authorLastName){
        Optional<AuthorEntity> existingAuthor = findAuthorByName(authorFirstName,authorLastName);
        return existingAuthor.orElseGet(()-> createAuthor(authorFirstName,authorLastName));
   }

    public Optional<AuthorEntity> findAuthorByName(String firstName, String lastName){
        return authorRepository.findByFirstNameAndLastName(firstName, lastName);
    }
}
