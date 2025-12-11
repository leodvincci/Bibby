# CLI Command Spec – `book new`

## Purpose
Create a new book entry in the user’s library with minimum friction.

## Behavior
- Prompts for missing required fields (title, author).
- Displays a success message with the new book’s ID.
- [PLANNED] Validates ISBN via Isbn value object if provided.


## Edge Cases
- [PLANNED] If a book with the same title + author already exists,
    - Ask user if they want to:
        - [ ] create a duplicate
        - [ ] cancel

## History
- **v0.2.0** – Added `book new` command.
- **v0.5.0** – Updated description to “Create a new book entry”.
- **v0.6.0 [PLANNED]** – Add `--tags` option.