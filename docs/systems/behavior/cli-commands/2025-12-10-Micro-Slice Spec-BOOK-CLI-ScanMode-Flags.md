------

## Micro-Slice Spec

**Name:** `BOOK-CLI-ScanMode-Flags`
 **Area:** CLI → `book new` scan options
**Date:** December 10, 2025

------

### 1. Problem / Motivation (Original Issue)

Previously, the CLI used **String-based flags**, which meant:

- `--scan` required a *value* (unsupported for a boolean-style flag).
- Passing `--scan` with no value resulted in `null`, causing ambiguous routing.
- The CLI could not reliably distinguish:
  - **single-scan** vs.
  - **multi-scan**

Replacing string flags with **boolean flags** resolves this and makes the CLI behave as intended.

------

### 2. Desired Behavior (Updated CLI Contract)

**User-facing contract now behaves exactly as intended:**

1. `book new`
   - No flags.
   - Behavior: **Manual book registration**
     - Prompt for title, authors, ISBN, then create a `BookRequestDTO` and persist.
2. `book new --scan`
   - Meaning: “Scan **one** book.”
   - Behavior: **single-scan**
     - Display “Single Book Scan” header.
     - Run `scanBook("single")`.
3. `book new --scan --multi`
   - Meaning: “Scan **multiple** books.”
   - Behavior: **multi-scan**
     - Display “Multi-Book Scan” header.
     - Run `multiBookScan()`.

No values follow flags.
 Presence → `true`.
 Absence → `false`.

------

### 3. Decision Table / Truth Table

| `scan` | `multi` | Mode   | Behavior             |
| ------ | ------- | ------ | -------------------- |
| false  | false   | NONE   | Manual book creation |
| true   | false   | SINGLE | Single-book scan     |
| true   | true    | MULTI  | Multi-book scan      |

This table explicitly defines how the CLI should map raw flags → mode → action.

------

### 4. Implementation Thinking (No Code)

The CLI command must:

1. **Interpret raw Spring Shell flags**
   - `scan` and `multi` arrive as booleans automatically.
   - Map them to one of three conceptual modes:
      `NONE`, `SINGLE`, `MULTI`.
2. **Route based on mode**
   - `NONE` → manual creation
   - `SINGLE` → call the single-scan flow
   - `MULTI` → call the multi-scan flow
3. **Keep this logic inside the CLI/application boundary**
   - Domain should not know about flags.
   - Domain only receives structured DTOs or book lists.

This isolates input semantics (CLI flags) from domain logic.

------

### 5. Tests / Checks You Should Add

**Scenario 1 — Manual mode (`book new`)**

- `scan = false`, `multi = false`
- Should run manual creation flow only.
- Should *not* call scanBook or multiBookScan.

**Scenario 2 — Single scan (`book new --scan`)**

- `scan = true`, `multi = false`
- Should call `scanBook("single")` only.
- Should *not* run multiBookScan or manual prompts.

**Scenario 3 — Multi scan (`book new --scan --multi`)**

- `scan = true`, `multi = true`
- Should call `multiBookScan()` only.
- Should skip both manual and single-scan flows.

**Testing notes:**
 Mock/spy the following:

- `scanBook`
- `multiBookScan`
- `cliPrompt`
- `authorFacade`
- `bookFacade`

Assert correct invocation ordering based on the truth table.

------

### 6. Out of Scope (for this micro-slice)

- Changing manual registration flow UX
- Modifying domain objects or persistence logic
- Redesigning scanning algorithms themselves
- Adding new modes or default behaviors

This slice focuses *only* on clean input → correct mode → correct flow.

------

### 7. Notes / Future Refactor Hooks (Optional)

- Introduce:

  ```java
  enum ScanMode { NONE, SINGLE, MULTI }
  ```

  and centralize mode derivation to improve readability.

- Move manual registration logic into a helper method to reduce branching inside `registerBook`.

- Update CLI help text to reflect:

  - `--scan` → scan a single book
  - `--scan --multi` → scan multiple books

This keeps CLI documentation in sync with behavior.

