
![Adobe Express - Terminal_AI_Librarian_Animation (1)](https://github.com/user-attachments/assets/7fb5a30e-bef4-4ae4-bb65-675b80ce7d8f)


<!--
![BibbyLogo](https://github.com/user-attachments/assets/fc548a52-3855-4615-8639-0fc9d58ef6a4)


https://github.com/user-attachments/assets/b3d09ca7-bf11-45cf-9578-fab523e91975

-->


---


**Bibby** is a personal project — a Spring Shell–based command-line tool for managing my own physical book library.
It began as an experiment to explore **Spring Shell**, **ComponentFlow**, and how to design an interactive CLI system in Java.

I have a real library downstairs with about 15 shelves. Finding a book can take forever.
So Bibby is both a technical sandbox and a way to bring order to that chaos.

---

## ⚙️ Current Focus

I’m currently building out the **Book Command Suite** — a collection of commands that handle core library management:

* Add books interactively
* Search by title or keyword
* Assign shelf locations
* Check books in and out
* List and rate books

Each command currently has placeholder `ComponentFlow` logic while I design and test the interaction flow.

---

## 🧠 Project Goals

Bibby is a way to:

1. **Practice building CLI systems from first principles** — parsing, validation, and flow control.
2. **Understand Spring Shell deeply** — especially how `@Command` and `ComponentFlow` work together.
3. **Build complete technical documentation** in Confluence (User Stories, Command Specs, and Change Logs).
4. Eventually connect to a **Spring Boot + PostgreSQL** backend for persistent book storage.

---

## 🧩 Example Commands

```bash
Bibby:_ book add
Bibby:_ book search --title "Meditations"
Bibby:_ book check-out --title "Sapiens"
Bibby:_ book assign-shelf --title "Deep Work" --shelf "B2"
Bibby:_ book list
```

---

## 🏗️ Project Structure

```
src/
 ├── main/java/com/penrose/bibby/
 │    ├── BookCommands.java         # Command group for 'book'
 │    ├── CustomPromptProvider.java # Custom prompt: Bibby:_
 │    └── model/                    # Book model and related DTOs
 ├── resources/
 │    └── application.yml
```

---

## 📘 Documentation

All documentation is tracked in **Confluence**, including:

* **User Stories**
* **Command Specifications**
* **Change Logs**
* **Component Flow Designs**

👉 [View the Bibby Command Suite Documentation](https://android42.atlassian.net/wiki/external/ZGJjZWU0NTIyZmVhNGQyOWI2NzAyYjEyMTBhM2YxZjE)

Each command follows this format:

```
Command: check-out
Category: Book Management / Borrowing
Status: Not Implemented
Version Introduced: v0.1

Purpose:
Marks a book as checked out, borrowed, or temporarily removed from the shelf.

Usage Example:
book check-out --title "Sapiens"
```

---

## 🧾 Current Status

* ✅ Book command suite defined
* 🚧 ComponentFlow logic in progress
* 🗂️ Documentation structure complete in Confluence
* ⏳ Database integration planned
* 🧠 AI-based shelving and reading recommendations (future)

---

## 🪄 Custom Shell Prompt

Bibby uses a custom prompt provider:

```java
return new AttributedString("Bibby:_ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
```

It’s minimal, functional, and gives the CLI a quiet identity of its own.

---

## 🧑‍💻 Personal Notes

This project isn’t about shipping fast — it’s about **learning deliberately**.
Bibby is a study in flow design, code structure, and documentation discipline.

The larger goal is to master how systems like this evolve from scratch — command, to flow, to persistence, to intelligence.

---

## 🧭 Version

**v0.1** — Initial structure, placeholder commands, and documentation framework.

---

## 🧱 Development Log

| Date           | Update                                                                     |
| -------------- | -------------------------------------------------------------------------- |
| **2025-10-27** | Added `book search`, `list`, `assign-shelf`, and `check-out` placeholders. |
| **2025-10-27** | Created full Confluence command specifications under “📘 Book Commands.”   |
| **2025-10-27** | Implemented custom `PromptProvider` with cyan “Bibby:_” prompt.            |
| **2025-10-27** | Added changelog section and git commit tracking.                           |

## 2025-10-30

- Refactored `BookcaseService` to use SLF4J for structured logging.
- Added reusable `ResponseStatusException` for bookcase label conflicts.
- Improved error handling: logs errors before throwing conflict exceptions.
- Added informative logs on successful bookcase creation.
- Cleaned up commented code for better readability.
- Enhanced RESTful error handling for bookcase creation API responses.
- Merged updates from main branch into feature branches to stay up-to-date.

*For more details, see the full commit history at: [Bibby Commits](https://github.com/leodvincci/Bibby/commits?sort=author-date&direction=desc)*

### 🧩 Change Log — *v0.x.x*

**Status:** `/status Released`
 **Date:** 2025-10-31
 **Component:** CLI / Service / Repository

#### 🆕 Feature: Multi-Author Add Flow

**Git Branch:** `feat/cli-multi-author-add`
 **Commit:** `feat(cli): support multiple authors during book add flow`

**Summary:**
 Bibby can now handle books with multiple authors directly through the CLI.
 Users are prompted for the number of authors when adding a new book, and the system iteratively collects each author’s name before saving.

**Implementation Details:**

- Introduced interactive prompt sequence using `ComponentFlow` for multi-author input.
- Added helper method `authorNameComponentFlow()` in `BookCommands` for collecting author names.
- Refactored `BookService.createNewBook()` to support multiple authors per book in a single transaction.
- Added `@Transactional` annotation to ensure persistence consistency across the author set.
- Updated `BookEntity` to safely append new `AuthorEntity` instances using `setAuthors(AuthorEntity author)`.
- Expanded `BookRepository` with `findByTitle()` for efficient lookups before creation.

**Outcome:**
 Books can now be linked to multiple authors without manual database edits.
 This marks the first functional use of the many-to-many relationship between `BookEntity` and `AuthorEntity`.

**Notes / Observations:**

- Next iteration: introduce de-duplication to reuse existing authors instead of always creating new ones.
- Future enhancement: add fuzzy or AI-assisted author name suggestions.


---



## 👤 Author

**Leo D. Penrose**
Builder • Systems Thinker • Lifelong Learner
[LinkedIn](https://linkedin.com/in/leodpenrose) • [GitHub](https://github.com/<your-username>)

---

