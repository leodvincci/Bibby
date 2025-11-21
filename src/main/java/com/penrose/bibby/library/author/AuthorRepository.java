package com.penrose.bibby.library.author;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository <AuthorEntity, Long> {

    AuthorEntity getByFullName(String fullName);

    AuthorEntity findByFirstNameAndLastName(String firstName, String lastName);

    @Query("SELECT a FROM AuthorEntity a JOIN a.books b WHERE b.bookId = :bookId")
    List<AuthorEntity> findByBooks_BookId(@Param("bookId") Long bookId);
}
