December 24, 2026

------

## Standup Summary

**Completed**

- \#171: Fixed ISBN add crash when publisher is null — null-safe metadata rendering (PR #181)
- \#178: Added CLI book edit flow for metadata correction — publisher field with cancel-safe confirmation (PR #184)

**Today's Plan**

- \#151: Duplicate ISBN detection + "Add another copy?" prompt in scan flow

**Blockers/Risks**

- None identified

**Confidence Level:** **Medium-High** — Clear acceptance criteria, reuses existing patterns (duplicate check is just a `findByIsbn` call, prompt pattern exists from #178). Main complexity is integrating the check at the right point in the scan flow.

------

## Mini Sprint Meeting

### Critical Path Focus

**#151 — Duplicate ISBN Detection**

Good selection. This directly addresses the sprint success criterion "Duplicate ISBNs are detected and handled gracefully." With #171 (no crashes) and #178 (edit flow) complete, you've stabilized the core intake path. Now you're hardening data quality—preventing accidental duplicates is the next logical step.

### Next Smallest Slice

**Slice: Detect existing ISBN + prompt before persistence (Option A)**

Ship the UX without the model refactor. Create duplicate Book records intentionally for now; defer Book/BookCopy split to future slice.

#### Acceptance Criteria

- [x] After ISBN lookup, check if book already exists in library (`findByIsbn` or equivalent)

- [x] If exists: display "Already in library" message with title, author
- [ ] If exists: display "Already in library" location
- [x] Prompt: "Add another copy? (y/N)"
- [x] "No" → exit cleanly, zero writes, message "No changes made."
- [x] "Yes" → continue to placement flow, persist new record
- [ ] At least one test covers "No" path (no persistence)
- [ ] At least one test covers "Yes" path (persistence occurs)

### Risk Radar

| Risk                                                 | Likelihood | Mitigation                                                   |
| ---------------------------------------------------- | ---------- | ------------------------------------------------------------ |
| **Scope creep into Book/Copy model refactor**        | High       | Explicitly defer. Option A only. Add breadcrumb comment for BOOK-COPY-02 |
| **"No" path leaks a write**                          | Medium     | Same pattern as #178: only persist after explicit "Yes". Test with `never()` verification |
| **Integration point confusion**                      | Medium     | Check must happen AFTER Google Books lookup, BEFORE any persistence or placement prompts |
| **`findByIsbn` doesn't exist or returns wrong type** | Low        | Verify repository method exists; may need to add if missing  |

### Test Targets

**Unit Tests**

- Repository/Facade: `findByIsbn` returns existing BookDTO when ISBN exists, null/empty when not
- Scan flow: existing ISBN + "No" → `verify(bookFacade, never()).createBook(...)`
- Scan flow: existing ISBN + "Yes" → `verify(bookFacade, times(1)).createBook(...)`

**Integration Tests (if applicable)**

- Full scan flow with mocked prompt returning "No" → verify DB unchanged
- Full scan flow with mocked prompt returning "Yes" → verify new record created

**Manual Tests**

1. `book add` → scan ISBN for book already in library → verify prompt appears with correct info
2. Select "No" → verify "No changes made" and no new record
3. Select "Yes" → complete placement → verify second record exists
4. `book add` → scan new ISBN → verify no duplicate prompt (normal flow)

### Exit Criteria for Today

Done when:

- [x] `findByIsbn` (or equivalent) check added to scan flow after Google Books lookup
- [ ] "Already in library" message displays existing book summary (title, author, location)
- [x] "Add another copy? (y/N)" prompt implemented
- [x] "No" exits with no persistence
- [x] "Yes" continues normal flow and persists
- [x] At least two tests: one for "No" path, one for "Yes" path
- [x] No model changes (Option A only)
- [x] PR opened or merged for #151
- [x] Commit messages reference #151

------

### Integration Point (Implementation Hint)

The check goes here in the scan flow:

```
1. Prompt for ISBN
2. Call Google Books API → get metadata
3. ★ CHECK: Does ISBN already exist locally? ★
   → If yes: show summary, prompt "Add another copy?"
   → If no: continue
4. Prompt for placement (bookcase, shelf)
5. Persist
```

The duplicate check is a local DB query, not an API call. Don't confuse "book exists in Google Books" with "book exists in my library."

------

**Scope reminder:** Option A only. You're making duplicates *intentional*, not *impossible*. The Book/BookCopy model split is BOOK-COPY-02, not today.