
![Adobe Express - Terminal_AI_Librarian_Animation (1)](https://github.com/user-attachments/assets/7fb5a30e-bef4-4ae4-bb65-675b80ce7d8f)


<!--
![BibbyLogo](https://github.com/user-attachments/assets/fc548a52-3855-4615-8639-0fc9d58ef6a4)


https://github.com/user-attachments/assets/b3d09ca7-bf11-45cf-9578-fab523e91975

-->
![Java](https://img.shields.io/badge/Java-17-blue?style=for-the-badge&logo=openjdk)
![Spring Shell](https://img.shields.io/badge/Spring%20Shell-Interactive%20CLI-brightgreen?style=for-the-badge&logo=spring)
![Status](https://img.shields.io/badge/Version-v0.2-orange?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-CLI%20Tools-yellow?style=for-the-badge)


---
# **Bibby — Personal Library CLI**

Bibby is a Spring Shell–powered command-line tool for managing my real-life library of physical books. It started as a sandbox for interactive CLI flows in Java, and somewhere along the way it became an actual system for tracking the books sitting on the shelves in my basement.

It organizes my library, one terminal prompt at a time — with a bit of personality sprinkled in.

------

## ⚙️ **What Bibby Does (Today)**

Bibby’s feature set is now centered on the **full browse + command workflow**:

### **Bookcase → Shelf → Book (New!)**

The browse flow now walks the user through:

1. Select a **Bookcase**
2. Select a **Shelf** within that bookcase
3. View and select **Books** from that shelf

This is powered by two new projections:

- `ShelfSummary` — shelfId, label, bookCount
- `BookSummary` — bookId, title

And two new ComponentFlow selectors for shelves and books.

### **Book Command Suite**

- Add books interactively
- Multi-author input
- Assign shelf locations (Book → Shelf → Bookcase)
- Check books in and out (with personality)
- Search by title/keyword
- List and rate books
- Browse using cascading selectors

Most flows use Spring Shell’s **ComponentFlow** to create stateful, prompt-driven UX.

------

## 🧠 **Why This Project Exists**

Bibby is deliberately built as a long-term practice arena where I can:

- Build expressive, stateful CLI flows
- Explore Spring Shell and ComponentFlow at depth
- Practice structured software design (commands, specs, domain rules)
- Design PostgreSQL-backed domain models (books, shelves, authors, bookcases)
- Build toward a full Spring Boot API + Bibby CLI frontend

The development rhythm stays consistent:
 **command → flow → service → repository → persistence**

------

## 🧩 **Example Commands**

```
book add
book search --title "Meditations"
book check-out --title "Sapiens"
book assign-shelf --title "Deep Work" --shelf "B2"
book list
browse bookcases   (new)
```

------

## 🏗️ **Project Structure**

```
src/
 ├── main/java/com/penrose/bibby/
 │    ├── BookCommands.java          # Book commands
 │    ├── BookcaseCommands.java      # Bookcase → Shelf → Book browse flow
 │    ├── CustomPromptProvider.java  # "Bibby:_" CLI prompt
 │    └── library/
 │         ├── book/                 # Entities, DTOs, BookSummary
 │         ├── author/
 │         ├── shelf/                # ShelfSummary, shelf services, repos
 │         └── bookcase/
 └── resources/
      └── application.yml
```

------

## 📘 **Documentation**

All documentation is maintained in Confluence:

- User stories
- Command specifications
- ComponentFlow designs
- ERDs, relationships, and domain rules
- Dev logs and micro-slice journals

Each command follows a consistent template: purpose, usage, flow, exceptions, domain rules.

------

## 🧾 **Current Status (Accurate as of Nov 15, 2025)**

- ✅ Multi-step browse flow: **Bookcase → Shelf → Books**
- ✅ ShelfSummary + BookSummary implemented
- 🚧 Extending ComponentFlow to handle empty shelf cases
- 🚧 CLI screens being improved (formatting, ANSI color, UX polish)
- 🗂️ Documentation updated with each micro-slice
- 🛢️ Database wiring in progress (Entities, repos, tested queries)
- 💡 Future features queued:
  - Better selectors (pagination, colors, animations)
  - “Librarian sass rotation”
  - Recommendations, stats, and analytics
  - AI-assisted shelf organization

Bibby is no longer just a sandbox — it’s turning into a small, expressive information system.

------

## 🪄 **Custom CLI Prompt**

```
return new AttributedString("Bibby:_ ",
    AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
```

Clean. Distinctive. Happily nerdy.

------

## 🧭 **Version**

**v0.2 — Bookcase → Shelf → Book navigation implemented**

------

## 🧱 **Dev Log Highlights**

### **2025-11-15 — Completed Shelf → Book Selection Flow**

- Added `BookSummary` record
- Created repository query for ordered shelf books
- Built book selector using ComponentFlow
- Connected cascade: Bookcase → Shelf → Book
- Handled empty-shelf UX (in progress)

### **2025-11-12 — Book Checkout (Persistent State)**

- Status tracking implemented
- Friendly librarian responses
- Persistence confirmed in PostgreSQL

### **2025-10-31 — Multi-Author Add Flow**

- Interactive author gathering
- Many-to-many relationship implemented
- Added transactional consistency

### **2025-10-31 — Shelf Assignment Flow**

- Select Bookcase → then Shelf
- Updated BookEntity.shelf
- Completed Book ↔ Shelf ↔ Bookcase model

Full commit history is available on GitHub.

------

## 👤 **Author**

**Leo D. Penrose**
 Builder • Systems Thinker • Lifelong Learner
 [LinkedIn] • [GitHub]
