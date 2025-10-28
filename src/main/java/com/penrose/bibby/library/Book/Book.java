package com.penrose.bibby.library.Book;
import java.time.LocalDate;
import com.penrose.bibby.library.Shelf.*;

public class Book {
//    private BookStatus bookStatus;
    private Integer id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String publicationYear;
    private String genre;
    private Shelf shelf;
//    private Enum<BookStatus> status;
    private Integer checkoutCount;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
