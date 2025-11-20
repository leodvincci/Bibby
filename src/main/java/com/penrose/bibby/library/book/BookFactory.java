package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;

import java.util.HashSet;
import java.util.Set;

public class BookFactory {
    public static BookEntity createBook(String title, AuthorEntity author){
        BookEntity bookEntity = new BookEntity();
        bookEntity.setTitle(title);
        bookEntity.setAuthors(author);
        return bookEntity;
    }
}
