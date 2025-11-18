# Java & Spring Fundamentals Learning Guide - Master Index
## Your Personalized Journey Through Bibby

**Project:** Bibby - Library Management CLI Application
**Developer:** Building solid fundamentals through real code
**Focus:** Java 17 + Spring Boot 3.5.7 + Spring Shell + PostgreSQL
**Approach:** Every concept grounded in YOUR actual code - zero generic examples

---

## Progress Tracker

- [x] **Section 1:** Codebase Foundation ✅ **(COMPLETE)**
- [x] **Section 2:** Java Type System & Primitives ✅ **(COMPLETE)**
- [x] **Section 3:** Control Flow & Logic ✅ **(COMPLETE)**
- [x] **Section 4:** Methods & Parameters ✅ **(COMPLETE)**
- [x] **Section 5:** OOP - Encapsulation & Abstraction ✅ **(COMPLETE)**
- [x] **Section 6:** OOP - Inheritance & Polymorphism ✅ **(COMPLETE)**
- [x] **Section 7:** Constructors & Object Lifecycle ✅ **(COMPLETE)**
- [ ] Section 8: Packages & Visibility
- [ ] Section 9: Collections Framework
- [ ] Section 10: Generics & Type Safety
- [ ] Section 11: Exception Handling
- [ ] Section 12: Memory Model Basics
- [ ] Section 13: Lambda Expressions & Functional Interfaces
- [ ] Section 14: Stream API
- [ ] Section 15: Optional Best Practices
- [ ] Section 16: Records & Modern Features
- [ ] Section 17: JUnit & Testing Strategy
- [ ] Section 18: Mockito & Mocking
- [ ] Section 19: Maven & Build Process
- [ ] Section 20: Logging Strategy
- [ ] Section 21: Spring IoC & Dependency Injection
- [ ] Section 22: Component Scanning & Stereotypes
- [ ] Section 23: Bean Lifecycle & Scopes
- [ ] Section 24: Spring Boot Auto-Configuration
- [ ] Section 25: Spring Data JPA Deep Dive
- [ ] Section 26: Entity Design & JPA
- [ ] Section 27: DTO Pattern & Layer Boundaries
- [ ] Section 28: Validation
- [ ] Section 29: Transaction Management
- [ ] Section 30: Spring Shell Commands
- [ ] Section 31: Comprehensive Code Review
- [ ] Section 32: Hands-On Exercises
- [ ] Section 33: Your Personalized Learning Roadmap

**Last Updated:** 2025-11-17
**Sections Completed:** 7 / 33
**Current Focus:** Constructors & Object Lifecycle Complete

---

## Section Guide

### Foundation (Section 1) ✅

**Section 1: Your Codebase - The Foundation**
- **File:** `01-codebase-foundation.md`
- **Duration:** 20-25 min read
- **Prerequisites:** None
- **Status:** ✅ COMPLETE
- **Key Outcomes:**
  - Understand Bibby's domain (library management)
  - Grasp architectural layers (Commands → Services → Repositories → Entities)
  - Recognize technology stack (Spring Boot 3.5.7, Java 17, PostgreSQL)
  - Identify strengths (constructor injection, records, transactions)
  - Acknowledge growth areas (testing, Optional usage, validation)

### Core Java Fundamentals (Sections 2-12)

**Section 2: Java Type System & Primitives**
- **File:** `02-type-system-primitives.md`
- **Duration:** 20-25 min read
- **Prerequisites:** Section 1
- **Status:** ✅ COMPLETE
- **Key Outcomes:**
  - Understand primitives (int, long) vs wrappers (Integer, Long)
  - Learn when to use each type in JPA entities
  - Identify autoboxing risks and NullPointerException traps
  - Refactor BookEntity numeric fields for consistency
  - Master null safety with wrapper types

**Section 3: Control Flow & Logic**
- **File:** `03-control-flow-logic.md`
- **Duration:** 25-30 min read
- **Prerequisites:** Sections 1-2
- **Status:** ✅ COMPLETE
- **Key Outcomes:**
  - Master guard clauses to reduce nesting (flatten pyramid of doom)
  - Use switch expressions instead of if-else chains (Java 14+)
  - Safe Optional handling (check isEmpty() before get())
  - Short-circuit evaluation for null safety
  - Choose appropriate loop types (for vs enhanced for)

**Section 4: Methods & Parameters**
- **File:** `04-methods-parameters.md`
- **Duration:** 25-30 min read
- **Prerequisites:** Sections 1-3
- **Status:** ✅ COMPLETE
- **Key Outcomes:**
  - Understand pass-by-value (Java copies references, not objects)
  - Master method overloading (multiple constructors in BookcaseEntity)
  - Add parameter validation (null checks, fail-fast principle)
  - Return Optional instead of null for queries
  - Rename confusing methods (setAuthors → addAuthor)
  - Apply single responsibility to methods

**Section 5: OOP - Encapsulation & Abstraction**
- **File:** `05-oop-encapsulation-abstraction.md`
- **Duration:** 30-35 min read
- **Prerequisites:** Sections 1-4
- **Status:** ✅ COMPLETE
- **Key Outcomes:**
  - Fix package-private fields (Genre, ShelfService)
  - Use defensive copies for collections (BookEntity.authors)
  - Understand access modifiers (private, public, protected, package-private)
  - Build rich domain models (add behavior to entities)
  - Master repository interfaces as abstractions
  - Apply information hiding principles

**Section 6: OOP - Inheritance & Polymorphism**
- **File:** `06-oop-inheritance-polymorphism.md`
- **Duration:** 30-35 min read
- **Prerequisites:** Sections 1-5
- **Status:** ✅ COMPLETE
- **Key Outcomes:**
  - Understand IS-A vs HAS-A relationships (inheritance vs composition)
  - Master @Override annotation (compiler safety)
  - Recognize polymorphism in action (repository interfaces)
  - Apply favor composition over inheritance principle
  - Avoid fragile base class problem
  - Use interface-first design pattern

**Section 7: Constructors & Object Lifecycle**
- **File:** `07-constructors-lifecycle.md`
- **Duration:** 30-35 min read
- **Prerequisites:** Sections 1-6
- **Status:** ✅ COMPLETE
- **Key Outcomes:**
  - Master constructor overloading (BookcaseEntity pattern)
  - Eliminate derived state (AuthorEntity fullName)
  - Use protected no-arg constructors for JPA
  - Add validation to record constructors
  - Apply builder pattern for complex objects
  - Understand JPA entity lifecycle
  - Create immutable objects with records

**Section 8: Packages & Visibility**
- **Planned Topics:** Package structure, access modifiers, visibility boundaries
- **Real Examples From:** com.penrose.bibby.library.* organization, public/private/package-private usage
- **Duration:** 10-15 min read
- **Prerequisites:** Sections 1-7

**Section 9: Collections Framework**
- **Planned Topics:** List/Set/Map usage, initialization patterns, immutability
- **Real Examples From:** Set<AuthorEntity> in BookEntity, LinkedHashMap in BookCommands, repository return types
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 1-8

**Section 10: Generics & Type Safety**
- **Planned Topics:** Generic types, bounded parameters, type erasure, wildcards
- **Real Examples From:** JpaRepository<BookEntity, Long>, repository method signatures, Optional<T>
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 1-9

**Section 11: Exception Handling**
- **Planned Topics:** Checked vs unchecked, exception hierarchy, custom exceptions
- **Real Examples From:** ResponseStatusException in BookcaseService, transaction rollback scenarios
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 1-10

**Section 12: Memory Model Basics**
- **Planned Topics:** Stack vs heap, object references, garbage collection, immutability
- **Real Examples From:** Entity lifecycle, String handling, collection memory patterns
- **Duration:** 15-20 min read
- **Prerequisites:** Sections 1-11

### Modern Java Features (Sections 13-16)

**Section 13: Lambda Expressions & Functional Interfaces**
- **Planned Topics:** Lambda syntax, functional interfaces, method references
- **Real Examples From:** Opportunities in for-loops, stream replacements, Optional chaining
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 1-12

**Section 14: Stream API**
- **Planned Topics:** Stream operations, map/filter/collect, readability tradeoffs
- **Real Examples From:** For-loops in BookCommands, collection transformations, query results processing
- **Duration:** 25-30 min read
- **Prerequisites:** Section 13

**Section 15: Optional Best Practices**
- **Planned Topics:** Optional anti-patterns, chaining, proper usage, when NOT to use
- **Real Examples From:** BookService.findBookByTitle, get() calls in BookCommands, null returns
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 13-14

**Section 16: Records & Modern Features**
- **Planned Topics:** Records for DTOs, text blocks, switch expressions, var inference
- **Real Examples From:** BookRequestDTO, BookSummary, native queries, opportunities for var
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 13-15

### Development Craft (Sections 17-20)

**Section 17: JUnit & Testing Strategy**
- **Planned Topics:** Test structure, AAA pattern, test coverage, unit vs integration
- **Real Examples From:** Missing tests for BookService, empty BookCommandsTest, testing opportunities
- **Duration:** 25-30 min read
- **Prerequisites:** Sections 1-16

**Section 18: Mockito & Mocking**
- **Planned Topics:** Mocking patterns, stubbing, verification, when to mock
- **Real Examples From:** Testing services with repository mocks, testing commands with service mocks
- **Duration:** 20-25 min read
- **Prerequisites:** Section 17

**Section 19: Maven & Build Process**
- **Planned Topics:** Dependency management, build lifecycle, plugins
- **Real Examples From:** Your pom.xml, Spring Boot parent, dependency versions
- **Duration:** 15-20 min read
- **Prerequisites:** Section 1

**Section 20: Logging Strategy**
- **Planned Topics:** Log levels, logging patterns, structured logging, performance
- **Real Examples From:** SLF4J in BookcaseService, System.out.println in BookCommands, logging gaps
- **Duration:** 15-20 min read
- **Prerequisites:** Section 1

### Spring & Spring Boot (Sections 21-30)

**Section 21: Spring IoC & Dependency Injection**
- **Planned Topics:** ApplicationContext, bean definitions, constructor injection, circular dependencies
- **Real Examples From:** BookService constructor, AuthorService missing injection, bean lifecycle
- **Duration:** 25-30 min read
- **Prerequisites:** Sections 1-20

**Section 22: Component Scanning & Stereotypes**
- **Planned Topics:** @Component, @Service, @Repository usage, component scan config
- **Real Examples From:** Your service classes, repository interfaces, command components
- **Duration:** 15-20 min read
- **Prerequisites:** Section 21

**Section 23: Bean Lifecycle & Scopes**
- **Planned Topics:** Singleton beans, scope decisions, initialization callbacks
- **Real Examples From:** Service singletons, stateful vs stateless beans, lifecycle hooks
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 21-22

**Section 24: Spring Boot Auto-Configuration**
- **Planned Topics:** Starters, auto-configuration, customization
- **Real Examples From:** Your pom.xml dependencies, application.properties, auto-configured beans
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 21-23

**Section 25: Spring Data JPA Deep Dive**
- **Planned Topics:** Repository magic, query methods, custom queries, projections
- **Real Examples From:** BookRepository query methods, native queries, BookSummary projections
- **Duration:** 30-35 min read
- **Prerequisites:** Sections 21-24

**Section 26: Entity Design & JPA**
- **Planned Topics:** Entity annotations, relationships, ID generation, equals/hashCode
- **Real Examples From:** BookEntity, AuthorEntity, @ManyToMany, missing @ManyToOne opportunities
- **Duration:** 30-35 min read
- **Prerequisites:** Section 25

**Section 27: DTO Pattern & Layer Boundaries**
- **Planned Topics:** DTO usage, entity exposure, mapping strategies
- **Real Examples From:** BookRequestDTO, BookEntity vs Book confusion, controller layer
- **Duration:** 25-30 min read
- **Prerequisites:** Sections 25-26

**Section 28: Validation**
- **Planned Topics:** Bean Validation, custom validators, validation groups
- **Real Examples From:** Missing @NotNull/@Size, validation opportunities in DTOs and entities
- **Duration:** 20-25 min read
- **Prerequisites:** Section 27

**Section 29: Transaction Management**
- **Planned Topics:** @Transactional, propagation, rollback rules
- **Real Examples From:** BookService.createNewBook, missing transactions, transaction boundaries
- **Duration:** 25-30 min read
- **Prerequisites:** Sections 25-28

**Section 30: Spring Shell Commands**
- **Planned Topics:** Command structure, parameter binding, testing commands
- **Real Examples From:** BookCommands, ComponentFlow usage, command vs service separation
- **Duration:** 20-25 min read
- **Prerequisites:** Sections 21-29

### Integration & Practice (Sections 31-33)

**Section 31: Comprehensive Code Review**
- **Planned Topics:** SRP violations, design weaknesses, priority refactorings
- **Real Examples From:** Complete codebase analysis with specific improvements
- **Duration:** 40-50 min read
- **Prerequisites:** Sections 1-30

**Section 32: Hands-On Exercises**
- **Planned Topics:** 15 refactoring exercises with solutions
- **Real Examples From:** All concepts applied to your actual code
- **Duration:** Variable (practice-based)
- **Prerequisites:** Sections 1-31

**Section 33: Your Personalized Learning Roadmap**
- **Planned Topics:** 12-week plan, daily habits, resources, milestones
- **Real Examples From:** Your specific gaps and growth areas
- **Duration:** 30-40 min read
- **Prerequisites:** Sections 1-32

---

## Quick Navigation by Topic

**OOP Concepts:** Sections 5, 6, 7
**Collections & Generics:** Sections 9, 10
**Modern Java:** Sections 13, 14, 15, 16
**Testing:** Sections 17, 18
**Spring Core:** Sections 21, 22, 23, 24
**Spring Data:** Sections 25, 26, 27
**Professional Practices:** Sections 19, 20, 28, 29, 30

---

## Quick Navigation by Priority

**High Priority (Critical Gaps):**
- Section 17: JUnit & Testing Strategy
- Section 15: Optional Best Practices
- Section 26: Entity Design & JPA (equals/hashCode)
- Section 27: DTO Pattern & Layer Boundaries
- Section 28: Validation

**Medium Priority (Enhancement):**
- Section 11: Exception Handling
- Section 29: Transaction Management
- Section 25: Spring Data JPA Deep Dive
- Section 14: Stream API
- Section 20: Logging Strategy

**Advanced (Mastery):**
- Section 31: Comprehensive Code Review
- Section 32: Hands-On Exercises
- Section 33: Personalized Learning Roadmap
- Section 16: Records & Modern Features
- Section 24: Spring Boot Auto-Configuration

---

## Learning Path Recommendations

**Intensive Track (4 weeks):**
- **Week 1:** Sections 1-8 (Foundation + Core Java)
- **Week 2:** Sections 9-16 (Collections, Modern Java)
- **Week 3:** Sections 17-24 (Testing + Spring Core)
- **Week 4:** Sections 25-33 (Spring Data + Integration)

**Balanced Track (8 weeks):**
- **Weeks 1-2:** Sections 1-6 (Foundation + OOP)
- **Weeks 3-4:** Sections 7-12 (Constructors, Collections, Exceptions)
- **Weeks 5-6:** Sections 13-20 (Modern Java + Craft)
- **Weeks 7-8:** Sections 21-33 (Spring + Integration)

**Thorough Track (12 weeks - RECOMMENDED):**
- **Weeks 1-2:** Sections 1-4 (Foundation + Core Java)
- **Weeks 3-4:** Sections 5-8 (OOP Deep Dive)
- **Weeks 5-6:** Sections 9-12 (Collections, Generics, Exceptions)
- **Weeks 7-8:** Sections 13-16 (Modern Java)
- **Weeks 9-10:** Sections 17-26 (Testing + Spring)
- **Weeks 11-12:** Sections 27-33 (Integration & Practice)

---

## Action Items Summary

### Week 1 (Section 1 Complete)

**From Section 1:**
- [ ] Add test coverage for BookService.createNewBook()
- [ ] Audit Optional usage across all services
- [ ] Add equals() and hashCode() to BookEntity

**Total Action Items:** 3
**Completed:** 0
**In Progress:** 0
**Priority Items:** All HIGH priority

---

## Files & Resources

**Completed Section Files:**
- [01-codebase-foundation.md](./01-codebase-foundation.md) ✅

**Supporting Materials:** *(Will be created at journey's end)*
- Complete Consolidated Guide
- Quick Reference Cheatsheet
- Master Action Items List
- Refactoring Checklist

---

## Your Bibby Statistics

**Codebase Metrics:**
- Total Java Files: 43
- Main Code: 41 files
- Tests: 2 files (4.6% coverage)
- Entities: 5 (Book, Author, Shelf, Bookcase, Catalog)
- Services: 5
- Repositories: 5
- Controllers: 5 REST + 2 CLI

**Technology Stack:**
- Spring Boot 3.5.7
- Java 17
- Spring Shell 3.4.1
- Spring Data JPA
- PostgreSQL
- Maven

**Domain:**
Library management system for organizing books, authors, shelves, and bookcases with both CLI and REST interfaces.

---

## How to Use This Guide

1. **Read sections sequentially** - Each builds on previous concepts
2. **Complete action items** before moving forward
3. **Practice exercises** reinforce learning
4. **Refer back** to earlier sections as needed
5. **Take breaks** - Absorb concepts before rushing ahead

**After Each Section:**
- ✅ Read the section (15-35 min)
- ✅ Complete the practice exercise
- ✅ Implement the action items
- ✅ Commit your changes
- ✅ Mark the section complete here
- ✅ Request next section

**Remember:** This is YOUR learning journey. Go at YOUR pace. The code isn't going anywhere, but your understanding will grow with each section.

---

**Mentor's Note:**

You've completed the foundation analysis. You now understand what Bibby is, how it's structured, what you're doing well, and where you'll grow.

Every subsequent section will reference **your actual code** from these files. No generic User/Product examples. Just real learning from real code.

Take pride in what you've built. Now let's make it even better.

---

*Master Index Last Updated: 2025-11-17*
*Journey Started: 2025-11-17*
*Current Section: 1 of 33*
*Next Up: Section 2 - Java Type System & Primitives*
