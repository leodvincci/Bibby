# Bibby

**A Spring Shell–powered personal library manager with barcode scanning, hierarchical storage (bookcase → shelf → book), and hexagonal architecture.**

[![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![Spring Shell](https://img.shields.io/badge/Spring%20Shell-3.4.1-green?style=flat-square)](https://spring.io/projects/spring-shell)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-red?style=flat-square&logo=apache-maven)](https://maven.apache.org/)
[![CI](https://github.com/leodvincci/Bibby/actions/workflows/ci.yml/badge.svg)](https://github.com/leodvincci/Bibby/actions/workflows/ci.yml)

---

## Table of Contents

- [Demo / Screenshots](#demo--screenshots)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Database](#database)
- [Auth](#auth)
- [API Docs](#api-docs)
- [Testing](#testing)
- [Lint / Format](#lint--format)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Demo / Screenshots

![Bibby intro](https://github.com/user-attachments/assets/740509ac-3692-4c69-bb3e-5af9f6a1082e)

*(Add screenshots of CLI and/or web UI here if you have them.)*

---

## Features

- **ISBN barcode scanning** — CLI and web; metadata from Google Books API
- **Hierarchical organization** — Bookcase → Shelf → Book
- **Shelf capacity** — Per-shelf limits; overflow prevented
- **Multi-author** — Many-to-many book–author
- **Availability** — AVAILABLE, CHECKED_OUT, RESERVED, LOST, ARCHIVED
- **Interactive CLI** — Spring Shell commands with prompts
- **REST API** — CRUD and search for books, bookcases, shelves; import by ISBN
- **Multi-user auth** — User registration, form login, session-based access; Spring Security with per-user bookcases/data
- **Hexagonal architecture** — Ports & adapters; ArchUnit in tests

---

## Tech Stack

| Layer        | Technology        | Notes                          |
|-------------|--------------------|--------------------------------|
| Language    | Java 17            | `pom.xml`; CI runs on Java 21  |
| Framework   | Spring Boot 3.5.7  | Web, Data JPA, Security        |
| CLI         | Spring Shell 3.4.1 | Interactive shell              |
| HTTP client | Spring WebFlux     | Google Books API               |
| Database    | PostgreSQL         | Runtime; H2 for tests          |
| API docs    | SpringDoc OpenAPI 2.8.0 | Swagger UI                 |
| Build       | Maven              | Wrapper: `./mvnw`              |
| Format      | Spotless (Google Java Format) | `spotless:check` / `apply` |

---

## Architecture Overview

- **Inbound:** Spring Shell CLI (`cli/command/`), REST controllers (`web/`).
- **Core:** Domain modules under `library/` — `cataloging` (book, author; includes book location/search queries), `stacks` (bookcase, shelf), `registration`, `classification` (booklist). Each has contracts (ports/DTOs), core (domain + application), infrastructure (JPA, external clients).
- **Outbound:** JPA repositories (PostgreSQL), Google Books client (WebClient).

```
  CLI + REST  →  Facades / DTOs  →  Application services  →  Domain
                     ↓
  PostgreSQL, Google Books API  ←  Repository interfaces  ←  Ports
```

---

## Getting Started

### Prerequisites

- **Java 17+** (OpenJDK/Temurin; CI uses 21)
- **PostgreSQL** (local or Docker)
- **Maven 3.8+** or use the project Maven Wrapper (`./mvnw`)

### Install

```bash
git clone https://github.com/leodvincci/Bibby.git
cd Bibby
```

### Configure

The app reads the database from **environment variables** (see [Environment Variables](#environment-variables)). There is no `.env.example` in the repo; set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for your PostgreSQL instance.

For a **local Postgres** (e.g. the Quickstart container below):

```bash
export DB_URL=jdbc:postgresql://localhost:5332/amigos
export DB_USERNAME=amigoscode
export DB_PASSWORD=password
```

### Run

**1. Start PostgreSQL (e.g. Docker)**

```bash
docker run -d \
  --name bibby-postgres \
  -e POSTGRES_DB=amigos \
  -e POSTGRES_USER=amigoscode \
  -e POSTGRES_PASSWORD=password \
  -p 5332:5432 \
  postgres:latest
```

**2. Start the app**

```bash
./mvnw spring-boot:run
```

**3. Use the CLI**

You should see the Bibby banner and prompt:

```
Bibby:_
```

Type `help` for commands.

**Run from JAR (after build):**

```bash
./mvnw clean package
java -jar target/Bibby-0.0.1-SNAPSHOT.jar
```

*Common gotcha:* If you see database connection errors, ensure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are set and match the running Postgres (host, port, database, user, password).

---

## Environment Variables

Used by `src/main/resources/application.properties` (no in-file defaults for DB).

| Variable       | Purpose                    | Example (local)                                      |
|----------------|----------------------------|------------------------------------------------------|
| `DB_URL`       | PostgreSQL JDBC URL        | `jdbc:postgresql://localhost:5332/amigos`            |
| `DB_USERNAME`  | Database user              | `amigoscode`                                         |
| `DB_PASSWORD`  | Database password          | `password`                                           |

Optional overrides (Spring Boot convention): `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` can override if you prefer.

The repo contains a `.env` and `.envrc` (e.g. `dotenv`); do not commit real secrets. Use `.env` locally and set the same variables in your deployment environment.

---

## Database

- **Schema:** Managed by Hibernate; `spring.jpa.hibernate.ddl-auto=create-drop` in the main config (schema recreated on each run; **data is lost on restart**).
- **Migrations:** No Flyway/Liquibase; schema is driven by JPA entities.
- **Seeds:** No seed scripts in the repo; use CLI or API to create data.
- **Tests:** Use an in-memory H2 database (`src/test/resources/application.properties`).

For production, switch to a different `ddl-auto` (e.g. `update` or `validate`) and/or add migrations in a separate change.

---

## Auth

Bibby supports **multiple users**: each user registers, logs in with their own credentials, and sees their own data (e.g. bookcases are scoped by user).

- **Registration:** `POST /api/v1/user/registration/register` with JSON body `{"email":"...", "password":"..."}`. Passwords are stored with BCrypt.
- **Login:** Form login; `POST /login` with form fields (e.g. `username` = email, `password`). Success returns 200 and JSON `{"message":"Login successful"}`; session cookie is set.
- **Session:** Authenticated requests use the session cookie. Most API endpoints require authentication. Permitted without auth: registration, Swagger UI, OpenAPI docs, `/actuator/health`, `/actuator/info`, `/actuator/readiness`, and `/h2-console/**`. Logout: `POST /logout` (default Spring Security).
- **CORS:** Allowed origins include `http://localhost:5173`, `https://*.vercel.app`, and `https://bibby-web.vercel.app`; credentials allowed. If the frontend runs elsewhere, update `WebSecurityConfigs.java`.

---

## API Docs

- **Swagger UI:** `http://localhost:8080/swagger-ui.html` (or `/swagger-ui/index.html`)
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

Base URL when running locally: `http://localhost:8080`. Protected endpoints require an authenticated session (log in via `/login` first when using the browser).

**Example endpoints (from the controllers):**

| Area | Method | Path | Notes |
|------|--------|------|--------|
| Auth | POST | `/api/v1/user/registration/register` | Body: `{"email","password"}` |
| Books | POST | `/api/v1/books/fetchbookmetadata` | Body: `{"isbn":"..."}`; returns metadata |
| Books | GET | `/api/v1/books/lookup/{isbn}` | Google Books lookup |
| Books | GET | `/api/v1/books/search/{isbn}` | Find in DB by ISBN |
| Books | GET | `/api/v1/books/findBookByTitle` | Body: `{"title":"..."}` (GET with body) |
| Books | POST | `/api/v1/books/addnewbook` | Body: book DTO (title, isbn, authors, publisher, shelfId) |
| Books | POST | `/api/v1/books/{bookId}/shelf` | Body: `{"shelfId":...}` |
| Books | GET | `/api/v1/books/shelf/{shelfId}` | Books on shelf |
| Books | GET | `/api/v1/books/booklocation` | Query: `?bookId=...` |
| Bookcase | POST | `/api/v1/bookcase/create` | Auth required. Body: `location`, `zone`, `indexId`, `shelfCount`, `shelfCapacity` |
| Bookcase | DELETE | `/api/v1/bookcase/delete/{bookcaseId}` | Auth required |
| Bookcase | GET | `/api/v1/bookcase/locations` | All locations |
| Bookcase | GET | `/api/v1/bookcase/location/{location}` | Bookcases at location |
| Bookcase | GET | `/api/v1/bookcase/all` | All bookcases for current user (auth) |
| Shelves | GET | `/api/v1/shelves/options` | All shelf options |
| Shelves | GET | `/api/v1/shelves/options/{bookcaseId}` | Shelf options for one bookcase |

Full request/response shapes: use Swagger UI at `/swagger-ui.html`.

---

## Testing

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=BookServiceTest

# Full build + tests (same as CI)
./mvnw clean verify
```

Test reports: `target/surefire-reports/` (and failsafe if integration tests are added). No JaCoCo plugin is configured in the repo; for coverage you would need to add the plugin and then run `./mvnw test jacoco:report` (TODO/verify if you add it).

---

## Lint / Format

Spotless (Google Java Format) is configured and runs in CI:

```bash
# Check only
./mvnw spotless:check

# Apply formatting
./mvnw spotless:apply
```

---

## Deployment

- **Docker:** The repo includes a multi-stage `Dockerfile`. Build and run the app image; the app expects `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` at runtime (e.g. pass via `-e` or your orchestrator’s env config).

  ```bash
  docker build -t bibby:latest .
  docker run -it --rm \
    -e DB_URL=jdbc:postgresql://host.docker.internal:5332/amigos \
    -e DB_USERNAME=amigoscode \
    -e DB_PASSWORD=password \
    -p 8080:8080 \
    bibby:latest
  ```

- **Railway:** Referenced in config (e.g. `application-dev.properties`); deploy by connecting the repo and setting the same env vars. No Railway-specific config files are in the repo.
- **Docker Compose:** Not in the repo; you can add a `docker-compose.yml` that starts Postgres and the app if you want one-command local run.

---

## Troubleshooting

| Issue | What to try |
|-------|-------------|
| **DB connection refused** | Ensure Postgres is running; check host/port in `DB_URL`; verify user/password and that the database exists. For Docker Postgres: `docker ps \| grep postgres`. |
| **Port 8080 in use** | Run on another port: `./mvnw spring-boot:run -Dserver.port=8081` |
| **CORS / cookie errors from frontend** | Session cookies use `SameSite=None; Secure`. Use HTTPS in production; for local frontend use `http://localhost:5173` (already in CORS config). Add other origins in `WebSecurityConfigs.java` if needed. |
| **Env vars not applied** | The app reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from the environment. Export them in the same shell where you run `./mvnw spring-boot:run`, or use a `.env` + tool that injects them (e.g. `dotenv` via `.envrc`). |
| **Data gone after restart** | Expected with `create-drop`. Use a different profile or override `spring.jpa.hibernate.ddl-auto` for persistence. |
| **Build / test failures** | `./mvnw clean install -U`; run tests with `./mvnw test`. CI runs `mvn -B -ntp clean verify` and `mvn spotless:check`. |

---

## Contributing

- Open issues and discussions on GitHub. For code changes, use a **feature branch** and open a **pull request** against `main`.
- CI must pass (`mvn clean verify` and `spotless:check`). Format with `./mvnw spotless:apply` before pushing.
- Conventions: hexagonal boundaries (see `docs/` and ArchUnit tests), package-by-feature under `library/`, and documented decisions in `docs/the-devlogs/`.

---

## License

This project is a personal learning project and is not licensed for external use. For educational or derivative use, please reach out for permission.

**Copyright © 2024–2026 Leo D. Penrose. All rights reserved.**

---

**Author:** [Leo D. Penrose](https://github.com/leodvincci) · [Building Bibby](https://buildingbibby.hashnode.dev/)
