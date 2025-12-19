Date: 2025-12-19

Module(s): CLI / Cataloging (Book + Search + Card Renderer)

Type: Fix | UX

Status: 🟢 Done

------

# Devlog — Prevent CLI Book Card Overflow (Truncate + Normalize Authors)

## 1) High-Level Summary

I fixed a UI break in the CLI book card where long **Author** (and sometimes Publisher) strings would spill past the right border and destroy the card layout. The card now stays intact by **normalizing** author formatting and **truncating** the author string with an ellipsis when it exceeds the card’s safe width.

## 2) The Underlying Problem or Friction

The book card renderer used `String.format` padding (e.g., `%-31s`) to align columns, but those width values are **minimums**, not hard caps. When the author list was long, `%s` printed the entire string, pushing content beyond the border characters and breaking the “boxed” UI.

In list/search mode, this is especially painful: I’m scanning quickly and the UI gets visually corrupted exactly when I need it most.

## 3) What Changed (Behavior)

### Before

- Long author values could overflow the right border.
- Author lists sometimes included bracket characters (`[...]`) and inconsistent comma spacing.
- Logging was effectively disabled, making debugging harder.

### After

- Author string is normalized before rendering (remove brackets, normalize commas).
- Author line is truncated to fit the card width, and appends `...` when the author string exceeds a threshold.
- Logging is re-enabled at INFO level in `application.properties`.

## 4) Implementation Notes (Key Edits)

- Updated the book card template:
  - Added precision formatting to cap author output: `%-31.31s` (hard max width).
  - Added a separate placeholder for the ellipsis: `%-3.3s`.
  - Adjusted spacing alignment for ISBN/Location/Bookcase/Shelf labels to keep the card visually consistent.
- Added `formater(String authors)`:
  - removes `[` and `]`
  - normalizes comma spacing (ensures consistent separation)
- Added `countAuthors(String authors)` as a utility (currently unused in the diff, but useful for future “N authors” display).
- Re-enabled `INFO` logging by commenting out the `OFF` settings.

## 5) Acceptance Criteria Check

-  Card border remains intact even with long author lists (no overflow).
-  Author formatting is cleaner and more consistent.
-  Search/list view remains compact and scannable (truncation instead of wrapping).
-  Logging is available again for debugging.

## 6) Follow-ups / Next Slices

- **Detail view should wrap instead of truncate** so users can see full author/publisher metadata when they intentionally drill into a book (list view truncation is correct; detail view should be complete).
- Replace the `author.length() > 42` threshold with a width derived from the card’s inner width (single source of truth).
- Remove or gate the debug `System.out.println(authorFacade.findByBookId(...))` output once verified.

## 7) The Lesson

`String.format("%-31s")` creates alignment only when data is “well-behaved.” The moment real-world strings get long, you need a layout rule that enforces a *maximum* (truncate) or a multi-line strategy (wrap). For a CLI list view, truncation is the right trade: stable layout beats completeness—*as long as detail view exists for the full truth.*
<img width="1578" height="461" alt="image" src="https://github.com/user-attachments/assets/c5e42a9e-dda4-4fba-abd0-0a2cd9832c00" />

<img width="1442" height="629" alt="image" src="https://github.com/user-attachments/assets/f2db83ac-923b-40df-9117-5e5e5ffc5170" />
<img width="873" height="257" alt="image" src="https://github.com/user-attachments/assets/8a15f893-05a8-471d-9f87-6a9be60bf844" />
<img width="873" height="257" alt="image" src="https://github.com/user-attachments/assets/0d4fdb11-133f-43f0-b252-2ad7c7d52952" />


