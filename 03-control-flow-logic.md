# Section 3: Control Flow & Logic

Welcome to Section 3! Now that you understand Java's type system, let's examine how you **control the flow** of execution in your code. Every program is fundamentally about making decisions and repeating actions - if/else statements and loops are the building blocks of logic.

Your Bibby application has excellent examples of control flow patterns - some that work well, and some that could be simplified for better readability and maintainability.

---

## What Is Control Flow?

Control flow determines **the order in which statements execute**. Java provides several constructs:

**Decision Making:**
- `if`, `else if`, `else` - Conditional branching
- `switch` - Multi-way branching (and modern switch expressions in Java 14+)
- Ternary operator `? :` - Compact conditional

**Looping:**
- `for` - Counting loops
- Enhanced `for` (for-each) - Iterating collections
- `while` - Conditional loops
- `do-while` - Execute-then-check loops

**Transfer Control:**
- `return` - Exit method
- `break` - Exit loop/switch
- `continue` - Skip to next iteration
- `throw` - Exception handling

---

## Why This Matters: Readable vs Complex Logic

Consider these two versions of the same logic:

**Complex (Hard to Read):**
```java
if (book != null) {
    if (book.getShelfId() != null) {
        Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());
        if (shelf.isPresent()) {
            Optional<BookcaseEntity> bookcase = bookcaseService.findBookCaseById(shelf.get().getBookcaseId());
            if (bookcase.isPresent()) {
                System.out.println("Found at: " + bookcase.get().getBookcaseLabel());
            } else {
                System.out.println("Bookcase not found");
            }
        } else {
            System.out.println("Shelf not found");
        }
    } else {
        System.out.println("Book has no shelf");
    }
} else {
    System.out.println("Book not found");
}
```

**Clear (Easy to Read - Guard Clauses):**
```java
if (book == null) {
    System.out.println("Book not found");
    return;
}

if (book.getShelfId() == null) {
    System.out.println("Book has no shelf");
    return;
}

Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());
if (shelf.isEmpty()) {
    System.out.println("Shelf not found");
    return;
}

Optional<BookcaseEntity> bookcase = bookcaseService.findBookCaseById(shelf.get().getBookcaseId());
if (bookcase.isEmpty()) {
    System.out.println("Bookcase not found");
    return;
}

System.out.println("Found at: " + bookcase.get().getBookcaseLabel());
```

The second version uses **guard clauses** - check for error conditions first and exit early. Let's see how this pattern appears in your code.

---

## In Your Code: What You're Doing Well

### ✅ Early Return Pattern (Partial)

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 43-54

```java
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());
    }

    if(bookEntity.isEmpty()){
        return null;
    }
    return bookEntity.get();
}
```

**What You're Doing Right:**

You check `isEmpty()` and return early! This avoids the "else" pyramid of doom:

```java
// Instead of:
if (bookEntity.isEmpty()) {
    return null;
} else {
    return bookEntity.get();
}

// You wrote:
if (bookEntity.isEmpty()) {
    return null;
}
return bookEntity.get();
```

**Why This Is Better:**
- Less nesting (easier to read)
- Shorter lines (fits on screen)
- Clear failure path
- Symmetrical structure

This is called the **"guard clause"** pattern, and you used it instinctively!

---

### ✅ Enhanced For Loop

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 46-48

```java
for(BookEntity b : bookEntities){
    System.out.println(b.getTitle());
}
```

**Why This Is Good:**

You're using the enhanced for loop (for-each) instead of index-based:

```java
// You could have written (worse):
for (int i = 0; i < bookEntities.size(); i++) {
    BookEntity b = bookEntities.get(i);
    System.out.println(b.getTitle());
}

// You wrote (better):
for (BookEntity b : bookEntities) {
    System.out.println(b.getTitle());
}
```

**Advantages:**
- ✅ No index management (no off-by-one errors)
- ✅ Clearer intent ("for each book...")
- ✅ Works with any Iterable
- ✅ Shorter, more readable

**When Enhanced For Is Perfect:**
- You don't need the index
- You're iterating the entire collection
- You're not modifying the collection while iterating

---

### ✅ Counting For Loop

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 114-116

```java
for(int i = 0; i < authorCount; i++){
    authorNameComponentFlow(title);
}
```

**Why This Is Correct:**

Here you **DO** need the index-based loop because:
- You're counting (0 to authorCount)
- Not iterating a collection
- Repeating an action N times

This is the **right tool for the job**. Well done choosing the appropriate loop type!

---

## In Your Code: Where You Can Improve

### ⚠️ Deep Nesting (Pyramid of Doom)

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 356-364

```java
if (bookEntity == null) {
    System.out.println("\n\u001B[36m</>\u001B[0m:I just flipped through every shelf — no luck this time.\n");
}else if(bookEntity.getShelfId() == null){
    System.out.println("\nBook Was Found Without a Location\n");
}else{
    Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
    Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
    System.out.println("\nBook Was Found \nBookcase: " + bookcaseEntity.get().getBookcaseLabel() + "\nShelf: " + shelfEntity.get().getShelfLabel() + "\n");
}
```

**The Problems:**

1. **No null safety on Optional.get()** - Line 362-363 call `get()` without checking `isPresent()`
2. **Mixed concerns** - Error messages and success logic intertwined
3. **Hard to extend** - Adding more conditions requires more nesting

**The Danger:**

```java
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
// What if shelf doesn't exist? Next line crashes!
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
//                                                                         ^^^^^^^ CRASH HERE
```

**Refactored Version (Guard Clauses):**

```java
// Check for missing book first
if (bookEntity == null) {
    System.out.println("\n\u001B[36m</>\u001B[0m: I just flipped through every shelf — no luck this time.\n");
    return;
}

// Check for missing shelf ID
if (bookEntity.getShelfId() == null) {
    System.out.println("\nBook Was Found Without a Location\n");
    return;
}

// Try to find shelf
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
if (shelfEntity.isEmpty()) {
    System.out.println("\nBook references a shelf that doesn't exist.\n");
    return;
}

// Try to find bookcase
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
if (bookcaseEntity.isEmpty()) {
    System.out.println("\nShelf references a bookcase that doesn't exist.\n");
    return;
}

// Success case - only executes if all checks passed
System.out.println(String.format(
    "\nBook Was Found\nBookcase: %s\nShelf: %s\n",
    bookcaseEntity.get().getBookcaseLabel(),
    shelfEntity.get().getShelfLabel()
));
```

**What Changed:**
- ✅ Each error condition exits immediately (guard clauses)
- ✅ Optional.get() is safe (only called after isEmpty() check)
- ✅ No nesting - linear flow
- ✅ Easy to add new conditions
- ✅ Clear error messages for each failure case

---

### ⚠️ Duplicated Get Calls on Optional

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 474-476

```java
if (book.getShelfId() != null){
    Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());
    Optional<BookcaseEntity> bookcase = bookcaseService.findBookCaseById(shelf.get().getBookcaseId());
    //                                                                     ^^^^^^^^^^
    bookcaseName = bookcase.get().getBookcaseLabel();
    //             ^^^^^^^^^^^
    shelfName = shelf.get().getShelfLabel();
    //          ^^^^^^^^^^^
}
```

**The Problem:**

Calling `get()` without checking `isPresent()` is **dangerous**. If the shelf or bookcase doesn't exist, you get:
```
java.util.NoSuchElementException: No value present
```

**Refactored Version:**

```java
if (book.getShelfId() != null) {
    Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());

    if (shelf.isPresent()) {
        shelfName = shelf.get().getShelfLabel();

        Optional<BookcaseEntity> bookcase = bookcaseService.findBookCaseById(shelf.get().getBookcaseId());
        if (bookcase.isPresent()) {
            bookcaseName = bookcase.get().getBookcaseLabel();
        }
    }
}
```

**Even Better (Store the Value):**

```java
if (book.getShelfId() != null) {
    Optional<ShelfEntity> shelfOpt = shelfService.findShelfById(book.getShelfId());

    if (shelfOpt.isPresent()) {
        ShelfEntity shelf = shelfOpt.get(); // Get once, use many times
        shelfName = shelf.getShelfLabel();

        Optional<BookcaseEntity> bookcaseOpt = bookcaseService.findBookCaseById(shelf.getBookcaseId());
        if (bookcaseOpt.isPresent()) {
            BookcaseEntity bookcase = bookcaseOpt.get();
            bookcaseName = bookcase.getBookcaseLabel();
        }
    }
}
```

**Best (Use Optional Methods - Section 15 will cover this!):**

```java
if (book.getShelfId() != null) {
    shelfService.findShelfById(book.getShelfId())
        .ifPresent(shelf -> {
            shelfName = shelf.getShelfLabel();

            bookcaseService.findBookCaseById(shelf.getBookcaseId())
                .ifPresent(bookcase -> {
                    bookcaseName = bookcase.getBookcaseLabel();
                });
        });
}
```

---

### ⚠️ Complex Nested If-Else Chain

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 203-209

```java
String searchType = result.getContext().get("searchType", String.class);
if (searchType.equalsIgnoreCase("author")){
    searchByAuthor();
}else if(searchType.equalsIgnoreCase("title")){
    searchByTitle();
}
```

**This Works, But...**

What happens when you add more search types?

```java
if (searchType.equalsIgnoreCase("author")){
    searchByAuthor();
} else if(searchType.equalsIgnoreCase("title")){
    searchByTitle();
} else if(searchType.equalsIgnoreCase("genre")){
    searchByGenre();
} else if(searchType.equalsIgnoreCase("shelf")){
    searchByShelf();
} else if(searchType.equalsIgnoreCase("status")){
    searchByStatus();
} else {
    System.out.println("Unknown search type");
}
```

This becomes a **maintenance nightmare**. There's a better way...

---

### 🚀 Switch Expression (Java 14+)

Since you're on Java 17, you can use **switch expressions**:

**Refactored with Switch Expression:**

```java
String searchType = result.getContext().get("searchType", String.class);

switch (searchType.toLowerCase()) {
    case "author" -> searchByAuthor();
    case "title" -> searchByTitle();
    case "genre" -> searchByGenre();
    case "shelf" -> searchByShelf();
    case "status" -> searchByStatus();
    default -> System.out.println("Unknown search type: " + searchType);
}
```

**Why This Is Better:**
- ✅ More compact (arrow syntax `->`)
- ✅ No break statements needed
- ✅ No fall-through bugs
- ✅ Easier to read (vertical list of cases)
- ✅ Exhaustiveness checking possible

**Even More Powerful - Switch as Expression:**

```java
String searchType = result.getContext().get("searchType", String.class);

Runnable searchAction = switch (searchType.toLowerCase()) {
    case "author" -> this::searchByAuthor;
    case "title" -> this::searchByTitle;
    case "genre" -> this::searchByGenre;
    case "shelf" -> this::searchByShelf;
    case "status" -> this::searchByStatus;
    default -> () -> System.out.println("Unknown search type: " + searchType);
};

searchAction.run();
```

**Or Return Values:**

```java
String message = switch (searchType.toLowerCase()) {
    case "author", "title" -> "Searching by text...";
    case "genre", "status" -> "Filtering by category...";
    case "shelf" -> "Looking at physical location...";
    default -> "Unknown search type";
};
System.out.println(message);
```

---

### ⚠️ String Comparison with equals()

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 57-59

```java
if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
    bookEntity.setBookStatus("CHECKED_OUT");
    bookRepository.save(bookEntity);
}
```

**The Problem:**

You have a `BookStatus` enum but you're comparing Strings! This defeats the purpose of the enum.

**What Could Go Wrong:**

```java
bookEntity.setBookStatus("CHECKED_OUT");  // Correct
bookEntity.setBookStatus("Checked_Out");  // Typo - compiles but wrong!
bookEntity.setBookStatus("checked-out");  // Typo - compiles but wrong!
```

**Refactored (Use Enum Everywhere):**

First, change BookEntity to store enum:

```java
// BookEntity.java
@Enumerated(EnumType.STRING) // Tell JPA to store as String in DB
private BookStatus bookStatus; // Not String!

public BookStatus getBookStatus() {
    return bookStatus; // Return enum
}

public void setBookStatus(BookStatus status) {
    this.bookStatus = status; // Accept enum
}
```

Then your service code becomes:

```java
// BookService.java
if (bookEntity.getBookStatus() != BookStatus.CHECKED_OUT) {
    bookEntity.setBookStatus(BookStatus.CHECKED_OUT);
    bookRepository.save(bookEntity);
}
```

**Benefits:**
- ✅ Compile-time safety (typos caught by compiler)
- ✅ IDE autocomplete
- ✅ Can't use invalid values
- ✅ Can use switch with exhaustiveness checking

---

### ⚠️ Mixing Business Logic in Commands

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 507-517

```java
if (confirmationResult.getContext().get("isConfirmed").equals("y")){
    bookService.checkOutBook(book);
    System.out.println(
        String.format("""

        \u001B[38;5;63m  .---.
        \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "All set — \u001B[38;5;42m %s \u001B[38;5;15m is checked out and ready to go with you. \033[36m"
        \u001B[38;5;63m  \\|=|/

        """,bookTitle));
}
```

**This Is Actually Good, But...**

The conditional logic here is straightforward. However, notice the pattern:

```java
if (confirmationResult.getContext().get("isConfirmed").equals("y"))
```

**This Could Be:**

```java
String confirmation = confirmationResult.getContext().get("isConfirmed");
if ("y".equals(confirmation)) { // Null-safe
    // ...
}
```

**Why `"y".equals(confirmation)` instead of `confirmation.equals("y")`?**

If `confirmation` is null:
- `confirmation.equals("y")` → NullPointerException ❌
- `"y".equals(confirmation)` → Returns false ✅

This is called **Yoda Conditions** (constant first), and it prevents NPE.

---

## Short-Circuit Evaluation

Java uses **short-circuit** evaluation for `&&` and `||`:

**AND (`&&`):**
```java
if (book != null && book.getTitle() != null) {
    // book.getTitle() only called if book != null
}
```

If `book` is null, the second condition is **never evaluated**. This prevents NullPointerException!

**OR (`||`):**
```java
if (status.equals("AVAILABLE") || status.equals("RESERVED")) {
    // If first is true, second never checked
}
```

**Using This in Your Code:**

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 478

```java
if (book.getBookStatus().equals("CHECKED_OUT")){
    // What if bookStatus is null? NPE!
}
```

**Refactored (Short-Circuit Protection):**

```java
if (book.getBookStatus() != null && book.getBookStatus().equals("CHECKED_OUT")) {
    // Safe: bookStatus checked before equals()
}

// Or better (Yoda condition):
if ("CHECKED_OUT".equals(book.getBookStatus())) {
    // Safe: null-safe equals
}

// Or best (use enum):
if (BookStatus.CHECKED_OUT.equals(book.getBookStatus())) {
    // Type-safe and null-safe
}
```

---

## Loop Patterns in Your Code

### Enhanced For (For-Each)

You use this well! Examples:

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java`
📍 Lines: 35-37

```java
for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
    addShelf(bookcaseEntity, i, i);
}
```

**This is correct** because you need the index `i` for the label and position.

**Alternative Consideration:**

If you just needed to repeat N times without using the index:

```java
// Current (index needed)
for(int i = 0; i < capacity; i++){
    addShelf(bookcaseEntity, i, i);
}

// If you didn't need i (hypothetical)
IntStream.range(0, capacity)
    .forEach(i -> addShelf(bookcaseEntity, i, i));
```

But your current code is **clearer and more readable**. Don't use streams just for the sake of it!

---

### Potential Infinite Loop Risk

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 375-377

```java
if (result.getContext().get("searchDecision",String.class).equalsIgnoreCase("Yes")){
    searchBook(); // Calls the method we're already in!
}
```

**Analysis:**

This is **recursive** - `searchByTitle()` calls `searchBook()` which might call `searchByTitle()` again. This creates a loop until the user says "no."

**This is actually intentional** (interactive CLI loop), but be aware:
- No base case could mean infinite recursion
- Deep recursion could cause StackOverflowError
- Consider iterative loop instead:

**Alternative (Iterative):**

```java
public void interactiveSearch() {
    boolean keepSearching = true;

    while (keepSearching) {
        searchBook();

        // Ask if they want to continue
        ComponentFlow flow = componentFlowBuilder.clone()
            .withSingleItemSelector("searchDecision")
            .name("Would you like to search again?")
            .selectItems(yesNoOptions())
            .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        keepSearching = result.getContext().get("searchDecision", String.class).equalsIgnoreCase("Yes");
    }
}
```

**Why This Might Be Better:**
- ✅ Explicit loop condition (clear intent)
- ✅ No recursion (no stack overflow risk)
- ✅ Easy to add "search history" or other state
- ⚠️ More lines of code

Your recursive approach works for interactive CLI, but know the tradeoff!

---

## Ternary Operator

The ternary operator `? :` is a compact if-else:

```java
// If-else
String status;
if (book.isAvailable()) {
    status = "Available";
} else {
    status = "Checked Out";
}

// Ternary
String status = book.isAvailable() ? "Available" : "Checked Out";
```

**When to Use:**
- ✅ Simple assignments
- ✅ Return statements
- ✅ One-liners
- ❌ Nested ternaries (unreadable!)

**Example in Your Code:**

📁 File: `src/main/java/com/penrose/bibby/library/book/BookMapper.java`
📍 Lines: 25, 42, 45, 47

```java
book.setStatus(e.getBookStatus() != null ? BookStatus.valueOf(e.getBookStatus()) : null);
bookEntity.setGenre(book.getGenre() !=null ? book.getGenre().getGenreName() : null);
bookEntity.setShelfId(book.getShelf() != null ? book.getShelf().getId() : null);
bookEntity.setBookStatus(book.getStatus() != null ? book.getStatus().name() : null);
```

**These are perfect uses of ternary!** They're:
- ✅ Simple (null check → value or null)
- ✅ One line (easy to read)
- ✅ Assigninments (natural use case)

Well done!

---

## Key Takeaways

**Control Flow Best Practices:**

1. **Guard Clauses** - Check error conditions early, return/exit immediately
2. **Avoid Deep Nesting** - More than 3 levels is a code smell
3. **Use Enhanced For** - When you don't need the index
4. **Switch Over If-Else Chains** - For multiple equality checks
5. **Short-Circuit &&/||** - Leverage for null safety
6. **Yoda Conditions** - Put constant first in equals() for null safety
7. **Early Returns** - Flatten your code, reduce nesting
8. **Ternary for Simple Cases** - Keep it to one level

**Decision Tree for Choosing Constructs:**

```
Multiple equality checks on same variable?
├─ Yes: Use switch (expression if Java 14+)
└─ No: Use if/else

Iterating a collection?
├─ Need index: for (int i = 0; i < size; i++)
├─ Just iterating: for (Type item : collection)
└─ Transforming/filtering: Stream API (Section 14)

Null safety needed?
├─ Use short-circuit: obj != null && obj.method()
└─ Or Yoda: "constant".equals(variable)
```

---

## Practice Exercise: Refactor searchByTitle()

**Your Task:**

Refactor the `searchByTitle()` method to use guard clauses and proper Optional handling.

📁 File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`
📍 Lines: 333-378

**Current Code Issues:**
1. Nested if-else (pyramid of doom)
2. Unsafe Optional.get() calls
3. Mixed success/error handling
4. Potential NullPointerException

**Step 1: Identify Error Paths**
- Book not found
- Book found but no shelf ID
- Shelf ID exists but shelf not found
- Shelf found but bookcase not found
- Success - everything found

**Step 2: Apply Guard Clauses**
- Check each error condition
- Return/handle immediately
- Let success case be at the end (not nested)

**Step 3: Add Optional Safety**
- Check isEmpty() before get()
- Or use ifPresent()/orElse()

**Solution:**

```java
public void searchByTitle() throws InterruptedException {
    System.out.println("\n");

    // Get title from user
    ComponentFlow flow = componentFlowBuilder.clone()
        .withStringInput("bookTitle")
        .name("Enter book title:_")
        .and().build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    String title = result.getContext().get("bookTitle", String.class);

    // Show searching message
    System.out.println("\u001B[36m</>\u001B[0m: Hold on, I'm diving into the stacks — Let's see if I can find " + title);
    System.out.print("\u001B[36m</>\u001B[0m:");
    Thread.sleep(1000);

    // Search for book
    BookEntity bookEntity = bookService.findBookByTitle(title);
    showLoading();
    Thread.sleep(500);

    // Guard: Check if book exists
    if (bookEntity == null) {
        System.out.println("\n\u001B[36m</>\u001B[0m: I just flipped through every shelf — no luck this time.\n");
        askSearchAgain();
        return;
    }

    // Guard: Check if book has shelf assignment
    if (bookEntity.getShelfId() == null) {
        System.out.println("\nBook Was Found Without a Location\n");
        askSearchAgain();
        return;
    }

    // Try to find shelf
    Optional<ShelfEntity> shelfOpt = shelfService.findShelfById(bookEntity.getShelfId());

    // Guard: Check if shelf exists
    if (shelfOpt.isEmpty()) {
        System.out.println("\nBook references shelf ID " + bookEntity.getShelfId() + " but that shelf doesn't exist.\n");
        askSearchAgain();
        return;
    }

    ShelfEntity shelf = shelfOpt.get(); // Safe - we checked isEmpty()

    // Try to find bookcase
    Optional<BookcaseEntity> bookcaseOpt = bookcaseService.findBookCaseById(shelf.getBookcaseId());

    // Guard: Check if bookcase exists
    if (bookcaseOpt.isEmpty()) {
        System.out.println("\nShelf references bookcase ID " + shelf.getBookcaseId() + " but that bookcase doesn't exist.\n");
        askSearchAgain();
        return;
    }

    BookcaseEntity bookcase = bookcaseOpt.get(); // Safe - we checked isEmpty()

    // Success case - only reached if all checks passed
    System.out.println(String.format(
        "\nBook Was Found\nBookcase: %s\nShelf: %s\n",
        bookcase.getBookcaseLabel(),
        shelf.getShelfLabel()
    ));

    askSearchAgain();
}

// Extract repeated logic
private void askSearchAgain() throws InterruptedException {
    Thread.sleep(2000);
    ComponentFlow flow = componentFlowBuilder.clone()
        .withSingleItemSelector("searchDecision")
        .name("Would you like to search again?")
        .selectItems(yesNoOptions())
        .and().build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    if (result.getContext().get("searchDecision", String.class).equalsIgnoreCase("Yes")) {
        searchBook();
    }
}
```

**What Improved:**
- ✅ Guard clauses - each error exits immediately
- ✅ No nesting - linear, readable flow
- ✅ Optional safety - isEmpty() checked before get()
- ✅ Clear error messages for each failure case
- ✅ Extracted repeated logic (askSearchAgain)
- ✅ Success case at end - only executes if all guards pass

---

## Action Items for This Week

**1. Refactor searchByTitle() with Guard Clauses**
**Priority:** HIGH
**Estimated Time:** 30 minutes
**File:** `src/main/java/com/penrose/bibby/cli/BookCommands.java`

Apply the refactoring from the practice exercise:
- Add guard clauses for each error condition
- Add Optional.isEmpty() checks before get()
- Extract askSearchAgain() helper method
- Test all code paths

**2. Convert If-Else Chain to Switch Expression**
**Priority:** MEDIUM
**Estimated Time:** 15 minutes
**File:** `src/main/java/com/penrose/bibby/cli/BookCommands.java`
**Lines:** 203-209

Convert the searchType if-else chain to a switch expression:
```java
switch (searchType.toLowerCase()) {
    case "author" -> searchByAuthor();
    case "title" -> searchByTitle();
    // ... add other cases from buildSearchOptions()
    default -> System.out.println("Unknown search type");
}
```

**3. Audit All Optional.get() Calls**
**Priority:** HIGH
**Estimated Time:** 20 minutes

Search for patterns:
```bash
grep -n "\.get()" src/main/java/**/*.java
```

Find all `.get()` calls and verify:
- [ ] Is there an isEmpty()/isPresent() check before?
- [ ] Is the value stored in a variable (not calling get() multiple times)?
- [ ] Could this use ifPresent()/orElse() instead?

Create a list of unsafe get() calls to fix.

---

## Further Study

**Books:**
- *Clean Code* by Robert Martin - Chapter 17: "Smells and Heuristics" (G5: Duplication, G34: Functions Should Descend Only One Level of Abstraction)
- *Refactoring* by Martin Fowler - "Replace Nested Conditional with Guard Clauses"

**Articles:**
- Baeldung: "Java Switch Statement" - https://www.baeldung.com/java-switch
- Oracle: "The switch Expressions" - Java Language Updates
- "Flattening Arrow Code" - https://blog.codinghorror.com/flattening-arrow-code/

**Java 17 Features:**
- Switch Expressions (JEP 361)
- Pattern Matching for switch (Preview in Java 17, stable in Java 21)

---

## Summary

You've learned:
- ✅ Guard clauses reduce nesting and improve readability
- ✅ Enhanced for loops are perfect when you don't need indices
- ✅ Switch expressions (Java 14+) beat if-else chains
- ✅ Short-circuit evaluation provides null safety
- ✅ Yoda conditions prevent NullPointerException
- ✅ Early returns flatten code structure
- ✅ Ternary operators work great for simple assignments

**Your code showed:**
- **Strengths:** Enhanced for loops, early returns, ternary operators
- **Opportunities:** Deep nesting, unsafe Optional.get(), if-else chains → switch

**Next Up:** Section 4 - Methods & Parameters (examining method design, parameter passing, overloading, and return types in your services and entities)

---

*Section created: 2025-11-17*
*Files analyzed: BookCommands, BookService, BookcaseService, BookMapper*
*Control flow patterns identified: 15+*
*Refactoring opportunities: 8*
