![Adobe Express - Generating_Bibby_Retro_Futuristic_Intro_Sequence (1)](https://github.com/user-attachments/assets/740509ac-3692-4c69-bb3e-5af9f6a1082e)

# Bibby

**A Spring Shell-powered personal library management system for organizing physical books with barcode scanning, hierarchical storage, and clean architecture.**

![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen?style=flat-square&logo=spring)
![Spring Shell](https://img.shields.io/badge/Spring%20Shell-3.4.1-green?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?style=flat-square&logo=postgresql)
![Maven](https://img.shields.io/badge/Maven-Build-red?style=flat-square&logo=apache-maven)
![Platform](https://img.shields.io/badge/Platform-CLI%20%2B%20Web-yellow?style=for-the-badge)
![Spring Shell](https://img.shields.io/badge/Spring%20Shell-Interactive%20CLI-brightgreen?style=for-the-badge&logo=spring)
---

## 📖 Overview

**Bibby** is an interactive command-line application for managing a personal library of physical books. It provides a dual interface—CLI and web—for cataloging books, organizing them on virtual bookshelves, tracking availability, and enriching metadata via the Google Books API.

Built as a learning project to practice **Hexagonal Architecture**, **Domain-Driven Design**, and enterprise Java patterns, Bibby demonstrates clean separation of concerns across domain, application, and infrastructure layers.

### What It Does

- **Scan ISBNs** via CLI or webcam to add books automatically
- **Organize hierarchically**: Bookcase → Shelf → Book
- **Track availability**: AVAILABLE, CHECKED_OUT, RESERVED, LOST, ARCHIVED
- **Manage authors** with many-to-many relationships
- **Search books** by title, author, or ISBN
- **Browse interactively** through bookcases, shelves, and books
- **Enforce shelf capacity** constraints
- **Enrich metadata** automatically from Google Books API

---

## ✨ Features

| Feature | Description | Status |
|---------|-------------|--------|
| **ISBN Barcode Scanning** | Scan barcodes via CLI or web UI, auto-fetch metadata from Google Books API | ✅ Complete |
| **Scan-to-Shelf Workflow** | Scan a book and place it on a shelf in one flow | ✅ Complete |
| **Interactive CLI** | Spring Shell-powered commands with multi-step flows | ✅ Complete |
| **Web Barcode Scanner** | Webcam-based ISBN scanning using ZXing.js | ✅ Complete |
| **Hierarchical Organization** | Bookcase → Shelf → Book with cascading navigation | ✅ Complete |
| **Shelf Capacity Management** | Track and enforce book limits per shelf | ✅ Complete |
| **Multi-Author Support** | Many-to-many book-author relationships | ✅ Complete |
| **Check-in/Check-out** | Track book availability with friendly responses | ✅ Complete |
| **REST API** | HTTP endpoints for book, shelf, and bookcase operations | ✅ Complete |
| **Hexagonal Architecture** | Clean ports & adapters separation across all modules | ✅ Complete |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **PostgreSQL** (running on port 5332 or configure your own)
- **Maven 3.8+** (or use the included Maven Wrapper)

### Database Setup

1. Install and start PostgreSQL
2. Create a database named `amigos`:
   ```bash
   psql -U postgres
   CREATE DATABASE amigos;
   ```
3. Update credentials in `src/main/resources/application.properties` if needed:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
   spring.datasource.username=amigoscode
   spring.datasource.password=password
   ```

**Note:** The application uses `spring.jpa.hibernate.ddl-auto=create-drop`, which drops and recreates tables on every restart. This is intentional for development but **data will not persist** across runs.

### Build and Run

**Using Maven Wrapper (recommended):**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

**Using installed Maven:**
```bash
mvn clean install
mvn spring-boot:run
```

### First Launch

On startup, you'll see the Bibby ASCII banner and drop into an interactive shell:

```
██████╗ ██╗██████╗ ██████╗ ██╗   ██╗
██╔══██╗██║██╔══██╗██╔══██╗╚██╗ ██╔╝
██████╔╝██║██████╔╝██████╔╝ ╚████╔╝
██╔══██╗██║██╔══██╗██╔══██╗  ╚██╔╝
██████╔╝██║██████╔╝██████╔╝   ██║
╚═════╝ ╚═╝╚═════╝ ╚═════╝    ╚═╝

Bibby:_
```

### Accessing the Web UI

While the CLI is running, open your browser to:
```
http://localhost:8080
```

This loads the barcode scanner interface using your webcam.

---

## 📚 Usage

### Available Commands

Type `help` in the CLI to see all commands. Below are the main workflows:

#### Book Management

**Scan a book (recommended):**
```
Bibby:_ book scan
```
This starts the scan flow:
1. Enter or scan an ISBN barcode
2. Bibby fetches metadata from Google Books API
3. Review title, authors, description
4. Confirm to add to your library
5. Optionally assign to a shelf immediately

**Manual book entry:**
```
Bibby:_ book new
```
Interactive prompts guide you through title, authors, and ISBN entry.

**Search for a book:**
```
Bibby:_ book search
```
Choose search type (title, author, or ISBN) and enter your query.

**Check out a book:**
```
Bibby:_ book check-out
Book Title:_ Designing Data-Intensive Applications
```
Marks the book as CHECKED_OUT with a friendly confirmation message.

**Check in a book:**
```
Bibby:_ book check-in
Book Title:_ Designing Data-Intensive Applications
```
Returns the book to AVAILABLE status.

**Assign a book to a shelf:**
```
Bibby:_ book shelf
```
Prompts for book title, bookcase, and shelf selection.

#### Bookcase & Shelf Management

**Browse your library:**
```
Bibby:_ bookcase browse
```
Navigate through:
1. **Select a Bookcase** → displays all bookcases with book counts
2. **Select a Shelf** → shows shelves within that bookcase
3. **View Books** → lists all books on the selected shelf
4. **Check out** directly from the browse view

**Create a bookcase:**
```
Bibby:_ bookcase create
```
Interactive flow to define bookcase label and shelf configuration.

### Example Session

```bash
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

Select Bookcase:
> Main Library

Select Shelf:
> Shelf A-1 (2/10 books)

</> Book added to the library database successfully!
```

---

## 🏛️ Architecture

Bibby follows **Hexagonal Architecture** (Ports & Adapters) with **Domain-Driven Design** principles.

### High-Level Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    Driving Adapters                         │
│         (CLI Commands, REST Controllers, Web UI)            │
├─────────────────────────────────────────────────────────────┤
│                     Inbound Ports                           │
│            (Facades, DTOs, API Contracts)                   │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                         │
│            (Services, Use Case Orchestration)               │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│       (Entities, Value Objects, Business Rules)             │
├─────────────────────────────────────────────────────────────┤
│                    Outbound Ports                           │
│            (Repository Interfaces, Gateways)                │
├─────────────────────────────────────────────────────────────┤
│                    Driven Adapters                          │
│    (JPA Repositories, Google Books Client, PostgreSQL)      │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure

The codebase is organized by **domain modules** under `com.penrose.bibby.library/`:

```
library/
├── cataloging/
│   ├── book/
│   │   ├── contracts/         # Inbound ports (DTOs, Facades)
│   │   │   ├── ports/inbound/ # BookFacade
│   │   │   └── dtos/          # BookDTO, BookMetaDataResponse, etc.
│   │   ├── core/
│   │   │   ├── domain/        # Book, BookId, Title, Isbn, AvailabilityStatus
│   │   │   └── application/   # BookService, IsbnEnrichmentService
│   │   └── infrastructure/    # BookEntity, BookJpaRepository, GoogleBooksResponse
│   └── author/
│       ├── contracts/         # AuthorFacade, AuthorDTO
│       ├── core/domain/       # Author, AuthorId, AuthorName
│       ├── core/application/  # AuthorService
│       └── infrastructure/    # AuthorEntity, AuthorJpaRepository
├── stacks/
│   ├── bookcase/
│   │   ├── contracts/         # BookcaseFacade, BookcaseDTO
│   │   ├── core/domain/       # Bookcase
│   │   ├── core/application/  # BookcaseService
│   │   └── infrastructure/    # BookcaseEntity, BookcaseRepository
│   └── shelf/
│       ├── contracts/         # ShelfFacade, ShelfDTO, ShelfSummary
│       ├── core/domain/       # Shelf, ShelfId
│       ├── core/application/  # ShelfService, BrowseShelfUseCase
│       └── infrastructure/    # ShelfEntity, ShelfJpaRepository
└── discovery/
    └── core/application/      # SearchBooksUseCase
```

### Adapters

**Driving Adapters (inbound):**
- `cli/commands/` — Spring Shell command handlers
- `infrastructure/web/` — REST controllers

**Driven Adapters (outbound):**
- `infrastructure/repository/` — JPA repository implementations
- `infrastructure/external/` — Google Books API integration

### Domain Models

**Core Entities:**
- `Book` — Aggregates title, ISBN, authors, availability status, shelf location
- `Author` — Manages author identity with many-to-many book relationships
- `Shelf` — Tracks books and enforces capacity constraints
- `Bookcase` — Contains shelves with configurable capacity

**Value Objects:**
- `BookId`, `Title`, `Isbn`, `AuthorRef` (for Book)
- `AuthorId`, `AuthorName` (for Author)
- `ShelfId` (for Shelf)

**Enums:**
- `AvailabilityStatus` — AVAILABLE | CHECKED_OUT | RESERVED | LOST | ARCHIVED

### Design Patterns

- **Hexagonal Architecture** — Ports & Adapters for testability and flexibility
- **Domain-Driven Design** — Rich domain models with business logic
- **Repository Pattern** — Data access abstraction
- **Factory Pattern** — Centralized object creation (BookFactory, AuthorFactory)
- **DTO Pattern** — Data transfer across architectural boundaries
- **Projection Pattern** — Lightweight read models (BookSummary, ShelfSummary)
- **Facade Pattern** — Simplified cross-domain communication

### Entity/Domain Separation

Bibby maintains **strict separation** between persistence and domain layers:

- **Domain Models** (`Book`, `Author`) contain business logic and invariants
- **Entities** (`BookEntity`, `AuthorEntity`) are JPA-annotated persistence models
- **Mappers** bridge between layers without leaking persistence concerns into the domain

---

## 🗄️ Domain Model

The domain captures a physical library with hierarchical organization:

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

### Entities & Relationships

| Entity | Attributes | Relationships |
|--------|-----------|---------------|
| **Book** | id, title, isbn, publisher, publicationYear, genre, description, availabilityStatus, shelfId, createdAt, updatedAt | Many-to-Many with Author, Many-to-One with Shelf |
| **Author** | id, firstName, lastName | Many-to-Many with Book |
| **Shelf** | id, shelfLabel, shelfPosition, bookcaseId, bookCapacity, bookIds | One-to-Many with Book, Many-to-One with Bookcase |
| **Bookcase** | id, bookcaseLabel, shelfCapacity, totalBookCapacity | One-to-Many with Shelf |

### Availability Status

Books track their current state via `AvailabilityStatus`:
- **AVAILABLE** — On shelf, ready to check out
- **CHECKED_OUT** — Currently borrowed
- **RESERVED** — Reserved for future pickup
- **LOST** — Marked as lost
- **ARCHIVED** — Removed from active circulation

---

## 🔌 REST API

The application exposes HTTP endpoints for programmatic access.

**Base URL:** `http://localhost:8080/api/v1`

### Book Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/books` | Add a new book manually |
| GET | `/books` | Search for a book by title (request body) |
| POST | `/books/{bookId}/shelf` | Assign a book to a shelf |
| GET | `/lookup/{isbn}` | Fetch metadata from Google Books API |

**Example: Add a book**
```bash
curl -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Architecture",
    "isbn": "9780134494166",
    "authors": [
      {"firstName": "Robert", "lastName": "Martin"}
    ]
  }'
```

**Example: Assign book to shelf**
```bash
curl -X POST http://localhost:8080/api/v1/books/1/shelf \
  -H "Content-Type: application/json" \
  -d '{"shelfId": 5}'
```

### Shelf Endpoints

Located in `ShelfController.java` (endpoints not fully documented in code).

### Bookcase Endpoints

Located in `BookCaseController.java` (endpoints not fully documented in code).

### Author Endpoints

Located in `AuthorController.java` (endpoints not fully documented in code).

**Note:** Some REST endpoints are under active development and may not have complete request/response documentation. Refer to controller source files for details.

---

## ⚙️ Configuration

### Application Properties

Located at `src/main/resources/application.properties`:

```properties
# Application
spring.application.name=Bibby
spring.shell.interactive.enabled=true

# Database
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
spring.datasource.username=amigoscode
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=create-drop

# Logging (disabled for clean CLI experience)
logging.level.org.springframework=OFF
logging.level.org.hibernate=OFF
logging.level.root=OFF

# ANSI Colors
spring.output.ansi.enabled=ALWAYS
```

### Key Settings

| Setting | Value | Impact |
|---------|-------|--------|
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | **Database schema is recreated on every restart. Data does not persist.** |
| `logging.level.root` | `OFF` | Suppresses all Spring/Hibernate logs for a clean CLI experience |
| `spring.output.ansi.enabled` | `ALWAYS` | Enables colored output in terminal |

**⚠️ Important:** The `create-drop` setting means all data is lost when the application stops. This is intentional for development but should be changed to `update` or `validate` for production use.

---

## 🧪 Testing

Bibby includes unit tests using **JUnit 5**, **Mockito**, and **ArchUnit**.

### Test Coverage

- **7 test files** with **28+ test methods**
- Located in `src/test/java/com/penrose/bibby/`

**Test files:**
```
BibbyApplicationTests.java
cli/BookCommandLineTest.java
library/author/core/domain/AuthorNameTest.java
library/author/core/domain/AuthorRepositoryTest.java
library/book/BookServiceTest.java
library/book/infrastructure/repository/BookDomainRepositoryImplTest.java
library/stacks/shelf/core/application/BrowseShelfUseCaseTest.java
```

### Running Tests

**Run all tests:**
```bash
./mvnw test
```

**Run tests with coverage:**
```bash
./mvnw test jacoco:report
```

**Run specific test:**
```bash
./mvnw test -Dtest=BookServiceTest
```

### Testing Strategy

- **Unit Tests** — Domain logic validation, service layer orchestration
- **Repository Tests** — Entity-domain mapping correctness
- **Architectural Tests** — ArchUnit rules to enforce hexagonal boundaries (planned)

**Note:** Test coverage is growing. Current focus areas include domain models, services, and repository implementations. Integration tests and end-to-end CLI tests are planned.

---

## 🛠️ Tech Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Java | 17 | Core language |
| **Framework** | Spring Boot | 3.5.7 | Application framework, DI, auto-configuration |
| **CLI Framework** | Spring Shell | 3.4.1 | Interactive command-line interface |
| **Web Framework** | Spring Web | (via Boot) | REST API controllers |
| **HTTP Client** | Spring WebFlux | (via Boot) | Reactive HTTP calls to Google Books API |
| **ORM** | Spring Data JPA | (via Boot) | Repository abstraction, domain persistence |
| **Database** | PostgreSQL | Latest | Relational data storage |
| **Build Tool** | Maven | 3.8+ | Dependency management, build automation |
| **Testing** | JUnit 5 | 5.x | Unit testing framework |
| **Mocking** | Mockito | 5.17.0 | Test doubles and mocking |
| **Architecture Testing** | ArchUnit | 1.3.0 | Enforce architectural boundaries |
| **Barcode Scanning** | ZXing | Latest (via CDN) | Web-based ISBN barcode scanning |

---

## 🧑‍💻 Development

### Project Goals

Bibby is explicitly designed as a **long-term practice arena** for software engineering fundamentals:

- Build expressive, stateful CLI flows with Spring Shell
- Practice **Domain-Driven Design** with entity/domain separation
- Implement **Hexagonal Architecture** (Ports & Adapters)
- Design PostgreSQL-backed domain models
- Integrate external APIs (Google Books) with reactive programming
- Develop comprehensive unit tests with JUnit 5 and Mockito
- Follow systematic refactoring and architectural evolution

**The development rhythm:**
```
command → flow → service → domain → repository → persistence
```

### Development Workflow

1. Implement feature in one domain module as a template
2. Validate approach through testing and code review
3. Apply learned patterns to other domains systematically
4. Maintain clean git history with detailed commit messages
5. Document decisions in `docs/` directory

### Development Principles

1. **Hexagonal Architecture** — Clear boundaries between domain and infrastructure
2. **Domain-Driven Design** — Business rules live in domain models
3. **Start simple, refactor when justified** — Avoid premature complexity
4. **Test early, test often** — Catch architectural issues before they grow
5. **Document decisions** — Maintain clear records of architectural choices
6. **Package by feature** — Related components stay together

### Formatting & Linting

**Note:** No automated formatters or linters are currently configured. Follow existing code style conventions:
- Standard Java naming conventions
- Consistent indentation (4 spaces)
- Package-by-feature organization

### Common Troubleshooting

**Database connection errors:**
- Ensure PostgreSQL is running on port 5332
- Verify database `amigos` exists
- Check credentials in `application.properties`

**Port already in use:**
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

**Maven build fails:**
```bash
# Clean and rebuild
./mvnw clean install -U
```

**Tests fail with Mockito errors:**
- Ensure Java 17 is being used (not Java 8/11)
- Check Maven Surefire plugin configuration in `pom.xml`

---

## 📝 Documentation

Comprehensive development documentation is maintained in the `docs/` directory:

- **`docs/the-devlogs/`** — Detailed logs of each development session with learnings
- **`docs/engineering/`** — Technical specifications, architecture decisions, closed issues
- **`docs/systems/`** — System design documents

Each feature follows a documentation trail: **problem → design → implementation → reflection**.

**Blog:** [Building Bibby](https://buildingbibby.hashnode.dev/) — Development journey and learnings.

---

## 🚧 Current Status & Roadmap

### ✅ Completed (v0.0.1-SNAPSHOT)

- ✅ Hexagonal architecture migration across all domain modules
- ✅ ISBN barcode scanning with Google Books API integration
- ✅ Scan-to-shelf complete workflow
- ✅ Multi-author book support with many-to-many relationships
- ✅ Shelf capacity management and enforcement
- ✅ Entity/domain separation with factory patterns
- ✅ REST API for web-based book imports
- ✅ Web-based barcode scanner UI with ZXing.js
- ✅ Interactive browse flow (Bookcase → Shelf → Books)
- ✅ Check-in/Check-out with availability tracking
- ✅ Cascading CLI prompts with Spring Shell ComponentFlow

### 🔄 In Progress

- 🔄 Consolidating mapper classes (`BookMapper` / `BookMapperTwo`)
- 🔄 Implementing facade contracts for cross-domain communication
- 🔄 Expanding unit test coverage across all modules
- 🔄 Error handling improvements for Google Books API failures

### 🎯 Planned

- ⬜ ArchUnit tests to enforce architectural boundaries
- ⬜ Pagination for large result sets in browse/search flows
- ⬜ Book recommendations based on reading history
- ⬜ Statistics and analytics dashboard (CLI + web)
- ⬜ Export/import functionality (CSV, JSON)
- ⬜ Enhanced selector UX (colors, animations, fuzzy search)
- ⬜ Persistent data storage option (toggle `create-drop` → `update`)
- ⬜ Integration tests for CLI flows
- ⬜ Dockerized PostgreSQL setup for easier onboarding
- ⬜ Multi-library support (multiple users/libraries per database)

### Known Limitations

- **No data persistence** — `create-drop` setting wipes data on restart
- **Single user only** — No authentication or multi-user support
- **Google Books API quota** — External API may rate-limit requests
- **Limited REST API docs** — Some endpoints lack full OpenAPI/Swagger documentation
- **No pagination** — Large book collections may slow down browse/search
- **No Docker setup** — Manual PostgreSQL installation required
- **Incomplete test coverage** — Some modules lack comprehensive tests

---

## 🤝 Contributing

This is a personal learning project and is not currently accepting external contributions. However, feedback, suggestions, and architectural discussions are welcome!

If you find a bug or have a feature suggestion, feel free to open an issue on GitHub.

---

## 📄 License

This project is a personal learning endeavor and is **not currently licensed for external use**.

If you'd like to use this code for educational purposes, please reach out first.

---

## 👤 Author

**Leo D. Penrose**
*Builder • Systems Thinker • Lifelong Learner*

- GitHub: [@leodvincci](https://github.com/leodvincci)
- Blog: [Building Bibby](https://buildingbibby.hashnode.dev/)

---

## 🙏 Acknowledgments

- **Spring Framework** — For the powerful ecosystem that makes this project possible
- **Spring Shell** — For making CLI development elegant and expressive
- **Google Books API** — For free metadata enrichment
- **ZXing** — For barcode scanning capabilities
- **Amigoscode** — For inspiring clean architecture practices

---

## 📚 Additional Resources

- [Spring Shell Documentation](https://docs.spring.io/spring-shell/docs/current/reference/htmlsingle/)
- [Hexagonal Architecture Guide](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design Reference](https://www.domainlanguage.com/ddd/reference/)
- [Google Books API Docs](https://developers.google.com/books/docs/v1/using)

---

**Bibby** — *Your Library. Your Rules.*
