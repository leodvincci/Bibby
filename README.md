
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

## 🧩 **Usage & Examples**

### **Interactive Flows**

Bibby's strength is in its conversational, multi-step flows powered by Spring Shell's ComponentFlow.

#### **Adding a Book**
```bash
Bibby:_ book add
```
This starts an interactive flow that walks you through:
1. Enter the book title
2. Specify number of authors
3. For each author, enter first and last name
4. Book is saved to the database

**Sample Session:**
```
Bibby:_ book add
Title:_ The Pragmatic Programmer
How many authors?:_ 2
Author's First Name:_ David
Author's Last Name:_ Thomas
Author's First Name:_ Andrew
Author's Last Name:_ Hunt

✓ "The Pragmatic Programmer" by David Thomas, Andrew Hunt added to your library.
```

#### **Browsing Your Library**
```bash
Bibby:_ browse bookcases
```
This initiates the cascading browse flow:
1. **Select a Bookcase** → displays all bookcases with book counts
2. **Select a Shelf** → shows shelves within that bookcase
3. **View Books** → lists all books on the selected shelf

**Use case:** Perfect for when you remember where a book is physically located but forgot the title.

#### **Checking Out a Book**
```bash
Bibby:_ book check-out --title "Sapiens"
```
Marks the book as checked out and increments the checkout counter. Bibby responds with personality:
```
"Sapiens" is ready for checkout. Try not to leave coffee rings on it this time.
```

#### **Assigning Shelf Locations**
```bash
Bibby:_ book assign-shelf --title "Deep Work"
```
Interactive flow to assign a physical location:
1. Select which bookcase
2. Select which shelf within that bookcase
3. Book location is updated

#### **Searching Your Library**
```bash
Bibby:_ book search --title "Meditations"
```
Searches by title keyword and returns matching results with authors and status.

#### **All Available Commands**
```bash
book add                    # Interactive book creation flow
book list                   # Display all books in your library
book search --title <text>  # Search by title keyword
book check-out --title      # Check out a book
book check-in --title       # Return a book
book assign-shelf --title   # Assign physical shelf location
browse bookcases            # Navigate Bookcase → Shelf → Books
```

------

## 🏗️ **Project Structure**

```
src/
 ├── main/java/com/penrose/bibby/
 │    ├── cli/
 │    │    ├── BookCommands.java          # Book command handlers
 │    │    ├── BookcaseCommands.java      # Browse flow commands
 │    │    ├── CustomPromptProvider.java  # "Bibby:_" CLI prompt
 │    │    └── LoadingBar.java            # CLI visual components
 │    ├── library/
 │    │    ├── book/                      # Book domain (Entity, Service, Repo, DTOs)
 │    │    ├── author/                    # Author domain
 │    │    ├── shelf/                     # Shelf domain + ShelfSummary projection
 │    │    ├── bookcase/                  # Bookcase domain
 │    │    └── catalog/                   # Catalog aggregation (future)
 │    └── BibbyApplication.java           # Spring Boot entry point
 └── resources/
      ├── application.properties           # DB config, JPA settings
      └── banner.txt                       # Custom ASCII art banner
```

------

## 🔧 **Technical Details**

### **Tech Stack**

| Layer              | Technology                          | Purpose                                    |
|--------------------|-------------------------------------|--------------------------------------------|
| **CLI Framework**  | Spring Shell 3.4.1                  | Interactive command-line interface         |
| **Backend**        | Spring Boot 3.5.7                   | Application framework & dependency injection |
| **Persistence**    | Spring Data JPA                     | ORM and repository abstractions            |
| **Database**       | PostgreSQL                          | Relational data storage                    |
| **Language**       | Java 17                             | Core language                              |
| **Build Tool**     | Maven                               | Dependency management & build automation   |

### **Architecture Overview**

Bibby follows a **layered architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────┐
│   CLI Layer (Commands + Flows)          │  ← User interaction via Spring Shell
├─────────────────────────────────────────┤
│   Service Layer                          │  ← Business logic & orchestration
├─────────────────────────────────────────┤
│   Repository Layer (Spring Data JPA)    │  ← Data access abstractions
├─────────────────────────────────────────┤
│   PostgreSQL Database                    │  ← Persistent storage
└─────────────────────────────────────────┘
```

**Key Design Patterns:**
- **Repository Pattern** — Spring Data JPA repositories for data access
- **DTO Pattern** — `BookRequestDTO`, `BookSummary`, `ShelfSummary` for data transfer
- **Service Layer Pattern** — Business logic isolated from CLI commands
- **Projection Pattern** — Lightweight read models for browse flows (e.g., `ShelfSummary`)

### **Database Schema**

The domain model captures a physical library with hierarchical organization:

```
Bookcase (1) ──────< Shelf (many)
                        ↓
                    Book (many)
                        ↓
                   (many-to-many)
                        ↓
                    Author (many)
```

**Core Tables:**

- **`books`** — title, isbn, publisher, publicationYear, genre, edition, description, status, checkoutCount, shelfId
- **`authors`** — firstName, lastName
- **`book_authors`** — join table for many-to-many relationship
- **`shelves`** — shelfLabel, shelfPosition, bookcaseId
- **`bookcases`** — label, location metadata

**Key Relationships:**
- A **Book** can have multiple **Authors** (many-to-many via `book_authors`)
- A **Book** belongs to one **Shelf** (many-to-one)
- A **Shelf** belongs to one **Bookcase** (many-to-one)

**Status Tracking:**
- Books track `bookStatus` (available, checked_out, reading)
- `checkoutCount` increments each time a book is checked out
- Timestamps: `createdAt`, `updatedAt`

### **Spring Shell Integration**

Bibby leverages **ComponentFlow** for multi-step, interactive workflows:

- **StringInput** — for titles, author names
- **SingleItemSelector** — for selecting from bookcases, shelves, books
- **NumberInput** — for specifying author counts

Example flow architecture (from `book add`):
```java
ComponentFlow flow = componentFlowBuilder.clone()
    .withStringInput("title")
        .name("Title:_")
    .and()
    .withNumberInput("authorCount")
        .name("How many authors?:_")
    .and().build();
```

This creates a stateful, conversational interface that feels less like running commands and more like having a dialogue.

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
