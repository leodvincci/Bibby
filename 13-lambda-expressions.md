# Section 13: Lambda Expressions & Functional Interfaces

**Estimated Time:** 55 minutes
**Prerequisites:** Sections 1-12
**Complexity:** ⭐⭐⭐⭐ (Intermediate-Advanced)

---

## Learning Objectives

By the end of this section, you will:

1. ✅ Understand lambda syntax and when to use it
2. ✅ Master functional interfaces (Predicate, Function, Consumer, Supplier)
3. ✅ Use method references (`::` operator)
4. ✅ Know when NOT to use lambdas (readability matters!)
5. ✅ Refactor loops to lambdas (where appropriate)
6. ✅ Replace anonymous classes with lambdas
7. ✅ Chain Optional methods with lambdas

---

## The Surprising Discovery: Zero Lambdas in Bibby

**Your entire Bibby codebase has:**
- ❌ **ZERO lambda expressions**
- ❌ **ZERO streams**
- ❌ **ZERO method references**
- ❌ **ZERO functional interfaces** (beyond what Spring provides)

**Is this a problem?**

**NO! Traditional loops are often more readable.**

But there ARE places where lambdas would improve your code. Let's learn when and how to use them.

---

## What Are Lambda Expressions?

**Lambda expressions are anonymous functions** - functions without a name.

### Traditional Approach (Pre-Java 8)

```java
// Before lambdas: Anonymous class
List<BookEntity> books = bookRepository.findAll();
books.sort(new Comparator<BookEntity>() {
    @Override
    public int compare(BookEntity b1, BookEntity b2) {
        return b1.getTitle().compareTo(b2.getTitle());
    }
});
```

**That's 5 lines of boilerplate for a simple comparison!**

### Lambda Approach (Java 8+)

```java
// With lambda: One line!
List<BookEntity> books = bookRepository.findAll();
books.sort((b1, b2) -> b1.getTitle().compareTo(b2.getTitle()));
//         ↑ Parameters  ↑ Arrow  ↑ Expression
```

**Much cleaner!**

---

## Lambda Syntax

### Basic Form

```java
(parameters) -> expression

// Examples:
() -> 42                          // No parameters
x -> x * 2                        // One parameter (parentheses optional)
(x, y) -> x + y                   // Multiple parameters
(String s) -> s.length()          // With type declaration
(x, y) -> { return x + y; }       // With code block and return
```

### Anatomy of a Lambda

```java
(BookEntity book) -> book.getTitle()
 ↑                ↑  ↑
 Parameters      Arrow  Body (expression)

// Breakdown:
// Parameters: Input to the function
// Arrow (->): Separates parameters from body
// Body: The code to execute
```

### Single vs Multi-Statement Bodies

```java
// Single expression (no braces, implicit return):
x -> x * 2

// Multiple statements (requires braces and explicit return):
x -> {
    int result = x * 2;
    System.out.println("Doubled: " + result);
    return result;
}
```

---

## Functional Interfaces

**A functional interface has EXACTLY ONE abstract method.**

### Built-in Functional Interfaces

Java provides common functional interfaces in `java.util.function`:

| Interface | Method | Use Case | Example |
|-----------|--------|----------|---------|
| `Predicate<T>` | `boolean test(T t)` | Test condition | `book -> book.getStatus().equals("AVAILABLE")` |
| `Function<T, R>` | `R apply(T t)` | Transform input | `book -> book.getTitle()` |
| `Consumer<T>` | `void accept(T t)` | Perform action | `book -> System.out.println(book.getTitle())` |
| `Supplier<T>` | `T get()` | Supply value | `() -> new BookEntity()` |
| `BiFunction<T, U, R>` | `R apply(T t, U u)` | Two inputs | `(a, b) -> a + b` |
| `BiPredicate<T, U>` | `boolean test(T t, U u)` | Test two inputs | `(book, status) -> book.getStatus().equals(status)` |

### Examples with Bibby Domain

```java
// Predicate: Test if book is available
Predicate<BookEntity> isAvailable = book ->
    book.getBookStatus().equals("AVAILABLE");

if (isAvailable.test(myBook)) {
    System.out.println("Book is available!");
}

// Function: Extract title from book
Function<BookEntity, String> getTitle = book -> book.getTitle();
String title = getTitle.apply(myBook);

// Consumer: Print book title
Consumer<BookEntity> printTitle = book ->
    System.out.println(book.getTitle());
printTitle.accept(myBook);

// Supplier: Create new book
Supplier<BookEntity> newBook = () -> new BookEntity();
BookEntity book = newBook.get();
```

---

## Refactoring Opportunity #1: Nested Loops

### Current Code - BookcaseCommands.java:63-73

```java
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();

    for (BookcaseEntity b : bookcaseEntities) {  // ← Traditional loop
        int shelfBookCount = 0;
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());

        for(ShelfEntity s : shelves){  // ← Nested loop
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
            shelfBookCount += bookList.size();
        }

        options.put(bookcaseRowFormater(b, shelfBookCount), b.getBookcaseId().toString());
    }
    return options;
}
```

### Refactored with Streams and Lambdas

```java
private Map<String, String> bookCaseOptions() {
    return bookcaseService.getAllBookcases().stream()
        .collect(Collectors.toMap(
            bookcase -> {
                int bookCount = shelfService.findByBookcaseId(bookcase.getBookcaseId()).stream()
                    .mapToInt(shelf -> bookService.findBooksByShelf(shelf.getShelfId()).size())
                    .sum();
                return bookcaseRowFormater(bookcase, bookCount);
            },
            bookcase -> bookcase.getBookcaseId().toString(),
            (existing, replacement) -> existing,  // Merge function (in case of duplicates)
            LinkedHashMap::new  // Preserve insertion order
        ));
}
```

**Is this better?**

**Debatable!** The lambda version is more concise but arguably less readable. Let's compare:

| Aspect | Traditional Loop | Lambda/Stream |
|--------|------------------|---------------|
| **Readability** | ✅ Clear step-by-step logic | ❌ More cognitive load |
| **Lines of code** | ❌ More verbose (11 lines) | ✅ More concise (9 lines) |
| **Debugging** | ✅ Easy to step through | ❌ Harder to debug |
| **Performance** | ✅ Slightly faster | ❌ Slight overhead |
| **Modern** | ❌ "Old-fashioned" | ✅ "Modern" Java style |

**Recommendation: Keep the traditional loop for this case.** The readability is more important than being "modern."

---

## Refactoring Opportunity #2: forEach() Loop

### Current Code - BookcaseService.java:35-37

```java
for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
    addShelf(bookcaseEntity, i, i);
}
```

### Could Refactor To:

```java
IntStream.range(0, bookcaseEntity.getShelfCapacity())
    .forEach(i -> addShelf(bookcaseEntity, i, i));
```

**Is this better?**

**NO!** The traditional for-loop is clearer here. The lambda doesn't add value.

**When forEach() IS better:**

```java
// If you already HAVE a collection:
List<BookEntity> books = bookRepository.findAll();

// Traditional:
for (BookEntity book : books) {
    System.out.println(book.getTitle());
}

// Lambda (equally readable):
books.forEach(book -> System.out.println(book.getTitle()));

// Method reference (even cleaner):
books.forEach(System.out::println);
```

---

## Method References: The `::` Operator

**Method references are shorthand for lambdas that just call a method.**

### Syntax

```java
ClassName::methodName
```

### Types of Method References

#### 1. Static Method Reference

```java
// Lambda:
list.forEach(s -> System.out.println(s));

// Method reference:
list.forEach(System.out::println);  // ← Cleaner!
```

#### 2. Instance Method Reference (on specific object)

```java
BookService bookService = new BookService();

// Lambda:
books.forEach(book -> bookService.saveBook(book));

// Method reference:
books.forEach(bookService::saveBook);  // ← Cleaner!
```

#### 3. Instance Method Reference (on parameter)

```java
// Lambda:
books.stream()
    .map(book -> book.getTitle())
    .collect(Collectors.toList());

// Method reference:
books.stream()
    .map(BookEntity::getTitle)  // ← Cleaner!
    .collect(Collectors.toList());
```

#### 4. Constructor Reference

```java
// Lambda:
Supplier<BookEntity> supplier = () -> new BookEntity();

// Constructor reference:
Supplier<BookEntity> supplier = BookEntity::new;  // ← Cleaner!
```

### Example Refactoring in Bibby

**If you had this code:**

```java
List<BookEntity> books = bookRepository.findAll();
for (BookEntity book : books) {
    System.out.println(book);
}
```

**You could refactor to:**

```java
bookRepository.findAll().forEach(System.out::println);
```

**Method reference benefits:**
- ✅ More concise
- ✅ Clearer intent (just printing each book)
- ✅ No temporary variables

---

## Optional and Lambdas: A Perfect Match

**Your code has many Optional.get() calls that could be safer with lambdas.**

### Current Code - BookService.java:50-53

```java
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));

    if(bookEntity.isEmpty()){
        return null;  // ❌ Defeats purpose of Optional
    }
    return bookEntity.get();
}
```

### Refactored with Lambda

```java
public BookEntity findBookByTitle(String title){
    return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title))
        .orElse(null);  // ✅ More concise (though still returns null)
}

// OR better:
public Optional<BookEntity> findBookByTitle(String title){
    return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    // ✅ Returns Optional - caller decides how to handle empty
}

// OR with custom exception:
public BookEntity findBookByTitle(String title){
    return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title))
        .orElseThrow(() -> new BookNotFoundException(title));
        // ↑ Lambda creates exception only if needed
}
```

### Optional Method Chaining with Lambdas

```java
// Current unsafe code (BookCommands.java:362):
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
//                                                                                    ↑ Unsafe!

// Refactored with lambdas (safe):
Optional<BookcaseEntity> bookcaseEntity = shelfService.findShelfById(bookEntity.getShelfId())
    .flatMap(shelf -> bookcaseService.findBookCaseById(shelf.getBookcaseId()));
    // ↑ flatMap only calls lambda if shelfEntity is present
```

**Breakdown:**
- `.flatMap(shelf -> ...)` - Only executes if Optional has a value
- `shelf ->` - Lambda parameter (the ShelfEntity)
- `bookcaseService.findBookCaseById(...)` - Returns `Optional<BookcaseEntity>`
- `flatMap` "flattens" `Optional<Optional<BookcaseEntity>>` to `Optional<BookcaseEntity>`

### Optional Chaining Examples

```java
// Transform value if present:
bookRepository.findById(id)
    .map(BookEntity::getTitle)  // Extract title
    .map(String::toUpperCase)   // Transform to uppercase
    .orElse("UNKNOWN");         // Default if not found

// Filter based on condition:
bookRepository.findById(id)
    .filter(book -> book.getBookStatus().equals("AVAILABLE"))
    .ifPresent(book -> checkOutBook(book));
    // ↑ Only checks out if book is available

// Perform action if present (Consumer):
bookRepository.findById(id)
    .ifPresent(book -> System.out.println("Found: " + book.getTitle()));
    // ↑ Safer than: if (!opt.isEmpty()) { ... opt.get() ... }

// Throw if absent (Supplier):
BookEntity book = bookRepository.findById(id)
    .orElseThrow(() -> new BookNotFoundException(id));
    // ↑ Lambda creates exception only if needed (lazy evaluation)
```

---

## When to Use Lambdas

### ✅ Use Lambdas When:

1. **Passing behavior as a parameter**

```java
// Sorting with custom comparator:
books.sort((b1, b2) -> b1.getTitle().compareTo(b2.getTitle()));
```

2. **Simplifying Optional handling**

```java
bookRepository.findById(id)
    .orElseThrow(() -> new BookNotFoundException(id));
```

3. **Simple forEach operations**

```java
books.forEach(book -> log.info("Processing: {}", book.getTitle()));
```

4. **Replacing single-method anonymous classes**

```java
// BEFORE:
button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Clicked!");
    }
});

// AFTER:
button.addActionListener(e -> System.out.println("Clicked!"));
```

5. **Stream operations** (Section 14!)

```java
books.stream()
    .filter(book -> book.getBookStatus().equals("AVAILABLE"))
    .map(BookEntity::getTitle)
    .collect(Collectors.toList());
```

### ❌ DON'T Use Lambdas When:

1. **Traditional loops are clearer**

```java
// Traditional loop is fine:
for (BookEntity book : books) {
    book.setStatus("ARCHIVED");
    book.setUpdatedAt(LocalDate.now());
    bookRepository.save(book);
}

// Lambda doesn't add value:
books.forEach(book -> {
    book.setStatus("ARCHIVED");
    book.setUpdatedAt(LocalDate.now());
    bookRepository.save(book);
});
```

2. **Complex nested logic**

```java
// Keep traditional loops for complex logic:
for (BookcaseEntity bookcase : bookcases) {
    for (ShelfEntity shelf : bookcase.getShelves()) {
        for (BookEntity book : shelf.getBooks()) {
            // Complex logic here...
        }
    }
}
```

3. **You need the loop index**

```java
// Traditional for-loop is clearer:
for (int i = 0; i < capacity; i++) {
    addShelf(bookcase, i, i);  // Index is used
}
```

4. **Early termination needed**

```java
// Need to break early - traditional loop is better:
for (BookEntity book : books) {
    if (book.getIsbn().equals(targetIsbn)) {
        return book;  // ✅ Can return early
    }
}

// Lambda can't break/return from enclosing method:
books.forEach(book -> {
    if (book.getIsbn().equals(targetIsbn)) {
        return;  // ❌ Only returns from lambda, not method!
    }
});
```

---

## Creating Your Own Functional Interfaces

**You can create custom functional interfaces for your domain.**

### Example: BookValidator

```java
@FunctionalInterface
public interface BookValidator {
    boolean validate(BookEntity book);

    // Can have default methods:
    default BookValidator and(BookValidator other) {
        return book -> this.validate(book) && other.validate(book);
    }
}
```

**Usage:**

```java
// Define validators:
BookValidator hasIsbn = book -> book.getIsbn() != null && !book.getIsbn().isEmpty();
BookValidator hasTitle = book -> book.getTitle() != null && !book.getTitle().isEmpty();
BookValidator isAvailable = book -> book.getBookStatus().equals("AVAILABLE");

// Combine validators:
BookValidator fullValidator = hasIsbn.and(hasTitle);

// Use:
if (fullValidator.validate(myBook)) {
    System.out.println("Book is valid!");
}
```

### Example: EntityMapper

```java
@FunctionalInterface
public interface EntityMapper<E, D> {
    D toDTO(E entity);
}

// Implementation with lambda:
EntityMapper<BookEntity, BookSummary> toSummary =
    book -> new BookSummary(book.getBookId(), book.getTitle());

// Usage:
BookSummary summary = toSummary.toDTO(bookEntity);
```

---

## Real Refactoring Examples for Bibby

### Example 1: Book Status Check

**Current (if you had this):**

```java
List<BookEntity> availableBooks = new ArrayList<>();
for (BookEntity book : allBooks) {
    if (book.getBookStatus().equals("AVAILABLE")) {
        availableBooks.add(book);
    }
}
```

**Refactored:**

```java
List<BookEntity> availableBooks = allBooks.stream()
    .filter(book -> book.getBookStatus().equals("AVAILABLE"))
    .collect(Collectors.toList());
```

### Example 2: Extract Titles

**Current (if you had this):**

```java
List<String> titles = new ArrayList<>();
for (BookEntity book : books) {
    titles.add(book.getTitle());
}
```

**Refactored:**

```java
List<String> titles = books.stream()
    .map(BookEntity::getTitle)  // Method reference!
    .collect(Collectors.toList());
```

### Example 3: Count Books by Status

**Current (if you had this):**

```java
int availableCount = 0;
for (BookEntity book : books) {
    if (book.getBookStatus().equals("AVAILABLE")) {
        availableCount++;
    }
}
```

**Refactored:**

```java
long availableCount = books.stream()
    .filter(book -> book.getBookStatus().equals("AVAILABLE"))
    .count();
```

### Example 4: Find First Match

**Current (if you had this):**

```java
BookEntity found = null;
for (BookEntity book : books) {
    if (book.getIsbn().equals(targetIsbn)) {
        found = book;
        break;
    }
}
```

**Refactored:**

```java
Optional<BookEntity> found = books.stream()
    .filter(book -> book.getIsbn().equals(targetIsbn))
    .findFirst();
```

---

## Lambda Performance Considerations

### Myth: "Lambdas are slow"

**FALSE!** The JVM optimizes lambdas very well.

```java
// Traditional loop:
for (BookEntity book : books) {
    System.out.println(book);
}

// Lambda:
books.forEach(System.out::println);

// Performance: Nearly identical after JIT compilation
```

### When Lambdas CAN Be Slower

1. **Creating many short-lived lambda instances in tight loops**

```java
// ❌ Creates new lambda each iteration:
for (int i = 0; i < 1000000; i++) {
    list.forEach(item -> process(item, i));  // Lambda captures 'i'
}

// ✅ Traditional loop is faster:
for (int i = 0; i < 1000000; i++) {
    for (Item item : list) {
        process(item, i);
    }
}
```

2. **Parallel streams with small datasets**

```java
// ❌ Overhead > benefit for small lists:
smallList.parallelStream()
    .filter(...)
    .collect(Collectors.toList());

// ✅ Sequential is faster:
smallList.stream()
    .filter(...)
    .collect(Collectors.toList());
```

**Bottom line:** Don't micro-optimize. Use lambdas where they improve readability.

---

## Testing Your Understanding

### Question 1: Lambda Syntax

Rewrite this anonymous class as a lambda:

```java
Comparator<BookEntity> comparator = new Comparator<BookEntity>() {
    @Override
    public int compare(BookEntity b1, BookEntity b2) {
        return b1.getTitle().compareTo(b2.getTitle());
    }
};
```

**Answer:**

```java
Comparator<BookEntity> comparator = (b1, b2) -> b1.getTitle().compareTo(b2.getTitle());
```

### Question 2: Method Reference

Rewrite this lambda as a method reference:

```java
books.stream()
    .map(book -> book.getTitle())
    .collect(Collectors.toList());
```

**Answer:**

```java
books.stream()
    .map(BookEntity::getTitle)  // Method reference
    .collect(Collectors.toList());
```

### Question 3: When NOT to Use Lambda

Should you refactor this to a lambda?

```java
for (int i = 0; i < bookcase.getShelfCapacity(); i++) {
    addShelf(bookcase, i, i);
}
```

**Answer:** **NO** - The traditional loop is clearer because you need the index `i`.

---

## Action Items for Your Codebase

### Priority 1: Safe Optional Handling

- [ ] **Refactor Optional.get() calls** with lambda alternatives
  ```java
  // Change:
  return bookEntity.isEmpty() ? null : bookEntity.get();

  // To:
  return bookEntity.orElse(null);

  // Or better:
  return bookEntity.orElseThrow(() -> new BookNotFoundException(...));
  ```

### Priority 2: Optional Chaining

- [ ] **Fix unsafe Optional chaining** (BookCommands.java:362)
  ```java
  // Change:
  Optional<ShelfEntity> shelf = ...;
  bookcaseService.findBookCaseById(shelf.get().getBookcaseId());

  // To:
  shelf.flatMap(s -> bookcaseService.findBookCaseById(s.getBookcaseId()));
  ```

### Priority 3: Consider forEach (Optional)

- [ ] **Evaluate simple loops** for forEach conversion
  ```java
  // If you have:
  for (BookEntity book : books) {
      log.info("Book: {}", book.getTitle());
  }

  // Could use:
  books.forEach(book -> log.info("Book: {}", book.getTitle()));
  ```

**Note:** Only refactor if it improves readability!

---

## Key Concepts Recap

| Concept | What It Means | Example |
|---------|---------------|---------|
| **Lambda** | Anonymous function | `(x, y) -> x + y` |
| **Functional Interface** | Interface with ONE abstract method | `Predicate<T>`, `Function<T,R>` |
| **Method Reference** | Shorthand for lambda calling method | `BookEntity::getTitle` |
| **Predicate** | Test condition | `book -> book.getStatus().equals("AVAILABLE")` |
| **Function** | Transform input | `book -> book.getTitle()` |
| **Consumer** | Perform action | `book -> System.out.println(book)` |
| **Supplier** | Supply value | `() -> new BookEntity()` |
| **forEach** | Iterate with action | `list.forEach(item -> print(item))` |
| **orElseThrow** | Optional with exception | `.orElseThrow(() -> new Exception())` |

---

## Summary

### What You Learned

1. ✅ **Lambda syntax** - `(params) -> expression`
2. ✅ **Functional interfaces** - Predicate, Function, Consumer, Supplier
3. ✅ **Method references** - `::` operator for cleaner lambdas
4. ✅ **When to use lambdas** - Optional chaining, forEach, stream operations
5. ✅ **When NOT to use lambdas** - Complex logic, need index, early termination
6. ✅ **Optional with lambdas** - `orElseThrow`, `flatMap`, `ifPresent`
7. ✅ **Readability first** - Don't use lambdas just to be "modern"

### Current State of Bibby

**Lambda Usage:** ZERO (not necessarily bad!)

**Opportunities:**
- ✅ Optional handling (orElseThrow instead of get())
- ✅ Optional chaining (flatMap instead of nested get())
- ⚠️ forEach for simple loops (optional - doesn't add much)
- ❌ Complex nested loops (keep traditional - more readable)

**Recommendation:** Focus on Optional lambdas first (Priority 1 and 2). Other refactorings are optional.

---

## What's Next?

In **Section 14: Stream API**, we'll examine:
- Stream operations (filter, map, collect)
- Refactoring loops to streams
- Terminal vs intermediate operations
- When streams improve readability
- When to stick with traditional loops

**Your Progress:**
- ✅ Sections 1-13 Complete (39% of mentorship guide)
- ⏳ 20 sections remaining

---

**Section 13 Complete! 🎉**

**Key takeaway:** Lambdas are tools, not goals. Use them where they improve readability - especially with Optional and streams. Traditional loops are often clearer for complex logic.

**Ready for Section 14?** Reply **"yes"** or **"continue"**.
