# Sprint Log: Robust Book Intake & Metadata Quality

## Daily Standup — December 23, 2025

**Sprint:** Robust Book Intake & Metadata Quality  
**Sprint Duration:** Week of December 22, 2025  
**Status:** In Progress  
**Sprint Progress:** 2/8 issues complete (25%)

---

## Standup Summary

### Completed (Prior Session)
- #171: Fixed ISBN add crash when publisher is null — null-safe metadata rendering shipped via PR #181

### Today's Plan
- #178: Add CLI book edit flow for metadata correction (select book → edit fields → validate → persist)

### Blockers/Risks
- None identified

### Confidence Level
**Medium** — Feature build with multiple integration points (book lookup, prompts, validation, persistence, cancel handling). Scope well-defined but larger surface area than #171.

---

## Mini Sprint Meeting

### Critical Path Focus
**#178 — CLI book edit flow**

Correct priority. With #171 done, users can now *see* incomplete metadata without crashing—but they're stuck with it. The edit flow closes that loop and satisfies the sprint criterion "Users can edit book metadata after creation." This also sets up #135 (confirmation gating) since the edit flow uses the same cancel-safe pattern.

### Slice Shipped
**`book edit` for publisher field via ISBN lookup**

Shipped the narrowest vertical that proves the pattern works.

#### Acceptance Criteria
- [x] `book edit` command exists and prompts for ISBN
- [x] Book is retrieved and current values displayed (book card)
- [x] User can select "Publisher" field and enter new value
- [x] Empty input keeps existing value (no accidental clears)
- [x] Confirm step shows old → new diff before persisting
- [x] Cancel at any step exits with no changes persisted
- [x] Updated book card displays new publisher value
- [x] At least one test covers: null publisher → "Addison-Wesley" → persisted

### Risk Radar

| Risk | Likelihood | Mitigation | Outcome |
|------|------------|------------|---------|
| **Scope creep to multi-field edit** | High | Ship publisher-only first | ✅ Held scope to publisher only |
| **Cancel leaks a write** | Medium | Collect edits in memory; persist only after confirm | ✅ Tested and verified |
| **Book lookup ambiguity** | Medium | Start with ISBN-only lookup | ✅ ISBN lookup implemented |
| **Validation gaps** | Low | Reuse existing field validation | ✅ No issues encountered |

---

## Implementation Review

### Architecture (Hexagonal Layering)

```
CLI (BookManagementCommands)
    ↓
Facade (BookFacade / BookFacadeAdapter)
    ↓
Domain Repository (BookDomainRepository / BookDomainRepositoryImpl)
    ↓
JPA (bookJpaRepository)
```

### New Methods Added

**Facade Layer (`BookFacade`):**
- `updatePublisher(String isbn, String newPublisher)`
- `findBookMetaDataByIsbn(String isbn)` — with Javadoc

**Repository Layer (`BookDomainRepository` / `BookDomainRepositoryImpl`):**
- `updatePublisher(String isbn, String newPublisher)`

**CLI Layer:**
- `BookManagementCommands.BookEditCommand()` — main edit flow
- `CliPromptService.promptForBookEditSelection()`
- `CliPromptService.promptForEditPublisher()`
- `CliPromptService.promptToConfirmChange(String newValue)`
- `PromptOptions.metaDataSelection()` — with cancel option

### Test Coverage

| Test | Description | Status |
|------|-------------|--------|
| `testBookEditCommand_EditPublisherSuccess` | Happy path: edit → confirm → persist | ✅ |
| `testBookEditCommand_EditPublisherCancelledByUser` | Cancel path: no write on decline | ✅ |
| `testBookEditCommand_BookNotFound` | Error path: exception on missing ISBN | ✅ |

### Key Verification: Cancel-Safe Flow
```java
when(cliPromptServiceMock.promptToConfirmChange("New Publisher")).thenReturn(false);
// ...
verify(bookFacadeMock, never()).updatePublisher(anyString(), anyString());
```
No write leaks on cancel. ✅

---

## Commits (21 total)

| Commit | Description |
|--------|-------------|
| `744672f` | feat(cli): add BookManagementCommands class placeholder |
| `a39932b` | feat(cli): add `edit` command to BookManagementCommands |
| `7285a1b` | feat(cli): integrate CliPromptService into `BookEditCommand` |
| `d93df93` | feat(cli): refactor and integrate `scanBook` into `BookManagementCommands` |
| `2b5b75f` | feat(cli): add `promptForBookEditSelection` to CliPromptService |
| `dea013d` | feat(cli): add `metaDataSelection` method to PromptOptions |
| `4239d2c` | feat(cli): update `BookEditCommand` to call `promptForBookEditSelection` |
| `baa13c7` | refactor(cli): remove unused metadata options from `PromptOptions` |
| `c3d2522` | feat(cli): add publisher edit flow to `BookEditCommand` |
| `056b27c` | feat(repository): add `updatePublisher` method to BookDomainRepositoryImpl |
| `4b5fd66` | feat(repository): add `updatePublisher` method to BookDomainRepository |
| `23241fd` | feat(facade): add `findBookMetaDataByIsbn` and `updatePublisher` to BookFacade |
| `49af91b` | feat(facade): add `findBookMetaDataByIsbn` and `updatePublisher` to BookFacadeAdapter |
| `a7f4b76` | feat(cli): add `promptToConfirmChange` and enhance publisher edit flow |
| `04cc96f` | refactor(cli): remove `BookCreateIsbnCommands` from `BookManagementCommands` |
| `dc3303d` | feat(cli): add cancel option to `metaDataSelection` in `PromptOptions` |
| `144af9b` | style(cli): fix prompt text formatting in `CliPromptService` |
| `516e7f7` | feat(cli): add cancel option to book edit flow in `BookManagementCommands` |
| `e13737c` | test(cli): add unit tests for `BookEditCommand` in `BookManagementCommands` |

---

## Exit Criteria

| Criteria | Status |
|----------|--------|
| `book edit` command wired up in Spring Shell | ✅ |
| ISBN prompt retrieves existing book and displays current card | ✅ |
| User can update publisher field (at minimum) | ✅ |
| Confirm step shows before/after and requires explicit "Yes" | ✅ |
| Cancel exits cleanly with no persistence | ✅ |
| At least one automated test covers the happy path | ✅ (3 tests) |
| Book card after edit reflects new value | ✅ |
| Commit messages reference #178 | ✅ |

---

## Minor Technical Debt Noted

1. **Method naming:** `BookEditCommand()` should be `bookEditCommand()` per Java conventions
2. **Exception type:** `RuntimeException("Book not found")` could be domain-specific `BookNotFoundException`
3. **Good practice observed:** `updatedAt` field properly updated on edit

---

## Scope Boundaries

### Shipped (Today)
- Publisher field edit via ISBN lookup
- Cancel-safe confirmation flow
- Unit test coverage

### Deferred (Future Slices)
- Title, Year, Description fields → extend `metaDataSelection()`
- Bulk edit across multiple books
- Author management (merge/split)
- Version history / audit trail

---

## Sprint Backlog Status

### Critical Path (Must-Have)

| Issue | Title | Status |
|-------|-------|--------|
| #171 | Fix ISBN add crash when publisher is null | ✅ Complete |
| #178 | Add CLI book edit flow for metadata correction | ✅ Complete |
| #135 | Confirm details before persisting (cancel = no write) | Todo |
| #169 | Fix `book shelve` to match help text | Todo |

### Enhancement Layer (Should-Have)

| Issue | Title | Status |
|-------|-------|--------|
| #151 | Duplicate ISBN detection + "Add another copy?" prompt | Todo |
| #159 | Extract author resolution refactor | Todo |
| #156 | Move ISBN validation closer to creation | Todo |
| #155 | Introduce `InfoCard` model for better CLI output | Todo |

---

## Next Up

With #178 complete, the next critical path items are:
- **#135** — Confirm details before persisting (cancel = no write)
- **#169** — Fix `book shelve` to match help text

Note: The confirmation pattern implemented in #178 (`promptToConfirmChange`) may be reusable for #135.
