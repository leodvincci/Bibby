# Devlog: Enforcing Architecture with ArchUnit

**Date:** 2025-12-05  
**Focus:** Making architectural boundaries testable and enforceable  
**Commit Type:** `test`

---

## Summary

Added ArchUnit to the project and wrote the first architecture test: CLI packages cannot depend on infrastructure packages. This transforms architecture from "convention we try to follow" to "rule that breaks the build."

---

## What Changed

### 1. Added ArchUnit Dependency

**pom.xml:**
```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

### 2. Architecture Test

**CliArchitectureTest.java:**
```java
class CliArchitectureTest {

    private static final JavaClasses importedClasses =
            new ClassFileImporter().importPackages("com.penrose.bibby");

    @Test
    void cli_should_not_depend_on_infrastructure() {
        noClasses()
                .that().resideInAPackage("..cli..")
                .and().haveSimpleNameNotEndingWith("Test")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..infrastructure..",
                        "..external.."
                )
                .check(importedClasses);
    }
}
```

### 3. Fixed ShelfMapper Signature

```java
// Before
public Shelf toDomain(ShelfEntity shelfEntity, List<Book> books)

// After
public Shelf toDomain(ShelfEntity shelfEntity, List<Long> bookIds)
```

Updated `ShelfDomainRepositoryImpl` to extract IDs before calling mapper:
```java
List<Book> books = bookDomainRepository.getBooksByShelfId(id);
List<Long> bookIds = books.stream().map(Book::getId).toList();
return shelfMapper.toDomain(entity, bookIds);
```

### 4. Removed Unused Import

```java
// BookCommandLine.java

-
```

---

## Why ArchUnit?

### The Problem with Convention-Only Architecture

Without enforcement, architectural rules decay:

```
Day 1:    "CLI should only use facades and DTOs"
Day 30:   "Just this one ShelfEntity import, I'll fix it later"
Day 90:   "Why does CLI import 15 infrastructure classes?"
```

Every "just this once" exception becomes permanent. Architecture erodes through a thousand small compromises.

### The Solution: Tests That Fail

```java
noClasses()
    .that().resideInAPackage("..cli..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..infrastructure..")
    .check(importedClasses);
```

Now if someone adds an infrastructure import to CLI code:
1. Tests fail
2. Build fails
3. PR can't merge

The rule is no longer a suggestion. It's enforced.

---

## How ArchUnit Works

### 1. Import Classes

```java
private static final JavaClasses importedClasses =
        new ClassFileImporter().importPackages("com.penrose.bibby");
```

ArchUnit scans the compiled bytecode. It sees every class, every import, every dependency.

### 2. Define Rules

```java
noClasses()
    .that().resideInAPackage("..cli..")      // Source: CLI packages
    .should().dependOnClassesThat()
    .resideInAnyPackage("..infrastructure..") // Target: Infrastructure packages
```

The `..` is a wildcard that matches any sub-package depth.

### 3. Check

```java
.check(importedClasses);
```

Runs the rule against the imported classes. Throws `AssertionError` if violated.

---

## Current Architecture Rules

| Rule | Status |
|------|--------|
| CLI cannot import infrastructure | ✅ Enforced |
| CLI cannot import domain directly | ❌ Not yet |
| Domain cannot import infrastructure | ❌ Not yet |
| Application cannot import other modules' internals | ❌ Not yet |

### Planned Rules

```java
// Domain purity
@Test
void domain_should_not_depend_on_infrastructure() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..", "..application..")
        .check(importedClasses);
}

// Module boundaries
@Test
void book_module_should_not_depend_on_shelf_internals() {
    noClasses()
        .that().resideInAPackage("..book..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "..shelf.domain..",
            "..shelf.infrastructure..",
            "..shelf.application.."
        )
        .check(importedClasses);
}
```

---

## Known Violations to Fix

The test currently exposes a violation:

```java
// BookCommandLine.java still imports:
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
```

This needs to be replaced with `AuthorDTO` or `AuthorFacade` usage. The test is doing its job—it found a boundary violation I need to fix.

---

## Struggle Journal

### Challenge: Test Filtering

Initially the test flagged test classes themselves as violations (test classes can legitimately import infrastructure for testing). Added filter:

```java
.and().haveSimpleNameNotEndingWith("Test")
```

This excludes test classes from the rule.

### Challenge: Package Wildcards

ArchUnit uses `..` for recursive package matching:
- `..cli..` matches `com.penrose.bibby.cli` and all sub-packages
- `..infrastructure..` matches any `infrastructure` package at any depth

Initially used single dots which didn't match sub-packages.

---

## Interview Talking Points

### "How do you enforce architectural boundaries?"

> I use ArchUnit to write tests that verify package dependencies. For example, I have a test that fails if any CLI class imports from an infrastructure package. This turns architecture from documentation into executable rules. If someone violates a boundary, the build fails—they can't merge the code until they fix it.

### "Why test architecture instead of just documenting it?"

> Documentation gets stale. People forget to read it, or they make "temporary" exceptions that become permanent. Tests run on every build. They catch violations immediately, before code gets merged. It's the difference between "please don't do this" and "you cannot do this."

### "What architectural rules do you enforce?"

> Currently I enforce that CLI layers can only depend on API packages—facades and DTOs, not infrastructure or direct domain access. I'm adding rules for domain purity (domain can't import infrastructure) and module boundaries (Book module can't reach into Shelf internals). Each rule is a test that fails if violated.

---

## What's Next

1. **Fix AuthorEntity import in BookCommandLine** — Wire up AuthorFacade properly

2. **Add domain purity test** — `domain/` packages cannot import `infrastructure/`

3. **Add module boundary tests** — Modules only access each other through `api/` packages

4. **Consider ArchUnit freeze** — Capture current violations, prevent new ones while fixing old

---

## Files Changed

| File | Change |
|------|--------|
| `pom.xml` | Added ArchUnit 1.3.0 dependency |
| `CliArchitectureTest.java` | New test enforcing CLI-infrastructure boundary |
| `ShelfMapper.java` | Parameter changed from `List<Book>` to `List<Long>` |
| `ShelfDomainRepositoryImpl.java` | Extract book IDs before mapping |
| `BookCommandLine.java` | Removed unused `Author` import |

---

## Reflection

This feels like a turning point. Before, I was refactoring toward clean architecture and hoping it stayed clean. Now I have a test that will fail if it drifts. The architecture is self-defending.

The test also immediately found a violation I'd missed (`AuthorEntity` import). That's exactly what it's for—catching things humans overlook.

The next step is fixing that violation by completing the `AuthorFacade` wiring, then adding more rules until the entire hexagonal structure is enforced.
