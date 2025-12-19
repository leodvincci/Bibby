# Devlog: Extract Prompt Menu Options into PromptOptions Class

**Date:** December 15, 2025  
**Module:** CLI (Prompt)  
**Type:** Refactor  
**Status:** ✅ Complete

---

## 1. High-Level Summary

- **Extracted menu-building logic into dedicated `PromptOptions` class** — centralizing all CLI menu construction
- **Removed duplicate `yesNoOptions()` method** — existed in both `BookCommands` and `CliPromptService`
- **Moved 5 menu-building methods** to single location for reuse and testability
- **Simplified `BookCommands` and `CliPromptService`** — now focused on flow orchestration, not menu construction

---

## 2. The Underlying Problem or Friction

Menu-building methods were scattered across multiple classes:

```
BookCommands.java
├── yesNoOptions()        ← Duplicate!
└── bookCaseOptions()

CliPromptService.java
├── yesNoOptions()        ← Duplicate!
├── bookShelfOptions()
├── buildSearchOptions()
└── buildAuthorOptions()
```

**Problems:**
1. **Duplication** — `yesNoOptions()` existed in two places with identical code
2. **Mixed responsibilities** — Command classes handled both flow AND menu construction
3. **Hard to test** — Menu logic embedded in large classes
4. **Hard to reuse** — Adding a new command meant copy-pasting menu methods

---

## 3. The Solution

Extract all menu-building into a single `PromptOptions` class:

```
PromptOptions.java (NEW)
├── yesNoOptions()
├── bookCaseOptions()
├── bookShelfOptions(Long bookcaseId)
├── searchOptions()
└── authorOptions(AuthorDTO author)

BookCommands.java
└── Injects PromptOptions, calls promptOptions.bookCaseOptions()

CliPromptService.java
└── Injects PromptOptions, calls promptOptions.yesNoOptions()
```

---

## 4. What Changed

### New Class: `PromptOptions`

**Location:** `com.penrose.bibby.cli.prompt.domain.PromptOptions`

```java
@Component
public class PromptOptions {
    private final BookcaseFacade bookcaseFacade;
    private final ShelfFacade shelfFacade;
    private final AuthorFacade authorFacade;
    private final BookFacade bookFacade;
    
    // Constructor injection...
    
    public Map<String, String> yesNoOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Yes  — \u001B[32mLet's Do It\u001B[0m", "Yes");
        options.put("No  —  \u001B[32mNot this time\u001B[0m", "No");
        return options;
    }
    
    public Map<String, String> bookCaseOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("\u001B[38;5;202m[Cancel]\033[36m", "cancel");
        for (BookcaseDTO b : bookcaseFacade.getAllBookcases()) {
            options.put(b.bookcaseLabel(), b.bookcaseId().toString());
        }
        return options;
    }
    
    public Map<String, String> bookShelfOptions(Long bookcaseId) { ... }
    public Map<String, String> searchOptions() { ... }
    public List<SelectItem> authorOptions(AuthorDTO author) { ... }
}
```

### BookCommands Changes

```java
// Before
public class BookCommands extends AbstractShellComponent {
    // ...
    
    private Map<String, String> yesNoOptions() { ... }      // Removed
    private Map<String, String> bookCaseOptions() { ... }   // Removed
    
    // Usage
    cliPrompt.promptForBookCase(bookCaseOptions());
}

// After
public class BookCommands extends AbstractShellComponent {
    private final PromptOptions promptOptions;  // New dependency
    
    // Usage
    cliPrompt.promptForBookCase(promptOptions.bookCaseOptions());
}
```

### CliPromptService Changes

```java
// Before
public class CliPromptService implements PromptFacade {
    private final ShelfFacade shelfFacade;  // Only needed for menu building
    
    private Map<String, String> yesNoOptions() { ... }           // Removed
    private Map<String, String> bookShelfOptions() { ... }       // Removed
    private Map<String, String> buildSearchOptions() { ... }     // Removed
    private List<SelectItem> buildAuthorOptions() { ... }        // Removed
}

// After
public class CliPromptService implements PromptFacade {
    private final PromptOptions promptOptions;  // Replaced ShelfFacade
    
    // Usage
    .selectItems(promptOptions.yesNoOptions())
    .selectItems(promptOptions.bookShelfOptions(bookcaseId))
    .selectItems(promptOptions.searchOptions())
    .selectItems(promptOptions.authorOptions(author))
}
```

---

## 5. Methods Moved

| Method | From | To |
|--------|------|----|
| `yesNoOptions()` | BookCommands | PromptOptions |
| `yesNoOptions()` | CliPromptService | PromptOptions (deduplicated) |
| `bookCaseOptions()` | BookCommands | PromptOptions |
| `bookShelfOptions(Long)` | CliPromptService | PromptOptions |
| `buildSearchOptions()` | CliPromptService | PromptOptions.searchOptions() |
| `buildAuthorOptions(AuthorDTO)` | CliPromptService | PromptOptions.authorOptions() |

---

## 6. Dependency Changes

### BookCommands

```java
// Before
public BookCommands(ComponentFlow.Builder componentFlowBuilder,
                    AuthorFacade authorFacade,
                    ShelfFacade shelfFacade,
                    CliPromptService cliPrompt,
                    BookFacade bookFacade,
                    BookcaseFacade bookcaseFacade)

// After
public BookCommands(ComponentFlow.Builder componentFlowBuilder,
                    AuthorFacade authorFacade,
                    ShelfFacade shelfFacade,
                    CliPromptService cliPrompt,
                    BookFacade bookFacade,
                    BookcaseFacade bookcaseFacade,
                    PromptOptions promptOptions)  // Added
```

### CliPromptService

```java
// Before
public CliPromptService(ComponentFlow.Builder componentFlowBuilder,
                        ComponentFlow.Builder componentFlowBuilder1,
                        ShelfFacade shelfFacade,  // Removed
                        AuthorFacade authorFacade,
                        BookFacade bookFacade)

// After
public CliPromptService(ComponentFlow.Builder componentFlowBuilder,
                        ComponentFlow.Builder componentFlowBuilder1,
                        AuthorFacade authorFacade,
                        BookFacade bookFacade,
                        PromptOptions promptOptions)  // Added
```

---

## 7. The Architectural Meaning

### Single Responsibility

| Class | Before | After |
|-------|--------|-------|
| BookCommands | Flow + Menu building | Flow only |
| CliPromptService | Prompts + Menu building | Prompts only |
| PromptOptions | (didn't exist) | Menu building only |

### Dependency Graph

```
Before:
BookCommands ──────┬──▶ BookcaseFacade (for menu building)
                   └──▶ CliPromptService
                            └──▶ ShelfFacade (for menu building)

After:
BookCommands ──────┬──▶ PromptOptions ──┬──▶ BookcaseFacade
                   │                    ├──▶ ShelfFacade
                   │                    ├──▶ AuthorFacade
                   │                    └──▶ BookFacade
                   └──▶ CliPromptService
                            └──▶ PromptOptions (same instance)
```

### Benefits

1. **Testability** — Can unit test menu construction independently
2. **Reusability** — Any new command class just injects `PromptOptions`
3. **Consistency** — All menus built the same way, in one place
4. **Maintainability** — Change menu styling once, applies everywhere

---

## 8. Package Placement

```
com.penrose.bibby.cli.prompt
├── application/
│   └── CliPromptService.java
├── contracts/
│   └── PromptFacade.java
└── domain/
    └── PromptOptions.java  ← New class
```

**Why `domain/`?**

`PromptOptions` contains the "domain knowledge" of how CLI menus should be structured. It's not a service (no business logic), not a contract (not an interface), but it encapsulates CLI presentation rules.

Alternative valid locations:
- `cli.prompt.infrastructure.PromptOptions` — if you view it as infrastructure
- `cli.shared.PromptOptions` — if shared across CLI modules

---

## 9. Talking Points (Interview / Portfolio)

- **Applied Extract Class refactoring** to eliminate duplicate menu-building code across CLI components

- **Improved separation of concerns** by moving presentation logic (menu construction) out of command handlers

- **Reduced coupling** between command classes and facade layer — commands no longer need facades just for menu building

- **Centralized CLI styling decisions** in one class, making it easier to maintain consistent user experience

- **Improved testability** by isolating menu construction logic into a dedicated, injectable component

---

## 10. Potential Interview Questions

1. Why did you extract menu-building into a separate class rather than a utility class with static methods?

2. How does this refactoring improve testability?

3. What's the difference between this `PromptOptions` class and a Service class?

4. Why inject `PromptOptions` rather than having commands call it statically?

5. How would you unit test the `authorOptions()` method?

6. What design pattern does this follow? (Strategy? Builder? Neither?)

7. How does this change affect the dependency graph of your CLI layer?

---

## 11. Before/After Comparison

### Lines of Code

| File | Before | After | Change |
|------|--------|-------|--------|
| BookCommands.java | ~650 | ~620 | -30 |
| CliPromptService.java | ~260 | ~200 | -60 |
| PromptOptions.java | 0 | ~80 | +80 |
| **Total** | ~910 | ~900 | -10 |

Net reduction is small, but **clarity improved significantly**.

### Duplicate Code Eliminated

```java
// This exact method existed in TWO files:
private Map<String, String> yesNoOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    options.put("Yes  — \u001B[32mLet's Do It\u001B[0m", "Yes");
    options.put("No  —  \u001B[32mNot this time\u001B[0m", "No");
    return options;
}
```

Now exists once in `PromptOptions`.

---

## 12. Minor Cleanup Needed

Extra blank lines at end of `CliPromptService.java` — remove before committing.

---

## 13. Commit Message

```
refactor(cli): extract prompt menu options into PromptOptions class

- Create PromptOptions class in cli.prompt.domain package
- Move yesNoOptions, bookCaseOptions, bookShelfOptions to PromptOptions
- Move searchOptions, authorOptions to PromptOptions
- Remove duplicate yesNoOptions from BookCommands and CliPromptService
- Inject PromptOptions into BookCommands and CliPromptService
- Remove ShelfFacade dependency from CliPromptService (now in PromptOptions)

Benefits:
- Eliminates code duplication
- Single responsibility: commands handle flow, PromptOptions handles menus
- Improved testability for menu construction
- Centralized CLI styling decisions
```
