diff --git a/docs/engineering/closed-issues/closed-issue-11.md b/docs/engineering/closed-issues/closed-issue-11.md
index a769f6a..2f8da77 100644
--- a/docs/engineering/closed-issues/closed-issue-11.md
+++ b/docs/engineering/closed-issues/closed-issue-11.md
@@ -36,7 +36,7 @@ The fix was implemented across **four architectural layers** to ensure the invar
 
 ### 1. Domain Layer: `AvailabilityStatus` Enum
 
-**File:** [`src/main/java/com/penrose/bibby/library/book/domain/AvailabilityStatus.java`](../../../src/main/java/com/penrose/bibby/library/catalog/book/core/AvailabilityStatus.java)
+**File:** [`src/main/java/com/penrose/bibby/library/book/domain/AvailabilityStatus.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/core/AvailabilityStatus.java)
 
 ```java
 public enum AvailabilityStatus {
@@ -54,7 +54,7 @@ The enum defines all valid book statuses, with `AVAILABLE` as the designated ini
 
 ### 2. Entity Layer: `BookEntity` Constructor
 
-**File:** [`src/main/java/com/penrose/bibby/library/book/infrastructure/entity/BookEntity.java`](../../../src/main/java/com/penrose/bibby/library/catalog/book/infrastructure/entity/BookEntity.java)
+**File:** [`src/main/java/com/penrose/bibby/library/book/infrastructure/entity/BookEntity.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/entity/BookEntity.java)
 
 **Lines 33-38:**
 ```java
@@ -72,7 +72,7 @@ public BookEntity(String title, HashSet<AuthorEntity> authors) {
 
 ### 3. Factory Layer: `BookFactory` Methods
 
-**File:** [`src/main/java/com/penrose/bibby/library/book/domain/BookFactory.java`](../../../src/main/java/com/penrose/bibby/library/catalog/book/core/BookFactory.java)
+**File:** [`src/main/java/com/penrose/bibby/library/book/domain/BookFactory.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/core/BookFactory.java)
 
 #### 3.1 Entity Creation (CLI Interactive Flow)
 
@@ -114,7 +114,7 @@ public Book createBookDomainFromJSON(String title, String publisher,
 
 ### 4. Service Layer: `BookService.createScannedBook`
 
-**File:** [`src/main/java/com/penrose/bibby/library/book/application/BookService.java`](../../../src/main/java/com/penrose/bibby/library/catalog/book/core/application/BookService.java)
+**File:** [`src/main/java/com/penrose/bibby/library/book/application/BookService.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/core/application/BookService.java)
 
 **Lines 61-80:**
 ```java
@@ -143,7 +143,7 @@ public BookEntity createScannedBook(GoogleBooksResponse googleBooksResponse, Str
 
 ### 5. Mapper Layer: Status Preservation
 
-**File:** [`src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapper.java`](../../../src/main/java/com/penrose/bibby/library/catalog/book/infrastructure/mapping/BookMapper.java)
+**File:** [`src/main/java/com/penrose/bibby/library/book/infrastructure/mapping/BookMapper.java`](../../../src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapper.java)
 
 **Line 54:**
 ```java
diff --git a/docs/the-devlogs/devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md b/docs/the-devlogs/devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md
index 042bbea..7525d34 100644
--- a/docs/the-devlogs/devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md
+++ b/docs/the-devlogs/devlog-2025-11-22-FactoryPattern-ShelfEntity-Improve-OptionalUsage.md
@@ -111,8 +111,8 @@ public class BookcaseService {
 
 ```java
 
-import com.penrose.bibby.library.placement.shelf.core.domain.ShelfFactory;
-import com.penrose.bibby.library.placement.shelf.infrastructure.repository.ShelfJpaRepository;
+import com.penrose.bibby.library.stacks.shelf.core.domain.ShelfFactory;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
 
 public class BookcaseService {
     private final ShelfFactory shelfFactory;
@@ -213,10 +213,10 @@ private final ShelfFactory shelfFactory;
 ```java
 package com.penrose.bibby.library.bookcase;
 
-import com.penrose.bibby.library.placement.bookcase.infrastructure.BookcaseEntity;
-import com.penrose.bibby.library.placement.bookcase.infrastructure.BookcaseRepository;
-import com.penrose.bibby.library.placement.shelf.infrastructure.repository.ShelfJpaRepository;
-import com.penrose.bibby.library.placement.shelf.core.domain.ShelfFactory;
+import com.penrose.bibby.library.stacks.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.stacks.bookcase.infrastructure.BookcaseRepository;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
+import com.penrose.bibby.library.stacks.shelf.core.domain.ShelfFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.http.HttpStatus;
diff --git a/docs/the-devlogs/devlog-2025-12-05-Infrastructure-Consolidation-Domain-Purification.md b/docs/the-devlogs/devlog-2025-12-05-Infrastructure-Consolidation-Domain-Purification.md
index b5ec13f..5e03425 100644
--- a/docs/the-devlogs/devlog-2025-12-05-Infrastructure-Consolidation-Domain-Purification.md
+++ b/docs/the-devlogs/devlog-2025-12-05-Infrastructure-Consolidation-Domain-Purification.md
@@ -105,8 +105,8 @@ public Shelf(String shelfLabel, int shelfPosition, int bookCapacity)
 ```java
 package com.penrose.bibby.library.shelf.domain;
 
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;  // ❌ Infrastructure leak
-import com.penrose.bibby.library.placement.bookcase.core.domain.Bookcase;              // ❌ Cross-aggregate coupling
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;  // ❌ Infrastructure leak
+import com.penrose.bibby.library.stacks.bookcase.core.domain.Bookcase;              // ❌ Cross-aggregate coupling
 
 public class Shelf {
     private Long id;
@@ -133,7 +133,7 @@ public class Shelf {
 ```java
 package com.penrose.bibby.library.shelf.domain;
 
-import com.penrose.bibby.library.catalog.book.core.domain.Book;  // ✓ Domain-to-domain only
+import com.penrose.bibby.library.cataloging.book.core.domain.Book;  // ✓ Domain-to-domain only
 
 import java.util.List;
 
diff --git a/docs/the-devlogs/devlog-2025-12-07-Ports-and-Adapters-Restructuring.md b/docs/the-devlogs/devlog-2025-12-07-Ports-and-Adapters-Restructuring.md
index 9a8333e..6043b24 100644
--- a/docs/the-devlogs/devlog-2025-12-07-Ports-and-Adapters-Restructuring.md
+++ b/docs/the-devlogs/devlog-2025-12-07-Ports-and-Adapters-Restructuring.md
@@ -218,7 +218,7 @@ The interface that Book defines for accessing author data. The Author module pro
 package com.penrose.bibby.library.book.application;
 
 import com.penrose.bibby.library.author.contracts.AuthorFacade;  // ❌ Depends on Author
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;    // ❌ Depends on Author
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;    // ❌ Depends on Author
 
 public class BookService implements BookFacade {
     private final AuthorFacade authorFacade;
@@ -235,8 +235,8 @@ public class BookService implements BookFacade {
 ```java
 package com.penrose.bibby.library.book.core.application;
 
-import com.penrose.bibby.library.catalog.book.core.domain.AuthorRef;                           // ✓ Book's own type
-import com.penrose.bibby.library.catalog.book.contracts.ports.outbound.AuthorAccessPort;  // ✓ Book's own port
+import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;                           // ✓ Book's own type
+import com.penrose.bibby.library.cataloging.book.contracts.ports.outbound.AuthorAccessPort;  // ✓ Book's own port
 
 public class BookService implements BookFacade {
     private final AuthorAccessPort authorAccessPort;
diff --git a/docs/the-devlogs/devlog-2025-12-09-ShelfAccessPort-CrossModule-Decoupling.md b/docs/the-devlogs/devlog-2025-12-09-ShelfAccessPort-CrossModule-Decoupling.md
index ecb64ae..0f09243 100644
--- a/docs/the-devlogs/devlog-2025-12-09-ShelfAccessPort-CrossModule-Decoupling.md
+++ b/docs/the-devlogs/devlog-2025-12-09-ShelfAccessPort-CrossModule-Decoupling.md
@@ -22,7 +22,7 @@ Also expanded `BookDomainRepository` with update and lookup methods, fixed depen
 `BookService` directly imported `ShelfService`:
 
 ```java
-import com.penrose.bibby.library.placement.shelf.core.application.ShelfService;
+import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
 
 public class BookService implements BookFacade {
     private final ShelfService shelfService;  // Direct coupling
diff --git a/src/main/java/com/penrose/bibby/cli/commands/book/BookCommands.java b/src/main/java/com/penrose/bibby/cli/commands/book/BookCommands.java
index 32a08cc..5e8b729 100644
--- a/src/main/java/com/penrose/bibby/cli/commands/book/BookCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/commands/book/BookCommands.java
@@ -8,16 +8,16 @@ import org.springframework.shell.standard.ShellComponent;
 import org.springframework.shell.component.flow.ComponentFlow;
 import org.springframework.shell.standard.ShellOption;
 
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookMetaDataResponse;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDTO;
-import com.penrose.bibby.library.catalog.book.contracts.ports.inbound.BookFacade;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookRequestDTO;
-import com.penrose.bibby.library.placement.bookcase.contracts.dtos.BookcaseDTO;
-import com.penrose.bibby.library.placement.bookcase.contracts.ports.inbound.BookcaseFacade;
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.contracts.ports.AuthorFacade;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
-import com.penrose.bibby.library.placement.shelf.contracts.ports.inbound.ShelfFacade;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookRequestDTO;
+import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
+import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
 
 import com.penrose.bibby.cli.prompt.application.CliPromptService;
 
diff --git a/src/main/java/com/penrose/bibby/cli/commands/bookcase/BookcaseCommands.java b/src/main/java/com/penrose/bibby/cli/commands/bookcase/BookcaseCommands.java
index 2b4ff95..b2b1a5c 100644
--- a/src/main/java/com/penrose/bibby/cli/commands/bookcase/BookcaseCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/commands/bookcase/BookcaseCommands.java
@@ -1,20 +1,20 @@
 package com.penrose.bibby.cli.commands.bookcase;
 
 import com.penrose.bibby.cli.commands.book.BookCommands;
-import com.penrose.bibby.library.placement.bookcase.contracts.ports.inbound.BookcaseFacade;
+import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
 import org.springframework.shell.command.annotation.Command;
 import org.springframework.shell.component.flow.ComponentFlow;
 import org.springframework.shell.standard.AbstractShellComponent;
 import org.springframework.stereotype.Component;
 
-import com.penrose.bibby.library.catalog.book.contracts.ports.inbound.BookFacade;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDTO;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDetailView;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookSummary;
-import com.penrose.bibby.library.placement.bookcase.contracts.dtos.BookcaseDTO;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
-import com.penrose.bibby.library.placement.shelf.contracts.ports.inbound.ShelfFacade;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfSummary;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDetailView;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookSummary;
+import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfSummary;
 import java.util.*;
 
 @Component
diff --git a/src/main/java/com/penrose/bibby/cli/prompt/application/CliPromptService.java b/src/main/java/com/penrose/bibby/cli/prompt/application/CliPromptService.java
index 437895e..9d49092 100644
--- a/src/main/java/com/penrose/bibby/cli/prompt/application/CliPromptService.java
+++ b/src/main/java/com/penrose/bibby/cli/prompt/application/CliPromptService.java
@@ -1,11 +1,11 @@
 package com.penrose.bibby.cli.prompt.application;
 
 import com.penrose.bibby.cli.prompt.contracts.PromptFacade;
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.contracts.ports.AuthorFacade;
-import com.penrose.bibby.library.catalog.book.contracts.ports.inbound.BookFacade;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
-import com.penrose.bibby.library.placement.shelf.contracts.ports.inbound.ShelfFacade;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
 import org.springframework.shell.component.flow.ComponentFlow;
 import org.springframework.shell.component.flow.SelectItem;
 import org.springframework.stereotype.Component;
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/book/BookController.java b/src/main/java/com/penrose/bibby/infrastructure/web/book/BookController.java
index 654ad07..ba9b6eb 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/book/BookController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/book/BookController.java
@@ -1,19 +1,19 @@
 package com.penrose.bibby.infrastructure.web.book;
 
-import com.penrose.bibby.library.catalog.author.infrastructure.repository.AuthorJpaRepository;
-import com.penrose.bibby.library.catalog.book.contracts.ports.inbound.BookFacade;
-import com.penrose.bibby.library.catalog.book.core.application.IsbnLookupService;
-import com.penrose.bibby.library.catalog.book.infrastructure.external.GoogleBooksResponse;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookPlacementResponse;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookRequestDTO;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookShelfAssignmentRequest;
-import com.penrose.bibby.library.catalog.book.core.application.IsbnEnrichmentService;
-import com.penrose.bibby.library.catalog.book.core.application.BookService;
-import com.penrose.bibby.library.placement.bookcase.contracts.dtos.BookcaseDTO;
-import com.penrose.bibby.library.placement.bookcase.core.application.BookcaseService;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
-import com.penrose.bibby.library.placement.shelf.core.application.ShelfService;
+import com.penrose.bibby.library.cataloging.author.infrastructure.repository.AuthorJpaRepository;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
+import com.penrose.bibby.library.cataloging.book.core.application.IsbnLookupService;
+import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookPlacementResponse;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookRequestDTO;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookShelfAssignmentRequest;
+import com.penrose.bibby.library.cataloging.book.core.application.IsbnEnrichmentService;
+import com.penrose.bibby.library.cataloging.book.core.application.BookService;
+import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
+import com.penrose.bibby.library.stacks.bookcase.core.application.BookcaseService;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/book/BookImportController.java b/src/main/java/com/penrose/bibby/infrastructure/web/book/BookImportController.java
index 90cc576..128af2a 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/book/BookImportController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/book/BookImportController.java
@@ -1,11 +1,11 @@
 package com.penrose.bibby.infrastructure.web.book;
 
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookMetaDataResponse;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.external.BookImportRequest;
-import com.penrose.bibby.library.catalog.book.infrastructure.external.GoogleBooksResponse;
-import com.penrose.bibby.library.catalog.book.core.application.IsbnLookupService;
-import com.penrose.bibby.library.catalog.book.core.application.IsbnEnrichmentService;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.external.BookImportRequest;
+import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
+import com.penrose.bibby.library.cataloging.book.core.application.IsbnLookupService;
+import com.penrose.bibby.library.cataloging.book.core.application.IsbnEnrichmentService;
 import org.slf4j.Logger;
 import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.RequestBody;
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/bookcase/BookCaseController.java b/src/main/java/com/penrose/bibby/infrastructure/web/bookcase/BookCaseController.java
index 8cc7594..a011c35 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/bookcase/BookCaseController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/bookcase/BookCaseController.java
@@ -1,7 +1,7 @@
 package com.penrose.bibby.infrastructure.web.bookcase;
 
-import com.penrose.bibby.library.placement.bookcase.contracts.dtos.BookcaseDTO;
-import com.penrose.bibby.library.placement.bookcase.core.application.BookcaseService;
+import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
+import com.penrose.bibby.library.stacks.bookcase.core.application.BookcaseService;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.PostMapping;
diff --git a/src/main/java/com/penrose/bibby/infrastructure/web/shelf/ShelfController.java b/src/main/java/com/penrose/bibby/infrastructure/web/shelf/ShelfController.java
index 68d52ab..a22f821 100644
--- a/src/main/java/com/penrose/bibby/infrastructure/web/shelf/ShelfController.java
+++ b/src/main/java/com/penrose/bibby/infrastructure/web/shelf/ShelfController.java
@@ -1,7 +1,7 @@
 package com.penrose.bibby.infrastructure.web.shelf;
 
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfOptionResponse;
-import com.penrose.bibby.library.placement.shelf.core.application.ShelfService;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfOptionResponse;
+import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
 import org.springframework.web.bind.annotation.RestController;
 import org.springframework.web.bind.annotation.GetMapping;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/contracts/AuthorDTO.java b/src/main/java/com/penrose/bibby/library/cataloging/author/contracts/AuthorDTO.java
index e95cadf..cfba0c6 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/contracts/AuthorDTO.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/contracts/AuthorDTO.java
@@ -1,8 +1,8 @@
-package com.penrose.bibby.library.catalog.author.contracts;
+package com.penrose.bibby.library.cataloging.author.contracts;
 
 
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.book.core.domain.AuthorRef;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
 
 import java.util.Optional;
 import java.util.Set;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/contracts/ports/AuthorFacade.java b/src/main/java/com/penrose/bibby/library/cataloging/author/contracts/ports/AuthorFacade.java
index 484c37a..099d656 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/contracts/ports/AuthorFacade.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/contracts/ports/AuthorFacade.java
@@ -1,7 +1,7 @@
-package com.penrose.bibby.library.catalog.author.contracts.ports;
+package com.penrose.bibby.library.cataloging.author.contracts.ports;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
 
 import java.util.List;
 import java.util.Set;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/core/application/AuthorService.java b/src/main/java/com/penrose/bibby/library/cataloging/author/core/application/AuthorService.java
index 08ec3c7..cac1e71 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/core/application/AuthorService.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/core/application/AuthorService.java
@@ -1,9 +1,9 @@
-package com.penrose.bibby.library.catalog.author.core.application;
+package com.penrose.bibby.library.cataloging.author.core.application;
 import org.springframework.stereotype.Service;
 import java.util.Optional;
 import java.util.Set;
-import com.penrose.bibby.library.catalog.author.core.domain.Author;
-import com.penrose.bibby.library.catalog.author.core.domain.AuthorRepository;
+import com.penrose.bibby.library.cataloging.author.core.domain.Author;
+import com.penrose.bibby.library.cataloging.author.core.domain.AuthorRepository;
 
 @Service
 public class AuthorService {
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/Author.java b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/Author.java
index 5e3b1ff..5a6012e 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/Author.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/Author.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.author.core.domain;
+package com.penrose.bibby.library.cataloging.author.core.domain;
 
 public class Author {
     private AuthorId authorId;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorFactory.java b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorFactory.java
index 92120e9..0f3e138 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorFactory.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorFactory.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.catalog.author.core.domain;
+package com.penrose.bibby.library.cataloging.author.core.domain;
 
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
 import org.springframework.stereotype.Component;
 
 @Component
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorId.java b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorId.java
index 9c54240..a35fa73 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorId.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorId.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.author.core.domain;
+package com.penrose.bibby.library.cataloging.author.core.domain;
 
 public record AuthorId(Long id) {
 }
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorName.java b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorName.java
index ba286a7..51c9a4f 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorName.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorName.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.author.core.domain;
+package com.penrose.bibby.library.cataloging.author.core.domain;
 
 public class AuthorName {
     String authorName;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorRepository.java b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorRepository.java
index 4a8e562..08d60ee 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorRepository.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/core/domain/AuthorRepository.java
@@ -1,7 +1,7 @@
-package com.penrose.bibby.library.catalog.author.core.domain;
+package com.penrose.bibby.library.cataloging.author.core.domain;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
 import org.springframework.stereotype.Repository;
 
 import java.util.List;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/adapters/AuthorFacadeImpl.java b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/adapters/AuthorFacadeImpl.java
index 65a3ca3..0a5a5bb 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/adapters/AuthorFacadeImpl.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/adapters/AuthorFacadeImpl.java
@@ -1,11 +1,11 @@
-package com.penrose.bibby.library.catalog.author.infrastructure.adapters;
-
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.contracts.ports.AuthorFacade;
-import com.penrose.bibby.library.catalog.author.core.domain.Author;
-import com.penrose.bibby.library.catalog.author.core.domain.AuthorRepository;
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.author.infrastructure.mapping.AuthorMapper;
+package com.penrose.bibby.library.cataloging.author.infrastructure.adapters;
+
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
+import com.penrose.bibby.library.cataloging.author.core.domain.Author;
+import com.penrose.bibby.library.cataloging.author.core.domain.AuthorRepository;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.infrastructure.mapping.AuthorMapper;
 import org.slf4j.Logger;
 import org.springframework.stereotype.Component;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/entity/AuthorEntity.java b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/entity/AuthorEntity.java
index 9c499f1..19a7aa1 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/entity/AuthorEntity.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/entity/AuthorEntity.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.catalog.author.infrastructure.entity;
+package com.penrose.bibby.library.cataloging.author.infrastructure.entity;
 
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
 import jakarta.persistence.*;
 
 import java.util.HashSet;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/mapping/AuthorMapper.java b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/mapping/AuthorMapper.java
index a8c60c8..af0ada8 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/mapping/AuthorMapper.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/mapping/AuthorMapper.java
@@ -1,9 +1,9 @@
-package com.penrose.bibby.library.catalog.author.infrastructure.mapping;
+package com.penrose.bibby.library.cataloging.author.infrastructure.mapping;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.core.domain.Author;
-import com.penrose.bibby.library.catalog.author.core.domain.AuthorId;
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.core.domain.Author;
+import com.penrose.bibby.library.cataloging.author.core.domain.AuthorId;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
 
 import java.util.List;
 import java.util.Set;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/mapping/AuthorMapperTwo.java b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/mapping/AuthorMapperTwo.java
index 2ba7986..35542d4 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/mapping/AuthorMapperTwo.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/mapping/AuthorMapperTwo.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.catalog.author.infrastructure.mapping;
+package com.penrose.bibby.library.cataloging.author.infrastructure.mapping;
 
-import com.penrose.bibby.library.catalog.author.core.domain.AuthorFactory;
+import com.penrose.bibby.library.cataloging.author.core.domain.AuthorFactory;
 import org.springframework.stereotype.Component;
 
 @Component
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/repository/AuthorJpaRepository.java b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/repository/AuthorJpaRepository.java
index 1087efb..1b57ba4 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/repository/AuthorJpaRepository.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/repository/AuthorJpaRepository.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.catalog.author.infrastructure.repository;
+package com.penrose.bibby.library.cataloging.author.infrastructure.repository;
 
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
 import org.springframework.data.repository.query.Param;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/repository/AuthorRepositoryImpl.java b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/repository/AuthorRepositoryImpl.java
index 59ce70f..364cfbd 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/repository/AuthorRepositoryImpl.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/author/infrastructure/repository/AuthorRepositoryImpl.java
@@ -1,10 +1,10 @@
-package com.penrose.bibby.library.catalog.author.infrastructure.repository;
+package com.penrose.bibby.library.cataloging.author.infrastructure.repository;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.core.domain.Author;
-import com.penrose.bibby.library.catalog.author.core.domain.AuthorRepository;
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.author.infrastructure.mapping.AuthorMapper;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.core.domain.Author;
+import com.penrose.bibby.library.cataloging.author.core.domain.AuthorRepository;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.infrastructure.mapping.AuthorMapper;
 import org.slf4j.Logger;
 import org.springframework.stereotype.Repository;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/AuthorAccessPortAdapter.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/AuthorAccessPortAdapter.java
index 8fa7e9b..708bd28 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/AuthorAccessPortAdapter.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/AuthorAccessPortAdapter.java
@@ -1,10 +1,10 @@
-package com.penrose.bibby.library.catalog.book.contracts.adapters;
+package com.penrose.bibby.library.cataloging.book.contracts.adapters;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.contracts.ports.AuthorFacade;
-import com.penrose.bibby.library.catalog.book.core.domain.AuthorName;
-import com.penrose.bibby.library.catalog.book.core.domain.AuthorRef;
-import com.penrose.bibby.library.catalog.book.contracts.ports.outbound.AuthorAccessPort;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
+import com.penrose.bibby.library.cataloging.book.core.domain.AuthorName;
+import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.outbound.AuthorAccessPort;
 import org.springframework.stereotype.Component;
 
 import java.util.Set;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/BookFacadeAdapter.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/BookFacadeAdapter.java
index 8be9fd8..c31bc36 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/BookFacadeAdapter.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/BookFacadeAdapter.java
@@ -1,17 +1,17 @@
-package com.penrose.bibby.library.catalog.book.contracts.adapters;
+package com.penrose.bibby.library.cataloging.book.contracts.adapters;
 
-import com.penrose.bibby.library.catalog.author.contracts.ports.AuthorFacade;
+import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
 import com.penrose.bibby.library.book.contracts.dtos.*;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.*;
-import com.penrose.bibby.library.catalog.book.contracts.ports.inbound.BookFacade;
-import com.penrose.bibby.library.catalog.book.core.application.IsbnLookupService;
-import com.penrose.bibby.library.catalog.book.core.domain.Book;
-import com.penrose.bibby.library.catalog.book.core.domain.BookDomainRepository;
-import com.penrose.bibby.library.catalog.book.core.domain.Isbn;
-import com.penrose.bibby.library.catalog.book.core.domain.Title;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.external.GoogleBooksResponse;
-import com.penrose.bibby.library.catalog.book.infrastructure.mapping.BookMapper;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.*;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
+import com.penrose.bibby.library.cataloging.book.core.application.IsbnLookupService;
+import com.penrose.bibby.library.cataloging.book.core.domain.Book;
+import com.penrose.bibby.library.cataloging.book.core.domain.BookDomainRepository;
+import com.penrose.bibby.library.cataloging.book.core.domain.Isbn;
+import com.penrose.bibby.library.cataloging.book.core.domain.Title;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
+import com.penrose.bibby.library.cataloging.book.infrastructure.mapping.BookMapper;
 import org.slf4j.Logger;
 import org.springframework.stereotype.Component;
 import org.springframework.transaction.annotation.Transactional;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/ShelfAccessPortAdapter.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/ShelfAccessPortAdapter.java
index e606b15..6222481 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/ShelfAccessPortAdapter.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/adapters/ShelfAccessPortAdapter.java
@@ -1,9 +1,9 @@
-package com.penrose.bibby.library.catalog.book.contracts.adapters;
+package com.penrose.bibby.library.cataloging.book.contracts.adapters;
 
-import com.penrose.bibby.library.catalog.book.contracts.ports.outbound.ShelfAccessPort;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
-import com.penrose.bibby.library.placement.shelf.contracts.ports.inbound.ShelfFacade;
-import com.penrose.bibby.library.placement.shelf.core.application.ShelfService;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.outbound.ShelfAccessPort;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
+import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
 import org.springframework.stereotype.Component;
 
 import java.util.Optional;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDTO.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDTO.java
index c40f898..93de6fa 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDTO.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDTO.java
@@ -1,8 +1,8 @@
-package com.penrose.bibby.library.catalog.book.contracts.dtos;
+package com.penrose.bibby.library.cataloging.book.contracts.dtos;
 
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.book.core.domain.AvailabilityStatus;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
 
 import java.time.LocalDate;
 import java.util.*;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDetailView.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDetailView.java
index 179ac13..9e0783c 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDetailView.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookDetailView.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.contracts.dtos;
+package com.penrose.bibby.library.cataloging.book.contracts.dtos;
 
 public record BookDetailView(Long bookId, String title, String authors, String bookcaseLabel, String shelfLabel, String bookStatus) {
 }
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookMetaDataResponse.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookMetaDataResponse.java
index f1c2164..4813a1c 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookMetaDataResponse.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookMetaDataResponse.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.contracts.dtos;
+package com.penrose.bibby.library.cataloging.book.contracts.dtos;
 
 import java.util.List;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookPlacementResponse.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookPlacementResponse.java
index 544f3a6..6137fa8 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookPlacementResponse.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookPlacementResponse.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.contracts.dtos;
+package com.penrose.bibby.library.cataloging.book.contracts.dtos;
 
 public record BookPlacementResponse(
         Long bookId,
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookReference.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookReference.java
index a951214..9a60d18 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookReference.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookReference.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.catalog.book.contracts.dtos;
+package com.penrose.bibby.library.cataloging.book.contracts.dtos;
 
-import com.penrose.bibby.library.catalog.book.core.domain.BookId;
+import com.penrose.bibby.library.cataloging.book.core.domain.BookId;
 
 import java.util.List;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookRequestDTO.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookRequestDTO.java
index 7be0c26..edc8ff9 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookRequestDTO.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookRequestDTO.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.catalog.book.contracts.dtos;
+package com.penrose.bibby.library.cataloging.book.contracts.dtos;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
 
 import java.util.List;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookShelfAssignmentRequest.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookShelfAssignmentRequest.java
index 510128d..33fc349 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookShelfAssignmentRequest.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookShelfAssignmentRequest.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.contracts.dtos;
+package com.penrose.bibby.library.cataloging.book.contracts.dtos;
 
 public record BookShelfAssignmentRequest(Long shelfId) {
 }
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookSummary.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookSummary.java
index 51fe140..7841198 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookSummary.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/dtos/BookSummary.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.contracts.dtos;
+package com.penrose.bibby.library.cataloging.book.contracts.dtos;
 
 public record BookSummary(Long bookId, String title) {
 }
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/inbound/BookFacade.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/inbound/BookFacade.java
index 9dfc0a7..7ff3a18 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/inbound/BookFacade.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/inbound/BookFacade.java
@@ -1,7 +1,7 @@
-package com.penrose.bibby.library.catalog.book.contracts.ports.inbound;
+package com.penrose.bibby.library.cataloging.book.contracts.ports.inbound;
 
 import com.penrose.bibby.library.book.contracts.dtos.*;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.*;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.*;
 
 import java.util.List;
 import java.util.Optional;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/outbound/AuthorAccessPort.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/outbound/AuthorAccessPort.java
index 4e4709c..3313834 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/outbound/AuthorAccessPort.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/outbound/AuthorAccessPort.java
@@ -1,7 +1,7 @@
-package com.penrose.bibby.library.catalog.book.contracts.ports.outbound;
+package com.penrose.bibby.library.cataloging.book.contracts.ports.outbound;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.book.core.domain.AuthorRef;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
 
 import java.util.Set;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/outbound/ShelfAccessPort.java b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/outbound/ShelfAccessPort.java
index 1a4c783..d7bce83 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/outbound/ShelfAccessPort.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/contracts/ports/outbound/ShelfAccessPort.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.catalog.book.contracts.ports.outbound;
+package com.penrose.bibby.library.cataloging.book.contracts.ports.outbound;
 
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
 
 import java.util.Optional;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/BookService.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/BookService.java
index 95aaa3a..2802d3e 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/BookService.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/BookService.java
@@ -1,26 +1,24 @@
-package com.penrose.bibby.library.catalog.book.core.application;
-
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDTO;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookRequestDTO;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookSummary;
-import com.penrose.bibby.library.catalog.book.contracts.ports.outbound.ShelfAccessPort;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.*;
-import com.penrose.bibby.library.catalog.book.contracts.ports.outbound.AuthorAccessPort;
-import com.penrose.bibby.library.catalog.book.core.domain.*;
-import com.penrose.bibby.library.catalog.book.core.domain.Book;
-import com.penrose.bibby.library.catalog.book.core.domain.BookDomainRepository;
-import com.penrose.bibby.library.catalog.book.core.domain.BookFactory;
+package com.penrose.bibby.library.cataloging.book.core.application;
+
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookRequestDTO;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookSummary;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.outbound.ShelfAccessPort;
+import com.penrose.bibby.library.cataloging.book.contracts.ports.outbound.AuthorAccessPort;
+import com.penrose.bibby.library.cataloging.book.core.domain.Book;
+import com.penrose.bibby.library.cataloging.book.core.domain.BookDomainRepository;
+import com.penrose.bibby.library.cataloging.book.core.domain.BookFactory;
 import org.slf4j.Logger;
 import org.springframework.stereotype.Service;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
 
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
 
 
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.mapping.BookMapper;
-import com.penrose.bibby.library.catalog.book.infrastructure.repository.BookJpaRepository;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.mapping.BookMapper;
+import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
 
 import java.util.*;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/IsbnEnrichmentService.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/IsbnEnrichmentService.java
index 542d20a..599e8f5 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/IsbnEnrichmentService.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/IsbnEnrichmentService.java
@@ -1,15 +1,15 @@
-package com.penrose.bibby.library.catalog.book.core.application;
+package com.penrose.bibby.library.cataloging.book.core.application;
 
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.contracts.ports.AuthorFacade;
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.author.core.domain.AuthorFactory;
-import com.penrose.bibby.library.catalog.book.core.domain.AuthorRef;
-import com.penrose.bibby.library.catalog.book.core.domain.AvailabilityStatus;
-import com.penrose.bibby.library.catalog.book.core.domain.Book;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.external.GoogleBooksResponse;
-import com.penrose.bibby.library.catalog.book.infrastructure.mapping.BookMapperTwo;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.core.domain.AuthorFactory;
+import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
+import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
+import com.penrose.bibby.library.cataloging.book.core.domain.Book;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
+import com.penrose.bibby.library.cataloging.book.infrastructure.mapping.BookMapperTwo;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.stereotype.Service;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/IsbnLookupService.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/IsbnLookupService.java
index 493f334..4df48d6 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/IsbnLookupService.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/application/IsbnLookupService.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.catalog.book.core.application;
+package com.penrose.bibby.library.cataloging.book.core.application;
 
-import com.penrose.bibby.library.catalog.book.infrastructure.external.GoogleBooksResponse;
+import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
 import org.springframework.stereotype.Service;
 import org.springframework.web.reactive.function.client.WebClient;
 import reactor.core.publisher.Mono;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AuthorName.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AuthorName.java
index b6f09c5..a06deb3 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AuthorName.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AuthorName.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
 public record AuthorName (String firstName, String lastName) {
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AuthorRef.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AuthorRef.java
index bab22a1..9c4fd06 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AuthorRef.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AuthorRef.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
 public class AuthorRef {
     private final Long authorId;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AvailabilityStatus.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AvailabilityStatus.java
index 1deb240..58deddd 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AvailabilityStatus.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/AvailabilityStatus.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
 public enum AvailabilityStatus {
     AVAILABLE,
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Book.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Book.java
index aa63468..0426f3a 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Book.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Book.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
 import java.time.LocalDate;
 import java.util.List;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookDomainRepository.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookDomainRepository.java
index 39e9a61..1d1191a 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookDomainRepository.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookDomainRepository.java
@@ -1,8 +1,8 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDetailView;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookMetaDataResponse;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDetailView;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
 
 import java.util.List;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookFactory.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookFactory.java
index ac7b187..2ec36b0 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookFactory.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookFactory.java
@@ -1,7 +1,7 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
 import org.springframework.stereotype.Component;
 
 import java.util.List;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookId.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookId.java
index b33677c..bf5bdda 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookId.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookId.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
 public class BookId {
     private final Long id;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookMetaData.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookMetaData.java
index 4920647..ee9c7df 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookMetaData.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/BookMetaData.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
 
 public record BookMetaData(String title, String[] authors, String publisher, String description, String isbn_13, String[] categories) {
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Isbn.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Isbn.java
index a8a0f17..5536428 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Isbn.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Isbn.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
 public class Isbn {
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Title.java b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Title.java
index ac67fbe..539bcea 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Title.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/core/domain/Title.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.core.domain;
+package com.penrose.bibby.library.cataloging.book.core.domain;
 
 public class Title {
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/entity/BookEntity.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/entity/BookEntity.java
index 49b4f31..4ce9579 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/entity/BookEntity.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/entity/BookEntity.java
@@ -1,7 +1,7 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.entity;
+package com.penrose.bibby.library.cataloging.book.infrastructure.entity;
 
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.book.core.domain.AvailabilityStatus;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
 import jakarta.persistence.*;
 
 import java.time.LocalDate;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/BookImportRequest.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/BookImportRequest.java
index d047dd4..4168753 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/BookImportRequest.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/BookImportRequest.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.external;
+package com.penrose.bibby.library.cataloging.book.infrastructure.external;
 
 public record BookImportRequest(String isbn) {
 }
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/GoogleBookItems.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/GoogleBookItems.java
index 327514b..17d85b5 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/GoogleBookItems.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/GoogleBookItems.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.external;
+package com.penrose.bibby.library.cataloging.book.infrastructure.external;
 
 public record GoogleBookItems(VolumeInfo volumeInfo) {
 }
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/GoogleBooksResponse.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/GoogleBooksResponse.java
index e11f212..1fe5e36 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/GoogleBooksResponse.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/GoogleBooksResponse.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.external;
+package com.penrose.bibby.library.cataloging.book.infrastructure.external;
 
 import java.util.List;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/VolumeInfo.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/VolumeInfo.java
index b5531da..81499a5 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/VolumeInfo.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/external/VolumeInfo.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.external;
+package com.penrose.bibby.library.cataloging.book.infrastructure.external;
 
 import java.util.List;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapper.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapper.java
index 83d024f..baa273b 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapper.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapper.java
@@ -1,19 +1,19 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.mapping;
-
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.contracts.ports.AuthorFacade;
-import com.penrose.bibby.library.catalog.author.core.domain.Author;
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.author.infrastructure.mapping.AuthorMapper;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookMetaDataResponse;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookRequestDTO;
-import com.penrose.bibby.library.catalog.book.core.domain.*;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDTO;
+package com.penrose.bibby.library.cataloging.book.infrastructure.mapping;
+
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
+import com.penrose.bibby.library.cataloging.author.core.domain.Author;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.author.infrastructure.mapping.AuthorMapper;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookRequestDTO;
+import com.penrose.bibby.library.cataloging.book.core.domain.*;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
 import com.penrose.bibby.library.book.core.domain.*;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.external.GoogleBooksResponse;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
-import com.penrose.bibby.library.placement.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
 import org.slf4j.Logger;
 import org.springframework.stereotype.Component;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapperTwo.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapperTwo.java
index fc657f6..09cd66b 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapperTwo.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/mapping/BookMapperTwo.java
@@ -1,10 +1,10 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.mapping;
+package com.penrose.bibby.library.cataloging.book.infrastructure.mapping;
 
-import com.penrose.bibby.library.catalog.author.core.domain.Author;
-import com.penrose.bibby.library.catalog.author.infrastructure.mapping.AuthorMapperTwo;
-import com.penrose.bibby.library.catalog.book.core.domain.Book;
-import com.penrose.bibby.library.catalog.book.core.domain.BookFactory;
-import com.penrose.bibby.library.catalog.book.infrastructure.external.GoogleBooksResponse;
+import com.penrose.bibby.library.cataloging.author.core.domain.Author;
+import com.penrose.bibby.library.cataloging.author.infrastructure.mapping.AuthorMapperTwo;
+import com.penrose.bibby.library.cataloging.book.core.domain.Book;
+import com.penrose.bibby.library.cataloging.book.core.domain.BookFactory;
+import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
 import org.springframework.stereotype.Component;
 
 import java.util.ArrayList;
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookDomainRepositoryImpl.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookDomainRepositoryImpl.java
index a692ee9..d6c348c 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookDomainRepositoryImpl.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookDomainRepositoryImpl.java
@@ -1,15 +1,15 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.repository;
-
-import com.penrose.bibby.library.catalog.author.core.application.AuthorService;
-import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDetailView;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookMetaDataResponse;
-import com.penrose.bibby.library.catalog.book.core.domain.Book;
-import com.penrose.bibby.library.catalog.book.core.domain.BookDomainRepository;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.mapping.BookMapper;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
+package com.penrose.bibby.library.cataloging.book.infrastructure.repository;
+
+import com.penrose.bibby.library.cataloging.author.core.application.AuthorService;
+import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDetailView;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
+import com.penrose.bibby.library.cataloging.book.core.domain.Book;
+import com.penrose.bibby.library.cataloging.book.core.domain.BookDomainRepository;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.mapping.BookMapper;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
 import org.slf4j.Logger;
 import org.springframework.stereotype.Component;
 
diff --git a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookJpaRepository.java b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookJpaRepository.java
index b4ba4a6..9dc9c81 100644
--- a/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookJpaRepository.java
+++ b/src/main/java/com/penrose/bibby/library/cataloging/book/infrastructure/repository/BookJpaRepository.java
@@ -1,8 +1,8 @@
-package com.penrose.bibby.library.catalog.book.infrastructure.repository;
+package com.penrose.bibby.library.cataloging.book.infrastructure.repository;
 
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDetailView;
-import com.penrose.bibby.library.catalog.book.contracts.dtos.BookSummary;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDetailView;
+import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookSummary;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
 import org.springframework.stereotype.Repository;
diff --git a/src/main/java/com/penrose/bibby/library/classification/docs/Booklist.java b/src/main/java/com/penrose/bibby/library/classification/docs/Booklist.java
index 1fdb7a1..2371f8a 100644
--- a/src/main/java/com/penrose/bibby/library/classification/docs/Booklist.java
+++ b/src/main/java/com/penrose/bibby/library/classification/docs/Booklist.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.classification;
+package com.penrose.bibby.library.classification.docs;
 
 public class Booklist {
     ListId listId;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/dtos/BookcaseDTO.java b/src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/dtos/BookcaseDTO.java
index 16e00f6..3124d94 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/dtos/BookcaseDTO.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/dtos/BookcaseDTO.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.placement.bookcase.contracts.dtos;
+package com.penrose.bibby.library.stacks.bookcase.contracts.dtos;
 
-import com.penrose.bibby.library.placement.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.stacks.bookcase.infrastructure.BookcaseEntity;
 
 import java.util.Optional;
 
diff --git a/src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/ports/inbound/BookcaseFacade.java b/src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/ports/inbound/BookcaseFacade.java
index fdae15a..ff124eb 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/ports/inbound/BookcaseFacade.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/bookcase/contracts/ports/inbound/BookcaseFacade.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.placement.bookcase.contracts.ports.inbound;
+package com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound;
 
-import com.penrose.bibby.library.placement.bookcase.contracts.dtos.BookcaseDTO;
+import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
 
 import java.util.List;
 import java.util.Optional;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/bookcase/core/application/BookcaseService.java b/src/main/java/com/penrose/bibby/library/stacks/bookcase/core/application/BookcaseService.java
index bb3d81f..5eaa3ac 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/bookcase/core/application/BookcaseService.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/bookcase/core/application/BookcaseService.java
@@ -1,10 +1,10 @@
-package com.penrose.bibby.library.placement.bookcase.core.application;
-import com.penrose.bibby.library.placement.bookcase.contracts.dtos.BookcaseDTO;
-import com.penrose.bibby.library.placement.bookcase.contracts.ports.inbound.BookcaseFacade;
-import com.penrose.bibby.library.placement.bookcase.infrastructure.BookcaseEntity;
-import com.penrose.bibby.library.placement.bookcase.infrastructure.BookcaseRepository;
-import com.penrose.bibby.library.placement.shelf.core.domain.ShelfFactory;
-import com.penrose.bibby.library.placement.shelf.infrastructure.repository.ShelfJpaRepository;
+package com.penrose.bibby.library.stacks.bookcase.core.application;
+import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
+import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
+import com.penrose.bibby.library.stacks.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.stacks.bookcase.infrastructure.BookcaseRepository;
+import com.penrose.bibby.library.stacks.shelf.core.domain.ShelfFactory;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.http.HttpStatus;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/bookcase/core/domain/Bookcase.java b/src/main/java/com/penrose/bibby/library/stacks/bookcase/core/domain/Bookcase.java
index c476c81..45b87ef 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/bookcase/core/domain/Bookcase.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/bookcase/core/domain/Bookcase.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.placement.bookcase.core.domain;
+package com.penrose.bibby.library.stacks.bookcase.core.domain;
 
 public class Bookcase {
     private Long bookcaseId;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseEntity.java b/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseEntity.java
index a5f4232..f8dbc54 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseEntity.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseEntity.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.placement.bookcase.infrastructure;
+package com.penrose.bibby.library.stacks.bookcase.infrastructure;
 
 import jakarta.persistence.*;
 
diff --git a/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseMapper.java b/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseMapper.java
index 9ea3b86..de6a434 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseMapper.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseMapper.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.placement.bookcase.infrastructure;
+package com.penrose.bibby.library.stacks.bookcase.infrastructure;
 
-import com.penrose.bibby.library.placement.bookcase.core.domain.Bookcase;
+import com.penrose.bibby.library.stacks.bookcase.core.domain.Bookcase;
 
 public class BookcaseMapper {
 
diff --git a/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseRepository.java b/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseRepository.java
index 07507c1..fe93c41 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseRepository.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/bookcase/infrastructure/BookcaseRepository.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.placement.bookcase.infrastructure;
+package com.penrose.bibby.library.stacks.bookcase.infrastructure;
 
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.stereotype.Repository;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfDTO.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfDTO.java
index 9c1f0a7..8571cee 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfDTO.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfDTO.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.placement.shelf.contracts.dtos;
+package com.penrose.bibby.library.stacks.shelf.contracts.dtos;
 
-import com.penrose.bibby.library.placement.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
 
 import java.util.List;
 
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfOptionResponse.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfOptionResponse.java
index eb77b9c..5c21ea9 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfOptionResponse.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfOptionResponse.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.placement.shelf.contracts.dtos;
+package com.penrose.bibby.library.stacks.shelf.contracts.dtos;
 
 public record ShelfOptionResponse(
         Long shelfId,
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfSummary.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfSummary.java
index ecb188c..5582013 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfSummary.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/dtos/ShelfSummary.java
@@ -1,3 +1,3 @@
-package com.penrose.bibby.library.placement.shelf.contracts.dtos;
+package com.penrose.bibby.library.stacks.shelf.contracts.dtos;
 
 public record ShelfSummary(Long shelfId, String label, long bookCount) { }
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/ports/inbound/ShelfFacade.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/ports/inbound/ShelfFacade.java
index a04ad8d..3bd290b 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/ports/inbound/ShelfFacade.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/contracts/ports/inbound/ShelfFacade.java
@@ -1,8 +1,8 @@
-package com.penrose.bibby.library.placement.shelf.contracts.ports.inbound;
+package com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound;
 
 import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfSummary;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfSummary;
 
 import java.util.List;
 import java.util.Optional;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java
index 449e6b3..9e39f15 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/application/ShelfService.java
@@ -1,20 +1,20 @@
-package com.penrose.bibby.library.placement.shelf.core.application;
+package com.penrose.bibby.library.stacks.shelf.core.application;
 
 import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
 import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
 import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
-import com.penrose.bibby.library.placement.shelf.contracts.ports.inbound.ShelfFacade;
-import com.penrose.bibby.library.placement.shelf.core.domain.Shelf;
-import com.penrose.bibby.library.placement.shelf.infrastructure.entity.ShelfEntity;
-import com.penrose.bibby.library.placement.shelf.infrastructure.mapping.ShelfMapper;
-import com.penrose.bibby.library.placement.shelf.infrastructure.repository.ShelfJpaRepository;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfOptionResponse;
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfSummary;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
+import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
+import com.penrose.bibby.library.stacks.shelf.core.domain.Shelf;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfOptionResponse;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfSummary;
 import org.springframework.stereotype.Service;
 
-import com.penrose.bibby.library.placement.bookcase.infrastructure.BookcaseEntity;
-import com.penrose.bibby.library.placement.bookcase.infrastructure.BookcaseRepository;
+import com.penrose.bibby.library.stacks.bookcase.infrastructure.BookcaseEntity;
+import com.penrose.bibby.library.stacks.bookcase.infrastructure.BookcaseRepository;
 import org.springframework.transaction.annotation.Transactional;
 
 import java.util.List;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/Shelf.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/Shelf.java
index 5e5007c..d3dffe8 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/Shelf.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/Shelf.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.placement.shelf.core.domain;
+package com.penrose.bibby.library.stacks.shelf.core.domain;
 
 import java.util.List;
 
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfDomainRepository.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfDomainRepository.java
index 81818f1..567c552 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfDomainRepository.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfDomainRepository.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.placement.shelf.core.domain;
+package com.penrose.bibby.library.stacks.shelf.core.domain;
 
 public interface ShelfDomainRepository {
 
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfDomainRepositoryImpl.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfDomainRepositoryImpl.java
index 974f89a..f434903 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfDomainRepositoryImpl.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfDomainRepositoryImpl.java
@@ -1,10 +1,10 @@
-package com.penrose.bibby.library.placement.shelf.core.domain;
+package com.penrose.bibby.library.stacks.shelf.core.domain;
 
 import com.penrose.bibby.library.cataloging.book.core.domain.Book;
 import com.penrose.bibby.library.cataloging.book.core.domain.BookDomainRepository;
-import com.penrose.bibby.library.placement.shelf.infrastructure.entity.ShelfEntity;
-import com.penrose.bibby.library.placement.shelf.infrastructure.repository.ShelfJpaRepository;
-import com.penrose.bibby.library.placement.shelf.infrastructure.mapping.ShelfMapper;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
 import org.springframework.stereotype.Component;
 
 import java.util.ArrayList;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfFactory.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfFactory.java
index 7ed2b44..1078ec4 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfFactory.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/core/domain/ShelfFactory.java
@@ -1,6 +1,6 @@
-package com.penrose.bibby.library.placement.shelf.core.domain;
+package com.penrose.bibby.library.stacks.shelf.core.domain;
 
-import com.penrose.bibby.library.placement.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
 import org.springframework.stereotype.Component;
 
 @Component
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/entity/ShelfEntity.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/entity/ShelfEntity.java
index 30aae68..5da6aea 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/entity/ShelfEntity.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/entity/ShelfEntity.java
@@ -1,4 +1,4 @@
-package com.penrose.bibby.library.placement.shelf.infrastructure.entity;
+package com.penrose.bibby.library.stacks.shelf.infrastructure.entity;
 
 import jakarta.persistence.Entity;
 import jakarta.persistence.GeneratedValue;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/mapping/ShelfMapper.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/mapping/ShelfMapper.java
index bc330ce..d0caf9a 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/mapping/ShelfMapper.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/mapping/ShelfMapper.java
@@ -1,7 +1,7 @@
-package com.penrose.bibby.library.placement.shelf.infrastructure.mapping;
+package com.penrose.bibby.library.stacks.shelf.infrastructure.mapping;
 
-import com.penrose.bibby.library.placement.shelf.core.domain.Shelf;
-import com.penrose.bibby.library.placement.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.stacks.shelf.core.domain.Shelf;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
 import org.springframework.stereotype.Component;
 
 import java.util.List;
diff --git a/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/repository/ShelfJpaRepository.java b/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/repository/ShelfJpaRepository.java
index b8678bd..d6c1b89 100644
--- a/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/repository/ShelfJpaRepository.java
+++ b/src/main/java/com/penrose/bibby/library/stacks/shelf/infrastructure/repository/ShelfJpaRepository.java
@@ -1,7 +1,7 @@
-package com.penrose.bibby.library.placement.shelf.infrastructure.repository;
+package com.penrose.bibby.library.stacks.shelf.infrastructure.repository;
 
-import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfSummary;
-import com.penrose.bibby.library.placement.shelf.infrastructure.entity.ShelfEntity;
+import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfSummary;
+import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
 import org.springframework.data.repository.query.Param;
diff --git a/src/test/java/com/penrose/bibby/library/author/core/domain/AuthorNameTest.java b/src/test/java/com/penrose/bibby/library/author/core/domain/AuthorNameTest.java
index 1787688..66cddc4 100644
--- a/src/test/java/com/penrose/bibby/library/author/core/domain/AuthorNameTest.java
+++ b/src/test/java/com/penrose/bibby/library/author/core/domain/AuthorNameTest.java
@@ -1,6 +1,6 @@
 package com.penrose.bibby.library.author.core.domain;
 
-import com.penrose.bibby.library.catalog.author.core.domain.AuthorName;
+import com.penrose.bibby.library.cataloging.author.core.domain.AuthorName;
 import org.junit.jupiter.api.Test;
 
 import static org.junit.jupiter.api.Assertions.*;
diff --git a/src/test/java/com/penrose/bibby/library/book/BookServiceTest.java b/src/test/java/com/penrose/bibby/library/book/BookServiceTest.java
index 0aa65f0..00187a1 100644
--- a/src/test/java/com/penrose/bibby/library/book/BookServiceTest.java
+++ b/src/test/java/com/penrose/bibby/library/book/BookServiceTest.java
@@ -1,7 +1,7 @@
 package com.penrose.bibby.library.book;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.repository.BookJpaRepository;
-import com.penrose.bibby.library.catalog.book.core.application.BookService;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
+import com.penrose.bibby.library.cataloging.book.core.application.BookService;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.InjectMocks;
diff --git a/src/test/java/com/penrose/bibby/library/book/infrastructure/repository/BookDomainRepositoryImplTest.java b/src/test/java/com/penrose/bibby/library/book/infrastructure/repository/BookDomainRepositoryImplTest.java
index 61042f5..cbf49f4 100644
--- a/src/test/java/com/penrose/bibby/library/book/infrastructure/repository/BookDomainRepositoryImplTest.java
+++ b/src/test/java/com/penrose/bibby/library/book/infrastructure/repository/BookDomainRepositoryImplTest.java
@@ -1,10 +1,10 @@
 package com.penrose.bibby.library.book.infrastructure.repository;
 
-import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
-import com.penrose.bibby.library.catalog.book.core.domain.*;
-import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
-import com.penrose.bibby.library.catalog.book.infrastructure.repository.BookDomainRepositoryImpl;
-import com.penrose.bibby.library.catalog.book.infrastructure.repository.BookJpaRepository;
+import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
+import com.penrose.bibby.library.cataloging.book.core.domain.*;
+import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
+import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookDomainRepositoryImpl;
+import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.InjectMocks;
