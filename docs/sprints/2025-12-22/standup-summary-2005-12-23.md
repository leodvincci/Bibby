December 23, 2025

------

## Standup Summary

**Completed**

- \#171: Fixed ISBN add crash when publisher is null — null-safe metadata rendering shipped via PR #181

**Today's Plan**

- \#178: Add CLI book edit flow for metadata correction (select book → edit fields → validate → persist)

**Blockers/Risks**

- None identified

**Confidence Level:** **Medium** — This is a feature build, not a bug fix. Multiple integration points (book lookup, prompts, validation, persistence, cancel handling). Scope is well-defined but execution has more surface area than #171.

------

## Mini Sprint Meeting

### Critical Path Focus

**#178 — CLI book edit flow**

Correct priority. With #171 done, users can now *see* incomplete metadata without crashing—but they're stuck with it. The edit flow closes that loop and satisfies the sprint criterion "Users can edit book metadata after creation." This also sets up #135 (confirmation gating) since the edit flow will need the same cancel-safe pattern.

### Next Smallest Slice

**Slice: `book edit` for a single field (publisher) via ISBN lookup**

Don't boil the ocean. Ship the narrowest vertical that proves the pattern works.

#### Acceptance Criteria

- [ ] `book edit` command exists and prompts for ISBN
- [ ] Book is retrieved and current values displayed (book card)
- [ ] User can select "Publisher" field and enter new value
- [ ] Empty input keeps existing value (no accidental clears)
- [ ] Confirm step shows old → new diff before persisting
- [ ] Cancel at any step exits with no changes persisted
- [ ] Updated book card displays new publisher value
- [ ] At least one test covers: null publisher → "Addison-Wesley" → persisted

### Risk Radar

| Risk                                                        | Likelihood | Mitigation                                                   |
| ----------------------------------------------------------- | ---------- | ------------------------------------------------------------ |
| **Scope creep to multi-field edit**                         | High       | Ship publisher-only first. Add title/year in follow-up commit, not same PR |
| **Cancel leaks a write**                                    | Medium     | Collect all edits in memory; only call `bookFacade.update()` after explicit confirm |
| **Book lookup ambiguity** (ISBN not found, multiple copies) | Medium     | Start with ISBN-only lookup; defer search/list to enhancement. Return clear error if not found |
| **Validation gaps**                                         | Low        | Reuse existing field validation from manual entry if available; add year-is-numeric check |

### Test Targets

**Unit Tests**

- Book update service/facade: accepts partial update DTO, persists only changed fields
- Validation: year must be numeric (or null), publisher can be any string

**Integration Tests**

- Edit flow with mocked prompt inputs: ISBN → select publisher → enter value → confirm → verify persistence
- Cancel path: ISBN → select publisher → cancel → verify no DB write

**Manual Tests**

1. `book edit` → enter ISBN with null publisher → set to "O'Reilly" → confirm → `book view` shows new publisher
2. `book edit` → enter ISBN → select publisher → type `:q` or cancel → verify original value unchanged
3. `book edit` → enter nonexistent ISBN → verify graceful "Book not found" message

### Exit Criteria for Today

Done when:

- [ ] `book edit` command wired up in Spring Shell
- [ ] ISBN prompt retrieves existing book and displays current card
- [ ] User can update publisher field (at minimum)
- [ ] Confirm step shows before/after and requires explicit "Yes"
- [ ] Cancel exits cleanly with no persistence
- [ ] At least one automated test covers the happy path
- [ ] Book card after edit reflects new value
- [ ] Commit message references #178

------

**Scope warning:** The issue lists title, publisher, year, description. Don't try to ship all four today. Get publisher working end-to-end first. Once that's solid, adding more fields is mechanical. If you finish publisher early, add *one* more field (title or year), not all of them.

Ship the vertical, then widen.