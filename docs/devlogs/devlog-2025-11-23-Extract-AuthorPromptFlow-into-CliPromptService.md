# Micro-Slice Spec: Extract Author Prompt Flow into `CliPromptService`

## 1. Context

Previously, `BookCommands` owned a private helper method:

```
public Author authorNameComponentFlow(String title) { ... }
```

This method:

- Built a `ComponentFlow`
- Prompted for first/last name
- Returned an `Author` domain object

You’re now starting to centralize CLI prompting logic in a dedicated **`CliPromptService`**, so commands don’t have to manually construct flows every time.

This slice is the first step: **move the “prompt for author” flow out of `BookCommands` and into `CliPromptService`.**

------

## 2. Goal / Outcome

**Goal:**
 Replace the inline, command-specific author prompt logic with a reusable service method so that:

- `BookCommands` is slimmer and more focused on orchestration.
- Author-prompt behavior is reusable from other commands.
- CLI UX for author prompts can later be improved in one place.

**Done looks like:**

- `BookCommands` no longer contains `authorNameComponentFlow`.
- `BookCommands` depends on `CliPromptService`.
- When adding a book, authors are collected via `cliPrompt.promptForAuthor(title)` and everything still works exactly as before from the user’s perspective.

------

## 3. Code Changes (What This Slice Actually Did)

### 3.1. Inject `CliPromptService` into `BookCommands`

Constructor updated:

```
final CliPromptService cliPrompt;

public BookCommands(
        ComponentFlow.Builder componentFlowBuilder,
        BookService bookService,
        BookController bookController,
        BookcaseService bookcaseService,
        ShelfService shelfService,
        AuthorService authorService,
        CliPromptService cliPrompt
) {
    this.componentFlowBuilder = componentFlowBuilder;
    this.bookService = bookService;
    this.bookController = bookController;
    this.bookcaseService = bookcaseService;
    this.shelfService = shelfService;
    this.authorService = authorService;
    this.cliPrompt = cliPrompt;
}
```

### 3.2. Remove `authorNameComponentFlow` from `BookCommands`

Old method (now deleted):

```
public Author authorNameComponentFlow(String title){
    ComponentFlow flow2;
    flow2 = componentFlowBuilder.clone()
            .withStringInput("authorFirstName")
            .name("Author's First Name:_")
            .and()
            .withStringInput("authorLastName")
            .name("Author's Last Name:_")
            .and().build();

    ComponentFlow.ComponentFlowResult result = flow2.run();
    String firstName  = result.getContext().get("authorFirstName", String.class);
    String lastName = result.getContext().get("authorLastName", String.class);

    return new Author(firstName,lastName);
}
```

This logic now lives in `CliPromptService` (not shown in the diff, but implied by the usage).

### 3.3. Use `CliPromptService` in `addBook`

Before:

```
List<Author> authors = new ArrayList<>();
for (int i = 0; i < authorCount; i++) {
    authors.add(authorNameComponentFlow(title));
}
```

After:

```
List<Author> authors = new ArrayList<>();
for (int i = 0; i < authorCount; i++) {
    authors.add(cliPrompt.promptForAuthor(title));
}
```

Behavior is the same: for each author in `authorCount`, we interactively prompt for first/last name and get an `Author` back. But the knowledge of *how* to prompt lives in `CliPromptService`.

------

## 4. Design Intent

**Why this change?**

- **Single Responsibility**: `BookCommands` should orchestrate flows, not own the exact mechanics of every CLI prompt.
- **Reuse**: Other commands (future: edit book, add author independently, etc.) should be able to reuse the same “prompt user for author” interaction.
- **Consistency**: If you later tweak the prompt text, validation, or add extra fields (middle name, suffix, etc.), you do it **once** in `CliPromptService`.

**Conceptual responsibilities:**

- `BookCommands`
  - Decides *when* to ask for authors.
  - Decides *how many* authors to collect.
  - Passes context (e.g., book title) into the prompt service.
- `CliPromptService`
  - Knows *how* to prompt the user for an author.
  - Owns the `ComponentFlow` setup for author input.
  - Returns a domain-friendly `Author` object.

------

## 5. Scope

### In Scope

- Remove inline author prompt helper from `BookCommands`.
- Inject and use `CliPromptService` to get `Author` instances.
- Keep behavior identical from the user’s point of view (same questions, same flow).

### Out of Scope (Future Slices)

- Adding validation or richer UX to author prompts.
- Centralizing other prompts (title, ISBN, shelves, etc.) into `CliPromptService`.
- Refactoring `CliPromptService` into multiple specialized prompt helpers if it grows too large.

------

## 6. Acceptance Criteria

1. **Behavior parity**

   From the user’s perspective:

   - Running the `book add` command still:
     - Prompts for book title.
     - Prompts for number of authors.
     - Prompts for each author’s first and last name.
   - Books are still created with the same author data as before.

2. **Code-level expectations**

   - `BookCommands` no longer contains `authorNameComponentFlow`.
   - `CliPromptService` exposes `Author promptForAuthor(String title)` (or equivalent) and is called in the `for` loop.
   - `BookCommands` constructor includes `CliPromptService` and Spring can construct the bean without errors.

3. **Compile & run**

   - The app compiles successfully.
   - `BookCommands` bean is created properly by Spring (no missing dependency).
   - The `addBook` flow runs end-to-end without exceptions.