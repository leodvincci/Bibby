diff --git a/src/main/java/com/penrose/bibby/cli/commands/BookcaseCommands.java b/src/main/java/com/penrose/bibby/cli/commands/BookcaseCommands.java
index 896bb72..5c3aba2 100644
--- a/src/main/java/com/penrose/bibby/cli/commands/BookcaseCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/commands/BookcaseCommands.java
@@ -1,19 +1,19 @@
 package com.penrose.bibby.cli.commands;
 
+import com.penrose.bibby.library.bookcase.contracts.BookcaseFacade;
+import org.springframework.shell.command.annotation.Command;
+import org.springframework.shell.component.flow.ComponentFlow;
+import org.springframework.shell.standard.AbstractShellComponent;
+import org.springframework.stereotype.Component;
+
+import com.penrose.bibby.library.book.contracts.BookFacade;
 import com.penrose.bibby.library.book.contracts.BookDTO;
 import com.penrose.bibby.library.book.contracts.BookDetailView;
-import com.penrose.bibby.library.book.application.BookService;
 import com.penrose.bibby.library.book.contracts.BookSummary;
 import com.penrose.bibby.library.bookcase.contracts.BookcaseDTO;
-import com.penrose.bibby.library.bookcase.application.BookcaseService;
 import com.penrose.bibby.library.shelf.contracts.ShelfDTO;
 import com.penrose.bibby.library.shelf.contracts.ShelfFacade;
 import com.penrose.bibby.library.shelf.contracts.ShelfSummary;
-import org.springframework.shell.command.annotation.Command;
-import org.springframework.shell.component.flow.ComponentFlow;
-import org.springframework.shell.standard.AbstractShellComponent;
-import org.springframework.stereotype.Component;
-
 import java.util.*;
 
 @Component
@@ -22,16 +22,17 @@ public class BookcaseCommands extends AbstractShellComponent {
 
 
     private final ComponentFlow.Builder componentFlowBuilder;
-    private final BookcaseService bookcaseService;
-    private final BookService bookService;
     private final ShelfFacade shelfFacade;
+    private final BookFacade bookFacade;
+    private final BookcaseFacade bookcaseFacade;
 
 
-    public BookcaseCommands(ComponentFlow.Builder componentFlowBuilder, BookcaseService bookcaseService, BookService bookService, ShelfFacade shelfFacade) {
+    public BookcaseCommands(ComponentFlow.Builder componentFlowBuilder,
+                            ShelfFacade shelfFacade, BookFacade bookFacade, BookcaseFacade bookcaseFacade) {
         this.componentFlowBuilder = componentFlowBuilder;
-        this.bookcaseService = bookcaseService;
-        this.bookService = bookService;
         this.shelfFacade = shelfFacade;
+        this.bookFacade = bookFacade;
+        this.bookcaseFacade = bookcaseFacade;
     }
 
     public String bookcaseRowFormater(BookcaseDTO bookcaseDTO, int bookCount){
@@ -90,7 +91,7 @@ public class BookcaseCommands extends AbstractShellComponent {
 
       ComponentFlow.ComponentFlowResult res =  flow.run();
       if(res.getContext().get("confirmation").equals("Y") | res.getContext().get("confirmation").equals("y")) {
-          bookcaseService.createNewBookCase(bookcaseLabel,shelfCount,bookCapacity);
+          bookcaseFacade.createNewBookCase(bookcaseLabel,shelfCount,bookCapacity);
           System.out.println("Created");
       }else{
           System.out.println("Not Created");
@@ -103,7 +104,7 @@ public class BookcaseCommands extends AbstractShellComponent {
     private Map<String, String> bookCaseOptions() {
         // LinkedHashMap keeps insertion order so the menu shows in the order you add them
         Map<String, String> options = new LinkedHashMap<>();
-        List<BookcaseDTO> bookcaseDTOs = bookcaseService.getAllBookcases();
+        List<BookcaseDTO> bookcaseDTOs = bookcaseFacade.getAllBookcases();
         for (BookcaseDTO bookcaseDTO : bookcaseDTOs) {
             int shelfBookCount = 0;
             List<ShelfDTO> shelves = shelfFacade.findByBookcaseId(bookcaseDTO.bookcaseId());
@@ -162,10 +163,10 @@ public class BookcaseCommands extends AbstractShellComponent {
     public void selectBookFromShelf(Long shelfId){
         Map<String, String> bookOptions = new LinkedHashMap<>();
 
-        for(BookSummary bs: bookService.getBooksForShelf(shelfId) ){
+        for(BookSummary bookSummary: bookFacade.getBooksForShelf(shelfId) ){
             bookOptions.put(String.format(
                     "\u001B[38;5;197m%-10s  \u001B[0m"
-                    ,bs.title()),String.valueOf(bs.bookId()));
+                    ,bookSummary.title()),String.valueOf(bookSummary.bookId()));
         }
 
         if (bookOptions.isEmpty()) {
@@ -189,7 +190,7 @@ public class BookcaseCommands extends AbstractShellComponent {
     }
 
     public void getBookDetailsView(Long bookId){
-        BookDetailView bookDetails = bookService.getBookDetails(bookId);
+        BookDetailView bookDetails = bookFacade.getBookDetails(bookId);
         String res = String.format(
                 """
                     Title \u001B[38;5;197m%-10s  \u001B[0m
@@ -215,8 +216,8 @@ public class BookcaseCommands extends AbstractShellComponent {
         ComponentFlow.ComponentFlowResult result = flow.run();
 
         if(result.getContext().get("optionSelected").equals("1") ){
-            Optional<BookDTO> bookDTO = bookService.findBookById(bookId);
-            bookService.checkOutBook(bookDTO.get());
+            Optional<BookDTO> bookDTO = bookFacade.findBookById(bookId);
+            bookFacade.checkOutBook(bookDTO.get());
         }
 
     }
diff --git a/src/main/java/com/penrose/bibby/library/author/application/AuthorService.java b/src/main/java/com/penrose/bibby/library/author/application/AuthorService.java
index f1c563c..c149386 100644
--- a/src/main/java/com/penrose/bibby/library/author/application/AuthorService.java
+++ b/src/main/java/com/penrose/bibby/library/author/application/AuthorService.java
@@ -1,15 +1,17 @@
 package com.penrose.bibby.library.author.application;
 
-import com.penrose.bibby.library.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.author.contracts.AuthorFacade;
-import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.author.domain.AuthorFactory;
-import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
 import org.springframework.stereotype.Service;
 import java.util.List;
 import java.util.Optional;
 import java.util.Set;
 
+import com.penrose.bibby.library.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.author.contracts.AuthorFacade;
+import com.penrose.bibby.library.author.domain.AuthorFactory;
+
+import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
+
 @Service
 public class AuthorService implements AuthorFacade {
     private final AuthorRepository authorRepository;
diff --git a/src/main/java/com/penrose/bibby/library/book/contracts/BookFacade.java b/src/main/java/com/penrose/bibby/library/book/contracts/BookFacade.java
index b111991..9b79c3d 100644
--- a/src/main/java/com/penrose/bibby/library/book/contracts/BookFacade.java
+++ b/src/main/java/com/penrose/bibby/library/book/contracts/BookFacade.java
@@ -21,4 +21,10 @@ public interface BookFacade {
     void checkOutBook(BookDTO bookDTO);
 
     void checkInBook(String bookTitle);
+
+    List<BookSummary> getBooksForShelf(Long shelfId);
+
+    BookDetailView getBookDetails(Long bookId);
+
+    Optional<BookDTO> findBookById(Long bookId);
 }
diff --git a/src/main/java/com/penrose/bibby/library/bookcase/contracts/BookcaseFacade.java b/src/main/java/com/penrose/bibby/library/bookcase/contracts/BookcaseFacade.java
index ed40a07..1cf67ab 100644
--- a/src/main/java/com/penrose/bibby/library/bookcase/contracts/BookcaseFacade.java
+++ b/src/main/java/com/penrose/bibby/library/bookcase/contracts/BookcaseFacade.java
@@ -10,4 +10,5 @@ public interface BookcaseFacade {
 
     List<BookcaseDTO> getAllBookcases();
 
+    void createNewBookCase(String bookcaseLabel, int shelfCount, int bookCapacity);
 }
