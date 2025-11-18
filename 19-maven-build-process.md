# Section 19: Maven & Build Process
## Understanding Bibby's Dependency Management

**File:** `pom.xml`
**Concept:** Maven coordinates, dependency management, build lifecycle
**Time:** 45 min read

---

## What You'll Learn

By the end of this section, you'll understand:

- What Maven coordinates (groupId, artifactId, version) mean
- How parent POM inheritance works in Spring Boot
- The difference between `<dependencies>` and `<dependencyManagement>`
- Dependency scopes (compile, runtime, test)
- What Spring Boot starters are and how they simplify configuration
- Maven build lifecycle phases
- How transitive dependencies work
- Essential Maven commands for daily development

Every concept is explained using **your actual `pom.xml`** from Bibby.

---

## Why Maven Matters

Maven is the **dependency manager and build tool** for Java projects. It:

1. **Downloads libraries** (Spring Boot, JUnit, PostgreSQL driver) automatically
2. **Manages versions** so you don't have conflicting libraries
3. **Builds your project** (compiles code, runs tests, packages JAR)
4. **Handles transitive dependencies** (if you depend on A, and A depends on B, Maven gets B too)

Without Maven, you'd manually download hundreds of JAR files and manage version conflicts yourself. Maven automates this entirely.

---

## Your pom.xml Structure

Let's analyze Bibby's `pom.xml` section by section:

**pom.xml:5-10**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.7</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
```

**What this means:**

Bibby **inherits** from the Spring Boot parent POM. This single line gives you:
- Pre-configured versions for 200+ libraries
- Sensible defaults for Maven plugins
- Java 17 compiler settings
- UTF-8 encoding
- Dependency management for entire Spring ecosystem

**Analogy:** It's like inheriting a fully-furnished house instead of buying furniture piece by piece.

---

## Maven Coordinates: The GAV Model

**pom.xml:11-13**
```xml
<groupId>com.penrose</groupId>
<artifactId>Bibby</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

Every Maven project (and dependency) is identified by **GAV**:

| Part | Bibby's Value | Meaning |
|------|---------------|---------|
| **G**roupId | `com.penrose` | Your organization/company (reverse domain name) |
| **A**rtifactId | `Bibby` | Project name (what you're building) |
| **V**ersion | `0.0.1-SNAPSHOT` | Version number (-SNAPSHOT = work in progress) |

**Full Coordinate:** `com.penrose:Bibby:0.0.1-SNAPSHOT`

When you publish Bibby to a Maven repository, other projects could depend on it using:

```xml
<dependency>
    <groupId>com.penrose</groupId>
    <artifactId>Bibby</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**SNAPSHOT versions** indicate development builds. When you release version 1.0.0, you'd remove `-SNAPSHOT`:

```xml
<version>1.0.0</version>
```

---

## Properties: Configuration Variables

**pom.xml:29-32**
```xml
<properties>
    <java.version>17</java.version>
    <spring-shell.version>3.4.1</spring-shell.version>
</properties>
```

Properties are **variables** you can reference elsewhere in the POM:

- `java.version`: Tells Maven to compile with Java 17
- `spring-shell.version`: Centralizes Spring Shell version (referenced in `<dependencyManagement>`)

**Why centralize versions?**

Without properties:
```xml
<dependency>
    <groupId>org.springframework.shell</groupId>
    <artifactId>spring-shell-starter</artifactId>
    <version>3.4.1</version>
</dependency>
<dependency>
    <groupId>org.springframework.shell</groupId>
    <artifactId>spring-shell-starter-test</artifactId>
    <version>3.4.1</version>  <!-- Duplicate! Risk of mismatch -->
</dependency>
```

With properties:
```xml
<spring-shell.version>3.4.1</spring-shell.version>

<!-- Later, reference with ${spring-shell.version} -->
<version>${spring-shell.version}</version>
```

Now you **update once** to upgrade all Spring Shell dependencies.

---

## Dependencies: Your Project's Libraries

**pom.xml:33-62**
```xml
<dependencies>
    <!-- 7 dependencies total -->
</dependencies>
```

Bibby declares **7 direct dependencies**. Let's analyze each:

### 1. Spring Shell Starter (CLI Framework)

**pom.xml:34-37**
```xml
<dependency>
    <groupId>org.springframework.shell</groupId>
    <artifactId>spring-shell-starter</artifactId>
</dependency>
```

**Notice:** No `<version>` tag!

**Why?** Version comes from `<dependencyManagement>` (see pom.xml:63-73). This ensures all Spring Shell artifacts use the same version (3.4.1).

**What it provides:**
- Spring Shell framework (your CLI commands in `BookCommands.java`, `BookcaseCommands.java`)
- `@ShellComponent`, `@ShellMethod` annotations
- ComponentFlow for interactive prompts
- Terminal utilities

**Scope:** `compile` (default) - Available at compile time, runtime, and in tests

---

### 2. Spring Boot Starter Test (Testing Framework)

**pom.xml:39-43**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Scope:** `test` - Only available when running tests, NOT included in final JAR

**What it provides:**
- JUnit 5 (Jupiter)
- Mockito
- AssertJ
- Spring Test utilities
- Hamcrest matchers
- JSONassert

This **one dependency** gives you everything from Sections 17-18!

**Test scope saves disk space:** If you deploy Bibby to production, test libraries aren't packaged (smaller JAR).

---

### 3. Spring Shell Starter Test

**pom.xml:44-48**
```xml
<dependency>
    <groupId>org.springframework.shell</groupId>
    <artifactId>spring-shell-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Scope:** `test`

**What it provides:**
- Testing utilities for Shell commands
- `@ShellTest` annotation
- Command execution assertions

This lets you test `BookCommands.java` methods without manually typing in the CLI.

---

### 4. Spring Boot Starter Web (REST API)

**pom.xml:49-52**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**What it provides:**
- Spring MVC framework
- Embedded Tomcat server
- JSON serialization (Jackson)
- `@RestController`, `@GetMapping`, `@PostMapping` annotations

This powers your REST controllers (`BookController.java`, `BookcaseController.java`).

**Scope:** `compile` (default)

---

### 5. Spring Boot Starter Data JPA (Database Layer)

**pom.xml:53-56**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**What it provides:**
- Hibernate ORM (object-relational mapping)
- Spring Data JPA
- JpaRepository interface
- `@Entity`, `@Id`, `@ManyToMany` annotations
- Transaction management

This enables your repositories (`BookRepository.java`, etc.) and entities (`BookEntity.java`, etc.).

**Scope:** `compile`

---

### 6. PostgreSQL Driver (Database Connector)

**pom.xml:57-61**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Scope:** `runtime` - NOT needed at compile time, only when running the app

**Why runtime scope?**

Your code never directly imports PostgreSQL classes:

```java
// ❌ You never write this:
import org.postgresql.Driver;

// ✅ You write this (generic JDBC/JPA):
public interface BookRepository extends JpaRepository<BookEntity, Long> {}
```

The PostgreSQL driver is loaded **at runtime** via JDBC. Spring Boot auto-configuration detects it and configures the connection.

**Version:** Not specified - inherited from `spring-boot-starter-parent` (ensures compatibility with Spring Boot 3.5.7)

---

## Dependency Scopes Summary

| Scope | Compile Time | Runtime | Test Runtime | Packaged in JAR |
|-------|--------------|---------|--------------|-----------------|
| `compile` (default) | ✅ | ✅ | ✅ | ✅ |
| `runtime` | ❌ | ✅ | ✅ | ✅ |
| `test` | ❌ | ❌ | ✅ | ❌ |
| `provided` | ✅ | ❌ | ✅ | ❌ |

**In Bibby:**
- **compile:** spring-shell-starter, spring-boot-starter-web, spring-boot-starter-data-jpa
- **runtime:** postgresql
- **test:** spring-boot-starter-test, spring-shell-starter-test

---

## Dependency Management vs Dependencies

**pom.xml:63-73**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.shell</groupId>
            <artifactId>spring-shell-dependencies</artifactId>
            <version>${spring-shell.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Key distinction:**

- `<dependencyManagement>` **declares** versions but **doesn't add** dependencies to your project
- `<dependencies>` **actually adds** libraries to your classpath

**Think of it like a menu:**
- `<dependencyManagement>` is the **menu** (lists what's available and prices)
- `<dependencies>` is your **order** (what you actually get)

**What's happening here:**

1. Bibby imports the Spring Shell **BOM** (Bill of Materials) - a POM that defines versions for all Spring Shell artifacts
2. When you add `spring-shell-starter` in `<dependencies>` without a version, Maven looks up the version from the imported BOM
3. Version resolves to `3.4.1` (from `${spring-shell.version}`)

**BOM benefits:**

- Guarantees compatible versions across related libraries
- Prevents "version hell" (spring-shell-starter 3.4.1 with spring-shell-core 3.2.0 = crashes)
- Centralized version management

---

## Parent POM Magic

**pom.xml:5-10**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.7</version>
    <relativePath/>
</parent>
```

By inheriting from `spring-boot-starter-parent`, Bibby automatically gets:

**1. Dependency versions for 200+ libraries:**

```xml
<!-- You write: -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <!-- No version needed -->
</dependency>

<!-- Parent provides: -->
<!-- version 42.7.5 (compatible with Spring Boot 3.5.7) -->
```

**2. Plugin configuration:**

```xml
<!-- Maven compiler plugin configured for Java 17 -->
<!-- Maven surefire plugin for running tests -->
<!-- Maven resources plugin for copying resources -->
```

**3. Build defaults:**

```xml
<!-- Source encoding: UTF-8 -->
<!-- Java version: 17 (from your <properties>) -->
<!-- Packaging: jar -->
```

**4. Property overrides:**

```xml
<properties>
    <!-- Override parent's Java version if needed -->
    <java.version>17</java.version>
</properties>
```

**To see what the parent POM provides:**

```bash
mvn help:effective-pom
```

This outputs the **effective POM** - your `pom.xml` merged with all parent POMs. You'll see 1000+ lines!

---

## Spring Boot Starters: The Secret Sauce

Notice how Bibby's dependencies end with `-starter`?

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-test`
- `spring-shell-starter`

**Starters are dependency bundles.** Each starter pulls in multiple related libraries.

**Example: spring-boot-starter-web**

When you add this one dependency, you get:
- `spring-webmvc` (Spring MVC framework)
- `spring-web` (Web utilities)
- `jackson-databind` (JSON serialization)
- `tomcat-embed-core` (Embedded web server)
- `tomcat-embed-el` (Expression language)
- `tomcat-embed-websocket` (WebSocket support)
- `spring-boot-starter` (Core Spring Boot)
- `spring-boot-autoconfigure` (Auto-configuration)
- And 20+ more transitive dependencies!

**Without starters, you'd write:**

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>6.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-core</artifactId>
    <version>10.1.34</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.2</version>
</dependency>
<!-- ...and 30 more -->
```

**With starters:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**That's it.** Spring Boot guarantees all versions are compatible.

---

## Transitive Dependencies

**Direct dependencies:** What you declare in `pom.xml` (7 in Bibby)

**Transitive dependencies:** What your dependencies depend on

**Example:**

```
Bibby
 └─ spring-boot-starter-data-jpa (DIRECT)
     ├─ spring-data-jpa (TRANSITIVE)
     ├─ hibernate-core (TRANSITIVE)
     │   ├─ jakarta.persistence-api (TRANSITIVE)
     │   ├─ byte-buddy (TRANSITIVE)
     │   └─ antlr4-runtime (TRANSITIVE)
     ├─ spring-orm (TRANSITIVE)
     └─ spring-tx (TRANSITIVE)
```

You declare **1 dependency**, Maven downloads **30+ JARs** automatically.

**To see all dependencies (direct + transitive):**

```bash
mvn dependency:tree
```

Output for Bibby:

```
[INFO] com.penrose:Bibby:jar:0.0.1-SNAPSHOT
[INFO] +- org.springframework.shell:spring-shell-starter:jar:3.4.1:compile
[INFO] |  +- org.springframework.shell:spring-shell-core:jar:3.4.1:compile
[INFO] |  +- org.springframework.shell:spring-shell-standard:jar:3.4.1:compile
[INFO] |  \- org.jline:jline:jar:3.28.0:compile
[INFO] +- org.springframework.boot:spring-boot-starter-web:jar:3.5.7:compile
[INFO] |  +- org.springframework.boot:spring-boot-starter:jar:3.5.7:compile
[INFO] |  +- org.springframework:spring-webmvc:jar:6.2.5:compile
[INFO] |  +- org.apache.tomcat.embed:tomcat-embed-core:jar:10.1.34:compile
... (100+ more lines)
```

---

## Build Section: Plugins

**pom.xml:75-82**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

**spring-boot-maven-plugin** enables:

**1. Executable JAR creation:**

```bash
mvn package
```

Creates `target/Bibby-0.0.1-SNAPSHOT.jar` - a **fat JAR** (includes all dependencies in one file).

You can run it with:

```bash
java -jar target/Bibby-0.0.1-SNAPSHOT.jar
```

**Without this plugin,** you'd get a JAR with only your code (no Spring, no PostgreSQL driver), and it wouldn't run.

**2. Spring Boot layered JARs** (for Docker optimization)

**3. Build-info generation** (for Spring Boot Actuator)

**Plugin configuration is inherited** from the parent POM, so you don't need to specify versions or configuration.

---

## Maven Build Lifecycle

Maven has **3 built-in lifecycles**, each with **phases**:

### 1. Default Lifecycle (Building)

Common phases in order:

| Phase | What It Does | Command |
|-------|--------------|---------|
| `validate` | Validates project structure | `mvn validate` |
| `compile` | Compiles `src/main/java` | `mvn compile` |
| `test` | Runs unit tests | `mvn test` |
| `package` | Creates JAR/WAR | `mvn package` |
| `verify` | Runs integration tests | `mvn verify` |
| `install` | Installs JAR to local repo | `mvn install` |
| `deploy` | Deploys to remote repo | `mvn deploy` |

**Phases execute sequentially.** Running `mvn package` runs:
1. validate
2. compile
3. test
4. package

**Skip tests:**

```bash
mvn package -DskipTests
```

### 2. Clean Lifecycle (Cleaning)

| Phase | What It Does |
|-------|--------------|
| `clean` | Deletes `target/` directory |

```bash
mvn clean
```

**Common combo:**

```bash
mvn clean install
```

(Deletes old build artifacts, then builds fresh)

### 3. Site Lifecycle (Documentation)

| Phase | What It Does |
|-------|--------------|
| `site` | Generates project documentation |

```bash
mvn site
```

(Creates HTML reports in `target/site/`)

---

## Essential Maven Commands for Bibby

**1. Clean build:**

```bash
mvn clean package
```

**What happens:**
1. Deletes `target/` (clean)
2. Compiles code
3. Runs tests (BibbyApplicationTests, BookCommandsTest)
4. Creates `target/Bibby-0.0.1-SNAPSHOT.jar`

---

**2. Run tests:**

```bash
mvn test
```

**What happens:**
- Compiles test code
- Runs all `@Test` methods in `src/test/java`
- Reports pass/fail

**Run single test class:**

```bash
mvn test -Dtest=BookCommandsTest
```

---

**3. Run application:**

```bash
mvn spring-boot:run
```

**What happens:**
- Compiles code
- Starts Spring Boot application
- Connects to PostgreSQL
- Launches Spring Shell CLI

(Equivalent to running `main()` in `BibbyApplication.java`)

---

**4. Install to local Maven repository:**

```bash
mvn install
```

**What happens:**
- Runs `mvn package`
- Copies JAR to `~/.m2/repository/com/penrose/Bibby/0.0.1-SNAPSHOT/`

**Why?** If you had another project depending on Bibby, it could find it in your local repository.

---

**5. View dependency tree:**

```bash
mvn dependency:tree
```

Shows all direct and transitive dependencies.

**Filter for specific dependency:**

```bash
mvn dependency:tree -Dincludes=org.postgresql
```

---

**6. Check for dependency updates:**

```bash
mvn versions:display-dependency-updates
```

Shows newer versions available for your dependencies.

---

**7. View effective POM:**

```bash
mvn help:effective-pom
```

Shows your `pom.xml` merged with parent POMs (helpful for debugging inheritance).

---

## Dependency Resolution: How Maven Finds JARs

**Where does Maven look for dependencies?**

**1. Local repository** (`~/.m2/repository/`)
- If JAR exists locally, use it

**2. Remote repositories** (if not found locally)
- Maven Central: https://repo.maven.apache.org/maven2/
- Spring repositories (configured in parent POM)

**3. Download and cache**
- Downloads JAR to local repository
- Future builds use cached version

**Example: PostgreSQL driver**

When you first run `mvn compile`:

```
Downloading from central: https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar
Downloaded: postgresql-42.7.5.jar (1.2 MB)
```

Cached to: `~/.m2/repository/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar`

Next build: **Instant** (uses cached JAR).

---

## Common pom.xml Mistakes

### ❌ Mistake 1: Specifying Versions for Spring Boot Starters

```xml
<!-- DON'T DO THIS -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.5.7</version>  <!-- ❌ Redundant -->
</dependency>
```

**Why it's wrong:** Parent POM already manages this version. Hardcoding it risks version conflicts.

**✅ Correct:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Version inherited from parent -->
</dependency>
```

---

### ❌ Mistake 2: Wrong Scope for Database Driver

```xml
<!-- DON'T DO THIS -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>compile</scope>  <!-- ❌ Too broad -->
</dependency>
```

**Why it's wrong:** Your code never imports PostgreSQL classes. `runtime` scope is sufficient and clearer.

**✅ Correct (Bibby's approach):**

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

### ❌ Mistake 3: Mixing Dependency Versions

```xml
<properties>
    <spring-shell.version>3.4.1</spring-shell.version>
</properties>

<dependency>
    <groupId>org.springframework.shell</groupId>
    <artifactId>spring-shell-starter</artifactId>
    <version>3.2.0</version>  <!-- ❌ Doesn't match property! -->
</dependency>
```

**Result:** Version conflict, potential runtime errors.

**✅ Correct (Bibby's approach):** Use `<dependencyManagement>` + BOM import, no version in `<dependencies>`.

---

## Your Action Items

**Priority 1: Understand Your Dependencies**

1. Run `mvn dependency:tree` to see all transitive dependencies
2. Identify how many total JARs Bibby uses (direct + transitive)
3. Find which dependency brings in `hibernate-core`

**Priority 2: Practice Maven Commands**

4. Run `mvn clean test` and verify tests pass
5. Run `mvn package` and check `target/` directory for the JAR
6. Run `java -jar target/Bibby-0.0.1-SNAPSHOT.jar` to start Bibby from the JAR

**Priority 3: Explore the Parent POM**

7. Run `mvn help:effective-pom > effective-pom.xml`
8. Open `effective-pom.xml` and search for `<dependencyManagement>` section
9. Find the version number for `postgresql` driver (inherited from parent)

**Priority 4: Check for Updates**

10. Run `mvn versions:display-dependency-updates`
11. Check if Spring Boot 3.5.7 is the latest version
12. Check if Spring Shell 3.4.1 is the latest version

---

## Key Takeaways

**1. Maven Coordinates (GAV):**
- Every library has groupId, artifactId, version
- Uniquely identifies dependencies

**2. Parent POM Inheritance:**
- `spring-boot-starter-parent` provides 200+ dependency versions
- Eliminates version conflicts
- Provides sensible build defaults

**3. Dependency Scopes:**
- `compile` (default): Always available
- `runtime`: Only at runtime (e.g., database drivers)
- `test`: Only for tests (e.g., JUnit, Mockito)

**4. Starters Simplify Configuration:**
- One starter = dozens of compatible libraries
- `spring-boot-starter-web` brings in Spring MVC, Tomcat, Jackson, etc.

**5. Transitive Dependencies:**
- Your 7 direct dependencies pull in 100+ transitive dependencies
- Maven resolves versions automatically

**6. Build Lifecycle:**
- `mvn compile` → Compiles code
- `mvn test` → Runs tests
- `mvn package` → Creates JAR
- `mvn clean` → Deletes build artifacts

**7. Spring Boot Maven Plugin:**
- Creates executable "fat JAR" with all dependencies
- Enables `mvn spring-boot:run`

---

## Bibby's Dependency Grade: A

**What you're doing well:**

✅ Using parent POM for version management
✅ Using starters instead of individual dependencies
✅ Correct scopes (`runtime` for PostgreSQL, `test` for test libraries)
✅ Centralizing versions with properties
✅ Using BOM import for Spring Shell

**No improvements needed** - your `pom.xml` follows Spring Boot best practices perfectly!

---

## What's Next

**Section 20: Logging Strategy**

Now that you understand how Maven brings in dependencies, we'll explore **logging** - the art of knowing what your application is doing.

You'll learn:
- SLF4J vs java.util.logging
- Log levels (DEBUG, INFO, WARN, ERROR)
- Where Bibby logs (and where it should log more)
- Structured logging for production systems
- Performance implications of logging

We'll analyze Bibby's actual logging gaps and teach you when to log, what to log, and how to log safely.

**Ready when you are!**

---

**Mentor's Note:**

Maven is the **invisible infrastructure** that makes Java development productive. You write 7 dependencies in your `pom.xml`, and Maven quietly downloads 100+ JARs, verifies compatibility, compiles your code, runs tests, and packages everything into a runnable JAR.

Understanding Maven transforms you from "I run `mvn package` and hope it works" to "I understand exactly what happens when I build my project."

You've now completed 19 of 33 sections. You're 58% through the fundamentals journey - well past halfway!

Take pride in your progress. Maven mastery is a professional superpower.

---

**Files Referenced:**
- `pom.xml:5-10` (Parent POM)
- `pom.xml:11-13` (Maven coordinates)
- `pom.xml:29-32` (Properties)
- `pom.xml:33-62` (Dependencies)
- `pom.xml:63-73` (Dependency management)
- `pom.xml:75-82` (Build plugins)

**Total Lines Analyzed:** 84 lines of XML (your entire build configuration!)

**Estimated Reading Time:** 45 minutes
**Estimated Action Items Time:** 30 minutes
**Total Section Time:** 75 minutes

---

*Section 19 Complete - Section 20: Logging Strategy Next*
