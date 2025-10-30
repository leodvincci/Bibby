package com.penrose.bibby.util;
import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.bookcase.BookCase;
import com.penrose.bibby.library.shelf.ShelfController;
import com.penrose.bibby.library.shelf.ShelfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {
    @Autowired
    ShelfService shelfService;
    @Override
    public void run(String... args) throws Exception {

        System.out.println(">>> Bibby is awake. Seeding data now...");
        BookCase bookCase = new BookCase();
        AuthorEntity author = new AuthorEntity("Leo","Penrose");
        shelfService.addToShelf(new Book(42L,"The Can Man",author),"bookCase");

        BookCase bookCase2 = new BookCase();
        AuthorEntity author2 = new AuthorEntity("Bpb","ose");
        shelfService.addToShelf(new Book(421L,"Tplo af ",author2),"bookCase");

        // call whatever you want here
        // e.g. create a book, save it, print results, etc.
        // bookService.addBook("Pride and Prejudice", "Jane Austen");
    }
}