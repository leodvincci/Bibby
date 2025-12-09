package com.penrose.bibby.library.author.infrastructure.repository;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface AuthorRepository extends JpaRepository <AuthorEntity, Long> {

    Optional<AuthorEntity> findByFirstNameAndLastName(String firstName, String lastName);

    @Query("SELECT a FROM AuthorEntity a JOIN a.books b WHERE b.bookId = :bookId")
    Set<AuthorEntity> findByBooks_BookId(@Param("bookId") Long bookId);
}
