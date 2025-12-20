### Purpose of the Scan Book feature
2025-12-19
- What it is: A CLI workflow (command `scan`) that lets you add a physical book to the library by scanning its ISBN barcode and walking through placement on shelves.
- Primary goals:
    - Capture a book’s ISBN from a scanner/prompt and validate it.
    - Fetch trusted metadata (title, authors, publisher, description, etc.) from the cataloging layer based on that ISBN.
    - Let the librarian confirm the match, then choose where the book lives in the stacks (location → bookcase → shelf).
    - Ensure the book’s authors exist in the system (create them if needed) and persist the new Book record with its shelf placement.
    - Show a human‑readable “book card” before and after saving so you can verify details and where it was shelved.

### How it works at a glance

- Entry point: `BookCommands.scanBook` in `src/main/java/com/penrose/bibby/cli/commands/book/BookCommands.java`.
- Flow:
    1. Prompt for ISBN scan: `cliPrompt.promptForIsbnScan()` → validate with `cliPrompt.isbnValidator(...)`.
    2. Lookup metadata: `bookFacade.findBookMetaDataByIsbn(isbn)`.
    3. Show a "book card" with metadata and placeholders for placement (bookcase/shelf/location = PENDING).
    4. On confirmation: prompt for placement
        - Choose location → bookcase (via `promptOptions.bookCaseOptionsByLocation(location)` and `cliPrompt.promptForBookCase(...)`)
        - Choose shelf (`cliPrompt.promptForShelf(bookcaseId)`).
    5. Ensure authors exist from metadata: `createAuthorsFromMetaData(...)` → returns author IDs (creates missing ones as needed).
    6. Persist the book: `bookFacade.createBookFromMetaData(...)` with author IDs, ISBN, and shelf ID.
    7. Print an updated "book card" with the actual bookcase/shelf/location labels (via `bookcaseFacade` and `shelfFacade`).

### Outcomes and scope

- Result: A new, fully registered Book linked to its authors and assigned to a specific shelf within a bookcase at a location.
- User value: Rapid cataloging from a barcode scan with minimal typing and fewer data entry errors.
- Notes/limits:
    - The `multi` (multi‑scan) path is present in the signature but currently not enabled (commented out).
    - There is also a separate `suggest-shelf` command stub for future AI placement suggestions (not part of the scan flow today).

### Where to look in code

- `BookCommands.scanBook(...)` — orchestrates the entire flow (validation, metadata, placement, save).
- Collaborators:
    - `CliPromptService` — prompts and ISBN validation.
    - `BookFacade` — metadata lookup and create-from-metadata.
    - `PromptOptions`, `BookcaseFacade`, `ShelfFacade` — placement selection and label lookups.
    - Local helpers like `createAuthorsFromMetaData(...)` — ensures author records exist.