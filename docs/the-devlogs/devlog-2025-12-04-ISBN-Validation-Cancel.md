# Devlog: ISBN Validation and Cancel Option

**Date:** 2025-12-04  
**Focus:** Adding input validation for ISBN entries and escape hatch for users  
**Commit Type:** `feat(cli)`

---

## Summary

Added ISBN format validation with inline retry and a `:q` command to cancel ISBN entry. Users now get immediate feedback on invalid ISBNs and can re-enter without restarting the command.

---

## What Changed

### ISBN Validator

**CliPromptService.java:**
```java
public boolean isbnValidator(String isbn){
    if(isbn.startsWith("978") && isbn.length() == 13){
        return true;
    }else if(isbn.equalsIgnoreCase(":q")){
        System.out.println("\u001B[31mISBN entry cancelled by user.\u001B[0m");
        return false;
    }else {
        System.out.println("\u001B[31mInvalid ISBN. Please enter a valid 13-digit ISBN starting with '978'.\u001B[0m");
        return false;
    }
}
```

**Validation rules:**
- Must start with "978" (ISBN-13 prefix)
- Must be exactly 13 characters
- `:q` exits gracefully

### Inline Retry in Prompt Flow

```java
.withStringInput("isbn")
.name("ISBN Number:_")
.next( ctx -> {
    String value = ctx.getResultValue();
    if(value.equalsIgnoreCase(":q")){
        return null;  // Exit flow
    }else if(isbnValidator(value)){
        return null;  // Valid, continue
    }
    // Invalid - reset and retry
    ctx.setResultValue(null);
    ctx.setInput("");
    ctx.setDefaultValue("");
    return "isbn";  // Loop back to same prompt
 })
```

**How it works:**
- `.next()` callback fires after input
- Return `null` → proceed to next step (or exit)
- Return `"isbn"` → repeat the ISBN prompt
- Context reset clears the invalid input

### Null Guards in Callers

**BookCommandLine.java:**
```java
// In scanBook()
if(!cliPrompt.isbnValidator(isbn)){
    return;
}

// In searchByIsbn()
if(isbn == null){
    System.out.println("NULL ISBN RETURNED");
    return;
}
```

---

## User Flow

### Valid ISBN
```
Single Book Scan
ISBN Number:_ 9780134685991
[proceeds to Google Books lookup]
```

### Invalid ISBN (retry)
```
Single Book Scan
ISBN Number:_ 12345
Invalid ISBN. Please enter a valid 13-digit ISBN starting with '978'.
ISBN Number:_ 9780134685991
[proceeds normally]
```

### Cancel with :q
```
Single Book Scan
ISBN Number:_ :q
ISBN entry cancelled by user.
[returns to prompt]
```

---

## Technical Decisions

### Why `:q` for Cancel?

Borrowed from vim/less—a familiar pattern for CLI power users. Alternatives considered:

| Option | Pros | Cons |
|--------|------|------|
| `:q` | Familiar to devs, short | Not obvious to casual users |
| `quit` | Clear intent | Could be mistaken for book title |
| `Ctrl+C` | Standard cancel | Kills entire shell session |
| Empty enter | Easy | Ambiguous—mistake or intentional? |

`:q` wins because it's unlikely to conflict with actual ISBN input and signals clear intent.

### ISBN-13 Only (For Now)

Current validation only accepts ISBN-13 (978 prefix, 13 digits). This covers most modern books but excludes:

- **ISBN-10**: Older books (10 digits, no prefix)
- **979 prefix**: Some newer ISBN-13s use 979 instead of 978

**Future enhancement:**
```java
public boolean isbnValidator(String isbn){
    // ISBN-13
    if((isbn.startsWith("978") || isbn.startsWith("979")) && isbn.length() == 13){
        return true;
    }
    // ISBN-10
    if(isbn.length() == 10 && isbn.matches("\\d{9}[\\dX]")){
        return true;
    }
    // ... error handling
}
```

For now, ISBN-13 covers the common case. Can expand when needed.

### Retry Loop via Flow Callback

Spring Shell's `.next()` callback enables inline retry without external loops:

```java
.next( ctx -> {
    if(valid) return null;      // Proceed
    ctx.setResultValue(null);   // Clear bad input
    return "isbn";              // Repeat this step
})
```

This keeps the retry logic contained within the flow definition rather than wrapping the whole thing in a while loop.

---

## Struggle Journal

### Challenge: Context Reset

Initial attempt just returned `"isbn"` to retry, but the previous invalid input persisted in the field. Had to explicitly clear:

```java
ctx.setResultValue(null);
ctx.setInput("");
ctx.setDefaultValue("");
```

All three are needed to fully reset the input state.

### Challenge: Double Validation

The validator runs twice in some paths:
1. Inside the `.next()` callback (for retry logic)
2. In `BookCommandLine.scanBook()` (for early return)

This is redundant but harmless—validation is cheap. Could refactor to single validation point, but the current approach is defensive.

### TODO: Remove Debug Output

Left a debug line in:
```java
System.out.println("Passes Here");
```

Need to remove before final commit.

---

## Interview Talking Points

### "How does the retry loop work without a while statement?"

> Spring Shell's flow builder has a `.next()` callback that fires after each input. If I return the current step's name, it loops back. If I return null, it proceeds. So instead of wrapping everything in a while loop, I use the framework's built-in flow control. The callback returns "isbn" to retry or null to continue.

### "Why validate ISBN format at all? The API will reject bad ones."

> Fail fast. If I can catch an obvious format error locally, why waste an API call? It's better UX too—the user sees "Invalid ISBN" immediately instead of waiting for a network round-trip to fail. Local validation handles the obvious cases; the API handles edge cases like valid format but non-existent ISBN.

### "How would you extend this to support ISBN-10?"

> Add a second condition: if length is 10 and matches the ISBN-10 pattern (9 digits plus a check digit that can be X). I'd use a regex like `\d{9}[\dX]`. The validator becomes a simple OR of the two formats. For now ISBN-13 covers most modern books, so I shipped the simpler version first.

---

## What's Next

1. **Remove debug output**: Delete "Passes Here" line
2. **ISBN-10 support**: Extend validator for older books
3. **979 prefix**: Some ISBN-13s use 979 instead of 978
4. **Checksum validation**: ISBN has a check digit—could validate mathematically
5. **Apply to multi-scan**: Add same validation to batch scanning flow

---

## Files Changed

| File | Changes |
|------|---------|
| `CliPromptService.java` | `isbnValidator()`, `.next()` retry callback |
| `BookCommandLine.java` | Null guards, validation calls, header styling |

---

## Feature Status

```
[✓] ISBN-13 format validation (978 prefix, 13 digits)
[✓] :q cancel command
[✓] Inline retry on invalid input
[✓] Red error messages for feedback
[✓] Null guards in callers
[ ] ISBN-10 support
[ ] 979 prefix support
[ ] Checksum validation
[ ] Apply validation to multi-scan mode
```
