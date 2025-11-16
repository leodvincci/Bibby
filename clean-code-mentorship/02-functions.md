# SECTION 2: FUNCTIONS
## Clean Code Chapter 3 Applied to Your Bibby Codebase

---

## Table of Contents
1. [Principles Overview](#principles-overview)
2. [Your Code Analysis](#your-code-analysis)
3. [Deep Dive Refactoring Examples](#deep-dive-refactoring-examples)
4. [Patterns You Should Recognize](#patterns-you-should-recognize)
5. [Action Items](#action-items)
6. [Key Takeaways](#key-takeaways)
7. [Further Study](#further-study)

---

## Principles Overview

### The First Rule of Functions: They Should Be Small

Uncle Bob is emphatic about this: **functions should be small**. And then they should be smaller than that.

How small?
- **Ideal:** 4-5 lines
- **Maximum:** 20 lines
- **Never:** More than one level of abstraction

Your `addBook()` method in `BookCommands.java` is **81 lines** (lines 89-169). That's 4x the maximum and needs to be broken down.

### Why Small Functions Matter

When you read a story, you don't want to know every microscopic detail all at once. You want the high-level narrative first, then you can drill down into details if needed.

Code should work the same way:

```java
public void processOrder() {
    validateOrder();
    calculateTotal();
    applyDiscounts();
    chargeCustomer();
    sendConfirmation();
}
```

This tells a story. Each function name describes what happens at that level of abstraction. If you want to know *how* we validate the order, you read the `validateOrder()` function.

### The Single Responsibility Principle for Functions

**A function should do one thing. It should do it well. It should do it only.**

How do you know if a function does more than one thing? If you can extract another function from it with a name that is not merely a restatement of its implementation.

### One Level of Abstraction Per Function

This is one of the hardest rules to follow, but it's critical. Mixing levels of abstraction is confusing.

**Bad - Mixed Abstraction Levels:**
```java
public void renderPage() {
    StringBuilder html = new StringBuilder();
    html.append("<html>");  // Low level
    includeSetups();        // High level
    html.append("<body>");  // Low level
    renderBody();           // High level
}
```

**Good - Single Level:**
```java
public void renderPage() {
    renderOpeningTags();
    includeSetups();
    renderBody();
    renderClosingTags();
}
```

### The Stepdown Rule

Code should read like a top-down narrative. Every function should be followed by those at the next level of abstraction.

### Function Arguments

**The ideal number of arguments for a function is zero (niladic).** Next comes one (monadic), followed closely by two (dyadic). Three arguments (triadic) should be avoided where possible. More than three (polyadic) requires very special justification.

**Why?**
- Arguments are hard from a testing point of view
- Arguments require conceptual power to understand
- Output arguments are harder to understand than input arguments

### Have No Side Effects

Your function promises to do one thing, but it also does other hidden things. Sometimes it makes unexpected changes to variables, sometimes to the system state.

**Example from your code - `addBook()` has side effects:**
- Creates book in database
- Prints to console
- Sleeps the thread
- Shows interactive prompts

That's not adding a book - that's running an entire workflow.

### Command Query Separation

Functions should either **do something** or **answer something**, but not both.

**Bad:**
```java
public boolean set(String attribute, String value) {
    // Sets attribute AND returns success
}

if (set("username", "bob")) { ... }  // Confusing!
```

**Good:**
```java
public void setAttribute(String attribute, String value) {
    // Only sets
}

public boolean attributeExists(String attribute) {
    // Only queries
}

if (attributeExists("username")) {
    setAttribute("username", "bob");
}
```

### Prefer Exceptions to Returning Error Codes

Error codes lead to deeply nested structures. Exceptions allow you to separate error handling from the happy path.

---

## Your Code Analysis

Let's look at the actual violations in your Bibby codebase. I'm going to show you functions that violate Clean Code principles and teach you how to fix them.

### Violation #1: Function Way Too Long

**Location:** `BookCommands.java:89-169`

**Current Code:**
```java
@Command(command = "add", description = "Add a new book to your library database")
public void addBook() throws InterruptedException {
    ComponentFlow flow;
    ComponentFlow flow2;
    int authorCount;
    String title;
    String author;

    flow = componentFlowBuilder.clone()
            .withStringInput("title")
            .name("Book Title:_")
            .and()
            .withStringInput("author_count")
            .name("Number of Authors:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    authorCount = Integer.parseInt(result.getContext().get("author_count",String.class));

    title  = result.getContext().get("title", String.class);

    for(int i = 0; i < authorCount; i++){
        authorNameComponentFlow(title);
    }

    Thread.sleep(1000);

    System.out.println("\n\u001B[36m</>\033[0m: Ah, a brand-new book...");
    Thread.sleep(1750);
    System.out.printf("\u001B[36m</>\033[0m:'%s', right?",title);
    Thread.sleep(2350);
    System.out.println("\n\u001B[36m</>\033[0m: I'll handle adding it to the database and prepare it for the library.");
    Thread.sleep(3800);
    System.out.println("\n\u001B[36m</>\033[0m: Should I recommend where it belongs?\n");
    Thread.sleep(1000);

    flow = componentFlowBuilder.clone()
            .withSingleItemSelector("recommendShelf")
            .selectItems(yesNoOptions())
            .and().build();
    result = flow.run();

    if(result.getContext().get("recommendShelf",String.class).equalsIgnoreCase("yes")){
        Thread.sleep(2000);
        System.out.println(
                """
                \u001B[36m</>\033[0m: Recommended Shelf → \u001B[33mD-48\033[0m: Programming & Engineering.
                     Fits best near "\u001B[31mThe Pragmatic Programmer\033[0m" and "\u001B[31mRefactoring\033[0m".
                     These titles share themes of maintainable code and engineering craftsmanship.
                     Placing them together makes your shelf flow logically by topic.
                """
        );

        Thread.sleep(2000);
        System.out.println("\u001B[36m</>\033[0m:Shall I make it official and slide this one onto the shelf?\n");

        flow = componentFlowBuilder.clone()
                .withSingleItemSelector("recommendShelf")
                .selectItems(yesNoOptions())
                .and().build();
        result = flow.run();

        if(result.getContext().get("recommendShelf",String.class).equalsIgnoreCase("yes")){
            Thread.sleep(2000);
            System.out.println("\u001B[36m</>\033[0m: And there it is — " + "Shelf \u001B[33mD-48\033[0m" + ", freshly updated with another gem.\n");
        }else{
            Thread.sleep(2000);
            System.out.println("\u001B[36m</>\033[0m: No rush. Every book finds its home eventually.\n");
        }
    }else{
        Thread.sleep(2000);
        System.out.println("\u001B[36m</>\033[0m: Fair enough. We can pick another shelf anytime.\n");
    }
}
```

**Line Count:** 81 lines
**Responsibilities:** 7+ different things

**Why This Hurts:**

This function is doing:
1. Collecting book title input
2. Collecting author count
3. Looping through author collection
4. Displaying personality messages with delays
5. Asking about shelf recommendation
6. Showing shelf recommendation
7. Asking for confirmation
8. Showing final confirmation message

**You cannot understand this function at a glance.** You have to read every line to know what it does.

**How to Fix - Extract Till You Drop:**

```java
@Command(command = "add", description = "Add a new book to your library database")
public void addBook() throws InterruptedException {
    String bookTitle = collectBookTitle();
    int authorCount = collectAuthorCount();
    addAuthorsToBook(bookTitle, authorCount);

    displayBookAddedConfirmation(bookTitle);

    if (userWantsShelfRecommendation()) {
        offerShelfRecommendation(bookTitle);
    }
}

private String collectBookTitle() {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withStringInput("title")
            .name("Book Title:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("title", String.class);
}

private int collectAuthorCount() {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withStringInput("author_count")
            .name("Number of Authors:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return Integer.parseInt(result.getContext().get("author_count", String.class));
}

private void addAuthorsToBook(String bookTitle, int authorCount) {
    for(int i = 0; i < authorCount; i++){
        collectAuthorNameInteractively(bookTitle);
    }
}

private void displayBookAddedConfirmation(String bookTitle) throws InterruptedException {
    Thread.sleep(1000);
    System.out.println("\n\u001B[36m</>\033[0m: Ah, a brand-new book...");
    Thread.sleep(1750);
    System.out.printf("\u001B[36m</>\033[0m:'%s', right?", bookTitle);
    Thread.sleep(2350);
    System.out.println("\n\u001B[36m</>\033[0m: I'll handle adding it to the database and prepare it for the library.");
    Thread.sleep(3800);
}

private boolean userWantsShelfRecommendation() throws InterruptedException {
    System.out.println("\n\u001B[36m</>\033[0m: Should I recommend where it belongs?\n");
    Thread.sleep(1000);

    ComponentFlow flow = componentFlowBuilder.clone()
            .withSingleItemSelector("recommendShelf")
            .selectItems(yesNoOptions())
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("recommendShelf", String.class).equalsIgnoreCase("yes");
}

private void offerShelfRecommendation(String bookTitle) throws InterruptedException {
    displayRecommendation();

    if (userWantsToPlaceOnShelf()) {
        confirmShelfPlacement();
    } else {
        displayDeferredPlacement();
    }
}

private void displayRecommendation() throws InterruptedException {
    Thread.sleep(2000);
    System.out.println(
            """
            \u001B[36m</>\033[0m: Recommended Shelf → \u001B[33mD-48\033[0m: Programming & Engineering.
                 Fits best near "\u001B[31mThe Pragmatic Programmer\033[0m" and "\u001B[31mRefactoring\033[0m".
                 These titles share themes of maintainable code and engineering craftsmanship.
                 Placing them together makes your shelf flow logically by topic.
            """
    );
    Thread.sleep(2000);
    System.out.println("\u001B[36m</>\033[0m:Shall I make it official and slide this one onto the shelf?\n");
}

private boolean userWantsToPlaceOnShelf() {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withSingleItemSelector("recommendShelf")
            .selectItems(yesNoOptions())
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("recommendShelf", String.class).equalsIgnoreCase("yes");
}

private void confirmShelfPlacement() throws InterruptedException {
    Thread.sleep(2000);
    System.out.println("\u001B[36m</>\033[0m: And there it is — Shelf \u001B[33mD-48\033[0m, freshly updated with another gem.\n");
}

private void displayDeferredPlacement() throws InterruptedException {
    Thread.sleep(2000);
    System.out.println("\u001B[36m</>\033[0m: No rush. Every book finds its home eventually.\n");
}
```

**After Refactoring:**
- Main function: **6 lines** (was 81)
- Each extracted function: **4-12 lines**
- **Every function has a single responsibility**
- **Function names tell you exactly what happens**

Now when you read `addBook()`, you understand the entire flow in 5 seconds.

---

### Violation #2: Mixed Levels of Abstraction

**Location:** `BookCommands.java:458-529`

**Current Code:**
```java
@Command(command = "check-out", description = "Check-Out a book from the library")
public void checkOutBook(){
    ComponentFlow flow;
    flow = componentFlowBuilder.clone()
            .withStringInput("bookTitle" )
            .name("Book Title:")
            .and().build();
    ComponentFlow.ComponentFlowResult res = flow.run();

    String bookTitle = res.getContext().get("bookTitle");

    BookEntity bookEntity = bookService.findBookByTitle(bookTitle);
    String bookcaseLabel = "N.A";
    String bookshelfLabel ="N.A";
    if(bookEntity == null){
        System.out.println("Book Not Found.");
    }else if(bookEntity.getShelfId() != null){
            Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
            Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
            bookcaseLabel = bookcaseEntity.get().getBookcaseLabel();
            bookshelfLabel = shelfEntity.get().getShelfLabel();
    }if (bookEntity.getBookStatus().equals("CHECKED_OUT")){
        System.out.println(
                """

                \u001B[38;5;63m  .---.
                \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "This one's already off the shelf. No double-dipping on checkouts."
                \u001B[38;5;63m  \\|=|/

                """);


    }else{
        List<AuthorEntity> authors = bookService.findAuthorsByBookId(bookEntity.getBookId());
        System.out.println(String.format("""
                \n\u001B[32mConfirm Checkout\n\u001B[0m
                        \033[31mTitle\u001B[0m %s
                        \033[31mAuthor/s\u001B[0m %s

                        \033[31mStatus %s

                        \033[31mBookcase\u001B[0m %s
                        \033[31mShelf\u001B[0m %s
                """,bookEntity.getTitle(), authors, bookEntity.getBookStatus(), bookcaseLabel ,bookshelfLabel));
        flow = componentFlowBuilder.clone()
                .withStringInput("isConfirmed" )
                .name("y or n:_ ")
                .and().build();
        res = flow.run();

        if (res.getContext().get("isConfirmed").equals("y")){
            bookService.checkOutBook(bookEntity);
            System.out.println(
                    String.format("""

                    \u001B[38;5;63m  .---.
                    \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "All set — \u001B[38;5;42m %s \u001B[38;5;15m is checked out and ready to go with you. \033[36m"
                    \u001B[38;5;63m  \\|=|/

                    """,bookTitle));
        }else{
            System.out.println(
                    """

                    \u001B[38;5;63m  .---.
                    \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "Cool, I'll just… put this back by myself...and whisper *maybe next time* to the shelves... Again."
                    \u001B[38;5;63m  \\|=|/

                    """);
        }
    }
}
```

**Why This Hurts:**

This function mixes at least 4 different levels of abstraction:
1. **High level:** "check out a book"
2. **Medium level:** "find the book and its location"
3. **Low level:** "get shelf from optional, get bookcase from optional"
4. **Presentation level:** "format console output with ANSI codes"

Your brain has to context-switch constantly while reading this.

**How to Fix - One Level Per Function:**

```java
@Command(command = "check-out", description = "Check-Out a book from the library")
public void checkOutBook(){
    String bookTitle = promptForBookTitle();

    Optional<BookEntity> book = bookService.findBookByTitle(bookTitle);

    if (book.isEmpty()) {
        displayBookNotFound();
        return;
    }

    if (isAlreadyCheckedOut(book.get())) {
        displayAlreadyCheckedOut();
        return;
    }

    displayCheckoutConfirmation(book.get());

    if (userConfirmsCheckout()) {
        performCheckout(book.get());
        displayCheckoutSuccess(bookTitle);
    } else {
        displayCheckoutCancelled();
    }
}

private String promptForBookTitle() {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withStringInput("bookTitle")
            .name("Book Title:")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("bookTitle");
}

private boolean isAlreadyCheckedOut(BookEntity book) {
    return "CHECKED_OUT".equals(book.getBookStatus());
}

private void displayCheckoutConfirmation(BookEntity book) {
    BookLocationInfo location = getBookLocation(book);
    List<AuthorEntity> authors = bookService.findAuthorsByBookId(book.getBookId());

    System.out.println(formatCheckoutConfirmation(
        book.getTitle(),
        authors,
        book.getBookStatus(),
        location.bookcaseLabel(),
        location.shelfLabel()
    ));
}

private BookLocationInfo getBookLocation(BookEntity book) {
    if (book.getShelfId() == null) {
        return new BookLocationInfo("N.A", "N.A");
    }

    Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());
    Optional<BookcaseEntity> bookcase = bookcaseService.findBookCaseById(shelf.get().getBookcaseId());

    return new BookLocationInfo(
        bookcase.get().getBookcaseLabel(),
        shelf.get().getShelfLabel()
    );
}

private boolean userConfirmsCheckout() {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withStringInput("isConfirmed")
            .name("y or n:_ ")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return "y".equals(result.getContext().get("isConfirmed"));
}

private void performCheckout(BookEntity book) {
    bookService.checkOutBook(book);
}

// Helper record for book location
private record BookLocationInfo(String bookcaseLabel, String shelfLabel) {}
```

**Notice:**
- Main function now reads like a story
- Each step is at the same level of abstraction
- Low-level details are hidden in helper functions
- Display logic is separated

---

### Violation #3: Function Has Side Effects

**Location:** `BookService.java:43-54`

**Current Code:**
```java
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());  // SIDE EFFECT!
    }

    if(bookEntity.isEmpty()){
        return null;
    }
    return bookEntity.get();
}
```

**Why This Hurts:**

The function name says "find book by title". It promises to return a book. But it also **prints to the console** as a side effect.

Side effects create temporal coupling and order dependencies. They also make testing difficult—how do you test console output?

**How to Fix - Separate Query from Side Effect:**

```java
public Optional<BookEntity> findBookByExactTitle(String title){
    return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
}

public List<BookEntity> findBooksContainingTitle(String titleFragment){
    return bookRepository.findByTitleContaining(titleFragment);
}

// If you need to debug, do it in the calling code or use logging
public Optional<BookEntity> findBookByTitleWithLogging(String title){
    Optional<BookEntity> book = findBookByExactTitle(title);

    if (book.isEmpty()) {
        logger.debug("No exact match found for title: {}", title);
        List<BookEntity> partialMatches = findBooksContainingTitle(title);
        logger.debug("Found {} partial matches", partialMatches.size());
    }

    return book;
}
```

**Better Approach - Return Results, Let Caller Display:**

```java
// Service layer - no side effects
public SearchResult findBookByTitle(String title){
    Optional<BookEntity> exactMatch = findBookByExactTitle(title);
    List<BookEntity> partialMatches = findBooksContainingTitle(title);
    return new SearchResult(exactMatch, partialMatches);
}

// Command layer - handles display
public void searchByTitle() {
    String title = promptForTitle();
    SearchResult result = bookService.findBookByTitle(title);

    if (result.exactMatch().isPresent()) {
        displayBook(result.exactMatch().get());
    } else if (!result.partialMatches().isEmpty()) {
        displayPartialMatches(result.partialMatches());
    } else {
        displayNoResults();
    }
}
```

---

### Violation #4: Too Many Arguments

**Location:** `LoadingBar.java:5`

**Current Code:**
```java
public static void showProgressBar(String taskName, int totalSteps, int delayMs) throws InterruptedException {
```

**Why This Hurts:**

Three arguments is pushing the limit. Testing this requires creating test cases for all combinations of arguments.

**How to Fix - Use a Configuration Object:**

```java
public class ProgressBarConfig {
    private final String taskName;
    private final int totalSteps;
    private final int delayMs;

    public ProgressBarConfig(String taskName, int totalSteps, int delayMs) {
        this.taskName = taskName;
        this.totalSteps = totalSteps;
        this.delayMs = delayMs;
    }

    public static ProgressBarConfig standard(String taskName) {
        return new ProgressBarConfig(taskName, 40, 150);
    }

    public static ProgressBarConfig fast(String taskName) {
        return new ProgressBarConfig(taskName, 20, 50);
    }

    // getters...
}

// Usage
public static void showProgressBar(ProgressBarConfig config) throws InterruptedException {
    System.out.println(config.getTaskName());
    for (int i = 0; i <= config.getTotalSteps(); i++) {
        int percent = (i * 100) / config.getTotalSteps();
        String bar = "\uD83D\uDFE9".repeat(i) + " ".repeat(config.getTotalSteps() - i);
        System.out.print("\r[" + bar + "] " + percent + "%");
        Thread.sleep(config.getDelayMs());
    }
    System.out.println("\n✅ Done!");
}

// Calling code is much cleaner
ProgressBarUtils.showProgressBar(ProgressBarConfig.standard("Loading books"));
```

---

### Violation #5: Function Doesn't Match Its Name

**Location:** `BookCommands.java:214-254`

**Current Code:**
```java
@Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
public void addToShelf(){
    ComponentFlow flow;
    flow = componentFlowBuilder.clone()
            .withStringInput("bookTitle")
            .name("What book are you shelving?:_")
            .and()
            .withSingleItemSelector("bookcase")
            .name("Choose a Bookcase:_")
            .selectItems(bookCaseOptions())
            .and().build();
    ComponentFlow.ComponentFlowResult res = flow.run();
    String title = res.getContext().get("bookTitle",String.class);
    Long bookCaseId = Long.parseLong(res.getContext().get("bookcase",String.class));
    System.out.println("BOOK CASE ID: " + bookCaseId);


    flow = componentFlowBuilder.clone()
            .withSingleItemSelector("bookshelf")
            .name("Chose a shelf position")
            .selectItems(bookShelfOptions(bookCaseId))
            .and().build();


    res = flow.run();




    BookEntity bookEnt = bookService.findBookByTitle(title);
    if(bookEnt == null){
        System.out.println("Book Not Found In Library");
    }else {
        Long shelfId = Long.parseLong(res.getContext().get("bookshelf",String.class));
        System.out.println(shelfId);
        System.out.println(title);
        bookEnt.setShelfId(shelfId);
        bookService.updateBook(bookEnt);
        System.out.println("Added Book To the Shelf!");
    }
}
```

**Why This Hurts:**

The function name is `addToShelf` but:
1. It prompts for book title
2. It prompts for bookcase selection
3. It prompts for shelf selection
4. It finds the book
5. It updates the book's shelf
6. It has debug print statements

This is an entire workflow, not "adding to shelf."

**How to Fix - Name It What It Does:**

```java
@Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
public void assignBookToShelf(){
    String bookTitle = promptForBookTitle();

    Optional<BookEntity> book = bookService.findBookByTitle(bookTitle);
    if (book.isEmpty()) {
        displayBookNotFound();
        return;
    }

    Long bookcaseId = promptForBookcaseSelection();
    Long shelfId = promptForShelfSelection(bookcaseId);

    assignShelf(book.get(), shelfId);
    displayShelfAssignmentSuccess(bookTitle, shelfId);
}

private void assignShelf(BookEntity book, Long shelfId) {
    book.setShelfId(shelfId);
    bookService.updateBook(book);
}
```

---

### Violation #6: Dead/Unused Function

**Location:** `BookCommands.java:256-259`

**Current Code:**
```java
public void searchByAuthorVoice(){
    List<String> searchResponses = new ArrayList<>();
}
```

**Why This Hurts:**

This function creates a list and does nothing with it. It's dead code. Dead code confuses readers—they wonder if it's supposed to do something or if it's a work-in-progress.

**How to Fix:**

Delete it. If you need it later, it's in Git history.

---

### Violation #7: Debug Code in Production

**Location:** `BookCommands.java:227, 246, 247, 434`

**Current Code:**
```java
System.out.println("BOOK CASE ID: " + bookCaseId);
System.out.println(shelfId);
System.out.println(title);
System.out.println(s.getBookcaseId());
```

**Why This Hurts:**

Debug print statements in production code are unprofessional. They clutter the output and make the application look unfinished.

**How to Fix - Use Proper Logging:**

```java
logger.debug("Selected bookcase ID: {}", bookcaseId);
logger.debug("Assigning book '{}' to shelf ID: {}", title, shelfId);
```

Or remove them entirely if they were just for development.

---

### Violation #8: Violation of Command-Query Separation

**Location:** `BookService.java:56-62`

**Current Code:**
```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**This one is actually not bad!** It's a command (changes state, returns void). Good job.

But there's room for improvement:

**Better Version:**
```java
public void checkOutBook(BookEntity book){
    if(isAlreadyCheckedOut(book)){
        throw new BookAlreadyCheckedOutException("Book is already checked out");
    }

    book.setBookStatus(BookStatus.CHECKED_OUT.toString());
    book.setCheckoutCount(book.getCheckoutCount() + 1);  // Increment checkout count!
    bookRepository.save(book);
}

private boolean isAlreadyCheckedOut(BookEntity book) {
    return BookStatus.CHECKED_OUT.toString().equals(book.getBookStatus());
}
```

---

## Deep Dive Refactoring Examples

Let's take your three worst functions and completely transform them with detailed commentary.

### Deep Dive #1: The `addBook()` Transformation

**Before:** 81 lines, 7 responsibilities
**After:** 6 lines main function + 10 focused helpers

I showed this earlier, but let's talk about the *thought process*.

**Step 1: Read the function and list what it does**
1. Collect book title
2. Collect author count
3. Collect author names (loop)
4. Display confirmation messages
5. Ask about recommendations
6. Show recommendation
7. Ask for confirmation
8. Show final message

**Step 2: Group related actions**
- Input collection: 1, 2, 3
- User feedback: 4, 8
- Recommendation workflow: 5, 6, 7

**Step 3: Extract groups into functions**

Each group becomes a function. Then each function's implementation gets extracted further if needed.

**Step 4: Name functions by intent, not implementation**

- ❌ `getInputFromUser()`
- ✅ `collectBookTitle()`

**Step 5: Ensure each function is at one level of abstraction**

The main function should read like a table of contents.

---

### Deep Dive #2: The `searchByAuthor()` Transformation

**Location:** `BookCommands.java:261-303`

**Before:**
```java
public void searchByAuthor() throws InterruptedException {
    System.out.println("\n\u001B[95mSearch by Author");

    ComponentFlow componentFlow;
    String authorFirstName;
    String authorLastName;
    componentFlow = componentFlowBuilder.clone()
            .withStringInput("authorFirstName")
            .name("Enter author First Name:_ ")
            .and()
            .withStringInput("authorLastName")
            .name("Enter author Last Name:_")
            .and().build();

    ComponentFlow.ComponentFlowResult res = componentFlow.run();
    authorFirstName =res.getContext().get("authorFirstName",String.class);
    authorLastName =res.getContext().get("authorLastName",String.class);

    Thread.sleep(1000);
    System.out.println("\n\u001B[36m</>\u001B[0m: Ah, the works of " + authorFirstName + " " + authorLastName + " — a fine choice. Let me check the shelves...");
    Thread.sleep(4000);
    showLoading();

    System.out.println("\n\u001B[36m</>\u001B[0m: Found 2 titles — both are sitting on their shelves, available.");
    Thread.sleep(2000);

    System.out.println("""
            ──────────────────────────────────────────────
            [12] \u001B[33mMy Life Decoded: The Story of Leo\u001B[0m   \n[Shelf A1] (AVAILABLE)

            [29] \u001B[33mThe Answer is 42 \u001B[0m   \n[Shelf B2] (AVAILABLE)
            """);
    System.out.println("\u001B[90m───────────────────────────────────────────────\u001B[0m");

    Thread.sleep(500);

    askBookCheckOut();
}
```

**Issues:**
1. Hardcoded search results (not actually searching!)
2. Mixed abstraction levels
3. Side effects everywhere
4. Thread.sleep calls scattered throughout

**After - Properly Refactored:**

```java
public void searchByAuthor() throws InterruptedException {
    displaySearchHeader();

    AuthorName author = promptForAuthorName();

    displaySearchingMessage(author);
    showSearchingProgress();

    List<BookEntity> books = bookService.findBooksByAuthor(author.firstName(), author.lastName());

    displaySearchResults(books);

    offerCheckout(books);
}

private void displaySearchHeader() {
    System.out.println("\n\u001B[95mSearch by Author\u001B[0m");
}

private AuthorName promptForAuthorName() {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withStringInput("authorFirstName")
            .name("Enter author First Name:_ ")
            .and()
            .withStringInput("authorLastName")
            .name("Enter author Last Name:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();

    String firstName = result.getContext().get("authorFirstName", String.class);
    String lastName = result.getContext().get("authorLastName", String.class);

    return new AuthorName(firstName, lastName);
}

private void displaySearchingMessage(AuthorName author) throws InterruptedException {
    Thread.sleep(1000);
    System.out.printf("\n\u001B[36m</>\u001B[0m: Ah, the works of %s %s — a fine choice. Let me check the shelves...\n",
            author.firstName(), author.lastName());
    Thread.sleep(4000);
}

private void showSearchingProgress() throws InterruptedException {
    LoadingBar.showProgressBar("Searching library...", 40, 150);
}

private void displaySearchResults(List<BookEntity> books) throws InterruptedException {
    if (books.isEmpty()) {
        displayNoResults();
        return;
    }

    System.out.printf("\n\u001B[36m</>\u001B[0m: Found %d title(s) — all available.\n", books.size());
    Thread.sleep(2000);

    System.out.println("──────────────────────────────────────────────");
    for (BookEntity book : books) {
        displayBookSummary(book);
    }
    System.out.println("\u001B[90m───────────────────────────────────────────────\u001B[0m");
    Thread.sleep(500);
}

private void displayBookSummary(BookEntity book) {
    String shelfInfo = getShelfInfo(book);
    System.out.printf("[%d] \u001B[33m%s\u001B[0m\n%s (%s)\n\n",
            book.getBookId(),
            book.getTitle(),
            shelfInfo,
            book.getBookStatus());
}

private String getShelfInfo(BookEntity book) {
    if (book.getShelfId() == null) {
        return "[No shelf assigned]";
    }

    Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());
    return shelf.map(s -> "[Shelf " + s.getShelfLabel() + "]")
                .orElse("[Unknown shelf]");
}

private void offerCheckout(List<BookEntity> books) throws InterruptedException {
    if (!books.isEmpty()) {
        askBookCheckOut();
    }
}

// Helper record
private record AuthorName(String firstName, String lastName) {}
```

**Key Improvements:**
1. **Actually searches** instead of showing hardcoded results
2. Each function does one thing
3. Easy to test each piece independently
4. Reads like a story

---

### Deep Dive #3: The `bookcaseRowFormater()` Fix

**Location:** `BookcaseCommands.java:40-42`

**Before:**
```java
public String bookcaseRowFormater(BookcaseEntity bookcaseEntity, int bookCount){
    return String.format(" %-12s \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mShelves    \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mBooks ",
        bookcaseEntity.getBookcaseLabel().toUpperCase(),
        bookcaseEntity.getShelfCapacity(),
        bookCount);
}
```

**Issues:**
1. Typo in name
2. Method does formatting but name suggests it's a noun
3. Mixing business logic (toUpperCase) with presentation
4. ANSI codes are magic strings

**After:**
```java
private static final String ANSI_BOLD = "\u001B[1m";
private static final String ANSI_RESET_BOLD = "\u001B[22m";
private static final String ANSI_COLOR_PRIMARY = "\u001B[38;5;63m";
private static final String ANSI_COLOR_TEXT = "\u001B[38;5;15m";

public String formatBookcaseRow(BookcaseEntity bookcase, int bookCount){
    String label = formatBookcaseLabel(bookcase);
    int shelfCount = bookcase.getShelfCapacity();

    return String.format(" %-12s %s%s%-2d%s%sShelves    %s%s%-2d%s%sBooks ",
        label,
        ANSI_BOLD, ANSI_COLOR_PRIMARY, shelfCount, ANSI_RESET_BOLD, ANSI_COLOR_TEXT,
        ANSI_BOLD, ANSI_COLOR_PRIMARY, bookCount, ANSI_RESET_BOLD, ANSI_COLOR_TEXT);
}

private String formatBookcaseLabel(BookcaseEntity bookcase) {
    return bookcase.getBookcaseLabel().toUpperCase();
}
```

Even better - create a formatting utility:

```java
public class ConsoleFormatter {
    private static final String BOLD = "\u001B[1m";
    private static final String RESET_BOLD = "\u001B[22m";
    private static final String COLOR_PRIMARY = "\u001B[38;5;63m";

    public static String emphasize(Object value) {
        return BOLD + COLOR_PRIMARY + value + RESET_BOLD;
    }
}

// Usage
public String formatBookcaseRow(BookcaseEntity bookcase, int bookCount){
    return String.format(" %-12s %s Shelves    %s Books",
        bookcase.getBookcaseLabel().toUpperCase(),
        ConsoleFormatter.emphasize(bookcase.getShelfCapacity()),
        ConsoleFormatter.emphasize(bookCount));
}
```

---

## Patterns You Should Recognize

### Pattern #1: The Stepdown Anti-Pattern

Your code often does this:

```java
public void mainFunction() {
    // high level
    doStep1();

    // low level detail
    String result = someVariable.getValue().toString().trim();

    // high level
    doStep2();
}
```

This is jarring. All functions in a file should be organized by level of abstraction, with higher-level functions at the top.

**Fix:** Extract the low-level detail:

```java
public void mainFunction() {
    doStep1();
    String result = extractCleanValue();
    doStep2();
}

private String extractCleanValue() {
    return someVariable.getValue().toString().trim();
}
```

### Pattern #2: The Long Method Trap

You're falling into this pattern:
1. Start writing a function
2. Need to add one more thing
3. Need to handle another case
4. Before you know it, 80 lines

**Fix:** Set a line limit alarm in your head. When you hit line 15, ask yourself: "Can I extract something?"

### Pattern #3: The "And" in Function Names

If your function name has "and" in it, it's doing two things:

- `validateAndSave()`
- `findAndDisplay()`
- `collectAndProcess()`

**Fix:** Split them.

### Pattern #4: Console Output Scattered Everywhere

Your service layer, command layer, and even utilities print to console. This makes testing impossible.

**Rule:**
- **Service layer:** No console output, only return values
- **Command layer:** Handle all user interaction
- **Utilities:** No I/O at all

---

## Action Items

### 🔴 High Priority (This Week)

#### 1. **Refactor `addBook()` Method**
**File:** `BookCommands.java:89-169`
- [ ] Extract to at least 6 smaller methods as shown in Deep Dive #1
- [ ] Ensure main method is under 10 lines
- [ ] Test after each extraction
- **Estimated time:** 2 hours
- **Impact:** Massive readability improvement, template for other refactorings

#### 2. **Remove Debug Print Statements**
**Files:** `BookCommands.java`, `BookcaseCommands.java`, `BookService.java`
- [ ] Remove or replace with logger.debug() all System.out.println for debugging
- [ ] Keep intentional user-facing messages
- **Estimated time:** 20 minutes
- **Impact:** Professional appearance

#### 3. **Delete Dead Code**
**File:** `BookCommands.java:256-259`
- [ ] Delete `searchByAuthorVoice()` method
- **Estimated time:** 2 minutes
- **Impact:** Code cleanliness

#### 4. **Fix `checkOutBook()` Method**
**File:** `BookCommands.java:458-529`
- [ ] Extract into smaller functions as shown in Violation #2
- [ ] Separate business logic from presentation
- **Estimated time:** 1.5 hours
- **Impact:** Major readability improvement

#### 5. **Remove Side Effects from Service Layer**
**File:** `BookService.java:43-54`
- [ ] Remove System.out.println from `findBookByTitle()`
- [ ] Split into two methods: `findBookByExactTitle()` and `findBooksContainingTitle()`
- [ ] Return Optional<BookEntity> instead of nullable
- **Estimated time:** 30 minutes
- **Impact:** Testability, clean architecture

---

### 🟡 Medium Priority (Next 2 Weeks)

#### 6. **Refactor `searchByAuthor()`**
**File:** `BookCommands.java:261-303`
- [ ] Make it actually search instead of showing hardcoded results
- [ ] Extract display logic as shown in Deep Dive #2
- **Estimated time:** 2 hours
- **Impact:** Functionality + clean code

#### 7. **Refactor `addToShelf()`**
**File:** `BookCommands.java:214-254`
- [ ] Extract prompting logic
- [ ] Separate business logic (update) from UI
- [ ] Rename to `assignBookToShelf()`
- **Estimated time:** 1 hour
- **Impact:** Clarity

#### 8. **Introduce Logging**
**All service files**
- [ ] Add SLF4J dependency if not present
- [ ] Replace debug prints with logger.debug()
- [ ] Add logger.info() for significant operations
- **Estimated time:** 1 hour
- **Impact:** Professional logging

---

### 🟢 Low Priority (Nice to Have)

#### 9. **Extract ConsoleFormatter Utility**
- [ ] Create utility class for ANSI color codes
- [ ] Replace magic strings with named constants
- [ ] Centralize console formatting
- **Estimated time:** 1 hour
- **Impact:** Maintainability

#### 10. **Reduce Function Arguments**
**File:** `LoadingBar.java`
- [ ] Introduce ProgressBarConfig as shown
- [ ] Add factory methods for common configurations
- **Estimated time:** 45 minutes
- **Impact:** API cleanliness

---

## Function Writing Checklist

Use this before committing any new function:

### Size & Responsibility
- [ ] Function is under 20 lines (preferably under 10)
- [ ] Function does ONE thing
- [ ] Function has one level of abstraction
- [ ] Can't extract another function with a name that isn't just restating implementation

### Naming
- [ ] Name starts with a verb
- [ ] Name describes WHAT it does, not HOW
- [ ] Name is at the right level of abstraction
- [ ] No "and", "or" in the name

### Arguments
- [ ] 0-2 arguments (3 max)
- [ ] No boolean arguments (split into two functions instead)
- [ ] No output arguments (use return value)
- [ ] Arguments are at same level of abstraction

### Side Effects
- [ ] No hidden side effects
- [ ] Doesn't modify unexpected state
- [ ] Doesn't do more than the name promises

### Error Handling
- [ ] Uses exceptions, not error codes
- [ ] Doesn't return null (returns Optional instead)
- [ ] Try/catch blocks are extracted to their own function

### Testing
- [ ] Can be tested easily
- [ ] No direct I/O (console, file) unless that's its purpose
- [ ] Testable without complex setup

---

## Key Takeaways

### What You're Doing Right

1. **Good instincts on helper methods:** You've already started extracting helpers like `yesNoOptions()`, `bookCaseOptions()`
2. **Descriptive names:** Most of your function names describe what they do
3. **Use of service layer:** You're delegating to services, which is correct architecture

### What Needs Work

1. **Function length:** Many functions are 3-4x longer than they should be
2. **Mixed abstraction levels:** High and low-level operations in the same function
3. **Side effects:** Service layer has console output; functions do more than they promise
4. **Debug code:** Production code has debug print statements

### The Big Lesson

**Extract till you drop.**

When in doubt, extract. You will almost never regret making a function smaller. You will frequently regret making it larger.

The goal is not to write less code. The goal is to make the code **easier to read**.

A 10-line function split into 5 two-line functions with good names is more readable than the original, even though it's more total lines.

**Readability > Brevity**

---

## Further Study

### From Clean Code (Chapter 3)
- Re-read pages 31-52
- Study the `SetupTeardownIncluder.java` example (page 46)
- Pay attention to the "prefer exceptions to error codes" section

### Refactoring Catalog
- Martin Fowler's "Refactoring" book, specifically:
  - Extract Method (most important refactoring)
  - Replace Temp with Query
  - Introduce Parameter Object
  - Decompose Conditional

### Practice Katas
- "Gilded Rose Kata" - practice extracting functions from a mess
- "Tennis Refactoring Kata" - practice small functions
- "Trip Service Kata" - practice separating concerns

### Video Resources
- Sandi Metz: "All the Little Things" (RailsConf 2014)
- Uncle Bob: "Clean Code - Functions" (Clean Coders series)

---

## Final Thoughts from Your Mentor

You've built complex workflows with Spring Shell's ComponentFlow, which is impressive. The interactive flows you've created show creativity and user-centric thinking.

Now it's time to bring that same care to the internal structure of your code.

Your functions are like paragraphs in a book. Right now, some of your paragraphs are a full page long. Readers get lost. They forget what the paragraph was about by the time they reach the end.

Break them up. Make each paragraph (function) express one idea clearly. Then arrange the paragraphs so they tell a story from high level to low level.

Start with `addBook()`. It's your worst offender, which makes it your best learning opportunity. Refactor it using the example I provided. Feel the difference in readability.

Then tackle the others.

Remember: **This is not about being clever. This is about being kind to the next person who reads your code.**

And that next person is you, six months from now.

---

**End of Section 2: Functions**
