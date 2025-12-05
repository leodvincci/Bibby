diff --git a/src/main/java/com/penrose/bibby/infrastructure/config/WebClientConfig.java b/src/main/java/com/penrose/bibby/infrastructure/config/WebClientConfig.java
index 7e0bfe7..38219ab 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/config/WebClientConfig.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/config/WebClientConfig.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.util;
+package com.penrose.bibby.infrastructure.config;
 
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/author/AuthorController.java b/src/main/java/com/penrose/bibby/infrastructure/web/author/AuthorController.java
index 0871fdf..8c8d361 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/author/AuthorController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/author/AuthorController.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.web.author;
+package com.penrose.bibby.infrastructure.web.author;
 
 public class AuthorController {
 }
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/book/BookController.java b/src/main/java/com/penrose/bibby/infrastructure/web/book/BookController.java
index 864ba2d..b7cad25 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/book/BookController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/book/BookController.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.web.book;
+package com.penrose.bibby.infrastructure.web.book;
 
 import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
 import com.penrose.bibby.library.book.application.IsbnLookupService;
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/book/BookImportController.java b/src/main/java/com/penrose/bibby/infrastructure/web/book/BookImportController.java
index 6a5b9f0..e40dad5 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/book/BookImportController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/book/BookImportController.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.web.book;
+package com.penrose.bibby.infrastructure.web.book;
 
 import com.penrose.bibby.library.book.infrastructure.external.BookImportRequest;
 import com.penrose.bibby.library.book.infrastructure.external.BookImportResponse;
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/bookcase/BookCaseController.java b/src/main/java/com/penrose/bibby/infrastructure/web/bookcase/BookCaseController.java
index 85a65b9..111c1a6 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/bookcase/BookCaseController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/bookcase/BookCaseController.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.web.bookcase;
+package com.penrose.bibby.infrastructure.web.bookcase;
 
 import com.penrose.bibby.library.bookcase.api.BookcaseDTO;
 import com.penrose.bibby.library.bookcase.application.BookcaseService;
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/shelf/ShelfController.java b/src/main/java/com/penrose/bibby/infrastructure/web/shelf/ShelfController.java
index 05124a1..2b820c0 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/shelf/ShelfController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/shelf/ShelfController.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.web.shelf;
+package com.penrose.bibby.infrastructure.web.shelf;
 
 import com.penrose.bibby.library.shelf.api.ShelfOptionResponse;
 import com.penrose.bibby.library.shelf.application.ShelfService;
