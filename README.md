
![Adobe Express - Terminal_AI_Librarian_Animation (1)](https://github.com/user-attachments/assets/7fb5a30e-bef4-4ae4-bb65-675b80ce7d8f)


<!--
![BibbyLogo](https://github.com/user-attachments/assets/fc548a52-3855-4615-8639-0fc9d58ef6a4)


https://github.com/user-attachments/assets/b3d09ca7-bf11-45cf-9578-fab523e91975

-->


---

# **Bibby — Personal Library CLI**

Bibby is a Spring Shell–powered command-line tool for managing my real-life library of physical books. It started as a playground for building interactive CLI flows in Java, and it turned into an actual system for keeping track of the books on my shelves downstairs.

Bibby helps bring structure to the chaos—and gives the CLI a little personality along the way.

------

## ⚙️ **What Bibby Does (Today)**

Bibby’s main features live in the **Book Command Suite**, including:

- Add books interactively
- Search by title or keyword
- Assign shelf locations
- Check books in and out
- List and rate books

Most commands use Spring Shell’s `ComponentFlow` for prompt-driven interactions.

------

## 🧠 **Why This Project Exists**

Bibby is a deliberate practice project meant to help me:

- Learn how to design expressive, stateful CLI systems
- Go deep into Spring Shell, prompt design, and flow control
- Build real documentation (User Stories, Command Specs, Change Logs)
- Evolve toward a Spring Boot + PostgreSQL backend for persistence

It’s a slow-built system: command → flow → service → persistence.

------

## 🧩 **Example Commands**

```
book add
book search --title "Meditations"
book check-out --title "Sapiens"
book assign-shelf --title "Deep Work" --shelf "B2"
book list
```

------

## 🏗️ **Project Structure**

```
src/
 ├── main/java/com/penrose/bibby/
 │    ├── BookCommands.java         # Book-focused CLI commands
 │    ├── CustomPromptProvider.java # "Bibby:_" CLI prompt
 │    └── library/                  # Books, authors, shelves, services, repos
 └── resources/
      └── application.yml
```

------

## 📘 **Documentation**

All documentation lives in Confluence and includes:

- User stories
- Command specifications
- Change logs
- ComponentFlow designs

Each command follows a standardized format (purpose, usage, flow, exceptions, domain rules).

------

## 🧾 **Current Status**

- ✅ Command suite defined
- 🚧 ComponentFlow logic evolving
- 🗂️ Full documentation structured in Confluence
- ⏳ DB integration underway
- 💡 Future: AI-assisted shelving, recommendations, librarian sass rotation

------

## 🪄 **Custom CLI Prompt**

Bibby uses a simple cyan prompt:

```
return new AttributedString("Bibby:_ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
```

Minimal. Recognizable. Slightly nerdy.

------

## 🧭 **Version**

**v0.1** — Initial structure, command suite, documentation framework.

------

## 🧱 **Dev Log Highlights**

**2025-11-12 — Book Checkout (Persistent State)**

- Implemented real status tracking (`AVAILABLE → CHECKED_OUT`)
- Added personality-filled librarian messages
- Cleaned logs + stabilized flow
- Persistent state confirmed in Postgres

**2025-10-31 — Multi-Author Add Flow**

- Full interactive multi-author input
- Many-to-many Book ↔ Author implemented
- Transactional consistency added

**2025-10-31 — Shelf Assignment Flow**

- Interactive selection of bookcase + shelf
- Updates `BookEntity.shelf`
- Completes Book → Shelf → Bookcase relationship

(Full commit history available on GitHub.)

------

## 👤 **Author**

**Leo D. Penrose**
Builder • Systems Thinker • Lifelong Learner
[LinkedIn](#) • [GitHub](#)
