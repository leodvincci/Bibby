## 🗓️ **Devlog — November 12, 2025**

### **Feature:** Guard Against Double Checkouts

**Branch:** `feat/cli-checkout-guard`
**Commit:** `feat(checkout): add CLI-level guard for already checked-out books`

------

### 🎯 **Objective**

Prevent users from checking out a book that’s already marked as `CHECKED_OUT`. This is a UX-first slice: stopping the behavior early in the CLI and giving the librarian a fittingly judgmental message.

------

### ⚙️ **What I Implemented**

- Added a simple **CLI-level guard** inside the `book check-out` command.
- If a book is already checked out, Bibby now stops the flow immediately.
- Introduced a tailored librarian message:

```
</>: This one’s already off the shelf. No double-dipping on checkouts.
```

- Kept service-layer logic unchanged for now—domain invariants will be a separate micro-slice.

------

### 🧠 **Why This Slice Matters**

This change is about tightening user experience, not the architecture—yet.

It makes Bibby feel coherent and intentional:

- No silent successes.
- No confusing re-checkouts.
- Clear feedback with attitude.

This sets the stage for the next slice:
**moving the rule into the service layer so all future interfaces (REST, batch jobs, admin tools) inherit the same invariant.**

------

### 💬 **Sample Output**

```
Guest </>BIBBY:_  book check-out
? Book Title: a first course in database systems
</>: This one’s already off the shelf. No double-dipping on checkouts.
```

------

### 🧩 **Next Planned Slice**

- **Domain-Level Checkout Invariant**
    - Move the `CHECKED_OUT → error` rule into `BookService`.
    - Introduce `BookAlreadyCheckedOutException`.
    - CLI simply reacts—service enforces the rule.

------

### 💭 **Reflection**

Even a tiny slice like this changes how Bibby feels.
The system becomes less mechanical and more opinionated.
That’s the identity I want for Bibby: technically precise, quietly humorous, and always protective of the library.