------

# Micro-Slice Spec: Bookcase Shelf Count & Shelf Capacity

## 1. Context

This slice extends the **Bookcase creation flow** so that:

- The user can specify:
  - How many shelves a bookcase has.
  - How many books fit on each shelf.
- The system:
  - Confirms the configuration before saving.
  - Persists both the **number of shelves** and the **per-shelf capacity**, and also stores **total book capacity** at the bookcase level.

This is groundwork for future behavior like “prevent adding a book to a full shelf.”

------

## 2. Goal / Outcome

**Goal:**
 When creating a new bookcase (via CLI or REST), the user specifies:

- Bookcase label
- Shelf count
- Books per shelf

The program:

- Confirms these values with the user.
- Persists:
  - The bookcase record (with total capacity).
  - The correct number of shelf records, each with the per-shelf capacity value.

------

## 3. CLI Behavior (BookcaseCommands)

### Command

```
bookcase add
```

### Flow

1. Prompt for label:

   ```java
   .withStringInput("bookcaseLabel")
   .name("What should we call this new bookcase?:_")
   ```

2. Prompt for shelf count:

   ```java
   .withStringInput("shelfCount")
   .name("How many shelves does it have?:_")
   ```

3. Prompt for books per shelf:

   ```java
   .withStringInput("bookCapacity")
   .name("And how many books fit on a single shelf?:_")
   ```

4. The code parses values:

   ```java
   String bookcaseLabel = result.getContext().get("bookcaseLabel");
   int shelfCount = Integer.parseInt(result.getContext().get("shelfCount"));
   int bookCapacity = Integer.parseInt(result.getContext().get("bookCapacity"));
   ```

5. Summary & confirmation:

   ```text
   -----------------------------------
           NEW BOOKCASE SUMMARY
   -----------------------------------
   Label:          <bookcaseLabel>
   Shelf Count:    <shelfCount>
   Capacity/Shelf: <bookCapacity>
   
   Total Storage:  <shelfCount * bookCapacity> books
   -----------------------------------
   ```

6. Confirmation prompt:

   ```java
   .withStringInput("confirmation")
   .name("Are these details correct? (y/n):_")
   ```

7. Behavior:

   ```java
   if (res.getContext().get("confirmation").equals("Y") |
       res.getContext().get("confirmation").equals("y")) {
   
       bookcaseService.createNewBookCase(bookcaseLabel, shelfCount, bookCapacity);
       System.out.println("Created");
   } else {
       System.out.println("Not Created");
   }
   ```

> Note for future cleanup:
>
> - `|` should be `||` (logical OR).
> - You’ll probably want validation and retry on bad integers / non-numeric input.

------

## 4. REST API Behavior (BookCaseController + DTO)

### Request DTO

```java
public record BookcaseDTO(
    Long bookcaseId,
    String bookcaseLabel,
    int shelfCapacity,
    int bookCapacity
) {}
```

Here, in practice:

- `shelfCapacity` = number of shelves
- `bookCapacity` = books per shelf (from the API’s perspective)

### Endpoint

```java
@PostMapping("/create/bookcase")
public ResponseEntity<String> createBookCase(@RequestBody BookcaseDTO bookcaseDTO){
    String message = bookCaseService.createNewBookCase(
        bookcaseDTO.bookcaseLabel(),
        bookcaseDTO.shelfCapacity(),
        bookcaseDTO.bookCapacity()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(message);
}
```

So the HTTP API mirrors the CLI:

- `shelfCapacity` → shelf count
- `bookCapacity` → books per shelf

------

## 5. Service Logic (BookcaseService)

### Signature

```java
public String createNewBookCase(String label, int shelfCapacity, int bookCapacity)
```

Interpretation:

- `label` → bookcase label
- `shelfCapacity` → number of shelves on this bookcase
- `bookCapacity` → books per shelf

### Behavior

1. Check for existing bookcase:

   ```java
   BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
   if (bookcaseEntity != null) {
       log.error("Failed to save Record - Record already exist", existingRecordError);
       throw existingRecordError;
   }
   ```

2. Create and save new bookcase:

   ```java
   bookcaseEntity = new BookcaseEntity(
       label,
       shelfCapacity,
       bookCapacity * shelfCapacity // total book capacity for whole bookcase
   );
   bookcaseRepository.save(bookcaseEntity);
   ```

   - `BookcaseEntity.shelfCapacity` = number of shelves
   - `BookcaseEntity.bookCapacity` = total number of books the **entire** bookcase can hold

3. Create shelves:

   ```java
   for (int i = 0; i < bookcaseEntity.getShelfCapacity(); i++) {
       addShelf(bookcaseEntity, i, i, bookCapacity);
   }
   ```

4. Shelf creation:

   ```java
   public void addShelf(BookcaseEntity bookcaseEntity, int label, int position, int bookCapacity){
       shelfRepository.save(
           shelfFactory.createEntity(
               bookcaseEntity.getBookcaseId(),
               position,
               "Shelf " + label,
               bookCapacity
           )
       );
   }
   ```

5. Return message:

   ```java
   return "Created New Bookcase " + label + " with shelf shelfCapacity of " + shelfCapacity;
   ```

> Minor nit: “shelf shelfCapacity” is a tiny typo; later you’ll want this to be more human (“with 5 shelves, 40 books per shelf”).

------

## 6. Persistence Model

### BookcaseEntity

```java
public class BookcaseEntity {
    private Long bookcaseId;
    private String bookcaseLabel;
    private String bookcaseDescription;

    private int bookCapacity;   // currently: total book capacity for the bookcase
    private int shelfCapacity;  // currently: number of shelves

    public BookcaseEntity(String bookcaseLabel, int shelfCapacity, int bookCapacity) {
        this.bookcaseLabel = bookcaseLabel;
        this.shelfCapacity = shelfCapacity;
        this.bookCapacity = bookCapacity;
    }

    public BookcaseEntity(Long bookcaseId, String bookcaseLabel, int shelfCapacity, int bookCapacity) {
        this.bookcaseId = bookcaseId;
        this.bookcaseLabel = bookcaseLabel;
        this.shelfCapacity = shelfCapacity;
        this.bookCapacity = bookCapacity;
    }

    public int getBookCapacity() { return bookCapacity; }
    public void setBookCapacity(int bookCapacity) { this.bookCapacity = bookCapacity; }
}
```

Semantics as currently implemented:

- `shelfCapacity` = *how many shelves*.
- `bookCapacity` = *total* books the bookcase can hold (not per shelf).

You might later rename these to something like:

- `int shelfCount;`
- `int totalBookCapacity;`

to better reflect reality.

### ShelfEntity

```java
public class ShelfEntity {
    private Long shelfId;
    private String shelfLabel;
    private Long bookcaseId;
    private int shelfPosition;
    private int bookCapacity;         // books allowed on this shelf
    private String shelfDescription;

    public int getBookCapacity() { return bookCapacity; }

    public void setBookCapacity(int shelfCapacity) {
        this.bookCapacity = shelfCapacity;
    }
}
```

Semantics:

- `bookCapacity` = books per shelf (this is the “per-shelf capacity”).

Again, future-name-clarity idea: `shelfBookCapacity` or just `capacity`.

### ShelfFactory

```java
public ShelfEntity createEntity(Long bookCaseId, int shelfPosition, String shelfLabel, int bookCapacity){
    ShelfEntity shelfEntity = new ShelfEntity();
    shelfEntity.setBookcaseId(bookCaseId);
    shelfEntity.setShelfLabel(shelfLabel);
    shelfEntity.setShelfPosition(shelfPosition);
    shelfEntity.setBookCapacity(bookCapacity);
    return shelfEntity;
}
```

------

## 7. Acceptance Criteria (What This Slice Delivers)

1. **CLI prompts for all three fields**
   - Label
   - Shelf count
   - Books per shelf
   - And asks for confirmation before creation.
2. **REST API can create bookcases**
   - `/create/bookcase` accepts `BookcaseDTO` with `bookcaseLabel`, `shelfCapacity` (shelf count), and `bookCapacity` (per-shelf).
3. **Bookcase persistence**
   - New `BookcaseEntity` is created with:
     - `shelfCapacity` = shelf count
     - `bookCapacity` = shelfCount * booksPerShelf
4. **Shelves persistence**
   - Exactly `shelfCapacity` shelves are created.
   - Each `ShelfEntity.bookCapacity` = books per shelf from the CLI/API input.
5. **No duplicate bookcase labels**
   - Attempting to create a bookcase with an existing label throws `existingRecordError`.

------

## 8. Small Refactor / Cleanup Notes (For Next Micro-Slice)

*Not part of this slice’s “done”, but good targets:*

- Fix `if (Y | y)` to use logical OR (`||`).
- Improve naming for clarity:
  - `shelfCapacity` → `shelfCount`
  - `bookCapacity` in `BookcaseEntity` → `totalBookCapacity`
  - `bookCapacity` in `ShelfEntity` → `shelfBookCapacity` or `capacity`.
- Add validation for:
  - `shelfCount >= 1`
  - `bookCapacity >= 1`
  - and handle non-integer CLI input gracefully.

------

This micro-slice gives you a clean foundation: you now know **how many shelves** exist and **how much each shelf can carry**. The natural next slice is teaching Shelf and Book operations to respect that capacity and fail loudly when someone tries to shove book #41 onto a 40-book shelf.