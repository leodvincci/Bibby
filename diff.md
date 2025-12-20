diff --git a/src/main/java/com/penrose/bibby/cli/command/book/BookCommands.java b/src/main/java/com/penrose/bibby/cli/command/book/BookCommands.java
index ec22bba..2cc2a1a 100644
--- a/src/main/java/com/penrose/bibby/cli/command/book/BookCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/command/book/BookCommands.java
@@ -1,7 +1,8 @@
-package com.penrose.bibby.cli.commands.book;
+package com.penrose.bibby.cli.command.book;
 
 import com.penrose.bibby.cli.ConsoleColors;
 import com.penrose.bibby.cli.prompt.domain.PromptOptions;
+import com.penrose.bibby.cli.ui.BookcardRenderer;
 import org.slf4j.Logger;
 import org.springframework.shell.command.annotation.Command;
 import org.springframework.shell.standard.AbstractShellComponent;
@@ -36,6 +37,7 @@ public class BookCommands extends AbstractShellComponent {
     private final CliPromptService cliPrompt;
     private final ComponentFlow.Builder componentFlowBuilder;
     private final PromptOptions promptOptions;
+    private final BookcardRenderer bookcardRenderer;
     Logger log = org.slf4j.LoggerFactory.getLogger(BookCommands.class);
 
     public BookCommands(ComponentFlow.Builder componentFlowBuilder,
@@ -43,7 +45,7 @@ public class BookCommands extends AbstractShellComponent {
                         ShelfFacade shelfFacade,
                         CliPromptService cliPrompt,
                         BookFacade bookFacade,
-                        BookcaseFacade bookcaseFacade, PromptOptions promptOptions) {
+                        BookcaseFacade bookcaseFacade, PromptOptions promptOptions, BookcardRenderer bookcardRenderer) {
         this.componentFlowBuilder = componentFlowBuilder;
         this.authorFacade = authorFacade;
         this.shelfFacade = shelfFacade;
@@ -51,6 +53,7 @@ public class BookCommands extends AbstractShellComponent {
         this.bookFacade = bookFacade;
         this.bookcaseFacade = bookcaseFacade;
         this.promptOptions = promptOptions;
+        this.bookcardRenderer = bookcardRenderer;
     }
 
     // ───────────────────────────────────────────────────────────────────
@@ -84,7 +87,7 @@ public class BookCommands extends AbstractShellComponent {
 
         log.debug("Authors verified/created for book.");
         log.info(bookMetaDataResponse.toString());
-        String bookcard = createBookCard(bookMetaDataResponse.title(),
+        String bookcard = bookcardRenderer.createBookCard(bookMetaDataResponse.title(),
                 bookMetaDataResponse.isbn(),
                 bookMetaDataResponse.authors().toString(),
                 bookMetaDataResponse.publisher(),
@@ -107,7 +110,7 @@ public class BookCommands extends AbstractShellComponent {
             List<Long> authorIds = createAuthorsFromMetaData(bookMetaDataResponse.authors());
 
             bookFacade.createBookFromMetaData(bookMetaDataResponse, authorIds, isbn, shelfId);
-            String updatedBookCard = createBookCard(bookMetaDataResponse.title(),
+            String updatedBookCard = bookcardRenderer.createBookCard(bookMetaDataResponse.title(),
                     bookMetaDataResponse.isbn(),
                     bookMetaDataResponse.authors().toString(),
                     bookMetaDataResponse.publisher(),
@@ -325,46 +328,7 @@ public class BookCommands extends AbstractShellComponent {
 
 
 
-    public String createBookCard(String title, String isbn, String author, String publisher, String bookcase, String shelf, String location) {
-
-        // %-42s ensures the text is left-aligned and padded to 42 characters
-        // The emojis take up extra visual space, so adjusted padding slightly
-        return """
-                
-                ╭──────────────────────────────────────────────────────────────────────────────────────╮
-                │  📖 \033[38;5;63m%-73s\033[0m        │     
-                ├──────────────────────────────────────────────────────────────────────────────────────┤
-                │  \033[38;5;42mISBN\033[0m:      %-31s                                          │
-                │  \033[38;5;42mAuthor\033[0m:    %-31.31s%-3.3s                                       │                                                              
-                │  \033[38;5;42mPublisher\033[0m: %-31s                                          │
-                │                                                                                      │
-                │  \033[38;5;42mLocation\033[0m:  %-35s                                      │
-                │  \033[38;5;42mBookcase\033[0m:  %-35s                                      │
-                │  \033[38;5;42mShelf\033[0m:     %-35s                                      │
-                ╰──────────────────────────────────────────────────────────────────────────────────────╯
-                
-                
-        """.formatted(
-                title,
-                isbn,
-                formater(author),
-                author.length() > 42 ? "..." : " ",
-                publisher,
-                location,
-                bookcase,
-                shelf
-        );
-    }
-    public String formater(String authors){
-        String normalizedAuthors = authors.replaceAll("[\\[\\]]", ""); // Remove brackets
-        authors = normalizedAuthors.replaceAll(",\\s*", ","); // Ensure single space after commas
-        return authors;
-    }
 
-    public int countAuthors(String authors) {
-        String[] authorArray = authors.split(",");
-        return authorArray.length;
-    }
 
 // Usage:
 // System.out.println(createBookCard("Building Microservices", "978-1491950357", "Sam Newman", "PENDING / NOT SET"));
@@ -418,7 +382,7 @@ public class BookCommands extends AbstractShellComponent {
                 }
                 Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());
 
-                String bookCard = createBookCard(
+                String bookCard = bookcardRenderer.createBookCard(
                         bookDTO.title(),
                         bookDTO.isbn(),
                         authors.toString(),
@@ -439,26 +403,7 @@ public class BookCommands extends AbstractShellComponent {
         askBookCheckOut();
     }
 
-    public void printNotFound(String title) {
-        String msg = """
-                
-                
-                ╭──────────────────────────────────────────────╮
-                │  🚫 No Results Found                         │
-                ├──────────────────────────────────────────────┤
-           \033[0m     │  \033[0mQuery:\033[0m  %-34s  │
-                │                                              │
-                │  Status: Not in library.                     │
-                │  Action: Check spelling or add new book.     │
-                ╰──────────────────────────────────────────────╯
-                
-                
-        """.formatted(
-                title.length() > 34 ? title.substring(0, 31) + "..." : title
-        ); // Truncates title if it's too long to fit the box
-
-        System.out.println(ConsoleColors.RED + msg + ConsoleColors.RESET);
-    }
+
 
 
     public void searchByTitle() throws InterruptedException {
@@ -470,10 +415,10 @@ public class BookCommands extends AbstractShellComponent {
 
         if (bookDTO == null) {
 
-            printNotFound(title);
+            bookcardRenderer.printNotFound(title);
             return;
         }else if(bookDTO.shelfId() == null){
-            String bookCard = createBookCard(
+            String bookCard = bookcardRenderer.createBookCard(
                     bookDTO.title(),
                     bookDTO.isbn(),
                     authorFacade.findByBookId(bookDTO.id()).toString(),
@@ -489,7 +434,7 @@ public class BookCommands extends AbstractShellComponent {
             Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
             Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
             System.out.println(authorFacade.findByBookId(bookDTO.id()).toString());
-            String bookCard = createBookCard(
+            String bookCard = bookcardRenderer.createBookCard(
                     bookDTO.title(),
                     bookDTO.isbn(),
                     authorFacade.findByBookId(bookDTO.id()).toString(),
@@ -657,36 +602,4 @@ public class BookCommands extends AbstractShellComponent {
             System.out.println("\n\u001B[36m</>\u001B[0m:Cool, I’ll just… put all these back by myself...and whisper *maybe next time* to the shelves... Again.\n");
         }
     }
-
-    @Command(command = "suggest-shelf", description = "Use AI to recommend optimal shelf placement for a book.")
-    public void suggestBookShelf(){
-        System.out.println("Book should be placed on Shelf: G-16");
-    }
-
-
-
-    public boolean addScanResultCommand(BookMetaDataResponse bookMetaData,String isbn) {
-        String title = bookMetaData.title();
-        String authors =bookMetaData.authors().toString();
-//        String publishingDate = bookMetaData.pulblishedDate();
-//        String categories = bookMetaData.categories().toString();
-        String description = bookMetaData.description();
-//        System.out.println("\n\u001B[36m</>\u001B[0m:");
-//
-//        System.out.printf(""
-//                + "========================================\n"
-//                + "📚  Book Metadata\n"
-//                + "========================================\n"
-//                + "\n"
-//                + "ISBN:              %s\n"
-//                + "Title:             %s\n"
-//                + "Authors:           %s\n"
-//                + "\n"
-//                + "Description:\n"
-//                + "%s\n"
-//                + "\n"
-//                + "========================================\n",isbn,title,authors,description);
-        System.out.println();
-        return cliPrompt.promptBookConfirmation();
-    }
 }
diff --git a/src/main/java/com/penrose/bibby/cli/command/book/ScanMode.java b/src/main/java/com/penrose/bibby/cli/command/book/ScanMode.java
index ed51acb..3463df7 100644
--- a/src/main/java/com/penrose/bibby/cli/command/book/ScanMode.java
+++ b/src/main/java/com/penrose/bibby/cli/command/book/ScanMode.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.cli.commands.book;
+package com.penrose.bibby.cli.command.book;
 
 public enum ScanMode {
     NONE,
diff --git a/src/main/java/com/penrose/bibby/cli/command/bookcase/BookcaseCommands.java b/src/main/java/com/penrose/bibby/cli/command/bookcase/BookcaseCommands.java
index d8dbacb..66c15c2 100644
--- a/src/main/java/com/penrose/bibby/cli/command/bookcase/BookcaseCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/command/bookcase/BookcaseCommands.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.cli.commands.bookcase;
+package com.penrose.bibby.cli.command.bookcase;
 
-import com.penrose.bibby.cli.commands.book.BookCommands;
+import com.penrose.bibby.cli.command.book.BookCommands;
 import com.penrose.bibby.cli.prompt.application.CliPromptService;
 import com.penrose.bibby.cli.prompt.domain.PromptOptions;
 import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
@@ -14,8 +14,6 @@ import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFac
 import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
 import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDetailView;
 import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookSummary;
-import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
-import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
 import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
 import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfSummary;
 import java.util.*;
diff --git a/src/main/java/com/penrose/bibby/cli/command/library/LibraryCommands.java b/src/main/java/com/penrose/bibby/cli/command/library/LibraryCommands.java
index d022708..1a36e5a 100644
--- a/src/main/java/com/penrose/bibby/cli/command/library/LibraryCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/command/library/LibraryCommands.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.cli.commands.library;
+package com.penrose.bibby.cli.command.library;
 
 import org.slf4j.Logger;
 import org.springframework.shell.command.annotation.Command;
