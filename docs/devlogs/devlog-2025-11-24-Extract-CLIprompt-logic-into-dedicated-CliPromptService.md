## Devlog Entry - [Date: 2025-11-24]

### Refactor: Extract CLI prompt logic into dedicated CliPromptService

**What Changed:**

- Created `CliPromptService` class to centralize all ComponentFlow prompt building
- Extracted 8 prompt methods from `BookCommands`:
  - `promptForBookTitle()` - returns String
  - `promptForBookAuthorCount()` - returns int
  - `promptForAuthor()` - returns Author domain object
  - `promptForSearchType()` - returns String
  - `promptForBookCase()` - returns Long
  - `promptForShelf(Long bookCaseId)` - returns Long
  - `promptSearchAgain()` - returns boolean
  - Moved helper methods: `buildSearchOptions()`, `bookCaseOptions()`, `bookShelfOptions()`, `yesNoOptions()`
- Simplified `BookCommands` methods significantly:
  - `addBook()`: Removed ~30 lines of ComponentFlow boilerplate
  - `searchByTitle()`: Cleaner flow with single prompt call
  - `searchByAuthor()`: Same - much more readable
  - `addToShelf()`: Reduced from complex nested flows to clear sequential prompts
  - `checkOutBook()`, `checkInBook()`: Simplified prompt logic
- Added convenience method `LoadingBar.showLoading()` to wrap common loading bar usage
- Reorganized `BookCommands` with clear section headers:
  - Book Create Commands
  - Book Search Commands
  - Book Update Commands

**Why:**

- BookCommands was violating Single Responsibility Principle - it was both orchestrating business logic AND building UI prompts
- Repetitive ComponentFlow building scattered throughout every command method
- Hard to read the actual business logic through all the UI code
- Difficult to maintain consistent prompt behavior

**Result:** Command methods are now **dramatically** more readable. Compare before/after:

java

```java
// Before:
ComponentFlow flow = componentFlowBuilder.clone()
    .withStringInput("title")
    .name("Book Title:_")
    .and()
    .build();
ComponentFlow.ComponentFlowResult result = flow.run();
String title = result.getContext().get("title", String.class);

// After:
String title = cliPrompt.promptForBookTitle();
```

This is a huge win for maintainability. The intent of each command is now crystal clear.

------

### Known Issues / Tech Debt Created:

**1. Dependency Injection Smell (HIGH PRIORITY)**

- `CliPromptService` now depends on `BookcaseService` and `ShelfService`
- This couples the UI layer to business services
- Makes testing harder (need to mock services to test prompts)
- VIOLATES dependency rule: UI should not depend on domain services

**Solution to implement later:**

- Pass data INTO prompt methods, don't fetch it inside them
- Example: `promptForBookCase(List<BookcaseEntity> bookcases)` instead of fetching bookcases inside the method
- Keep CliPromptService **pure** - only concerned with prompting, not data fetching

**2. Input Validation Missing**

- `promptForBookAuthorCount()` calls `Integer.parseInt()` with no error handling
- Will crash on non-numeric input
- Need to add validation loop with try-catch

**3. Incomplete Refactoring**

- Still have inline ComponentFlow building in `checkInBook()` (confirmation prompt)
- Still have inline ComponentFlow building in `askBookCheckOut()`
- Should extract these to CliPromptService too

**4. Code Duplication**

- `yesNoOptions()` method exists in BOTH BookCommands and CliPromptService
- Should only be in one place (likely CliPromptService as private helper)

**5. Constructor Parameter Duplication**

- CliPromptService constructor has `ComponentFlow.Builder` parameter listed twice with different names
- This is clearly a mistake - clean it up

------

### What I Learned:

**Refactoring can shift complexity rather than reduce it:**

- I successfully made BookCommands cleaner
- But I created new coupling in CliPromptService by injecting services
- This is an example of moving a problem rather than solving it
- The right solution: make CliPromptService accept data, not services

**Code organization matters:**

- Adding section comments in BookCommands made a huge difference
- Public methods first, private helpers after - this ordering is important
- Clear, consistent naming (`promptFor*`) makes the API predictable

**Incremental refactoring is okay:**

- I didn't extract EVERY ComponentFlow usage
- That's fine - I can finish it later
- The 80% improvement is still worth committing

------

### Next Steps:

1. Fix CliPromptService dependency injection (pass data, not services)
2. Add input validation to `promptForBookAuthorCount()`
3. Remove `yesNoOptions()` duplication
4. Extract remaining inline ComponentFlows from `checkInBook()` and `askBookCheckOut()`
5. Fix constructor parameter duplication
6. Add tests for CliPromptService (once dependencies are fixed)

------

### Reflection:

This refactoring taught me an important lesson about **coupling vs cohesion**. I improved cohesion (each class has a clearer single purpose) but accidentally increased coupling (CliPromptService now depends on too much).

The sign of good architecture isn't just "does it look cleaner?" but "can I test this easily?" and "what happens if requirements change?" When I ask those questions, I can see the dependency injection issue clearly.

I'm getting better at recognizing code smells BEFORE they become big problems. Old me would have left BookCommands as a 500+ line mess. Current me knows when to extract and refactor. Next-level me will know how to extract WITHOUT creating new coupling issues.

Progress, not perfection.