diff --git a/docs/the-devlogs/devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md b/docs/the-devlogs/devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md
index de8be1d..4ed22ba 100644
--- a/docs/the-devlogs/devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md
+++ b/docs/the-devlogs/devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md
@@ -110,7 +110,9 @@ public class BookcaseService {
 **After (Initial Implementation):**
 
 ```java
-import com.penrose.bibby.library.shelf.*;
+
+import com.penrose.bibby.library.shelf.domain.ShelfFactory;
+import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;
 
 public class BookcaseService {
     private final ShelfFactory shelfFactory;
@@ -164,10 +166,7 @@ Wildcard imports obscure class origins and can cause naming conflicts.
 **Corrected:**
 
 ```java
-import com.penrose.bibby.library.shelf.Shelf;
-import com.penrose.bibby.library.shelf.ShelfEntity;
-import com.penrose.bibby.library.shelf.ShelfFactory;
-import com.penrose.bibby.library.shelf.ShelfJpaRepository;
+
 ```
 
 #### Issue 2: Overcomplicated Type Conversion
@@ -214,10 +213,9 @@ private final ShelfFactory shelfFactory;
 ```java
 package com.penrose.bibby.library.bookcase;
 
-import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
-import com.penrose.bibby.library.bookcase.repository.BookcaseRepository;
-import com.penrose.bibby.library.shelf.ShelfFactory;
-import com.penrose.bibby.library.shelf.ShelfJpaRepository;
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseRepository;
+import com.penrose.bibby.library.shelf.domain.ShelfFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.http.HttpStatus;
@@ -228,14 +226,14 @@ import org.springframework.web.server.ResponseStatusException;
 public class BookcaseService {
     private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
     private final BookcaseRepository bookcaseRepository;
-    private final ShelfJpaRepository shelfRepository;
+    private final com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository shelfRepository;
     private final ShelfFactory shelfFactory;
     private final ResponseStatusException existingRecordError =
             new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");
 
     public BookcaseService(
             BookcaseRepository bookcaseRepository,
-            ShelfJpaRepository shelfJpaRepository,
+            com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository shelfJpaRepository,
             ShelfFactory shelfFactory
     ) {
         this.bookcaseRepository = bookcaseRepository;
diff --git a/docs/the-devlogs/devlog-2025-12-03-ImportUpdates-PersistenceTypoFix.md b/docs/the-devlogs/devlog-2025-12-03-ImportUpdates-PersistenceTypoFix.md
index 5ce209f..929d124 100644
--- a/docs/the-devlogs/devlog-2025-12-03-ImportUpdates-PersistenceTypoFix.md
+++ b/docs/the-devlogs/devlog-2025-12-03-ImportUpdates-PersistenceTypoFix.md
@@ -134,9 +134,8 @@ After this refactor, imports in dependent files follow a clear pattern:
 
 ```java
 // Domain-specific imports grouped by sub-package
-import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
-import com.penrose.bibby.library.bookcase.repository.BookcaseRepository;
-import com.penrose.bibby.library.bookcase.service.BookcaseService;
+
+
 ```
 
 The sub-package names in the import path now communicate architectural layer:
diff --git a/docs/the-specs/001-DomainEntity-Layer-Separation.md b/docs/the-specs/001-DomainEntity-Layer-Separation.md
index 69175ed..499b1cc 100644
--- a/docs/the-specs/001-DomainEntity-Layer-Separation.md
+++ b/docs/the-specs/001-DomainEntity-Layer-Separation.md
@@ -19,7 +19,7 @@ private AuthorEntity authorEntity;  // ❌ Domain → Persistence dependency
 
 - `com.penrose.bibby.library.book.domain.Book` - Domain model with persistence dependency
 - `com.penrose.bibby.library.book.mapping.BookMapper` - Expects `AuthorEntity` parameter
-- `com.penrose.bibby.library.author.AuthorEntity` - Persistence entity that needs domain counterpart
+- `com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity` - Persistence entity that needs domain counterpart
 
 ### What Exists
 
@@ -41,7 +41,7 @@ Based on `BookMapper`, the established pattern is:
 
 ### 1. Create Author Domain Model
 
-**File:** `com.penrose.bibby.library.author.Author`
+**File:** `com.penrose.bibby.library.author.domain.Author`
 
 **Requirements:**
 
@@ -56,7 +56,7 @@ Based on `BookMapper`, the established pattern is:
 
 ### 2. Create AuthorMapper
 
-**File:** `com.penrose.bibby.library.author.AuthorMapper`
+**File:** `com.penrose.bibby.library.author.infrastructure.mapping.AuthorMapper`
 
 **Requirements:**
 
@@ -102,7 +102,7 @@ public static Author toDomain(AuthorEntity e) {
 
 **Changes required:**
 
-- Add import: `import com.penrose.bibby.library.author.AuthorMapper;`
+- Add import: `import com.penrose.bibby.library.author.infrastructure.mapping.AuthorMapper;`
 - Change line: `book.setAuthor(authorEntity);`
 - To: `book.setAuthor(AuthorMapper.toDomain(authorEntity));`
 - Keep method signature accepting `AuthorEntity` (that's correct - mapper receives entities)
diff --git a/src/main/java/com/penrose/bibby/cli/BookCommands.java b/src/main/java/com/penrose/bibby/cli/BookCommands.java
index 9db677c..3aa764f 100644
--- a/src/main/java/com/penrose/bibby/cli/BookCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/BookCommands.java
@@ -1,17 +1,21 @@
 package com.penrose.bibby.cli;
 
-import com.penrose.bibby.library.author.Author;
-import com.penrose.bibby.library.author.AuthorEntity;
-import com.penrose.bibby.library.author.AuthorService;
+import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.author.application.AuthorService;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
 import com.penrose.bibby.library.book.api.BookRequestDTO;
 import com.penrose.bibby.library.book.infrastructure.mapping.BookMapper;
 import com.penrose.bibby.library.book.application.BookService;
-import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
-import com.penrose.bibby.library.bookcase.service.BookcaseService;
-import com.penrose.bibby.library.shelf.*;
-
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.bookcase.application.BookcaseService;
+
+import com.penrose.bibby.library.shelf.application.ShelfService;
+import com.penrose.bibby.library.shelf.domain.Shelf;
+import com.penrose.bibby.library.shelf.domain.ShelfDomainRepositoryImpl;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.shelf.infrastructure.mapping.ShelfMapper;
 import com.penrose.bibby.util.LoadingBar;
 import org.springframework.shell.command.annotation.Command;
 import org.springframework.shell.component.flow.ComponentFlow;
diff --git a/src/main/java/com/penrose/bibby/cli/BookcaseCommands.java b/src/main/java/com/penrose/bibby/cli/BookcaseCommands.java
index ecfb1d5..052933a 100644
--- a/src/main/java/com/penrose/bibby/cli/BookcaseCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/BookcaseCommands.java
@@ -4,11 +4,11 @@ import com.penrose.bibby.library.book.api.BookDetailView;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import com.penrose.bibby.library.book.application.BookService;
 import com.penrose.bibby.library.book.api.BookSummary;
-import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
-import com.penrose.bibby.library.bookcase.service.BookcaseService;
-import com.penrose.bibby.library.shelf.ShelfEntity;
-import com.penrose.bibby.library.shelf.ShelfService;
-import com.penrose.bibby.library.shelf.ShelfSummary;
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.bookcase.application.BookcaseService;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.shelf.application.ShelfService;
+import com.penrose.bibby.library.shelf.api.ShelfSummary;
 import org.springframework.shell.command.annotation.Command;
 import org.springframework.shell.component.flow.ComponentFlow;
 import org.springframework.shell.standard.AbstractShellComponent;
diff --git a/src/main/java/com/penrose/bibby/cli/CliPromptService.java b/src/main/java/com/penrose/bibby/cli/CliPromptService.java
index 39f58d2..3f8dc67 100644
--- a/src/main/java/com/penrose/bibby/cli/CliPromptService.java
+++ b/src/main/java/com/penrose/bibby/cli/CliPromptService.java
@@ -1,9 +1,9 @@
 package com.penrose.bibby.cli;
 
-import com.penrose.bibby.library.author.Author;
-import com.penrose.bibby.library.bookcase.service.BookcaseService;
-import com.penrose.bibby.library.shelf.ShelfEntity;
-import com.penrose.bibby.library.shelf.ShelfService;
+import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.bookcase.application.BookcaseService;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.shelf.application.ShelfService;
 import org.springframework.shell.component.flow.ComponentFlow;
 import org.springframework.stereotype.Component;
 
diff --git a/src/main/java/com/penrose/bibby/library/author/api/AuthorFacade.java b/src/main/java/com/penrose/bibby/library/author/api/AuthorFacade.java
index 02c3c4d..42dae9a 100644
--- a/src/main/java/com/penrose/bibby/library/author/api/AuthorFacade.java
+++ b/src/main/java/com/penrose/bibby/library/author/api/AuthorFacade.java
@@ -1,4 +1,7 @@
-package com.penrose.bibby.library.author;
+package com.penrose.bibby.library.author.api;
+
+import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
 
 public interface AuthorFacade {
     AuthorEntity getOrCreateAuthorEntity(Author author);
diff --git a/src/main/java/com/penrose/bibby/library/author/application/AuthorService.java b/src/main/java/com/penrose/bibby/library/author/application/AuthorService.java
index 54135ab..78e8445 100644
--- a/src/main/java/com/penrose/bibby/library/author/application/AuthorService.java
+++ b/src/main/java/com/penrose/bibby/library/author/application/AuthorService.java
@@ -1,5 +1,8 @@
-package com.penrose.bibby.library.author;
+package com.penrose.bibby.library.author.application;
 
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.author.domain.AuthorFactory;
+import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
 import org.springframework.stereotype.Service;
 import java.util.List;
 import java.util.Optional;
diff --git a/src/main/java/com/penrose/bibby/library/author/domain/Author.java b/src/main/java/com/penrose/bibby/library/author/domain/Author.java
index a8672f3..34ee46e 100644
--- a/src/main/java/com/penrose/bibby/library/author/domain/Author.java
+++ b/src/main/java/com/penrose/bibby/library/author/domain/Author.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.author;
+package com.penrose.bibby.library.author.domain;
 
 public class Author {
     private Long authorId;
diff --git a/src/main/java/com/penrose/bibby/library/author/domain/AuthorFactory.java b/src/main/java/com/penrose/bibby/library/author/domain/AuthorFactory.java
index efb6ca3..742d9ae 100644
--- a/src/main/java/com/penrose/bibby/library/author/domain/AuthorFactory.java
+++ b/src/main/java/com/penrose/bibby/library/author/domain/AuthorFactory.java
@@ -1,5 +1,6 @@
-package com.penrose.bibby.library.author;
+package com.penrose.bibby.library.author.domain;
 
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
 import org.springframework.stereotype.Component;
 
 @Component
diff --git a/src/main/java/com/penrose/bibby/library/author/infrastructure/entity/AuthorEntity.java b/src/main/java/com/penrose/bibby/library/author/infrastructure/entity/AuthorEntity.java
index f74c22d..5455973 100644
--- a/src/main/java/com/penrose/bibby/library/author/infrastructure/entity/AuthorEntity.java
+++ b/src/main/java/com/penrose/bibby/library/author/infrastructure/entity/AuthorEntity.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.author.infrastructure;
+package com.penrose.bibby.library.author.infrastructure.entity;
 
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import jakarta.persistence.*;
diff --git a/src/main/java/com/penrose/bibby/library/author/infrastructure/mapping/AuthorMapper.java b/src/main/java/com/penrose/bibby/library/author/infrastructure/mapping/AuthorMapper.java
index 65d3f6e..b2a3487 100644
--- a/src/main/java/com/penrose/bibby/library/author/infrastructure/mapping/AuthorMapper.java
+++ b/src/main/java/com/penrose/bibby/library/author/infrastructure/mapping/AuthorMapper.java
@@ -1,6 +1,7 @@
-package com.penrose.bibby.library.author.infrastructure;
+package com.penrose.bibby.library.author.infrastructure.mapping;
 
 import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
 
 public class AuthorMapper {
 
diff --git a/src/main/java/com/penrose/bibby/library/author/infrastructure/mapping/AuthorMapperTwo.java b/src/main/java/com/penrose/bibby/library/author/infrastructure/mapping/AuthorMapperTwo.java
index c7b6215..eeedc46 100644
--- a/src/main/java/com/penrose/bibby/library/author/infrastructure/mapping/AuthorMapperTwo.java
+++ b/src/main/java/com/penrose/bibby/library/author/infrastructure/mapping/AuthorMapperTwo.java
@@ -1,7 +1,8 @@
-package com.penrose.bibby.library.author.infrastructure;
+package com.penrose.bibby.library.author.infrastructure.mapping;
 
 import com.penrose.bibby.library.author.domain.Author;
 import com.penrose.bibby.library.author.domain.AuthorFactory;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
 import org.springframework.stereotype.Component;
 
 import java.util.HashSet;
diff --git a/src/main/java/com/penrose/bibby/library/author/infrastructure/repository/AuthorRepository.java b/src/main/java/com/penrose/bibby/library/author/infrastructure/repository/AuthorRepository.java
index c333118..9250bc6 100644
--- a/src/main/java/com/penrose/bibby/library/author/infrastructure/repository/AuthorRepository.java
+++ b/src/main/java/com/penrose/bibby/library/author/infrastructure/repository/AuthorRepository.java
@@ -1,5 +1,6 @@
-package com.penrose.bibby.library.author.infrastructure;
+package com.penrose.bibby.library.author.infrastructure.repository;
 
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
 import org.springframework.data.repository.query.Param;
diff --git a/src/main/java/com/penrose/bibby/library/book/api/BookRequestDTO.java b/src/main/java/com/penrose/bibby/library/book/api/BookRequestDTO.java
index 5ae358b..74a3241 100644
--- a/src/main/java/com/penrose/bibby/library/book/api/BookRequestDTO.java
+++ b/src/main/java/com/penrose/bibby/library/book/api/BookRequestDTO.java
@@ -1,6 +1,6 @@
 package com.penrose.bibby.library.book.api;
 
-import com.penrose.bibby.library.author.Author;
+import com.penrose.bibby.library.author.domain.Author;
 import java.util.List;
 
 public record BookRequestDTO(String title, List<Author> authors) {
diff --git a/src/main/java/com/penrose/bibby/library/book/application/BookService.java b/src/main/java/com/penrose/bibby/library/book/application/BookService.java
index ab08a6a..179ab8d 100644
--- a/src/main/java/com/penrose/bibby/library/book/application/BookService.java
+++ b/src/main/java/com/penrose/bibby/library/book/application/BookService.java
@@ -1,8 +1,8 @@
 package com.penrose.bibby.library.book.application;
 
-import com.penrose.bibby.library.author.Author;
-import com.penrose.bibby.library.author.AuthorEntity;
-import com.penrose.bibby.library.author.AuthorService;
+import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.author.application.AuthorService;
 import com.penrose.bibby.library.book.domain.Book;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import com.penrose.bibby.library.book.domain.BookFactory;
@@ -11,9 +11,9 @@ import com.penrose.bibby.library.book.api.BookRequestDTO;
 import com.penrose.bibby.library.book.api.BookSummary;
 import com.penrose.bibby.library.book.infrastructure.mapping.BookMapper;
 import com.penrose.bibby.library.book.infrastructure.repository.BookRepository;
-import com.penrose.bibby.library.shelf.ShelfEntity;
-import com.penrose.bibby.library.shelf.ShelfFacade;
-import com.penrose.bibby.library.shelf.ShelfService;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.shelf.api.ShelfFacade;
+import com.penrose.bibby.library.shelf.application.ShelfService;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;
 
@@ -23,20 +23,22 @@ import java.util.List;
 import java.util.Optional;
 
 @Service
-public class BookService {
+    public class BookService {
 
     private final BookRepository bookRepository;
     private final AuthorService authorService;
     private final BookFactory BookFactory;
     private final ShelfService shelfService;
     private final BookMapper bookMapper;
+    private final ShelfFacade shelfFacade;
 
-    public BookService(BookRepository bookRepository, AuthorService authorService, BookFactory bookFactory, ShelfService shelfService, BookMapper bookMapper){
+    public BookService(BookRepository bookRepository, AuthorService authorService, BookFactory bookFactory, ShelfService shelfService, BookMapper bookMapper, ShelfFacade shelfFacade){
         this.bookRepository = bookRepository;
         this.authorService = authorService;
         this.BookFactory = bookFactory;
         this.shelfService = shelfService;
         this.bookMapper = bookMapper;
+        this.shelfFacade = shelfFacade;
     }
 
     // ============================================================
diff --git a/src/main/java/com/penrose/bibby/library/book/application/IsbnEnrichmentService.java b/src/main/java/com/penrose/bibby/library/book/application/IsbnEnrichmentService.java
index 29421db..71ae26d 100644
--- a/src/main/java/com/penrose/bibby/library/book/application/IsbnEnrichmentService.java
+++ b/src/main/java/com/penrose/bibby/library/book/application/IsbnEnrichmentService.java
@@ -1,9 +1,9 @@
 package com.penrose.bibby.library.book.application;
 
-import com.penrose.bibby.library.author.Author;
-import com.penrose.bibby.library.author.AuthorEntity;
-import com.penrose.bibby.library.author.AuthorFactory;
-import com.penrose.bibby.library.author.AuthorService;
+import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.author.domain.AuthorFactory;
+import com.penrose.bibby.library.author.application.AuthorService;
 import com.penrose.bibby.library.book.domain.*;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
diff --git a/src/main/java/com/penrose/bibby/library/book/domain/Book.java b/src/main/java/com/penrose/bibby/library/book/domain/Book.java
index 1d0539d..657c1a4 100644
--- a/src/main/java/com/penrose/bibby/library/book/domain/Book.java
+++ b/src/main/java/com/penrose/bibby/library/book/domain/Book.java
@@ -4,9 +4,9 @@ import java.time.LocalDate;
 import java.util.HashSet;
 import java.util.Objects;
 
-import com.penrose.bibby.library.author.Author;
+import com.penrose.bibby.library.author.domain.Author;
 import com.penrose.bibby.library.genre.Genre;
-import com.penrose.bibby.library.shelf.Shelf;
+import com.penrose.bibby.library.shelf.domain.Shelf;
 
 public class Book {
     private Long id;
diff --git a/src/main/java/com/penrose/bibby/library/book/domain/BookFactory.java b/src/main/java/com/penrose/bibby/library/book/domain/BookFactory.java
index ffa1bce..d919ff4 100644
--- a/src/main/java/com/penrose/bibby/library/book/domain/BookFactory.java
+++ b/src/main/java/com/penrose/bibby/library/book/domain/BookFactory.java
@@ -1,7 +1,7 @@
 package com.penrose.bibby.library.book.domain;
 
-import com.penrose.bibby.library.author.Author;
-import com.penrose.bibby.library.author.AuthorEntity;
+import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import org.springframework.stereotype.Component;
 
diff --git a/src/main/java/com/penrose/bibby/library/book/infrastructure/entity/BookEntity.java b/src/main/java/com/penrose/bibby/library/book/infrastructure/entity/BookEntity.java
index c70d04e..9e41d5a 100644
--- a/src/main/java/com/penrose/bibby/library/book/infrastructure/entity/BookEntity.java
+++ b/src/main/java/com/penrose/bibby/library/book/infrastructure/entity/BookEntity.java
@@ -1,6 +1,6 @@
 package com.penrose.bibby.library.book.infrastructure.entity;
 
-import com.penrose.bibby.library.author.AuthorEntity;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
 import com.penrose.bibby.library.book.domain.AvailabilityStatus;
 import jakarta.persistence.*;
 
diff --git a/src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapper.java b/src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapper.java
index 3bb216f..80ed7be 100644
--- a/src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapper.java
+++ b/src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapper.java
@@ -1,15 +1,15 @@
 package com.penrose.bibby.library.book.infrastructure.mapping;
 
-import com.penrose.bibby.library.author.Author;
-import com.penrose.bibby.library.author.AuthorEntity;
-import com.penrose.bibby.library.author.AuthorMapper;
+import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.author.infrastructure.mapping.AuthorMapper;
 import com.penrose.bibby.library.book.domain.AvailabilityStatus;
 import com.penrose.bibby.library.book.domain.Book;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.shelf.Shelf;
-import com.penrose.bibby.library.shelf.ShelfDomainRepositoryImpl;
-import com.penrose.bibby.library.shelf.ShelfEntity;
-import com.penrose.bibby.library.shelf.ShelfMapper;
+import com.penrose.bibby.library.shelf.domain.Shelf;
+import com.penrose.bibby.library.shelf.domain.ShelfDomainRepositoryImpl;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.shelf.infrastructure.mapping.ShelfMapper;
 import org.springframework.stereotype.Component;
 
 import java.util.HashSet;
diff --git a/src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapperTwo.java b/src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapperTwo.java
index c6c505a..1e71dbb 100644
--- a/src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapperTwo.java
+++ b/src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapperTwo.java
@@ -1,8 +1,8 @@
 package com.penrose.bibby.library.book.infrastructure.mapping;
 
-import com.penrose.bibby.library.author.Author;
-import com.penrose.bibby.library.author.AuthorEntity;
-import com.penrose.bibby.library.author.AuthorMapperTwo;
+import com.penrose.bibby.library.author.domain.Author;
+import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.author.infrastructure.mapping.AuthorMapperTwo;
 import com.penrose.bibby.library.book.domain.Book;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import com.penrose.bibby.library.book.domain.BookFactory;
diff --git a/src/main/java/com/penrose/bibby/library/book/infrastructure/repository/BookDomainRepositoryImpl.java b/src/main/java/com/penrose/bibby/library/book/infrastructure/repository/BookDomainRepositoryImpl.java
index b320235..d055060 100644
--- a/src/main/java/com/penrose/bibby/library/book/infrastructure/repository/BookDomainRepositoryImpl.java
+++ b/src/main/java/com/penrose/bibby/library/book/infrastructure/repository/BookDomainRepositoryImpl.java
@@ -1,6 +1,6 @@
 package com.penrose.bibby.library.book.infrastructure.repository;
 
-import com.penrose.bibby.library.author.*;
+import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
 import com.penrose.bibby.library.book.domain.Book;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import com.penrose.bibby.library.book.infrastructure.mapping.BookMapperTwo;
diff --git a/src/main/java/com/penrose/bibby/library/bookcase/api/BookcaseDTO.java b/src/main/java/com/penrose/bibby/library/bookcase/api/BookcaseDTO.java
index 5be0520..e7c9ccc 100644
--- a/src/main/java/com/penrose/bibby/library/bookcase/api/BookcaseDTO.java
+++ b/src/main/java/com/penrose/bibby/library/bookcase/api/BookcaseDTO.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.bookcase;
+package com.penrose.bibby.library.bookcase.api;
 
 public record BookcaseDTO(Long bookcaseId, String bookcaseLabel, int shelfCapacity, int bookCapacity) {
 }
diff --git a/src/main/java/com/penrose/bibby/library/bookcase/api/BookcaseFacade.java b/src/main/java/com/penrose/bibby/library/bookcase/api/BookcaseFacade.java
index 8e6e135..389d636 100644
--- a/src/main/java/com/penrose/bibby/library/bookcase/api/BookcaseFacade.java
+++ b/src/main/java/com/penrose/bibby/library/bookcase/api/BookcaseFacade.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.bookcase;
+package com.penrose.bibby.library.bookcase.api;
 
 public class BookcaseFacade {
 }
diff --git a/src/main/java/com/penrose/bibby/library/bookcase/application/BookcaseService.java b/src/main/java/com/penrose/bibby/library/bookcase/application/BookcaseService.java
index 264e280..d1c1051 100644
--- a/src/main/java/com/penrose/bibby/library/bookcase/application/BookcaseService.java
+++ b/src/main/java/com/penrose/bibby/library/bookcase/application/BookcaseService.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.bookcase.service;
-import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
-import com.penrose.bibby.library.bookcase.repository.BookcaseRepository;
+package com.penrose.bibby.library.bookcase.application;
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseRepository;
 import com.penrose.bibby.library.shelf.domain.ShelfFactory;
 import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;
 import org.slf4j.Logger;
diff --git a/src/main/java/com/penrose/bibby/library/bookcase/domain/Bookcase.java b/src/main/java/com/penrose/bibby/library/bookcase/domain/Bookcase.java
index 1cbe584..69c5b06 100644
--- a/src/main/java/com/penrose/bibby/library/bookcase/domain/Bookcase.java
+++ b/src/main/java/com/penrose/bibby/library/bookcase/domain/Bookcase.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.bookcase;
+package com.penrose.bibby.library.bookcase.domain;
 
 public class Bookcase {
     private Long bookcaseId;
diff --git a/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseEntity.java b/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseEntity.java
index dfb007b..cbadb67 100644
--- a/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseEntity.java
+++ b/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseEntity.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.bookcase.persistence;
+package com.penrose.bibby.library.bookcase.infrastructure;
 
 import jakarta.persistence.*;
 
diff --git a/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseMapper.java b/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseMapper.java
index f9b939e..bbd393c 100644
--- a/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseMapper.java
+++ b/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseMapper.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.bookcase.mapping;
+package com.penrose.bibby.library.bookcase.infrastructure;
 
-import com.penrose.bibby.library.bookcase.Bookcase;
+import com.penrose.bibby.library.bookcase.domain.Bookcase;
 
 public class BookcaseMapper {
 
diff --git a/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseRepository.java b/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseRepository.java
index efe319b..5df60da 100644
--- a/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseRepository.java
+++ b/src/main/java/com/penrose/bibby/library/bookcase/infrastructure/BookcaseRepository.java
@@ -1,6 +1,5 @@
-package com.penrose.bibby.library.bookcase.repository;
+package com.penrose.bibby.library.bookcase.infrastructure;
 
-import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.stereotype.Repository;
 
diff --git a/src/main/java/com/penrose/bibby/library/shelf/api/ShelfFacade.java b/src/main/java/com/penrose/bibby/library/shelf/api/ShelfFacade.java
index 3f89d75..b666f7d 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/api/ShelfFacade.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/api/ShelfFacade.java
@@ -1,4 +1,6 @@
-package com.penrose.bibby.library.shelf;
+package com.penrose.bibby.library.shelf.api;
+
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
 
 import java.util.Optional;
 
diff --git a/src/main/java/com/penrose/bibby/library/shelf/api/ShelfOptionResponse.java b/src/main/java/com/penrose/bibby/library/shelf/api/ShelfOptionResponse.java
index 86fa82a..489b58a 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/api/ShelfOptionResponse.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/api/ShelfOptionResponse.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.shelf;
+package com.penrose.bibby.library.shelf.api;
 
 public record ShelfOptionResponse(
         Long shelfId,
diff --git a/src/main/java/com/penrose/bibby/library/shelf/api/ShelfSummary.java b/src/main/java/com/penrose/bibby/library/shelf/api/ShelfSummary.java
index 7ae592f..1e894d0 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/api/ShelfSummary.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/api/ShelfSummary.java
@@ -1,3 +1,3 @@
-package com.penrose.bibby.library.shelf;
+package com.penrose.bibby.library.shelf.api;
 
 public record ShelfSummary(Long shelfId, String label, long bookCount) { }
diff --git a/src/main/java/com/penrose/bibby/library/shelf/application/ShelfService.java b/src/main/java/com/penrose/bibby/library/shelf/application/ShelfService.java
index 7f33e26..fae8e39 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/application/ShelfService.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/application/ShelfService.java
@@ -1,10 +1,14 @@
-package com.penrose.bibby.library.shelf;
+package com.penrose.bibby.library.shelf.application;
 
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;
+import com.penrose.bibby.library.shelf.api.ShelfOptionResponse;
+import com.penrose.bibby.library.shelf.api.ShelfSummary;
 import org.springframework.stereotype.Service;
 
 import com.penrose.bibby.library.book.infrastructure.repository.BookRepository;
-import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
-import com.penrose.bibby.library.bookcase.repository.BookcaseRepository;
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseRepository;
 
 import java.util.List;
 import java.util.Optional;
diff --git a/src/main/java/com/penrose/bibby/library/shelf/domain/Shelf.java b/src/main/java/com/penrose/bibby/library/shelf/domain/Shelf.java
index 4515129..45df31b 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/domain/Shelf.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/domain/Shelf.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.shelf;
+package com.penrose.bibby.library.shelf.domain;
 
 import com.penrose.bibby.library.book.domain.Book;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
diff --git a/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfDomainRepository.java b/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfDomainRepository.java
index 2204a00..b780d0e 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfDomainRepository.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfDomainRepository.java
@@ -1,6 +1,4 @@
-package com.penrose.bibby.library.shelf.infrastructure;
-
-import com.penrose.bibby.library.shelf.domain.Shelf;
+package com.penrose.bibby.library.shelf.domain;
 
 public interface ShelfDomainRepository {
 
diff --git a/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfDomainRepositoryImpl.java b/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfDomainRepositoryImpl.java
index 4b55307..5b6c7d5 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfDomainRepositoryImpl.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfDomainRepositoryImpl.java
@@ -1,12 +1,10 @@
-package com.penrose.bibby.library.shelf;
+package com.penrose.bibby.library.shelf.domain;
 
 import com.penrose.bibby.library.book.domain.Book;
 import com.penrose.bibby.library.book.infrastructure.repository.BookDomainRepository;
-import com.penrose.bibby.library.shelf.domain.Shelf;
-import com.penrose.bibby.library.shelf.infrastructure.ShelfDomainRepository;
-import com.penrose.bibby.library.shelf.infrastructure.ShelfEntity;
-import com.penrose.bibby.library.shelf.infrastructure.ShelfJpaRepository;
-import com.penrose.bibby.library.shelf.infrastructure.ShelfMapper;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;
+import com.penrose.bibby.library.shelf.infrastructure.mapping.ShelfMapper;
 import org.springframework.stereotype.Component;
 
 import java.util.List;
diff --git a/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfFactory.java b/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfFactory.java
index 90d2aac..0cbb41f 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfFactory.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/domain/ShelfFactory.java
@@ -1,5 +1,6 @@
-package com.penrose.bibby.library.shelf;
+package com.penrose.bibby.library.shelf.domain;
 
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
 import org.springframework.stereotype.Component;
 
 @Component
diff --git a/src/main/java/com/penrose/bibby/library/shelf/infrastructure/entity/ShelfEntity.java b/src/main/java/com/penrose/bibby/library/shelf/infrastructure/entity/ShelfEntity.java
index c52df64..c8c7bbc 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/infrastructure/entity/ShelfEntity.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/infrastructure/entity/ShelfEntity.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.shelf.infrastructure;
+package com.penrose.bibby.library.shelf.infrastructure.entity;
 
 import jakarta.persistence.Entity;
 import jakarta.persistence.GeneratedValue;
diff --git a/src/main/java/com/penrose/bibby/library/shelf/infrastructure/mapping/ShelfMapper.java b/src/main/java/com/penrose/bibby/library/shelf/infrastructure/mapping/ShelfMapper.java
index 8eeb7fd..43ae120 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/infrastructure/mapping/ShelfMapper.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/infrastructure/mapping/ShelfMapper.java
@@ -1,7 +1,8 @@
-package com.penrose.bibby.library.shelf.infrastructure;
+package com.penrose.bibby.library.shelf.infrastructure.mapping;
 
 import com.penrose.bibby.library.book.domain.Book;
 import com.penrose.bibby.library.shelf.domain.Shelf;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
 import org.springframework.stereotype.Component;
 
 import java.util.List;
diff --git a/src/main/java/com/penrose/bibby/library/shelf/infrastructure/repository/ShelfJpaRepository.java b/src/main/java/com/penrose/bibby/library/shelf/infrastructure/repository/ShelfJpaRepository.java
index 5df890c..02c5d5b 100644
--- a/src/main/java/com/penrose/bibby/library/shelf/infrastructure/repository/ShelfJpaRepository.java
+++ b/src/main/java/com/penrose/bibby/library/shelf/infrastructure/repository/ShelfJpaRepository.java
@@ -1,6 +1,7 @@
-package com.penrose.bibby.library.shelf.infrastructure;
+package com.penrose.bibby.library.shelf.infrastructure.repository;
 
 import com.penrose.bibby.library.shelf.api.ShelfSummary;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
 import org.springframework.data.repository.query.Param;
diff --git a/src/main/java/com/penrose/bibby/util/StartupRunner.java b/src/main/java/com/penrose/bibby/util/StartupRunner.java
index 2abbcad..fc1458a 100644
--- a/src/main/java/com/penrose/bibby/util/StartupRunner.java
+++ b/src/main/java/com/penrose/bibby/util/StartupRunner.java
@@ -1,5 +1,5 @@
 package com.penrose.bibby.util;
-import com.penrose.bibby.library.shelf.ShelfService;
+import com.penrose.bibby.library.shelf.application.ShelfService;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.CommandLineRunner;
 import org.springframework.stereotype.Component;
diff --git a/src/main/java/com/penrose/bibby/web/book/BookController.java b/src/main/java/com/penrose/bibby/web/book/BookController.java
index 6b8402d..968603a 100644
--- a/src/main/java/com/penrose/bibby/web/book/BookController.java
+++ b/src/main/java/com/penrose/bibby/web/book/BookController.java
@@ -1,6 +1,6 @@
 package com.penrose.bibby.web.book;
 
-import com.penrose.bibby.library.author.AuthorRepository;
+import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
 import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
 import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
 import com.penrose.bibby.library.book.api.BookPlacementResponse;
@@ -9,10 +9,10 @@ import com.penrose.bibby.library.book.api.BookShelfAssignmentRequest;
 import com.penrose.bibby.library.book.application.IsbnEnrichmentService;
 import com.penrose.bibby.library.book.application.BookInfoService;
 import com.penrose.bibby.library.book.application.BookService;
-import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
-import com.penrose.bibby.library.bookcase.service.BookcaseService;
-import com.penrose.bibby.library.shelf.ShelfEntity;
-import com.penrose.bibby.library.shelf.ShelfService;
+import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.bookcase.application.BookcaseService;
+import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.shelf.application.ShelfService;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;
diff --git a/src/main/java/com/penrose/bibby/web/bookcase/BookCaseController.java b/src/main/java/com/penrose/bibby/web/bookcase/BookCaseController.java
index 5816ac7..85a65b9 100644
--- a/src/main/java/com/penrose/bibby/web/bookcase/BookCaseController.java
+++ b/src/main/java/com/penrose/bibby/web/bookcase/BookCaseController.java
@@ -1,6 +1,7 @@
-package com.penrose.bibby.library.bookcase;
+package com.penrose.bibby.web.bookcase;
 
-import com.penrose.bibby.library.bookcase.service.BookcaseService;
+import com.penrose.bibby.library.bookcase.api.BookcaseDTO;
+import com.penrose.bibby.library.bookcase.application.BookcaseService;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.PostMapping;
diff --git a/src/main/java/com/penrose/bibby/web/shelf/ShelfController.java b/src/main/java/com/penrose/bibby/web/shelf/ShelfController.java
index 69b3b0c..05124a1 100644
--- a/src/main/java/com/penrose/bibby/web/shelf/ShelfController.java
+++ b/src/main/java/com/penrose/bibby/web/shelf/ShelfController.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.web;
+package com.penrose.bibby.web.shelf;
 
 import com.penrose.bibby.library.shelf.api.ShelfOptionResponse;
 import com.penrose.bibby.library.shelf.application.ShelfService;
