
https://github.com/user-attachments/assets/0e50d227-056a-47e7-b62b-65f9c86aeaf6

![Java](https://img.shields.io/badge/Java-17-blue?style=for-the-badge&logo=openjdk)
![Spring Shell](https://img.shields.io/badge/Spring%20Shell-Interactive%20CLI-brightgreen?style=for-the-badge&logo=spring)
![Status](https://img.shields.io/badge/Version-v0.3-orange?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-CLI%20%2B%20Web-yellow?style=for-the-badge)

---

# **Bibby — Personal Library CLI**
**A Spring Shell–powered command-line library management system for organizing physical books.**

Bibby is an interactive CLI tool for managing a real-life library of physical books. It started as a sandbox for exploring interactive CLI flows in Java, and evolved into a full-fledged system for tracking books, shelves, and bookcases — with personality sprinkled in.

**New in v0.3:** Scan book barcodes with your webcam, automatically fetch metadata from Google Books, and place books on shelves in one seamless workflow.
<img width="2538" height="1022" alt="image" src="https://github.com/user-attachments/assets/726b6fed-1272-4745-853c-3fbef4b019f4" />

---

## 🎯 What Bibby Does

### Core Features

- ✅ **Barcode scanning** — Scan ISBNs via webcam, auto-fetch metadata from Google Books API
- ✅ **Scan-to-shelf workflow** — Scan a book and place it on a shelf in one flow
- ✅ **Add books interactively** with multi-author input
- ✅ **Assign shelf locations** (Book → Shelf → Bookcase)
- ✅ **Check books in and out** (with personality)
- ✅ **Search by title/keyword**
- ✅ **Browse using cascading selectors** (Bookcase → Shelf → Books)
- ✅ **Shelf capacity management** — Track and enforce book limits per shelf
- ✅ **REST API** for web-based book imports

### Browse Flow

The browse flow walks you through:
- **Select a Bookcase**
- **Select a Shelf** within that bookcase
- **View and select Books** from that shelf

Powered by lightweight projections (`ShelfSummary`, `BookSummary`) and cascading `ComponentFlow` selectors.

---

## 🎓 Why Bibby Exists

Bibby is deliberately built as a **long-term practice arena** for software engineering fundamentals:

- Build expressive, stateful CLI flows with Spring Shell
- Practice **Domain-Driven Design** with entity/domain separation
- Implement **Hexagonal Architecture** (Ports & Adapters)
- Design PostgreSQL-backed domain models
- Integrate external APIs (Google Books) with reactive programming
- Develop comprehensive unit tests with JUnit 5 and Mockito
- Follow systematic refactoring and architectural evolution

**The development rhythm:** command → flow → service → domain → repository → persistence

Bibby serves as a hands-on learning project focused on mastering software engineering principles through practice rather than having tools build it.

---

## 🚀 Quick Start Examples

### Scanning a Book (New!)

```
Bibby:_ book scan
```

This starts the scan flow:
1. Enter or scan an ISBN barcode
2. Bibby fetches metadata from Google Books API
3. Review title, authors, description
4. Confirm to add to your library
5. Optionally assign to a shelf immediately

**Sample Session:**
```
Bibby:_ book scan
ISBN Number:_ 9781449373320

========================================
📚  Book Metadata
========================================

ISBN:              9781449373320
Title:             Designing Data-Intensive Applications
Authors:           [Martin Kleppmann]
Published:         2017-03-16
Categories:        [Computers]

Description:
Data is at the center of many challenges in system design today...

========================================

? Would you like to add this book to the library?
> Yes  — Let's Do It

</>: Book added to the library database successfully!
```

### Adding a Book Manually

```
Bibby:_ book add
```

Interactive flow for manual entry:
1. Enter the book title
2. Specify number of authors
3. For each author, enter first and last name

### Browsing by Location

```
Bibby:_ bookcase browse
```

Navigate through your physical library:
1. **Select a Bookcase** → displays all bookcases with book counts
2. **Select a Shelf** → shows shelves within that bookcase
3. **View Books** → lists all books on the selected shelf
4. **Check out** directly from the browse view

### Checking Out a Book

```
Bibby:_ book check-out
Book Title:_ Sapiens
```

Marks the book as checked out. Bibby responds with personality:

```
"All set — Sapiens is checked out and ready to go with you."
```

---

## 📋 Available Commands

```bash
# Book Commands
book add                    # Interactive book creation flow
book scan                   # Scan ISBN barcode to add book
book scan --type multi      # Continuous scanning mode
book search                 # Search by title, author, or other criteria
book check-out              # Check out a book
book check-in               # Return a book
book shelf                  # Assign book to shelf location
book list                   # View all books

# Bookcase Commands
bookcase browse             # Navigate Bookcase → Shelf → Books
bookcase create             # Create new bookcase with shelf configuration
```

---

## 🏗️ Project Structure

Bibby follows **Hexagonal Architecture** (Ports & Adapters) with package-by-feature organization:

```
src/main/java/com/penrose/bibby/
├── cli/                           # CLI Layer
│   ├── book/
│   │   └── BookCommandLine.java   # Book command handlers
│   ├── bookcase/
│   │   └── BookcaseCommandLine.java
│   └── prompt/
│       └── application/
│           └── CliPromptService.java
│
├── library/                       # Domain Modules
│   ├── book/
│   │   ├── api/                   # Inbound ports (DTOs, Facades)
│   │   │   ├── BookDetailView.java
│   │   │   ├── BookRequestDTO.java
│   │   │   └── BookSummary.java
│   │   ├── application/           # Use cases (Services)
│   │   │   ├── BookService.java
│   │   │   ├── BookInfoService.java
│   │   │   └── IsbnEnrichmentService.java
│   │   ├── domain/                # Core business logic
│   │   │   ├── Book.java
│   │   │   ├── BookFactory.java
│   │   │   └── AvailabilityStatus.java
│   │   └── infrastructure/        # Outbound adapters
│   │       ├── entity/
│   │       ├── external/          # Google Books API types
│   │       ├── mapping/
│   │       └── repository/
│   │
│   ├── author/                    # Same structure
│   ├── shelf/
│   └── bookcase/
│
├── web/                           # REST Controllers
│   ├── book/
│   │   ├── BookController.java
│   │   └── BookImportController.java
│   ├── shelf/
│   └── bookcase/
│
└── util/                          # Utilities
    └── WebClientConfig.java
```

Each domain module follows consistent layering:
- **api/** — Inbound port contracts (DTOs, Facades)
- **application/** — Use case orchestration
- **domain/** — Core business logic and rules
- **infrastructure/** — Persistence, external integrations

---

## 🛠️ Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| CLI Framework | Spring Shell 3.4.1 | Interactive command-line interface |
| Backend | Spring Boot 3.5.7 | Application framework & dependency injection |
| HTTP Client | Spring WebFlux | Reactive HTTP calls to Google Books API |
| Persistence | Spring Data JPA | ORM and repository abstractions |
| Database | PostgreSQL | Relational data storage |
| Language | Java 17 | Core language |
| Testing | JUnit 5 & Mockito | Unit testing framework |
| Build Tool | Maven | Dependency management & build automation |

---

## 🏛️ Architecture

Bibby follows **Hexagonal Architecture** (Ports & Adapters) with Domain-Driven Design principles:

```
┌─────────────────────────────────────────────────────────────┐
│                    Driving Adapters                         │
│         (CLI Commands, REST Controllers, Web UI)            │
├─────────────────────────────────────────────────────────────┤
│                     Inbound Ports                           │
│              (DTOs, Facades, API Contracts)                 │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                         │
│            (Services, Use Case Orchestration)               │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│       (Entities, Business Rules, Domain Services)           │
├─────────────────────────────────────────────────────────────┤
│                    Outbound Ports                           │
│            (Repository Interfaces, Gateways)                │
├─────────────────────────────────────────────────────────────┤
│                    Driven Adapters                          │
│    (JPA Repositories, Google Books Client, PostgreSQL)      │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

- **Hexagonal Architecture** — Clear boundaries between ports and adapters
- **Domain-Driven Design** — Separate domain models from persistence entities
- **Factory Pattern** — Centralized entity/domain object creation
- **Repository Pattern** — Spring Data JPA repositories for data access
- **DTO Pattern** — Data transfer objects for API boundaries
- **Projection Pattern** — Lightweight read models for browse flows
- **Facade Pattern** — Cross-domain communication contracts

### Entity/Domain Separation

Bibby maintains **full entity/domain separation**:

- **Domain Models** (`Book`, `Author`, `Shelf`) — Contain business logic, validation, and invariants
- **Entities** (`BookEntity`, `AuthorEntity`) — Handle persistence mapping only
- **Mappers** — Bridge between domain and entity layers
- **Factories** — Create domain objects and entities consistently

---

## 📊 Domain Model

The domain model captures a physical library with hierarchical organization:

```
Bookcase (1) ──────< Shelf (many)
                        │
                        │ has capacity limit
                        ↓
                    Book (many)
                        │
                   (many-to-many)
                        ↓
                   Author (many)
```

### Core Entities

- **Book** — title, isbn, publisher, publicationYear, genre, description, status, shelfId
- **Author** — firstName, lastName (many-to-many with books)
- **Shelf** — shelfLabel, shelfPosition, bookcaseId, bookCapacity
- **Bookcase** — label, shelfCapacity, totalBookCapacity

### Status Tracking

- Books track `availabilityStatus` (AVAILABLE, CHECKED_OUT, RESERVED, LOST, ARCHIVED)
- Timestamps: `createdAt`, `updatedAt`

---

## 🔄 Barcode Scanning Flow

Bibby integrates with the Google Books API for automatic metadata retrieval:

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐
│ ISBN Scan   │────▶│ BookInfoService │────▶│ Google Books API │
└─────────────┘     └─────────────────┘     └──────────────────┘
                            │
                            ▼
                    ┌───────────────────┐
                    │ Display Metadata  │
                    └───────────────────┘
                            │
                            ▼
                    ┌───────────────────┐
                    │ User Confirmation │
                    └───────────────────┘
                            │
                            ▼
               ┌────────────────────────────┐
               │ IsbnEnrichmentService      │
               │ • Create Author entities   │
               │ • Create Book entity       │
               │ • Persist to database      │
               └────────────────────────────┘
                            │
                            ▼
                    ┌───────────────────┐
                    │ Assign to Shelf   │
                    │    (optional)     │
                    └───────────────────┘
```

### Web Scanner UI

A web-based barcode scanner is available at the root URL when the application runs, enabling webcam-based ISBN scanning with automatic shelf placement.

---

## 🧪 Testing & Quality

Bibby includes comprehensive unit testing:

- **JUnit 5** for test structure and assertions
- **Mockito** for mocking dependencies
- **Focus areas:** Domain model validation, service layer logic, entity-domain synchronization

**Testing philosophy:** Write tests that verify business logic and catch architectural issues early.

---

## 📚 Documentation

Comprehensive development documentation is maintained in the `docs/` directory:

- **Devlogs** — Detailed logs of each development session with learnings
- **Specs** — Micro-slice specifications for refactoring work
- **Mentor Feedback** — Code review notes and architectural guidance

Each feature follows a documentation trail: **problem → design → implementation → reflection**.

---

## 🚧 Current Status & Roadmap

### ✅ Completed (v0.3)

- Hexagonal architecture migration across all domain modules
- ISBN barcode scanning with Google Books API integration
- Scan-to-shelf complete workflow
- Multi-author book support with many-to-many relationships
- Shelf capacity management and enforcement
- Entity/domain separation with factory patterns
- REST API for web-based book imports
- Web-based barcode scanner UI

### 🔄 In Progress

- Consolidating mapper classes (`BookMapper` / `BookMapperTwo`)
- Implementing facade contracts for cross-domain communication
- Expanding unit test coverage
- Error handling improvements for API failures

### 🎯 Upcoming

- ArchUnit tests to enforce architectural boundaries
- Pagination for large result sets
- Book recommendations based on reading history
- Statistics and analytics dashboard
- Export/import functionality
- Enhanced selector UX (colors, animations)

---

## 💻 Development Principles

Bibby follows a **learning-focused, systematic approach**:

1. **Hexagonal Architecture** — Clear boundaries between domain and infrastructure
2. **Domain-Driven Design** — Business rules live in domain models
3. **Start simple, refactor when justified** — Avoid premature complexity
4. **Test early, test often** — Catch architectural issues before they grow
5. **Document decisions** — Maintain clear records of architectural choices
6. **Package by feature** — Related components stay together

**Development workflow:**
1. Implement feature in one domain as a template
2. Validate approach through testing and code review
3. Apply learned patterns to other domains systematically
4. Maintain clean git history with detailed commit messages

---

## 🎨 The Bibby Prompt

```java
return new AttributedString("Guest </>\uD835\uDC01\uD835\uDC08\uD835\uDC01\uD835\uDC01\uD835\uDC18:_ ", 
    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
```

Clean. Distinctive. Happily nerdy.

---

## 📝 Recent Highlights

### v0.3 — Hexagonal Architecture & Barcode Scanning

- Complete hexagonal architecture migration across all domain modules
- Google Books API integration with reactive WebClient
- ISBN barcode scanning via CLI and web interface
- Scan-to-shelf workflow with capacity checking
- Web-based scanner UI for webcam barcode scanning
- Facade interfaces for cross-domain communication
- Package reorganization to Ports & Adapters structure

### v0.2 — Bookcase → Shelf → Book Navigation

- Added `BookSummary` and `ShelfSummary` projections
- Built cascading browse flow with ComponentFlow selectors
- Connected full navigation: Bookcase → Shelf → Book → Checkout
- Handled empty-shelf UX gracefully

### v0.1 — Core Features

- Status tracking and friendly librarian responses
- Interactive author gathering with multi-author support
- Many-to-many relationships with PostgreSQL persistence
- Transactional consistency

---

## 👨‍💻 About

Built by **Leo D. Penrose**  
*Builder • Systems Thinker • Lifelong Learner*

[LinkedIn](#) • [GitHub](https://github.com/leodvincci)

---

## 📄 License

This project is a personal learning endeavor and is not currently licensed for external use.
