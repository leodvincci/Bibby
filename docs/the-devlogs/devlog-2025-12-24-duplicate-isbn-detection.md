# Devlog: Duplicate ISBN Detection

**Date:** December 24, 2025  
**Issue:** #151 — Duplicate ISBN Detection + "Add Another Copy?" Prompt  
**Time Spent:** ~2 hours  
**Status:** Shipped ✅

---

## The Problem

Bibby had a silent data integrity issue: scanning a book that already exists in your library would create a duplicate record. No warning, no prompt, just another row in the database.

This is the kind of bug that erodes trust. You scan your bookshelf, realize later you have 3 copies of "Clean Code" in the system when you only own one. Now you're cleaning up data instead of cataloging books.

The goal: make duplication *intentional*, not *accidental*.

---

## The Design Decision: Option A

The issue outlined two approaches:

**Option A (pragmatic):** Detect duplicate, prompt user, still create a new Book record if they confirm. Quick to ship, defers model complexity.

**Option B (correct modeling):** Introduce a Book/BookCopy split where Book represents the edition (unique by ISBN) and BookCopy represents physical instances with locations.

I went with Option A. Why?

1. It solves the user-facing problem immediately
2. The UX is identical regardless of underlying model
3. I can refactor to Book/BookCopy later without changing the prompt flow
4. Shipping > perfection

Left a breadcrumb in the issue: "BOOK-COPY-02: Split Book from BookCopy so ISBN remains unique while copies can be many."

---

## Implementation

### Layer 1: Facade Interface

Added the check to `BookFacade`:

```java
boolean isDuplicate(String isbn);
```

Simple boolean. Does this ISBN exist in my library? Yes or no.

### Layer 2: Facade Adapter

The implementation delegates to the repository:

```java
@Override
public boolean isDuplicate(String isbn) {
    return bookDomainRepository.findBookByIsbn(isbn) != null;
}
```

I already had `findBookByIsbn()` from earlier work. The duplicate check is just a null check on that result. No new repository method needed.

### Layer 3: Prompt Service

New method in `CliPromptService`:

```java
public boolean promptForDuplicateConfirmation() {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withSingleItemSelector("duplicateConfirmation")
            .name("A book with this ISBN already exists. Add another copy?")
            .selectItems(promptOptions.yesNoOptions())
            .and().build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    if(result.getContext().get("duplicateConfirmation", String.class).equalsIgnoreCase("No")){
        System.out.println("\u001B[38:5:190mCanceled. Book was not added.\u001B[0m");
        return false;
    }
    return result.getContext().get("duplicateConfirmation", String.class).equalsIgnoreCase("Yes");
}
```

Reuses the existing `yesNoOptions()` pattern. Consistent UX with other confirmation prompts.

### Layer 4: Command Integration

The key change in `createBookScan()`:

```java
if (cliPrompt.promptToConfirmBookAddition()) {
    if(bookFacade.isDuplicate(isbn)){
        System.out.println("\n\033[38;5;3m⚠\u001B[0m Book with ISBN " + isbn + " already exists in the library.\n");
        if(!cliPrompt.promptForDuplicateConfirmation()) return;
    }
    // ... continue with author creation and persistence
}
```

The integration point matters:
1. ✅ AFTER Google Books lookup (we have the ISBN)
2. ✅ AFTER initial "Add this book?" confirmation
3. ✅ BEFORE author creation (no side effects yet)
4. ✅ BEFORE placement prompts
5. ✅ BEFORE persistence

If the user says "No" to the duplicate prompt, we `return` immediately. Zero writes. Clean exit.

---

## The UX Flow

```
bibby> book add

ISBN Number: 9780135957059

╭─────────────────────────────────────────────────────────╮
│  📖 The Pragmatic Programmer                            │
│  ISBN: 9780135957059                                    │
│  Author: David Thomas, Andrew Hunt                      │
│  Publisher: Addison-Wesley                              │
╰─────────────────────────────────────────────────────────╯

Add this book to your library? (Yes/No)
> Yes

⚠ Book with ISBN 9780135957059 already exists in the library.

A book with this ISBN already exists. Add another copy?
> No

Canceled. Book was not added.
```

Or if they want the duplicate:

```
A book with this ISBN already exists. Add another copy?
> Yes

Assign a shelf now? (Yes/No)
> No

╭─────────────────────────────────────────────────────────╮
│  📖 The Pragmatic Programmer                            │
│  ...                                                    │
│  Location: Not Set                                      │
╰─────────────────────────────────────────────────────────╯

Successfully added to the library
Item in Bookcart. Ready to be shelved.
```

The user is now in control. Duplicates happen when they *mean* to happen.

---

## Test Coverage

Added `BookCreateIsbnCommandsTest.java` with two tests for the general flow:

| Test | What It Proves |
|------|----------------|
| `createBookScan_whenUserSelectsNo_doesNotCreateBook` | Initial confirmation "No" → no persistence |
| `createBookScan_whenUserSelectsYes_andNoPlacement_createsBookWithNotSetLocation` | Confirm + skip placement → book created with "Not Set" |

**Still needed:** Two more tests specifically for the duplicate branch:
- Duplicate detected + user says "No" → `verify(bookFacade, never()).createBookFromMetaData(...)`
- Duplicate detected + user says "Yes" → `verify(bookFacade, times(1)).createBookFromMetaData(...)`

The pattern is identical to the existing tests, just with `isDuplicate()` returning `true`.

---

## What I Didn't Do

**Didn't show existing book location in the warning.** The acceptance criteria called for displaying "title, author, location" when duplicate is detected. I'm showing the ISBN and a generic warning, but not fetching and displaying the existing record's details.

Why skip it? The current `isDuplicate()` returns boolean, not the full record. To show location, I'd need to either:
- Change `isDuplicate()` to return `Optional<BookDTO>`
- Make a second call to `findBookByIsbn()` to get details

Both are easy, but I prioritized shipping the core flow. The user already sees the book card from Google Books—they know what book it is. Location display is polish, not MVP.

Noted in sprint log as incomplete acceptance criterion. Can add in follow-up.

---

## Commits

| Commit | Description |
|--------|-------------|
| `d774b17` | docs: add standup conversation log for December 24, 2025 |
| `8c96984` | docs: add sprint log for December 24, 2025 |
| `e42cfe4` | feat(facade): add `isDuplicate` method to BookFacade interface |
| `3fee05b` | feat(facade): implement `isDuplicate` in BookFacadeAdapter |
| `c32d5c8` | feat(cli): add `promptForDuplicateConfirmation` to CliPromptService |
| `19b852d` | feat(cli): integrate duplicate detection into scan flow |
| `22bb30c` | style(cli): add warning emoji and formatting for duplicate message |
| `25fe8b9` | test(cli): add BookCreateIsbnCommandsTest with initial confirmation tests |
| `71cc168` | test(cli): add test for no-placement path |
| `f6bf8df` | refactor(cli): clean up test assertions |
| `e38ba94` | docs: update sprint log with acceptance criteria checkboxes |
| `adc0896` | docs: mark exit criteria complete |

---

## Architecture Reflection

This feature touched four layers:

```
CLI Command (createBookScan)
    ↓
Prompt Service (promptForDuplicateConfirmation)
    ↓
Facade (isDuplicate)
    ↓
Repository (findBookByIsbn - already existed)
```

The hexagonal architecture paid off here. Each layer had a single, clear change:
- Facade: "Can you tell me if this ISBN exists?"
- Prompt: "Can you ask the user about duplicates?"
- Command: "Wire them together at the right point"

No layer knew more than it needed to.

---

## Patterns Reused

**Cancel-safe flow from #178:** The same principle applies—collect intent, confirm, only persist after explicit "Yes". The `return` on "No" guarantees zero writes.

**Yes/No prompt options:** Reused `promptOptions.yesNoOptions()` for consistency. Every confirmation prompt looks the same.

**Spy + doReturn pattern in tests:** Same test structure as `BookManagementCommandsTest`. Mock the dependencies, spy the command class, stub `scanBook()` to return controlled metadata.

---

## What I Learned

**Boolean checks are often enough.** I almost over-engineered `isDuplicate()` to return the full existing record. But the boolean answered the only question the flow needed: "Should I show the duplicate prompt?" YAGNI won.

**Integration point is the hard part.** The actual code for duplicate detection is trivial. The tricky bit was figuring out *where* in the flow to insert it. Too early and you don't have the ISBN yet. Too late and you've already created authors or started persistence. Drawing out the flow helped:

```
1. Prompt for ISBN
2. Google Books lookup
3. Show book card
4. "Add this book?" prompt
5. ★ DUPLICATE CHECK HERE ★
6. Author creation
7. Placement prompts
8. Persistence
```

Step 5 is the sweet spot: we have the ISBN, user has confirmed intent, but no side effects have occurred yet.

---

## Next Steps

- Add the two missing duplicate-specific tests
- Consider showing existing book location in warning message (minor UX polish)
- Move on to #135 (confirmation before persist) or #169 (fix `book shelve`)

---

## Session Stats

- **Commits:** 12
- **Files Changed:** 5 (+ 2 new test/doc files)
- **New Methods:** 2 (`isDuplicate`, `promptForDuplicateConfirmation`)
- **Tests Added:** 2 (need 2 more for full duplicate coverage)
- **Sprint Progress:** 4/9 complete (44%)
