# Devlog: Handling Duplicate Authors & Making ISBN Metadata Real

**Date:** December 11, 2025
**Diff Context:** Book CLI commands, PromptService, Author/Book facades & repositories, Google Books metadata mapping

---

## 1. High-Level Summary

* Added an **author disambiguation flow** so the CLI can handle multiple authors with the same name (e.g., multiple “Mike Jones” entries).
* Cleaned up the **book registration CLI** by introducing a `ScanMode` and splitting manual registration into smaller, focused methods.
* Upgraded the **PromptService** into a proper hexagonal adapter that talks to `AuthorFacade` and `BookFacade` to build dynamic menus.
* Extended the **AuthorFacade / AuthorRepository** with richer operations (existence checks, “find all by name”, “get authors by id”) that support the new UX.
* Completed the **ISBN → Google Books → BookEntity** pipeline, turning metadata lookup into a real “import book” feature.

---

## 2. The Friction I Hit

There were two main sources of pain:

### 2.1. “Which Mike Jones do you mean?”

The old flow for manual book registration assumed that “name = identity”.

* If the user entered “Mike Jones”, I’d just create a brand-new author every time.
* There was no way to:

  * detect that multiple “Mike Jones” already exist, or
  * let the user pick which one they meant.

Result: duplicated authors, messy associations, and no way to reuse existing records.

### 2.2. CLI branching logic was turning into spaghetti

The `book new` command had boolean flags (`scan`, `multi`) driving nested `if` branches. Add in ISBN scanning, manual entry, and multi-scan and the code was clearly heading toward “if-else soup”.

I needed a clearer model for:

* no scan (manual entry)
* single scan
* multi-scan

And I wanted the CLI logic to *read* like a decision table, not a choose-your-own-adventure script.

---

## 3. What Changed in Behavior

### 3.1. Author disambiguation is now interactive

Manual book creation now goes through a richer author flow:

1. User enters a book title.
2. For each author:

   * The CLI asks for the author’s name.
   * If that first/last name already exists:

     * Bibby displays **all authors with that name** plus a **preview of books** they’ve written.
     * The user can choose:

       * an existing author by ID, or
       * “Create New Author”.
   * The chosen (or newly created) author is added to the list for that book.

Under the hood, this is powered by:

* `authorFacade.authorExistFirstNameLastName(...)`
* `authorFacade.getAllAuthorsByName(...)`
* `bookFacade.getBooksByAuthorId(...)`

The user experience goes from “we just create someone” to “we ask who you meant”.

---

### 3.2. Scan mode is explicit and simpler

The `book new` workflow now uses a `ScanMode` instead of knotted `if` branches:

```java
@Command(command = "new", description = "Create a new book entry")
public void registerBook(
        @ShellOption(defaultValue = "false") boolean scan,
        @ShellOption(defaultValue = "false") boolean multi) {

    log.info("Starting new book registration process.");
    log.debug("{} {}", scan, multi);

    ScanMode mode = ScanMode.from(scan, multi);
    switch(mode){
        case SINGLE -> scanBook(false);
        case MULTI  -> multiBookScan();
        case NONE   -> createBookManually();
    }
}
```

* `ScanMode` encapsulates the decision logic.
* The command method is now a very readable mini-router.

---

### 3.3. ISBN metadata import is a real feature now

The `scan` flow now does something meaningful when metadata is found:

* Looks up Google Books data via `IsbnLookupService`.
* Maps that response into a `BookMetaDataResponse`.
* Persists a new `BookEntity` with:

  * title
  * isbn
  * authors (auto-created if missing)
  * publisher
  * description
  * availability status

The CLI prints a little “import summary” for the user after saving.

---

## 4. Architectural Shifts

### 4.1. PromptService became a true adapter

`CliPromptService` now depends on:

* `AuthorFacade`
* `BookFacade`
* `ShelfFacade` (already existed)

so it can build interactive menus based on real domain data.

Example: prompt for author selection when there are duplicates:

```java
public Long promptMultipleAuthorConfirmation(AuthorDTO author){
    ComponentFlow flow = componentFlowBuilder.clone()
            .withSingleItemSelector("chooseAuthor")
            .name("Author Selection")
            .selectItems(buildAuthorOptions(author))
            .and().build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return Long.parseLong(result.getContext().get("chooseAuthor", String.class));
}
```

This is exactly what a hexagonal “inbound adapter” should look like:
it *asks* the domain through facades and builds a UI from the answers.

---

### 4.2. AuthorFacade now talks straight to the repository

`AuthorFacadeImpl` no longer delegates through `AuthorService` for most operations. Instead, it calls `AuthorRepository` directly:

* `findByBookId` now uses the repository.
* `updateAuthor` calls `authorRepository.updateAuthor(...)`.
* New workflows like:

  * `createAuthorsIfNotExist(...)`
  * `getAuthorsById(...)`
  * `authorExistFirstNameLastName(...)`
  * `getAllAuthorsByName(...)`

live in the facade + repository combo.

The significance:

* Fewer layers doing “just pass-through”.
* Clearer role for the facade: **API boundary + orchestration**.
* Domain logic is closer to the persistence layer where it belongs for these operations.

---

### 4.3. BookDomainRepository & BookMapper grew into metadata ingestion

Two major changes here:

1. **Lookup & mapping from Google Books:**

```java
public BookMetaDataResponse toBookMetaDataResponseFromGoogleBooksResponse(
        GoogleBooksResponse googleBooksResponse, String isbn) {

    if (googleBooksResponse == null 
        || googleBooksResponse.items() == null 
        || googleBooksResponse.items().isEmpty()) {
        throw new RuntimeException("No book found for ISBN: " + isbn);
    }

    List<String> authors =
            new ArrayList<>(googleBooksResponse.items().get(0).volumeInfo().authors());

    log.info("""
            
            Mapped GoogleBooksResponse to BookMetaDataResponse for ISBN: {}
            Book Title: {}
            Authors: {}
            
            """,
            isbn,
            googleBooksResponse.items().get(0).volumeInfo().title(),
            String.join(", ", authors)
    );

    return new BookMetaDataResponse(
            null,
            googleBooksResponse.items().get(0).volumeInfo().title(),
            isbn,
            authors,
            googleBooksResponse.items().get(0).volumeInfo().publisher(),
            googleBooksResponse.items().get(0).volumeInfo().description()
    );
}
```

2. **Turning that metadata into a real BookEntity:**

```java
public BookEntity toEntityFromBookMetaDataResponse(
        BookMetaDataResponse meta, String isbn, Long shelfId) {

    if (meta == null) return null;

    authorFacade.createAuthorsIfNotExist(meta.authors());

    BookEntity bookEntity = new BookEntity();
    bookEntity.setTitle(meta.title());
    bookEntity.setIsbn(isbn);

    log.info("Fetching AuthorEntities for authors: " + String.join(", ", meta.authors()));
    bookEntity.setAuthors(authorFacade.getAuthorsById(meta.authors()));

    bookEntity.setPublisher(meta.publisher());
    bookEntity.setDescription(meta.description());
    bookEntity.setShelfId(shelfId);
    bookEntity.setAvailabilityStatus(AvailabilityStatus.AVAILABLE.name());
    bookEntity.setCreatedAt(LocalDate.now());
    bookEntity.setUpdatedAt(LocalDate.now());

    return bookEntity;
}
```

Then `BookDomainRepositoryImpl.createBookFromMetaData(...)` saves the entity and prints a nice summary in the CLI.

This is the beginning of a proper “ingestion pipeline” for external data.

---

## 5. The New Author Flow (Core Code Slice)

The heart of the duplicate-author problem lives in how manual book creation collects authors.

### Old mental model: “Collect author DTOs → Save everyone blindly.”

New mental model: **“Resolve identity per author, interactively.”**

```java
public List<AuthorDTO> createAuthors() {
    int numberOfAuthors = cliPrompt.promptForBookAuthorCount();
    List<AuthorDTO> authors = new ArrayList<>();

    for (int i = 0; i < numberOfAuthors; i++) {
        AuthorDTO authorDTO = cliPrompt.promptForAuthor();
        log.info("Collected Author Details: {}", authorDTO);

        if (authorFacade.authorExistFirstNameLastName(authorDTO.firstName(), authorDTO.lastName())) {
            log.info("Author already exists: {} {}", authorDTO.firstName(), authorDTO.lastName());
            System.out.println("Multiple Authors with this name.\n");

            Long authorId = cliPrompt.promptMultipleAuthorConfirmation(authorDTO);
            log.info("Existing author selected with ID: {}", authorId);

            if (authorId == 0) {
                // user explicitly chose "Create New Author"
                log.info("Creating new author as per user request: {} {}", authorDTO.firstName(), authorDTO.lastName());
                authors.add(authorFacade.saveAuthor(authorDTO));
                log.info("Author saved: {}", authors.get(i));
            } else {
                log.info("Fetching existing author with ID: {}", authorId);
                AuthorDTO existingAuthor = authorFacade.findById(authorId);
                authors.add(existingAuthor);
                log.info("Existing author added to list: {}", existingAuthor);
            }

        } else {
            // No existing authors with that name
            log.info("Creating new author: {} {}", authorDTO.firstName(), authorDTO.lastName());
            authors.add(authorFacade.saveAuthor(authorDTO));
            log.info("Author saved: {}", authors.get(i));
        }
    }

    log.info("Authors Created and Returned: {}", authors);
    return authors;
}
```

This is the core “author identity resolution” feature.

---

## 6. Lessons & Next Steps

**What I learned:**

* “Name” is not a stable identity. The domain needs explicit workflows to resolve ambiguity.
* CLI flows quickly become unmaintainable without a small abstraction (`ScanMode`) to encode decisions.
* Prompt/UI layers are powerful when they can query the domain directly through facades and build dynamic screens.

**Next steps I’m considering:**

* Extract a dedicated “AuthorResolutionService” so multiple flows (CLI, REST, etc.) can reuse the same logic.
* Add tests around:

  * “multiple authors, same name” scenarios
  * “import metadata for an ISBN that already exists”
* Consider a richer `AuthorSummaryDTO` for those selection screens to reduce coupling between CLI and BookFacade.

---

This devlog marks the point where Bibby stopped treating “author name” as a dumb string and started treating it as an identity problem with real UX and real domain rules.