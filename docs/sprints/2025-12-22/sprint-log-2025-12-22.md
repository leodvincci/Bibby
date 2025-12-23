# Sprint Log: Robust Book Intake & Metadata Quality

## Daily Standup — December 22, 2025

**Sprint:** Robust Book Intake & Metadata Quality  
**Sprint Duration:** Week of December 22, 2025  
**Status:** In Progress

---

## Standup Summary

### Completed (Prior Session)

- Separated scan and manual book creation workflows (`BookCreateScanCommands` extraction)
- Reduced `BookCreateCommands` dependencies from 7 → 3
- Implemented CSV bulk import via `LibraryCommands`
- Added optional placement prompt (catalog now, shelve later)
- Introduced `m` / `:q` escape hatches for flexible ISBN input
- Renamed commands semantically (`add` → scan path, `register` → manual)
- 8 PRs merged (#157, #158, #160, #161, #163, #165, #166, #167)

### Today's Plan

- **#171:** Make metadata rendering null-safe so Add Book (ISBN) never crashes on missing publisher/fields

### Blockers/Risks

- None identified
- *Hidden risk to watch:* fixing publisher but missing other optional fields (authors, publishedDate, etc.) that can still crash the renderer

### Confidence Level

**High** — Clear scope, isolated fix, no external dependencies. This is a defensive coding task with well-defined boundaries.

---

## Mini Sprint Meeting

### Critical Path Focus

**#171 — Null-safe metadata rendering**

This is the correct priority. A crash on missing publisher data is a production blocker that undermines user trust in the entire ISBN workflow. Every other enhancement (edit flow, duplicate detection, confirmation prompts) assumes the happy path works reliably first. Fixing this first also gives a stable base to build confirmation (#135) and edit flow (#178) on top of.

### Next Smallest Slice

**Slice: Null-safe `BookcardRenderer` + `BookMetaDataResponse` handling**

#### Acceptance Criteria

- [ ] `BookcardRenderer.createBookCard()` renders `"Unknown"` or `"Not Provided"` for null/blank publisher
- [ ] `BookcardRenderer.bookImportCard()` applies same null-safety
- [ ] `BookMetaDataResponse` fields that can be null are documented (or use `Optional` / default values)
- [ ] Manual entry path unaffected (already prompts for all fields)
- [ ] Existing unit tests pass; at least 1 new test covers null publisher scenario

### Risk Radar

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| **Whack-a-mole nulls** — publisher fixed, then authors or publishedDate crashes next | Medium | Centralize "safe string" formatting in one place (e.g., `safe(value, "NOT PROVIDED")`) and audit all rendered fields |
| **Accidental data coercion** — turning null into `"UNKNOWN"` in persistence, losing distinction between "unknown" vs "missing" | Low | Use placeholders only at the **presentation** layer; store `null` in domain/entity |
| **Confirmation/save path leaks writes on cancel** — touching code around card display and prompts | Medium | Keep slice strictly to rendering + confirm path; do not refactor flow control. Trace where persistence happens |

### Test Targets

**Unit Tests**
- `BookcardRenderer` with null publisher, null author list, empty strings
- `BookMetaDataResponse` construction with partial data
- "Safe formatting" helper: null/blank/whitespace cases (if added)

**Integration Tests**
- Metadata fetch stubbed to return null publisher → scan flow prints preview and reaches "confirm" prompt

**Manual Tests**
- Run `book add` with an ISBN known to return incomplete metadata from Google Books API
- Confirm save → view book record
- Regression: full-metadata books still render correctly

### Exit Criteria for Today

- [ ] Repro case documented (ISBN + stack trace or stubbed response) and now no longer crashes
- [ ] `book add` with null publisher displays card without crashing
- [ ] Card shows `"Unknown"` or similar placeholder for missing publisher
- [ ] Same fix applied to `bookImportCard()` (CSV import path)
- [ ] Quick audit: any other optional metadata fields rendered safely (authors/date/subtitle/etc.)
- [ ] At least one unit test exercises the null-publisher case
- [ ] No regressions in existing book add flow (full metadata still works)
- [ ] Code changes are localized (renderer/formatter), not a wide refactor
- [ ] PR opened or merged for #171
- [ ] Commit message references #171 and describes behavioral change

---

## Scope Boundaries

### In Scope (Today)
- Null-safe rendering at presentation layer
- Placeholder text for missing fields
- Confirm → save with incomplete metadata

### Out of Scope (Defer)
- User editing missing fields inline → #178
- Duplicate ISBN detection → #151
- Confirmation gating refactor → #135

---

## Next Up (After #171)

Once crashes are resolved, the next priority from the critical path is either:
- **#135** — Confirmation gating (cancel = no write)
- **#178** — CLI book edit flow for metadata correction

Do not start these until #171 is merged.



---
### Standup Summary

**Completed**

- (No updates provided today)

**Today’s Plan**

- **#171** Make metadata rendering null-safe in the “Add Book (ISBN)” flow:
    - Render placeholders like `NOT PROVIDED` for missing fields (publisher and any other optional fields)
    - Ensure confirm → save works even when metadata is incomplete
    - Keep manual entry as the escape hatch (optional “enter publisher now” only if it’s cheap)

**Blockers/Risks**

- None reported
- *Hidden risk to watch:* you “fix publisher” but another optional field (authors, publishedDate, etc.) can still crash the renderer.

**Confidence Level**

- **High** — narrow scope, clear failure mode, easy to verify with a known ISBN that returns incomplete metadata.

------

## Mini Sprint Meeting (6-minute output)

### Critical Path Focus

**#171 is the single most important next issue** because a crash is a hard stop: it blocks the entire “book intake” loop and invalidates your sprint’s #1 success criterion (“No crashes when adding books via ISBN”). Fixing this first also gives you a stable base to build confirmation (#135) and edit flow (#178) on top of.

### Next Smallest Slice (shippable) for #171

**Slice: “Null-safe BookCard rendering for metadata”**

**Acceptance criteria**

- When Google Books metadata returns `publisher = null`, the CLI book preview renders without throwing exceptions.
- Book card shows `Publisher: NOT PROVIDED` (or equivalent) instead of crashing.
- Confirming the preview persists the book successfully with `publisher = null` (or empty) in storage.
- At least one automated test covers the null-publisher scenario (unit or integration).
- No other metadata field can crash the renderer (quick audit: title/authors/publishedDate/isbn).

### Risk Radar (top 3) + mitigation

1. **Whack-a-mole nulls** (publisher fixed, then authors or publishedDate crashes next)
    - *Mitigation:* Centralize “safe string” formatting in one place (e.g., `safe(value, "NOT PROVIDED")`) and run a quick pass over all rendered fields.
2. **Accidental data coercion** (turning null into `"UNKNOWN"` in persistence and later you can’t distinguish “unknown” vs “missing”)
    - *Mitigation:* Use placeholders only at the **presentation** layer; store `null` (or a proper optional) in the domain/entity if that’s your current model.
3. **Confirmation/save path still has side effects on cancel** (you’re touching code around card display and prompts; easy to leak a write)
    - *Mitigation:* Keep this slice strictly to rendering + confirm path; do not refactor flow control unless needed. Log/trace where persistence happens and ensure it’s only after confirm.

### Test Targets (for today’s plan)

- **Unit tests**
    - Book card renderer: `publisher=null` → output contains placeholder and no exception.
    - “safe formatting” helper: null/blank/whitespace cases if you add it.
- **Integration tests (if you have a facade/service test harness)**
    - Metadata fetch stubbed to return null publisher → scan flow prints preview and reaches “confirm” prompt.
- **Manual test**
    - Run `book scan` with an ISBN you know returns missing publisher (or temporarily stub response) → confirm save → view book record.

### Exit Criteria for Today (“Done when…”)

-  Repro case documented (ISBN + stack trace or stubbed response) and now no longer crashes
-  Book preview card renders with placeholder for null publisher
-  Confirm → save works with null publisher (no exception)
-  Quick audit: any other optional metadata fields rendered safely (authors/date/subtitle/etc.)
-  At least one test added that would fail on the old behavior
-  Code changes are localized (renderer/formatter), not a wide refactor
-  Commit message references **#171** and describes behavioral change (“null-safe metadata rendering”)

Next up in the sprint queue after #171 (don’t do it yet): **#135 confirmation gating** or **#178 edit flow** — but only once crashes are dead.