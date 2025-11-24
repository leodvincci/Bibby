diff --git a/src/main/java/com/penrose/bibby/cli/BookCommands.java b/src/main/java/com/penrose/bibby/cli/BookCommands.java
index fdca95b..539ebab 100644
--- a/src/main/java/com/penrose/bibby/cli/BookCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/BookCommands.java
@@ -220,7 +220,6 @@ public class BookCommands extends AbstractShellComponent {
                 throw new IllegalStateException("Shelf is full");
             }else{
                 //            shelf.addBook(bookMapper.toDomain(bookEnt,authorService.findByBookId(bookEnt.getBookId()),shelfService.findShelfById(shelfId).get()));
-                System.out.println(shelfDomain);
                 bookEnt.setShelfId(shelfId);
                 bookService.saveBook(bookEnt);
                 System.out.println("Added Book To the Shelf!");
diff --git a/src/main/java/com/penrose/bibby/library/author/Author.java b/src/main/java/com/penrose/bibby/library/author/Author.java
index fef35f8..a8672f3 100644
--- a/src/main/java/com/penrose/bibby/library/author/Author.java
+++ b/src/main/java/com/penrose/bibby/library/author/Author.java
@@ -15,6 +15,7 @@ public class Author {
     }
 
     public Author(Long authorId, String firstName, String lastName) {
+        this.authorId = authorId;
         this.firstName = firstName;
         this.lastName = lastName;
     }
@@ -42,4 +43,11 @@ public class Author {
     public void setLastName(String lastName) {
         this.lastName = lastName;
     }
+
+    @Override
+    public String toString() {
+        return "Author{" + "firstName='" + firstName + '\'' +
+                ", lastName='" + lastName + '\'' +
+                '}';
+    }
 }
diff --git a/src/main/java/com/penrose/bibby/library/author/AuthorFactory.java b/src/main/java/com/penrose/bibby/library/author/AuthorFactory.java
index 7c1a9c9..fb0edfa 100644
--- a/src/main/java/com/penrose/bibby/library/author/AuthorFactory.java
+++ b/src/main/java/com/penrose/bibby/library/author/AuthorFactory.java
@@ -3,8 +3,15 @@ package com.penrose.bibby.library.author;
 import org.springframework.stereotype.Component;
 
 @Component
-public class AuthorEntityFactory {
+public class AuthorFactory {
+    AuthorFactory(){}
+
     public AuthorEntity createEntity(String firstName, String lastName){
         return new AuthorEntity(firstName, lastName);
     }
+
+    public Author createDomain(Long id, String firstName, String lastName){
+        return new Author(id,firstName, lastName);
+    }
+
 }
diff --git a/src/main/java/com/penrose/bibby/library/author/AuthorService.java b/src/main/java/com/penrose/bibby/library/author/AuthorService.java
index 6767b64..e03d55e 100644
--- a/src/main/java/com/penrose/bibby/library/author/AuthorService.java
+++ b/src/main/java/com/penrose/bibby/library/author/AuthorService.java
@@ -8,11 +8,11 @@ import java.util.Set;
 @Service
 public class AuthorService {
     private final AuthorRepository authorRepository;
-    private final AuthorEntityFactory authorEntityFactory;
+    private final AuthorFactory authorFactory;
 
-    public AuthorService(AuthorRepository authorRepository, AuthorEntityFactory authorEntityFactory) {
+    public AuthorService(AuthorRepository authorRepository, AuthorFactory authorFactory) {
         this.authorRepository = authorRepository;
-        this.authorEntityFactory = authorEntityFactory;
+        this.authorFactory = authorFactory;
     }
 
     public Set<AuthorEntity> findByBookId(Long id){
@@ -28,7 +28,7 @@ public class AuthorService {
    }
 
    public AuthorEntity createAuthor(String authorFirstName, String authorLastName){
-        return authorRepository.save(authorEntityFactory.createEntity(authorFirstName,authorLastName));
+        return authorRepository.save(authorFactory.createEntity(authorFirstName,authorLastName));
    }
 
    public AuthorEntity findOrCreateAuthor(String authorFirstName, String authorLastName){
diff --git a/src/main/java/com/penrose/bibby/library/book/Book.java b/src/main/java/com/penrose/bibby/library/book/Book.java
index 320a20e..0026088 100644
--- a/src/main/java/com/penrose/bibby/library/book/Book.java
+++ b/src/main/java/com/penrose/bibby/library/book/Book.java
@@ -2,6 +2,7 @@ package com.penrose.bibby.library.book;
 
 import java.time.LocalDate;
 import java.util.HashSet;
+import java.util.List;
 import java.util.Objects;
 
 import com.penrose.bibby.library.author.Author;
@@ -12,7 +13,7 @@ public class Book {
     private Long id;
     private int edition;
     private String title;
-    private HashSet<Author> authors;
+    private List<Author> authors;
     private String isbn;
     private String publisher;
     private int publicationYear;
@@ -26,7 +27,7 @@ public class Book {
     public Book() {
     }
 
-    public Book(Long id, String title, HashSet<Author> authors) {
+    public Book(Long id, String title, List<Author> authors) {
         this.id = id;
         this.title = title;
         this.authors = authors;
@@ -74,7 +75,7 @@ public class Book {
         this.title = title;
     }
 
-    public HashSet<Author> getAuthors() {
+    public List<Author> getAuthors() {
         return authors;
     }
 
@@ -82,7 +83,7 @@ public class Book {
         authors.add(author);
     }
 
-    public void setAuthors(HashSet<Author> authors) {
+    public void setAuthors(List<Author> authors) {
         this.authors = authors;
     }
 
@@ -160,20 +161,8 @@ public class Book {
 
     @Override
     public String toString() {
-        return "Book{" +
-                "id=" + id +
-                ", edition=" + edition +
-                ", title='" + title + '\'' +
+        return "Book{ title='" + title + '\'' +
                 ", authors=" + authors +
-                ", isbn='" + isbn + '\'' +
-                ", publisher='" + publisher + '\'' +
-                ", publicationYear=" + publicationYear +
-                ", genre=" + genre +
-                ", shelf=" + shelf +
-                ", description='" + description + '\'' +
-                ", availabilityStatus=" + availabilityStatus +
-                ", createdAt=" + createdAt +
-                ", updatedAt=" + updatedAt +
                 '}';
     }
 
diff --git a/src/main/java/com/penrose/bibby/library/book/BookFactory.java b/src/main/java/com/penrose/bibby/library/book/BookFactory.java
index fe90b31..449b727 100644
--- a/src/main/java/com/penrose/bibby/library/book/BookFactory.java
+++ b/src/main/java/com/penrose/bibby/library/book/BookFactory.java
@@ -4,13 +4,13 @@ import com.penrose.bibby.library.author.Author;
 import com.penrose.bibby.library.author.AuthorEntity;
 import org.springframework.stereotype.Component;
 
-import java.util.HashSet;
+import java.util.List;
 import java.util.Set;
 
 @Component
 public class BookFactory {
 
-    public BookEntity createBook(String title, Set<AuthorEntity> authors){
+    public BookEntity createBookEntity(String title, Set<AuthorEntity> authors){
         BookEntity bookEntity = new BookEntity();
         bookEntity.setTitle(title);
         bookEntity.setAuthors(authors);
@@ -19,4 +19,13 @@ public class BookFactory {
         return bookEntity;
     }
 
+    public Book createBookDomain(BookEntity bookEntity, List<Author> authors){
+        Book book = new Book();
+        book.setId(bookEntity.getBookId());
+        book.setTitle(bookEntity.getTitle());
+        book.setAuthors(authors);
+        book.setAvailabilityStatus(AvailabilityStatus.valueOf(bookEntity.getAvailabilityStatus()));
+        return book;
+    }
+
 }
diff --git a/src/main/java/com/penrose/bibby/library/book/BookMapper.java b/src/main/java/com/penrose/bibby/library/book/BookMapper.java
index 11df182..d3735f4 100644
--- a/src/main/java/com/penrose/bibby/library/book/BookMapper.java
+++ b/src/main/java/com/penrose/bibby/library/book/BookMapper.java
@@ -9,7 +9,9 @@ import com.penrose.bibby.library.shelf.ShelfEntity;
 import com.penrose.bibby.library.shelf.ShelfMapper;
 import org.springframework.stereotype.Component;
 
+import java.util.ArrayList;
 import java.util.HashSet;
+import java.util.List;
 import java.util.Set;
 @Component
 public class BookMapper {
@@ -25,7 +27,7 @@ public class BookMapper {
                          Set<AuthorEntity> authorEntities,
                          ShelfEntity shelfEntity){
 
-        HashSet<Author> authors = new HashSet<>();
+        List<Author> authors = new ArrayList<>();
 //        Shelf shelf = shelfMapper.toDomain(shelfEntity);
         Shelf shelf = shelfDomainRepositoryImpl.getById(shelfEntity.getShelfId());
 
diff --git a/src/main/java/com/penrose/bibby/library/book/BookService.java b/src/main/java/com/penrose/bibby/library/book/BookService.java
index 02a0a51..51e397b 100644
--- a/src/main/java/com/penrose/bibby/library/book/BookService.java
+++ b/src/main/java/com/penrose/bibby/library/book/BookService.java
@@ -45,7 +45,7 @@ public class BookService {
     public void createNewBook(BookRequestDTO bookDTO){
         validateRequest(bookDTO);
         validateBookDoesNotExist(bookDTO);
-        saveBook(BookFactory.createBook(bookDTO.title(), extractAuthorEntities(bookDTO)));
+        saveBook(BookFactory.createBookEntity(bookDTO.title(), extractAuthorEntities(bookDTO)));
     }
 
     private Set<AuthorEntity> extractAuthorEntities(BookRequestDTO bookRequestDTO){
diff --git a/src/main/java/com/penrose/bibby/library/shelf/Shelf.java b/src/main/java/com/penrose/bibby/library/shelf/Shelf.java
index 160bb35..b8ee2e7 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/Shelf.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/Shelf.java
@@ -13,7 +13,7 @@ public class Shelf {
     private String shelfDescription;
     private int shelfPosition;
     private int bookCapacity;
-    private List<BookEntity> books;
+    private List<Book> books;
 
 
     public Shelf(Bookcase bookCase, String shelfLabel, int shelfPosition, int bookCapacity) {
@@ -35,10 +35,10 @@ public class Shelf {
         return books.size() >= bookCapacity;
     }
 
-    public List<BookEntity> getBooks() {
+    public List<Book> getBooks() {
         return books;
     }
-    public void setBooks(List<BookEntity> books) {
+    public void setBooks(List<Book> books) {
         this.books = books;
     }
 
diff --git a/src/main/java/com/penrose/bibby/library/shelf/ShelfDomainRepositoryImpl.java b/src/main/java/com/penrose/bibby/library/shelf/ShelfDomainRepositoryImpl.java
index 7d33aa7..de599db 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/ShelfDomainRepositoryImpl.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/ShelfDomainRepositoryImpl.java
@@ -1,27 +1,32 @@
 package com.penrose.bibby.library.shelf;
 
+import com.penrose.bibby.library.book.Book;
+import com.penrose.bibby.library.book.BookDomainRepository;
 import com.penrose.bibby.library.book.BookRepository;
 import org.springframework.stereotype.Component;
 
+import java.util.List;
+
 @Component
 public class ShelfDomainRepositoryImpl implements ShelfDomainRepository{
 
     private final ShelfJpaRepository jpaRepository;
     private final ShelfMapper shelfMapper;
-    private final BookRepository bookRepository;
+    private final BookDomainRepository bookDomainRepository;
     private final ShelfJpaRepository shelfJpaRepository;
     public ShelfDomainRepositoryImpl(ShelfJpaRepository jpaRepository,
-                                     ShelfMapper shelfMapper, BookRepository bookRepository, ShelfJpaRepository shelfJpaRepository) {
+                                     ShelfMapper shelfMapper, BookDomainRepository bookDomainRepository, ShelfJpaRepository shelfJpaRepository) {
         this.jpaRepository = jpaRepository;
         this.shelfMapper = shelfMapper;
-        this.bookRepository = bookRepository;
+        this.bookDomainRepository = bookDomainRepository;
         this.shelfJpaRepository = shelfJpaRepository;
     }
 
     @Override
     public Shelf getById(Long id) {
         ShelfEntity entity = jpaRepository.findById(id).orElse(null);
-        return shelfMapper.toDomain(entity,bookRepository.findByShelfId(id));
+        List<Book> books = bookDomainRepository.getBooksByShelfId(id);
+        return shelfMapper.toDomain(entity,books);
     }
 
     @Override
diff --git a/src/main/java/com/penrose/bibby/library/shelf/ShelfMapper.java b/src/main/java/com/penrose/bibby/library/shelf/ShelfMapper.java
index dfdb006..5cdce21 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/ShelfMapper.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/ShelfMapper.java
@@ -1,5 +1,6 @@
 package com.penrose.bibby.library.shelf;
 
+import com.penrose.bibby.library.book.Book;
 import com.penrose.bibby.library.book.BookEntity;
 import com.penrose.bibby.library.bookcase.BookcaseEntity;
 import org.springframework.stereotype.Component;
@@ -10,7 +11,7 @@ import java.util.List;
 public class ShelfMapper {
 
 
-    public Shelf toDomain(ShelfEntity shelfEntity, List<BookEntity> books){
+    public Shelf toDomain(ShelfEntity shelfEntity, List<Book> books){
         Shelf shelf = new Shelf();
         shelf.setId(shelfEntity.getShelfId());
         shelf.setBookCapacity(shelfEntity.getBookCapacity());
