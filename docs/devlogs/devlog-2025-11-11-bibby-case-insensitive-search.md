## 🧾 Dev Log — *Case-Insensitive Title Search*

**Date:** November 11, 2025
**Project:** Bibby
**Feature:** Case-Insensitive Book Lookup
**Commit:** `feat(search): make book title lookup case-insensitive`

------

### 🎯 Objective

Allow users to find books by title regardless of letter case.
Before: `"A First Course in Database Systems"` had to be typed exactly.
After: `"a first course in database systems"` or `"A FIRST COURSE..."` both succeed.

------

### 🧠 Discovery

Spring Data JPA can generate **case-insensitive queries automatically** using the `IgnoreCase` keyword in repository method names.

```
BookEntity findByTitleIgnoreCase(String title);
```

Spring internally parses the method name, detects `IgnoreCase`, and injects SQL like:

```
select * from book where lower(title) = lower(?)
```

This feature is database-agnostic and relies on JPA’s query derivation mechanism.

------

### ⚙️ Implementation

- Added the method `findByTitleIgnoreCase(String title)` to `BookRepository`.
- Updated `BookService.findBookByTitle()` to call it instead of the old case-sensitive one.
- Verified no other logic changed (same output formatting and messages).

------

### ✅ Verification

| Input                                  | Expected Output   |
| -------------------------------------- | ----------------- |
| `"A First Course in Database Systems"` | ✅ Book Found      |
| `"a first course in database systems"` | ✅ Book Found      |
| `"A FIRST COURSE IN DATABASE SYSTEMS"` | ✅ Book Found      |
| `"nonexistent title"`                  | 🟠 No luck message |

------

### 📈 Outcome

Users can now type titles naturally without worrying about case.
No schema changes, no custom query strings, and the repository remains pure JPA.

------

### 🔍 Future Curiosity

- Dig into how **Spring Data query derivation** parses method names and constructs JPQL.
- Explore additional derived keywords like `Containing`, `StartingWith`, and `OrderBy`.
- Consider adding **partial matching** (`findByTitleContainingIgnoreCase`) next.