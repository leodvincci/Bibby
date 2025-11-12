package com.penrose.bibby.library.author;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository <AuthorEntity, Long> {

    AuthorEntity getByFullName(String fullName);

    AuthorEntity findByFirstNameAndLastName(String firstName, String lastName);

    List<AuthorEntity> findByBooks_BookId(Long bookId);
}
