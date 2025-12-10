# Devlog: Book Card Display for Search Results

**Date:** 2025-12-09  
**Module:** CLI  
**Type:** Feature  
**Status:** Complete

---

## Summary

Added a formatted ASCII book card display for search results, replacing the plain text output with a visually distinct card that shows title, ID, author, and location at a glance.

---

## Problem

Search results were plain and hard to scan:

```
Book Was Found 
Bookcase: Main
Shelf 2
```

Or worse:

```
Book Was Found Without a Location
```

No visual hierarchy, no book details, easy to miss.

---

## Solution

Created an ASCII book card with box-drawing characters:

```
╭──────────────────────────────────────────────────────────────────────────────╮
│  📖 Building Microservices                                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│  ID: 353                                                                     │
│  Author: Sam Newman                                                          │
│                                                                              │
│📍Location: PENDING / NOT SET                                                 │
╰──────────────────────────────────────────────────────────────────────────────╯
```

### createBookCard() Method

```java
public String createBookCard(String title, String id, String author, String location) {
    return """
            ╭──────────────────────────────────────────────────────────────────────────────╮
            │  📖 %-73s│
            ├──────────────────────────────────────────────────────────────────────────────┤
            │  ID: %-31s                                         │
            │  Author: %-31s                                     │
            │                                                                              │
            │📍Location: %-35s                               │
            ╰──────────────────────────────────────────────────────────────────────────────╯
    """.formatted(title, id, author, location);
}
```

Uses:
- Text blocks (Java 15+) for readable template
- `%-Ns` format specifiers for left-aligned, fixed-width fields
- Unicode box-drawing characters for clean borders
- Emoji for visual markers (📖 book, 📍 location)

### Usage in searchByTitle()

**Book found with location:**
```java
String bookCard = createBookCard(
        bookDTO.title(),
        bookDTO.id().toString(),
        authorFacade.findByBookId(bookDTO.id()).toString(),
        "Bookcase " + bookcaseDTO.get().bookcaseLabel() + ", Shelf " + shelfDTO.get().shelfLabel()
);
System.out.println(bookCard);
```

**Book found without location:**
```java
String bookCard = createBookCard(
        bookDTO.title(),
        bookDTO.id().toString(),
        authorFacade.findByBookId(bookDTO.id()).toString(),
        "PENDING / NOT SET"
);
System.out.println(bookCard);
```

---

## Supporting Changes

### AuthorDTO.toString()

Added to make author names readable:

```java
@Override
public String toString() {
    return firstName + " " + lastName;
}
```

**Note:** Currently calling `.toString()` on a `Set<AuthorDTO>` produces `[Sam Newman]` with brackets. For cleaner multi-author display, consider:

```java
String authors = authorFacade.findByBookId(bookDTO.id()).stream()
        .map(AuthorDTO::toString)
        .collect(Collectors.joining(", "));
```

### Yes/No Prompt Cleanup

Removed stray newlines from menu options:

```java
// Before
options.put("Yes  — \u001B[32mLet's Do It\n\u001B[0m", "Yes");

// After
options.put("Yes  — \u001B[32mLet's Do It\u001B[0m", "Yes");
```

---

## Design Decisions

### Why Fixed-Width Formatting?

ASCII box art requires consistent line lengths. The `%-73s` format ensures the title field is always 73 characters (left-aligned, space-padded), so the right border aligns correctly regardless of title length.

### Why Emoji?

Adds visual scanning anchors without requiring color support. Works in most modern terminals. Falls back gracefully to placeholder characters in terminals that don't support emoji.

### Why Not a Separate Formatter Class?

For now, `createBookCard()` is a private method in `BookCommands`. If other commands need similar cards (author cards, shelf cards), extract to a `CliCardFormatter` utility class.

---

## Screenshot

```
Guest </>BIBBY:_ book find
? Select a search method: title

Search by Title
? Book Title:_ Building Microservices
        ╭──────────────────────────────────────────────────────────────────────────────╮
        │  📖 Building Microservices                                                   │
        ├──────────────────────────────────────────────────────────────────────────────┤
        │  ID: 353                                                                     │
        │  Author: [Sam Newman]                                                        │
        │                                                                              │
        │📍Location: PENDING / NOT SET                                                 │
        ╰──────────────────────────────────────────────────────────────────────────────╯

? Would you like to search again? [Use arrows to move], type to filter
> Yes  — Let's Do It
  No  —  Not this time
```

---

## Files Changed

- `BookCommands.java` — Added createBookCard(), updated searchByTitle()
- `AuthorDTO.java` — Added toString()
- `CliPromptService.java` — Removed stray newlines from yes/no options

---

## Interview Talking Points

**"How did you approach CLI UX?"**
> I wanted search results to be scannable at a glance. Plain text blends together, so I added ASCII card formatting with box-drawing characters. The fixed-width formatting ensures alignment regardless of content length, and emoji provide visual anchors for quick identification.

**"Why use text blocks?"**
> Text blocks (Java 15+) make the template readable in code—you see exactly what the output looks like. Combined with `.formatted()`, it's clean and maintainable. No string concatenation noise.

---

## Outstanding Work

1. **Author display shows brackets** — `[Sam Newman]` instead of `Sam Newman`. Use stream/join for cleaner output.
2. **Long titles may overflow** — Truncation logic needed for titles > 73 chars.
3. **Extract to utility class** — If more card types needed, create `CliCardFormatter`.
4. **Apply to other search methods** — `searchByAuthor()`, `searchByLocation()` could use same pattern.

---

## Commit

```
feat(cli): add book card display for search results

- Add createBookCard() for formatted ASCII book display
- Update searchByTitle() to use book cards
- Add AuthorDTO.toString() for readable author names
- Clean up yes/no prompt options (remove stray newlines)
```
