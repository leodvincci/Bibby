# 📘 **BIBBY DEV LOG — Bug Fix: Shelf Assignment Wrong Due to Using Shelf Position Instead of Shelf ID**

**Date:** 2025-11-13
 **Author:** Leo D. Penrose

------

## **1. Summary of the Bug**

While testing the new Bookcase Browser and Shelf Selector, I noticed that newly-added books were **not appearing on the correct shelf** inside the selector.

After placing a book on a shelf, the book failed to show under that shelf in the browser. Instead, the book seemed to disappear or appear on a random shelf.

This led to discovering a deeper issue in the shelf assignment logic.

------

## **2. Root Cause**

Bibby stored the **shelf position** (the *index* of the shelf inside a bookcase) instead of the **shelf ID** (the *actual primary key* in the database).

### Example of the problem:

A book assigned to *Shelf Position 3* would be saved with:

```
shelfId = 3
```

But that is wrong.

Each bookcase has its own shelf IDs:

- Basement Shelf 3 might be **ID 152**
- Office Shelf 3 might be **ID 87**
- Bedroom Shelf 3 might be **ID 201**

By saving “3,” Bibby was pointing books to **Shelf ID 3** globally — not the shelf inside the intended bookcase.

This caused books to be placed on the wrong shelf entity entirely.

------

## **3. How I Diagnosed It**

1. After adding a book, I placed it on a shelf to test if the Bookcase Browser showed the new book count.

2. It didn't.

3. I checked the database:

   - The `shelf_id` stored in the book record was incorrect.

4. I looked at the CLI code that maps shelf selection → shelf ID:

   ```
   options.put(s.getShelfLabel(), String.valueOf(s.getShelfPosition()));
   ```

5. That meant the UI was returning a **shelf position**, not a **shelf ID**.

6. That incorrect value ended up being written as the shelf ID on the book.

The bug was subtle because:

- Shelf positions *look similar* to shelf IDs numerically.
- The app doesn’t validate the ID against the bookcase.
- The UI worked for selecting—but returned the wrong identifier.

------

## **4. The Fix**

Changed the UI option mapping:

### **Before (wrong):**

```
options.put(s.getShelfLabel(), String.valueOf(s.getShelfPosition()));
```

### **After (correct):**

```
options.put(s.getShelfLabel(), String.valueOf(s.getShelfId()));
```

Also printed extra debug info to confirm:

```
System.out.println(s.getBookcaseId());
System.out.println("BOOK CASE ID: " + bookCaseId);
```

Everything now lines up correctly:

- Books are mapped to the correct shelf ID.
- Shelf selector displays the correct books.
- Bookcase counters update properly.

------

## **5. Why This Bug Mattered**

This wasn’t just a UI bug — it threatened the **integrity of the entire data model**:

- Shelves were being treated globally instead of per-bookcase.
- Books were assigned to the wrong bookcases.
- The selector couldn't find them.
- Browsers showed incorrect counts.
- The relationship graph (Book → Shelf → Bookcase) was broken.

Fixing this restored the causal chain in Bibby’s domain model:

```
Book ⟶ Shelf ⟶ Bookcase
```

------

## **6. Lessons Learned**

### ✔ ID ≠ Position

IDs are global primary keys.
 Positions are display-order context.

Never interchange them.

### ✔ UI should return stable identifiers

The CLI selector must always return the **database ID**, not a derived or displayed field.

### ✔ Internal consistency depends on correct wiring

This bug showed how:

- One incorrect mapping
- Leads to incorrect writes
- Leads to invisible or missing data
- Leads to broken selectors
- Leads to deep debugging
   All due to a single inappropriate value.

### ✔ Debug Prints Help

Temporarily printing:

```
BOOK CASE ID: X
```

and

```
s.getBookcaseId()
```

helped verify the correct flow.

------

## **7. Additional Improvement Made**

Adjusted table formatting width:

```
%-9s  →  %-12s
```

This prevents label truncation and aligns columns better visually.

------

## **8. Next Steps**

- Finish Shelf Selector slice (now unblocked thanks to correct shelf IDs).
- Build Shelf Browser to show books per shelf.
- Add visual confirmation when placing/moving books.
- Eventually remove debug prints once stable.

------

## **9. Final Thought**

This bug fix reinforces that Bibby’s architecture is maturing.
 You’re no longer just adding features — you’re **protecting the integrity of the system**, exactly like a real enterprise domain model.

This was a foundational bug, and you crushed it.