
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
**A Spring Shell–powered command-line library management system for organizing physical books.**

Bibby is an interactive CLI tool for managing a real-life library of physical books. It started as a sandbox for exploring interactive CLI flows in Java, and evolved into a full-fledged system for tracking books, shelves, and bookcases — with personality sprinkled in.

It organizes your library, one terminal prompt at a time.

---

## 🎯 What Bibby Does

Bibby's feature set is centered on the **browse + command workflow**:

### Browse Flow

The browse flow walks you through:
- **Select a Bookcase**
- **Select a Shelf** within that bookcase
- **View and select Books** from that shelf

This is powered by lightweight projections:
- `ShelfSummary` — shelfId, label, bookCount
- `BookSummary` — bookId, title

And cascading `ComponentFlow` selectors for shelves and books.

### Core Features

- ✅ **Add books interactively** with multi-author input
- ✅ **Assign shelf locations** (Book → Shelf → Bookcase)
- ✅ **Check books in and out** (with personality)
- ✅ **Search by title/keyword**
- ✅ **List and rate books**
- ✅ **Browse using cascading selectors**

Most flows use Spring Shell's `ComponentFlow` to create stateful, prompt-driven UX.

---

## 🎓 Why Bibby Exists

Bibby is deliberately built as a **long-term practice arena** for software engineering fundamentals:

- Build expressive, stateful CLI flows with Spring Shell
- Practice **Domain-Driven Design** with entity/domain separation
- Implement clean architecture with proper separation of concerns
- Design PostgreSQL-backed domain models (books, shelves, authors, bookcases)
- Develop comprehensive unit tests with JUnit 5 and Mockito
- Follow systematic refactoring and architectural evolution

**The development rhythm:** command → flow → service → domain → repository → persistence

Bibby serves as a hands-on learning project focused on mastering software engineering principles through practice rather than having tools build it.

---

## 🚀 Quick Start Examples

### Adding a Book

```
Bibby:_ book add
```

This starts an interactive flow:
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

### Browsing by Location

```
Bibby:_ browse bookcases
```

This initiates the cascading browse flow:
1. **Select a Bookcase** → displays all bookcases with book counts
2. **Select a Shelf** → shows shelves within that bookcase  
3. **View Books** → lists all books on the selected shelf

**Use case:** Perfect for when you remember where a book is physically located but forgot the title.

### Checking Out a Book

```
Bibby:_ book check-out --title "Sapiens"
```

Marks the book as checked out and increments the checkout counter. Bibby responds with personality:

```
"Sapiens" is ready for checkout. Try not to leave coffee rings on it this time.
```

### Assigning a Shelf Location

```
Bibby:_ book assign-shelf --title "Deep Work"
```

Interactive flow to assign a physical location:
1. Select which bookcase
2. Select which shelf within that bookcase
3. Book location is updated

### Searching for Books

```
Bibby:_ book search --title "Meditations"
```

Searches by title keyword and returns matching results with authors and status.

---

## 📋 Available Commands

```bash
book add                    # Interactive book creation flow
book list                   # Display all books in your library
book search --title <text>  # Search by title keyword
book check-out --title      # Check out a book
book check-in --title       # Return a book
book assign-shelf --title   # Assign physical shelf location
browse bookcases            # Navigate Bookcase → Shelf → Books
```

---

## 🏗️ Project Structure

```
src/
├── main/java/com/penrose/bibby/
│   ├── cli/
│   │   ├── BookCommands.java         # Book command handlers
│   │   ├── BookcaseCommands.java     # Browse flow commands
│   │   ├── CustomPromptProvider.java # "Bibby:_" CLI prompt
│   │   └── LoadingBar.java           # CLI visual components
│   ├── library/
│   │   ├── book/        # Book domain (Domain Model, Entity, Service, Repo, DTOs)
│   │   ├── author/      # Author domain
│   │   ├── shelf/       # Shelf domain + ShelfSummary projection
│   │   ├── bookcase/    # Bookcase domain
│   │   └── catalog/     # Catalog aggregation (future)
│   └── BibbyApplication.java  # Spring Boot entry point
└── resources/
    ├── application.properties # DB config, JPA settings
    └── banner.txt             # Custom ASCII art banner
```

**Package-by-Feature Organization:** Each domain (book, author, shelf, bookcase) contains all related components in one package.

---

## 🛠️ Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| CLI Framework | Spring Shell 3.4.1 | Interactive command-line interface |
| Backend | Spring Boot 3.5.7 | Application framework & dependency injection |
| Persistence | Spring Data JPA | ORM and repository abstractions |
| Database | PostgreSQL | Relational data storage |
| Language | Java 17 | Core language |
| Testing | JUnit 5 & Mockito | Unit testing framework |
| Build Tool | Maven | Dependency management & build automation |

---

## 🏛️ Architecture

Bibby follows a **layered architecture with Domain-Driven Design principles**:

```
┌─────────────────────────────────────────┐
│   CLI Layer (Commands + Flows)         │ ← User interaction via Spring Shell
├─────────────────────────────────────────┤
│   Service Layer                         │ ← Business logic orchestration
├─────────────────────────────────────────┤
│   Domain Model Layer                    │ ← Business rules & invariants
├─────────────────────────────────────────┤
│   Repository Layer (Spring Data JPA)   │ ← Data access abstractions
├─────────────────────────────────────────┤
│   Entity Layer                          │ ← Persistence mapping
├─────────────────────────────────────────┤
│   PostgreSQL Database                   │ ← Persistent storage
└─────────────────────────────────────────┘
```

### Key Design Patterns

- **Domain-Driven Design** — Separate domain models from persistence entities
- **Repository Pattern** — Spring Data JPA repositories for data access
- **DTO Pattern** — `BookRequestDTO`, `BookSummary`, `ShelfSummary` for data transfer
- **Service Layer Pattern** — Business logic isolated from CLI commands
- **Projection Pattern** — Lightweight read models for browse flows

### Entity/Domain Separation

Bibby uses **full entity/domain separation** to maintain clean architecture:

- **Domain Models** — Contain business logic, validation, and invariants (e.g., `Book`, `Author`)
- **Entities** — Handle persistence mapping only (e.g., `BookEntity`, `AuthorEntity`)
- **Synchronization** — Services coordinate between domain and entity layers

This separation ensures business logic stays independent of persistence concerns.

---

## 📊 Domain Model

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

### Core Tables

- `books` — title, isbn, publisher, publicationYear, genre, edition, description, status, checkoutCount, shelfId
- `authors` — firstName, lastName
- `book_authors` — join table for many-to-many relationships
- `shelves` — shelfLabel, shelfPosition, bookcaseId
- `bookcases` — label, location metadata

### Key Relationships

- A **Book** can have multiple **Authors** (many-to-many via `book_authors`)
- A **Book** belongs to one **Shelf** (many-to-one)
- A **Shelf** belongs to one **Bookcase** (many-to-one)

### Status Tracking

- Books track `bookStatus` (available, checked_out, reading)
- `checkoutCount` increments each time a book is checked out
- Timestamps: `createdAt`, `updatedAt`

---

## 🔄 ComponentFlow Architecture

Bibby leverages `ComponentFlow` for multi-step, interactive workflows:

- **StringInput** — for titles, author names
- **SingleItemSelector** — for selecting from bookcases, shelves, books
- **NumberInput** — for specifying author counts

**Example flow architecture** (from `book add`):

```java
ComponentFlow flow = componentFlowBuilder.clone()
    .withStringInput("title")
        .name("Title:_")
        .and()
    .withNumberInput("authorCount")
        .name("How many authors?:_")
        .and()
    .build();
```

This creates a stateful, conversational interface that feels less like running commands and more like having a dialogue.

---

## 🧪 Testing & Quality

Bibby includes comprehensive unit testing:

- **JUnit 5** for test structure and assertions
- **Mockito** for mocking dependencies
- **Focus areas:** Domain model validation, service layer logic, entity-domain synchronization

**Testing philosophy:** Write tests that verify business logic and catch architectural issues early.

---

## 📚 Documentation

All comprehensive documentation is maintained in **Confluence**:

- User stories
- Command specifications
- ComponentFlow designs
- ERDs, relationships, and domain rules
- Dev logs and architectural decision records
- Micro-slice journals tracking development progress

Each command follows a consistent template: **purpose, usage, flow, exceptions, domain rules**.

---

## 🚧 Current Status & Roadmap

### ✅ Completed

- Multi-step browse flow: Bookcase → Shelf → Books
- `ShelfSummary` + `BookSummary` projections implemented
- Status tracking and checkout/checkin functionality
- Multi-author book support with many-to-many relationships
- Book-to-Shelf-to-Bookcase assignment model

### 🔄 In Progress

- **Comprehensive architectural refactoring** to committed DDD approach
- Implementing consistent `Optional` patterns throughout codebase
- Fixing entity-domain model synchronization
- Establishing proper validation and invariant management
- Expanding unit test coverage
- Improving CLI formatting (ANSI colors, better selectors)

### 🎯 Upcoming

- Pagination for large result sets
- Enhanced selector UX (colors, animations)
- Book recommendations based on reading history
- Statistics and analytics dashboard
- Export/import functionality
- Better error handling and user feedback

---

## 💻 Development Principles

Bibby follows a **learning-focused, systematic approach**:

1. **Start simple, refactor when justified** — Avoid premature complexity
2. **Separation of concerns** — Keep business logic out of persistence layer
3. **Test early, test often** — Catch architectural issues before they grow
4. **Document decisions** — Maintain clear records of architectural choices
5. **Package by feature** — Related components stay together
6. **Domain-first thinking** — Business rules live in domain models, not scattered across services

**Development workflow:**
1. Implement feature in one domain (e.g., Book) as a template
2. Validate approach through testing and code review
3. Apply learned patterns to other domains systematically
4. Maintain clean git history with detailed commit messages

---

## 🎨 The Bibby Prompt

```java
return new AttributedString("Bibby:_ ", 
    AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
```

Clean. Distinctive. Happily nerdy.

---

## 📝 Recent Highlights

### v0.3 — Domain-Driven Design Refactoring

- Implemented entity/domain separation across all domains
- Established consistent validation patterns
- Added comprehensive JavaDoc documentation
- Expanded unit test coverage with JUnit and Mockito
- Fixed entity-domain synchronization issues
- Implemented proper `Optional` handling

### v0.2 — Bookcase → Shelf → Book Navigation

- Added `BookSummary` record
- Created repository query for ordered shelf books
- Built book selector using ComponentFlow
- Connected cascade: Bookcase → Shelf → Book
- Handled empty-shelf UX

### v0.1 — Core Features

- Status tracking implemented
- Friendly librarian responses
- Persistence confirmed in PostgreSQL
- Interactive author gathering
- Many-to-many relationship implemented
- Added transactional consistency

Full commit history is available on GitHub.

---

## 👨‍💻 About

Built by **Leo D. Penrose**  
*Builder • Systems Thinker • Lifelong Learner*

[LinkedIn](#) • [GitHub](https://github.com/leodvincci)

---

## 📄 License

This project is a personal learning endeavor and is not currently licensed for external use.
