# Devlog: Cancelable Placement Prompts (No Partial Records)

**Date:** 2025-12-18  
**Module(s):** CLI / Cataloging (Book + Shelf + Bookcase)  
**Type:** Bugfix | UX  
**Status:** 🟢 Shipped  

**Related Slices:**
- Slice — Add Cancel Options to Bookcase & Shelf Prompts (No Partial Records)
- Slice — Place Book After Scan (Confirm → Place → Save)

---

## High-Level Summary

Added explicit `[Cancel]` options to Bookcase and Shelf selection prompts during `book new --scan`. Canceling at any point exits cleanly and does **not** create a book—preserving data integrity and respecting user intent.

---

## The Problem

The scan flow introduced interactive placement (Bookcase → Shelf). But when a user needed to back out mid-flow, the CLI either had no clear Cancel option or risked leaving the system in a partially-completed state.

**Invariant violated:**
> If the user cancels placement, no book should be created.

This was both:
- **A UX problem** — the user felt trapped in a multi-step wizard with no exit
- **A data integrity problem** — risk of locationless or orphaned records

---

## What Changed

1. **Prompt layer** — Added `[Cancel]` menu option to:
   - Bookcase selection
   - Shelf selection

2. **Flow control** — Modified scan registration to short-circuit when selection returns empty/null:
   - If user cancels → exit before any persistence

3. **Messaging** — Standardized cancel output as a normal user action (not an error):
   - `Canceled. No changes were made.`
   - `Canceled. Book was not added.` (when declining initial confirmation)

---

## Behavior Change

### Before

```
? Would you like to add this book to the library?
> Yes — Let's Do It

? Choose a Bookcase:
  Bookcase 1
  Bookcase 2

# No clear cancel path
# Risk: partial/invalid records if user aborts mid-flow (Ctrl+C)
```

### After

```
? Would you like to add this book to the library?
> Yes — Let's Do It

? Choose a Bookcase:
  [Cancel]          ← NEW
  Bookcase 1
  Bookcase 2

# If user selects Cancel:
Canceled. No changes were made.
# No DB writes occur

? Choose a Shelf in "Bookcase 1":
  [Cancel]          ← NEW
  Shelf 1
  Shelf 2

# If user selects Cancel:
Canceled. No changes were made.
# No DB writes occur
```

---

## Why This Matters

| Lens | Impact |
|------|--------|
| **Product** | Flow respects user intent; no "trapped in wizard" feeling |
| **Engineering** | Prevents invalid/partial records; maintains location invariant |
| **Operations** | Cancel = exit code 0; predictable for scripting |

---

## Design Decisions

### Cancel as First-Class Outcome

Cancel is treated as an expected, valid user action—not an error or exception. This means:
- Clean exit (no stack trace, no error styling)
- Consistent messaging regardless of *where* in the flow the user cancels
- Exit code 0 (success) rather than non-zero

### All-or-Nothing Persistence

The book is only persisted after *all* required data is collected:
1. ✅ ISBN lookup succeeds
2. ✅ User confirms they want to add the book
3. ✅ User selects a bookcase
4. ✅ User selects a shelf
5. → **Then** persist to database

Canceling at steps 2, 3, or 4 results in zero database writes.

---

## Tradeoffs

| Decision | Upside | Downside |
|----------|--------|----------|
| Cancel option in every prompt | User always has an exit | Flow is slightly more verbose |
| All-or-nothing persistence | No orphan records | User must repeat full flow if they cancel late |

The verbosity tradeoff is worth it—reliability and user control outweigh the minor friction of an extra menu item.

---

## Follow-ups

- [ ] **Pin `[Cancel]` position** — Currently may shift depending on list sorting; should be consistently first or last
- [ ] **Inline "Create Bookcase/Shelf"** — If user has no bookcases, they hit a dead end; allow creation from within the flow
- [ ] **CLI E2E tests** — Add test cases that cancel at each step and verify no records created

---

## Verification

```bash
# Test 1: Cancel at bookcase prompt
bibby book new --scan
# → confirm add
# → cancel at bookcase prompt
# Expected: "Canceled. No changes were made."
# Verify: no new book in database

# Test 2: Cancel at shelf prompt
bibby book new --scan
# → confirm add
# → choose bookcase
# → cancel at shelf prompt
# Expected: "Canceled. No changes were made."
# Verify: no new book in database

# Test 3: Decline initial confirmation
bibby book new --scan
# → select "No — Not this time"
# Expected: "Canceled. Book was not added."
# Verify: no new book in database
```

---

## Interview Talking Points

1. **Data integrity invariant** — "I identified that cancel could leave partial records, so I restructured the flow to be all-or-nothing. The book only persists after all required data is collected."

2. **UX design principle** — "Users should never feel trapped. Every multi-step flow needs a clear exit at each step."

3. **Error vs. expected outcome** — "Cancel isn't an error—it's a valid user choice. I made sure the messaging and exit codes reflect that distinction."

4. **Testing strategy** — "For flows with side effects, I verify the *absence* of records after cancel, not just the presence of records after success."
