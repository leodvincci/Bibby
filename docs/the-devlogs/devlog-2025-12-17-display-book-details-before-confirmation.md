# Devlog: Display Book Details Before Add Confirmation

**Date:** 2025-12-17  
**Feature:** Book scan confirmation preview  
**Status:** Complete

---

## Problem Statement

When scanning a book via `book new --scan`, users were prompted to confirm adding a book to their library with only the ISBN visible. This created a poor UX—users had no way to verify they were adding the correct book before committing.

**Before:**
```
? ISBN Number: 9781492078005
? Would you like to add this book to the library?
```

The metadata *was* being fetched successfully (visible in logs), but never surfaced to the user in the clean, logs-off experience.

---

## User Story

> As a user scanning a book, I want to see the book's details (title, authors, publisher, ISBN) before confirming addition, so that I can verify I'm adding the correct book.

---

## Solution

Display a formatted card showing book metadata between the ISBN input and the confirmation prompt. The card uses box-drawing characters for visual distinction and includes:

- Title (with edition if available)
- ISBN
- Author(s)
- Publisher
- Bookcase/Shelf status (showing "PENDING / NOT SET" for new books)

**After:**
```
? ISBN Number: 9781098110338

┌──────────────────────────────────────────────────────────────┐
│ 📖 Learning TypeScript                                       │
│                                                              │
│ ISBN: 9781098110338                                          │
│ Author: [Josh Goldberg]                                      │
│ Publisher: O'Reilly Media                                    │
│                                                              │
│ Bookcase: PENDING / NOT SET                                  │
│ Shelf: PENDING / NOT SET                                     │
└──────────────────────────────────────────────────────────────┘

? Would you like to add this book to the library?
```

---

## Implementation Notes

### Scope Boundaries

**In scope:**
- Formatting and displaying the already-fetched `BookMetaDataResponse`
- Presentation changes in the CLI command layer only

**Out of scope:**
- Changing fetch logic
- Handling fetch failures (existing behavior preserved)
- Adding edition/description fields to the card

### Key Decisions

1. **Card-based layout** — Box-drawing characters (`┌ ─ ┐ │ └ ┘`) create clear visual separation from the surrounding prompts

2. **Book emoji (📖)** — Provides a visual anchor for the title line; however, emoji width rendering varies by terminal and may affect alignment (see Known Issues)

3. **"PENDING / NOT SET" for location** — Communicates to users that shelf assignment is a separate step, setting correct expectations for the workflow

4. **Color-coded labels** — Field names (ISBN, Author, Publisher, etc.) use distinct coloring to improve scannability

### Data Flow

```
ISBN input
    ↓
BookFacade.findBookMetaDataByIsbn(isbn)
    ↓
BookMetaDataResponse (title, isbn, authors, publisher, description)
    ↓
Format card for display  ← NEW
    ↓
Confirmation prompt
    ↓
BookFacade.createBookFromMetaData(...)
```

The `BookMetaDataResponse` record already contained all necessary fields—this was purely a presentation layer change.

---

## Known Issues

### Potential Card Misalignment

Observed a vertical line fragment on the right edge near certain content rows. Suspected causes:

1. **Emoji width calculation** — The 📖 emoji renders as 1 character in string length but occupies 2 columns visually in most terminals
2. **ANSI escape codes** — If color codes are included in string length calculations, padding will be incorrect
3. **Variable content length** — Long titles or publisher names may exceed expected box width

**Mitigation options:**
- Calculate "display width" separately from string length (account for emoji as 2 columns)
- Strip ANSI codes before calculating padding
- Set a max content width and truncate with ellipsis if exceeded

---

## Files Changed

- CLI command layer (presentation logic for book scan flow)

---

## Testing

Manual verification:
- [x] Short title displays correctly
- [x] Multiple authors display correctly  
- [x] Long publisher name (e.g., "NO STARCH PRESS, INC") displays correctly
- [x] Card appears before confirmation prompt
- [ ] Edge case: very long title (potential truncation needed)

---

## Follow-up Considerations

1. **Extract card formatting to a reusable component** — Other CLI views (book details, shelf contents) could benefit from consistent card styling

2. **Fix alignment issues** — Investigate terminal-aware width calculation for emoji and ANSI codes

3. **Add edition to display** — The `BookMetaDataResponse` doesn't currently include edition; could be added if Google Books API provides it

---

## Reflection

This was a clean, focused slice—purely presentation, no domain logic changes. The `BookMetaDataResponse` already had everything needed; the gap was just surfacing it to the user at the right moment in the flow.

The alignment issue is a good reminder that terminal UI has its own quirks (emoji width, ANSI codes, variable-width fonts in some terminals). Worth extracting a proper "terminal card" utility if this pattern gets reused.
