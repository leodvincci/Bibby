# Current Sprint: Robust Book Intake & Metadata Quality

**Sprint Duration:** Week of December 22, 2025  
**Status:** In Progress

## Overview

This sprint focuses on making the "add book by ISBN or manual entry" workflow bulletproof, user-friendly, and production-ready. Users should be able to add books confidently without crashes, validate/correct metadata post-creation, and handle edge cases gracefully.

## Key Objectives

- Fix critical bugs blocking ISBN workflow (null publisher crash)
- Enable metadata correction after book creation via CLI edit flow
- Improve data quality through validation and duplicate detection
- Enhance UX with confirmation prompts and better feedback
- Clean up code through targeted refactoring for maintainability

## Sprint Backlog

### Critical Path (Must-Have)

| Issue | Title | Status |
|-------|-------|--------|
| [#171](https://github.com/leodvincci/Bibby/issues/171) | Fix ISBN add crash when publisher is null | In Progress |
| [#178](https://github.com/leodvincci/Bibby/issues/178) | Add CLI book edit flow for metadata correction | Todo |
| [#135](https://github.com/leodvincci/Bibby/issues/135) | Confirm details before persisting (cancel = no write) | Todo |
| [#169](https://github.com/leodvincci/Bibby/issues/169) | Fix `book shelve` to match help text | Todo |

### Enhancement Layer (Should-Have)

| Issue | Title | Status |
|-------|-------|--------|
| [#151](https://github.com/leodvincci/Bibby/issues/151) | Duplicate ISBN detection + "Add another copy?" prompt | Todo |
| [#159](https://github.com/leodvincci/Bibby/issues/159) | Extract author resolution refactor | Todo |
| [#156](https://github.com/leodvincci/Bibby/issues/156) | Move ISBN validation closer to creation | Todo |
| [#155](https://github.com/leodvincci/Bibby/issues/155) | Introduce `InfoCard` model for better CLI output | Todo |

## Success Criteria

- No crashes when adding books via ISBN (even with incomplete data)
- Users can edit book metadata after creation
- Duplicate ISBNs are detected and handled gracefully
- Users must confirm before data is persisted
- `book shelve` command works as documented
- Code is cleaner with author resolution extracted

## Out of Scope

The following features are intentionally excluded from this sprint:

- Shelf/booklist features ([#175](https://github.com/leodvincci/Bibby/issues/175), [#176](https://github.com/leodvincci/Bibby/issues/176), [#177](https://github.com/leodvincci/Bibby/issues/177))
- Advanced placement workflows ([#168](https://github.com/leodvincci/Bibby/issues/168), [#170](https://github.com/leodvincci/Bibby/issues/170))
- Documentation overhaul ([#174](https://github.com/leodvincci/Bibby/issues/174))

## Progress

**Completion:** 0/8 issues (0%)

---

> **Note:** This sprint is tracked in the [Sprint Project Board](https://github.com/leodvincci/Bibby/projects) and the ["Sprint: Robust Book Intake & Metadata Quality" milestone](https://github.com/leodvincci/Bibby/milestones).