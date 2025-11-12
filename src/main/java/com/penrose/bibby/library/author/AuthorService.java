package com.penrose.bibby.library.author;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {
    AuthorRepository authorRepository;

    public List<AuthorEntity> findByBookId(Long id){
        return authorRepository.findByBooks_BookId(id);
    }

}
