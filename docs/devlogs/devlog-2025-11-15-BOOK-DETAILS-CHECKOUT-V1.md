

------

# Dev Log — `BOOK-DETAILS-CHECKOUT-V1`

**Date:** November 15, 2025
 **Slice:** `BOOK-DETAILS-CHECKOUT-V1`
 **Goal:** After selecting a book from the browse flow, show a Book Detail screen and let the user check the book out, persisting the change in the database.

------

## 🎯 What I Implemented

### 1. Domain & Service Layer

- **Removed legacy `authorId` field** from `BookEntity`
  - Cleaned up the old single-author design so the entity is no longer tied to a single `authorId` column.
  - Commented out the old mapping code in `BookMapper` that tried to set `authorId`.
- **Added detail + lookup methods to `BookService`:**
  - `BookDetailView getBookDetails(Long bookId)`
    - Delegates to the repository’s projection query.
  - `Optional<BookEntity> findBookById(Long bookId)`
    - Thin wrapper over `bookRepository.findById(bookId)` for the checkout operation.

------

### 2. View Model / Mapping

- Created / used a **record-based DTO** for the detail view:

  ```java
  public record BookDetailView(
      Long bookId,
      String title,
      String authors,
      String bookcaseLabel,
      String shelfLabel,
      String bookStatus
  ) {}
  ```

- Wired a **native query projection** in `BookRepository`:

  ```java
  @Query(value = """
      SELECT b.book_id, b.title,
             STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
             bc.bookcase_label, s.shelf_label, b.book_status
      FROM books b
      JOIN book_authors ba ON b.book_id = ba.book_id
      JOIN authors a ON ba.author_id = a.author_id
      JOIN shelves s ON s.shelf_id = b.shelf_id
      JOIN bookcases bc ON bc.bookcase_id = s.bookcase_id
      WHERE b.book_id = :bookId
      GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.book_status
      """, nativeQuery = true)
  BookDetailView getBookDetailView(Long bookId);
  ```

- This query:

  - Joins **books**, **book_authors**, **authors**, **shelves**, and **bookcases**.
  - Aggregates multiple authors with `STRING_AGG` into a single comma-separated string.
  - Returns a **single `BookDetailView`** wired by constructor.

------

### 3. CLI Rendering & Flow

- Extended `BookcaseCommands` with a **Book Detail + Checkout flow**:

  ```java
  public void getBookDetailsView(Long bookId) {
      BookDetailView bookDetails = bookService.getBookDetails(bookId);
  
      String res = String.format("""
          Title      \u001B[38;5;197m%-10s  \u001B[0m
          Authors    \u001B[38;5;197m%-10s  \u001B[0m
          Bookcase   \u001B[38;5;197m%-10s  \u001B[0m
          Bookshelf  \u001B[38;5;197m%-10s  \u001B[0m
          Book Status\u001B[38;5;197m%-10s  \u001B[0m
          """,
          bookDetails.title(),
          bookDetails.authors(),
          bookDetails.bookcaseLabel(),
          bookDetails.shelfLabel(),
          bookDetails.bookStatus()
      );
  
      System.out.println(res);
      // … checkout prompt …
  }
  ```

- Added a **Yes/No checkout prompt** using `ComponentFlow`:

  ```java
  Map<String, String> checkOutOptions = new LinkedHashMap<>();
  checkOutOptions.put("Yes", "1");
  checkOutOptions.put("No", "2");
  
  ComponentFlow flow = componentFlowBuilder.clone()
      .withSingleItemSelector("optionSelected")
      .name("Would You Like To Check-Out?")
      .selectItems(checkOutOptions)
      .and()
      .build();
  
  ComponentFlow.ComponentFlowResult result = flow.run();
  ```

- If the user selects **Yes**:

  ```java
  if (result.getContext().get("optionSelected").equals("1")) {
      Optional<BookEntity> bookEntity = bookService.findBookById(bookId);
      bookService.checkOutBook(bookEntity.get());
  }
  ```

- This completes the UX loop:

  - `bookcase browse` → select **Bookcase**
  - → select **Shelf**
  - → select **Book**
  - → view **Book Details** → choose **Check-Out**

------

### 4. Wiring Into Browse Flow

- After the user selects a book from the shelf selector, the flow now calls:

  ```java
  getBookDetailsView(
      Long.parseLong(result.getContext().get("shelfSelected"))
  );
  ```

- That value is treated as the selected `bookId` and passed into the detail + checkout flow, which then:

  - Pulls the `BookDetailView` via the new repository method.
  - Renders the detail screen.
  - Offers the checkout option.

------

## ✅ Current Behavior (from the run)

From the screenshot:

- Bookcase browse flow works end-to-end.
- Detail screen shows:
  - Title
  - Authors (aggregated from `book_authors` + `authors`)
  - Bookcase label
  - Shelf label
  - Book status (currently `null` for some records, which is a data/model issue to tighten up later).
- Prompt appears:

```text
? Would You Like To Check-Out? [Use arrows to move], type to filter
> No
  Yes
```

Choosing **Yes** calls `checkOutBook` on the selected `BookEntity`, updating the DB.

------

## 🧪 Next Steps / Follow-Ups

Not required for this slice, but worth parking:

- Add **service-level tests** for:
  - `getBookDetailView` native query (happy path).
  - `checkOutBook` (status change, error when book not found).
- Update seed data to ensure `book_status` isn’t `null` (e.g., default to `"Available"`).
- Add a **post-checkout confirmation message**, like:
  - `Bibby: "Got it. This one is now checked out."`
- Consider guarding the checkout call:
  - If status is already `"Checked Out"`, print a friendly message instead of blindly calling `checkOutBook`.

------

