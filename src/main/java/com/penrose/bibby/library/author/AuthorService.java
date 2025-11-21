package com.penrose.bibby.library.author;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {
    AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<AuthorEntity> findByBookId(Long id){
        return authorRepository.findByBooks_BookId(id);
    }

   public AuthorEntity findByAuthorFirstNameLastName(String firstName, String lastName){
       return authorRepository.findByFirstNameAndLastName(firstName, lastName);
   }

   public AuthorEntity createNewAuthor(String authorFirstName, String authorLastName){
        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.setFirstName(authorFirstName);
        authorEntity.setLastName(authorLastName);
        authorEntity.setFullName(authorFirstName + " " + authorLastName);
        return authorRepository.save(authorEntity);
   }

}
