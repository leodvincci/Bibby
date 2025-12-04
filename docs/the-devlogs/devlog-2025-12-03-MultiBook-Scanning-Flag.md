# Devlog: Multi-Book Scanning Flag

**Date:** 2025-12-03  
**Focus:** Adding command-line flag to support batch ISBN scanning  
**Commit Type:** `feat(cli)`

---

## Summary

Extended the `book scan` command with a `--type` flag to support both single-book and multi-book scanning modes. This is scaffolding for a batch scanning workflow where users can continuously scan ISBNs without restarting the command.

---

## What Changed

### Before
```bash
bibby> book scan
# Always scans one book, then returns to prompt
```

### After
```bash
bibby> book scan                    # Defaults to single
bibby> book scan --type single      # Explicit single-book mode
bibby> book scan --type multi       # Multi-book mode (pending implementation)
```

---

## Implementation

### Command Signature Update

```java
@Command(command = "scan", description = "Scan a book's ISBN to add it to your library database", group = "Book Commands")
public void scanBook(
    @Option(required = false, defaultValue = "single", description = "scan multiple books") 
    @ShellOption(value = {"--type"}) String multi
) {
    // ...
}
```

**Key annotations:**
- `@Option(required = false, defaultValue = "single")` - Makes flag optional with sensible default
- `@ShellOption(value = {"--type"})` - Defines the flag name

### Branching Logic

```java
if(multi.equalsIgnoreCase("multi")){
    System.out.println("Scanning multiple books...");
    // TODO: Implement loop
}

if(multi.equalsIgnoreCase("single")) {
    System.out.println("Scanning single book...");
    String isbn = cliPrompt.promptForIsbnScan();
    // ... existing flow
}
```

---

## Design Intent

### Why a Flag Instead of Separate Commands?

Considered alternatives:
1. `book scan` vs `book scan-multi` (separate commands)
2. `book scan` vs `book batch-scan` (separate commands)
3. `book scan --type single|multi` (flag on single command) ✓

**Chose option 3 because:**
- Single command is easier to discover (`book scan --help` shows all options)
- Shared implementation—both modes use the same underlying ISBN lookup and persistence
- Mirrors common CLI patterns (e.g., `git log --oneline` vs `git log`)

### Planned Multi-Book Flow

```
bibby> book scan --type multi
Scanning multiple books...
[Scan ISBN or type 'done' to finish]

ISBN:_ 9780134685991
📚 "Effective Java" by Joshua Bloch - Added!

ISBN:_ 9780596009205  
📚 "Head First Design Patterns" by Eric Freeman - Added!

ISBN:_ done
✓ Added 2 books to your library.
```

The loop will:
1. Continuously prompt for ISBNs
2. Look up and display each book
3. Auto-confirm (or quick confirm) each entry
4. Track count and display summary on exit

---

## Struggle Journal

### Challenge: Spring Shell Option vs ShellOption

Spring Shell has two annotation styles that can be confusing:

```java
// Newer style (Spring Shell 3.x)
@Option(required = false, defaultValue = "single")

// Legacy style (still works)
@ShellOption(value = {"--type"})
```

I ended up using both because:
- `@Option` handles the `required` and `defaultValue` cleanly
- `@ShellOption` was needed to define the actual flag name `--type`

**TODO**: Research if there's a cleaner single-annotation approach in Spring Shell 3.x.

### Challenge: Null Handling

```java
multi = multi == null ? "multi" : multi;
```

This line is defensive but odd—if `multi` is null, it defaults to "multi"? That seems backwards given `defaultValue = "single"`.

**What happened**: Experimented with different default behaviors during development. This line is likely vestigial and should be removed or corrected.

**Correct behavior**: If `defaultValue = "single"` works properly, this null check is unnecessary. If it doesn't, the fallback should also be "single".

---

## Technical Notes

### Commented-Out Experimental Code

```java
//    @Command(command = "", description = "...")
//    public void scanMultiBook(@ShellOption(defaultValue = "leo", value = {"--multi"}) String multi) {
//        ...
//    }
```

This was an alternative approach—a separate command entirely for multi-book scanning. Decided against it in favor of the flag approach, but left the code as reference for the annotation syntax.

**TODO**: Remove commented code before merging to main.

---

## Interview Talking Points

### "How did you decide between a flag and separate commands?"

> I considered both approaches. Separate commands (`scan` vs `batch-scan`) would be more explicit, but a flag keeps related functionality together. When users run `book scan --help`, they see all scanning options in one place. It also means shared implementation—both modes use the same ISBN lookup, the same persistence logic. The flag just controls the loop behavior.

### "What's your plan for the multi-book implementation?"

> The multi mode will run a loop that continuously prompts for ISBNs until the user types 'done' or hits a special key. Each scan will do the API lookup and show a condensed confirmation—probably auto-accept with just the title displayed, since batch mode implies the user wants speed. At the end, we'll show a summary: "Added 5 books to your library."

---

## What's Next

1. **Implement multi-book loop**: Continuous scanning until exit signal
2. **Condensed output for batch mode**: Show just title + author, skip full metadata display
3. **Summary on completion**: "Added N books to your library"
4. **Error resilience**: If one ISBN fails, continue to next (don't break the loop)
5. **Clean up**: Remove null-check workaround, remove commented experimental code

---

## Files Changed

- `BookCommandLine.java`: Added `--type` flag, branching logic, commented experiments

---

## Feature Status

```
[✓] Single-book scanning (existing, working)
[✓] --type flag parsing
[~] Multi-book mode (scaffolded, not implemented)
[ ] Continuous scanning loop
[ ] Batch-mode condensed output
[ ] Completion summary
[ ] Error handling for failed lookups in batch
```
