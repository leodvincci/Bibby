package com.penrose.bibby.library.catalog.author.infrastructure.repository;

import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AuthorJpaRepository extends JpaRepository <AuthorEntity, Long> {

    Optional<AuthorEntity> getByFirstNameAndLastName(String firstName, String lastName);

    @Query("SELECT a FROM AuthorEntity a JOIN a.books b WHERE b.bookId = :bookId")
    Set<AuthorEntity> findByBooks_BookId(@Param("bookId") Long bookId);

    List<AuthorEntity> getAllByFirstNameAndLastName(String firstName, String lastName);

    List<AuthorEntity> findByFirstNameAndLastName(String firstName, String lastName);
}
