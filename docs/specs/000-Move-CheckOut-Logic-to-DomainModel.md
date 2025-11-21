# Micro Slice Specification: Move CheckOut Logic to Domain Model

## Branch Name

```
refactor/ddd-book-checkout-logic
```

------

## Overview

Refactor the `checkOutBook` method in `BookService` to follow Domain-Driven Design principles by moving business logic from the service layer into the `Book` domain model.

## Current State

### Location: `BookService.java`

```
public void checkOutBook(BookEntity bookEntity){    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){        bookEntity.setBookStatus("CHECKED_OUT");        saveBook(bookEntity);    } } 
```

**Problems:**

1. Business logic (checking if already checked out) lives in the service layer
2. Domain knowledge about valid state transitions is outside the domain model
3. Service layer must know implementation details about status comparison
4. Violates DDD principle: the domain model should protect its invariants

------

## Target State

### Book.java (Domain Model)

Add a new method that encapsulates the checkout business logic:

```
/** * Attempts to check out this book. *  * @return true if the book was successfully checked out, false if already checked out */ public boolean checkOut() {    if (this.status == BookStatus.CHECKED_OUT) {        return false;    }    this.status = BookStatus.CHECKED_OUT;    return true; } 
```

### BookService.java (Service Layer)

Simplify to orchestration only:

```
public void checkOutBook(BookEntity bookEntity){    if (bookEntity.checkOut()) {        saveBook(bookEntity);    } } 
```

**Alternative (more explicit):**

```
public boolean checkOutBook(BookEntity bookEntity){    boolean wasCheckedOut = bookEntity.checkOut();    if (wasCheckedOut) {        saveBook(bookEntity);    }    return wasCheckedOut; } 
```

------

## Implementation Steps

1. **Add** `checkOut()` **method to Book domain model**
   - Encapsulate the status check logic
   - Return boolean to indicate success/failure
   - Make the domain model responsible for its state transitions
2. **Update BookService.checkOutBook()**
   - Replace inline logic with call to `book.checkOut()`
   - Only save if checkout was successful
   - Consider whether to return boolean to caller
3. **Consider consistency with BookEntity**
   - Verify whether `BookEntity` should also get this method
   - Note: Based on service code, it appears `BookEntity` is used, not `Book`
   - May need to add method to `BookEntity` instead/also

------

## Verification Checklist

- [ ] Business logic moved from service to domain model
- [ ] Service method simplified to orchestration only
- [ ] Status transition logic encapsulated in domain model
- [ ] Return value properly communicated to caller
- [ ] No behavioral changes to existing functionality
- [ ] Consider: Should there be a `checkIn()` method added as well for symmetry?

------

## Notes & Considerations

**Entity vs Domain Model Clarification:**

- The service references `BookEntity` but the document shows `Book.java`
- Verify which class is actually used at runtime
- May need to apply changes to correct class

**Future Enhancement:**

- Consider adding `checkIn()` method following same pattern
- Consider adding `isAvailableForCheckout()` query method
- Could introduce a `BookState` value object for more complex state machines

**DDD Benefits:**

- Domain model protects its invariants
- Business rules live close to the data they govern
- Service layer becomes thinner, focused on orchestration
- Easier to test domain logic in isolation